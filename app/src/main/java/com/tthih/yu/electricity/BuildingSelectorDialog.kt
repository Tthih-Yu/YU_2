package com.tthih.yu.electricity

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tthih.yu.R

class BuildingSelectorDialog(
    context: Context,
    private val buildingsByCategory: Map<String, List<String>>,
    private val onBuildingSelected: (String) -> Unit
) : Dialog(context) {
    
    private lateinit var maleButton: Button
    private lateinit var femaleButton: Button
    private lateinit var postGradButton: Button
    private lateinit var mengxiButton: Button
    private lateinit var cancelButton: Button
    private lateinit var recyclerView: RecyclerView
    
    private var currentCategory = "男生宿舍"
    private var adapter: BuildingAdapter? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.dialog_building_selector)
        
        // 设置对话框宽度和位置
        window?.apply {
            val width = (context.resources.displayMetrics.widthPixels * 0.9).toInt() // 屏幕宽度的90%
            setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            // 设置对话框居中显示
            attributes?.gravity = android.view.Gravity.CENTER
        }
        
        // 初始化控件
        maleButton = findViewById(R.id.btn_male)
        femaleButton = findViewById(R.id.btn_female)
        postGradButton = findViewById(R.id.btn_postgrad)
        mengxiButton = findViewById(R.id.btn_mengxi)
        cancelButton = findViewById(R.id.btn_cancel)
        recyclerView = findViewById(R.id.rv_buildings)
        
        // 设置RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(context)
        updateBuildingList("男生宿舍")
        
        // 设置类别按钮点击事件
        maleButton.setOnClickListener { updateCategorySelection("男生宿舍") }
        femaleButton.setOnClickListener { updateCategorySelection("女生宿舍") }
        postGradButton.setOnClickListener { updateCategorySelection("研究生宿舍") }
        mengxiButton.setOnClickListener { updateCategorySelection("梦溪宿舍") }
        
        // 设置取消按钮点击事件
        cancelButton.setOnClickListener { dismiss() }
    }
    
    private fun updateCategorySelection(category: String) {
        currentCategory = category
        
        // 重置所有按钮样式
        maleButton.setBackgroundColor(context.getColor(R.color.matcha_very_light))
        maleButton.setTextColor(context.getColor(R.color.matcha_text_primary))
        femaleButton.setBackgroundColor(context.getColor(R.color.matcha_very_light))
        femaleButton.setTextColor(context.getColor(R.color.matcha_text_primary))
        postGradButton.setBackgroundColor(context.getColor(R.color.matcha_very_light))
        postGradButton.setTextColor(context.getColor(R.color.matcha_text_primary))
        mengxiButton.setBackgroundColor(context.getColor(R.color.matcha_very_light))
        mengxiButton.setTextColor(context.getColor(R.color.matcha_text_primary))
        
        // 设置选中按钮样式
        when (category) {
            "男生宿舍" -> {
                maleButton.setBackgroundColor(context.getColor(R.color.matcha_primary))
                maleButton.setTextColor(Color.WHITE)
            }
            "女生宿舍" -> {
                femaleButton.setBackgroundColor(context.getColor(R.color.matcha_primary))
                femaleButton.setTextColor(Color.WHITE)
            }
            "研究生宿舍" -> {
                postGradButton.setBackgroundColor(context.getColor(R.color.matcha_primary))
                postGradButton.setTextColor(Color.WHITE)
            }
            "梦溪宿舍" -> {
                mengxiButton.setBackgroundColor(context.getColor(R.color.matcha_primary))
                mengxiButton.setTextColor(Color.WHITE)
            }
        }
        
        // 更新宿舍楼列表
        updateBuildingList(category)
    }
    
    private fun updateBuildingList(category: String) {
        val buildings = buildingsByCategory[category] ?: emptyList()
        adapter = BuildingAdapter(buildings) { buildingName ->
            onBuildingSelected(buildingName)
            dismiss()
        }
        recyclerView.adapter = adapter
    }
    
    private inner class BuildingAdapter(
        private val buildings: List<String>,
        private val onBuildingClicked: (String) -> Unit
    ) : RecyclerView.Adapter<BuildingAdapter.BuildingViewHolder>() {
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BuildingViewHolder {
            val view = layoutInflater.inflate(R.layout.item_building, parent, false)
            return BuildingViewHolder(view)
        }
        
        override fun onBindViewHolder(holder: BuildingViewHolder, position: Int) {
            val building = buildings[position]
            holder.bind(building)
        }
        
        override fun getItemCount(): Int = buildings.size
        
        inner class BuildingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val buildingNameTextView: TextView = itemView.findViewById(R.id.tv_building_name)
            
            fun bind(buildingName: String) {
                buildingNameTextView.text = buildingName
                itemView.setOnClickListener { onBuildingClicked(buildingName) }
            }
        }
    }
} 