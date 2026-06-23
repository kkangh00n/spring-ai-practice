package com.kgh.chat

import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.client.ChatClient.AdvisorSpec
import org.springframework.ai.chat.client.ChatClient.ChatClientRequestSpec
import org.springframework.ai.chat.client.advisor.api.Advisor
import org.springframework.ai.chat.memory.ChatMemory
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux


@Service
class ChatService(
    chatClientBuilder: ChatClient.Builder,
    advisors: Array<Advisor>
) {

    private val chatClient: ChatClient = chatClientBuilder.defaultAdvisors(*advisors).build()

    // 응답을 받아오는 코드 추가
    fun stream(prompt: Prompt, conversationId: String): Flux<String> {
        return prepareRequest(prompt, conversationId)
            .stream()
            .content()
    }


    private fun prepareRequest(prompt: Prompt, conversationId: String): ChatClientRequestSpec {
        return chatClient.prompt(prompt)
            //설정한 어드바이저의 설정된 ChatMemory의 conversationId를 설정
            .advisors { advisorSpec: AdvisorSpec? ->
                advisorSpec!!.param(
                    ChatMemory.CONVERSATION_ID,
                    conversationId
                )
            }
    }

}