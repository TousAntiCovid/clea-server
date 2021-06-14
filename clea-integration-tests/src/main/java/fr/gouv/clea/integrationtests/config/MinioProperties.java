package fr.gouv.clea.integrationtests.config;

import lombok.Data;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;

@Slf4j
@Data
@ConstructorBinding
@ConfigurationProperties("minio")
public class MinioProperties {

    private final MinioAccessProperties access;

    private final String url;

    private final String bucketName;

    private final String defaultBaseFolder;

    @Value
    static class MinioAccessProperties {

        String name;

        String secret;
    }
}

