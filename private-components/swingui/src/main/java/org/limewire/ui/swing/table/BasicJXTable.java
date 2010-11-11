package org.limewire.ui.swing.table;

import java.awt.event.KeyEvent;
import java.awt.event.MouseListener;
import java.util.Vector;

import javax.swing.Action;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

import org.jdesktop.swingx.JXTable;

/**
 * A JXTable with some default functionality removed.
 * Specifically, this removes the 'Find' feature from the actionMap
 * and changes the 'enter' key to not move down.
 */
public class BasicJXTable extends JXTable {

    // DO NOT INITIALIZE proxyMouseListener!!!!!
    // It is used in addMouseListener BEFORE the constuctor is run
    // (called from the base class constructor). If it is set here
    // or in the constructor, it will clobber the value set by the
    // call from the base class constructor.
    private AquaMouseListenerProxy proxyMouseListener;

    public BasicJXTable() {
        super();
        initialize();
    }

    public BasicJXTable(int numRows, int numColumns) {
        super(numRows, numColumns);
        initialize();
    }

    public BasicJXTable(Object[][] rowData, Object[] columnNames) {
        super(rowData, columnNames);
        initialize();
    }

    public BasicJXTable(TableModel dm, TableColumnModel cm, ListSelectionModel sm) {
        super(dm, cm, sm);
        initialize();
    }

    public BasicJXTable(TableModel dm, TableColumnModel cm) {
        super(dm, cm);
        initialize();
    }

    public BasicJXTable(TableModel dm) {
        super(dm);
        initialize();
    }

    public BasicJXTable(Vector rowData, Vector columnNames) {
        super(rowData, columnNames);
        initialize();
    }

    private void initialize() {
        getActionMap().remove("find");
        //Default java behavior for the enter key is the same as the down arrow.  We don't want this.
        setEnterKeyAction(null);
    }

    /**
     * This method has been overridden so that Apple's Aqua mouse listener objects are wrapped
     * by a proxy before being added as a mouse listener. This is to prevent a bug with Aqua
     *  which causes items to be deselected when a user simulates a right mouse click with 
     *  a CTRL click. See AquaMouseListenerProxy for more information.
     */
    @Override
    public synchronized void addMouseListener (MouseListener mouseListener) {
        if ((proxyMouseListener == null) && AquaMouseListenerProxy.isAquaMouseListener(mouseListener)) {
            proxyMouseListener = new AquaMouseListenerProxy(mouseListener);
            super.addMouseListener(proxyMouseListener);
        } else { 
            super.addMouseListener(mouseListener);
        }
    }
    
    @Override
    public synchronized void removeMouseListener (MouseListener mouseListener) {
        if ((proxyMouseListener != null) && (mouseListener == proxyMouseListener.getPeer())) {
            super.removeMouseListener(proxyMouseListener);
            proxyMouseListener = null;
        } else {
            super.removeMouseListener(mouseListener);
        }
    }
    
    /**
     * @param action the action that occurs when the user presses the enter key on the table
     */
    public void setEnterKeyAction(Action action){
        getInputMap(JTable.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER,0), "launchAction");
        getActionMap().put("launchAction", action);
    }
}
