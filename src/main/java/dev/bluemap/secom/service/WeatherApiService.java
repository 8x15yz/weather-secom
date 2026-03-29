// WeatherApiService.java

package dev.bluemap.secom.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * 기상 API 호출 서비스
 *
 * optimal-loads 백엔드(52.78.244.211)의 /api/griddata 엔드포인트를 호출하여
 * NOAA GFS 기상 데이터를 가져옴.
 *
 * 예시 URL:
 * http://52.78.244.211/api/griddata
 *   ?source=noaa&dataset_code=original&model=gfs
 *   &variable=DIRPW
 *   &run_time_utc=2025-07-01T00:00:00Z
 *   &step_hours=0
 *   &lat=35.0&lon=129.0&buffer_km=50.0
 */
@Service
@Slf4j
public class WeatherApiService {

    @Value("${weather.api.base-url}")
    private String baseUrl;

    @Value("${weather.api.path}")
    private String apiPath;

    @Value("${weather.api.default-source:noaa}")
    private String defaultSource;

    @Value("${weather.api.default-dataset:original}")
    private String defaultDataset;

    @Value("${weather.api.default-model:gfs}")
    private String defaultModel;

    private final WebClient webClient;

    public WeatherApiService(WebClient.Builder webClientBuilder,
                             @Value("${weather.api.base-url}") String baseUrl) {
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
    }

    /**
     * 기상 데이터 조회
     *
     * @param variable   기상 변수 (예: DIRPW, HTSGW, PERPW, UGRD, VGRD)
     * @param lat        위도 중심
     * @param lon        경도 중심
     * @param bufferKm   반경 (km)
     * @param runTimeUtc 모델 실행 시각 (UTC)
     * @param stepHours  예보 시간 (0=현재)
     * @return JSON 응답 문자열
     */
    public String getWeatherData(String variable,
                                  double lat,
                                  double lon,
                                  double bufferKm,
                                  Instant runTimeUtc,
                                  int stepHours) {
        String formattedTime = DateTimeFormatter
                .ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
                .withZone(ZoneOffset.UTC)
                .format(runTimeUtc);

        log.debug("기상 API 호출: variable={}, lat={}, lon={}, buffer={}km, time={}",
                variable, lat, lon, bufferKm, formattedTime);

        try {
            String response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(apiPath)
                            .queryParam("source", defaultSource)
                            .queryParam("dataset_code", defaultDataset)
                            .queryParam("model", defaultModel)
                            .queryParam("variable", variable)
                            .queryParam("run_time_utc", formattedTime)
                            .queryParam("step_hours", stepHours)
                            .queryParam("lat", lat)
                            .queryParam("lon", lon)
                            .queryParam("buffer_km", bufferKm)
                            .build())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            log.debug("기상 API 응답 수신 완료 (길이: {} bytes)",
                    response != null ? response.length() : 0);
            return response;

        } catch (Exception e) {
            log.error("기상 API 호출 실패: {}", e.getMessage());
            throw new RuntimeException("기상 데이터 조회 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 현재 시각 기준 기상 데이터 조회 (편의 메서드)
     */
    public String getCurrentWeatherData(String variable, double lat, double lon, double bufferKm) {
        // 현재 시각에서 가장 가까운 6시간 단위 모델 실행 시각으로 반올림
        Instant now = Instant.now();
        return getWeatherData(variable, lat, lon, bufferKm, now, 0);
    }
}
