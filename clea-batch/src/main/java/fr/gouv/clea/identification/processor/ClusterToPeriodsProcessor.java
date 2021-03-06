package fr.gouv.clea.identification.processor;

import fr.gouv.clea.dto.SinglePlaceCluster;
import fr.gouv.clea.dto.SinglePlaceClusterPeriod;
import fr.gouv.clea.mapper.ClusterPeriodModelsMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.ItemProcessor;

import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class ClusterToPeriodsProcessor implements ItemProcessor<SinglePlaceCluster, List<SinglePlaceClusterPeriod>> {

    private final ClusterPeriodModelsMapper mapper;

    @Override
    public List<SinglePlaceClusterPeriod> process(SinglePlaceCluster cluster) {
        return cluster.getPeriods().stream()
                .map(period -> mapper.map(cluster, period))
                .collect(Collectors.toUnmodifiableList());
    }
}
