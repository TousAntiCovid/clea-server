package fr.gouv.clea.integrationtests.feature;

import io.cucumber.java.Before;
import lombok.RequiredArgsConstructor;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;

import java.io.IOException;

@RequiredArgsConstructor
public class CucumberHooks {

    private final RestHighLevelClient esClient;

    @Before
    public void clearElasticIndices() throws IOException {
        esClient.indices().delete(new DeleteIndexRequest("*"), RequestOptions.DEFAULT);
    }
}
