package com.kgh.chat.tool

import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.ai.tool.annotation.Tool
import org.springframework.ai.tool.annotation.ToolParam
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import org.springframework.web.util.UriBuilder
import java.util.function.Function


@Service
class Tools(restClientBuilder: RestClient.Builder) {

    private val restClient: RestClient = restClientBuilder
        .baseUrl("https://wttr.in")
        .build()

    @Tool(description = "지역 이름을 받아 현재 날씨를 조회합니다.", returnDirect = true)
    fun getWeather(
        @ToolParam(description = "지역 이름") location: String
    ): String? {
        val customFormat = "현재 %l의 날씨는 %c 상태이며, 기온은 %t, 체감 기온은 %f, 풍속은 %w, 습도는 %h, 강수량은 %p입니다"

        return restClient.get()
            .uri(Function { uriBuilder: UriBuilder? ->
                uriBuilder!!
                    .path("/{location}")
                    .queryParam("format", customFormat)
                    .queryParam("lang", "ko")
                    .build(location)
            })
            .retrieve()
            .body(String::class.java)
    }

    @Tool(description = "지역 이름을 받아 현재 3일간의 날씨와 천문 정보(달의 밝기, 달의 위상, 해/달의 뜨고 지는 시각) 를 조회합니다.")
    fun getWeatherDetails(@ToolParam(description = "지역 이름") location: String): WeatherResponse? {
        return restClient.get()
            .uri(Function { uriBuilder: UriBuilder? ->
                uriBuilder!!
                    .path("/{location}")
                    .queryParam("format", "j1") // json 출력으로 제공
                    .queryParam("lang", "ko")
                    .build(location)
            })
            .retrieve()
            .body(WeatherResponse::class.java) // JSON 응답을 즉시 WeatherResponse Record 객체로 맵핑
    }

}

@JvmRecord
data class WeatherResponse(
    val weather: MutableList<WeatherForecast>
)

// 일별 예보 (hourly 제외)
data class WeatherForecast(
    @Schema(description = "천문 정보(일출, 일몰 등)") val astronomy: MutableList<Astronomy>,
    @Schema(description = "해당 날짜(yyyy-MM-dd)") val date: String,
    @Schema(description = "평균 기온(섭씨)") val avgtempC: Int,
    @Schema(description = "평균 기온(화씨)") val avgtempF: Int,
    @Schema(description = "최고 기온(섭씨)") val maxtempC: Int,
    @Schema(description = "최고 기온(화씨)") val maxtempF: Int,
    @Schema(description = "최저 기온(섭씨)") val mintempC: Int,
    @Schema(description = "최저 기온(화씨)") val mintempF: Int,
    @Schema(description = "일조 시간(시간 단위)") val sunHour: Double,
    @Schema(description = "적설량(센티미터)") val totalSnow_cm: Double,
    @Schema(description = "자외선 지수") val uvIndex: Int
)

// 천문 정보
@JvmRecord
data class Astronomy(
    @Schema(description = "달 밝기(%)") val moon_illumination: Int,
    @Schema(description = "달의 위상(예: Full Moon 등)") val moon_phase: String,
    @Schema(description = "달 뜨는 시각") val moonrise: String,
    @Schema(description = "달 지는 시각") val moonset: String,
    @Schema(description = "해 뜨는 시각") val sunrise: String,
    @Schema(description = "해 지는 시각") val sunset: String
)