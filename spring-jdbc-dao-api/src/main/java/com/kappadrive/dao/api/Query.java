package com.kappadrive.dao.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Maps method to some generated method pattern.
 */
@Retention(RetentionPolicy.SOURCE)
public @interface Query {

    /**
     * Marks custom method parameter to some field in named query.
     */
    @Retention(RetentionPolicy.SOURCE)
    @Target({ElementType.ANNOTATION_TYPE, ElementType.PARAMETER})
    @interface Param {
        /**
         * Returns parameter name in named query.
         *
         * @return parameter name in named query.
         */
        String value();

        /**
         * Returns type of parameter in named query.
         * Required, if provided java class differs from database type.
         *
         * @return type of parameter in named query.
         */
        int type() default Integer.MIN_VALUE;
    }

    /**
     * Marks that method should be generated with SELECT pattern.
     * Method can have any name in this case.
     */
    @Retention(RetentionPolicy.SOURCE)
    @Target({ElementType.ANNOTATION_TYPE, ElementType.METHOD})
    @interface Select {
        /**
         * Returns SQL query for SELECT.
         * If there is no custom SQL query, default one will be used.
         *
         * @return SQL query for SELECT.
         */
        String value() default "";
    }

    /**
     * Marks that method should be generated with INSERT pattern.
     * Method can have any name in this case.
     */
    @Retention(RetentionPolicy.SOURCE)
    @Target({ElementType.ANNOTATION_TYPE, ElementType.METHOD})
    @interface Insert {
        /**
         * Returns SQL query for INSERT.
         * If there is no custom SQL query, default one will be used.
         *
         * @return SQL query for INSERT.
         */
        String value() default "";
    }

    /**
     * Marks that method should be generated with DELETE pattern.
     * Method can have any name in this case.
     */
    @Retention(RetentionPolicy.SOURCE)
    @Target({ElementType.ANNOTATION_TYPE, ElementType.METHOD})
    @interface Delete {
        /**
         * Returns SQL query for DELETE.
         * If there is no custom SQL query, default one will be used.
         *
         * @return SQL query for DELETE.
         */
        String value() default "";
    }

    /**
     * Marks that method should be generated with UPDATE pattern.
     * Method can have any name in this case.
     */
    @Retention(RetentionPolicy.SOURCE)
    @Target({ElementType.ANNOTATION_TYPE, ElementType.METHOD})
    @interface Update {
        /**
         * Returns SQL query for UPDATE.
         * If there is no custom SQL query, default one will be used.
         *
         * @return SQL query for UPDATE.
         */
        String value() default "";
    }
}
