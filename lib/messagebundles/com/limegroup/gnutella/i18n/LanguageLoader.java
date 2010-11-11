package com.limegroup.gnutella.i18n;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;

@SuppressWarnings("unchecked")
class LanguageLoader {
    /** @see LanguageInfo#getLink() */
    static final String BUNDLE_NAME = "MessagesBundle"; //$NON-NLS-1$
    /** @see LanguageInfo#getLink() */
    static final String PROPS_EXT = ".properties"; //$NON-NLS-1$
    /** @see LanguageInfo#getLink() */
    static final String UTF8_EXT = ".UTF-8.txt"; //$NON-NLS-1$
    private final Map/* <String, LanguageInfo> */langs;
    private final File lib;

    /**
     * @param directory
     */
    LanguageLoader(File directory) {
        this.langs = new TreeMap/* <String, LanguageInfo> */();
        this.lib = directory;
    }

    /**
     * List and load all available bundles and map them into the languages map.
     * Note that resources are not expanded here per base language, and not
     * cleaned here from extra keys (needed to support the resources "check"
     * option).
     * 
     * @return the languages map (from complete locale codes to LocaleInfo)
     */
    Map<String, LanguageInfo> loadLanguages() {
        if (!this.lib.isDirectory())
            throw new IllegalArgumentException("invalid lib: " + this.lib);
        final String[] files = this.lib.list();
        for (int i = 0; i < files.length; i++) {
            if (!files[i].startsWith(BUNDLE_NAME + '_')
                    || !files[i].endsWith(PROPS_EXT)
                    || files[i].startsWith(BUNDLE_NAME + "_en")) //$NON-NLS-1$
                continue;
            /* See if a .UTF-8.txt file exists; if so, use that as the link. */
            String linkFileName = files[i];
            int idxProperties = linkFileName.indexOf(PROPS_EXT);
            final File utf8 = new File(this.lib, linkFileName.substring(0,
                    idxProperties)
                    + UTF8_EXT);
            boolean skipUTF8LeadingBOM = false;
            if (utf8.exists()) {
                /*
                 * properties files are normally read as streams of ISO-8859-1
                 * bytes but we want to check the UTF-8 source file. The non
                 * ASCII characters in key values will be read as sequences of
                 * Extended Latin 1 characters instead of the actual Unicode
                 * character coded as Unicode escapes in the .properties file.
                 * So they won't have the actual run-time value; however it
                 * allows easier checking and validation here for messages
                 * printed on the Console, that will output ISO-8859-1; the
                 * result output still be interpretable as Unicode UTF-8.
                 */
                linkFileName = utf8.getName();
                skipUTF8LeadingBOM = true;
            }
            try {
                final File toRead = new File(this.lib, linkFileName);
                final InputStream in = new FileInputStream(toRead);
                // skip the three-bytes leading BOM
                if (skipUTF8LeadingBOM)
                    try {
                        /*
                         * the leading BOM (U+FEFF), if present, is coded in
                         * UTF-8 as three bytes 0xEF, 0xBB, 0xBF; they are not
                         * part of a resource key.
                         */
                        in.mark(3);
                        if (in.read() != 0xEF || in.read() != 0xBB
                                || in.read() != 0xBF)
                            in.reset();
                    } catch (java.io.IOException ioe) {/* ignored */}
                loadFile(this.langs, in, linkFileName, files[i],
                        skipUTF8LeadingBOM, toRead);
            } catch (FileNotFoundException fnfe) {
                fnfe.printStackTrace();
            }
        }
        return this.langs;
    }

    /**
     * Constructs a list of each line in the default English properties file.
     * 
     * @return a list of Line instances
     * @throws IOException
     * @see Line
     */
    List<Line> getEnglishLines() throws IOException {
        final BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(new File(this.lib,
                        BUNDLE_NAME + PROPS_EXT)), "ISO-8859-1")); //$NON-NLS-1$
        final List/* <Line> */lines = new LinkedList/* <Line> */();
        String read;
        while ((read = reader.readLine()) != null)
            lines.add(new Line(read));
        return lines;
    }

    /**
     * Scans the file for translations that mistakenly still have a #? sign
     * before them and adds them into the properties. This assumes the file is
     * ISO-8859-1 encoded, just like Properties.load. If the file is UTF8
     * encoded, you will have to manually convert the resulting properties to
     * UTF8.
     * 
     * @param file
     * @param props
     * @throws IOException
     */
    private void scanForCommentedTranslations(File file, Properties props)
            throws IOException {
        InputStream in = new BufferedInputStream(new FileInputStream(file));
        in.mark(3);
        if (in.read() != 0xEF || in.read() != 0xBB || in.read() != 0xBF)
            in.reset();
        BufferedReader reader = new BufferedReader(new InputStreamReader(in,
                "ISO-8859-1")); //$NON-NLS-1$
        String read;
        while ((read = reader.readLine()) != null) {
            Line line = new Line(read);
            if (line.hadExtraComment())
                props.put(line.getKey(), line.getValue());
        }
        reader.close();
    }

    /**
     * Retrieves the default properties.
     * 
     * @return the loaded Properties
     * @throws IOException
     */
    Properties getDefaultProperties() throws java.io.IOException {
        Properties p = new Properties();
        InputStream in = new FileInputStream(new File(this.lib, BUNDLE_NAME
                + PROPS_EXT));
        p.load(in);
        in.close();
        return p;
    }

    /**
     * Retrieves the advanced keys.
     * 
     * @return a the Set of Strings for the key names of advanced properties.
     * @throws IOException
     */
    Set getAdvancedKeys() throws java.io.IOException {
        final BufferedReader reader;
        reader = new BufferedReader(new InputStreamReader(new FileInputStream(
                new File(this.lib, BUNDLE_NAME + PROPS_EXT)), "ISO-8859-1")); //$NON-NLS-1$
        String read;
        while ((read = reader.readLine()) != null)
            if (read
                    .startsWith("## TRANSLATION OF ALL ADVANCED RESOURCE STRINGS AFTER THIS LIMIT IS OPTIONAL")) //$NON-NLS-1$
                break;
        final StringBuffer sb = new StringBuffer();
        while ((read = reader.readLine()) != null) {
            if (read.length() == 0 || read.charAt(0) == '#')
                continue;
            sb.append(read).append("\n"); //$NON-NLS-1$
        }
        InputStream in = new ByteArrayInputStream(sb.toString().getBytes(
                "ISO-8859-1")); //$NON-NLS-1$
        Properties p = new Properties();
        p.load(in);
        in.close();
        reader.close();
        return p.keySet();
    }

    /**
     * Extend variant resources from *already loaded* base languages.
     */
    void extendVariantLanguages() {
        /* Extends missing resources with those from the base language */
        for (final Iterator/* <Map.Entry<String, LanguageInfo>> */i = this.langs
                .entrySet().iterator(); i.hasNext();) {
            final Map.Entry entry = (Map.Entry)i.next();
            // final String code = (String)entry.getKey();
            final LanguageInfo li = (LanguageInfo)entry.getValue();
            final Properties props = li.getProperties();
            if (li.isVariant()) {
                final LanguageInfo liBase = (LanguageInfo)this.langs.get(li
                        .getBaseCode());
                if (liBase != null) {
                    /* Get a copy of base properties */
                    final Properties propsBase = new Properties();
                    propsBase.putAll(liBase.getProperties());
                    /* Remove properties already defined in the current locale */
                    propsBase.keySet().removeAll(props.keySet());
                    /* Add the remaining base properties to the current locale */
                    props.putAll(propsBase);
                }
            }
        }
    }

    /**
     * Iterates through all languages and retains only those within 'keys'.
     * 
     * @param keys
     *            a Set of String for key names to retain in properties.
     */
    void retainKeys(Set keys) {
        /* Extends missing resources with those from the base language */
        for (final Iterator/* <Map.Entry<String, LanguageInfo>> */i = this.langs
                .entrySet().iterator(); i.hasNext();) {
            final Map.Entry/* <String, LanguageInfo> */entry = (Map.Entry)i
                    .next();
            // final String code = (String)entry.getKey();
            final LanguageInfo li = (LanguageInfo)entry.getValue();
            final Properties props = li.getProperties();
            props.keySet().retainAll(keys);
        }
    }

    /**
     * Iterates through the properties and removes all entries that have empty
     * values.
     * 
     * @param props
     */
    private void removeEmptyProperties(Properties props) {
        for (Iterator/* <Map.Entry<String, String>> */i = props.entrySet()
                .iterator(); i.hasNext();) {
            final Map.Entry entry = (Map.Entry)i.next();
            if ("".equals(entry.getValue())) {//$NON-NLS-1$
                final String key = (String)entry.getKey();
                // exceptions for special keys to keep despite an empty value
                if (!"LOCALE_COUNTRY_CODE".equals(key)
                        && !"LOCALE_VARIANT_CODE".equals(key))
                    i.remove();
            }
        }
    }

    /**
     * Loads a single file into the languages map.
     * 
     * @param newlangs
     * @param is
     * @param filename
     * @param baseFileName
     * @param isUTF8
     * @param toRead
     * @return
     */
    private LanguageInfo loadFile(final Map newlangs, final InputStream is,
            final String filename, final String baseFileName,
            final boolean isUTF8, final File toRead) {
        try {
            final BufferedInputStream in = new BufferedInputStream(is);
            final Properties props = new Properties();
            props.load(in);
            scanForCommentedTranslations(toRead, props);
            /* no more needed, as we already check "#? key=" lines without value */
            removeEmptyProperties(props);
            /*
             * note that the file is read in ISO-8859-1 only, even if it is
             * encoded with another charset. However, the Properties has its
             * unique legacy parser and we want to use it to make sure we use
             * the same syntax. So we'll need to correct the parsed values after
             * the file is read and interpreted as a set of properties
             * (keys,values).
             */
            if (isUTF8) {
                // actually the file was UTF-8-encoded: convert bytes read
                // incorrectly as
                // ISO-8859-1 characters, into actual Unicode UTF-16 code units.
                for (Iterator i = props.entrySet().iterator(); i.hasNext();) {
                    final Map.Entry entry = (Map.Entry)i.next();
                    final String key = (String)entry.getKey();
                    final String value = (String)entry.getValue();
                    byte[] bytes = null;
                    try {
                        bytes = value.getBytes("ISO-8859-1"); //$NON-NLS-1$
                    } catch (java.io.IOException ioe) {
                        ioe.printStackTrace();
                    }
                    try {
                        final String correctedValue = new String(bytes, "UTF-8"); //$NON-NLS-1$
                        if (!correctedValue.equals(value))
                            props.put(key, correctedValue);
                    } catch (java.io.IOException ioe) {
                        // may occur if .UTF-8.txt file was incorrectly encoded.
                        ioe.printStackTrace();
                    }
                }
            }
            String lc = props.getProperty("LOCALE_LANGUAGE_CODE", ""); //$NON-NLS-1$//$NON-NLS-2$
            String cc = props.getProperty("LOCALE_COUNTRY_CODE", ""); //$NON-NLS-1$//$NON-NLS-2$
            String vc = props.getProperty("LOCALE_VARIANT_CODE", ""); //$NON-NLS-1$//$NON-NLS-2$
            String sc = props.getProperty("LOCALE_SCRIPT_CODE", ""); //$NON-NLS-1$//$NON-NLS-2$
            String ln = props.getProperty("LOCALE_LANGUAGE_NAME", lc); //$NON-NLS-1$
            String cn = props.getProperty("LOCALE_COUNTRY_NAME", cc); //$NON-NLS-1$
            String vn = props.getProperty("LOCALE_VARIANT_NAME", vc); //$NON-NLS-1$
            String sn = props.getProperty("LOCALE_SCRIPT_NAME", sc); //$NON-NLS-1$
            String dn = props.getProperty("LOCALE_ENGLISH_LANGUAGE_NAME", ln); //$NON-NLS-1$
            String nsisName = props.getProperty("LOCALE_NSIS_NAME", //$NON-NLS-1$
                    ""); //$NON-NLS-1$
            boolean rtl = props.getProperty("LAYOUT_RIGHT_TO_LEFT", //$NON-NLS-1$
                    "false").equals("true"); //$NON-NLS-1$//$NON-NLS-2$
            LanguageInfo li = new LanguageInfo(lc, cc, vc, sc, ln, cn, vn, sn,
                    dn, nsisName, rtl, filename, props, baseFileName);
            newlangs.put(li.getCode(), li);
            return li;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (is != null)
                try {
                    is.close();
                } catch (IOException ioe) {}
        }
        return null;
    }
}
