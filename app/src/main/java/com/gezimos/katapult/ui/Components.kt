package com.gezimos.katapult.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gezimos.katapult.R

val LatoFamily = FontFamily(
    Font(R.font.lato_regular, FontWeight.Normal),
    Font(R.font.lato_bold, FontWeight.Bold),
)

val IconSize = 72.dp
val PagePadding = 8.dp
val ArrowSize = 40.dp

val EinkTintMatrix = ColorMatrix(floatArrayOf(
    0f, 0f, 0f, 0f, 0f,
    0f, 0f, 0f, 0f, 0f,
    0f, 0f, 0f, 0f, 0f,
    -0.2126f * 1.5f, -0.7152f * 1.5f, -0.0722f * 1.5f,
    2f, 255f * 1.5f - 2f * 255f
))
val EinkColorFilter = ColorFilter.colorMatrix(EinkTintMatrix)

val RoundedIconShape = RoundedCornerShape(19.dp)
val RoundedSmallShape = RoundedCornerShape(10.dp)
val RoundedBadgeShape = RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp, bottomEnd = 0.dp, bottomStart = 8.dp)

fun Modifier.dashedDotBorder(strokeWidth: Dp = 2.dp, isRounded: Boolean, outset: Dp = 0.dp, cornerRadius: Dp = 19.dp): Modifier = this.drawBehind {
    val sw = strokeWidth.toPx()
    val out = outset.toPx()
    val dotRadius = sw * 0.7f
    val targetSpacing = sw * 5f
    val drawWidth = size.width + 2 * out
    val drawHeight = size.height + 2 * out

    if (isRounded) {
        val r = cornerRadius.toPx() + out
        val w = drawWidth - sw
        val h = drawHeight - sw
        val cr = r.coerceAtMost(w / 2).coerceAtMost(h / 2)
        val straightH = w - 2 * cr
        val straightV = h - 2 * cr
        val cornerArc = (Math.PI * cr / 2).toFloat()
        val perimeter = 2 * straightH + 2 * straightV + 4 * cornerArc
        val dotCount = kotlin.math.round(perimeter / targetSpacing).toInt().coerceAtLeast(1)
        val spacing = perimeter / dotCount

        data class Segment(val length: Float, val point: (Float) -> Offset)
        val left = -out + sw / 2
        val top = -out + sw / 2
        val right = size.width + out - sw / 2
        val bottom = size.height + out - sw / 2
        val segments = listOf(
            Segment(straightH) { t -> Offset(left + cr + t, top) },
            Segment(cornerArc) { t ->
                val a = t / cr
                Offset(right - cr + cr * kotlin.math.sin(a), top + cr - cr * kotlin.math.cos(a))
            },
            Segment(straightV) { t -> Offset(right, top + cr + t) },
            Segment(cornerArc) { t ->
                val a = t / cr
                Offset(right - cr + cr * kotlin.math.cos(a), bottom - cr + cr * kotlin.math.sin(a))
            },
            Segment(straightH) { t -> Offset(right - cr - t, bottom) },
            Segment(cornerArc) { t ->
                val a = t / cr
                Offset(left + cr - cr * kotlin.math.sin(a), bottom - cr + cr * kotlin.math.cos(a))
            },
            Segment(straightV) { t -> Offset(left, bottom - cr - t) },
            Segment(cornerArc) { t ->
                val a = t / cr
                Offset(left + cr - cr * kotlin.math.cos(a), top + cr - cr * kotlin.math.sin(a))
            },
        )
        for (i in 0 until dotCount) {
            var dist = i * spacing
            for (seg in segments) {
                if (dist <= seg.length) {
                    drawCircle(Color.Black, dotRadius, seg.point(dist))
                    break
                }
                dist -= seg.length
            }
        }
    } else {
        val perimeter = (Math.PI * drawWidth).toFloat()
        val dotCount = kotlin.math.round(perimeter / targetSpacing).toInt().coerceAtLeast(1)
        val cx = size.width / 2
        val cy = size.height / 2
        val radius = drawWidth / 2 - sw / 2
        for (i in 0 until dotCount) {
            val angle = (2.0 * Math.PI * i / dotCount).toFloat()
            val x = cx + radius * kotlin.math.cos(angle)
            val y = cy + radius * kotlin.math.sin(angle)
            drawCircle(Color.Black, dotRadius, Offset(x, y))
        }
    }
}
val LocalIconShape = compositionLocalOf<Shape> { CircleShape }
val LocalSmallIconShape = compositionLocalOf<Shape> { CircleShape }

@Composable
fun AppIconCircle(bitmap: Bitmap?, size: Dp, borderWidth: Dp = 2.5.dp, shape: Shape = LocalIconShape.current) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(shape)
            .background(Color.White),
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                colorFilter = EinkColorFilter,
            )
        }
        Box(
            Modifier
                .fillMaxSize()
                .border(borderWidth, Color.Black, shape)
        )
    }
}

@Composable
fun ArrowButton(iconRes: Int, onClick: () -> Unit) {
    val shape = LocalSmallIconShape.current
    Box(
        modifier = Modifier
            .size(ArrowSize)
            .border(2.5.dp, Color.Black, shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        val context = LocalContext.current
        val drawable = remember(iconRes) { context.getDrawable(iconRes) }
        if (drawable != null) {
            val bmp = remember(iconRes) {
                val b = android.graphics.Bitmap.createBitmap(48, 48, android.graphics.Bitmap.Config.ARGB_8888)
                val c = android.graphics.Canvas(b)
                drawable.setBounds(0, 0, 48, 48)
                drawable.draw(c)
                b
            }
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                contentScale = ContentScale.Fit,
            )
        }
    }
}

@Composable
fun CircleDot(size: Dp, borderWidth: Dp = 1.5.dp) {
    val shape = LocalIconShape.current
    Box(
        Modifier
            .size(size)
            .border(borderWidth, Color.Black, shape)
    )
}

val LocalBadgeShape = compositionLocalOf<Shape> { CircleShape }

@Composable
fun NotificationBadge(count: Int, modifier: Modifier = Modifier) {
    val shape = LocalBadgeShape.current
    Box(
        modifier = modifier
            .size(26.dp)
            .background(Color.White, shape)
            .border(2.5.dp, Color.Black, shape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (count > 99) stringResource(R.string.badge_overflow) else count.toString(),
            color = Color.Black,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = LatoFamily,
        )
    }
}

@Composable
fun BottomSheetOption(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            androidx.compose.material3.Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.Black,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.width(12.dp))
        }
        Text(
            text = text,
            fontSize = 18.sp,
            fontFamily = LatoFamily,
            color = Color.Black,
        )
    }
}
