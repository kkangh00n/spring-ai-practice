package com.kgh.chat

import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.model.ChatResponse
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux

@RestController
class ChatController(
    chatClientBuilder: ChatClient.Builder
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

}
