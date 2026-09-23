package com.imnotndesh.truehub.ui.widgets

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager

object AppsUpdateWidgetUpdater {
    suspend fun update(context: Context) {
        GlanceAppWidgetManager(context)
            .getGlanceIds(AppsUpdateWidget::class.java)
            .forEach { AppsUpdateWidget().update(context, it) }
    }
}