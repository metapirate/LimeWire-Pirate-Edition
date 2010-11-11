/*
 * Mojito Distributed Hash Table (Mojito DHT)
 * Copyright (C) 2006-2007 LimeWire LLC
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */
 
package org.limewire.mojito.settings;

import java.io.File;

import org.limewire.setting.BasicSettingsGroup;
import org.limewire.setting.SettingsFactory;
import org.limewire.util.CommonUtils;


/**
 * Handler for all Mojito Settings.
 */
public class MojitoProps extends BasicSettingsGroup {
    
    private static final MojitoProps INSTANCE = new MojitoProps();
    
    // The FACTORY is used for subclasses of LimeProps, so they know
    // which factory to add classes to.
    protected static final SettingsFactory FACTORY = INSTANCE.getFactory();
    
    // This is protected so that subclasses can extend from it, but
    // subclasses should NEVER instantiate a copy themselves.
    protected MojitoProps() {
        super(new File(CommonUtils.getUserSettingsDir(), "mojito.props"), "Mojito properties file");
        assert (getClass() == MojitoProps.class);
    }
    
    /**
     * Returns the only instance of this class.
     */
    public static MojitoProps instance() { return INSTANCE; }
}
