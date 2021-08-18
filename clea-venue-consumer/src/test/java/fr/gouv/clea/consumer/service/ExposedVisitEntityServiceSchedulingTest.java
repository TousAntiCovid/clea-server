package fr.gouv.clea.consumer.service;

import fr.gouv.clea.consumer.model.ExposedVisitEntity;
import fr.gouv.clea.consumer.repository.visits.ExposedVisitRepository;
import fr.inria.clea.lsp.utils.TimeUtils;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.config.CronTask;
import org.springframework.scheduling.config.ScheduledTask;
import org.springframework.scheduling.config.ScheduledTaskHolder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@DirtiesContext
@TestPropertySource(properties = { "clea.conf.scheduling.purge.cron=*/10 * * * * *",
        "clea.conf.scheduling.purge.enabled=true" })
class ExposedVisitEntityServiceSchedulingTest {

    @Value("${clea.conf.scheduling.purge.cron}")
    private String cronValue;

    @Autowired
    private ExposedVisitRepository repository;

    @Autowired
    private ScheduledTaskHolder scheduledTaskHolder;

    private static ExposedVisitEntity createExposedVisit(int timeSlot, long periodStart) {
        return new ExposedVisitEntity(
                null, // handled by db
                UUID.randomUUID(),
                RandomUtils.nextInt(),
                RandomUtils.nextInt(),
                RandomUtils.nextInt(),
                periodStart,
                timeSlot,
                RandomUtils.nextLong(),
                RandomUtils.nextLong(),
                null, // handled by db
                null // handled by db
        );
    }

    @AfterEach
    void clean() {
        repository.deleteAll();
    }

    @Test
    @DisplayName("check that croned job is active")
    void testCronIsActive() {
        assertThat(scheduledTaskHolder.getScheduledTasks())
                .extracting(ScheduledTask::getTask)
                .filteredOn(task -> task instanceof CronTask)
                .extracting(task -> (CronTask) task)
                .extracting(task -> tuple(task.getExpression(), task.toString()))
                .containsExactly(
                        tuple(
                                cronValue,
                                "fr.gouv.clea.consumer.service.ExposedVisitEntityService.deleteOutdatedExposedVisits"
                        )
                );
    }

    @Test
    @DisplayName("check that croned job remove outdated exposed visits from DB")
    void deleteOutdatedExposedVisits() {
        Instant now = Instant.now();

        long _15DaysLater = TimeUtils.ntpTimestampFromInstant(now.plus(15, ChronoUnit.DAYS));
        long _14DaysLater = TimeUtils.ntpTimestampFromInstant(now.plus(14, ChronoUnit.DAYS));
        long _2DaysLater = TimeUtils.ntpTimestampFromInstant(now.plus(2, ChronoUnit.DAYS));
        long yesterday = TimeUtils.ntpTimestampFromInstant(now.minus(1, ChronoUnit.DAYS));
        long _2DaysAgo = TimeUtils.ntpTimestampFromInstant(now.minus(2, ChronoUnit.DAYS));
        long _14DaysAgo = TimeUtils.ntpTimestampFromInstant(now.minus(14, ChronoUnit.DAYS));
        long _15DaysAgo = TimeUtils.ntpTimestampFromInstant(now.minus(15, ChronoUnit.DAYS));
        long _16DaysAgo = TimeUtils.ntpTimestampFromInstant(now.minus(16, ChronoUnit.DAYS));

        repository.saveAll(
                List.of(
                        createExposedVisit(1, _15DaysLater), // keep
                        createExposedVisit(2, _14DaysLater), // keep
                        createExposedVisit(3, _2DaysLater), // keep
                        createExposedVisit(4, yesterday), // keep
                        createExposedVisit(5, _2DaysAgo), // keep
                        createExposedVisit(0, _14DaysAgo), // keep, will only be purged if query condition is >=
                        createExposedVisit(1, _14DaysAgo), // keep
                        createExposedVisit(0, _15DaysAgo), // purge
                        createExposedVisit(9, _16DaysAgo) // purge
                )
        );

        await().atMost(30, TimeUnit.SECONDS)
                .untilAsserted(() -> assertThat(repository.count()).isEqualTo(7));

        assertThat(repository.findAll())
                .extracting(ExposedVisitEntity::getPeriodStart)
                .contains(
                        _15DaysLater,
                        _14DaysLater,
                        _2DaysLater,
                        yesterday,
                        _2DaysAgo,
                        _14DaysAgo
                )
                .doesNotContain(
                        _15DaysAgo,
                        _16DaysAgo
                );
    }
}
