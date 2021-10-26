package fr.gouv.clea.ws.model;

import fr.inria.clea.lsp.EncryptedLocationSpecificPart;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Builder
@Value
public class DecodedVisit {

    Instant qrCodeScanTime; // t_qrScan

    EncryptedLocationSpecificPart encryptedLocationSpecificPart;

    boolean isBackward;

    public UUID getLocationTemporaryPublicId() {
        return this.encryptedLocationSpecificPart.getLocationTemporaryPublicId();
    }

    public String getStringLocationTemporaryPublicId() {
        return this.getLocationTemporaryPublicId().toString();
    }

    public boolean isForward() {
        return !this.isBackward();
    }
}
