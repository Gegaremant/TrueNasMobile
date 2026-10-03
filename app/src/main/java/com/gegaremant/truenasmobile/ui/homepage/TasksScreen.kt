package com.gegaremant.truenasmobile.ui.homepage

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.gegaremant.truenasmobile.R
import com.gegaremant.truenasmobile.data.api.TrueNASApiManager
import com.gegaremant.truenasmobile.data.models.Apps
import com.gegaremant.truenasmobile.data.models.Container
import com.gegaremant.truenasmobile.data.models.Vm
import com.gegaremant.truenasmobile.ui.components.UnifiedScreenHeader
import com.gegaremant.truenasmobile.ui.services.apps.AppsScreen
import com.gegaremant.truenasmobile.ui.services.containers.ContainersScreen
import com.gegaremant.truenasmobile.ui.services.vm.VmsScreen

private val TASKS_SUBTABS = listOf(
    R.string.tasks_tab_apps to "apps",
    R.string.tasks_tab_containers to "containers",
    R.string.tasks_tab_vms to "vms"
)

/**
 * Вкладка «Задачи»: шапка-шаблон, под ней переключатель
 * приложения/контейнеры/ВМ, под ним — ряд фильтров и список.
 *
 * Шапка живёт здесь, а не в под-экранах: раньше переключатель под-вкладок
 * стоял выше неё, и шапка «прыгала» между вкладками. Теперь она всегда на
 * одном месте, а под-экраны рисуют только фильтры и содержимое
 * ([headerShownByParent]).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(
    manager: TrueNASApiManager,
    onSearchClick: (() -> Unit)? = null,
    onNavigateToAppInfo: (Apps.AppQueryResponse) -> Unit = {},
    onOpenAdvanced: (String) -> Unit = {},
    onNavigateToUpgrade: (String) -> Unit = {},
    onNavigateToRollback: (String) -> Unit = {},
    onNavigateToMarketplace: () -> Unit = {},
    onNavigateToContainerInfo: (Container.ContainerResponse) -> Unit = {},
    onNavigateToVmInfo: (Vm.VmQueryResponse) -> Unit = {},
    onNavigateToInstanceSettings: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onNavigateToProfileLongPress: (() -> Unit)? = null,
    onNavigateToApplicationSettings: () -> Unit = {}
) {
    var selectedSubTab by rememberSaveable { mutableIntStateOf(0) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Шапка одна на всю вкладку: бренд-строка, название «Задачи» и иконки.
        UnifiedScreenHeader(
            title = stringResource(R.string.nav_tasks),
            subtitle = "",
            isLoading = false,
            isRefreshing = false,
            onDismissError = {},
            manager = manager,
            onSearchClick = onSearchClick,
            showBrandLine = true,
            onInstanceSettingsClick = onNavigateToInstanceSettings,
            onProfileClick = onNavigateToProfile,
            onProfileLongClick = onNavigateToProfileLongPress,
            onApplicationSettingsClick = onNavigateToApplicationSettings,
            showPowerControl = true
        )

        PrimaryTabRow(
            selectedTabIndex = selectedSubTab,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            TASKS_SUBTABS.forEachIndexed { index, (titleRes, _) ->
                Tab(
                    selected = selectedSubTab == index,
                    onClick = { selectedSubTab = index },
                    text = {
                        Text(
                            text = stringResource(titleRes),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                )
            }
        }
        Box(modifier = Modifier.fillMaxSize().weight(1f)) {
            when (selectedSubTab) {
                0 -> AppsScreen(
                    manager = manager,
                    onNavigateToAppInfo = onNavigateToAppInfo,
                    onOpenAdvanced = onOpenAdvanced,
                    onNavigateToUpgrade = onNavigateToUpgrade,
                    onNavigateToRollback = onNavigateToRollback,
                    onNavigateToMarketplace = onNavigateToMarketplace,
                    onSearchClick = onSearchClick,
                    onNavigateToInstanceSettings = onNavigateToInstanceSettings,
                    onNavigateToProfile = onNavigateToProfile,
                    headerShownByParent = true,
                    onNavigateToProfileLongPress = onNavigateToProfileLongPress,
                    onNavigateToApplicationSettings = onNavigateToApplicationSettings
                )
                1 -> ContainersScreen(
                    manager = manager,
                    onNavigateToContainerInfo = onNavigateToContainerInfo,
                    onSearchClick = onSearchClick,
                    onNavigateToInstanceSettings = onNavigateToInstanceSettings,
                    onNavigateToProfile = onNavigateToProfile,
                    headerShownByParent = true,
                    onNavigateToProfileLongPress = onNavigateToProfileLongPress,
                    onNavigateToApplicationSettings = onNavigateToApplicationSettings
                )
                else -> VmsScreen(
                    manager = manager,
                    onNavigateToVmInfo = onNavigateToVmInfo,
                    onSearchClick = onSearchClick,
                    onNavigateToInstanceSettings = onNavigateToInstanceSettings,
                    onNavigateToProfile = onNavigateToProfile,
                    headerShownByParent = true,
                    onNavigateToProfileLongPress = onNavigateToProfileLongPress,
                    onNavigateToApplicationSettings = onNavigateToApplicationSettings
                )
            }
        }
    }
}