package fr.gouv.clea.consumer.controller;

import fr.gouv.clea.consumer.model.Generate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.validation.Valid;

@Controller
@Slf4j
public class GenerateClusterController {

    @GetMapping("/generate")
    public String generate(@ModelAttribute("generate") Generate generate) {
        return "generate";
    }

    @PostMapping(value = "/generate")
    public String generate(@Valid @ModelAttribute("generate") Generate generate, ModelMap model,
            RedirectAttributes redirectAttributes) {

        model.addAttribute("deeplink", generate.getDeeplink());
        model.addAttribute("date", generate.getDate());

        log.info(model.toString());

        redirectAttributes.addFlashAttribute("success", "Success");

        return "generate";
    }

}
