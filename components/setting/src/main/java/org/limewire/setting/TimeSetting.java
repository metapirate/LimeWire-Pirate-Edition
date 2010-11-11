package org.limewire.setting;

import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * A {@link Setting} to hold a time duration.
 * 
 * <p>NOTE: The time is stored internally in milliseconds. This allows
 * us to migrate all existing {@link LongSetting}s that were holding
 * the time in milliseconds to {@link TimeSetting}.
 */
public class TimeSetting extends LongSetting {

    /**
     * Creates a {@link TimeSetting} with the given arguments.
     */
    public TimeSetting(Properties defaultProps, Properties props, 
            String key, long defaultValue, TimeUnit unit) {
        super(defaultProps, props, key, unit.toMillis(defaultValue));
    }
    
    /**
     * Returns the time in the given {@link TimeUnit}
     */
    public long getTime(TimeUnit unit) {
        return unit.convert(getValue(), TimeUnit.MILLISECONDS);
    }
    
    /**
     * Returns the time in milliseconds.
     */
    public long getTimeInMillis() {
        return getTime(TimeUnit.MILLISECONDS);
    }
    
    /**
     * Sets the time in the given {@link TimeUnit}
     */
    public void setTime(long time, TimeUnit unit) {
        setValue(unit.toMillis(time));
    }
}
