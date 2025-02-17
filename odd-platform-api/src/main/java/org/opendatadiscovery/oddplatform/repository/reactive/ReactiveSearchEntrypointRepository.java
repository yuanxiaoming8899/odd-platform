package org.opendatadiscovery.oddplatform.repository.reactive;

import java.util.Collection;
import java.util.List;
import reactor.core.publisher.Mono;

public interface ReactiveSearchEntrypointRepository {
    Mono<Void> recalculateVectors(final List<Long> dataEntityIds);

    Mono<Integer> updateDataEntityVectors(final long dataEntityId);

    Mono<Integer> updateDataEntityVectors(final List<Long> dataEntityIds);

    Mono<Integer> updateDataSourceVectorsForDataEntities(final List<Long> dataEntityIds);

    Mono<Integer> updateNamespaceVectorForDataEntity(final long dataEntityId);

    Mono<Integer> updateNamespaceVectorForDataEntities(final List<Long> dataEntityIds);

    Mono<Integer> updateChangedNamespaceVector(final long namespaceId);

    Mono<Integer> clearNamespaceVector(final long dataSourceId);

    Mono<Integer> updateStructureVectorForDataEntitiesByIds(final List<Long> dataEntityIds);

    Mono<Integer> updateStructureVectorForDataEntitiesByOddrns(final Collection<String> dataEntityOddrns);

    Mono<Integer> updateChangedDataSourceVector(final long dataSourceId);

    Mono<Integer> updateTagVectorsForDataEntity(final long dataEntityId);

    Mono<Integer> updateTagVectorsForDataEntities(final List<Long> dataEntityIds);

    Mono<Integer> updateChangedTagVectors(final long tagId);

    Mono<Integer> updateChangedTagStructureVector(final long tagId);

    Mono<Integer> updateChangedOwnerVectors(final long ownerId);

    Mono<Integer> updateChangedOwnershipVectors(final long ownershipId);

    Mono<Integer> updateDatasetFieldSearchVectors(final long datasetFieldId);

    Mono<Integer> updateMetadataVectors(final long dataEntityId);

    Mono<Integer> updateMetadataVectors(final List<Long> dataEntityIds);
}
