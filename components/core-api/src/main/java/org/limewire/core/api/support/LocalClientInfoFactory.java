package org.limewire.core.api.support;

import com.google.inject.assistedinject.Assisted;


public interface LocalClientInfoFactory {

    /** Constructs a new LocalClientInfo with the given names. */
    public LocalClientInfo createLocalClientInfo(Throwable bug, @Assisted("threadName") String threadName, @Assisted("detail") String detail,
            boolean fatal);

}