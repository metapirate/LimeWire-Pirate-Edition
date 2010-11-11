package org.limewire.core.impl.search.torrentweb;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Map;

import org.limewire.bittorrent.BTData;
import org.limewire.bittorrent.Torrent;
import org.limewire.core.api.Category;
import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.endpoint.RemoteHost;
import org.limewire.core.api.search.SearchResult;
import org.limewire.friend.api.Friend;
import org.limewire.friend.api.FriendPresence;
import org.limewire.friend.api.IncomingChatListener;
import org.limewire.friend.api.MessageReader;
import org.limewire.friend.api.MessageWriter;
import org.limewire.friend.api.Network;
import org.limewire.friend.api.PresenceEvent;
import org.limewire.friend.api.feature.Feature;
import org.limewire.friend.api.feature.FeatureTransport;
import org.limewire.friend.api.feature.ReferrerFeature;
import org.limewire.listener.EventListener;
import org.limewire.util.Base32;
import org.limewire.util.StringUtils;
import org.limewire.util.URIUtils;

import com.limegroup.gnutella.URN;

/**
 * A torrent web search result.
 */
public class TorrentWebSearchResult implements SearchResult {
    
    private final URI referrer;
    private final URN urn;
    private final BTData torrentData;
    private final File torrentFile;
    private final Torrent torrent;

    public TorrentWebSearchResult(BTData torrentData,
            URI referrer, File torrentFile, Torrent torrent) {
        this.referrer = referrer;
        this.torrentData = torrentData;
        this.torrentFile = torrentFile;
        this.torrent = torrent;
        try {
            URN topLevelURN = null;
            try {
                topLevelURN = URN.createSHA1Urn(torrentFile);
            } catch (InterruptedException e) {
                topLevelURN =  URN.createSHA1UrnFromBytes(torrentData.getInfoHash());
            }
            urn = topLevelURN;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Category getCategory() {
        return Category.TORRENT;
    }

    @Override
    public String getFileExtension() {
        return "torrent";
    }

    @Override
    public String getFileName() {
        return torrentData.getName() + ".torrent";
    }

    @Override
    public String getFileNameWithoutExtension() {
        return torrentData.getName();
    }

    @Override
    public String getMagnetURL() {
        return null;
    }

    @Override
    public Object getProperty(FilePropertyKey key) {
        switch (key) {
        case FILE_SIZE:
            return getSize();
        case NAME:
            return torrentData.getName();
        case TORRENT:
            return torrent;
        case REFERRER:
            return getReferrer();
        }
        return null;
    }

    @Override
    public float getRelevance(String query) {
        return 0;
    }

    @Override
    public long getSize() {
        return torrent.getTotalPayloadSize();
    }

    @Override
    public URN getUrn() {
        return urn;
    }

    @Override
    public boolean isLicensed() {
        return false;
    }

    @Override
    public boolean isSpam() {
        return false;
    }

    public File getTorrentFile() {
        return torrentFile;
    }

    private URI getReferrer() {
        
        if (referrer != null) {
            return referrer;
        }
        
        String sha1Base16 = torrent.getSha1();
        String sha1Base32 = Base32.encode(StringUtils.fromHexString(sha1Base16));
        String uriString = "http://www.google.com/#q=" + sha1Base16 + "+|+" + sha1Base32;
        try {
            return URIUtils.toURI(uriString);
        } catch (URISyntaxException e) {
            // Ignore
        }
        return null;
    }
    
    @Override
    public RemoteHost getSource() {
        return new RemoteHost() {
            
            @Override
            public boolean isSharingEnabled() {
                return false;
            }
            
            @Override
            public boolean isChatEnabled() {
                return false;
            }
            
            @Override
            public boolean isBrowseHostEnabled() {
                return false;
            }
            
            @Override
            public FriendPresence getFriendPresence() {
                return new FriendPresence() {

                    @Override
                    public void addFeature(Feature feature) {
                    }

                    @Override
                    public <D, F extends Feature<D>> void addTransport(Class<F> clazz,
                            FeatureTransport<D> transport) {
                    }

                    @Override
                    public Feature getFeature(URI id) {
                        if (id.equals(ReferrerFeature.ID)) {
                            URI referrerURI = getReferrer();
                            if (referrerURI != null) {
                                return new ReferrerFeature(referrerURI);
                            }
                        }
                        return null;
                    }

                    @Override
                    public Collection<Feature> getFeatures() {
                        return null;
                    }

                    @Override
                    public Friend getFriend() {
                        return new Friend() {

                            @Override
                            public void addPresenceListener(
                                    EventListener<PresenceEvent> presenceListener) {
                            }

                            @Override
                            public MessageWriter createChat(MessageReader reader) {
                                return null;
                            }

                            @Override
                            public FriendPresence getActivePresence() {
                                return null;
                            }

                            @Override
                            public String getFirstName() {
                                return null;
                            }

                            @Override
                            public String getId() {
                                return null;
                            }

                            @Override
                            public String getName() {
                                return null;
                            }

                            @Override
                            public Network getNetwork() {
                                return new Network() {
                                    @Override
                                    public String getCanonicalizedLocalID() {
                                        return "";
                                    }

                                    @Override
                                    public String getNetworkName() {
                                        return "Torrent Web Search";
                                    }

                                    @Override
                                    public Type getType() {
                                        return Network.Type.WEBSEARCH;
                                    }
                                };
                            }

                            @Override
                            public Map<String, FriendPresence> getPresences() {
                                return null;
                            }

                            @Override
                            public String getRenderName() {
                                return null;
                            }

                            @Override
                            public boolean hasActivePresence() {
                                return false;
                            }

                            @Override
                            public boolean isAnonymous() {
                                return true;
                            }

                            @Override
                            public boolean isSignedIn() {
                                return false;
                            }

                            @Override
                            public boolean isSubscribed() {
                                return false;
                            }

                            @Override
                            public void removeChatListener() {
                            }

                            @Override
                            public void setChatListenerIfNecessary(IncomingChatListener listener) {
                            }

                            @Override
                            public void setName(String name) {
                            }
                            
                        };
                    }

                    @Override
                    public Mode getMode() {
                        return null;
                    }

                    @Override
                    public String getPresenceId() {
                        return null;
                    }

                    @Override
                    public int getPriority() {
                        return 0;
                    }

                    @Override
                    public String getStatus() {
                        return null;
                    }

                    @Override
                    public <F extends Feature<D>, D> FeatureTransport<D> getTransport(
                            Class<F> feature) {
                        return null;
                    }

                    @Override
                    public Type getType() {
                        return null;
                    }

                    @Override
                    public boolean hasFeatures(URI... id) {
                        return false;
                    }

                    @Override
                    public void removeFeature(URI id) {
                    }
                    
                };
            }
        };
    }
}
