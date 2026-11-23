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
    version = 2,
    exportSchema = false
)
@TypeConverters(OperPadTypeConverters::class)
abstract class OperPadDatabase : RoomDatabase() {
    abstract fun operPadDao(): OperPadDao

    companion object {
        @Volatile
        private var INSTANCE: OperPadDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE presets ADD COLUMN leftMoveZoneEnabled INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        fun getInstance(context: Context): OperPadDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    OperPadDatabase::class.java,
                    "operpad.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
