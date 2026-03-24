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

    @Query("SELECT * FROM reclassified_apps")
    fun getAllReclassifiedApps(): Flow<List<ReclassifiedAppEntity>>
}
