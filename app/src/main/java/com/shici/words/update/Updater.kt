package com.shici.words.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * 应用内更新：查询 GitHub Releases → 下载 APK → 调起系统安装。
 * 更新链路：push 标签 v* → GitHub Actions 自动构建并发布 Release。
 */
object Updater {
    private const val REPO = "Sanqianli-24/shici-words"
    private const val API = "https://api.github.com/repos/$REPO/releases/latest"

    data class ReleaseInfo(
        val tag: String,       // 如 v1.1.0
        val name: String,      // 发布标题
        val notes: String,     // 更新说明（body）
        val apkUrl: String,    // APK 下载地址
        val apkSize: Long
    )

    class UpdateException(msg: String) : Exception(msg)

    /** 查询最新版本（IO 线程） */
    suspend fun fetchLatest(): ReleaseInfo? = withContext(Dispatchers.IO) {
        val conn = URL(API).openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = 10000
            conn.readTimeout = 15000
            conn.setRequestProperty("Accept", "application/vnd.github+json")
            val code = conn.responseCode
            if (code == 404) return@withContext null  // 尚无任何 Release
            if (code != 200) throw UpdateException("GitHub 接口返回 $code")
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            val root = JSONObject(body)
            val assets = root.optJSONArray("assets") ?: JSONArray()
            var apkUrl: String? = null
            var apkSize = 0L
            for (i in 0 until assets.length()) {
                val a = assets.getJSONObject(i)
                val name = a.optString("name", "")
                if (name.endsWith(".apk")) {
                    apkUrl = a.getString("browser_download_url")
                    apkSize = a.optLong("size")
                    break
                }
            }
            if (apkUrl == null) throw UpdateException("该版本没有 APK 附件")
            ReleaseInfo(
                tag = root.optString("tag_name", ""),
                name = root.optString("name", ""),
                notes = root.optString("body", ""),
                apkUrl = apkUrl,
                apkSize = apkSize
            )
        } finally {
            conn.disconnect()
        }
    }

    /** 语义化版本比较：远程是否比当前新 */
    fun isNewer(current: String, remote: String): Boolean {
        val c = current.removePrefix("v").split('.').map { it.toIntOrNull() ?: 0 }
        val r = remote.removePrefix("v").split('.').map { it.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(c.size, r.size)) {
            val a = c.getOrElse(i) { 0 }
            val b = r.getOrElse(i) { 0 }
            if (a != b) return b > a
        }
        return false
    }

    /**
     * 下载 APK 到应用私有目录（无需存储权限）。
     * @param onProgress 0..100
     */
    suspend fun downloadApk(
        context: Context,
        url: String,
        onProgress: (Int) -> Unit
    ): File = withContext(Dispatchers.IO) {
        val dir = File(context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS), "shici")
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, "shici-update.apk")
        if (file.exists()) file.delete()

        val conn = URL(url).openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = 15000
            conn.readTimeout = 30000
            if (conn.responseCode != 200) throw UpdateException("下载失败：HTTP ${conn.responseCode}")
            val total = conn.contentLengthLong
            var read = 0L
            conn.inputStream.use { input ->
                FileOutputStream(file).use { out ->
                    val buf = ByteArray(8192)
                    while (true) {
                        val n = input.read(buf)
                        if (n < 0) break
                        out.write(buf, 0, n)
                        read += n
                        if (total > 0) {
                            val p = (read * 100 / total).toInt()
                            withContext(Dispatchers.Main) { onProgress(p) }
                        }
                    }
                }
            }
            withContext(Dispatchers.Main) { onProgress(100) }
            file
        } catch (e: Exception) {
            file.delete()
            throw e
        } finally {
            conn.disconnect()
        }
    }

    /** 通过 FileProvider 调起系统安装器；未授权时引导去开启「安装未知应用」 */
    fun installApk(context: Context, file: File) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O &&
            !context.packageManager.canRequestPackageInstalls()
        ) {
            val i = Intent(
                android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}")
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(i)
            return
        }
        val uri: Uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
