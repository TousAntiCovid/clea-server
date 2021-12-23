package fr.gouv.clea.integrationtests.cucumber;

import io.cucumber.java.Before;
import lombok.RequiredArgsConstructor;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

import java.io.IOException;

@RequiredArgsConstructor
public class CucumberHooks {

    private final RestHighLevelClient esClient;

    private final Environment environment;

    @Before
    public void clearElasticIndices() throws IOException {
        if (!environment.acceptsProfiles(Profiles.of("int"))) {
            esClient.indices().delete(new DeleteIndexRequest("*"), RequestOptions.DEFAULT);
        }
    }
}
