package org.limewire.ui.swing.components;

/**
 * Listens for the disposal of a DisposalListenerList.
 *
 */
public interface DisposalListener {
    
    /**
     * 
     * @param source the DisposalListenerList being disposed
     */
    public void objectDisposed(Disposable source);

}
