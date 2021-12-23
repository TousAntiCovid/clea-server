package fr.gouv.clea.integrationtests.config;

import lombok.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import java.net.URL;

@Value
@Validated
@ConstructorBinding
@ConfigurationProperties("integration-tests")
public class ApplicationProperties {

    @NotNull
    String serverAuthorityPublicKey;

    @NotNull
    String manualContactTracingAuthorityPublicKey;

    CleaWebService wsRest;

    CleaWebService venueConsumer;

    Batch batch;

    Bucket bucket;

    @Value
    public static class Batch {

        @NotBlank
        String command;
    }

    @Value
    public static class CleaWebService {

        @NotNull
        URL baseUrl;

        URL managementUrl;
    }

    @Value
    public static class Bucket {

        @NotNull
        URL url;

        @NotBlank
        String name;
    }
}
