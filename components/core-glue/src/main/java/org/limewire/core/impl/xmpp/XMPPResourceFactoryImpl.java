package org.limewire.core.impl.xmpp;

import org.apache.commons.codec.binary.Base64;
import org.limewire.security.SHA1;
import org.limewire.util.StringUtils;
import com.limegroup.gnutella.ApplicationServices;
import org.limewire.core.api.xmpp.XMPPResourceFactory;

import com.google.inject.Inject;

public class XMPPResourceFactoryImpl implements XMPPResourceFactory {
    
    private final ApplicationServices applicationServices;
    
    @Inject
    public XMPPResourceFactoryImpl(ApplicationServices applicationServices) {
        this.applicationServices = applicationServices;
    }
    
    public String getResource() {
        // The resource is set to the hash of the GUID to uniquely identify
        // the instance of the client
        byte[] hash = new SHA1().digest(applicationServices.getMyGUID());
        byte[] base64 = Base64.encodeBase64(hash);
        return StringUtils.getUTF8String(base64);
    }
}
