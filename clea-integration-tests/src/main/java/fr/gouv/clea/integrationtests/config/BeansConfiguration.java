package fr.gouv.clea.integrationtests.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class BeansConfiguration {

    private final MinioProperties minioProperties;

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    @Profile("docker | dev")
    public MinioClient dockerMinioClient() {
        return MinioClient.builder()
                .endpoint(minioProperties.getUrl())
                .credentials(minioProperties.getAccess().getName(), minioProperties.getAccess().getSecret())
                .build();
    }

    @Bean
    // TODO: Next step, test on actual platform. No guarantees that it works yet.
    @Profile("!(docker | dev)")
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(minioProperties.getUrl())
                .build();
    }
}
