package com.gegaremant.truenasmobile.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gegaremant.truenasmobile.R
import com.gegaremant.truenasmobile.data.api.TrueNASApiManager
import com.gegaremant.truenasmobile.data.helpers.MultiAccountPrefs
import com.gegaremant.truenasmobile.ui.alerts.AlertsBellButton
import com.gegaremant.truenasmobile.ui.homepage.ShutdownDialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun UnifiedScreenHeader(
    title: String,
    subtitle: String,
    isLoading: Boolean,
    isRefreshing: Boolean,
    error: String? = null,
    onRefresh: (() -> Unit)? = null,
    onDismissError: () -> Unit,
    manager: TrueNASApiManager ? = null,
    onBackPressed: (() -> Unit)? = null,
    onNavigateToSettings: (() -> Unit)? = null,
    onShutdownInvoke: (() -> Unit)? = null,
    trailingActions: @Composable RowScope.() -> Unit = {},
    onSearchClick: (() -> Unit)? = null,
    autoHideSubtitle: Boolean = false,
    /**
     * Шапка-шаблон главных вкладок: верхняя линия — название приложения
     * слева и автор справа; вторая линия — название вкладки и иконки:
     * настройки инстанса (ящик с шестерёнкой), поиск, профиль подключения,
     * уведомления, питание и настройки приложения (робот с шестерёнкой).
     */
    showBrandLine: Boolean = false,
    onInstanceSettingsClick: (() -> Unit)? = null,
    onProfileClick: (() -> Unit)? = null,
    /**
     * Долгое нажатие на аватарку профиля: список сохранённых аккаунтов для
     * мгновенного переключения, без захода на страницу аккаунтов.
     */
    onProfileLongClick: (() -> Unit)? = null,
    onApplicationSettingsClick: (() -> Unit)? = null,
    showPowerControl: Boolean = false
) {
    var isSubtitleVisible by remember { mutableStateOf(true) }
    // Когда питанием управляет сама шапка (showPowerControl), диалог
    // выключения живёт здесь, а не в экране.
    var showPowerDialog by remember { mutableStateOf(false) }

    // ── Адаптивность шапки под конкретное устройство ───────────────────────
    // Два независимых фактора: вырез камеры сверху и ширина экрана.
    val density = LocalDensity.current
    val screenWidthDp = LocalConfiguration.current.screenWidthDp.dp
    val isNarrow = screenWidthDp < 400.dp
    // Вырез камеры: если он есть, верхняя строка уходит под него и держится
    // по краям (имя слева, автор справа), а шрифт мельче. Если выреза нет —
    // места больше, шрифт крупнее.
    val cutoutTop = with(density) { WindowInsets.displayCutout.getTop(density).toDp() }
    val hasCameraCutout = cutoutTop > 0.dp

    // Имя текущего профиля для аватарки. Читается из DataStore один раз на
    // менеджер; актуально только когда аватарка включена.
    val context = LocalContext.current
    var profileInitial by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(manager) {
        if (manager == null || onProfileClick == null) return@LaunchedEffect
        profileInitial = runCatching {
            val (_, accountId) = MultiAccountPrefs.getLastUsedProfile(context)
                ?: return@runCatching null
            // Аватара нет - показываем первую букву имени пользователя.
            MultiAccountPrefs.getAccount(context, accountId)?.username
                ?.trim()
                ?.firstOrNull()
                ?.uppercaseChar()
                ?.toString()
        }.getOrNull()
    }

    LaunchedEffect(subtitle, autoHideSubtitle) {
        isSubtitleVisible = true
        if (autoHideSubtitle) {
            delay(4000.milliseconds)
            isSubtitleVisible = false
        }
    }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            if (showBrandLine) {
                // Верхняя линия шаблона: приложение слева, автор справа.
                // Вырез камеры сверху — либо строка уходит под него и держится
                // по краям от камеры, либо (выреза нет) шрифт крупнее.
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            top = if (hasCameraCutout) cutoutTop else 0.dp,
                            bottom = 10.dp
                        ),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.brand_app_name),
                        style = when {
                            isNarrow -> MaterialTheme.typography.titleSmall
                            hasCameraCutout -> MaterialTheme.typography.titleSmall
                            else -> MaterialTheme.typography.titleMedium
                        },
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Text(
                        text = stringResource(R.string.brand_author),
                        style = when {
                            isNarrow -> MaterialTheme.typography.labelSmall
                            hasCameraCutout -> MaterialTheme.typography.labelSmall
                            else -> MaterialTheme.typography.bodySmall
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    if (onBackPressed != null) {
                        ExpressiveIconButton(
                            onClick = onBackPressed,
                            icon = Icons.Default.ArrowBackIosNew,
                            contentDescription = stringResource(R.string.cd_back_cd),
                            enabled = !isLoading && !isRefreshing,
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                    }

                    Column(modifier = Modifier.weight(1f)) {

                        AnimatedContent(
                            targetState = title,
                            transitionSpec = {
                                (slideInVertically { height -> height } + fadeIn()).togetherWith(
                                    slideOutVertically { height -> -height } + fadeOut())
                            },
                            label = "TitleAnimation"
                        ) { targetTitle ->
                            Text(
                                text = targetTitle,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }


                        AnimatedVisibility(
                            visible = isSubtitleVisible && subtitle.isNotEmpty(),
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(if (isNarrow) 0.dp else 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    // На узком экране шесть иконок и название не помещаются
                    // рядом: сжимаем кнопки, а если всё равно тесно - ряд
                    // прокручивается, чтобы ничего не обрезалось молча.
                    modifier = if (isNarrow) {
                        Modifier.horizontalScroll(rememberScrollState())
                    } else {
                        Modifier
                    }
                ) {
                    trailingActions()

                    // Порядок иконок второй линии по ТЗ: настройки инстанса,
                    // поиск, профиль подключения, уведомления, питание,
                    // настройки приложения. Остальные (refresh/настройки)
                    // остаются как legacy-параметры для детальных экранов.
                    onInstanceSettingsClick?.let { instanceSettings ->
                        GearedIconButton(
                            onClick = instanceSettings,
                            mainIcon = Icons.Default.Dns,
                            contentDescription = stringResource(R.string.cd_instance_settings_cd),
                            compact = isNarrow
                        )
                    }

                    if (onSearchClick != null) {
                        ExpressiveIconButton(
                            onClick = onSearchClick,
                            icon = Icons.Default.Search,
                            contentDescription = stringResource(R.string.search_cd),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = if (isNarrow) Modifier.size(40.dp) else Modifier
                        )
                    }

                    if (onProfileClick != null) {
                        ProfileAvatar(
                            initial = profileInitial,
                            onClick = onProfileClick,
                            onLongClick = onProfileLongClick
                        )
                    }

                    manager?.let{
                        AlertsBellButton(manager = manager)
                    }

                    if (onShutdownInvoke != null || showPowerControl) {
                        ExpressiveIconButton(
                            onClick = {
                                onShutdownInvoke?.invoke()
                                    ?: run { showPowerDialog = true }
                            },
                            icon = Icons.Default.PowerSettingsNew,
                            contentDescription = stringResource(R.string.cd_power_cd),
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = if (isNarrow) Modifier.size(40.dp) else Modifier
                        )
                    }

                    onApplicationSettingsClick?.let { appSettings ->
                        GearedIconButton(
                            onClick = appSettings,
                            mainIcon = Icons.Default.PhoneAndroid,
                            contentDescription = stringResource(R.string.cd_application_settings_cd),
                            compact = isNarrow
                        )
                    }

                    onRefresh?.let{ onRefresh ->
                        ExpressiveIconButton(
                            onClick = onRefresh,
                            icon = Icons.Default.Refresh,
                            contentDescription = stringResource(R.string.cd_refresh_cd),
                            enabled = !isLoading && !isRefreshing,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = if (isNarrow) Modifier.size(40.dp) else Modifier
                        )
                    }

                    if (onNavigateToSettings != null && onApplicationSettingsClick == null) {
                        ExpressiveIconButton(
                            onClick = onNavigateToSettings,
                            icon = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.cd_settings_cd),
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = if (isNarrow) Modifier.size(40.dp) else Modifier
                        )
                    }
                }
            }
            AnimatedVisibility(
                visible = isRefreshing,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(16.dp))
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(2.dp)
                            ),
                        color = MaterialTheme.colorScheme.primary, trackColor = Color.Transparent
                    )
                }
            }

            AnimatedVisibility(
                visible = error != null,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                if (error != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = error,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(
                                onClick = onDismissError
                            ) {
                                Text(
                                    "Dismiss",
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showPowerDialog && manager != null) {
        // Питанием управляет сама шапка-шаблон: одинаковый диалог на всех
        // вкладках, без дублирования логики в каждом экране.
        val scope = rememberCoroutineScope()
        ShutdownDialog(
            onShutdown = { reason ->
                showPowerDialog = false
                scope.launch {
                    ToastManager.showInfoRes(R.string.toast_shutdown_init)
                    when (val result = manager.system.shutdownSystemWithResult(reason)) {
                        is com.gegaremant.truenasmobile.data.ApiResult.Success ->
                            ToastManager.showSuccessRes(R.string.toast_shutdown_success)
                        is com.gegaremant.truenasmobile.data.ApiResult.Error ->
                            ToastManager.showError(result.message)
                        is com.gegaremant.truenasmobile.data.ApiResult.Loading -> {}
                    }
                }
            },
            onRestart = { reason ->
                showPowerDialog = false
                scope.launch {
                    ToastManager.showInfoRes(R.string.toast_restart_init)
                    when (val result = manager.system.rebootSystem(reason)) {
                        is com.gegaremant.truenasmobile.data.ApiResult.Success ->
                            ToastManager.showSuccessRes(R.string.toast_restart_success)
                        is com.gegaremant.truenasmobile.data.ApiResult.Error ->
                            ToastManager.showError(result.message)
                        is com.gegaremant.truenasmobile.data.ApiResult.Loading -> {}
                    }
                }
            },
            onDismiss = { showPowerDialog = false }
        )
    }
}

/**
 * Minimal header variant: renders only the back button, using the exact
 * same [ExpressiveIconButton] styling as [UnifiedScreenHeader]'s back
 * action (same icon, same press-scale animation, same default container
 * color).
 *
 * Unlike [UnifiedScreenHeader], this does NOT wrap itself in a full-width
 * [Surface]/[Column] -- there's no title, subtitle, trailing actions,
 * refresh bar, or error card. It's just the button, sized to its content,
 * so it doesn't claim a header-height band across the top of the screen.
 * It's meant to be layered as an overlay (e.g. inside a [Box]) on top of
 * your own content, so that content can occupy the space a full header
 * would otherwise have taken -- including scrolling/being visible behind
 * and around the button itself.
 *
 * Example:
 * ```
 * Box(modifier = Modifier.fillMaxSize()) {
 *     YourContent(modifier = Modifier.fillMaxSize())
 *     MinimalBackHeader(
 *         onBackPressed = onNavigateBack,
 *         modifier = Modifier
 *             .align(Alignment.TopStart)
 *             .padding(16.dp)
 *     )
 * }
 * ```
 */
@Composable
fun MinimalBackHeader(
    onBackPressed: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f),
    error: String? = null,
    onDismissError: () -> Unit = {}
) {
    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            containerColor.copy(alpha = 0.85f),
                            containerColor.copy(alpha = 0.7f)
                        )
                    )
                )
        ) {
            ExpressiveIconButton(
                onClick = onBackPressed,
                icon = Icons.Default.ArrowBackIosNew,
                contentDescription = stringResource(R.string.cd_back_cd),
                enabled = enabled,
                containerColor = Color.Transparent,
                modifier = Modifier.fillMaxSize()
            )
        }

        if (error != null) {
            Column {
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.85f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.widthIn(max = 260.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        TextButton(
                            onClick = onDismissError,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.15f)
                                )
                        ) {
                            Text(
                                "Dismiss",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ExpressiveIconButton(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    containerColor: Color = Color.Transparent
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1f,
        animationSpec = tween(durationMillis = 100),
        label = "ButtonScale"
    )

    IconButton(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interactionSource,
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = containerColor,
            contentColor = tint,
            disabledContainerColor = containerColor.copy(alpha = 0.5f),
            disabledContentColor = tint.copy(alpha = 0.3f)
        ),
        modifier = modifier
            .scale(scale)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(24.dp)
        )
    }
}

/**
 * Иконка с маленькой шестерёнкой в углу: «ящик с шестерёнкой» (настройки
 * инстанса) и «робот с шестерёнкой» (настройки приложения) из шаблона шапки.
 */
@Composable
private fun GearedIconButton(
    onClick: () -> Unit,
    mainIcon: ImageVector,
    contentDescription: String,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    compact: Boolean = false
) {
    Box(modifier = Modifier.size(if (compact) 40.dp else 44.dp)) {
        ExpressiveIconButton(
            onClick = onClick,
            icon = mainIcon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.fillMaxSize()
        )
        Icon(
            imageVector = Icons.Default.Settings,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(if (compact) 11.dp else 13.dp)
        )
    }
}

/**
 * Аватарка текущего профиля: круг с первой буквой имени пользователя (своей
 * картинки у профиля нет, поэтому буква — это и есть аватар). Короткое нажатие
 * открывает страницу аккаунтов, длинное - список аккаунтов для быстрого
 * переключения. Имя профиля рядом не показывается намеренно: длинное имя
 * съедало бы ширину, из-за чего на узком экране не помещались иконки.
 */
@Composable
private fun ProfileAvatar(
    initial: String?,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = tween(durationMillis = 100),
        label = "ProfileAvatarScale"
    )
    val cd = stringResource(R.string.profile_title)

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onLongClick = onLongClick,
                onClick = onClick
            )
            .scale(scale)
            .semantics { contentDescription = cd }
    ) {
        Text(
            text = initial ?: "?",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}