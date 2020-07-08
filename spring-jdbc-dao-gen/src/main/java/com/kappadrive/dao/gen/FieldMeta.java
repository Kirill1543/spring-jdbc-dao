package com.kappadrive.dao.gen;

import lombok.Builder;
import lombok.Getter;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;

@Getter
@Builder
public class FieldMeta {

    private final VariableElement field;
    private final TypeMirror type;
    private final ExecutableElement getter;
    private final ExecutableElement setter;
    private final String name;
    private final String columnName;
    private final Integer sqlType;
    private final boolean isKey;
    private final boolean isGenerated;
}
