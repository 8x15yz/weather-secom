package dev.bluemap.ecs.secom.controller;

import dev.bluemap.ecs.secom.client.MetaClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * MetaController
 *
 * Web-ECDIS가 호출하는 메타정보 엔드포인트.
 *
 * GET /api/sources            → weather-secom /api/meta/sources
 * GET /api/variables?source=  → weather-secom /api/meta/variables?source=
 *
 * 기존 /api/griddata 와 동일한 /api 경로 사용
 * (Web-ECDIS 입장에서 엔드포인트 주소만 변경하면 됨)
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class MetaController {

    private final MetaClient metaClient;

    /**
     * 사용 가능한 데이터 소스 목록
     * GET /api/sources
     *
     * 응답 예시:
     * {
     *   "sources": [
     *     { "source": "ecmwf", "model": "ifs" },
     *     { "source": "noaa",  "model": "gfs" }
     *   ]
     * }
     */
    @GetMapping("/sources")
    public ResponseEntity<?> getSources() {
        log.info("Web-ECDIS sources 요청");
        try {
            String result = metaClient.getSources();
            return ResponseEntity.ok()
                    .header("Content-Type", "application/json; charset=UTF-8")
                    .body(result);
        } catch (Exception e) {
            log.error("sources 조회 실패: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body("{\"error\": \"sources 조회 실패\", \"message\": \"" + e.getMessage() + "\"}");
        }
    }

    /**
     * 소스별 사용 가능한 변수 목록
     * GET /api/variables?source=ecmwf
     *
     * 응답 예시:
     * {
     *   "variables": ["10u", "10v", "swh", ...]
     * }
     */
    @GetMapping("/variables")
    public ResponseEntity<?> getVariables(
            @RequestParam(required = true) String source) {
        log.info("Web-ECDIS variables 요청: source={}", source);
        try {
            String result = metaClient.getVariables(source);
            return ResponseEntity.ok()
                    .header("Content-Type", "application/json; charset=UTF-8")
                    .body(result);
        } catch (Exception e) {
            log.error("variables 조회 실패: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body("{\"error\": \"variables 조회 실패\", \"message\": \"" + e.getMessage() + "\"}");
        }
    }
}