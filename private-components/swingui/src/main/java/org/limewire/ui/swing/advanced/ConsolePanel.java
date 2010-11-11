package org.limewire.ui.swing.advanced;

import java.awt.BorderLayout;
import java.awt.Color;

import javax.swing.BorderFactory;
import javax.swing.JLabel;

import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.LogUtils;

import com.google.inject.Inject;

/**
 * An Advanced Tools tab panel that displays the console.
 */
public class ConsolePanel extends TabPanel {
    
    private BorderLayout panelLayout = new BorderLayout();
    private JLabel naLabel = new JLabel();
    
    private Console console;

    /**
     * Constructs a ConsolePanel with the specified Console component. 
     */
    @Inject
    public ConsolePanel(Console console) {
        this.console = console;
        initComponents();
    }
    
    /**
     * Initializes the components in the container.
     */
    private void initComponents() {
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        setLayout(panelLayout);
        
        if (LogUtils.isLog4JAvailable()) {
            console.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
            add(console, BorderLayout.CENTER);
            
        } else {
            naLabel.setText(I18n.tr("Console not available"));
            naLabel.setHorizontalAlignment(JLabel.CENTER);
            naLabel.setVerticalAlignment(JLabel.CENTER);
            add(naLabel, BorderLayout.CENTER);
        }
    }
    
    /**
     * Sets the background color for the panel.
     */
    @Override
    public void setBackground(Color bgColor) {
        super.setBackground(bgColor);
        if (console != null) {
            console.setBackground(bgColor);
        }
    }
    
    @Override
    public boolean isTabEnabled() {
        return true;
    }

    @Override
    public void initData() {
        console.attachLogs();
    }

    @Override
    public void dispose() {
        console.removeLogs();
    }
}
