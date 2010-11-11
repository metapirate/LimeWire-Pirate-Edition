/*
 * XMLStringUtils.java
 *
 * Created on April 24, 2001, 11:03 AM
 */

package com.limegroup.gnutella.xml;

import java.util.ArrayList;
import java.util.List;

/**
 * Provides utility methods to process the canonicalized strings we use to
 * represent XML document structure. The structure is explained below:
 * <p>
 * So as to preserve the structure, Structure.Field will be represented as
 * Structure__Field (Double Underscore is being used as a delimiter to 
 * represent the structure).
 *<p>
 * In case of multiple structured values with same name, 
 * as might occur while using + or * in the regular expressions in schema,
 * those should be represented as using the array index using the __ 
 * notation (without the square brackets)
 * <p>
 * Tor e.g. myarray[0].name ==> myarray__0__name
 * <p>    
 * Attribute names for an element in the XML schema should be postfixed 
 * with __ (double underscore).
 * <p>
 * So element.attribute ==> element__attribute__
 * @author  asingla
 */
public class XMLStringUtils {
    
    /**
     * Delimiter used to preserve the structural information in the
     * canonicalized xml field strings.
     */
    public static final String DELIMITER = "__";
    
    /**
     * Breaks the given string (which confirms to the pattern defined above
     * in the class description) into a list (of strings) such that the 
     * first element in the list is the top most structural element, 
     * and the last one the actual field/attribute name.
     *
     * @param canonicalizedField The string thats needed to be split
     *
     * @return List (of strings) . The first element in the list is the top
     * most structural element, and the last one the actual field/attribute
     * name
     */ 
    public static List<String> split(String canonicalizedField) {
        List<String> returnList = new ArrayList<String>();
        
        int lastIndex = 0;
        int index = 0;
        //break into parts
        while((index = canonicalizedField.indexOf(DELIMITER, lastIndex)) != -1) {
            //add the structural part
            returnList.add(canonicalizedField.substring(lastIndex, index));
            lastIndex = index + DELIMITER.length();
            //index = index + DELIMITER.length();
        }
        
        //if the last part is element (and not attribute that ends with the
        //DELIMITER), then we need to store that also
        if(!canonicalizedField.endsWith(DELIMITER))
            returnList.add(canonicalizedField.substring(lastIndex));
        
        return returnList;
    }
    
    /**
     * Derives the last field name from a given name.
     * With input "things__thing__field__", this will return "field".
     */
    public static String getLastField(String canonicalKey, String full) {
        //      things__thing__field__
        //      ^                   ^
        //     idx                 idx2
        
        int idx = full.indexOf(canonicalKey);
        if(idx == -1 || idx != 0)
            return null;
            
        int length = canonicalKey.length();
        int idx2 = full.indexOf(DELIMITER, length);
        if(idx2 == -1)
            return null;
            
        // insert quotes around field name if it has a space.
        return full.substring(length, idx2);
    }
}
