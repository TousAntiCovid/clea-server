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

    private static final String TOKEN_LABEL = "report.token";

    private static final String PIVOT_LABEL = "report.pivotDate";

    //
    private final MeterRegistry meterRegistry;

    private final Counter processedCounter;

    private final Counter rejectedCounter;

    private final Counter duplicateCounter;

    private final Counter outdatedCounter;

    private final Counter notCurrentCounter;

    private final Counter futureCounter;

    private final Counter missingTokenCounter;

    private final Counter invalidTokenCounter;

    public MetricsService(
            @Autowired MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        this.processedCounter = Counter.builder(VISIT_LABEL)
                .tag("type", "processed")
                .description("The number of processed reports")
                .register(meterRegistry);

        this.rejectedCounter = Counter.builder(VISIT_LABEL)
                .tag("type", "rejected")
                .description("The number of rejected reports")
                .register(meterRegistry);

        this.duplicateCounter = Counter.builder(VISIT_LABEL)
                .tag("type", "duplicate")
                .description("The number of duplicated reports")
                .register(meterRegistry);

        this.outdatedCounter = Counter.builder(VISIT_LABEL)
                .tag("type", "outdated")
                .description("The number of outdated reports")
                .register(meterRegistry);

        this.futureCounter = Counter.builder(VISIT_LABEL)
                .tag("type", "future")
                .description("The number of reports in future")
                .register(meterRegistry);

        this.notCurrentCounter = Counter.builder(PIVOT_LABEL)
                .tag("type", "notCurrent")
                .description("The number of non current pivotDates")
                .register(meterRegistry);

        this.missingTokenCounter = Counter.builder(TOKEN_LABEL)
                .tag("type", "missing")
                .description("The number of requests with a missing JWT token")
                .register(meterRegistry);

        this.invalidTokenCounter = Counter.builder(TOKEN_LABEL)
                .tag("type", "invalid")
                .description("The number of requests with an invalid JWT token")
                .register(meterRegistry);
    }
}
