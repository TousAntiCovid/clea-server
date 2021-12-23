package fr.gouv.clea.integrationtests.cucumber;

import io.cucumber.java.Before;
import lombok.RequiredArgsConstructor;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;

import java.io.IOException;

// TODO: rework tests so that they don't work only with empty ES indices
@RequiredArgsConstructor
public class CucumberHooks {

    private final RestHighLevelClient esClient;

    @Before
    public void clearElasticIndices() throws IOException {
        esClient.indices().delete(new DeleteIndexRequest("*"), RequestOptions.DEFAULT);
    }
}
