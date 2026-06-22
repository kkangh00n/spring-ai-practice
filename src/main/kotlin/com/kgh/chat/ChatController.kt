package com.kgh.chat

import org.springframework.ai.chat.client.ChatClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

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
            .content() // 받아온 응답 중 메타데이터는 버리고, 순수 content만 추출!
    }

}