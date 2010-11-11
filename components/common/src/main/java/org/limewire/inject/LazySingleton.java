package org.limewire.inject;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import java.lang.annotation.Retention;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import java.lang.annotation.Target;

import com.google.inject.Injector;
import com.google.inject.ScopeAnnotation;

/**
 * Apply this to implementation classes when you want only one instance
 * (per {@link Injector}) to be reused for all injections for that binding.
 * 
 * The singleton is guaranteed to be constructed lazily.
 */
@Target( { TYPE, METHOD })
@Retention(RUNTIME)
@ScopeAnnotation
public @interface LazySingleton {

}
