package com.kgh.chat

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.ai.chat.model.ChatModel
import org.springframework.ai.document.Document
import org.springframework.ai.document.DocumentReader
import org.springframework.ai.document.DocumentTransformer
import org.springframework.ai.document.DocumentWriter
import org.springframework.ai.model.transformer.KeywordMetadataEnricher
import org.springframework.ai.reader.tika.TikaDocumentReader
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.Resource
import org.springframework.core.io.support.PathMatchingResourcePatternResolver


@Configuration
class RagConfig {

    @Bean
    fun documentReaders(
        @Value("\${app.rag.documents-location-pattern}") documentsLocationPattern: String
    ): MutableList<DocumentReader> {
        // 1. 설정한 경로 패턴에 맞는 파일 찾기
        val resolver = PathMatchingResourcePatternResolver()
        val resources: Array<Resource> = resolver.getResources(documentsLocationPattern)

        // 2. 찾아온 파일들을 담을 빈 리스트 생성
        val readers: MutableList<DocumentReader> = ArrayList()

        // 3. 파일의 개수만큼 for문을 돌면서 tika로 예쁘게 포장해서 리스트에 넣기
        for (resource in resources) {
            readers.add(TikaDocumentReader(resource))
        }

        // 4. 리스트 반환
        return readers
    }

    @Bean
    fun textSplitter(): DocumentTransformer {
        return LengthTextSplitter(200, 100)
    }

    @Bean
    fun keywordMetadataEnricher(chatModel: ChatModel): KeywordMetadataEnricher {
        return KeywordMetadataEnricher(chatModel, 4)
    }

    @Bean
    fun jsonConsoleDocumentWriter(objectMapper: ObjectMapper): DocumentWriter {
        // 앞 단계에서 가공되어 넘어온 문서 조각 리스트(documents)를 받아서 로직실행
        return DocumentWriter { documents: MutableList<Document> ->

            // 1. 현재 들어온 총 문서 조각(Chunk)의 개수가 몇 개인지 콘솔에 명확하게 표기
            println("======= 저장할 문서 조각(Chunk) 개수: " + documents.size + " ========")
            try {
                // 들여쓰기와 줄바꿈 적용된 예쁜 JSON 문자열로 출력
                val jsonString: String? =
                    objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(documents)

                println(jsonString)

            } catch (e: Exception) {
                println("JSON 변환 중 에러가 발생했습니다: " + e.message)
            }
            println("======================================================")
        }
    }

}