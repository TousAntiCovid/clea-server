package fr.gouv.clea.integrationtests.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.With;

@Value
@With
@Builder
@AllArgsConstructor
public class Visit {

    @JsonProperty("qrCode")
    String localPart;

    @JsonProperty("qrCodeScanTime")
    Long scanTime;
}
