package org.limewire.xmpp.client.impl;

import org.limewire.util.SystemUtils;

import com.google.inject.Singleton;

@Singleton
public class IdleTimeImpl implements IdleTime {

    @Override
    public long getIdleTime() {
        return SystemUtils.getIdleTime();
    }

    @Override
    public boolean supportsIdleTime() {
        return SystemUtils.supportsIdleTime();
    }
}
