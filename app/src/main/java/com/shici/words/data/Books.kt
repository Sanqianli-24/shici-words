package com.shici.words.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** 单词：拼写、音标、释义、例句（英/中） */
data class Word(val w: String, val ipa: String, val def: String, val ex: String, val exZh: String)

data class BookMeta(val id: String, val name: String, val level: String, val desc: String)

/** 词书加载：从 assets 读取 words_{id}.json */
object Books {
    val metas = listOf(
        BookMeta("cet4", "四级核心词", "大学", "大学英语四级高频词汇，考研打底必备"),
        BookMeta("cet6", "六级核心词", "大学", "大学英语六级高频词汇，进阶提升"),
        BookMeta("kaoyan", "考研核心词", "考研", "考研英语高频核心词，阅读提分关键")
    )

    fun meta(id: String): BookMeta = metas.first { it.id == id }

    private val cache = mutableMapOf<String, List<Word>>()

    fun words(context: Context, bookId: String): List<Word> {
        return cache.getOrPut(bookId) {
            val text = context.assets.open("words_$bookId.json").bufferedReader().use { it.readText() }
            val arr = JSONArray(text)
            List(arr.length()) { i ->
                val e = arr.getJSONArray(i)
                Word(e.getString(0), e.getString(1), e.getString(2), e.getString(3), e.getString(4))
            }
        }
    }
}
