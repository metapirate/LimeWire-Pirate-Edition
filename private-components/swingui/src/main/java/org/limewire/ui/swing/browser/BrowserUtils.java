package org.limewire.ui.swing.browser;

import java.net.URI;
import java.net.URISyntaxException;

import org.limewire.concurrent.ManagedThread;
import org.limewire.core.api.magnet.MagnetFactory;
import org.limewire.core.api.magnet.MagnetLink;
import org.limewire.ui.swing.util.MagnetHandler;
import org.limewire.ui.swing.util.NativeLaunchUtils;
import org.limewire.util.URIUtils;
import org.mozilla.browser.XPCOMUtils;
import org.mozilla.interfaces.nsIDOMEventTarget;
import org.mozilla.interfaces.nsIDOMWindow2;
import org.mozilla.interfaces.nsIWebBrowserChrome;
import org.w3c.dom.Node;

import com.google.inject.Inject;
import com.google.inject.Provider;

public class BrowserUtils {

    private static final LimeDomListener DOM_ADAPTER = new LimeDomListener();

    /**
     * Registers a handler for URLs with target = "_blank"
     */
    @Inject
    public static void registerBlankTarget() {
        addTargetedUrlAction("_blank", new UriAction() {
            @Override
            public boolean uriClicked(final TargetedUri targetedUrl) {
                // Open url in new thread to keep Mozilla thread responsive
                new ManagedThread(new Runnable() {
                    public void run() {
                        NativeLaunchUtils.openURL(targetedUrl.getUri());
                    }
                }).start();
                return true;
            }
        });
    }

    @Inject
    public static void registerMagnetProtocol(final MagnetFactory magnetFactory,
            final Provider<MagnetHandler> magnetHandler) {
        addProcotolHandlerAction("magnet", new UriAction() {
            @Override
            public boolean uriClicked(TargetedUri targetedUrl) {
                try {
                    URI uri = URIUtils.toURI(targetedUrl.getUri());
                    MagnetLink[] magnetLinks = magnetFactory.parseMagnetLink(uri);
                    for (MagnetLink magnetLink : magnetLinks) {
                        magnetHandler.get().handleMagnet(magnetLink);
                    }
                } catch (URISyntaxException e) {
                    return false;
                }
                return true;
            }
        });
    }
    
    @Inject
    public static void registerMailToProtocol(final MagnetFactory magnetFactory) {
        addProcotolHandlerAction("mailto", new UriAction() {
            @Override
            public boolean uriClicked(final TargetedUri targetedUrl) {
             // Open url in new thread to keep Mozilla thread responsive
                new ManagedThread(new Runnable() {
                    public void run() {
                        NativeLaunchUtils.openURL(targetedUrl.getUri());
                    }
                }).start();
                return true;
            }
        });
    }

    /**
     * Adds a {@link UriAction} for the specified target. They are only invoked
     * if there is no matching protocol action.
     */
    public static void addTargetedUrlAction(String target, UriAction action) {
        DOM_ADAPTER.addTargetedUrlAction(target, action);
    }

    /**
     * Adds a {@link UriAction} for the specified URI protocol (magnet, etc..)
     */
    public static void addProcotolHandlerAction(String protocol, UriAction action) {
        DOM_ADAPTER.addProtocolHandlerAction(protocol, action);
    }

    /**
     * 
     * @return true if node is a text input, password input or textarea
     */
    static boolean isTextControl(Node node) {

        boolean isText = false;

        if ("input".equalsIgnoreCase(node.getNodeName())) {
            Node type = node.getAttributes().getNamedItem("type");
            // null, text or password are text controls. Filters out checkbox,
            // etc
            isText = type == null || "text".equalsIgnoreCase(type.getNodeValue())
                    || "password".equalsIgnoreCase(type.getNodeValue());
        } else {
            isText = "textarea".equalsIgnoreCase(node.getNodeName());
        }

        return isText;
    }

    /**
     * Adds LimeDomListener to chromeAdapter.
     */
    static void addDomListener(final nsIWebBrowserChrome chrome) {
        nsIDOMEventTarget eventTarget = XPCOMUtils.qi(chrome.getWebBrowser().getContentDOMWindow(),
                nsIDOMWindow2.class).getWindowRoot();
        // TODO: some way to listen for javascript?
        eventTarget.addEventListener("click", DOM_ADAPTER, true);
        eventTarget.addEventListener("submit", DOM_ADAPTER, true);
    }

    static void removeDomListener(final nsIWebBrowserChrome chrome) {
        nsIDOMEventTarget eventTarget = XPCOMUtils.qi(chrome.getWebBrowser().getContentDOMWindow(),
                nsIDOMWindow2.class).getWindowRoot();
        eventTarget.removeEventListener("click", DOM_ADAPTER, true);
        eventTarget.removeEventListener("submit", DOM_ADAPTER, true);
    }

}
