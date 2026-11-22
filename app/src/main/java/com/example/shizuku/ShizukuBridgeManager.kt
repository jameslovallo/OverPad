package com.example.shizuku

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import com.example.BuildConfig
import com.example.xinput.XInputState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import rikka.shizuku.Shizuku
import java.util.concurrent.Executors

enum class ShizukuConnectionStage {
    NOT_INSTALLED,
    SERVICE_STOPPED,
    PERMISSION_REQUIRED,
    CONNECTING,
    CONNECTED
}

data class ShizukuStatus(
    val isShizukuInstalled: Boolean = false,
    val isBinderAlive: Boolean = false,
    val isPermissionGranted: Boolean = false,
    val isServiceConnected: Boolean = false,
    val remoteUid: Int? = null,
    val uinputWritable: Boolean = false,
    val injectedCount: Long = 0L,
    val lastError: String? = null
) {
    val stage: ShizukuConnectionStage
        get() = when {
            !isShizukuInstalled && !isBinderAlive -> ShizukuConnectionStage.NOT_INSTALLED
            !isBinderAlive -> ShizukuConnectionStage.SERVICE_STOPPED
            !isPermissionGranted -> ShizukuConnectionStage.PERMISSION_REQUIRED
            !isServiceConnected -> ShizukuConnectionStage.CONNECTING
            else -> ShizukuConnectionStage.CONNECTED
        }

    val modeLabel: String
        get() = when {
            stage != ShizukuConnectionStage.CONNECTED -> "Not Connected"
            remoteUid == 0 && uinputWritable -> "Root Mode (UID 0 • /dev/uinput + INJECT_EVENTS)"
            remoteUid == 0 -> "Root Mode (UID 0 • INJECT_EVENTS)"
            else -> "Wireless ADB / Shell Mode (UID ${remoteUid ?: 2000} • INJECT_EVENTS)"
        }
}

class ShizukuBridgeManager(private val context: Context) {
    private val binderExecutor = Executors.newSingleThreadExecutor()
    private val binderScope = CoroutineScope(SupervisorJob() + binderExecutor.asCoroutineDispatcher())

    private val _status = MutableStateFlow(ShizukuStatus())
    val status: StateFlow<ShizukuStatus> = _status.asStateFlow()

    @Volatile
    private var injector: IXInputInjector? = null
    private var isBound = false
    private var listenersRegistered = false

    private val userServiceArgs by lazy {
        Shizuku.UserServiceArgs(
            ComponentName(BuildConfig.APPLICATION_ID, ShizukuXInputService::class.java.name)
        )
            .daemon(false)
            .processNameSuffix("xinput_injector")
            .debuggable(BuildConfig.DEBUG)
            .version(BuildConfig.VERSION_CODE)
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            if (service == null || !service.pingBinder()) {
                _status.value = _status.value.copy(
                    isServiceConnected = false,
                    lastError = "Shizuku binder returned dead service"
                )
                return
            }
            val remote = IXInputInjector.Stub.asInterface(service)
            injector = remote
            binderScope.launch {
                val uid = runCatching { remote.uid }.getOrNull()
                val uinput = runCatching { remote.isUinputWritable }.getOrDefault(false)
                _status.value = _status.value.copy(
                    isServiceConnected = true,
                    remoteUid = uid,
                    uinputWritable = uinput,
                    lastError = null
                )
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            injector = null
            isBound = false
            _status.value = _status.value.copy(
                isServiceConnected = false,
                remoteUid = null
            )
        }
    }

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        refreshStatusAndAutoConnect()
    }

    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        injector = null
        isBound = false
        _status.value = _status.value.copy(
            isBinderAlive = false,
            isServiceConnected = false,
            remoteUid = null
        )
    }

    private val permissionResultListener =
        Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
            if (requestCode == SHIZUKU_PERMISSION_REQUEST_CODE) {
                val granted = grantResult == PackageManager.PERMISSION_GRANTED
                _status.value = _status.value.copy(isPermissionGranted = granted)
                if (granted) {
                    bindInjectorService()
                }
            }
        }

    fun initialize() {
        if (!listenersRegistered) {
            runCatching {
                Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
                Shizuku.addBinderDeadListener(binderDeadListener)
                Shizuku.addRequestPermissionResultListener(permissionResultListener)
                listenersRegistered = true
            }
        }
        refreshStatusAndAutoConnect()
    }

    fun refreshStatusAndAutoConnect() {
        val installed = isPackageInstalled(SHIZUKU_PACKAGE)
        val binderAlive = runCatching { Shizuku.pingBinder() }.getOrDefault(false)
        val permissionGranted = if (binderAlive) {
            runCatching {
                !Shizuku.isPreV11() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
            }.getOrDefault(false)
        } else {
            false
        }

        _status.value = _status.value.copy(
            isShizukuInstalled = installed,
            isBinderAlive = binderAlive,
            isPermissionGranted = permissionGranted
        )

        if (binderAlive && permissionGranted && injector == null && !isBound) {
            bindInjectorService()
        }
    }

    fun requestShizukuPermission() {
        runCatching {
            if (!Shizuku.pingBinder()) {
                _status.value = _status.value.copy(lastError = "Start Shizuku first before requesting access.")
                return
            }
            if (Shizuku.isPreV11()) {
                _status.value = _status.value.copy(lastError = "Please update Shizuku to v11 or newer.")
                return
            }
            if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                _status.value = _status.value.copy(isPermissionGranted = true)
                bindInjectorService()
            } else {
                Shizuku.requestPermission(SHIZUKU_PERMISSION_REQUEST_CODE)
            }
        }.onFailure { err ->
            _status.value = _status.value.copy(lastError = err.message)
        }
    }

    fun bindInjectorService() {
        if (isBound && injector != null) return
        runCatching {
            if (!Shizuku.pingBinder()) return
            isBound = true
            Shizuku.bindUserService(userServiceArgs, serviceConnection)
        }.onFailure { err ->
            isBound = false
            _status.value = _status.value.copy(lastError = err.message)
        }
    }

    fun unbindInjectorService() {
        runCatching {
            injector?.resetAll()
            Shizuku.unbindUserService(userServiceArgs, serviceConnection, true)
        }
        injector = null
        isBound = false
        _status.value = _status.value.copy(isServiceConnected = false, remoteUid = null)
    }

    fun pushXInputState(state: XInputState) {
        val remote = injector ?: return
        binderScope.launch {
            runCatching {
                remote.sendGamepadState(
                    state.buttonsMask,
                    state.leftTrigger,
                    state.rightTrigger,
                    state.thumbLX,
                    state.thumbLY,
                    state.thumbRX,
                    state.thumbRY,
                    state.hatX,
                    state.hatY
                )
                _status.value = _status.value.copy(injectedCount = _status.value.injectedCount + 1L)
            }.onFailure {
                injector = null
                isBound = false
                _status.value = _status.value.copy(isServiceConnected = false)
            }
        }
    }

    fun openShizukuApp(ctx: Context): Boolean {
        val launchIntent = ctx.packageManager.getLaunchIntentForPackage(SHIZUKU_PACKAGE)
        return if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ctx.startActivity(launchIntent)
            true
        } else {
            false
        }
    }

    fun openShizukuDownloadPage(ctx: Context) {
        val playIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("market://details?id=$SHIZUKU_PACKAGE")
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val startedPlay = runCatching { ctx.startActivity(playIntent) }.isSuccess
        if (!startedPlay) {
            val webIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://shizuku.rikka.app/download/")
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            runCatching { ctx.startActivity(webIntent) }
        }
    }

    private fun isPackageInstalled(pkg: String): Boolean {
        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(pkg, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(pkg, 0)
            }
            true
        }.getOrDefault(false)
    }

    fun release() {
        unbindInjectorService()
        if (listenersRegistered) {
            runCatching {
                Shizuku.removeBinderReceivedListener(binderReceivedListener)
                Shizuku.removeBinderDeadListener(binderDeadListener)
                Shizuku.removeRequestPermissionResultListener(permissionResultListener)
            }
            listenersRegistered = false
        }
        binderExecutor.shutdown()
    }

    companion object {
        const val SHIZUKU_PACKAGE = "moe.shizuku.manager"
        private const val SHIZUKU_PERMISSION_REQUEST_CODE = 9042
    }
}
