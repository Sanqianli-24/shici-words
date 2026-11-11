package com.shici.words.ui

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shici.words.data.AppState
import com.shici.words.data.Books

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun Onboarding(onDone: () -> Unit) {
    var bookId by remember { mutableStateOf(AppState.settings.book) }
    var daily by remember { mutableStateOf(AppState.settings.dailyNew) }
    val ctx = androidx.compose.ui.platform.LocalContext.current

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .padding(top = 64.dp, bottom = 24.dp)
    ) {
        Text("📖", fontSize = 56.sp)
        Text(
            "拾词",
            modifier = Modifier.padding(top = 12.dp),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            "极简背单词 · 科学复习",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(Modifier.height(32.dp))
        SectionTitle("选择一本词书", Modifier.padding(0.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Books.metas.forEach { meta ->
                val selected = bookId == meta.id
                OutlinedCard(
                    onClick = { bookId = meta.id },
                    modifier = Modifier.fillMaxWidth(),
                    border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                    else CardDefaults.outlinedCardBorder(),
                    colors = if (selected)
                        CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f))
                    else CardDefaults.outlinedCardColors()
                ) {
                    Row(
                        Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("${meta.name}  ·  ${meta.level}", style = MaterialTheme.typography.titleMedium)
                            Text(
                                meta.desc,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                        Text(
                            "${Books.words(ctx, meta.id).size}词",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        SectionTitle("每天学多少新词", Modifier.padding(0.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf(5, 10, 15, 20, 30, 50).forEach { n ->
                FilterChip(
                    selected = daily == n,
                    onClick = { daily = n },
                    label = { Text("$n 个/天") }
                )
            }
        }
        Text(
            "到期的复习词每天自动安排，不受此数量限制",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp)
        )

        Spacer(Modifier.weight(1f))
        Button(
            onClick = {
                AppState.settings.book = bookId
                AppState.settings.dailyNew = daily
                onDone()
            },
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            Text("开始学习", fontSize = 16.sp)
        }
        Text(
            "进度保存在手机本地，无需注册",
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
