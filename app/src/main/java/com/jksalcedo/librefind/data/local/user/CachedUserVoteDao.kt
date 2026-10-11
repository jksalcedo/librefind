package com.jksalcedo.librefind.data.local.user

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface CachedUserVoteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVote(vote: CachedUserVoteEntity)

    @Query("SELECT * FROM cached_user_votes WHERE solutionId = :solutionId LIMIT 1")
    suspend fun getVoteForSolution(solutionId: String): CachedUserVoteEntity?

    @Query("SELECT * FROM cached_user_votes WHERE synced = 0")
    suspend fun getUnsyncedVotes(): List<CachedUserVoteEntity>

    @Query("UPDATE cached_user_votes SET synced = 1 WHERE solutionId = :solutionId")
    suspend fun markVoteAsSynced(solutionId: String)
}
