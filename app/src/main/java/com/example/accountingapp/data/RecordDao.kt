package com.example.accountingapp.data

import androidx.lifecycle.LiveData
import androidx.room.*
import java.util.Date

@Dao
interface RecordDao {
    @Query("SELECT * FROM records ORDER BY date DESC")
    fun getAllRecords(): LiveData<List<Record>>
    
    @Query("SELECT * FROM records WHERE date >= :startTime AND date <= :endTime ORDER BY date DESC")
    fun getRecordsByDateRange(startTime: Long, endTime: Long): LiveData<List<Record>>
    
    @Query("SELECT * FROM records WHERE strftime('%Y-%m', date/1000, 'unixepoch') = :month ORDER BY date DESC")
    fun getRecordsByMonth(month: String): LiveData<List<Record>>
    
    @Insert
    suspend fun insert(record: Record): Long
    
    @Delete
    suspend fun delete(record: Record)
    
    @Query("SELECT SUM(amount) FROM records WHERE type = 1 AND date >= :startTime AND date <= :endTime")
    suspend fun getTotalIncome(startTime: Long, endTime: Long): Double?
    
    @Query("SELECT SUM(amount) FROM records WHERE type = 0 AND date >= :startTime AND date <= :endTime")
    suspend fun getTotalExpense(startTime: Long, endTime: Long): Double?
    
    @Query("SELECT SUM(amount) FROM records WHERE type = 1")
    suspend fun getTotalIncomeAll(): Double?
    
    @Query("SELECT SUM(amount) FROM records WHERE type = 0")
    suspend fun getTotalExpenseAll(): Double?
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY type, id")
    fun getAllCategories(): LiveData<List<Category>>
    
    @Query("SELECT * FROM categories WHERE type = :type ORDER BY id")
    fun getCategoriesByType(type: Int): LiveData<List<Category>>
    
    @Insert
    suspend fun insert(category: Category): Long
    
    @Delete
    suspend fun delete(category: Category)
    
    @Query("SELECT * FROM categories WHERE type = :type")
    suspend fun getCategoriesByTypeSync(type: Int): List<Category>
}
