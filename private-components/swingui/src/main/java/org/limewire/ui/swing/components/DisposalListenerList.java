package org.limewire.ui.swing.components;

/**
 * Alerts DisposalListeners when it is disposed.
 */
public interface DisposalListenerList extends Disposable{

    public void addDisposalListener(DisposalListener listener);
    
    public void removeDisposalListener(DisposalListener listener);

}
