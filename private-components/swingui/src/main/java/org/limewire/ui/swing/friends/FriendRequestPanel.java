package org.limewire.ui.swing.friends;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import org.limewire.friend.api.FriendConnectionEvent;
import org.limewire.friend.api.FriendRequest;
import org.limewire.listener.EventListener;
import org.limewire.listener.ListenerSupport;
import org.limewire.listener.SwingEDTEvent;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.components.Disposable;
import org.limewire.ui.swing.components.HyperlinkButton;
import org.limewire.ui.swing.util.BackgroundExecutorService;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;

public class FriendRequestPanel extends JPanel implements Disposable {
    
    private final List<FriendRequest> pendingRequests;
    private EventListener<FriendConnectionEvent> connectionListener;
    private ListenerSupport<FriendConnectionEvent> connectionSupport;
    
    private JLabel requestLabel;
    
    @Inject 
    public FriendRequestPanel() {
        
        GuiUtils.assignResources(this);
        
        setLayout(new MigLayout("nogrid, gap 0, insets 2 8" +
        		" 8 8, fill"));

        setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));
        
        pendingRequests = new ArrayList<FriendRequest>();
       
        JComponent yes = new HyperlinkButton(new AbstractAction(I18n.tr("Yes")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                completeRequest(true);
            }
        });
        JComponent no = new HyperlinkButton(new AbstractAction(I18n.tr("No")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                completeRequest(false);
            }
        });


        requestLabel = new JLabel();

        add(requestLabel, "growx, wrap");
        add(new JLabel(I18n.tr("Accept?")), "gapbefore push");
        add(yes);
        add(new JLabel("/"));
        add(no);
    }
     
    @Inject
    void registerListener(ListenerSupport<FriendConnectionEvent> connectionSupport) {
    
        this.connectionSupport = connectionSupport;
        
        connectionListener = new EventListener<FriendConnectionEvent>() {
            @Override
            @SwingEDTEvent
            public void handleEvent(FriendConnectionEvent event) {
                switch(event.getType()) {
                case CONNECT_FAILED:
                case DISCONNECTED:
                    close();
                }
            }
        };
        
        connectionSupport.addListener(connectionListener);
    }

    public void addRequest(FriendRequest request) {
        pendingRequests.add(request);
        ensureRequestVisible();
    }
    
    private void ensureRequestVisible() {
        if(pendingRequests.size() > 0) {
            String start = "<html><img src='" 
                + getURL("/org/limewire/ui/swing/mainframe/resources/icons/friends/friends_icon.png")
                + "' />&nbsp;";
            String end = "</html>";
            
            requestLabel.setText(start + I18n.tr("{0} wants to be your friend.",
                    pendingRequests.get(0).getFriendUsername()) + end);
        } else {
            close();
        }
    }
    
    private static String getURL(String path) {
        URL resource = FriendRequestPanel.class.getResource(path);
        return resource != null ? resource.toExternalForm() : "";
    }
    
    private void completeRequest(final boolean accept) {
        final FriendRequest request = pendingRequests.remove(0);
        BackgroundExecutorService.execute(new Runnable() {
            @Override
            public void run() {
                request.getDecisionHandler().handleDecision(request.getFriendUsername(), accept);
            }
        });
        ensureRequestVisible();
    }
        
    private void close() {
        setVisible(false);
    }

    @Override
    public void dispose() {
        connectionSupport.removeListener(connectionListener);
    }
}
