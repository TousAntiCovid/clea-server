package fr.gouv.clea.ws.utils;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Getter
public class MetricsService {

    private static final String VISIT_LABEL = "report.visit";

    private static final String PIVOT_LABEL = "report.pivotDate";

    private final MeterRegistry meterRegistry;

    private final Counter processedVisitCounter;

    private final Counter duplicateVisitCounter;

    private final Counter outdatedVisitCounter;

    private final Counter notCurrentPivotDatesCounter;

    private final Counter futureVisitCounter;

    public MetricsService(
            @Autowired MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        this.processedVisitCounter = Counter.builder(VISIT_LABEL)
                .tag("type", "processed")
                .description("The number of processed visits")
                .register(meterRegistry);

        this.duplicateVisitCounter = Counter.builder(VISIT_LABEL)
                .tag("type", "rejected_duplicate")
                .description("The number of duplicated visits")
                .register(meterRegistry);

        this.outdatedVisitCounter = Counter.builder(VISIT_LABEL)
                .tag("type", "rejected_outdated")
                .description("The number of outdated visits")
                .register(meterRegistry);

        this.futureVisitCounter = Counter.builder(VISIT_LABEL)
                .tag("type", "rejected_future")
                .description("The number of visits in future")
                .register(meterRegistry);

        this.notCurrentPivotDatesCounter = Counter.builder(PIVOT_LABEL)
                .tag("type", "notCurrent")
                .description("The number of non current pivotDates")
                .register(meterRegistry);
    }
}
