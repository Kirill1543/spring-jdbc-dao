package com.kappadrive.dao.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Main annotation which triggers DAO generation from interface.
 * Implementation name in this case will be "*Impl", where * is interface name.
 * Supports next method patterns: SELECT, INSERT, UPDATE, DELETE.
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface GenerateDao {

    /**
     * Returns entity type.
     *
     * @return entity type.
     */
    Class<?> value();

    /**
     * Marks if generated structure should be abstract.
     * May be used for partial usage of generated functionality or fixing existing bugs.
     * In this case some other not-generated class should extend generated implementation.
     * Implementation name in this case will be "Abstract*", where * is interface name.
     *
     * <code>false</code> is default.
     *
     * @return <code>true</code> if generated structure should be abstract,
     * <code>false</code> otherwise.
     */
    boolean makeAbstract() default false;
}
