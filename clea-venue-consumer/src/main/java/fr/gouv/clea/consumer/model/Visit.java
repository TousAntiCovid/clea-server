package fr.gouv.clea.consumer.model;

import java.time.Instant;

import fr.inria.clea.lsp.LocationSpecificPart;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Getter
@ToString
public class Visit extends LocationSpecificPart {
    
    public static Visit from(LocationSpecificPart lsp, DecodedVisit decodedVisit) {
        return builder()
            .version(lsp.getVersion())
            .type(lsp.getType())
            .countryCode(lsp.getCountryCode())
            .locationTemporaryPublicId(lsp.getLocationTemporaryPublicId())
            .qrCodeRenewalIntervalExponentCompact(lsp.getQrCodeRenewalIntervalExponentCompact())
            .venueType(lsp.getVenueType())
            .venueCategory1(lsp.getVenueCategory1())
            .venueCategory2(lsp.getVenueCategory2())
            .periodDuration(lsp.getPeriodDuration())
            .compressedPeriodStartTime(lsp.getCompressedPeriodStartTime())
            .qrCodeValidityStartTime(lsp.getQrCodeValidityStartTime())
            .locationTemporarySecretKey(lsp.getLocationTemporarySecretKey())
            .encryptedLocationContactMessage(lsp.getEncryptedLocationContactMessage())
            .qrCodeScanTime(decodedVisit.getQrCodeScanTime())
            .isBackward(decodedVisit.isBackward())
            .build();
    }
    
    protected Instant qrCodeScanTime;
    protected boolean isBackward;
    
}
