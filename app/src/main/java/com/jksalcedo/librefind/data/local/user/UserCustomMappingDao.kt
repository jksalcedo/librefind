package com.jksalcedo.librefind.data.local.user

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface UserCustomMappingDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMapping(mapping: UserCustomMappingEntity)

    @Query("DELETE FROM user_custom_mappings WHERE targetPkg = :targetPkg AND solutionPkg = :solutionPkg")
    suspend fun deleteMapping(targetPkg: String, solutionPkg: String)

    @Query("SELECT * FROM user_custom_mappings WHERE targetPkg = :targetPkg")
    suspend fun getMappingsForTarget(targetPkg: String): List<UserCustomMappingEntity>
}
