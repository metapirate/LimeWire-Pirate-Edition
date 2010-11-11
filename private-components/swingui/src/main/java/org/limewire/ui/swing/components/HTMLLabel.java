package org.limewire.ui.swing.components;

import java.awt.Color;
import java.awt.Font;

import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.StyleSheet;

import org.jdesktop.application.Resource;
import org.limewire.ui.swing.util.GuiUtils;

/**
 * Label that understands HTML text and allows users to add a {@link HyperlinkListener}.
 * <p>
 * A default 
 */
public class HTMLLabel extends JEditorPane {
    
    public static final Color TRANSPARENT_COLOR = new Color(0, 0, 0, 0);
    
    private StyleSheet fontStyle = null;
    
    private StyleSheet colorStyle = null;
    
    private StyleSheet linkStyle = null;
    
    private final StyleSheet mainStyle;
    
    private Font htmlFont = null;

    private Color htmlFontColor;
    
    
    @Resource
    private Color linkColor = Color.BLUE;
    
    /**
     * null if listener is not set and installed, otherwise set.
     */
    private HyperlinkListener openInNativeBrowserListener = GuiUtils.getHyperlinkListener();
    
    /**
     * Initializes the label with <code>htmlText</code>
     */
    public HTMLLabel(String htmlText) {
        super("text/html", htmlText);
        GuiUtils.assignResources(this);
        setEditable(false);
        setCaretPosition(0);
        mainStyle = ((HTMLDocument)getDocument()).getStyleSheet();
        setSelectionColor(TRANSPARENT_COLOR);
        setHtmlLinkForeground(linkColor);
        addHyperlinkListener(openInNativeBrowserListener);
        // make it mimic a JLabel
        JLabel label = new JLabel();
        setBackground(label.getBackground());
        setHtmlFont(label.getFont());
    }

    /**
     * Creates a label with no text.
     */
    public HTMLLabel() {
        this(null);
    }
    
    /**
     * Enable or disable a listener that opens hyper links that point to 
     * full URLs natively. Enabled by default.
     */
    public void setOpenUrlsNatively(boolean enable) {
        if (enable) {
            if (openInNativeBrowserListener == null) {
                openInNativeBrowserListener = GuiUtils.getHyperlinkListener();
                addHyperlinkListener(openInNativeBrowserListener);
            } else {
                // already installed
            }
        } else {
            if (openInNativeBrowserListener != null) {
                removeHyperlinkListener(openInNativeBrowserListener);
                openInNativeBrowserListener = null;
            } else {
                // not installed
            }
        }
    }
    
    /**
     * @return the font used for rendering the HTML text, or null if none
     * has been set
     */
    public Font getHtmlFont() {
        return htmlFont;
    }
    
    /**
     * Sets the font for rendering the HTML text.
     * @param font must not be null 
     */
    public void setHtmlFont(Font font) {
        htmlFont = font;
        if (fontStyle != null) {
            mainStyle.removeStyleSheet(fontStyle);
        }
        fontStyle = new StyleSheet();
        fontStyle.addRule(createCSS(font));
        mainStyle.addStyleSheet(fontStyle);
    }
    
    /**
     * Sets the foreground color of the HTML text.
     * @param color must not be null
     */
    public void setHtmlForeground(Color color) {
        htmlFontColor = color;
        if (colorStyle != null) {
            mainStyle.removeStyleSheet(colorStyle);
        }
        colorStyle = new StyleSheet();
        colorStyle.addRule(createCSS(color));
        mainStyle.addStyleSheet(colorStyle);
    }
    
    private String createCSS(Color color) {
        StringBuilder builder = new StringBuilder("body {");
        builder.append("color: ").append(GuiUtils.colorToHex(color)).append(";");
        builder.append("}");
        return builder.toString();
    }

    /**
     * @return the foreground color used for rendering HTML text, null if none
     * is set
     */
    public Color getHtmlForeground() {
        return htmlFontColor;
    }
    
    public void setHtmlLinkForeground(Color color) {
        linkColor = color;
        if (linkStyle != null) {
            mainStyle.removeStyleSheet(linkStyle);
        }
        linkStyle = new StyleSheet();
        linkStyle.addRule(createLinkCSS(color));
        mainStyle.addStyleSheet(linkStyle);
    }

    private String createLinkCSS(Color color) {
        StringBuilder builder = new StringBuilder("body a {");
        builder.append("color: ").append(GuiUtils.colorToHex(color)).append(";");
        builder.append("}");
        return builder.toString();
    }

    private String createCSS(Font font) {
        StringBuilder builder = new StringBuilder("body {");
        builder.append("font-family: ").append(font.getFamily()).append(";");
        builder.append("font-size: ").append(font.getSize()).append("pt;");
        if (font.isItalic()) {
            builder.append("font-style: italic;");
        }
        if (font.isBold()) {
            builder.append("font-weight: bold;");
        }
        builder.append("}");
        return builder.toString();
    }
}
