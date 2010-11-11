package org.limewire.ui.swing.search;

import javax.swing.JLabel;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import org.limewire.ui.swing.components.MessageComponent;
import org.limewire.ui.swing.util.I18n;
import org.limewire.util.OSUtils;

public class ProgramsNotAllowedPanel extends JPanel {
    
    public ProgramsNotAllowedPanel() {
    
        super(new MigLayout("fill"));
        
        MessageComponent messageComponent = new MessageComponent(12, 22, 10, 14);
        
        JLabel title = new JLabel(I18n.tr("Downloading programs can result in viruses"));
        
        JLabel message;
            
        if (OSUtils.isMacOSX()) {
            message = new JLabel("<html>" + I18n.tr("We recommend you don't download programs" +
                    " using LimeWire. However, you can enable downloading programs via Preferences > " +
                    "Search > Configure...")+"</html>");
        }
        else {
            message = new JLabel("<html>" + I18n.tr("We recommend you don't download programs" +
        		" using LimeWire. However, you can enable downloading programs via Tools > Options > " +
        		"Search > Configure...")+"</html>");
        }
        
        messageComponent.decorateHeaderLabel(title);
        messageComponent.decorateSubLabel(message);
        messageComponent.addComponent(title, "align left, wmax 400, wrap");
        messageComponent.addComponent(message, "align center, wmax 400");
        
        this.add(messageComponent, "align 50% 40%");
    }

}
