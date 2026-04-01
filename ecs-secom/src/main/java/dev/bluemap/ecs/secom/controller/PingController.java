package dev.bluemap.ecs.secom.controller;

import jakarta.ws.rs.Path;
import lombok.extern.slf4j.Slf4j;
import org.grad.secom.core.interfaces.PingSecomInterface;
import org.grad.secom.core.models.PingResponseObject;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * PingController
 *
 * SECOM 표준 엔드포인트: GET /api/secom/v1/ping
 *
 * ecs-secom도 나중에 MSR에 등록될 수 있으므로
 * weather-secom과 동일하게 Ping 구현 (SECOM 필수 인터페이스).
 */
@Component
@Path("/")
@Validated
@Slf4j
public class PingController implements PingSecomInterface {

    @Override
    public PingResponseObject ping() {
        log.debug("Ping 요청 수신");
        PingResponseObject response = new PingResponseObject();
        response.setLastPrivateInteractionTime(LocalDateTime.now(ZoneOffset.UTC));
        log.debug("Ping 응답 반환");
        return response;
    }
}
