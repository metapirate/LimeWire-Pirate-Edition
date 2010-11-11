package org.limewire.ui.swing.table;
import java.awt.Component;


public interface TablePopupHandler {

	public void maybeShowPopup(Component component, int x, int y);
	
	public boolean isPopupShowing(int row);
	
}
