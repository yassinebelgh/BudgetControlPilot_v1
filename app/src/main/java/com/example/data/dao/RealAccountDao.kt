package com.example.data.dao

import androidx.room.*
import com.example.data.model.RealAccountEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RealAccountDao {
    @Query("SELECT * FROM real_accounts ORDER BY name ASC")
    fun getAllRealAccounts(): Flow<List<RealAccountEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRealAccount(account: RealAccountEntity): Long

    @Update
    suspend fun updateRealAccount(account: RealAccountEntity)

    @Query("UPDATE real_accounts SET balance = :newBalance, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateAccountBalance(id: Long, newBalance: Double, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM real_accounts WHERE id = :id")
    suspend fun deleteRealAccountById(id: Long)

    @Query("DELETE FROM real_accounts")
    suspend fun clearAll()
}
