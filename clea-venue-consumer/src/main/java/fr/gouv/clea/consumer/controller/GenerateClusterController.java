package fr.gouv.clea.consumer.controller;

import fr.gouv.clea.consumer.model.DecodedVisit;
import fr.gouv.clea.consumer.model.Visit;
import fr.gouv.clea.consumer.service.DecodedVisitService;
import fr.gouv.clea.consumer.service.VisitExpositionAggregatorService;
import fr.inria.clea.lsp.LocationSpecificPartDecoder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.validation.Valid;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
@Slf4j
public class GenerateClusterController {

    private final DecodedVisitService decodedVisitService;

    private final VisitExpositionAggregatorService visitExpositionAggregatorService;

    private final LocationSpecificPartDecoder decoder;

    @GetMapping("/cluster-declaration")
    public String generate(
            @ModelAttribute("clusterDeclarationRequest") ClusterDeclarationRequest clusterDeclarationRequest) {
        return "cluster-declaration";
    }

    @PostMapping(value = "/cluster-declaration")
    public String generate(
            @Valid @ModelAttribute("clusterDeclarationRequest") ClusterDeclarationRequest clusterDeclarationRequest,
            BindingResult result, RedirectAttributes redirectAttributes) throws Exception {
        if (result.hasErrors()) {
            log.info("Erreurs dans la déclaration du cluster : {}", clusterDeclarationRequest);
        }

        DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
        try {
            Instant date = LocalDateTime.parse(clusterDeclarationRequest.getDate(), formatter)
                    .atZone(ZoneId.of("UTC")).toInstant();
            final var urlsPart = clusterDeclarationRequest.getDeeplink().split("#");
            if (urlsPart.length > 1) {
                final var binaryLocationSpecificPart = Base64.getUrlDecoder().decode(urlsPart[1]);
                DecodedVisit decodedVisit = DecodedVisit.builder()
                        .encryptedLocationSpecificPart(decoder.decodeHeader(binaryLocationSpecificPart))
                        .qrCodeScanTime(date)
                        .isBackward(false)
                        .build();

                Optional<Visit> optionalVisit = decodedVisitService.decryptAndValidate(decodedVisit);

                optionalVisit.ifPresentOrElse(
                        visit -> {
                            log.info("Consumer: visit after decrypt + validation: {}, ", visit);
                            visitExpositionAggregatorService.updateExposureCount(visit, true);
                        },
                        () -> {
                            log.info("empty visit after decrypt + validation");
                        }
                );
            } else {
                log.info(
                        "Erreur dans le qrCode de déclaration du cluster : {} ", clusterDeclarationRequest.getDeeplink()
                );
            }
        } catch (Exception e) {
            log.info("Erreur dans la date de déclaration du cluster : {} ", clusterDeclarationRequest.getDate());
        }
        return "generate";
    }

}
