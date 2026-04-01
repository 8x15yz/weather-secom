package dev.bluemap.ecs.secom.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * MetaClient
 *
 * weather-secom의 메타 엔드포인트를 호출하는 클라이언트.
 *
 * GET /api/meta/sources   → sources 목록
 * GET /api/meta/variables → variables 목록
 *
 * weather-secom base URL에서 /api/secom 제거한 주소 사용
 * (메타 엔드포인트는 SECOM 표준 경로가 아닌 last mile 영역)
 */
@Component
@Slf4j
public class MetaClient {

    private final WebClient webClient;

    public MetaClient(@Value("${weather.secom.url}") String weatherSecomUrl) {
        // weather.secom.url = http://3.38.61.108:8766/api/secom
        // 메타 API는 /api/meta 경로 → base URL에서 /api/secom 제거
        String baseUrl = weatherSecomUrl.replace("/api/secom", "");
        this.webClient = WebClient.builder().baseUrl(baseUrl).build();
        log.info("MetaClient 초기화: {}", baseUrl);
    }

    /**
     * sources 목록 조회
     * GET /api/meta/sources
     */
    public String getSources() {
        log.info("weather-secom sources 요청");
        try {
            return webClient.get()
                    .uri("/api/meta/sources")
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
        } catch (Exception e) {
            log.error("sources 조회 실패: {}", e.getMessage());
            throw new RuntimeException("sources 조회 실패: " + e.getMessage(), e);
        }
    }

    /**
     * variables 목록 조회
     * GET /api/meta/variables?source={source}
     */
    public String getVariables(String source) {
        log.info("weather-secom variables 요청: source={}", source);
        try {
            return webClient.get()
                    .uri(uriBuilder -> {
                        var builder = uriBuilder.path("/api/meta/variables");
                        if (source != null && !source.isBlank()) {
                            builder.queryParam("source", source);
                        }
                        return builder.build();
                    })
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
        } catch (Exception e) {
            log.error("variables 조회 실패: {}", e.getMessage());
            throw new RuntimeException("variables 조회 실패: " + e.getMessage(), e);
        }
    }
}