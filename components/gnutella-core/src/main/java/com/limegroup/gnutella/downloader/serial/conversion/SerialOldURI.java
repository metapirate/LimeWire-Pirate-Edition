package com.limegroup.gnutella.downloader.serial.conversion;

import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;

import org.limewire.util.URIUtils;


/**
 * Helps to migrate persistent org.apache.commons.httpclient.URI's to java.net.URI's.
 */
@SuppressWarnings("unused")
public class SerialOldURI implements Serializable {
    private static final long serialVersionUID = 604752400577948726L;


    private int hash;
    private char[] _uri;
    private String protocolCharset;
    private char[] _scheme;
    private char[] _opaque;
    private char[] _authority;
    private char[] _userinfo;
    private char[] _host;
    private int _port;
    private char[] _path;
    private char[] _query;
    private char[] _fragment;
    private boolean _is_hier_part;
    private boolean _is_opaque_part;
    private boolean _is_net_path;
    private boolean _is_abs_path;
    private boolean _is_rel_path;
    private boolean _is_reg_name;
    private boolean _is_server; 
    private boolean _is_hostname;
    private boolean _is_IPv4address;
    private boolean _is_IPv6reference;

    public URI toURI() throws URISyntaxException {
        return (_uri == null) ? null : URIUtils.toURI(new String(_uri));    
    }
}
