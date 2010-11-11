package org.limewire.friend.impl.feature;

import org.limewire.friend.api.FileMetaData;
import org.limewire.friend.api.feature.AuthToken;
import org.limewire.friend.api.feature.FeatureRegistry;
import org.limewire.friend.api.feature.FeatureTransport;
import org.limewire.friend.api.feature.LibraryChangedNotifier;
import org.limewire.io.Address;
import org.limewire.net.ConnectBackRequest;

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;

public class LimeWireFriendFeatureModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(FeatureRegistry.class).to(FeatureRegistryImpl.class);

        bind(new TypeLiteral<FeatureTransport.Handler<Address>>(){}).to(AddressDispatcher.class);
        bind(new TypeLiteral<FeatureTransport.Handler<AuthToken>>(){}).to(AuthTokenDispatcher.class);
        bind(new TypeLiteral<FeatureTransport.Handler<LibraryChangedNotifier>>(){}).to(LibraryChangedDispatcher.class);
        bind(new TypeLiteral<FeatureTransport.Handler<ConnectBackRequest>>(){}).to(ConnectBackRequestFeatureTransportHandler.class);
        bind(new TypeLiteral<FeatureTransport.Handler<FileMetaData>>(){}).to(FileOfferFeatureTransportHandler.class);
    }
}
