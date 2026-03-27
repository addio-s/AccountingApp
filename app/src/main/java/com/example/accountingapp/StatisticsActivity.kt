package com.example.accountingapp

import android.graphics.Color
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.accountingapp.data.AppDatabase
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.PercentFormatter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.HashMap

class StatisticsActivity : AppCompatActivity() {
    
    private lateinit var database: AppDatabase
    private lateinit var tabGroup: RadioGroup
    private lateinit var tvMonthTitle: TextView
    private lateinit var tvMonthIncome: TextView
    private lateinit var tvMonthExpense: TextView
    private lateinit var tvMonthBalance: TextView
    private lateinit var pieChart: PieChart
    private lateinit var barChart: BarChart
    private lateinit var btnPrevMonth: ImageButton
    private lateinit var btnNextMonth: ImageButton
    
    private var currentYear: Int = Calendar.getInstance().get(Calendar.YEAR)
    private var currentMonth: Int = Calendar.getInstance().get(Calendar.MONTH)
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_statistics)
        
        database = AppDatabase.getDatabase(this)
        
        initViews()
        setupListeners()
        updateStatistics()
    }
    
    private fun initViews() {
        tabGroup = findViewById(R.id.tabGroup)
        tvMonthTitle = findViewById(R.id.tvMonthTitle)
        tvMonthIncome = findViewById(R.id.tvMonthIncome)
        tvMonthExpense = findViewById(R.id.tvMonthExpense)
        tvMonthBalance = findViewById(R.id.tvMonthBalance)
        pieChart = findViewById(R.id.pieChart)
        barChart = findViewById(R.id.barChart)
        btnPrevMonth = findViewById(R.id.btnPrevMonth)
        btnNextMonth = findViewById(R.id.btnNextMonth)
        
        updateMonthTitle()
    }
    
    private fun setupListeners() {
        tabGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.tabMonth -> {
                    barChart.visibility = View.GONE
                    pieChart.visibility = View.VISIBLE
                    btnPrevMonth.visibility = View.VISIBLE
                    btnNextMonth.visibility = View.VISIBLE
                    updateStatistics()
                }
                R.id.tabYear -> {
                    barChart.visibility = View.VISIBLE
                    pieChart.visibility = View.GONE
                    btnPrevMonth.visibility = View.GONE
                    btnNextMonth.visibility = View.GONE
                    updateYearStatistics()
                }
            }
        }
        
        btnPrevMonth.setOnClickListener {
            currentMonth--
            if (currentMonth < 0) {
                currentMonth = 11
                currentYear--
            }
            updateMonthTitle()
            updateStatistics()
        }
        
        btnNextMonth.setOnClickListener {
            currentMonth++
            if (currentMonth > 11) {
                currentMonth = 0
                currentYear++
            }
            updateMonthTitle()
            updateStatistics()
        }
        
        // 返回按钮
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            finish()
        }
    }
    
    private fun updateMonthTitle() {
        tvMonthTitle.text = "$currentYear年${currentMonth + 1}月"
    }
    
    private fun updateStatistics() {
        CoroutineScope(Dispatchers.IO).launch {
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.YEAR, currentYear)
            calendar.set(Calendar.MONTH, currentMonth)
            calendar.set(Calendar.DAY_OF_MONTH, 1)
            val monthStart = calendar.timeInMillis
            
            calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
            calendar.set(Calendar.HOUR_OF_DAY, 23)
            calendar.set(Calendar.MINUTE, 59)
            calendar.set(Calendar.SECOND, 59)
            val monthEnd = calendar.timeInMillis
            
            val totalIncome = database.recordDao().getTotalIncome(monthStart, monthEnd) ?: 0.0
            val totalExpense = database.recordDao().getTotalExpense(monthStart, monthEnd) ?: 0.0
            val balance = totalIncome - totalExpense
            
            // 获取分类统计
            val records = database.recordDao().getRecordsByDateRange(monthStart, monthEnd).value ?: emptyList()
            val categoryStats = HashMap<String, Double>()
            records.filter { it.type == 0 }.forEach {
                categoryStats[it.categoryName] = (categoryStats[it.categoryName] ?: 0.0) + it.amount
            }
            
            withContext(Dispatchers.Main) {
                tvMonthIncome.text = "¥${String.format("%.2f", totalIncome)}"
                tvMonthExpense.text = "¥${String.format("%.2f", totalExpense)}"
                tvMonthBalance.text = "¥${String.format("%.2f", balance)}"
                tvMonthBalance.setTextColor(if (balance >= 0) Color.parseColor("#4CAF50") else Color.parseColor("#F44336"))
                
                setupPieChart(categoryStats, totalExpense)
            }
        }
    }
    
    private fun updateYearStatistics() {
        CoroutineScope(Dispatchers.IO).launch {
            val monthData = HashMap<String, FloatArray>()
            
            for (i in 0..11) {
                val calendar = Calendar.getInstance()
                calendar.set(Calendar.YEAR, currentYear)
                calendar.set(Calendar.MONTH, i)
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                val monthStart = calendar.timeInMillis
                
                calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                val monthEnd = calendar.timeInMillis
                
                val income = (database.recordDao().getTotalIncome(monthStart, monthEnd) ?: 0.0).toFloat()
                val expense = (database.recordDao().getTotalExpense(monthStart, monthEnd) ?: 0.0).toFloat()
                monthData["${i + 1}月"] = floatArrayOf(income, expense)
            }
            
            withContext(Dispatchers.Main) {
                setupBarChart(monthData)
            }
        }
    }
    
    private fun setupPieChart(categoryStats: HashMap<String, Double>, total: Double) {
        if (total <= 0) {
            pieChart.clear()
            pieChart.setNoDataText("暂无支出数据")
            return
        }
        
        val entries = categoryStats.mapIndexed { index, entry ->
            PieEntry(entry.value.toFloat(), entry.key)
        }
        
        val dataSet = PieDataSet(entries, "支出分类")
        dataSet.colors = getCategoryColors(categoryStats.size)
        dataSet.valueTextSize = 12f
        dataSet.valueTextColor = Color.WHITE
        
        val data = PieData(dataSet)
        data.setValueFormatter(PercentFormatter(pieChart))
        
        pieChart.data = data
        pieChart.setUsePercentValues(true)
        pieChart.description.isEnabled = false
        pieChart.setEntryLabelColor(Color.WHITE)
        pieChart.setEntryLabelTextSize(10f)
        pieChart.legend.isEnabled = true
        pieChart.legend.textColor = Color.WHITE
        pieChart.invalidate()
    }
    
    private fun setupBarChart(monthData: HashMap<String, FloatArray>) {
        val months = monthData.keys.toList()
        val incomeValues = monthData.values.map { it[0] }
        val expenseValues = monthData.values.map { it[1] }
        
        val incomeEntries = incomeValues.mapIndexed { index, value ->
            BarEntry(index.toFloat(), value)
        }
        val expenseEntries = expenseValues.mapIndexed { index, value ->
            BarEntry(index.toFloat(), value)
        }
        
        val incomeSet = BarDataSet(incomeEntries, "收入")
        incomeSet.color = Color.parseColor("#4CAF50")
        incomeSet.valueTextColor = Color.WHITE
        
        val expenseSet = BarDataSet(expenseEntries, "支出")
        expenseSet.color = Color.parseColor("#F44336")
        expenseSet.valueTextColor = Color.WHITE
        
        val data = BarData(incomeSet, expenseSet)
        data.barWidth = 0.35f
        data.setValueTextSize(10f)
        
        barChart.data = data
        barChart.description.isEnabled = false
        barChart.xAxis.valueFormatter = IndexAxisValueFormatter(months)
        barChart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        barChart.xAxis.textColor = Color.WHITE
        barChart.xAxis.setDrawGridLines(false)
        barChart.axisLeft.textColor = Color.WHITE
        barChart.axisRight.isEnabled = false
        barChart.legend.textColor = Color.WHITE
        barChart.setFitBars(true)
        barChart.invalidate()
    }
    
    private fun getCategoryColors(count: Int): List<Int> {
        val colors = listOf(
            Color.parseColor("#FF6384"),
            Color.parseColor("#36A2EB"),
            Color.parseColor("#FFCE56"),
            Color.parseColor("#4BC0C0"),
            Color.parseColor("#9966FF"),
            Color.parseColor("#FF9F40"),
            Color.parseColor("#C9CBCF"),
            Color.parseColor("#4CAF50"),
            Color.parseColor("#E91E63"),
            Color.parseColor("#00BCD4")
        )
        return List(count) { index -> colors[index % colors.size] }
    }
}
