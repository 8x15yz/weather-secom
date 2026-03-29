// GetController.java

package dev.bluemap.secom.controller;

import dev.bluemap.secom.service.WeatherApiService;
import jakarta.ws.rs.Path;
import lombok.extern.slf4j.Slf4j;
import org.grad.secom.core.interfaces.GetSecomInterface;
import org.grad.secom.core.models.*;
import org.grad.secom.core.models.enums.AckRequestEnum;
import org.grad.secom.core.models.enums.ContainerTypeEnum;
import org.grad.secom.core.models.enums.SECOM_DataProductType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;  // ← import 변경
import java.util.Base64;
import java.util.Collections;
import java.util.UUID;

import java.time.Instant;
import java.time.ZoneOffset;

/**
 * SECOM Get 컨트롤러 - 핵심 엔드포인트
 *
 * 엔드포인트: GET /api/secom/v1/object
 *
 * SECOM 소비자(ECDIS 등)가 이 엔드포인트로 기상 데이터를 요청함.
 *
 * 요청 파라미터 예시:
 *   GET /v1/object?dataProductType=S412&geometry=POINT(129.0 35.0)
 *
 * 처리 흐름:
 *   1. SECOM 요청 파라미터 수신 (geometry, validFrom, validTo 등)
 *   2. geometry → lat/lon/buffer_km 변환
 *   3. optimal-loads API 호출
 *   4. JSON 응답 → Base64 인코딩
 *   5. SECOM DataResponseObject로 감싸서 반환
 */
@Component
@Path("/")
@Validated
@Slf4j
public class GetController implements GetSecomInterface {

    @Autowired
    private WeatherApiService weatherApiService;

    /**
     * GET /v1/object
     * SECOM 표준 데이터 조회 인터페이스
     *
     * @param dataReference  데이터 참조 UUID (특정 데이터셋 지정 시)
     * @param containerType  컨테이너 타입 (S100_DataSet 등)
     * @param dataProductType 데이터 제품 타입 (S412 등)
     * @param productVersion 제품 버전
     * @param geometry       요청 지역 (WKT 형식, 예: POINT(129.0 35.0))
     * @param unlocode       UN/LOCODE (예: KRPUS = 부산항)
     * @param validFrom      유효 시작 시각
     * @param validTo        유효 종료 시각
     * @param page           페이지 번호
     * @param pageSize       페이지 크기
     */
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

        log.info("GET /v1/object 요청 수신: geometry={}, dataProductType={}, validFrom={}",
                geometry, dataProductType, validFrom);

        // 1. geometry 파싱 → lat/lon 추출
        double[] latLon = parseGeometry(geometry);
        double lat = latLon[0];
        double lon = latLon[1];
        double bufferKm = 20.0; // 기본값 50km

        // 2. 기상 API 호출 시각 결정

        // 3. 기상 데이터 조회 (기본 변수: DIRPW 파도 방향)
        // TODO: dataProductType이나 다른 파라미터에 따라 variable 선택 로직 추가 가능
        String weatherVariable = "DIRPW";
        Instant queryTime = validFrom != null
                ? validFrom.toInstant(ZoneOffset.UTC)
                : Instant.parse("2025-07-01T00:00:00Z");

        String weatherJson = weatherApiService.getWeatherData(
                weatherVariable, lat, lon, bufferKm, queryTime, 0);

        // 4. 기상 데이터 → Base64 인코딩
        // SECOM 표준: data 필드는 byte[] (Base64로 직렬화됨)
        byte[] dataBytes = weatherJson.getBytes(StandardCharsets.UTF_8);

        // 5. SECOM 응답 객체 구성
        // ExchangeMetadata - 데이터에 대한 메타정보
        SECOM_ExchangeMetadataObject metadata = new SECOM_ExchangeMetadataObject();
        // 서명/암호화 미사용 (로컬 개발 단계)
        // 실제 배포 시: signatureProvider 연결 후 자동으로 서명됨

        // DataResponseObject - 실제 데이터 + 메타데이터
        DataResponseObject dataResponse = new DataResponseObject();
        dataResponse.setData(dataBytes);
        dataResponse.setExchangeMetadata(metadata);
        dataResponse.setAckRequest(AckRequestEnum.NO_ACK_REQUESTED);

        // Pagination 정보
        PaginationObject pagination = new PaginationObject();
        pagination.setTotalItems(1);
        pagination.setMaxItemsPerPage(pageSize != null ? pageSize : 100);

        // 최종 GetResponseObject 구성
        GetResponseObject response = new GetResponseObject();
        response.setDataResponseObject(Collections.singletonList(dataResponse));
        response.setPagination(pagination);
        response.setResponseText("OK");

        log.info("GET /v1/object 응답 완료: lat={}, lon={}, dataSize={} bytes",
                lat, lon, dataBytes.length);
        return response;
    }

    /**
     * WKT geometry 문자열에서 lat/lon 파싱
     *
     * 지원 형식:
     * - POINT(lon lat)          → 예: POINT(129.0 35.0)
     * - null/빈 문자열          → 기본값 반환 (한국 중심)
     *
     * TODO: POLYGON, LINESTRING 등 다른 형식 추가 지원 가능
     */
    private double[] parseGeometry(String geometry) {
        // 기본값: 부산 인근 (한국 주요 항구)
        double defaultLat = 35.1;
        double defaultLon = 129.0;

        if (geometry == null || geometry.isBlank()) {
            log.debug("geometry 없음 - 기본값 사용: lat={}, lon={}", defaultLat, defaultLon);
            return new double[]{defaultLat, defaultLon};
        }

        try {
            // POINT(lon lat) 파싱
            if (geometry.toUpperCase().startsWith("POINT")) {
                String coords = geometry
                        .replaceAll("(?i)POINT\\s*\\(", "")
                        .replace(")", "")
                        .trim();
                String[] parts = coords.split("\\s+");
                double lon = Double.parseDouble(parts[0]);
                double lat = Double.parseDouble(parts[1]);
                log.debug("POINT geometry 파싱 완료: lat={}, lon={}", lat, lon);
                return new double[]{lat, lon};
            }

            // TODO: POLYGON 중심점 계산 추가
            // geometry가 POLYGON이면 중심점(centroid) 계산 필요

        } catch (Exception e) {
            log.warn("geometry 파싱 실패 '{}' - 기본값 사용: {}", geometry, e.getMessage());
        }

        return new double[]{defaultLat, defaultLon};
    }
}
