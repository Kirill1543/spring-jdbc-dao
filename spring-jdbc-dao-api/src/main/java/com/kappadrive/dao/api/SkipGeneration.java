package com.kappadrive.dao.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks method which implementation should not be generated
 * during DAO generation process.
 * Works only if {@link GenerateDao#makeAbstract()} is <code>true</code>.
 */
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.ANNOTATION_TYPE, ElementType.METHOD})
public @interface SkipGeneration {
}
