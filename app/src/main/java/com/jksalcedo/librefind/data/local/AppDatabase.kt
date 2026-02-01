package com.jksalcedo.librefind.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.jksalcedo.librefind.data.local.cache.AppCacheDao
import com.jksalcedo.librefind.data.local.cache.entities.CachedSolution
import com.jksalcedo.librefind.data.local.cache.entities.CachedTarget

@Database(
    entities = [
        IgnoredAppEntity::class,
        CachedTarget::class,
        CachedSolution::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun ignoredAppDao(): IgnoredAppDao
    abstract fun appCacheDao(): AppCacheDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "librefind_database"
                )
                    .fallbackToDestructiveMigration(false)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
