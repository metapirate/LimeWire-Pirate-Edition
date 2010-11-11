package org.limewire.setting;

import java.util.Properties;

/**
 * Provides a power of 2 setting value. As a subclass of 
 * <code>Setting</code>, the setting has a key.
 * <p>
 * <code>PowerOfTwoSetting</code>'s value must be a power of 2 and 
 * greater than 0. If not a power of two, the value is set to the previous 
 * power of 2. For example, setting a value of 1023 returns a value of 512 
 * because 1023 is not a power of 2 (2^9 = 512 and 2^10 = 1024). 
 * <p>
 * Create a <code>PowerOfTwoSetting</code> object with a 
 * {@link SettingsFactory#createPowerOfTwoSetting(String, long)}.
 <pre>
        File f = new File("setting.txt");
        SettingsFactory sf = new SettingsFactory(f);
        PowerOfTwoSetting p2 = sf.createPowerOfTwoSetting("p2", 1);
        System.out.println("SetValue->GetValue");  
        for(int i = 1; i < 2000; ){
            p2.setValue(i);
            System.out.print(i + "->" + p2.getValue() + " ");
            i = i * 2;
        }
        System.out.print("\n\n");        
        for(int i = 0; i < 10; i++){
            p2.setValue(i);
            System.out.print(i + "->" + p2.getValue() + " ");
        }
        
        
        
    Output:
        SetValue->GetValue
        1->1 2->2 4->4 8->8 16->16 32->32 64->64 128->128 256->256 512->512 1024->1024 
        
        0->1 1->1 2->2 3->2 4->4 5->4 6->4 7->4 8->8 9->8 
 </pre>
 *
 *
 */

public final class PowerOfTwoSetting extends LongSetting {
    /**
     * Creates a new <tt>PowerOfTwoSetting</tt> instance with the specified
     * key and default value.  A PowerOfTwoSitting may take on only values
     * that are powers of two.
     *
     * @param key the constant key to use for the setting
     * @param defaultLong the default value to use for the setting, which 
     *            must be a power of two.
     */
    PowerOfTwoSetting(Properties defaultProps, Properties props, String key, 
                                         long defaultLong) {
        super(defaultProps, props, key, defaultLong);
        if (! isPowerOfTwo(defaultLong)) {
            throw new IllegalArgumentException("Default value is not a power of two");
        }
    }

    PowerOfTwoSetting(Properties defaultProps, Properties props, String key, 
                long defaultLong, long min, long max) {
        super(defaultProps, props, key, defaultLong, min, max);
        if (! isPowerOfTwo(defaultLong)) {
            throw new IllegalArgumentException("Default value is not a power of two");
        }
        if (! isPowerOfTwo(max)) {
            throw new IllegalArgumentException("Max value is not a power of two");
        }
        if (! isPowerOfTwo(min)) {
            throw new IllegalArgumentException("Min value is not a power of two");
        }
    }
    
    /** Utility method to determine if a long is zero or a power of two 
     */
    private static boolean isPowerOfTwo(long x) {
        if (x <= 0) {
            return false;
        }
        return ((~x+1)&x) == x;
    }
    
    /** Makes value a power of two by rounding down if necessary
     * and delegates the rest of the normalization to the superclass.
     * 
     * Non-positive values cannot be made made powers of two by rounding
     * down, and are special-cased to return MIN_VALUE, which is forced by
     * the constructor to be non-negative.
     * 
     * Strings that can't be parsed as longs will result in DEFALT_VALUE.
     */
    @Override
    protected String normalizeValue(String value) {
        long longValue;
        
        try {
            longValue = Long.parseLong(value);
        } catch (NumberFormatException e) {
            // Attempts to set with non-numbers numbers will result in DEFAULT_VALUE.
            return DEFAULT_VALUE;
        }
        
        if (longValue <= 0) {
            if (MIN_VALUE != null) {
                return MIN_VALUE.toString();
            }
            return super.normalizeValue("1"); // The smallest power of two
        }
        
        long lowestSetBit = (~longValue+1) & longValue;
        if (lowestSetBit != longValue) {
            do {
                // take away lowest set bit until we get a power of two or zero
                longValue -= lowestSetBit;
                lowestSetBit = (~longValue+1) & longValue;
            } while (lowestSetBit  != longValue);
            if (longValue == 0) {
                longValue = 1;
            }
            value = String.valueOf(longValue);
        }
        return super.normalizeValue(value);
    }
}
