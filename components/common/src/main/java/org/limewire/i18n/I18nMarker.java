package org.limewire.i18n;

/**
 * Class to mark messages for translation.
 * <p>
 * The external <code>xgettext</code> tool picks up occurrences of 
 * {@link #marktr(String)} and extracts the string literal arguments 
 * into a template file for translation.
 */
public class I18nMarker {

    /**
     * Marks the string <code>text</code> to be translated but does not translate it.
     * @return the argument.
     */
    public static String marktr(String text) {
        return text;
    }

    public static String[] marktrn(String singularText, String pluralText) {
        return new String[] { singularText, pluralText };
    }
}
