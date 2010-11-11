package org.limewire.core.settings;

import org.limewire.setting.BooleanSetting;
import org.limewire.setting.CharArraySetting;
import org.limewire.setting.FloatSetting;
import org.limewire.setting.IntSetting;
import org.limewire.setting.ProbabilisticBooleanSetting;
import org.limewire.setting.StringArraySetting;
import org.limewire.setting.StringSetting;

/**
 * Settings for searches.
 */
public final class SearchSettings extends LimeProps {

    private SearchSettings() {
    }

    /**
     * Constant for the characters that are banned from search strings.
     */
    private static final char[] BAD_CHARS = { '_', '#', '!', '|', '?', '<', '>', '^', '(', ')',
            ':', ';', '/', '\\', '[', ']', '\t', '\n', '\r', '\f', // these
                                                                   // cannot be
                                                                   // last or
                                                                   // first
                                                                   // 'cause
                                                                   // they're
                                                                   // trimmed
            '{', '}',

            /* CHARACTERS THAT TURN AFTER NORMALIZATION TO BAD CHARS */

            // Characters that turn into ';'
            '\u037E', // GREEK QUESTION MARK
            '\uFE54', // SMALL SEMICOLON
            '\uFF1B', // FULLWIDTH SEMICOLON

            // Characters that turn into '!'
            '\u203C', // DOUBLE EXCLAMATION MARK
            '\u2048', // QUESTION EXCLAMATION MARK
            '\u2049', // EXCLAMATION QUESTION MARK
            '\uFE57', // SMALL EXCLAMATION MARK
            '\uFF01', // FULLWIDTH EXCLAMATION MARK

            // Characters that turn into '?'
            '\u2047', // DOUBLE QUESTION MARK
            // '\u2048', // QUESTION EXCLAMATION MARK (see '!')
            // '\u2049', // EXCLAMATION QUESTION MARK (see '!')
            '\uFE56', // SMALL QUESTION MARK
            '\uFF1F', // FULLWIDTH QUESTION MARK

            // Characters that turn into '('
            '\u207D', // SUPERSCRIPT LEFT PARENTHESIS
            '\u208D', // SUBSCRIPT LEFT PARENTHESIS
            '\u2474', // PARENTHESIZED DIGIT ONE
            '\u2475', // PARENTHESIZED DIGIT TWO
            '\u2476', // PARENTHESIZED DIGIT THREE
            '\u2477', // PARENTHESIZED DIGIT FOUR
            '\u2478', // PARENTHESIZED DIGIT FIVE
            '\u2479', // PARENTHESIZED DIGIT SIX
            '\u247A', // PARENTHESIZED DIGIT SEVEN
            '\u247B', // PARENTHESIZED DIGIT EIGHT
            '\u247C', // PARENTHESIZED DIGIT NINE
            '\u247D', // PARENTHESIZED NUMBER TEN
            '\u247E', // PARENTHESIZED NUMBER ELEVEN
            '\u247F', // PARENTHESIZED NUMBER TWELVE
            '\u2480', // PARENTHESIZED NUMBER THIRTEEN
            '\u2481', // PARENTHESIZED NUMBER FOURTEEN
            '\u2482', // PARENTHESIZED NUMBER FIFTEEN
            '\u2483', // PARENTHESIZED NUMBER SIXTEEN
            '\u2484', // PARENTHESIZED NUMBER SEVENTEEN
            '\u2485', // PARENTHESIZED NUMBER EIGHTEEN
            '\u2486', // PARENTHESIZED NUMBER NINETEEN
            '\u2487', // PARENTHESIZED NUMBER TWENTY
            '\u249C', // PARENTHESIZED LATIN SMALL LETTER A
            '\u249D', // PARENTHESIZED LATIN SMALL LETTER B
            '\u249E', // PARENTHESIZED LATIN SMALL LETTER C
            '\u249F', // PARENTHESIZED LATIN SMALL LETTER D
            '\u24A0', // PARENTHESIZED LATIN SMALL LETTER E
            '\u24A1', // PARENTHESIZED LATIN SMALL LETTER F
            '\u24A2', // PARENTHESIZED LATIN SMALL LETTER G
            '\u24A3', // PARENTHESIZED LATIN SMALL LETTER H
            '\u24A4', // PARENTHESIZED LATIN SMALL LETTER I
            '\u24A5', // PARENTHESIZED LATIN SMALL LETTER J
            '\u24A6', // PARENTHESIZED LATIN SMALL LETTER K
            '\u24A7', // PARENTHESIZED LATIN SMALL LETTER L
            '\u24A8', // PARENTHESIZED LATIN SMALL LETTER M
            '\u24A9', // PARENTHESIZED LATIN SMALL LETTER N
            '\u24AA', // PARENTHESIZED LATIN SMALL LETTER O
            '\u24AB', // PARENTHESIZED LATIN SMALL LETTER P
            '\u24AC', // PARENTHESIZED LATIN SMALL LETTER Q
            '\u24AD', // PARENTHESIZED LATIN SMALL LETTER R
            '\u24AE', // PARENTHESIZED LATIN SMALL LETTER S
            '\u24AF', // PARENTHESIZED LATIN SMALL LETTER T
            '\u24B0', // PARENTHESIZED LATIN SMALL LETTER U
            '\u24B1', // PARENTHESIZED LATIN SMALL LETTER V
            '\u24B2', // PARENTHESIZED LATIN SMALL LETTER W
            '\u24B3', // PARENTHESIZED LATIN SMALL LETTER X
            '\u24B4', // PARENTHESIZED LATIN SMALL LETTER Y
            '\u24B5', // PARENTHESIZED LATIN SMALL LETTER Z
            '\u3200', // PARENTHESIZED HANGUL KIYEOK
            '\u3201', // PARENTHESIZED HANGUL NIEUN
            '\u3202', // PARENTHESIZED HANGUL TIKEUT
            '\u3203', // PARENTHESIZED HANGUL RIEUL
            '\u3204', // PARENTHESIZED HANGUL MIEUM
            '\u3205', // PARENTHESIZED HANGUL PIEUP
            '\u3206', // PARENTHESIZED HANGUL SIOS
            '\u3207', // PARENTHESIZED HANGUL IEUNG
            '\u3208', // PARENTHESIZED HANGUL CIEUC
            '\u3209', // PARENTHESIZED HANGUL CHIEUCH
            '\u320A', // PARENTHESIZED HANGUL KHIEUKH
            '\u320B', // PARENTHESIZED HANGUL THIEUTH
            '\u320C', // PARENTHESIZED HANGUL PHIEUPH
            '\u320D', // PARENTHESIZED HANGUL HIEUH
            '\u320E', // PARENTHESIZED HANGUL KIYEOK A
            '\u320F', // PARENTHESIZED HANGUL NIEUN A
            '\u3210', // PARENTHESIZED HANGUL TIKEUT A
            '\u3211', // PARENTHESIZED HANGUL RIEUL A
            '\u3212', // PARENTHESIZED HANGUL MIEUM A
            '\u3213', // PARENTHESIZED HANGUL PIEUP A
            '\u3214', // PARENTHESIZED HANGUL SIOS A
            '\u3215', // PARENTHESIZED HANGUL IEUNG A
            '\u3216', // PARENTHESIZED HANGUL CIEUC A
            '\u3217', // PARENTHESIZED HANGUL CHIEUCH A
            '\u3218', // PARENTHESIZED HANGUL KHIEUKH A
            '\u3219', // PARENTHESIZED HANGUL THIEUTH A
            '\u321A', // PARENTHESIZED HANGUL PHIEUPH A
            '\u321B', // PARENTHESIZED HANGUL HIEUH A
            '\u321C', // PARENTHESIZED HANGUL CIEUC U
            '\u3220', // PARENTHESIZED IDEOGRAPH ONE
            '\u3221', // PARENTHESIZED IDEOGRAPH TWO
            '\u3222', // PARENTHESIZED IDEOGRAPH THREE
            '\u3223', // PARENTHESIZED IDEOGRAPH FOUR
            '\u3224', // PARENTHESIZED IDEOGRAPH FIVE
            '\u3225', // PARENTHESIZED IDEOGRAPH SIX
            '\u3226', // PARENTHESIZED IDEOGRAPH SEVEN
            '\u3227', // PARENTHESIZED IDEOGRAPH EIGHT
            '\u3228', // PARENTHESIZED IDEOGRAPH NINE
            '\u3229', // PARENTHESIZED IDEOGRAPH TEN
            '\u322A', // PARENTHESIZED IDEOGRAPH MOON
            '\u322B', // PARENTHESIZED IDEOGRAPH FIRE
            '\u322C', // PARENTHESIZED IDEOGRAPH WATER
            '\u322D', // PARENTHESIZED IDEOGRAPH WOOD
            '\u322E', // PARENTHESIZED IDEOGRAPH METAL
            '\u322F', // PARENTHESIZED IDEOGRAPH EARTH
            '\u3230', // PARENTHESIZED IDEOGRAPH SUN
            '\u3231', // PARENTHESIZED IDEOGRAPH STOCK
            '\u3232', // PARENTHESIZED IDEOGRAPH HAVE
            '\u3233', // PARENTHESIZED IDEOGRAPH SOCIETY
            '\u3234', // PARENTHESIZED IDEOGRAPH NAME
            '\u3235', // PARENTHESIZED IDEOGRAPH SPECIAL
            '\u3236', // PARENTHESIZED IDEOGRAPH FINANCIAL
            '\u3237', // PARENTHESIZED IDEOGRAPH CONGRATULATION
            '\u3238', // PARENTHESIZED IDEOGRAPH LABOR
            '\u3239', // PARENTHESIZED IDEOGRAPH REPRESENT
            '\u323A', // PARENTHESIZED IDEOGRAPH CALL
            '\u323B', // PARENTHESIZED IDEOGRAPH STUDY
            '\u323C', // PARENTHESIZED IDEOGRAPH SUPERVISE
            '\u323D', // PARENTHESIZED IDEOGRAPH ENTERPRISE
            '\u323E', // PARENTHESIZED IDEOGRAPH RESOURCE
            '\u323F', // PARENTHESIZED IDEOGRAPH ALLIANCE
            '\u3240', // PARENTHESIZED IDEOGRAPH FESTIVAL
            '\u3241', // PARENTHESIZED IDEOGRAPH REST
            '\u3242', // PARENTHESIZED IDEOGRAPH SELF
            '\u3243', // PARENTHESIZED IDEOGRAPH REACH
            '\uFE35', // PRESENTATION FORM FOR VERTICAL LEFT PARENTHESIS
            '\uFE59', // SMALL LEFT PARENTHESIS
            '\uFF08', // FULLWIDTH LEFT PARENTHESIS

            // Characters that turn into ')'
            '\u207E', // SUPERSCRIPT RIGHT PARENTHESIS
            '\u208E', // SUBSCRIPT RIGHT PARENTHESIS
            // '\u2474', // PARENTHESIZED DIGIT ONE
            // ... see '('
            // '\u3243', // PARENTHESIZED IDEOGRAPH REACH
            '\uFE36', // PRESENTATION FORM FOR VERTICAL RIGHT PARENTHESIS
            '\uFE5A', // SMALL RIGHT PARENTHESIS
            '\uFF09', // FULLWIDTH RIGHT PARENTHESIS

            // Characters that turn into '/'
            '\u2100', // ACCOUNT OF
            '\u2101', // ADDRESSED TO THE SUBJECT
            '\u2105', // CARE OF
            '\u2106', // CADA UNA
            '\uFF0F', // FULLWIDTH SOLIDUS

            // Characters that turn into '<'
            '\u226E', // NOT LESS-THAN
            '\uFE64', // SMALL LESS-THAN SIGN
            '\uFF1C', // FULLWIDTH LESS-THAN SIGN

            // Characters that turn into '>'
            '\u226F', // NOT GREATER-THAN
            '\uFE65', // SMALL GREATER-THAN SIGN
            '\uFF1E', // FULLWIDTH GREATER-THAN SIGN

            // Characters that turn into ':'
            '\u2A74', // DOUBLE COLON EQUAL
            '\uFE55', // SMALL COLON
            '\uFF1A', // FULLWIDTH COLON

            // Characters that turn into '_'
            '\uFE33', // PRESENTATION FORM FOR VERTICAL LOW LINE
            '\uFE34', // PRESENTATION FORM FOR VERTICAL WAVY LOW LINE
            '\uFE4D', // DASHED LOW LINE
            '\uFE4E', // CENTRELINE LOW LINE
            '\uFE4F', // WAVY LOW LINE
            '\uFF3F', // FULLWIDTH LOW LINE

            // Characters that turn into '{'
            '\uFE37', // PRESENTATION FORM FOR VERTICAL LEFT CURLY BRACKET
            '\uFE5B', // SMALL LEFT CURLY BRACKET
            '\uFF5B', // FULLWIDTH LEFT CURLY BRACKET

            // Characters that turn into '}'
            '\uFE38', // PRESENTATION FORM FOR VERTICAL RIGHT CURLY BRACKET
            '\uFE5C', // SMALL RIGHT CURLY BRACKET
            '\uFF5D', // FULLWIDTH RIGHT CURLY BRACKET

            // Characters that turn into '#'
            '\uFE5F', // SMALL NUMBER SIGN
            '\uFF03', // FULLWIDTH NUMBER SIGN

            // Characters that turn into '\'
            '\uFE68', // SMALL REVERSE SOLIDUS
            '\uFF3C', // FULLWIDTH REVERSE SOLIDUS

            // Characters that turn into '['
            '\uFF3B', // FULLWIDTH LEFT SQUARE BRACKET

            // Characters that turn into ']'
            '\uFF3D', // FULLWIDTH RIGHT SQUARE BRACKET

            // Characters that turn into '^'
            '\uFF3E', // FULLWIDTH CIRCUMFLEX ACCENT

            // Characters that turn into '|'
            '\uFF5C', // FULLWIDTH VERTICAL LINE
    };

    public static final int DISPLAY_JUNK_IN_PLACE = 0;

    public static final int MOVE_JUNK_TO_BOTTOM = 1;

    public static final int HIDE_JUNK = 2;

    /**
     * Setting for whether or not GUESS searching is enabled.
     */
    public static final BooleanSetting GUESS_ENABLED = FACTORY.createBooleanSetting(
            "GUESS_ENABLED", true);

    /**
     * Setting for whether or not OOB searching is enabled.
     */
    public static final BooleanSetting OOB_ENABLED = FACTORY.createBooleanSetting("OOB_ENABLED",
            true);

    /**
     * Setting for whether to force OOB searching to be enabled (for testing
     * purposes).
     */
    public static final BooleanSetting FORCE_OOB = FACTORY.createBooleanSetting("FORCE_OOB", false);

    /**
     * Setting for whether old-style OOB searching is enabled.
     */
    public static final ProbabilisticBooleanSetting DISABLE_OOB_V2
        = FACTORY.createProbabilisticBooleanSetting("DISABLE_OOB_V2_2", 0.999f);

    /**
     * Minimum success rate for OOB proxying (percentage).
     */
    public static final IntSetting OOB_SUCCESS_RATE_GREAT
        = FACTORY.createIntSetting("OOB_SUCCESS_RATE_GREAT_2", 70);

    /**
     * Minimum success rate for OOB querying (percentage).
     */
    public static final IntSetting OOB_SUCCESS_RATE_GOOD
        = FACTORY.createIntSetting("OOB_SUCCESS_RATE_GOOD_2", 50);

    /**
     * Whether to temporarily ignore addresses that send OOB responses from
     * multiple ports.
     */
    public static final BooleanSetting OOB_IGNORE_MULTIPLE_PORTS = FACTORY
            .createRemoteBooleanSetting("OOB_IGNORE_MULTIPLE_PORTS", true);

    /**
     * Whether to temporarily ignore addresses that send more results than they
     * offered.
     */
    public static final BooleanSetting OOB_IGNORE_EXCESS_RESULTS = FACTORY
            .createRemoteBooleanSetting("OOB_IGNORE_EXCESS_RESULTS", true);

    /**
     * Minimum success rate for attempting OOB occasionally (percentage).
     */
    public static final IntSetting OOB_SUCCESS_RATE_TERRIBLE = FACTORY.createRemoteIntSetting(
            "OOB_SUCCESS_RATE_TERRIBLE", 40);

    /**
     * Setting for the characters that are not allowed in search strings.
     */
    public static final CharArraySetting ILLEGAL_CHARS = FACTORY.createCharArraySetting(
            "ILLEGAL_CHARS", BAD_CHARS);

    /**
     * Setting for the maximum number of characters to allow in queries.
     */
    public static final IntSetting MAX_QUERY_LENGTH = FACTORY.createIntSetting(
            "MAX_QUERY_LENGTH_2", 256);

    /**
     * Setting for the maximum number of bytes to allow in XML queries.
     */
    public static final IntSetting MAX_XML_QUERY_LENGTH = FACTORY.createIntSetting(
            "MAX_XML_QUERY_LENGTH", 500);

    /**
     * The minimum quality (number of stars) for search results to display.
     */
    public static final IntSetting MINIMUM_SEARCH_QUALITY = FACTORY.createIntSetting(
            "MINIMUM_SEARCH_QUALITY_2", 0);

    /**
     * The minimum speed for search results to display.
     */
    public static final IntSetting MINIMUM_SEARCH_SPEED = FACTORY.createIntSetting(
            "MINIMUM_SEARCH_SPEED_2", 0);

    /**
     * Whether or not to enable the spam filter.
     */
    public static final BooleanSetting ENABLE_SPAM_FILTER = FACTORY.createBooleanSetting(
            "ENABLE_SPAM_FILTER_2", true);

    /**
     * Set how sensitive the spam filter should be.
     */
    public static final FloatSetting FILTER_SPAM_RESULTS = FACTORY.createFloatSetting(
            "FILTER_SPAM_RESULTS_2", 0.85f, 0.5f, 1.0f);

    /**
     * Whether to replace IP addresses with friendly-looking strings.
     */
    public static final BooleanSetting FRIENDLY_ADDRESS_DESCRIPTIONS = FACTORY
            .createBooleanSetting("FRIENDLY_ADDRESS_DESCRIPTIONS", true);

    /**
     * Do not issue query keys more than this often.
     */
    public static final IntSetting QUERY_KEY_DELAY = FACTORY.createRemoteIntSetting(
            "QUERY_KEY_DELAY", 500);

    public static final ProbabilisticBooleanSetting SEND_LIME_RESPONSES
        = FACTORY.createProbabilisticBooleanSetting("SEND_LIME_RESPONSES_2", 0.999f);

    public static final ProbabilisticBooleanSetting PUBLISH_LIME_KEYWORDS = FACTORY
            .createRemoteProbabilisticBooleanSetting("PUBLISH_LIME_KEYWORDS", 1.0f);

    public static final StringArraySetting LIME_SEARCH_TERMS = FACTORY
            .createRemoteStringArraySetting("LIME_SEARCH_TERMS", new String[] { "limewire" });

    public static final StringSetting LIME_SIGNED_RESPONSE = FACTORY
            .createRemoteStringSetting(
                    "LIME_SIGNED_RESPONSE",
                    "VTWQABLTOIACAY3PNUXGY2LNMVTXE33VOAXGO3TVORSWY3DBFZ2XI2LMFZCGC5DBD4HW4LDZA65LCAQAAFNQABDEMF2GC5AAAJNUE6DQOVZAAAS3IKWPGF7YAYEFJYACAAAHQ4AAAAAUMAOKDBAD2GNL77776777"
                            + "77776AAEAAAEY2LNMVLWS4TFEBIFETZAIF3GC2LMMFRGYZJAMF2CATDJNVSVO2LSMUXGG33NAAAEYSKNIUCDYONUAAAMHASCJBAMGASTIJAIGU2JI5XDALACCR5UR6XTYJEZVCPOYJWXZXF2ESOLUKXMM4BBIFF5"
                            + "T7EFWL6YYKMY3SK65A6WH5DA53GIAPB7PBWWYIDWMVZHG2LPNY6SEMJOGARD6PR4MF2WI2LPOMQHQ43JHJXG6TTBNVSXG4DBMNSVGY3IMVWWCTDPMNQXI2LPNY6SE2DUORYDULZPO53XOLTMNFWWK53JOJSS4Y3P"
                            + "NUXXGY3IMVWWC4ZPMF2WI2LPFZ4HGZBCHY6GC5LENFXSAYLDORUW63R5EJUHI5DQHIXS653XO4XGY2LNMV3WS4TFFZRW63JPOVYGIYLUMU7WS3TDNRUWK3TUEIQGS3TEMV4D2IRQEIXT4PBPMF2WI2LPOM7AAAAA"
                            + "AAAAAAAAAAAAAAAAAAAAAAA");

    public static final StringArraySetting LIME_QRP_ENTRIES
        = FACTORY.createStringArraySetting("LIME_QRP_ENTRIES_2", new String[] {
                "lime", "wire", "limewire", "pro", "limewirepro" });

    /**
     * Whether the user wishes to receive results for partial files.
     */
    public static final BooleanSetting DESIRES_PARTIAL_RESULTS = FACTORY.createBooleanSetting(
            "DESIRES_PARTIAL_RESULTS", true);

    /**
     * Whether client side is enabled at all.
     */
    public static final BooleanSetting DESIRES_PARTIAL_RESULTS_REMOTE = FACTORY
            .createRemoteBooleanSetting("DESIRES_PARTIAL_RESULTS_REMOTE_BETA", true);

    public static boolean desiresPartialResults() {
        return DESIRES_PARTIAL_RESULTS.getValue() && DESIRES_PARTIAL_RESULTS_REMOTE.getValue();
    }

    /**
     * Whether or not to include metadata in plaintext searches.
     */
    public static final BooleanSetting INCLUDE_METADATA_IN_PLAINTEXT_SEARCH = FACTORY
            .createRemoteBooleanSetting("INCLUDE_METADATA_IN_PLAINTEXT_SEARCH", true);
    
    /**
     * Setting to turn on whether client is interested in non-metadata
     * sha1 urns for responses in query replies.
     */
    public static final BooleanSetting DESIRES_NMS1_URNS = 
        FACTORY.createBooleanSetting("DESIRES_NMS1_URNS", false);
    
    /**
     * Aims to reduce the number of non torrent responses generated from old clients when doing a torrent category search
     *  by appending a torrent search term to the query to match on the extension.
     */
    public static final BooleanSetting APPEND_TORRENT_TO_TORRENT_QUERIES =
        FACTORY.createRemoteBooleanSetting("APPEND_TORRENT_TO_TORRENT_QUERIES", true);
    
    /**
     * Whether torrent scraping should be used or not.  If false scrape requests will be ignored.
     * 
     * TODO: hide search columns...
     */
    public static final BooleanSetting USE_TORRENT_SCRAPER =
        FACTORY.createRemoteBooleanSetting("USE_TORRENT_SCRAPER", true);
    
    /**
     * Whether torrent web search should be used or not.
     */
    public static final BooleanSetting USE_TORRENT_WEB_SEARCH =
        FACTORY.createRemoteBooleanSetting("USE_TORRENT_WEB_SEARCH", true);
    
    /**
     * Torrent websearch uri.
     */
    public static final StringSetting TORRENT_WEB_SEARCH_URI_TEMPLATE =
        FACTORY.createRemoteStringSetting("TORRENT_WEB_SEARCH_URI_TEMPLATE", "http://ajax.googleapis.com/ajax/services/search/web?v=1.0&rsz=small&safe=off&q={0}%20filetype%3Atorrent");
}
