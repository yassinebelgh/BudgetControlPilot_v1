package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val isExpense: Boolean = true,
    val iconName: String = "Category",
    val colorHex: String = "#3B82F6"
)
