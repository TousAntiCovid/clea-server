package fr.gouv.clea.integrationtests.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.gouv.clea.ApiClient;
import fr.gouv.clea.api.CleaApi;
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

    private final ApplicationProperties applicationProperties;

    private final MinioProperties minioProperties;

    @Bean
    public CleaApi cleaApi() {
        CleaApi cleaApi = new CleaApi();
        final ApiClient apiClient = new ApiClient();
        apiClient.setBasePath(applicationProperties.getWsRest().getBaseUrl().toString());

        cleaApi.setApiClient(apiClient);
        return cleaApi;
    }

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
    //TODO: Next step, test on actual platform. No guarantees that it works yet.
    @Profile("!(docker | dev)")
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(minioProperties.getUrl())
                .build();
    }
}
