package org.limewire.mojito.visual;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import org.limewire.mojito.Context;
import org.limewire.mojito.routing.Bucket;
import org.limewire.mojito.routing.RouteTable;
import org.limewire.mojito.visual.components.BinaryEdge;
import org.limewire.mojito.visual.components.BinaryEdge.EdgeType;
import org.limewire.mojito.visual.graph.BucketGraph;
import org.limewire.mojito.visual.graph.NodeGraph;
import org.limewire.mojito.visual.graph.RouteTableGraph;
import org.limewire.mojito.visual.helper.RouteTableGraphMousePlugin;
import org.limewire.mojito.visual.helper.RouteTableToolTipFunction;
import org.limewire.mojito.visual.helper.RouteTableVertexPaintFunction;
import org.limewire.mojito.visual.helper.RouteTableVertexShapeFunction;


import edu.uci.ics.jung.graph.ArchetypeEdge;
import edu.uci.ics.jung.graph.ArchetypeVertex;
import edu.uci.ics.jung.graph.decorators.EdgeShape;
import edu.uci.ics.jung.graph.decorators.EdgeStringer;
import edu.uci.ics.jung.graph.decorators.PickableEdgePaintFunction;
import edu.uci.ics.jung.graph.decorators.VertexStringer;
import edu.uci.ics.jung.visualization.DefaultGraphLabelRenderer;
import edu.uci.ics.jung.visualization.GraphZoomScrollPane;
import edu.uci.ics.jung.visualization.Layout;
import edu.uci.ics.jung.visualization.PluggableRenderer;
import edu.uci.ics.jung.visualization.ShapePickSupport;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.contrib.TreeLayout;
import edu.uci.ics.jung.visualization.control.LayoutScalingControl;
import edu.uci.ics.jung.visualization.control.PickingGraphMousePlugin;
import edu.uci.ics.jung.visualization.control.PluggableGraphMouse;
import edu.uci.ics.jung.visualization.control.ScalingGraphMousePlugin;
import edu.uci.ics.jung.visualization.control.ViewScalingControl;

/**
 * A visualizer for the Routing table. This is only experimental!
 *
 */
public class RouteTableVisualizer implements RouteTableGraphCallback,
                                             RouteTableUICallback{
    
    private JPanel graphComponent;
    
    private JTextArea txtArea;
    
    private VisualizationViewer vv;
    
    private RouteTableGraph routeTableGraph;
    
    RouteTable routeTable;
    
    public static RouteTableVisualizer show(final RouteTable routeTable) {
        RouteTableVisualizer viz = new RouteTableVisualizer(routeTable);
        createAndShowFrame(viz, "RouteTable");
        return viz;
    }
    
    public static RouteTableVisualizer show(final Context context) {
        RouteTableVisualizer viz = new RouteTableVisualizer(context);
        createAndShowFrame(viz, context.getName());
        return viz;
    }
    
    private static void createAndShowFrame(final RouteTableVisualizer visualizer, final String name) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                final JFrame jf = new JFrame(name);
                jf.getContentPane().add (visualizer.getComponent());
                jf.pack();
                jf.addWindowListener(new WindowListener() {
                    public void windowActivated(WindowEvent e) {}
                    public void windowClosed(WindowEvent e) {}
                    public void windowClosing(WindowEvent e) {
                        visualizer.stop();
                    }
                    public void windowDeactivated(WindowEvent e) {}
                    public void windowDeiconified(WindowEvent e) {}
                    public void windowIconified(WindowEvent e) {}
                    public void windowOpened(WindowEvent e) {}
                });
                jf.setVisible(true);
            }
        });
    }
    
    public RouteTableVisualizer(Context dht) {
        this(dht.getRouteTable());
    }
    
    public RouteTableVisualizer(RouteTable routetTable) {
        routeTable = routetTable;
        routeTableGraph = new BucketGraph(routeTable, this);
        init();
    }
    
    private void init() {
        //now update the graph with the route table data
        routeTableGraph.populateGraph();

        //create layout
        Layout layout = new TreeLayout(routeTableGraph.getTree());
        //render graph
        PluggableRenderer pr = new PluggableRenderer();
        //vertex
        pr.setVertexPaintFunction(new RouteTableVertexPaintFunction(
                pr, 
                Color.black, 
                Color.white,
                Color.blue,
                Color.yellow,
                Color.red));
        pr.setVertexShapeFunction(new RouteTableVertexShapeFunction());
        VertexStringer vertStringer = new VertexStringer() {
            public String getLabel(ArchetypeVertex v) {
                return getLabelForVertex(v);
            }
            
        };
        pr.setVertexStringer(vertStringer);
        //edge
        EdgeStringer edgeStringer = new EdgeStringer(){
            public String getLabel(ArchetypeEdge e) {
                if(!(e instanceof BinaryEdge)) {
                    return e.toString();
                }
                BinaryEdge be = (BinaryEdge)e;
                return (be.getType().equals(EdgeType.LEFT)?"0":"1");
            }
        };
        pr.setEdgeStringer(edgeStringer);
        pr.setEdgePaintFunction(new PickableEdgePaintFunction(pr, Color.black, Color.cyan));
        pr.setEdgeShapeFunction(new EdgeShape.Line()); 
        pr.setGraphLabelRenderer(new DefaultGraphLabelRenderer(Color.cyan, Color.cyan));

        //create JPanel
        vv =  new VisualizationViewer(layout, pr, new Dimension(400,400));
        vv.setPickSupport(new ShapePickSupport());
        vv.setBackground(Color.white);
        // add a listener for ToolTips
        vv.setToolTipFunction(new RouteTableToolTipFunction());

        PluggableGraphMouse graphMouse = new PluggableGraphMouse();
        graphMouse.add(new PickingGraphMousePlugin());
        graphMouse.add(new ScalingGraphMousePlugin(
                new ViewScalingControl(), MouseEvent.CTRL_MASK));
        graphMouse.add(new ScalingGraphMousePlugin(
                new LayoutScalingControl(), 0));
        graphMouse.add(new RouteTableGraphMousePlugin(this));
        vv.setGraphMouse(graphMouse);
        
        //create south panel
        txtArea = new JTextArea();
        txtArea.setText(routeTableGraph.getGraphInfo());        
        JScrollPane pane = new JScrollPane(txtArea);
        
        //create main compononent
        JPanel graphPanel = new GraphZoomScrollPane(vv);
        graphComponent = new JPanel(new BorderLayout());
        graphComponent.add(graphPanel, BorderLayout.CENTER);
        graphComponent.add(pane, BorderLayout.SOUTH);
        repaint();
    }
    
    private synchronized void repaint() {
        vv.setGraphLayout(new TreeLayout(routeTableGraph.getTree()));
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                vv.invalidate();
                vv.revalidate();
                vv.repaint();
                txtArea.setText(routeTableGraph.getGraphInfo());
            }
        });
    }
    
    /**
     * Displays the graph containing all the Route Table's buckets
     */
    private synchronized void showBucketGraph() {
        stop();
        routeTableGraph = new BucketGraph(routeTable, this);
        routeTableGraph.populateGraph();
        repaint();
    }
    
    /**
     * Displays the graph containing all the specified bucket's contacts
     * 
     */
    private synchronized void showNodeGraph(Bucket bucket) {
        stop();
        routeTableGraph = new NodeGraph(routeTable, this, bucket);
        routeTableGraph.populateGraph();
        repaint();
    }
    
    private synchronized void updateTextArea() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                txtArea.setText(routeTableGraph.getGraphInfo());
            }
        });
    }
    
    private String getLabelForVertex(ArchetypeVertex v) {
        return routeTableGraph.getLabelForVertex(v);
    }
    
    public synchronized void handleGraphLayoutUpdated() {
        repaint();
    }
    
    public void handleRouteTableCleared() {
        showBucketGraph();
    }

    public synchronized void handleGraphInfoUpdated() {
        updateTextArea();        
    }
    
    public void handleBucketSelected(Bucket bucket) {
        showNodeGraph(bucket);
        updateTextArea();
    }

    public void handleNodeGraphRootSelected() {
        showBucketGraph();
    }

    public Component getComponent() {
        return graphComponent;
    }
    
    public void stop() {
        routeTableGraph.deregister();
    }
}
