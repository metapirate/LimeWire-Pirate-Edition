package org.limewire.core.settings;

import org.limewire.setting.BooleanSetting;
import org.limewire.setting.ByteSetting;
import org.limewire.setting.FloatSetting;
import org.limewire.setting.IntSetting;
import org.limewire.setting.LongSetting;
import org.limewire.setting.PowerOfTwoSetting;
import org.limewire.setting.StringArraySetting;
import org.limewire.setting.StringSetting;

/**
 * Settings for Gnutella TCP connections.
 */
public final class ConnectionSettings extends LimeProps {

    private ConnectionSettings() {
    }

    /**
     * Constants for proxy settings.
     */
    public static final int C_NO_PROXY = 0;

    public static final int C_SOCKS4_PROXY = 4;

    public static final int C_SOCKS5_PROXY = 5;

    public static final int C_HTTP_PROXY = 1;

    /**
     * Settings for whether or not an incoming connection has ever been
     * accepted.
     */
    public static final BooleanSetting EVER_ACCEPTED_INCOMING = FACTORY.createBooleanSetting(
            "EVER_ACCEPTED_INCOMING", false);

    /**
     * Setting for whether we have ever determined that we are not able to do
     * Firewall-to-firewall transfers in the past based on information received
     * in pongs.
     */
    public static final BooleanSetting CANNOT_DO_FWT = FACTORY.createExpirableBooleanSetting(
            "LAST_FWT_STATE", false);

    /**
     * Whether we are behind a stable port (i.e., our NAT is not changing it
     * per-host or per-request).
     */
    public static final BooleanSetting HAS_STABLE_PORT = FACTORY.createBooleanSetting(
            "HAS_STABLE_PORT", true);

    /**
     * Settings for whether or not to automatically connect to the network on
     * startup.
     */
    public static final BooleanSetting CONNECT_ON_STARTUP = FACTORY.createBooleanSetting(
            "CONNECT_ON_STARTUP_2", true);

    /**
     * Settings for the number of connections to maintain.
     */
    public static final IntSetting NUM_CONNECTIONS = FACTORY.createRemoteIntSetting(
            "NUM_CONNECTIONS", 32);

    /** The maximum ratio of non-limewire peers to allow. */
    public static final FloatSetting MAX_NON_LIME_PEERS = FACTORY.createRemoteFloatSetting(
            "MAX_NON_LIME_PEERS", 0.2f);

    /** The minimum ratio of non-limewire peers to allow. */
    public static final FloatSetting MIN_NON_LIME_PEERS = FACTORY.createRemoteFloatSetting(
            "MIN_NON_LIME_PEERS", 0.1f);

    /**
     * Setting for the "soft max" ttl. This is the limit for hops+ttl on
     * incoming messages. The soft max is invoked if the following is true:
     * <p>
     * 
     * ttl + hops > SOFT_MAX
     * <p>
     * 
     * If this is the case, the TTL is set to SOFT_MAX - hops.
     */
    public static final ByteSetting SOFT_MAX = FACTORY.createRemoteByteSetting("SOFT_MAX",
            (byte) 3);

    /**
     * Settings for whether or not to local addresses should be considered
     * private, and therefore ignored when connecting.
     */
    public static final BooleanSetting LOCAL_IS_PRIVATE = FACTORY.createBooleanSetting(
            "LOCAL_IS_PRIVATE", true);

    /**
     * Whether to filter incoming/outgoing pongs by class C network. Necessary
     * for testing.
     */
    public static final BooleanSetting FILTER_CLASS_C = FACTORY.createBooleanSetting(
            "FILTER_CLASS_C", true);

    /**
     * Whether to allow duplicate incoming connections. Necessary for testing.
     */
    public static final BooleanSetting ALLOW_DUPLICATE = FACTORY.createBooleanSetting(
            "ALLOW_DUPLICATE", false);

    /**
     * Setting for whether or not to activate the connection watchdog thread.
     * Particularly useful in testing.
     */
    public static final BooleanSetting WATCHDOG_ACTIVE = FACTORY.createBooleanSetting(
            "WATCHDOG_ACTIVE", true);

    /**
     * Setting for the multicast address.
     */
    public static final StringSetting MULTICAST_ADDRESS = FACTORY.createStringSetting(
            "MULTICAST_ADDRESS", "234.21.81.1");

    /**
     * Setting for the multicast port.
     */
    public static final IntSetting MULTICAST_PORT = FACTORY
            .createIntSetting("MULTICAST_PORT", 6347);

    /**
     * Setting for whether or not to allow multicast message loopback.
     */
    public static final BooleanSetting ALLOW_MULTICAST_LOOPBACK = FACTORY.createBooleanSetting(
            "ALLOW_MULTICAST_LOOPBACK", false);

    /**
     * Setting for whether or not to use connection preferencing -- used
     * primarily for testing.
     */
    public static final BooleanSetting PREFERENCING_ACTIVE = FACTORY.createBooleanSetting(
            "PREFERENCING_ACTIVE", true);

    /**
     * Setting for whether or not connections should be allowed to be made while
     * we're disconnected.
     */
    public static final BooleanSetting ALLOW_WHILE_DISCONNECTED = FACTORY.createBooleanSetting(
            "ALLOW_WHILE_DISCONNECTED", false);

    /**
     * Setting for whether or not the removal of connections should be allowed
     * -- used for testing.
     */
    public static final BooleanSetting REMOVE_ENABLED = FACTORY.createBooleanSetting(
            "REMOVE_ENABLED", true);

    /**
     * Setting for whether or not hosts should exchange QRP tables. This is
     * particularly useful for testing.
     */
    public static BooleanSetting SEND_QRP = FACTORY.createBooleanSetting("SEND_QRP", true);

    /**
     * Setting for whether or not we'll accept incoming connections that are
     * compressed via deflate.
     */
    public static final BooleanSetting ACCEPT_DEFLATE = FACTORY.createBooleanSetting(
            "ACCEPT_GNUTELLA_DEFLATE", true);

    /**
     * Setting for whether or not we'll encode outgoing connections via deflate.
     */
    public static final BooleanSetting ENCODE_DEFLATE = FACTORY.createBooleanSetting(
            "ENCODE_GNUTELLA_DEFLATE", true);

    /**
     * The time to live.
     */
    public static final ByteSetting TTL = FACTORY.createByteSetting("TTL", (byte) 4);

    /**
     * Sets whether or not the users ip address should be forced to the value
     * they have entered.
     */
    public static final BooleanSetting FORCE_IP_ADDRESS = FACTORY.createBooleanSetting(
            "FORCE_IP_ADDRESS", false);

    /**
     * Forces IP address to the given address.
     */
    public static final StringSetting FORCED_IP_ADDRESS_STRING = (StringSetting) FACTORY
            .createStringSetting("FORCED_IP_ADDRESS_STRING", "0.0.0.0").setPrivate(true);

    /**
     * The port to use when forcing the ip address.
     */
    public static final IntSetting FORCED_PORT = FACTORY.createIntSetting("FORCED_PORT", 6346);

    /**
     * Whether we should not try to use UPnP to open ports.
     */
    public static final BooleanSetting DISABLE_UPNP = FACTORY.createBooleanSetting("DISABLE_UPNP",
            false);

    /**
     * Whether we are currently using UPNP - used to detect whether clearing of
     * the mappings on shutdown was definitely not successful. Since the
     * shutdown hooks may fail, this cannot guarantee if it was successful.
     */
    public static final BooleanSetting UPNP_IN_USE = FACTORY.createBooleanSetting("UPNP_IN_USE",
            false);

    public static final String CONNECT_STRING_FIRST_WORD = "GNUTELLA";

    public static final StringSetting CONNECT_STRING = FACTORY.createStringSetting(
            "CONNECT_STRING", "GNUTELLA CONNECT/0.4");

    public static final StringSetting CONNECT_OK_STRING = FACTORY.createStringSetting(
            "CONNECT_OK_STRING", "GNUTELLA OK");

    /** Whether or not to bind to a specific address for outgoing connections. */
    public static final BooleanSetting CUSTOM_NETWORK_INTERFACE = FACTORY.createBooleanSetting(
            "CUSTOM_NETWORK_INTERFACE", false);

    /** The inetaddress to use if we're using a custom interface for binding. */
    public static final StringSetting CUSTOM_INETADRESS = FACTORY.createStringSetting(
            "CUSTOM_INETADRESS_TO_BIND", "0.0.0.0");

    /**
     * Setting for the address of the proxy.
     */
    public static final StringSetting PROXY_HOST = FACTORY.createStringSetting("PROXY_HOST", "");

    /**
     * Setting for the port of the proxy.
     */
    public static final IntSetting PROXY_PORT = FACTORY.createIntSetting("PROXY_PORT", 0);

    /**
     * Setting for whether to use the proxy for private IP addresses.
     */
    public static final BooleanSetting USE_PROXY_FOR_PRIVATE = FACTORY.createBooleanSetting(
            "USE_PROXY_FOR_PRIVATE", false);

    /**
     * Setting for which proxy type to use or if any at all.
     */
    public static final IntSetting CONNECTION_METHOD = FACTORY.createIntSetting("CONNECTION_TYPE",
            C_NO_PROXY);

    /**
     * Setting for whether or not to authenticate at the remote proxy.
     */
    public static final BooleanSetting PROXY_AUTHENTICATE = FACTORY.createBooleanSetting(
            "PROXY_AUTHENTICATE", false);

    /**
     * Setting for the username to use for the proxy.
     */
    public static final StringSetting PROXY_USERNAME = FACTORY.createStringSetting(
            "PROXY_USERNAME", "");

    /**
     * Setting for the password to use for the proxy.
     */
    public static final StringSetting PROXY_PASS = FACTORY.createStringSetting("PROXY_PASS", "");

    /**
     * Setting for locale preferencing.
     */
    public static final BooleanSetting USE_LOCALE_PREF = FACTORY.createBooleanSetting(
            "USE_LOCALE_PREF", true);

    /**
     * Number of slots to reserve for those connections that match the local
     * locale.
     */
    public static final IntSetting NUM_LOCALE_PREF = FACTORY.createIntSetting("NUMBER_LOCALE_PREF",
            2);

    /**
     * How many attempts to connect to a remote host must elapse before we start
     * accepting non-LW vendors as UPs.
     */
    public static final IntSetting LIME_ATTEMPTS = FACTORY.createIntSetting("LIME_ATTEMPTS", 50);

    /**
     * How long we believe firewalls will let us send solicited udp traffic.
     * Field tests show at least a minute with most firewalls, so lets try 55
     * seconds.
     */
    public static final LongSetting SOLICITED_GRACE_PERIOD = FACTORY.createLongSetting(
            "SOLICITED_GRACE_PERIOD", 85000l);

    /**
     * How many pongs to send back for each ping.
     */
    public static final IntSetting NUM_RETURN_PONGS = FACTORY.createRemoteIntSetting(
            "NUM_RETURN_PONGS", 10);

    /**
     * The percentage of dht hosts to gnutella hosts in pongs.
     */
    public static final IntSetting DHT_TO_GNUT_HOSTS_PONG = FACTORY.createRemoteIntSetting(
            "DHT_TO_GNUT_HOSTS_PONG", 50);

    /**
     * Setting to disable bootstrapping.. used only in tests.
     */
    public static final BooleanSetting DO_NOT_BOOTSTRAP = FACTORY.createBooleanSetting(
            "DO_NOT_BOOTSTRAP", false);

    /**
     * Setting to not send a multicast bootstrap ping.
     */
    public static final BooleanSetting DO_NOT_MULTICAST_BOOTSTRAP = FACTORY.createBooleanSetting(
            "DO_NOT_MULTICAST_BOOTSTRAP", false);

    /**
     * How long to try hosts from gnutella.net before bootstrapping (ms).
     */
    public static final IntSetting BOOTSTRAP_DELAY = FACTORY.createRemoteIntSetting(
            "BOOTSTRAP_DELAY", 20000);

    /**
     * Time in milliseconds to delay prior to flushing data on peer -> peer.
     * connections.
     */
    public static final LongSetting FLUSH_DELAY_TIME = FACTORY.createLongSetting(
            "FLUSH_DELAY_TIME_2", 0);

    /**
     * Lowercase hosts that are evil.
     */
    public static final StringArraySetting EVIL_HOSTS = FACTORY.createStringArraySetting(
            "EVIL_HOSTS_2", new String[0]);

    /**
     * How many connections to maintain as a leaf when idle.
     */
    public static final IntSetting IDLE_CONNECTIONS = FACTORY.createIntSetting(
            "IDLE_CONNECTIONS_2", 2);

    /**
     * The maximum line length we'll try to parse while reading a header.
     */
    public static final IntSetting MAX_HANDSHAKE_LINE_SIZE = FACTORY.createRemoteIntSetting(
            "MAX_HANDSHAKE_LINE_SIZE", 1024);

    /**
     * The maximum number of headers to try and parse.
     */
    public static final IntSetting MAX_HANDSHAKE_HEADERS = FACTORY.createRemoteIntSetting(
            "MAX_HANDSHAKZE_LIMIT", 30);

    /**
     * The size of our Query Routing Tables, in multiples of 1024 entries. (In
     * 1998, the IEC standardized "kibi" as the prefix denoting 1024, or
     * "kilo binary".)
     */
    public static final PowerOfTwoSetting QRT_SIZE_IN_KIBI_ENTRIES = FACTORY
            .createRemotePowerOfTwoSetting("QRT_SIZE_IN_KIBI_ENTRIES", 128);

    public static final IntSetting STABLE_PERCONNECT_MESSAGES_THRESHOLD = FACTORY.createIntSetting(
            "STABLE_MESSAGES_THRESHOLD", 5);

    public static final IntSetting STABLE_TOTAL_MESSAGES_THRESHOLD = FACTORY.createIntSetting(
            "STABLE_TOTAL_MESSAGES_THRESHOLD", 45);
    
    /**
     * Minimum number of bytes to have been received between two connection
     * watch dog checks for the watchdog not to close the connection.
     */
    public static final IntSetting MIN_BYTES_RECEIVED = FACTORY.createRemoteIntSetting(
            "MIN_BYTES_RECEIVED", 1024);
    
    /**
     * Minimum number of bytes to have been sent between two connection
     * watch dog checks for the watchdog not to close the connection.
     */
    public static final IntSetting MIN_BYTES_SENT = FACTORY.createRemoteIntSetting(
            "MIN_BYTES_SENT", 1024);

    /** Bootstrap servers. */
    public static final StringArraySetting BOOTSTRAP_SERVERS =
        FACTORY.createStringArraySetting("ConnectionSettings.bootstrapServers",
                new String[0]); // ADD BOOTSTRAP SERVERS HERE
}
