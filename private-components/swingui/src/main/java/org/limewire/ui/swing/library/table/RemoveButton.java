package org.limewire.ui.swing.library.table;

import javax.swing.Icon;

import org.jdesktop.application.Resource;
import org.limewire.ui.swing.components.IconButton;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

public class RemoveButton extends IconButton {

    @Resource Icon removeIcon;
    @Resource Icon removeIconRollover;
    @Resource Icon removeIconPressed;
    
    public RemoveButton() {
        GuiUtils.assignResources(this);
        
        setIcon(removeIcon);
        setRolloverIcon(removeIconRollover);
        setPressedIcon(removeIconPressed);
        setToolTipText(I18n.tr("Remove this file from this list"));
    }
}
