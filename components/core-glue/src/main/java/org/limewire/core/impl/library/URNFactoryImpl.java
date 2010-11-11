package org.limewire.core.impl.library;

import java.io.IOException;

import org.limewire.core.api.URN;
import org.limewire.core.api.library.URNFactory;

import com.google.inject.Inject;

/**
 * Live core implementation of URNFactory.
 */
public class URNFactoryImpl implements URNFactory {

    @Inject
    public URNFactoryImpl() {}
    
    @Override
    public URN createSHA1Urn(String sha1Urn) throws IOException {
        return com.limegroup.gnutella.URN.createSHA1Urn(sha1Urn);
    }
}
