package org.limewire.inject;

import java.lang.annotation.Annotation;

import com.google.inject.Binding;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Provider;
import com.google.inject.Scope;
import com.google.inject.Scopes;
import com.google.inject.internal.LinkedBindingImpl;
import com.google.inject.spi.BindingScopingVisitor;

/**
 * Extensions to the default Guice Scoping.
 */
public class MoreScopes {
    
    /**
     * Returns the scope of the binding, or the scope of the linked binding if
     * it was linked.
     */
    public static Scope getLinkedScope(Binding<?> binding) {
        BindingScopingVisitor<Scope> scoper = new BindingScopingVisitor<Scope>() {
            public Scope visitNoScoping() {
                return Scopes.NO_SCOPE;
            }

            public Scope visitScopeAnnotation(Class<? extends Annotation> scopeAnnotation) {
                throw new IllegalStateException("no annotations allowed here");
            }

            public Scope visitScope(Scope scope) {
                return scope;
            }

            public Scope visitEagerSingleton() {
                return EAGER_SINGLETON;
            }
        };
        
        do {
            Scope scope = binding.acceptScopingVisitor(scoper);

            if (scope != Scopes.NO_SCOPE) {
                return scope;
            }

            if (binding instanceof LinkedBindingImpl) {
                LinkedBindingImpl<?> linkedBinding = (LinkedBindingImpl) binding;
                Injector injector = linkedBinding.getInjector();
                if (injector != null) {
                    binding = injector.getBinding(linkedBinding.getLinkedKey());
                    continue;
                }
            }

            return Scopes.NO_SCOPE;
        } while (true);
    }

    /**
     * A singleton that will never be eager, in contrast to
     * {@link Scopes#SINGLETON}, which Guice eagerly creates sometimes.
     */
    public static final Scope LAZY_SINGLETON = new Scope() {
      public <T> Provider<T> scope(Key<T> key, Provider<T> creator) {
          return Scopes.SINGLETON.scope(key, creator);
      }

      @Override public String toString() {
        return "MoreScopes.LAZY_SINGLETON";
      }
    };
    
    /**
     * A singleton that will be eagerly loaded. A class with
     * an EagerSingleton annotation will be created at startup.
     */
    public static final Scope EAGER_SINGLETON = new Scope() {
        @Override
        public <T> Provider<T> scope(Key<T> key, Provider<T> unscoped) {
            return Scopes.SINGLETON.scope(key, unscoped);
        }        
        
        @Override public String toString() {
            return "MoreScopes.EAGER_SINGLETON";
        }
    };

}
