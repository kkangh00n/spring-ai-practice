package com.kgh.chat

import ch.qos.logback.classic.LoggerContext
import org.slf4j.LoggerFactory
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor
import org.springframework.ai.chat.memory.ChatMemory
import org.springframework.ai.chat.memory.MessageWindowChatMemory
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.ai.document.Document
import org.springframework.ai.rag.Query
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter
import org.springframework.ai.rag.postretrieval.document.DocumentPostProcessor
import org.springframework.ai.rag.preretrieval.query.expansion.MultiQueryExpander
import org.springframework.ai.rag.preretrieval.query.transformation.TranslationQueryTransformer
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever
import org.springframework.ai.vectorstore.VectorStore
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.*


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


    /**
     * 1. 새로운 CLI 설정
     */
    @ConditionalOnProperty(prefix = "app.cli", name = ["enabled"], havingValue = "true")
    @Bean // 스프링 부트 서버가 완전히 켜지기 전에 단 한번 자동으로 실행
    fun cli(
        ragChatService: RagChatService,
        @Value("\${spring.application.name}") applicationName: String?,
        @Value("\${app.cli.filter-expression:}") filterExpression: String?
    ): CommandLineRunner {
        return CommandLineRunner { _ ->
            // 1. 스프링 기본 로그 끄기 (채팅에 방해되지 않도록)
            val context: LoggerContext = LoggerFactory.getILoggerFactory() as LoggerContext
            context.getLogger("ROOT").detachAppender("CONSOLE")

            println("=======================================")
            println("🤖 [" + applicationName + "] CLI 챗봇을 시작합니다!")
            println("   (종료하려면 'exit' 또는 'quit' 입력)")
            println("=======================================")
            Scanner(System.`in`).use { scanner ->
                while (true) {
                    print("\nUSER: ")
                    val userMessage: String = scanner.nextLine()

                    // 2. 대화 종료 조건 (무한 루프 탈출)
                    if (userMessage.equals("exit", ignoreCase = true) || userMessage.equals(
                            "quit",
                            ignoreCase = true
                        )
                    ) {
                        println("대화를 종료합니다. 안녕히 계세요!")
                        break
                    }

                    print("ASSISTANT: ")

                    // 3.스트리밍 처리 (핵심 변경 포인트)
                    // Flux(스트림)를 toIterable()로 바꾸면 일반적인 for-each 문으로 한 글자씩 꺼내 쓸 수 있음!
                    val chatStream: Iterable<String?> = ragChatService.stream(
                        Prompt(userMessage),
                        "cli",
                        // filterExpression의 값이 있을때만 통과
                        filterExpression?.takeIf { it.isNotBlank() }
                    ).toIterable()

                    for (token in chatStream) {
                        print(token) // 한 글자씩 화면에 출력 (타이핑 효과)
                    }
                    println() // AI 대답이 끝나면 줄바꿈 한 번
                }
            }
        }
    }

    /**
     * 2. 후처리기
     */
    @ConditionalOnProperty(prefix = "app.cli", name = ["enabled"], havingValue = "true")
    @Bean
    fun printDocumentsPostProcessor(): DocumentPostProcessor {
        return DocumentPostProcessor { query: Query?, documents: MutableList<Document>? ->
            println("\n[ Search Results ]")
            println("===============================================")

            if (documents == null || documents.isEmpty()) {
                println("  No search results found.")
                println("===============================================")
                return@DocumentPostProcessor documents!!
            }

            for (i in documents.indices) {
                val document: Document = documents.get(i)
                System.out.printf("▶ %d Document, Score: %.2f%n", i + 1, document.score)
                println("-----------------------------------------------")
                document.text?.split("\n")?.forEach { line -> println(line) }
                println("===============================================")
            }
            print("\n[ RAG 사용 응답 ]\n\n")
            documents
        }
    }

    /**
     * 후처리기 결합
     */
    @Bean
    fun retrievalAugmentationAdvisor(
        vectorStore: VectorStore,
        chatClientBuilder: ChatClient.Builder,
        @Autowired(required = false) documentPostProcessor: DocumentPostProcessor?
    ): RetrievalAugmentationAdvisor {
        // 1. 문서 검색기 도구 설정

        val documentRetriever = VectorStoreDocumentRetriever.builder()
            .vectorStore(vectorStore)
            .similarityThreshold(0.3)
            .topK(3)
            .build()

        // 2. 다른 구성 요소들 생성
        val queryAugmenter = ContextualQueryAugmenter.builder()
            .allowEmptyContext(true)
            .build()

        val queryExpander = MultiQueryExpander.builder()
            .chatClientBuilder(chatClientBuilder)
            .build()

        val queryTransformer = TranslationQueryTransformer.builder()
            .chatClientBuilder(chatClientBuilder)
            .targetLanguage("korean")
            .build()

        // 3. 최종 합체 (RetrievalAugmentationAdvisor 빌더에서 후처리기 등록)
        val builder = RetrievalAugmentationAdvisor.builder()
            .documentRetriever(documentRetriever)
            .queryAugmenter(queryAugmenter)
            .queryExpander(queryExpander)
            .queryTransformers(queryTransformer)

        // 후처리기가 존재한다면 advisor 빌더에 추가
        if (documentPostProcessor != null) {
            builder.documentPostProcessors(documentPostProcessor)
        }

        return builder.build()
    }

}