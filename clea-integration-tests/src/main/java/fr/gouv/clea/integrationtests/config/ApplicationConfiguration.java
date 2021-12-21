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

    private final ApplicationProperties applicationProperties;

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(applicationProperties.getBucket().getUrl())
                .build();
    }
}
