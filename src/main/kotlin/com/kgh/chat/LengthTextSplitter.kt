package com.kgh.chat

import org.springframework.ai.transformer.splitter.TextSplitter
import org.springframework.util.StringUtils
import kotlin.math.min

/**
 * 글자수 기반 슬라이딩 윈도우 텍스트 분할기
 * 긴 문서를 지정된 글자수(chunkSize)만큼 자르되,
 * 문맥이 끊기지 않도록 이전 조각과 일정 부분을 겹치게(chunkOverlap) 자르는 클래스
 */
class LengthTextSplitter(
    // 한 조각(Chunk)의 최대 글자 수 (예: 1000자)
    private val chunkSize: Int,
    // 다음 조각과 겹치게 할 글자 수 (예: 200자)
    private val chunkOverlap: Int
) : TextSplitter() {

    override fun splitText(text: String): MutableList<String> {
        // "안녕하세요반갑습니다"(10글자), chunk:5, over:2

        // 1. 잘라낼 조각들을 담을 빈 리스트 생성
        val chunks: MutableList<String> = ArrayList()

        // 2. 만약 넘어온 텍스트가 비어있거나, 공백뿐이라면 자를게 없으므로 그대로 리턴
        if (!StringUtils.hasText(text)) {
            return chunks
        }

        val textLength = text.length // 10글자

        // 3. 자르기 시작할 '시작점(인덱스)'를 0으로 세팅
        var chunkStart = 0

        // 4. 끝까지 반복해서 자르기
        while (chunkStart < textLength) {
            // [끝점 계산] 자를 조각의 끝점 계산
            // (시작점 + 자를 크기)를 하되, 만약 남은 글자가 부족해서 전체 길이를 넘어가 버리면 전체 길이에서 딱 멈추기

            val chunkEnd = min(chunkStart + chunkSize, textLength) // 5,10중에 작은값 5 /  8,10 중에 작은값 8

            // [자르기]
            val slicedText = text.substring(chunkStart, chunkEnd) // "안녕하세요", "세요반갑습"
            chunks.add(slicedText)

            // [다음 시작점 계산]
            // 방금 자른 조각의 '끝점'에서 '겹칠 크기(Overlap)'만큼 뒤로 되돌아간 곳이 다음 번 시작점이 됨
            // 이렇게 해야 다음 조각을 자를 때 이전 조각의 뒷부분이 자연스럽게 포함됨.
            val nextStart = chunkEnd - chunkOverlap // 3

            // 남은 텍스트가 너무 짧거나 설정 오류로 인해 다음 시작점이 제자리에 머물거나 뒤로 밀리면 무한루프에 빠짐.
            // 이를 방지하기 위해 강제로 반복문을 탈출
            if (nextStart <= chunkStart) {
                break
            }

            // [위치 이동] 시작점을 방금 계산한 다음 시작점으로 이동시키고 다시 루프를 돌기
            chunkStart = nextStart // 3
        }

        // 5. 리스트 반환
        return chunks
    }
}