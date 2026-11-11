package com.shici.words.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shici.words.data.AppState
import com.shici.words.data.Books
import java.time.LocalDate

@Composable
fun StatsScreen() {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    AppState.revision
    val bookId = AppState.settings.book
    val meta = Books.meta(bookId)
    val words = remember(bookId) { Books.words(ctx, bookId) }
    val stats = AppState.bookStats(bookId, words.size)

    val activeDays = AppState.history.values.count { it.new + it.review > 0 }
    val totalLearned = AppState.progress.values.sumOf { it.size }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 16.dp)
    ) {
        Text(
            "统计",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)
        )

        // 打卡数据
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard("🔥", "${AppState.streak.cur}", "连续打卡", Modifier.weight(1f))
            StatCard("📅", "$activeDays", "累计天数", Modifier.weight(1f))
            StatCard("📚", "$totalLearned", "学习总词数", Modifier.weight(1f))
        }

        SectionTitle("学习日历")
        Surface(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(Modifier.padding(16.dp)) {
                Heatmap()
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Spacer(Modifier.weight(1f))
                    Text("少", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    listOf(0.12f, 0.35f, 0.6f, 0.85f, 1f).forEach { alpha ->
                        Box(
                            Modifier
                                .padding(horizontal = 2.dp)
                                .width(11.dp)
                                .height(11.dp)
                                .background(
                                    MaterialTheme.colorScheme.primary.copy(alpha = alpha),
                                    RoundedCornerShape(3.dp)
                                )
                        )
                    }
                    Text("多", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        SectionTitle("记忆状态 · ${meta.name}")
        Surface(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(Modifier.padding(16.dp)) {
                DistributionBar(stats)
                Spacer(Modifier.height(12.dp))
                LegendRow(
                    listOf(
                        "未学 ${stats.newCount}" to AppColors.gray(isDarkTheme()),
                        "复习中 ${stats.learning}" to MaterialTheme.colorScheme.primary,
                        "待复习 ${stats.due}" to AppColors.orange(isDarkTheme()),
                        "已掌握 ${stats.mastered}" to AppColors.green(isDarkTheme())
                    )
                )
            }
        }

        SectionTitle("近 7 天")
        Surface(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            WeekBars()
        }
    }
}

@Composable
private fun StatCard(emoji: String, value: String, label: String, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface) {
        Column(
            Modifier.fillMaxWidth().padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(emoji, fontSize = 20.sp)
            Text(
                value,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 4.dp)
            )
            Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp))
        }
    }
}

/** 近 15 周打卡热力图（列 = 周，行 = 周一..周日） */
@Composable
private fun Heatmap() {
    val today = LocalDate.now()
    val days = remember(today) {
        // 对齐周一，回溯 14 个完整周 + 本周
        val thisMonday = today.minusDays(today.dayOfWeek.value - 1L)
        val start = thisMonday.minusWeeks(14)
        (0..104).map { start.plusDays(it.toLong()) }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        for (week in 0..14) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                for (day in 0..6) {
                    val date = days[week * 7 + day]
                    val future = date.isAfter(today)
                    val count = if (future) -1
                    else AppState.history[date.toString()]?.let { it.new + it.review } ?: 0
                    val cellColor = when {
                        future || count == 0 -> MaterialTheme.colorScheme.surfaceVariant
                        count < 10 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                        count < 20 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        count < 40 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.75f)
                        else -> MaterialTheme.colorScheme.primary
                    }
                    Box(
                        Modifier
                            .width(13.dp)
                            .height(13.dp)
                            .background(cellColor, RoundedCornerShape(3.dp))
                    )
                }
            }
        }
    }
}

/** 近 7 天学习量柱状图 */
@Composable
private fun WeekBars() {
    val today = LocalDate.now()
    val data = remember(today) {
        (6 downTo 0).map { offset ->
            val d = today.minusDays(offset.toLong())
            val stat = AppState.history[d.toString()]
            d to ((stat?.new ?: 0) + (stat?.review ?: 0))
        }
    }
    val max = (data.maxOfOrNull { it.second } ?: 0).coerceAtLeast(1)
    Column(Modifier.padding(16.dp)) {
        Row(
            Modifier.fillMaxWidth().height(110.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            data.forEach { (date, count) ->
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    if (count > 0) {
                        Text("$count", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                    }
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height((count.toFloat() / max * 82).dp.coerceAtLeast(3.dp))
                            .background(
                                if (count > 0) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant,
                                RoundedCornerShape(6.dp, 6.dp, 2.dp, 2.dp)
                            )
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            data.forEach { (date, _) ->
                Text(
                    "${date.monthValue}/${date.dayOfMonth}",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}
