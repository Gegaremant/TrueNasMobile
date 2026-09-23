package com.imnotndesh.truehub.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun ExpressiveFAB(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    icon: ImageVector = Icons.Default.Edit,
    text: String = "Edit",
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    initiallyExpanded: Boolean = true,
    expandedDurationMillis: Int = 2500,
    visible: Boolean = true,
) {
    var isExpanded by remember { mutableStateOf(initiallyExpanded) }

    if (initiallyExpanded) {
        LaunchedEffect(Unit) {
            delay(expandedDurationMillis.toLong().milliseconds)
            isExpanded = false
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(),
        exit = scaleOut() + fadeOut()
    ) {
        Surface(
            onClick = onClick,
            modifier = modifier.animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            ),
            shape = RoundedCornerShape(24.dp),
            color = containerColor,
            contentColor = contentColor,
            shadowElevation = 0.dp
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(
                    horizontal = if (isExpanded) 24.dp else 16.dp,
                    vertical   = 16.dp
                )
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = text,
                    modifier = Modifier.size(24.dp)
                )
                AnimatedVisibility(
                    visible = isExpanded,
                    enter = fadeIn() + scaleIn(spring()),
                    exit = fadeOut() + scaleOut(spring())
                ) {
                    Row {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = text,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    }
}