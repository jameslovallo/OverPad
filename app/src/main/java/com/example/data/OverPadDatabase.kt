package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [PresetEntity::class, AppBindingEntity::class],
    version = 3,
    exportSchema = false
)
@TypeConverters(OverPadTypeConverters::class)
abstract class OverPadDatabase : RoomDatabase() {
    abstract fun overPadDao(): OverPadDao

    companion object {
        @Volatile
        private var INSTANCE: OverPadDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE presets ADD COLUMN leftMoveZoneEnabled INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `presets_new` (
                        `id` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `category` TEXT NOT NULL,
                        `isBuiltIn` INTEGER NOT NULL,
                        `opacity` REAL NOT NULL,
                        `gridRows` INTEGER NOT NULL,
                        `snapToGrid` INTEGER NOT NULL,
                        `leftMoveZoneEnabled` INTEGER NOT NULL,
                        `fpsRightZoneEnabled` INTEGER NOT NULL,
                        `fpsLookSensitivity` REAL NOT NULL,
                        `triggerDragAimEnabled` INTEGER NOT NULL,
                        `triggerDragMode` TEXT NOT NULL,
                        `triggerDragRadiusDp` REAL NOT NULL,
                        `invertRightY` INTEGER NOT NULL,
                        `controlsJson` TEXT NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO `presets_new` (
                        id, name, description, category, isBuiltIn, opacity, gridRows,
                        snapToGrid, leftMoveZoneEnabled, fpsRightZoneEnabled, fpsLookSensitivity,
                        triggerDragAimEnabled, triggerDragMode, triggerDragRadiusDp, invertRightY,
                        controlsJson, updatedAt
                    )
                    SELECT
                        id, name, description, category, isBuiltIn, opacity, 20,
                        snapToGrid, leftMoveZoneEnabled, fpsRightZoneEnabled, fpsLookSensitivity,
                        triggerDragAimEnabled, triggerDragMode, triggerDragRadiusDp, invertRightY,
                        controlsJson, updatedAt
                    FROM `presets`
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE `presets`")
                db.execSQL("ALTER TABLE `presets_new` RENAME TO `presets`")
            }
        }

        fun getInstance(context: Context): OverPadDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    OverPadDatabase::class.java,
                    "operpad.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
