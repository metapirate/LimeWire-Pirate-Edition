package org.limewire.core.impl.library;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import org.limewire.core.api.library.FileItem;
import org.limewire.core.api.library.MagnetLinkFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.ApplicationServices;
import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.browser.MagnetOptions;

@Singleton
public class MagnetLinkFactoryImpl implements MagnetLinkFactory {
    
    private NetworkManager networkManager;
    private ApplicationServices applicationServices;

    @Inject
    public MagnetLinkFactoryImpl(NetworkManager networkManager, ApplicationServices applicationServices){
        this.networkManager = networkManager;
        this.applicationServices = applicationServices;
    }
    

    @Override
    public String createMagnetLink(FileItem fileItem) {
        if (fileItem instanceof CoreLocalFileItem){
            return createMagnetLink((CoreLocalFileItem)fileItem);
        } 
        throw new IllegalArgumentException("FileItem must be instance of CoreLocalFileItem or CoreRemoteFileItem: " + fileItem);
    }
    
    private String createMagnetLink(CoreLocalFileItem fileItem) {
        return MagnetOptions.createMagnet(fileItem.getFileDetails(), 
                getInetSocketAddress(), applicationServices.getMyGUID()).toExternalForm();
    }
    
    private InetSocketAddress getInetSocketAddress() {
        // TODO maybe cache this, even statically
        try {
            return new InetSocketAddress(InetAddress.getByAddress(networkManager.getAddress()), networkManager.getPort());
        } catch (UnknownHostException e) {
            //TODO what should be done with UnknownHostException?
            throw new RuntimeException(e);
        }
    }

}
