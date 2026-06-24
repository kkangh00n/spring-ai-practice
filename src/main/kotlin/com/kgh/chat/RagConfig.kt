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
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
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

    @ConditionalOnProperty(prefix = "app.etl.pipeline", name = ["init"], havingValue = "true")
    @Order(1) // 다른 실행 코드들보다 가장 먼저 파이프라인을 가동하라
    @Bean
    fun initEtlPipeline(
        documentReaders: MutableList<DocumentReader>,  // 1. Extract
        textSplitter: DocumentTransformer,  // 2. Transform
        keywordMetadataEnricher: DocumentTransformer,
        documentWriters: MutableList<DocumentWriter> // 3. Load(콘솔 출력기, VectorDB 등)
    ): ApplicationRunner {

        return ApplicationRunner { args: ApplicationArguments? ->
            println("[System] ETL 파이프라인 가동 시작")

            // 1. 등록된 모든 파일 리더기(Reader)들을 하나씩 꺼내서 실행
            for (reader in documentReaders) {

                // 1. Extract 원본 파일에서 거대한 텍스트 덩어리를 읽어오기
                val rawDocuments = reader.get()
                println("[Extract] 파일 읽기 완료")

                // 2. Transform 읽어온 문서를 AI가 소화하기 좋게 조각조각(Chunk) 자르기
                var chunkedDocuments = textSplitter.apply(rawDocuments)
                println("[Transform] 문서 분할 완료")

                // 키워드 추출기 이어서 적용
                chunkedDocuments = keywordMetadataEnricher.apply(chunkedDocuments)

                // 3. Load  가공된 문서 조각들을 준비된 모든 저장소에 집어넣음
                for (writer in documentWriters) {
                    writer.accept(chunkedDocuments)
                }
                println("[Load] 저장소 적재 완료")
            }
            println("[System] ETL 파이프라인 적재 종료")
        }
    }

}