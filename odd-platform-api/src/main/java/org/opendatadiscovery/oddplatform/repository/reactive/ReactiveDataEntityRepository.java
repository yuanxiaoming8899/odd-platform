package org.opendatadiscovery.oddplatform.repository.reactive;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.opendatadiscovery.oddplatform.dto.DataEntityDetailsDto;
import org.opendatadiscovery.oddplatform.dto.DataEntityDimensionsDto;
import org.opendatadiscovery.oddplatform.dto.DataEntityDomainInfoDto;
import org.opendatadiscovery.oddplatform.dto.DataEntityDto;
import org.opendatadiscovery.oddplatform.dto.FacetStateDto;
import org.opendatadiscovery.oddplatform.model.tables.pojos.DataEntityPojo;
import org.opendatadiscovery.oddplatform.model.tables.pojos.OwnerPojo;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ReactiveDataEntityRepository extends ReactiveCRUDRepository<DataEntityPojo> {
    Flux<DataEntityPojo> get(final List<Long> ids);

    Mono<DataEntityPojo> updateDEG(final DataEntityPojo dataEntityPojo);

    Mono<Boolean> existsIncludingSoftDeleted(final long dataEntityId);

    Mono<Boolean> existsNonDeletedByDataSourceId(final long dataSourceId);

    Mono<Boolean> existsNonDeletedByNamespaceId(final long namespaceId);

    Mono<Long> incrementViewCount(final long id);

    Mono<DataEntityDimensionsDto> getDimensions(final long id);

    Mono<List<DataEntityDimensionsDto>> getDimensions(Collection<String> oddrns);

    Mono<DataEntityDetailsDto> getDetails(final long id);

    default Flux<DataEntityPojo> listByOddrns(final Collection<String> oddrns,
                                              boolean includeHollow,
                                              boolean includeDeleted) {
        return listByOddrns(oddrns, includeHollow, includeDeleted, null, null);
    }

    Flux<DataEntityPojo> listByOddrns(final Collection<String> oddrns,
                                      boolean includeHollow,
                                      boolean includeDeleted,
                                      final Integer page,
                                      final Integer size);

    Flux<DataEntityPojo> getPojosForStatusSwitch();

    Mono<DataEntityDimensionsDto> getDataEntityWithDataSourceAndNamespace(final long dataEntityId);

    Flux<DataEntityDimensionsDto> getDataEntitiesWithDataSourceAndNamespace(final Collection<String> oddrns);

    Mono<List<DataEntityDimensionsDto>> getDataEntityWithOwnership(final Collection<String> oddrns);

    Mono<List<DataEntityPojo>> getDEGEntities(final String groupOddrn);

    Mono<Map<String, Set<DataEntityPojo>>> getDEGEntities(final Collection<String> groupOddrns);

    Mono<List<DataEntityDimensionsDto>> getDEGExperimentRuns(final Long dataEntityGroupId,
                                                             final Integer page,
                                                             final Integer size);

    Mono<Map<String, Long>> getExperimentRunsCount(final Collection<String> groupOddrns);

    Mono<Void> createHollow(final Collection<String> hollowOddrns);

    Mono<DataEntityPojo> setInternalName(final long dataEntityId, final String name);

    Mono<DataEntityPojo> setInternalDescription(final long dataEntityId, final String description);

    default Mono<Long> countByState(final FacetStateDto state) {
        return countByState(state, null);
    }

    Mono<Long> countByState(final FacetStateDto state, final OwnerPojo owner);

    Flux<DataEntityDto> getQuerySuggestions(final String query, final Integer entityClassId,
                                            final Boolean manuallyCreated);

    default Flux<DataEntityDto> listByOwner(final long ownerId) {
        return listByOwner(ownerId, null, null);
    }

    Flux<DataEntityDto> listByOwner(final long ownerId, final Integer page, final Integer size);

    Flux<DataEntityDto> listPopular(final int page, final int size);

    Flux<DataEntityDimensionsDto> listByTerm(final long termId, final String query, final Integer entityClassId,
                                             final int page, final int size);

    Mono<List<DataEntityDimensionsDto>> listByDatasourceAndType(final long datasourceId, final Integer typeId,
                                                                final int page, final int size);

    Mono<Long> countByDatasourceAndType(final long datasourceId, final Integer typeId);

    Mono<List<DataEntityDimensionsDto>> findByState(final FacetStateDto state,
                                                    final int page,
                                                    final int size,
                                                    final OwnerPojo owner);

    Mono<Map<String, Set<DataEntityPojo>>> getParentDEGs(final Collection<String> oddrns);

    Mono<DataEntityDetailsDto> getDataEntitySearchFields(final long dataEntityId);

    Mono<String> getHighlightedResult(final String text, final String query);

    Flux<Integer> getDataSourceEntityTypeIds(final long dataSourceId);

    default Mono<Map<Long, Long>> getCountByDataSources() {
        return getCountByDataSources(List.of());
    }

    Mono<Map<Long, Long>> getCountByDataSources(final Collection<Long> dataSourceIds);

    Flux<DataEntityDomainInfoDto> getDataEntityDomainsInfo();
}