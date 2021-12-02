package fr.gouv.clea.consumer.controller;

import fr.gouv.clea.consumer.model.ClusterDeclarationRequest;
import fr.gouv.clea.consumer.model.DecodedVisit;
import fr.gouv.clea.consumer.model.Visit;
import fr.gouv.clea.consumer.service.DecodedVisitService;
import fr.gouv.clea.consumer.service.VisitExpositionAggregatorService;
import fr.inria.clea.lsp.EncryptedLocationSpecificPart;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.validation.Valid;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
@Slf4j
public class ClusterDeclarationController {

    private final DecodedVisitService decodedVisitService;

    private final VisitExpositionAggregatorService visitExpositionAggregatorService;

    @GetMapping("/cluster-declaration")
    public String generate(
            @ModelAttribute("clusterDeclarationRequest") ClusterDeclarationRequest clusterDeclarationRequest) {
        return "cluster-declaration";
    }

    @PostMapping(value = "/cluster-declaration")
    public String generate(
            @Valid @ModelAttribute("clusterDeclarationRequest") ClusterDeclarationRequest clusterDeclarationRequest,
            BindingResult result,
            RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            log.error("Erreurs dans la déclaration du cluster");
        }

        Optional<Visit> optionalVisit = decodedVisitService.decryptAndValidate(
                new DecodedVisit(
                        ZonedDateTime.parse(
                                clusterDeclarationRequest.getDate(), DateTimeFormatter.ofPattern("yyyy-MM-ddThh:mm")
                        )
                                .toInstant(),
                        EncryptedLocationSpecificPart.builder()
                                .locationTemporaryPublicId(UUID.fromString(clusterDeclarationRequest.getDeeplink()))
                                .build(),
                        false
                )
        );

        optionalVisit.ifPresentOrElse(
                visit -> {
                    log.debug("Consumer: visit after decrypt + validation: {}, ", visit);
                    visitExpositionAggregatorService.updateExposureCount(visit, true);
                },
                () -> log.info("empty visit after decrypt + validation")
        );

        redirectAttributes.addFlashAttribute("success", "Success");

        return "cluster-declaration";

    }

}
