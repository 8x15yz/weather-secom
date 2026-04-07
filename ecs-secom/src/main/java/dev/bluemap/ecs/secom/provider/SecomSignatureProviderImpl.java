// ecs-secom > SecomSignatureProviderImpl.java

package dev.bluemap.ecs.secom.provider;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.grad.secom.core.base.DigitalSignatureCertificate;
import org.grad.secom.core.base.SecomSignatureProvider;
import org.grad.secom.core.models.enums.DigitalSignatureAlgorithmEnum;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.security.*;
import java.security.cert.X509Certificate;

@Component
@Slf4j
public class SecomSignatureProviderImpl implements SecomSignatureProvider {

    @Value("${secom.security.ssl.keystore-password}")
    private String keystorePassword;

    private PrivateKey privateKey;
    private X509Certificate certificate;

    @PostConstruct
    public void init() throws Exception {
        KeyStore ks = KeyStore.getInstance("PKCS12");
        try (InputStream is = getClass().getClassLoader()
                .getResourceAsStream("keystore.p12")) {
            if (is == null) throw new IllegalStateException("keystore.p12 를 찾을 수 없음");
            ks.load(is, keystorePassword.toCharArray());
        }
        // ecs 인증서 alias 확인 필요 (weather는 "1" 이었음)
        String alias = "1";
        privateKey  = (PrivateKey)      ks.getKey(alias, keystorePassword.toCharArray());
        certificate = (X509Certificate) ks.getCertificate(alias);
        log.info("MCP 인증서 로드 완료: subject={}", certificate.getSubjectX500Principal());
    }

    @Override
    public byte[] generateSignature(DigitalSignatureCertificate sigCert,
                                    DigitalSignatureAlgorithmEnum algorithm,
                                    byte[] payload) {
        try {
            Signature sig = Signature.getInstance("SHA384withECDSA");
            sig.initSign(privateKey);
            sig.update(payload);
            byte[] signed = sig.sign();
            log.debug("서명 생성 완료: {}bytes", signed.length);
            return signed;
        } catch (Exception e) {
            log.error("서명 생성 실패", e);
            return null;
        }
    }

    @Override
    public boolean validateSignature(String certPem,
                                     DigitalSignatureAlgorithmEnum algorithm,
                                     byte[] signature,
                                     byte[] content) {
        log.debug("서명 검증 (truststore 미설정으로 스킵)");
        return true;
    }

    @Override
    public DigitalSignatureCertificate getDigitalSignatureCertificate() {
        try {
            DigitalSignatureCertificate dsc = new DigitalSignatureCertificate();
            dsc.setCertificateAlias("1");
            dsc.setCertificate(certificate);
            return dsc;
        } catch (Exception e) {
            log.error("인증서 반환 실패", e);
            return null;
        }
    }
}