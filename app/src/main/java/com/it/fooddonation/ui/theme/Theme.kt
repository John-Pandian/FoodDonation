package com.it.fooddonation.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Primary,
    secondary = PrimaryLight,
    tertiary = PrimaryHover,
    background = Charcoal,
    surface = Charcoal,
    onPrimary = BackgroundLight,
    onSecondary = Charcoal,
    onBackground = BackgroundLight,
    onSurface = BackgroundLight
)

private val LightColorScheme = lightColorScheme(
    primary = Primary,
    secondary = Gray500,
    tertiary = PrimaryHover,
    background = BackgroundLight,
    surface = BackgroundLight,
    onPrimary = BackgroundLight,
    onSecondary = Charcoal,
    onBackground = Charcoal,
    onSurface = Charcoal,
    surfaceVariant = Gray100,
    onSurfaceVariant = Gray500
)

@Composable
fun FoodDonationTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}