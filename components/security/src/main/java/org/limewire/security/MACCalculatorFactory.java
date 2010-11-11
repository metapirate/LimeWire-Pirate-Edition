package org.limewire.security;

/**
 * Defines the interface to create a {@link MACCalculator}.
 */
public interface MACCalculatorFactory {
    public MACCalculator createMACCalculator();
}
