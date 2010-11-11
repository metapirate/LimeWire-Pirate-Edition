package com.limegroup.gnutella.i18n;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Converts all translation bundles by replacing their keys with the values of
 * the English translation.
 * 
 * Shortcoming: msginit does not set the charset correctly, so do a manual replace
 * afterwards
 * 
 * perl -p -i -e 's/ASCII/UTF-8/g' *.po
 */
public class TranslationBundleKeyConverter {

    /**
     * Call this in lib/messagebundles.
     */
    public static void main(String[] args) throws Exception {
        File dir = new File(".");
        LanguageLoader loader = new LanguageLoader(dir);
        Map<String, LanguageInfo> languages = loader.loadLanguages();

        for (LanguageInfo info : languages.values()) {
            createPoFile(info, loader.getEnglishLines());
        }
    }

    private static void createPoFile(LanguageInfo info, List<Line> englishLines) throws Exception {
        
        if (info.getCode().length() == 0) {
            return;
        }
        
        // TODO generate correct header using 'msginit' so we get the right plural forms
        System.out.println("Generating file for: " + info.getCode() + ".UTF-8");
        System.out.println(Runtime.getRuntime().exec(new String[] { "msginit", "-l", info.getCode() + ".UTF-8" , 
                "--no-translator", "-i", "keys.pot" }).waitFor());

        File file = new File(System.getProperty("user.dir"), info.getCode() + ".po");
        System.out.println(file.getAbsolutePath());
        
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file, true), "UTF-8"));
                
        // skip until translation header comes:
        Iterator<Line> lines = englishLines.iterator();
        for (;lines.hasNext() && !LanguageUpdater.MARKER.equals(lines.next().getLine()););
        
        while (lines.hasNext()) {
            Line line = lines.next();
            if (line.isComment()) {
                continue;
            }
            
            String msgid = line.getValue();
            String msgstr = info.getProperties().getProperty(line.getKey(), "");
            
            if (msgid.length() > 0 && msgstr.length() > 0) {
                writer.print("msgid \"");
                writer.print(escapeQuotes(msgid));
                writer.println("\"");
                
                writer.print("msgstr \"");
                writer.print(escapeQuotes(msgstr));
                writer.println("\"");
                
                writer.println();
            }
        }
        
        writer.flush();
        writer.close();
    }
    
    private static String escapeQuotes(String str) {
        return str.replace("\"", "\\\"").replace("\n", "\\n");
    }

}