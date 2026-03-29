package dev.bluemap.secom.controller;

import jakarta.ws.rs.Path;
import lombok.extern.slf4j.Slf4j;
import org.grad.secom.core.interfaces.CapabilitySecomInterface;
import org.grad.secom.core.models.CapabilityObject;
import org.grad.secom.core.models.CapabilityResponseObject;
import org.grad.secom.core.models.ImplementedInterfaces;
import org.grad.secom.core.models.enums.ContainerTypeEnum;
import org.grad.secom.core.models.enums.SECOM_DataProductType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.util.Collections;

/**
 * SECOM Capability 컨트롤러
 *
 * 엔드포인트: GET /api/secom/v1/capability
 *
 * 역할: "나는 어떤 서비스야" 라고 선언하는 엔드포인트
 * - 어떤 데이터 타입을 제공하는지 (S412 Weather Overlay)
 * - 어떤 인터페이스를 구현했는지 (Get, Ping 등)
 */
@Component
@Path("/")
@Validated
@Slf4j
public class CapabilityController implements CapabilitySecomInterface {

    @Value("${secom.service.version:0.0.1}")
    private String serviceVersion;

    /**
     * GET /v1/capability
     * 서비스가 지원하는 기능 목록 반환
     */
    @Override
    public CapabilityResponseObject capability() {
        log.debug("Capability 요청 수신");

        // 구현한 인터페이스 선언
        ImplementedInterfaces implementedInterfaces = new ImplementedInterfaces();
        implementedInterfaces.setGet(true);         // /v1/object - 데이터 조회
        implementedInterfaces.setGetSummary(false); // /v1/object/summary - 미구현
        implementedInterfaces.setSubscription(false); // 구독 - 미구현

        // 서비스 기능 객체 구성
        CapabilityObject capabilityObject = new CapabilityObject();
        // S412: Weather Overlay - 우리 기상 데이터에 가장 적합한 타입
        // S413: Marine Weather Conditions 도 고려 가능
        capabilityObject.setDataProductType(SECOM_DataProductType.S412);
        capabilityObject.setContainerType(ContainerTypeEnum.S100_DataSet);
        capabilityObject.setImplementedInterfaces(implementedInterfaces);
        capabilityObject.setServiceVersion(serviceVersion);

        // 응답 객체 구성
        CapabilityResponseObject response = new CapabilityResponseObject();
        response.setCapability(Collections.singletonList(capabilityObject));

        log.debug("Capability 응답 반환: dataProductType=S412, version={}", serviceVersion);
        return response;
    }
}
