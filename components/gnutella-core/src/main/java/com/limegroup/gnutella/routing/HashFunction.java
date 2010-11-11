package com.limegroup.gnutella.routing;

import org.limewire.util.I18NConvert;
import org.limewire.util.StringUtils;

import com.limegroup.gnutella.util.QueryUtils;

/** 
 * The official platform-independent hashing function for query-routing.  The
 * key property is that it allows interpolation of hash tables of different
 * sizes.  More formally, with x&gt;=0, n&gt;=0, k&gt;=0, 0&lt;=r&lt;=n,<ul>
 * <li>2 ^ k * hash(x, n) &lt;= hash(x, n+k) &lt 2 ^ (k+1) * hash(x, n);</li>
 * <li>hash(x, n-r) = int(hash(x, n) / 2 ^ r).</li>
 * </ul>
 *
 * This version should now work cross-platform, however it is not intended
 * to be secure, only very fast to compute.  See Chapter 12.3.2. of CLR
 * for details of multiplication-based algorithms.
 */
public class HashFunction {
    //private static final double A=(Math.sqrt(5.0)-1.0)/2.0;
    //private static final long TWO_31=0x80000000l;
    //private static final int A_INT=(int)(A*TWO_31); //=1327217884
    private static final int A_INT=0x4F1BBCDC;
        
    /**
     * Returns the n-<b>bit</b> hash of x, where n="bits".  That is, the
     * returned value value can fit in "bits" unsigned bits, and is
     * between 0 and (2^bits)-1.
     */
    private static int hashFast(int x, byte bits) {
        // Keep only the "bits" highest bits of the 32 *lowest* bits of the
        // product (ignore overflowing bits of the 64-bit product result).
        // The constant factor should distribute equally each byte of x in
        // the returned bits.
        return (x * A_INT) >>> (32 - bits);
    }

    /*
     * Returns the n-bit hash of x.toLowerCase(), where n=<tt>bits</tt>.
     * That is, the returned value value can fit in "<tt>bits</tt>" unsigned
     * bits, and is between 0 and <tt>(2 ^ bits) - 1</tt>.
     *
     * @param x the string to hash
     * @param bits the number of bits to use in the resulting answer
     * @return the hash value
     * @see hash(String,int,int,byte)
     */    
    public static int hash(String x, byte bits) {
        return hash(x, 0, x.length(), bits);
    }       

    /**
     * Returns the same value as hash(x.substring(start, end), bits), but tries
     * to avoid allocations.<p>
     *
     * Note that x is lower-cased when hashing, using a locale-neutral
     * character case conversion based on the UTF-16 representation of the
     * source string to hash.  So it is stable across all platforms and locales.
     * However this does not only convert ASCII characters but ALL Unicode
     * characters having a single lowercase mapping character.  No attempt is
     * made here to remove accents and diacritics.<p>
     *
     * The string is supposed to be in NFC canonical form, but this is not
     * enforced here.  Conversion to lowercase of characters uses Unicode rules
     * built into the the java.lang.Character core class, excluding all special
     * case rules (N-to-1, 1-to-M, N-to-M, locale-sensitive and contextual).<p>
     *
     * A better way to hash strings would be to use String conversion in the
     * Locale.US context (for stability across servents) after transformation
     * to NFKD and removal of all diacritics from hashed keywords.  If needed,
     * this should be done before splitting the query string into hashable
     * keywords.
     *
     * @param x the string to hash
     * @param start the start offset of the substring to hash
     * @param end just PAST the end of the substring to hash
     * @param bits the number of bits to use in the resulting answer
     * @return the hash value 
     */   
    public static int hash(String x, int start, int end, byte bits) {
        //1. First turn x[start...end-1] into a number by treating all 4-byte
        //chunks as a little-endian quadword, and XOR'ing the result together.
        //We pad x with zeroes as needed. 
        //    To avoid having do deal with special cases, we do this by XOR'ing
        //a rolling value one byte at a time, taking advantage of the fact that
        //x XOR 0==x.
        int xor=0;  //the running total
        int j=0;    //the byte position in xor.  INVARIANT: j==8*((i-start)%4)
        for (int i=start; i<end; i++) {
            // internationalization be damned? Not a problem here:
            // we just hash the lower 8 bits of the lowercase UTF-16 code-units
            // representing characters, ignoring only the high 8 bits that
            // indicate a Unicode page, and it is not very widely distributed
            // even though they could also have feeded the hash function.
            xor ^= (Character.toLowerCase(x.charAt(i)) & 0xFF) << j;
            j = (j + 8) & 24;
        }
        //2. Now map number to range 0 - (2^bits-1).
        return hashFast(xor, bits);
    }


    /** 
     * Returns a list of canonicalized keywords in the given file name, suitable
     * for passing to hash(String,int).  The returned keywords are
     * lower-cased, though that is not strictly needed as hash ignores
     * case.<p>
     *
     * This function is not consistent for case conversion: it uses a locale
     * dependant String conversion, which also considers special casing rules
     * (N-to-1, 1-to-M, N-to-N, locale-sensitive and contextual variants),
     * unlike the simplified case conversion done in
     * <tt>hash(String, int, int, byte)</tt>, which is locale-neutral.<p>
     *
     * A better way to hash strings would be to use String conversion in the
     * Locale.US context (for stability across servents) after transformation
     * to NFKD and removal of all diacritics from hashed keywords.  If needed,
     * this should be done before splitting the file name string into hashable
     * keywords. Then we should remove the unneeded toLowerCase() call in
     * the <tt>hash(String, int, int, byte)</tt> function.
     * 
     * @param fileName The name of the file to break up into keywords.  These
     *  keywords will subsequently be hashed for inclusion in the bit vector.
     */
    public static String[] keywords(String filePath) {
        //TODO1: this isn't a proper implementation.  It should really be
        //to tokenized by ALL non-alphanumeric characters.

        //TODO2: perhaps we should do an English-specific version that accounts
        //for plurals, common keywords, etc.  But that's only necessary for 
        //our own files, since the assumption is that queries have already been
        //canonicalized. 
        return StringUtils.split(
            // TODO: a better canonicalForm(query) function here that
            // also removes accents by converting first to NFKD and keeping
            // only PRIMARY differences
            I18NConvert.instance().getNorm(filePath),
            QueryUtils.DELIMITERS);
    }

    /** 
     * Returns the index of the keyword starting at or after the i'th position
     * of query, or -1 if no such luck.
     */
    public static int keywordStart(String query, int i) {
        //Search for the first character that is not a delimiterer TODO3: we can
        //make this O(|DELIMITERS|) times faster by converting
        //FileManager.DELIMITERS into a Set in this' static initializer.  But
        //then we have to allocate Strings here.  Can work around the problem,
        //but it's trouble.
        final String DELIMITERS=QueryUtils.DELIMITERS;
        for ( ; i<query.length() ; i++) {
            char c=query.charAt(i);
            //If c not in DELIMITERS, declare success.
            if (DELIMITERS.indexOf(c)<0)
                return i;
        }
        return -1;
    }   

    /** 
     * Returns the index just past the end of the keyword starting at the i'th
     * position of query, or query.length() if no such index.
     */
    public static int keywordEnd(String query, int i) {
        //Search for the first character that is a delimiter.  
        //TODO3: see above
        final String DELIMITERS=QueryUtils.DELIMITERS;
        for ( ; i<query.length() ; i++) {
            char c=query.charAt(i);
            //If c in DELIMITERS, declare success.
            if (DELIMITERS.indexOf(c)>=0)
                return i;
        }
        return query.length();
    }    
        

    /**
     * @return an array of strings with the original strings and prefixes
     */
    public static String[] getPrefixes(String[] words){
        // 1. Count the number of words that can have prefixes (5 chars or more)
        int prefixable = 0;
        for (int i = 0; i < words.length; i++) {
            if (words[i].length() > 4)
                prefixable++;
        }
        // 2. If none, just returns the same words (saves allocations)
        if (prefixable == 0)
            return words;
        // 3. Create an expanded array with words and prefixes
        final String[] retArray = new String[words.length + prefixable * 2];
        int j = 0;
        for (int i = 0; i < words.length; i++) {
            final String word = words[i];
            retArray[j++] = word;
            final int len = word.length();
            if (len > 4) {
                retArray[j++] = word.substring(0, len - 1);
                retArray[j++] = word.substring(0, len - 2);
            }
        }
        return retArray;
    }
}
