package com.limegroup.gnutella;

/**
 * The minimum amount of information that a remote client would
 * need to download the file.  <code>HostDetails</code> are typically
 * serialized in a <code>QueryReply</code>.  <code>FileDetails</code> are
 * typically serialzied in a <code>Response</code> object inside a <code>QueryReply</code>.
 */
public interface RemoteFileDetails extends FileDetails, HostDetails {
}
