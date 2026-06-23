package com.kgh.chat

import jakarta.validation.Valid
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.messages.Message
import org.springframework.ai.chat.messages.SystemMessage
import org.springframework.ai.chat.messages.UserMessage
import org.springframework.ai.chat.model.ChatResponse
import org.springframework.ai.chat.prompt.ChatOptions
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux

@RestController
class ChatController(
    chatClientBuilder: ChatClient.Builder,
    private val chatService: ChatService
) {

    private val chatClient: ChatClient = chatClientBuilder.build()

    @GetMapping("/ai")
    fun generation(userPrompt: String): String? {
        return chatClient.prompt()
            .user(userPrompt)
            .call()
            .content() // 0. 받아온 응답 중 메타데이터는 버리고, 순수 content만 추출!
//            .chatResponse() // 1. ChatResponse 객체로 받기
//            .entity() // 2. Entity로 받기
    }

    @GetMapping("/streaming", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun stream(userPrompt: String): Flux<String> {
        return chatClient.prompt()
            .user(userPrompt)
            .stream()
            .content()
    }

    @PostMapping("/call", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun call(
        @RequestBody @Valid promptBody: PromptBody
    ): ChatResponse {

        val prompt: Prompt = createPrompt(promptBody)

        return chatService.call(prompt, promptBody.conversationId)!!
    }

    @PostMapping("/stream", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun stream(@RequestBody promptBody: @Valid PromptBody): Flux<String> {
        val prompt = createPrompt(promptBody)
        return chatService.stream(prompt, promptBody.conversationId)
    }

    private fun createPrompt(promptBody: PromptBody): Prompt {
        // 1. 메시지들을 차곡차곡 담을 빈 List를 생성
        val messages: MutableList<Message> = ArrayList()

        // 2. systemPrompt가 입력으로 들어왔다면 리스트에 넣자
        if (promptBody.systemPrompt != null && !promptBody.systemPrompt.isBlank()) {
            messages.add(SystemMessage(promptBody.systemPrompt))
        }

        // 3. userPrompt는 필수 값이니 무조건 리스트에 넣자
        messages.add(UserMessage(promptBody.userPrompt))

        // 4. 리스트에 담긴 메시지들로 프롬프트 조립하기
        val promptBuilder: Prompt.Builder = Prompt.builder().messages(messages)

        // 5. 프론트엔드에서 보낸 chatOptions가 있다면 적용하기
        if (promptBody.chatOptions != null) {
            promptBuilder.chatOptions(promptBody.chatOptions.toChatOptions())
        }

        return promptBuilder.build()
    }

}

data class PromptBody(
    val conversationId: String,
    val userPrompt: String,
    val systemPrompt: String?,
    val chatOptions: ChatOptionsRequest?
)

// JSON 요청 바디 → ChatOptions 변환용 DTO
// (DefaultChatOptions는 기본 생성자가 없어 Jackson이 직접 역직렬화할 수 없음)
data class ChatOptionsRequest(
    val model: String? = null,
    val frequencyPenalty: Double? = null,
    val maxTokens: Int? = null,
    val presencePenalty: Double? = null,
    val stopSequences: List<String>? = null,
    val temperature: Double? = null,
    val topK: Int? = null,
    val topP: Double? = null
) {
    fun toChatOptions(): ChatOptions =
        ChatOptions.builder()
            .apply {
                model?.let { model(it) }
                frequencyPenalty?.let { frequencyPenalty(it) }
                maxTokens?.let { maxTokens(it) }
                presencePenalty?.let { presencePenalty(it) }
                stopSequences?.let { stopSequences(it) }
                temperature?.let { temperature(it) }
                topK?.let { topK(it) }
                topP?.let { topP(it) }
            }
            .build()
}
