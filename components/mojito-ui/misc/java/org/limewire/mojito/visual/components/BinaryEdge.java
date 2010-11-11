package org.limewire.mojito.visual.components;

import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.graph.impl.DirectedSparseEdge;

public class BinaryEdge extends DirectedSparseEdge{
    
    public static enum EdgeType {
        LEFT, RIGHT;
    }
    
    private EdgeType type;

    public BinaryEdge(Vertex from, Vertex to, EdgeType type) {
        super(from, to);
        this.type = type;
    }

    public EdgeType getType() {
        return type;
    }
    
}
