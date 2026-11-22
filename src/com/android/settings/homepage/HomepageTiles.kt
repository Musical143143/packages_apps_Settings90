/*
 * Copyright (C) 2024-2026 Lunaris AOSP
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.settings.homepage

import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.PorterDuff
import androidx.annotation.DrawableRes
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import com.android.settings.R
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenu
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke

internal val GRID_TILE_SPACING = 12.dp
internal val SEARCH_BAR_HEIGHT = 56.dp
internal val SEARCH_BAR_RADIUS = 28.dp
internal val SEARCH_BAR_HORIZONTAL_PADDING = 20.dp
internal val SEARCH_BAR_BOTTOM_PADDING = 20.dp

private val SMALL_TILE_HEIGHT = 78.dp
private val MEDIUM_TILE_HEIGHT = 132.dp
private val LARGE_TILE_HEIGHT = 156.dp

private val GROUP_DIVIDER_START_INSET = 72.dp
private const val ICON_BACKGROUND_ALPHA = 0.5f
private val CHEVRON_CONTAINER_PADDING = 4.dp

private val ROW_GAP_THICKNESS = 2.dp
private val SPLIT_CARD_GAP = 3.dp

internal val SEARCH_HINTS = listOf(
    "Are you searching something?",
    "Click here to search",
    "Lost? Tap to search",
    "What are we looking for today?",
    "Psst… search lives down here",
)

data class TileStyle(
    val cornerShape: Shape,
    val iconStyle: IconStyle,
    val showSummaries: Boolean,
    val dividerStyle: DividerStyle,
    val iconColor: Color? = null,
    val iconShape: IconShape = IconShape.CIRCLE,
    val iconPosition: IconPosition = IconPosition.START,
    val chevronStyle: ChevronStyle = ChevronStyle.PLAIN,
)

fun HomepageLayout.tileStyle(): TileStyle = TileStyle(
    cornerShape = cornerStyle.toShape(),
    iconStyle = iconStyle,
    showSummaries = showSummaries,
    dividerStyle = dividerStyle,
    iconColor = iconColor?.let { Color(it) },
    iconShape = iconShape,
    iconPosition = iconPosition,
    chevronStyle = chevronStyle,
)

internal fun TileSize.fixedHeight(): Dp? = when (this) {
    TileSize.SMALL -> SMALL_TILE_HEIGHT
    TileSize.MEDIUM -> MEDIUM_TILE_HEIGHT
    TileSize.LARGE -> LARGE_TILE_HEIGHT
    TileSize.ROW -> null
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun IconShape.containerShape(): Shape = when (this) {
    IconShape.CIRCLE -> CircleShape
    IconShape.ARROW -> MaterialShapes.Arrow.toShape()
    IconShape.PILL -> MaterialShapes.Pill.toShape()
    IconShape.TRIANGLE -> MaterialShapes.Triangle.toShape()
    IconShape.GEM -> MaterialShapes.Gem.toShape()
    IconShape.COOKIE_4 -> MaterialShapes.Cookie4Sided.toShape()
    IconShape.COOKIE_6 -> MaterialShapes.Cookie6Sided.toShape()
    IconShape.CLOVER_4 -> MaterialShapes.Clover4Leaf.toShape()
    IconShape.SQUARE -> MaterialShapes.Square.toShape()
    IconShape.DIAMOND -> MaterialShapes.Diamond.toShape()
    IconShape.PENTAGON -> MaterialShapes.Pentagon.toShape()
    IconShape.SUNNY -> MaterialShapes.Sunny.toShape()
    IconShape.COOKIE_9 -> MaterialShapes.Cookie9Sided.toShape()
    IconShape.CLOVER_8 -> MaterialShapes.Clover8Leaf.toShape()
    IconShape.ARCH -> MaterialShapes.Arch.toShape()
    IconShape.FAN -> MaterialShapes.Fan.toShape()
    IconShape.SLANTED -> MaterialShapes.Slanted.toShape()
    IconShape.OVAL -> MaterialShapes.Oval.toShape()
    IconShape.SEMI_CIRCLE -> MaterialShapes.SemiCircle.toShape()
    IconShape.CLAM_SHELL -> MaterialShapes.ClamShell.toShape()
    IconShape.FLOWER -> MaterialShapes.Flower.toShape()
    IconShape.GHOSTISH -> MaterialShapes.Ghostish.toShape()
    IconShape.PUFFY -> MaterialShapes.Puffy.toShape()
    IconShape.PUFFY_DIAMOND -> MaterialShapes.PuffyDiamond.toShape()
    IconShape.BUN -> MaterialShapes.Bun.toShape()
    IconShape.BOOM -> MaterialShapes.Boom.toShape()
    IconShape.SOFT_BOOM -> MaterialShapes.SoftBoom.toShape()
    IconShape.BURST -> MaterialShapes.Burst.toShape()
    IconShape.SOFT_BURST -> MaterialShapes.SoftBurst.toShape()
    IconShape.PILL_STAR -> MaterialShapes.Pill.toShape()
    IconShape.PIXEL_CIRCLE -> MaterialShapes.PixelCircle.toShape()
    IconShape.VERY_SUNNY -> MaterialShapes.VerySunny.toShape()
    IconShape.COOKIE_7 -> MaterialShapes.Cookie7Sided.toShape()
    IconShape.COOKIE_12 -> MaterialShapes.Cookie12Sided.toShape()
}

@Composable
private fun rememberDrawableBitmap(
    @DrawableRes resId: Int,
    size: Dp,
    tint: Color,
): ImageBitmap? {
    val context = LocalContext.current
    val sizePx = with(LocalDensity.current) { size.roundToPx() }
    val tintArgb = tint.toArgb()
    return remember(context, resId, sizePx, tintArgb) {
        if (sizePx <= 0) return@remember null
        val drawable = context.getDrawable(resId)?.mutate() ?: return@remember null
        drawable.alpha = 255
        drawable.setTintList(ColorStateList.valueOf(tintArgb))
        drawable.setTintMode(PorterDuff.Mode.SRC_IN)
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        drawable.setBounds(0, 0, sizePx, sizePx)
        drawable.draw(Canvas(bitmap))
        bitmap.asImageBitmap()
    }
}

internal fun Color.onColor(): Color = if (luminance() > 0.5f) Color.Black else Color.White

@Composable
internal fun HomepageIcon(
    icon: ImageVector?,
    @DrawableRes iconRes: Int? = null,
    style: IconStyle = IconStyle.ACCENT,
    tinted: Boolean = true,
    containerSize: Dp = 40.dp,
    glyphSize: Dp = 22.dp,
    color: Color? = null,
    shape: Shape = CircleShape,
) {
    val scheme = MaterialTheme.colorScheme
    val accent = color ?: scheme.primary
    val background = when {
        !tinted && style == IconStyle.FILLED -> color ?: scheme.surfaceContainerHighest
        style == IconStyle.ACCENT -> accent.copy(alpha = ICON_BACKGROUND_ALPHA)
        style == IconStyle.FILLED -> accent
        else -> Color.Transparent
    }
    val tint = when {
        !tinted -> Color.Unspecified
        style == IconStyle.FILLED -> if (color == null) scheme.onPrimary else color.onColor()
        style == IconStyle.PLAIN -> accent
        else -> scheme.onSurface
    }
    Box(
        modifier = Modifier
            .size(containerSize)
            .clip(shape)
            .background(background),
        contentAlignment = Alignment.Center,
    ) {
        val bitmap = if (iconRes != null && tinted) {
            rememberDrawableBitmap(iconRes, glyphSize, tint)
        } else {
            null
        }
        when {
            bitmap != null -> Icon(
                bitmap = bitmap,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(glyphSize),
            )

            iconRes != null -> Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(glyphSize + 8.dp),
            )

            icon != null -> Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(glyphSize),
            )
        }
    }
}

@Composable
internal fun HomepageTile(
    item: HomepageItem,
    size: TileSize,
    style: TileStyle,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    clickable: Boolean = true,
    checked: Boolean = false,
    onCheckedChange: (Boolean) -> Unit = {},
    searchHint: String = "",
    backdrop: ImageBitmap? = null,
) {
    val height = size.fixedHeight()
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .then(if (height != null) Modifier.height(height) else Modifier)
            .clip(style.cornerShape)
            .then(if (clickable) Modifier.clickable(onClick = onClick) else Modifier),
        shape = style.cornerShape,
        color = cardSurfaceColor(backdrop),
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Box {
            if (backdrop != null) CardBackdrop(backdrop)
            TileBody(
                item = item,
                size = size,
                style = style,
                checked = checked,
                onCheckedChange = onCheckedChange,
                interactive = clickable,
                searchHint = searchHint,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun TileBody(
    item: HomepageItem,
    size: TileSize,
    style: TileStyle,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    interactive: Boolean,
    searchHint: String,
) {
    if (item.kind == ItemKind.SEARCH_BAR) {
        SearchBarBody(size = size, hint = searchHint)
        return
    }
    val summary = item.summary?.takeIf { style.showSummaries }
    val isToggle = item.kind == ItemKind.TOGGLE
    val iconColor = item.iconColor ?: style.iconColor
    val iconShape = (item.iconShape ?: style.iconShape).containerShape()
    val iconAtEnd = (item.iconPosition ?: style.iconPosition) == IconPosition.END
    val chevron = style.chevronStyle

    val iconSlot: @Composable (Dp, Dp) -> Unit = { containerSize, glyphSize ->
        HomepageIcon(
            icon = item.icon,
            iconRes = item.iconRes,
            style = style.iconStyle,
            tinted = item.tintIcon,
            containerSize = containerSize,
            glyphSize = glyphSize,
            color = iconColor,
            shape = iconShape,
        )
    }

    when (size) {
        TileSize.ROW -> {
            val showChevron = chevron != ChevronStyle.HIDDEN
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (!iconAtEnd) {
                    iconSlot(40.dp, 22.dp)
                    Spacer(Modifier.width(16.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    TileTitle(item.title)
                    if (summary != null) {
                        TileSummary(
                            text = summary,
                            color = if (isToggle) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            maxLines = 2,
                        )
                    }
                }
                if (iconAtEnd) {
                    Spacer(Modifier.width(16.dp))
                    iconSlot(40.dp, 22.dp)
                }
                if (isToggle) {
                    Spacer(Modifier.width(8.dp))
                    HomepageSwitch(checked, onCheckedChange, interactive)
                } else if (showChevron) {
                    Spacer(Modifier.width(8.dp))
                    Chevron(chevron)
                }
            }
        }

        TileSize.SMALL -> Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 12.dp, end = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (!iconAtEnd) {
                iconSlot(40.dp, 22.dp)
                Spacer(Modifier.width(10.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                TileTitle(item.title)
                if (summary != null) TileSummary(summary, small = true)
            }
            if (iconAtEnd) {
                Spacer(Modifier.width(10.dp))
                iconSlot(40.dp, 22.dp)
            }
            if (isToggle) {
                HomepageSwitch(checked, onCheckedChange, interactive, compact = true)
            }
        }

        TileSize.MEDIUM, TileSize.LARGE -> {
            val large = size == TileSize.LARGE
            val showChevron = large && chevron != ChevronStyle.HIDDEN
            val container = if (large) 52.dp else 44.dp
            val glyph = if (large) 28.dp else 24.dp
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = if (large) 20.dp else 14.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (iconAtEnd) {
                        if (isToggle) {
                            HomepageSwitch(checked, onCheckedChange, interactive, compact = !large)
                        } else if (showChevron) {
                            Chevron(chevron)
                        }
                        Spacer(Modifier.weight(1f))
                        iconSlot(container, glyph)
                    } else {
                        iconSlot(container, glyph)
                        Spacer(Modifier.weight(1f))
                        if (isToggle) {
                            HomepageSwitch(checked, onCheckedChange, interactive, compact = !large)
                        } else if (showChevron) {
                            Chevron(chevron)
                        }
                    }
                }
                Column {
                    TileTitle(item.title, large = large)
                    if (summary != null) {
                        TileSummary(summary, small = !large, maxLines = if (large) 2 else 2)
                    }
                }
            }
        }
    }
}

@Composable
internal fun HomepageCard(
    entries: List<HomepageItem>,
    style: TileStyle,
    onEntryClick: (HomepageItem) -> Unit,
    checkedOf: (HomepageItem) -> Boolean,
    onToggle: (HomepageItem, Boolean) -> Unit,
    searchHint: String,
    modifier: Modifier = Modifier,
    backdrop: ImageBitmap? = null,
) {
    if (style.dividerStyle == DividerStyle.SPLIT) {
        Column(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(SPLIT_CARD_GAP),
        ) {
            entries.forEach { item ->
                HomepageTile(
                    item = item,
                    size = TileSize.ROW,
                    style = style,
                    onClick = { onEntryClick(item) },
                    checked = checkedOf(item),
                    onCheckedChange = { onToggle(item, it) },
                    searchHint = searchHint,
                    backdrop = backdrop,
                )
            }
        }
        return
    }
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = style.cornerShape,
        color = cardSurfaceColor(backdrop),
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Box {
            if (backdrop != null) CardBackdrop(backdrop)
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                entries.forEachIndexed { index, item ->
                    Box(modifier = Modifier.clickable { onEntryClick(item) }) {
                        TileBody(
                            item = item,
                            size = TileSize.ROW,
                            style = style,
                            checked = checkedOf(item),
                            onCheckedChange = { onToggle(item, it) },
                            interactive = true,
                            searchHint = searchHint,
                        )
                    }
                    if (index < entries.lastIndex) {
                        HomepageDivider(style.dividerStyle, style.iconPosition)
                    }
                }
            }
        }
    }
}

/** Full-width canvas row used by the drawn divider styles. */
@Composable
private fun DividerCanvas(draw: DrawScope.() -> Unit) {
    Canvas(Modifier.fillMaxWidth().height(12.dp)) {
        // Guard against unbounded or zero width: the drawn styles loop across the width.
        if (size.width.isFinite() && size.width in 1f..100_000f) draw()
    }
}

@Composable
internal fun HomepageDivider(
    style: DividerStyle,
    iconPosition: IconPosition = IconPosition.START,
) {
    val scheme = MaterialTheme.colorScheme
    when (style) {
        DividerStyle.NONE -> Unit

        DividerStyle.GAP -> HorizontalDivider(
            thickness = ROW_GAP_THICKNESS,
            color = scheme.surfaceContainer,
        )

        DividerStyle.INSET -> HorizontalDivider(
            modifier = if (iconPosition == IconPosition.END) {
                Modifier.padding(start = 16.dp, end = GROUP_DIVIDER_START_INSET)
            } else {
                Modifier.padding(start = GROUP_DIVIDER_START_INSET, end = 16.dp)
            },
            color = scheme.outlineVariant.copy(alpha = 0.4f),
        )

        DividerStyle.FULL -> HorizontalDivider(
            color = scheme.outlineVariant.copy(alpha = 0.4f),
        )

        DividerStyle.THICK -> HorizontalDivider(
            thickness = 2.dp,
            color = scheme.outlineVariant.copy(alpha = 0.8f),
        )

        DividerStyle.DOUBLE -> Column {
            HorizontalDivider(color = scheme.outlineVariant.copy(alpha = 0.4f))
            Spacer(Modifier.height(3.dp))
            HorizontalDivider(color = scheme.outlineVariant.copy(alpha = 0.4f))
        }

        DividerStyle.SPLIT -> Unit

        DividerStyle.DOTTED -> DividerCanvas {
            val y = size.height / 2f
            drawLine(
                color = scheme.outlineVariant.copy(alpha = 0.8f),
                start = Offset(0f, y), end = Offset(size.width, y),
                strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(0f, 9.dp.toPx()), 0f),
            )
        }

        DividerStyle.DASHED -> DividerCanvas {
            val y = size.height / 2f
            drawLine(
                color = scheme.outlineVariant.copy(alpha = 0.8f),
                start = Offset(0f, y), end = Offset(size.width, y),
                strokeWidth = 1.5f.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12.dp.toPx(), 8.dp.toPx()), 0f),
            )
        }

        DividerStyle.LONG_DASH -> DividerCanvas {
            val y = size.height / 2f
            drawLine(
                color = scheme.outlineVariant.copy(alpha = 0.8f),
                start = Offset(0f, y), end = Offset(size.width, y),
                strokeWidth = 2.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(28.dp.toPx(), 10.dp.toPx()), 0f),
            )
        }

        DividerStyle.DOT_DASH -> DividerCanvas {
            val y = size.height / 2f
            drawLine(
                color = scheme.outlineVariant.copy(alpha = 0.8f),
                start = Offset(0f, y), end = Offset(size.width, y),
                strokeWidth = 1.5f.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(
                    floatArrayOf(10.dp.toPx(), 6.dp.toPx(), 2.dp.toPx(), 6.dp.toPx()), 0f,
                ),
            )
        }

        DividerStyle.WAVY -> DividerCanvas {
            val y = size.height / 2f
            val step = 12.dp.toPx()
            val amp = 3.dp.toPx()
            val path = Path().apply {
                moveTo(0f, y)
                var x = 0f
                var up = true
                while (x < size.width) {
                    val next = x + step
                    quadraticTo(x + step / 2f, if (up) y - amp else y + amp, next, y)
                    x = next
                    up = !up
                }
            }
            drawPath(path, scheme.outlineVariant.copy(alpha = 0.8f), style = Stroke(width = 2.dp.toPx()))
        }

        DividerStyle.ZIGZAG -> DividerCanvas {
            val y = size.height / 2f
            val step = 8.dp.toPx()
            val amp = 4.dp.toPx()
            val path = Path().apply {
                moveTo(0f, y - amp)
                var x = 0f
                var up = false
                while (x < size.width) {
                    x += step
                    lineTo(x, if (up) y - amp else y + amp)
                    up = !up
                }
            }
            drawPath(
                path, scheme.outlineVariant.copy(alpha = 0.8f),
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
        }

        DividerStyle.TRIPLE -> DividerCanvas {
            val y = size.height / 2f
            val gap = 3.dp.toPx()
            val color = scheme.outlineVariant.copy(alpha = 0.6f)
            listOf(-gap, 0f, gap).forEach { dy ->
                drawLine(color, Offset(0f, y + dy), Offset(size.width, y + dy), strokeWidth = 1.dp.toPx())
            }
        }

        DividerStyle.FADE_EDGES -> DividerCanvas {
            val y = size.height / 2f
            drawRect(
                brush = Brush.horizontalGradient(
                    0f to Color.Transparent,
                    0.5f to scheme.outlineVariant.copy(alpha = 0.6f),
                    1f to Color.Transparent,
                ),
                topLeft = Offset(0f, y - 1.dp.toPx()),
                size = Size(size.width, 2.dp.toPx()),
            )
        }

        DividerStyle.CENTER_GLOW -> DividerCanvas {
            val y = size.height / 2f
            drawRect(
                brush = Brush.horizontalGradient(
                    0f to Color.Transparent,
                    0.5f to scheme.primary,
                    1f to Color.Transparent,
                ),
                topLeft = Offset(0f, y - 1.5f.dp.toPx()),
                size = Size(size.width, 3.dp.toPx()),
            )
        }

        DividerStyle.PRIMARY_LINE -> HorizontalDivider(
            thickness = 2.dp,
            color = scheme.primary,
        )

        DividerStyle.TERTIARY_LINE -> HorizontalDivider(
            thickness = 2.dp,
            color = scheme.tertiary.copy(alpha = 0.7f),
        )

        DividerStyle.GROOVE -> Column {
            HorizontalDivider(thickness = 1.dp, color = Color.Black.copy(alpha = 0.25f))
            HorizontalDivider(thickness = 1.dp, color = scheme.surfaceBright)
        }

        DividerStyle.RAISED -> Column {
            HorizontalDivider(thickness = 1.dp, color = scheme.surfaceBright)
            HorizontalDivider(thickness = 1.dp, color = Color.Black.copy(alpha = 0.25f))
        }

        DividerStyle.CENTER_DIAMOND -> DividerCanvas {
            val y = size.height / 2f
            val cx = size.width / 2f
            val r = 4.dp.toPx()
            drawLine(
                scheme.outlineVariant.copy(alpha = 0.6f),
                Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx(),
            )
            val diamond = Path().apply {
                moveTo(cx, y - r)
                lineTo(cx + r, y)
                lineTo(cx, y + r)
                lineTo(cx - r, y)
                close()
            }
            drawPath(diamond, scheme.primary)
        }

        DividerStyle.ROUNDED_BAR -> DividerCanvas {
            val y = size.height / 2f
            drawRoundRect(
                color = scheme.outlineVariant.copy(alpha = 0.5f),
                topLeft = Offset(0f, y - 2.dp.toPx()),
                size = Size(size.width, 4.dp.toPx()),
                cornerRadius = CornerRadius(2.dp.toPx()),
            )
        }

        DividerStyle.SEGMENTED -> DividerCanvas {
            val y = size.height / 2f
            val seg = 24.dp.toPx()
            val gap = 8.dp.toPx()
            var x = 0f
            while (x < size.width) {
                drawRect(
                    color = scheme.outlineVariant.copy(alpha = 0.7f),
                    topLeft = Offset(x, y - 1.dp.toPx()),
                    size = Size(minOf(seg, size.width - x), 2.dp.toPx()),
                )
                x += seg + gap
            }
        }

        DividerStyle.LENS -> DividerCanvas {
            val y = size.height / 2f
            val bulge = 3.dp.toPx()
            val lens = Path().apply {
                moveTo(0f, y)
                quadraticTo(size.width / 2f, y - bulge, size.width, y)
                quadraticTo(size.width / 2f, y + bulge, 0f, y)
                close()
            }
            drawPath(lens, scheme.primary.copy(alpha = 0.7f))
        }

        DividerStyle.BEADS -> DividerCanvas {
            val y = size.height / 2f
            val spacing = 12.dp.toPx()
            val r = 2.dp.toPx()
            var x = r
            while (x < size.width - r) {
                drawCircle(
                    color = scheme.outlineVariant.copy(alpha = 0.8f),
                    radius = r,
                    center = Offset(x, y),
                )
                x += spacing
            }
        }

        DividerStyle.DOUBLE_DASH -> DividerCanvas {
            val dash = PathEffect.dashPathEffect(floatArrayOf(10.dp.toPx(), 6.dp.toPx()), 0f)
            val color = scheme.outlineVariant.copy(alpha = 0.7f)
            val y = size.height / 2f
            listOf(-2.dp.toPx(), 2.dp.toPx()).forEach { dy ->
                drawLine(color, Offset(0f, y + dy), Offset(size.width, y + dy),
                    strokeWidth = 1.dp.toPx(), pathEffect = dash)
            }
        }

        DividerStyle.TICKS -> DividerCanvas {
            val y = size.height / 2f
            val color = scheme.outlineVariant.copy(alpha = 0.7f)
            drawLine(color, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
            val spacing = 16.dp.toPx()
            var x = 0f
            while (x <= size.width) {
                drawLine(color, Offset(x, y - 4.dp.toPx()), Offset(x, y + 4.dp.toPx()), strokeWidth = 1.dp.toPx())
                x += spacing
            }
        }
        else -> HorizontalDivider(
            thickness = style.thickness,
            color = scheme.outlineVariant.copy(alpha = style.alpha),
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun TileTitle(text: String, large: Boolean = false) {
    Text(
        text = text,
        style = if (large) {
            MaterialTheme.typography.titleLargeEmphasized
        } else {
            MaterialTheme.typography.titleMediumEmphasized
        },
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = if (large) 2 else 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun TileSummary(
    text: String,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    small: Boolean = false,
    maxLines: Int = 1,
) {
    Text(
        text = text,
        style = if (small) {
            MaterialTheme.typography.bodySmall
        } else {
            MaterialTheme.typography.bodyMedium
        },
        color = color,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun Chevron(style: ChevronStyle, size: Dp = 24.dp) {
    val glyph: @Composable () -> Unit = {
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(size),
        )
    }
    val corner = style.corner
    if (corner != null) {
        Box(
            modifier = Modifier
                .size(size + CHEVRON_CONTAINER_PADDING * 2)
                .clip(RoundedCornerShape(corner))
                .then(
                    if (style.outlined) {
                        Modifier.border(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant,
                            RoundedCornerShape(corner),
                        )
                    } else {
                        Modifier.background(MaterialTheme.colorScheme.surfaceContainerHighest)
                    }
                ),
            contentAlignment = Alignment.Center,
        ) {
            glyph()
        }
    } else {
        glyph()
    }
}

@Composable
internal fun HomepageSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    interactive: Boolean = true,
    compact: Boolean = false,
) {
    Switch(
        checked = checked,
        onCheckedChange = if (interactive) onCheckedChange else null,
        modifier = if (compact) Modifier.scale(0.75f) else Modifier,
        thumbContent = {
            Crossfade(targetState = checked, label = "homepage_switch_icon") { isChecked ->
                Icon(
                    imageVector = if (isChecked) Icons.Filled.Check else Icons.Filled.Close,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
            }
        },
        colors = SwitchDefaults.colors(
            checkedThumbColor = MaterialTheme.colorScheme.primary,
            checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
            checkedIconColor = MaterialTheme.colorScheme.onPrimary,
            uncheckedThumbColor = MaterialTheme.colorScheme.outline,
            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
            uncheckedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    )
}

@Composable
private fun SearchBarBody(size: TileSize, hint: String) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = if (size.width == TileWidth.FULL) 20.dp else 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (size.width == TileWidth.FULL) {
            Arrangement.Start
        } else {
            Arrangement.Center
        },
    ) {
        Icon(
            imageVector = Icons.Outlined.Search,
            contentDescription = if (size.width == TileWidth.FULL) null else "Search",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp),
        )
        if (size.width == TileWidth.FULL) {
            Spacer(modifier = Modifier.width(16.dp))
            RgbHintText(text = hint)
        }
    }
}

/**
 * Search hint that cycles through the hue wheel. Uses a slightly deeper,
 * darker-value palette on light surfaces and a brighter, softer one on dark
 * surfaces so the color stays readable in both modes.
 */
@Composable
private fun RgbHintText(text: String, modifier: Modifier = Modifier) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val transition = rememberInfiniteTransition(label = "rgbHint")
    val hue by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "rgbHue",
    )
    Text(
        text = text,
        color = Color.hsv(
            hue = hue,
            saturation = if (isDark) 0.45f else 0.85f,
            value = if (isDark) 1f else 0.7f,
        ),
        style = MaterialTheme.typography.bodyLarge,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun PinnedSearchBar(
    hint: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    elevated: Boolean = true,
    glass: HomepageGlassBackdrop? = null,
    onLongEdit: (() -> Unit)? = null,
) {
    val shape = RoundedCornerShape(SEARCH_BAR_RADIUS)
    var menuOpen by remember { mutableStateOf(false) }
    val canEdit = onLongEdit != null
    val openMenu: () -> Unit = { menuOpen = true }
    Box(modifier = modifier) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(SEARCH_BAR_HEIGHT)
            .then(
                if (glass != null) {
                    Modifier.homepageGlass(
                        backdrop = glass,
                        shape = shape,
                        tint = MaterialTheme.colorScheme.surfaceBright.copy(alpha = 0.6f),
                        highlight = MaterialTheme.colorScheme.surfaceBright.copy(alpha = 0.5f),
                    )
                } else {
                    Modifier
                }
            )
            .clip(shape)
            .combinedClickable(
                onClick = onClick,
                onLongClick = if (canEdit) openMenu else null,
            ),
        shape = shape,
        color = if (glass != null) Color.Transparent else MaterialTheme.colorScheme.surfaceBright,
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f),
        ),
        shadowElevation = if (elevated) 12.dp else 0.dp,
        tonalElevation = if (elevated && glass == null) 6.dp else 0.dp,
    ) {
        SearchBarBody(size = TileSize.ROW, hint = hint)
    }
    val scheme = MaterialTheme.colorScheme
    val menuIsDark = scheme.surface.luminance() < 0.5f
    DropdownMenu(
        expanded = menuOpen && canEdit,
        onDismissRequest = { menuOpen = false },
        shape = RoundedCornerShape(28.dp),
        containerColor = if (menuIsDark) {
            scheme.surfaceContainerHigh.copy(alpha = 0.88f)
        } else {
            Color.White.copy(alpha = 0.86f)
        },
        tonalElevation = 0.dp,
        shadowElevation = 10.dp,
        border = BorderStroke(
            1.dp,
            if (menuIsDark) Color.White.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.8f),
        ),
    ) {
        DropdownMenuItem(
            text = {
                Text(
                    text = stringResource(R.string.homepage_edit_action),
                    style = MaterialTheme.typography.titleMedium,
                    color = scheme.onSurface,
                )
            },
            leadingIcon = {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(scheme.primary.copy(alpha = 0.16f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = null,
                        tint = scheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            },
            onClick = {
                menuOpen = false
                onLongEdit?.invoke()
            },
        )
    }
    }
}

@Composable
internal fun InlineSearchBarTile(
    size: TileSize,
    hint: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    clickable: Boolean = true,
    backdrop: ImageBitmap? = null,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(size.fixedHeight() ?: SEARCH_BAR_HEIGHT)
            .clip(RoundedCornerShape(SEARCH_BAR_RADIUS))
            .then(if (clickable) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(SEARCH_BAR_RADIUS),
        color = cardSurfaceColor(backdrop),
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f),
        ),
    ) {
        Box {
            if (backdrop != null) CardBackdrop(backdrop)
            SearchBarBody(size = size, hint = hint)
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
/** Greeting templates for the random greeting mode; %s is the user's name. */
internal val HOMEPAGE_GREETINGS = listOf(
    "Hi, %s",
    "Welcome back, %s",
    "Good to see you, %s",
    "Have a great day, %s",
    "Hey %s, ready?",
    "Nice to see you, %s",
    "Let's make it count, %s",
    "Hello again, %s",
    "Good morning, %s",
    "Good afternoon, %s",
    "Good evening, %s",
    "Morning, %s, let's go",
    "Afternoon, %s",
    "Evening, %s",
    "Hey there, %s",
    "Hello, %s",
    "Greetings, %s",
    "Howdy, %s",
    "Yo %s, what's up",
    "Hi there, %s",
    "Welcome, %s",
    "Welcome home, %s",
    "Welcome in, %s",
    "Good to have you, %s",
    "Glad you're here, %s",
    "So good to see you, %s",
    "Nice to have you back, %s",
    "Hope you're well, %s",
    "Hope your day is going great, %s",
    "Have a wonderful day, %s",
    "Have a lovely day, %s",
    "Have a productive day, %s",
    "Have a relaxing day, %s",
    "Have a happy day, %s",
    "Enjoy your day, %s",
    "Make today great, %s",
    "Make today amazing, %s",
    "Make today yours, %s",
    "Make it a good one, %s",
    "Let's do this, %s",
    "Let's get started, %s",
    "Ready to roll, %s?",
    "Ready when you are, %s",
    "What's the plan, %s?",
    "What's on your mind, %s?",
    "Time to shine, %s",
    "Shine bright, %s",
    "Keep going, %s",
    "You've got this, %s",
    "Believe in yourself, %s",
    "Stay positive, %s",
    "Think positive, %s",
    "Stay curious, %s",
    "Keep it simple, %s",
    "One step at a time, %s",
    "Small steps count, %s",
    "Progress, not perfection, %s",
    "Be kind to yourself, %s",
    "Take a breath, %s",
    "Stay calm, %s",
    "Stay focused, %s",
    "Stay strong, %s",
    "Dream big, %s",
    "Chase your goals, %s",
    "Your time is now, %s",
    "Every day is a fresh start, %s",
    "Today is a new page, %s",
    "Good vibes only, %s",
    "Positive energy, %s",
    "Smile, %s",
    "Smile more, %s",
    "Happy to see you, %s",
    "Always a pleasure, %s",
    "Looking good, %s",
    "Looking sharp, %s",
    "Welcome to your phone, %s",
    "Your phone, your rules, %s",
    "Hey %s, let's customize",
    "Let's make it yours, %s",
    "Your space, %s",
    "Your home screen, %s",
    "Ready for a great day, %s",
    "Here's to a great day, %s",
    "Cheers, %s",
    "Hooray, %s, you're here",
    "Nice day for it, %s",
    "Sunshine and good things, %s",
    "Good luck today, %s",
    "Good luck, %s",
    "All the best, %s",
    "Wishing you a great day, %s",
    "Wishing you peace, %s",
    "Wishing you joy, %s",
    "Wishing you success, %s",
    "Sending good vibes, %s",
    "You're doing great, %s",
    "Proud of you, %s",
    "Keep shining, %s",
    "Be your best self, %s",
    "Be happy, %s",
)

/** Fills a greeting template: %s becomes the name, or is removed when there is no name. */
internal fun fillGreeting(template: String, name: String?): String =
    template.replace("%s", name ?: "").replace(" ,", ",").trim()

@Composable
internal fun HomepageHeader(
    config: HeaderConfig,
    title: String,
    greeting: String?,
    avatarBitmap: Bitmap?,
    onAvatarClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val compact = config.size == HeaderSize.COMPACT
    if (!config.showHeader) {
        Spacer(modifier.height(8.dp))
        return
    }
    if (!config.showTitle && !config.showAvatar && greeting == null) {
        Spacer(modifier.height(if (compact) 16.dp else 32.dp))
        return
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = 12.dp,
                end = 12.dp,
                top = if (compact) 24.dp else 74.dp,
                bottom = if (compact) 4.dp else 12.dp,
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.Start,
        ) {
            if (greeting != null) {
                Text(
                    text = greeting,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Start,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(2.dp))
            }
            if (config.showTitle) {
                Text(
                    text = title,
                    style = if (compact) {
                        MaterialTheme.typography.headlineMediumEmphasized
                    } else {
                        MaterialTheme.typography.displayMediumEmphasized
                    },
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Start,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        if (config.showAvatar) {
            Spacer(Modifier.width(12.dp))
            val avatarSize = if (compact) 44.dp else 52.dp
            Box(
                modifier = Modifier
                    .size(avatarSize)
                    .clip(config.avatarShape.containerShape())
                    .background(MaterialTheme.colorScheme.surfaceBright)
                    .clickable(onClick = onAvatarClick),
                contentAlignment = Alignment.Center,
            ) {
                if (avatarBitmap != null) {
                    Image(
                        bitmap = avatarBitmap.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Icon(
                        imageVector = Icons.Outlined.AccountCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(26.dp),
                    )
                }
            }
        }
    }
}
