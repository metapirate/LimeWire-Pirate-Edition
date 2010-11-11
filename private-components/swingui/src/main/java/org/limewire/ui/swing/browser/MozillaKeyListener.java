package org.limewire.ui.swing.browser;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import org.limewire.util.OSUtils;
import org.mozilla.browser.MozillaExecutor;
import org.mozilla.browser.MozillaKeyEvent;
import org.mozilla.browser.XPCOMUtils;
import org.mozilla.interfaces.nsIWebBrowserChrome;
import org.mozilla.interfaces.nsIWebNavigation;

/**
 * Provides key binding for forward and back navigation for
 * nsIWebBrowserChrome-based browsers.
 */
class MozillaKeyListener extends KeyAdapter {
    
    /**
     * nsIWebNavigation, like most Mozilla classes, is not thread-safe and must
     * only be set and used from the Mozilla Thread
     */
    private nsIWebNavigation nav;

    /**
     * 
     * @param chrome the browser targeted by the forward/back key bindings
     */
    public MozillaKeyListener(final nsIWebBrowserChrome chrome) {
        MozillaExecutor.mozAsyncExec(new Runnable() {
            public void run() {
                nav = XPCOMUtils.qi(chrome.getWebBrowser(), nsIWebNavigation.class);
            }
        });
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (e instanceof MozillaKeyEvent) {
            MozillaKeyEvent mozKeyEvent = (MozillaKeyEvent) e;

            boolean altDown = OSUtils.isMacOSX() ? 
                    mozKeyEvent.isMetaDown() : mozKeyEvent.isAltDown();

            boolean rightPressed = altDown && mozKeyEvent.getKeyCode() == KeyEvent.VK_RIGHT;

            boolean leftPressed = altDown && mozKeyEvent.getKeyCode() == KeyEvent.VK_LEFT;

            //back buton is pressed and focus is not inside a text control
            boolean backPressed = mozKeyEvent.getKeyCode() == KeyEvent.VK_BACK_SPACE
                    && !BrowserUtils.isTextControl(mozKeyEvent.getSourceNode());

            if ((leftPressed || backPressed)) {
                MozillaExecutor.mozAsyncExec(new Runnable() {
                    public void run() {
                        if (nav.getCanGoBack())
                            nav.goBack();
                    }
                });

            } else if (rightPressed) {
                MozillaExecutor.mozAsyncExec(new Runnable() {
                    public void run() {
                        if (nav.getCanGoForward())
                            nav.goForward();
                    }
                });
            } else if (mozKeyEvent.getKeyCode() == KeyEvent.VK_F5) {
                MozillaExecutor.mozAsyncExec(new Runnable() {
                    public void run() {
                        nav.reload(nsIWebNavigation.LOAD_FLAGS_NONE);
                    }
                }); 
            }  else if (mozKeyEvent.getKeyCode() == KeyEvent.VK_ESCAPE) {
                MozillaExecutor.mozAsyncExec(new Runnable() {
                    public void run() {
                        nav.stop(nsIWebNavigation.STOP_ALL);
                    }
                }); 
            }
            
        }
    }
    
}
