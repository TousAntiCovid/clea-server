package fr.gouv.clea.consumer.model;

import lombok.Builder;
import lombok.Value;

import java.time.OffsetDateTime;

@Value
@Builder
public class LocationStatKafka {

    OffsetDateTime periodStart;

    long venueType;

    long venueCategory1;

    long venueCategory2;

    boolean backward;
}
