package fr.gouv.clea.integrationtests.service;

import fr.gouv.clea.integrationtests.repository.ClusterExpositionRepository;
import fr.gouv.clea.integrationtests.repository.model.Cluster;
import fr.gouv.clea.integrationtests.repository.model.ClusterExposition;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ClusterExpositionService {

    private final ClusterExpositionRepository clusterExpositionRepository;

    public float getRiskLevelForPlaceAtInstant(String locationTemporaryPublicId, Instant instant) {
        final var clusterIndex = clusterExpositionRepository.getClusterIndex();
        return clusterIndex.getPrefixes()
                .stream()
                .filter(locationTemporaryPublicId::startsWith)
                .map(prefix -> clusterExpositionRepository.getClusterFile(clusterIndex.getIteration(), prefix))
                .flatMap(List::stream)
                .filter(cluster -> cluster.getLocationTemporaryPublicID().equals(locationTemporaryPublicId))
                .map(Cluster::getExpositions)
                .flatMap(List::stream)
                .filter(clusterExposition -> clusterExposition.affects(instant))
                .map(ClusterExposition::getRisk)
                .sorted()
                .findFirst()
                .orElse(0f);
    }
}
