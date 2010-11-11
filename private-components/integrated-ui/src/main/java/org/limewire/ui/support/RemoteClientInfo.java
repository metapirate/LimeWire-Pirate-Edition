package org.limewire.ui.support;

import java.net.URISyntaxException;
import java.util.StringTokenizer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.util.URIUtils;

/**
 * Handles the client-side representation of an 
 * <tt>RemoteServletInfo</tt> object, which reconstructs the data
 * for the update created on the server side.  This parses that 
 * information so BugManager can store it.
 */
//2345678|012345678|012345678|012345678|012345678|012345678|012345678|012345678|
public final class RemoteClientInfo extends RemoteAbstractInfo {
    
    private static final Log LOG = LogFactory.getLog(RemoteClientInfo.class);
    
    /**
     * The amount of time to wait should the server be down.
     */
    private static final long FAILURE_TIME = 60 * 60 * 1000; // 1 hour

    /**
     * The next time this particular bug can be sent.
     */
	private String _nextThisBugTime;
	
	/**
	 * The next time any bug can be sent.
	 */
	private String _nextAnyBugTime;
    
	/**
	 * Adds multiple key/value pairs based on the information
	 * in infoLine.  infoLine is expected to be in the form of
	 * 'key=URLEncoder.encode(value)&key2=URLEncode.encode(value2)'
     * 
     * This method is used on the client side to reconstruct the 
     * <tt>RemoteServletInfo</tt> object originally created on the server.
	 *
	 * @param infoLine string containing all name/value pairs.
	 */
	final void addRemoteInfo(final String infoLine) {
	    if(LOG.isDebugEnabled())
	        LOG.debug("Adding info: " + infoLine);
	        
        StringTokenizer st = new StringTokenizer(infoLine, "=&");
        if( st.countTokens() % 2 != 0 )
            return; //invalid response.
        while(st.hasMoreTokens()) {
            handleKeyValuePair(st.nextToken(), st.nextToken());
        }
    }

	/**
	 * Handles a single key/value pair, storing the values in this
	 * object.
     * 
     * This method is used on the client side to reconstruct the 
     * <tt>RemoteUpdateInfo</tt> object originally created on the server.
	 *
	 * @param line a string representing one key-value pair of
	 *             information from the server's response
	 */
    private final void handleKeyValuePair(final String k, final String v) {
        String value = "";
        try {
            value = URIUtils.decodeToUtf8(v);
        } catch (URISyntaxException e) {
            return;
        }
        if(k.equalsIgnoreCase(NEXT_THIS_BUG_TIME))
            _nextThisBugTime = value;
        else if (k.equalsIgnoreCase(NEXT_ANY_BUG_TIME))
            _nextAnyBugTime = value;
        // else something random -- don't worry about it
	}
	
	/**
	 * Marks the next bug time & next any bug time for the appropriate values
	 * given a connection failure.
	 */
	final void connectFailed() {
	    _nextThisBugTime = "" + FAILURE_TIME;
	    _nextAnyBugTime = "" + FAILURE_TIME;
    }
	
	/**
	 * Returns the next time, as a long, that this bug is allowed to be sent.
	 * The return value is in milliseconds, and should be added to
	 * System.currentTimeMillis() to retrieve the exact millisecond at which
	 * the next bug should be sent.
	 */
	long getNextThisBugTime() {
	    if( _nextThisBugTime == null)
	        return 0;
	    else {
	        long nextTime;
	        try {
	            nextTime = Long.parseLong(_nextThisBugTime);
	        } catch(NumberFormatException nfe) {
	            nextTime = 0;
	        }
	        return nextTime;
	    }
	}
	
	/**
	 * Returns the next time, as a long, that any bug is allowed to be sent.
	 * The return value is in milliseconds, and should be added to
	 * System.currentTimeMillis() to retrieve the exact millisecond at which
	 * the next bug should be sent.
	 */
	long getNextAnyBugTime() {
	    if( _nextAnyBugTime == null)
	        return 0;
	    else {
	        long nextTime;
	        try {
	            nextTime = Long.parseLong(_nextAnyBugTime);
	        } catch(NumberFormatException nfe) {
	            nextTime = 0;
	        }
	        return nextTime;
	    }	    
	}
}
