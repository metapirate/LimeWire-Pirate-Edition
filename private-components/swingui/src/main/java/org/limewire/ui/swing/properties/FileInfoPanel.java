package org.limewire.ui.swing.properties;

import javax.swing.JComponent;

import org.limewire.core.api.library.PropertiableFile;
import org.limewire.ui.swing.components.Disposable;

/**
 * A subPanel that displays information about a given file.
 */
public interface FileInfoPanel extends Disposable {

    /**
     * Returns the Component for this panel.
     */
    public JComponent getComponent();
    
    /**
     * Returns true if any information within this panel has changed
     * during the session.
     */
    public boolean hasChanged();
    
    /**
     * Saves any information that may have changed state.
     */
    public void save();
    
    /**
     * Replaces the current PropertiableFile with this one.
     */
    public void updatePropertiableFile(PropertiableFile file);
}
