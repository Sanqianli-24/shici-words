package com.shici.words

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import com.shici.words.data.AppState
import com.shici.words.ui.AppTheme
import com.shici.words.ui.HomeScreen
import com.shici.words.ui.LibraryScreen
import com.shici.words.ui.Onboarding
import com.shici.words.ui.SettingsScreen
import com.shici.words.ui.StatsScreen
import com.shici.words.ui.StudyScreen
import java.util.Locale

/** TTS 发音（系统自带，离线） */
object Speaker {
    private var tts: TextToSpeech? = null
    private var ready = false

    fun init(context: Context) {
        if (tts != null) return
        tts = TextToSpeech(context.applicationContext) { status ->
            ready = status == TextToSpeech.SUCCESS
            if (ready) {
                tts?.language = Locale.US
                tts?.setSpeechRate(0.9f)
            }
        }
    }

    fun speak(text: String) {
        val t = tts ?: return
        if (ready) t.speak(text, TextToSpeech.QUEUE_FLUSH, null, "word")
    }

    fun shutdown() {
        tts?.shutdown()
        tts = null
        ready = false
    }
}

/** 全局 UI 状态 */
object UiState {
    var studyActive by mutableStateOf(false)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        AppState.load(this)
        Speaker.init(this)
        setContent {
            AppTheme { App() }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Speaker.shutdown()
    }
}

private data class Tab(val label: String, val icon: @Composable () -> Unit)

@Composable
fun App() {
    if (!AppState.onboarded) {
        Onboarding(onDone = {
            AppState.onboarded = true
            AppState.save()
        })
        return
    }

    var tab by rememberSaveable { mutableStateOf(0) }
    val tabs = listOf(
        Tab("首页") { Icon(Icons.Filled.Home, null) },
        Tab("词库") { Icon(Icons.Filled.List, null) },
        Tab("统计") { Text("📊", fontSize = 20.sp) },
        Tab("设置") { Icon(Icons.Filled.Settings, null) }
    )

    Box {
        Scaffold(
            bottomBar = {
                NavigationBar {
                    tabs.forEachIndexed { i, t ->
                        NavigationBarItem(
                            selected = tab == i,
                            onClick = { tab = i },
                            icon = t.icon,
                            label = { Text(t.label) }
                        )
                    }
                }
            }
        ) { padding ->
            Box(Modifier.padding(padding)) {
                when (tab) {
                    0 -> HomeScreen()
                    1 -> LibraryScreen()
                    2 -> StatsScreen()
                    3 -> SettingsScreen()
                }
            }
        }
        if (UiState.studyActive) {
            StudyScreen(onExit = { UiState.studyActive = false })
        }
    }
}
