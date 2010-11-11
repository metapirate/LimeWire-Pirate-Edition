package com.limegroup.gnutella.downloader;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Set;

import org.limewire.io.Address;
import org.limewire.io.InvalidDataException;
import org.limewire.io.IpPort;

import com.limegroup.gnutella.PushEndpoint;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.downloader.serial.RemoteHostMemento;
import com.limegroup.gnutella.xml.LimeXMLDocument;

public interface RemoteFileDescFactory {

    /**
     * Constructs a RemoteFileDesc based on this memento.
     */
    public RemoteFileDesc createFromMemento(RemoteHostMemento remoteHostMemento)
            throws InvalidDataException;

    /**
     * Constructs a new RemoteFileDescImpl exactly like the other one,
     * but with a different remote host.
     * <p>
     * It is okay to use the same internal structures
     * for URNs because the Set is immutable.
     */
    public RemoteFileDesc createRemoteFileDesc(RemoteFileDesc rfd, IpPort ep);

    /**
     * Constructs a new RemoteFileDescImpl exactly like the other one,
     * but with a different push proxy host.  Will be handy when processing
     * head pongs.
     */
    public RemoteFileDesc createRemoteFileDesc(RemoteFileDesc rfd, PushEndpoint pe);

    /** 
     * Constructs a new RemoteFileDescImpl with metadata.
     * @param index the index of the file that the client sent
     * @param filename the name of the file
     * @param size the completed size of this file
     * @param clientGUID the unique identifier of the client
     * @param speed the speed of the connection
     * @param quality the quality of the connection, where 0 is the
     *  worst and 3 is the best.  (This is the same system as in the
     *  GUI but on a 0 to N-1 scale.)
     * @param browseHost specifies whether or not the remote host supports
     *  browse host
     * @param xmlDoc the <tt>LimeXMLDocument</tt> for the response
     * @param urns the <tt>Set</tt> of <tt>URN</tt>s for the file
     * @param replyToMulticast true if its from a reply to a multicast query
     * @param vendor the vendor of the remote host
     * @param createTime the network-wide creation time of this file
     *
     * @throws <tt>IllegalArgumentException</tt> if any of the arguments are
     *  not valid
     * @throws <tt>NullPointerException</tt> if the host argument is 
     *  <tt>null</tt> or if the file name is <tt>null</tt>
     */
    public RemoteFileDesc createRemoteFileDesc(Address address, long index,
            String filename, long size, byte[] clientGUID, int speed,
            int quality, boolean browseHost, LimeXMLDocument xmlDoc,
            Set<? extends URN> urns, boolean replyToMulticast, String vendor,
            long createTime);
    
    /** 
     * Constructs a new RemoteFileDescImpl with metadata.
     * @param filename the name of the file
     * @param clientGUID the unique identifier of the client
     * @param speed the speed of the connection
     * @param quality the quality of the connection, where 0 is the
     *  worst and 3 is the best.  (This is the same system as in the
     *  GUI but on a 0 to N-1 scale.)
     * @param browseHost specifies whether or not the remote host supports
     *  browse host
     * @param xmlDoc the <tt>LimeXMLDocument</tt> for the response
     * @param urns the <tt>Set</tt> of <tt>URN</tt>s for the file
     * @param replyToMulticast true if its from a reply to a multicast query
     * @param queryGUID the GUID of the query if the RFD comes from a query
     *  reply, otherwise null
     * @throws <tt>IllegalArgumentException</tt> if any of the arguments are
     *  not valid
     * @throws <tt>NullPointerException</tt> if the host argument is 
     *  <tt>null</tt> or if the file name is <tt>null</tt>
     */
    public RemoteFileDesc createRemoteFileDesc(Address address, long index,
            String filename, long size, byte[] clientGUID, int speed,
            int quality, boolean browseHost, LimeXMLDocument xmlDoc,
            Set<? extends URN> urns, boolean replyToMulticast, String vendor,
            long createTime, boolean http1, byte[] queryGUID);

    /**
     * Constructs a URLRemoteFileDesc, looking up the size from the URL if no size is known.<p>
     * 
     * <b>This method can block if the size is <= 0.</b>
     */
    public RemoteFileDesc createUrlRemoteFileDesc(URL url, String filename, URN urn, long size)
            throws IOException, URISyntaxException;

    /**
     * Registers a {@link RemoteFileDescDeserializer} for a type. Type is the type
     * returned by {@link RemoteHostMemento#getType()}.
     */
    public void register(String type, RemoteFileDescDeserializer remoteFileDescDeserializer);

    public void register(RemoteFileDescCreator remoteFileDescCreator);
}
