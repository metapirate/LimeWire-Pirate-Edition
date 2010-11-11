package com.limegroup.gnutella.downloader.serial.conversion;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

import com.limegroup.gnutella.URN;

interface SerialRemoteFileDesc {

    public boolean isTlsCapable();

    public String getHttpPushAddr();

    public String getHost();

    public int getPort();

    public String getFilename();

    public long getIndex();

    public byte[] getClientGUID();

    public int getSpeed();

    public long getSize();

    public boolean isChatEnabled();

    public int getQuality();

    public boolean isReplyToMulticast();

    public String getXml();

    public Set<URN> getUrns();

    public boolean isBrowseHostEnabled();

    public boolean isFirewalled();

    public String getVendor();

    public boolean isHttp11();

    public Map<String, Serializable> getPropertiesMap();

}