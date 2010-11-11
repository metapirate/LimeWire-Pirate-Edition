package com.limegroup.gnutella.security;

import java.security.PublicKey;

import com.limegroup.gnutella.util.DataUtils;

/**
 * A null certificate that doesn't verify against any {@link CertificateVerifier}.
 */
public class NullCertificate implements Certificate {
    
    @Override
    public String getCertificateString() {
        return null;
    }
    
    @Override
    public int getKeyVersion() {
        return 3;
    }
    
    @Override
    public PublicKey getPublicKey() {
        return new PublicKey() {
            @Override
            public String getFormat() {
                return "";
            }
            @Override
            public byte[] getEncoded() {
                return DataUtils.EMPTY_BYTE_ARRAY;
            }
            @Override
            public String getAlgorithm() {
                return "";
            }
        };
    }
    
    @Override
    public byte[] getSignature() {
        return DataUtils.EMPTY_BYTE_ARRAY;
    }
    
    @Override
    public byte[] getSignedPayload() {
        return DataUtils.EMPTY_BYTE_ARRAY;
    }
}