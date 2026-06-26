package com.kgh.chat.tool

import com.drew.lang.annotations.Nullable
import io.swagger.v3.oas.annotations.media.Schema
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
import org.springframework.context.annotation.Profile
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux

@RestController
@RequestMapping("/tool")
@Profile("tool")
class ToolController(private val toolService: ToolService) {
    @JvmRecord
    data class PromptBody(
        @field:Schema(
            description = "대화 식별자",
            example = "jscode-1234"
        ) @param:Schema(
            description = "대화 식별자",
            example = "jscode-1234"
        ) val conversationId: @NotEmpty String?,
        @field:Schema(description = "사용자 입력 프롬프트", example = "안녕하세요, 제주도 날씨 어때요?") @param:Schema(
            description = "사용자 입력 프롬프트",
            example = "안녕하세요, 제주도 날씨 어때요?"
        ) val userPrompt: @NotEmpty String?,
        @field:Schema(
            description = "시스템 프롬프트(선택)",
            example = "You are a helpful assistant."
        ) @field:Nullable @param:Nullable @param:Schema(
            description = "시스템 프롬프트(선택)",
            example = "You are a helpful assistant."
        ) val systemPrompt: String?,
        @field:Schema(
            description = "채팅 옵션(선택)",
            implementation = DefaultChatOptions::class
        ) @field:Nullable @param:Nullable @param:Schema(
            description = "채팅 옵션(선택)",
            implementation = DefaultChatOptions::class
        ) val chatOptions: DefaultChatOptions?
    )

    @PostMapping(value = ["/call"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun call(@RequestBody @Valid promptBody: PromptBody): ChatResponse? {
        val prompt = createPrompt(promptBody)

        // 완성된 프롬프트와 conversationID를 가지고 서비스 메소드 호출
        return toolService.call(promptBody.conversationId!!, prompt)
    }

    @PostMapping(value = ["/stream"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun stream(@RequestBody @Valid promptBody: PromptBody): Flux<String> {
        val prompt = createPrompt(promptBody)
        return toolService.stream(promptBody.conversationId!!, prompt)
    }

    companion object {
        private fun createPrompt(promptBody: PromptBody): Prompt {
            val messages: MutableList<Message> = ArrayList()

            if (promptBody.systemPrompt != null && !promptBody.systemPrompt.isBlank()) {
                messages.add(SystemMessage(promptBody.systemPrompt))
            }
            messages.add(UserMessage(promptBody.userPrompt))

            val promptBuilder = Prompt.builder().messages(messages)

            if (promptBody.chatOptions != null) {
                promptBuilder.chatOptions(promptBody.chatOptions)
            }
            return promptBuilder.build()
        }
    }
}