package com.jksalcedo.librefind.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ReclassifiedAppDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(app: ReclassifiedAppEntity)

    @Query("DELETE FROM reclassified_apps WHERE packageName = :packageName")
    suspend fun delete(packageName: String)

    @Query("SELECT packageName FROM reclassified_apps")
    fun getAllReclassifiedPackageNames(): Flow<List<String>>
}
