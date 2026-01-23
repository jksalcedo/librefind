package com.jksalcedo.librefind.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface IgnoredAppDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(app: IgnoredAppEntity)

    @Query("DELETE FROM ignored_apps WHERE packageName = :packageName")
    suspend fun delete(packageName: String)

    @Query("SELECT * FROM ignored_apps")
    fun getAllIgnored(): Flow<List<IgnoredAppEntity>>

    @Query("SELECT packageName FROM ignored_apps")
    fun getAllIgnoredPackageNames(): Flow<List<String>>
}
