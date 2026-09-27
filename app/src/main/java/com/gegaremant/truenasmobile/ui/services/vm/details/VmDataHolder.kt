package com.gegaremant.truenasmobile.ui.services.vm.details

import com.gegaremant.truenasmobile.data.models.Vm

object VmDataHolder {
    var selectedVm: Vm.VmQueryResponse ?= null

    /**
     * Drops the selection.
     *
     * These are process-wide objects, so the value outlives the session it was
     * set in: without this, a detail screen reached after switching accounts can
     * render the previous server's pool, app or container.
     */
    fun clear() {
        selectedVm = null
    }
}
