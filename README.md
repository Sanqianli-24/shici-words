# 拾词 · 极简背单词

参考墨墨背单词、不背单词、百词斩的核心玩法，去掉花哨功能的 Android 原生背单词应用。

**无需注册、无需联网、无广告** —— 所有数据保存在手机本地。

## 核心功能

- 📖 **内置词书**：四级核心 200 词 / 六级核心 150 词 / 考研核心 203 词（音标 + 词性释义 + 中英例句），随时切换，进度独立保存
- 🎯 **每日计划**：到期复习词全部安排 + 可设置的每日新词量（5~50）
- 🧠 **间隔重复**：SM-2 改良记忆算法，认识 → 间隔 1 天 → 3 天 → 按记忆系数增长；间隔 ≥ 21 天即为「已掌握」
- ✋ **自评式学习**（墨墨式）：看单词 → 自己回忆 → 翻面看释义 → 自评「认识 / 模糊 / 不认识」；模糊和不认识会在本轮稍后重现，直到认识才算完成
- 🔀 **新词复习交叉出题**，学的和复习的混在一起更符合记忆规律
- 🔊 **自动发音**（系统 TTS，离线）
- 📚 **词库浏览**：状态筛选（未学/复习中/待复习/已掌握）、搜索、收藏、单词详情
- 📊 **学习统计**：连续打卡、15 周打卡热力图、记忆状态分布、近 7 天学习量
- 🌙 深色模式 / 浅色模式 / 跟随系统
- 🔄 **应用内检查更新**：直连 GitHub Releases 下载新版本安装

## 安装

1. 到 [Releases](https://github.com/Sanqianli-24/shici-words/releases) 下载最新的 `shici-words-v*.apk`
2. 传到手机后点击安装，允许「未知来源应用」即可
3. 已装旧版本的用户可直接在 App 内 **设置 → 检查更新** 升级（签名一致，覆盖安装，学习数据不丢失）

## 如何发布新版本（迭代流程）

1. 修改代码 / 词书数据
2. 修改 `app/build.gradle.kts` 中的 `versionCode`（+1）和 `versionName`（如 `1.1.0`）
3. 提交并打标签推送：
   ```bash
   git add -A && git commit -m "release: v1.1.0"
   git tag v1.1.0 && git push origin main --tags
   ```
4. GitHub Actions 自动构建并发布 Release（含 APK）
5. 用户在 App 内「检查更新」即可收到新版本

手动触发构建：Actions → Release → Run workflow。

## 自定义词书

词书是纯 JSON，位于 `app/src/main/assets/`，格式为单词数组：

```json
[
  ["abandon", "əˈbændən", "v. 放弃；抛弃", "He abandoned the plan.", "他放弃了计划。"]
]
```

五个字段依次是：**单词、音标、词性+中文释义、英文例句、例句翻译**。

添加新书步骤：
1. 新建 `app/src/main/assets/words_你的词书id.json`
2. 在 `app/src/main/java/com/shici/words/data/Books.kt` 的 `metas` 列表加一行元信息
3. 重新构建安装即可（学习进度不受影响）

## 技术说明

- Kotlin + Jetpack Compose (Material 3)，单 Activity，无 Room / 无网络框架（更新用 HttpURLConnection）
- 记忆算法在 `core/Srs.kt`，纯 Kotlin 且带单元测试（`gradlew test`）
- 数据持久化：SharedPreferences 单键 JSON
- 统一签名密钥 `shici.keystore` 随仓库提交，保证本地与 CI 构建的 APK 签名一致，应用内更新可直接覆盖安装（个人开源项目的取舍）
- 构建：JDK 17 + Android SDK 34 + Gradle 8.7（含 gradlew，CI 开箱即用）

## 本地构建

```bash
./gradlew assembleRelease
# 产物：app/build/outputs/apk/release/app-release.apk
```
