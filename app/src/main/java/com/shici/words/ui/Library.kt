package com.shici.words.ui

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shici.words.Speaker
import com.shici.words.core.Card
import com.shici.words.core.WordStatus
import com.shici.words.data.AppState
import com.shici.words.data.Books
import com.shici.words.data.BookStats
import java.time.LocalDate

private val FILTERS = listOf("全部", "未学", "复习中", "待复习", "已掌握", "⭐收藏")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen() {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    AppState.revision
    val bookId = AppState.settings.book
    val meta = Books.meta(bookId)
    val words = remember(bookId) { Books.words(ctx, bookId) }
    val stats = AppState.bookStats(bookId, words.size)
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf(0) }
    var detailIdx by remember { mutableStateOf<Int?>(null) }

    val shown = remember(query, filter, AppState.revision, bookId) {
        words.indices.filter { i ->
            val w = words[i]
            val q = query.trim().lowercase()
            val matchQ = q.isEmpty() || w.w.lowercase().contains(q) || w.def.contains(query.trim())
            val st = AppState.statusOf(bookId, i)
            val matchF = when (filter) {
                1 -> st == WordStatus.NEW
                2 -> st == WordStatus.LEARNING
                3 -> st == WordStatus.DUE
                4 -> st == WordStatus.MASTERED
                5 -> AppState.isFav(bookId, i)
                else -> true
            }
            matchQ && matchF
        }
    }

    Column(Modifier.fillMaxSize()) {
        // 头部
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("词库", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.width(8.dp))
                Text(
                    meta.name,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                LinearProgressIndicator(
                    progress = { if (stats.total > 0) stats.learned.toFloat() / stats.total else 0f },
                    modifier = Modifier.weight(1f),
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    "${stats.learned}/${stats.total}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 搜索
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            placeholder = { Text("搜索单词 / 中文释义", fontSize = 14.sp) },
            leadingIcon = { Icon(Icons.Filled.Search, null) },
            singleLine = true,
            shape = RoundedCornerShape(14.dp)
        )

        // 筛选
        LazyRow(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(FILTERS) { i, label ->
                FilterChip(
                    selected = filter == i,
                    onClick = { filter = i },
                    label = { Text(label) }
                )
            }
        }

        // 词表
        if (shown.isEmpty()) {
            EmptyHint("没有符合条件的单词")
        } else {
            LazyColumn(contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 16.dp)) {
                items(shown) { idx ->
                    WordRow(words[idx], AppState.statusOf(bookId, idx), AppState.isFav(bookId, idx)) {
                        detailIdx = idx
                    }
                }
            }
        }
    }

    detailIdx?.let { idx ->
        WordDetailSheet(
            word = words[idx],
            card = AppState.card(bookId, idx),
            status = AppState.statusOf(bookId, idx),
            isFav = AppState.isFav(bookId, idx),
            onDismiss = { detailIdx = null },
            onToggleFav = { AppState.toggleFav(bookId, idx) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WordRow(word: com.shici.words.data.Word, status: WordStatus, fav: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(word.w, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        "  /${word.ipa}/",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    word.def,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            if (fav) {
                Text("⭐", fontSize = 13.sp, modifier = Modifier.padding(end = 6.dp))
            }
            StatusBadge(status, compact = true)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordDetailSheet(
    word: com.shici.words.data.Word,
    card: Card?,
    status: WordStatus,
    isFav: Boolean,
    onDismiss: () -> Unit,
    onToggleFav: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(Modifier.fillMaxWidth()) {
                StatusBadge(status, Modifier.align(Alignment.CenterStart))
                IconToggleButton(checked = isFav, onCheckedChange = { onToggleFav() }, modifier = Modifier.align(Alignment.CenterEnd)) {
                    Icon(
                        Icons.Filled.Star, "收藏",
                        tint = if (isFav) AppColors.gold(isDarkTheme()) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                word.w,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 10.dp)
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 6.dp)
            ) {
                Text("/${word.ipa}/", fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    "🔊",
                    fontSize = 20.sp,
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .clickable { Speaker.speak(word.w) }
                )
            }
            Spacer(Modifier.height(16.dp))
            Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(word.def, fontSize = 16.sp)
                    Spacer(Modifier.height(10.dp))
                    Text(word.ex, fontSize = 13.sp)
                    Text(word.exZh, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 3.dp))
                }
            }
            if (card != null) {
                Spacer(Modifier.height(14.dp))
                val dueDate = LocalDate.ofEpochDay(card.due.toLong())
                val today = LocalDate.now()
                val dueText = when {
                    card.due <= today.toEpochDay().toInt() -> "已到期"
                    card.due == today.toEpochDay().toInt() + 1 -> "明天"
                    else -> "${card.due - today.toEpochDay().toInt()} 天后"
                }
                Text(
                    "复习 ${card.reps} 次 · 遗忘 ${card.lapses} 次 · 下次复习：$dueText",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Spacer(Modifier.height(14.dp))
                Text("还未学习过", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
