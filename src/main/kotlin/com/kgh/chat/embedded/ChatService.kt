package com.kgh.chat.embedded

import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.client.ChatClient.AdvisorSpec
import org.springframework.ai.chat.client.ChatClient.ChatClientRequestSpec
import org.springframework.ai.chat.client.advisor.api.Advisor
import org.springframework.ai.chat.memory.ChatMemory
import org.springframework.ai.chat.model.ChatResponse
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

    fun call(prompt: Prompt, conversationId: String): ChatResponse? {
        return prepareRequest(prompt, conversationId)
            .call()
            .chatResponse()
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

    fun csEvaluation(prompt: Prompt, conversationId: String): CsEvaluation? {
        return prepareRequest(prompt, conversationId).call()
            .entity(CsEvaluation::class.java)
    }

}

// 1. 긴급도, 문의 카테고리 Enum 정의
enum class Urgency {
    LOW, NORMAL, HIGH, URGENT
}

enum class Category {
    REFUND, SHIPPING, DEFECT, INQUIRY
}

// 2. 응답 레코드 정의
data class CsEvaluation(
    val category: Category?,
    val urgency: Urgency?,
    val keywords: MutableList<String?>? // 예: ["배송지연", "환불요청", "파손"]
)