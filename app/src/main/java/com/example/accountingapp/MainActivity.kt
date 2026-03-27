package com.example.accountingapp

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.accountingapp.data.AppDatabase
import com.example.accountingapp.data.Record
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {
    
    private lateinit var database: AppDatabase
    private lateinit var recordAdapter: RecordAdapter
    private lateinit var tvTodayIncome: TextView
    private lateinit var tvTodayExpense: TextView
    private lateinit var tvMonthIncome: TextView
    private lateinit var tvMonthExpense: TextView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        database = AppDatabase.getDatabase(this)
        
        initViews()
        setupRecyclerView()
        observeRecords()
        updateSummary()
    }
    
    private fun initViews() {
        tvTodayIncome = findViewById(R.id.tvTodayIncome)
        tvTodayExpense = findViewById(R.id.tvTodayExpense)
        tvMonthIncome = findViewById(R.id.tvMonthIncome)
        tvMonthExpense = findViewById(R.id.tvMonthExpense)
        
        val fabAdd: FloatingActionButton = findViewById(R.id.fabAdd)
        fabAdd.setOnClickListener {
            startActivity(Intent(this, AddRecordActivity::class.java))
        }
        
        val bottomNav: BottomNavigationView = findViewById(R.id.bottomNav)
        bottomNav.selectedItemId = R.id.nav_home
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> true
                R.id.nav_stats -> {
                    startActivity(Intent(this, StatisticsActivity::class.java))
                    true
                }
                R.id.nav_category -> {
                    startActivity(Intent(this, CategoryActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }
    
    private fun setupRecyclerView() {
        val recyclerView: RecyclerView = findViewById(R.id.recyclerView)
        recordAdapter = RecordAdapter { record ->
            // 点击记录可以编辑或删除（简化版本暂不实现）
        }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = recordAdapter
    }
    
    private fun observeRecords() {
        database.recordDao().getAllRecords().observe(this) { records ->
            recordAdapter.submitList(records)
        }
    }
    
    private fun updateSummary() {
        CoroutineScope(Dispatchers.IO).launch {
            val calendar = Calendar.getInstance()
            
            // 今日统计
            val todayStart = getStartOfDay(calendar).timeInMillis
            val todayEnd = getEndOfDay(calendar).timeInMillis
            val todayIncome = database.recordDao().getTotalIncome(todayStart, todayEnd) ?: 0.0
            val todayExpense = database.recordDao().getTotalExpense(todayStart, todayEnd) ?: 0.0
            
            // 本月统计
            val monthStart = getStartOfMonth(calendar).timeInMillis
            val monthEnd = getEndOfMonth(calendar).timeInMillis
            val monthIncome = database.recordDao().getTotalIncome(monthStart, monthEnd) ?: 0.0
            val monthExpense = database.recordDao().getTotalExpense(monthStart, monthEnd) ?: 0.0
            
            withContext(Dispatchers.Main) {
                tvTodayIncome.text = "¥${String.format("%.2f", todayIncome)}"
                tvTodayExpense.text = "¥${String.format("%.2f", todayExpense)}"
                tvMonthIncome.text = "¥${String.format("%.2f", monthIncome)}"
                tvMonthExpense.text = "¥${String.format("%.2f", monthExpense)}"
            }
        }
    }
    
    private fun getStartOfDay(calendar: Calendar): Calendar {
        val cal = calendar.clone() as Calendar
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal
    }
    
    private fun getEndOfDay(calendar: Calendar): Calendar {
        val cal = calendar.clone() as Calendar
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        return cal
    }
    
    private fun getStartOfMonth(calendar: Calendar): Calendar {
        val cal = calendar.clone() as Calendar
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal
    }
    
    private fun getEndOfMonth(calendar: Calendar): Calendar {
        val cal = calendar.clone() as Calendar
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        return cal
    }
    
    override fun onResume() {
        super.onResume()
        updateSummary()
    }
}

class RecordAdapter(
    private val onItemClick: (Record) -> Unit
) : RecyclerView.Adapter<RecordAdapter.RecordViewHolder>() {
    
    private var records: List<Record> = emptyList()
    
    fun submitList(newRecords: List<Record>) {
        records = newRecords
        notifyDataSetChanged()
    }
    
    inner class RecordViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        private val tvCategory: TextView = itemView.findViewById(R.id.tvCategory)
        private val tvNote: TextView = itemView.findViewById(R.id.tvNote)
        private val tvAmount: TextView = itemView.findViewById(R.id.tvAmount)
        
        fun bind(record: Record) {
            val dateFormat = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
            tvDate.text = dateFormat.format(Date(record.date))
            tvCategory.text = record.categoryName
            tvNote.text = record.note
            tvAmount.text = "${if (record.type == 1) "+" else "-"}¥${String.format("%.2f", record.amount)}"
            tvAmount.setTextColor(if (record.type == 1) 0xFF4CAF50.toInt() else 0xFFF44336.toInt())
            
            itemView.setOnClickListener { onItemClick(record) }
        }
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecordViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_record, parent, false)
        return RecordViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: RecordViewHolder, position: Int) {
        holder.bind(records[position])
    }
    
    override fun getItemCount() = records.size
}
