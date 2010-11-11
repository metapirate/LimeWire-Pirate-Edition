package com.limegroup.gnutella.version;

import com.limegroup.gnutella.URN;

public interface DownloadInformation {
    
    public URN getUpdateURN();
    public String getTTRoot();
    public String getUpdateCommand();
    public String getUpdateFileName();
    public long getSize();
    
}