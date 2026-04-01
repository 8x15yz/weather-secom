package dev.bluemap.ecs.secom.provider;

import lombok.extern.slf4j.Slf4j;
import org.grad.secom.core.base.DigitalSignatureCertificate;
import org.grad.secom.core.base.SecomSignatureProvider;
import org.grad.secom.core.models.enums.DigitalSignatureAlgorithmEnum;
import org.springframework.stereotype.Component;

/**
 * SecomSignatureProviderImpl
 *
 * SECOMLib 구동에 반드시 필요한 빈.
 * weather-secom과 동일하게 개발 단계에서는 더미 구현.
 *
 * TODO: MCP 인증서 연결 후 실제 서명 로직으로 교체
 */
@Component
@Slf4j
public class SecomSignatureProviderImpl implements SecomSignatureProvider {

    @Override
    public byte[] generateSignature(DigitalSignatureCertificate signatureCertificate,
                                    DigitalSignatureAlgorithmEnum algorithm,
                                    byte[] payload) {
        log.debug("[DEV MODE] 서명 생성 스킵");
        return null;
    }

    @Override
    public boolean validateSignature(String signatureCertificate,
                                     DigitalSignatureAlgorithmEnum algorithm,
                                     byte[] signature,
                                     byte[] content) {
        log.debug("[DEV MODE] 서명 검증 스킵 - 항상 true");
        return true;
    }
}
