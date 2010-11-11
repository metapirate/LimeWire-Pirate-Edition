package org.limewire.ui.swing.components.decorators;

import javax.swing.Icon;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXPanel;
import org.limewire.ui.swing.components.MessageComponent;
import org.limewire.ui.swing.painter.factories.MessagePainterFactory;
import org.limewire.ui.swing.util.GuiUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class MessageDecorator {
    
    private final MessagePainterFactory painterFactory;
    
    @Resource
    private Icon grayArrowIcon;
    @Resource
    private Icon greenArrowIcon;
    
    @Inject
    public MessageDecorator(MessagePainterFactory painterFactory) {
        GuiUtils.assignResources(this);
        
        this.painterFactory = painterFactory;
    }
    
    public void decorateGreenRectangleMessage(JXPanel panel) {
        panel.setBackgroundPainter(painterFactory.createGreenRectanglePainter());
    }
    
    public void decorateGreenMessage(MessageComponent component) {
        component.setMessageBackroundPainter(painterFactory.createGreenMessagePainter());
        component.setArrowIcon(greenArrowIcon);
    }
    
    public void decorateGrayMessage(MessageComponent component) {
        component.setMessageBackroundPainter(painterFactory.createGrayMessagePainter());
        component.setArrowIcon(grayArrowIcon);
    }
}
