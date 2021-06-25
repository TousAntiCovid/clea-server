package fr.gouv.clea.integrationtests.feature.context;

import fr.gouv.clea.integrationtests.config.ApplicationProperties;
import fr.gouv.clea.integrationtests.dto.WreportResponse;
import fr.gouv.clea.integrationtests.model.Cluster;
import fr.gouv.clea.integrationtests.model.ClusterExposition;
import fr.gouv.clea.integrationtests.model.Visit;
import fr.gouv.clea.integrationtests.service.CleaS3Service;
import fr.gouv.clea.integrationtests.utils.QrCodeDecoder;
import fr.inria.clea.lsp.utils.TimeUtils;
import io.minio.errors.*;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

@Data
@Slf4j
@RequiredArgsConstructor
public class Visitor {

    private final String name;

    private final CleaS3Service s3Service;

    private final ApplicationProperties applicationProperties;

    private List<Visit> localList = new ArrayList<>();

    @Getter(AccessLevel.NONE)
    private WreportResponse lastReportResponse = null;

    public float getStatus() throws IOException, ServerException, InsufficientDataException, ErrorResponseException,
            NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException,
            InternalException {
        final var clusterIndex = s3Service.getClusterIndex().orElseThrow();
        return clusterIndex.getPrefixes().stream()
                .filter(this::matchesVisitedPlacesIds)
                .map(prefix -> getRiskLevelsFromQrCodesMatchingPrefix(clusterIndex.getIteration(), prefix))
                .flatMap(Stream::distinct)
                .flatMap(Stream::distinct)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .max(Comparator.naturalOrder()).orElse(0f);
    }

    private Stream<Stream<Optional<Float>>> getRiskLevelsFromQrCodesMatchingPrefix(final int iteration,
            final String prefix) {
        try {
            return s3Service.getClusterFile(iteration, prefix).stream()
                    .map(cluster -> localList.stream().map(qr -> getQrcodeRiskLevel(cluster, qr)));
        } catch (IOException | ServerException | InsufficientDataException | ErrorResponseException
                | NoSuchAlgorithmException | InvalidKeyException | InvalidResponseException | XmlParserException
                | InternalException e) {
            throw new RuntimeException(e);
        }
    }

    private Optional<Float> getQrcodeRiskLevel(final Cluster cluster, final Visit visit) {
        if (QrCodeDecoder.getLocationTemporaryId(visit).toString().equals(cluster.getLocationTemporaryPublicID())) {
            return cluster.getExpositions().stream()
                    .filter(exp -> exp.isInExposition(TimeUtils.instantFromTimestamp(visit.getScanTime())))
                    .map(ClusterExposition::getRisk)
                    .max(Float::compare);
        }
        return Optional.empty();
    }

    public Optional<WreportResponse> getLastReportResponse() {
        return Optional.ofNullable(lastReportResponse);
    }

    public void registerDeepLink(final String deepLink, final Instant scanTime) {

        // check if prefix is present then removes it
        if (!deepLink.startsWith(applicationProperties.getQrCodePrefix())) {
            throw new RuntimeException("Scanned deeplink has wrong prefix");
        }
        final var encodedInformation = deepLink.substring(applicationProperties.getQrCodePrefix().length());

        localList.add(
                Visit.builder()
                        .deepLinkExtractedInformation(encodedInformation)
                        .scanTime(TimeUtils.ntpTimestampFromInstant(scanTime)).build()
        );
    }

    private boolean matchesVisitedPlacesIds(final String prefix) {
        return localList.stream()
                .map(QrCodeDecoder::getLocationTemporaryId)
                .map(UUID::toString)
                .anyMatch(qrId -> qrId.startsWith(prefix));
    }
}
