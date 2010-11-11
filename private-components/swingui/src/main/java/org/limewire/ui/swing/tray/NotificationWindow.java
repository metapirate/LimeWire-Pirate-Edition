package org.limewire.ui.swing.tray;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.StringTokenizer;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JRadioButton;
import javax.swing.JWindow;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXPanel;
import org.limewire.listener.EventListener;
import org.limewire.listener.EventListenerList;
import org.limewire.listener.ListenerSupport;
import org.limewire.ui.swing.animate.AnimatorEvent;
import org.limewire.ui.swing.animate.FadeInOutAnimator;
import org.limewire.ui.swing.animate.MoveAnimator;
import org.limewire.ui.swing.util.FontUtils;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.util.StringUtils;
import org.limewire.util.SystemUtils;

/**
 * Notification window for system messages. This class handles drawing the
 * window and kickstarts the animation.
 */
class NotificationWindow extends JWindow implements ListenerSupport<WindowDisposedEvent> {
    private final Notification notification;

    private final EventListenerList<WindowDisposedEvent> eventListenerList;

    @Resource
    private Icon trayNotifyClose;

    @Resource
    private Icon trayNotifyCloseRollover;

    @Resource
    private Font titleFont;

    @Resource
    private Color titleFontColor;

    @Resource
    private Font bodyFont;

    @Resource
    private Color bodyFontColor;

    @Resource
    private Font linkFont;

    @Resource
    private Color linkFontColor;

    @Resource
    private Color backgroundColor;

    @Resource
    private Color borderColour;

    private MoveAnimator currentMoveAnimator;

    public NotificationWindow(Icon icon, final Notification notification) {
        eventListenerList = new EventListenerList<WindowDisposedEvent>();
        GuiUtils.assignResources(this);
        this.notification = notification;

        setAlwaysOnTop(true);

        SystemUtils.setWindowTopMost(this);

        FadeInOutAnimator fadeInOutAnimator = new FadeInOutAnimator(this, 500, 2500, 500);
        fadeInOutAnimator.addListener(new EventListener<AnimatorEvent<JWindow>>() {
            @Override
            public void handleEvent(AnimatorEvent event) {
                if (event.getType() == AnimatorEvent.Type.STOPPED) {
                    eventListenerList.broadcast(new WindowDisposedEvent(NotificationWindow.this));
                }
            }
        });

        JXPanel panel = new JXPanel(new MigLayout("fill, gap 0px 0px, insets 0 5 5 0"));
        add(panel);

        panel.setBackground(backgroundColor);
        panel.setBorder(BorderFactory.createLineBorder(borderColour, 2));

        PerformNotificationActionsMouseListener performNotificationActionsMouseListener = new PerformNotificationActionsMouseListener();
        panel.addMouseListener(performNotificationActionsMouseListener);

        final JRadioButton closeButton = new JRadioButton(trayNotifyClose);
        closeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                eventListenerList.broadcast(new WindowDisposedEvent(NotificationWindow.this));
            }
        });

        closeButton.addMouseListener(new HoverButtonMouseListener(closeButton, trayNotifyClose,
                trayNotifyCloseRollover));

        String titleLine1 = " ";// keeping at least a space so icon in JLabel
                                // renders properly
        String titleLine2 = "";

        if (notification.getTitle() != null) {
            StringTokenizer title = new StringTokenizer(notification.getTitle(), " \t\n\r");
            StringBuffer titleBuffer1 = new StringBuffer();
            StringBuffer remainingMessage = buildLine(titleBuffer1, title, titleFont, 150);
            titleLine1 = titleBuffer1.toString().trim();
            titleLine2 = FontUtils.getTruncatedMessage(remainingMessage.toString().trim(), titleFont, 180);
        }

        JLabel titleLabel1 = new JLabel(titleLine1);
        titleLabel1.setIcon(icon);
        titleLabel1.setIconTextGap(3);
        titleLabel1.setFont(titleFont);
        titleLabel1.setForeground(titleFontColor);

        JLabel titleLabel2 = new JLabel(titleLine2);
        titleLabel2.setFont(titleFont);
        titleLabel2.setForeground(titleFontColor);

        titleLabel1.addMouseListener(performNotificationActionsMouseListener);
        titleLabel2.addMouseMotionListener(performNotificationActionsMouseListener);

        StringTokenizer message = new StringTokenizer(notification.getMessage(), " \t\n\r");
        StringBuffer messageBuffer1 = new StringBuffer();
        StringBuffer remainingMessage = buildLine(messageBuffer1, message, bodyFont, 180);

        String messageLine1 = messageBuffer1.toString().trim();
        String messageLine2 = FontUtils.getTruncatedMessage(remainingMessage.toString().trim(), bodyFont, 180);

        JLabel messageLabel1 = new JLabel(messageLine1);
        messageLabel1.setFont(bodyFont);
        messageLabel1.setForeground(bodyFontColor);
        JLabel messageLabel2 = new JLabel(messageLine2);
        messageLabel2.setFont(bodyFont);
        messageLabel2.setForeground(bodyFontColor);

        messageLabel1.addMouseListener(performNotificationActionsMouseListener);
        messageLabel2.addMouseListener(performNotificationActionsMouseListener);

        // adding components to panel
        panel.add(titleLabel1, "aligny top, gaptop 5");

        panel.add(closeButton, "alignx right, aligny top, wrap");

        if (!StringUtils.isEmpty(titleLine2)) {
            panel.add(titleLabel2, "spanx 2, wrap");
        }

        panel.add(messageLabel1, "spanx 2, wrap, gaptop 6");

        if (!StringUtils.isEmpty(messageLine2)) {
            panel.add(messageLabel2, "spanx 2, wrap");
        }

        // if actions are available add a launchable link
        if (notification.getActions() != null && notification.getActions().length > 0) {
            String launchLinkHtml = "<html><u>" + notification.getActionName() + "</u></html>";
            JLabel launchLink = new JLabel(launchLinkHtml);
            launchLink.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            launchLink.setFont(linkFont);
            launchLink.setForeground(linkFontColor);
            launchLink.addMouseListener(performNotificationActionsMouseListener);
            panel.add(launchLink, "spanx 2, alignx right, aligny bottom, gaptop 6,  gapright 5");
        }
        setPreferredSize(new Dimension(204, 97));
        pack();

        fadeInOutAnimator.start();
    }

    private class PerformNotificationActionsMouseListener extends MouseAdapter {
        @Override
        public void mouseClicked(MouseEvent e) {
            performActions();
        }
    }

    private void performActions() {
        if (notification.getActions() != null) {
            for (Action action : notification.getActions()) {
                action.actionPerformed(new ActionEvent(NotificationWindow.this,
                        ActionEvent.ACTION_PERFORMED, "Message Clicked"));
            }
        }
    }

    /**
     * Returns the notification represented by this window.
     */
    public Notification getNotification() {
        return notification;
    }

    @Override
    public void addNotify() {
        super.addNotify();
        SystemUtils.setWindowTopMost(this);
    }

    @Override
    public void addListener(EventListener<WindowDisposedEvent> listener) {
        eventListenerList.addListener(listener);
    }

    @Override
    public boolean removeListener(EventListener<WindowDisposedEvent> listener) {
        return eventListenerList.removeListener(listener);
    }

    /**
     * Adds a line of text to line StringBuffer passed in the argument. All
     * remaining text is returned in a new StringBuffer.
     */
    private StringBuffer buildLine(StringBuffer line, StringTokenizer message, Font font,
            int pixelWidth) {
        StringBuffer remaining = new StringBuffer();

        // find the first line.
        while (message.hasMoreTokens()) {
            String currentToken = message.nextToken();
            int pixels = FontUtils.getPixelWidth(line + currentToken, font);
            if (pixels < (pixelWidth)) {
                line.append(currentToken);
                if (message.hasMoreTokens()) {
                    line.append(" ");
                }
            } else {
                remaining.append(currentToken);
                if (message.hasMoreTokens()) {
                    remaining.append(" ");
                }
                break;
            }
        }

        while (message.hasMoreTokens()) {
            String currentToken = message.nextToken();
            remaining.append(currentToken);
            if (message.hasMoreTokens()) {
                remaining.append(" ");
            }
        }

        return remaining;
    }

    /**
     * Sets the rollover image for the close button when moused over.
     */
    private final static class HoverButtonMouseListener extends MouseAdapter {
        private final JRadioButton closeButton;

        private final Icon trayNotifyClose;

        private final Icon trayNotifyCloseRollover;

        private HoverButtonMouseListener(JRadioButton closeButton, Icon trayNotifyClose,
                Icon trayNotifyCloseRollover) {
            this.closeButton = closeButton;
            this.trayNotifyClose = trayNotifyClose;
            this.trayNotifyCloseRollover = trayNotifyCloseRollover;
        }

        @Override
        public void mouseEntered(MouseEvent e) {
            closeButton.setIcon(trayNotifyCloseRollover);
        }

        @Override
        public void mouseExited(MouseEvent e) {
            closeButton.setIcon(trayNotifyClose);
        }
    }

    /**
     * Moves the window from its current location to the new one.
     */
    public synchronized void moveTo(Point newLocation) {
        if (this.currentMoveAnimator != null) {
            currentMoveAnimator.stop();
        }
        MoveAnimator moveAnimator = new MoveAnimator(this, 250, newLocation);
        moveAnimator.start();
        currentMoveAnimator = moveAnimator;
    }

}