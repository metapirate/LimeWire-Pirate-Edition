package org.limewire.xmpp.client.impl;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.util.prefs.Preferences;

import org.limewire.friend.api.PasswordManager;
import org.limewire.inject.LazySingleton;
import org.limewire.security.certificate.CipherProvider;

import com.google.inject.Inject;

/**
 * Stores passwords under a usernames in {@link Preferences} and also
 * allows for deletion and retrieval of passwords.
 */
@LazySingleton
public class PasswordManagerImpl implements PasswordManager {

    static final String PREFERENCES_NODE = "/limewire/xmpp/auth";

    private CipherProvider cipherProvider;

    @Inject
    public PasswordManagerImpl(CipherProvider cipherProvider) {
        this.cipherProvider = cipherProvider;
    }

    @Override
    public String loadPassword(String userName) throws IOException {
        if (userName == null || userName.isEmpty()) {
            throw new IllegalArgumentException("userName cannot be null or empty");
        }

        String encryptedPassword = loadEncryptedPassword(userName);
        Password pwd = new Password(cipherProvider, encryptedPassword, true);

        // test to see if we can decrypt it without error
        try {
            return pwd.decryptPassword();
        } catch (IOException e) {
            throw new IOException("Error decrypting password", e);
        } catch (GeneralSecurityException e) {
            throw new IOException("Error decrypting password", e);
        }
    }

    @Override
    public void storePassword(String userName, String rawPassword) throws IOException {
        if (userName == null || userName.isEmpty()) {
            throw new IllegalArgumentException("username cannot be null or empty");
        }
        if (rawPassword == null || rawPassword.isEmpty()) {
            throw new IllegalArgumentException("password cannot be null or empty");
        }

        Preferences prefs = getPreferences();
        Password pwd = new Password(cipherProvider, rawPassword, false);

        try {
            prefs.put(userName, pwd.encryptPassword());
        } catch (NoSuchAlgorithmException e) {
            throw new IOException("Error encrypting password", e);
        } catch (IOException e) {
            throw new IOException("Error encrypting password", e);
        }
    }

    @Override
    public void removePassword(String userName) {
        if (userName == null || userName.isEmpty()) {
            throw new IllegalArgumentException("userName must not be null or empty");
        }
        Preferences prefs = getPreferences();
        prefs.remove(userName);
    }

    private String loadEncryptedPassword(String userName) {
        Preferences prefs = getPreferences();
        return prefs.get(userName, "");
    }

    private Preferences getPreferences() {
        return Preferences.userRoot().node(PREFERENCES_NODE);
    }
}
