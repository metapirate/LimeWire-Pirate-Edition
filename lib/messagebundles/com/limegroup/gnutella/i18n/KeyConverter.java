package com.limegroup.gnutella.i18n;

import java.io.File;
import java.util.Enumeration;
import java.util.Map;
import java.util.Properties;

/**
 * Was used to convert certain keys in the old MessageBundle_**_.properties files.
 */
public class KeyConverter {

    @SuppressWarnings("unused")
    private final static Rule[] rules = new Rule[] {
            new ConcatenateAndInsertRule("DOWNLOAD_APPLY_NEW_THEME_START",
                    "DOWNLOAD_APPLY_NEW_THEME_END", "{0}",
                    "DOWNLOAD_APPLY_NEW_THEME"),
            new ConcatenateAndInsertRule("ERROR_BROWSE_HOST_FAILED_BEGIN_KEY",
                    "ERROR_BROWSE_HOST_FAILED_END_KEY", " {0} ",
                    "ERROR_BROWSE_HOST_FAILED"),
            new ConcatenateAndInsertRule("ERROR_CANT_RESUME_START",
                    "ERROR_CANT_RESUME_END", " {0} ", "ERROR_CANT_RESUME"),
            new ConcatenateAndInsertRule("MESSAGE_UNABLE_TO_RENAME_FILE_START",
                    "MESSAGE_UNABLE_TO_RENAME_FILE_END", " {0} ",
                    "MESSAGE_UNABLE_TO_RENAME_FILE"),
            new ConcatenateAndInsertRule("MESSAGE_FILE_CORRUPT",
                    "MESSAGE_CONTINUE_DOWNLOAD", " {0} ",
                    "MESSAGE_FILE_CORRUPT"),
            new ConcatenateAndInsertRule("MESSAGE_SENSITIVE_SHARE_TOP",
                    "MESSAGE_SENSITIVE_SHARE_BOTTOM", "\n\n{0}\n\n",
                    "MESSAGE_SENSITIVE_SHARE"),
            new ConcatenateRule("SEARCH_VIRUS_MSG", "SEARCH_VIRUS_MSG_ONE",
                    "SEARCH_VIRUS_MSG_TWO", "SEARCH_VIRUS_MSG_THREE") };

    @SuppressWarnings("unused")
    private static final Rule encodeMnemonics = new EncodeMnemonicAsAmpersAnd();
    
    @SuppressWarnings("unused")
    private static final Rule[] rules2 = new Rule[] {
        new ConcatenateAndInsertRule("STATISTICS_SHARING_TOOLTIP",
                "STATISTICS_FILES_TOOLTIP", " {0} ",
        "STATISTICS_SHARING_TOOLTIP_NEW"),
        new ConcatenateAndInsertRule("STATISTICS_SHARING_TOOLTIP", 
                "STATISTICS_FILES_TOOLTIP_PENDING", " {0} ",
                "STATISTICS_SHARING_TOOLTIP_PENDING_NEW"),
    };
    

    private static final Rule[] rules3 = new Rule[] {
        new ConcatenateAndInsertRule("DOWNLOAD_STATUS_WAITING_FOR_REQUERY_START",
                "DOWNLOAD_STATUS_WAITING_FOR_REQUERY_END", " {0}",
                "DOWNLOAD_STATUS_WAITING_FOR_REQUERY")
    };
    
    /**
     * Call this in lib/messagebundles.
     */
    public static void main(String[] args) throws Exception {
        File dir = new File(".");
        LanguageLoader loader = new LanguageLoader(dir);
        Map<String, LanguageInfo> languages = loader.loadLanguages();
        
        // apply conversions
        for (LanguageInfo language : languages.values()) {
            applyRules(language.getProperties(), rules3);
        }

        LanguageUpdater updater = new LanguageUpdater(dir, languages, loader
                .getEnglishLines());
        updater.updateAllLanguages();
    }

    private static void applyRules(Properties props, Rule...rules) {
        for (Rule rule : rules) {
            rule.apply(props);
        }
    }

    private interface Rule {
        void apply(Properties props);
    }

    /**
     * Concateantes first value and second value inserting <code>insert</code>
     * between them.
     */
    private static class ConcatenateAndInsertRule implements Rule {

        private String firstKey;

        private String secondKey;

        private String insert;

        private String newKey;

        public ConcatenateAndInsertRule(String firstKey, String secondKey,
                String insert, String newKey) {
            this.firstKey = firstKey;
            this.secondKey = secondKey;
            this.insert = insert;
            this.newKey = newKey;
        }

        public void apply(Properties props) {
            String firstValue = props.getProperty(firstKey, null);
            String secondValue = props.getProperty(secondKey, null);
            if (firstValue != null && secondValue != null) {
                System.out.println(newKey + "=" + firstValue + insert
                        + secondValue);
                props.setProperty(newKey, firstValue + insert + secondValue);
            }
        }

    }

    /**
     * Adds spaces between values.
     */
    private static class ConcatenateRule implements Rule {

        private String newKey;

        private String[] keys;

        public ConcatenateRule(String newKey, String... keys) {
            this.newKey = newKey;
            this.keys = keys;
        }

        public void apply(Properties props) {
            StringBuilder builder = new StringBuilder();
            for (String key : keys) {
                String value = props.getProperty(key, null);
                if (value == null) {
                    return;
                }
                builder.append(value);
                builder.append(" ");
            }
            builder.setLength(builder.length() - 1);
            System.out.println(newKey + "=" + builder.toString());
            props.setProperty(newKey, builder.toString());
        }
    }
    
    private static class EncodeMnemonicAsAmpersAnd implements Rule {

        public void apply(Properties props) {
            for (Enumeration<?> e = props.propertyNames(); e.hasMoreElements();) {
                String key = (String)e.nextElement();
                if (key.endsWith("_MNEMONIC")) {
                    // find corresponding label key
                    String labelKey = key.substring(0, key.length() - "_MNEMONIC".length());
                    String label = props.getProperty(labelKey, null);
                    if (label != null) {
                        String mnemonic = props.getProperty(key);
                        String replaced = label.replaceFirst(mnemonic, "&" + mnemonic);
                        if (replaced.equals(label)) {
                            replaced = label.replaceFirst(mnemonic.toLowerCase(), "&" + mnemonic.toLowerCase());
                        }
                        System.out.println("Replaced: " + replaced);
                        props.put(labelKey, replaced);
                    }
                }
            }
        }
        
    }

}
