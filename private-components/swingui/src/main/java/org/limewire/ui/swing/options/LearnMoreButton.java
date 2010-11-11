package org.limewire.ui.swing.options;

import org.limewire.ui.swing.action.UrlAction;
import org.limewire.ui.swing.components.HyperlinkButton;
import org.limewire.ui.swing.util.I18n;

public class LearnMoreButton extends HyperlinkButton {
    
    public LearnMoreButton(String learnMoreUrl) {
        super(new UrlAction(I18n.tr("Learn more"), learnMoreUrl));
    }
}
