package fr.gouv.clea.ws.service.model;

import fr.inria.clea.lsp.utils.TimeUtils;
import lombok.AllArgsConstructor;
import lombok.Value;

import java.time.Instant;

@Value
@AllArgsConstructor
public class Visit {

    /**
     * The locationSpecificPart is Base64url encoded.
     */
    String locationSpecificPart;

    Instant qrCodeScanTime;

    public Visit(String locationSpecificPart, Long qrCodeScanTimeAsNtpTimestamp) {
        this.locationSpecificPart = locationSpecificPart;
        this.qrCodeScanTime = qrCodeScanTimeAsNtpTimestamp == null ? null
                : TimeUtils.instantFromTimestamp(qrCodeScanTimeAsNtpTimestamp);
    }
}
