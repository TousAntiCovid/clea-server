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

    Instant scanTime;

    public Visit(String locationSpecificPart, Long scanTimeAsNtpTimestamp) {
        this.locationSpecificPart = locationSpecificPart;
        this.scanTime = scanTimeAsNtpTimestamp == null ? null
                : TimeUtils.instantFromTimestamp(scanTimeAsNtpTimestamp);
    }
}
