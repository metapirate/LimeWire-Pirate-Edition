package org.limewire.security;

/**
 * A factory for TEA key generators.
 */
class TEAMACCalculatorFactory implements MACCalculatorFactory {
    public MACCalculator createMACCalculator() {
        return new TEAMACCalculator();
    }
}