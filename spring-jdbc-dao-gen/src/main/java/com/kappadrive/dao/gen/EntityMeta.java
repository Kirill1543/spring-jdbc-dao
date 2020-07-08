package com.kappadrive.dao.gen;

import lombok.Builder;
import lombok.Getter;

import java.util.Collection;

@Getter
@Builder
public class EntityMeta {

    private final String tableName;
    private final Collection<FieldMeta> fields;
    private final Collection<FieldMeta> keyFields;
}
