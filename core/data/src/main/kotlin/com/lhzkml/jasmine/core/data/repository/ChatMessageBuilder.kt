package com.lhzkml.jasmine.core.data.repository

import com.lhzkml.jasmine.core.data.model.SimpleChatMessage
import com.lhzkml.jasmine.core.domain.repository.SkillManager
import com.lhzkml.jasmine.core.prompt.llm.SystemPromptManager
import com.lhzkml.jasmine.core.prompt.model.ChatMessage
import com.lhzkml.jasmine.core.prompt.model.ToolCall
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatMessageBuilder @Inject constructor() {

    private val systemPromptManager = SystemPromptManager()

    suspend fun buildApiMessagesWithSystemPrompt(
        messages: List<SimpleChatMessage>,
        providerRepo: ChatProviderRepository,
        skillManager: SkillManager,
        uiEnabled: Boolean = true,
        webSearchEnabled: Boolean = true,
    ): List<ChatMessage> {
        val result = mutableListOf<ChatMessage>()

        val id = providerRepo.getActiveProviderId() ?: ""
        val customPrompt = providerRepo.getSystemPrompt(id)
        var systemContent = systemPromptManager.createSystemMessage(
            if (customPrompt.isNotBlank()) customPrompt else null,
            uiEnabled = uiEnabled,
        ).content

        // Inject selected skill instructions
        val skillsInstructions = skillManager.getSelectedSkillsInstructions()
        if (skillsInstructions != "No active skills.") {
            systemContent += "\n\n<available_skills>\n" +
                "用户已启用以下技能。当用户请求涉及这些技能的功能时，你应使用 manage_skills 工具加载技能，然后按技能指令调用 run_js 或 run_intent 工具执行。\n" +
                skillsInstructions +
                "\n</available_skills>"
        }

        // Inject web search guidance
        if (webSearchEnabled) {
            systemContent += "\n\n<web_search>\n" +
                "你可以使用 web_search 工具获取网络实时信息。当用户询问近期事件、新闻、最新数据或需要查询实时信息时，主动调用 web_search。\n" +
                "搜索时使用精准的关键词，优先使用用户消息中的原词。对于需要最新信息的查询（日期、价格、版本号等），务必搜索确认。\n" +
                "</web_search>"
        }

        result.add(ChatMessage.system(systemContent))

        for (msg in messages) {
            result.add(toApiMessage(msg))
        }

        return result
    }

    fun toApiMessage(msg: SimpleChatMessage): ChatMessage {
        return when (msg.role) {
            "user" -> ChatMessage.user(msg.content)
            "assistant" -> {
                if (!msg.toolCalls.isNullOrEmpty()) {
                    val toolCalls = msg.toolCalls.map {
                        ToolCall(
                            id = it.id,
                            name = it.name,
                            arguments = it.arguments,
                        )
                    }
                    ChatMessage.assistantWithToolCalls(toolCalls, msg.content)
                } else {
                    ChatMessage.assistant(msg.content)
                }
            }
            "tool" -> ChatMessage(
                role = "tool",
                content = msg.content,
                toolCallId = msg.toolCallId,
                toolName = msg.toolName,
            )
            "system" -> ChatMessage.system(msg.content)
            else -> ChatMessage.user(msg.content)
        }
    }
}
