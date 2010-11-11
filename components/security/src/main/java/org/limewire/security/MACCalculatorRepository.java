package org.limewire.security;

/**
 * A token generator chain that holds the valid security token generators.
 */
interface MACCalculatorRepository {
    /**
     * @return the <tt>MACCalculator</tt>'s that are currently valid.
     */
    public MACCalculator [] getValidMACCalculators();
    
    /**
     * @return the current token generator
     */
    public MACCalculator getCurrentMACCalculator();
}