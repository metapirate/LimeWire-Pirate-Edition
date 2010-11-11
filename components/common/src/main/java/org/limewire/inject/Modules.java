package org.limewire.inject;

import java.util.Map;
import java.util.logging.Logger;

import com.google.inject.Binder;
import com.google.inject.Binding;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.Provider;
import com.google.inject.Scope;
import com.google.inject.Stage;

public class Modules {
    
    private Modules() {}

    /**
     * Returns a module that returns all providers from the parent module.
     */
    public static Module providersFrom(final Injector parent) {
        return new Module() {
            @SuppressWarnings("unchecked")
            public void configure(Binder binder) {
                // These types cannot be rebound.
                Key loggerKey = Key.get(Logger.class);
                Key injectorKey = Key.get(Injector.class);
                Key stageKey = Key.get(Stage.class);
                
                for(Map.Entry<Key<?>, Binding<?>> entry : parent.getAllBindings().entrySet()) {
                    Key key = entry.getKey();
                    Binding<?> binding = entry.getValue();
                    Scope scope = MoreScopes.getLinkedScope(binding);
                    if(!key.equals(loggerKey) && !key.equals(injectorKey) && !key.equals(stageKey) && !key.getTypeLiteral().getRawType().equals(Provider.class)) {
                        binder.bind(key).toProvider(binding.getProvider()).in(scope) ;
                    }
                }
                
            }
        };
    }

}
