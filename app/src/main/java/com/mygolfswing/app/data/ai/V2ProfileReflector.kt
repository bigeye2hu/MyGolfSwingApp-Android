package com.mygolfswing.app.data.ai

import com.mygolfswing.app.data.ServiceLocator
import com.mygolfswing.app.data.model.V2AIMessageRole
import com.mygolfswing.app.data.model.V2Goal
import com.mygolfswing.app.data.model.V2MemoKind
import com.mygolfswing.app.data.model.V2ProfileSource
import com.mygolfswing.app.data.store.V2AIChatStore
import com.mygolfswing.app.data.store.V2GoalStore
import com.mygolfswing.app.data.store.V2GolfProfileStore
import com.mygolfswing.app.data.store.V2MemoStore
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

/**
 * V2 综合反射器（对齐 iOS `V2ProfileReflector`）。
 */
object V2ProfileReflector {

    sealed class Result {
        data class Updated(val itemsAdded: Int, val summaryChanged: Boolean) : Result()
        data object NoChange : Result()
        data object Empty : Result()
        data object Skipped : Result()
        data class Failed(val message: String) : Result()
    }

    @Volatile
    private var isReflecting = false

    suspend fun reflectAll(conversationId: UUID? = null): Result {
        if (isReflecting) return Result.Skipped
        isReflecting = true
        try {
            val sections = buildSections(conversationId).filter { it.second.isNotBlank() }
            if (sections.isEmpty()) return Result.Empty

            val prompt = buildPrompt(sections)
            val response = try {
                ServiceLocator.aiService.generateSummary(prompt)
            } catch (e: Exception) {
                return Result.Failed(e.message ?: "未知错误")
            }
            val (added, summaryChanged) = parseAndUpdateProfile(response)
            return if (added > 0 || summaryChanged) {
                Result.Updated(added, summaryChanged)
            } else {
                Result.NoChange
            }
        } finally {
            isReflecting = false
        }
    }

    private fun buildSections(conversationId: UUID?): List<Pair<String, String>> {
        val convSection = buildConversationSection(conversationId)
        val memoSection = buildInsightSection()
        val videoSection = buildVideoSection()
        val goalSection = buildGoalSection()
        return listOf(
            "【最近对话】" to (convSection ?: ""),
            "【最近文字笔记】" to (memoSection ?: ""),
            "【最近视频备忘录】" to (videoSection ?: ""),
            "【当前目标】" to (goalSection ?: ""),
        )
    }

    private fun buildConversationSection(conversationId: UUID?): String? {
        val session = conversationId?.let { id ->
            V2AIChatStore.sessions.value.firstOrNull { it.id == id }
                ?: if (V2AIChatStore.currentSession.value.id == id) V2AIChatStore.currentSession.value else null
        } ?: V2AIChatStore.currentSession.value

        val recent = session.messages
            .filter { it.role == V2AIMessageRole.USER || it.role == V2AIMessageRole.ASSISTANT }
            .filter { it.text.isNotBlank() && !it.isStreaming }
            .takeLast(20)
        if (recent.isEmpty()) return null
        return recent.joinToString("\n") { m ->
            val role = if (m.role == V2AIMessageRole.USER) "用户" else "小高"
            "[$role]: ${m.text}"
        }
    }

    private fun buildInsightSection(): String? {
        val now = LocalDateTime.now()
        val recent = V2MemoStore.allMemos.value
            .filter { it.kind == V2MemoKind.TEXT || it.kind == V2MemoKind.MIXED }
            .mapNotNull { memo ->
                val days = ChronoUnit.DAYS.between(memo.createdAt, now).toInt()
                if (days > 30) null else memo to days
            }
            .sortedBy { it.second }
            .take(5)
        if (recent.isEmpty()) return null
        return recent.joinToString("\n") { (memo, days) ->
            val title = memo.displayTitle ?: memo.title ?: "笔记"
            val summary = (memo.text ?: memo.subText ?: "").take(200)
            val tags = memo.tags.take(4).joinToString("·")
            val tagPart = if (tags.isEmpty()) "" else "  标签: $tags"
            "- (" + days + "天前) " + title + tagPart + "\n  内容: " + summary
        }
    }

    private fun buildVideoSection(): String? {
        val now = LocalDateTime.now()
        val recent = V2MemoStore.allMemos.value
            .filter { it.kind == V2MemoKind.VIDEO || it.kind == V2MemoKind.MIXED }
            .mapNotNull { memo ->
                val days = ChronoUnit.DAYS.between(memo.createdAt, now).toInt()
                if (days > 30) null else memo to days
            }
            .sortedBy { it.second }
            .take(5)
        if (recent.isEmpty()) return null
        return recent.joinToString("\n") { (memo, days) ->
            val title = memo.displayTitle ?: memo.title ?: "视频"
            val tags = memo.tags.take(4).joinToString("·")
            val tagPart = if (tags.isEmpty()) "" else "  标签: $tags"
            val desc = (memo.text ?: "").take(150)
            val descPart = if (desc.isEmpty()) "" else "\n  描述: $desc"
            "- (" + days + "天前) " + title + tagPart + descPart
        }
    }

    private fun buildGoalSection(): String? {
        val now = LocalDateTime.now()
        val candidates = V2GoalStore.goals.value.filter { goal ->
            when (goal.status) {
                V2Goal.Status.ACTIVE, V2Goal.Status.PAUSED -> true
                V2Goal.Status.COMPLETED -> {
                    val ref = goal.completedAt ?: goal.updatedAt
                    ChronoUnit.DAYS.between(ref, now) <= 30
                }
                V2Goal.Status.ARCHIVED -> false
            }
        }
        if (candidates.isEmpty()) return null
        val sorted = candidates.sortedWith(
            compareBy<V2Goal> { it.status != V2Goal.Status.ACTIVE }
                .thenBy { priorityRank(it.priority) }
                .thenByDescending { it.startedAt },
        ).take(8)
        return sorted.joinToString("\n") { goal ->
            val daysAgo = ChronoUnit.DAYS.between(goal.startedAt, now).toInt()
            val newTag = if (daysAgo <= 7) " 🆕新增" else ""
            "- (" + daysAgo + "天前立" + newTag + ") 「" + goal.title + "」  " +
                "${goal.priority.emoji}${goal.priority.shortName}优先 · " +
                "${goal.metricDisplay} · 进度${goal.sessionProgressDisplay} · " +
                "${goal.cadence?.displayText ?: "长期"} · ${goal.status.displayText}"
        }
    }

    private fun priorityRank(p: V2Goal.Priority): Int = when (p) {
        V2Goal.Priority.CRITICAL -> 0
        V2Goal.Priority.HIGH -> 1
        V2Goal.Priority.NORMAL -> 2
        V2Goal.Priority.LOW -> 3
    }

    private fun buildPrompt(sections: List<Pair<String, String>>): String {
        val body = sections.joinToString("\n\n") { "${it.first}\n${it.second}" }
        return """
            你是一个高尔夫训练数据分析助手。请基于下面用户的多源数据（最近对话、最近文字笔记、最近视频备忘录、当前目标），综合提炼用户当前的训练情况，用严格 JSON 输出。不要任何额外说明，不要用 markdown 代码块包裹。

            $body

            要求：
            1. 综合所有数据源，提炼用户真实的痛点 / 进步 / 当前关注 / 状态总结 / 标签
            2. 即使对话很少，也要从文字笔记 / 视频备忘录 / 当前目标 里提取有效信息
            3. 优先关注「🆕新增」的目标和最新的笔记/视频
            4. 痛点要尽量具体（"切杆站位太前导致打厚"，而不是"切杆有问题"）

            直接输出以下 JSON（所有字段都是字符串数组，每项不超过 25 字，最多 4 项）：
            {
              "painPoints": ["技术痛点1", "技术痛点2"],
              "progressUpdates": ["进步/变化1"],
              "currentFocus": ["当前训练重点1", "当前训练重点2"],
              "statusSummary": "一句话总结用户当前训练状态（不超过 60 字）",
              "tags": ["#标签1", "#标签2", "#标签3"]
            }

            tags 字段要求：提取核心话题标签，每个以 # 开头，最多 5 个。
            如果某个字段确实没有相关信息，填空数组 [] 或空字符串 ""。
        """.trimIndent()
    }

    private fun parseAndUpdateProfile(jsonString: String): Pair<Int, Boolean> {
        val jsonData = extractJson(jsonString) ?: return 0 to false
        val root = runCatching { Json.parseToJsonElement(jsonData).jsonObject }.getOrNull()
            ?: return 0 to false

        var added = 0
        var summaryChanged = false
        val profile = V2GolfProfileStore.profile.value
        val source = V2ProfileSource.AI_INFERRED

        root["painPoints"]?.jsonArray?.forEach { el ->
            val p = el.jsonPrimitive.content
            if (p.isNotBlank() && profile.painPoints.none { it.content == p }) {
                if (V2GolfProfileStore.addItem("painPoints", p, source, evictOldestAiWhenFull = true)) added++
            }
        }
        root["currentFocus"]?.jsonArray?.forEach { el ->
            val f = el.jsonPrimitive.content
            if (f.isNotBlank() && profile.currentFocus.none { it.content == f }) {
                if (V2GolfProfileStore.addItem("currentFocus", f, source, evictOldestAiWhenFull = true)) added++
            }
        }
        root["progressUpdates"]?.jsonArray?.forEach { el ->
            val s = el.jsonPrimitive.content
            if (s.isNotBlank() && profile.strengths.none { it.content == s }) {
                if (V2GolfProfileStore.addItem("strengths", s, source, evictOldestAiWhenFull = true)) added++
            }
        }
        root["statusSummary"]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() }?.let { summary ->
            V2GolfProfileStore.updateStatusSummary(summary)
            summaryChanged = true
        }
        return added to summaryChanged
    }

    private fun extractJson(text: String): String? {
        var cleaned = text
        val fenceJson = "```json"
        val fence = "```"
        if (cleaned.contains(fenceJson)) {
            val start = cleaned.indexOf(fenceJson) + fenceJson.length
            val end = cleaned.indexOf(fence, start)
            if (end > start) cleaned = cleaned.substring(start, end)
        } else if (cleaned.contains(fence)) {
            val start = cleaned.indexOf(fence) + fence.length
            val end = cleaned.indexOf(fence, start)
            if (end > start) cleaned = cleaned.substring(start, end)
        }
        val startIdx = cleaned.indexOf('{')
        if (startIdx < 0) return null
        var depth = 0
        var inString = false
        var escape = false
        var endIdx = -1
        for (i in startIdx until cleaned.length) {
            val ch = cleaned[i]
            if (escape) {
                escape = false
                continue
            }
            if (ch == '\\') {
                escape = true
                continue
            }
            if (ch == '"') {
                inString = !inString
                continue
            }
            if (inString) continue
            when (ch) {
                '{' -> depth++
                '}' -> {
                    depth--
                    if (depth == 0) {
                        endIdx = i
                        break
                    }
                }
            }
        }
        if (endIdx < 0) return null
        return cleaned.substring(startIdx, endIdx + 1)
    }
}
