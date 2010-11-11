package com.limegroup.gnutella.rudp;

import org.limewire.core.settings.DownloadSettings;
import org.limewire.rudp.RUDPSettings;

import com.google.inject.Singleton;

@Singleton
class LimeRUDPSettings implements RUDPSettings {

    public int getMaxSkipAcks() {
        return DownloadSettings.MAX_SKIP_ACKS.getValue();
    }

    public float getMaxSkipDeviation() {
        return DownloadSettings.DEVIATION.getValue();
    }

    public int getSkipAckHistorySize() {
        return DownloadSettings.HISTORY_SIZE.getValue();
    }

    public int getSkipAckPeriodLength() {
        return DownloadSettings.PERIOD_LENGTH.getValue();
    }

    public boolean isSkipAcksEnabled() {
        return DownloadSettings.SKIP_ACKS.getValue();
    }

}
