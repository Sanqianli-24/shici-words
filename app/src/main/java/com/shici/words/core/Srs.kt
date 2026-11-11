package com.shici.words.core

/**
 * SM-2 改良间隔重复算法（纯 Kotlin，可单元测试）
 *
 * 评分：0 = 不认识，1 = 模糊，2 = 认识
 * 间隔：认识后 1天 → 3天 → 间隔 × 难度系数 EF（初始 2.5）
 * 掌握：间隔 ≥ 21 天视为「已掌握」（仍会周期性复习）
 */
object Srs {
    const val MASTERED_INTERVAL = 21
    const val EF_INIT = 2.5
    const val EF_MIN = 1.3
    const val EF_MAX = 3.0

    /**
     * 对卡片评分。
     * @param today 今天的天数序号（LocalDate.toEpochDay）
     * @return true = 过卡（今天不再出现）；false = 未通过，需在本次学习中稍后重现
     */
    fun rate(card: Card, grade: Int, today: Int): Boolean {
        card.lastDay = today
        if (grade == 2) {
            card.reps += 1
            card.ef = (card.ef + 0.05).coerceAtMost(EF_MAX)
            card.interval = when {
                card.reps == 1 -> 1
                card.reps == 2 -> 3
                else -> maxOf(1, Math.round(card.interval * card.ef).toInt())
            }
            card.due = today + card.interval
            return true
        }
        // 模糊 / 不认识：重置进度，今天稍后重现，通过后从 1 天间隔重来
        card.reps = 0
        card.interval = 0
        card.due = today
        if (grade == 0) {
            card.lapses += 1
            card.ef = (card.ef - 0.2).coerceAtLeast(EF_MIN)
        } else {
            card.ef = (card.ef - 0.05).coerceAtLeast(EF_MIN)
        }
        return false
    }

    /** 单词当前展示状态 */
    fun statusOf(card: Card?, today: Int): WordStatus {
        if (card == null) return WordStatus.NEW
        if (card.due <= today && card.reps == 0 && card.interval == 0) return WordStatus.DUE
        return when {
            card.interval >= MASTERED_INTERVAL && card.due > today -> WordStatus.MASTERED
            card.due <= today -> WordStatus.DUE
            else -> WordStatus.LEARNING
        }
    }

    /** 是否到期需要复习 */
    fun isDue(card: Card, today: Int): Boolean = card.due <= today

    data class SessionItem(val idx: Int, val isNew: Boolean)
    data class SessionPlan(val items: List<SessionItem>, val dueCount: Int, val newCount: Int)

    /**
     * 组卷：全部到期复习词 + 剩余额度的新词，新词与复习词交叉排序
     * @param cards 已有学习记录的卡片（idx → card）
     * @param totalWords 词书总词数
     * @param dailyNew 每日新词量
     * @param newDoneToday 今天已学新词数
     */
    fun buildSession(
        cards: Map<Int, Card>,
        totalWords: Int,
        dailyNew: Int,
        newDoneToday: Int,
        today: Int
    ): SessionPlan {
        val due = cards.values
            .filter { it.due <= today }
            .sortedWith(compareBy({ it.due }, { it.idx }))
            .map { SessionItem(it.idx, false) }

        val quota = (dailyNew - newDoneToday).coerceAtLeast(0)
        val news = ArrayList<SessionItem>()
        if (quota > 0) {
            for (i in 0 until totalWords) {
                if (i !in cards) {
                    news.add(SessionItem(i, true))
                    if (news.size >= quota) break
                }
            }
        }

        val items = ArrayList<SessionItem>(due.size + news.size)
        if (news.isEmpty()) {
            items += due
        } else if (due.isEmpty()) {
            items += news
        } else {
            // 交叉：每 gap 张复习词后插入 1 张新词
            val gap = Math.ceil(due.size.toDouble() / news.size).toInt().coerceAtLeast(1)
            var di = 0
            for (n in news) {
                repeat(gap) { if (di < due.size) items.add(due[di++]) }
                items.add(n)
            }
            while (di < due.size) items.add(due[di++])
        }
        return SessionPlan(items, due.size, news.size)
    }
}

/** 单词学习状态（用于词库筛选、徽章展示） */
enum class WordStatus { NEW, LEARNING, DUE, MASTERED }

/** 每个单词的学习卡片 */
data class Card(
    val idx: Int,
    var reps: Int = 0,
    var lapses: Int = 0,
    var ef: Double = Srs.EF_INIT,
    var interval: Int = 0,
    var due: Int = 0,
    var firstDay: Int = 0,
    var lastDay: Int = 0
)
