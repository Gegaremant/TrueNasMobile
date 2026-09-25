package com.gegaremant.truenasmobile.ui.homepage.pools

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Scanner
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gegaremant.truenasmobile.R
import com.gegaremant.truenasmobile.data.api.TrueNASApiManager
import com.gegaremant.truenasmobile.data.helpers.JobState
import com.gegaremant.truenasmobile.data.models.Storage
import com.gegaremant.truenasmobile.data.models.System.Pool
import com.gegaremant.truenasmobile.data.models.System.PoolDevice
import com.gegaremant.truenasmobile.data.models.System.PoolScan
import com.gegaremant.truenasmobile.data.models.System.PoolTopology
import com.gegaremant.truenasmobile.ui.background.WavyGradientBackground
import com.gegaremant.truenasmobile.ui.components.LoadingScreen
import com.gegaremant.truenasmobile.ui.components.UnifiedScreenHeader
import java.text.DecimalFormat
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun PoolDetailsScreen(
    manager: TrueNASApiManager,
    onNavigateBack: () -> Unit,
    onNavigateToFiles: (String) -> Unit
) {
    val viewModel: PoolDetailsViewModel = viewModel(
        factory = PoolDetailsViewModel.PoolDetailsViewModelFactory(manager)
    )
    val uiState by viewModel.uiState.collectAsState()
    var isLoading by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var currentPool: Pool? by remember { mutableStateOf(null) }

    when (val state = uiState) {
        is PoolDetailsUiState.Loading -> {
            isLoading = true
            isRefreshing = false
            error = null
        }
        is PoolDetailsUiState.Success -> {
            isLoading = false
            isRefreshing = state.isRefreshing
            error = null
            currentPool = state.pool
        }
        is PoolDetailsUiState.Error -> {
            isLoading = false
            isRefreshing = false
            error = state.message
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            UnifiedScreenHeader(
                title = stringResource(R.string.pooldetails_title),
                subtitle = stringResource(R.string.pooldetails_subtitle_fmt, currentPool?.name ?: stringResource(R.string.pooldetails_subtitle_current)),
                isLoading = isLoading,
                isRefreshing = isRefreshing,
                error = error,
                onRefresh = { viewModel.refresh() },
                onDismissError = { error = null },
                manager = manager,
                onBackPressed = onNavigateBack
            )

            when (val state = uiState) {
                is PoolDetailsUiState.Loading -> LoadingScreen(stringResource(R.string.pooldetails_loading))
                is PoolDetailsUiState.Error -> {}
                is PoolDetailsUiState.Success -> PoolDetailsContent(
                    pool = state.pool,
                    scrubTasks = state.scrubTasks,
                    onCreateScrubTask = viewModel::createScrubTask,
                    onUpdateScrubTask = viewModel::updateScrubTask,
                    onDeleteScrubTask = { id -> viewModel.deleteScrubTask(id) },
                    onRunScrubTask = viewModel::runScrubTask,
                    onSwitchScrubState = viewModel::switchScrubTaskState,
                    jobStates = state.jobStates,
                    onNavigateToFiles = { onNavigateToFiles(state.pool.name) }
                )
            }
        }
    }
}

@Composable
private fun PoolDetailsContent(
    pool: Pool,
    scrubTasks: List<Storage.PoolScrubQueryResponse>,
    jobStates: Map<String, JobState>,
    onCreateScrubTask: (Storage.UpdatePoolScrubDetails) -> Unit,
    onUpdateScrubTask: (Int, Storage.UpdatePoolScrubDetails) -> Unit,
    onDeleteScrubTask: (Int) -> Unit,
    onRunScrubTask: (Storage.RunPoolScrubArgs) -> Unit,
    onSwitchScrubState: (String, Long, Storage.PoolScrubAction) -> Unit,
    onNavigateToFiles: () -> Unit
) {
    val poolIdLong = pool.id?.toLong() ?: -1L
    val scrubTask = scrubTasks.find { it.pool == poolIdLong }
    var showCreateDialog by remember { mutableStateOf(false) }
    var taskToEdit by remember { mutableStateOf<Storage.PoolScrubQueryResponse?>(null) }
    var taskToDelete by remember { mutableStateOf<Storage.PoolScrubQueryResponse?>(null) }
    var taskToRun by remember { mutableStateOf<Storage.PoolScrubQueryResponse?>(null) }


    if (showCreateDialog) {
        CreateScrubTaskDialog(
            poolId = pool.id?.toLong() ?: -1L,
            onDismiss = { showCreateDialog = false },
            onConfirm = onCreateScrubTask
        )
    }

    taskToEdit?.let { task ->
        CreateScrubTaskDialog(
            poolId = pool.id?.toLong() ?: -1L,
            existingTask = task,
            onDismiss = { taskToEdit = null },
            onConfirm = { updatedDetails -> onUpdateScrubTask(task.id.toInt(), updatedDetails) }
        )
    }

    taskToDelete?.let { task ->
        AlertDialog(
            onDismissRequest = { taskToDelete = null },
            title = { Text(stringResource(R.string.pooldetails_delete_task_title)) },
            text = { Text(stringResource(R.string.pooldetails_delete_task_message)) },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteScrubTask(task.id.toInt())
                        taskToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text(stringResource(R.string.common_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { taskToDelete = null }) { Text(stringResource(R.string.common_cancel)) }
            }
        )
    }
    taskToRun?.let { task ->
        AlertDialog(
            onDismissRequest = { taskToRun = null },
            title = { Text(stringResource(R.string.pooldetails_run_task_title)) },
            text = { Text(stringResource(R.string.pooldetails_run_task_message, task.pool_name)) },
            confirmButton = {
                Button(onClick = {
                    val args = Storage.RunPoolScrubArgs(name = task.pool_name, threshold = task.threshold)
                    onRunScrubTask(args)
                    taskToRun = null
                }) { Text(stringResource(R.string.pooldetails_run)) }
            },
            dismissButton = {
                TextButton(onClick = { taskToRun = null }) { Text(stringResource(R.string.common_cancel)) }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        PoolInfoHeader(pool = pool)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.End
        ) {
            FilledTonalButton(
                onClick = onNavigateToFiles,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.pooldetails_browse_datasets))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            PoolStorageUsageCard(pool = pool)
            PoolStatusSection(pool = pool)
            pool.scan?.let {
                PoolScanSection(scan = it)
            }
            if (scrubTask != null) {
                val jobKey = "scrub_${scrubTask.id}"
                PoolScrubSection(
                    scrubTask = scrubTask,
                    onAddClick = { showCreateDialog = true },
                    onEditClick = { taskToEdit = scrubTask },
                    onDeleteClick = { taskToDelete = scrubTask },
                    onRunClick = { taskToRun = scrubTask },
                    onSwitchStateClick = {
                        val action =
                            if (scrubTask.enabled) Storage.PoolScrubAction.STOP else Storage.PoolScrubAction.START
                        onSwitchScrubState(pool.name, scrubTask.id, action)
                    },
                    jobState = jobStates[jobKey]
                )
            } else {
                NoScrubTaskSection(onAddClick = { showCreateDialog = true })
            }
        }
    }
}

@Composable
private fun PoolInfoHeader(pool: Pool) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(
                RoundedCornerShape(
                    bottomStart = 24.dp,
                    bottomEnd = 24.dp,
                    topStart = 24.dp,
                    topEnd = 24.dp
                )
            )
            .padding(horizontal = 10.dp)
    ) {
        WavyGradientBackground {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Storage,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(48.dp)
                )

                Text(
                    text = pool.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = stringResource(R.string.pooldetails_total_fmt, pool.status, pool.size!!.toGigabytesString()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                    fontSize = 14.sp
                )
            }
        }
    }
}
fun Long.toGigabytesString(): String {
    if (this == 0L) {
        return "0 GB"
    }

    val gibibyte = 1024.0 * 1024.0 * 1024.0
    val gbValue = this.toDouble() / gibibyte

    val decimalFormat = DecimalFormat("#.#")

    return "${decimalFormat.format(gbValue)} GB"
}

@Composable
private fun PoolStorageUsageCard(pool: Pool) {
    val usedPercentage = pool.size?.let { if (it > 0) (pool.allocated!!.toFloat() / pool.size.toFloat()) else 0f }
    val progressColor = when {
        usedPercentage!! > 0.9f -> MaterialTheme.colorScheme.error
        usedPercentage > 0.75f -> Color(0xFFF57C00) // Orange
        else -> MaterialTheme.colorScheme.primary
    }

    // Animated progress
    var targetProgress by remember { mutableFloatStateOf(0f) }
    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(durationMillis = 1000, delayMillis = 100),
        label = "storageProgress"
    )

    LaunchedEffect(usedPercentage) {
        targetProgress = usedPercentage!!
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.pooldetails_storage_usage),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${DecimalFormat("#.#").format(usedPercentage * 100)}%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = progressColor
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = progressColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = ProgressIndicatorDefaults.LinearStrokeCap
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    stringResource(R.string.pooldetails_used_fmt, pool.allocated!!.toGigabytesString()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    stringResource(R.string.pooldetails_free_fmt, pool.free!!.toGigabytesString()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PoolInfoSection(
    title: String,
    icon: ImageVector,
    action: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            action?.invoke()
        }
        Column(content = content)
    }
}

@Composable
private fun PoolInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun PoolStatusSection(pool: Pool) {
    PoolInfoSection(title = stringResource(R.string.pooldetails_status_health), icon = Icons.Default.Info) {
        val statusColor = when {
            !pool.healthy -> MaterialTheme.colorScheme.error
            pool.warning -> Color(0xFFF57C00)
            else -> Color(0xFF2E7D32)
        }
        val statusIcon = when {
            !pool.healthy -> Icons.Default.Error
            pool.warning -> Icons.Default.Warning
            else -> Icons.Default.CheckCircle
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = statusColor.copy(alpha = 0.1f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = statusIcon,
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = pool.status,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                    if (!pool.status_detail.isNullOrEmpty()) {
                        Text(
                            text = pool.status_detail,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        PoolInfoRow(stringResource(R.string.attr_fragmentation), pool.fragmentation!!)
        PoolInfoRow(stringResource(R.string.pooldetails_autotrim), pool.autotrim?.value ?: stringResource(R.string.pooldetails_na))
    }
}

@Composable
private fun PoolScanSection(scan: PoolScan) {
    fun formatEpoch(timeMap: Map<String, Long>?): String {
        val epochSeconds = timeMap?.get("\$date")?.div(1000) ?: return stringResource(R.string.common_na)
        val dateTime = LocalDateTime.ofInstant(Instant.ofEpochSecond(epochSeconds), ZoneId.systemDefault())
        return dateTime.format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm"))
    }

    // Animated scan progress
    var targetScanProgress by remember { mutableFloatStateOf(0f) }
    val animatedScanProgress by animateFloatAsState(
        targetValue = targetScanProgress,
        animationSpec = tween(durationMillis = 1000, delayMillis = 100),
        label = "scanProgress"
    )

    LaunchedEffect(scan.percentage) {
        targetScanProgress = (scan.percentage ?: 0.0).toFloat() / 100f
    }

    PoolInfoSection(title = stringResource(R.string.pool_last_scan), icon = Icons.Default.Scanner) {
        PoolInfoRow(stringResource(R.string.pool_function), scan.function ?: stringResource(R.string.pool_none_found))
        PoolInfoRow(stringResource(R.string.pool_state), scan.state ?: stringResource(R.string.pool_none_found))
        PoolInfoRow(stringResource(R.string.pool_started), formatEpoch(scan.start_time))
        PoolInfoRow(stringResource(R.string.pool_ended), formatEpoch(scan.end_time))
        PoolInfoRow(stringResource(R.string.pool_errors), scan.errors.toString())

        if (scan.state == "SCANNING") {
            Spacer(modifier = Modifier.height(8.dp))
            if (scan.percentage != null){
                LinearProgressIndicator(
                    progress = { animatedScanProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        text = "Progress: ${"%.2f".format(scan.percentage)}%",
                        style = MaterialTheme.typography.bodySmall
                    )
                    scan.total_secs_left?.let {
                        Text(
                            text = "Time left: ${it / 60}m ${it % 60}s",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }else{
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        text = stringResource(R.string.pool_no_usage_stat),
                        style = MaterialTheme.typography.bodySmall
                            .copy(color = MaterialTheme.colorScheme.error)
                    )
                }
            }
        }
    }
}
@Composable
private fun PoolScrubSection(
    scrubTask: Storage.PoolScrubQueryResponse,
    onAddClick: () -> Unit,
    jobState: JobState?,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onRunClick: () -> Unit,
    onSwitchStateClick: () -> Unit
) {

    fun formatSchedule(schedule: Storage.DeletionSchedule): String {
        return "${schedule.minute} ${schedule.hour} ${schedule.dom} ${schedule.month} ${schedule.dow}"
    }

    PoolInfoSection(
        title = stringResource(R.string.pool_scrub_details),
        icon = Icons.Default.EventRepeat,
        action = {
            Row {
                IconButton(onClick = onEditClick) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = stringResource(R.string.pool_edit_scrub_cd),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onDeleteClick) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.pool_delete_scrub_cd),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    )  {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (scrubTask.enabled)
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                else
                    MaterialTheme.colorScheme.surfaceVariant
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                PoolInfoRow(stringResource(R.string.attr_status), if (scrubTask.enabled) stringResource(R.string.common_enabled) else stringResource(R.string.common_disabled))
                PoolInfoRow(stringResource(R.string.share_description), scrubTask.description)
                PoolInfoRow(stringResource(R.string.pool_threshold_days), scrubTask.threshold.toString())
                PoolInfoRow(stringResource(R.string.pool_cron_schedule), formatSchedule(scrubTask.schedule))
            }
        }
        if (jobState != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Column(modifier = Modifier.padding(horizontal = 4.dp)) {
                Text(
                    "Job Status: ${jobState.state}",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { jobState.progress / 100f },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))
                )
                jobState.description?.let {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}


@Composable
private fun NoScrubTaskSection(onAddClick: () -> Unit) {
    PoolInfoSection(
        title = stringResource(R.string.pool_scrub_details),
        icon = Icons.Default.EventRepeat,
        action = {
            IconButton(onClick = onAddClick) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.pool_create_scrub_cd),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Text(
                text = stringResource(R.string.pool_no_scrub_task),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .padding(16.dp)
                    .align(Alignment.CenterHorizontally),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CreateScrubTaskDialog(
    poolId: Long,
    existingTask: Storage.PoolScrubQueryResponse? = null,
    onDismiss: () -> Unit,
    onConfirm: (Storage.UpdatePoolScrubDetails) -> Unit
) {
    var description by remember { mutableStateOf(existingTask?.description ?: "") }
    var threshold by remember { mutableStateOf(existingTask?.threshold?.toString() ?: "35") }
    var minute by remember { mutableStateOf(existingTask?.schedule?.minute ?: "00") }
    var hour by remember { mutableStateOf(existingTask?.schedule?.hour ?: "00") }
    var dayOfMonth by remember { mutableStateOf(existingTask?.schedule?.dom ?: "*") }
    var month by remember { mutableStateOf(existingTask?.schedule?.month ?: "*") }
    var dayOfWeek by remember { mutableStateOf(existingTask?.schedule?.dow ?: "7") }
    var enabled by remember { mutableStateOf(existingTask?.enabled ?: true) }

    val title = stringResource(if (existingTask == null) R.string.pool_create_scrub_task else R.string.pool_edit_scrub_task)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(R.string.share_description)) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = threshold,
                    onValueChange = { threshold = it },
                    label = { Text(stringResource(R.string.pool_threshold_days)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Text(stringResource(R.string.pool_schedule_cron), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = minute, onValueChange = { minute = it }, label = { Text(stringResource(R.string.pool_min)) }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = hour, onValueChange = { hour = it }, label = { Text(stringResource(R.string.pool_hour)) }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = dayOfMonth, onValueChange = { dayOfMonth = it }, label = { Text(stringResource(R.string.pool_day_m)) }, modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = month, onValueChange = { month = it }, label = { Text("Month") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = dayOfWeek, onValueChange = { dayOfWeek = it }, label = { Text("Day (W)") }, modifier = Modifier.weight(1f))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = enabled, onCheckedChange = { enabled = it })
                    Text("Enabled")
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val schedule = Storage.DeletionSchedule(
                    minute = minute,
                    hour = hour,
                    dom = dayOfMonth,
                    month = month,
                    dow = dayOfWeek
                )
                val details = Storage.UpdatePoolScrubDetails(
                    pool = poolId,
                    threshold = threshold.toIntOrNull() ?: 35,
                    description = description,
                    schedule = schedule,
                    enabled = enabled
                )
                onConfirm(details)
                onDismiss()
            }) {
                Text(if (existingTask == null) "Create" else "Update")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}