package com.tthih.yu.schedule

import android.content.Intent
import android.widget.RemoteViewsService

/**
 * Service to provide data to the schedule widget's list view.
 */
class ScheduleWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        // Return the factory that will create the views for the list items
        return ScheduleWidgetViewsFactory(this.applicationContext, intent)
    }
} 