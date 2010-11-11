package org.limewire.setting.jmx;

import java.io.File;

import javax.management.MXBean;

import org.limewire.setting.AbstractNumberSetting;
import org.limewire.setting.BooleanSetting;
import org.limewire.setting.ByteSetting;
import org.limewire.setting.FileSetting;
import org.limewire.setting.FloatSetting;
import org.limewire.setting.IntSetting;
import org.limewire.setting.LongSetting;
import org.limewire.setting.Setting;
import org.limewire.setting.StringSetting;

/**
 * A JMX Bean interface for {@link Setting}s.
 */
@MXBean
public interface SettingBean {

    /**
     * Returns the type of the {@link Setting}.
     */
    public String getType();
    
    /**
     * Returns the value of the {@link Setting}
     */
    public String getValue();
    
    /**
     * Sets the value of the {@link Setting}
     */
    public void setValue(String value);
    
    /**
     * Returns the minimum value of the {@link Setting} or {@code null} 
     * if it doesn't have a minimum value.
     */
    public String getMinValue();
    
    /**
     * Returns the maximum value of the {@link Setting} or {@code null} 
     * if it doesn't have a maximum value.
     */
    public String getMaxValue();
    
    /**
     * Reverts the current value to default
     */
    public void revertToDefault();
    
    /**
     * Returns true if the current value is the default value
     */
    public boolean isDefault();
    
    /**
     * Returns true if the current value is private
     */
    public boolean isPrivate();
    
    /**
     * Sets weather or not the current value is private
     */
    public void setPrivate(boolean value);
    
    /**
     * Determines whether or not this value should always be saved to disk.
     */
    public boolean isShouldAlwaysSave();
    
    /**
     * Returns the key of the {@link Setting}
     */
    public String getKey();
    
    /**
     * Returns the remote key of the {@link Setting}
     */
    public String getRemoteKey();
    
    /**
     * Reloads the {@link Setting}'s value from the properties
     */
    public void reload();
    
    /**
     * An implementation of {@link SettingBean}
     */
    public static class Impl implements SettingBean {
        
        private final Setting setting;
        
        private final String remoteKey;
        
        public Impl(Setting setting) {
            this(setting, null);
        }
        
        public Impl(Setting setting, String remoteKey) {
            this.setting = setting;
            this.remoteKey = remoteKey;
        }
        
        @Override
        public String getType() {
            return setting.getClass().getName();
        }

        @Override
        public String getValue() {
            return setting.getValueAsString();
        }

        @Override
        public void setValue(String value) {
            if (setting instanceof BooleanSetting) {
                ((BooleanSetting)setting).setValue(Boolean.parseBoolean(value));
            } else if (setting instanceof ByteSetting) {
                ((ByteSetting)setting).setValue(Byte.parseByte(value));
            } else if (setting instanceof IntSetting) {
                ((IntSetting)setting).setValue(Integer.parseInt(value));
            } else if (setting instanceof FloatSetting) {
                ((FloatSetting)setting).setValue(Float.parseFloat(value));
            } else if (setting instanceof LongSetting) {
                ((LongSetting)setting).setValue(Long.parseLong(value));
            } else if (setting instanceof FileSetting) {
                ((FileSetting)setting).set(new File(value));
            } else if (setting instanceof StringSetting) {
                ((StringSetting)setting).set(value);
            }
        }
        
        @Override
        public String getMinValue() {
            return getMinMaxValue(setting, true);
        }
        
        @Override
        public String getMaxValue() {
            return getMinMaxValue(setting, false);
        }
        
        @Override
        public void revertToDefault() {
            setting.revertToDefault();
        }
        
        @Override
        public boolean isDefault() {
            return setting.isDefault();
        }
        
        @Override
        public boolean isPrivate() {
            return setting.isPrivate();
        }
        
        @Override
        public void setPrivate(boolean value) {
            setting.setPrivate(value);
        }
        
        @Override
        public boolean isShouldAlwaysSave() {
            return setting.shouldAlwaysSave();
        }
        
        @Override
        public String getKey() {
            return setting.getKey();
        }
        
        @Override
        public String getRemoteKey() {
            return remoteKey;
        }

        @Override
        public void reload() {
            setting.reload();
        }
        
        /**
         * Returns the minimum or maximum value of the given {@link Setting}
         * if it's an instance of {@link AbstractNumberSetting}.
         */
        private static String getMinMaxValue(Setting setting, boolean min) {
            Number value = null;
            if (setting instanceof AbstractNumberSetting<?>) {
                if (min) {
                    value = ((AbstractNumberSetting<?>)setting).getMinValue();
                } else {
                    value = ((AbstractNumberSetting<?>)setting).getMaxValue();
                }
            }
            
            return value != null ? value.toString() : null;
        }
    }
}
