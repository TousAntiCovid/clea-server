package fr.gouv.clea.ws.service;

import fr.gouv.clea.ws.model.DecodedVisit;
import lombok.RequiredArgsConstructor;

import java.time.Duration;
import java.time.Instant;

@RequiredArgsConstructor
public class VisitsInSameUnitCounter {

    private final long exposureTimeUnit;

    private int count = 0;

    private Instant lastScanTime = null;

    public int getCount() {
        return count;
    }

    public DecodedVisit incrementIfScannedInSameTimeUnitThanLastScanTime(final DecodedVisit decodedVisit) {
        final Instant qrCodeScanTime = decodedVisit.getQrCodeScanTime();
        if (lastScanTime != null && visitIsScannedAfterLessThanExposureTime(qrCodeScanTime)) {
            count++;
        }
        lastScanTime = qrCodeScanTime;
        return decodedVisit;
    }

    private boolean visitIsScannedAfterLessThanExposureTime(Instant qrCodeScanTime) {
        return Duration.between(lastScanTime, qrCodeScanTime).getSeconds() < exposureTimeUnit;
    }
}
