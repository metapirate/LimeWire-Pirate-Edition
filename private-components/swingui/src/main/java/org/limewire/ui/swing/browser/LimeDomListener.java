package org.limewire.ui.swing.browser;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.utils.URIUtils;
import org.limewire.ui.swing.browser.UriAction.TargetedUri;
import org.mozilla.browser.MozillaExecutor;
import org.mozilla.browser.MozillaRuntimeException;
import org.mozilla.dom.NodeFactory;
import org.mozilla.interfaces.nsIDOMEvent;
import org.mozilla.interfaces.nsIDOMEventListener;
import org.mozilla.interfaces.nsISupports;
import org.mozilla.xpcom.Mozilla;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

/**
 * Notifies registered actions of links being clicked and if the actions
 * handle the event, it prevents the default action from happening.
 */
public class LimeDomListener implements nsIDOMEventListener {

    private static final Log LOG = LogFactory.getLog(LimeDomListener.class);

    private final Map<String, UriAction> targetActions = new ConcurrentHashMap<String, UriAction>();

    private final Map<String, UriAction> protocolActions = new ConcurrentHashMap<String, UriAction>();

    /**
     * Adds a {@link UriAction} for the specified target. They are only invoked
     * if there is no matching protocol action. Use empty string to register
     * actions for link clicks that don't have a target
     */
    public void addTargetedUrlAction(String target, UriAction action) {
        targetActions.put(target, action);
    }

    /**
     * Adds a {@link UriAction} for the specified uri protocol (magnet, etc..)
     * Protocol handlers take precedence over target handlers, so if a uri matches both,
     * only the protocol handler will be run.
     */
    void addProtocolHandlerAction(String protocol, UriAction action) {
        protocolActions.put(protocol, action);
    }

    public void handleEvent(nsIDOMEvent event) {
        assert MozillaExecutor.isMozillaThread();
        try {
            UriAction.TargetedUri targetedUri = getTargetedUri(event);
            if (targetedUri != null) {
                String protocol = targetedUri.getProtocol();
                if (protocol != null) {
                    UriAction action = protocolActions.get(protocol);
                    if (action != null) {
                        if (action.uriClicked(targetedUri)) {
                            event.preventDefault();
                            return;
                        }
                    }
                }
                String target = targetedUri.getTarget();
                    UriAction action = targetActions.get(target);
                    if (action != null) {
                        if (action.uriClicked(targetedUri)) {
                            event.preventDefault();
                            return;
                        }
                    }
                }
        } catch (MozillaRuntimeException e) {
            // This should not occur
            LOG.error("MozillaRuntimeException", e);
        }
    }

    /**
     * 
     * @return TargetedUrl if the event contains a URL, null if there is no URL.
     */
    private UriAction.TargetedUri getTargetedUri(nsIDOMEvent event) {
        UriAction.TargetedUri targetedUrl = null;
        Node node = NodeFactory.getNodeInstance(event.getTarget());
        
        if("click".equals(event.getType())) {
            if (!"html".equalsIgnoreCase(node.getNodeName())) {
                targetedUrl = getTargetedUri(node);
                if (targetedUrl == null) {
                    // also check parent node
                    targetedUrl = getTargetedUri(node.getParentNode());
                }
            }
        } else if("submit".equals(event.getType())) {
            //TODO this is a special case to only handle submits for magnet links
            //it would make sense in the future to just move all magnet link handling
            //to its own listener, to not pollute the code with this special casing 
            targetedUrl = getTargetedFormAction(node);
            if("magnet".equals(targetedUrl.getProtocol())) {
                return targetedUrl;
            } else {
                //only do this for magent links so that forms can be submitted still.
                return null;
            }
        }
        return targetedUrl;
    }

    /**
     * Crawls up the DOM for the given node finding the first form. Returning 
     * a TargetedUri populated with its action, or return null of no form or 
     * action could be found. 
     */
    private TargetedUri getTargetedFormAction(Node node) {
        if(node != null) {            
            if("form".equalsIgnoreCase(node.getNodeName())) {
                NamedNodeMap map = node.getAttributes();
                Node actionNode = map.getNamedItem("action");
                Node targetNode = map.getNamedItem("target");
                String action = actionNode != null ? actionNode.getNodeValue() : null;
                String target = targetNode != null && targetNode.getNodeValue() != null ? targetNode.getNodeValue() : "";
                
                if(action != null) {
                    return new UriAction.TargetedUri(target, action);
                }
            } else {
                return getTargetedFormAction(node.getParentNode());
            }
        }
        return null ;
    }

    /**
     * 
     * @return a TargetedUrl if the nodes attributes contain href, null if not.
     */
    private UriAction.TargetedUri getTargetedUri(Node node) {
        if (node != null) {
            NamedNodeMap map = node.getAttributes();
            if (map != null) {
                Node hrefNode = map.getNamedItem("href");
                if (hrefNode != null) {
                    String target = "";
                    URI absoluteURI = null;
                    try {
                        absoluteURI = new URI(encode(hrefNode.getNodeValue()));
                        if(!absoluteURI.isAbsolute()) {
                            //we need to pass an absolute uri to the targeted urls.
                            if(node.getBaseURI() != null) {
                                URI baseUri = new URI(encode(node.getBaseURI()));
                                URI relativeURI = new URI(encode(hrefNode.getNodeValue()));
                                absoluteURI = URIUtils.resolve(baseUri, relativeURI);
                            }
                        }
                    } catch (URISyntaxException e) {
                        absoluteURI = null;
                    }
                    
                    if (absoluteURI == null || !absoluteURI.isAbsolute()) {
                        return null;
                    }
                    
                    Node targetNode = map.getNamedItem("target");
                    if (targetNode != null) {
                        target = targetNode.getNodeValue();
                    }
                    return new UriAction.TargetedUri(target, absoluteURI.toASCIIString());
                }
            }
        }
        return null;
    }

    public String encode(String uri) {
        if(uri == null) {
            return null;
        }
        return uri.replaceAll(" ", "%20");
    }
    
    public nsISupports queryInterface(String uuid) {
        return Mozilla.queryInterface(this, uuid);
    }

}