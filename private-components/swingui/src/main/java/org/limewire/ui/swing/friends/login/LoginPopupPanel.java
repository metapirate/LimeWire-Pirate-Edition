package org.limewire.ui.swing.friends.login;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Panel;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.border.MatteBorder;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXPanel;
import org.limewire.core.settings.FriendSettings;
import org.limewire.ui.swing.components.Disposable;
import org.limewire.ui.swing.components.PopupHeaderBar;
import org.limewire.ui.swing.components.Resizable;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.ResizeUtils;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

@Singleton
public class LoginPopupPanel extends Panel implements Resizable {

    @Resource private Dimension size;
    @Resource private Color background;
    @Resource private Color border;
    @Resource private Font titleBarFont;
    
    @Resource private Icon windowIcon;
    
    private final Provider<ServiceSelectionLoginPanel> serviceSelectionLoginPanelProvider;
    private volatile ServiceSelectionLoginPanel serviceSelectionLoginPanel;
    
    private JXPanel frame = null;
    private JPanel contentPanel = null;
    
    @Inject
    public LoginPopupPanel(Provider<ServiceSelectionLoginPanel> serviceSelectionLoginPanelProvider) {        
        this.serviceSelectionLoginPanelProvider = serviceSelectionLoginPanelProvider;
       
        GuiUtils.assignResources(this);
        
        setLayout(new BorderLayout());
        setVisible(false);
    }
    
    private void initContent() {
        frame = new JXPanel(new BorderLayout());
        frame.setPreferredSize(size);
        frame.setBackground(background);
         
        PopupHeaderBar headerBar = new PopupHeaderBar(I18n.tr("File sharing with friends"), new AbstractAction() {
            public void actionPerformed(ActionEvent arg0) {
                finished();
            }
        });
        headerBar.setIcon(windowIcon);
        headerBar.setFont(titleBarFont);
        ResizeUtils.forceHeight(headerBar, 23);
        
        contentPanel = new JPanel(new BorderLayout());
        contentPanel.setOpaque(false);
        
        frame.add(headerBar, BorderLayout.NORTH);
        frame.add(contentPanel, BorderLayout.CENTER);
        frame.setBorder(new MatteBorder(0, 3, 3, 3, border));
       
        add(frame, BorderLayout.CENTER);
    }
    
    @Override
    public void resize() {
        Rectangle parentBounds = getParent().getBounds();
        Dimension childPreferredSize = frame.getPreferredSize();
        int w = (int) childPreferredSize.getWidth();
        int h = (int) childPreferredSize.getHeight();
        setBounds((int)parentBounds.getWidth()/2-w/2,
                (int)parentBounds.getHeight()/2-h/2+20,
                w, h);
    }
    
    @Override
    public void setVisible(boolean visible) {
        if (frame == null && visible) {
            FriendSettings.EVER_OPENED_SIGN_IN_DIALOG.setValue(true);
            initContent();
            start();
            resize();
            repaint();
            validate();
        } else if (visible && !isVisible()) {
            restart();
        } else if (!visible && contentPanel != null){
            clearContentPanel();
            serviceSelectionLoginPanel = null;
        }
        super.setVisible(visible);
    }
    
    public void start() {
        serviceSelectionLoginPanel = serviceSelectionLoginPanelProvider.get();
        contentPanel.add(serviceSelectionLoginPanel, BorderLayout.CENTER);
     }
    
    public void restart() {
        clearContentPanel();
        start();
        contentPanel.repaint();
        contentPanel.validate();
    }
    
    public void finished() {
        this.setVisible(false);
    }
    
    /**
     * Sets the login component for a given service.
     */
    public void setLoginComponent(JComponent loginPanel) {
        clearContentPanel();
        contentPanel.add(loginPanel, BorderLayout.CENTER);
        loginPanel.requestFocusInWindow();
        contentPanel.repaint();
        contentPanel.validate();
    }

    private void clearContentPanel() {
        if(contentPanel != null) {
            for ( Component component : contentPanel.getComponents() ) {
                if (component instanceof Disposable) {
                    ((Disposable) component).dispose();
                }
            }
            contentPanel.removeAll();
        }
    }
    
    public ServiceSelectionLoginPanel getServiceSelectionLoginPanel() {
        return serviceSelectionLoginPanel;
    }
}
