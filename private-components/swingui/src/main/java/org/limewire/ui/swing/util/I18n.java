package org.limewire.ui.swing.util;

import java.util.Locale;

import org.xnap.commons.i18n.I18nFactory;

public class I18n {

    private static final String BASENAME = "org.limewire.i18n.Messages";
    private static org.xnap.commons.i18n.I18n i18n = I18nFactory.getI18n(I18n.class, BASENAME);
    
    public static void setLocale(Locale locale) {
       i18n.setResources(BASENAME, locale, ClassLoader.getSystemClassLoader());
    }    
    
    public static String tr(String text) {
        return i18n.tr(text);
    }

    private static org.xnap.commons.i18n.I18n getNonCachedI18n(Locale locale) {
        return I18nFactory.getI18n(I18n.class, BASENAME, locale, I18nFactory.NO_CACHE);
    }
    
    /**
     * Returns the translation of a text in the given locale if available.
     * 
     * This allows you to look up a translation for a specific locale. Should be 
     * used with care since the whole hierarchy for the message bundle might be loaded.
     * @param locale the locale to look up the translation for
     * @param text the text to translate
     */
    public static String trl(Locale locale, String text) {
        return getNonCachedI18n(locale).tr(text);
    }

    public static String tr(String text, Object... args) {
        return i18n.tr(text.replace("'", "''"), args);
    }

    public static String trc(String comment, String text) {
        return i18n.trc(comment, text);
    }
    
    public static String trnc(String context, String singularText, String pluralText, long number) {
        return i18n.trnc(context, singularText, pluralText, number, number);
    }
    
    public static String trnc(String context, String singularText, String pluralText, long number, Object...args) {
        return i18n.trnc(context, singularText.replace("'", "''"), pluralText.replace("'", "''"), number, args);
    }

    /**
     * Returns the translated singular or plural form of the strings.  The singular
     * case is shown for number == 1, the plural form is shown for all other cases
     * including number == 0. Variable substitution '{0}' may still be used with the
     * singular and plural forms.
     */
    public static String trn(String singularText, String pluralText, long number) {
        return trn(singularText, pluralText, number, number);
    }

    public static String trn(String singularText, String pluralText, long number, Object...args) {
        return trn(i18n, singularText, pluralText, number, args); 
    }
    
    private static String trn(org.xnap.commons.i18n.I18n i18n, String singularText, String pluralText, long number, Object...args) {
        return i18n.trn(singularText.replace("'", "''"), pluralText.replace("'", "''"), number, args);
    }

    /**
     * Returns the translation of a text in the given locale if available.
     * <p>
     * This allows you to look up a translation for a specific locale. Should be 
     * used with care since the whole hierarchy for the message bundle might be loaded.
     * <p>
     * Delegates to {@link #trn(String, String, long)} using different 
     * {@link org.xnap.commons.i18n.I18n} instance.
     */
    public static String trln(Locale locale, String singularText, String pluralText, long number, Object... args) {
        return trn(getNonCachedI18n(locale), singularText, pluralText, number, args);
    }
}
