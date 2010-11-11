package org.limewire.ui.swing.search;

import java.awt.Component;
import java.awt.Dimension;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import org.limewire.ui.swing.components.ColoredBusyLabel;
import org.limewire.ui.swing.components.HeaderBar;
import org.limewire.ui.swing.components.decorators.HeaderBarDecorator;
import org.limewire.ui.swing.util.I18n;

/**
 * This panel is shown if LimeWire has not fully connected to its peers AND has not
 * received any results for the user's query.
 * It shows a "LimeWire is connecting..." message with a busy icon.
 *
 */
class AwaitingConnectionsPanel extends JPanel
{

    AwaitingConnectionsPanel(HeaderBarDecorator headerBarDecorator) {
        super( new MigLayout("hidemode 2, insets 0 0 0 0, gap 0!, novisualpadding", 
                                                                        "[]",        // col constraints
                                                                        "[][grow]") );
        
        HeaderBar header = new HeaderBar(new JLabel(""));
        header.setLayout(new MigLayout("insets 0, gap 0!, novisualpadding, alignx 100%, aligny 100%"));
        header.setAlignmentX(Component.CENTER_ALIGNMENT);
        headerBarDecorator.decorateBasic(header);
        add(header, "growx, growy, wrap");

        add(createBusyIconPanel(), "grow, wrap, aligny center, spany");       
    }
  
    private JPanel createBusyIconPanel() {
        JPanel innerPanel = new JPanel();
        innerPanel.setLayout(new BoxLayout(innerPanel, BoxLayout.Y_AXIS));
        
        innerPanel.add(Box.createVerticalGlue());
        
        ColoredBusyLabel busyLabel = new ColoredBusyLabel(new Dimension(40, 40));
        busyLabel.setBusy(true);
        busyLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        innerPanel.add(busyLabel);
        
        JLabel connectingLabel = new JLabel(I18n.tr("LimeWire is connecting..."));
        connectingLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        innerPanel.add(connectingLabel);

        innerPanel.add(Box.createVerticalGlue());
        
        innerPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        innerPanel.setAlignmentY(Component.CENTER_ALIGNMENT);

        return innerPanel;
    }
}