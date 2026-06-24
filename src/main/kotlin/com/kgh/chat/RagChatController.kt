package com.kgh.chat

import jakarta.validation.Valid
import jakarta.validation.constraints.NotEmpty
import org.springframework.ai.chat.messages.Message
import org.springframework.ai.chat.messages.SystemMessage
import org.springframework.ai.chat.messages.UserMessage
import org.springframework.ai.chat.model.ChatResponse
import org.springframework.ai.chat.prompt.DefaultChatOptions
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux


@RestController
@RequestMapping("/rag")
class RagChatController(private val ragChatService: RagChatService) {

    @PostMapping(value = ["/call"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun call(@RequestBody @Valid ragPromptBody: RagPromptBody): ChatResponse? {
        val prompt = createPrompt(ragPromptBody)

        // 완성된 프롬프트와 conversationID를 가지고 서비스 메서드 호출
        return ragChatService.call(
            prompt,
            ragPromptBody.conversationId!!,
            ragPromptBody.filterExpression
        )
    }

    @PostMapping(value = ["/stream"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun stream(@RequestBody @Valid ragPromptBody: RagPromptBody): Flux<String> {
        val prompt = createPrompt(ragPromptBody)
        return ragChatService.stream(
            prompt,
            ragPromptBody.conversationId!!,
            ragPromptBody.filterExpression
        )
    }

    companion object {
        private fun createPrompt(ragPromptBody: RagPromptBody): Prompt {
            // 1. 메시지들을 차곡차곡 담을 빈 List를 생성
            val messages: MutableList<Message> = ArrayList()

            // 2. systemPrompt가 입력으로 들어왔다면 리스트에 넣자
            if (ragPromptBody.systemPrompt != null && !ragPromptBody.systemPrompt.isBlank()) {
                messages.add(SystemMessage(ragPromptBody.systemPrompt))
            }

            // 3. userPrompt는 필수 값이니 무조건 리스트에 넣자
            messages.add(UserMessage(ragPromptBody.userPrompt))

            // 4. 리스트에 담긴 메시지들로 프롬프트 조립하기
            val promptBuilder = Prompt.builder().messages(messages)

            // 5. 프론트엔드에서 보낸 chatOptions가 있다면 적용하기
            if (ragPromptBody.chatOptions != null) {
                promptBuilder.chatOptions(ragPromptBody.chatOptions)
            }
            return promptBuilder.build()
        }
    }
}

data class RagPromptBody(
    val conversationId: @NotEmpty String?,
    val userPrompt: @NotEmpty String?,
    val systemPrompt: String?,
    val chatOptions: DefaultChatOptions?,
    val filterExpression: String?
)