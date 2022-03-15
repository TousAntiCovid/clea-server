package fr.gouv.clea.consumer.utils;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Getter
public class MetricsService {

    private static final String PURGE_LABEL = "consumer.purge";

    private MeterRegistry meterRegistry;

    private Counter purgedCounter;

    @Autowired
    public MetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        this.purgedCounter = Counter.builder(PURGE_LABEL)
                .tag("type", "purged")
                .description("The number of purged entries")
                .register(meterRegistry);
    }
}
