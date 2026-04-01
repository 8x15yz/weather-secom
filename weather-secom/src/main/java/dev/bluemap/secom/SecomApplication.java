package dev.bluemap.secom;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * BlueMap SECOM Wrapper 서버
 *
 * 역할: 기상 API(optimal-loads)를 SECOM 표준 인터페이스로 래핑
 * - GET /v1/ping       → 서비스 상태 확인 (MSR이 매일 호출)
 * - GET /v1/capability → 서비스 기능 선언
 * - GET /v1/object     → 기상 데이터 조회 (핵심 엔드포인트)
 */
@SpringBootApplication
public class SecomApplication {

    public static void main(String[] args) {
        SpringApplication.run(SecomApplication.class, args);
    }
}
