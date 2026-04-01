package dev.bluemap.ecs.secom.controller;

import dev.bluemap.ecs.secom.client.WeatherSecomClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;

/**
 * WeatherController
 *
 * Web-ECDIS 개발자가 기존에 쓰던 경로/파라미터 그대로 호출 가능.
 * 엔드포인트 주소만 변경하면 동작하도록 설계.
 *
 * 기존: http://52.78.244.211/api/griddata?source=ecmwf&model=ifs&...
 * 변경: http://52.78.244.211:8767/api/griddata?source=ecmwf&model=ifs&...
 *
 * 내부적으로 SECOM 흐름을 거쳐서 weather-secom(8766)에 요청함.
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class WeatherController {

    private final WeatherSecomClient weatherSecomClient;

    /**
     * 기상 데이터 조회
     *
     * 기존 weather API와 동일한 파라미터를 그대로 사용:
     *   GET /api/griddata
     *     ?source=ecmwf
     *     &dataset_code=original
     *     &model=ifs
     *     &variable=10v
     *     &run_time_utc=2025-07-01T00:00:00Z
     *     &step_hours=3
     *     &lat=35.0
     *     &lon=129.0
     *     &buffer_km=50.0
     */
    @GetMapping("/griddata")
    public ResponseEntity<?> getGridData(
            // 데이터 소스 파라미터
            @RequestParam(required = false) String source,
            @RequestParam(name = "dataset_code", required = false) String datasetCode,
            @RequestParam(required = false) String model,
            @RequestParam(required = false) String variable,
            // 시간 파라미터
            @RequestParam(name = "run_time_utc", required = false) String runTimeUtc,
            @RequestParam(name = "step_hours", required = false, defaultValue = "0") int stepHours,
            // 좌표 파라미터 (중심점 방식)
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lon,
            @RequestParam(name = "buffer_km", required = false, defaultValue = "50.0") double bufferKm,
            // 좌표 파라미터 (bbox 방식)
            @RequestParam(name = "nw_lat", required = false) Double nwLat,
            @RequestParam(name = "nw_lon", required = false) Double nwLon,
            @RequestParam(name = "se_lat", required = false) Double seLat,
            @RequestParam(name = "se_lon", required = false) Double seLon) {

        log.info("Web-ECDIS 기상 요청: source={}, model={}, variable={}, lat={}, lon={}, runTime={}",
                source, model, variable, lat, lon, runTimeUtc);

        // geometry 조합 (좌표 방식에 따라)
        String geometry = buildGeometry(lat, lon, nwLat, nwLon, seLat, seLon);
        if (geometry == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "좌표 파라미터가 필요합니다",
                    "examples", Map.of(
                            "center", "/api/griddata?lat=35.0&lon=129.0&buffer_km=50.0",
                            "bbox",   "/api/griddata?nw_lat=36.0&nw_lon=128.0&se_lat=34.0&se_lon=130.0"
                    )
            ));
        }

        // validFrom 파싱
        LocalDateTime queryTime = parseRunTime(runTimeUtc);

        try {
            String weatherJson = weatherSecomClient.getWeatherData(
                    geometry, queryTime,
                    source, datasetCode, model, variable, stepHours, bufferKm
            );

            if (weatherJson == null) {
                return ResponseEntity.noContent().build();
            }

            return ResponseEntity.ok()
                    .header("Content-Type", "application/json; charset=UTF-8")
                    .body(weatherJson);

        } catch (Exception e) {
            log.error("기상 데이터 조회 실패: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "기상 데이터 조회 실패",
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * 헬스체크
     */
    @GetMapping("/health")
    public ResponseEntity<?> health() {
        boolean weatherSecomAlive = weatherSecomClient.ping();
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "optimal-ecs-secom",
                "role", "SECOM Consumer",
                "weatherSecom", weatherSecomAlive ? "UP" : "DOWN"
        ));
    }

    // ================================================================
    // 내부 헬퍼
    // ================================================================

    /**
     * 좌표 파라미터 → WKT geometry 변환
     * 중심점 방식 우선, 없으면 bbox 방식
     */
    private String buildGeometry(Double lat, Double lon,
                                 Double nwLat, Double nwLon,
                                 Double seLat, Double seLon) {
        if (lat != null && lon != null) {
            return String.format("POINT(%s %s)", lon, lat);
        }
        if (nwLat != null && nwLon != null && seLat != null && seLon != null) {
            // WKT POLYGON: 시계방향, 닫힌 링
            return String.format("POLYGON((%s %s, %s %s, %s %s, %s %s, %s %s))",
                    nwLon, nwLat,
                    seLon, nwLat,
                    seLon, seLat,
                    nwLon, seLat,
                    nwLon, nwLat);
        }
        return null;
    }

    /**
     * run_time_utc 문자열 → LocalDateTime 변환
     */
    private LocalDateTime parseRunTime(String runTimeUtc) {
        if (runTimeUtc == null || runTimeUtc.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.ofInstant(Instant.parse(runTimeUtc), ZoneOffset.UTC);
        } catch (Exception e) {
            log.warn("run_time_utc 파싱 실패: '{}' - weather-secom 기본값 사용", runTimeUtc);
            return null;
        }
    }
}