package dev.bluemap.ecs.secom.client;

import lombok.extern.slf4j.Slf4j;
import org.grad.secom.core.models.GetResponseObject;
import org.grad.secom.core.models.enums.SECOM_DataProductType;
import org.grad.secom.springboot3.components.SecomClient;
import org.grad.secom.springboot3.components.SecomConfigProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * WeatherSecomClient
 *
 * SECOMLib의 SecomClient를 사용해서 weather-secom Provider에
 * SECOM 표준 GET 요청을 보내는 클라이언트.
 *
 * 파라미터 전달 전략 (방법 B - last mile 자유 구현):
 *   - geometry   : WKT POINT / POLYGON → SECOM 표준 파라미터
 *   - validFrom  : run_time_utc → SECOM 표준 파라미터
 *   - productVersion : "source:datasetCode:model:variable:stepHours:bufferKm"
 *                      → weather-secom GetController에서 파싱
 *                      예) "ecmwf:original:ifs:10v:3:50.0"
 *
 * weather-secom GetController의 parseProductVersion()이
 * 이 포맷을 이미 지원하고 있음 (source, model, variable, stepHours).
 */
@Component
@Slf4j
public class WeatherSecomClient {

    @Value("${weather.secom.url}")
    private String weatherSecomUrl;

    private final SecomConfigProperties secomConfig;
    private SecomClient secomClient;

    public WeatherSecomClient(SecomConfigProperties secomConfig) {
        this.secomConfig = secomConfig;
    }

    @PostConstruct
    public void init() {
        try {
            this.secomClient = new SecomClient(
                    new URL(weatherSecomUrl + "/api/secom"),  // http://localhost:8766/api/secom
                    secomConfig
            );
            log.info("SecomClient 초기화 완료: {}", weatherSecomUrl);
        } catch (Exception e) {
            log.error("SecomClient 초기화 실패: {}", e.getMessage());
            throw new RuntimeException("SecomClient 초기화 실패", e);
        }
    }

    /**
     * weather-secom에 기상 데이터 요청
     *
     * @param geometry     WKT (POINT 또는 POLYGON)
     * @param validFrom    run_time_utc (null이면 weather-secom 기본값)
     * @param source       데이터 소스 (noaa, ecmwf / null이면 weather-secom 기본값)
     * @param datasetCode  데이터셋 코드 (original / null이면 기본값)
     * @param model        예측 모델 (gfs, ifs / null이면 기본값)
     * @param variable     기상 변수 (10v, DIRPW 등 / null이면 기본값)
     * @param stepHours    예보 스텝
     * @param bufferKm     버퍼 반경 km
     * @return 디코딩된 기상 데이터 JSON 문자열
     */
    public String getWeatherData(String geometry,
                                 LocalDateTime validFrom,
                                 String source,
                                 String datasetCode,
                                 String model,
                                 String variable,
                                 int stepHours,
                                 double bufferKm) {

        // productVersion 인코딩: "source:datasetCode:model:variable:stepHours:bufferKm"
        // weather-secom GetController.parseProductVersion()이 이 순서로 파싱함
        String productVersion = buildProductVersion(source, datasetCode, model, variable, stepHours, bufferKm);

        log.info("weather-secom GET 요청: geometry={}, validFrom={}, productVersion={}",
                geometry, validFrom, productVersion);

        try {
            Optional<GetResponseObject> responseOpt = secomClient.get(
                    null,                       // dataReference
                    null,                       // containerType
                    SECOM_DataProductType.S413, // dataProductType
                    productVersion,             // ← 여기에 파라미터 인코딩
                    geometry,                   // WKT geometry
                    null,                       // unlocode
                    validFrom,                  // validFrom
                    null,                       // validTo
                    0,                          // page
                    100                         // pageSize
            );

            if (responseOpt.isEmpty()) {
                log.warn("weather-secom 응답 없음");
                return null;
            }

            GetResponseObject response = responseOpt.get();

            if (response.getDataResponseObject() == null
                    || response.getDataResponseObject().isEmpty()) {
                log.warn("weather-secom 응답에 데이터 없음. responseText={}", response.getResponseText());
                return null;
            }

            byte[] dataBytes = response.getDataResponseObject().get(0).getData();
            if (dataBytes == null) {
                log.warn("data 필드가 null");
                return null;
            }

            String weatherJson = new String(dataBytes, StandardCharsets.UTF_8);
            log.debug("기상 데이터 수신 완료: {}bytes", weatherJson.length());
            return weatherJson;

        } catch (Exception e) {
            log.error("weather-secom 호출 실패: {}", e.getMessage());
            throw new RuntimeException("weather-secom 데이터 조회 실패: " + e.getMessage(), e);
        }
    }

    /**
     * weather-secom ping 확인
     */
    public boolean ping() {
        try {
            return secomClient.ping().isPresent();
        } catch (Exception e) {
            log.warn("weather-secom ping 실패: {}", e.getMessage());
            return false;
        }
    }

    // ================================================================
    // 내부 헬퍼
    // ================================================================

    /**
     * productVersion 문자열 생성
     * 형식: "source:datasetCode:model:variable:stepHours:bufferKm"
     * null 값은 빈 문자열로 처리 → weather-secom이 기본값으로 대체
     *
     * 예) "ecmwf:original:ifs:10v:3:50.0"
     *     ":original::DIRPW:0:50.0"  (source/model 없는 경우)
     */
    private String buildProductVersion(String source, String datasetCode, String model,
                                       String variable, int stepHours, double bufferKm) {
        return String.format("%s:%s:%s:%s:%d:%.1f",
                source      != null ? source      : "",
                datasetCode != null ? datasetCode : "",
                model       != null ? model       : "",
                variable    != null ? variable    : "",
                stepHours,
                bufferKm
        );
    }
}