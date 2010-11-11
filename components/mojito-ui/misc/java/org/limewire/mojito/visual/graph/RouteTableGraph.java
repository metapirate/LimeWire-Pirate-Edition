package org.limewire.mojito.visual.graph;

import org.limewire.mojito.routing.RouteTable;
import org.limewire.mojito.routing.RouteTable.RouteTableListener;
import org.limewire.mojito.visual.RouteTableGraphCallback;
import org.limewire.mojito.visual.components.BinaryEdge;
import org.limewire.mojito.visual.components.InteriorNodeVertex;
import org.limewire.mojito.visual.components.BinaryEdge.EdgeType;


import edu.uci.ics.jung.graph.ArchetypeVertex;
import edu.uci.ics.jung.graph.Edge;
import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.graph.impl.SparseTree;
import edu.uci.ics.jung.utils.UserData;

public abstract class RouteTableGraph implements RouteTableListener {
    
    protected RouteTable routeTable;
    protected RootableSparseTree tree;
    protected Vertex root;
    protected RouteTableGraphCallback callback;

    public RouteTableGraph(RouteTable routeTable, RouteTableGraphCallback callback) {
        this.routeTable = routeTable;
        this.callback = callback;
        routeTable.addRouteTableListener(this);
    }
    
    public abstract void populateGraph();
    
    public abstract String getGraphInfo();
    
    public abstract String getLabelForVertex(ArchetypeVertex v);
    
    protected InteriorNodeVertex createInteriorNode(InteriorNodeVertex previousVertex, EdgeType type) {
        InteriorNodeVertex vertex = new InteriorNodeVertex();
        tree.addVertex(vertex);
        tree.addEdge(new BinaryEdge(previousVertex, vertex, type));
        return vertex;
    }
    
    protected InteriorNodeVertex removeRouteTableVertex(Vertex vertex) {
        InteriorNodeVertex predecessor = 
            (InteriorNodeVertex)vertex.getPredecessors().iterator().next(); 
        tree.removeEdge((Edge)vertex.getInEdges().iterator().next());
        tree.removeVertex(vertex);
        return predecessor;
    }
    
    public SparseTree getTree(){
        return tree;
    }
    
    public void deregister() {
        routeTable.removeRouteTableListener(this);
    }

    public static class RootableSparseTree extends SparseTree {
        public RootableSparseTree(Vertex root) {
            super(root);
        }
        
        /**
         * Clears the tree and add this <tt>Vertex</tt>
         * as the new root.
         */
        public synchronized void newRoot(Vertex root) {
            removeAllEdges();
            removeAllVertices();
            this.mRoot = root;
            addVertex( root );
            mRoot.setUserDatum(SPARSE_ROOT_KEY, SPARSE_ROOT_KEY, UserData.SHARED);
            mRoot.setUserDatum(IN_TREE_KEY, IN_TREE_KEY, UserData.SHARED);
        }
    }
}
