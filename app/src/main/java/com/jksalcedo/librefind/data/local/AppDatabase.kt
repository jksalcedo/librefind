package com.jksalcedo.librefind.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.jksalcedo.librefind.data.local.cache.AppCacheDao
import com.jksalcedo.librefind.data.local.cache.entities.CachedSolution
import com.jksalcedo.librefind.data.local.cache.entities.CachedTarget
import com.jksalcedo.librefind.data.local.catalog.CatalogDao
import com.jksalcedo.librefind.data.local.catalog.CatalogMappingEntity
import com.jksalcedo.librefind.data.local.catalog.CatalogSolutionEntity
import com.jksalcedo.librefind.data.local.catalog.CatalogSyncMetadataEntity
import com.jksalcedo.librefind.data.local.catalog.CatalogTargetEntity
import com.jksalcedo.librefind.data.local.user.CachedUserVoteDao
import com.jksalcedo.librefind.data.local.user.CachedUserVoteEntity
import com.jksalcedo.librefind.data.local.user.UserCustomMappingDao
import com.jksalcedo.librefind.data.local.user.UserCustomMappingEntity

@Database(
    entities = [
        IgnoredAppEntity::class,
        CachedTarget::class,
        CachedSolution::class,
        ReclassifiedAppEntity::class,
        CatalogTargetEntity::class,
        CatalogSolutionEntity::class,
        CatalogMappingEntity::class,
        CatalogSyncMetadataEntity::class,
        CachedUserVoteEntity::class,
        UserCustomMappingEntity::class
    ],
    version = 5,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun ignoredAppDao(): IgnoredAppDao
    abstract fun appCacheDao(): AppCacheDao
    abstract fun reclassifiedAppDao(): ReclassifiedAppDao
    abstract fun catalogDao(): CatalogDao
    abstract fun cachedUserVoteDao(): CachedUserVoteDao
    abstract fun userCustomMappingDao(): UserCustomMappingDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `reclassified_apps` (" +
                            "`packageName` TEXT NOT NULL, " +
                            "PRIMARY KEY(`packageName`))"
                )
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `reclassified_apps` ADD COLUMN `status` TEXT NOT NULL DEFAULT 'FOSS'")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `catalog_targets` (" +
                            "`id` TEXT NOT NULL, " +
                            "`packageName` TEXT NOT NULL, " +
                            "`name` TEXT NOT NULL, " +
                            "`categories` TEXT NOT NULL, " +
                            "PRIMARY KEY(`id`))"
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_catalog_targets_packageName` " +
                            "ON `catalog_targets` (`packageName`)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `catalog_solutions` (" +
                            "`id` TEXT NOT NULL, " +
                            "`packageName` TEXT NOT NULL, " +
                            "`name` TEXT NOT NULL, " +
                            "`description` TEXT, " +
                            "`license` TEXT, " +
                            "`iconUrl` TEXT, " +
                            "`websiteUrl` TEXT, " +
                            "`repositoryUrl` TEXT, " +
                            "`fdroidUrl` TEXT, " +
                            "`upvotes` INTEGER NOT NULL, " +
                            "`downvotes` INTEGER NOT NULL, " +
                            "PRIMARY KEY(`id`))"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `catalog_mappings` (" +
                            "`targetId` TEXT NOT NULL, " +
                            "`solutionId` TEXT NOT NULL, " +
                            "PRIMARY KEY(`targetId`, `solutionId`), " +
                            "FOREIGN KEY(`targetId`) REFERENCES `catalog_targets`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE, " +
                            "FOREIGN KEY(`solutionId`) REFERENCES `catalog_solutions`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_catalog_mappings_targetId` " +
                            "ON `catalog_mappings` (`targetId`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_catalog_mappings_solutionId` " +
                            "ON `catalog_mappings` (`solutionId`)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `catalog_sync_metadata` (" +
                            "`id` INTEGER NOT NULL, " +
                            "`contentHash` TEXT NOT NULL, " +
                            "`timestamp` INTEGER NOT NULL, " +
                            "PRIMARY KEY(`id`))"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `cached_user_votes` (" +
                            "`solutionId` TEXT NOT NULL, " +
                            "`voteType` INTEGER NOT NULL, " +
                            "`synced` INTEGER NOT NULL, " +
                            "PRIMARY KEY(`solutionId`))"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `user_custom_mappings` (" +
                            "`targetPkg` TEXT NOT NULL, " +
                            "`solutionPkg` TEXT NOT NULL, " +
                            "`createdAt` INTEGER NOT NULL, " +
                            "PRIMARY KEY(`targetPkg`, `solutionPkg`))"
                )
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "librefind_database"
                )
                    .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
