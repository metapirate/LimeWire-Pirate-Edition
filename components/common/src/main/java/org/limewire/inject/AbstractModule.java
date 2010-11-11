package org.limewire.inject;

import java.lang.annotation.Annotation;

import com.google.inject.Key;
import com.google.inject.Provider;

/**
 * An extension for (@link com.google.inject.AbstractModule} that adds extra utility methods.
 */
public abstract class AbstractModule extends com.google.inject.AbstractModule {
    
    /*
     *  TODO: Convert the methods to use the a builder pattern instead, ie:
     *    bindMany(Interface.class).and(I1.class, I2.class, I3.class).[annotatedWith].to(....);
     *  or
     *    bind(Interface.class, I1.class, I2.class, I3.class)...
     *    
     */    
    
    /** 
     * Allows the caller to easily bind multiple interfaces to a single provider.
     * 
     * Example use:
     * <pre>
     *     bindAll(Names.named("backgroundExecutor"), ScheduledExecutorService.class, ScheduledExecutorServiceImpl.class, ExecutorService.class, Executor.class);
     * </pre>
     * 
     * @param annotation A shared annotation that defines the traits of the binding.
     * @param submostClass The class the provider provides.
     * @param providerClass The class of the provider.
     * @param otherClasses Other classes the provider can provide.
     */
    protected <T> void bindAll(Annotation annotation, Class<T> submostClass, Class<? extends Provider<? extends T>> providerClass, Class<? super T>... otherClasses) {
        Key<T> mainKey = Key.get(submostClass, annotation);
        bind(mainKey).toProvider(providerClass);
        for(Class<? super T> clazz : otherClasses) {
            bind(clazz).annotatedWith(annotation).to(mainKey);
        }
    }    
    
    /** A utility method for calling bindAll with one parameter without seeing a compile warning. */
    @SuppressWarnings("unchecked")
    protected <T> void bindAll(Annotation annotation, Class<T> submostClass, Class<? extends Provider<? extends T>> providerClass, Class<? super T> other1) {
        bindAll(annotation, submostClass, providerClass, new Class[] { other1 } );
    }
    
    /** A utility method for calling bindAll with two parameters without seeing a compile warning. */
    @SuppressWarnings("unchecked")    
    protected <T> void bindAll(Annotation annotation, Class<T> submostClass, Class<? extends Provider<? extends T>> providerClass, Class<? super T> other1, Class<? super T> other2) {
        bindAll(annotation, submostClass, providerClass, new Class[] { other1, other2 } );
    }
    
    /** A utility method for calling bindAll with three parameters without seeing a compile warning. */
    @SuppressWarnings("unchecked")
    protected <T> void bindAll(Annotation annotation, Class<T> submostClass, Class<? extends Provider<? extends T>> providerClass, Class<? super T> other1, Class<? super T> other2, Class<? super T> other3) {
        bindAll(annotation, submostClass, providerClass, new Class[] { other1, other2, other3 } );
    }

}
