package com.example.accountingapp

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.accountingapp.data.AppDatabase
import com.example.accountingapp.data.Record
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

class AddRecordActivity : AppCompatActivity() {
    
    private lateinit var database: AppDatabase
    private lateinit var radioType: RadioGroup
    private lateinit var spinnerCategory: Spinner
    private lateinit var editAmount: TextInputEditText
    private lateinit var editNote: TextInputEditText
    private lateinit var tvDate: TextView
    private lateinit var tvTime: TextView
    private lateinit var btnSubmit: Button
    
    private var selectedDate: Long = System.currentTimeMillis()
    private var expenseCategories: List<String> = emptyList()
    private var incomeCategories: List<String> = emptyList()
    private var currentType: Int = 0 // 0: 支出，1: 收入
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_record)
        
        database = AppDatabase.getDatabase(this)
        
        initViews()
        loadCategories()
        setupListeners()
    }
    
    private fun initViews() {
        radioType = findViewById(R.id.radioType)
        spinnerCategory = findViewById(R.id.spinnerCategory)
        editAmount = findViewById(R.id.editAmount)
        editNote = findViewById(R.id.editNote)
        tvDate = findViewById(R.id.tvDate)
        tvTime = findViewById(R.id.tvTime)
        btnSubmit = findViewById(R.id.btnSubmit)
        
        // 设置默认日期时间
        updateDateTimeDisplay()
    }
    
    private fun loadCategories() {
        CoroutineScope(Dispatchers.IO).launch {
            expenseCategories = database.categoryDao()
                .getCategoriesByTypeSync(0)
                .map { it.name }
            incomeCategories = database.categoryDao()
                .getCategoriesByTypeSync(1)
                .map { it.name }
            
            // 如果没有分类，添加默认分类
            if (expenseCategories.isEmpty()) {
                expenseCategories = listOf("餐饮", "交通", "购物", "娱乐", "医疗", "教育", "居住", "其他")
            }
            if (incomeCategories.isEmpty()) {
                incomeCategories = listOf("工资", "奖金", "兼职", "投资", "其他")
            }
            
            withContext(Dispatchers.Main) {
                updateCategorySpinner()
            }
        }
    }
    
    private fun updateCategorySpinner() {
        val categories = if (currentType == 0) expenseCategories else incomeCategories
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCategory.adapter = adapter
    }
    
    private fun setupListeners() {
        radioType.setOnCheckedChangeListener { _, checkedId ->
            currentType = if (checkedId == R.id.radioExpense) 0 else 1
            updateCategorySpinner()
        }
        
        tvDate.setOnClickListener {
            showDatePicker()
        }
        
        tvTime.setOnClickListener {
            showTimePicker()
        }
        
        btnSubmit.setOnClickListener {
            saveRecord()
        }
        
        // 返回按钮
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            finish()
        }
    }
    
    private fun showDatePicker() {
        val calendar = Calendar.getInstance().apply { timeInMillis = selectedDate }
        DatePickerDialog(
            this,
            { _, year, month, day ->
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, month)
                calendar.set(Calendar.DAY_OF_MONTH, day)
                selectedDate = calendar.timeInMillis
                updateDateTimeDisplay()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }
    
    private fun showTimePicker() {
        val calendar = Calendar.getInstance().apply { timeInMillis = selectedDate }
        TimePickerDialog(
            this,
            { _, hourOfDay, minute ->
                calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                calendar.set(Calendar.MINUTE, minute)
                selectedDate = calendar.timeInMillis
                updateDateTimeDisplay()
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            true
        ).show()
    }
    
    private fun updateDateTimeDisplay() {
        val dateFormat = android.text.format.DateFormat.format("yyyy-MM-dd", selectedDate)
        val timeFormat = android.text.format.DateFormat.format("HH:mm", selectedDate)
        tvDate.text = dateFormat
        tvTime.text = timeFormat
    }
    
    private fun saveRecord() {
        val amountStr = editAmount.text.toString().trim()
        if (amountStr.isEmpty()) {
            Toast.makeText(this, "请输入金额", Toast.LENGTH_SHORT).show()
            return
        }
        
        val amount = amountStr.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            Toast.makeText(this, "请输入有效的金额", Toast.LENGTH_SHORT).show()
            return
        }
        
        val category = spinnerCategory.selectedItem.toString()
        val note = editNote.text.toString().trim()
        
        CoroutineScope(Dispatchers.IO).launch {
            val record = Record(
                type = currentType,
                amount = amount,
                categoryId = 0,
                categoryName = category,
                note = note,
                date = selectedDate
            )
            database.recordDao().insert(record)
            
            withContext(Dispatchers.Main) {
                Toast.makeText(this@AddRecordActivity, "记录已保存", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
}
