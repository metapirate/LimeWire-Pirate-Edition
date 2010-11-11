package org.limewire.xmpp.client.impl;

import java.io.IOException;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.friend.api.FriendConnectionConfiguration;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.Record;
import org.xbill.DNS.SRVRecord;
import org.xbill.DNS.Type;

/**
 * Uses a <code>XMPPConnectionConfiguration</code> to look up the
 * XMPP host and port in DNS.  Then creates a smack <code>ConnectionConfiguration</code>
 * based on the data in the DNS entry.
 */
public class DNSConnectionConfigurationFactory implements ConnectionConfigurationFactory {
    
    private static final Log LOG = LogFactory.getLog(DNSConnectionConfigurationFactory.class);
    
    /**
     * The max number of times to attempt looking up
     * the XMPP host in DNS.
     */
    private static final int MAX_XMPP_HOST_LOOKUPS = 3;

    @Override
    public boolean hasMore(FriendConnectionConfiguration connectionConfiguration, RequestContext requestContext) {
        return requestContext.getNumRequests() < MAX_XMPP_HOST_LOOKUPS;
    }

    /**
     * Converts a LimeWire XMPPConnectionConfiguration into a Smack
     * ConnectionConfiguration, trying to obtain the hostname from DNS SRV
     * as per RFC 3920 and falling back to the service name and default port
     * if the SRV lookup fails. This method blocks during the DNS lookup.
     */
    public ConnectionConfiguration getConnectionConfiguration(FriendConnectionConfiguration configuration, RequestContext requestContext) {
        return getConnectionConfig(configuration, requestContext);
    }

    private ConnectionConfiguration getConnectionConfig(FriendConnectionConfiguration configuration, RequestContext requestContext) {
        checkHasMore(configuration, requestContext);
        HostAndPort hostAndPort = new HostAndPort(configuration.getServiceName(), 5222); // fallback
        String serviceName = configuration.getServiceName();
        try {
            String domain = "_xmpp-client._tcp." + serviceName;
            Lookup lookup = new Lookup(domain, Type.SRV);
            Record[] answers = lookup.run();
            int result = lookup.getResult();
            if(result == Lookup.SUCCESSFUL) {
                hostAndPort = readHostAndPortFromDNSEntry(hostAndPort, domain, answers);
            } else if(result == Lookup.HOST_NOT_FOUND) {
                LOG.debugf("dns lookup of {0} failed: host not found", domain);
            } else if(result == Lookup.UNRECOVERABLE) {
                LOG.debugf("dns lookup of {0} failed: unrecoverable", domain);
            } else if(result == Lookup.TYPE_NOT_FOUND) {
                LOG.debugf("dns lookup of {0} failed: type not found", domain);
            } else if(result == Lookup.TRY_AGAIN) {
                LOG.debugf("dns lookup of {0} failed: try again", domain);
                requestContext.incrementRequests();
                if(hasMore(configuration, requestContext)) {
                    return getConnectionConfig(configuration, requestContext);
                }
            }
        } catch(IOException iox) {
            // Failure looking up the SRV record - use the service name and default port
            LOG.debug("Failed to look up SRV record", iox);
        }
        return new ConnectionConfiguration(hostAndPort.getHost(), hostAndPort.getPort(), serviceName);
    }

    private static HostAndPort readHostAndPortFromDNSEntry(HostAndPort fallback, String domain, Record[] answers) {
        HostAndPort hostAndPort = fallback;
        if(answers != null && answers.length > 0) {
            // RFC 2782: use the server with the lowest-numbered priority,
            // break ties by preferring servers with higher weights
            int lowestPriority = Integer.MAX_VALUE;
            int highestWeight = Integer.MIN_VALUE;
            for(Record rec : answers) {
                if(rec instanceof SRVRecord) {
                    SRVRecord srvRec = (SRVRecord)rec;
                    int priority = srvRec.getPriority();
                    int weight = srvRec.getWeight();
                    if(priority < lowestPriority && weight > highestWeight) {
                        hostAndPort = new HostAndPort(srvRec.getTarget().toString(), srvRec.getPort());
                        lowestPriority = priority;
                        highestWeight = weight;
                    }
                } else {
                    LOG.debugf("dns lookup of {0} was successful, but contains non-SRV record: {1}: {2}",
                            domain, rec.getClass().getSimpleName(), rec.toString());
                }
            }
        } else {
            LOG.debugf("dns lookup of {0} was successful, but contained no records", domain);
        }
        return hostAndPort;
    }

    private static class HostAndPort {
        private final String host;
        private final int port;

        public HostAndPort(String host, int defaultPort) {
            int colonIdx = host.indexOf(':');
            if(colonIdx == -1) {
                this.host = host;
                this.port = defaultPort;
            } else {
                this.host = host.substring(0, colonIdx);
                int p = -1;
                if (colonIdx < host.length() - 1) {
                    String portS = host.substring(colonIdx+1);
                    try {
                        p = Integer.parseInt(portS);
                    } catch(NumberFormatException nfe) {
                    }
                }
                if(p > 0) {
                    this.port = p;
                } else {
                    this.port = defaultPort;
                }
            }
        }

        public String getHost() {
            return host;
        }

        public int getPort() {
            return port;
        }
    }
    
    private void checkHasMore(FriendConnectionConfiguration connectionConfiguration, RequestContext requestContext) {
        if(!hasMore(connectionConfiguration, requestContext)) {
            throw new IllegalArgumentException("no more ConnectionConfigurations");
        }
    }
    
}
