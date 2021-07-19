package fr.gouv.clea.consumer.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Embeddable;

import java.io.Serializable;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class StatLocationKey implements Serializable {

    private static final long serialVersionUID = 1L;

    private Instant period;

    private int venueType;

    private int venueCategory1;

    private int venueCategory2;
}
