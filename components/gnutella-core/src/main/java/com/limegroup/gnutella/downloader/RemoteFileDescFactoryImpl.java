package com.limegroup.gnutella.downloader;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.protocol.HTTP;
import org.limewire.http.httpclient.LimeHttpClient;
import org.limewire.io.Address;
import org.limewire.io.Connectable;
import org.limewire.io.ConnectableImpl;
import org.limewire.io.InvalidDataException;
import org.limewire.io.IpPort;
import org.limewire.io.IpPortImpl;
import org.limewire.net.address.AddressFactory;
import org.limewire.util.URIUtils;
import org.xml.sax.SAXException;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.limegroup.gnutella.PushEndpoint;
import com.limegroup.gnutella.PushEndpointFactory;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.UrnSet;
import com.limegroup.gnutella.altlocs.AlternateLocation;
import com.limegroup.gnutella.browser.MagnetOptions;
import com.limegroup.gnutella.downloader.serial.RemoteHostMemento;
import com.limegroup.gnutella.util.LimeWireUtils;
import com.limegroup.gnutella.xml.LimeXMLDocument;
import com.limegroup.gnutella.xml.LimeXMLDocumentFactory;
import com.limegroup.gnutella.xml.SchemaNotFoundException;

@Singleton
class RemoteFileDescFactoryImpl implements RemoteFileDescFactory {

    private static final int COPY_INDEX = Integer.MAX_VALUE;

    private final LimeXMLDocumentFactory limeXMLDocumentFactory;

    private final PushEndpointFactory pushEndpointFactory;

    private final Provider<LimeHttpClient> httpClientProvider;
    
    private final AddressFactory addressFactory;
    
    private final ConcurrentMap<String, RemoteFileDescDeserializer> deserializers = new ConcurrentHashMap<String, RemoteFileDescDeserializer>();
    
    private final List<RemoteFileDescCreator> creators = new CopyOnWriteArrayList<RemoteFileDescCreator>();

    @Inject
    public RemoteFileDescFactoryImpl(LimeXMLDocumentFactory limeXMLDocumentFactory,
            PushEndpointFactory pushEndpointFactory, Provider<LimeHttpClient> httpClientProvider,
            AddressFactory addressFactory) {
        this.limeXMLDocumentFactory = limeXMLDocumentFactory;
        this.pushEndpointFactory = pushEndpointFactory;
        this.httpClientProvider = httpClientProvider;
        this.addressFactory = addressFactory;
    }

    public RemoteFileDesc createRemoteFileDesc(RemoteFileDesc rfd, IpPort ep) {
        return createRemoteFileDesc(ep.getAddress(), // host
                ep.getPort(), // port
                COPY_INDEX, // index (unknown)
                rfd.getFileName(), // filename
                rfd.getSize(), // filesize
                rfd.getClientGUID(), // client GUID
                0, // speed
                false, // chat capable
                2, // quality
                false, // browse hostable
                rfd.getXMLDocument(), // xml doc
                rfd.getUrns(), // urns
                false, // reply to MCast
                false, // is firewalled
                AlternateLocation.ALT_VENDOR, // vendor
                IpPort.EMPTY_SET, // push proxies
                rfd.getCreationTime(), // creation time
                0, // firewalled transfer
                null, // no PE cause not firewalled
                ep instanceof Connectable ? ((Connectable) ep).isTLSCapable() : false // TLS
                                                                                        // capable
                                                                                        // if
                                                                                        // ep
                                                                                        // is.
        );
    }

    public RemoteFileDesc createRemoteFileDesc(RemoteFileDesc rfd, PushEndpoint pe) {
        return createRemoteFileDesc(pe, 
                COPY_INDEX, // index (unknown)
                rfd.getFileName(), // filename
                rfd.getSize(), // filesize
                pe.getClientGUID(),
                rfd.getSpeed(), // speed
                rfd.getQuality(), // quality
                false,  // browse hostable
                rfd.getXMLDocument(), // xml doc
                rfd.getUrns(), // urns
                false, // reply to MCast
                AlternateLocation.ALT_VENDOR, // vendor
                rfd.getCreationTime()); // creation time
    }

    public RemoteFileDesc createRemoteFileDesc(String host, int port, long index, String filename,
            long size, byte[] clientGUID, int speed, boolean chat, int quality, boolean browseHost,
            LimeXMLDocument xmlDoc, Set<? extends URN> urns, boolean replyToMulticast,
            boolean firewalled, String vendor, Set<? extends IpPort> proxies, long createTime,
            int FWTVersion, boolean tlsCapable) {
        return createRemoteFileDesc(host, port, index, filename, size, clientGUID, speed, chat,
                quality, browseHost, xmlDoc, urns, replyToMulticast, firewalled, vendor, proxies,
                createTime, FWTVersion, null, // this will create a PE to
                                                // house the data if the host is
                                                // firewalled
                tlsCapable);
    }

    public RemoteFileDesc createRemoteFileDesc(String host, int port, long index, String filename,
            long size, int speed, boolean chat, int quality, boolean browseHost,
            LimeXMLDocument xmlDoc, Set<? extends URN> urns, boolean replyToMulticast,
            boolean firewalled, String vendor, long createTime, PushEndpoint pe) {
        return createRemoteFileDesc(host, port, index, filename, size, null, speed, chat, quality,
                browseHost, xmlDoc, urns, replyToMulticast, firewalled, vendor, null, createTime,
                0, pe, false); // use exising pe
    }

    @Override
    public RemoteFileDesc createRemoteFileDesc(Address address, long index,
            String filename, long size, byte[] clientGUID, int speed,
            int quality, boolean browseHost, LimeXMLDocument xmlDoc,
            Set<? extends URN> urns, boolean replyToMulticast, String vendor,
            long createTime) {
        return createRemoteFileDesc(address, index, filename, size, clientGUID,
                speed, quality, browseHost, xmlDoc, urns, replyToMulticast,
                vendor, createTime, !urns.isEmpty(), null);
    }

    private RemoteFileDesc createRemoteFileDesc(String host, int port, long index, String filename,
            long size, byte[] clientGUID, int speed, boolean chat, int quality, boolean browseHost,
            LimeXMLDocument xmlDoc, Set<? extends URN> urns, boolean replyToMulticast,
            boolean firewalled, String vendor, Set<? extends IpPort> proxies, long createTime,
            int FWTVersion, PushEndpoint pe, boolean tlsCapable) {
        Address address = null;
        if (firewalled) {
            if (pe == null) {
                // Don't allow the bogus_ip in here.
                IpPort ipp;
                if(!host.equals(RemoteFileDesc.BOGUS_IP)) {
                    try {
                        ipp = new IpPortImpl(host, port);
                    } catch(UnknownHostException uhe) {
                        throw new IllegalArgumentException(uhe);
                    }
                } else {
                    ipp = null;
                    FWTVersion = 0;
                }
                address = pushEndpointFactory.createPushEndpoint(clientGUID, proxies,
                        PushEndpoint.PLAIN, FWTVersion, ipp);
            }
        } else {
            assert pe == null;
            try {
                address = new ConnectableImpl(host, port, tlsCapable);
            } catch (UnknownHostException e) {
                throw new IllegalArgumentException("invalid host: " + host);
            } 
        }
        if (urns == null)
            urns = Collections.emptySet();
        boolean http11 = !urns.isEmpty();

        return new RemoteFileDescImpl(address, index, filename, size, clientGUID, speed, quality,
                browseHost, xmlDoc, urns, replyToMulticast, vendor, createTime, 
                http11, addressFactory, null);
    }

    public RemoteFileDesc createUrlRemoteFileDesc(Address address, String filename,
            long size, Set<? extends URN> urns, URL url) {
        RemoteFileDesc rfd = new UrlRemoteFileDescImpl(address, filename, size, urns, url, addressFactory);
        assert !rfd.isHTTP11();
        return rfd;
    }

    public RemoteFileDesc createUrlRemoteFileDesc(URL url, String filename, URN urn, long size)
            throws IOException, URISyntaxException{
        // Use the URL class to do a little parsing for us.
        int port = url.getPort();
        if (port < 0)
            port = 80; // assume default for HTTP (not 6346)

        Set<URN> urns = new UrnSet();
        if (urn != null)
            urns.add(urn);

        URI uri = URIUtils.toURI(url.toExternalForm());

        return createUrlRemoteFileDesc(new ConnectableImpl(url.getHost(), port, false), filename != null ? filename
                : MagnetOptions.extractFileName(uri), size <= 0 ? contentLength(uri) : size, urns,
                url);
    }

    private long contentLength(URI uri) throws IOException {
        HttpHead head = new HttpHead(uri);
        head.addHeader("User-Agent", LimeWireUtils.getHttpServer());
        HttpResponse response = null;
        LimeHttpClient client = httpClientProvider.get();
        try {
            response = client.execute(head);
            // Extract Content-length, but only if the response was 200 OK.
            // Generally speaking any 2xx response is ok, but in this situation
            // we expect only 200.
            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK)
                throw new IOException("Got " + response.getStatusLine().getStatusCode()
                        + " instead of 200 for URL: " + uri);

            // Head requests are not going to have an entity, so we cannot
            // get the content length by looking at the entity.
            // Instead, we have to parse the header.
            Header contentLength = response.getFirstHeader(HTTP.CONTENT_LEN);
            if(contentLength != null) {
                try {
                    long len = Long.parseLong(contentLength.getValue());
                    if(len < 0) {
                        throw new IOException("Invalid length: " + len);
                    } else {
                        return len;
                    }
                } catch(NumberFormatException nfe) {
                    throw new IOException("can't parse content length", nfe);
                }
            } else {
                throw new IOException("no content length header");
            }
        } finally {
            client.releaseConnection(response);
        }
    }

    public RemoteFileDesc createFromMemento(RemoteHostMemento remoteHostMemento)
            throws InvalidDataException {
        try {
            RemoteFileDescDeserializer deserializer = deserializers.get(remoteHostMemento.getType());
            if (deserializer != null) {
                return deserializer.createRemoteFileDesc(remoteHostMemento.getAddress(addressFactory, pushEndpointFactory), remoteHostMemento.getIndex(), 
                        remoteHostMemento.getFileName(),
                        remoteHostMemento.getSize(), remoteHostMemento.getClientGuid(),
                        remoteHostMemento.getSpeed(), remoteHostMemento
                        .getQuality(), xml(remoteHostMemento.getXml()),
                        remoteHostMemento.getUrns(), remoteHostMemento.getVendor(),
                        -1L);
            }
            if (remoteHostMemento.getCustomUrl() != null) {
                return createUrlRemoteFileDesc(remoteHostMemento.getAddress(addressFactory, pushEndpointFactory),
                        remoteHostMemento.getFileName(), remoteHostMemento
                        .getSize(), remoteHostMemento.getUrns(), remoteHostMemento
                        .getCustomUrl());
            } else {
                return createRemoteFileDesc(remoteHostMemento.getAddress(addressFactory, pushEndpointFactory), remoteHostMemento.getIndex(), 
                        remoteHostMemento.getFileName(),
                        remoteHostMemento.getSize(), remoteHostMemento.getClientGuid(),
                        remoteHostMemento.getSpeed(), remoteHostMemento
                        .getQuality(), remoteHostMemento.isBrowseHost(), xml(remoteHostMemento.getXml()),
                        remoteHostMemento.getUrns(), remoteHostMemento.isReplyToMulticast(),
                        remoteHostMemento.getVendor(), 
                        -1L);
            }
        } catch (SAXException e) {
            throw new InvalidDataException(e);
        } catch (SchemaNotFoundException e) {
            throw new InvalidDataException(e);
        } catch (IOException e) {
            throw new InvalidDataException(e);
        }
    }
    
    private LimeXMLDocument xml(String xml) throws SAXException, SchemaNotFoundException,
            IOException {
        if (xml != null)
            return limeXMLDocumentFactory.createLimeXMLDocument(xml);
        else
            return null;
    }

    @Override
    public RemoteFileDesc createRemoteFileDesc(Address address, long index,
            String filename, long size, byte[] clientGUID, int speed,
            int quality, boolean browseHost, LimeXMLDocument xmlDoc,
            Set<? extends URN> urns, boolean replyToMulticast, String vendor,
            long createTime, boolean http1, byte[] queryGUID) {
        for (RemoteFileDescCreator creator : creators) {
            if (creator.canCreateFor(address)) {
                return creator.create(address, index, filename, size,
                        clientGUID, speed, quality, browseHost, xmlDoc, urns,
                        replyToMulticast, vendor, createTime, http1);
            }
        }
        return new RemoteFileDescImpl(address, index, filename, size,
                clientGUID, speed, quality, browseHost, xmlDoc, urns,
                replyToMulticast, vendor, createTime, http1, addressFactory,
                queryGUID);
    }

    @Override
    public void register(String type, RemoteFileDescDeserializer remoteFileDescDeserializer) {
        RemoteFileDescDeserializer other = deserializers.putIfAbsent(type, remoteFileDescDeserializer);
        assert other == null : "two deserializers registered for: " + type;
    }
    
    public void register(RemoteFileDescCreator creator) {
        creators.add(creator);
    }

}
