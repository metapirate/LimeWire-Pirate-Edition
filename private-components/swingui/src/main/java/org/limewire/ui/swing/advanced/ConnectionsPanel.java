package org.limewire.ui.swing.advanced;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.Timer;

import net.miginfocom.swing.MigLayout;

import org.limewire.ui.swing.advanced.connection.AddConnectionPanel;
import org.limewire.ui.swing.advanced.connection.ConnectionDetailPanel;
import org.limewire.ui.swing.advanced.connection.ConnectionSummaryPanel;
import org.limewire.ui.swing.advanced.connection.IncomingSearchPanel;

import com.google.inject.Inject;

/**
 * The Connections tab panel for the Advanced Tools window.  This displays the
 * current connections and incoming search phrases. 
 */
public class ConnectionsPanel extends TabPanel implements ActionListener {
    /** Default delay between timer events. */
    private static final int DEFAULT_DELAY = 1000;

    /** Timer to refresh connection list. */
    private Timer timer;
    
    private ConnectionSummaryPanel summaryPanel;
    private ConnectionDetailPanel detailPanel;
    private AddConnectionPanel addConnectionPanel;
    private IncomingSearchPanel incomingPanel;

    /**
     * Constructs a ConnectionsPanel with the specified component panels.
     */
    @Inject
    public ConnectionsPanel(
            ConnectionSummaryPanel summaryPanel,
            ConnectionDetailPanel detailPanel,
            AddConnectionPanel addConnectionPanel,
            IncomingSearchPanel incomingPanel) {

        this.summaryPanel = summaryPanel;
        this.detailPanel = detailPanel;
        this.addConnectionPanel = addConnectionPanel;
        this.incomingPanel = incomingPanel;
        
        initComponents();
    }
    
    /**
     * Initializes the components in the container.
     */
    private void initComponents() {
        setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        setLayout(new MigLayout("fill",
            "[left,fill]12[left,grow,fill]",        // col constraints
            "[top,fill]12[top]12[top,grow,fill]")); // row constraints

        add(summaryPanel      , "cell 0 0");
        add(detailPanel       , "cell 1 0 1 2");
        add(addConnectionPanel, "cell 0 1");
        add(incomingPanel     , "cell 0 2 2 1");
    }

    /**
     * Sets the background color for the panel.
     */
    @Override
    public void setBackground(Color bgColor) {
        super.setBackground(bgColor);
        if (summaryPanel != null) {
            summaryPanel.setBackground(bgColor);
        }
    }
    
    /**
     * Returns true if the tab content is enabled.  Always true.
     */
    @Override
    public boolean isTabEnabled() {
        return true;
    }
    
    /**
     * Performs startup tasks for the tab.  This method is called when the 
     * parent window is opened. 
     */
    @Override
    public void initData() {
        // Initialize data models.
        summaryPanel.initData();
        detailPanel.initData();
        incomingPanel.initData();
        
        // Start refresh timer.
        startTimer();
    }

    /**
     * Performs clean up tasks for the tab.  This method is called when the
     * parent window is closed.
     */
    @Override
    public void dispose() {
        // Stop refresh timer.
        stopTimer();
        
        // Clear data models.  This disposes of instances of TransformedList 
        // that are used to handle data.
        summaryPanel.dispose();
        detailPanel.dispose();
        incomingPanel.dispose();
    }

    /**
     * Handles action performed event from the timer to refresh the display.
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        summaryPanel.refresh();
        detailPanel.refresh();
    }
    
    /**
     * Starts the timer used to refresh the display.
     */
    private void startTimer() {
        // Stop current timer.
        stopTimer();
        
        // Create new timer, and start.
        timer = new Timer(DEFAULT_DELAY, this);
        timer.start();
    }
    
    /**
     * Stops the timer used to refresh the display.
     */
    private void stopTimer() {
        if (timer != null) {
            timer.stop();
            timer.removeActionListener(this);
            timer = null;
        }
    }
}
