package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("OverPad", appName)
    assertEquals("OverPad", context.getString(R.string.qs_tile_label))

    val standby = com.example.overlay.OverlayNotificationHelper.buildStandbyNotification(context, "FPS Pro")
    assertEquals("Enable", standby.actions?.firstOrNull()?.title?.toString())

    val active = com.example.overlay.OverlayNotificationHelper.buildActiveNotification(context, "FPS Pro")
    assertEquals("Disable", active.actions?.firstOrNull()?.title?.toString())
  }
}
