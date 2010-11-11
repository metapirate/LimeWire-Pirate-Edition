package com.limegroup.gnutella.filters;

import org.limewire.util.Visitor;

/**
 * Manages a file containing blacklisted URNs, which is updated periodically
 * via HTTP. The manager's <code>iterator()</code> method can be used to read
 * the URNs from disk as base32-encoded strings.
 */
public interface URNBlacklistManager {
    /** The maximum length of the blacklist in bytes (20 bytes per URN). */
    static final int MAX_LENGTH = 600000; // 30 thousand URNs
    /** The length of the signature in bytes. */
    static final int SIG_LENGTH = 46;
    /** The algorithm for verifying the signature. */
    static final String SIG_ALGORITHM = "SHA1withDSA"; // Same as SIMPP
    /** The algorithm for reconstructing the public key. */
    static final String KEY_ALGORITHM = "DSA"; // Same as SIMPP
    /** The public key, base32-encoded. */
    static final String PUBLIC_KEY = "GCBADNZQQIASYBQHFKDERTRYAQATBAQBD4BIDAIA7V7VHAI5OUJCSUW7JKOC53HE473BDN2SHTXUIAGDDY7YBNSREZUUKXKAEJI7WWJ5RVMPVP6F6W5DB5WLTNKWZV4BHOAB2NDP6JTGBN3LTFIKLJE7T7UAI6YQELBE7O5J277LPRQ37A5VPZ6GVCTBKDYE7OB7NU6FD3BQENKUCNNBNEJS6Z27HLRLMHLSV37SEIBRTHORJAA4OAQVACLWAUEPCURQXTFSSK4YFIXLQQF7AWA46UBIDAIA67Q2BBOWTM655S54VNODNOCXXF4ZJL537I5OVAXZK5GAWPIHQJTVCWKXR25NIWKP4ZYQOEEBQC2ESFTREPUEYKAWCO346CJSRTEKNYJ4CZ5IWVD4RUUOBI5ODYV3HJTVSFXKG7YL7IQTKYXR7NRHUAJEHPGKJ4N6VBIZBCNIQPP6CWXFT4DJFC3GL2AHWVJFMQAUYO76Z5ESUA4BQQAAFAMAO23AF7C247RPE4RGGMCU3XQTRVG3ZIKKQUVAS2BKNDBDB3W7L375GYP7ZWZL2RP3WAIBOHZ52G7KT46EAGBUG7DWQNZS4IWC2GDVU4PQ74Q64BJWMK2DZ6G7GYESYHUPBNDOB5PLI2WPF33NIAOXNYQXSEJLTSPUXBMY3RHAQY3TRG6EKQ6CNNZJ2NRVY3RZXLAV3QMVENJIQ";

    /**
     * Loads and verifies the URN blacklist, then passes each successfully
     * loaded URN to the given visitor as a base32-encoded string. This method
     * blocks.
     */
    void loadURNs(Visitor<String> visitor);
}
