package org.limewire.ui.swing.wizard;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.jdesktop.swingx.JXButton;
import org.limewire.ui.swing.action.UrlAction;
import org.limewire.ui.swing.components.HyperlinkButton;
import org.limewire.ui.swing.components.MultiLineLabel;
import org.limewire.ui.swing.components.ToggleExtenderListener;
import org.limewire.ui.swing.options.LearnMoreButton;
import org.limewire.ui.swing.util.I18n;

public abstract class WizardPage extends JPanel {
    
    private final SetupComponentDecorator decorator;
    
    public WizardPage(SetupComponentDecorator decorator) {
        this.decorator = decorator;
    }
    
    public abstract void applySettings();
    public abstract String getLine1();
    public abstract String getLine2();
    public abstract String getFooter();
    
    protected String getForwardButtonText() {
        return I18n.tr("Continue");
    }
    
    protected boolean hasBackButton() {
        return true;
    }
    
    protected JLabel createAndDecorateHeader(String text) {
        JLabel label = new JLabel("<html>"+text+"</html>");
        decorator.decorateHeadingText(label);
        return label;
    }
    
    protected JLabel createAndDecorateMultiLine(String text, JCheckBox checkBox) {
        JLabel label = new MultiLineLabel(text, 500);
        label.addMouseListener(new ToggleExtenderListener(checkBox));
        decorator.decorateNormalText(label); 
        return label;
    }
    
    protected JLabel createAndDecorateMultiLine(String text) {
        JLabel label = new MultiLineLabel(text, 500);
        decorator.decorateNormalText(label);
        return label;
    }
    
    protected JLabel createAndDecorateLabel(String text) {
        JLabel label = new JLabel(text);
        decorator.decorateNormalText(label);
        return label;
    }
    
    protected JLabel createAndDecorateSubHeading(String text) {
        JLabel label = new MultiLineLabel(text, 500);
        decorator.decorateSubHeading(label);
        return label;
    }

    protected JCheckBox createAndDecorateCheckBox(boolean isSelected) {
        JCheckBox checkBox = new JCheckBox();
        checkBox.setSelected(isSelected);
        decorator.decorateLargeCheckBox(checkBox);
        return checkBox;
    }
    
    protected HyperlinkButton createAndDecorateHyperlink(final String url) {
        HyperlinkButton learnMoreButton = new LearnMoreButton(url);
        decorator.decorateLink(learnMoreButton);
        return learnMoreButton;
    }

    protected HyperlinkButton createAndDecorateHyperlink(final String url, String text) {
        UrlAction urlAction = new UrlAction(text, url);
        HyperlinkButton hyperlinkButton = new HyperlinkButton(urlAction);
        decorator.decorateLink(hyperlinkButton);
        return hyperlinkButton;
    }
    
    protected JXButton createAndDecorateButton(String text) {
        JXButton button = new JXButton(text);
        decorator.decorateGreyButton(button);
        return button;
    }
}
