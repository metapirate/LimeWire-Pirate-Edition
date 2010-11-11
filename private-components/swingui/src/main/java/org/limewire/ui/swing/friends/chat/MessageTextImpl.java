package org.limewire.ui.swing.friends.chat;

import java.awt.Font;
import java.awt.FontMetrics;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import javax.swing.text.html.StyleSheet;

import org.jdesktop.application.Resource;
import org.limewire.ui.swing.util.GuiUtils;

class MessageTextImpl extends AbstractMessageImpl implements MessageText {

    private static final int MAX_LENGTH_PIXELS = 250;// smaller than chat window
                                                     // width to account for
                                                     // non-fixed width fonts

    private final String message;

    @Resource(key = "ChatInputPanel.textFont")
    private Font textFont;

    public MessageTextImpl(String senderName, String chatFriendId, Type type,
            String message) {
        super(senderName, chatFriendId, type);
        GuiUtils.assignResources(this);
        this.message = message;
    }

    @Override
    public String getMessageText() {
        return message;
    }

    @Override
    public String toString() {
        return getMessageText();
    }

    @Override
    public String format() {
        return insertBreaksAddAnchorsTags(this.message.replace("<", "&lt;").replace(">", "&gt;"));
    }

    /**
     * Takes the given string creating anchor tags for whereever it finds urls,
     * and creating wbr tags whenever a word over the MAX_LENGTH_PIXELS is
     * encountered.
     */
    private String insertBreaksAddAnchorsTags(String wrap) {
        StringTokenizer stringTokenizer = new StringTokenizer(wrap, " \n\t\r");
        StringBuffer htmlString = new StringBuffer();
        while (stringTokenizer.hasMoreTokens()) {
            String token = stringTokenizer.nextToken();
            boolean isURL = URLWrapper.isURL(token);
            StringBuffer brokenString = new StringBuffer();
            String[] brokenTokens = breakString(token);
            for (int i = 0; i < brokenTokens.length; i++) {
                brokenString.append(brokenTokens[i]);
                if (brokenTokens.length > 1) {
                    brokenString.append("<wbr>");
                }
            }
            // if the string is a url make sure to wrap it in an anchor tag
            if (isURL) {
                htmlString.append(URLWrapper.createAnchorTag(token, brokenString.toString().trim()));
            } else {
                htmlString.append(brokenString.toString());
            }

            if(stringTokenizer.hasMoreTokens()) {
                htmlString.append(" ");
            }
        }
        return htmlString.toString();
    }

    /**
     * Breaks up the given token into multiple Strings each with a maximum of
     * MAX_LENGTH_PIXELS wide.
     */
    private String[] breakString(String token) {
        // TODO update to support full string
        int maxCharacters = getMaxCharacters(token);
        List<String> brokenStrings = new ArrayList<String>();
        int index = 0;
        int length = token.length();
        while (index < length) {
            int start = index;
            int end = index + maxCharacters;
            if (end > length) {
                end = length;
            }
            String brokenString = token.substring(start, end);
            brokenStrings.add(brokenString);
            index = end;
        }
        return brokenStrings.toArray(new String[brokenStrings.size()]);
    }

    private int getMaxCharacters(String token) {
        int pixelWidth1Character = getAverageCharacterWidthInString(token, textFont);
        return (MAX_LENGTH_PIXELS / pixelWidth1Character);
    }

    /**
     * Returns the width of the message in the given font and editor kit.
     */
    private int getAverageCharacterWidthInString(String text, Font font) {
        //TODO optimize
        StyleSheet css = new StyleSheet();
        FontMetrics fontMetrics = css.getFontMetrics(font);
        int averageCharacterWidthForFont = (int)Math.ceil((fontMetrics.stringWidth(text) /(double) text.length()));
        return averageCharacterWidthForFont;
    }
}
