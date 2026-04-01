package dev.bluemap.ecs.secom.controller;

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
 * CapabilityController
 *
 * SECOM 표준 엔드포인트: GET /api/secom/v1/capability
 *
 * ecs-secom은 Consumer 역할:
 * - weather-secom(Provider)에서 S413 기상 데이터를 받음
 * - Upload 인터페이스 구현 = Provider가 Push할 수 있음을 선언
 * - Get 인터페이스는 false = 우리가 Pull 요청을 받지는 않음
 */
@Component
@Path("/")
@Validated
@Slf4j
public class CapabilityController implements CapabilitySecomInterface {

    @Value("${secom.service.version:0.0.1}")
    private String serviceVersion;

    @Override
    public CapabilityResponseObject capability() {
        log.debug("Capability 요청 수신");

        ImplementedInterfaces implementedInterfaces = new ImplementedInterfaces();
        implementedInterfaces.setGet(false);
        implementedInterfaces.setUpload(false);      // Provider Push 수신 가능
        implementedInterfaces.setSubscription(false);

        CapabilityObject capabilityObject = new CapabilityObject();
        capabilityObject.setDataProductType(SECOM_DataProductType.S413);
        capabilityObject.setContainerType(ContainerTypeEnum.S100_DataSet);
        capabilityObject.setImplementedInterfaces(implementedInterfaces);
        capabilityObject.setServiceVersion(serviceVersion);

        CapabilityResponseObject response = new CapabilityResponseObject();
        response.setCapability(Collections.singletonList(capabilityObject));

        log.debug("Capability 응답 반환: Consumer(S413), version={}", serviceVersion);
        return response;
    }
}
