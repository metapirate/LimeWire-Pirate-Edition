package org.limewire.core.impl.library;

import com.limegroup.gnutella.library.FileDesc;

interface CoreLocalFileItemFactory {
    
    CoreLocalFileItem createCoreLocalFileItem(FileDesc fd);

}
