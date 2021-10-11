package fr.gouv.clea.qr.model;

import fr.inria.clea.lsp.EncryptedLocationSpecificPart;
import fr.inria.clea.lsp.LocationSpecificPartDecoder;
import fr.inria.clea.lsp.exception.CleaEncodingException;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class QRCode {

    private URL deepLink;

    private Instant qrCodeValidityStartTime;

    private long qrCodeRenewalInterval;

    public QRCode(String deepLink, Instant qrCodeValidityStartTime, long qrCodeRenewalInterval)
            throws MalformedURLException {
        this(new URL(deepLink), qrCodeValidityStartTime, qrCodeRenewalInterval);
    }

    public boolean isValidScanTime(Instant instant) {
        if (this.qrCodeRenewalInterval > 0) {
            Instant qrCodeValidityEndTime = qrCodeValidityStartTime.plus(qrCodeRenewalInterval, ChronoUnit.SECONDS);
            return (instant.isAfter(qrCodeValidityStartTime) || instant.equals(qrCodeValidityStartTime))
                    && (instant.isBefore(qrCodeValidityEndTime) || instant.equals(qrCodeValidityEndTime));
        } else {
            return instant.isAfter(qrCodeValidityStartTime) || instant.equals(qrCodeValidityStartTime);
        }
    }

    public UUID getLocationTemporaryPublicID() {
        EncryptedLocationSpecificPart encryptedLsp;
        try {
            encryptedLsp = new LocationSpecificPartDecoder().decodeHeader(
                    Base64.getUrlDecoder().decode(deepLink.getRef())
            );
        } catch (CleaEncodingException e) {
            throw new RuntimeException(e);
        }
        return encryptedLsp.getLocationTemporaryPublicId();
    }
}
