/*
 * This message represents a list of ultrapeer connections that has been 
 * returned by an ultrapeer.  Its payload is a byte indicating how many
 * IpPorts are about to follow and their serialized list.
 */
package com.limegroup.gnutella.messages.vendor;

import org.limewire.io.GUID;

public class UDPCrawlerPong extends AbstractVendorMessage {
	
	public static final int VERSION = 1;
	
	public static final String AGENT_SEP = ";";
    
	//The masks for the DHT status information
	public static final byte DHT_WAITING_MASK       = 0x01;
	public static final byte DHT_ACTIVE_MASK        = 0x02;
    public static final byte DHT_PASSIVE_MASK       = 0x04;
    public static final byte DHT_PASSIVE_LEAF_MASK  = 0x08;
    
	//this message is sent only as a reply to a request message, so when 
	//constructing it we need the object representing the request message
	
	UDPCrawlerPong(UDPCrawlerPing request, byte[] payload){
		super(F_LIME_VENDOR_ID,F_CRAWLER_PONG, VERSION, payload);
		setGUID(new GUID(request.getGUID()));
	}
	
}
