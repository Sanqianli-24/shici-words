package com.shici.words.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shici.words.UiState
import com.shici.words.core.Srs
import com.shici.words.data.AppState
import com.shici.words.data.Books
import com.shici.words.data.BookStats
import java.time.LocalDate
import java.time.LocalTime

@Composable
fun HomeScreen() {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    AppState.revision // 数据变化时重组
    val bookId = AppState.settings.book
    val meta = Books.meta(bookId)
    val words = remember(bookId) { Books.words(ctx, bookId) }
    val stats = AppState.bookStats(bookId, words.size)
    val today = AppState.todayStat()
    val plan = Srs.buildSession(
        AppState.cards(bookId), words.size,
        AppState.settings.dailyNew, today.new, AppState.todayNum()
    )
    val doneToday = today.new + today.review
    val plannedTotal = doneToday + plan.items.size
    val ringPct = if (plannedTotal > 0) doneToday.toFloat() / plannedTotal else 1f
    val allDone = plan.items.isEmpty()

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 16.dp)
    ) {
        // 顶部问候
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(greeting(), style = MaterialTheme.typography.titleLarge)
                Text(
                    dateText(),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.surface) {
                Text("🔥 连续 ${AppState.streak.cur} 天", Modifier.padding(horizontal = 12.dp, vertical = 6.dp), fontSize = 13.sp)
            }
        }

        // 今日任务卡
        Surface(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ProgressRing(percent = ringPct) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("$doneToday", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text(
                                "/ $plannedTotal",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(Modifier.width(20.dp))
                    Column {
                        Text(meta.name, style = MaterialTheme.typography.titleMedium)
                        if (!allDone) {
                            Text(
                                "今日任务：复习 ${plan.dueCount} · 新词 ${plan.newCount}",
                                fontSize = 14.sp,
                                modifier = Modifier.padding(top = 6.dp)
                            )
                            Text(
                                "预计用时 ${((plan.items.size * 6 + 59) / 60)} 分钟",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        } else {
                            Text(
                                if (stats.newCount == 0) "这本词书已全部学完 🎉" else "今日任务已完成 🎉",
                                fontSize = 14.sp,
                                modifier = Modifier.padding(top = 6.dp)
                            )
                            Text(
                                "明天约复习 ${AppState.tomorrowDueCount(bookId)} 个单词",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(18.dp))
                if (allDone) {
                    OutlinedButton(
                        onClick = { /* 切到统计页由用户手动 */ },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        enabled = false
                    ) {
                        Text(if (stats.newCount == 0) "词书已学完，去设置换一本吧" else "今日已全部完成，明天见")
                    }
                } else {
                    Button(
                        onClick = { UiState.studyActive = true },
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                    ) {
                        Text(if (doneToday > 0) "继续学习" else "开始学习", fontSize = 16.sp)
                    }
                }
            }
        }

        // 速览
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MiniStat("${stats.learned}", "已学单词", Modifier.weight(1f))
            MiniStat("${stats.mastered}", "已掌握", Modifier.weight(1f))
            MiniStat("${AppState.streak.best}", "最长连续", Modifier.weight(1f))
            MiniStat(fmtTime(today.time), "今日用时", Modifier.weight(1f))
        }

        // 记忆状态分布
        SectionTitle("学习概览 · ${meta.name}")
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
    }
}

@Composable
private fun MiniStat(value: String, label: String, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface) {
        Column(
            Modifier.fillMaxWidth().padding(vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp))
        }
    }
}

@Composable
fun DistributionBar(stats: BookStats) {
    val dark = isDarkTheme()
    val segments = listOf(
        stats.newCount to AppColors.gray(dark),
        stats.learning to MaterialTheme.colorScheme.primary,
        stats.due to AppColors.orange(dark),
        stats.mastered to AppColors.green(dark)
    )
    val total = stats.total.coerceAtLeast(1)
    Box(
        Modifier
            .fillMaxWidth()
            .height(14.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
    ) {
        Row(Modifier.fillMaxSize()) {
            segments.forEach { (count, color) ->
                if (count > 0) {
                    Box(
                        Modifier
                            .weight(count.toFloat() / total)
                            .fillMaxSize()
                            .background(color)
                    )
                }
            }
        }
    }
}

@Composable
fun isDarkTheme(): Boolean = when (AppState.settings.theme) {
    "dark" -> true
    "light" -> false
    else -> androidx.compose.foundation.isSystemInDarkTheme()
}

fun fmtTime(sec: Long): String {
    return if (sec < 60) "${sec}秒"
    else if (sec < 3600) "${sec / 60}分${if (sec % 60 > 0) "${sec % 60}秒" else ""}"
    else "${sec / 3600}时${(sec % 3600) / 60}分"
}

private fun greeting(): String {
    val h = LocalTime.now().hour
    return when {
        h < 5 -> "夜深了"
        h < 9 -> "早上好"
        h < 12 -> "上午好"
        h < 14 -> "中午好"
        h < 18 -> "下午好"
        else -> "晚上好"
    } + " 👋"
}

private fun dateText(): String {
    val d: LocalDate = LocalDate.now()
    val week = "周" + "一二三四五六日"[d.dayOfWeek.value - 1]
    return "${d.monthValue}月${d.dayOfMonth}日 $week"
}
