package dev.bluemap.secom.provider;

import lombok.extern.slf4j.Slf4j;
import org.grad.secom.core.base.DigitalSignatureCertificate;
import org.grad.secom.core.base.SecomSignatureProvider;
import org.grad.secom.core.models.enums.DigitalSignatureAlgorithmEnum;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * [로컬 개발용] 서명 없이 동작하는 더미 Signature Provider
 *
 * SECOMLib은 SecomSignatureProvider 빈이 반드시 있어야 구동됨.
 * 실제 MIR 인증서 발급 전까지 이 클래스를 사용.
 *
 * TODO: 인증서 발급 후 실제 서명 로직으로 교체 필요
 */
@Component
@Slf4j
public class SecomSignatureProviderImpl implements SecomSignatureProvider {

    /**
     * 서명 생성
     * - 로컬 테스트 단계: null 반환 (서명 없음)
     * - MSR 등록 후: 실제 private key로 서명
     */
    @Override
    public byte[] generateSignature(DigitalSignatureCertificate signatureCertificate,
                                    DigitalSignatureAlgorithmEnum algorithm,
                                    byte[] payload) {
        log.debug("[DEV MODE] 서명 생성 스킵 - 실제 배포 전 인증서 교체 필요");
        // TODO: 인증서 발급 후 아래 코드로 교체
        // try {
        //     Signature sign = Signature.getInstance(algorithm.getValue());
        //     sign.initSign(this.privateKey);
        //     sign.update(payload);
        //     return sign.sign();
        // } catch (Exception ex) { ... }
        return null;
    }

    /**
     * 서명 검증
     * - 로컬 테스트 단계: 항상 true 반환
     * - MSR 등록 후: 실제 인증서로 검증
     */
    @Override
    public boolean validateSignature(String signatureCertificate,
                                     DigitalSignatureAlgorithmEnum algorithm,
                                     byte[] signature,
                                     byte[] content) {
        log.debug("[DEV MODE] 서명 검증 스킵 - 항상 true 반환");
        return true;
    }
}
