package com.shici.words.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SrsTest {

    private val TODAY = 20000

    @Test
    fun `连续认识间隔按1_3_乘系数增长`() {
        val card = Card(idx = 0)
        assertTrue(Srs.rate(card, 2, TODAY))
        assertEquals(1, card.interval)
        assertEquals(TODAY + 1, card.due)

        assertTrue(Srs.rate(card, 2, TODAY + 1))
        assertEquals(3, card.interval)
        assertEquals(TODAY + 1 + 3, card.due)

        assertTrue(Srs.rate(card, 2, TODAY + 4))
        // 3 * ef(2.6) = 7.8 → 8
        assertEquals(8, card.interval)
    }

    @Test
    fun `认识提升难度系数且不超过上限`() {
        val card = Card(idx = 0)
        repeat(20) { Srs.rate(card, 2, TODAY + it) }
        assertTrue(card.ef <= Srs.EF_MAX)
    }

    @Test
    fun `不认识重置进度并当天重现`() {
        val card = Card(idx = 0)
        Srs.rate(card, 2, TODAY)
        Srs.rate(card, 2, TODAY + 1)
        val passed = Srs.rate(card, 0, TODAY + 4)
        assertFalse(passed)
        assertEquals(0, card.reps)
        assertEquals(0, card.interval)
        assertEquals(TODAY + 4, card.due) // 当天重现
        assertEquals(1, card.lapses)
        // EF 下降
        assertTrue(card.ef < Srs.EF_INIT)
    }

    @Test
    fun `模糊同样重现但EF降幅小`() {
        val c1 = Card(idx = 0).also { Srs.rate(it, 0, TODAY) }
        val c2 = Card(idx = 1).also { Srs.rate(it, 1, TODAY) }
        assertTrue(c1.ef < c2.ef)
        assertEquals(TODAY, c1.due)
        assertEquals(TODAY, c2.due)
    }

    @Test
    fun `遗忘后重新认识从1天间隔开始`() {
        val card = Card(idx = 0)
        Srs.rate(card, 2, TODAY)
        Srs.rate(card, 2, TODAY + 1)
        Srs.rate(card, 0, TODAY + 4)     // 遗忘
        assertTrue(Srs.rate(card, 2, TODAY + 4))
        assertEquals(1, card.reps)
        assertEquals(1, card.interval)
        assertEquals(TODAY + 5, card.due)
    }

    @Test
    fun `EF有下限`() {
        val card = Card(idx = 0, ef = 1.32)
        repeat(5) { Srs.rate(card, 0, TODAY) }
        assertTrue(card.ef >= Srs.EF_MIN)
    }

    @Test
    fun `状态流转`() {
        assertEquals(WordStatus.NEW, Srs.statusOf(null, TODAY))

        val card = Card(idx = 0)
        Srs.rate(card, 2, TODAY)          // 1天后到期
        assertEquals(WordStatus.LEARNING, Srs.statusOf(card, TODAY))
        assertEquals(WordStatus.DUE, Srs.statusOf(card, TODAY + 1))

        // 推到间隔>=21 → 掌握
        Srs.rate(card, 2, TODAY)
        Srs.rate(card, 2, TODAY)
        var guard = 0
        while (card.interval < Srs.MASTERED_INTERVAL && guard++ < 50) {
            Srs.rate(card, 2, TODAY)
        }
        assertTrue(card.interval >= Srs.MASTERED_INTERVAL)
        assertEquals(WordStatus.MASTERED, Srs.statusOf(card, TODAY))
    }

    @Test
    fun `组卷_无记录时只出新词`() {
        val plan = Srs.buildSession(mapOf(), totalWords = 100, dailyNew = 15, newDoneToday = 0, today = TODAY)
        assertEquals(15, plan.newCount)
        assertEquals(0, plan.dueCount)
        assertEquals(15, plan.items.size)
        assertTrue(plan.items.all { it.isNew })
        // 按词书顺序取
        assertEquals(listOf(0, 1, 2), plan.items.take(3).map { it.idx })
    }

    @Test
    fun `组卷_复习词与新词交叉`() {
        val cards = mutableMapOf<Int, Card>()
        for (i in 0 until 9) cards[i] = Card(idx = i, reps = 3, interval = 5, due = TODAY - 1)
        val plan = Srs.buildSession(cards, totalWords = 100, dailyNew = 3, newDoneToday = 0, today = TODAY)
        assertEquals(9, plan.dueCount)
        assertEquals(3, plan.newCount)
        assertEquals(12, plan.items.size)
        // 新词不出现在开头，且新词分散
        assertFalse(plan.items.first().isNew)
        val newPositions = plan.items.mapIndexedNotNull { i, it -> if (it.isNew) i else null }
        assertEquals(3, newPositions.size)
        assertTrue(newPositions.last() < plan.items.size)
    }

    @Test
    fun `组卷_当日额度用完只出复习词`() {
        val cards = mutableMapOf(0 to Card(idx = 0, reps = 1, interval = 1, due = TODAY))
        val plan = Srs.buildSession(cards, totalWords = 100, dailyNew = 10, newDoneToday = 10, today = TODAY)
        assertEquals(0, plan.newCount)
        assertEquals(1, plan.dueCount)
    }

    @Test
    fun `组卷_全部完成时为空`() {
        val cards = mutableMapOf(0 to Card(idx = 0, reps = 2, interval = 5, due = TODAY + 5))
        val plan = Srs.buildSession(cards, totalWords = 1, dailyNew = 10, newDoneToday = 10, today = TODAY)
        assertTrue(plan.items.isEmpty())
    }

    @Test
    fun `组卷_新词额度受剩余词数限制`() {
        val plan = Srs.buildSession(mapOf(), totalWords = 3, dailyNew = 15, newDoneToday = 0, today = TODAY)
        assertEquals(3, plan.newCount)
    }
}
