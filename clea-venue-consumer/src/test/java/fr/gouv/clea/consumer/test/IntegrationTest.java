package fr.gouv.clea.consumer.test;

import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestExecutionListeners;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.test.context.TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS;

@Retention(RUNTIME)
@Target(TYPE)
@SpringBootTest(webEnvironment = RANDOM_PORT)
@TestExecutionListeners(listeners = { ElasticManager.class, KafkaManager.class,
        PostgreSqlManager.class, RestAssuredManager.class }, mergeMode = MERGE_WITH_DEFAULTS)
@DisplayNameGeneration(ReplaceUnderscores.class)
public @interface IntegrationTest {

}
