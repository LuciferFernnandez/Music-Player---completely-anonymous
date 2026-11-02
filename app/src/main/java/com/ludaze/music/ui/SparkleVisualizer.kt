package com.ludaze.music.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.ludaze.music.audio.AudioBus
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

private class Sparkle(var angle: Float, var dist: Float, var size: Float, var tw: Float, var tint: Float)

private fun spawn(startRandom: Boolean) = Sparkle(
    Random.nextFloat() * 6.2832f, if (startRandom) Random.nextFloat() else Random.nextFloat() * 0.05f,
    1.5f + Random.nextFloat() * 4f, Random.nextFloat() * 6.28f, Random.nextFloat()
)

@Composable
fun SparkleVisualizer(isPlaying: Boolean, modifier: Modifier = Modifier.fillMaxSize()) {
    val sparkles = remember { List(140) { spawn(true) } }
    val playing by rememberUpdatedState(isPlaying)
    var amp by remember { mutableFloatStateOf(0f) }
    var frame by remember { mutableLongStateOf(0L) }
    val teal = Accent; val ice = RoseGold

    LaunchedEffect(Unit) {
        var last = 0L
        while (true) {
            withFrameNanos { now ->
                val dt = if (last == 0L) 0.016f else min(0.05f, (now - last) / 1e9f)
                last = now
                val target = if (playing) AudioBus.level.value else 0f
                amp += (target - amp) * (if (target > amp) 0.5f else 0.08f) // fast attack, slow release
                val speed = 0.04f + amp * 1.1f               // quiet = slow, loud = fast
                sparkles.forEach { s ->
                    s.dist += speed * dt * (0.4f + s.dist)   // accelerate outward
                    s.tw += dt * (2f + amp * 10f)
                    if (s.dist > 1.15f) { val n = spawn(false); s.angle = n.angle; s.dist = n.dist; s.size = n.size; s.tint = n.tint }
                }
                frame = now
            }
        }
    }

    Canvas(modifier) {
        frame // observe
        val c = Offset(size.width / 2, size.height / 2)
        val maxR = max(size.width, size.height) * 0.62f
        drawCircle(
            Brush.radialGradient(listOf(teal.copy(alpha = 0.06f + amp * 0.22f), Color.Transparent), c, maxR * (0.35f + amp * 0.3f)),
            maxR, c
        )
        sparkles.forEach { s ->
            val r = s.dist * maxR
            val p = Offset(c.x + cos(s.angle) * r, c.y + sin(s.angle) * r)
            val fade = min(1f, s.dist * 6f) * (1f - max(0f, s.dist - 0.8f) / 0.35f)
            val twinkle = 0.6f + 0.4f * sin(s.tw)
            val a = (fade * twinkle).coerceIn(0f, 1f)
            val col = if (s.tint > 0.7f) ice else teal
            val sz = s.size * (1f + amp * 1.6f) * (0.5f + s.dist)
            drawCircle(Brush.radialGradient(listOf(col.copy(alpha = a * 0.55f), Color.Transparent), p, sz * 4f), sz * 4f, p)
            drawCircle(Color.White.copy(alpha = a), sz * 0.45f, p)
        }
    }
}
