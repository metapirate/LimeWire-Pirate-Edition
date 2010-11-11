package org.limewire.ui.swing.options;

import java.awt.Color;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;

import net.miginfocom.swing.MigLayout;

/**
 * Abstract Option panel for initializing and saving the options within the
 * panel. When constructing a specific instance of the class, make sure the option tab item
 * which may hold the panel is specifically set.
 * 
 * @see #setOptionTabItem(OptionTabItem)
 */
public abstract class OptionPanel extends JPanel {

    /**
     * This class keeps track of the results of an option panel when applied.
     */
    public static class ApplyOptionResult {
        private boolean restartRequired = false;

        /**
         * Assume by default operation will be always be successful. This is
         * required, otherwise, {@link #updateSuccessful(boolean)} will not correctly work
         */
        private boolean successful = true;

        /**
         * Configures the result of options applied
         * 
         * @param restartReq <code>true</code> if restart is required
         * @param isSuccess <code>true</code> if options were successfully
         *        applied
         */
        ApplyOptionResult(final boolean restartReq, final boolean isSuccess) {
            updateRestart(restartReq);
            updateSuccessful(isSuccess);
        }

        public void applyResult(final ApplyOptionResult applyOptions) {
            updateRestart(applyOptions.isRestartRequired());
            updateSuccessful(applyOptions.isSuccessful());
        }

        public boolean isRestartRequired() {
            return restartRequired;
        }

        public boolean isSuccessful() {
            return successful;
        }

        public void updateRestart(final boolean restartRequired) {
            this.restartRequired = isRestartRequired() || restartRequired;
        }

        public void updateSuccessful(final boolean successful) {
            this.successful = isSuccessful() && successful;
             
        }
    }

    /** The tab item which holds this option panel **/
    private OptionTabItem optionTabItem = null;
    
    public OptionPanel() {
    }

    public OptionPanel(final String title) {
        setBorder(BorderFactory.createTitledBorder(null, title,
                TitledBorder.DEFAULT_JUSTIFICATION,
                TitledBorder.DEFAULT_POSITION,
                new Font("Dialog", Font.BOLD, 12), new Color(0x313131)));
        setLayout(new MigLayout("insets 4, fill, nogrid"));
        setOpaque(false);
    }

    /**
     * Initializes the options for this panel. Listeners should not be attached
     * in this method. It will be called multiple times as the options dialog is
     * brought up. To prevent memory leaks or recreating the same components
     * this method should only setup the options and rearrange components as
     * necessary. More heavy weight tasks that should only be done once should
     * be done in the constructor.
     */
    public abstract void initOptions();

    abstract ApplyOptionResult applyOptions();

    abstract boolean hasChanged();
    
    /**
     * Sets the parent tab which holds this option panel The panel may use the
     * tab item to flip the panel, making it the active visible panel.
     * 
     * @param tab the parent option tab
     * @see OptionsDialog
     */
    
    void setOptionTabItem(OptionTabItem tab) {
        this.optionTabItem = tab;
    }
    
    OptionTabItem getOptionTabItem(){
        return this.optionTabItem;
    }
}
