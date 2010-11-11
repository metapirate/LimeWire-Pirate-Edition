package org.limewire.ui.swing.util;

import java.util.Locale;

import com.google.inject.Inject;

/**
 * Simple wrapper for {@link I18n} to aid testing. 
 */
public class Translator {
    @Inject
    public Translator() {
    }
   
    /**
     * Wraps {@link I18n#tr(String)}.
     */
    public String translate(String text) {
        return I18n.tr(text);
    }
    
    /**
     * Wraps {@link I18n#trc(String,String)}.
     */
    public String translateWithComment(String comment, String text) {
        return I18n.trc(comment, text);
    }
    
    /**
     * @return if the current language is English.
     */
    public boolean isCurrentLanguageEnglish() {
        return LanguageUtils.isEnglishLocale(LanguageUtils.getCurrentLocale());
    }
    
    /**
     * @return the lower case text assuming the English locale.
     */
    public String toLowerCaseEnglish(String text) {
    	return text.toLowerCase(Locale.US);
    }
    
    /**
     * @return the lower case text assuming the current locale.
     */
    public String toLowerCaseCurrentLocale(String text) {
        return text.toLowerCase();
    }
}
