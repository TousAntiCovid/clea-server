package fr.gouv.clea.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.gouv.clea.indexation.IndexationPartitioner;
import fr.gouv.clea.indexation.model.output.ClusterFile;
import fr.gouv.clea.indexation.processor.SinglePlaceClusterBuilder;
import fr.gouv.clea.indexation.reader.StepExecutionContextReader;
import fr.gouv.clea.indexation.writer.IndexationWriter;
import fr.gouv.clea.mapper.ClusterPeriodModelsMapper;
import fr.gouv.clea.service.PrefixesStorageService;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.core.partition.support.TaskExecutorPartitionHandler;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

@Configuration
public class IndexationStepBatchConfig {

    @Autowired
    private StepBuilderFactory stepBuilderFactory;

    @Autowired
    private PrefixesStorageService prefixesStorageService;

    @Autowired
    private BatchProperties properties;

    @Autowired
    private ClusterPeriodModelsMapper mapper;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Bean
    public Step clustersIndexation() {
        return this.stepBuilderFactory.get("clustersIndexation")
                .partitioner("partitioner", prefixPartitioner())
                .partitionHandler(partitionHandler())
                .build();
    }

    @Bean
    public Partitioner prefixPartitioner() {
        return new IndexationPartitioner(prefixesStorageService);
    }

    @Bean
    public TaskExecutorPartitionHandler partitionHandler() {
        final TaskExecutorPartitionHandler partitionHandler = new TaskExecutorPartitionHandler();
        partitionHandler.setGridSize(properties.getGridSize());
        partitionHandler.setStep(partitionedClustersIndexation());
        partitionHandler.setTaskExecutor(indexationTaskExecutor());
        return partitionHandler;
    }

    @Bean
    public Step partitionedClustersIndexation() {
        return stepBuilderFactory
                .get("partitionedClustersIndexation").<Map.Entry<String, List<String>>, ClusterFile>chunk(
                        properties.getIndexationStepChunkSize()
                )
                .reader(memoryMapItemReader(null, null))
                .processor(singlePlaceClusterBuilder()) // build a Map of ClusterFile at once
                .writer(indexationWriter(objectMapper)) // build Files and index
                .build();
    }

    @Bean
    @StepScope
    public ItemReader<Map.Entry<String, List<String>>> memoryMapItemReader(
            // values provided through step execution context by prefixPartitioner
            @Value("#{stepExecutionContext['prefixes']}") List<String> prefixes,
            @Value("#{stepExecutionContext['ltids']}") List<List<String>> ltids) {
        return new StepExecutionContextReader(prefixes, ltids);
    }

    @Bean
    public ItemProcessor<Map.Entry<String, List<String>>, ClusterFile> singlePlaceClusterBuilder() {
        return new SinglePlaceClusterBuilder(jdbcTemplate, mapper);
    }

    @Bean
    public IndexationWriter indexationWriter(final ObjectMapper objectMapper) {
        return new IndexationWriter(properties, objectMapper);
    }

    @Bean
    public SimpleAsyncTaskExecutor indexationTaskExecutor() {
        return new SimpleAsyncTaskExecutor("batch-index");
    }
}
