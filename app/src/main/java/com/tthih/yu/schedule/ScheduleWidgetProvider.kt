package com.tthih.yu.schedule

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import com.tthih.yu.MainActivity // Import MainActivity
import com.tthih.yu.R
import java.text.SimpleDateFormat
import java.util.*

/**
 * Implementation of App Widget functionality for the Schedule.
 */
class ScheduleWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_REFRESH = "com.tthih.yu.schedule.ACTION_REFRESH_SCHEDULE_WIDGET"
        const val ACTION_VIEW_WEEK = "com.tthih.yu.schedule.ACTION_VIEW_WEEK" // Action for item click
        const val EXTRA_ITEM_POSITION = "com.tthih.yu.schedule.EXTRA_ITEM_POSITION"
        const val EXTRA_WEEK_NUMBER = "com.tthih.yu.schedule.EXTRA_WEEK_NUMBER" // Optional extra
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        // There may be multiple widgets active, so update all of them
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
        super.onUpdate(context, appWidgetManager, appWidgetIds)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (ACTION_REFRESH == intent.action) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisAppWidget = ComponentName(context.packageName, javaClass.name)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(thisAppWidget)
            
            // Notify the factory to refresh data
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.widget_list_view)
            
            // Optionally, update the header date as well
            for (appWidgetId in appWidgetIds) {
                 updateAppWidgetHeader(context, appWidgetManager, appWidgetId)
            }
        }
    }

    override fun onEnabled(context: Context) {
        // Enter relevant functionality for when the first widget is created
    }

    override fun onDisabled(context: Context) {
        // Enter relevant functionality for when the last widget is disabled
    }
}

internal fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
    // Construct the RemoteViews object
    val views = RemoteViews(context.packageName, R.layout.schedule_widget_layout)

    // Set up the intent that starts the ScheduleWidgetService
    val serviceIntent = Intent(context, ScheduleWidgetService::class.java).apply {
        // Add the app widget ID to the intent extras.
        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        // Make the intent unique for this widget instance
        data = Uri.parse(this.toUri(Intent.URI_INTENT_SCHEME))
    }
    views.setRemoteAdapter(R.id.widget_list_view, serviceIntent)

    // Set the empty view
    views.setEmptyView(R.id.widget_list_view, R.id.widget_empty_view)

    // Update header date
    updateAppWidgetHeader(context, appWidgetManager, appWidgetId, views)

    // Set up pending intent for refresh button
    val refreshIntent = Intent(context, ScheduleWidgetProvider::class.java).apply {
        action = ScheduleWidgetProvider.ACTION_REFRESH
        // Ensure the intent is unique for the refresh action of this widget
        data = Uri.parse("intent://widget/id/$appWidgetId/refresh") 
        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId) 
    }
    val refreshPendingIntent = PendingIntent.getBroadcast(
        context,
        appWidgetId, // Use appWidgetId as request code to ensure uniqueness for the refresh action
        refreshIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    views.setOnClickPendingIntent(R.id.widget_refresh_button, refreshPendingIntent)

    // Set up pending intent template for list items
    val itemClickIntent = Intent(context, ScheduleActivity::class.java).apply {
        action = ScheduleWidgetProvider.ACTION_VIEW_WEEK
        // Ensure this intent is unique per widget instance for the template
        data = Uri.parse("intent://widget/id/$appWidgetId/item_click")
        // Add flags to clear task and bring activity to front
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    }
    val itemClickPendingIntent = PendingIntent.getActivity(
        context,
        appWidgetId + 2, // Different request code from refresh and title click
        itemClickIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    views.setPendingIntentTemplate(R.id.widget_list_view, itemClickPendingIntent)
    
    // Set up pending intent to open the app when the title is clicked (goes to MainActivity)
    val openAppIntent = Intent(context, MainActivity::class.java)
    val openAppPendingIntent = PendingIntent.getActivity(
        context, 
        appWidgetId + 1, // Use a different request code based on appWidgetId
        openAppIntent, 
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    views.setOnClickPendingIntent(R.id.widget_title, openAppPendingIntent)

    // Instruct the widget manager to update the widget layout FIRST
    appWidgetManager.updateAppWidget(appWidgetId, views)
    
    // THEN, notify that the data for the ListView needs to be refreshed.
    // This triggers the RemoteViewsFactory (ScheduleWidgetViewsFactory).
    appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_list_view)
}

// Helper function to update just the header (e.g., after refresh)
internal fun updateAppWidgetHeader(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, existingViews: RemoteViews? = null) {
    val views = existingViews ?: RemoteViews(context.packageName, R.layout.schedule_widget_layout)
    // Set current date
    val dateFormat = SimpleDateFormat("M月d日 E", Locale.CHINA)
    val currentDate = dateFormat.format(Date())
    views.setTextViewText(R.id.widget_date, currentDate)
    
    // If we are only updating the header (existingViews is null), use partiallyUpdateAppWidget
    if (existingViews == null) {
        appWidgetManager.partiallyUpdateAppWidget(appWidgetId, views)
    }
} 