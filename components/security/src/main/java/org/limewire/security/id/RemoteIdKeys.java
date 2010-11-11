package org.limewire.security.id;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.limewire.io.BadGGEPBlockException;
import org.limewire.io.BadGGEPPropertyException;
import org.limewire.io.GGEP;
import org.limewire.io.GUID;
import org.limewire.io.InvalidDataException;

/**
 * RemoteIdKeys contains keys that the local node uses to securely communicate
 * with a remote node identified with the id field.  
 */
class RemoteIdKeys {
    private final PublicKey signaturePublicKey;

    private final SecretKey outgoingMacHmacKey;

    private final SecretKey incomingVerificationHmacKey;

    private final SecretKey outgoingEncryptionKey;

    private final SecretKey incomingDecryptionKey;

    private final GUID id;
    
    public RemoteIdKeys(byte[] data) throws InvalidDataException {
        try {
            GGEP ggep = new GGEP(data);
            id = new GUID(ggep.getBytes("ID"));
            KeyFactory factory = KeyFactory.getInstance(SecureIdManager.SIG_KEY_ALGO);
            signaturePublicKey = factory.generatePublic(new X509EncodedKeySpec(ggep.getBytes("SPK")));
            outgoingMacHmacKey = new SecretKeySpec(ggep.getBytes("OUTHMAC"), SecureIdManager.MAC_ALGO);
            incomingVerificationHmacKey = new SecretKeySpec(ggep.getBytes("INHMAC"), SecureIdManager.MAC_ALGO);
            outgoingEncryptionKey = new SecretKeySpec(ggep.getBytes("OUTENC"), SecureIdManager.ENCRYPTION_KEY_ALGO);
            incomingDecryptionKey = new SecretKeySpec(ggep.getBytes("INDEC"), SecureIdManager.ENCRYPTION_KEY_ALGO);
        } catch (BadGGEPBlockException e) {
            throw new InvalidDataException(e);
        } catch (BadGGEPPropertyException e) {
            throw new InvalidDataException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new InvalidDataException(e);
        } catch (InvalidKeySpecException e) {
            throw new InvalidDataException(e);
        }
    }
    
    public RemoteIdKeys(GUID id, PublicKey pk, SecretKey macHmacKey, SecretKey verificationHmacKey, SecretKey outgoingEncryptionKey, SecretKey incomingDecryptionKey) {
        this.id = id;
        signaturePublicKey = pk;
        this.outgoingMacHmacKey = macHmacKey;
        this.incomingVerificationHmacKey = verificationHmacKey;
        this.outgoingEncryptionKey = outgoingEncryptionKey;
        this.incomingDecryptionKey = incomingDecryptionKey;
    }

    public GUID getId() {
        return id;
    }

    public PublicKey getSignaturePublicKey() {
        return signaturePublicKey;
    }

    public SecretKey getOutgoingMacHmacKey() {
        return outgoingMacHmacKey;
    }
    public SecretKey getIncomingVerificationHmacKey() {
        return incomingVerificationHmacKey;
    }

    public SecretKey getOutgoingEncryptionKey() {
        return outgoingEncryptionKey;
    }
    public SecretKey getIncomingDecryptionKey() {
        return incomingDecryptionKey;
    }

    public byte[] toByteArray() {
        GGEP ggep = new GGEP();
        ggep.put("ID", id.bytes());
        ggep.put("SPK", signaturePublicKey.getEncoded());
        ggep.put("OUTHMAC", outgoingMacHmacKey.getEncoded());
        ggep.put("INHMAC", incomingVerificationHmacKey.getEncoded());
        ggep.put("OUTENC", outgoingEncryptionKey.getEncoded());
        ggep.put("INDEC", incomingDecryptionKey.getEncoded());
        return ggep.toByteArray();
    }
}