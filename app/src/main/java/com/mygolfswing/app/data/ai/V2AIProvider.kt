package com.mygolfswing.app.data.ai

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * AI 教练 Provider 抽象（对齐 iOS `AIService`）。
 */
interface V2AIProvider {
    val providerName: String
    val modelName: String

    /** 流式返回回答片段（按字符 / 句子 chunked） */
    fun streamReply(prompt: String, history: List<Pair<String, String>>): Flow<String>
}

/**
 * 默认内置 mock provider（无 API Key 即可演示）。
 *
 * 后续可以再加 `OpenAIProvider` / `DeepSeekProvider` 实现，调真接口。
 */
object V2AIMockProvider : V2AIProvider {
    override val providerName = "挥呗 AI"
    override val modelName = "demo-v1"

    private val swingReplies = listOf(
        "好的，根据你的描述，建议先把握把压力降到 6 分力。手腕越紧、节奏越僵，下杆容易发飘。",
        "可以试试「1234」节奏数：上杆 1-2，停顿 3，下杆 4。这能帮你建立稳定的杆头释放点。",
        "如果你今天感觉杆面打开多，试着把双手握把往里旋一指节，看看转身后的杆面位置。",
    )
    private val puttingReplies = listOf(
        "推杆距离感的关键是「钟摆 + 视线下方目标」。先把杆面朝向目标，再让肩膀像钟摆一样均匀摆动。",
        "5 米左右，建议 30% 用力打底 + 70% 滚动延伸。试 10 球，记下你今天的均速。",
    )
    private val sandReplies = listOf(
        "沙坑救球先把杆面打开 30°，瞄球后约 5cm 落地。挥杆速度比力度更重要。",
        "脚下站稳一些，重心放左脚 60%。下杆时让杆头从沙底掠过球，不是直接击球。",
    )

    override fun streamReply(prompt: String, history: List<Pair<String, String>>): Flow<String> = flow {
        delay(180)
        val pool = when {
            prompt.contains("推") || prompt.contains("putt", ignoreCase = true) -> puttingReplies
            prompt.contains("沙") -> sandReplies
            else -> swingReplies
        }
        val full = pool.random()
        // 按字符流式吐
        val chars = full.toCharArray()
        val buffer = StringBuilder()
        for (c in chars) {
            buffer.append(c)
            emit(c.toString())
            delay(if (c == '。' || c == '，' || c == '\n') 90 else 28)
        }
    }
}
