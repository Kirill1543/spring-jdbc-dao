package com.kappadrive.dao.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
public @interface Query {

    @Retention(RetentionPolicy.SOURCE)
    @Target({ElementType.ANNOTATION_TYPE, ElementType.PARAMETER})
    @interface Param {
        String value();

        int type() default Integer.MIN_VALUE;
    }

    @Retention(RetentionPolicy.SOURCE)
    @Target({ElementType.ANNOTATION_TYPE, ElementType.METHOD})
    @interface Select {
        String value() default "";
    }

    @Retention(RetentionPolicy.SOURCE)
    @Target({ElementType.ANNOTATION_TYPE, ElementType.METHOD})
    @interface Insert {
        String value() default "";
    }

    @Retention(RetentionPolicy.SOURCE)
    @Target({ElementType.ANNOTATION_TYPE, ElementType.METHOD})
    @interface Delete {
        String value() default "";
    }

    @Retention(RetentionPolicy.SOURCE)
    @Target({ElementType.ANNOTATION_TYPE, ElementType.METHOD})
    @interface Update {
        String value() default "";
    }
}
