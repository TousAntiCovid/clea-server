package fr.gouv.clea.integrationtests.service.visitorsimulator;

import fr.gouv.clea.integrationtests.service.ClusterExpositionService;
import fr.gouv.clea.integrationtests.utils.QrCodeDecoder;
import fr.inria.clea.lsp.utils.TimeUtils;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.*;

@RequiredArgsConstructor
public class Visitor {

    private static final String DEEPLINK_COUNTRY_PART = "https://tac.gouv.fr?v=0#";

    @Getter
    private final String name;

    private final ClusterExpositionService clusterExpositionService;

    private final List<Visit> localVisitsList = new ArrayList<>();

    @Setter
    private WreportResponse lastReportResponse = null;

    public List<Visit> getLocalVisitsList() {
        return Collections.unmodifiableList(localVisitsList);
    }

    public float getStatus() {
        return localVisitsList.stream()
                .map(this::getRiskLevelForVisit)
                .max(Comparator.naturalOrder())
                .orElse(0f);
    }

    private float getRiskLevelForVisit(Visit visit) {
        final var ltid = QrCodeDecoder.getLocationTemporaryPublicId(visit);
        final var visitTime = TimeUtils.instantFromTimestamp(visit.getScanTime());
        return clusterExpositionService.getRiskLevelForPlaceAtInstant(ltid.toString(), visitTime);
    }

    public Optional<WreportResponse> getLastReportResponse() {
        return Optional.ofNullable(lastReportResponse);
    }

    public void registerDeepLink(final String deepLink, final Instant scanTime) {

        // check if prefix is present then removes it
        if (!deepLink.startsWith(DEEPLINK_COUNTRY_PART)) {
            throw new RuntimeException("Scanned deeplink has wrong prefix");
        }
        final var encodedInformation = deepLink.substring(DEEPLINK_COUNTRY_PART.length());

        localVisitsList.add(
                Visit.builder()
                        .deepLinkLocationSpecificPart(encodedInformation)
                        .scanTime(TimeUtils.ntpTimestampFromInstant(scanTime)).build()
        );
    }
}
