package fr.gouv.clea.consumer.test;

import lombok.SneakyThrows;
import org.apache.http.HttpHost;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.ListAssert;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.refresh.RefreshRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.client.Node;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.FreezeIndexRequest;
import org.elasticsearch.search.SearchHit;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Arrays.stream;

public class ElasticManager implements TestExecutionListener {

    private static final ElasticsearchContainer ELASTIC = new ElasticsearchContainer(
            DockerImageName.parse("docker.elastic.co/elasticsearch/elasticsearch:7.6.2")
    );

    private static RestHighLevelClient ES_CLIENT;

    static {
        ELASTIC.start();
        System.setProperty("spring.elasticsearch.rest.uris", ELASTIC.getHttpHostAddress());

        ES_CLIENT = new RestHighLevelClient(
                RestClient.builder(new Node(HttpHost.create(ELASTIC.getHttpHostAddress())))
        );
    }

    @Override
    public void beforeTestMethod(TestContext testContext) throws Exception {
        ES_CLIENT.indices().delete(new DeleteIndexRequest("_all"), RequestOptions.DEFAULT);
    }

    @SneakyThrows
    public static ListAssert<Map<String, Object>> assertThatAllDocumentsFromElastic() {
        // refresh indices to be sure to find up to date data
        ES_CLIENT.indices().refresh(new RefreshRequest("_all"), RequestOptions.DEFAULT);

        // fetch all documents
        final var response = ES_CLIENT.search(new SearchRequest(), RequestOptions.DEFAULT);
        final List<Map<String, Object>> collect = stream(response.getHits().getHits())
                .map(SearchHit::getSourceAsMap)
                .peek(hit -> hit.remove("_class"))
                .collect(Collectors.toList());
        return Assertions.assertThat(collect);
    }

    @SneakyThrows
    public static void givenElasticIndexIsFrozen(String indexName) {
        try {
            ES_CLIENT.indices().create(new CreateIndexRequest(indexName), RequestOptions.DEFAULT);
        } catch (ElasticsearchException ignored) {
        }
        ES_CLIENT.indices().freeze(new FreezeIndexRequest(indexName), RequestOptions.DEFAULT);
    }
}
