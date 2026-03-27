package com.example.accountingapp

import android.app.Application
import com.example.accountingapp.data.AppDatabase
import com.example.accountingapp.data.Category
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AccountingApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // 初始化数据库并添加默认分类
        CoroutineScope(Dispatchers.IO).launch {
            val database = AppDatabase.getDatabase(this@AccountingApplication)
            val categoryDao = database.categoryDao()
            
            // 检查是否已有分类
            val existingCategories = categoryDao.getAllCategories().value
            if (existingCategories.isNullOrEmpty()) {
                // 添加默认支出分类
                val expenseCategories = listOf(
                    "餐饮", "交通", "购物", "娱乐", "医疗",
                    "教育", "居住", "通讯", "人情", "其他"
                )
                
                // 添加默认收入分类
                val incomeCategories = listOf(
                    "工资", "奖金", "兼职", "投资", "理财",
                    "红包", "报销", "其他"
                )
                
                expenseCategories.forEach { name ->
                    categoryDao.insert(Category(name = name, type = 0))
                }
                
                incomeCategories.forEach { name ->
                    categoryDao.insert(Category(name = name, type = 1))
                }
            }
        }
    }
}
