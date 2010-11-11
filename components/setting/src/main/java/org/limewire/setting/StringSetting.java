package org.limewire.setting;

import java.util.Properties;


/**
 * Provides a <code>String</code> setting value. As a subclass of 
 * <code>Setting</code>, the setting has a key.
 * <p>
 * Create a <code>StringSetting</code> object with a 
 * {@link SettingsFactory#createStringSetting(String, String)}.
 */
public final class StringSetting extends AbstractSetting<String> {
    
    private volatile String value;

	/**
	 * Creates a new <tt>SettingBool</tt> instance with the specified
	 * key and default value.

	 * @param key the constant key to use for the setting
	 * @param defaultStr the default value to use for the setting
	 */
	StringSetting(Properties defaultProps, Properties props, String key, 
                                       String defaultStr) {
		super(defaultProps, props, key, defaultStr);
	}
        
	/**
	 * Returns the value of this setting.
	 * 
	 * @return the value of this setting
	 */
	public String get() {
        return value;
	}

	/**
	 * Mutator for this setting.
	 *
	 * @param str the <tt>String</tt> to store
	 */
	public void set(String str) {
	    setValueInternal(str);
	}
    
    /** Load value from property string value
     * @param sValue property string value
     *
     */
    @Override
    protected void loadValue(String sValue) {
        value = sValue;
    }
}
