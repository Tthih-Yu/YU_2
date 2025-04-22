package com.tthih.yu.about

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tthih.yu.R

class UpdateLogActivity : AppCompatActivity() {
    
    private lateinit var toolbar: Toolbar
    private lateinit var recyclerView: RecyclerView
    private var currentVersionCode = 0
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_update_log)
        
        // 初始化视图
        initViews()
        
        // 获取当前版本号
        getCurrentVersion()
        
        // 设置适配器
        setupRecyclerView()
    }
    
    private fun initViews() {
        // 设置工具栏
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "更新日志"
        
        // 初始化RecyclerView
        recyclerView = findViewById(R.id.recycler_view)
    }
    
    private fun getCurrentVersion() {
        try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            currentVersionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode
            }
        } catch (e: PackageManager.NameNotFoundException) {
            currentVersionCode = 0
        }
    }
    
    private fun setupRecyclerView() {
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = VersionHistoryAdapter(this, VersionHistory.history, currentVersionCode)
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}

class VersionHistoryAdapter(
    private val context: Context,
    private val items: List<VersionHistoryItem>,
    private val currentVersionCode: Int
) : RecyclerView.Adapter<VersionHistoryAdapter.ViewHolder>() {
    
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val versionNameTextView: TextView = view.findViewById(R.id.tv_version_name)
        val releaseDateTextView: TextView = view.findViewById(R.id.tv_release_date)
        val currentVersionTextView: TextView = view.findViewById(R.id.tv_current_version)
        val changesContainer: LinearLayout = view.findViewById(R.id.container_changes)
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_version_history, parent, false)
        return ViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        
        holder.versionNameTextView.text = "v${item.versionName}"
        holder.releaseDateTextView.text = item.releaseDate
        
        // 显示当前版本标记
        if (item.versionCode == currentVersionCode) {
            holder.currentVersionTextView.visibility = View.VISIBLE
        } else {
            holder.currentVersionTextView.visibility = View.GONE
        }
        
        // 清除已有内容
        holder.changesContainer.removeAllViews()
        
        // 添加每项变更
        for (change in item.changes) {
            val changeView = LayoutInflater.from(context)
                .inflate(R.layout.item_version_change, holder.changesContainer, false)
            val changeTextView = changeView.findViewById<TextView>(R.id.tv_change)
            changeTextView.text = change
            holder.changesContainer.addView(changeView)
        }
    }
    
    override fun getItemCount() = items.size
} 