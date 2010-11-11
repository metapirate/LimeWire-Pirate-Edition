package org.limewire.mojito.visual.helper;

import org.limewire.mojito.visual.components.RouteTableVertex;

import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.graph.decorators.DefaultToolTipFunction;

public class RouteTableToolTipFunction extends DefaultToolTipFunction{
    @Override
    public String getToolTipText(Vertex v) {
        if(v instanceof RouteTableVertex) {
            RouteTableVertex routeTableVertex = (RouteTableVertex)v;
            String nodeString = routeTableVertex.getNode().toString();
            String ttString = "<html>"+nodeString.replace("\n","<br>")+"</html>";
            return ttString;
        }
        return super.getToolTipText(v);
    }
    
}
