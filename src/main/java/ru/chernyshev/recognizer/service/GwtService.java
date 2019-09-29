package ru.chernyshev.recognizer.service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import ru.chernyshev.recognizer.service.AimTokenService;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.Date;

@Service
public class GwtService {

    private static Logger logger = LoggerFactory.getLogger(AimTokenService.class);

    private final String keyPath;
    private final String keyValue;
    private final String serviceAccountId;
    private final String serviceKeyId;
    private final String tokensUrl;

    @Autowired
    public GwtService(@Value("${yandex.key.path}") String keyPath,
                      @Value("${yandex.key.value}") String keyValue,
                      @Value("${yandex.serviceAccountId}") String serviceAccountId,
                      @Value("${yandex.serviceKeyId}") String serviceKeyId,
                      @Value("${yandex.tokensUrl}") String tokensUrl) {
        this.keyPath = keyPath;
        this.keyValue = keyValue;
        this.serviceAccountId = serviceAccountId;
        this.serviceKeyId = serviceKeyId;
        this.tokensUrl = tokensUrl;
    }

    public String getGWTToken() {
        logger.info("Try generate GWT Token");
        PemObject privateKeyPem = getPemKey();
        if (privateKeyPem == null) {
            logger.error("Private key not found");
            return null;
        }

        logger.info("Private key was get");

        KeyFactory keyFactory;
        PrivateKey privateKey;
        try {
            keyFactory = KeyFactory.getInstance("RSA");
            privateKey = keyFactory.generatePrivate(new PKCS8EncodedKeySpec(privateKeyPem.getContent()));
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            logger.error("Private key read error", e);
            return null;
        }

        Instant now = Instant.now();

        // Формирование JWT.
        String encodedToken = Jwts.builder()
                .setHeaderParam("kid", serviceKeyId)
                .setIssuer(serviceAccountId)
                .setAudience(tokensUrl)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plusSeconds(360)))
                .signWith(SignatureAlgorithm.PS256, privateKey)
                .compact();

        logger.info("JWT was generated");

        return encodedToken;
    }

    private PemObject getPemKey() {
        logger.info("Start find private key");

        PemObject privateKeyPem = null;
        if (!StringUtils.isEmpty(keyPath) && new File(keyPath).isFile()) {
            logger.info("Find key file");
            try (PemReader reader = new PemReader(new FileReader(keyPath))) {
                privateKeyPem = reader.readPemObject();
            } catch (IOException e) {
                logger.error("Cant read file", e);
                return null;
            }
        }
        if (!StringUtils.isEmpty(keyValue)) {
            try {
                try (PemReader reader = new PemReader(new StringReader(keyValue))) {
                    privateKeyPem = reader.readPemObject();
                }
            } catch (IOException e) {
                logger.error("Cant read key value", e);
                return null;
            }
            logger.info("Read key value");
        }
        return privateKeyPem;
    }
}
