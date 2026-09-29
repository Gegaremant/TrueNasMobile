package com.gegaremant.truenasmobile.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

@Composable
fun TrueNasMobileAppTheme(
    theme: AppTheme = AppTheme.TRUENASMOBILE,
    darkTheme: Boolean = isSystemInDarkTheme(),
    isBlackMode: Boolean = false,
    content: @Composable () -> Unit
) {
    var colorScheme = when (theme) {
        AppTheme.DYNAMIC -> {
            val context = LocalContext.current
            // Material You wallpaper colours arrived in Android 12 (API 31).
            // The minSdk is 29, so on 10 and 11 there is nothing to read and the
            // call would throw NoSuchMethodError - lint caught it as NewApi the
            // moment the floor moved, and it shipped in 1.0.3 because only
            // lintVital had ever run.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (darkTheme) dynamicDarkColorScheme(context)
                else dynamicLightColorScheme(context)
            } else {
                if (darkTheme) TrueNasMobileDarkColors else TrueNasMobileLightColors
            }
        }
        AppTheme.TRUENASMOBILE -> if (darkTheme) TrueNasMobileDarkColors else TrueNasMobileLightColors
        AppTheme.OCEAN -> if (darkTheme) OceanDarkColors else OceanLightColors
        AppTheme.FOREST -> if (darkTheme) ForestDarkColors else ForestLightColors
        AppTheme.SUNSET -> if (darkTheme) SunsetDarkColors else SunsetLightColors
        AppTheme.LAVENDER -> if (darkTheme) LavenderDarkColors else LavenderLightColors
        AppTheme.MONOCHROME -> if (darkTheme) MonochromeDarkColors else MonochromeLightColors
    }

    if (darkTheme && isBlackMode) {
        colorScheme = colorScheme.copy(
            // True OLED black canvas — no grey bleed-through on any surface role.
            surface = Color.Black,
            background = Color.Black,
            surfaceTint = Color.Black,
            surfaceDim = Color.Black,
            surfaceBright = Color.Black,
            surfaceContainer = Color(0xFF0A0A0A),
            surfaceContainerLow = Color(0xFF030303),
            surfaceContainerLowest = Color.Black,
            surfaceContainerHigh = Color(0xFF101010),
            surfaceContainerHighest = Color(0xFF141414),
            surfaceVariant = Color(0xFF151515)
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}