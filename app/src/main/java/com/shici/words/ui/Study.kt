package com.shici.words.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shici.words.Speaker
import com.shici.words.core.Srs
import com.shici.words.data.AppState
import com.shici.words.data.Books

/** 一次学习会话：队列管理 + 统计 */
class StudySession(private val bookId: String, plan: Srs.SessionPlan) {
    class QItem(val idx: Int, val isNew: Boolean, var shown: Boolean = false)

    private val queue = ArrayList<QItem>(plan.items.map { QItem(it.idx, it.isNew) })
    val total = plan.items.size
    var done = 0; private set
    var newPassed = 0; private set
    var reviewPassed = 0; private set
    var okFirst = 0; private set
    var totalFirst = 0; private set
    var finished = false; private set
    var current: QItem? = null; private set
    private val startAt = System.currentTimeMillis()
    var elapsedSec = 0L; private set
    private var timeRecorded = false

    fun prepareNext(): Boolean {
        if (queue.isEmpty()) {
            finished = true
            recordTime()
            return false
        }
        current = queue.removeAt(0)
        return true
    }

    private fun recordTime() {
        if (timeRecorded) return
        timeRecorded = true
        elapsedSec = (System.currentTimeMillis() - startAt) / 1000
        AppState.todayStat().time += elapsedSec
        AppState.save()
    }

    /** 评分。返回 (是否过卡, 是否被安排重现) */
    fun rate(grade: Int): Pair<Boolean, Boolean> {
        val item = current ?: return true to false
        val today = AppState.todayNum()
        val card = AppState.getOrCreateCard(bookId, item.idx)
        val first = !item.shown
        if (first) {
            item.shown = true
            totalFirst++
        }
        val passed = Srs.rate(card, grade, today)
        if (first) {
            val h = AppState.todayStat()
            h.total++
            if (grade == 2) { okFirst++; h.ok++ }
        }
        var requeued = false
        if (passed) {
            done++
            val h = AppState.todayStat()
            if (item.isNew) { newPassed++; h.new++ } else { reviewPassed++; h.review++ }
            AppState.touchStreak()
        } else {
            // 模糊/不认识：隔约 4 张卡重现，直到认识才算完成
            val pos = minOf(queue.size, 3 + (0..1).random())
            queue.add(pos, QItem(item.idx, item.isNew, item.shown))
            requeued = true
        }
        AppState.save()
        return passed to requeued
    }

    fun exitEarly() = recordTime()
}

@Composable
fun StudyScreen(onExit: () -> Unit) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val bookId = AppState.settings.book
    val words = remember(bookId) { Books.words(ctx, bookId) }
    val session = remember(bookId) {
        val plan = Srs.buildSession(
            AppState.cards(bookId), words.size,
            AppState.settings.dailyNew, AppState.todayStat().new, AppState.todayNum()
        )
        StudySession(bookId, plan).also { it.prepareNext() }
    }

    var revealed by remember { mutableStateOf(false) }
    var hint by remember { mutableStateOf<String?>(null) }
    var exitDialog by remember { mutableStateOf(false) }
    var tick by remember { mutableStateOf(0) } // 每张卡推进时触发重组

    val item = session.current
    val word = item?.let { words[it.idx] }

    // 自动发音
    LaunchedEffect(item?.idx, tick) {
        if (item != null && !session.finished && AppState.settings.autoPlay) {
            Speaker.speak(words[item.idx].w)
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        if (session.finished) {
            CompletionView(session = session, words = words.size, onExit = onExit)
            return@Column
        }

        // 顶栏
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { exitDialog = true }) {
                Icon(Icons.Filled.Close, contentDescription = "退出")
            }
            LinearProgressIndicator(
                progress = { if (session.total > 0) session.done.toFloat() / session.total else 0f },
                modifier = Modifier.weight(1f),
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Text(
                "${session.done}/${session.total}",
                modifier = Modifier.padding(horizontal = 14.dp),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // 词卡
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (word != null && item != null) {
                // 标签行
                Box(Modifier.fillMaxWidth()) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (item.isNew) MaterialTheme.colorScheme.primaryContainer
                        else AppColors.orange(isDarkTheme()).copy(alpha = 0.15f)
                    ) {
                        Text(
                            if (item.isNew) "新词" else "复习",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            fontSize = 12.sp,
                            color = if (item.isNew) MaterialTheme.colorScheme.primary else AppColors.orange(isDarkTheme())
                        )
                    }
                    // 收藏
                    val fav = AppState.isFav(bookId, item.idx)
                    androidx.compose.material3.IconToggleButton(
                        checked = fav,
                        onCheckedChange = { AppState.toggleFav(bookId, item.idx) }
                    ) {
                        Icon(
                            Icons.Filled.Star,
                            contentDescription = "收藏",
                            tint = if (fav) AppColors.gold(isDarkTheme()) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))
                Text(
                    word.w,
                    fontSize = if (word.w.length > 12) 34.sp else 44.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 10.dp)
                ) {
                    Text("/${word.ipa}/", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "🔊",
                        fontSize = 22.sp,
                        modifier = Modifier
                            .padding(6.dp)
                            .clickable { Speaker.speak(word.w) }
                    )
                }

                Spacer(Modifier.height(20.dp))
                if (!revealed) {
                    Text(
                        "回想它的意思，然后翻面",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(word.def, fontSize = 18.sp, textAlign = TextAlign.Center)
                            Spacer(Modifier.height(14.dp))
                            Text(word.ex, fontSize = 14.sp, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onBackground)
                            Text(
                                word.exZh,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        // 底部操作区
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)) {
            hint?.let { AutoHint(it) { hint = null } }
            AnimatedVisibility(visible = !revealed) {
                Button(
                    onClick = { revealed = true },
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    Text("显示释义", fontSize = 16.sp)
                }
            }
            AnimatedVisibility(visible = revealed) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    RateButton("😵 不认识", Color(0xFFE5484D), filled = false, modifier = Modifier.weight(1f)) { onRate(session, 0, { revealed = false; hint = null; tick++ }) { hint = it } }
                    RateButton("🤔 模糊", Color(0xFFF59E0B), filled = false, modifier = Modifier.weight(1f)) { onRate(session, 1, { revealed = false; hint = null; tick++ }) { hint = it } }
                    RateButton("😄 认识", Color(0xFF22B573), filled = true, modifier = Modifier.weight(1f)) { onRate(session, 2, { revealed = false; hint = null; tick++ }) { hint = it } }
                }
            }
        }
    }

    if (exitDialog) {
        AlertDialog(
            onDismissRequest = { exitDialog = false },
            title = { Text("结束本次学习？") },
            text = { Text("已完成的 ${session.done} 张卡片进度已保存，未完成的词仍会再次安排。") },
            confirmButton = {
                TextButton(onClick = {
                    exitDialog = false
                    session.exitEarly()
                    onExit()
                }) { Text("结束") }
            },
            dismissButton = {
                TextButton(onClick = { exitDialog = false }) { Text("继续学习") }
            }
        )
    }
}

private fun onRate(
    session: StudySession,
    grade: Int,
    advance: () -> Unit,
    setHint: (String) -> Unit
) {
    val (passed, requeued) = session.rate(grade)
    session.prepareNext()
    advance()
    if (requeued) setHint("这个单词稍后会再出现")
}

@Composable
private fun RateButton(
    text: String,
    color: Color,
    filled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    if (filled) {
        Button(
            onClick = onClick,
            modifier = modifier.height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = color, contentColor = Color.White),
            shape = RoundedCornerShape(14.dp)
        ) { Text(text, fontSize = 15.sp, maxLines = 1) }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier.height(50.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = color),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, color),
            shape = RoundedCornerShape(14.dp)
        ) { Text(text, fontSize = 15.sp, maxLines = 1) }
    }
}

@Composable
private fun CompletionView(session: StudySession, words: Int, onExit: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("🎉", fontSize = 60.sp)
        Text(
            "今日任务完成！",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(top = 12.dp)
        )
        Text(
            "坚持就是胜利",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )
        Spacer(Modifier.height(28.dp))
        Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp)) {
                Row {
                    StatCell("新学", "${session.newPassed}", Modifier.weight(1f))
                    StatCell("复习", "${session.reviewPassed}", Modifier.weight(1f))
                }
                Row(Modifier.padding(top = 14.dp)) {
                    StatCell(
                        "首答正确率",
                        if (session.totalFirst > 0) "${session.okFirst * 100 / session.totalFirst}%" else "-",
                        Modifier.weight(1f)
                    )
                    StatCell("用时", fmtTime(session.elapsedSec), Modifier.weight(1f))
                }
            }
        }
        Text(
            "明天约复习 ${AppState.tomorrowDueCount(AppState.settings.book)} 个单词",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 16.dp)
        )
        Spacer(Modifier.height(32.dp))
        Button(onClick = onExit, modifier = Modifier.fillMaxWidth().height(52.dp)) {
            Text("回到首页", fontSize = 16.sp)
        }
    }
}

@Composable
private fun StatCell(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp))
    }
}
