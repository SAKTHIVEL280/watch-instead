package com.lockin.app.core.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Tag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import androidx.compose.ui.layout.ContentScale
import com.lockin.app.R
import com.lockin.app.domain.Category
import com.lockin.app.domain.OpenReason
import com.lockin.app.domain.Platform

@get:StringRes
val Category.labelRes: Int
    get() = when (this) {
        Category.MOTIVATION -> R.string.category_motivation
        Category.EDUCATION -> R.string.category_education
        Category.CONSISTENCY -> R.string.category_consistency
        Category.PRODUCTIVITY -> R.string.category_productivity
        Category.OTHER -> R.string.category_other
    }

@get:StringRes
val OpenReason.labelRes: Int
    get() = when (this) {
        OpenReason.BORED -> R.string.reason_bored
        OpenReason.HABIT -> R.string.reason_habit
        OpenReason.STRESSED -> R.string.reason_stressed
        OpenReason.TIRED -> R.string.reason_tired
        OpenReason.LONELY -> R.string.reason_lonely
        OpenReason.AVOIDING_SOMETHING -> R.string.reason_avoiding
        OpenReason.TAKING_A_BREAK -> R.string.reason_break
        OpenReason.REPLYING_TO_SOMEONE -> R.string.reason_replying
        OpenReason.NEED_SOMETHING -> R.string.reason_need_something
    }

val Platform.icon: ImageVector
    get() = when (this) {
        Platform.YOUTUBE -> Icons.Outlined.PlayCircle
        Platform.INSTAGRAM -> Icons.Outlined.CameraAlt
        Platform.TIKTOK -> Icons.Outlined.MusicNote
        Platform.X -> Icons.Outlined.Tag
        Platform.OTHER -> Icons.Outlined.Language
    }

/** Connected single-choice row built from Expressive toggle buttons. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun <T> ConnectedChoice(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    label: @Composable (T) -> String,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
    ) {
        options.forEachIndexed { index, option ->
            ToggleButton(
                checked = option == selected,
                onCheckedChange = { onSelect(option) },
                modifier = Modifier.weight(1f).semantics { role = Role.RadioButton },
                shapes = when (index) {
                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                    options.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                },
            ) {
                Text(label(option), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

/**
 * Video thumbnail drawn over a platform glyph, which shows through until the image loads or if it
 * fails. AsyncImage rather than the subcomposing variant, which is too costly for list items.
 */
@Composable
fun LinkThumbnail(url: String?, platform: Platform, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val request = remember(url) {
        url?.let { ImageRequest.Builder(context).data(it).crossfade(true).build() }
    }
    Box(
        modifier.clip(MaterialTheme.shapes.medium).background(MaterialTheme.colorScheme.secondaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            platform.icon,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(0.4f),
            tint = MaterialTheme.colorScheme.onSecondaryContainer,
        )
        if (request != null) {
            AsyncImage(
                model = request,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
fun StatusBanner(title: String, body: String, action: String, onAction: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        ),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(body, style = MaterialTheme.typography.bodyMedium)
            Button(onClick = onAction, modifier = Modifier.align(Alignment.End)) {
                Text(action)
            }
        }
    }
}

@Composable
fun EmptyState(icon: ImageVector, title: String, body: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        MorphingBlob(icon)
        Text(title, style = MaterialTheme.typography.titleLarge)
        Text(
            body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}

@Composable
fun AppIcon(packageName: String, size: Dp, load: suspend (String) -> ImageBitmap?) {
    val icon by produceState<ImageBitmap?>(null, packageName) { value = load(packageName) }
    Box(Modifier.size(size).clip(CircleShape)) {
        icon?.let { Image(it, contentDescription = null, Modifier.size(size)) }
    }
}
