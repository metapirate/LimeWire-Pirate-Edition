package org.limewire.ui.swing.browser;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.JLabel;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.swingx.JXBusyLabel;
import org.jdesktop.swingx.JXCollapsiblePane;
import org.jdesktop.swingx.JXPanel;
import org.limewire.ui.swing.components.ColoredBusyLabel;
import org.limewire.ui.swing.painter.GenericBarPainter;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.PainterUtils;
import org.limewire.ui.swing.util.SwingUtils;
import org.limewire.util.OSUtils;
import org.mozilla.browser.MozillaException;
import org.mozilla.browser.MozillaExecutor;
import org.mozilla.browser.MozillaPanel;
import org.mozilla.browser.XPCOMUtils;
import org.mozilla.browser.impl.ChromeAdapter;
import org.mozilla.interfaces.nsIBaseWindow;
import org.mozilla.interfaces.nsIHistoryEntry;
import org.mozilla.interfaces.nsIRequest;
import org.mozilla.interfaces.nsISHistory;
import org.mozilla.interfaces.nsISupports;
import org.mozilla.interfaces.nsIURI;
import org.mozilla.interfaces.nsIWebNavigation;
import org.mozilla.interfaces.nsIWebProgress;
import org.mozilla.interfaces.nsIWebProgressListener;
import org.mozilla.xpcom.Mozilla;

/**
 * Extension to Mozilla's browser that adds the correct listeners.
 */
public class Browser extends MozillaPanel {
    
    private final Listener listener = new Listener();
    
    private volatile boolean lastRequestFailed = true;
    private volatile boolean requestInProgress;
    
    private final VisibilityMode loadStatus;
    
    private JXCollapsiblePane loadingPane;

    public Browser() {
        super();
        loadStatus = VisibilityMode.FORCED_HIDDEN;
    }

    public Browser(boolean attachNewBrowserOnCreation, VisibilityMode toolbarVisMode,
            VisibilityMode statusbarVisMode) {
        super(null, attachNewBrowserOnCreation, toolbarVisMode, statusbarVisMode);
        loadStatus = VisibilityMode.FORCED_HIDDEN;
    }

    public Browser(VisibilityMode toolbarVisMode, VisibilityMode statusbarVisMode) {
        super(toolbarVisMode, statusbarVisMode);
        loadStatus = VisibilityMode.FORCED_HIDDEN;
    }
    
    public Browser(VisibilityMode toolbarVisMode, VisibilityMode statusbarVisMode, VisibilityMode loadingMode) {
        super(toolbarVisMode, statusbarVisMode);
        this.loadStatus = loadingMode;
    }
    
    /**
     * Returns an Iterable of HistoryEntry items that can be used
     * to see what's previously been loaded in the browser.
     * 
     */
    public Iterable<HistoryEntry> getHistory(final int maxEntries, final AtomicReference<Integer> currentPosition) {
        assert !OSUtils.isMacOSX() : "The history menu is not working on Mac OS X. Calls to retrieve it go into native code and never return";

        try {
            return MozillaExecutor.mozSyncExec(new Callable<Iterable<HistoryEntry>>() {
                @Override
                public Iterable<HistoryEntry> call() throws Exception {
                    nsIWebNavigation nav = XPCOMUtils.qi(getChromeAdapter().getWebBrowser(), nsIWebNavigation.class);
                    nsISHistory history = nav.getSessionHistory();
                    List<HistoryEntry> entries = new ArrayList<HistoryEntry>(maxEntries);
                    if(history != null) {
                        for(int i = history.getCount() - 1; entries.size() < maxEntries && i >= 0; i--) {
                            nsIHistoryEntry entry = history.getEntryAtIndex(i, false);
                            entries.add(new HistoryEntry(i, entry));
                        }
                        if(currentPosition != null) {
                            currentPosition.set(history.getIndex());
                        }
                    }
                    return entries;
                }
            });
        } catch (MozillaException e) {
            return Collections.emptyList();
        }
    }
    
    /**
     * Navigates to a specific item in the history.
     */
    public void loadHistoryEntry(final HistoryEntry entry) {
        MozillaExecutor.mozAsyncExec(new Runnable() {            
            @Override
            public void run() {
                nsIWebNavigation nav = XPCOMUtils.qi(getChromeAdapter().getWebBrowser(), nsIWebNavigation.class);
                nav.gotoIndex(entry.getIndex());
            }
        });
    }
    
    @Override
    protected void createChrome() {
        super.createChrome();
        loadingPane = new JXCollapsiblePane();
        
        if(loadStatus != VisibilityMode.FORCED_HIDDEN) {
            loadingPane.setLayout(new BorderLayout());
            
            JXBusyLabel busySpinner = new ColoredBusyLabel(new Dimension(12, 12));
            
            busySpinner.setBusy(true);
            busySpinner.setOpaque(false);
            
            JLabel busyLabel = new JLabel(I18n.tr("Loading..."));
            busyLabel.setFont(new Font("DIALOG", Font.PLAIN, 12));
            busyLabel.setForeground(new Color(0x313131));
            busyLabel.setOpaque(false);
            
            JXPanel loadingPaneInner = new JXPanel(new MigLayout("insets 6, gap 4, aligny center"));
            
            loadingPaneInner.setBackgroundPainter(new GenericBarPainter<JXCollapsiblePane>(
                    new GradientPaint(0,0,new Color(0xc8c8c8),0,1, new Color(0xf9f9f9)), new Color(0x696969),
                    new Color(0xebebeb), PainterUtils.TRANSPARENT, PainterUtils.TRANSPARENT));
            
            loadingPane.setOpaque(false);
            loadingPaneInner.setOpaque(true);
            
            loadingPaneInner.add(busySpinner, "gaptop 1");
            loadingPaneInner.add(busyLabel);
            loadingPane.add(loadingPaneInner, BorderLayout.CENTER);
            
            add(loadingPane, BorderLayout.SOUTH);
        }
    }

    public boolean isRequestInProgress() {
        return requestInProgress;
    }
    
    /** Returns true if the last request is currently in progress or succeeded. */
    public boolean isLastRequestSuccessful() {
        return !lastRequestFailed;
    }

    @Override
    public void onSetTitle(String title) {
        // Don't set it!
    }

    // overridden to remove LimeDomListener
    @Override
    public void onDetachBrowser() {
        if (getChromeAdapter() != null) {
            BrowserUtils.removeDomListener(getChromeAdapter());
            getChromeAdapter().getWebBrowser().removeWebBrowserListener(listener, nsIWebProgressListener.NS_IWEBPROGRESSLISTENER_IID);
        }
        super.onDetachBrowser();
    }

    // overridden for browser initialization that can not be done earlier
    @Override
    public void onAttachBrowser(final ChromeAdapter chromeAdapter, ChromeAdapter parentChromeAdapter) {
        super.onAttachBrowser(chromeAdapter, parentChromeAdapter);
        BrowserUtils.addDomListener(chromeAdapter);
        chromeAdapter.getWebBrowser().addWebBrowserListener(listener, nsIWebProgressListener.NS_IWEBPROGRESSLISTENER_IID);
        SwingUtils.invokeNowOrLater(new Runnable() {
            @Override
            public void run() {
                addKeyListener(new MozillaKeyListener(chromeAdapter));
                addComponentListener(new VisibilityListener());
            }
        });
    }
    
    public void showLoadingPanel() {
        loadingPane.setCollapsed(false);
    }
    
    public void pageLoadStarted() {
        lastRequestFailed = false;
        requestInProgress = true;
        if(loadStatus == VisibilityMode.DEFAULT) {
            SwingUtils.invokeNowOrLater(new Runnable() {
                @Override
                public void run() {
                    loadingPane.setCollapsed(false);
                }
            });
        }
    }

    public void pageLoadStopped(boolean failed) {
        lastRequestFailed = failed;
        requestInProgress = false;
        if(loadStatus == VisibilityMode.DEFAULT) {
            SwingUtils.invokeNowOrLater(new Runnable() {
                @Override
                public void run() {
                    loadingPane.setCollapsed(true);
                }
            });
        }
    }
    

    private class VisibilityListener extends ComponentAdapter {
        @Override
        public void componentHidden(ComponentEvent e) {
            setBrowserVisibility(false);
        }

        @Override
        public void componentShown(ComponentEvent e) {
            setBrowserVisibility(true);
        }

    }

    private void setBrowserVisibility(final boolean isVisible) {
        // Mozilla is not threadsafe so most mozilla methods must be called on
        // the
        // mozilla thread. This is asynchronous so as not to tie up the EDT.
        MozillaExecutor.mozAsyncExec(new Runnable() {
            public void run() {
                ChromeAdapter chromeAdapter = getChromeAdapter();
                if (chromeAdapter != null) {
                    nsIBaseWindow baseWindow = XPCOMUtils.qi(chromeAdapter.getWebBrowser(),
                            nsIBaseWindow.class);
                    baseWindow.setVisibility(isVisible);
                }
            }
        });

    }

    private class Listener implements nsIWebProgressListener {
        @Override
        public void onLocationChange(nsIWebProgress webProgress, nsIRequest request, nsIURI location) {
        }

        @Override
        public void onProgressChange(nsIWebProgress webProgress, nsIRequest request,
                int curSelfProgress, int maxSelfProgress, int curTotalProgress, int maxTotalProgress) {

        }

        @Override
        public void onSecurityChange(nsIWebProgress webProgress, nsIRequest request, long state) {
        }

        @Override
        public void onStateChange(nsIWebProgress webProgress, nsIRequest request, long stateFlags,
                long status) {
            if ((stateFlags & nsIWebProgressListener.STATE_IS_NETWORK) != 0
                    && (stateFlags & nsIWebProgressListener.STATE_START) != 0) {
                pageLoadStarted();
            }

            if ((stateFlags & nsIWebProgressListener.STATE_IS_NETWORK) != 0
                    && (stateFlags & nsIWebProgressListener.STATE_STOP) != 0) {
                pageLoadStopped(status != 0);
            }
        }

        @Override
        public void onStatusChange(nsIWebProgress webProgress, nsIRequest request, long status,
                String message) {
            
        }

        @Override
        public nsISupports queryInterface(String iid) {
            return Mozilla.queryInterface(this, iid);
        }
    }

}
