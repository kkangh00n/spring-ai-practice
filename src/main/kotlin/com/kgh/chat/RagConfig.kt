package com.kgh.chat

import org.springframework.ai.document.DocumentReader
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
}