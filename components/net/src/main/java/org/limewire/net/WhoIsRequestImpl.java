package org.limewire.net;

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.StringTokenizer;

import org.limewire.io.IP;
import org.limewire.io.NetworkUtils;
import org.limewire.io.IOUtils;
import org.limewire.util.StringUtils;

/**
 * Represents a WHOIS request. The request value will be
 * sent to the appropriate whois server, and the response
 * will be parsed for name:value pairs.
 * <p>
 * Some whois servers merely report the authoritative
 * server for a given domain, and we support ONE level of
 * forwarding here.
 * 
 */
public class WhoIsRequestImpl implements WhoIsRequest {
    
    /**
     * Map of whois servers keyed on the tld and "0" (zero)
     * for IP addresses.
     */
    protected Map<String,String> servers;
    
    /**
     * The exact query value.
     */
    protected String name;
    
    /**
     * All parsable values in the reply for this request.
     */
    protected Map<String,String> values;
    
    /**
     * Name of the whois server that was used to fulfill
     * this request (assuming that the request has already
     * been fulfilled. Null until then.
     */
    protected String whoisServer;
    
    /**
     * To prevent us from getting caught in any annoying
     * infinite loops, use this value to make sure that
     * we only follow a whois referral once.
     */
    protected Set<String> referrals;
    
    /**
     * The socket manager, as you may have guessed.
     */
    protected SocketsManager socketsManager;
    
    /**
     * Creates a new request object with a request name.
     * It is this request name that will be handed off to
     * the whois server and the request can be either for
     * a DNS name or an IP address.
     * 
     * @param name request name (ie, "apple.com" or 
     * "17.149.160.49").
     */
    public WhoIsRequestImpl(String name, SocketsManager socketsManager, Map<String,String> defaultServers) {
        if (name == null)
            throw new NullPointerException();
        
        if (name.length() == 0)
            throw new IllegalArgumentException("Zero-length name is not allowed.");
        
        // if there are no periods in the request name, 
        // then it is definitely not a valid DNS name nor
        // an IP address.
        //
        // if the period is at the end of the request name,
        // then it is similarly invalid.
        //
        {
            int ndx = name.lastIndexOf(".");
            
            if (ndx == -1 || ndx == name.length() - 1)
                throw new IllegalArgumentException("Invalid request name.");
        }
        
        this.name = name;
        this.values = new HashMap<String,String>();
        this.socketsManager = socketsManager;
        this.referrals = new HashSet<String>();
        this.servers = defaultServers;
    }
    
    /**
     * Connects to the whois server, sends the request and
     * then reads and parses the reply. Parsed values are
     * accessible via getValue().
     * <p>
     * In most circumstances, this should return in well 
     * under one second, but with sockets there is no such
     * guarantee. Don't block in the UI on a call to this.
     * 
     */
    public void doRequest() throws IOException {
        // only parse and find a whois server if there is
        // not already one defined.
        //
        // if this is a second call to doRequest(), based
        // on following a referral from a previous request,
        // then the whois server will have already been
        // defined for us.
        //
        if (this.whoisServer == null)
            this.whoisServer = this.getWhoIsServer();
        
        // no server, no luck
        if (this.whoisServer == null)
            throw new IllegalArgumentException("Unsupported request name, '" + this.name + "'");
        
        // store this whois server so that when following
        // referrals, we do not ever enter a loop.
        //
        this.referrals.add(this.whoisServer);
        
        // there really isn't a standard whois format, 
        // other than to say that there is a rough
        // adherence to a "name:value" format, where the
        // value portion may be on the same line or the
        // next line, or it may be multiple lines.
        //
        // you might say that the value extends until
        // the start of the next "name:value" pair.
        //
        // we will store all names in upper-case.
        //
        // 1. if a value starts on the same line as its
        //    corresponding name, then the value is also
        //    wholly contained on that one line.
        //
        // 2. names are always followed by colons.
        //
        // 3. names are sometimes followed by colon-space.
        //
        // 4. arbitrary whitespace before, after and 
        //    between names and values must be supported.
        //
        // 5. if a name is re-used, only the last value is
        //    retained.
        //
        {
            StringTokenizer st = new StringTokenizer(doRequestGetWhoIsResponse(), "\r\n");
            String name = null;
            StringBuilder value = new StringBuilder(100);
            boolean skipTillRegistrant = false;
            
            // we're splitting on new-lines only.
            while (st.hasMoreTokens()) {
                int index = 0;
                String line = st.nextToken().trim();
                
                // some text is often included with the 
                // whois result, and we want to skip that
                // text. the start of final portion of the
                // data we want to parse appears to always
                // be "Registrant:". look for that point to
                // continue parsing, if we find that we are
                // in a block of text that we don't care 
                // about.
                //
                if (skipTillRegistrant) {
                    if (-1 == line.indexOf("Registrant:"))
                        continue;
                }
                else if (-1 != line.indexOf(">>>") || -1 != line.indexOf("NOTICE:") || -1 != line.indexOf("TERMS OF USE:")) {
                    skipTillRegistrant = true;
                    continue;
                }
                else if (line.startsWith("#")) {
                    continue;
                }
                
                // if we are already parsing a value....
                if (name != null) {
                    // if this is the start of a new name:value pair....
                    if (-1 != (index = line.indexOf(":"))) {
                        this.setValue(name.toUpperCase(Locale.ENGLISH), value.toString());
                        name = line.substring(0, index);
                        value = new StringBuilder(100);
                        
                        // if this is a one-line name:value pair...
                        if ((line = line.substring(index+1).trim()).length() > 0) {
                            if (line.startsWith("//")) {
                                name = null;
                                continue;
                            }
                            
                            this.setValue(name.toUpperCase(Locale.ENGLISH), line);
                            name = null;
                        }
                    }
                    // if this is a continuation of a value....
                    else if (value.length() != 0) {
                        value.append(" ");
                        value.append(line);
                    }
                    else {
                        value.append(line);
                    }
                }
                // if this is the start of a new name:value pair....
                else if (-1 != (index = line.indexOf(":"))) {
                    name = line.substring(0, index);
                    
                    // if this is a one-line name:value pair...
                    if ((line = line.substring(index+1).trim()).length() > 0) {
                        if (line.startsWith("//")) {
                            name = null;
                            continue;
                        }
                        
                        this.setValue(name.toUpperCase(Locale.ENGLISH), line);
                        name = null;
                    }
                }
                // this is garbage text in the reply that we don't care about....
                else {
                }
            }
            
            if (this.name != null)
                this.setValue(this.name.toUpperCase(Locale.ENGLISH), value.toString());
        }
        
        // if this whois response contains the key "WHOIS 
        // SERVER", then we should refer to the target
        // server specified there. only do this once,
        // however.
        //
        // don't follow the referral if the server we are
        // presently talking to is the server mentioned in
        // "WHOIS SERVER".
        //
        {
            String referral = this.values.get("WHOIS SERVER");
            
            if (referral != null && referral.length() != 0 && 
                    !this.referrals.contains(referral)) {
                this.whoisServer = referral;
                this.doRequest();
            }
        }
    }
    
    /**
     * Get an arbitrary value by name.
     * 
     * @param name the name of the value that you want.
     * @return the value associated with the given name.
     */
    public String getValue(String name) {
        return values.get(name);
    }
    
    /**
     * Set a value for the given name.
     * 
     * @param name key name for reply value
     * @param value value to store for the key
     */
    protected void setValue(String name, String value) {
        values.put(name, value);
    }
    
    /**
     * Returns the net range of the ip address, or null if
     * there is none.
     */
    public IP getNetRange () {
        String range = this.getValue("CIDR");
        
        if (range != null)
            return new IP(range);
        
        return null;
    }
    
    /**
     * Contacts the whois server, sends the request, reads
     * and returns the reply.
     * 
     * @return Verbatim reply from the whois server.
     * @throws IOException 
     */
    protected String doRequestGetWhoIsResponse() throws IOException {
        StringBuilder sb = new StringBuilder(1000);
        Socket socket = null;
        
        // connect, send the request, read the reply, close
        // the socket.
        //
        try {
            socket = socketsManager.connect(new InetSocketAddress(this.whoisServer, 43), 0);
            InputStream is = socket.getInputStream();
            OutputStream os = socket.getOutputStream();
            byte[] bytes = new byte[1000];
            int read = 0;
            
            os.write(StringUtils.toAsciiBytes(this.name+"\r\n"));
            
            // read until we hit EOF or the socket closes.
            try {
                while (0 < (read = is.read(bytes, 0, 1000))) {
                    if (read > 0)
                        sb.append(StringUtils.getASCIIString(bytes, 0, read));
                }
                
            }
            catch (IOException e2) {
                // this can occur when the socket closes.
            }
        }
        catch (IOException e) {
            throw e;
        }
        finally {
            IOUtils.close(socket);
        }
        
        return sb.toString();
    }
    
    /**
     * Returns the appropriate whois server based on the
     * server list for the request name.
     * 
     * @return Appropriate whois server host name.
     */
    public String getWhoIsServer() {
        String server = null;
        
        if (NetworkUtils.isDottedIPV4(this.name))
            server = this.servers.get("0");
        else
            server = this.servers.get(this.name.substring(this.name.lastIndexOf(".")+1).toLowerCase(Locale.ENGLISH));
        
        return server;
    }
    
}
