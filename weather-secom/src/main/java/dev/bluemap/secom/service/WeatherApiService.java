// weather-secom
// WeatherApiService.java

package dev.bluemap.secom.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriBuilder;

import java.net.URI;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.function.Function;

/**
 * 기상 API 호출 서비스 (Last Mile)
 *
 * optimal-loads 백엔드의 엔드포인트를 호출하여 기상 데이터를 가져옴.
 * SECOM 표준 범위 밖(Last Mile) 구간이므로 파라미터 구조 자유 설계 가능.
 *
 * 지원 엔드포인트:
 *   GET /api/griddata  - 격자 기상 데이터 조회
 *   GET /api/sources   - 사용 가능한 데이터 소스 목록
 *   GET /api/variables - 소스별 사용 가능한 변수 목록
 *
 * 좌표 방식 3가지:
 *   방식1: 중심점 + 버퍼   lat, lon, buffer_km
 *   방식2: NW/SE 코너      nw_lat, nw_lon, se_lat, se_lon
 *   방식3: 전체 영역       좌표 파라미터 없음
 */
@Service
@Slf4j
public class WeatherApiService {

    @Value("${weather.api.base-url}")
    private String baseUrl;

    @Value("${weather.api.path:/api/griddata}")
    private String gridDataPath;

    @Value("${weather.api.sources-path:/api/sources}")
    private String sourcesPath;

    @Value("${weather.api.variables-path:/api/variables}")
    private String variablesPath;

    // 기본값 (파라미터로 override 가능)
    @Value("${weather.api.default-source:noaa}")
    private String defaultSource;

    @Value("${weather.api.default-dataset:original}")
    private String defaultDataset;

    @Value("${weather.api.default-model:gfs}")
    private String defaultModel;

    private final WebClient webClient;

    private static final DateTimeFormatter UTC_FORMATTER = DateTimeFormatter
            .ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
            .withZone(ZoneOffset.UTC);

    public WeatherApiService(WebClient.Builder webClientBuilder,
                             @Value("${weather.api.base-url}") String baseUrl) {
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
    }

    // ================================================================
    // 방식1: 중심점 + 버퍼 (lat, lon, buffer_km)
    // ================================================================

    /**
     * 기상 데이터 조회 - 중심점 + 버퍼 방식
     *
     * @param source      데이터 소스 (noaa, ecmwf / null이면 기본값)
     * @param datasetCode 데이터셋 코드 (original, computed / null이면 기본값)
     * @param model       예측 모델 (gfs, ifs / null이면 기본값)
     * @param variable    기상 변수 (DIRPW, HTSGW, PERPW, UGRD, VGRD, swh ...)
     * @param runTimeUtc  모델 실행 시각 (UTC)
     * @param stepHours   예보 스텝 (0 = 현재)
     * @param lat         위도 중심
     * @param lon         경도 중심
     * @param bufferKm    반경 km (0이면 단일 격자점)
     */
    public String getWeatherDataByCenter(String source,
                                         String datasetCode,
                                         String model,
                                         String variable,
                                         Instant runTimeUtc,
                                         int stepHours,
                                         double lat,
                                         double lon,
                                         double bufferKm) {
        String formattedTime = UTC_FORMATTER.format(runTimeUtc);
        String resolvedSource = source != null ? source : defaultSource;
        String resolvedDataset = datasetCode != null ? datasetCode : defaultDataset;
        String resolvedModel = model != null ? model : defaultModel;

        log.debug("기상 API 호출 [중심점+버퍼]: source={}, variable={}, lat={}, lon={}, buffer={}km, time={}",
                resolvedSource, variable, lat, lon, bufferKm, formattedTime);

        return callGridData(uriBuilder -> uriBuilder
                .path(gridDataPath)
                .queryParam("source", resolvedSource)
                .queryParam("dataset_code", resolvedDataset)
                .queryParam("model", resolvedModel)
                .queryParam("variable", variable)
                .queryParam("run_time_utc", formattedTime)
                .queryParam("step_hours", stepHours)
                .queryParam("lat", lat)
                .queryParam("lon", lon)
                .queryParam("buffer_km", bufferKm)
                .build());
    }

    // ================================================================
    // 방식2: NW/SE 코너 bbox 방식
    // ================================================================

    /**
     * 기상 데이터 조회 - NW/SE 코너 bbox 방식
     *
     * @param source      데이터 소스 (noaa, ecmwf / null이면 기본값)
     * @param datasetCode 데이터셋 코드 (original, computed / null이면 기본값)
     * @param model       예측 모델 (gfs, ifs / null이면 기본값)
     * @param variable    기상 변수
     * @param runTimeUtc  모델 실행 시각 (UTC)
     * @param stepHours   예보 스텝
     * @param nwLat       북서쪽 위도
     * @param nwLon       북서쪽 경도
     * @param seLat       남동쪽 위도
     * @param seLon       남동쪽 경도
     */
    public String getWeatherDataByBbox(String source,
                                       String datasetCode,
                                       String model,
                                       String variable,
                                       Instant runTimeUtc,
                                       int stepHours,
                                       double nwLat,
                                       double nwLon,
                                       double seLat,
                                       double seLon) {
        String formattedTime = UTC_FORMATTER.format(runTimeUtc);
        String resolvedSource = source != null ? source : defaultSource;
        String resolvedDataset = datasetCode != null ? datasetCode : defaultDataset;
        String resolvedModel = model != null ? model : defaultModel;

        log.debug("기상 API 호출 [NW/SE bbox]: source={}, variable={}, nw=({},{}), se=({},{}), time={}",
                resolvedSource, variable, nwLat, nwLon, seLat, seLon, formattedTime);

        return callGridData(uriBuilder -> uriBuilder
                .path(gridDataPath)
                .queryParam("source", resolvedSource)
                .queryParam("dataset_code", resolvedDataset)
                .queryParam("model", resolvedModel)
                .queryParam("variable", variable)
                .queryParam("run_time_utc", formattedTime)
                .queryParam("step_hours", stepHours)
                .queryParam("nw_lat", nwLat)
                .queryParam("nw_lon", nwLon)
                .queryParam("se_lat", seLat)
                .queryParam("se_lon", seLon)
                .build());
    }

    // ================================================================
    // 방식3: 전체 영역 (좌표 파라미터 없음)
    // ================================================================

    /**
     * 기상 데이터 조회 - 전체 영역
     *
     * ⚠️ 셀 수 제한(5,250,000) 초과 가능성 있음. 주의해서 사용.
     *
     * @param source      데이터 소스 (noaa, ecmwf / null이면 기본값)
     * @param datasetCode 데이터셋 코드 (original, computed / null이면 기본값)
     * @param model       예측 모델 (gfs, ifs / null이면 기본값)
     * @param variable    기상 변수
     * @param runTimeUtc  모델 실행 시각 (UTC)
     * @param stepHours   예보 스텝
     */
    public String getWeatherDataAll(String source,
                                    String datasetCode,
                                    String model,
                                    String variable,
                                    Instant runTimeUtc,
                                    int stepHours) {
        String formattedTime = UTC_FORMATTER.format(runTimeUtc);
        String resolvedSource = source != null ? source : defaultSource;
        String resolvedDataset = datasetCode != null ? datasetCode : defaultDataset;
        String resolvedModel = model != null ? model : defaultModel;

        log.debug("기상 API 호출 [전체 영역]: source={}, variable={}, time={}",
                resolvedSource, variable, formattedTime);

        return callGridData(uriBuilder -> uriBuilder
                .path(gridDataPath)
                .queryParam("source", resolvedSource)
                .queryParam("dataset_code", resolvedDataset)
                .queryParam("model", resolvedModel)
                .queryParam("variable", variable)
                .queryParam("run_time_utc", formattedTime)
                .queryParam("step_hours", stepHours)
                .build());
    }

    // ================================================================
    // 메타 API
    // ================================================================

    /**
     * 사용 가능한 데이터 소스 목록 조회
     * GET /api/sources
     */
    public String getSources() {
        log.debug("소스 목록 조회");
        try {
            return webClient.get()
                    .uri(sourcesPath)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
        } catch (Exception e) {
            log.error("소스 목록 조회 실패: {}", e.getMessage());
            throw new RuntimeException("소스 목록 조회 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 소스별 사용 가능한 변수 목록 조회
     * GET /api/variables?source={source}
     *
     * @param source 데이터 소스 (noaa, ecmwf / null이면 전체)
     */
    public String getVariables(String source) {
        log.debug("변수 목록 조회: source={}", source);
        try {
            return webClient.get()
                    .uri(uriBuilder -> {
                        var builder = uriBuilder.path(variablesPath);
                        if (source != null && !source.isBlank()) {
                            builder.queryParam("source", source);
                        }
                        return builder.build();
                    })
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
        } catch (Exception e) {
            log.error("변수 목록 조회 실패: {}", e.getMessage());
            throw new RuntimeException("변수 목록 조회 실패: " + e.getMessage(), e);
        }
    }

    // ================================================================
    // 내부 공통 호출
    // ================================================================

    private String callGridData(Function<UriBuilder, URI> uriFunction) {
        try {
            String response = webClient.get()
                    .uri(uriFunction)
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
}