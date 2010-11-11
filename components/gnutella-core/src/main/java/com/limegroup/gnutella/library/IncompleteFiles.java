package com.limegroup.gnutella.library;

import com.google.inject.BindingAnnotation;
import java.lang.annotation.Target;
import java.lang.annotation.Retention;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;

/**
 * A marker annotation for injecting a {@link FileView} of incomplete files.
 */
@BindingAnnotation
@Target( { FIELD, PARAMETER, METHOD })
@Retention(RUNTIME)
public @interface IncompleteFiles {
}