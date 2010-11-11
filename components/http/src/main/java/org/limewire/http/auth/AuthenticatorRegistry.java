package org.limewire.http.auth;

/**
 * Registry for {@link Authenticator authenticators} that
 * lets them register with it.
 */
public interface AuthenticatorRegistry {

    /**
     * Registers an Authenticator with this registry.
     * The authenticator must not be null.
     *
     * @param authenticator the {@link Authenticator} to
     * register with this authenticator registry.
     * @throws NullPointerException if authenticator is null
     */
    void register(Authenticator authenticator);
}
