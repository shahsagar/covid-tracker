package com.sagar.assignment1

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Sign::class], version = 2)
abstract class AppDatabase : RoomDatabase() {
    abstract fun signDao(): SignDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        val DATABASE_NAME = "database.db"

        fun getDatabase(context: Context): AppDatabase {
            val tempInstance = INSTANCE
            if (tempInstance != null) return tempInstance
            synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                return instance
            }
        }
    }
}