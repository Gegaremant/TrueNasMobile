package com.gegaremant.truenasmobile.ui.services.containers.details

import com.gegaremant.truenasmobile.data.models.Virt

object ContainerDataHolder {
    var selectedContainer: Virt.ContainerResponse? = null

    /**
     * Drops the selection.
     *
     * These are process-wide objects, so the value outlives the session it was
     * set in: without this, a detail screen reached after switching accounts can
     * render the previous server's pool, app or container.
     */
    fun clear() {
        selectedContainer = null
    }
}
