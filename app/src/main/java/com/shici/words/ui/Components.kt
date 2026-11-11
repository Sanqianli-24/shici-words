package com.shici.words.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shici.words.core.WordStatus

/** 环形进度条 */
@Composable
fun ProgressRing(
    percent: Float,
    modifier: Modifier = Modifier,
    size: Dp = 116.dp,
    stroke: Dp = 10.dp,
    center: @Composable () -> Unit = {}
) {
    val animated by animateFloatAsState(
        targetValue = percent.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 600),
        label = "ring"
    )
    val primary = MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.surfaceVariant
    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            val s = stroke.toPx()
            val r = (this.size.minDimension - s) / 2
            drawArc(
                color = track,
                startAngle = -90f, sweepAngle = 360f, useCenter = false,
                style = Stroke(s, cap = StrokeCap.Round)
            )
            if (animated > 0f) {
                drawArc(
                    color = primary,
                    startAngle = -90f, sweepAngle = 360f * animated, useCenter = false,
                    style = Stroke(s, cap = StrokeCap.Round)
                )
            }
        }
        center()
    }
}

/** 学习状态徽章 */
@Composable
fun StatusBadge(status: WordStatus, modifier: Modifier = Modifier, compact: Boolean = false) {
    val dark = MaterialTheme.colorScheme.background == Color(0xFF12141C)
    val (text, color) = when (status) {
        WordStatus.NEW -> "未学" to AppColors.gray(dark)
        WordStatus.LEARNING -> "复习中" to MaterialTheme.colorScheme.primary
        WordStatus.DUE -> "待复习" to AppColors.orange(dark)
        WordStatus.MASTERED -> "已掌握" to AppColors.green(dark)
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.13f))
            .padding(horizontal = if (compact) 8.dp else 10.dp, vertical = 3.dp)
    ) {
        Text(
            text,
            color = color,
            fontSize = if (compact) 10.sp else 11.sp,
            style = MaterialTheme.typography.labelMedium
        )
    }
}

/** 区块标题 */
@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier = modifier.padding(horizontal = 20.dp, vertical = 8.dp),
        style = MaterialTheme.typography.titleMedium,
        fontSize = 16.sp
    )
}

/** 空状态占位 */
@Composable
fun EmptyHint(text: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("🍃", fontSize = 34.sp)
        Text(
            text,
            modifier = Modifier.padding(top = 10.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
    }
}

/** 自动消失的提示文本 */
@Composable
fun AutoHint(text: String, onTimeout: () -> Unit) {
    Text(
        text,
        modifier = Modifier.padding(vertical = 6.dp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 13.sp
    )
    LaunchedEffect(text) {
        kotlinx.coroutines.delay(1400)
        onTimeout()
    }
}

/** 图例圆点 */
@Composable
fun LegendDot(color: Color) {
    Box(
        Modifier
            .size(8.dp)
            .clip(RoundedCornerShape(50))
            .background(color)
    )
}

@Composable
fun LegendRow(items: List<Pair<String, Color>>, modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        items.forEach { (label, color) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                LegendDot(color)
                Text(
                    label,
                    modifier = Modifier.padding(start = 4.dp),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
