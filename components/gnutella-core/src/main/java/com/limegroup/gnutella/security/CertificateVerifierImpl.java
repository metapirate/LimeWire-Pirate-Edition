package com.limegroup.gnutella.security;

import java.security.SignatureException;

import org.limewire.security.SignatureVerifier;
import org.limewire.util.Objects;

/**
 * Verifies certificates against a public DSA key given as base32 encoded
 * string.
 */
public class CertificateVerifierImpl implements CertificateVerifier {

    private static final String PUBLIC_MASTER_KEY= "GCBADNZQQIASYBQHFKDERTRYAQATBAQBD4BIDAIA7V7VHAI5OUJCSUW7JKOC53HE473BDN2SHTXUIAGDDY7YBNSREZUUKXKAEJI7WWJ5RVMPVP6F6W5DB5WLTNKWZV4BHOAB2NDP6JTGBN3LTFIKLJE7T7UAI6YQELBE7O5J277LPRQ37A5VPZ6GVCTBKDYE7OB7NU6FD3BQENKUCNNBNEJS6Z27HLRLMHLSV37SEIBRTHORJAA4OAQVACLWAUEPCURQXTFSSK4YFIXLQQF7AWA46UBIDAIA67Q2BBOWTM655S54VNODNOCXXF4ZJL537I5OVAXZK5GAWPIHQJTVCWKXR25NIWKP4ZYQOEEBQC2ESFTREPUEYKAWCO346CJSRTEKNYJ4CZ5IWVD4RUUOBI5ODYV3HJTVSFXKG7YL7IQTKYXR7NRHUAJEHPGKJ4N6VBIZBCNIQPP6CWXFT4DJFC3GL2AHWVJFMQAUYO76Z5ESUA4BQQAAFAMAOFY3UY6LTOJAHMJNA7BXD7WYJOEEOJAFFLWD46OKDK7QU36XXZZ7HCINKZ7TJBWDVKX4H6Z3TFVQDWVTNNK4LHACECHPRSBUHS43TZIJFHNLPDBJPZS6ZG4AKS4DW5BGMG4VWX52SPE6PT2XYUW4KLBIMOF2I5N22NIJFJJ4B5H3BM3R75KRDYJWI2GSNE65YSDVGUFP3XNCK";
    private final String base32MasterKey;
    
    /**
     * Uses internal {@link #PUBLIC_MASTER_KEY} for verification.
     */
    public CertificateVerifierImpl() {
        this(PUBLIC_MASTER_KEY);
    }
    
    /**
     * Uses the given base32 encoded public DSA key for certificate verification.
     * @param base32MasterKey
     */
    public CertificateVerifierImpl(String base32MasterKey) {
        this.base32MasterKey = Objects.nonNull(base32MasterKey, "base32MasterKey");
    }
    
    @Override
    public Certificate verify(Certificate certificate) throws SignatureException {
        SignatureVerifier signatureVerifier = new SignatureVerifier(certificate.getSignedPayload(), certificate.getSignature(), SignatureVerifier.readKey(base32MasterKey, "DSA"), "DSA");
        if (!signatureVerifier.verifySignature()) {
            throw new SignatureException("Invalid signature for: " + certificate);
        }
        return certificate;
    }

}
