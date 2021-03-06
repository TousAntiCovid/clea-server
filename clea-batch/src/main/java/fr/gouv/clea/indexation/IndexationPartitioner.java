package fr.gouv.clea.indexation;

import fr.gouv.clea.service.PrefixesStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.item.ExecutionContext;

import java.util.*;

import static fr.gouv.clea.config.BatchConstants.LTIDS_LIST_PARTITION_KEY;
import static fr.gouv.clea.config.BatchConstants.PREFIXES_PARTITION_KEY;

@Slf4j
public class IndexationPartitioner implements Partitioner {

    private final PrefixesStorageService prefixesStorageService;

    public IndexationPartitioner(PrefixesStorageService prefixesStorageService) {
        this.prefixesStorageService = prefixesStorageService;
    }

    /**
     * Create a set of distinct {@link ExecutionContext} instances together with a
     * unique identifier for each one. The identifiers should be short, mnemonic
     * values, and only have to be unique within the return value (e.g. use an
     * incrementer).
     *
     * @param gridSize the size of the map to return
     * @return a map from identifier to input parameters
     */
    @Override
    public Map<String, ExecutionContext> partition(int gridSize) {

        log.info("Computing indexation partitions...");
        final Map<String, List<String>> prefixLtidsMap = prefixesStorageService.getPrefixWithAssociatedLtidsMap();

        final Iterator<Map.Entry<String, List<String>>> prefixLtidsMapIterator = prefixLtidsMap.entrySet().iterator();
        final Map<String, ExecutionContext> result = new HashMap<>();

        // At least 1 prefix per partition
        final int partitionSize = Math.max(prefixLtidsMap.size() / gridSize, 1);

        // prefixLtidsMap.size() if less prefixes than parameterized gridSize, otherwise
        // gridSize
        final int partitionsTotalNumber = Math.min(prefixLtidsMap.size(), gridSize);

        // Build partitions execution contexts by splitting map into X equal parts, X
        // being partitionsTotalNumber
        for (int partitionsIndex = 0; partitionsIndex < partitionsTotalNumber; partitionsIndex++) {
            final ExecutionContext value = createPartitionExecutionContext(prefixLtidsMapIterator, partitionSize);
            result.put("partition-" + partitionsIndex, value);
        }

        // handle possible remaining entries due to non-null euclidian partitionSize
        // division quotient
        dispatchRemainingEntries(prefixLtidsMapIterator, result);
        log.debug("{} partitions created", result.size());
        return result;
    }

    private ExecutionContext createPartitionExecutionContext(
            Iterator<Map.Entry<String, List<String>>> prefixesLtidsMapIterator, int partitionSize) {
        final ExecutionContext value = new ExecutionContext();
        final List<String> prefixes = new ArrayList<>();
        final List<List<String>> ltids = new ArrayList<>();
        for (int partitionIndex = 0; partitionIndex < partitionSize; partitionIndex++) {
            if (prefixesLtidsMapIterator.hasNext()) {
                final var nextItem = prefixesLtidsMapIterator.next();
                prefixes.add(nextItem.getKey());
                ltids.add(nextItem.getValue());
            } else {
                break;
            }
            value.put(PREFIXES_PARTITION_KEY, prefixes);
            value.put(LTIDS_LIST_PARTITION_KEY, ltids);
        }
        return value;
    }

    private void dispatchRemainingEntries(final Iterator<Map.Entry<String, List<String>>> prefixLtidsMapIterator,
            final Map<String, ExecutionContext> result) {
        int partitionIndex = 0;
        while (prefixLtidsMapIterator.hasNext()) {
            final Map.Entry<String, List<String>> currentValue = prefixLtidsMapIterator.next();
            final ExecutionContext executionContext = result.get("partition-" + partitionIndex);
            ((List<String>) executionContext.get(PREFIXES_PARTITION_KEY)).add(currentValue.getKey());
            ((List<List<String>>) executionContext.get(LTIDS_LIST_PARTITION_KEY)).add(currentValue.getValue());
            partitionIndex++;
        }
    }
}
