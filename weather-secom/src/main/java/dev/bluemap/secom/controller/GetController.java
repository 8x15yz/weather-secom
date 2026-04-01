package dev.bluemap.secom.controller;

import dev.bluemap.secom.service.WeatherApiService;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.UriInfo;
import lombok.extern.slf4j.Slf4j;
import org.grad.secom.core.interfaces.GetSecomInterface;
import org.grad.secom.core.models.*;
import org.grad.secom.core.models.enums.AckRequestEnum;
import org.grad.secom.core.models.enums.ContainerTypeEnum;
import org.grad.secom.core.models.enums.SECOM_DataProductType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.UUID;

/**
 * SECOM Get 컨트롤러 - 핵심 엔드포인트
 *
 * 엔드포인트: GET /api/secom/v1/object
 *
 * SECOM 파라미터 → weather-api 파라미터 매핑:
 *   geometry POINT(lon lat)   → lat, lon, buffer_km (Center 방식)
 *   geometry POLYGON(...)     → nw_lat, nw_lon, se_lat, se_lon (Bbox 방식)
 *   validFrom                 → run_time_utc
 *   productVersion            → "source:datasetCode:model:variable:stepHours:bufferKm" 형식
 *                               예) "ecmwf:original:ifs:10v:3:50.0"
 *                               null이면 application.properties 기본값 사용
 */
@Component
@Path("/")
@Validated
@Slf4j
public class GetController implements GetSecomInterface {

    @Autowired
    private WeatherApiService weatherApiService;

    @Context
    private UriInfo uriInfo;

    @Value("${weather.api.default-variable:DIRPW}")
    private String defaultVariable;

    @Value("${weather.api.default-buffer-km:50.0}")
    private double defaultBufferKm;

    @Value("${weather.api.default-step-hours:0}")
    private int defaultStepHours;

    @Value("${weather.api.default-run-time:2025-07-01T00:00:00Z}")
    private String defaultRunTime;

    @Override
    public GetResponseObject get(UUID dataReference,
                                 ContainerTypeEnum containerType,
                                 SECOM_DataProductType dataProductType,
                                 String productVersion,
                                 String geometry,
                                 String unlocode,
                                 LocalDateTime validFrom,
                                 LocalDateTime validTo,
                                 Integer page,
                                 Integer pageSize) {

        log.info("GET /v1/object 요청: geometry={}, validFrom(raw)={}, productVersion={}",
                geometry, getRawValidFrom(), productVersion);

        // 1. productVersion 파싱 → source/model/variable/stepHours/bufferKm
        WeatherParams params = parseProductVersion(productVersion);

        // 2. validFrom → run_time_utc
        Instant runTimeUtc = parseValidFrom(validFrom);

        // 3. geometry 파싱 → 좌표 방식 결정
        String weatherJson;

        if (geometry != null && geometry.toUpperCase().startsWith("POLYGON")) {
            double[] bbox = parsePolygonToBbox(geometry);
            if (bbox != null) {
                log.debug("POLYGON Bbox: nw=({},{}), se=({},{})", bbox[0], bbox[1], bbox[2], bbox[3]);
                weatherJson = weatherApiService.getWeatherDataByBbox(
                        params.source, params.datasetCode, params.model,
                        params.variable, runTimeUtc, params.stepHours,
                        bbox[0], bbox[1], bbox[2], bbox[3]
                );
            } else {
                log.warn("POLYGON 파싱 실패 - 기본값 Center 방식 사용");
                weatherJson = weatherApiService.getWeatherDataByCenter(
                        params.source, params.datasetCode, params.model,
                        params.variable, runTimeUtc, params.stepHours,
                        35.1, 129.0, params.bufferKm
                );
            }
        } else if (geometry != null && geometry.toUpperCase().startsWith("POINT")) {
            double[] point = parsePoint(geometry);
            double lat = point != null ? point[0] : 35.1;
            double lon = point != null ? point[1] : 129.0;
            log.debug("POINT Center: lat={}, lon={}, buffer={}km", lat, lon, params.bufferKm);
            weatherJson = weatherApiService.getWeatherDataByCenter(
                    params.source, params.datasetCode, params.model,
                    params.variable, runTimeUtc, params.stepHours,
                    lat, lon, params.bufferKm
            );
        } else {
            log.debug("geometry 없음 - 기본값 사용");
            weatherJson = weatherApiService.getWeatherDataByCenter(
                    params.source, params.datasetCode, params.model,
                    params.variable, runTimeUtc, params.stepHours,
                    35.1, 129.0, params.bufferKm
            );
        }

        // 4. 응답 구성
        byte[] dataBytes = weatherJson.getBytes(StandardCharsets.UTF_8);

        SECOM_ExchangeMetadataObject metadata = new SECOM_ExchangeMetadataObject();

        DataResponseObject dataResponse = new DataResponseObject();
        dataResponse.setData(dataBytes);
        dataResponse.setExchangeMetadata(metadata);
        dataResponse.setAckRequest(AckRequestEnum.NO_ACK_REQUESTED);

        PaginationObject pagination = new PaginationObject();
        pagination.setTotalItems(1);
        pagination.setMaxItemsPerPage(pageSize != null ? pageSize : 100);

        GetResponseObject response = new GetResponseObject();
        response.setDataResponseObject(Collections.singletonList(dataResponse));
        response.setPagination(pagination);
        response.setResponseText("OK");

        log.info("GET /v1/object 응답 완료: variable={}, dataSize={}bytes",
                params.variable, dataBytes.length);
        return response;
    }

    // ================================================================
    // validFrom 파싱
    // ================================================================

    private String getRawValidFrom() {
        if (uriInfo == null) return null;
        return uriInfo.getQueryParameters().getFirst("validFrom");
    }

    private Instant parseValidFrom(LocalDateTime validFromParam) {
        if (validFromParam != null) {
            return validFromParam.toInstant(ZoneOffset.UTC);
        }

        String raw = getRawValidFrom();
        if (raw == null || raw.isBlank()) {
            log.debug("validFrom 없음 - 기본값 사용: {}", defaultRunTime);
            return Instant.parse(defaultRunTime);
        }

        try { return Instant.parse(raw); } catch (DateTimeParseException ignored) {}

        try {
            return LocalDateTime.parse(raw, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                    .toInstant(ZoneOffset.UTC);
        } catch (DateTimeParseException ignored) {}

        try {
            DateTimeFormatter compact = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'")
                    .withZone(ZoneOffset.UTC);
            return Instant.from(compact.parse(raw));
        } catch (DateTimeParseException ignored) {}

        log.warn("validFrom 파싱 실패: '{}' - 기본값 사용", raw);
        return Instant.parse(defaultRunTime);
    }

    // ================================================================
    // geometry 파싱
    // ================================================================

    private double[] parsePoint(String geometry) {
        try {
            String coords = geometry
                    .replaceAll("(?i)POINT\\s*\\(", "")
                    .replace(")", "")
                    .trim();
            String[] parts = coords.split("\\s+");
            double lon = Double.parseDouble(parts[0]);
            double lat = Double.parseDouble(parts[1]);
            return new double[]{lat, lon};
        } catch (Exception e) {
            log.warn("POINT 파싱 실패: {} - {}", geometry, e.getMessage());
            return null;
        }
    }

    private double[] parsePolygonToBbox(String geometry) {
        try {
            String coords = geometry
                    .replaceAll("(?i)POLYGON\\s*\\(\\(", "")
                    .replaceAll("\\)\\)", "")
                    .trim();

            String[] points = coords.split(",");
            double minLat = Double.MAX_VALUE, maxLat = -Double.MAX_VALUE;
            double minLon = Double.MAX_VALUE, maxLon = -Double.MAX_VALUE;

            for (String point : points) {
                String[] parts = point.trim().split("\\s+");
                double lon = Double.parseDouble(parts[0]);
                double lat = Double.parseDouble(parts[1]);
                minLat = Math.min(minLat, lat);
                maxLat = Math.max(maxLat, lat);
                minLon = Math.min(minLon, lon);
                maxLon = Math.max(maxLon, lon);
            }

            return new double[]{maxLat, minLon, minLat, maxLon};
        } catch (Exception e) {
            log.warn("POLYGON 파싱 실패: {} - {}", geometry, e.getMessage());
            return null;
        }
    }

    // ================================================================
    // productVersion 파싱
    // ================================================================

    /**
     * productVersion 파싱
     * 형식: "source:datasetCode:model:variable:stepHours:bufferKm"
     * 예)   "ecmwf:original:ifs:10v:3:50.0"
     *
     * 빈 문자열 필드는 기본값으로 대체 (null 처리는 WeatherApiService에서)
     */
    private WeatherParams parseProductVersion(String productVersion) {
        WeatherParams params = new WeatherParams();
        params.variable  = defaultVariable;
        params.stepHours = defaultStepHours;
        params.bufferKm  = defaultBufferKm;

        if (productVersion == null || productVersion.isBlank()) {
            return params;
        }

        try {
            String[] parts = productVersion.split(":", -1); // -1: 빈 문자열 유지
            if (parts.length >= 1 && !parts[0].isBlank()) params.source      = parts[0];
            if (parts.length >= 2 && !parts[1].isBlank()) params.datasetCode = parts[1];
            if (parts.length >= 3 && !parts[2].isBlank()) params.model       = parts[2];
            if (parts.length >= 4 && !parts[3].isBlank()) params.variable    = parts[3];
            if (parts.length >= 5 && !parts[4].isBlank()) params.stepHours   = Integer.parseInt(parts[4]);
            if (parts.length >= 6 && !parts[5].isBlank()) params.bufferKm    = Double.parseDouble(parts[5]);

            log.debug("productVersion 파싱: source={}, model={}, variable={}, step={}, buffer={}km",
                    params.source, params.model, params.variable, params.stepHours, params.bufferKm);
        } catch (Exception e) {
            log.warn("productVersion 파싱 실패: '{}' - 기본값 사용", productVersion);
        }

        return params;
    }

    private static class WeatherParams {
        String source;
        String datasetCode;
        String model;
        String variable;
        int    stepHours;
        double bufferKm;
    }
}