package com.limegroup.gnutella.downloader.serial;

public interface InNetworkDownloadMemento extends GnutellaDownloadMemento {

    public String getTigerTreeRoot();

    public int getDownloadAttempts();

    public long getStartTime();

    public void setTigerTreeRoot(String tigerTreeRoot);

    public void setStartTime(long startTime);

    public void setDownloadAttempts(int downloadAttempts);

}