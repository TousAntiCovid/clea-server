package fr.gouv.clea.integrationtests.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class ApplicationConfiguration {

    private final ApplicationProperties appProperties;

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    public MinioClient dockerMinioClient() {
        return MinioClient.builder()
                .endpoint(appProperties.getBucket().getUrl())
                .credentials(appProperties.getBucket().getAccessKey(), appProperties.getBucket().getSecretKey())
                .build();
    }
}
