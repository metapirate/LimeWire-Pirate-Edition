package org.limewire.inject;

import java.lang.annotation.Annotation;
import java.util.Map;

import com.google.inject.Binding;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Scope;
import com.google.inject.spi.BindingScopingVisitor;

/**
 * Various Guice Utility methods.
 */
public class GuiceUtils {

    /**
     * ScopingVisitor that checks for eagerly bound singletons. Will return true 
     * if a given binding is an EagerSingleton, false otherwise. 
     */
    private static BindingScopingVisitor<Boolean> eagerScopingVisitor = new BindingScopingVisitor<Boolean>(){

        @Override public Boolean visitEagerSingleton(){
            return true;
        }

        @Override public Boolean visitNoScoping(){
            return false;
        }

        @Override public Boolean visitScope(Scope scope){
            return scope == MoreScopes.EAGER_SINGLETON;
        }

        @Override public Boolean visitScopeAnnotation(Class<? extends Annotation> scopeAnnotation){
            return false;
        }
    };
    
    /**
     * Given an Injector, creates an instance of all known bindings with
     * an EagerSingleton annotation or eagerly loaded bindings located
     * within a module.
     */
    public static void loadEagerSingletons(Injector injector) {
        Map<Key<?>, Binding<?>> map = injector.getAllBindings();
        for(Binding<?> binding : map.values()) {
            if(binding.acceptScopingVisitor(eagerScopingVisitor)) {
                binding.getProvider().get();
            }
        }
    }
}
