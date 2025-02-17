package org.opendatadiscovery.oddplatform.mapper;

import com.fasterxml.jackson.core.type.TypeReference;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.opendatadiscovery.oddplatform.api.contract.model.DataEntity;
import org.opendatadiscovery.oddplatform.api.contract.model.DataEntityBaseObject;
import org.opendatadiscovery.oddplatform.api.contract.model.DataEntityClass;
import org.opendatadiscovery.oddplatform.api.contract.model.DataEntityClassAndTypeDictionary;
import org.opendatadiscovery.oddplatform.api.contract.model.DataEntityClassUsageInfo;
import org.opendatadiscovery.oddplatform.api.contract.model.DataEntityDetails;
import org.opendatadiscovery.oddplatform.api.contract.model.DataEntityGroupFormData;
import org.opendatadiscovery.oddplatform.api.contract.model.DataEntityGroupItem;
import org.opendatadiscovery.oddplatform.api.contract.model.DataEntityList;
import org.opendatadiscovery.oddplatform.api.contract.model.DataEntityRef;
import org.opendatadiscovery.oddplatform.api.contract.model.DataEntityRun;
import org.opendatadiscovery.oddplatform.api.contract.model.DataEntityStatus;
import org.opendatadiscovery.oddplatform.api.contract.model.DataEntityType;
import org.opendatadiscovery.oddplatform.api.contract.model.DataEntityTypeUsageInfo;
import org.opendatadiscovery.oddplatform.api.contract.model.DataEntityUsageInfo;
import org.opendatadiscovery.oddplatform.api.contract.model.DataQualityTestExpectation;
import org.opendatadiscovery.oddplatform.api.contract.model.DataQualityTestSeverity;
import org.opendatadiscovery.oddplatform.api.contract.model.DataSetStats;
import org.opendatadiscovery.oddplatform.api.contract.model.LinkedTerm;
import org.opendatadiscovery.oddplatform.api.contract.model.LinkedUrl;
import org.opendatadiscovery.oddplatform.api.contract.model.PageInfo;
import org.opendatadiscovery.oddplatform.dto.DataEntityClassDto;
import org.opendatadiscovery.oddplatform.dto.DataEntityDetailsDto;
import org.opendatadiscovery.oddplatform.dto.DataEntityDimensionsDto;
import org.opendatadiscovery.oddplatform.dto.DataEntityDto;
import org.opendatadiscovery.oddplatform.dto.DataEntityStatusDto;
import org.opendatadiscovery.oddplatform.dto.DataEntityTypeDto;
import org.opendatadiscovery.oddplatform.dto.DataSourceDto;
import org.opendatadiscovery.oddplatform.dto.attributes.LinkedUrlAttribute;
import org.opendatadiscovery.oddplatform.model.tables.pojos.DataEntityPojo;
import org.opendatadiscovery.oddplatform.model.tables.pojos.DataEntityStatisticsPojo;
import org.opendatadiscovery.oddplatform.model.tables.pojos.DataQualityTestSeverityPojo;
import org.opendatadiscovery.oddplatform.model.tables.pojos.NamespacePojo;
import org.opendatadiscovery.oddplatform.service.DataEntityStaleDetector;
import org.opendatadiscovery.oddplatform.service.ingestion.util.DateTimeUtil;
import org.opendatadiscovery.oddplatform.utils.JSONSerDeUtils;
import org.opendatadiscovery.oddplatform.utils.Page;
import org.springframework.stereotype.Component;

import static java.util.function.Function.identity;
import static org.opendatadiscovery.oddplatform.dto.DataEntityDimensionsDto.DataQualityTestDetailsDto;

@Component
@RequiredArgsConstructor
public class DataEntityMapperImpl implements DataEntityMapper {
    private static DataEntityClassAndTypeDictionary TYPE_DICTIONARY = null;

    private final DataSourceMapper dataSourceMapper;
    private final DataSourceSafeMapper dataSourceSafeMapper;
    private final OwnershipMapper ownershipMapper;
    private final TagMapper tagMapper;
    private final MetadataFieldValueMapper metadataFieldValueMapper;
    private final DatasetVersionMapper datasetVersionMapper;
    private final DataEntityRunMapper dataEntityRunMapper;
    private final TermMapper termMapper;
    private final DateTimeMapper dateTimeMapper;
    private final DataEntityStatusMapper dataEntityStatusMapper;
    private final DataEntityStaleDetector dataEntityStaleDetector;

    @Override
    public DataEntity mapPojo(final DataEntityDimensionsDto dto) {
        final Set<DataEntityClassDto> entityClasses =
            DataEntityClassDto.findByIds(dto.getDataEntity().getEntityClassIds());
        final DataEntityType type = getDataEntityType(dto.getDataEntity());
        final List<DataEntityRef> groups = Optional.ofNullable(dto.getParentGroups()).stream()
            .flatMap(Collection::stream)
            .map(this::mapReference)
            .toList();

        final DataEntity entity = mapPojo(dto.getDataEntity())
            .entityClasses(entityClasses.stream().map(this::mapEntityClass).toList())
            .type(type)
            .ownership(ownershipMapper.mapDtos(dto.getOwnership()))
            .dataSource(dataSourceSafeMapper.mapDto(new DataSourceDto(dto.getDataSource(), dto.getNamespace(), null)))
            .dataEntityGroups(groups);

        if (entityClasses.contains(DataEntityClassDto.DATA_SET)) {
            entity.setStats(mapStats(dto.getDataSetDetailsDto()));
        }

        if (entityClasses.contains(DataEntityClassDto.DATA_TRANSFORMER)) {
            entity.setSourceList(dto.getDataTransformerDetailsDto().sourceList()
                .stream()
                .distinct()
                .map(this::mapReference)
                .collect(Collectors.toList()));

            entity.setTargetList(dto.getDataTransformerDetailsDto()
                .targetList()
                .stream()
                .distinct()
                .map(this::mapReference)
                .collect(Collectors.toList()));
        }

        if (entityClasses.contains(DataEntityClassDto.DATA_QUALITY_TEST)) {
            entity.setLinkedUrlList(mapLinkedUrlList(dto.getDataQualityTestDetailsDto().linkedUrlList()));

            entity.setDatasetsList(dto.getDataQualityTestDetailsDto()
                .datasetList()
                .stream()
                .distinct()
                .map(this::mapReference)
                .collect(Collectors.toList()));
        }

        if (entityClasses.contains(DataEntityClassDto.DATA_CONSUMER)) {
            entity.setInputList(dto.getDataConsumerDetailsDto()
                .inputList()
                .stream()
                .distinct()
                .map(this::mapReference).collect(Collectors.toList()));
        }

        if (entityClasses.contains(DataEntityClassDto.DATA_INPUT)) {
            entity.setOutputList(dto.getDataInputDetailsDto()
                .outputList()
                .stream()
                .distinct()
                .map(this::mapReference).collect(Collectors.toList()));
        }

        if (entityClasses.contains(DataEntityClassDto.DATA_ENTITY_GROUP) && dto.getGroupsDto() != null) {
            final List<DataEntityRef> dataEntityRefs = dto.getGroupsDto().entities().stream()
                .map(this::mapReference)
                .toList();

            entity.setEntities(dataEntityRefs);
            entity.setItemsCount(dto.getGroupsDto().itemsCount());
        }

        return entity;
    }

    private DataEntity mapPojo(final DataEntityPojo pojo) {
        return new DataEntity()
            .id(pojo.getId())
            .internalName(pojo.getInternalName())
            .externalName(pojo.getExternalName())
            .oddrn(pojo.getOddrn())
            .sourceCreatedAt(dateTimeMapper.mapUTCDateTime(pojo.getSourceCreatedAt()))
            .sourceUpdatedAt(dateTimeMapper.mapUTCDateTime(pojo.getSourceUpdatedAt()))
            .lastIngestedAt(dateTimeMapper.mapUTCDateTime(pojo.getLastIngestedAt()))
            .viewCount(pojo.getViewCount())
            .status(dataEntityStatusMapper.mapStatus(pojo))
            .isStale(dataEntityStaleDetector.isDataEntityStale(pojo));
    }

    @Override
    public DataEntityList mapPojos(final List<DataEntityDimensionsDto> dataEntityDto) {
        final List<DataEntity> entities = dataEntityDto.stream().map(this::mapPojo).toList();
        final PageInfo pageInfo = pageInfo(dataEntityDto.size());
        return new DataEntityList(entities, pageInfo);
    }

    @Override
    public DataEntityList mapPojos(final Page<DataEntityDimensionsDto> dataEntityDto) {
        final List<DataEntity> entities = dataEntityDto.getData().stream().map(this::mapPojo).toList();
        final PageInfo pageInfo = pageInfo(dataEntityDto);
        return new DataEntityList(entities, pageInfo);
    }

    @Override
    public DataEntityPojo mapCreatedDEGPojo(final DataEntityGroupFormData formData,
                                            final DataEntityClassDto classDto,
                                            final NamespacePojo namespacePojo) {
        final LocalDateTime now = DateTimeUtil.generateNow();
        return new DataEntityPojo()
            .setInternalName(formData.getName())
            .setNamespaceId(namespacePojo != null ? namespacePojo.getId() : null)
            .setEntityClassIds(new Integer[] {classDto.getId()})
            .setTypeId(formData.getType().getId())
            .setPlatformCreatedAt(now)
            .setStatus(DataEntityStatusDto.UNASSIGNED.getId())
            .setStatusUpdatedAt(now)
            .setManuallyCreated(true)
            .setHollow(false)
            .setExcludeFromSearch(false);
    }

    @Override
    public DataEntityPojo applyToPojo(final DataEntityGroupFormData formData,
                                      final NamespacePojo namespacePojo,
                                      final DataEntityPojo pojo) {
        if (pojo == null) {
            return null;
        }
        return pojo
            .setInternalName(formData.getName())
            .setNamespaceId(namespacePojo != null ? namespacePojo.getId() : null)
            .setTypeId(formData.getType().getId());
    }

    @Override
    public DataEntityPojo applyStatus(final DataEntityPojo pojo, final DataEntityStatus status) {
        if (pojo == null) {
            return null;
        }
        final DataEntityStatusDto statusDto = DataEntityStatusDto.valueOf(status.getStatus().getValue());
        pojo.setStatus(statusDto.getId());
        pojo.setStatusSwitchTime(DateTimeUtil.mapUTCDateTime(status.getStatusSwitchTime()));
        if (statusDto.getId() != pojo.getStatus()) {
            pojo.setStatusUpdatedAt(DateTimeUtil.generateNow());
        }
        return pojo;
    }

    @Override
    public DataEntityDetails mapDtoDetails(final DataEntityDetailsDto dto) {
        final DataEntityPojo pojo = dto.getDataEntity();
        final Set<DataEntityClassDto> entityClasses =
            DataEntityClassDto.findByIds(dto.getDataEntity().getEntityClassIds());
        final DataEntityType type = getDataEntityType(pojo);
        final List<DataEntityRef> groups = Optional.ofNullable(dto.getParentGroups()).stream()
            .flatMap(Collection::stream)
            .map(this::mapReference)
            .toList();
        final List<LinkedTerm> linkedTerms = dto.getTerms().stream()
            .map(termMapper::mapToLinkedTerm)
            .toList();

        final DataEntityDetails details = new DataEntityDetails()
            .id(pojo.getId())
            .externalName(pojo.getExternalName())
            .internalName(pojo.getInternalName())
            .oddrn(pojo.getOddrn())
            .internalDescription(pojo.getInternalDescription())
            .externalDescription(pojo.getExternalDescription())
            .sourceCreatedAt(dateTimeMapper.mapUTCDateTime(pojo.getSourceCreatedAt()))
            .sourceUpdatedAt(dateTimeMapper.mapUTCDateTime(pojo.getSourceUpdatedAt()))
            .lastIngestedAt(dateTimeMapper.mapUTCDateTime(pojo.getLastIngestedAt()))
            .isStale(dataEntityStaleDetector.isDataEntityStale(pojo))
            .dataEntityGroups(groups)
            .entityClasses(entityClasses.stream().map(this::mapEntityClass).toList())
            .type(type)
            .status(dataEntityStatusMapper.mapStatus(pojo))
            .ownership(ownershipMapper.mapDtos(dto.getOwnership()))
            .dataSource(dataSourceSafeMapper.mapDto(new DataSourceDto(dto.getDataSource(), dto.getNamespace(), null)))
            .tags(tagMapper.mapToTagList(dto.getTags()))
            .metadataFieldValues(metadataFieldValueMapper.mapDtos(dto.getMetadata()))
            .terms(linkedTerms)
            .viewCount(pojo.getViewCount());

        if (entityClasses.contains(DataEntityClassDto.DATA_SET)) {
            details.setVersionList(datasetVersionMapper.mapPojo(dto.getDatasetVersions()));
            details.setStats(mapStats(dto.getDataSetDetailsDto()));
        }

        if (entityClasses.contains(DataEntityClassDto.DATA_TRANSFORMER)) {
            details.setSourceList(dto.getDataTransformerDetailsDto().sourceList()
                .stream()
                .distinct()
                .map(this::mapReference)
                .collect(Collectors.toList()));

            details.setTargetList(dto.getDataTransformerDetailsDto()
                .targetList()
                .stream()
                .distinct()
                .map(this::mapReference)
                .collect(Collectors.toList()));
        }

        if (entityClasses.contains(DataEntityClassDto.DATA_QUALITY_TEST)) {
            final DataQualityTestExpectation expectation = new DataQualityTestExpectation()
                .type(dto.getDataQualityTestDetailsDto().expectationType());

            expectation.putAll(MapUtils.emptyIfNull(dto.getDataQualityTestDetailsDto().expectationParameters()));

            details.expectation(expectation)
                .datasetsList(dto.getDataQualityTestDetailsDto()
                    .datasetList()
                    .stream()
                    .distinct()
                    .map(this::mapReference)
                    .collect(Collectors.toList()))
                .linkedUrlList(mapLinkedUrlList(dto.getDataQualityTestDetailsDto().linkedUrlList()))
                .latestRun(dataEntityRunMapper.mapDataEntityRun(
                    dto.getDataEntity().getId(),
                    dto.getDataQualityTestDetailsDto().latestTaskRun())
                )
                .suiteName(dto.getDataQualityTestDetailsDto().suiteName())
                .suiteUrl(dto.getDataQualityTestDetailsDto().suiteUrl());
        }

        if (entityClasses.contains(DataEntityClassDto.DATA_CONSUMER)) {
            details.setInputList(dto.getDataConsumerDetailsDto()
                .inputList()
                .stream()
                .distinct()
                .map(this::mapReference).collect(Collectors.toList()));
        }

        if (entityClasses.contains(DataEntityClassDto.DATA_ENTITY_GROUP)) {
            final List<DataEntityRef> dataEntityRefs = dto.getGroupsDto().entities().stream()
                .map(this::mapReference)
                .toList();
            details.setEntities(dataEntityRefs);
            details.setHasChildren(dto.getGroupsDto().hasChildren());
            details.setManuallyCreated(dto.getDataEntity().getManuallyCreated());
        }

        if (entityClasses.contains(DataEntityClassDto.DATA_INPUT)) {
            details.setOutputList(dto.getDataInputDetailsDto()
                .outputList()
                .stream()
                .distinct()
                .map(this::mapReference).collect(Collectors.toList()));
        }

        return details;
    }

    @Override
    public DataEntity mapDataQualityTest(final DataEntityDimensionsDto dto,
                                         final String severity) {
        final DataQualityTestDetailsDto dqDto = dto.getDataQualityTestDetailsDto();

        final DataEntityRun latestRun = dqDto.latestTaskRun() != null
            ? dataEntityRunMapper.mapDataEntityRun(dto.getDataEntity().getId(), dqDto.latestTaskRun())
            : null;

        return mapPojo(dto)
            .suiteName(dqDto.suiteName())
            .suiteUrl(dqDto.suiteUrl())
            .expectation(mapDataQualityTestExpectation(dqDto))
            .latestRun(latestRun)
            .linkedUrlList(mapLinkedUrlList(dqDto.linkedUrlList()))
            .severity(severity != null ? DataQualityTestSeverity.valueOf(severity) : null)
            .datasetsList(dqDto
                .datasetList()
                .stream()
                .map(this::mapRef)
                .collect(Collectors.toList()));
    }

    @Override
    public DataEntityList mapDataQualityTests(final Collection<DataEntityDimensionsDto> dtos,
                                              final Collection<DataQualityTestSeverityPojo> severities) {
        final Map<Long, DataQualityTestSeverityPojo> severityMap = severities
            .stream()
            .collect(Collectors.toMap(
                DataQualityTestSeverityPojo::getDataQualityTestId,
                identity()
            ));

        final List<DataEntity> items = dtos.stream()
            .map(d -> {
                final String severity = Optional
                    .ofNullable(severityMap.get(d.getDataEntity().getId()))
                    .map(DataQualityTestSeverityPojo::getSeverity)
                    .orElse(null);

                return mapDataQualityTest(d, severity);
            })
            .toList();

        return new DataEntityList().items(items);
    }

    @Override
    public DataEntityClass mapEntityClass(final DataEntityClassDto entityClass) {
        if (entityClass == null) {
            return null;
        }

        return new DataEntityClass()
            .id(entityClass.getId())
            .name(DataEntityClass.NameEnum.fromValue(entityClass.name()))
            .types(entityClass.getTypes().stream().map(this::mapType).toList());
    }

    @Override
    public DataEntityType mapType(final DataEntityTypeDto type) {
        if (type == null) {
            return null;
        }
        return new DataEntityType(type.getId(), DataEntityType.NameEnum.fromValue(type.name()));
    }

    @Override
    public DataEntityClassAndTypeDictionary getTypeDict() {
        if (TYPE_DICTIONARY == null) {
            TYPE_DICTIONARY = new DataEntityClassAndTypeDictionary()
                .entityClasses(Arrays.stream(DataEntityClassDto.values()).map(this::mapEntityClass).toList())
                .types(Arrays.stream(DataEntityTypeDto.values()).map(this::mapType).toList());
        }

        return TYPE_DICTIONARY;
    }

    @Override
    public DataEntityRef mapRef(final DataEntityDto dto) {
        return mapReference(dto);
    }

    @Override
    public DataEntityRef mapRef(final DataEntityPojo pojo) {
        return mapReference(pojo);
    }

    @Override
    public DataEntityUsageInfo mapUsageInfo(final DataEntityStatisticsPojo pojo,
                                            final Long filledEntitiesCount) {
        final Map<Integer, Map<Integer, Long>> classesAndTypesCount = pojo.getDataEntityClassesTypesCount() != null
            ? JSONSerDeUtils.deserializeJson(pojo.getDataEntityClassesTypesCount().data(), new TypeReference<>() {})
            : new HashMap<>();

        return new DataEntityUsageInfo()
            .totalCount(pojo.getTotalCount())
            .unfilledCount(pojo.getTotalCount() - filledEntitiesCount)
            .dataEntityClassesInfo(
                Arrays.stream(DataEntityClassDto.values())
                    .filter(dto -> dto != DataEntityClassDto.DATA_QUALITY_TEST_RUN
                        && dto != DataEntityClassDto.DATA_TRANSFORMER_RUN)
                    .map(dto -> mapToEntityClassUsage(dto, classesAndTypesCount))
                    .toList()
            );
    }

    @Override
    public DataEntityGroupItem mapGroupItem(final DataEntityDimensionsDto dimensionsDto,
                                            final boolean isUpperGroup) {
        final DataEntityGroupItem item = new DataEntityGroupItem();
        item.setIsUpperGroup(isUpperGroup);

        final DataEntityPojo pojo = dimensionsDto.getDataEntity();
        final Set<DataEntityClassDto> entityClasses =
            DataEntityClassDto.findByIds(dimensionsDto.getDataEntity().getEntityClassIds());
        final DataEntityType type = getDataEntityType(dimensionsDto.getDataEntity());

        final DataEntityBaseObject dataEntity = new DataEntityBaseObject()
            .id(pojo.getId())
            .oddrn(pojo.getOddrn())
            .externalName(pojo.getExternalName())
            .internalName(pojo.getInternalName())
            .ownership(ownershipMapper.mapDtos(dimensionsDto.getOwnership()))
            .entityClasses(entityClasses.stream().map(this::mapEntityClass).toList())
            .type(type)
            .status(dataEntityStatusMapper.mapStatus(pojo))
            .sourceCreatedAt(DateTimeUtil.mapUTCDateTime(pojo.getSourceCreatedAt()))
            .sourceUpdatedAt(DateTimeUtil.mapUTCDateTime(pojo.getSourceUpdatedAt()))
            .lastIngestedAt(DateTimeUtil.mapUTCDateTime(pojo.getLastIngestedAt()))
            .isStale(dataEntityStaleDetector.isDataEntityStale(pojo));
        item.setDataEntity(dataEntity);
        return item;
    }

    private DataEntityClassUsageInfo mapToEntityClassUsage(final DataEntityClassDto classDto,
                                                           final Map<Integer, Map<Integer, Long>> infoMap) {
        final DataEntityClassUsageInfo classUsageInfo = new DataEntityClassUsageInfo();
        classUsageInfo.setEntityClass(mapEntityClass(classDto));
        final Map<Integer, Long> typesInfo = infoMap.getOrDefault(classDto.getId(), Map.of());
        Long classSum = 0L;
        final List<DataEntityTypeUsageInfo> typeInfos = new ArrayList<>();
        for (final Map.Entry<Integer, Long> entry : typesInfo.entrySet()) {
            classSum += entry.getValue();
            final DataEntityTypeUsageInfo typeUsageInfo = new DataEntityTypeUsageInfo();
            final DataEntityTypeDto typeDto = DataEntityTypeDto.findById(entry.getKey())
                .orElseThrow(
                    () -> new IllegalArgumentException("Can't find type with id %d".formatted(entry.getKey())));
            typeUsageInfo.setEntityType(mapType(typeDto));
            typeUsageInfo.setTotalCount(entry.getValue());
            typeInfos.add(typeUsageInfo);
        }
        classUsageInfo.setTotalCount(classSum);
        classUsageInfo.setDataEntityTypesInfo(typeInfos);
        return classUsageInfo;
    }

    private LinkedUrl mapLinkedUrl(final LinkedUrlAttribute linkedUrlAttribute) {
        return new LinkedUrl(linkedUrlAttribute.url(), linkedUrlAttribute.name());
    }

    private List<LinkedUrl> mapLinkedUrlList(final Collection<LinkedUrlAttribute> linkedUrlAttributes) {
        return CollectionUtils.emptyIfNull(linkedUrlAttributes)
            .stream()
            .map(this::mapLinkedUrl).toList();
    }

    private DataEntityRef mapReference(final DataEntityDto dto) {
        return mapReference(dto.getDataEntity()).hasAlerts(dto.isHasAlerts());
    }

    private DataEntityRef mapReference(final DataEntityPojo pojo) {
        final List<DataEntityClass> entityClasses = DataEntityClassDto.findByIds(pojo.getEntityClassIds())
            .stream()
            .map(this::mapEntityClass)
            .toList();

        return new DataEntityRef()
            .id(pojo.getId())
            .oddrn(pojo.getOddrn())
            .externalName(pojo.getExternalName())
            .internalName(pojo.getInternalName())
            .entityClasses(entityClasses)
            .manuallyCreated(pojo.getManuallyCreated())
            .status(dataEntityStatusMapper.mapStatus(pojo))
            .isStale(dataEntityStaleDetector.isDataEntityStale(pojo))
            .url("");
    }

    private DataSetStats mapStats(final DataEntityDetailsDto.DataSetDetailsDto dataSetDetailsDto) {
        if (dataSetDetailsDto == null) {
            return new DataSetStats();
        }

        return new DataSetStats()
            .consumersCount(dataSetDetailsDto.consumersCount())
            .fieldsCount(dataSetDetailsDto.fieldsCount())
            .rowsCount(dataSetDetailsDto.rowsCount());
    }

    private DataQualityTestExpectation mapDataQualityTestExpectation(final DataQualityTestDetailsDto dto) {
        final DataQualityTestExpectation expectation = new DataQualityTestExpectation().type(dto.expectationType());
        expectation.putAll(MapUtils.emptyIfNull(dto.expectationParameters()));
        return expectation;
    }

    private DataEntityType getDataEntityType(final DataEntityPojo pojo) {
        final Integer typeId = pojo.getTypeId();

        return DataEntityTypeDto.findById(typeId)
            .map(this::mapType)
            .orElseThrow(() -> new IllegalArgumentException(
                String.format("No type with id %d for entity %s was found", typeId, pojo.getOddrn())));
    }

    private PageInfo pageInfo(final long total) {
        return new PageInfo(total, false);
    }

    private PageInfo pageInfo(final Page<?> page) {
        return new PageInfo(page.getTotal(), page.isHasNext());
    }
}
