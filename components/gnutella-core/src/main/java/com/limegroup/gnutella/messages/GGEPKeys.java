package com.limegroup.gnutella.messages;

public class GGEPKeys {

    /** The extension header (key) for Browse Host. */
    public static final String GGEP_HEADER_BROWSE_HOST = "BH";
    /** The extension header (key) for average daily uptime. */
    public static final String GGEP_HEADER_DAILY_AVERAGE_UPTIME = "DU";
    /** The extension header (key) for unicast protocol support. */
    public static final String GGEP_HEADER_UNICAST_SUPPORT = "GUE";
    /** The extension header (key) for Ultrapeer support. */
    public static final String GGEP_HEADER_UP_SUPPORT = "UP";
    /** The extension header (key) for AddressSecurityToken support. */
    public static final String GGEP_HEADER_QUERY_KEY_SUPPORT = "QK";
    /** 
     * The extension header (key) for oob query requests to require the new
     * protocol version of oob messages that supports security tokens for
     * authentication.
     */
    public static final String GGEP_HEADER_SECURE_OOB = "SO";
    /** The extension header (key) for AddressSecurityToken support. */
    public static final String GGEP_HEADER_MULTICAST_RESPONSE = "MCAST";
    /** The extension header (key) for PushProxy support. */
    public static final String GGEP_HEADER_PUSH_PROXY = "PUSH";
    /** The extension header (key) for PushProxy TLS indexes. */
    public static final String GGEP_HEADER_PUSH_PROXY_TLS = "PUSH_TLS";
    /** The extension header (key) for AlternateLocation support. */
    public static final String GGEP_HEADER_ALTS = "ALT";
    /** The extension header (key) for AlternateLocations that support TLS. */
    public static final String GGEP_HEADER_ALTS_TLS = "ALT_TLS";
    /** The extention header (key) for IpPort request. */
    public static final String GGEP_HEADER_IPPORT="IP";
    /** The extension header (key) for UDP HostCache pongs. */
    public static final String GGEP_HEADER_UDP_HOST_CACHE = "UDPHC";
    /** The extension header (key) for indicating support for packed ip/ports & udp host caches. */
    public static final String GGEP_HEADER_SUPPORT_CACHE_PONGS = "SCP";
    /** The extension header (key) for packed IP/Ports. */
    public static final String GGEP_HEADER_PACKED_IPPORTS="IPP";
    /** The extension header (key) for which packed IP/Ports support TLS. */
    public static final String GGEP_HEADER_PACKED_IPPORTS_TLS="IPP_TLS";
    /** The extension header (key) for understanding TLS. */
    public static final String GGEP_HEADER_TLS_CAPABLE="TLS";
    /** The extension header (key) for packed UDP Host Caches. */
    public static final String GGEP_HEADER_PACKED_HOSTCACHES="PHC";
    /** The extension header (key) for SHA1 urns. */
    public static final String GGEP_HEADER_SHA1 = "S1";
    /** The extension header (key) for TTROOT urns. */
    public static final String GGEP_HEADER_TTROOT = "TT";
    /** The extension header (key) to determine if a SHA1 is valid. */
    public static final String GGEP_HEADER_SHA1_VALID = "SV";
    /** The extension header (key) for DHT support. */
    public static final String GGEP_HEADER_DHT_SUPPORT = "DHT";
    /** The extension header (key) for DHT IPP requests. */
    public static final String GGEP_HEADER_DHT_IPPORTS = "DHTIPP";
    /**
     * The extension header (key) for a feature query.
     * This is 'WH' for legacy reasons, because 'What is New' was the first.
     */
    public static final String GGEP_HEADER_FEATURE_QUERY = "WH";

    /**
     * To support queries longer than previous length limit
     * on query string fields.
     */
    public static final String GGEP_HEADER_EXTENDED_QUERY = "XQ";

    /** The extension header disabling OOB proxying. */
    public static final String GGEP_HEADER_NO_PROXY = "NP";
    /** The extension header (key) for MetaType query support. */
    public static final String GGEP_HEADER_META = "M";
    /** The extension header (key) for client locale. */
    public static final String GGEP_HEADER_CLIENT_LOCALE = "LOC";
    /** The extension header (key) for creation time. */
    public static final String GGEP_HEADER_CREATE_TIME = "CT";
    /** The extension header (key) for Firewalled Transfer support in Hits. */
    public static final String GGEP_HEADER_FW_TRANS = "FW";
    /** The extension header (key) indicating the GGEP block is the 'secure' block. */
    public static final String GGEP_HEADER_SECURE_BLOCK = "SB";
    /** The extension header (key) indicating the value has a signature in it. */
    public static final String GGEP_HEADER_SIGNATURE = "SIG";
    /** The extention header (key) indicating the size of the file is 64 bit. */
    public static final String GGEP_HEADER_LARGE_FILE = "LF";
    /** The prefix of the extention header (key) indicating support for partial results. */
    public static final String GGEP_HEADER_PARTIAL_RESULT_PREFIX = "PR";
    /** The extension header (key) to determine if the encoded ranges are unverified. */
    public static final String GGEP_HEADER_PARTIAL_RESULT_UNVERIFIED = "PRU";
    /** Various information contained in a return path entry GGEP block. */
    public static final String GGEP_HEADER_RETURN_PATH_SOURCE = "RPS";
    public static final String GGEP_HEADER_RETURN_PATH_HOPS = "RPH";
    public static final String GGEP_HEADER_RETURN_PATH_ME = "RPI";
    public static final String GGEP_HEADER_RETURN_PATH_TTL = "RPT";
    /** The extension header key to signal interest in non-metadata sha1 urns. */
    public static final String GGEP_HEADER_NMS1 = "NM";
}
