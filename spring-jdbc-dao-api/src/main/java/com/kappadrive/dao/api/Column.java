package com.kappadrive.dao.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Configure mapping entity field to database field.
 * If there is no {@link Column} annotation on field,
 * field will be mapped to same database field, converting
 * lowercase and camelcase style to uppercase with downscore.
 */
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.FIELD, ElementType.ANNOTATION_TYPE})
public @interface Column {

    /**
     * Returns database field name.
     * Is required if entity field spelling differs from database spelling.
     *
     * @return database field name.
     */
    String name() default "";

    /**
     * Returns database field type.
     * Is required if entity field type differs from database type.
     *
     * @return database field type.
     */
    int type() default Integer.MIN_VALUE;
}
