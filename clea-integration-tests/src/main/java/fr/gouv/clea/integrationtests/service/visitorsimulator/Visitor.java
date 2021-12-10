package fr.gouv.clea.integrationtests.service.visitorsimulator;

import fr.gouv.clea.integrationtests.config.ApplicationProperties;
import fr.gouv.clea.integrationtests.service.ClusterExpositionService;
import fr.gouv.clea.integrationtests.service.model.Cluster;
import fr.gouv.clea.integrationtests.service.model.ClusterExposition;
import fr.gouv.clea.integrationtests.utils.DeepLinkDecoder;
import fr.inria.clea.lsp.utils.TimeUtils;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.net.URL;
import java.time.Instant;
import java.util.*;
import java.util.stream.Stream;

import static fr.inria.clea.lsp.utils.TimeUtils.ntpTimestampFromInstant;

@Data
@Slf4j
@RequiredArgsConstructor
public class Visitor {

    private final String name;

    private final ClusterExpositionService s3Service;

    private final ApplicationProperties applicationProperties;

    private List<Visit> localList = new ArrayList<>();

    @Getter(AccessLevel.NONE)
    private WreportResponse lastReportResponse = null;

    public float getStatus() {
        final var clusterIndex = s3Service.getClusterIndex();
        return clusterIndex.getPrefixes().stream()
                .filter(this::matchesVisitedPlacesIds)
                .map(prefix -> getRiskLevelsFromDeepLinksMatchingPrefix(clusterIndex.getIteration(), prefix))
                .flatMap(Stream::distinct)
                .flatMap(Stream::distinct)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .max(Comparator.naturalOrder()).orElse(0f);
    }

    private Stream<Stream<Optional<Float>>> getRiskLevelsFromDeepLinksMatchingPrefix(final int iteration,
            final String prefix) {
        return s3Service.getClusterFile(iteration, prefix).stream()
                .map(
                        cluster -> localList.stream()
                                .map(deepLink -> getRiskLevel(cluster, deepLink))
                );
    }

    private Optional<Float> getRiskLevel(final Cluster cluster, final Visit visit) {
        UUID locationTemporaryId = DeepLinkDecoder.getLocationTemporaryId(visit);
        if (locationTemporaryId.toString().equals(cluster.getLocationTemporaryPublicID())) {
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

    public void registerDeepLink(final URL deepLink, final Instant scanTime) {
        localList.add(
                Visit.builder()
                        .deepLinkLocationSpecificPart(deepLink.getRef())
                        .scanTime(ntpTimestampFromInstant(scanTime)).build()
        );
    }

    private boolean matchesVisitedPlacesIds(final String prefix) {
        return localList.stream()
                .map(DeepLinkDecoder::getLocationTemporaryId)
                .map(UUID::toString)
                .anyMatch(qrId -> qrId.startsWith(prefix));
    }
}
