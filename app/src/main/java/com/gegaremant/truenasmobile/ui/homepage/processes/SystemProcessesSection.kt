package com.gegaremant.truenasmobile.ui.homepage.processes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gegaremant.truenasmobile.R
import com.gegaremant.truenasmobile.data.ApiResult
import com.gegaremant.truenasmobile.data.api.TrueNASApiManager
import com.gegaremant.truenasmobile.data.models.System
import com.gegaremant.truenasmobile.ui.components.ToastManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * What the system is busy with.
 *
 * The complaint this answers: "the system says it is busy and there is no way
 * to see what it is doing". `core.get_jobs` already carries method, state,
 * percent and description for everything the stand runs, so this is a list of
 * those, live, with a way to stop the ones that can be stopped.
 */
@Composable
fun SystemProcessesSection(
    manager: TrueNASApiManager,
    onOpenFullList: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var jobs by remember { mutableStateOf<List<System.Job>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var abortingJobId by remember { mutableStateOf<Int?>(null) }
    var confirmAbort by remember { mutableStateOf<System.Job?>(null) }

    suspend fun refresh() {
        when (val result = manager.system.getJobsWithResult()) {
            is ApiResult.Success -> jobs = result.data
            is ApiResult.Error -> Unit // keep the last known list; a blip is not news
            is ApiResult.Loading -> Unit
        }
        isLoading = false
    }

    // A running job is exactly what this screen is for, so it polls while one
    // is in flight and stops when the system is quiet.
    LaunchedEffect(Unit) {
        while (true) {
            refresh()
            val busy = jobs.any { it.state.equals("RUNNING", true) }
            delay(if (busy) 2_000 else 10_000)
        }
    }

    val active = jobs.filter { it.state.equals("RUNNING", true) || it.state.equals("PENDING", true) }
    val failed = jobs.filter {
        it.state.equals("FAILED", true) || it.state.equals("ERROR", true)
    }
    val visible = if (active.isNotEmpty()) active else failed.take(3)

    if (visible.isEmpty() && !isLoading) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (active.isNotEmpty()) Icons.Default.Bolt else Icons.Default.HourglassTop,
                contentDescription = null,
                tint = if (active.isNotEmpty()) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                },
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (active.isNotEmpty()) {
                    stringResource(R.string.processes_active_count, active.size)
                } else {
                    stringResource(R.string.processes_recent_failed)
                },
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            if (onOpenFullList != null) {
                TextButton(onClick = onOpenFullList) {
                    Text(stringResource(R.string.processes_show_all))
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        visible.forEach { job ->
            JobRow(
                job = job,
                isAborting = abortingJobId == job.id,
                onAbort = { confirmAbort = job }
            )
        }
    }

    confirmAbort?.let { job ->
        AlertDialog(
            onDismissRequest = { confirmAbort = null },
            title = { Text(stringResource(R.string.processes_abort_title)) },
            text = { Text(stringResource(R.string.processes_abort_message, job.method)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmAbort = null
                    abortingJobId = job.id
                    scope.launch {
                        when (manager.system.abortJobWithResult(job.id)) {
                            is ApiResult.Success -> ToastManager.showSuccessRes(R.string.processes_abort_ok)
                            is ApiResult.Error -> ToastManager.showErrorRes(R.string.processes_abort_failed)
                            is ApiResult.Loading -> Unit
                        }
                        abortingJobId = null
                        refresh()
                    }
                }) {
                    Text(
                        text = stringResource(R.string.processes_abort_confirm),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmAbort = null }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }
}

@Composable
private fun JobRow(
    job: System.Job,
    isAborting: Boolean,
    onAbort: () -> Unit
) {
    val running = job.state.equals("RUNNING", true) || job.state.equals("PENDING", true)
    val percent = job.progress?.percent

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = when {
                    running -> Icons.Default.HourglassTop
                    job.error != null || job.state.equals("FAILED", true) -> Icons.Default.Error
                    else -> Icons.Default.CheckCircle
                },
                contentDescription = null,
                tint = when {
                    running -> MaterialTheme.colorScheme.primary
                    job.error != null || job.state.equals("FAILED", true) -> MaterialTheme.colorScheme.error
                    else -> Color0x2E7D32
                },
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = humanJobTitle(job.method),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (isAborting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp
                )
            } else if (running) {
                Text(
                    text = if (percent != null && percent > 0) "$percent%" else stringResource(R.string.processes_state_running),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (running && job.abortable && !isAborting) {
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(onClick = onAbort) {
                    Text(
                        text = stringResource(R.string.processes_abort_action),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        val description = job.progress?.description?.takeIf { it.isNotBlank() }
            ?: job.description?.takeIf { it.isNotBlank() }
        if (description != null) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 28.dp)
            )
        }

        if (running && percent != null && percent > 0) {
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { (percent / 100.0).toFloat().coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 28.dp)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

private val Color0x2E7D32 = androidx.compose.ui.graphics.Color(0xFF2E7D32)

/** `disk.sync_all` -> `Disk sync all`; the raw method stays in the subtitle. */
internal fun humanJobTitle(method: String): String {
    val tail = method.substringAfterLast('.')
    return tail.split('_').joinToString(" ") { word ->
        word.replaceFirstChar { it.uppercase() }
    }.ifBlank { method }
}