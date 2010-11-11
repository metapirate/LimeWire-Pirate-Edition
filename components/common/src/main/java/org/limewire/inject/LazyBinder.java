package org.limewire.inject;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicReference;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Provider;
import com.google.inject.Scope;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.google.inject.spi.BindingScopingVisitor;

/**
 * A factory for creating Providers that will enforce extreme laziness of
 * creating the implementation class.  The implementation class can be
 * injected directly, and it will not be constructed until at least
 * one method is called on the implementation.
 * <p>
 * This enforces that all bindings are done on implementations scoped
 * to a {@code @}{@link Singleton} or {@code @}{@link LazySingleton},
 * to ensure that methods are called on the same underlying instance
 * of the implementation through repeated calls.
 */
public class LazyBinder<T> implements Provider<T> {
    
    private final AtomicReference<T> providee = new AtomicReference<T>();
    private final Class<T> expected;
    private final Class<? extends T> implClass;
    private Injector injector;
    
    /**
     * Constructs a new Provider that will ensure the implementation class is
     * created only after at least one method is called.
     */
    public static <T> Provider<T> newLazyProvider(Class<T> expected, Class<? extends T> implClass) {
        if (!expected.isInterface()) {
            throw new RuntimeException("Expected class must be an interface");
        }
        return new LazyBinder<T>(expected, implClass);
    }
    
    private LazyBinder(Class<T> expected, Class<? extends T> implClass) {
        this.expected = expected;
        this.implClass = implClass;
    }
    
    @SuppressWarnings("unused")
    @Inject private void registerAndCheckTypes(Injector injector) {
        this.injector = injector;
        injector.getBinding(implClass).acceptScopingVisitor(new BindingScopingVisitor<Void>() {
            public Void visitEagerSingleton() {
                return null;
            }

            public Void visitNoScoping() {
                throw new RuntimeException("Class: " + implClass + " must be in scope @Singleton or @LazySingleton or @EagerSingleton");
            };

            public Void visitScope(Scope scope) {
                if(scope != Scopes.SINGLETON && scope != MoreScopes.LAZY_SINGLETON && scope != MoreScopes.EAGER_SINGLETON) {
                    throw new RuntimeException("Class: " + implClass + " must be in scope @Singleton or @LazySingleton or @EagerSingleton");    
                }
                return null;
            };

            public Void visitScopeAnnotation(
                    Class<? extends Annotation> scopeAnnotation) {
                throw new RuntimeException("Wasn't expecting this");
            };
        });
    }
    
    @Override
    public T get() {
        // Keep only one LazyT.
        T got = providee.get();
        if(got != null) {
            return got;
        } else {
            providee.compareAndSet(null, createProxy(expected, injector.getProvider(implClass)));
            return providee.get();
        }
    }
    
    private static <T> T createProxy(Class<T> expected, final Provider<? extends T> provider) {
        ClassLoader classLoader = expected.getClassLoader();
        return expected.cast(Proxy.newProxyInstance(classLoader, new Class[] { expected },
                new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args)
                            throws Throwable {
                        try {
                            return method.invoke(provider.get(), args);
                        } catch(InvocationTargetException ite) {
                            throw ite.getTargetException();
                        }
                    }
                }));
    }

}
