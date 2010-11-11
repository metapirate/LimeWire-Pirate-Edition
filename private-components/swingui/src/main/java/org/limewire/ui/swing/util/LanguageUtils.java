package org.limewire.ui.swing.util;

import java.awt.Font;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.core.settings.ApplicationSettings;
import org.limewire.io.IOUtils;
import org.limewire.util.FileUtils;
import org.limewire.util.OSUtils;


/**
 * This class provides utility methods retrieving supported languages and
 * changing language settings.
 */
public class LanguageUtils {

    private static Log LOG = LogFactory.getLog(LanguageUtils.class);

    private static final String BUNDLE_PREFIX = "org/limewire/i18n/Messages_";

    private static final String BUNDLE_POSTFIX = ".class";

    private static final String BUNDLE_MARKER = "org/limewire/i18n/Messages.class";

    /**
     * Applies this language code to be the new language of the program.
     */
    public static void setLocale(Locale locale) {
        ApplicationSettings.LANGUAGE.set(locale.getLanguage());
        ApplicationSettings.COUNTRY.set(locale.getCountry());
        ApplicationSettings.LOCALE_VARIANT.set(locale.getVariant());
        
        LocaleUtils.setLocaleFromPreferences();
    }
    
    /**
     * Returns a Locale of the current language that is being used.
     */
    public static Locale getCurrentLocale() {
        return LocaleUtils.getCurrentLocale();
    }

    /**
     * Returns an array of supported language as a LanguageInfo[], always having
     * the English language as the first element.
     * <p>
     * This will only include languages that can be displayed using the given
     * font. If the font is null, all languages are returned.
     */
    public static Locale[] getLocales(Font font) {
        final List<Locale> locales = new LinkedList<Locale>();
        
        File jar = FileUtils.getJarFromClasspath(LanguageUtils.class.getClassLoader(), BUNDLE_MARKER);
        if (jar != null) {
            addLocalesFromJar(locales, jar);
        } else {
            LOG.warn("Could not find bundle jar to determine locales");
        }

        // TODO: fix for build
      /*  if (LimeWireUtils.isTestingVersion()) { 
            addLocalesFromJar(locales, new File(CVS_BUNDLE_FILE));
        } */
        
        Collections.sort(locales, new Comparator<Locale>() {
            public int compare(Locale o1, Locale o2) {
                return o1.getDisplayName(o1).compareToIgnoreCase(
                        o2.getDisplayName(o2));
            }
        });
        
        locales.remove(Locale.ENGLISH);
        locales.add(0, Locale.ENGLISH);

        // remove languages that cannot be displayed using this font
        if (font != null && !OSUtils.isMacOSX()) {
            for (Iterator<Locale> it = locales.iterator(); it.hasNext();) {
                Locale locale = it.next();
                if (!GuiUtils.canDisplay(font, locale.getDisplayName(locale))) {
                    it.remove();
                }
            }
        }

        return locales.toArray(new Locale[0]);
    }

    /**
     * Returns the languages as found from the classpath in messages.jar.
     */
    static void addLocalesFromJar(List<Locale> locales, File jar) {
        ZipFile zip = null;
        try {
            zip = new ZipFile(jar);
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                String name = entries.nextElement().getName();
                if (!name.startsWith(BUNDLE_PREFIX) || !name.endsWith(BUNDLE_POSTFIX)
                        || name.indexOf("$") != -1) {
                    continue;
                }

                String iso = name.substring(BUNDLE_PREFIX.length(), name.length()
                        - BUNDLE_POSTFIX.length());
                List<String> tokens = new ArrayList<String>(Arrays.asList(iso.split("_", 3)));
                if (tokens.size() < 1) {
                    continue;
                }
                while (tokens.size() < 3) {
                    tokens.add("");
                }

                Locale locale = new Locale(tokens.get(0), tokens.get(1), tokens.get(2));
                locales.add(locale);
            }
        } catch (IOException e) {
            LOG.warn("Could not determine locales", e);
        } finally {
            if (zip != null) {
                try {
                    zip.close();
                } catch (IOException ioe) {
                }
            }
        }
    }

    /**
     * Returns true if the language of <code>locale</code> is English.
     */
    public static boolean isEnglishLocale(Locale locale) {
        return Locale.ENGLISH.getLanguage().equals(locale.getLanguage());
    }

    /**
     * Returns a score between -1 and 3 how well <code>specificLocale</code>
     * matches <code>genericLocale</code>.
     * 
     * @return -1, if locales do not match, 3 if locales are equal
     */
    public static int getMatchScore(Locale specificLocale, Locale genericLocale) {
        int i = 0;
        if (specificLocale.getLanguage().equals(genericLocale.getLanguage())) {
            i += 1;
        } else if (genericLocale.getLanguage().length() > 0) {
            return -1;
        }
        if (specificLocale.getCountry().equals(genericLocale.getCountry())) {
            i += 1;
        } else if (genericLocale.getCountry().length() > 0) {
            return -1;
        }
        if (specificLocale.getVariant().equals(genericLocale.getVariant())) {
            i += 1;
        } else if (genericLocale.getVariant().length() > 0) {
            return -1;
        }
        
        return i;
    }
    
    /**
     * Uses the guess mechanism to attempt to guess the current locale.  The guess
     *  is then compared to the given list of available languages and the closest
     *  match is returned.  If no match is found English is defaulted since
     *  English will always be available.
     */
    public static Locale guessBestAvailableLocale(Locale... locales) {
        Locale guessLocale = guessLocale();
        Locale bestLocale = Locale.ENGLISH;
        int bestScore = Integer.MIN_VALUE;
        
        for ( Locale item : locales ) {
            int checkScore = getMatchScore(guessLocale, item); 
            if (checkScore >= 0 && checkScore > bestScore) {
                bestLocale = item;
                bestScore = checkScore; 
            }
        }
        
        return bestLocale;
    }
    
    public static Locale guessLocale() {
        String[] language = guessLanguage();
        return new Locale(language[0], language[1], language[2]);
    }
    
    public static String[] guessLanguage() {
        String ln = ApplicationSettings.LANGUAGE.get();
        String cn = ApplicationSettings.COUNTRY.get();
        String vn = ApplicationSettings.LOCALE_VARIANT.get();
        
        File file = new File("language.prop");
        if(!file.exists())
            return new String[] { ln, cn, vn };
            
        InputStream in = null;
        BufferedReader reader = null;
        String code = "";
        try {
            in = new FileInputStream(file);
            reader = new BufferedReader(new InputStreamReader(in));
            code = reader.readLine();
        } catch(IOException ignored) {
        } finally {
            IOUtils.close(in);
            IOUtils.close(reader);
        }
        
        String[] mapped = getLCID(code);
        if(mapped != null)
            return mapped;
        else
            return new String[] { ln, cn, vn };
    }
    
    /**
     * Returns the String[] { languageCode, countryCode, variantCode }
     * for the Windows LCID.
     */
    public static String[] getLCID(String code) {
        Map<String, String[]> map = new HashMap<String, String[]>();
        map.put("1078", new String[] { "af", "", "" } );
        map.put("1052", new String[] { "sq", "", "" } );
        map.put("5121", new String[] { "ar", "", "" } );
        map.put("15361", new String[] { "ar", "", "" } );
        map.put("3073", new String[] { "ar", "", "" } );
        map.put("2049", new String[] { "ar", "", "" } );
        map.put("11265", new String[] { "ar", "", "" } );
        map.put("13313", new String[] { "ar", "", "" } );
        map.put("12289", new String[] { "ar", "", "" } );
        map.put("4097", new String[] { "ar", "", "" } );
        map.put("6145", new String[] { "ar", "", "" } );
        map.put("8193", new String[] { "ar", "", "" } );
        map.put("16385", new String[] { "ar", "", "" } );
        map.put("1025", new String[] { "ar", "", "" } );
        map.put("10241", new String[] { "ar", "", "" } );
        map.put("7169", new String[] { "ar", "", "" } );
        map.put("14337", new String[] { "ar", "", "" } );
        map.put("9217", new String[] { "ar", "", "" } );
        map.put("1069", new String[] { "eu", "", "" } );
        map.put("1059", new String[] { "be", "", "" } );
        map.put("1093", new String[] { "bn", "", "" } );
        map.put("1027", new String[] { "ca", "", "" } );
        map.put("3076", new String[] { "zh", "", "" } );
        map.put("5124", new String[] { "zh", "", "" } );
        map.put("2052", new String[] { "zh", "", "" } );
        map.put("4100", new String[] { "zh", "", "" } );
        map.put("1028", new String[] { "zh", "TW", "" } );
        map.put("1050", new String[] { "hr", "", "" } );
        map.put("1029", new String[] { "cs", "", "" } );
        map.put("1030", new String[] { "da", "", "" } );
        map.put("2067", new String[] { "nl", "", "" } );
        map.put("1043", new String[] { "nl", "", "" } );
        map.put("3081", new String[] { "en", "", "" } );
        map.put("10249", new String[] { "en", "", "" } );
        map.put("4105", new String[] { "en", "", "" } );
        map.put("9225", new String[] { "en", "", "" } );
        map.put("6153", new String[] { "en", "", "" } );
        map.put("8201", new String[] { "en", "", "" } );
        map.put("5129", new String[] { "en", "", "" } );
        map.put("13321", new String[] { "en", "", "" } );
        map.put("7177", new String[] { "en", "", "" } );
        map.put("11273", new String[] { "en", "", "" } );
        map.put("2057", new String[] { "en", "", "" } );
        map.put("1033", new String[] { "en", "", "" } );
        map.put("12297", new String[] { "en", "", "" } );
        map.put("1061", new String[] { "et", "", "" } );
        map.put("1035", new String[] { "fi", "", "" } );
        map.put("2060", new String[] { "fr", "", "" } );
        map.put("11276", new String[] { "fr", "", "" } );
        map.put("3084", new String[] { "fr", "", "" } );
        map.put("9228", new String[] { "fr", "", "" } );
        map.put("12300", new String[] { "fr", "", "" } );
        map.put("1036", new String[] { "fr", "", "" } );
        map.put("5132", new String[] { "fr", "", "" } );
        map.put("13324", new String[] { "fr", "", "" } );
        map.put("6156", new String[] { "fr", "", "" } );
        map.put("10252", new String[] { "fr", "", "" } );
        map.put("4108", new String[] { "fr", "", "" } );
        map.put("7180", new String[] { "fr", "", "" } );
        map.put("3079", new String[] { "de", "", "" } );
        map.put("1031", new String[] { "de", "", "" } );
        map.put("5127", new String[] { "de", "", "" } );
        map.put("4103", new String[] { "de", "", "" } );
        map.put("2055", new String[] { "de", "", "" } );
        map.put("1032", new String[] { "el", "", "" } );
        map.put("1037", new String[] { "iw", "", "" } );
        map.put("1081", new String[] { "hi", "", "" } );
        map.put("1038", new String[] { "hu", "", "" } );
        map.put("1039", new String[] { "is", "", "" } );
        map.put("1057", new String[] { "id", "", "" } );
        map.put("1040", new String[] { "it", "", "" } );
        map.put("2064", new String[] { "it", "", "" } );
        map.put("1041", new String[] { "ja", "", "" } );
        map.put("1042", new String[] { "ko", "", "" } );
        map.put("1062", new String[] { "lv", "", "" } );
        map.put("2110", new String[] { "ms", "", "" } );
        map.put("1086", new String[] { "ms", "", "" } );
        map.put("1082", new String[] { "mt", "", "" } );
        map.put("1044", new String[] { "no", "", "" } );
        map.put("2068", new String[] { "nn", "", "" } );
        map.put("1045", new String[] { "pl", "", "" } );
        map.put("1046", new String[] { "pt", "BR", "" } );
        map.put("2070", new String[] { "pt", "", "" } );
        map.put("1048", new String[] { "ro", "", "" } );
        map.put("2072", new String[] { "ro", "", "" } );
        map.put("1049", new String[] { "ru", "", "" } );
        map.put("2073", new String[] { "ru", "", "" } );
        map.put("3098", new String[] { "sr", "", "" } );
        map.put("2074", new String[] { "sr", "", "" } );
        map.put("1051", new String[] { "sk", "", "" } );
        map.put("1060", new String[] { "sl", "", "" } );
        map.put("11274", new String[] { "es", "", "" } );
        map.put("16394", new String[] { "es", "", "" } );
        map.put("13322", new String[] { "es", "", "" } );
        map.put("9226", new String[] { "es", "", "" } );
        map.put("5130", new String[] { "es", "", "" } );
        map.put("7178", new String[] { "es", "", "" } );
        map.put("12298", new String[] { "es", "", "" } );
        map.put("17418", new String[] { "es", "", "" } );
        map.put("4106", new String[] { "es", "", "" } );
        map.put("18442", new String[] { "es", "", "" } );
        map.put("3082", new String[] { "es", "", "" } );
        map.put("2058", new String[] { "es", "", "" } );
        map.put("19466", new String[] { "es", "", "" } );
        map.put("6154", new String[] { "es", "", "" } );
        map.put("15370", new String[] { "es", "", "" } );
        map.put("10250", new String[] { "es", "", "" } );
        map.put("20490", new String[] { "es", "", "" } );
        map.put("1034", new String[] { "es", "", "" } );
        map.put("14346", new String[] { "es", "", "" } );
        map.put("8202", new String[] { "es", "", "" } );
        map.put("1053", new String[] { "sv", "", "" } );
        map.put("2077", new String[] { "sv", "", "" } );
        map.put("1097", new String[] { "ta", "", "" } );
        map.put("1054", new String[] { "th", "", "" } );
        map.put("1055", new String[] { "tr", "", "" } );
        map.put("1058", new String[] { "uk", "", "" } );
        map.put("1056", new String[] { "ur", "", "" } );
        map.put("2115", new String[] { "uz", "", "" } );
        map.put("1091", new String[] { "uz", "", "" } );
        map.put("1066", new String[] { "vi", "", "" } );
        
        return map.get(code);
    }
        
}
