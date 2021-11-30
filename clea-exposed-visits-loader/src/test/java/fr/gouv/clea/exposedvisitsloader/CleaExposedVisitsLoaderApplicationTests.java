package fr.gouv.clea.exposedvisitsloader;

import fr.gouv.clea.exposedvisitsloader.test.IntegrationTest;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

import static fr.gouv.clea.exposedvisitsloader.Runner.MAX_VISITS_PER_PLACE;
import static fr.gouv.clea.exposedvisitsloader.Runner.NUMBER_OF_LOCATIONS_TO_GENERATE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;

@Slf4j
@IntegrationTest
class CleaExposedVisitsLoaderApplicationTests {

    @Autowired
    private DataSource ds;

    @Autowired
    private Runner runner;

    @Test
    void verify_runner_loaded_NUMBER_OF_LOCATIONS_TO_GENERATE_ltids_in_base() {
        final JdbcTemplate jdbcTemplate = new JdbcTemplate(ds);

        // execute code loading visits
        runner.run(null);

        // verify exact count of ltids
        assertThat(
                jdbcTemplate.queryForObject("select count(distinct ltid) from exposed_visits", Integer.class),
                equalTo(NUMBER_OF_LOCATIONS_TO_GENERATE)
        );
        // verify approximate count of visits (due to random number of visits per place)
        assertThat(
                jdbcTemplate.queryForObject("select count(*) from exposed_visits", Integer.class),
                lessThanOrEqualTo(NUMBER_OF_LOCATIONS_TO_GENERATE * MAX_VISITS_PER_PLACE)
        );
    }
}
