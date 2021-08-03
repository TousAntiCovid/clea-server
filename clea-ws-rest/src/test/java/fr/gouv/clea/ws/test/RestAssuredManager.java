package fr.gouv.clea.ws.test;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import io.restassured.RestAssured;
import io.restassured.specification.RequestSpecification;
import lombok.SneakyThrows;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

import static io.restassured.http.ContentType.JSON;
import static java.lang.String.format;
import static java.time.temporal.ChronoUnit.MINUTES;
import static org.springframework.http.HttpHeaders.ACCEPT_LANGUAGE;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;

public class RestAssuredManager implements TestExecutionListener {

    public static final KeyPair JWT_KEY_PAIR;

    static {
        // generate a JWT key pair and export system property to configure the test
        // application
        try {
            JWT_KEY_PAIR = KeyPairGenerator.getInstance("RSA")
                    .generateKeyPair();
            final var jwtPublicKey = Base64.getEncoder()
                    .encodeToString(JWT_KEY_PAIR.getPublic().getEncoded());
            System.setProperty("clea.conf.robert-jwt-public-key", jwtPublicKey);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void beforeTestMethod(TestContext testContext) {
        RestAssured.port = testContext.getApplicationContext()
                .getEnvironment()
                .getRequiredProperty("local.server.port", Integer.class);
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    public static JWTClaimsSet.Builder defaultJwtClaims() {
        return new JWTClaimsSet.Builder()
                .jwtID(UUID.randomUUID().toString())
                .issueTime(Date.from(Instant.now()))
                .expirationTime(Date.from(Instant.now().plus(2, MINUTES)));
    }

    private static RequestSpecification givenBaseHeaders() {
        return RestAssured.given()
                .accept(JSON)
                .header(ACCEPT_LANGUAGE, Locale.US);
    }

    public static RequestSpecification givenAuthenticated() {
        return givenBaseHeaders()
                .header(AUTHORIZATION, format("Bearer %s", generateToken(defaultJwtClaims())));
    }

    public static RequestSpecification givenJwt(JWTClaimsSet.Builder claims) {
        return givenBaseHeaders()
                .header(AUTHORIZATION, format("Bearer %s", generateToken(claims)));
    }

    @SneakyThrows
    private static String generateToken(JWTClaimsSet.Builder claims) {
        final var header = new JWSHeader.Builder(JWSAlgorithm.RS256).build();
        final var signedJWT = new SignedJWT(header, claims.build());
        signedJWT.sign(new RSASSASigner(JWT_KEY_PAIR.getPrivate()));
        return signedJWT.serialize();
    }
}
