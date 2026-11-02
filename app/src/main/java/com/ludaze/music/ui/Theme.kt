package com.ludaze.music.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Bg = Color(0xFF0D0D0D)
val Panel = Color(0xFF161A1D)
val Slate = Color(0xFF64748B)
val Accent = Color(0xFFE5384F)
val RoseGold = Color(0xFFE8B4A6)

@Composable
fun LudazeTheme(content: @Composable () -> Unit) = MaterialTheme(
    colorScheme = darkColorScheme(
        background = Bg, surface = Panel, surfaceVariant = Panel, primary = Accent, onPrimary = Bg,
        secondary = Slate, onBackground = Color(0xFFE6EDF3), onSurface = Color(0xFFE6EDF3),
        onSurfaceVariant = Slate
    ),
    content = content
)
