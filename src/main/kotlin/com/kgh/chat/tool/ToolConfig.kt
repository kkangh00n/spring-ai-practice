package com.kgh.chat.tool

import org.springframework.ai.model.tool.ToolCallingManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * - AI가 날씨 질문을 받게 되면 “`getWeather()` 실행해 줘” 하고 JSON 형태의 데이터를 던짐
 * - `ToolCallingManager`가 그 데이터를 낚아챔
 */
@Configuration
class ToolConfig {
    @Bean
    fun toolCallingManager(): ToolCallingManager {
        return ToolCallingManager.builder().build()
    }
}
