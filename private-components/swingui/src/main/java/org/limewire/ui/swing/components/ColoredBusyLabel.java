package org.limewire.ui.swing.components;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXBusyLabel;
import org.jdesktop.swingx.painter.BusyPainter;
import org.jdesktop.swingx.plaf.UIManagerExt;
import org.limewire.ui.swing.util.GuiUtils;

public class ColoredBusyLabel extends JXBusyLabel {
    
    @Resource private Color lightGrey;
    @Resource private Color darkGrey;

    public ColoredBusyLabel(Dimension dimension) {
        super(dimension);
        GuiUtils.assignResources(this);
        setBusyPainter(null); // null out the painter the super constructor set.
        getBusyPainter(); // recreate.
    }

    @Override
    protected BusyPainter createBusyPainter(final Dimension dim) {
        BusyPainter p = new BusyPainter() {
            @Override
            protected void init(Shape point, Shape trajectory, Color unused, Color unused2) {
                super.init(dim == null ? 
                                UIManagerExt.getShape("JXBusyLabel.pointShape") : getScaledCircularPoint(dim.height),
                           dim == null ?
                                UIManagerExt.getShape("JXBusyLabel.trajectoryShape") : getScaledDefaultTrajectory(dim.height),
                           lightGrey, darkGrey);
            }
            
            /**
             * The method getScaledDefaultPoint returns an elongated ellipse, which one of LimeWire's designers dislikes.
             * This method returns a circle instead...
             */
            private Ellipse2D.Float getScaledCircularPoint(float height) {
                return new Ellipse2D.Float(0, 0, height/5, height/5);              
            }
        };
        return p;
    }   
}
