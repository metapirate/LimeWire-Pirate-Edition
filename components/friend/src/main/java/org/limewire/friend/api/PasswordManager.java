package org.limewire.friend.api;

import java.io.IOException;

/**
 * Defines an interface for saving and loading passwords.
 */
public interface PasswordManager {
    /**
     * Returns the password stored for <code>username</code>.
     * @param username must not be null or empty
     * @return the password
     * @throws IOException if there is problem decoding the password or there
     * is no password for the username
     * @throws IllegalArgumentException if username is null or empty
     */
    public String loadPassword(String username) throws IOException;
    /**
     * Stores encrpyted password associated with username.
     * @param username must not be null or empty
     * @param password must not be null or empty
     * @throws IOException if there is a problem encrypting the password or storing it
     * @throws IllegalArgumentException if either username or password are null or empty
     */
    public void storePassword(String username, String password) throws IOException;
    /**
     * Removes the password and user name from the password store.
     * @param username must not be null or empty
     */
    public void removePassword(String username);
}
