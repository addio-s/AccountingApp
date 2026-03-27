package com.example.accountingapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.RadioButton
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.accountingapp.data.AppDatabase
import com.example.accountingapp.data.Category
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CategoryActivity : AppCompatActivity() {
    
    private lateinit var database: AppDatabase
    private lateinit var expenseAdapter: CategoryAdapter
    private lateinit var incomeAdapter: CategoryAdapter
    private lateinit var expenseRecyclerView: RecyclerView
    private lateinit var incomeRecyclerView: RecyclerView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_category)
        
        database = AppDatabase.getDatabase(this)
        
        initViews()
        setupRecyclerViews()
        observeCategories()
    }
    
    private fun initViews() {
        val fabAdd: FloatingActionButton = findViewById(R.id.fabAdd)
        fabAdd.setOnClickListener {
            showAddCategoryDialog()
        }
        
        // 返回按钮
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            finish()
        }
    }
    
    private fun setupRecyclerViews() {
        expenseRecyclerView = findViewById(R.id.expenseRecyclerView)
        incomeRecyclerView = findViewById(R.id.incomeRecyclerView)
        
        expenseAdapter = CategoryAdapter { category ->
            deleteCategory(category)
        }
        incomeAdapter = CategoryAdapter { category ->
            deleteCategory(category)
        }
        
        expenseRecyclerView.layoutManager = LinearLayoutManager(this)
        incomeRecyclerView.layoutManager = LinearLayoutManager(this)
        expenseRecyclerView.adapter = expenseAdapter
        incomeRecyclerView.adapter = incomeAdapter
    }
    
    private fun observeCategories() {
        database.categoryDao().getCategoriesByType(0).observe(this) { categories ->
            expenseAdapter.submitList(categories)
        }
        
        database.categoryDao().getCategoriesByType(1).observe(this) { categories ->
            incomeAdapter.submitList(categories)
        }
    }
    
    private fun showAddCategoryDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("添加分类")
        
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_add_category, null)
        builder.setView(view)
        
        val editName = view.findViewById<EditText>(R.id.editName)
        val radioExpense = view.findViewById<RadioButton>(R.id.radioExpense)
        val radioIncome = view.findViewById<RadioButton>(R.id.radioIncome)
        
        builder.setPositiveButton("确定") { _, _ ->
            val name = editName.text.toString().trim()
            if (name.isEmpty()) {
                android.widget.Toast.makeText(this, "请输入分类名称", android.widget.Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }
            
            val type = if (radioExpense.isChecked) 0 else 1
            
            CoroutineScope(Dispatchers.IO).launch {
                val category = Category(
                    name = name,
                    type = type,
                    icon = "",
                    color = 0
                )
                database.categoryDao().insert(category)
                
                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(this@CategoryActivity, "分类已添加", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
        
        builder.setNegativeButton("取消", null)
        builder.show()
    }
    
    private fun deleteCategory(category: Category) {
        AlertDialog.Builder(this)
            .setTitle("删除分类")
            .setMessage("确定要删除分类 \"${category.name}\" 吗？")
            .setPositiveButton("删除") { _, _ ->
                CoroutineScope(Dispatchers.IO).launch {
                    database.categoryDao().delete(category)
                    
                    withContext(Dispatchers.Main) {
                        android.widget.Toast.makeText(this@CategoryActivity, "分类已删除", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }
}

class CategoryAdapter(
    private val onItemClick: (Category) -> Unit
) : RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {
    
    private var categories: List<Category> = emptyList()
    
    fun submitList(newCategories: List<Category>) {
        categories = newCategories
        notifyDataSetChanged()
    }
    
    inner class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvName: TextView = itemView.findViewById(R.id.tvName)
        
        fun bind(category: Category) {
            tvName.text = category.name
            itemView.setOnClickListener { onItemClick(category) }
        }
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category, parent, false)
        return CategoryViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        holder.bind(categories[position])
    }
    
    override fun getItemCount() = categories.size
}
