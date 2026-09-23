package com.imnotndesh.truehub.data.helpers

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class TrackedJob(
    val jobId: Int,
    val appName: String,
    val type : String = "UPGRADING",
    val state: String = "WAITING",
    val progress: Int = 0,
    val description: String? = null
)

object JobRepository {
    private val _activeJobs = MutableStateFlow<Map<Int, TrackedJob>>(emptyMap())
    val activeJobs = _activeJobs.asStateFlow()

    fun updateJob(job: TrackedJob) {
        _activeJobs.value += (job.jobId to job)
    }

    fun removeJob(jobId: Int) {
        _activeJobs.value -= jobId
    }
}