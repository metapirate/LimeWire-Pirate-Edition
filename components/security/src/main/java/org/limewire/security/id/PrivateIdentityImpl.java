package org.limewire.security.id;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.interfaces.DHPrivateKey;

import org.limewire.io.BadGGEPBlockException;
import org.limewire.io.BadGGEPPropertyException;
import org.limewire.io.GGEP;
import org.limewire.io.GUID;
import org.limewire.io.InvalidDataException;
import org.limewire.util.ByteUtils;
import org.limewire.util.StringUtils;

public class PrivateIdentityImpl implements PrivateIdentity{
    
    private PrivateKey signaturePrivateKey;
    private DHPrivateKey dhPrivateKey;
    private int multiInstallationMark;
    private GUID id;
    private PublicKey signaturePublicKey;
    private BigInteger dhPublicComponent;
    private byte[] signature;
    
    public PrivateIdentityImpl(GUID id, PublicKey signatureKey, BigInteger dhPublicComponent, byte[] signature,
            PrivateKey signaturePrivateKey, DHPrivateKey diffieHellmanPrivateKey, int multiInstallationMark) {
        this.id = id;
        this.signaturePublicKey = signatureKey;
        this.dhPublicComponent = dhPublicComponent;
        this.signature = signature;
        this.signaturePrivateKey = signaturePrivateKey;
        this.dhPrivateKey = diffieHellmanPrivateKey;
        this.multiInstallationMark = multiInstallationMark; 
    }

    public PrivateIdentityImpl(byte[] data) throws InvalidDataException {        
        try{
            GGEP ggep = new GGEP(data);
            id = new GUID(ggep.getBytes("ID"));            
            KeyFactory factory = KeyFactory.getInstance(SecureIdManager.SIG_KEY_ALGO);
            signaturePublicKey = factory.generatePublic(new X509EncodedKeySpec(ggep.getBytes("SPU")));
            signaturePrivateKey = factory.generatePrivate(new PKCS8EncodedKeySpec(ggep.getBytes("SPV")));
            factory = KeyFactory.getInstance(SecureIdManager.AGREEMENT_ALGO);
            dhPublicComponent = new BigInteger(ggep.getBytes("DHPU"));
            dhPrivateKey = (DHPrivateKey) factory.generatePrivate(new PKCS8EncodedKeySpec(ggep.getBytes("DHPV")));
            signature = ggep.getBytes("SIG");
            multiInstallationMark = ByteUtils.beb2int(ggep.getBytes("MIM"), 0);
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
    
    @Override
    public byte[] toByteArray() {
        GGEP ggep = new GGEP();
        ggep.put("ID", id.bytes());
        ggep.put("SPU", signaturePublicKey.getEncoded());
        ggep.put("DHPU", dhPublicComponent.toByteArray());
        ggep.put("SIG", signature);
        ggep.put("SPV", signaturePrivateKey.getEncoded());
        ggep.put("DHPV", dhPrivateKey.getEncoded());
        byte[] buf = new byte[4];
        ByteUtils.int2beb(multiInstallationMark, buf, 0);
        ggep.put("MIM", buf);
        return ggep.toByteArray();
    }

    public GUID getGuid(){
        return id;
    }
    
    public PublicKey getPublicSignatureKey(){
        return signaturePublicKey;
    }
    
    public BigInteger getPublicDiffieHellmanComponent(){
        return dhPublicComponent;
    }
    
    public byte[] getSignature(){
        return signature;
    }
    
    public int getMultiInstallationMark() {
        return multiInstallationMark;
    }

    public PrivateKey getPrivateDiffieHellmanKey() {
        return dhPrivateKey;
    }

    public PrivateKey getPrivateSignatureKey() {
        return signaturePrivateKey;
    }

    @Override
    public String toString() {
        return StringUtils.toString(this);
    }    
}