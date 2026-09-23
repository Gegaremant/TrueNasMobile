package com.imnotndesh.truehub.ui.widgets.quicklaunch

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager

object QuickLaunchWidgetUpdater {
    suspend fun update(context: Context) {
        GlanceAppWidgetManager(context)
            .getGlanceIds(QuickLaunchWidget::class.java)
            .forEach { QuickLaunchWidget().update(context, it) }
    }
}