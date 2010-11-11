package org.limewire.util;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * Provides static methods to split, check for substrings, change case and
 * compare strings, along with additional string utility methods.
 */
public class StringUtils {

    /**
     * Collator used for internationalization.
     */
    private volatile static Collator COLLATOR;

    private static final ThreadLocal<CharsetEncoder> ASCII_ENCODER = new ThreadLocal<CharsetEncoder>() {
        @Override
        protected CharsetEncoder initialValue() {
            return Charset.forName("ISO-8859-1").newEncoder();
        }
    };

    static {
        COLLATOR = Collator.getInstance(Locale.getDefault());
        COLLATOR.setDecomposition(Collator.FULL_DECOMPOSITION);
        COLLATOR.setStrength(Collator.PRIMARY);
    }

    /** Updates the locale that string-matching will use. */
    public static void setLocale(Locale locale) {
        Collator later = Collator.getInstance(locale);
        later.setDecomposition(Collator.FULL_DECOMPOSITION);
        later.setStrength(Collator.PRIMARY);
        COLLATOR = later;
    }

    /**
     * Returns true if input contains the given pattern, which may contain the
     * wildcard character '*'. TODO: need more formal definition. Examples:
     * 
     * <pre>
     *  StringUtils.contains(&quot;&quot;, &quot;&quot;) ==&gt; true
     *  StringUtils.contains(&quot;abc&quot;, &quot;&quot;) ==&gt; true
     *  StringUtils.contains(&quot;abc&quot;, &quot;b&quot;) ==&gt; true
     *  StringUtils.contains(&quot;abc&quot;, &quot;d&quot;) ==&gt; false
     *  StringUtils.contains(&quot;abcd&quot;, &quot;a*d&quot;) ==&gt; true
     *  StringUtils.contains(&quot;abcd&quot;, &quot;*a**d*&quot;) ==&gt; true
     *  StringUtils.contains(&quot;abcd&quot;, &quot;d*a&quot;) ==&gt; false
     * </pre>
     */
    public static boolean contains(String input, String pattern) {
        return contains(input, pattern, false);
    }

    /**
     * Exactly like contains(input, pattern), but case is ignored if
     * ignoreCase==true.
     */
    public static boolean contains(String input, String pattern, boolean ignoreCase) {
        // More efficient algorithms are possible, e.g. a modified version of
        // the
        // Rabin-Karp algorithm, but they are unlikely to be faster with such
        // short strings. Also, some contant time factors could be shaved by
        // combining the second FOR loop below with the subset(..) call, but
        // that
        // just isn't important. The important thing is to avoid needless
        // allocations.

        final int n = pattern.length();
        // Where to resume searching after last wildcard, e.g., just past
        // the last match in input.
        int last = 0;
        // For each token in pattern starting at i...
        for (int i = 0; i < n;) {
            // 1. Find the smallest j>i s.t. pattern[j] is space, *, or +.
            char c = ' ';
            int j = i;
            for (; j < n; j++) {
                char c2 = pattern.charAt(j);
                if (c2 == ' ' || c2 == '+' || c2 == '*') {
                    c = c2;
                    break;
                }
            }

            // 2. Match pattern[i..j-1] against input[last...].
            int k = subset(pattern, i, j, input, last, ignoreCase);
            if (k < 0)
                return false;

            // 3. Reset the starting search index if got ' ' or '+'.
            // Otherwise increment past the match in input.
            if (c == ' ' || c == '+')
                last = 0;
            else if (c == '*')
                last = k + j - i;
            i = j + 1;
        }
        return true;
    }

    public static boolean containsCharacters(String input, char[] chars) {
        char[] inputChars = input.toCharArray();
        Arrays.sort(inputChars);
        for (char c : chars) {
            if (Arrays.binarySearch(inputChars, c) >= 0)
                return true;
        }
        return false;
    }

    /**
     * @requires TODO3: fill this in
     * @effects returns the the smallest i>=bigStart s.t.
     *          little[littleStart...littleStop-1] is a prefix of big[i...] or
     *          -1 if no such i exists. If ignoreCase==false, case doesn't
     *          matter when comparing characters.
     */
    private static int subset(String little, int littleStart, int littleStop, String big,
            int bigStart, boolean ignoreCase) {
        // Equivalent to
        // return big.indexOf(little.substring(littleStart, littleStop),
        // bigStart);
        // but without an allocation.
        // Note special case for ignoreCase below.

        if (ignoreCase) {
            final int n = big.length() - (littleStop - littleStart) + 1;
            outerLoop: for (int i = bigStart; i < n; i++) {
                // Check if little[littleStart...littleStop-1] matches with
                // shift i
                final int n2 = littleStop - littleStart;
                for (int j = 0; j < n2; j++) {
                    char c1 = big.charAt(i + j);
                    char c2 = little.charAt(littleStart + j);
                    if (c1 != c2 && c1 != toOtherCase(c2)) // Ignore case. See
                        // below.
                        continue outerLoop;
                }
                return i;
            }
            return -1;
        } else {
            final int n = big.length() - (littleStop - littleStart) + 1;
            outerLoop: for (int i = bigStart; i < n; i++) {
                final int n2 = littleStop - littleStart;
                for (int j = 0; j < n2; j++) {
                    char c1 = big.charAt(i + j);
                    char c2 = little.charAt(littleStart + j);
                    if (c1 != c2) // Consider case. See above.
                        continue outerLoop;
                }
                return i;
            }
            return -1;
        }
    }

    /**
     * If c is a lower case ASCII character, returns Character.toUpperCase(c).
     * Else if c is an upper case ASCII character, returns
     * Character.toLowerCase(c), Else returns c. Note that this is <b>not
     * internationalized</b>; but it is fast.
     */
    public static char toOtherCase(char c) {
        int i = c;
        final int A = 'A'; // 65
        final int Z = 'Z'; // 90
        final int a = 'a'; // 97
        final int z = 'z'; // 122
        final int SHIFT = a - A;

        if (i < A) // non alphabetic
            return c;
        else if (i <= Z) // upper-case
            return (char) (i + SHIFT);
        else if (i < a) // non alphabetic
            return c;
        else if (i <= z) // lower-case
            return (char) (i - SHIFT);
        else
            // non alphabetic
            return c;
    }

    /**
     * Exactly like split(s, Character.toString(delimiter)).
     */
    public static String[] split(String s, char delimiter) {
        return split(s, Character.toString(delimiter));
    }

    /**
     * Returns the tokens of s delimited by the given delimiter, without
     * returning the delimiter. Repeated sequences of delimiters are treated as
     * one. Examples:
     * 
     * <pre>
     *    split(&quot;a//b/ c /&quot;,&quot;/&quot;)=={&quot;a&quot;,&quot;b&quot;,&quot; c &quot;}
     *    split(&quot;a b&quot;, &quot;/&quot;)=={&quot;a b&quot;}.
     *    split(&quot;///&quot;, &quot;/&quot;)=={}.
     * </pre>
     * 
     * <b>Note:</b> whitespace is preserved if it is not part of the delimiter.
     * <p>
     * An older version of this trim()'ed each token of whitespace.
     */
    public static String[] split(String s, String delimiters) {
        // Tokenize s based on delimiters, adding to buffer.
        StringTokenizer tokenizer = new StringTokenizer(s, delimiters);
        List<String> tokens = new ArrayList<String>();
        while (tokenizer.hasMoreTokens())
            tokens.add(tokenizer.nextToken());

        return tokens.toArray(new String[tokens.size()]);
    }

    /**
     * Exactly like splitNoCoalesce(s, Character.toString(delimiter)).
     */
    public static String[] splitNoCoalesce(String s, char delimiter) {
        return splitNoCoalesce(s, Character.toString(delimiter));
    }

    /**
     * Similar to split(s, delimiters) except that subsequent delimiters are not
     * coalesced, so the returned array may contain empty strings. If s starts
     * (ends) with a delimiter, the returned array starts (ends) with an empty
     * strings. If s contains N delimiters, N+1 strings are always returned.
     * Examples:
     * 
     * <pre>
     *    split(&quot;a//b/ c /&quot;,&quot;/&quot;)=={&quot;a&quot;,&quot;&quot;,&quot;b&quot;,&quot; c &quot;, &quot;&quot;}
     *    split(&quot;a b&quot;, &quot;/&quot;)=={&quot;a b&quot;}.
     *    split(&quot;///&quot;, &quot;/&quot;)=={&quot;&quot;,&quot;&quot;,&quot;&quot;,&quot;&quot;}.
     * </pre>
     * 
     * @return an array A s.t. s.equals(A[0]+d0+A[1]+d1+...+A[N]), where for all
     *         dI, dI.size()==1 && delimiters.indexOf(dI)>=0; and for all c in
     *         A[i], delimiters.indexOf(c)<0
     */
    public static String[] splitNoCoalesce(String s, String delimiters) {
        // Tokenize s based on delimiters, adding to buffer.
        StringTokenizer tokenizer = new StringTokenizer(s, delimiters, true);
        List<String> tokens = new ArrayList<String>();
        // True if last token was a delimiter. Initialized to true to force
        // an empty string if s starts with a delimiter.
        boolean gotDelimiter = true;
        while (tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken();
            // Is token a delimiter?
            if (token.length() == 1 && delimiters.indexOf(token) >= 0) {
                // If so, add blank only if last token was a delimiter.
                if (gotDelimiter)
                    tokens.add("");
                gotDelimiter = true;
            } else {
                // If not, add "real" token.
                tokens.add(token);
                gotDelimiter = false;
            }
        }
        // Add trailing empty string UNLESS s is the empty string.
        if (gotDelimiter && !tokens.isEmpty())
            tokens.add("");

        return tokens.toArray(new String[tokens.size()]);
    }

    /**
     * This method will compare the two strings using full decomposition and
     * only look at primary differences The comparison will ignore case as well
     * as differences like FULLWIDTH vs HALFWIDTH.
     */
    public static int compareFullPrimary(String s1, String s2) {
        return COLLATOR.compare(s1, s2);
    }

    /**
     * Returns true iff <code>s</code> starts with prefix, ignoring case.
     * 
     * @return true iff s.toUpperCase().startsWith(prefix.toUpperCase())
     */
    public static boolean startsWithIgnoreCase(String s, String prefix) {
        final int pl = prefix.length();
        if (s.length() < pl)
            return false;
        for (int i = 0; i < pl; i++) {
            char sc = s.charAt(i);
            char pc = prefix.charAt(i);
            if (sc != pc) {
                sc = Character.toUpperCase(sc);
                pc = Character.toUpperCase(pc);
                if (sc != pc) {
                    sc = Character.toLowerCase(sc);
                    pc = Character.toLowerCase(pc);
                    if (sc != pc)
                        return false;
                }
            }
        }
        return true;
    }

    /**
     * Replaces all occurrences of old_str in str with new_str.
     * 
     * @param str the String to modify
     * @param old_str the String to be replaced
     * @param new_str the String to replace old_str with
     * 
     * @return the modified str.
     */
    public static String replace(String str, String old_str, String new_str) {
        int o = 0;
        StringBuilder buf = new StringBuilder();
        for (int i = str.indexOf(old_str); i > -1; i = str.indexOf(old_str, i + 1)) {
            if (i > o) {
                buf.append(str.substring(o, i));
            }
            buf.append(new_str);
            o = i + old_str.length();
        }
        buf.append(str.substring(o, str.length()));
        return buf.toString();
    }

    /**
     * Returns a truncated string, up to the maximum number of characters.
     */
    public static String truncate(final String string, final int maxLen) {
        if (string.length() <= maxLen)
            return string;
        else
            return string.substring(0, maxLen);
    }

    /**
     * Helper method to obtain the starting index of a substring within another
     * string, ignoring their case. This method is expensive because it has to
     * set each character of each string to lower case before doing the
     * comparison. Uses the default <code>Locale</code> for case conversion.
     * 
     * @param str the string in which to search for the <tt>substring</tt>
     *        argument
     * @param substring the substring to search for in <tt>str</tt>
     * @return if the <tt>substring</tt> argument occurs as a substring within
     *         <tt>str</tt>, then the index of the first character of the first
     *         such substring is returned; if it does not occur as a substring,
     *         -1 is returned
     */
    public static int indexOfIgnoreCase(String str, String substring) {
        return indexOfIgnoreCase(str, substring, Locale.getDefault());
    }

    /**
     * Helper method to obtain the starting index of a substring within another
     * string, ignoring their case. This method is expensive because it has to
     * set each character of each string to lower case before doing the
     * comparison.
     * 
     * @param str the string in which to search for the <tt>substring</tt>
     *        argument
     * @param substring the substring to search for in <tt>str</tt>
     * @param locale the <code>Locale</code> to use when converting the case of
     *        <code>str</code> and <code>substring</code>. This is necessary
     *        because case conversion is <code>Locale</code> specific.
     * @return if the <tt>substring</tt> argument occurs as a substring within
     *         <tt>str</tt>, then the index of the first character of the first
     *         such substring is returned; if it does not occur as a substring,
     *         -1 is returned
     */
    public static int indexOfIgnoreCase(String str, String substring, Locale locale) {
        // Look for the index after the expensive conversion to lower case.
        return str.toLowerCase(locale).indexOf(substring.toLowerCase(locale));
    }

    /**
     * Utility wrapper for getting a String object out of byte [] using the
     * ASCII encoding.
     */
    public static String getASCIIString(byte[] bytes) {
        return getEncodedString(bytes, "ISO-8859-1");
    }

    public static String getASCIIString(byte[] bytes, int offset, int length) {
        return new String(bytes, offset, length, Charset.forName("ISO-8859-1"));
    }

    /**
     * Utility wrapper for getting a String object out of byte [] using the
     * UTF-8 encoding.
     */
    public static String getUTF8String(byte[] bytes) {
        return getEncodedString(bytes, "UTF-8");
    }

    public static String getUTF8String(byte[] bytes, int offset, int length) {
        return new String(bytes, offset, length, Charset.forName("UTF-8"));
    }

    /**
     * @return a string with an encoding we know we support.
     */
    private static String getEncodedString(byte[] bytes, String encoding) {
        try {
            return new String(bytes, encoding);
        } catch (UnsupportedEncodingException impossible) {
            throw new RuntimeException(impossible);
        }
    }

    /**
     * Returns the tokens of array concatenated to a delimited by the given
     * delimiter. Examples:
     * 
     * <pre>
     *     explode({ &quot;a&quot;, &quot;b&quot; }, &quot; &quot;) == &quot;a b&quot;
     *     explode({ &quot;a&quot;, &quot;b&quot; }, &quot;&quot;) == &quot;ab&quot;
     * </pre>
     */
    public static String explode(Object[] array, String delimeter) {
        StringBuilder sb = new StringBuilder();
        if (array.length > 0) {
            sb.append(array[0]);
            for (int i = 1; i < array.length; i++) {
                sb.append(delimeter);
                sb.append(array[i]);
            }
        }
        return sb.toString();
    }

    /**
     * Concatenates/joins the elements of <code>iteratble</code> together,
     * separated by <code>delimiter</code>
     * 
     * <pre>
     *     explode({ &quot;a&quot;, &quot;b&quot; }, &quot; &quot;) == &quot;a b&quot;
     *     explode({ &quot;a&quot;, &quot;b&quot; }, &quot;&quot;) == &quot;ab&quot;
     * </pre>
     * 
     * @return "" if iterable doesn't have elements
     */
    public static <T> String explode(Iterable<T> iterable, String delimiter) {
        return explode(iterable, delimiter, Integer.MAX_VALUE, Integer.MAX_VALUE, "");      
    }
    
    /**
     * Concatenates/joins the elements of <code>iteratble</code> together,
     * separated by <code>delimiter</code>
     * 
     * @param <T>
     * @param iterable the list of items to join
     * @param delimiter the sequence to put between elements
     * @param maxRows the maximum number of elements to explode
     * @param maxCols the maximum number of characters in each element
     * @param moreRowsMsg the message to display if elements have been skipped
     * @return
     */
    public static <T> String explode(Iterable<T> iterable, String delimiter, int maxRows, int maxCols, String moreRowsMsg) {
        Iterator<T> iterator = iterable.iterator();
        if (!iterator.hasNext()) {
            return "";
        }
        
        int rowCount = 0;
        
        StringBuilder builder = new StringBuilder();
        while (iterator.hasNext()) {
            if (rowCount != 0) {
                builder.append(delimiter);
            }
            
            if (++rowCount > maxRows) {
                builder.append(moreRowsMsg);
                break;
            } else {
                String nextLine = String.valueOf(iterator.next());
                int length = Math.min(nextLine.length(), maxCols);
                builder.append(nextLine.substring(0, length));
                if (length == maxCols) {
                    builder.append("...");
                }
            }
        }
        return builder.toString();
    }
    
    /**
     * Concatenates the string representation of <code>object</code> 
     * <code>times</code> times together, separating it with <code>delimiter</code>.
     * 
     * @throws AssertionError whent times is smaller than 1
     */
    public static <T> String explode(T object, String delimiter, int times) {
        assert times >= 1;
        if (times == 1) {
            return String.valueOf(object);
        }
        StringBuilder builder = new StringBuilder();
        builder.append(object);
        for (int i = 1; i < times; i++) {
            builder.append(delimiter);
            builder.append(object);
        }
        return builder.toString();
    }

    /**
     * A wrapped version of {@link String#getBytes(String)} that changes the
     * unlikely encoding exception into a runtime exception. Returns empty array
     * if the passed in string is null.
     */
    public static byte[] toUTF8Bytes(String string) {
        if (string == null)
            return new byte[0];
        try {
            return string.getBytes("UTF-8");
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException("UTF-8 not supported?", ex);
        }
    }

    public static byte[] toAsciiBytes(String string) {
        if (string == null)
            return new byte[0];
        try {
            return string.getBytes("US-ASCII");
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException("US-ASCII not supported?", ex);
        }
    }

    /**
     * A wrapped version of {@link String#String(byte[], String)} that changes
     * the unlikely encoding exception into a runtime exception. Returns null if
     * the passed in array is null.
     */
    public static String toUTF8String(byte[] bytes) {
        if (bytes == null)
            return null;
        try {
            return new String(bytes, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException("UTF-8 not supported?", ex);
        }
    }

    private static ThreadLocal<IdentityHashMap<Object, Object>> threadLocal = new ThreadLocal<IdentityHashMap<Object, Object>>();

    /**
     * Creates a string representation of the object <code>thiz</code>.
     * <p>
     * Can optionally be given a whitelist of fields that should be part of the
     * string output.
     * <p>
     * Note: Should synchronize calling method if the fields of the instance can
     * be modified by other threads.
     * <p>
     * Note: Creates a temporary copy of arrays of primitive elements.
     * <p>
     * Calls {@link Object#toString()} on fields.
     */
    public static String toString(Object thiz, Object... whitelist) {
        return toStringBlackAndWhite(thiz, Arrays.asList(whitelist), Collections.emptyList());
    }

    /**
     * Creates a string representation of the object <code>thiz</code>.
     * <p>
     * Can optionally be given a blacklist of fields that should not be part of
     * the string output.
     * <p>
     * Note: Should synchronize calling method if the fields of the instance can
     * be modified by other threads.
     * <p>
     * Note: Creates a temporary copy of arrays of primitive elements.
     * <p>
     * Calls {@link Object#toString()} on fields.
     */
    public static String toStringBlacklist(Object thiz, Object... blacklist) {
        return toStringBlackAndWhite(thiz, Collections.emptyList(), Arrays.asList(blacklist));
    }

    /**
     * Creates a string representation of the object <code>thiz</code>.
     * <p>
     * Can optionally be given a blacklist and whitelist of fields that should
     * not be part of the string output.
     * <p>
     * Note: Should synchronize calling method if the fields of the instance can
     * be modified by other threads.
     * <p>
     * Note: Creates a temporary copy of arrays of primitive elements.
     * <p>
     * Calls {@link Object#toString()} on fields.
     */
    private static String toStringBlackAndWhite(Object thiz,
            Collection<? extends Object> whitelist, Collection<? extends Object> blacklist) {
        boolean cleanUp = false;
        try {
            IdentityHashMap<Object, Object> handledObjects = threadLocal.get();
            if (handledObjects == null) {
                cleanUp = true;
                handledObjects = new IdentityHashMap<Object, Object>();
                threadLocal.set(handledObjects);
            }
            if (handledObjects.containsKey(thiz)) {
                return "circular structure";
            }
            handledObjects.put(thiz, thiz);
            Map<String, String> fields = new LinkedHashMap<String, String>();
            for (Field field : thiz.getClass().getDeclaredFields()) {
                try {
                    boolean accessible = field.isAccessible();
                    field.setAccessible(true);
                    Object value = field.get(thiz);
                    field.setAccessible(accessible);
                    if (!Modifier.isStatic(field.getModifiers()) && !blacklist.contains(value)
                            && (whitelist.isEmpty() || whitelist.contains(value))) {
                        if (value == null) {
                            fields.put(field.getName(), String.valueOf(value));
                        } else {
                            Class clazz = value.getClass();
                            if (clazz.isArray()) {
                                if (!clazz.getComponentType().isPrimitive()) {
                                    fields.put(field.getName(), String.valueOf(Arrays
                                            .asList((Object[]) value)));
                                } else {
                                    int length = Array.getLength(value);
                                    List<Object> copy = new ArrayList<Object>(length);
                                    for (int i = 0; i < length; i++) {
                                        copy.add(Array.get(value, i));
                                    }
                                    fields.put(field.getName(), String.valueOf(copy));
                                }
                            } else {
                                fields.put(field.getName(), String.valueOf(value));
                            }
                        }
                    }
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
            return thiz.getClass().getSimpleName() + " " + fields.toString();
        } finally {
            if (cleanUp) {
                threadLocal.set(null);
            }
        }
    }

    /**
     * Returns true if the given string is null or its trimmed representation
     * is empty.
     */
    public static boolean isEmpty(String s) {
        if (s == null || s.length() == 0) {
            return true;
        }
        int length = s.length();
        for (int i = 0; i < length; i ++) {
            if (s.charAt(i) != ' ') {
                return false;
            }
        }
        return true;
    }

    /**
     * @return true if <code>sequence</code> can be encoded as ASCII only
     */
    public static boolean isAsciiOnly(CharSequence sequence) {
        return ASCII_ENCODER.get().canEncode(sequence);
    }

    /**
     * @return the number of occurrences of <code>c</code> in
     *         <code>sequence</code>
     */
    public static int countOccurrences(CharSequence sequence, char c) {
        int count = 0;
        for (int i = 0; i < sequence.length(); i++) {
            if (sequence.charAt(i) == c) {
                ++count;
            }
        }
        return count;
    }
    
    /**
     * Returns a hexString representation of the given byteArray.
     */
    public static String toHexString(byte[] block) {
        StringBuffer hexString = new StringBuffer(block.length * 2);
        char[] hexChars = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd',
                'e', 'f' };

        int high = 0;
        int low = 0;
        for (byte b : block) {
            high = ((b & 0xf0) >> 4);
            low = b & 0x0f;
            hexString.append(hexChars[high]);
            hexString.append(hexChars[low]);
        }

        return hexString.toString();
    }

    /**
     * Returns a byte array from the given hexString.
     * Assume string is a proper hexString.
     */
    public static byte[] fromHexString(String hexString) {
        byte[] bytes = new byte[hexString.length() / 2];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) Integer.parseInt(hexString.substring(2 * i, 2 * i + 2), 16);
        }
        return bytes;
    }
}
