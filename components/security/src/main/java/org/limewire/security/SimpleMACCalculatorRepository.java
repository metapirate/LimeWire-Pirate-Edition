package org.limewire.security;

/**
 * A simple token generator chain that does not expire its keys.
 */
class SimpleMACCalculatorRepository implements MACCalculatorRepository {
    private final MACCalculator[] generators;
    
    public SimpleMACCalculatorRepository(MACCalculatorFactory factory) {
        generators = new MACCalculator[] { factory.createMACCalculator() };
    }
    
    public MACCalculator[] getValidMACCalculators() {
        return generators;
    }
    
    public MACCalculator getCurrentMACCalculator() {
        return generators[0];
    }
}