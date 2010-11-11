package org.limewire.setting;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Properties;

public class PropertiesSetting extends AbstractSetting<Properties> {

    private volatile Properties value;
    
    PropertiesSetting(Properties defaultProps, Properties props, String key, 
            Properties defaultValue) {
        super(defaultProps, props, key, toString(defaultValue));
    }
    
    @Override
    protected void loadValue(String value) {
        this.value = fromString(value);
    }
    
    public Properties get() {
        return value;
    }
    
    public void set(Properties properties) {
        value = properties;
        super.setValueInternal(toString(properties));
    }

    static String toString(Properties props) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            props.store(out, "props");
            return new String(out.toByteArray(), "UTF-8");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    static Properties fromString(String value) {
        Properties props = new Properties();
        try {
            ByteArrayInputStream in = new ByteArrayInputStream(value.getBytes("UTF-8"));
            props.load(in);
            return props;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
