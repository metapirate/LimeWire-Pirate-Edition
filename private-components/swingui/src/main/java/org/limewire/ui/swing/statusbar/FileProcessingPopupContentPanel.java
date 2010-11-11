package org.limewire.ui.swing.statusbar;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.limewire.core.api.library.FileProcessingEvent;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.listener.EventListener;
import org.limewire.listener.SwingEDTEvent;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.components.Disposable;
import org.limewire.ui.swing.components.HyperlinkButton;
import org.limewire.ui.swing.components.PopupHeaderBar;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.IconManager;
import org.limewire.ui.swing.util.PainterUtils;
import org.limewire.util.FileUtils;

import com.google.inject.Inject;

public class FileProcessingPopupContentPanel extends JPanel implements Disposable {

    @Resource private Color border = PainterUtils.TRANSPARENT;
    @Resource private Font font;
    @Resource private Color foreground;
    
    private final LibraryManager libraryManager;
    
    private final JLabel processingLine;
    private final HyperlinkButton stopButton;
    private EventListener<FileProcessingEvent> listener;
    
    @Inject
    public FileProcessingPopupContentPanel(final LibraryManager libraryManager) {
        super(new BorderLayout());
        
        this.libraryManager = libraryManager;
        
        GuiUtils.assignResources(this);
        
        add(new PopupHeaderBar(I18n.tr("Adding Files"), new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
            }
        }), BorderLayout.NORTH);
        
        setBorder(BorderFactory.createMatteBorder(1, 1, 0, 1, border));
        
        processingLine = new JLabel(I18n.tr("Scanning..."));
        stopButton = new HyperlinkButton(new AbstractAction(I18n.tr("stop")){
            @Override
            public void actionPerformed(ActionEvent e) {
                libraryManager.getLibraryManagedList().cancelPendingTasks();
            }
        });
        
        processingLine.setFont(font);
        processingLine.setForeground(foreground);
        processingLine.setMaximumSize(new Dimension(140, 40));
        stopButton.setFont(font);
        
        JPanel bottomPanel = new JPanel(new MigLayout("insets 0 6 2 6, gap 5, fill"));
        bottomPanel.add(processingLine);
        bottomPanel.add(stopButton, "gapbefore push");
        add(bottomPanel, BorderLayout.CENTER);
        
    }
    
    @Inject
    void register(final IconManager iconManager) {
        listener = new EventListener<FileProcessingEvent>() {
            @Override
            @SwingEDTEvent
            public void handleEvent(final FileProcessingEvent event) {
                switch (event.getType()) {
                case PROCESSING:
                    String fileName = event.getSource().getName();
                    processingLine.setText(fileName);
                    processingLine.setToolTipText(fileName);
                    processingLine.setIcon(iconManager.getIconForExtension(FileUtils.getFileExtension(event.getSource().getName())));
                    stopButton.setVisible(true);
                    break;
                }
            }
        };
        
        libraryManager.getLibraryManagedList().addFileProcessingListener(listener);
    }
    
    
    public void notifyDone() {
        stopButton.setVisible(false);
        processingLine.setText(I18n.tr("All Completed"));
        processingLine.setIcon(null);
    }
    
    @Override
    public void dispose() {
        libraryManager.getLibraryManagedList().removeFileProcessingListener(listener);
    }
}
