package fr.gouv.clea.consumer.controller;

import fr.gouv.clea.consumer.model.Cluster;
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
public class GenerateClusterController {

    private final DecodedVisitService decodedVisitService;

    private final VisitExpositionAggregatorService visitExpositionAggregatorService;

    @GetMapping("/generate")
    public String generate(@ModelAttribute("cluster") Cluster cluster) {
        return "generate";
    }

    @PostMapping(value = "/generate")
    public String generate(@Valid @ModelAttribute("cluster") Cluster cluster,
            BindingResult result, RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            log.error("Erreurs dans la déclaration du cluster");
        }

        Optional<Visit> optionalVisit = decodedVisitService.decryptAndValidate(
                new DecodedVisit(
                        ZonedDateTime.parse(cluster.getDate(), DateTimeFormatter.ofPattern("yyyy-MM-ddThh:mm"))
                                .toInstant(),
                        EncryptedLocationSpecificPart.builder()
                                .locationTemporaryPublicId(UUID.fromString(cluster.getDeeplink())).build(),
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

        return "generate";

    }

}
