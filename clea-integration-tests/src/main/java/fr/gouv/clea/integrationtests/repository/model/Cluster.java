package fr.gouv.clea.integrationtests.repository.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Cluster {

    @JsonProperty("ltid")
    private String locationTemporaryPublicID; // LTid

    @JsonProperty("exp")
    private List<ClusterExposition> expositions;
}
