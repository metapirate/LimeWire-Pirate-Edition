package org.limewire.security.id;

import java.security.InvalidAlgorithmParameterException;
import java.security.PublicKey;

import org.limewire.io.GUID;
import org.limewire.io.InvalidDataException;

/**
 * The SecureIdManager manages the identity of local node and 
 * keys shared between the local node and other nodes in the network.
 * 
 * When contacting another node the first time, the local node sends 
 * the public information of the local identity to the other node and 
 * gets back the other node's identity information. After verify the 
 * other node's identity, the local node does Diffie-Hellman key
 * agreement to setup long term symmetric keys shared between these 
 * two nodes. Those keys can be used for authentication and encryption
 * between them.
 * 
 * See {@link SecureIdManagerImplTest#testAliceAndBobSecureCommunication()} as 
 * an example.
 */
public interface SecureIdManager {
    /**
     * signature algorithm is RSA with SHA1 as message digest algorithm 
     */
    public static final String SIG_ALGO = "SHA1withRSA";
    
    /**
     * signature key algorithm is RSA.
     */
    public static final String SIG_KEY_ALGO = "RSA";

    /**
     * signature size and signature key size are both 768 
     */
    public static final int SIGNATURE_KEY_SIZE = 768;

    /**
     * Diffie-Hellman is the key agreement algorithm 
     */
    public static final String AGREEMENT_ALGO = "DH";
    
    /**
     * Hash algorithm is MD5. 
     * Most of bytes of the local node's GUID is the public signature key's hash value 
     */
    public static final String HASH_ALGO = "MD5";

    /**
     * message authentication code algorithm is MD5 based hmac
     */
    public static final String MAC_ALGO = "HmacMD5";

    /**
     * AES cipher is used for encryption
     */
    public static final String ENCRYPTION_KEY_ALGO = "AES";

    /** 
     * We use AES cipher, CBC mode of encryption, and PKCS5Padding as padding scheme 
     */ 
    public static final String ENCRYPTION_ALGO = "AES/CBC/PKCS5Padding";

    /**
     * @return if the local node knows the remoteID and shares a key with the remote node
     */
    public boolean isKnown(GUID remoteID);

    /**
     * @return hmac value
     * @throws Exception when remoteID not known 
     */
    public byte[] createHmac(GUID remoteID, byte[] data);

    /**
     * @return true if the data can be authenticated, i.e., the remoteID generated the hmac using the data.  
     * @throws Exception when remoteID not known 
     */
    public boolean verifyHmac(GUID remoteId, byte[] data, byte[] hmacValue);

    /**
     * encrypt the plaintext. The node with remoteId is able to decrypt the 
     * returned ciphertext. We recommend to use the bytes of message guid as 
     * the iv. If it is not available, remoteId.byte() is fine too. 
     * @param remoteId is the receiver's guid.
     * @param iv will be used to initialize the cipher. It MUST be 16 bytes long.   
     * @return ciphertext 
     * @throws Exception when remoteID not known
     * @throws InvalidAlgorithmParameterException when IV is not 16 bytes.
     */
    public byte[] encrypt(GUID remoteId, byte[] plaintext, byte[] iv) 
            throws InvalidAlgorithmParameterException;

    /**
     * decrypt the ciphertext sent by remoteId. The byte array of a message guid 
     * is the recommended iv. remoteId.byte() can also be used as iv. The 
     * application of encrypt() and decrypt() shall agree on what iv to use.
     * @param remoteId is the sender's guid.
     * @param iv will be used to initialize the cipher. It MUST be 16 bytes long.   
     * @return plaintext 
     * @throws Exception when remoteID not known 
     * @throws InvalidData when the ciphertext padding is wrong
     * @throws InvalidAlgorithmParameterException when IV is not 16 bytes.
     */
    public byte[] decrypt(GUID remoteId, byte[] ciphertext, byte[] iv)
            throws InvalidDataException, InvalidAlgorithmParameterException;

    /**
     * @return the signature 
     */
    public byte[] sign(byte[] data);

    /**
     * @return true if the data can be authenticated, 
     *  i.e., the remoteID generated the signature using the data.
     * @throws Exception when remoteID not known 
     * @throws InvalidData 
     */
    public boolean verifySignature(GUID remoteId, byte[] data, byte[] signature) throws InvalidDataException;

    /**
     * @return true if the data can be authenticated, 
     *  i.e., the signature of the data can be verified with the public key.     
     */
    public boolean verifySignature(PublicKey publicKey, byte[] data, byte[] signature);

    /**
     * return the identity of the local node, including ID, signature public key, 
     * diffie-hellman public component, and the signature on the first three fields.
     * the identity can be sent to other nodes on the network to setup shared keys.
     * @return the identity of the local node
     */
    public Identity getLocalIdentity();

    public GUID getLocalGuid();

    /**
     * process a remote node's identity:
     * 1) verify the remote node's id against its signature public key
     * 2) verify the signature
     * 3) store the identity if it is not in my list
     * @param identity
     * @return true if the remote node's identity is valid based on step 1) and 2). 
     */
    public boolean addIdentity(Identity identity);
}