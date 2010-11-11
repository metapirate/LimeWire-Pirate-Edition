package org.limewire.core.settings;

import org.limewire.setting.SettingsFactory;

/**
 * Handler for all 'LimeWire.props' settings.  Classes such
 * as SearchSettings, ConnectionSettings, etc... should retrieve
 * the factory via LimeProps.instance().getFactory() and add
 * settings to that factory.
 */
public class LimeProps extends LimeWireSettings {
        
    private static final LimeProps INSTANCE = new LimeProps();
    
    // The FACTORY is used for subclasses of LimeProps, so they know
    // which factory to add classes to.
    protected static final SettingsFactory FACTORY = INSTANCE.getFactory();
    
    // This is protected so that subclasses can extend from it, but
    // subclasses should NEVER instantiate a copy themselves.
    protected LimeProps() {
        super("limewire.props", "LimeWire properties file");
        assert getClass() == LimeProps.class : "should not have a subclass instantiate";
    }
    
    /**
     * Returns the only instance of this class.
     */
    public static LimeProps instance() { return INSTANCE; }

}
