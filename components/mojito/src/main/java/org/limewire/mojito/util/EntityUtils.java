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

package org.limewire.mojito.util;

import java.util.Collection;

import org.limewire.mojito.KUID;
import org.limewire.mojito.db.DHTValueEntity;

public class EntityUtils {
    
    private EntityUtils() {}
    
    public static KUID getPrimaryKey(Collection<? extends DHTValueEntity> entities) {
        KUID primaryKey = null;
        for (DHTValueEntity entity : entities) {
            if (primaryKey == null) {
                primaryKey = entity.getPrimaryKey();
            }
            
            if (!entity.getPrimaryKey().equals(primaryKey)) {
                return null;
            }
        }
        return primaryKey;
    }
}
