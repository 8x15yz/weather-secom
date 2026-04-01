package dev.bluemap.ecs.secom;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * optimal-ecs-secom - SECOM Consumer Terminal
 *
 * 역할: Web-ECDIS와 weather-secom 사이의 SECOM 소비자 터미널
 *
 * 통신 흐름:
 *   Web-ECDIS
 *     → GET /api/ecs/weather?geometry=POINT(129.0 35.0)
 *     → [EcsSecomApplication]
 *         → SecomClient.get() → weather-secom /api/secom/v1/object
 *         ← GetResponseObject (Base64 디코딩된 기상 JSON 포함)
 *     ← 기상 JSON 반환
 *     → Web-ECDIS 지도에 표출
 *
 * 나중에 MSR 연동 시:
 *   weather.secom.url 하드코딩 → MSR API 검색으로 교체
 */
@SpringBootApplication
public class EcsSecomApplication {
    public static void main(String[] args) {
        SpringApplication.run(EcsSecomApplication.class, args);
    }
}
