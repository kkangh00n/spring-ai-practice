package com.kgh.chat.tool

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
import org.springframework.context.annotation.Profile
import java.util.Scanner

@Configuration
@Profile("tool")
class ToolChatConfig {

    @Bean
    fun simpleLoggerAdvisor(): SimpleLoggerAdvisor {
        // builder().build() 패턴을 사용하여 로거 객체 생성
        return SimpleLoggerAdvisor.builder()
            .build()
    }

    @Bean
    fun chatMemory(): ChatMemory {
        return MessageWindowChatMemory.builder()
            //10개의 대화 저장
            .maxMessages(10)
            .build()
    }

    @Bean
    fun messageChatMemoryAdvisor(chatMemory: ChatMemory): MessageChatMemoryAdvisor {
        return MessageChatMemoryAdvisor.builder(chatMemory).build()
    }


    @ConditionalOnProperty(prefix = "app.cli", name = ["enabled"], havingValue = "true")
    @Bean
    fun cli(
        toolService: ToolService,
        @Value("\${spring.application.name}") applicationName: String?,
        @Value("\${app.cli.filter-expression:}") filterExpression: String?
    ): CommandLineRunner {
        return CommandLineRunner { _ ->
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

                    if (userMessage.equals("exit", ignoreCase = true) || userMessage.equals(
                            "quit",
                            ignoreCase = true
                        )
                    ) {
                        println("대화를 종료합니다. 안녕히 계세요!")
                        break
                    }

                    print("ASSISTANT TOOL: ")

                    val chatStream: Iterable<String?> = toolService.stream(
                        "cli",
                        Prompt(userMessage)
                    ).toIterable()

                    for (token in chatStream) {
                        print(token)
                    }
                    println()
                }
            }
        }
    }

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