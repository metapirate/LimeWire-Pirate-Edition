package com.limegroup.gnutella.library;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import com.google.inject.BindingAnnotation;

/**
 * A marker annotation for injecting a {@link FileView} or
 * {@link FileCollection} of files visible to Gnutella.
 */
@BindingAnnotation
@Target( { FIELD, PARAMETER, METHOD })
@Retention(RUNTIME)
public @interface GnutellaFiles {
}