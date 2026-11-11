package com.shici.words.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import com.shici.words.core.Card
import com.shici.words.core.Srs
import com.shici.words.core.WordStatus
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

/** 每日学习统计 */
data class DayStat(
    var new: Int = 0,
    var review: Int = 0,
    var time: Long = 0,   // 秒
    var ok: Int = 0,      // 首答认识数
    var total: Int = 0    // 首答总数
)

data class StreakState(var cur: Int = 0, var best: Int = 0, var last: String = "")

data class Settings(
    var book: String = "cet4",
    var dailyNew: Int = 15,
    var autoPlay: Boolean = true,
    var theme: String = "system" // system | light | dark
)

data class BookStats(
    val total: Int, val learned: Int, val learning: Int,
    val due: Int, val mastered: Int
) {
    val newCount: Int get() = total - learned
}

/**
 * 全局应用状态：内存持有 + SharedPreferences 持久化（单键 JSON）。
 * revision 自增用于驱动 Compose 重组。
 */
object AppState {
    var settings = Settings()
    var progress = mutableMapOf<String, MutableMap<Int, Card>>()
    var history = mutableMapOf<String, DayStat>()
    var favorites = mutableSetOf<String>()   // "bookId:idx"
    var streak = StreakState()

    private val _onboarded = mutableStateOf(false)
    var onboarded: Boolean
        get() = _onboarded.value
        set(v) { _onboarded.value = v }

    private val _revision = mutableIntStateOf(0)
    var revision: Int
        get() = _revision.intValue
        private set(v) { _revision.intValue = v }

    private var prefs: SharedPreferences? = null

    fun todayNum(): Int = LocalDate.now().toEpochDay().toInt()
    fun todayStr(): String = LocalDate.now().toString()

    fun load(context: Context) {
        prefs = context.getSharedPreferences("shici_state", Context.MODE_PRIVATE)
        val raw = prefs?.getString("state", null) ?: return
        try {
            val root = JSONObject(raw)
            val s = root.optJSONObject("settings")
            if (s != null) {
                settings.book = s.optString("book", settings.book)
                settings.dailyNew = s.optInt("dailyNew", settings.dailyNew)
                settings.autoPlay = s.optBoolean("autoPlay", settings.autoPlay)
                settings.theme = s.optString("theme", settings.theme)
            }
            val p = root.optJSONObject("progress")
            if (p != null) {
                for (bookId in p.keys()) {
                    val m = p.optJSONObject(bookId) ?: continue
                    val map = mutableMapOf<Int, Card>()
                    for (k in m.keys()) {
                        val c = m.optJSONObject(k) ?: continue
                        map[k.toInt()] = Card(
                            idx = k.toInt(),
                            reps = c.optInt("reps"),
                            lapses = c.optInt("lapses"),
                            ef = c.optDouble("ef", Srs.EF_INIT),
                            interval = c.optInt("interval"),
                            due = c.optInt("due"),
                            firstDay = c.optInt("firstDay"),
                            lastDay = c.optInt("lastDay")
                        )
                    }
                    progress[bookId] = map
                }
            }
            val h = root.optJSONObject("history")
            if (h != null) {
                for (k in h.keys()) {
                    val d = h.optJSONObject(k) ?: continue
                    history[k] = DayStat(
                        d.optInt("new"), d.optInt("review"),
                        d.optLong("time"), d.optInt("ok"), d.optInt("total")
                    )
                }
            }
            val f = root.optJSONArray("favorites")
            if (f != null) for (i in 0 until f.length()) favorites.add(f.getString(i))
            val st = root.optJSONObject("streak")
            if (st != null) streak = StreakState(st.optInt("cur"), st.optInt("best"), st.optString("last"))
            onboarded = root.optBoolean("onboarded", false)
        } catch (_: Exception) {
            // 损坏则使用默认状态
        }
    }

    fun save() {
        val sp = prefs ?: return
        val root = JSONObject()
        root.put("settings", JSONObject().apply {
            put("book", settings.book); put("dailyNew", settings.dailyNew)
            put("autoPlay", settings.autoPlay); put("theme", settings.theme)
        })
        val p = JSONObject()
        for ((bookId, map) in progress) {
            val m = JSONObject()
            for ((idx, c) in map) {
                m.put(idx.toString(), JSONObject().apply {
                    put("reps", c.reps); put("lapses", c.lapses); put("ef", c.ef)
                    put("interval", c.interval); put("due", c.due)
                    put("firstDay", c.firstDay); put("lastDay", c.lastDay)
                })
            }
            p.put(bookId, m)
        }
        root.put("progress", p)
        val h = JSONObject()
        for ((k, d) in history) {
            h.put(k, JSONObject().apply {
                put("new", d.new); put("review", d.review); put("time", d.time)
                put("ok", d.ok); put("total", d.total)
            })
        }
        root.put("history", h)
        root.put("favorites", JSONArray(favorites))
        root.put("streak", JSONObject().apply {
            put("cur", streak.cur); put("best", streak.best); put("last", streak.last)
        })
            root.put("onboarded", onboarded)
        sp.edit().putString("state", root.toString()).apply()
        revision++
    }

    fun reset() {
        settings = Settings()
        progress.clear(); history.clear(); favorites.clear()
        streak = StreakState(); onboarded = false
        prefs?.edit()?.remove("state")?.apply()
        revision++
    }

    // ---------- 学习卡片 ----------

    fun cards(bookId: String): MutableMap<Int, Card> = progress.getOrPut(bookId) { mutableMapOf() }

    fun getOrCreateCard(bookId: String, idx: Int): Card {
        val m = cards(bookId)
        return m.getOrPut(idx) { Card(idx = idx, due = todayNum(), firstDay = todayNum()) }
    }

    fun card(bookId: String, idx: Int): Card? = cards(bookId)[idx]

    fun statusOf(bookId: String, idx: Int): WordStatus = Srs.statusOf(card(bookId, idx), todayNum())

    // ---------- 每日统计与打卡 ----------

    fun todayStat(): DayStat = history.getOrPut(todayStr()) { DayStat() }

    fun touchStreak() {
        val t = todayStr()
        if (streak.last != t) {
            val y = LocalDate.now().minusDays(1).toString()
            streak.cur = if (streak.last == y) streak.cur + 1 else 1
            streak.best = maxOf(streak.best, streak.cur)
            streak.last = t
        }
    }

    // ---------- 词书统计 ----------

    fun bookStats(bookId: String, total: Int): BookStats {
        var learning = 0; var due = 0; var mastered = 0
        val t = todayNum()
        for (c in cards(bookId).values) {
            when (Srs.statusOf(c, t)) {
                WordStatus.NEW -> {}
                WordStatus.LEARNING -> { learning++ }
                WordStatus.DUE -> due++
                WordStatus.MASTERED -> mastered++
            }
        }
        return BookStats(total, learning + due + mastered, learning, due, mastered)
    }

    // ---------- 收藏 ----------

    fun isFav(bookId: String, idx: Int): Boolean = favorites.contains("$bookId:$idx")
    fun toggleFav(bookId: String, idx: Int): Boolean {
        val key = "$bookId:$idx"
        val added = if (favorites.contains(key)) { favorites.remove(key); false } else { favorites.add(key); true }
        save()
        return added
    }

    // 明日（含今日未完成）待复习数
    fun tomorrowDueCount(bookId: String): Int {
        val t = todayNum()
        return cards(bookId).values.count { it.due <= t + 1 }
    }
}
