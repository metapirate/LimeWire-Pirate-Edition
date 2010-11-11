package com.limegroup.gnutella.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import org.limewire.core.settings.SearchSettings;
import org.limewire.util.I18NConvert;
import org.limewire.util.StringUtils;

public class QueryUtils {
    
    /**
     * Trivial words that are not considered keywords.
     */
    private static final List<String> TRIVIAL_WORDS;
    
    /**
     * Characters used to tokenize queries and file names.
     */
    public static final String DELIMITERS = " -._+/*()\\,";
    
    private static final char[] DELIMITERS_CHARACTERS;

    /**
     * default set of delimiter characters AND illegal characters
     */
    private static final String DELIMITERS_AND_ILLEGAL;
    
    static {
        // must be lower-case
        TRIVIAL_WORDS = Arrays.asList("the", "an", "a", "and");
        char[] characters = DELIMITERS.toCharArray();
        Arrays.sort(characters);
        DELIMITERS_CHARACTERS = characters;
        char[] illegal = SearchSettings.ILLEGAL_CHARS.get();
        StringBuilder sb = new StringBuilder(DELIMITERS.length() + illegal.length);
        DELIMITERS_AND_ILLEGAL = sb.append(illegal).append(DELIMITERS).toString();
    }
    

    /**
     * Extracts keywords from the given string, separated by delimiters and
     * illegal characters, and returns them in a new set.
     *
     * @param str the string to extract keywords from
     * @param allowNumbers whether numbers are treated as keywords
     * @return a new set containing the keywords
     */
    public static final Set<String> extractKeywords(String str,
            boolean allowNumbers) {
        Set<String> set = new LinkedHashSet<String>();
        extractKeywords(str, allowNumbers, set);
        return set;
    }

    /**
     * Extracts keywords from the given string, separated by delimiters and
     * illegal characters, and adds them to the supplied set.
     *
     * @param str the string to extract keywords from
     * @param allowNumbers whether numbers are treated as keywords
     * @param set the set to which the keywords should be added
     */
    public static final void extractKeywords(String str, boolean allowNumbers,
            Set<String> set) {        
        StringTokenizer st = new StringTokenizer(str, DELIMITERS_AND_ILLEGAL);
        while(st.hasMoreTokens()) {
            String currToken = st.nextToken().toLowerCase();
            if(!allowNumbers) {
                try {                
                    Double.parseDouble(currToken); // NFE if not a number
                    continue;
                } catch(NumberFormatException normalWord) {}
            }
            if(!TRIVIAL_WORDS.contains(currToken))
                set.add(currToken);
        }
    }

    /**
     * Convenience wrapper for 
     * {@link #extractKeywords(String, boolean) keywords(String, false)}.
     * @param fileName
     * @return
     */
    static final Set<String> extractKeywordsFromFileName(String fileName) {
    	return extractKeywords(ripExtension(fileName), false);
    }
    
    /**
     * Removes illegal characters and delimiters from a string, inserting
     * spaces instead.
     */
    public static final String removeIllegalChars(String str) {
        StringBuilder sb = new StringBuilder(str.length());
        StringTokenizer st = new StringTokenizer(str, DELIMITERS_AND_ILLEGAL);        
        while(st.hasMoreTokens()) {
            if(sb.length() > 0)
                sb.append(' ');
            sb.append(st.nextToken());
        }
        return sb.toString();
    }
    
    /**
     * Strips an extension off of a file's filename.
     */
    public static String ripExtension(String fileName) {
        int extStart = fileName.lastIndexOf('.');
        if (extStart == -1)
            return fileName;
        else
            return fileName.substring(0, extStart);
    }

    /**
     * 
     * Returns a string to be used for querying from the given name.
     *
     * @param name
     * @param allowNumbers whether numbers in the argument should be kept in
     * the result
     * @return
     */
    public static String createQueryString(String name, boolean allowNumbers) {
        if(name == null)
            throw new NullPointerException("null name");
        
        String retString = null;
        name = I18NConvert.instance().getNorm(name);
        int maxLen = SearchSettings.MAX_QUERY_LENGTH.getValue();
    
        //Get the set of keywords within the name.
        Set<String> keywords = extractKeywords(ripExtension(name), allowNumbers);
    
        if (keywords.isEmpty()) { // no suitable non-number words
            retString = removeIllegalChars(name);
            retString = StringUtils.truncate(retString, maxLen);
        } else {
            retString = constructQueryStringFromKeywords(maxLen, keywords);
        }

        // Added a bunch of asserts to catch bugs.  There is some form of
        // input we are not considering in our algorithms....
        assert retString.length() <= maxLen : "Original filename: " + name + ", converted: " + retString;
        if(!keywords.isEmpty())
            assert !retString.equals("") : "Original filename: " + name;
    
        return retString;
    }

    /**
     * Constructs a space(" ") delimited query string that
     * must be <= maxLen from a set of keywords.
     *
     * @param maxLen
     * @param keywords set of keywords from which to generate the query string
     * @return
     */
    public static String constructQueryStringFromKeywords(int maxLen, Set<String> keywords) {
        // adding keywords that fit when appended to query string field, skipping keywords that do not fit.
        StringBuilder queryFieldValue = new StringBuilder();
        for (String keyword : keywords) {
            String delimIncl = (queryFieldValue.length() == 0) ? "" : " ";

            if ((queryFieldValue.length() + keyword.length() + delimIncl.length())
                    <= maxLen) {
                queryFieldValue.append(delimIncl);
                queryFieldValue.append(keyword);
            }
        }

        // in case the query string field is blank
        // All keywords are longer than queryField_LIMIT,
        // query string field would use maxLen chars of 1st keyword
        if (queryFieldValue.length() == 0) {
            queryFieldValue.append(StringUtils.truncate(keywords.iterator().next(), maxLen));
        }
        return queryFieldValue.toString();
    }

    /**
     * Convenience wrapper for 
     * {@link #createQueryString(String, boolean) createQueryString(String, false)}.
     * @param name
     * @return
     */
    public static String createQueryString(String name) {
    	return createQueryString(name, false);
    }

    public static final boolean isDelimiter(char c) {
        return Arrays.binarySearch(DELIMITERS_CHARACTERS, c) >= 0;
    }
    
    /**
     * Returns true if the filename contains all the words in the query. The
     * system locale is used for converting case, so it's possible for this
     * method to return false for filenames that would match under other
     * locales.
     */
    public static boolean filenameMatchesQuery(String filename, String query) {
        if(query.isEmpty())
            return true;
        filename = filename.toLowerCase();
        query = query.toLowerCase();
        for(String queryWord : extractKeywords(query, false)) {
            if(!filename.contains(queryWord))
                return false;
        }
        return true;
    }
    
    /**
     * Mutates a query string by shuffling the words and removing trivial words.
     * The returned string may or may not differ from the argument.
     */
    public static String mutateQuery(String query) {
        ArrayList<String> words =
            new ArrayList<String>(extractKeywords(query, true));
        if(words.size() <= 1)
            return query;
        Collections.shuffle(words);
        return StringUtils.explode(words, " ");
    }
}
