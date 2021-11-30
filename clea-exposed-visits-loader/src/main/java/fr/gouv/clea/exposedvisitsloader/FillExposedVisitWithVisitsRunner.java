package fr.gouv.clea.exposedvisitsloader;

import fr.gouv.clea.entity.ExposedVisit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import static java.time.LocalDate.of;
import static java.util.UUID.randomUUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class FillExposedVisitWithVisitsRunner implements ApplicationRunner {

    private final DataSource ds;

    public static final int NUMBER_OF_LOCATIONS_TO_GENERATE = 500;

    public static final int MAX_VISITS_PER_PLACE = 80;

    private final Random random = new Random();

    @Override
    public void run(ApplicationArguments args) {

        final var date = of(2021, 1, 1);

        final var jdbcTemplate = new JdbcTemplate(ds);

        log.info("Starting to fill EXPOSED_VISITS...");

        for (int l = 0; l < NUMBER_OF_LOCATIONS_TO_GENERATE; l++) {
            UUID locationId = randomUUID();
            final var venueType = random.nextInt(18) + 1; // 1 to 18
            final var venueCategory1 = random.nextInt(4) + 1; // 1 to 4
            final var venueCategory2 = random.nextInt(4) + 1; // 1 to 4

            List<ExposedVisit> batch = new ArrayList<>();

            final var clusterStart = date.toEpochDay() + (random.nextInt(65) * 1000L);
            final var visitsPerPlace = random.nextInt(MAX_VISITS_PER_PLACE);
            for (var slot = 0; slot <= visitsPerPlace; slot++) {

                //@formatter:off
                final var exposedVisit = ExposedVisit.builder()
                        .locationTemporaryPublicId(locationId)
                        .venueType(venueType)
                        .venueCategory1(venueCategory1)
                        .venueCategory2(venueCategory2)
                        .periodStart(clusterStart)
                        .timeSlot(slot)
                        .forwardVisits(random.nextInt(2) * random.nextInt(100)) //  50%=0 or 50%= (1 to 10)
                        .backwardVisits(random.nextInt(2) * random.nextInt(100))
                        .build();
                //@formatter:on

                batch.add(exposedVisit);
            }
            jdbcTemplate.batchUpdate(
                    "insert into EXPOSED_VISITS (LTId, venue_type, venue_category1, venue_category2, period_start, timeslot,backward_Visits, forward_Visits)"
                            +
                            "values (?,?,?,?,?,?,?,?)",
                    batch,
                    10,
                    (ps, visit) -> {
                        ps.setObject(1, visit.getLocationTemporaryPublicId());
                        ps.setObject(2, visit.getVenueType());
                        ps.setObject(3, visit.getVenueCategory1());
                        ps.setObject(4, visit.getVenueCategory2());
                        ps.setObject(5, visit.getPeriodStart());
                        ps.setObject(6, visit.getTimeSlot());
                        ps.setObject(7, visit.getBackwardVisits());
                        ps.setObject(8, visit.getForwardVisits());
                    }
            );
        }
        log.info(
                "Nb records in EXPOSED_VISITS: "
                        + jdbcTemplate.queryForObject("select count(*) from EXPOSED_VISITS", Integer.class)
        );
    }
}
