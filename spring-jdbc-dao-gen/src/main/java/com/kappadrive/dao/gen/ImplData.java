package com.kappadrive.dao.gen;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import java.util.Collection;

class ImplData {

    private final String packageName;
    private final TypeElement interfaceElement;
    private final DeclaredType interfaceType;
    private final DeclaredType entityType;
    private final DeclaredType idType;
    private final Collection<FieldMeta> fields;
    private final Collection<ExecutableElement> daoMethods;

    ImplData(String packageName, TypeElement interfaceElement, DeclaredType interfaceType, DeclaredType entityType, DeclaredType idType, Collection<FieldMeta> fields, Collection<ExecutableElement> daoMethods) {
        this.packageName = packageName;
        this.interfaceElement = interfaceElement;
        this.interfaceType = interfaceType;
        this.entityType = entityType;
        this.idType = idType;
        this.fields = fields;
        this.daoMethods = daoMethods;
    }

    public String getPackageName() {
        return packageName;
    }

    public TypeElement getInterfaceElement() {
        return interfaceElement;
    }

    public DeclaredType getInterfaceType() {
        return interfaceType;
    }

    public DeclaredType getEntityType() {
        return entityType;
    }

    public DeclaredType getIdType() {
        return idType;
    }

    public Collection<FieldMeta> getFields() {
        return fields;
    }

    public Collection<ExecutableElement> getDaoMethods() {
        return daoMethods;
    }

    static Builder builder() {
        return new Builder();
    }

    static class Builder {

        private String packageName;
        private TypeElement interfaceElement;
        private DeclaredType interfaceType;
        private DeclaredType entityType;
        private DeclaredType idType;
        private Collection<FieldMeta> fields;
        private Collection<ExecutableElement> daoMethods;

        private Builder() {
        }

        public Builder packageName(String packageName) {
            this.packageName = packageName;
            return this;
        }

        public Builder interfaceElement(TypeElement interfaceElement) {
            this.interfaceElement = interfaceElement;
            return this;
        }

        public Builder interfaceType(DeclaredType interfaceType) {
            this.interfaceType = interfaceType;
            return this;
        }

        public Builder entityType(DeclaredType entityType) {
            this.entityType = entityType;
            return this;
        }

        public Builder idType(DeclaredType idType) {
            this.idType = idType;
            return this;
        }

        public Builder fields(Collection<FieldMeta> fields) {
            this.fields = fields;
            return this;
        }

        public Builder daoMethods(Collection<ExecutableElement> daoMethods) {
            this.daoMethods = daoMethods;
            return this;
        }

        ImplData build() {
            return new ImplData(packageName, interfaceElement, interfaceType, entityType, idType, fields, daoMethods);
        }
    }
}
