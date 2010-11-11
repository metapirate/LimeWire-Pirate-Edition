package com.limegroup.gnutella.i18n;

import java.util.Properties;

/**
 * Struct-like container for language information.
 */
class LanguageInfo implements Comparable {
    private final String languageCode;
    private final String countryCode;
    private final String variantCode;
    private final String scriptCode;
    private final String languageName;
    private final String countryName;
    private final String variantName;
    private final String scriptName;
    private boolean isRightToLeft;
    private final String displayName;
    private final String fileName;
    private final Properties properties;
    private double percentage;
    private final String alternateFileName;
    private final String nsisName;
    private static final String CHARSET_UTF8 = "UTF-8";
    private static final String CHARSET_8859_1 = "ISO-8859-1";
    private final String sourceCharset;

    /**
     * Constructs a new LanguageInfo object with the given languageCode,
     * countryCode, variantCode, languageName, countryName, and variantName.
     * 
     * @param lc
     *            ISO-639 language code
     * @param cc
     *            ISO-3166 country or region code
     * @param vc
     *            locale variant code
     * @param sc
     *            ISO-10644 script code
     * @param ln
     *            localized language name
     * @param cn
     *            localized country name
     * @param vn
     *            localized variant name
     * @param sn
     *            English script name
     * @param dn
     *            English language name
     * @param rtl
     *            true if locale layout is right-to-left
     * @param fn
     *            name of localized source (ISO-8859-1 or UTF-8) file
     * @param props
     *            properties loaded from localized file
     * @param alternateFile
     *            name of generated ISO-8859-1 file
     */
    public LanguageInfo(String lc, String cc, String vc, String sc, String ln,
            String cn, String vn, String sn, String dn, String nsName,
            boolean rtl, String fn, Properties props, String alternateFile) {
        languageCode = lc.trim();
        countryCode = cc.trim();
        variantCode = vc.trim();
        scriptCode = sc.trim();
        languageName = ln.trim();
        countryName = cn.trim();
        variantName = vn.trim();
        scriptName = sn.trim();
        isRightToLeft = rtl;
        displayName = dn.trim();
        fileName = fn.trim();
        nsisName = nsName.trim();
        properties = props;
        alternateFileName = alternateFile.trim();
        sourceCharset = alternateFileName.equals(fileName) ? CHARSET_8859_1
                : CHARSET_UTF8;
    }

    /**
     * Used to map the list of locales codes to their LanguageInfo data and
     * props. Must be unique per loaded localized properties file.
     * 
     * @see Comparable#compareTo(Object)
     */
    public int compareTo(Object other) {
        final LanguageInfo o = (LanguageInfo)other;
        int comp = languageCode.compareTo(o.languageCode);
        if (comp != 0)
            return comp;
        comp = countryCode.compareTo(o.countryCode);
        if (comp != 0)
            return comp;
        return variantCode.compareTo(o.variantCode);
    }

    /**
     * @return true if getCode()==getBaseCode()
     */
    public boolean isVariant() {
        return !"".equals(variantCode) || !"".equals(countryCode);
    }

    /**
     * @return the reduced Java locale code with format "lc"
     */
    public String getBaseCode() {
        return languageCode;
    }

    /**
     * @return the standard Java locale code with format: "lc[_CC[_vc]]"
     */
    public String getCode() {
        if (!variantCode.equals(""))
            return languageCode + "_" + countryCode + "_" + variantCode;
        if (!countryCode.equals(""))
            return languageCode + "_" + countryCode;
        return languageCode;
    }

    /**
     * @param percentage
     *            a completion level value to associate with this.
     */
    public void setPercentage(double percentage) {
        this.percentage = percentage;
    }

    /**
     * @return the percentage previously stored.
     * @see #setPercentage(double)
     */
    public double getPercentage() {
        return percentage;
    }

    /**
     * @return the properties loaded from the source file
     */
    public Properties getProperties() {
        return properties;
    }

    /**
     * Returns a native description of this language. If the variantName is not
     * 'international' or '', then the display is: languageName, variantName
     * (countryName) Otherwise, the display is: languageName (countryName) If
     * the language is Right-To-Left, the whole string is returned surrounded by
     * BiDi embedding controls.
     * 
     * @return the internationalized description string.
     * @see #getName()
     */
    public String toString() {
        final String bidi1, bidi2;
        if (isRightToLeft) {
            bidi1 = "\u202b"; /* RLE control: Right-To-Left Embedding */
            bidi2 = "\u202c"; /* PDF control: Pop Directional Format */
        } else {
            bidi1 = "";
            bidi2 = "";
        }
        if (variantName != null
                && !variantName.toLowerCase().equals("international")
                && !variantName.equals(""))
            return bidi1 + languageName + ", " + variantName + " ("
                    + countryName + ')' + bidi2;
        return bidi1 + languageName + " (" + countryName + ')' + bidi2;
    }

    /**
     * @return the English script name
     */
    public String getScript() {
        return scriptName;
    }

    /**
     * @return the ISO script code
     */
    public String getScriptCode() {
        return scriptCode;
    }

    /**
     * @return the source file name
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * @return the name in the NSIS installer.
     */
    public String getNSISName() {
        return nsisName;
    }

    /**
     * @return the source charset name
     */
    public String getSourceCharset() {
        return sourceCharset;
    }

    /**
     * @return true if the source charset is UTF-8, and there's no alternate
     *         file.
     */
    public boolean isUTF8() {
        return sourceCharset.equals(CHARSET_UTF8);
    }

    /**
     * @return the alternate file name, when the source file is not UTF-8
     *         encoded.
     */
    public String getAlternateFileName() {
        return alternateFileName;
    }

    /**
     * @return the descriptive English language name
     * @see #toString()
     */
    public String getName() {
        return displayName;
    }

    /**
     * @return a link to the source file in the repository
     */
    public String getLink() {
        return "<a href=\"" + HTMLOutput.PRE_LINK + fileName + "\" title=\""
                + toString() + "\">" + displayName + "</a>";
    }
    
    public String getLanguageCode() {
        return languageCode;
    }
}

