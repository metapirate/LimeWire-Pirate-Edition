package com.limegroup.gnutella;


import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import org.limewire.io.Connectable;
import org.limewire.io.IpPort;
import org.limewire.io.NetworkUtils;
import org.limewire.util.ByteUtils;
import org.limewire.util.StringUtils;


/**
 * Immutable IP/port pair.  Also contains an optional number and size
 * of files, mainly for legacy reasons.
 */
public class Endpoint implements Cloneable, Connectable, IpPort, java.io.Serializable {

    private static final long serialVersionUID = 4686711693494625070L; 
    
    private volatile InetAddress addr = null;
    private volatile String hostname = null;
    private volatile int port = 0;
    /** Number of files at the host, or -1 if unknown */
    private volatile long files=-1;
    /** Size of all files on the host, or -1 if unknown */
    private volatile long kbytes=-1;
    
    // so subclasses can serialize.
    protected Endpoint() { }

    /**
     * Returns a new Endpoint from a Gnutella-style host/port pair:
     * <ul>
     * <li>If hostAndPort is of the format "host:port", where port
     *   is a number, returns new Endpoint(host, port).
     * <li>If hostAndPort contains no ":" or a ":" at the end of the string,
     *   returns new Endpoint(hostAndPort, 6346).
     * <li>Otherwise throws IllegalArgumentException.
     * </ul>
     */
    public Endpoint(String hostAndPort) throws IllegalArgumentException 
    {
        this(hostAndPort, false);
    }

    /**
     * Same as new Endpoint(hostAndPort) but with additional restrictions on
     * hostAndPart; if requireNumeric==true and the host part of hostAndPort is
     * not as a numeric dotted-quad IP address, throws IllegalArgumentException.
     * Examples:
     * <pre>
     * new Endpoint("www.limewire.org:6346", false) ==> ok
     * new Endpoint("not a url:6346", false) ==> ok
     * new Endpoint("www.limewire.org:6346", true) ==> IllegalArgumentException
     * new Endpoint("64.61.25.172:6346", true) ==> ok
     * new Endpoint("64.61.25.172", true) ==> ok
     * new Endpoint("127.0.0.1:ABC", false) ==> IllegalArgumentException     
     * </pre> 
     *
     * If requireNumeric is true no DNS lookups are ever involved.
     * If requireNumeric is false a DNS lookup MAY be performed if the hostname
     * is not numeric.
     *
     * @see Endpoint (String))
     */
    public Endpoint(String hostAndPort, boolean requireNumeric) {
        this(hostAndPort, requireNumeric, true);
    }

    /**
     * Constructs a new endpoint.
     * If requireNumeric is true, or strict is false, no DNS lookups are ever involved.
     * If requireNumeric is false or strict is true, a DNS lookup MAY be performed
     * if the hostname is not numeric.
     *
     * To never block, make sure strict is false.
     */  
    public Endpoint(String hostAndPort, boolean requireNumeric, boolean strict) {
        final int DEFAULT=6346;
        int j=hostAndPort.indexOf(":");
        if (j<0) {
            this.hostname = hostAndPort;
            this.port=DEFAULT;
        } else if (j==0) {
            throw new IllegalArgumentException();
        } else if (j==(hostAndPort.length()-1)) {
            this.hostname = hostAndPort.substring(0,j);
            this.port=DEFAULT;
        } else {
            this.hostname = hostAndPort.substring(0,j);
            try {
                this.port=Integer.parseInt(hostAndPort.substring(j+1));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException();
            }
            
			if(!NetworkUtils.isValidPort(getPort()))
			    throw new IllegalArgumentException("invalid port");
        }

        if (requireNumeric)  {
            //TODO3: implement with fewer allocations
            String[] numbers=StringUtils.split(hostname, '.');
            if (numbers.length!=4)
                throw new IllegalArgumentException();
            for (int i=0; i<numbers.length; i++)  {
                try {
                    int x=Integer.parseInt(numbers[i]);
                    if (x<0 || x>255)
                        throw new IllegalArgumentException();
                } catch (NumberFormatException fail) {
                    throw new IllegalArgumentException();
                }
            }
        }
        
        if(strict && !NetworkUtils.isValidAddress(hostname))
            throw new IllegalArgumentException("invalid address: " + hostname);
    }

    public Endpoint(String hostname, int port) {
        this(hostname, port, true);
    }
    
    public Endpoint(InetAddress addr, int port) {
        if(!NetworkUtils.isValidPort(port))
            throw new IllegalArgumentException("invalid port: "+port);
        if(!NetworkUtils.isValidAddress(addr))
            throw new IllegalArgumentException("invalid address: " + addr);

        this.addr = addr;
        this.hostname = addr.getHostAddress();
        this.port=port;
    }
    
    /**
     * Constructs a new endpoint using the specific hostname & port.
     * If strict is true, this does a DNS lookup against the name,
     * failing if the lookup couldn't complete.
     */
    public Endpoint(String hostname, int port, boolean strict) {
        if(!NetworkUtils.isValidPort(port))
            throw new IllegalArgumentException("invalid port: "+port);
        if(strict && !NetworkUtils.isValidAddress(hostname))
            throw new IllegalArgumentException("invalid address: " + hostname);

        this.hostname = hostname;
        this.port=port;
    }

    /**
    * Creates a new Endpoint instance
    * @param hostBytes IP address of the host (MSB first)
    * @param port The port number for the host
    */
    public Endpoint(byte[] hostBytes, int port) {
        if(!NetworkUtils.isValidPort(port))
            throw new IllegalArgumentException("invalid port: "+port);
        if(!NetworkUtils.isValidAddress(hostBytes))
            throw new IllegalArgumentException("invalid address");

        this.port = port;
        this.hostname = NetworkUtils.ip2string(hostBytes);
    }
    
    
    /**
     * @param files the number of files the host has
     * @param kbytes the size of all of the files, in kilobytes
     */
    public Endpoint(String hostname, int port, long files, long kbytes)
    {
        this(hostname, port);
        this.files=files;
        this.kbytes=kbytes;
    }
    
    /**
    * Creates a new Endpoint instance
    * @param hostBytes IP address of the host (MSB first)
    * @param port The port number for the host
    * @param files the number of files the host has
    * @param kbytes the size of all of the files, in kilobytes
    */
    public Endpoint(byte[] hostBytes, int port, long files, long kbytes)
    {
        this(hostBytes, port);
        this.files=files;
        this.kbytes=kbytes;
    }
    
    
    /**
    * Constructs a new endpoint from pre-existing endpoint by copying the
    * fields
    * @param ep The endpoint from whom to initialize the member fields of
    * this new endpoint
    */
    public Endpoint(Endpoint ep)
    {
        this.files = ep.files;
        this.hostname = ep.hostname;
        this.kbytes = ep.kbytes;
        this.port = ep.port;
    }

    @Override
    public String toString()
    {
        return hostname+":"+port;
    }

    public String getAddress()
    {
        return hostname;
    }
    
    /**
     * Accessor for the <tt>InetAddress</tt> instance for this host.  Implements
     * <tt>IpPort</tt> interface.
     * 
     * @return the <tt>InetAddress</tt> for this host, or <tt>null</tt> if the
     *  <tt>InetAddress</tt> cannot be created
     */
    public InetAddress getInetAddress() {
        if(addr != null)
            return addr;
        
        try {
            addr = InetAddress.getByName(hostname);
            return addr;
        } catch (UnknownHostException e) {
            return null;
        }
    }

    public void setHostname(String hostname)
    {
        this.hostname = hostname;
        this.addr = null;
    }

    public int getPort()
    {
        return port;
    }

    public InetSocketAddress getInetSocketAddress() {
        InetAddress addr = getInetAddress();
        if (addr == null) {
            return null;
        }
        
        return new InetSocketAddress(addr, getPort());
    }
    
    @Override
    public String getAddressDescription() {
        return getInetSocketAddress().toString();
    }
    
    /** Returns the number of files the host has, or -1 if I don't know */
    public long getFiles()
    {
        return files;
    }

    /** Sets the number of files the host has */
    public void setFiles(long files)
    {
        this.files = files;
    }

    /** Returns the size of all files the host has, in kilobytes,
     *  or -1 if I don't know, it also makes sure that the kbytes/files
     *  ratio is not ridiculous, in which case it normalizes the values
     */
    public long getKbytes()
    {
        return kbytes;
    }

    /**
     * If the number of files or the kbytes exceed certain limit, it
     * considers them as false data, and initializes the number of
     * files as well as kbytes to zero in that case
     */
    public void normalizeFilesAndSize()
    {
        //normalize files
        try
        {
            if(kbytes > 20000000) // > 20GB
            {
                files = kbytes = 0;
                return;
            }
            else if(files > 5000)  //> 5000 files
            {
                files = kbytes = 0;
                return;
            }
            else if (kbytes/files > 250000) //> 250MB/file
            {
                files = kbytes = 0;
                return;
            }   
        }
        catch(ArithmeticException ae)
        {
            files = kbytes = 0;
            return;
        }

    }

    /** Sets the size of all files the host has, in kilobytes,
     */
    public void setKbytes(long kbytes)
    {
        this.kbytes = kbytes;
    }

    /**
     * Endpoints are equal if their hostnames and ports are.  The number
     * and size of files does not matter.
     */
    @Override
    public boolean equals(Object o) {
        if(!(o instanceof Endpoint))
            return false;
        if(o == this)
            return true;
        Endpoint e=(Endpoint)o;
        return hostname.equals(e.hostname) && port==e.port;
    }

    @Override
    public int hashCode()
    {
        //This is good enough, since one host rarely has multiple ports.
        return hostname.hashCode();
    }


    @Override
    protected Object clone()
    {
        return new Endpoint(new String(hostname), port, files, kbytes);
    }

    /**
     *This method  returns the IP of the end point as an array of bytes
     */
    public byte[] getHostBytes() throws UnknownHostException {
        InetAddress ad = getInetAddress();
        if(ad == null)
            throw new UnknownHostException(hostname);
        else
            return ad.getAddress();
    }

    /**
     * @requires this and other have dotted-quad addresses, or
     *  names that can be resolved.
     * @effects Returns true if this is on the same subnet as 'other',
     *  i.e., if this and other are in the same IP class and have the
     *  same network number.
     */
    public boolean isSameSubnet(Endpoint other) {
        byte[] a;
        byte[] b;
        int first;
        try {
            a=getHostBytes();
            first=ByteUtils.ubyte2int(a[0]);
            b=other.getHostBytes();
        } catch (UnknownHostException e) {
            return false;
        }

        //See http://www.3com.com/nsc/501302.html
        //class A
        if (first<=127)
            return a[0]==b[0];
        //class B
        else if (first <= 191)
            return a[0]==b[0] && a[1]==b[1];
        //class C
        else
            return a[0]==b[0] && a[1]==b[1] && a[2]==b[2];
    }
    
    /** Determines if this is a UDP host cache. */
    public boolean isUDPHostCache() {
        return false;
    }
    
    /** Determines if this endpoint supports TLS. */
    public boolean isTLSCapable() {
        return false;
    }
}

