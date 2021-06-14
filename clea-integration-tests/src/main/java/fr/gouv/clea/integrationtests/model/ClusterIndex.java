package fr.gouv.clea.integrationtests.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClusterIndex {
    @JsonProperty("i")
    private int iteration;
    
    @JsonProperty("c")
    private List<String> prefixes;
}
