package fr.gouv.clea.consumer.model;

import fr.inria.clea.lsp.LocationSpecificPart;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

@SuperBuilder(toBuilder = true)
@Getter
@Setter
@ToString
public class Visit extends LocationSpecificPart {

    protected Instant qrCodeScanTime;

    protected boolean isBackward;

    public static Visit from(LocationSpecificPart lsp, DecodedVisit decodedVisit) {
        return builder()
                .version(lsp.getVersion())
                .type(lsp.getType())
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

    public String getStringLocationTemporaryPublicId() {
        return this.getLocationTemporaryPublicId().toString();
    }

}
