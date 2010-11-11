package org.limewire.ui.swing.mainframe;

import java.awt.Component;

import org.jdesktop.application.ApplicationContext;
import org.jdesktop.application.SessionStorage;

class NullSessionStorage extends SessionStorage {

    NullSessionStorage(ApplicationContext context) {
        super(context);
    }

    @Override
    public Property getProperty(Class cls) {
        return super.getProperty(cls);
    }

    @Override
    public void putProperty(Class cls, Property property) {
    }

    @Override
    public void restore(Component root, String fileName) {
    }

    @Override
    public void save(Component root, String fileName) {
    }
    
    

}
