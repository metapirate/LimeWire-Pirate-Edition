package org.limewire.ui.swing.browser;

import com.google.inject.AbstractModule;


public class LimeWireUiBrowserModule extends AbstractModule {

    @Override
    protected void configure() {
        requestStaticInjection(BrowserUtils.class);
    }
}
