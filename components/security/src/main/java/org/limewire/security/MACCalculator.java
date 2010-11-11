package org.limewire.security;

import org.limewire.security.SecurityToken.TokenData;

/**<p>
 * Defines the interface that represents the embodiment of an algorithm and
 * secret keys used to generate a {@link SecurityToken}.
 * </p><p>
 * Attackers have knowledge of the algorithms implemented here and have the
 * ability to query a host for <code>getMACBytes(TokenData)</code> for many
 * different values. Therefore, it must be computationally infeasible for an
 * attacker, within the lifetime of a given <code>MACCalculator</code>
 * instance, to guess a byte array that satisfies
 * {@link SecurityToken#isFor(TokenData)} for any <code>TokenData</code> value
 * the attacker does not control.
 * </p><p>
 * Otherwise, the Gnutella network (which uses one implementation of 
 * <code>MACCalculator</code>) can be turned into a 
 * gigantic Distributed Denial-of-Service (DDoS) botnet.
 * </p><p>
 * Secure implementations likely use a cryptographically secure encryption
 * algorithm, message authentication code (keyed cryptographic message digest),
 * or a mathematical problem believed to be intractable (discrete log problem,
 * RSA problem, etc.) Straight-forward use of a linear encryption algorithm such
 * as RC4/ARC4/MARC4 is completely insecure.
 * </p>
 * See this <a href="http://en.wikipedia.org/wiki/Message_authentication_code">Wikipedia 
 * article</a> for more information on message authentication code, MAC.
 */
public interface MACCalculator {
    /**
     * Uses secret keys to generate a byte array from an InetAddres and
     * a port number.
     */
    public byte[] getMACBytes(TokenData data);
}
