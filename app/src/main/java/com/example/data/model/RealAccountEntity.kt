package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "real_accounts")
data class RealAccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val balance: Double,
    val accountType: String = "BANK", // BANK, SAVINGS, CASH, OTHER
    val updatedAt: Long = System.currentTimeMillis()
)
