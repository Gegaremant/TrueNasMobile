package com.gegaremant.truenasmobile.ui.homepage.pools

import com.gegaremant.truenasmobile.data.models.System

object PoolDataHolder {
    var currentPool: System.Pool? = null

    /**
     * Drops the selection.
     *
     * These are process-wide objects, so the value outlives the session it was
     * set in: without this, a detail screen reached after switching accounts can
     * render the previous server's pool, app or container.
     */
    fun clear() {
        currentPool = null
    }
}
