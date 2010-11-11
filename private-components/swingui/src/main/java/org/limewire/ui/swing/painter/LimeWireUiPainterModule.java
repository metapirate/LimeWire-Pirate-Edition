package org.limewire.ui.swing.painter;

import org.limewire.ui.swing.painter.factories.ProgressPainterFactory;
import org.limewire.ui.swing.painter.factories.ProgressPainterFactoryImpl;

import com.google.inject.AbstractModule;

public class LimeWireUiPainterModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(ProgressPainterFactory.class).to(ProgressPainterFactoryImpl.class);
        }
}
