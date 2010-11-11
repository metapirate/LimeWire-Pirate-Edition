package org.limewire.mojito.visual.graph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.limewire.mojito.KUID;
import org.limewire.mojito.routing.Bucket;
import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.routing.RouteTable;
import org.limewire.mojito.routing.RouteTable.RouteTableEvent;
import org.limewire.mojito.routing.RouteTable.RouteTableEvent.EventType;
import org.limewire.mojito.visual.RouteTableGraphCallback;
import org.limewire.mojito.visual.components.BinaryEdge;
import org.limewire.mojito.visual.components.ContactVertex;
import org.limewire.mojito.visual.components.InteriorNodeVertex;
import org.limewire.mojito.visual.components.BinaryEdge.EdgeType;


import edu.uci.ics.jung.graph.ArchetypeVertex;
import edu.uci.ics.jung.graph.Vertex;

public class NodeGraph extends RouteTableGraph {

    /**
     * A constant for the maximum depth of the tree
     */
    private static int MAX_DEPTH = 20;
    
    private Bucket bucket; 

    public NodeGraph(RouteTable routeTable, RouteTableGraphCallback callback, Bucket bucket) {
        super(routeTable, callback);
        root = new InteriorNodeVertex();
        tree = new RootableSparseTree(root);
        this.bucket = bucket;
    }

    @Override
    public String getGraphInfo() {
        synchronized (routeTable) {
            return bucket.toString();
        }
    }

    /**
     * The way to populate a node graph is to create a per-level Map of
     * nodes, in order to visualize only the smallest common bit-prefix.
     * 
     */
    @Override
    public void populateGraph() {
        //get a copy of the nodes
        List<Contact> nodes = new ArrayList<Contact>(bucket.getActiveContacts());

        if(nodes.isEmpty()) {
            return;
        }
        
        if(nodes.size() < 2) {
            createVertexForNode(nodes.get(0), 0);
            return;
        }
        
        //for every node, take the longest common prefix to any other node
        //and place the node in the tree at depth = common_prefix + 1
        Contact curNode;
        for(int i = 0; i < nodes.size(); i++) {
            curNode = nodes.get(i);
            insertNode(curNode, nodes);
        }
    }
    
    private void insertNode(Contact node, Collection<Contact> nodeList) {
        int longestPrefix = 0;
        for(Contact cmpNode: nodeList) {
            if(cmpNode.equals(node)) {
                continue;
            }
            int commonPrefix = node.getNodeID().bitIndex(cmpNode.getNodeID());
            if(commonPrefix > longestPrefix) {
                longestPrefix = commonPrefix;
            }
        }
        createVertexForNode(node, longestPrefix);
    }
    
    private void removeNode(Contact node) {
        KUID nodeId = node.getNodeID();
        
        InteriorNodeVertex vertex = (InteriorNodeVertex)root;
        Vertex child;
        for(int i= bucket.getDepth(); i <= MAX_DEPTH; i++) {
            child = null;
            if(nodeId.isBitSet(i)) {
                child = vertex.getRightChild();
            } else {
                child = vertex.getLeftChild();
            }
            
            if(child == null) {
                throw new IllegalStateException("Trying to remove a node that is not in the tree");
            } else if(child instanceof ContactVertex) {
                ContactVertex cv = (ContactVertex)child;
                if(cv.getNode().equals(node)) {
                    removeRouteTableVertex(child);
                } else if(i == MAX_DEPTH){
                    return;
                } else {
                    throw new IllegalStateException("Found a contact node " +
                            "in the path of node to be removed");
                }
            } else {
                vertex = (InteriorNodeVertex)child;
            }
        }
    }
    
    private ContactVertex createContactVertex(Contact contact, InteriorNodeVertex predecessor, 
            EdgeType type, int depth) {
        ContactVertex cv = new ContactVertex(contact, depth + 1);
        tree.addVertex(cv);
        tree.addEdge(new BinaryEdge(predecessor, cv, type));
        return cv;
    }
    
    private void createVertexForNode(Contact node, int depth) {
        InteriorNodeVertex vertex = (InteriorNodeVertex)root;
        KUID nodeId = node.getNodeID();

        Vertex child;
        EdgeType type;
        //start at the bucket's depth: every bit before that is common to all
        //the nodes in the bucket
        for(int i= bucket.getDepth(); i <= depth ; i++) {
            child = null;
            if(nodeId.isBitSet(i)) {
                child = vertex.getRightChild();
                type = EdgeType.RIGHT;
                
            } else {
                child = vertex.getLeftChild();
                type = EdgeType.LEFT;
            }
            
            if(child == null) {
                //if we have arrive to leaf node, create it and return
                //also create the node if we have reached the max depth.
                if(i == depth || i == MAX_DEPTH) {
                    createContactVertex(node, vertex, type, depth);
                    return;
                } 
                vertex = createInteriorNode(vertex, type);
                
            } else if(child instanceof ContactVertex) {
                //if we have reached max depth, it's normal that there is already a node
                if(i == (MAX_DEPTH - 1)) {
                    return;
                }
                ContactVertex cv = (ContactVertex)child;
                int commonPrefix = node.getNodeID().bitIndex(cv.getNode().getNodeID());
                
                if(i > commonPrefix) {
                    throw new IllegalStateException("Existing contact vertex " +
                    "in the path of other contact");
                }
                
                removeRouteTableVertex(cv);
                createVertexForNode(cv.getNode(), commonPrefix);
                createVertexForNode(node, commonPrefix);
                return;
            } else {
                vertex = (InteriorNodeVertex)child;
            }
        }
        
    }
    
    @Override
    public String getLabelForVertex(ArchetypeVertex v) {
        if(v.equals(root)) {
            String title = bucket.getBucketID().toBinString().substring(0, bucket.getDepth());
            return title+"...";
        } 
        
        if(v instanceof ContactVertex) {
            return v.toString()+"...";
        }
        return "";
    }

    public void handleRouteTableEvent(RouteTableEvent event) {
        if (event.getEventType().equals(EventType.ADD_ACTIVE_CONTACT)) {
            Bucket bucket = event.getBucket();
            if(!bucket.equals(this.bucket)) {
                return;
            }
            
            insertNode(event.getContact(), bucket.getActiveContacts());
            callback.handleGraphLayoutUpdated();
            
        } else if (event.getEventType().equals(EventType.REMOVE_CONTACT)) {
            Bucket bucket = event.getBucket();
            if(!bucket.equals(this.bucket)) {
                return;
            }
            removeNode(event.getContact());
            callback.handleGraphLayoutUpdated();
            
        } else if (event.getEventType().equals(EventType.REPLACE_CONTACT)) {
            Bucket bucket = event.getBucket();
            if(!bucket.equals(this.bucket)) {
                return;
            }
            removeNode(event.getExistingContact());
            insertNode(event.getContact(), bucket.getActiveContacts());
            callback.handleGraphLayoutUpdated();
            
        } else if (event.getEventType().equals(EventType.UPDATE_CONTACT)) {
            callback.handleGraphInfoUpdated();
            
        } else if (event.getEventType().equals(EventType.SPLIT_BUCKET)) {
            // This bucket has been split! Go back to bucket tree
            callback.handleRouteTableCleared();
        
        } else if (event.getEventType().equals(EventType.CLEAR)) {
            callback.handleRouteTableCleared();
        }
    }
}
