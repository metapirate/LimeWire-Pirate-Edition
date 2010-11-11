package org.limewire.ui.swing.statusbar;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.Timer;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXPanel;
import org.limewire.core.api.connection.ConnectionStrength;
import org.limewire.core.api.connection.GnutellaConnectionManager;
import org.limewire.ui.swing.components.HyperlinkButton;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;

class ConnectionStatusPanel extends JXPanel {
    
    private final String connectingText = I18n.tr("Connecting");
    
    private final GnutellaConnectionManager gnutellaConnectionManager;
    
    private ConnectionStrength currentStrength;
    
    /** 
     * Link to the currently running connecting "animate" timer
     *  that appends dots every 500ms to the connecting message
     */
    private Timer animateTimer = null;
    
    private JButton tryAgainButton = null;
    
    
    private final JLabel connectionStrengthLabel;
    private final JLabel connectionStatusLabel;
    
    @Resource private Icon noInternet;
    @Resource private Icon disconnected;
    @Resource private Icon connecting;
    @Resource private Icon weak;
    @Resource private Icon weakPlus;
    @Resource private Icon medium;
    @Resource private Icon mediumPlus;
    @Resource private Icon full;
    @Resource private Icon turbo;
    @Resource private Font font;

    @Inject
    ConnectionStatusPanel(GnutellaConnectionManager gnutellaConnectionManager) {
        
        this.gnutellaConnectionManager = gnutellaConnectionManager;
        
        GuiUtils.assignResources(this);
        
        this.setLayout(new BorderLayout());
        this.setOpaque(false);
        this.setBorder(BorderFactory.createEmptyBorder(0,0,0,0));
        
        connectionStrengthLabel = new JLabel();
        connectionStrengthLabel.setBorder(BorderFactory.createEmptyBorder(1,0,0,4));
        connectionStatusLabel = new JLabel();
        connectionStatusLabel.setBorder(BorderFactory.createEmptyBorder(0,0,0,4));
        connectionStatusLabel.setFont(font);
        connectionStatusLabel.setForeground(this.getForeground());
                
        this.add(this.connectionStrengthLabel,BorderLayout.WEST);
        this.add(this.connectionStatusLabel,BorderLayout.CENTER);
        
        gnutellaConnectionManager.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if(evt.getPropertyName().equals("strength")) {
                    setConnectionStrength((ConnectionStrength)evt.getNewValue());
                }
            }
        });
        
        // Assume that the program is connecting on startup until 
        //  notified otherwise -- avoids blank status or false positive on
        //  disconnected state 
        setConnectionStrength(ConnectionStrength.CONNECTING);
    }
    
    private void setConnectionStrength(ConnectionStrength strength) {
        
        currentStrength = strength;
        
        String statusMessage = "";
        String tooltipText = "";
        boolean shouldHideStatusLater = false;
        boolean showTryAgain = false;
        Icon strengthIcon = null;
        
        switch(strength) {
        case NO_INTERNET:
            statusMessage = I18n.tr("No internet connection");
            tooltipText = I18n.tr("You have no internet connection");
            strengthIcon = noInternet;
            break;
        case DISCONNECTED:
            tooltipText = I18n.tr("Couldn't connect");
            statusMessage = I18n.tr("You couldn't connect to LimeWire");
            strengthIcon = disconnected;
            showTryAgain = true;
            break;
        case CONNECTING:
            statusMessage = connectingText;
            tooltipText = I18n.tr("Connecting...");
            animateConnecting();
            strengthIcon = connecting;
            break;
        case WEAK:
            statusMessage = I18n.tr("Weak connection");
            tooltipText = I18n.tr("You have a weak connection");
            strengthIcon = weak;
            break;
         case WEAK_PLUS:
            statusMessage = I18n.tr("Weak connection");
            tooltipText = I18n.tr("You have a weak connection");
            strengthIcon = weakPlus;
            break;            
         case MEDIUM:
            statusMessage = I18n.tr("Medium connection");
            tooltipText = I18n.tr("You have a medium connection");
            strengthIcon = medium; 
            break;
         case MEDIUM_PLUS:
             statusMessage = I18n.tr("Medium connection");
             tooltipText = I18n.tr("You have a medium connection");
             strengthIcon = mediumPlus; 
             break;            
        case FULL:
            shouldHideStatusLater = true;
            statusMessage = I18n.tr("Full connection");
            tooltipText = I18n.tr("You are fully connected");
            strengthIcon = full;
            break;
        case TURBO:
            shouldHideStatusLater = true;
            statusMessage = I18n.tr("Turbo-charged connection");
            tooltipText = I18n.tr("You have a turbo-charged connection");
            strengthIcon = turbo; 
            break;
        }
               
        if (shouldHideStatusLater) {
            hideStatusLater();
        }
                    
        connectionStatusLabel.setVisible(true);
        connectionStatusLabel.setText(statusMessage);
        connectionStrengthLabel.setIcon(strengthIcon);
        
        if (tryAgainButton == null) {
            if (showTryAgain) {
                tryAgainButton = new HyperlinkButton(I18n.tr("Try Again"));
                tryAgainButton.setFont(font);
                tryAgainButton.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        gnutellaConnectionManager.restart();
                    }
                });
                this.add(this.tryAgainButton, BorderLayout.EAST);
            }
        } 
        else {
            tryAgainButton.setVisible(showTryAgain);
        }
        
        setToolTipText(tooltipText);
    }

    @Override
    public void setToolTipText(String totalText) {
        super.setToolTipText(totalText);
        connectionStatusLabel.setToolTipText(totalText);
        connectionStrengthLabel.setToolTipText(totalText);
    }
    
    private void hideStatusLater() {
        
        // We need to gaurd against hiding the status label when moving
        //  from Full or Turbo down to a lower strength by ensuring it 
        //  it is still in the same state it was when the hide was 
        //  scheduled. 
        // NOTE: Don't need to gaurd against double hides
        final ConnectionStrength initialStength = currentStrength;
        
        Timer hideSheduler = new Timer(3000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (initialStength == currentStrength) {
                    connectionStatusLabel.setVisible(false);
                }
            }
        });
        
        hideSheduler.setRepeats(false);
        hideSheduler.start();
    }
    
    private void animateConnecting() {
        
        // Gaurd against running multiple timers if the
        //  connection goes out of connecting and back
        //  quickly ( < 500ms in this case)
        if (animateTimer != null) {
            animateTimer.stop();
        }

        animateTimer = new Timer(500, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                if (currentStrength != ConnectionStrength.CONNECTING) {
                    // Dispose and stop this timer
                    animateTimer.stop();
                    animateTimer = null;
                    return;
                }
                
                int numDots = (connectionStatusLabel.getText().trim().length()
                            -  connectingText.length() + 1)
                            % 4;

                StringBuffer message = new StringBuffer(connectingText);
                // must be a nicer way of doing this. By always appending same # of .'s or ' '
                // we ensure the positioning on the status bar doesn't move while
                // in connecting mode
                for ( int i=0 ; i<4 ; i++ ) {
                    if(i < numDots)
                        message.append('.');
                    else
                        message.append(' ');
                }
                
                connectionStatusLabel.setText(message.toString());
            }
        });
        
        animateTimer.start();
    }
}
