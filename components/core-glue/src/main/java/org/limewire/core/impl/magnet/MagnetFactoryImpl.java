package org.limewire.core.impl.magnet;

import java.net.URI;

import org.limewire.core.api.magnet.MagnetFactory;
import org.limewire.core.api.magnet.MagnetLink;

import com.google.inject.Singleton;
import com.limegroup.gnutella.browser.MagnetOptions;

@Singleton
public class MagnetFactoryImpl implements MagnetFactory {

    @Override
    public boolean isMagnetLink(URI uri) {
        return uri != null && "magnet".equalsIgnoreCase(uri.getScheme());
    }

    @Override
    public MagnetLink[] parseMagnetLink(URI uri) {
        MagnetOptions[] magnetOptions = MagnetOptions.parseMagnet(uri.toString());
        MagnetLink[] magnetLinks = new MagnetLink[magnetOptions.length];
        for (int i = 0; i < magnetLinks.length; i++) {
            magnetLinks[i] = new MagnetLinkImpl(magnetOptions[i]);
        }
        return magnetLinks;
    }
}
