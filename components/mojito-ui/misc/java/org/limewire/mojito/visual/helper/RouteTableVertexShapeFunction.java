package org.limewire.mojito.visual.helper;
import java.awt.Shape;

import org.limewire.mojito.visual.components.InteriorNodeVertex;


import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.graph.decorators.AbstractVertexShapeFunction;

public class RouteTableVertexShapeFunction extends AbstractVertexShapeFunction{
    public Shape getShape(Vertex v)
    {
        if(v instanceof InteriorNodeVertex) {
            return factory.getEllipse(v);
        } else {
            return factory.getRectangle(v);
        }
    }
}
