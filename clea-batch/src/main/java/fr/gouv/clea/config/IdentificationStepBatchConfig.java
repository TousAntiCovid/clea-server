package fr.gouv.clea.config;

import fr.gouv.clea.dto.SinglePlaceCluster;
import fr.gouv.clea.dto.SinglePlaceClusterPeriod;
import fr.gouv.clea.dto.SinglePlaceExposedVisits;
import fr.gouv.clea.identification.ExposedVisitRowMapper;
import fr.gouv.clea.identification.processor.ClusterToPeriodsProcessor;
import fr.gouv.clea.identification.processor.SinglePlaceExposedVisitsBuilder;
import fr.gouv.clea.identification.processor.SinglePlaceExposedVisitsProcessor;
import fr.gouv.clea.identification.writer.SinglePlaceClusterPeriodListWriter;
import fr.gouv.clea.mapper.ClusterPeriodModelsMapper;
import fr.gouv.clea.scoring.configuration.risk.RiskConfiguration;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.support.CompositeItemProcessor;
import org.springframework.batch.item.support.SynchronizedItemStreamReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.sql.DataSource;

import java.util.List;

import static fr.gouv.clea.config.BatchConstants.SQL_SELECT_DISTINCT_LTID_FROM_EXPOSEDVISITS;

@Configuration
public class IdentificationStepBatchConfig {

    @Autowired
    private StepBuilderFactory stepBuilderFactory;

    @Autowired
    private BatchProperties properties;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private ClusterPeriodModelsMapper mapper;

    @Autowired
    private RiskConfiguration riskConfig;

    @Bean
    public Step clusterIdentification() {
        final CompositeItemProcessor<String, List<SinglePlaceClusterPeriod>> compositeProcessor = new CompositeItemProcessor<>();
        compositeProcessor.setDelegates(
                List.of(
                        exposedVisitBuilder(), // from String to ExposedVisit
                        singleClusterPlaceBuilder(), // from ExposedVisit to SingleClusterPlace
                        singlePlaceClusterPeriodListBuilder()
                ) // from SingleClusterPlace to List<SinglePlaceClusterPeriods>
        );

        final SynchronizedItemStreamReader<String> reader = new SynchronizedItemStreamReader<>();
        reader.setDelegate(identificationStepReader());
        return stepBuilderFactory.get("clusterIdentification")
                .<String, List<SinglePlaceClusterPeriod>>chunk(properties.getIdentificationStepChunkSize())
                .reader(reader)
                .processor(compositeProcessor)
                .writer(new SinglePlaceClusterPeriodListWriter(new NamedParameterJdbcTemplate(dataSource)))
                .taskExecutor(taskExecutor())
                .throttleLimit(20)
                .build();
    }

    @Bean
    public JdbcCursorItemReader<String> identificationStepReader() {

        JdbcCursorItemReader<String> reader = new JdbcCursorItemReader<>();
        reader.setDataSource(dataSource);
        reader.setSql(SQL_SELECT_DISTINCT_LTID_FROM_EXPOSEDVISITS);
        reader.setRowMapper((rs, i) -> rs.getString(1));
        return reader;
    }

    @Bean
    public ItemProcessor<String, SinglePlaceExposedVisits> exposedVisitBuilder() {
        return new SinglePlaceExposedVisitsBuilder(new JdbcTemplate(dataSource), new ExposedVisitRowMapper());
    }

    @Bean
    public ItemProcessor<SinglePlaceExposedVisits, SinglePlaceCluster> singleClusterPlaceBuilder() {
        return new SinglePlaceExposedVisitsProcessor(properties, riskConfig);
    }

    @Bean
    public ItemProcessor<SinglePlaceCluster, List<SinglePlaceClusterPeriod>> singlePlaceClusterPeriodListBuilder() {
        return new ClusterToPeriodsProcessor(mapper);
    }

    @Bean
    TaskExecutor taskExecutor() {
        return new SimpleAsyncTaskExecutor("batch-ident");
    }

}
