package com.limegroup.gnutella.messages;

public interface LocalPongInfo {

    long getNumSharedFiles();

    int getSharedFileSize();

    boolean isSupernode();

    byte getNumFreeLimeWireLeafSlots();

    byte getNumFreeLimeWireNonLeafSlots();

}
