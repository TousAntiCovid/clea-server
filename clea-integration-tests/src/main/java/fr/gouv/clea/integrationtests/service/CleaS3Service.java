package fr.gouv.clea.integrationtests.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.gouv.clea.integrationtests.config.MinioProperties;
import fr.gouv.clea.integrationtests.model.Cluster;
import fr.gouv.clea.integrationtests.model.ClusterIndex;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.errors.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CleaS3Service {

    private final ObjectMapper objectMapper;

    private final MinioClient minioClient;

    private final MinioProperties minioProperties;

    public Optional<ClusterIndex> getClusterIndex() throws IOException, ServerException, InsufficientDataException, ErrorResponseException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        byte[] file = this.getFile("clusterIndex" + ".json");
        ClusterIndex clusterIndex = objectMapper.readValue(file, ClusterIndex.class);
        return Optional.ofNullable(clusterIndex);
    }

    public List<Cluster> getClusterFile(int iteration, String prefix) throws IOException, ServerException, InsufficientDataException, ErrorResponseException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        byte[] file = this.getFile(iteration + "/" + prefix + ".json");
        return List.of(objectMapper.readValue(file, Cluster[].class));
    }

    private byte[] getFile(String key) throws IOException, ServerException, InsufficientDataException, ErrorResponseException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {

        log.info("{}, {}", minioProperties.getBucketName(), minioProperties.getDefaultBaseFolder());
        GetObjectArgs args = GetObjectArgs.builder()
                .bucket(minioProperties.getBucketName())
                .object(minioProperties.getDefaultBaseFolder() + "/" + key)
                .build();
        InputStream minioObjectStream = minioClient.getObject(args);

        byte[] content = IOUtils.toByteArray(minioObjectStream);
        minioObjectStream.close();
        return content;
    }
}
