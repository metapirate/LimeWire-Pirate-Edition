package org.limewire.ui.swing.tray;

import java.awt.Dimension;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EventObject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.Icon;

import org.jdesktop.application.Resource;
import org.limewire.listener.EventListener;
import org.limewire.ui.swing.util.GuiUtils;

/**
 * This class handles user notifications for platform that do not support JDIC.
 * It currently displays notifications only.
 * 
 * It contains a window manager to track and display multiple notifications on
 * the screen at one time.
 */
final class BasicNotifier implements TrayNotifier {

    private @Resource Icon notificationIcon;

    private WindowManager windowManager = new WindowManager();

    public BasicNotifier() {
        GuiUtils.assignResources(this);
    }

    public boolean supportsSystemTray() {
        return false;
    }

    public boolean showTrayIcon() {
        return false;
    }

    public void hideTrayIcon() {
    }

    public void showMessage(Notification notification) {
        windowManager.createWindow(notification);
    }

    public void hideMessage(Notification notification) {
        windowManager.hideMessage(notification);
    }

    public void updateUI() {
    }

    @Override
    public boolean isExitEvent(EventObject event) {
        return false;
    }

    private class WindowManager implements EventListener<WindowDisposedEvent> {
        private final List<NotificationWindow> notificationWindows;

        private final Map<Notification, NotificationWindow> notifications;

        public WindowManager() {
            this.notificationWindows = Collections
                    .synchronizedList(new ArrayList<NotificationWindow>());
            this.notifications = Collections
                    .synchronizedMap(new HashMap<Notification, NotificationWindow>());
        }

        public synchronized void hideMessage(Notification notification) {
            NotificationWindow notificationWindow = notifications.get(notification);
            notificationWindow.dispose();
            notifications.remove(notification);
            notificationWindows.remove(notification);
            bumpWindows();
        }

        public synchronized void createWindow(Notification notification) {
            Icon icon = notification.getIcon() != null ? notification.getIcon() : notificationIcon;
            NotificationWindow notificationWindow = new NotificationWindow(icon, notification);
            notificationWindow.addListener(this);
            notificationWindow.setLocation(getNewWindowLocation(notificationWindow, -1
                    * notificationWindow.getPreferredSize().height));
            notificationWindows.add(notificationWindow);
            
            synchronized (notificationWindows) {
                //guard to make sure not too many windows will be created
                //we could try and calculate how many windows can display 
                //on the screen as well but it is probably not worth it
                if(notificationWindows.size() > 5) {
                    disposeWindow(notificationWindows.get(0));
                }
            }
            
            bumpWindows();
        }

        private synchronized void bumpWindows() {
            int totalWindowHeight = 0;// will be relative to the system tray on
            // windows.
            for (int i = 0; i < notificationWindows.size(); i++) {
                NotificationWindow notificationWindow = notificationWindows.get(notificationWindows
                        .size()
                        - (i + 1));

                Point newLocation = getNewWindowLocation(notificationWindow, totalWindowHeight);
                notificationWindow.moveTo(newLocation);

                Dimension preferredSize = notificationWindow.getPreferredSize();
                totalWindowHeight += preferredSize.height + 10;//keep a space between windows
            }
        }

        public Point getNewWindowLocation(NotificationWindow notificationWindow, int startHeight) {
            final Rectangle screenBounds;
            final Insets screenInsets;

            Toolkit toolkit = Toolkit.getDefaultToolkit();
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            GraphicsConfiguration gc = getGraphicsConfiguration(new Point(screenSize.width - 1,
                    screenSize.height - 1));

            if (gc != null) {
                screenInsets = toolkit.getScreenInsets(gc);
                screenBounds = gc.getBounds();
            } else {
                screenInsets = new Insets(0, 0, 0, 0);
                screenBounds = new Rectangle(toolkit.getScreenSize());
            }

            final int screenWidth = screenBounds.width
                    - Math.abs(screenInsets.right);
            final int screenHeight = screenBounds.height
                    - Math.abs(screenInsets.bottom);

            Dimension preferredSize = notificationWindow.getPreferredSize();
            Point newLocation = new Point(screenWidth - preferredSize.width, screenHeight
                    - preferredSize.height - startHeight);

            return newLocation;

        }

        @Override
        public synchronized void handleEvent(WindowDisposedEvent event) {
            NotificationWindow notificationWindow = event.getNotWindow();
            disposeWindow(notificationWindow);
            bumpWindows();
        }

        private void disposeWindow(NotificationWindow notificationWindow) {
            notificationWindows.remove(notificationWindow);
            notifications.remove(notificationWindow.getNotification());
            notificationWindow.dispose();
        }
    }

    private GraphicsConfiguration getGraphicsConfiguration(Point location) {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice[] screenDevices = ge.getScreenDevices();
        for (GraphicsDevice screenDevice : screenDevices) {
            if (screenDevice.getType() == GraphicsDevice.TYPE_RASTER_SCREEN) {
                GraphicsConfiguration gc = screenDevice.getDefaultConfiguration();
                if (gc.getBounds().contains(location)) {
                    return gc;
                }
            }
        }
        return null;
    }

}
