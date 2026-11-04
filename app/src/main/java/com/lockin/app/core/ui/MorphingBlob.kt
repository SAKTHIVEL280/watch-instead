package com.lockin.app.core.ui

import android.provider.Settings
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.toPath
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.Morph

/** Slowly breathing Expressive shape behind an icon. Holds still when system animations are off. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MorphingBlob(icon: ImageVector, size: Dp = 112.dp, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val animate = remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f
    }
    val morph = remember { Morph(MaterialShapes.Cookie9Sided, MaterialShapes.Clover8Leaf) }
    val transition = rememberInfiniteTransition(label = "blob")
    val progress = transition.animateFloat(
        0f, 1f, infiniteRepeatable(tween(3_500), RepeatMode.Reverse), label = "blob morph",
    )
    val rotation = transition.animateFloat(
        0f, 360f, infiniteRepeatable(tween(24_000, easing = LinearEasing)), label = "blob spin",
    )
    val color = MaterialTheme.colorScheme.primaryContainer
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(size)
                .graphicsLayer { rotationZ = if (animate) rotation.value else 0f }
                .drawWithCache {
                    // Animated values are read while drawing, so the blob never causes a recomposition.
                    val path = Path()
                    val scale = Matrix().apply { scale(this@drawWithCache.size.width, this@drawWithCache.size.height) }
                    onDrawBehind {
                        path.rewind()
                        morph.toPath(if (animate) progress.value else 0f, path)
                        path.transform(scale)
                        drawPath(path, color)
                    }
                },
        )
        Icon(icon, null, Modifier.size(size * 0.42f), tint = MaterialTheme.colorScheme.onPrimaryContainer)
    }
}
