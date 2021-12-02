package fr.gouv.clea.consumer.controller;

import fr.gouv.clea.consumer.repository.visits.ExposedVisitRepository;
import fr.gouv.clea.consumer.test.IntegrationTest;
import fr.gouv.clea.consumer.test.ReferenceData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@IntegrationTest
public class GenerateClusterControllerTest {

    private final DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;

    @Autowired
    private ExposedVisitRepository repository;

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    private String deeplink;

    private String date;

    private MultiValueMap<String, String> clusterParams;

    @BeforeEach
    public void setup() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.webApplicationContext).build();
        deeplink = ReferenceData.LOCATION_1_URL.toString();
        LocalDateTime localDate = LocalDateTime.now().plus(1, ChronoUnit.HOURS);
        date = localDate.format(formatter);
        clusterParams = new LinkedMultiValueMap<>();
    }

    @Test
    void create_cluster_manually_with_correct_deeplink_and_date_thenOk() throws Exception {

        clusterParams.add("deeplink", deeplink);
        clusterParams.add("date", date);

        this.mockMvc.perform(
                post("/cluster-declaration").params(clusterParams).contentType(MediaType.APPLICATION_FORM_URLENCODED)
        ).andDo(print()).andExpect(status().isOk());

        var exposedVisitList = repository.findAll();

        assertThat(exposedVisitList)
                .allMatch(
                        exposedVisit -> exposedVisit.getLocationTemporaryPublicId()
                                .equals(ReferenceData.LOCATION_1_LOCATION_TEMPORARY_SPECIFIC_ID),
                        "has uuid" + ReferenceData.LOCATION_1_LOCATION_TEMPORARY_SPECIFIC_ID
                )
                .allMatch(exposedVisit -> exposedVisit.getForwardVisits() == 100, "has 100 forward visits");
    }

    @Test
    void create_cluster_manually_with_wrong_deeplink_and_correct_date_then_no_visit_save_in_database()
            throws Exception {

        clusterParams.add("deeplink", "test");
        clusterParams.add("date", date);

        this.mockMvc.perform(
                post("/cluster-declaration").params(clusterParams).contentType(MediaType.APPLICATION_FORM_URLENCODED)
        ).andDo(print()).andExpect(status().isOk());

        var exposedVisitList = repository.findAll();
        assertThat(exposedVisitList).isEmpty();

    }

    @Test
    void create_cluster_manually_with_correct_deeplink_and_wrong_date_then_no_visit_save_in_database()
            throws Exception {

        clusterParams.add("deeplink", deeplink);
        clusterParams.add("date", "");

        this.mockMvc.perform(
                post("/cluster-declaration").params(clusterParams).contentType(MediaType.APPLICATION_FORM_URLENCODED)
        ).andDo(print()).andExpect(status().isOk());

        var exposedVisitList = repository.findAll();
        assertThat(exposedVisitList).isEmpty();

    }

}
