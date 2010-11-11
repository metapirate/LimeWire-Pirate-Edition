package com.limegroup.gnutella.i18n;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Rebuilds the language files, based on the English one.
 */
class LanguageUpdater {
    static final String MARKER = "# TRANSLATIONS START BELOW.";
    private final File lib;
    private final Map/* <String, LanguageInfo> */langs;
    private final List/* <String> */englishList;
    private boolean verbose = true;

    /**
     * Constructs a new LanguageUpdater.
     * 
     * @param dir
     * @param langs
     * @param englishLines
     */
    LanguageUpdater(File dir, Map/* <String, LanguageInfo> */langs,
            List/* <String> */englishLines) {
        this.lib = dir;
        this.langs = langs;
        this.englishList = englishLines;
        removeInitialComments(englishList);
    }

    /**
     * Determines if stuff should be printed.
     * 
     * @param silent
     */
    void setSilent(boolean silent) {
        this.verbose = !silent;
    }

    /**
     * Prints a message out if we're being verbose.
     * 
     * @param msg
     */
    void print(String msg) {
        if (this.verbose)
            System.out.print(msg);
    }

    /**
     * @param msg
     */
    void println(String msg) {
        if (this.verbose)
            System.out.println(msg);
    }

    /**
     * 
     */
    void println() {
        if (this.verbose)
            System.out.println();
    }

    /**
     * Updates all languages.
     */
    void updateAllLanguages() {
        for (Iterator/* <LanguageInfo> */i = this.langs.values().iterator(); i
                .hasNext();) {
            LanguageInfo next = (LanguageInfo)i.next();
            updateLanguage(next);
        }
    }

    /**
     * Updates a single language.
     * 
     * @param info
     */
    void updateLanguage(LanguageInfo info) {
        if (info == null) {
            println("Unknown language.");
            return;
        }
        print("Updating language: " + info.getName() + " (" + info.getCode()
                + ")... ");
        String filename = info.getFileName();
        File f = new File(lib, filename);
        if (!f.isFile())
            throw new IllegalArgumentException("Invalid info: " + info);
        File temp;
        BufferedReader reader;
        PrintWriter printer;
        try {
            temp = File.createTempFile("TEMP", info.getCode(), lib);
            reader = new BufferedReader(new InputStreamReader(
                    new FileInputStream(f), info.isUTF8() ? "UTF-8"
                            : "ISO-8859-1"));
            printer = new PrintWriter(temp, info.isUTF8() ? "UTF-8"
                    : "ISO-8859-1");
            if (info.isUTF8()) {
                reader.mark(1);
                if (reader.read() != '\uFEFF')
                    reader.reset(); /* was not a leading BOM */
                printer.print('\uFEFF'); /* force a leading BOM */
            }
            printInitialComments(printer, reader, info);
            printBody(printer, info);
            reader.close();
            printer.close();
            if (isDifferent(f, temp)) {
                println("...changes.");
                f.delete();
                temp.renameTo(f);
            } else {
                println("...no changes!");
                temp.delete();
            }
            if (info.isUTF8())
                native2ascii(info);
        } catch (IOException ioe) {
            println("...error! (" + ioe.getMessage() + ")");
        }
    }

    /**
     * Home-made native2ascii.
     */
    private void native2ascii(LanguageInfo info) {
        if (!info.isUTF8())
            throw new IllegalArgumentException("requires utf8 language.");
        InputStream in = null;
        OutputStream out = null;
        print("\tConverting to ASCII... ");
        try {
            in = new BufferedInputStream(
                    new FileInputStream(info.getFileName()));
            in.mark(3);
            if (in.read() != 0xEF || in.read() != 0xBB || in.read() != 0xBF)
                in.reset();
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    in, "UTF8"));
            out = new BufferedOutputStream(new FileOutputStream(info
                    .getAlternateFileName()));
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                    out, "ISO-8859-1"));
            String read;
            while ((read = reader.readLine()) != null) {
                writer.write(ascii(read));
                writer.newLine();
            }
            writer.flush();
            out.flush();
            println("... done!");
        } catch (IOException ignored) {
            println("... error! (" + ignored.getMessage() + ")");
        } finally {
            if (in != null)
                try {
                    in.close();
                } catch (IOException ignored) {}
            if (out != null)
                try {
                    out.close();
                } catch (IOException ignored) {}
        }
    }

    /**
     * Determines if there is any difference between file a & file b.
     */
    private boolean isDifferent(File a, File b) {
        InputStream ia = null, ib = null;
        try {
            ia = new BufferedInputStream(new FileInputStream(a));
            ib = new BufferedInputStream(new FileInputStream(b));
            int c;
            while ((c = ia.read()) == ib.read()) {
                // if we got here, both got to EOF at same time
                if (c == -1)
                    return false;
            }
        } catch (IOException ignored) {} finally {
            if (ia != null)
                try {
                    ia.close();
                } catch (IOException ignored) {}
            if (ib != null)
                try {
                    ib.close();
                } catch (IOException ignored) {}
        }
        // if we didn't exit in the loop, a character was different
        // or one stream ended before another.
        return true;
    }

    /**
     * Writes the body of the bundle.
     */
    private void printBody(PrintWriter printer, LanguageInfo info) {
        Properties parent = null;
        final boolean isUTF8 = info.isUTF8();
        if (info.isVariant()) {
            LanguageInfo pi = (LanguageInfo)langs.get(info.getBaseCode());
            if (pi != null)
                parent = pi.getProperties();
        }
        Properties props = info.getProperties();
        boolean reachedTranslations = false;
        for (Iterator i = englishList.iterator(); i.hasNext();) {
            Line line = (Line)i.next();
            if (MARKER.equals(line.getLine()))
                reachedTranslations = true;
            if (line.isComment()) {
                printer.println(line.getLine());
            } else {
                String key = line.getKey();
                String value = props.getProperty(key);
                // always write the English version, so translators
                // have a reference point for possibly needing to update
                // an older translation.
                if (reachedTranslations) {
                    printer.print("#### ");
                    printer.print(key);
                    printer.print("=");
                    printer.print(escape(line.getValue(), isUTF8));
                    printer.println();
                    if (parent != null) {
                        String pv = parent.getProperty(key);
                        if (pv != null && !pv.equals("")) {
                            printer.print("###$ ");
                            printer.print(key);
                            printer.print("=");
                            printer.print(escape(pv, isUTF8));
                            printer.println();
                        }
                    }
                } else if (value == null) {
                    value = ""; // null before translations == ""
                }
                if (!reachedTranslations
                        || (value != null && !value.equals("") && line
                                .getBraceCount() == Line.parseBraceCount(value))) {
                    printer.print(key);
                    printer.print("=");
                    printer.println(escape(value, isUTF8));
                } else {
                    printer.print("#? ");
                    printer.print(key);
                    printer.print("=");
                    printer.println();
                }
            }
        }
    }

    /**
     * Writes the initial comments from a given file to fos.
     */
    private void printInitialComments(PrintWriter printer,
            BufferedReader reader, LanguageInfo info) throws IOException {
        // TODO: look into initial comments, to see if more information should
        // be generated from 'info'.
        try {
            String read;
            // Read through and write the initial lines until we reach a
            // non-comment
            while ((read = reader.readLine()) != null) {
                Line line = new Line(read);
                if (!line.isComment())
                    break;
                printer.println(read);
            }
        } finally {}
    }

    /**
     * Removes the initial comments from the English properties file.
     */
    private void removeInitialComments(List l) {
        for (Iterator i = l.iterator(); i.hasNext();) {
            Line line = (Line)i.next();
            if (line.isComment())
                i.remove();
            else
                break;
        }
    }

    /**
     * Returns a string suitable for insertion into a UTF-8 encoded Properties
     * file. Some characters will always be escaped.
     */
    private String escape(final String s, final boolean isUTF8) {
        final int n = s.length();
        StringBuffer sb = new StringBuffer(n);
        for (int i = 0; i < n; i++) {
            int cp;
            if (Character.isSupplementaryCodePoint(cp = s.codePointAt(i)))
                i++;
            // TODO: use switch(getType(cp)) for more generic handling.
            // Whitespace's include all Spacechar's but not non-breaking spaces;
            // Spacechar's include all Whitespace's but not C0 controls.
            if (Character.isWhitespace(cp)
                    || Character.isSpaceChar(cp)
                    || Character.isISOControl(cp)
                    // if the target file is not UTF8, we must escape all non
                    // ISO-8859-1 characters.
                    || (!isUTF8 && cp >= '\u0100')
                    // treat isolated surrogates like controls
                    || !Character.isSupplementaryCodePoint(cp)
                    && (Character.isLowSurrogate((char)cp) || Character
                            .isHighSurrogate((char)cp))) {
                switch (cp) {
                // only ASCII regular SPACE can be left unchanged;
                case ' ':
                    sb.append(' ');
                    break;
                // all other whitespaces and controls must be escaped.
                case '\n':
                    sb.append("\\n");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                case '\f':
                    sb.append("\\f");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                default:
                    sb.append(hexUnicode(cp));
                }
            } else {
                // valid non-controls non-whitespaces can be left unchanged.
                sb.appendCodePoint(cp);
            }
        }
        return sb.toString();
    }

    /**
     * Converts the input string to ascii, using \\u escapes.
     */
    private String ascii(final String s) {
        final int n = s.length();
        final StringBuffer sb = new StringBuffer(n * 5);
        for (int i = 0; i < n; i++) {
            int p = s.codePointAt(i);
            if (p > 0x00ff || // not Latin-1
                    p <= 0x001f || // C0 controls
                    p >= 0x007f && p <= 0x00a0 || // DEL, C1 controls, NBSP
                    p == 0x00ad) // SHY (Soft Hyphen)
                sb.append(hexUnicode(p));
            else
                sb.appendCodePoint(p);
        }
        return sb.toString();
    }

    /**
     * Returns the escaped unicode hex representation of the codepoint.
     * 
     * @param cp
     *            the codepoint to represent; must be in the 17 first planes,
     *            and not a surrogate.
     */
    private String hexUnicode(final int cp) {
        if (cp <= 0xffff) {
            final String hex = Integer.toHexString(cp);
            final StringBuffer sb = new StringBuffer(6);
            sb.append("\\u");
            for (int j = hex.length(); j < 4; j++)
                sb.append('0');
            sb.append(hex);
            return sb.toString();
        }
        return new StringBuffer(12).append("\\u").append(
                Integer.toHexString(((cp - 0x10000) >> 10) + 0xD800)).append(
                "\\u").append(Integer.toHexString((cp & 0x3ff) + 0xDC00))
                .toString();
    }
}
