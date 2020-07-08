package com.kappadrive.dao.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.ANNOTATION_TYPE, ElementType.METHOD})
public @interface Query {

    @Retention(RetentionPolicy.SOURCE)
    @Target({ElementType.ANNOTATION_TYPE, ElementType.METHOD})
    @interface Select {
    }

    @Retention(RetentionPolicy.SOURCE)
    @Target({ElementType.ANNOTATION_TYPE, ElementType.METHOD})
    @interface Insert {
    }

    @Retention(RetentionPolicy.SOURCE)
    @Target({ElementType.ANNOTATION_TYPE, ElementType.METHOD})
    @interface Delete {
    }

    @Retention(RetentionPolicy.SOURCE)
    @Target({ElementType.ANNOTATION_TYPE, ElementType.METHOD})
    @interface Update {
    }
}
