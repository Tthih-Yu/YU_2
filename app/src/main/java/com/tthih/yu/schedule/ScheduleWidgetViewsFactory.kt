package com.tthih.yu.schedule

import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.tthih.yu.R
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import java.util.*

/**
 * RemoteViewsFactory implementation to provide views for the schedule widget list.
 */
class ScheduleWidgetViewsFactory(
    private val context: Context,
    private val intent: Intent
) : RemoteViewsService.RemoteViewsFactory {

    private var scheduleList: List<ScheduleData> = emptyList()
    private lateinit var repository: ScheduleRepository
    private lateinit var timeNodes: List<ScheduleTimeNode>

    companion object {
        private const val TAG = "ScheduleWidgetFactory"
    }

    override fun onCreate() {
        Log.d(TAG, "onCreate called")
        // Initialize repository here
        repository = ScheduleRepository(context.applicationContext)
        // Load initial data
        loadData()
    }

    override fun onDataSetChanged() {
        Log.d(TAG, "onDataSetChanged called - reloading data")
        // This is triggered notifyAppWidgetViewDataChanged is called
        loadData()
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy called")
        // Clean up resources if needed
        scheduleList = emptyList()
    }

    override fun getCount(): Int {
        Log.d(TAG, "getCount called, returning: ${scheduleList.size}")
        return scheduleList.size
    }

    override fun getViewAt(position: Int): RemoteViews {
        Log.d(TAG, "getViewAt called for position: $position")
        if (position >= scheduleList.size) {
            Log.w(TAG, "getViewAt - Invalid position: $position, size: ${scheduleList.size}")
            // Return an empty view or a placeholder if position is out of bounds
            return RemoteViews(context.packageName, R.layout.schedule_widget_list_item) // Return an empty item
        }
        
        val course = scheduleList[position]
        Log.d(TAG, "getViewAt - Binding data for course: ${course.name}")
        val views = RemoteViews(context.packageName, R.layout.schedule_widget_list_item)

        // Bind data to the views
        val startTime = timeNodes.find { it.node == course.startNode }?.startTime ?: ""
        val timeText = "${course.startNode}-${course.endNode}节\n$startTime"
        val detailsText = "${course.classroom} | ${course.teacher}"

        views.setTextViewText(R.id.widget_item_time, timeText)
        views.setTextViewText(R.id.widget_item_name, course.name)
        views.setTextViewText(R.id.widget_item_details, detailsText)

        // Set up fill-in intent for item click
        val fillInIntent = Intent().apply {
            // Add extras to uniquely identify this click
            // Although we might not use them in ScheduleActivity if we always show the current week,
            // it's good practice to make the intent unique.
            putExtra(ScheduleWidgetProvider.EXTRA_ITEM_POSITION, position)
            putExtra("course_id", course.id) // Example extra
            // You could potentially add week/day info here too if needed later
            // putExtra(ScheduleWidgetProvider.EXTRA_WEEK_NUMBER, repository.getCurrentWeek())
        }
        // Set the fill-in intent on the root layout of the list item
        views.setOnClickFillInIntent(R.id.widget_item_layout_root, fillInIntent)

        return views
    }

    override fun getLoadingView(): RemoteViews? {
        Log.d(TAG, "getLoadingView called")
        // You can return a custom loading view if needed
        return null // Returning null uses the default loading view
    }

    override fun getViewTypeCount(): Int = 1 // Only one type of list item view

    override fun getItemId(position: Int): Long {
       if (position >= scheduleList.size) return -1L // Return an invalid ID
       return scheduleList[position].id.toLong()
    }

    override fun hasStableIds(): Boolean = true

    private fun loadData() {
        Log.d(TAG, "loadData started")
        // Fetch today's schedule data. 
        // WARNING: Performing database/network operations directly on the main thread 
        // from a RemoteViewsFactory can cause ANRs. Use runBlocking carefully 
        // or ensure your repository methods are designed for this context.
        runBlocking {
            Log.d(TAG, "runBlocking started")
            try {
                val todayWeekDay = repository.getTodayWeekDay()
                val currentWeek = repository.getCurrentWeek()
                Log.d(TAG, "Fetching schedule for week: $currentWeek, day: $todayWeekDay")
                val courses = repository.getSchedulesByWeekAndDay(currentWeek, todayWeekDay)
                                       .sortedBy { it.startNode } // Sort by start time
                scheduleList = courses
                timeNodes = repository.getTimeNodes()
                Log.d(TAG, "loadData successful - loaded ${scheduleList.size} courses")
            } catch (e: Exception) {
                Log.e(TAG, "loadData failed", e) // Log the full exception
                // Handle exceptions (e.g., database not ready)
                scheduleList = emptyList()
                timeNodes = emptyList()
                // Log error appropriately
            }
            Log.d(TAG, "runBlocking finished")
        }
        Log.d(TAG, "loadData finished")
    }
} 