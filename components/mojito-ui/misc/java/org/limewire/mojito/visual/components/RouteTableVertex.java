package org.limewire.mojito.visual.components;

import edu.uci.ics.jung.graph.DirectedEdge;
import edu.uci.ics.jung.graph.Edge;
import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.graph.impl.DirectedSparseVertex;
import edu.uci.ics.jung.graph.impl.SparseTree;

public abstract class RouteTableVertex<T> extends DirectedSparseVertex{
    
    @Override
    protected void addNeighbor_internal(Edge e, Vertex v) {
        super.addNeighbor_internal(e, v);
        DirectedEdge de = (DirectedEdge) e;
        if(de.getSource() == this) {
            SparseTree tree = (SparseTree)getGraph();
            //throw an exception when trying to add a child to a leaf node
            if(!tree.getRoot().equals(this)) {
                throw new IllegalStateException("A Route table vertex is always a tree leaf");
            }
        }
    }
    
    public abstract boolean isLocal() ;
    
    public abstract T getNode();

}
