package dev.bluemap.secom.controller;

import dev.bluemap.secom.service.WeatherApiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * MetaController
 *
 * 기상 데이터 메타정보 조회 엔드포인트 (SECOM last mile 영역)
 *
 * GET /api/meta/sources   → Python API /api/sources
 * GET /api/meta/variables → Python API /api/variables?source=
 *
 * ecs-secom이 이 엔드포인트를 호출해서 Web-ECDIS로 전달함.
 */
@RestController
@RequestMapping("/api/meta")
@RequiredArgsConstructor
@Slf4j
public class MetaController {

    private final WeatherApiService weatherApiService;

    /**
     * 사용 가능한 데이터 소스 목록
     * GET /api/meta/sources
     */
    @GetMapping("/sources")
    public ResponseEntity<?> getSources() {
        log.info("sources 목록 요청");
        try {
            String result = weatherApiService.getSources();
            return ResponseEntity.ok()
                    .header("Content-Type", "application/json; charset=UTF-8")
                    .body(result);
        } catch (Exception e) {
            log.error("sources 조회 실패: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body("{\"error\": \"sources 조회 실패\"}");
        }
    }

    /**
     * 소스별 사용 가능한 변수 목록
     * GET /api/meta/variables?source=ecmwf
     */
    @GetMapping("/variables")
    public ResponseEntity<?> getVariables(
            @RequestParam(required = false) String source) {
        log.info("variables 목록 요청: source={}", source);
        try {
            String result = weatherApiService.getVariables(source);
            return ResponseEntity.ok()
                    .header("Content-Type", "application/json; charset=UTF-8")
                    .body(result);
        } catch (Exception e) {
            log.error("variables 조회 실패: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body("{\"error\": \"variables 조회 실패\"}");
        }
    }
}