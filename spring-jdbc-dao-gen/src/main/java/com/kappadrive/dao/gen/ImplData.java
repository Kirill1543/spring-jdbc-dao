package com.kappadrive.dao.gen;

import lombok.Builder;
import lombok.Getter;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import java.util.Collection;

@Getter
@Builder
public class ImplData {

    private final String packageName;
    private final TypeElement interfaceElement;
    private final DeclaredType interfaceType;
    private final DeclaredType entityType;
    private final EntityMeta entityMeta;
    private final Collection<ExecutableElement> daoMethods;
}
