package com.kappadrive.dao.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Configure mapping entity to database table.
 * If there is no {@link Table} annotation on entity,
 * entity will be mapped to same database table, converting
 * lowercase and camelcase style to uppercase with downscore.
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface Table {

    /**
     * Returns name of the database table.
     *
     * @return name of the database table.
     */
    String name();
}
