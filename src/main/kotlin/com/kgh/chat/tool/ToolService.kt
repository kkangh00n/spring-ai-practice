package com.kgh.chat.tool

import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.client.ChatClient.AdvisorSpec
import org.springframework.ai.chat.client.ChatClient.ChatClientRequestSpec
import org.springframework.ai.chat.client.advisor.api.Advisor
import org.springframework.ai.chat.memory.ChatMemory
import org.springframework.ai.chat.model.ChatResponse
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.ai.model.tool.ToolCallingChatOptions
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux

@Service
class ToolService(
    chatClientBuilder: ChatClient.Builder,
    advisors: Array<Advisor>,
    @Value("\${app.chat.default-system-prompt:}") systemPrompt: String,
    tools: Tools
) {
    private val chatClient: ChatClient = chatClientBuilder
        .defaultSystem(systemPrompt)
        .defaultTools(tools)
        .defaultOptions(
            ToolCallingChatOptions.builder()
                .temperature(0.2).build().mutate()
        )
        .defaultAdvisors(*advisors)
        .build()

    private fun createRequest(conversationId: String, prompt: Prompt): ChatClientRequestSpec {
        return chatClient.prompt(prompt)
            .advisors { advisorSpec: AdvisorSpec? ->
                advisorSpec!!.param(
                    ChatMemory.CONVERSATION_ID,
                    conversationId
                )
            }
    }

    fun stream(conversationId: String, prompt: Prompt): Flux<String> {
        return createRequest(conversationId, prompt).stream().content()
    }

    fun call(conversationId: String, prompt: Prompt): ChatResponse? {
        return createRequest(conversationId, prompt).call().chatResponse()
    }
}