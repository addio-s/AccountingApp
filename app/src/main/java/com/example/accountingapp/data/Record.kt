package com.example.accountingapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "records")
data class Record(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: Int, // 0: 支出，1: 收入
    val amount: Double,
    val categoryId: Long,
    val categoryName: String,
    val note: String = "",
    val date: Long, // Unix timestamp
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val type: Int, // 0: 支出，1: 收入
    val icon: String = "",
    val color: Int = 0
)
