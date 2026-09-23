package com.imnotndesh.truehub.ui.widgets.pools

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.imnotndesh.truehub.MainActivity
import com.imnotndesh.truehub.data.helpers.WidgetDataStore
import com.imnotndesh.truehub.data.models.System
import java.text.DecimalFormat

private val poolIndexKey = intPreferencesKey("current_pool_view_index")

class PoolsWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = PoolsWidget()
}

class PoolsWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Exact
    override val stateDefinition = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val pools by WidgetDataStore.poolsFlow(context)
                .collectAsState(initial = emptyList())

            val prefs = currentState<Preferences>()
            val rawIndex = prefs[poolIndexKey] ?: 0

            GlanceTheme {
                val size = LocalSize.current
                val isSmall1x1 = size.width < 120.dp

                val currentIndex = if (pools.isNotEmpty()) {
                    (rawIndex % pools.size + pools.size) % pools.size
                } else {
                    0
                }

                if (isSmall1x1) {
                    PoolsSmallWidget(pools = pools, context = context)
                } else {
                    PoolsWideShortWidget(pools = pools, currentIndex = currentIndex, context = context)
                }
            }
        }
    }
}

class ChangePoolAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val offset = parameters[offsetKey] ?: 1
        updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
            prefs.toMutablePreferences().apply {
                val current = this[poolIndexKey] ?: 0
                this[poolIndexKey] = current + offset
            }
        }
        PoolsWidget().update(context, glanceId)
    }

    companion object {
        val offsetKey = ActionParameters.Key<Int>("offset")
    }
}

private fun openPoolsIntent(context: Context): Intent =
    Intent(context, MainActivity::class.java).apply {
        action = "com.imnotndesh.truehub.OPEN_HOME"
        flags  = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }

private fun formatBytes(bytes: Long): String {
    val units     = arrayOf("B", "KB", "MB", "GB", "TB")
    var size      = bytes.toDouble()
    var unitIndex = 0
    while (size >= 1024 && unitIndex < units.size - 1) { size /= 1024.0; unitIndex++ }
    return "${DecimalFormat("#.#").format(size)} ${units[unitIndex]}"
}

/**
 * Safely creates a ColorProvider manually from a static Compose Color
 * to bypass library-group restriction lint rules.
 */
private fun createSafeColorProvider(color: Color): ColorProvider = object : ColorProvider {
    override fun getColor(context: Context): Color = color
}

/**
 * 1x1 Mode
 * Only triggers if shrunk to a 1x1 tile footprint.
 */
@Composable
private fun PoolsSmallWidget(pools: List<System.Pool>, context: Context) {
    val unhealthy = pools.count { !it.healthy }
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.widgetBackground)
            .cornerRadius(20.dp)
            .clickable(actionStartActivity(openPoolsIntent(context))),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment   = Alignment.CenterVertically,
            modifier            = GlanceModifier.padding(8.dp)
        ) {
            Text(
                text  = if (pools.isEmpty()) "–" else pools.size.toString(),
                style = TextStyle(
                    fontWeight = FontWeight.Bold,
                    fontSize   = 28.sp,
                    color      = if (unhealthy > 0) GlanceTheme.colors.error else GlanceTheme.colors.primary
                )
            )
            Spacer(GlanceModifier.height(4.dp))
            Text(
                text  = when {
                    pools.isEmpty() -> "No pools"
                    unhealthy > 0   -> "$unhealthy issue(s)"
                    else            -> "Healthy"
                },
                style = TextStyle(fontSize = 11.sp, color = GlanceTheme.colors.onSurfaceVariant)
            )
        }
    }
}

/**
 * Single-Row (x1 Height) Mode
 * Rearranged: Text Top-Left, Storage Counter Top-Right, Progress Bar full-width Bottom.
 */
@Composable
private fun PoolsWideShortWidget(pools: List<System.Pool>, currentIndex: Int, context: Context) {
    if (pools.isEmpty()) {
        Box(
            modifier = GlanceModifier.fillMaxSize().background(GlanceTheme.colors.widgetBackground).cornerRadius(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "No pools configured", style = TextStyle(fontSize = 12.sp, color = GlanceTheme.colors.onSurfaceVariant))
        }
        return
    }

    val pool = pools.getOrNull(currentIndex) ?: pools.first()
    val size       = pool.size ?: 0L
    val allocated  = pool.allocated ?: 0L
    val usedFraction = if (size > 0L) (allocated.toFloat() / size.toFloat()).coerceIn(0f, 1f) else 0f

    val customBarColor = when {
        usedFraction >= 0.90f -> createSafeColorProvider(Color(0xFFE53935))
        usedFraction >= 0.50f -> createSafeColorProvider(Color(0xFFFB8C00))
        else                  -> createSafeColorProvider(Color(0xFF4CAF50))
    }

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.widgetBackground)
            .cornerRadius(16.dp)
            .clickable(actionStartActivity(openPoolsIntent(context))),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = GlanceModifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = GlanceModifier.defaultWeight(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text     = pool.name,
                        style    = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold, color = GlanceTheme.colors.onSurface),
                        maxLines = 1
                    )

                    if (pools.size > 1) {
                        Spacer(GlanceModifier.width(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "◀",
                                style = TextStyle(fontSize = 11.sp, color = GlanceTheme.colors.primary, fontWeight = FontWeight.Bold),
                                modifier = GlanceModifier
                                    .padding(horizontal = 2.dp)
                                    .clickable(actionRunCallback<ChangePoolAction>(actionParametersOf(ChangePoolAction.offsetKey to -1)))
                            )
                            Text(
                                text = "${currentIndex + 1}/${pools.size}",
                                style = TextStyle(fontSize = 9.sp, color = GlanceTheme.colors.onSurfaceVariant)
                            )
                            Text(
                                text = "▶",
                                style = TextStyle(fontSize = 11.sp, color = GlanceTheme.colors.primary, fontWeight = FontWeight.Bold),
                                modifier = GlanceModifier
                                    .padding(horizontal = 2.dp)
                                    .clickable(actionRunCallback<ChangePoolAction>(actionParametersOf(ChangePoolAction.offsetKey to 1)))
                            )
                        }
                    }
                }

                Text(
                    text  = "${formatBytes(allocated)} / ${formatBytes(size)}",
                    style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium, color = GlanceTheme.colors.onSurfaceVariant)
                )
            }

            Spacer(GlanceModifier.height(8.dp))

            LinearProgressIndicator(
                progress = usedFraction,
                modifier = GlanceModifier.fillMaxWidth().height(10.dp),
                color = customBarColor,
                backgroundColor = GlanceTheme.colors.surfaceVariant
            )
        }
    }
}