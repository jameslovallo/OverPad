package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [PresetEntity::class, AppBindingEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(OperPadTypeConverters::class)
abstract class OperPadDatabase : RoomDatabase() {
    abstract fun operPadDao(): OperPadDao

    companion object {
        @Volatile
        private var INSTANCE: OperPadDatabase? = null

        fun getInstance(context: Context): OperPadDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    OperPadDatabase::class.java,
                    "operpad.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
