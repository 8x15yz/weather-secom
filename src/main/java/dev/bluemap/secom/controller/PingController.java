package dev.bluemap.secom.controller;

import jakarta.ws.rs.Path;
import lombok.extern.slf4j.Slf4j;
import org.grad.secom.core.interfaces.PingSecomInterface;
import org.grad.secom.core.models.PingResponseObject;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/**
 * SECOM Ping 컨트롤러
 *
 * 엔드포인트: GET /api/secom/v1/ping
 *
 * G1191 요구사항:
 * - MSR이 등록된 서비스를 매일 ping 체크함
 * - HTTP 200 + 현재 타임스탬프 반환 필수
 * - 응답 없으면 MSR에서 서비스 비활성화됨
 */
@Component
@Path("/")
@Validated
@Slf4j
public class PingController implements PingSecomInterface {

    /**
     * GET /v1/ping
     * 서비스 상태 확인 - MSR이 주기적으로 호출
     */
    @Override
    public PingResponseObject ping() {
        log.debug("Ping 요청 수신");

        PingResponseObject response = new PingResponseObject();
        // PingResponseObject는 내부적으로 현재 타임스탬프를 포함함
        // G1191: "HTTP status 200 and message content must be the current timestamp"

        log.debug("Ping 응답 반환");
        return response;
    }
}
