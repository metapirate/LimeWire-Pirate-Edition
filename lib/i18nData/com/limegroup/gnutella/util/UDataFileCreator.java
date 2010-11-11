package com.limegroup.gnutella.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;


/**
 * this class is used to create the data files required by
 * I18NConvert and I18NData classes...
 * more details can be found at www.unicode.org on codepoints, categories, etc.
 * esp. UAX#15, UCD (Unicode Character Database Documentation)
 *
 * necessary files are : CaseFolding.txt, - from unicode.org
 *                       MnKeep.txt, - created
 *                       UnicodeData.txt - from unicode.org
 *                       NormalizationTest-3.2.0.txt - from unicode.org
 */
public class UDataFileCreator {

    private static final String DATA_DIR = "data/";
    private static final String BUILD_DIR = DATA_DIR + "built/";
    
    //created file names
    private static final String NUDATA = "nudata.txt";
    private static final String EXCLUDED_DAT = "excluded.dat";
    private static final String CASE_MAP = "caseMap.dat";
    private static final String REPLACE_SPACE = "replaceSpace.dat";
    
    //data file names
    //case mapping
    private static final String CASE_FOLDING = "CaseFolding.txt";
    //characters we don't want to exclude/replace even if they are punctuation
    private static final String MN_KEEP = "MnKeep.txt";
    //get all codepoints and categories
    private static final String UNICODE_DATA = "UnicodeData.txt";
    //get full deKomposition (nfkd)
    private static final String NORMALIZATION_TEST = "NormalizationTest-3.2.0.txt";
    

    /**
     * @param args unused
     */
    public static void main(String[] args) {
        UDataFileCreator ufc = new UDataFileCreator();
        ufc.createFile();
    }
    
    /**
     * 
     */
    public void createFile() {
        java.util.BitSet dontExclude = new java.util.BitSet();
        Map codepoints = new TreeMap(new StringComparator());

        HashMap caseMap = new HashMap();
        java.util.BitSet excludedChars = new java.util.BitSet();
        java.util.BitSet replaceWithSpace = new java.util.BitSet();

        HashMap tempNFKC = new HashMap();
        

        try {
            readNonExclusion(dontExclude);
            dealWithUnicodeData(codepoints, 
				dontExclude, 
				excludedChars,
				replaceWithSpace);
            readNTestPopKD(codepoints, tempNFKC);
            readCaseFolding(caseMap);
            replaceCase(codepoints, caseMap, excludedChars);
            //all we need now is caseMap and excludedChars
            writeOutObjects(codepoints, 
                            caseMap, 
                            excludedChars, 
                            replaceWithSpace,
                            tempNFKC);
        }
        catch(IOException e) {
            e.printStackTrace();
        }
        
        System.out.println("number of excluded code points : " + numEx);
    }

    /**
     * write out object files
     */
    private void writeOutObjects(Map codepoint,
                                 Map caseMap, 
                                 java.util.BitSet excludedChars,
                                 java.util.BitSet replaceWithSpace,
                                 HashMap nfkc) 
        throws IOException {

        FileOutputStream fo = 
            new FileOutputStream(new File(BUILD_DIR + NUDATA));

        BufferedWriter bufo =
            new BufferedWriter(new OutputStreamWriter(fo));
        
        Iterator iter = codepoint.keySet().iterator();
        while(iter.hasNext()) {
            String s = (String)iter.next();
            udata u = (udata)codepoint.get(s);

            if(!u.deKomp.equals("")) {
                bufo.write(s + ";");

                String composition = (String)nfkc.get(u.deKomp);
                composition = composition == null || composition.equals(u.deKomp)?"":composition;
                bufo.write(u.deKomp + ";" + composition + ";\n");
            }
        }
        
        bufo.flush();
        bufo.close();
        
        fo = new FileOutputStream(new File(BUILD_DIR+EXCLUDED_DAT));
        ObjectOutputStream oo = new ObjectOutputStream(fo);
        oo.writeObject(excludedChars);
        
        fo = new FileOutputStream(new File(BUILD_DIR+CASE_MAP));
        oo = new ObjectOutputStream(fo);
        oo.writeObject(caseMap);
        
        fo = new FileOutputStream(new File(BUILD_DIR+REPLACE_SPACE));
        oo = new ObjectOutputStream(fo);
        oo.writeObject(replaceWithSpace);
    
    }

    /**
     * read in the file that lists code points that are to be kept even
     * though they might belong to a category that should be excluded
     * (ie. dakuon, etc)
     */
    private void readNonExclusion(java.util.BitSet ex) 
        throws IOException {
        //most Mn will be excluded but few will be kept like voiced marks
        //this list may change
        BufferedReader buf = getBR(DATA_DIR+MN_KEEP);
        String line, codepoint;
        String[] s;
        int dI, start, end;

        while((line = buf.readLine()) != null) {
            dI = line.indexOf(';');
            codepoint = (line.substring(0,dI)).trim();
            
            //if the listed codepoint represents a range (ie. 3099..309A)
            if(codepoint.indexOf("..") > -1) {
                s = StringUtils.split(codepoint, "..");
                start = Integer.parseInt(s[0],16) -1;
                end = Integer.parseInt(s[1],16);
                while(end != start)
                    ex.set(end--);
            }
            else
                ex.set(Integer.parseInt(codepoint, 16));
        }
        buf.close();
    }
    

    /**
     * read in the unicode data file to get a list of all the codepoints
     * and to determine the category of these codepoints
     */
    private void dealWithUnicodeData(Map cp, 
				     java.util.BitSet dontExclude, 
				     java.util.BitSet excluded,
				     java.util.BitSet replaceWithSpace) 
        throws IOException {
        
        BufferedReader buf = getBR(DATA_DIR+UNICODE_DATA); 
        //file has codepoints below FFFD
        String line;
        boolean go = true;

        while((line = buf.readLine()) != null && go) 
            go = processLine(cp, dontExclude, line, excluded, replaceWithSpace);
        
        buf.close();
    }

    /**
     * variable keeping track of number of excluded codepoints.
     */
    int numEx = 0;
    
    private boolean processLine(Map cp, 
                                java.util.BitSet dontExclude, 
                                String line, 
                                java.util.BitSet excluded,
                                java.util.BitSet replaceWithSpace) {
        String[] parts = StringUtils.splitNoCoalesce(line, ";");
        if(parts[0].equals("FFEE")) return false;
        if(parts[2].charAt(0) == 'P' || parts[2].equals("Zs")) {
            //puctuations should be mostly be replaces with \u0020 (space).
            //except for apostraphes
            if(excludedPClass(parts[0])) {
                numEx++;
                excluded.set(Integer.parseInt(parts[0],16));
            }
            else if(!isExcluded(parts, dontExclude)) {
                //not expluded
                //put this codepoint into the cp map
                udata u = new udata();
                //populate the category for the data wrapper
                u.cat = parts[2];
                u.CC = parts[3];
                cp.put(parts[0], u);
            }
            else {
                replaceWithSpace.set(Integer.parseInt(parts[0], 16));
                udata u = new udata();
                //populate the category for the data wrapper
                u.cat = parts[2];
                u.CC = parts[3];
                cp.put(parts[0], u);
            }
        }
        else if(isExcluded(parts, dontExclude)) {
            //put the codepoint into the excluded list
            numEx++;
            excluded.set(Integer.parseInt(parts[0],16));
        }
        else { //not expluded
            //put this codepoint into the cp map
            udata u = new udata();
            //populate the category for the data wrapper
            u.cat = parts[2];
            u.CC = parts[3];
            cp.put(parts[0], u);
        }
        return true;
    }
    
    /**
     * check if this puctuation class should be excluded
     */
    private boolean excludedPClass(String codepoint) {
        //TODO: may need to add to this list of one
        //for all the different languages
        if (codepoint.equals("0027"))
            return true;
        return false;
    }

    /**
     * check to see if code point in the array p 
     * is excluded.  
     */
    private boolean isExcluded(String[] p, java.util.BitSet ex) {
        String cat = p[2];
        String cc = p[3];
        char first = cat.charAt(0);
        if(ex.get(Integer.parseInt(p[0].trim(), 16)))
            return false;
        else if(cat.equals("Lu") ||
                cat.equals("Ll") ||
                cat.equals("Lt") ||
                cat.equals("Lo") ||
                cat.equals("Lm") ||
                cat.equals("Nd") ||
                cat.equals("Mc") ||
                cat.equals("Cs") ||
                cat.equals("Co") ||
                cat.equals("Zs") ||
                cat.equals("So") ||
                first == 'P' 
                )
            return false;
        else if(cat.equals("Mn") && cc.equals("0")) {
            //don't exclude Mn category which has a combining class of 0
            return false;
        }
        else
            return true;
    }

    /**
     * read in the case folding file to find the correct
     * case mappings from uppercase to lowercase
     */
    private void readCaseFolding(Map c) 
        throws IOException {
        BufferedReader buf = getBR(DATA_DIR+CASE_FOLDING);
        String line, status;
        String[] splitUp;
        int index;
        
        while((line = buf.readLine()) != null) {
            if(line.length() > 0 &&
               line.charAt(0) != '#') {
                index = line.indexOf('#');
                line = line.substring(0,index).trim();
                splitUp = StringUtils.split(line, ";");
                status = splitUp[1].trim();
                //C - common case folding, F - full case folding
                if(status.equals("C") || status.equals("F")) {
                    //c.put(splitUp[0].trim(), splitUp[2].trim());
                    c.put(code2char(splitUp[0].trim()), code2char(splitUp[2].trim()));
                }
            }
        }
        
        buf.close();
    }

    /**
     * converts the hex representation of a String to a String
     * ie. 0020 -> " "
     *     0061 0062 -> "ab"
     * @param s String to convert
     * @return converted s
     */
    private String code2char(String s) {
        StringBuffer b = new StringBuffer();
        
        if(s.indexOf(" ") > -1) {
            String[] splitup = StringUtils.split(s, " ");
            for(int i = 0; i < splitup.length; i++) 
                b.append((char)Integer.parseInt(splitup[i], 16));
        }
        else
            b.append((char)Integer.parseInt(s, 16));
        
        return b.toString();
    }

    /**
     * reverse of code2char
     * converts from String to hex rep
     * ie. "ab" -> 0061 0062
     */
    private String char2code(String s) {
        if(s == null) return s;
        StringBuffer b = new StringBuffer();
        String temp;
        for(int i = 0, n = s.length(); i < n; i++) {
            temp = Integer.toString(s.charAt(i), 16);
            if(temp.length() < 4) {
                b.append("00");
                b.append(temp);
            }
            else
                b.append(temp);
            
            b.append(" ");
        }
        
        return b.toString().trim();
    }

    /**
     * run thru codepoints and replace case or replace with space 0020 if
     * necessary
     */
    private void replaceCase(Map codepoint, Map casF, java.util.BitSet ex) {
        //run thru and check the codepoint or deKomp for uppercase
        //this could probably done in the write out process?
        Iterator iter = codepoint.keySet().iterator();
        String code;
        String up;
        String[] splitUp;
        //final int CJKLow = Integer.parseInt("3400", 16);
        //final int CJKHigh = Integer.parseInt("9FA5", 16);
        while(iter.hasNext()) {
            code = (String)iter.next();
            udata u = (udata)codepoint.get(code);
            if(u.cat.indexOf("P") > -1 || u.cat.equals("Zs")) {
                //replace all punctuation with (ascii space)
                //and space (cat: Zs) with 0020 (ascii space)
                u.deKomp = "0020";
            }
            else {

                if(u.deKomp.equals("")) {
                    up = (String)casF.get(code2char(code));
                    if(up != null)
                        u.deKomp = char2code(up);
                }
                else {
                    StringBuffer dek = new StringBuffer();
                    splitUp = StringUtils.split(u.deKomp, " ");
                    //boolean removed = false;
                    for(int i = 0; i < splitUp.length; i++) {
                        //check if it should be removed...
                        int codeInt = Integer.parseInt(splitUp[i], 16);

                        if(!ex.get(codeInt)) {
                            up = char2code((String)casF.get(code2char(splitUp[i])));
                            if(up != null)
                                dek.append(up + " ");
                            else {
                                udata ud = (udata)codepoint.get(splitUp[i]);
                                String cat = 
                                    (ud == null)?"":ud.cat;
                                if(cat.indexOf("P") > -1)
                                    up = "0020";
                                else
                                    up = splitUp[i];
                                //dek.append(splitUp[i] + " ");
                                dek.append(up + " ");
                            }
                        }
                    }
                    u.deKomp = dek.toString().trim();
                }
                
            }
        }
    }

    /**
     * Reads through the NormalizationTest file provided by unicode.org
     * to get the full decomposition of codepoints.  UnicodeData file
     * doesn't provide the full decomposition but its used to get
     * the codepoints and combining classes... see NormalizationTest
     * file for more info.
     */
    private void readNTestPopKD(Map c, Map kc) 
        throws IOException {
        //c - codepoints that weren't excluded...
        BufferedReader buf = getBR(DATA_DIR+NORMALIZATION_TEST);

        String line;
        String[] parts;
        char first;
        boolean skip = false;

        //int hangulFirst = 0xAC00;
        //int hangulLast = 0xD7A3;

        while((line = buf.readLine()) != null) {
            first = line.charAt(0);
            if(first != '#') {
                if(first == '@') {
                    if(line.indexOf("Part2") > -1)
                        break;
                    else if(line.indexOf("Part0") > -1)
                        skip = true;
                    else
                        skip = false;
                }
                else {
                    if(!skip) {
                        line = line.substring(0, line.indexOf('#')).trim();
                        parts = StringUtils.split(line, ";");
                        udata u = (udata)c.get(parts[0].trim());

                        if(u != null) 
                            u.deKomp = parts[4].trim();
                        //create a KC mapping to be used to
                        //build final data... 
                        kc.put(parts[4].trim(), parts[3].trim());
                    }
                }
            }   
        }
        
        buf.close();
    }

    private BufferedReader getBR(String filename) 
        throws IOException {

        FileInputStream fi = 
            new FileInputStream(new File(filename));
        return new BufferedReader(new InputStreamReader(fi));

    }
    
    //just a datawrapper to be used during the building of the files
    private class udata {
        /**
         * Comment for <code>cat</code>
         */
        public String cat;
        /**
         * Comment for <code>CC</code>
         */
        public String CC;
        /**
         * Comment for <code>deKomp</code>
         */
        public String deKomp = "";
    }

}







