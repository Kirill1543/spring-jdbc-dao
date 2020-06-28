package com.kappadrive.dao.gen;

import javax.lang.model.element.VariableElement;

public class FieldMeta {

    private final VariableElement field;

    public FieldMeta(VariableElement field) {
        this.field = field;
    }

    public VariableElement getField() {
        return field;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private VariableElement field;

        private Builder() {
        }

        public Builder field(VariableElement field) {
            this.field = field;
            return this;
        }

        public FieldMeta build() {
            return new FieldMeta(field);
        }
    }
}
