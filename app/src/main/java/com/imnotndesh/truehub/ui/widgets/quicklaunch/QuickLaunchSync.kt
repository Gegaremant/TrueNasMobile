package com.imnotndesh.truehub.data.helpers

import android.content.Context
import com.imnotndesh.truehub.data.models.Apps
import com.imnotndesh.truehub.ui.widgets.quicklaunch.QuickLaunchWidgetUpdater

object QuickLaunchSync {
    suspend fun refresh(context: Context, apps: List<Apps.AppQueryResponse>) {
        val current = WidgetDataStore.getQuickLaunchApps(context)
        if (current.isEmpty()) return

        val byName = apps.associateBy { it.name }
        val refreshed = current.map { entry ->
            val live = byName[entry.appName] ?: return@map entry
            val liveUrl = live.portals?.values?.firstOrNull() ?: entry.webUiUrl
            val needsIconRefresh = entry.cachedIconPath == null || IconCache.loadCachedBitmap(entry.cachedIconPath) == null
            val iconPath = if (needsIconRefresh) {
                IconCache.cacheIcon(context, entry.appName, live.metadata?.icon) ?: entry.cachedIconPath
            } else entry.cachedIconPath

            entry.copy(webUiUrl = liveUrl, cachedIconPath = iconPath)
        }

        if (refreshed != current) {
            WidgetDataStore.saveQuickLaunchApps(context, refreshed)
            QuickLaunchWidgetUpdater.update(context)
        }
    }
}