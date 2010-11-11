package com.limegroup.gnutella.i18n;

import java.io.PrintStream;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

/**
 * Writes language info out in HTML format.
 */
@SuppressWarnings("unchecked")
class HTMLOutput {
    /** @see LanguageInfo#getLink() */
    static final String PRE_LINK = "http://www.limewire.org/fisheye/viewrep/~raw,r=MAIN/limecvs/lib/messagebundles/";
    private static final String DEFAULT_LINK = PRE_LINK
            + LanguageLoader.BUNDLE_NAME + LanguageLoader.PROPS_EXT;
    /** constant link to the translate mailing list. */
    private static final String HTML_TRANSLATE_EMAIL_ADDRESS = "<script type=\"text/javascript\" language=\"JavaScript\"><!--\n"
            + "// Protected email script by Joe Maller JavaScripts available at http://www.joemaller.com\n"
            + "// This script is free to use and distribute but please credit me and/or link to my site.\n"
            + "e_mA_iL_E = ('limewire' + \"&#46;\" + 'org'); e_mA_iL_E = ('translate' + \"&#64;\" + e_mA_iL_E);\n"
            + "document.write('<a href=\"mai' + \"lto:\" + e_mA_iL_E + '\">' + e_mA_iL_E + '</a>');\n"
            + "//--></script><noscript><a href=\"#\">[Email address protected by JavaScript:\n"
            + "please enable JavaScript to contact me]</a></noscript>";
    /** minimum completion levels for the status HTML page */
    private static final double MIN_PERCENTAGE_COMPLETED = 0.75;
    private static final double MIN_PERCENTAGE_NEED_REVISION = 0.65;
    private static final double MIN_PERCENTAGE_MIDWAY = 0.45;
    private static final int MIN_COUNT_STARTED = 20;
    private final StringBuffer page;
    private final DateFormat df;
    private final NumberFormat pc;
    private final Map/* <String code, LanguageInfo li> */langs;
    private final int basicTotal;

    /**
     * Constructs a new HTML output.
     * 
     * @param df
     * @param pc
     * @param langs
     * @param basicTotal
     */
    HTMLOutput(DateFormat df, NumberFormat pc, Map langs, int basicTotal) {
        this.df = df;
        this.pc = pc;
        this.langs = langs;
        this.basicTotal = basicTotal;
        this.page = buildHTML();
    }

    /**
     * Creates the HTML.
     * 
     * @return the HTML page in a StringBuffer.
     */
    StringBuffer buildHTML() {
        List langsCompleted = new LinkedList();
        List langsNeedRevision = new LinkedList();
        List langsMidway = new LinkedList();
        List langsStarted = new LinkedList();
        List langsEmbryonic = new LinkedList();
        Map charsets = new TreeMap(new CharsetNameComparator());
        for (Iterator i = langs.entrySet().iterator(); i.hasNext();) {
            final Map.Entry entry = (Map.Entry)i.next();
            // final String code = (String)entry.getKey();
            final LanguageInfo li = (LanguageInfo)entry.getValue();
            final Properties props = li.getProperties();
            final int count = props.size();
            final double percentage = (double)count / (double)basicTotal;
            li.setPercentage(percentage);
            if (percentage >= MIN_PERCENTAGE_COMPLETED)
                langsCompleted.add(li);
            else if (percentage >= MIN_PERCENTAGE_NEED_REVISION)
                langsNeedRevision.add(li);
            else if (percentage >= MIN_PERCENTAGE_MIDWAY)
                langsMidway.add(li);
            else if (count >= MIN_COUNT_STARTED)
                langsStarted.add(li);
            else
                langsEmbryonic.add(li);
            String script = li.getScript();
            List inScript = (List)charsets.get(script);
            if (inScript == null) {
                inScript = new LinkedList();
                charsets.put(script, inScript);
            }
            inScript.add(li);
        }
        StringBuffer newpage = new StringBuffer();
        buildStartOfPage(newpage);
        buildStatus(newpage, langsCompleted, langsNeedRevision, langsMidway,
                langsStarted, langsEmbryonic);
        buildAfterStatus(newpage);
        buildProgress(newpage, charsets);
        buildEndOfPage(newpage);
        return newpage;
    }

    /**
     * Prints the HTML to 'out'.
     * 
     * @param out
     */
    void printHTML(PrintStream out) {
        /*
         * Make sure printed page contains only ASCII, converting all other code
         * points to decimal NCRs. This will work whatever charset will be
         * selected by the user's browser.
         */
        int pageLength = page.length();
        for (int index = 0; index < pageLength;) {
            int c = page.charAt(index++); // char's are always positive
            if (c < 160) { /* C0 or Basic Latin or C1 */
                if (c >= 32 && c < 127 || c == '\t') /* Basic Latin or TAB */
                    out.print((char)c);
                else if (c == '\n') /* LF */
                    out.println(); /* platform's newline sequence */
                /* ignore all other C0 and C1 controls */
            } else { /* Use NCRs */
                /* special check for surrogate pairs */
                if (c >= 0xD800 && c <= 0xDBFF && index < pageLength) {
                    char c2 = page.charAt(index);
                    if (c2 >= 0xDC00 && c2 <= 0xDFFF) {
                        c = 0x10000 + ((c - 0xD800) << 10) + (c2 - 0xDC00);
                        index++;
                    }
                }
                out.print("&#");
                out.print(c);// decimal NCR notation
                out.print(';');
            }
        }
    }

    /**
     * Builds the start of the page.
     */
    private void buildStartOfPage(StringBuffer newpage) {
        newpage
                .append("  <div id=\"bod1\">\n"
                        + "   <h1>Help Internationalize LimeWire!</h1>\n"
                        + "   <table border=\"0\" cellpadding=\"0\" cellspacing=\"0\">\n"
                        + "   <tr>\n"
                        + /* Three columns */
                        "    <td valign=\"top\" style=\"line-height: 16px;\">\n"
                        +
                        /* Start column 1 (main content) */
                        "     The LimeWire Open Source Project has embarked on an effort to\n"
                        + "     internationalize LimeWire.&nbsp; If you are an avid English-speaking user\n"
                        + "     fluent in another language, we need your help!&nbsp; Helping requires no\n"
                        + "     programming knowledge and little computer savviness beyond using a word\n"
                        + "     processor.<br />\n"
                        + "     <br />\n"
                        + "      <!--#include virtual=\"/translationbounties.shtml\" -->\n"
                        + "     <br />\n"
                        + "     <b>HOW LIMEWIRE SUPPORTS MULTIPLE LANGUAGES</b><br />\n"
                        + "     <br />\n"
                        + "     First, view this <a\n"
                        + "     href=\"http://www.limewire.com/img/screenshots/search.jpg\"\n"
                        + "     target=\"_blank\">LimeWire screen-shot</a>.&nbsp; Notice how the tabs\n"
                        + "     (<b>Search</b>, <b>Monitor</b>, <b>Library</b>, etc.) and the buttons\n"
                        + "     (<b>Download</b>, <b>Cancel</b>, etc.) have text on them.&nbsp; All\n"
                        + "     elements of the LimeWire interface can be translated to any language very\n"
                        + "     easily.<br />\n"
                        + "     <br />\n"
                        + "     This translation is accomplished by packaging all the words of the\n"
                        + "     program into a &quot;message bundle&quot;.&nbsp; A message bundle is more\n"
                        + "     or less a list, with phrases corresponding to certain parts of the\n"
                        + "     software.&nbsp; There are message bundles for different languages, so\n"
                        + "     there is an English message bundle, a French message bundle, a Japanese\n"
                        + "     message bundle, etc.&nbsp; In English, the text for the download button\n"
                        + "     is &quot;Download&quot;, whereas in French the text is &quot;Charger&quot;\n"
                        + "     (which is French for &quot;download&quot;).<br />\n"
                        + "     <br />\n"
                        + "     When you start LimeWire, the program loads the appropriate message bundle\n"
                        + "     and uses its contents for any interface element that has text on it.&nbsp;\n"
                        + "     For instance, this is the <a\n"
                        + "href=\""
                        + PRE_LINK
                        + "MessagesBundle.properties\">English\n"
                        + "     message bundle</a>.&nbsp; Note the line:<br />\n"
                        + "     <blockquote>\n"
                        + "      <table border=\"0\" cellspacing=\"1\" cellpadding=\"4\""
                        + "       bgcolor=\"#b1b1b1\">\n"
                        + "      <tr bgcolor=\"#EFEFEF\">\n"
                        + "       <td><code>\n"
                        + "SEARCH_DOWNLOAD_BUTTON_LABEL=Download</code></td>\n"
                        + "      </tr>\n"
                        + "      </table>\n"
                        + "     </blockquote>\n"
                        + "     This line indicates that the label used on the download button on\n"
                        + "     the search tab should read &quot;Download&quot;.&nbsp; Contrast this\n"
                        + "     with the same line in the <a\n"
                        + "href=\""
                        + PRE_LINK
                        + "MessagesBundle_fr.properties\">French\n"
                        + "     message bundle</a>:<br />\n"
                        + "     <blockquote>\n"
                        + "      <table border=\"0\" cellspacing=\"1\" cellpadding=\"4\"\n"
                        + "       bgcolor=\"#b1b1b1\">\n"
                        + "      <tr bgcolor=\"#EFEFEF\">\n"
                        + "       <td><code>\n"
                        + "#### SEARCH_DOWNLOAD_BUTTON_LABEL=Download<br />\n"
                        + "SEARCH_DOWNLOAD_BUTTON_LABEL=Charger</code></td>\n"
                        + "      </tr>\n"
                        + "      </table>\n"
                        + "     </blockquote>\n"
                        + "     Note that the line starting with a &quot;#&quot; is a comment line,\n"
                        + "     meaning it is not used by LimeWire.&nbsp; The English translation will\n"
                        + "     always be present as a reference.&nbsp; A label that is not yet\n"
                        + "     translated in a bundle will look like the following:<br />\n"
                        + "     <blockquote>\n"
                        + "      <table border=\"0\" cellspacing=\"1\" cellpadding=\"4\"\n"
                        + "       bgcolor=\"#b1b1b1\">\n"
                        + "       <tr bgcolor=\"#EFEFEF\">\n"
                        + "        <td><code>\n"
                        + "#### SOME_NEW_LABEL=New!<br />\n"
                        + "#? SOME_NEW_LABEL=</code></td>\n"
                        + "       </tr>\n"
                        + "      </table>\n"
                        + "     </blockquote>\n"
                        + "     To provide a translation, one just needs to append the translated text\n"
                        + "     after the equal sign, and remove the leading comment mark and\n"
                        + "     space.&nbsp; The French translation would look like the following:<br />\n"
                        + "     <blockquote>\n"
                        + "      <table border=\"0\" cellspacing=\"1\" cellpadding=\"4\"\n"
                        + "       bgcolor=\"#b1b1b1\">\n"
                        + "       <tr bgcolor=\"#EFEFEF\">\n"
                        + "        <td><code>\n"
                        + "#### SOME_NEW_LABEL=New!<br />\n"
                        + "SOME_NEW_LABEL=Nouveau!</code></td>\n"
                        + "       </tr>\n" + "      </table>\n"
                        + "     </blockquote>\n" + "     <br />\n");
    }

    /**
     * Builds the status portion of the page.
     */
    private void buildStatus(StringBuffer newpage, List langsCompleted,
            List langsNeedRevision, List langsMidway, List langsStarted,
            List langsEmbryonic) {
        newpage.append("     <b>TRANSLATION STATUS</b>\n" + "     <br />\n"
                + "     <ol>\n");
        // ### Need subject-verb agreement here; need to check if
        // ### size of list == 1 or singular versus plural
        buildStatus(
                newpage,
                langsCompleted,
                "       are complete and will require only small revisions during the project\n"
                        + "       evolution.");
        buildStatus(
                newpage,
                langsNeedRevision,
                "       are mostly complete and can still be used reliably, but may need some\n"
                        + "       revisions and a few missing translations to work best with newest\n"
                        + "       versions.");
        buildStatus(
                newpage,
                langsMidway,
                "       have been completed for an old version, but may now require some work,\n"
                        + "       tests and revisions plus additional missing translations to reach a\n"
                        + "       reliable status.");
        buildStatus(
                newpage,
                langsStarted,
                "       are partly translated but still unfinished, and their use in LimeWire\n"
                        + "       may be difficult for native language users.&nbsp; Providing a more\n"
                        + "       complete translation would be very much appreciated.");
        buildStatus(newpage, langsEmbryonic,
                "       are only embryonic and need a complete translation.&nbsp;\n"
                        + "       The current files are largely untranslated.");
        newpage.append("     </ol><br />\n");
    }

    /**
     * Builds an individual bullet point in the status portion of the page.
     */
    private void buildStatus(StringBuffer newpage, List langsList, String status) {
        boolean first = true;
        for (Iterator i = langsList.iterator(); i.hasNext();) {
            LanguageInfo l = (LanguageInfo)i.next();
            if (first)
                newpage.append("      <li>\n");
            else if (!i.hasNext())
                newpage.append(" and\n");
            else
                newpage.append(",\n");
            newpage.append("       " + l.getLink());
            first = false;
        }
        if (!first)
            newpage.append("\n" + status + "</li>\n");
    }

    /**
     * Builds the info after the status portion.
     */
    private void buildAfterStatus(StringBuffer newpage) {
        newpage
                .append("     <b>GENERAL CONSIDERATIONS FOR TRANSLATORS</b><br />\n"
                        + "     <br />\n"
                        + "     Do not start with the existing message bundle installed with your current\n"
                        + "     version of LimeWire.&nbsp; Make sure you <b>work on the latest version of\n"
                        + "     a message bundle</b>.&nbsp; You can get the latest bundle by clicking on\n"
                        + "     a language in the list on the right side of this page.<br />\n"
                        + "     <br />\n"
                        + "     When translating, adopt the common terminology used in your localized\n"
                        + "     operating system.&nbsp; In some cases, some terms were imported from\n"
                        + "     English, despite other terms already existing in your own language.&nbsp;\n"
                        + "     If a local term can be used unambiguously, please use it in preference to\n"
                        + "     the English term, even if you have seen many uses of this English term on\n"
                        + "     web sites.&nbsp; A good translation must be understood by people who are\n"
                        + "     not entirely savvy with Internet and computer jargon.&nbsp; Pay\n"
                        + "     particularly attention to the non-technical translation of common terms:\n"
                        + "     download, upload, host, byte, firewall, address, file, directory, #\n"
                        + "     (number of), leaf (terminal node), etc.<br />\n"
                        + "     <br />\n"
                        + "     Avoid translating word for word and do not use blindly automatic\n"
                        + "     translators.&nbsp; Be imaginative but make a clear and concise\n"
                        + "     translation.&nbsp; For button labels and column names, do not translate\n"
                        + "     them with long sentences, as they need to be short.&nbsp; Suppress some\n"
                        + "     articles, or use abbreviations if necessary.<br />\n"
                        + "     <br />\n"
                        + "     During the translation process, you may <a\n"
                        + "     href=\"http://www.limewire.org/mailinglist.shtml\">subscribe to the\n"
                        + "     translate mailing list</a> where you may benefit from other translators'\n"
                        + "     questions and knowledge as well as receive assistance in English or\n"
                        + "     French.<br />\n"
                        + "     <br />\n"
                        + "     <b>HOW TO SUBMIT CORRECTIONS OR ENHANCEMENTS FOR YOUR LANGUAGE</b><br />\n"
                        + "     <br />\n"
                        + "     If your corrections are significant, you may send your complete message\n"
                        + "     bundle to\n"
                        + HTML_TRANSLATE_EMAIL_ADDRESS
                        + ".&nbsp;\n"
                        + "     Please be sure to include all resource strings defined in the latest\n"
                        + "     version of the existing message bundle before sending us your\n"
                        + "     revision.<br />\n"
                        + "     <br />\n"
                        + "     For simple few corrections or additions, just send the corrected lines in\n"
                        + "     the content body of an email (making sure to select the correct character\n"
                        + "     encoding in your email tool before sending it so that non-ASCII\n"
                        + "     characters are not lost or replaced), with your comments.<br />\n"
                        + "     <br />\n"
                        + "     <i>We will review submitted translations and integrate all valuable\n"
                        + "     contributions as quickly as possible.</i><br />\n"
                        + "     <br />\n"
                        + "     <b>WHICH TOOL OR EDITOR TO USE FOR TRANSLATIONS</b><br />\n"
                        + "     <br />\n"
                        + "     For <b>Basic Latin or Western European Latin-based languages</b>, which\n"
                        + "     can use the US-ASCII or ISO-8859-1 character set, any text editor (such\n"
                        + "     as Notepad on Windows) can be used on Windows and Linux.&nbsp; Once a\n"
                        + "     file is completed, it can be sent as a simple text file to\n"
                        + HTML_TRANSLATE_EMAIL_ADDRESS
                        + ".<br />\n"
                        + "     <br />\n"
                        + "     For <b>Central European languages</b>, the preferred format is a simple\n"
                        + "     text file encoded with the ISO-8859-2 character set, or a UTF-8 encoded\n"
                        + "     simple text file (which can be edited with Notepad on Windows 2000/XP),\n"
                        + "     or a correctly marked-up HTML document such as HTML email, or a Word\n"
                        + "     document.<br />\n"
                        + "     <br />\n"
                        + "     For <b>other European languages</b>, the preferred format is a plain-text\n"
                        + "     file, encoded preferably with UTF-8 or a ISO-8859-* character set that\n"
                        + "     you should explicitly specify, or a correctly marked-up HTML document, or\n"
                        + "     a Word document.&nbsp; Please specify your working operating system and\n"
                        + "     editor you used to create plain-text files (we may support incoming\n"
                        + "     Windows codepages or Mac charsets, but we will convert them to Unicode\n"
                        + "     UTF-8 in our repository).<br />\n"
                        + "     <br />\n"
                        + "     For <b>Semitic languages</b> (Arabic, Hebrew...), the preferred format is\n"
                        + "     a plain-text file edited with a editor that supports the right-to-left\n"
                        + "     layout, encoded preferably with UTF-8 or a ISO-8859-* character set, in\n"
                        + "     logical order.&nbsp; Be careful with the relative order of keys and\n"
                        + "     values, and with the appearance of ASCII punctuations around\n"
                        + "     right-to-left words: make sure that your editor uses the RTL layout with\n"
                        + "     the edited text aligned on the right; please do not insert BiDi control\n"
                        + "     overrides; but you may need to place LRE/PDF marks (U+202B/U+202C)\n"
                        + "     locally around non-Semitic words inserted within Semitic sentences.&nbsp;\n"
                        + "     Also the &quot;<code>\\n</code>&quot; sequences that encode a newline\n"
                        + "     will be displayed within semitic text as &quot;<code>n\\</code>&quot;: do\n"
                        + "     not use BiDi override controls for such special sequence whose appearance\n"
                        + "     in your editor is not important, but that MUST be entered with a leading\n"
                        + "     backslash before the &quot;n&quot; character.<br />\n"
                        + "     <br />\n"
                        + "     For <b>Asian Languages</b>, the preferred submission format is a Unicode\n"
                        + "     text file encoded with UTF-8.&nbsp; Users of Windows 2000/XP can use\n"
                        + "     Notepad but you must explicitly select the UTF-8 encoding when saving\n"
                        + "     your file.&nbsp; Users of localized versions of Windows 95/98/ME can only\n"
                        + "     save their file in the native local &quot;ANSI&quot; encoding, and should\n"
                        + "     then send us their translation by copying and pasting it in the content\n"
                        + "     body of the email.<br />\n"
                        + "     <br />\n"
                        + "     <b>Mac users</b> should use a word processor and send us their\n"
                        + "     translations in an unambiguous format.&nbsp; On Mac OSX, the best tool is\n"
                        + "     &quot;TextEdit&quot;, from the Jaguar accessories, with which you can\n"
                        + "     directly edit and save plain-text files encoded with UTF-8.<br />\n"
                        + "     <br />\n"
                        + "     <b>Linux users</b> can also participate if they have a correct\n"
                        + "     environment for their locale.&nbsp; Files can be edited with\n"
                        + "     &quot;vi&quot;, &quot;emacs&quot;, or other editors.<br />\n"
                        + "     <br />\n"
                        + "     For further information about internationalization standards, language\n"
                        + "     and country codes, character sets and encodings, the following web pages\n"
                        + "     may be helpful:<br />\n"
                        + "     <ul>\n"
                        + "      <li>Language codes: <a\n"
                        + "       href=\"http://www.loc.gov/standards/iso639-2/englangn.html\"\n"
                        + "target=\"_blank\">http://www.loc.gov/standards/iso639-2/englangn.html</a></li>\n"
                        + "      <li>Country codes: <a\n"
                        + "       href=\"http://www.iso.org/iso/en/prods-services/iso3166ma/index.html\"\n"
                        + "target=\"_blank\">http://www.iso.org/iso/en/prods-services/iso3166ma/index.html</a></li>\n"
                        + "      <li>Character sets: <a\n"
                        + "       href=\"http://www.w3.org/International/O-charset.html\"\n"
                        + "target=\"_blank\">http://www.w3.org/International/O-charset.html</a></li>\n"
                        + "      <li>Letter database (languages and character sets): <a\n"
                        + "       href=\"http://www.eki.ee/letter/\"\n"
                        + "       target=\"_blank\">http://www.eki.ee/letter/</a></li>\n"
                        + "      <li>Other internationalization data: <a\n"
                        + "       href=\"http://www.unicode.org/unicode/onlinedat/resources.html\"\n"
                        + "target=\"_blank\">http://www.unicode.org/unicode/onlinedat/resources.html</a></li>\n"
                        + "     </ul>\n"
                        + "     An excellent tutorial on various character sets, including the ASCII\n"
                        + "     variants, the ISO-8859 family, the Windows &quot;ANSI code pages&quot;,\n"
                        + "     Macintosh character codes, and Unicode (or its ISO/IEC 10646 repertoire)\n"
                        + "     can be found at <a href=\"http://www.cs.tut.fi/~jkorpela/chars.html\"\n"
                        + "     target=\"_blank\">http://www.cs.tut.fi/~jkorpela/chars.html</a>.<br />\n"
                        + "     <br />\n"
                        + "     Users that do not have the correct tools to edit a message bundle can\n"
                        + "     send us an email in English or in French that explains their needs.<br />\n"
                        + "     <br />\n"
                        + "     <b>HOW TO TEST A NEW TRANSLATION</b><br />\n"
                        + "     <br />\n"
                        + "     Only Windows and Unix simple text editors can create a plain-text file\n"
                        + "     which will work in LimeWire, and only for languages using the Western\n"
                        + "     European Latin character set.&nbsp; Do not use &quot;SimpleText&quot; on\n"
                        + "     Mac OS to edit properties files as SimpleText does not create plain-text\n"
                        + "     files.&nbsp; Other translations need to be converted into regular\n"
                        + "     properties files, encoded using the ISO-8859-1 Latin character set and\n"
                        + "     Unicode escape sequences, with a tool &quot;native2ascii&quot; found in\n"
                        + "     the Java Development Kit.<br />\n"
                        + "     <br />\n"
                        + "     You do not need to rename your translated and converted bundle, which can\n"
                        + "     co-exist with the English version.&nbsp; LimeWire will load the\n"
                        + "     appropriate resource file according to the\n"
                        + "     &quot;<code>LANGUAGE=</code>&quot;, and &quot;<code>COUNTRY=</code>&quot;\n"
                        + "     settings stored in your &quot;<code>limewire.props</code>&quot;\n"
                        + "     preferences file.&nbsp; The list on the right can help you to find the\n"
                        + "     correct language code to use.<br />\n"
                        + "     <br />\n"
                        + "     Bundles are stored in a single compressed archive named\n"
                        + "     &quot;<code>MessagesBundles.jar</code>&quot; installed with\n"
                        + "     LimeWire.&nbsp; All bundles are named\n"
                        + "     &quot;MessagesBundle_xx.properties&quot;, where &quot;xx&quot; is\n"
                        + "     replaced by the language code.&nbsp; Note that bundles for languages\n"
                        + "     using non-Western European Latin characters will be converted from UTF-8\n"
                        + "     to ASCII using a special format with hexadecimal Unicode escape\n"
                        + "     sequences, prior to their inclusion in this archive.&nbsp; This can be\n"
                        + "     performed using the <code>native2ascii</code> tool from the Java\n"
                        + "     Development Kit.&nbsp; If you do not know how to proceed to test the\n"
                        + "     translation yourself, ask us for assistance at\n"
                        + HTML_TRANSLATE_EMAIL_ADDRESS
                        + ".<br />\n"
                        + "     <br />\n"
                        + "     <b>HOW TO CREATE A NEW TRANSLATION</b><br />\n"
                        + "     <br />\n"
                        + "     Users that wish to contribute with a new translation must be fluent in\n"
                        + "     the target language, preferably native of a country where this language\n"
                        + "     is official.&nbsp; Before starting your work, please contact us at\n"
                        + HTML_TRANSLATE_EMAIL_ADDRESS
                        + ".<br />\n"
                        + "     <br />\n"
                        + "    </td>\n"
                        + /* End of column 1 (spacing) */
                        "    <td>&nbsp;&nbsp;&nbsp;</td>\n"
                        + /* Column 2 (spacing) */
                        "    <td valign=\"top\">\n"
                        + /* Start of column 3 (status) */
                        /* Start shaded right rectangle */
                        "     <table border=\"0\" cellspacing=\"1\" cellpadding=\"4\"\n"
                        + "      bgcolor=\"#b1b1b1\" width=\"270\">\n"
                        + "     <tr bgcolor=\"#EFEFEF\">\n"
                        + "      <td valign=\"top\"><br />\n"
                        + "       <b>LAST UPDATED: <font color=\"#FF0000\">"
                        + df.format(new Date())
                        + "</font><br />\n"
                        + "       <br />\n"
                        + "       To get the most recent version of a message bundle, <b>click on the\n"
                        + "       corresponding language</b> in the following list.<br />\n"
                        + "       <br />\n"
                        + "       LATEST TRANSLATIONS STATUS:</b><br />\n");
    }

    /**
     * Builds the progress table.
     */
    private void buildProgress(StringBuffer newpage, Map charsets) {
        newpage
                .append("       <table width=\"250\" border=\"0\" cellpadding=\"0\" cellspacing=\"4\">");
        List latin = (List)charsets.remove("Latin");
        newpage
                .append("       <tr>\n"
                        + "        <td colspan=\"3\" valign=\"top\">"
                        + "         <hr noshade size=\"1\">\n"
                        + "         Languages written with Latin (Basic) characters:</td>\n"
                        + "       </tr>\n" + "       <tr>\n"
                        + "        <td valign=\"top\"><a\n" + " href=\""
                        + DEFAULT_LINK + "\"\n"
                        + " target=\"_blank\"><b>English</b> (US)</a></td>\n"
                        + "        <td align=\"right\">(default)</td>\n"
                        + "        <td>en</td>\n" + "       </tr>\n");
        for (Iterator i = latin.iterator(); i.hasNext();) {
            LanguageInfo l = (LanguageInfo)i.next();
            newpage.append("       <tr>\n" + "        <td><b>" + l.getLink()
                    + "</b></td>\n" + "        <td align=\"right\">("
                    + pc.format(l.getPercentage()) + ")</td>\n"
                    + "        <td>" + l.getCode() + "</td>\n"
                    + "       </tr>\n");
        }
        for (Iterator i = charsets.entrySet().iterator(); i.hasNext();) {
            Map.Entry entry = (Map.Entry)i.next();
            String charset = (String)entry.getKey();
            List l = (List)entry.getValue();
            newpage.append("       <tr>\n"
                    + "        <td colspan=\"3\" valign=\"top\">\n"
                    + "         <hr noshade size=\"1\">\n"
                    + "         Languages written with " + charset
                    + " characters:</td>\n" + "       </tr>\n");
            for (Iterator j = l.iterator(); j.hasNext();) {
                LanguageInfo li = (LanguageInfo)j.next();
                newpage.append("       <tr>\n" + "        <td><b>"
                        + li.getLink() + "</b></td>\n"
                        + "        <td align=\"right\">("
                        + pc.format(li.getPercentage()) + ")</td>\n"
                        + "        <td>" + li.getCode() + "</td>\n"
                        + "       </tr>\n");
            }
        }
        newpage.append("       </table>\n");
    }

    /**
     * Builds the closing footers of the page.
     */
    private void buildEndOfPage(StringBuffer newpage) {
        newpage.append("      </td>\n" + "     </tr>\n" + "     </table>\n" + /*
                                                                                 * End
                                                                                 * of
                                                                                 * shaded
                                                                                 * right
                                                                                 * rectangle
                                                                                 */
        "    </td>\n" + /* End of column 3 (status) */
        "   </tr>\n" + "   </table>\n" + /*
                                             * End of the 3 columns table below
                                             * the title
                                             */
        "  </div>\n"); /* (div id="bod1") */
    }

    /**
     * This Comparator sorts charset names in a prefered order. It is consistent
     * with equals, so can be used to sort keys in a Map.
     */
    private static class CharsetNameComparator implements Comparator {
        /**
         * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
         */
        public int compare(Object arg0, Object arg1) {
            if (arg0 == arg1 || arg0.equals(arg1))
                return 0;
            final String charsetName0 = (String)arg0;
            final String charsetName1 = (String)arg1;
            // Make Latin charsets sorted before others.
            boolean has0 = charsetName0.contains("Latin");
            boolean has1 = charsetName1.contains("Latin");
            if (has0 && !has1)
                return -1;
            if (!has0 && has1)
                return 1;
            if (has0 && has1) {
                has0 = charsetName0.contains("Basic");
                has1 = charsetName1.contains("Basic");
                if (has0 && !has1)
                    return -1;
                if (!has0 && has1)
                    return 1;
                has0 = charsetName0.contains("Western");
                has1 = charsetName1.contains("Western");
                if (has0 && !has1)
                    return -1;
                if (!has0 && has1)
                    return 1;
            }
            // Then sort alphabetic charsets.
            has0 = charsetName0.contains("European");
            has1 = charsetName1.contains("European");
            if (has0 && !has1)
                return -1;
            if (!has0 && has1)
                return 1;
            if (has0 & has1)
                return charsetName0.compareTo(charsetName1);
            has0 = charsetName0.contains("Cyrillic");
            has1 = charsetName1.contains("Cyrillic");
            if (has0 && !has1)
                return -1;
            if (!has0 && has1)
                return 1;
            if (has0 & has1)
                return charsetName0.compareTo(charsetName1);
            // Then sort abjads charsets.
            has0 = charsetName0.contains("Semitic");
            has1 = charsetName1.contains("Semitic");
            if (has0 && !has1)
                return -1;
            if (!has0 && has1)
                return 1;
            if (has0 & has1)
                return charsetName0.compareTo(charsetName1);
            has0 = charsetName0.contains("Brahmic");
            has1 = charsetName1.contains("Brahmic");
            if (has0 && !has1)
                return -1;
            if (!has0 && has1)
                return 1;
            if (has0 & has1)
                return charsetName0.compareTo(charsetName1);
            // Sort the remaining complex charsets.
            return charsetName0.compareTo(charsetName1);
        }
    }
}
