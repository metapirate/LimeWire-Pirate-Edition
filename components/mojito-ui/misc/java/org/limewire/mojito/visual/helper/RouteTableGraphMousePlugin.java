package org.limewire.mojito.visual.helper;

import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Set;

import org.limewire.mojito.visual.RouteTableUICallback;
import org.limewire.mojito.visual.components.BucketVertex;
import org.limewire.mojito.visual.components.InteriorNodeVertex;


import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.graph.impl.SparseTree;
import edu.uci.ics.jung.visualization.PickedState;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.control.AbstractGraphMousePlugin;

public class RouteTableGraphMousePlugin extends AbstractGraphMousePlugin
            implements MouseListener{
    
    private RouteTableUICallback callback;
    
    public RouteTableGraphMousePlugin(RouteTableUICallback callback) {
        super(InputEvent.BUTTON1_DOWN_MASK);
        this.callback = callback;
    }

    public void mouseClicked(MouseEvent e) {
        //we only want double clicks
        if(e.getClickCount() < 2) {
            return;
        }
        
        VisualizationViewer vv = (VisualizationViewer)e.getSource();
        PickedState pickedState = vv.getPickedState();
        if(pickedState == null ) {
            return;
        }
        
        Set vSet = pickedState.getPickedVertices();
        if(vSet.isEmpty()) {
            return;
        }
        Object o = vSet.iterator().next();
        
        if(o == null) {
            return;
        }
        
        if(o instanceof InteriorNodeVertex) {
            SparseTree tree = (SparseTree)vv.getGraphLayout().getGraph();
            Vertex root = tree.getRoot();
            if(o.equals(root)) {
                callback.handleNodeGraphRootSelected();
                return;
            }
        }
        
        if(o instanceof BucketVertex) {
            BucketVertex bucketVertex = (BucketVertex)o;
            callback.handleBucketSelected(bucketVertex.getNode());
            return;
        } 
    }

    public void mouseEntered(MouseEvent e) {}

    public void mouseExited(MouseEvent e) {}

    public void mousePressed(MouseEvent e) {}

    public void mouseReleased(MouseEvent e) {}

}
