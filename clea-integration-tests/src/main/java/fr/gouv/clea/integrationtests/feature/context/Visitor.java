package fr.gouv.clea.integrationtests.feature.context;

import fr.gouv.clea.ApiException;
import fr.gouv.clea.api.CleaApi;
import fr.gouv.clea.integrationtests.config.ApplicationProperties;
import fr.gouv.clea.integrationtests.model.Cluster;
import fr.gouv.clea.integrationtests.model.ClusterExposition;
import fr.gouv.clea.integrationtests.model.ClusterIndex;
import fr.gouv.clea.integrationtests.service.CleaS3Service;
import fr.gouv.clea.integrationtests.utils.QrCodeDecoder;
import fr.gouv.clea.model.ReportRequest;
import fr.gouv.clea.model.ReportResponse;
import fr.gouv.clea.model.Visit;
import fr.inria.clea.lsp.exception.CleaEncodingException;
import fr.inria.clea.lsp.utils.TimeUtils;
import io.minio.errors.*;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Data
@Slf4j
@RequiredArgsConstructor
public class Visitor {

    private final String name;
    private final CleaApi cleaApi;
    private final CleaS3Service s3Service;
    private final ApplicationProperties applicationProperties;
    private List<Visit> localList = new ArrayList<>();

    @Getter(AccessLevel.NONE)
    private ReportResponse lastReportResponse = null;

    public float getStatus() throws IOException, CleaEncodingException, ServerException, InsufficientDataException, ErrorResponseException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        ClusterIndex clusterIndex = s3Service.getClusterIndex().orElseThrow();
        Set<String> matchingPrefixes = this.getClusterFilesMatchingPrefix(localList, clusterIndex);
        int iteration = clusterIndex.getIteration();
        List<Float> scores = new ArrayList<>();
        // gather all potential clusters
        for (String prefix : matchingPrefixes) {
            List<Cluster> clusters = s3Service.getClusterFile(iteration, prefix);
            for (Cluster cluster : clusters) {
                for (Visit qr : localList) {
                    getQrRiskLevel(qr, cluster).ifPresent(scores::add);
                }
            }
        }
        return scores.stream().max(Comparator.naturalOrder()).orElse(0f);

    }

    public Optional<ReportResponse> getLastReportResponse() {
        return Optional.ofNullable(lastReportResponse);
    }

    public void sendReportWithEmptyQrCodeField(final Instant pivotDate) throws ApiException {
        final List<Visit> alteredLocalList = this.emptyLocalListQrCodesFields();
        sendReportAndSaveResponse(pivotDate, alteredLocalList);
    }

    public void sendReportWithNullQrCodeField(final Instant pivotDate) throws ApiException {
        final List<Visit> alteredVisitsList = this.nullifyLocalListQrCodesFields();
        sendReportAndSaveResponse(pivotDate, alteredVisitsList);
    }

    public void sendReportWithMalformedScanTime(final Instant pivotDate) throws ApiException {
        final List<Visit> alteredVisitsList = this.malformLocalListScanTimes();
        sendReportAndSaveResponse(pivotDate, alteredVisitsList);
    }

    public void sendReportWithNullScanTime(final Instant pivotDate) throws ApiException {
        final List<Visit> alteredVisitsList = this.nullifyLocalListScanTimes();
        sendReportAndSaveResponse(pivotDate, alteredVisitsList);
    }

    public void sendReportWithEmptyPivotDate() throws ApiException {
        sendReportAndSaveResponse(null);
    }

    public void sendReportAndSaveResponse(final Instant pivotDate) throws ApiException {
        sendReportAndSaveResponse(pivotDate, localList);
    }

    public void sendReportAndSaveResponse() throws ApiException {
        //Backend should accept report without pivot date, not the case right now, using 14 day ago pivot date instead
        this.sendReportAndSaveResponse(Instant.now().minus(Duration.ofDays(14)));
    }

    public void scanQrCode(String qrCode, Instant scanTime) {

        //check if prefix is present then removes it
        if (!qrCode.startsWith(applicationProperties.getQrCodePrefix())) {
            return;
        }
        qrCode = qrCode.substring(applicationProperties.getQrCodePrefix().length());
        Visit scannedVisit = new Visit();
        scannedVisit.setQrCode(qrCode);
        scannedVisit.setQrCodeScanTime(TimeUtils.ntpTimestampFromInstant(scanTime));

        localList.add(scannedVisit);
    }

    protected Set<String> getClusterFilesMatchingPrefix(final List<Visit> localList, final ClusterIndex clusterIndex) {
        return clusterIndex.getPrefixes().stream().filter(prefix ->
                localList.stream().map(visit -> {
                    try {
                        return QrCodeDecoder.getLocationTemporaryId(visit);
                    } catch (CleaEncodingException e) {
                        log.error("an error occured during qr code decoding.Visit: {} ; error : {}", visit, e.getMessage());
                    }
                    return null;
                })
                        .filter(Objects::nonNull)
                        .map(UUID::toString)
                        .anyMatch(qrId -> qrId.startsWith(prefix))
        ).collect(Collectors.toSet());
    }

    protected Optional<Float> getQrRiskLevel(final Visit qr, final Cluster cluster) throws CleaEncodingException {
        Optional<Float> result = Optional.empty();

        if (QrCodeDecoder.getLocationTemporaryId(qr).toString().equals(cluster.getLocationTemporaryPublicID())) {
            for (ClusterExposition exposition : cluster.getExpositions()) {
                if (exposition.isInExposition(TimeUtils.instantFromTimestamp(qr.getQrCodeScanTime()))) {
                    float newRisk = Math.max(result.orElse(0f), exposition.getRisk());
                    result = Optional.of(newRisk);
                }
            }
        }
        return result;
    }

    private void sendReportAndSaveResponse(final Instant pivotDate, final List<Visit> alteredVisitsList) throws ApiException {
        final ReportRequest reportRequest = buildReportRequest(pivotDate, alteredVisitsList);
        final ReportResponse response = cleaApi.reportUsingPOST(reportRequest);
        lastReportResponse = Optional.ofNullable(response).orElse(null);
    }

    private ReportRequest buildReportRequest(final Instant pivotDate, final List<Visit> visits) {
        final ReportRequest reportRequest = new ReportRequest();

        reportRequest.setPivotDate(Optional.of(TimeUtils.ntpTimestampFromInstant(pivotDate)).orElse(null));
        reportRequest.setVisits(visits);
        return reportRequest;
    }

    private List<Visit> malformLocalListScanTimes() {
        return localList.stream()
                .map(visit -> {
                    visit.setQrCodeScanTime(-1L);
                    return visit;
                })
                .collect(Collectors.toList());
    }

    private List<Visit> nullifyLocalListScanTimes() {
        return localList.stream()
                .map(visit -> {
                    visit.setQrCodeScanTime(null);
                    return visit;
                })
                .collect(Collectors.toList());
    }

    private List<Visit> emptyLocalListQrCodesFields() {
        return localList.stream()
                .map(visit -> {
                    visit.setQrCode("");
                    return visit;
                })
                .collect(Collectors.toList());
    }

    private List<Visit> nullifyLocalListQrCodesFields() {
        return localList.stream()
                .map(visit -> {
                    visit.setQrCode(null);
                    return visit;
                })
                .collect(Collectors.toList());
    }
}
