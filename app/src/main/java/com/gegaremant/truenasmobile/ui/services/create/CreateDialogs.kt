package com.gegaremant.truenasmobile.ui.services.create

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.gegaremant.truenasmobile.R
import com.gegaremant.truenasmobile.ui.components.ToastManager

/**
 * "New container" and "New virtual machine", which used to say "in a future
 * update" - the API turned out to be callable: `container.create` (name plus an
 * image object, answered with a job id) and `vm.create` (name plus memory in
 * MiB, answered with the instance).
 *
 * Deliberately the minimum the API asks for. Anything richer would be guesswork
 * about the stand's pools and devices, and a create form that lies is worse
 * than a short one.
 */
@Composable
fun CreateContainerDialog(
    pools: List<String>,
    onDismiss: () -> Unit,
    onCreate: (name: String, image: String, pool: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var image by remember { mutableStateOf(DEFAULT_IMAGE) }
    var pool by remember { mutableStateOf(pools.firstOrNull().orEmpty()) }
    var poolMenuOpen by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.create_container_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.create_container_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = image,
                    onValueChange = { image = it },
                    label = { Text(stringResource(R.string.create_container_image)) },
                    supportingText = { Text(stringResource(R.string.create_container_image_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                // The API insists on a pool: without one it fails with
                // "Either configure a preferred pool in lxc settings or
                // provide a pool name".
                Box {
                    OutlinedButton(
                        onClick = { poolMenuOpen = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = pool.ifBlank { stringResource(R.string.create_container_pool_hint) },
                            modifier = Modifier.weight(1f)
                        )
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }
                    DropdownMenu(
                        expanded = poolMenuOpen,
                        onDismissRequest = { poolMenuOpen = false }
                    ) {
                        pools.forEach { candidate ->
                            DropdownMenuItem(
                                text = { Text(candidate) },
                                onClick = {
                                    pool = candidate
                                    poolMenuOpen = false
                                }
                            )
                        }
                    }
                }
                Text(
                    text = stringResource(R.string.create_container_pool_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    when {
                        name.isBlank() -> ToastManager.showErrorRes(R.string.create_name_required)
                        image.isBlank() -> ToastManager.showErrorRes(R.string.create_container_image_required)
                        pool.isBlank() -> ToastManager.showErrorRes(R.string.create_container_pool_required)
                        else -> onCreate(name.trim(), image.trim(), pool)
                    }
                }
            ) { Text(stringResource(R.string.create_action)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
        }
    )
}

@Composable
fun CreateVmDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, memoryMiB: Int, vcpus: Int) -> Unit
) {
    var name by remember { mutableStateOf("") }
    // The API takes MiB; the user thinks in GiB. 4 GiB is a sane first VM.
    var memoryGb by remember { mutableStateOf("4") }
    var vcpus by remember { mutableStateOf("1") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.create_vm_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.create_vm_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = memoryGb,
                        onValueChange = { memoryGb = it.filter(Char::isDigit) },
                        label = { Text(stringResource(R.string.create_vm_memory)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = vcpus,
                        onValueChange = { vcpus = it.filter(Char::isDigit) },
                        label = { Text(stringResource(R.string.create_vm_vcpus)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
                Text(
                    text = stringResource(R.string.create_vm_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val memory = memoryGb.toIntOrNull()
                    val cpu = vcpus.toIntOrNull()
                    when {
                        name.isBlank() -> ToastManager.showErrorRes(R.string.create_name_required)
                        memory == null || memory <= 0 ->
                            ToastManager.showErrorRes(R.string.create_vm_memory_invalid)
                        cpu == null || cpu <= 0 ->
                            ToastManager.showErrorRes(R.string.create_vm_vcpus_invalid)
                        else -> onCreate(name.trim(), memory * 1024, cpu)
                    }
                }
            ) { Text(stringResource(R.string.create_action)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
        }
    )
}

@Composable
fun CreatingProgressDialog(message: String) {
    AlertDialog(
        onDismissRequest = {},
        text = {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = message,
                    modifier = Modifier.padding(start = 14.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        },
        confirmButton = {}
    )
}

private const val DEFAULT_IMAGE = "alpine:latest"