package fr.gouv.clea.integrationtests.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Visit {

    private String qrCode;

    private Long qrCodeScanTime;
}
