package org.opendatadiscovery.oddplatform.repository.reactive;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Record2;
import org.jooq.SelectConditionStep;
import org.jooq.SelectHavingStep;
import org.jooq.SelectOnConditionStep;
import org.jooq.impl.DSL;
import org.opendatadiscovery.oddplatform.dto.DatasetFieldDto;
import org.opendatadiscovery.oddplatform.dto.DatasetStructureDto;
import org.opendatadiscovery.oddplatform.dto.TagDto;
import org.opendatadiscovery.oddplatform.dto.TagOrigin;
import org.opendatadiscovery.oddplatform.dto.dataset.DatasetVersionFields;
import org.opendatadiscovery.oddplatform.dto.metadata.DatasetFieldMetadataDto;
import org.opendatadiscovery.oddplatform.dto.term.LinkedTermDto;
import org.opendatadiscovery.oddplatform.dto.term.TermRefDto;
import org.opendatadiscovery.oddplatform.model.tables.pojos.DatasetFieldMetadataValuePojo;
import org.opendatadiscovery.oddplatform.model.tables.pojos.DatasetFieldPojo;
import org.opendatadiscovery.oddplatform.model.tables.pojos.DatasetFieldToTermPojo;
import org.opendatadiscovery.oddplatform.model.tables.pojos.DatasetVersionPojo;
import org.opendatadiscovery.oddplatform.model.tables.pojos.MetadataFieldPojo;
import org.opendatadiscovery.oddplatform.model.tables.pojos.NamespacePojo;
import org.opendatadiscovery.oddplatform.model.tables.pojos.TagPojo;
import org.opendatadiscovery.oddplatform.model.tables.pojos.TagToDatasetFieldPojo;
import org.opendatadiscovery.oddplatform.model.tables.pojos.TermPojo;
import org.opendatadiscovery.oddplatform.model.tables.records.DatasetVersionRecord;
import org.opendatadiscovery.oddplatform.repository.util.JooqQueryHelper;
import org.opendatadiscovery.oddplatform.repository.util.JooqReactiveOperations;
import org.opendatadiscovery.oddplatform.repository.util.JooqRecordHelper;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuples;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;
import static org.jooq.impl.DSL.countDistinct;
import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.jsonArrayAgg;
import static org.jooq.impl.DSL.max;
import static org.opendatadiscovery.oddplatform.model.Tables.DATASET_FIELD;
import static org.opendatadiscovery.oddplatform.model.Tables.DATASET_FIELD_METADATA_VALUE;
import static org.opendatadiscovery.oddplatform.model.Tables.DATASET_FIELD_TO_TERM;
import static org.opendatadiscovery.oddplatform.model.Tables.DATASET_STRUCTURE;
import static org.opendatadiscovery.oddplatform.model.Tables.DATASET_VERSION;
import static org.opendatadiscovery.oddplatform.model.Tables.DATA_ENTITY;
import static org.opendatadiscovery.oddplatform.model.Tables.ENUM_VALUE;
import static org.opendatadiscovery.oddplatform.model.Tables.METADATA_FIELD;
import static org.opendatadiscovery.oddplatform.model.Tables.NAMESPACE;
import static org.opendatadiscovery.oddplatform.model.Tables.TAG;
import static org.opendatadiscovery.oddplatform.model.Tables.TAG_TO_DATASET_FIELD;
import static org.opendatadiscovery.oddplatform.model.Tables.TERM;
import static reactor.function.TupleUtils.function;

@Repository
@Slf4j
public class ReactiveDatasetVersionRepositoryImpl
    extends ReactiveAbstractCRUDRepository<DatasetVersionRecord, DatasetVersionPojo>
    implements ReactiveDatasetVersionRepository {

    public static final String TAGS = "tags";
    public static final String TAG_RELATIONS = "tag_relations";
    public static final String ENUM_VALUE_COUNT = "enum_value_count";
    public static final String METADATA_VALUES = "metadata_values";
    public static final String METADATA = "metadata";
    public static final String TERMS = "terms";
    public static final String TERM_NAMESPACES = "term_namespaces";
    public static final String TERM_RELATIONS = "term_relations";

    private final JooqRecordHelper jooqRecordHelper;

    public ReactiveDatasetVersionRepositoryImpl(final JooqReactiveOperations jooqReactiveOperations,
                                                final JooqRecordHelper jooqRecordHelper,
                                                final JooqQueryHelper jooqQueryHelper) {
        super(jooqReactiveOperations, jooqQueryHelper, DATASET_VERSION, DatasetVersionPojo.class);
        this.jooqRecordHelper = jooqRecordHelper;
    }

    @Override
    public Mono<DatasetStructureDto> getDatasetVersion(final long datasetVersionId) {
        final List<Field<?>> selectFields = Stream.of(DATASET_VERSION.fields(), DATASET_FIELD.fields())
            .flatMap(Arrays::stream)
            .toList();

        final SelectHavingStep<Record> selectHavingStep = DSL
            .select(selectFields)
            .select(jsonArrayAgg(field(TAG_TO_DATASET_FIELD.asterisk().toString())).as(TAG_RELATIONS))
            .select(jsonArrayAgg(field(TAG.asterisk().toString())).as(TAGS))
            .select(jsonArrayAgg(field(DATASET_FIELD_METADATA_VALUE.asterisk().toString())).as(METADATA_VALUES))
            .select(jsonArrayAgg(field(METADATA_FIELD.asterisk().toString())).as(METADATA))
            .select(jsonArrayAgg(field(TERM.asterisk().toString())).as(TERMS))
            .select(jsonArrayAgg(field(DATASET_FIELD_TO_TERM.asterisk().toString())).as(TERM_RELATIONS))
            .select(jsonArrayAgg(field(NAMESPACE.asterisk().toString())).as(TERM_NAMESPACES))
            .select(countDistinct(ENUM_VALUE.ID).as(ENUM_VALUE_COUNT))
            .from(DATASET_VERSION)
            .leftJoin(DATASET_STRUCTURE).on(DATASET_STRUCTURE.DATASET_VERSION_ID.eq(DATASET_VERSION.ID))
            .leftJoin(DATASET_FIELD).on(DATASET_FIELD.ID.eq(DATASET_STRUCTURE.DATASET_FIELD_ID))
            .leftJoin(TAG_TO_DATASET_FIELD).on(DATASET_FIELD.ID.eq(TAG_TO_DATASET_FIELD.DATASET_FIELD_ID))
            .leftJoin(TAG).on(TAG_TO_DATASET_FIELD.TAG_ID.eq(TAG.ID)).and(TAG.DELETED_AT.isNull())
            .leftJoin(ENUM_VALUE).on(DATASET_FIELD.ID.eq(ENUM_VALUE.DATASET_FIELD_ID)
                .and(ENUM_VALUE.DELETED_AT.isNull()))
            .leftJoin(DATASET_FIELD_METADATA_VALUE)
            .on(DATASET_FIELD.ID.eq(DATASET_FIELD_METADATA_VALUE.DATASET_FIELD_ID))
            .leftJoin(METADATA_FIELD).on(DATASET_FIELD_METADATA_VALUE.METADATA_FIELD_ID.eq(METADATA_FIELD.ID))
            .leftJoin(DATASET_FIELD_TO_TERM).on(DATASET_FIELD.ID.eq(DATASET_FIELD_TO_TERM.DATASET_FIELD_ID))
            .leftJoin(TERM).on(DATASET_FIELD_TO_TERM.TERM_ID.eq(TERM.ID)).and(TERM.DELETED_AT.isNull())
            .leftJoin(NAMESPACE).on(TERM.NAMESPACE_ID.eq(NAMESPACE.ID))
            .where(DATASET_VERSION.ID.eq(datasetVersionId))
            .groupBy(selectFields);

        return jooqReactiveOperations
            .flux(selectHavingStep)
            .collect(groupingBy(this::extractDatasetVersion, mapping(this::extractDatasetFieldDto, toList())))
            .flatMap(m -> m.entrySet().stream().findFirst()
                .map(e -> Tuples.of(e.getKey(), isNullList(e.getValue()) ? List.<DatasetFieldDto>of() : e.getValue()))
                .map(Mono::just)
                .orElseGet(Mono::empty))
            .doOnNext(tuple -> setFieldDependencies(tuple.getT2()))
            .map(function((version, fields) -> DatasetStructureDto.builder()
                .datasetVersion(version)
                .datasetFields(fields)
                .build()));
    }

    @Override
    public Mono<List<DatasetVersionFields>> getDatasetVersionWithFields(final List<Long> datasetVersionIds) {
        final String fieldsAlias = "fields";
        final var query = DSL.select(DATASET_VERSION.fields())
            .select(jsonArrayAgg(field(DATASET_FIELD.asterisk().toString())).as(fieldsAlias))
            .from(DATASET_VERSION)
            .leftJoin(DATASET_STRUCTURE).on(DATASET_STRUCTURE.DATASET_VERSION_ID.eq(DATASET_VERSION.ID))
            .leftJoin(DATASET_FIELD).on(DATASET_FIELD.ID.eq(DATASET_STRUCTURE.DATASET_FIELD_ID))
            .where(DATASET_VERSION.ID.in(datasetVersionIds))
            .groupBy(DATASET_VERSION.fields());
        return jooqReactiveOperations.flux(query).map(r -> mapToDatasetVersionFields(r, fieldsAlias)).collectList();
    }

    @Override
    public Mono<DatasetStructureDto> getLatestDatasetVersion(final long datasetId) {
        final Field<Long> dsvMaxField = max(DATASET_VERSION.VERSION).as("dsv_max");

        final SelectHavingStep<Record2<String, Long>> subquery = DSL
            .select(DATASET_VERSION.DATASET_ODDRN, dsvMaxField)
            .from(DATASET_VERSION)
            .join(DATA_ENTITY).on(DATA_ENTITY.ODDRN.eq(DATASET_VERSION.DATASET_ODDRN))
            .where(DATA_ENTITY.ID.eq(datasetId))
            .groupBy(DATASET_VERSION.DATASET_ODDRN);

        final List<Field<?>> selectFields = Stream.of(DATASET_VERSION.fields(), DATASET_FIELD.fields())
            .flatMap(Arrays::stream)
            .toList();

        final SelectHavingStep<Record> selectHavingStep = DSL
            .select(selectFields)
            .select(jsonArrayAgg(field(TAG_TO_DATASET_FIELD.asterisk().toString())).as(TAG_RELATIONS))
            .select(jsonArrayAgg(field(TAG.asterisk().toString())).as(TAGS))
            .select(jsonArrayAgg(field(DATASET_FIELD_METADATA_VALUE.asterisk().toString())).as(METADATA_VALUES))
            .select(jsonArrayAgg(field(METADATA_FIELD.asterisk().toString())).as(METADATA))
            .select(jsonArrayAgg(field(TERM.asterisk().toString())).as(TERMS))
            .select(jsonArrayAgg(field(DATASET_FIELD_TO_TERM.asterisk().toString())).as(TERM_RELATIONS))
            .select(jsonArrayAgg(field(NAMESPACE.asterisk().toString())).as(TERM_NAMESPACES))
            .select(countDistinct(ENUM_VALUE.ID).as(ENUM_VALUE_COUNT))
            .from(subquery)
            .join(DATASET_VERSION)
            .on(DATASET_VERSION.DATASET_ODDRN.eq(subquery.field(DATASET_VERSION.DATASET_ODDRN)))
            .and(DATASET_VERSION.VERSION.eq(dsvMaxField))
            .leftJoin(DATASET_STRUCTURE).on(DATASET_STRUCTURE.DATASET_VERSION_ID.eq(DATASET_VERSION.ID))
            .leftJoin(DATASET_FIELD).on(DATASET_FIELD.ID.eq(DATASET_STRUCTURE.DATASET_FIELD_ID))
            .leftJoin(TAG_TO_DATASET_FIELD).on(DATASET_FIELD.ID.eq(TAG_TO_DATASET_FIELD.DATASET_FIELD_ID))
            .leftJoin(TAG).on(TAG_TO_DATASET_FIELD.TAG_ID.eq(TAG.ID)).and(TAG.DELETED_AT.isNull())
            .leftJoin(ENUM_VALUE).on(DATASET_FIELD.ID.eq(ENUM_VALUE.DATASET_FIELD_ID)
                .and(ENUM_VALUE.DELETED_AT.isNull()))
            .leftJoin(DATASET_FIELD_METADATA_VALUE)
            .on(DATASET_FIELD.ID.eq(DATASET_FIELD_METADATA_VALUE.DATASET_FIELD_ID))
            .leftJoin(METADATA_FIELD).on(DATASET_FIELD_METADATA_VALUE.METADATA_FIELD_ID.eq(METADATA_FIELD.ID))
            .leftJoin(DATASET_FIELD_TO_TERM).on(DATASET_FIELD.ID.eq(DATASET_FIELD_TO_TERM.DATASET_FIELD_ID))
            .leftJoin(TERM).on(DATASET_FIELD_TO_TERM.TERM_ID.eq(TERM.ID)).and(TERM.DELETED_AT.isNull())
            .leftJoin(NAMESPACE).on(TERM.NAMESPACE_ID.eq(NAMESPACE.ID))
            .groupBy(selectFields);

        return jooqReactiveOperations
            .flux(selectHavingStep)
            .collect(groupingBy(this::extractDatasetVersion, mapping(this::extractDatasetFieldDto, toList())))
            .flatMap(m -> m.entrySet().stream().findFirst()
                .map(e -> Tuples.of(e.getKey(), isNullList(e.getValue()) ? List.<DatasetFieldDto>of() : e.getValue()))
                .map(Mono::just)
                .orElseGet(Mono::empty))
            .doOnNext(tuple -> setFieldDependencies(tuple.getT2()))
            .map(function((version, fields) -> DatasetStructureDto.builder()
                .datasetVersion(version)
                .datasetFields(fields)
                .build()));
    }

    @Override
    public Mono<List<DatasetVersionPojo>> getVersions(final String datasetOddrn) {
        final SelectConditionStep<Record> selectConditionStep = DSL
            .select(DATASET_VERSION.fields())
            .from(DATASET_VERSION)
            .where(DATASET_VERSION.DATASET_ODDRN.eq(datasetOddrn));
        return jooqReactiveOperations.flux(selectConditionStep)
            .map(r -> r.into(DatasetVersionPojo.class))
            .collectList();
    }

    @Override
    public Flux<DatasetVersionPojo> getLatestVersions(final Collection<Long> datasetIds) {
        final String dsOddrnAlias = "dsv_dataset_oddrn";

        final Field<String> datasetOddrnField = DATASET_VERSION.DATASET_ODDRN.as(dsOddrnAlias);
        final Field<Long> dsvMaxField = max(DATASET_VERSION.VERSION).as("dsv_max");

        final SelectHavingStep<Record2<String, Long>> subquery = DSL
            .select(datasetOddrnField, dsvMaxField)
            .from(DATASET_VERSION)
            .join(DATA_ENTITY).on(DATA_ENTITY.ODDRN.eq(DATASET_VERSION.DATASET_ODDRN))
            .where(DATA_ENTITY.ID.in(datasetIds))
            .groupBy(DATASET_VERSION.DATASET_ODDRN);
        final SelectOnConditionStep<Record> conditionStep = DSL.select(DATASET_VERSION.fields())
            .from(subquery)
            .join(DATASET_VERSION).on(DATASET_VERSION.DATASET_ODDRN.eq(subquery.field(dsOddrnAlias, String.class)))
            .join(DATA_ENTITY).on(DATA_ENTITY.ODDRN.eq(DATASET_VERSION.DATASET_ODDRN))
            .and(DATASET_VERSION.VERSION.eq(dsvMaxField));

        return jooqReactiveOperations.flux(conditionStep).map(this::extractDatasetVersion);
    }

    @Override
    public Mono<List<DatasetVersionPojo>> getPenultimateVersions(final List<DatasetVersionPojo> lastVersions) {
        if (lastVersions.isEmpty()) {
            return Mono.just(List.of());
        }

        final Condition condition = lastVersions.stream()
            .map(v -> DATA_ENTITY.ODDRN.eq(v.getDatasetOddrn())
                .and(DATASET_VERSION.VERSION.eq(v.getVersion() - 1)))
            .reduce(Condition::or)
            .orElseThrow();

        final SelectConditionStep<Record> penultimateSelect = DSL
            .select(DATASET_VERSION.asterisk())
            .from(DATASET_VERSION)
            .join(DATA_ENTITY).on(DATA_ENTITY.ODDRN.eq(DATASET_VERSION.DATASET_ODDRN))
            .where(condition);

        return jooqReactiveOperations.flux(penultimateSelect)
            .map(this::extractDatasetVersion)
            .collectList();
    }

    @Override
    public Mono<Map<Long, List<DatasetFieldPojo>>> getDatasetVersionFields(final Set<Long> dataVersionPojoIds) {
        return jooqReactiveOperations.executeInPartitionReturning(new ArrayList<>(dataVersionPojoIds), versions -> {
            final var vidToFieldsSelect = DSL.select(DATASET_STRUCTURE.DATASET_VERSION_ID)
                .select(DATASET_FIELD.asterisk())
                .from(DATASET_FIELD)
                .join(DATASET_STRUCTURE).on(DATASET_STRUCTURE.DATASET_FIELD_ID.eq(DATASET_FIELD.ID))
                .where(DATASET_STRUCTURE.DATASET_VERSION_ID.in(versions));
            return jooqReactiveOperations.flux(vidToFieldsSelect);
        }).collect(
            groupingBy(r -> r.get(DATASET_STRUCTURE.DATASET_VERSION_ID), mapping(this::extractDatasetField, toList())));
    }

    private DatasetVersionPojo extractDatasetVersion(final Record datasetVersionRecord) {
        return jooqRecordHelper.extractRelation(datasetVersionRecord, DATASET_VERSION, DatasetVersionPojo.class);
    }

    private DatasetFieldPojo extractDatasetField(final Record datasetVersionRecord) {
        return jooqRecordHelper.extractRelation(datasetVersionRecord, DATASET_FIELD, DatasetFieldPojo.class);
    }

    private DatasetFieldDto extractDatasetFieldDto(final Record datasetVersionRecord) {
        final DatasetFieldPojo datasetFieldPojo = extractDatasetField(datasetVersionRecord);
        if (datasetFieldPojo == null) {
            return null;
        }
        return DatasetFieldDto.builder()
            .datasetFieldPojo(datasetFieldPojo)
            .tags(extractTags(datasetVersionRecord))
            .metadata(extractMetadata(datasetVersionRecord))
            .terms(extractTerms(datasetVersionRecord))
            .enumValueCount(datasetVersionRecord.get(ENUM_VALUE_COUNT, Integer.class))
            .build();
    }

    private DatasetVersionFields mapToDatasetVersionFields(final Record record,
                                                           final String fieldsAlias) {
        final DatasetVersionPojo version = jooqRecordHelper
            .extractRelation(record, DATASET_VERSION, DatasetVersionPojo.class);
        final Set<DatasetFieldPojo> fields = jooqRecordHelper
            .extractAggRelation(record, fieldsAlias, DatasetFieldPojo.class);
        return new DatasetVersionFields(version, fields);
    }

    private List<TagDto> extractTags(final Record record) {
        final Set<TagPojo> tags = jooqRecordHelper.extractAggRelation(record, TAGS, TagPojo.class);

        final Map<Long, TagToDatasetFieldPojo> relations = jooqRecordHelper
            .extractAggRelation(record, TAG_RELATIONS, TagToDatasetFieldPojo.class)
            .stream()
            .collect(Collectors.toMap(TagToDatasetFieldPojo::getTagId, identity()));

        return tags.stream()
            .map(tagPojo -> new TagDto(tagPojo, null,
                !TagOrigin.INTERNAL.name().equals(relations.get(tagPojo.getId()).getOrigin())))
            .toList();
    }

    private List<DatasetFieldMetadataDto> extractMetadata(final Record record) {
        final Set<MetadataFieldPojo> metadataFields =
            jooqRecordHelper.extractAggRelation(record, METADATA, MetadataFieldPojo.class);

        final Map<Long, DatasetFieldMetadataValuePojo> values = jooqRecordHelper
            .extractAggRelation(record, METADATA_VALUES, DatasetFieldMetadataValuePojo.class)
            .stream()
            .collect(Collectors.toMap(DatasetFieldMetadataValuePojo::getMetadataFieldId, identity()));

        return metadataFields.stream()
            .map(pojo -> new DatasetFieldMetadataDto(pojo, values.get(pojo.getId())))
            .toList();
    }

    private List<LinkedTermDto> extractTerms(final Record record) {
        final Set<TermPojo> terms = jooqRecordHelper.extractAggRelation(record, TERMS, TermPojo.class);

        final Map<Long, NamespacePojo> namespaces = jooqRecordHelper
            .extractAggRelation(record, TERM_NAMESPACES, NamespacePojo.class)
            .stream()
            .collect(Collectors.toMap(NamespacePojo::getId, identity()));

        final Map<Long, List<DatasetFieldToTermPojo>> relations = jooqRecordHelper
            .extractAggRelation(record, TERM_RELATIONS, DatasetFieldToTermPojo.class)
            .stream()
            .collect(Collectors.groupingBy(DatasetFieldToTermPojo::getTermId));

        return terms.stream()
            .map(pojo -> {
                final TermRefDto termRefDto = TermRefDto.builder()
                    .term(pojo)
                    .namespace(namespaces.get(pojo.getNamespaceId()))
                    .build();
                final boolean isDescriptionLink = relations.getOrDefault(pojo.getId(), List.of()).stream()
                    .anyMatch(r -> Boolean.TRUE.equals(r.getIsDescriptionLink()));
                return new LinkedTermDto(termRefDto, isDescriptionLink);
            })
            .toList();
    }

    private boolean isNullList(final List<DatasetFieldDto> list) {
        return CollectionUtils.isNotEmpty(list)
            && list.stream().allMatch(Objects::isNull);
    }

    private void setFieldDependencies(final List<DatasetFieldDto> fields) {
        final Map<String, Long> dsfIdMap = fields
            .stream()
            .map(DatasetFieldDto::getDatasetFieldPojo)
            .collect(Collectors.toMap(DatasetFieldPojo::getOddrn, DatasetFieldPojo::getId));

        for (final DatasetFieldDto field : fields) {
            final DatasetFieldPojo pojo = field.getDatasetFieldPojo();

            if (StringUtils.isNotEmpty(pojo.getParentFieldOddrn())) {
                final Long parentFieldId = dsfIdMap.get(pojo.getParentFieldOddrn());
                if (parentFieldId == null) {
                    log.warn("Dataset field with oddrn {} has unknown parent field with oddrn {}",
                        pojo.getOddrn(), pojo.getParentFieldOddrn());
                } else {
                    field.setParentFieldId(dsfIdMap.get(pojo.getParentFieldOddrn()));
                }
            }

            if (StringUtils.isNotEmpty(pojo.getReferenceOddrn())) {
                final Long referenceFieldId = dsfIdMap.get(pojo.getReferenceOddrn());
                if (referenceFieldId == null) {
                    log.warn("Dataset field with oddrn {} has unknown reference field with oddrn {}",
                        pojo.getOddrn(), pojo.getReferenceOddrn());
                } else {
                    field.setReferenceFieldId(dsfIdMap.get(pojo.getReferenceOddrn()));
                }
            }
        }
    }
}
