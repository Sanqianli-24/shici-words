package com.shici.words.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shici.words.BuildConfig
import com.shici.words.data.AppState
import com.shici.words.data.Books
import com.shici.words.update.Updater
import kotlinx.coroutines.launch
import java.io.File

private sealed class UpdateUi {
    object Idle : UpdateUi()
    object Checking : UpdateUi()
    object Latest : UpdateUi()
    data class Found(val info: Updater.ReleaseInfo) : UpdateUi()
    data class Downloading(val progress: Int) : UpdateUi()
    data class Ready(val file: File, val info: Updater.ReleaseInfo) : UpdateUi()
    data class Error(val msg: String) : UpdateUi()
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen() {
    val ctx = LocalContext.current
    AppState.revision
    val scope = rememberCoroutineScope()
    val bookId = AppState.settings.book
    val meta = Books.meta(bookId)
    val words = remember(bookId) { Books.words(ctx, bookId) }
    val stats = AppState.bookStats(bookId, words.size)

    var showBookPicker by remember { mutableStateOf(false) }
    var showDailyPicker by remember { mutableStateOf(false) }
    var showThemePicker by remember { mutableStateOf(false) }
    var showClearConfirm by remember { mutableStateOf(false) }
    var updateUi by remember { mutableStateOf<UpdateUi>(UpdateUi.Idle) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 16.dp)
    ) {
        Text(
            "设置",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)
        )

        SectionTitle("学习")
        Surface(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column {
                Cell(
                    title = "当前词书",
                    subtitle = "${meta.name} · 已学 ${stats.learned}/${stats.total}",
                    onClick = { showBookPicker = true }
                )
                Divider()
                Cell(
                    title = "每日新词量",
                    subtitle = "${AppState.settings.dailyNew} 个/天（复习词不受限）",
                    onClick = { showDailyPicker = true }
                )
            }
        }

        SectionTitle("偏好")
        Surface(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column {
                Cell(
                    title = "自动发音",
                    subtitle = "切换单词时自动朗读",
                    trailing = {
                        Switch(
                            checked = AppState.settings.autoPlay,
                            onCheckedChange = {
                                AppState.settings.autoPlay = it
                                AppState.save()
                            }
                        )
                    }
                )
                Divider()
                Cell(
                    title = "深色模式",
                    subtitle = when (AppState.settings.theme) {
                        "dark" -> "深色"; "light" -> "浅色"; else -> "跟随系统"
                    },
                    onClick = { showThemePicker = true }
                )
            }
        }

        SectionTitle("通用")
        Surface(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column {
                Cell(
                    title = "检查更新",
                    subtitle = "当前版本 v${BuildConfig.VERSION_NAME}",
                    onClick = {
                        updateUi = UpdateUi.Checking
                        scope.launch {
                            try {
                                val info = Updater.fetchLatest()
                                if (info == null || !Updater.isNewer(BuildConfig.VERSION_NAME, info.tag)) {
                                    updateUi = UpdateUi.Latest
                                } else {
                                    updateUi = UpdateUi.Found(info)
                                }
                            } catch (e: Exception) {
                                updateUi = UpdateUi.Error(e.message ?: "网络异常，请稍后再试")
                            }
                        }
                    }
                )
                Divider()
                Cell(
                    title = "清空学习数据",
                    subtitle = "删除全部进度、统计与收藏",
                    danger = true,
                    onClick = { showClearConfirm = true }
                )
            }
        }

        Text(
            "拾词 v${BuildConfig.VERSION_NAME} · 极简背单词\ngithub.com/Sanqianli-24/shici-words",
            modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }

    // ---------- 弹窗 ----------

    if (showBookPicker) {
        AlertDialog(
            onDismissRequest = { showBookPicker = false },
            title = { Text("选择词书") },
            text = {
                Column {
                    Books.metas.forEach { m ->
                        val selected = AppState.settings.book == m.id
                        val w = remember(m.id) { Books.words(ctx, m.id) }
                        val st = AppState.bookStats(m.id, w.size)
                        Row(
                            Modifier.fillMaxWidth().clickable {
                                AppState.settings.book = m.id
                                AppState.save()
                                showBookPicker = false
                            }.padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = selected, onClick = {
                                AppState.settings.book = m.id
                                AppState.save()
                                showBookPicker = false
                            })
                            Column(Modifier.weight(1f)) {
                                Text(m.name, fontSize = 15.sp)
                                Text(
                                    "${st.learned}/${st.total} 已学",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showBookPicker = false }) { Text("取消") }
            }
        )
    }

    if (showDailyPicker) {
        AlertDialog(
            onDismissRequest = { showDailyPicker = false },
            title = { Text("每日新词量") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(5, 10, 15, 20, 30, 50).forEach { n ->
                            FilterChip(
                                selected = AppState.settings.dailyNew == n,
                                onClick = {
                                    AppState.settings.dailyNew = n
                                    AppState.save()
                                    showDailyPicker = false
                                },
                                label = { Text("$n") }
                            )
                        }
                    }
                    Text(
                        "新词与到期复习词混合出现",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 10.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showDailyPicker = false }) { Text("取消") }
            }
        )
    }

    if (showThemePicker) {
        AlertDialog(
            onDismissRequest = { showThemePicker = false },
            title = { Text("深色模式") },
            text = {
                Column {
                    listOf("system" to "跟随系统", "light" to "浅色", "dark" to "深色").forEach { (k, label) ->
                        Row(
                            Modifier.fillMaxWidth().clickable {
                                AppState.settings.theme = k
                                AppState.save()
                                showThemePicker = false
                            }.padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = AppState.settings.theme == k,
                                onClick = {
                                    AppState.settings.theme = k
                                    AppState.save()
                                    showThemePicker = false
                                }
                            )
                            Text(label, fontSize = 15.sp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemePicker = false }) { Text("取消") }
            }
        )
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("确认清空全部数据？") },
            text = { Text("所有词书的学习进度、打卡记录和收藏都会被删除，且无法恢复。") },
            confirmButton = {
                TextButton(onClick = {
                    showClearConfirm = false
                    AppState.reset()
                }) { Text("清空", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) { Text("取消") }
            }
        )
    }

    // ---------- 更新流程弹窗 ----------

    when (val ui = updateUi) {
        is UpdateUi.Checking -> {
            AlertDialog(
                onDismissRequest = { },
                title = { Text("检查更新") },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.width(22.dp).height(22.dp), strokeWidth = 2.dp)
                        Text("正在查询最新版本…", Modifier.padding(start = 12.dp), fontSize = 14.sp)
                    }
                },
                confirmButton = {}
            )
        }
        is UpdateUi.Latest -> {
            AlertDialog(
                onDismissRequest = { updateUi = UpdateUi.Idle },
                title = { Text("检查更新") },
                text = { Text("已是最新版本 v${BuildConfig.VERSION_NAME} ✅") },
                confirmButton = {
                    TextButton(onClick = { updateUi = UpdateUi.Idle }) { Text("好的") }
                }
            )
        }
        is UpdateUi.Found -> {
            AlertDialog(
                onDismissRequest = { updateUi = UpdateUi.Idle },
                title = { Text("发现新版本 ${ui.info.tag}") },
                text = {
                    Column {
                        Text(ui.info.notes.ifBlank { "优化体验，修复已知问题" }, fontSize = 13.sp)
                        if (ui.info.apkSize > 0) {
                            Text(
                                "大小：${"%.1f".format(ui.info.apkSize / 1048576.0)} MB",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        val info = ui.info
                        updateUi = UpdateUi.Downloading(0)
                        scope.launch {
                            try {
                                val f = Updater.downloadApk(ctx, info.apkUrl) { p ->
                                    updateUi = UpdateUi.Downloading(p)
                                }
                                Updater.installApk(ctx, f)
                                updateUi = UpdateUi.Ready(f, info)
                            } catch (e: Exception) {
                                updateUi = UpdateUi.Error(e.message ?: "下载失败")
                            }
                        }
                    }) { Text("下载更新") }
                },
                dismissButton = {
                    TextButton(onClick = { updateUi = UpdateUi.Idle }) { Text("以后再说") }
                }
            )
        }
        is UpdateUi.Downloading -> {
            AlertDialog(
                onDismissRequest = { },
                title = { Text("正在下载") },
                text = {
                    Column {
                        LinearProgressIndicator(
                            progress = { ui.progress / 100f },
                            modifier = Modifier.fillMaxWidth(),
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                        Text(
                            "${ui.progress}%",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                },
                confirmButton = {}
            )
        }
        is UpdateUi.Ready -> {
            AlertDialog(
                onDismissRequest = { updateUi = UpdateUi.Idle },
                title = { Text("下载完成") },
                text = { Text("如未弹出安装界面，请允许本应用「安装未知应用」后重试。") },
                confirmButton = {
                    TextButton(onClick = {
                        Updater.installApk(ctx, ui.file)
                        updateUi = UpdateUi.Idle
                    }) { Text("打开安装") }
                },
                dismissButton = {
                    TextButton(onClick = { updateUi = UpdateUi.Idle }) { Text("关闭") }
                }
            )
        }
        is UpdateUi.Error -> {
            AlertDialog(
                onDismissRequest = { updateUi = UpdateUi.Idle },
                title = { Text("更新失败") },
                text = { Text(ui.msg) },
                confirmButton = {
                    TextButton(onClick = { updateUi = UpdateUi.Idle }) { Text("好的") }
                }
            )
        }
        UpdateUi.Idle -> {}
    }
}

@Composable
private fun Cell(
    title: String,
    subtitle: String? = null,
    danger: Boolean = false,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
            )
            if (subtitle != null) {
                Text(
                    subtitle,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
        if (trailing != null) trailing()
        else Text("›", fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun Divider() {
    androidx.compose.material3.HorizontalDivider(
        modifier = Modifier.padding(start = 16.dp),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outline
    )
}
