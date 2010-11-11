package org.limewire.ui.swing.friends.chat;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

class URLWrapper {
    private static Pattern REGEX;
    
    public static boolean isURL(String input) {
        Matcher matcher = getRegex().matcher(input.toLowerCase());
        return matcher.matches();
    }
    
    public static String createAnchorTag(String url, String text) {
        StringBuilder bldr = new StringBuilder();
        bldr.append("<a href=\"");
        if (url.matches("magnet://.*")) {
            //no-op guard to prevent appending http://
        } else if (!url.matches("http[s]?://.*")) {
            bldr.append("http://");
        }
        bldr.append(url).append("\">");
        bldr.append(text);
        bldr.append("</a>");
        return bldr.toString();
    }
    
    private static Pattern getRegex() {
        if (REGEX == null) {
            REGEX = buildRegex();
        }
        return REGEX;
    }

    private static Pattern buildRegex() {
      //This regular expression was borrowed from Mastering Regular Expressions (2nd Edition)
        String SubDomain  = "(?i:[a-z0-9]|[a-z0-9][-a-z0-9]*[a-z0-9])";
        String TopDomains = "(?x-i:com\\b         \n" +
                            "     |aero\\b        \n" +    
                            "     |asia\\b        \n" +
                            "     |biz\\b         \n" +
                            "     |cat\\b         \n" +
                            "     |coop\\b        \n" +
                            "     |edu\\b         \n" +
                            "     |gov\\b         \n" +
                            "     |in(?:t|fo)\\b  \n" +
                            "     |jobs\\b        \n" +
                            "     |mil\\b         \n" +
                            "     |mobi\\b        \n" +
                            "     |museum\\b      \n" +
                            "     |name\\b        \n" +
                            "     |net\\b         \n" +
                            "     |org\\b         \n" +
                            "     |pro\\b         \n" +
                            "     |tel\\b         \n" +
                            "     |travel\\b      \n" +
                            "     |[a-z][a-z]\\b  \n" + // country codes
                            ")                    \n";
        String Hostname = "(?:" + SubDomain + "\\.)+" + TopDomains;
        
        String NOT_IN   = ";\"'<>()\\[\\]\\{\\}\\s\\x7F-\\xFF";
        String NOT_END  = ".,?";
        String ANYWHERE = "[^" + NOT_IN + NOT_END + "]";
        String EMBEDDED = "[" + NOT_END + "]";
        String UrlPath  = "/"+ANYWHERE + "*("+EMBEDDED+"+"+ANYWHERE+"+)*";
        
        String Url =
          "(?x:                                                  \n"+
          "  \\b                                                 \n"+
          "  ## match the hostname part                          \n"+
          "  (                                                   \n"+
          "     (?: magnet | ftp | http s? ): // [-\\w]+(\\.\\w[-\\w]*)+  \n"+
          "   |                                                  \n"+
          "     " + Hostname + "                                 \n"+
          "  )                                                   \n"+
          "  # allow optional port                               \n"+
          "  (?: \\d+ )?                                         \n"+
          "                                                      \n"+
          "  # rest of url is optional, and begins with /        \n"+
          "  (?: " + UrlPath + ")?                               \n"+
          ")";
          
        return Pattern.compile(Url);
    }
}
