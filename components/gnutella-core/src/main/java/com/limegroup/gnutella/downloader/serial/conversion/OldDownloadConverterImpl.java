package com.limegroup.gnutella.downloader.serial.conversion;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.io.StreamCorruptedException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.collection.Range;
import org.limewire.core.api.Category;
import org.limewire.core.api.file.CategoryManager;
import org.limewire.core.settings.SharingSettings;
import org.limewire.io.Address;
import org.limewire.io.ConnectableImpl;
import org.limewire.io.IOUtils;
import org.limewire.io.IpPort;
import org.limewire.io.IpPortImpl;
import org.limewire.net.address.AddressFactory;
import org.limewire.util.CommonUtils;

import com.google.inject.Inject;
import com.limegroup.gnutella.PushEndpointFactory;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.browser.MagnetOptions;
import com.limegroup.gnutella.downloader.DownloaderType;
import com.limegroup.gnutella.downloader.RemoteFileDescImpl;
import com.limegroup.gnutella.downloader.serial.BTDiskManagerMemento;
import com.limegroup.gnutella.downloader.serial.BTDiskManagerMementoImpl;
import com.limegroup.gnutella.downloader.serial.BTDownloadMemento;
import com.limegroup.gnutella.downloader.serial.BTDownloadMementoImpl;
import com.limegroup.gnutella.downloader.serial.BTMetaInfoMemento;
import com.limegroup.gnutella.downloader.serial.BTMetaInfoMementoImpl;
import com.limegroup.gnutella.downloader.serial.DownloadMemento;
import com.limegroup.gnutella.downloader.serial.GnutellaDownloadMemento;
import com.limegroup.gnutella.downloader.serial.GnutellaDownloadMementoImpl;
import com.limegroup.gnutella.downloader.serial.MagnetDownloadMemento;
import com.limegroup.gnutella.downloader.serial.MagnetDownloadMementoImpl;
import com.limegroup.gnutella.downloader.serial.OldDownloadConverter;
import com.limegroup.gnutella.downloader.serial.RemoteHostMemento;
import com.limegroup.gnutella.downloader.serial.TorrentFileSystemMemento;
import com.limegroup.gnutella.downloader.serial.TorrentFileSystemMementoImpl;
import com.limegroup.gnutella.downloader.serial.conversion.DownloadConverterObjectInputStream.Version;

public class OldDownloadConverterImpl implements OldDownloadConverter {
    
    private static Log LOG = LogFactory.getLog(OldDownloadConverterImpl.class);
    private final PushEndpointFactory pushEndpointFactory;
    private final AddressFactory addressFactory;
    private final CategoryManager categoryManager;
    
    @Inject
    public OldDownloadConverterImpl(PushEndpointFactory pushEndpointFactory, AddressFactory addressFactory, CategoryManager categoryManager) {
        this.pushEndpointFactory = pushEndpointFactory;
        this.addressFactory = addressFactory;
        this.categoryManager = categoryManager;
    }
    
    public List<DownloadMemento> readAndConvertOldDownloads(File inputFile) throws IOException {
        if(!inputFile.exists())
            throw new java.io.FileNotFoundException("file " + inputFile + " doesn't exist!");
        
        DownloadConverterObjectInputStream in = null;
        List roots = null;
        SerialIncompleteFileManager sifm = null;
        Version[] versions = Version.values();
        
        for(int i = 0; i < versions.length; i++) {
            try {
                in = new DownloadConverterObjectInputStream(new BufferedInputStream(new FileInputStream(inputFile)));
                in.deserializeVersion(versions[i]);
                roots = (List)in.readObject();
                sifm = (SerialIncompleteFileManager)in.readObject();
                break;
            } catch(StreamCorruptedException sce) {
                LOG.debug("Unable to deserialize from version: " + versions[i], sce);
                continue;
            } catch(ClassNotFoundException cnfe) {
                throw (IOException)new IOException().initCause(cnfe);
            } finally {
                IOUtils.close(in);
            }
        }
        
        if(roots != null && sifm != null)
            return convertSerialRootsToMementos(roots, sifm);
        else
            return Collections.emptyList();
    }

    private List<DownloadMemento> convertSerialRootsToMementos(List roots,
            SerialIncompleteFileManager sifm) throws IOException {
        List<DownloadMemento> mementos = new ArrayList<DownloadMemento>(roots.size());
        
        for(Object o : roots) {
            if(o instanceof SerialBTDownloader)
                addBTDownloader(mementos, (SerialBTDownloader)o, sifm);
            else if(o instanceof SerialInNetworkDownloader)
                ; // ignore for conversions -- they'll restart on their own
            else if(o instanceof SerialMagnetDownloader)
                addMagnet(mementos, (SerialMagnetDownloader)o, sifm);
            else if(o instanceof SerialResumeDownloader)
                addResume(mementos, (SerialResumeDownloader)o, sifm);
            else if(o instanceof SerialRequeryDownloader)
                ; // ignore!
            else if(o instanceof SerialStoreDownloader)
                addStore(mementos, (SerialStoreDownloader)o, sifm);
            else if(o instanceof SerialManagedDownloader)
                addManaged(mementos, (SerialManagedDownloader)o, sifm);
            else
                LOG.warn("Unable to convert read object: " + o);
        }
        
        return mementos;
    }
    
    private void addGnutellaProperties(GnutellaDownloadMemento memento, Map<String, Serializable> properties, List<Range> ranges, File incompleteFile, Set<SerialRemoteFileDesc> rfds) {
        memento.setSavedBlocks(ranges);
        memento.setIncompleteFile(incompleteFile);
        memento.setRemoteHosts(convertToMementos(rfds));
        memento.setContentLength(properties.get("fileSize") == null ? -1 : ((Number)properties.get("fileSize")).longValue());
        memento.setSha1Urn((URN)properties.get("sha1Urn"));
        addCommonProperties(memento, properties);
    }
    
    @SuppressWarnings("unchecked")
    private void addCommonProperties(DownloadMemento memento, Map<String, Serializable> properties) {
        memento.setAttributes((Map<String, Object>)properties.get("attributes"));
        memento.setDefaultFileName((String)properties.get("defaultFileName"));
        memento.setSaveFile((File)properties.get("saveFile"));
    }

    private void addStore(List<DownloadMemento> mementos, SerialStoreDownloader o, SerialIncompleteFileManager sifm) {
        File incompleteFile = getIncompleteFile(o, sifm);
        List<Range> ranges = getRanges(incompleteFile, sifm);
        GnutellaDownloadMemento memento = new GnutellaDownloadMementoImpl();
        memento.setDownloadType(DownloaderType.STORE);
        addGnutellaProperties(memento, o.getProperties(), ranges, incompleteFile, o.getRemoteFileDescs());
        mementos.add(memento);
    }
    
    private void addManaged(List<DownloadMemento> mementos, SerialManagedDownloader o, SerialIncompleteFileManager sifm) {
        File incompleteFile = getIncompleteFile(o, sifm);
        List<Range> ranges = getRanges(incompleteFile, sifm);
        GnutellaDownloadMemento memento = new GnutellaDownloadMementoImpl();
        memento.setDownloadType(DownloaderType.MANAGED);
        addGnutellaProperties(memento, o.getProperties(), ranges, incompleteFile, o.getRemoteFileDescs());
        mementos.add(memento);                
    }

    private void addResume(List<DownloadMemento> mementos, SerialResumeDownloader o, SerialIncompleteFileManager sifm) {
        File incompleteFile = getIncompleteFile(o, sifm);
        List<Range> ranges = getRanges(incompleteFile, sifm);
        o.getProperties().put("fileSize", o.getSize());
        o.getProperties().put("sha1Urn", o.getUrn());
        o.getProperties().put("defaultFileName", o.getName());
        GnutellaDownloadMemento memento = new GnutellaDownloadMementoImpl();
        memento.setDownloadType(DownloaderType.MANAGED);
        addGnutellaProperties(memento, o.getProperties(), ranges, incompleteFile, o.getRemoteFileDescs());
        mementos.add(memento);       
    }

    private void addMagnet(List<DownloadMemento> mementos, SerialMagnetDownloader o, SerialIncompleteFileManager sifm) {
        File incompleteFile = getIncompleteFile(o, sifm);
        List<Range> ranges = getRanges(incompleteFile, sifm);
        if(o.getUrn() != null)
            o.getProperties().put("sha1Urn", o.getUrn());      
        MagnetDownloadMemento memento = new MagnetDownloadMementoImpl();
        memento.setDownloadType(DownloaderType.MAGNET);
        memento.setMagnet((MagnetOptions)o.getProperties().get("MAGNET"));
        addGnutellaProperties(memento, o.getProperties(), ranges, incompleteFile, o.getRemoteFileDescs());
        mementos.add(memento); 
    }

    private void addBTDownloader(List<DownloadMemento> mementos, SerialBTDownloader o, SerialIncompleteFileManager sifm) throws IOException {
        BTDownloadMemento memento = new BTDownloadMementoImpl();
        memento.setDownloadType(DownloaderType.BTDOWNLOADER);
        memento.setBtMetaInfoMemento(toBTMetaInfoMemento((SerialBTMetaInfo)o.getProperties().get("metainfo")));
        addCommonProperties(memento, o.getProperties());
        mementos.add(memento);
    }
    
    private BTMetaInfoMemento toBTMetaInfoMemento(SerialBTMetaInfo info) throws IOException {
        BTMetaInfoMemento memento = new BTMetaInfoMementoImpl();
        memento.setFileSystem(toFileSystemMemento(info.getFileSystem()));
        memento.setFolderData(toBTDiskManagerMemento(info.getDiskManagerData()));
        memento.setHashes(info.getHashes());
        memento.setInfoHash(info.getInfoHash());
        memento.setPieceLength(info.getPieceLength());
        memento.setPrivate(info.isPrivate());
        memento.setRatio(info.getHistoricRatio());
        try {
            memento.setTrackers(info.getTrackers());
        } catch (URISyntaxException e) {
            IOException ioe = new IOException();
            ioe.initCause(e);
            throw ioe;
        }
        return memento;
    }
    
    private BTDiskManagerMemento toBTDiskManagerMemento(SerialDiskManagerData data) {
        BTDiskManagerMemento memento = new BTDiskManagerMementoImpl();
        memento.setPartialBlocks(data.getPartialBlocks());
        memento.setVerifiedBlocks(data.getVerifiedBlocks());
        memento.setVerifying(data.isVerifying());
        return memento;
    }
    
    private TorrentFileSystemMemento toFileSystemMemento(SerialTorrentFileSystem system) {
        TorrentFileSystemMemento memento = new TorrentFileSystemMementoImpl();
        memento.setCompleteFile(system.getCompleteFile());
        memento.setFiles(system.getFiles());
        memento.setFolders(system.getFolders());
        memento.setIncompleteFile(system.getIncompleteFile());
        memento.setName(system.getName());
        memento.setTotalSize(system.getTotalSize());
        return memento;
    }
    
    private List<Range> getRanges(File incompleteFile, SerialIncompleteFileManager ifm) {
        List<Range> ranges = ifm.getBlocks().get(incompleteFile);
        if(ranges != null) {
            List<Range> fixedRanges = new ArrayList<Range>(ranges.size());
            for(Range range : ranges) {
                fixedRanges.add(Range.createRange(range.getLow(), range.getHigh() -1));
            }
            return fixedRanges;
        }
        
        return Collections.emptyList();
    }
    
    private File getIncompleteFile(SerialManagedDownloader download, SerialIncompleteFileManager sifm) {
        URN sha1 = getSha1(download);        
        File incompleteFile = null;
        
        if(download instanceof SerialResumeDownloader)
            incompleteFile = ((SerialResumeDownloader)download).getIncompleteFile();
        
        if(sha1 != null)
            incompleteFile = sifm.getHashes().get(sha1);
                
        if(incompleteFile == null) {
            File saveFile = (File)download.getProperties().get("saveFile");
            if(saveFile != null) {
                String defaultName = (String)download.getProperties().get("defaultFileName");
                if(defaultName != null) {
                    Category category = categoryManager.getCategoryForFilename(defaultName); 
                    saveFile = new File(SharingSettings.getSaveDirectory(category), defaultName);
                }
            }

            Number size = (Number)download.getProperties().get("fileSize");
            if(download instanceof SerialResumeDownloader)
                size = ((SerialResumeDownloader)download).getSize();
            
            if (saveFile != null && size != null) {
                String name = CommonUtils.convertFileName(saveFile.getName());
                incompleteFile = new File(SharingSettings.INCOMPLETE_DIRECTORY.get(), "T-"
                        + size.longValue() + "-" + name);
            }
        }
        
        return incompleteFile;
    }
    
    private URN getSha1(SerialManagedDownloader download) {
        URN sha1 = null;
        
        if(download instanceof SerialMagnetDownloader)
            sha1 = ((SerialMagnetDownloader)download).getUrn();
        else if(download instanceof SerialResumeDownloader)
            sha1 = ((SerialResumeDownloader)download).getUrn();
        
        if(sha1 != null && !sha1.isSHA1())
            sha1 = null;
        
        for(SerialRemoteFileDesc rfd : download.getRemoteFileDescs()) {
            if(sha1 != null)
                break;
            
            for(URN urn : rfd.getUrns()) {
               if(urn.isSHA1()) {
                   sha1 = urn;
                   break;
               }
            }
        }
        
      return sha1;
    }
    
    private Set<RemoteHostMemento> convertToMementos(Set<SerialRemoteFileDesc> rfds) {
        Set<RemoteHostMemento> mementos = new HashSet<RemoteHostMemento>(rfds.size());
        for(SerialRemoteFileDesc rfd : rfds) {
            try {
                Address address = getAddress(rfd);
                RemoteHostMemento memento = new RemoteHostMemento(address, rfd.getFilename(), rfd
                        .getIndex(), rfd.getClientGUID(), rfd.getSpeed(), rfd.getSize(), rfd.getQuality(), rfd.isReplyToMulticast(), rfd
                        .getXml(), rfd.getUrns(), rfd.isBrowseHostEnabled(), rfd.getVendor(),
                        rfd.isHttp11(), RemoteFileDescImpl.TYPE, addressFactory);
                if(rfd instanceof SerialUrlRemoteFileDesc)
                    memento.setCustomUrl(((SerialUrlRemoteFileDesc)rfd).getUrl());
                mementos.add(memento);
            } catch (IOException ie) {
                LOG.debug("", ie);
            }
        }
        return mementos;
    }

    Address getAddress(SerialRemoteFileDesc rfd) throws IOException {
        if (rfd.isFirewalled() ) {
            if(rfd.getHttpPushAddr() != null) {
                return pushEndpointFactory.createPushEndpoint(rfd.getHttpPushAddr());
            } else {
            	// This is from very old versions, or versions that didn't have proxies.
            	// In this case, we still make it, but the address might be private, in which
            	// case the only useful bit of info is the clientGUID.
                if (!RemoteFileDesc.BOGUS_IP.equals(rfd.getHost())) {
                    return pushEndpointFactory.createPushEndpoint(rfd.getClientGUID(), IpPort.EMPTY_SET, (byte)0, 0, new IpPortImpl(rfd.getHost(), rfd.getPort()));
                } else {
                    return pushEndpointFactory.createPushEndpoint(rfd.getClientGUID());
                }
            }
        } else {
            return new ConnectableImpl(rfd.getHost(), rfd.getPort(), rfd.isTlsCapable());
        }
    }
}
