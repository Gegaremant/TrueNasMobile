package com.imnotndesh.truehub.ui.widgets.pools

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager

object PoolsWidgetUpdater {
    suspend fun update(context: Context) {
        GlanceAppWidgetManager(context)
            .getGlanceIds(PoolsWidget::class.java)
            .forEach { PoolsWidget().update(context, it) }
    }
}