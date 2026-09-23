package com.imnotndesh.truehub.data.workers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class CancelJobReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_CANCEL_JOB = "com.imnotndesh.truehub.ACTION_CANCEL_JOB"
        const val EXTRA_JOB_ID = "extra_job_id"
        const val EXTRA_APP_NAME = "extra_app_name"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_CANCEL_JOB) return
        val jobId = intent.getIntExtra(EXTRA_JOB_ID, -1)
        if (jobId == -1) return
        CancelJobWorker.enqueue(context, jobId)
    }
}