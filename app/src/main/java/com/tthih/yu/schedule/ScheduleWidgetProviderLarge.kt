package com.tthih.yu.schedule

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import com.tthih.yu.MainActivity
import com.tthih.yu.R
import java.text.SimpleDateFormat
import java.util.*

/**
 * Implementation of App Widget functionality for the Large Schedule Widget (4x4).
 */
class ScheduleWidgetProviderLarge : AppWidgetProvider() { // Renamed class

    companion object {
        // Renamed constants to avoid potential conflicts if both providers are active
        const val ACTION_REFRESH_LARGE = "com.tthih.yu.schedule.ACTION_REFRESH_SCHEDULE_WIDGET_LARGE"
        const val ACTION_VIEW_WEEK_LARGE = "com.tthih.yu.schedule.ACTION_VIEW_WEEK_LARGE"
        const val EXTRA_ITEM_POSITION_LARGE = "com.tthih.yu.schedule.EXTRA_ITEM_POSITION_LARGE"
        const val EXTRA_WEEK_NUMBER_LARGE = "com.tthih.yu.schedule.EXTRA_WEEK_NUMBER_LARGE"
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            // Call the specific update function for the large widget
            updateAppWidgetLarge(context, appWidgetManager, appWidgetId)
        }
        super.onUpdate(context, appWidgetManager, appWidgetIds)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (ACTION_REFRESH_LARGE == intent.action) { // Use renamed action
            val appWidgetManager = AppWidgetManager.getInstance(context)
            // Use the correct ComponentName for this provider
            val thisAppWidget = ComponentName(context.packageName, javaClass.name)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(thisAppWidget)

            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.widget_list_view)

            for (appWidgetId in appWidgetIds) {
                // Call the specific header update function
                updateAppWidgetHeaderLarge(context, appWidgetManager, appWidgetId)
            }
        }
    }

    // onEnabled and onDisabled remain the same
    override fun onEnabled(context: Context) {}
    override fun onDisabled(context: Context) {}
}

// Renamed internal function to avoid conflicts and use correct layout
internal fun updateAppWidgetLarge(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
    // Use the large layout
    val views = RemoteViews(context.packageName, R.layout.schedule_widget_layout_large)

    // Service Intent remains the same (reuses the same service and factory)
    val serviceIntent = Intent(context, ScheduleWidgetService::class.java).apply {
        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        data = Uri.parse(this.toUri(Intent.URI_INTENT_SCHEME))
    }
    views.setRemoteAdapter(R.id.widget_list_view, serviceIntent)
    views.setEmptyView(R.id.widget_list_view, R.id.widget_empty_view)

    // Call the specific header update function
    updateAppWidgetHeaderLarge(context, appWidgetManager, appWidgetId, views)

    // Refresh button PendingIntent - use renamed action
    val refreshIntent = Intent(context, ScheduleWidgetProviderLarge::class.java).apply { // Target this provider
        action = ScheduleWidgetProviderLarge.ACTION_REFRESH_LARGE // Use renamed action
        data = Uri.parse("intent://widget/id/$appWidgetId/refresh_large") // Unique data URI
        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
    }
    val refreshPendingIntent = PendingIntent.getBroadcast(
        context,
        appWidgetId, // Request code can be the same if data URI is unique
        refreshIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    views.setOnClickPendingIntent(R.id.widget_refresh_button, refreshPendingIntent)

    // Item click PendingIntent template - use renamed action
    // Note: It still targets ScheduleActivity, which is likely desired.
    // The activity will need to check for BOTH actions if different behavior is needed.
    // For now, assume opening week view is the same for both widget sizes.
    val itemClickIntent = Intent(context, ScheduleActivity::class.java).apply {
        action = ScheduleWidgetProviderLarge.ACTION_VIEW_WEEK_LARGE // Use renamed action
        data = Uri.parse("intent://widget/id/$appWidgetId/item_click_large") // Unique data URI
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    }
    val itemClickPendingIntent = PendingIntent.getActivity(
        context,
        appWidgetId + 2, // Use different request code offset if needed, but uniqueness comes from data URI
        itemClickIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    views.setPendingIntentTemplate(R.id.widget_list_view, itemClickPendingIntent)

    // Title click PendingIntent (opens MainActivity) - remains similar
    val openAppIntent = Intent(context, MainActivity::class.java)
    val openAppPendingIntent = PendingIntent.getActivity(
        context,
        appWidgetId + 1, // Use different request code offset
        openAppIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    views.setOnClickPendingIntent(R.id.widget_title, openAppPendingIntent)

    appWidgetManager.updateAppWidget(appWidgetId, views)
    appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_list_view)
}

// Renamed internal function to use correct layout
internal fun updateAppWidgetHeaderLarge(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, existingViews: RemoteViews? = null) {
    // Use the large layout
    val views = existingViews ?: RemoteViews(context.packageName, R.layout.schedule_widget_layout_large)
    val dateFormat = SimpleDateFormat("M月d日 E", Locale.CHINA)
    val currentDate = dateFormat.format(Date())
    views.setTextViewText(R.id.widget_date, currentDate)

    if (existingViews == null) {
        appWidgetManager.partiallyUpdateAppWidget(appWidgetId, views)
    }
} 