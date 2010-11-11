package org.limewire.ui.swing.components;

import java.awt.Color;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXPanel;
import org.jdesktop.swingx.painter.Painter;
import org.limewire.ui.swing.util.GuiUtils;

/**
 * Creates a Lime Message Component. Currently
 * this is painted green by default and has a triangle, chat arrow
 * attached to the bottom of it.
 */
public class MessageComponent extends JPanel {

    @Resource
    private Font headingFont;
    @Resource
    private Color fontColor;
    @Resource
    private Font subFont;
    
    /**
     * Contains the actual subComponents.
     */
    private final JXPanel messageContainer;
    
    private final JLabel arrowLabel;
    
    public MessageComponent() {
        this(18, 22, 22, 18);
    }

    public MessageComponent(int topInset, int leftInset, int bottomInset, int rightInset) {
        super(new MigLayout("insets 0 0 0 0, gap 0"));
        
        GuiUtils.assignResources(this);
        
        setOpaque(false);
        
        messageContainer = new JXPanel(new MigLayout("insets " + topInset + " " + leftInset + " " + bottomInset + " " + rightInset + ", hidemode 3"));
        messageContainer.setOpaque(false);
        
        arrowLabel = new JLabel();
        
        add(arrowLabel, "pos (messageContainer.x + 25) 0.99al");
        add(messageContainer, "wrap");
    }
    
    public void setMessageBackroundPainter(Painter painter) {
        messageContainer.setBackgroundPainter(painter);
    }
    
    public void setArrowIcon(Icon icon) {
        arrowLabel.setIcon(icon);
        setBorder(BorderFactory.createEmptyBorder(0, 0, icon.getIconHeight()-2, 0));
    }
    
    public void addComponent(JComponent component, String layout) {
        messageContainer.add(component, layout);
    }
    
    public void decorateHeaderLabel(JComponent component) {
        component.setFont(headingFont);
        component.setForeground(fontColor);
    }
    
    public void decorateHeaderLink(HyperlinkButton link) {
        link.setFont(headingFont);
    }
    
    public void decorateSubLabel(JLabel component) {
        component.setFont(subFont);
        component.setForeground(fontColor);
    }
    
    public void decorateSubLabel(HTMLLabel label) {
        label.setHtmlFont(subFont);
        label.setHtmlForeground(fontColor);
    }
    
    public void decorateFont(JComponent component) {
        component.setFont(subFont);
    }
}
