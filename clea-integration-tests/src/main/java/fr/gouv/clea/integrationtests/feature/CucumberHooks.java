package fr.gouv.clea.integrationtests.feature;

import io.cucumber.java.Before;
import lombok.RequiredArgsConstructor;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.IOException;

@RequiredArgsConstructor
public class CucumberHooks {

    private final RestHighLevelClient esClient;

    private final JdbcTemplate jdbcTemplate;

    @Before
    public void emptyIndices() throws IOException {
        esClient.indices().delete(new DeleteIndexRequest("*"), RequestOptions.DEFAULT);
    }

    @Before
    public void emptyTables() {
        jdbcTemplate.execute("truncate table exposed_visits, cluster_periods, stat_location, stat_reports;");
    }
}
