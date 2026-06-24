package com.kgh.chat.rag

import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.client.ChatClient.AdvisorSpec
import org.springframework.ai.chat.client.ChatClient.ChatClientRequestSpec
import org.springframework.ai.chat.client.advisor.api.Advisor
import org.springframework.ai.chat.memory.ChatMemory
import org.springframework.ai.chat.model.ChatResponse
import org.springframework.ai.chat.prompt.ChatOptions
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux


@Service
class RagChatService(
    chatClientBuilder: ChatClient.Builder,
    advisors: Array<Advisor>
) {

    private val chatClient: ChatClient =
        chatClientBuilder
            //사실적인 답변을 위한 temperature 설정
            .defaultOptions(ChatOptions.builder().temperature(0.0))
            .defaultAdvisors(*advisors)
            .build()

    // 응답을 받아오는 코드 추가
    fun stream(prompt: Prompt, conversationId: String, filterExpressionAsOpt: String?): Flux<String> {
        return prepareRequest(prompt, conversationId, filterExpressionAsOpt)
            .stream()
            .content()
    }

    fun call(prompt: Prompt, conversationId: String, filterExpressionAsOpt: String?): ChatResponse? {
        return prepareRequest(prompt, conversationId, filterExpressionAsOpt)
            .call()
            .chatResponse()
    }


    private fun prepareRequest(
        prompt: Prompt,
        conversationId: String,
        filterExpressionAsOpt: String?
    ): ChatClientRequestSpec {
        return chatClient.prompt(prompt)
            //설정한 어드바이저의 설정된 ChatMemory의 conversationId를 설정
            .advisors { advisorSpec: AdvisorSpec? ->
                advisorSpec!!.param(
                    ChatMemory.CONVERSATION_ID,
                    conversationId
                )
            }
            /**
             * FilterExpression 적용
             */
            .advisors { advisorSpec: AdvisorSpec? ->
                advisorSpec!!.param(
                    VectorStoreDocumentRetriever.FILTER_EXPRESSION,
                    filterExpressionAsOpt ?: ""
                )
            }
    }

}
