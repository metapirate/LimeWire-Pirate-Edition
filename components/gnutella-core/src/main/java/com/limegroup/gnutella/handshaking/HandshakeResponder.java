package com.limegroup.gnutella.handshaking;


/**
 * Provides a servent ways to set connection handshake responses in response to 
 * a connection handshake response just received.  Note, however, incoming 
 * connections and outgoing connections will differ in the use 
 * of this interface.  
 * 
 * Outgoing connections use the interface after receiving a handshake response
 * from a remote host that it tried to connect to.  Here is a typical anonymous 
 * implementation of HandshakeResponder for outgoing connections:
 * <pre>
 * new HandshakeResponder() {
 *     public HandshakeResponse respond(HandshakeResponse response, 
 *                                      boolean outgoing) {
 *          //Checks for a "200 OK" response and sends a "userid" header
 *          //otherwise, returns null.  Also, checks to make sure the 
 *          //the connection is an outgoing one.
 *          if (!outgoing)
 *              return null;
 *          if (response.getStatusCode() == HandshakeResponse.OK) {
 *              Properties headers = new Properties();
 *              headers.setProperty("Userid", "Limewire");
 *              return new HandshakeResponse(headers);
 *          }
 *          return null;
 *     }
 * }
 * </pre>
 *
 * Incoming connections use the interface after reading headers from a remote
 * host.  Hence, they don't care about the status code and status response, only
 * the headers they received.  Here is a typical anonymous implementation of 
 * HandshakeResponder for incoming connections:
 *<pre>
 * new HandshakeResponder() {
 *     public HandshakeResponse respond(HandshakeResponse response, 
 *                                      boolean outgoing) {
 *          //first, checks to make sure the connection is an incoming one.  
 *          //Also, checks for a "userid" header and if not sets the 
 *          //"Authorization" header and appropriate status code.
 *          if (outgoing)
 *              return null;
 *          Properties read = response.getHeaders();
 *          if (read.getProperty("userid")== null) {
 *              Properties headers = new Properties();
 *              headers.setProperty("Authorization", "needed");
 *              return new HandshakeResponse(401, "Unauthorized", headers);
 *          }
 *          //return "200 OK" with no headers
 *          return new HandshakeResponse();
 *     }
 * }
 * </pre>
 * 
 */
public interface HandshakeResponder {
    /** 
     * Returns the corresponding handshake to be written to the remote host when
     * responding to the connection handshake response just received.  
     * Implementations should respond differently to incoming vs. outgoing 
     * connections.   
     * @param response The response received from the host on the
     * other side of teh connection.
     * @param outgoing whether the connection to the remote host is an outgoing
     * connection.
     */
    public HandshakeResponse respond(HandshakeResponse response, 
         boolean outgoing);

    /**
     * Optional method.<p>
     * Note: should this throw an UnsupportedOperationException
     */
    public void setLocalePreferencing(boolean b);
}
