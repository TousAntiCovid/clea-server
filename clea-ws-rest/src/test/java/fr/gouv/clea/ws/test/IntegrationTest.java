package fr.gouv.clea.ws.test;

import org.junit.jupiter.api.DisplayNameGeneration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.test.context.TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS;

@ActiveProfiles({ "dev", "test" })
@SpringBootTest(webEnvironment = RANDOM_PORT)
@TestExecutionListeners(listeners = { RestAssuredManager.class, KafkaManager.class }, mergeMode = MERGE_WITH_DEFAULTS)
@Retention(RUNTIME)
@Target(TYPE)
@DisplayNameGeneration(ReplaceUnderscores.class)
public @interface IntegrationTest {

}
