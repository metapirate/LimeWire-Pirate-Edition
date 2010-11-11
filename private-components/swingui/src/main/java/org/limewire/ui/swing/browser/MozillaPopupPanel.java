package org.limewire.ui.swing.browser;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JFrame;
import javax.swing.JToolBar;

import org.limewire.ui.swing.components.LimeIconInfo;
import org.limewire.ui.swing.util.NativeLaunchUtils;
import org.limewire.ui.swing.util.SwingUtils;
import org.limewire.util.OSUtils;
import org.limewire.util.SystemUtils;
import org.mozilla.browser.IMozillaWindow;
import org.mozilla.browser.MozillaPanel;
import org.mozilla.browser.impl.ChromeAdapter;

public class MozillaPopupPanel extends MozillaPanel {

    private final LimeIconInfo iconInfo = new LimeIconInfo();

    public MozillaPopupPanel(boolean attachNewBrowserOnCreation) {
        super(null, attachNewBrowserOnCreation, null, null);
        initialize();
    }

    @Override
    public void addNotify() {
        super.addNotify();
        SystemUtils.setWindowIcon(this, iconInfo.getIconFile());
    }

    @Override
    public void setContainerWindow(IMozillaWindow parentWin) {
        super.setContainerWindow(parentWin);
        if (parentWin != null && parentWin instanceof JFrame) {
            JFrame frame = (JFrame) parentWin;
            // We set the icon image on every OS except Mac OS X, because on Mac setting this doesn't actually place an icon
            // in a corner of the frame; it's used instead as an icon for the minimized application. (LWC-4726)
            if(!OSUtils.isMacOSX())
                frame.setIconImage(iconInfo.getImage());
        }
    }

    private void initialize() {
        JToolBar toolbar = getToolbar();
        toolbar.add(new AbstractAction("Out") {
            // TODO: Add a picture.
            public void actionPerformed(ActionEvent e) {
                NativeLaunchUtils.openURL(getUrl());
            }
        });
    }

    @Override
    public void onAttachBrowser(final ChromeAdapter chromeAdapter, ChromeAdapter parentChromeAdapter) {
        super.onAttachBrowser(chromeAdapter, parentChromeAdapter);
        BrowserUtils.addDomListener(chromeAdapter);
        SwingUtils.invokeNowOrLater(new Runnable() {
            public void run() {
                addKeyListener(new MozillaKeyListener(chromeAdapter));
            }
        });
    }

    @Override
    public void onDetachBrowser() {
        if (getChromeAdapter() != null) {
            BrowserUtils.removeDomListener(getChromeAdapter());
        }
        super.onDetachBrowser();
    }
}
