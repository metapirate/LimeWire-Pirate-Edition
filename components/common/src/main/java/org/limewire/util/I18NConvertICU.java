package org.limewire.util;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.HashMap;
import java.util.Map;

import com.ibm.icu.text.Normalizer;
/**
 * Removes accents and symbols, and normalizes strings.
 * 
 */
final class I18NConvertICU extends AbstractI18NConverter {

    /** excluded codepoints (like accents) */
    private java.util.BitSet _excluded;
    /** certain chars to be replaced by space (like commas, etc) */
    private java.util.BitSet _replaceWithSpace;
    private Map _cMap;

    /**
     * initializer:
     * this subclass of AbstractI18NConverter uses the icu4j's 
     * pacakges to normalize Strings.  
     * _excluded and _replaceWithSpace (BitSet) are read in from
     * files created by UDataFileCreator and are used to 
     * remove accents, etc. and replace certain code points with
     * ascii space (\u0020)
     */
    I18NConvertICU()
        throws IOException, ClassNotFoundException {
    	java.util.BitSet bs = null;
        java.util.BitSet bs2 = null;
    	Map hm = null;

        InputStream fi = CommonUtils.getResourceStream("org/limewire/util/excluded.dat");
        //read in the explusion bitset
        ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(fi));
        bs = (java.util.BitSet)ois.readObject();
        ois.close();
        
        fi = CommonUtils.getResourceStream("org/limewire/util/caseMap.dat");
        //read in the case map
        ois = new ConverterObjectInputStream(new BufferedInputStream(fi));
        hm = (HashMap)ois.readObject();
        ois.close();
        
        fi = CommonUtils.getResourceStream("org/limewire/util/replaceSpace.dat");
        ois = new ObjectInputStream(new BufferedInputStream(fi));
        bs2 = (java.util.BitSet)ois.readObject();
        ois.close();

    	_excluded = bs;
    	_cMap = hm;
        _replaceWithSpace = bs2;
    }
    
    /**
     * Return the converted form of the string s
     * this method will also split the s into the different
     * unicode blocks
     * @param s String to be converted
     * @return the converted string
     */
    @Override
    public String getNorm(String s) {
        return convert(s);
    } 
    
    /**
     * Simple composition of a String.
     */
    @Override
    public String compose(String s) {
        return Normalizer.compose(s, false);
    }
    
    /**
     * convert the string into NFKC + removal of accents, symbols, etc.
     * uses icu4j's Normalizer to first decompose to NFKD form,
     * then removes all codepoints in the exclusion BitSet 
     * finally composes to NFC and adds spaces '\u0020' between
     * different unicode blocks
     *
     * @param String to convert
     * @return converted String
     */
    private String convert(String s) {
    	//decompose to NFKD
    	String nfkd = Normalizer.decompose(s, true);
        StringBuilder buf = new StringBuilder();
    	int len = nfkd.length();
    	String lower;
    	char c;
    
    	//loop through the string and check for excluded chars
    	//and lower case if necessary
    	for(int i = 0; i < len; i++) {
    	    c = nfkd.charAt(i);
            if(_replaceWithSpace.get(c)) {
                buf.append(" ");
            }
    	    else if(!_excluded.get(c)) {
                lower = (String)_cMap.get(String.valueOf(c));
                if(lower != null)
                    buf.append(lower);
                else
                    buf.append(c);
    	    }
    	}
    	
    	//compose to nfc and split
    	return blockSplit(Normalizer.compose(buf.toString(), false));
    }

}





