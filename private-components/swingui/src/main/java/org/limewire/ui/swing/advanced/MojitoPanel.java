package org.limewire.ui.swing.advanced;

import java.awt.BorderLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;

import org.limewire.core.api.mojito.MojitoManager;
import org.limewire.ui.swing.plugin.SwingUiPlugin;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;
import com.google.inject.name.Named;

/**
 * The Mojito tab panel for the Advanced Tools window.  This displays the
 * Arcs view for the Mojito DHT.
 */
class MojitoPanel extends TabPanel {
    
    /** Manager instance for Mojito DHT. */
    private final MojitoManager mojitoManager;
    
    /** Plugin for Mojito Arcs view component. */
    private SwingUiPlugin plugin;
    
    /** Listener to handle start event on Mojito DHT. */
    private PropertyChangeListener dhtListener;
    
    /** Indicator that determines if DHT has started. */
    private boolean dhtStarted;
    
    private JLabel dhtLabel = new JLabel();
    private JComponent dhtRenderer;

    /**
     * Constructs a MojitoPanel using the specified MojitoManager.
     */
    @Inject
    public MojitoPanel(MojitoManager mojitoManager) {
        
        this.mojitoManager = mojitoManager;
        
        setBorder(BorderFactory.createEmptyBorder(3, 12, 12, 12));
        setLayout(new BorderLayout());

        // Install listener to request focus when tab panel is shown.  This
        // allows the Arcs view to begin handling mouse clicks immediately.
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                if (dhtRenderer != null) {
                    dhtRenderer.requestFocusInWindow();
                }
            }
        });
        
        dhtLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 3, 0));
        dhtLabel.setHorizontalAlignment(JLabel.CENTER);
    }
    
    @Inject(optional=true) void register(@Named("MojitoArcsPlugin") SwingUiPlugin mojitoPlugin) {
        this.plugin = mojitoPlugin;
    }

    /**
     * Returns true if the tab content is enabled.
     */
    @Override
    public boolean isTabEnabled() {
        // Tab enabled if plugin is installed and DHT has started.
        return ((plugin != null) && dhtStarted);
    }

    /**
     * Performs startup tasks for the tab.  This method is called when the 
     * parent window is opened. 
     */
    @Override
    public void initData() {
        // Render tab content.
        if (plugin != null) {
            renderPlugin();
        } else {
            renderNotAvailable();
        }
        
        // Initialize DHT state, and notify listeners if enabled.
        dhtStarted = mojitoManager.isRunning();
        dhtLabel.setText(mojitoManager.getName());
        fireEnabledChanged(isTabEnabled());
        
        // Add property change listener to update DHT state.
        if (dhtListener == null) {
            dhtListener = new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent e) {
                    if (MojitoManager.DHT_STARTED.equals(e.getPropertyName())) {
                        boolean wasStarted = dhtStarted;
                        
                        // Update DHT state.
                        dhtStarted = ((Boolean) e.getNewValue()).booleanValue();
                        dhtLabel.setText(mojitoManager.getName());
                        
                        // Render plugin if available and DHT just started. 
                        if (dhtStarted && !wasStarted && (plugin != null)) {
                            renderPlugin();
                        }
                        
                        // Notify listeners about enabled state.
                        fireEnabledChanged(isTabEnabled());
                    }
                }
            };
            mojitoManager.addPropertyChangeListener(dhtListener);
        }
    }

    /**
     * Displays the Mojito plugin component in the tab.  
     */
    private void renderPlugin() {
        dhtRenderer = plugin.getPluginComponent();
        if (dhtRenderer != null) {
            removeAll();
            add(dhtLabel, BorderLayout.NORTH);
            add(dhtRenderer, BorderLayout.CENTER);
        } else {
            renderNotAvailable();
        }
        plugin.startPlugin();
    }
    
    /**
     * Displays a "not available" message in the tab.
     */
    private void renderNotAvailable() {
        removeAll();
        dhtRenderer = null;
        JLabel naLabel = new JLabel();
        naLabel.setText(I18n.tr("Mojito Arcs View not available"));
        naLabel.setHorizontalAlignment(JLabel.CENTER);
        naLabel.setVerticalAlignment(JLabel.CENTER);
        add(naLabel, BorderLayout.CENTER);
    }

    /**
     * Performs clean up tasks for the tab.  This method is called when the
     * parent window is closed.
     */
    @Override
    public void dispose() {
        // Stop Mojito plugin.
        if (plugin != null) {
            plugin.stopPlugin();
        }
        
        // Remove property change listener.
        if (dhtListener != null) {
            mojitoManager.removePropertyChangeListener(dhtListener);
            dhtListener = null;
        }
    }
}
