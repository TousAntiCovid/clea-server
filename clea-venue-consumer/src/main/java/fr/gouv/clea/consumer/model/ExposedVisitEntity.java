package fr.gouv.clea.consumer.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "EXPOSED_VISITS")
public class ExposedVisitEntity {

    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "org.hibernate.id.UUIDGenerator")
    private String id;

    @Column(name = "LTId")
    @Type(type = "pg-uuid")
    private UUID locationTemporaryPublicId;

    private int venueType;

    private int venueCategory1;

    private int venueCategory2;

    private long periodStart;

    @Column(name = "timeslot")
    private int timeSlot;

    private long backwardVisits;

    private long forwardVisits;

    @CreationTimestamp
    private Instant createdAt; // for db ops/maintenance

    @UpdateTimestamp
    private Instant updatedAt; // for db ops/maintenance
}
