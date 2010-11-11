package org.limewire.ui.swing.statusbar;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXButton;
import org.limewire.core.api.library.SharedFileListManager;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
class SharedFileCountPanel extends JXButton {
        
    @Resource private Font font;
    @Resource private Color foreground;
    
    private volatile int shareButtonClicked = 0;
    
    @Inject
    SharedFileCountPanel(SharedFileListManager shareListManager) {
        super(I18n.tr("Sharing {0} files", shareListManager.getSharedFileCount()));
     
        GuiUtils.assignResources(this);
        
        setFont(font);
        setForeground(foreground);
        
        addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                shareButtonClicked++;
            }
        });
    }
        
    @Inject
    void register(SharedFileListManager shareListManager) {
        shareListManager.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if(evt.getPropertyName().equals(SharedFileListManager.SHARED_FILE_COUNT)) {
                    setText(I18n.trn("Sharing {0} file", "Sharing {0} files", (Integer)evt.getNewValue()));
                }
            }
        });
    }

}
