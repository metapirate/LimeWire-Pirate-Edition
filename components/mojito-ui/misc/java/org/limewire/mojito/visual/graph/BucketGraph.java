package org.limewire.mojito.visual.graph;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.SwingUtilities;

import org.limewire.mojito.routing.Bucket;
import org.limewire.mojito.routing.RouteTable;
import org.limewire.mojito.routing.RouteTable.RouteTableEvent;
import org.limewire.mojito.routing.RouteTable.RouteTableEvent.EventType;
import org.limewire.mojito.visual.RouteTableGraphCallback;
import org.limewire.mojito.visual.components.BinaryEdge;
import org.limewire.mojito.visual.components.BucketVertex;
import org.limewire.mojito.visual.components.InteriorNodeVertex;
import org.limewire.mojito.visual.components.BinaryEdge.EdgeType;


import edu.uci.ics.jung.graph.ArchetypeVertex;
import edu.uci.ics.jung.graph.Vertex;

public class BucketGraph extends RouteTableGraph {
    
    public BucketGraph(RouteTable routeTable, RouteTableGraphCallback callback) {
        super(routeTable, callback);
        
        List<Bucket> buckets = new ArrayList<Bucket>(routeTable.getBuckets());
        //create new sparse tree graph with one bucket
        root = new BucketVertex(buckets.get(0), true);
        tree = new RootableSparseTree(root);
    }

    @Override
    public void populateGraph() {
        List<Bucket> buckets = new ArrayList<Bucket>(routeTable.getBuckets());
        int count = buckets.size();
        
        if(count < 2) {
            root = new BucketVertex(buckets.get(0), true);
            tree.newRoot(root);
            return;
        } else {
            root = new InteriorNodeVertex();
            tree.newRoot(root);
        }
        
        //TODO: optimization -- or not?
        //BucketUtils.sortByDepth(buckets);
        Bucket currentBucket;
        for(Iterator<Bucket> it = buckets.iterator(); it.hasNext();) {
            currentBucket = it.next();
            updateGraphBucket(currentBucket);
            it.remove();
        }
    }
    
    @Override
    public String getGraphInfo() {
        String rtString = routeTable.toString();
        return rtString.substring(rtString.indexOf("Total"));
    }

    private void updateGraphBucket(Bucket bucket) {
        InteriorNodeVertex InteriorNode = getVertexForBucket(bucket);
        
        //now add the bucket
        if(bucket.getBucketID().isBitSet(bucket.getDepth()-1)) {
            createBucketVertex(bucket, InteriorNode, EdgeType.RIGHT);
        } else {
            createBucketVertex(bucket, InteriorNode, EdgeType.LEFT);
        }
    }
    
    private InteriorNodeVertex splitBucket(BucketVertex vertex, EdgeType type) {
        InteriorNodeVertex predecessor = removeRouteTableVertex(vertex);
        return createInteriorNode(predecessor, type);
    }
    
    private InteriorNodeVertex getVertexForBucket(Bucket bucket) {
        int depth = bucket.getDepth();

        InteriorNodeVertex vertex = (InteriorNodeVertex)root;
        Vertex child;
        EdgeType type;
        for(int i=1; i < depth ; i++) {
            child = null;

            if(bucket.getBucketID().isBitSet(i-1)) {
                child = vertex.getRightChild();
                type = EdgeType.RIGHT;
            } else {
                child = vertex.getLeftChild();
                type = EdgeType.LEFT;
            }

            if(child == null) {
                vertex = createInteriorNode(vertex, type);
                
            } else if(child instanceof BucketVertex) {
                //we have found a bucket along this bucket path
                //--> split it in order to be able to insert new bucket
                BucketVertex bv = (BucketVertex)child;
                vertex = splitBucket(bv, type);
            } else {
                vertex = (InteriorNodeVertex)child;
            }
        }
        return vertex;
    }
    
    private BucketVertex createBucketVertex(Bucket bucket, InteriorNodeVertex predecessor, EdgeType type) {
        boolean isLocalBucket = bucket.contains(routeTable.getLocalNode().getNodeID());
        BucketVertex bv = new BucketVertex(bucket, isLocalBucket);
        tree.addVertex(bv);
        tree.addEdge(new BinaryEdge(predecessor, bv, type));
        return bv;
    }
    
    @Override
    public String getLabelForVertex(ArchetypeVertex v) {
        if(v instanceof BucketVertex) {
            return v.toString()+"("+((BucketVertex)v).getNode().getActiveSize()+")";
        }
        else return "";
    }

    public void handleRouteTableEvent(final RouteTableEvent event) {
        if (event.getEventType().equals(EventType.ADD_ACTIVE_CONTACT)) {
            callback.handleGraphInfoUpdated();
        } else if (event.getEventType().equals(EventType.SPLIT_BUCKET)) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    Bucket left = event.getLeftBucket();
                    Bucket right = event.getRightBucket();
                    
                    //are we splitting the root bucket
                    if(left.getDepth() == 1) {
                        root = new InteriorNodeVertex();
                        tree.newRoot(root);
                    }
                    updateGraphBucket(left);
                    updateGraphBucket(right);
                    callback.handleGraphLayoutUpdated();
                }
            });
        }
    }
}
