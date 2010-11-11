package org.limewire.http.auth;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.http.auth.Credentials;
import org.limewire.util.Objects;

import com.google.inject.Singleton;

/**
 * Default implementation for {@link AuthenticatorRegistry}, also implements
 * {@link Authenticator} forwarding all authentication requests to the registered
 * authenticators.
 */
@Singleton
public class AuthenticatorRegistryImpl implements Authenticator, AuthenticatorRegistry {
    
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final Set<Authenticator> authenticators = new HashSet<Authenticator>();
    
    public void register(AuthenticatorRegistry registry) {
        // no-op since this is the registry itself
    }

    public boolean authenticate(Credentials credentials) {
        lock.readLock().lock();
        try {
            for(Authenticator authenticator : authenticators) {
                if(authenticator.authenticate(credentials)) {
                    return true;
                }
            }
        } finally {
            lock.readLock().unlock();
        }
        return false;
    }

    public void register(Authenticator authenticator) {
        Objects.nonNull(authenticator, "Authenticator");
        lock.writeLock().lock();
        try {
            authenticators.add(authenticator);
        } finally {
            lock.writeLock().unlock();
        }
    }
}
