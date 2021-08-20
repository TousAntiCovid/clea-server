package fr.gouv.clea.integrationtests.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.gouv.clea.integrationtests.config.ApplicationProperties;
import fr.gouv.clea.integrationtests.service.model.Cluster;
import fr.gouv.clea.integrationtests.service.model.ClusterIndex;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClusterExpositionService {

    private final ObjectMapper objectMapper;

    private final MinioClient minioClient;

    private final ApplicationProperties applicationProperties;

    public ClusterIndex getClusterIndex() {
        return getJsonFile("v1/clusterIndex.json", ClusterIndex.class);
    }

    public List<Cluster> getClusterFile(int iteration, String prefix) {
        final var filePath = String.format("v1/%d/%s.json", iteration, prefix);
        final var clusters = getJsonFile(filePath, Cluster[].class);
        return List.of(clusters);
    }

    private <T> T getJsonFile(String key, Class<T> valueType) {
        final var content = getFile(key);
        try {
            return objectMapper.readValue(content, valueType);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private byte[] getFile(String key) {
        final var args = GetObjectArgs.builder()
                .bucket(applicationProperties.getBucket().getBucketName())
                .object(key)
                .build();
        try (final var minioObjectStream = minioClient.getObject(args)) {
            return IOUtils.toByteArray(minioObjectStream);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
