package fr.gouv.clea.consumer.utils;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Data
public class MetricsService {

    private static final String PURGE_LABEL = "consumer.purge";

    private static final String STAT_LABEL = "consumer.stat";

    //
    private MeterRegistry meterRegistry;

    private Counter purgedCounter;

    private Counter failedPurgeCounter;

    private Counter statCounter;

    private Counter failedStatCounter;

    @Autowired
    public MetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        this.purgedCounter = Counter.builder(PURGE_LABEL)
                .tag("type", "purged")
                .description("The number of purged entries")
                .register(meterRegistry);

        this.failedPurgeCounter = Counter.builder(PURGE_LABEL)
                .tag("type", "purgeFailed")
                .description("The number of failed purge operations")
                .register(meterRegistry);

        this.statCounter = Counter.builder(STAT_LABEL)
                .tag("type", "statSaved")
                .description("The number of saved stats")
                .register(meterRegistry);

        this.failedStatCounter = Counter.builder(STAT_LABEL)
                .tag("type", "statFailed")
                .description("The number of failed stat saving")
                .register(meterRegistry);
    }
}
