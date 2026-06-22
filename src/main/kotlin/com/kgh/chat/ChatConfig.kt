package com.kgh.chat

import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor
import org.springframework.ai.chat.memory.ChatMemory
import org.springframework.ai.chat.memory.MessageWindowChatMemory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration


@Configuration
class ChatConfig {

    /**
     * Advisor란?
     * AI 모델과 주고받는 요청과 응답을 중간에서 가로채 데이터를 동적으로 수정, 확장, 차단할 수 있게 해주는 컴포넌트
     * 스택 구조로 동작
     * - 우선순위가 높은(Order 값이 작은) Advisor가 가장 먼저 요청을 가로채서 전처리함
     * - 우선순위가 높은(Order 값이 작은) Advisor가 가장 마지막에 응답을 가로채서 후처리함
     */

    //1. 로깅 관련 어드바이저 구성 (우선순위 0)
    @Bean
    fun simpleLoggerAdvisor(): SimpleLoggerAdvisor {
        // builder().build() 패턴을 사용하여 로거 객체 생성
        return SimpleLoggerAdvisor.builder()
            .build()
    }

    //대화 히스토리를 저장하는 메모리
    @Bean
    fun chatMemory(): ChatMemory {
        return MessageWindowChatMemory.builder()
            //10개의 대화 저장
            .maxMessages(10)
            .build()
    }

    //2. 우리가 짧은 질문만 던지더라도,
    // 해당 Advisor가 대답을 가로채서 기존의 대화 기록(ChatMemory)을 끼워 넣은 다음, 최종 프롬프트를 AI에게 대신 전달해줌
    @Bean
    fun messageChatMemoryAdvisor(chatMemory: ChatMemory): MessageChatMemoryAdvisor {
        return MessageChatMemoryAdvisor.builder(chatMemory).build()
    }

}