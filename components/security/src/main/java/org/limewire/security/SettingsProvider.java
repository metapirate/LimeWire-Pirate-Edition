package org.limewire.security;

/**<p>
 * Defines the interface to provide settings for the 
 * {@link AddressSecurityToken} class. A rule of thumb is to ensure the change 
 * period is longer than the grace period when you implement this interface.
 * </p>
 * A typical use case for <code>SettingsProvider</code> is to coordinate when 
 * to create a new {@link MACCalculator}.
 */
public interface SettingsProvider {
    
    /**
     * Time in milliseconds.
     */
    public long getChangePeriod();
    
    /**
     * Time in milliseconds.
     */
    public long getGracePeriod();
}