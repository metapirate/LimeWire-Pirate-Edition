package org.limewire.mojito.visual;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.geom.QuadCurve2D;
import java.net.SocketAddress;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.limewire.mojito.KUID;
import org.limewire.mojito.io.MessageDispatcher.MessageDispatcherEvent.EventType;
import org.limewire.mojito.messages.DHTMessage.OpCode;

/**
 * This looks a bit like a 2D Plasma Lamp (also known as
 * Plasma Ball).
 */
class PlasmaLamp extends Painter {

    private static final long ATTACK = 250L;
    
    private static final long RELEASE = 2750L;
    
    private static final long DURATION = ATTACK + RELEASE;
    
    private static final float DOT_SIZE = 6f;
    
    private static final Random GENERATOR = new Random();

    private final List<Node> nodes = new LinkedList<Node>();
    
    private final Point2D.Double localhost = new Point2D.Double();
    
    private final Ellipse2D.Double ellipse = new Ellipse2D.Double();
    
    private final Ellipse2D.Double dot = new Ellipse2D.Double();
    
    private final KUID nodeId;
    
    public PlasmaLamp(KUID nodeId) {
        this.nodeId = nodeId;
    }
    
    @Override
    public void paint(Component c, Graphics2D g2) {
        double width = c.getWidth();
        double height = c.getHeight();
        
        double gap = 50d;
        double radius = Math.max(Math.min(width/2d, height/2d) - gap, gap);
        
        double arc_x = width/2d-radius;
        double arc_y = height/2d-radius;
        double arc_width = 2d*radius;
        double arc_height = 2d*radius;
        
        g2.setColor(Color.orange);
        g2.setStroke(TWO_PIXEL_STROKE);
        
        ellipse.setFrame(arc_x, arc_y, arc_width, arc_height);
        g2.draw(ellipse);
        
        double fi = position(nodeId, 2d*Math.PI) - Math.PI/2d;
        double dx = width/2d + radius * Math.cos(fi);
        double dy = height/2d + radius * Math.sin(fi);
        
        localhost.setLocation(dx, dy);
        
        dot.setFrame(dx - DOT_SIZE/2d, dy - DOT_SIZE/2d, 
                DOT_SIZE, DOT_SIZE);
        
        synchronized (nodes) {
            for (Iterator<Node> it = nodes.iterator(); it.hasNext(); ) {
                if (it.next().paint(localhost, width, height, radius, g2)) {
                    it.remove();
                }
            }
        }
        
        g2.setColor(Color.orange);
        g2.setStroke(DEFAULT_STROKE);
        g2.fill(dot);
    }
    
    @Override
    public void handle(EventType type, KUID nodeId, SocketAddress dst, OpCode opcode, boolean request) {
        if (nodeId == null) {
            return;
        }
        
        synchronized (nodes) {
            nodes.add(new Node(dot, type, nodeId, opcode, request));
        }
    }
    
    @Override
    public void clear() {
        synchronized (nodes) {
            nodes.clear();
        }
    }
    
    private static class Node {
        
        private final Ellipse2D.Double dot;
        
        private final EventType type;
        
        private final KUID nodeId;
        
        private final boolean request;
        
        private final int noise;
        
        private final long timeStamp = System.currentTimeMillis();
        
        private final Point2D.Double remote = new Point2D.Double();
        
        private final Point2D.Double point = new Point2D.Double();
        
        private final QuadCurve2D.Double curve = new QuadCurve2D.Double();
        
        private final Ellipse2D.Double circle = new Ellipse2D.Double();
        
        private final Ellipse2D.Double prxDot = new Ellipse2D.Double();
        
        private final Stroke stroke;
        
        public Node(Ellipse2D.Double dot, EventType type, KUID nodeId, OpCode opcode, boolean request) {
            this.dot = dot;
            this.type = type;
            this.nodeId = nodeId;
            this.request = request;
            
            this.stroke = getStrokeForOpCode(opcode);
            
            int noise = GENERATOR.nextInt(50);
            if (GENERATOR.nextBoolean()) {
                noise = -noise;
            }
            this.noise = noise;
        }
        
        private int alpha() {
            long delta = System.currentTimeMillis() - timeStamp;
            
            if (delta < ATTACK) {
                return (int)(255f/ATTACK * delta);
            }
            
            return Math.max(255 - (int)(255f/DURATION * delta), 0);
        }
        
        private double radius() {
            final double r = 20d;
            long delta = System.currentTimeMillis() - timeStamp;
            if (delta < DURATION) {
                return r/DURATION * delta;
            }
            return r;
        }
        
        public boolean paint(Point2D.Double localhost, 
                double width, double height, double radius, Graphics2D g2) {
            
            double cx = width/2d;
            double cy = height/2d;
            
            double fi = position(nodeId, 2d*Math.PI) - Math.PI/2d;
            
            double dx = cx + radius * Math.cos(fi);
            double dy = cy + radius * Math.sin(fi);
            
            int red = 0;
            int green = 0;
            int blue = 0;
            
            if (type.equals(EventType.MESSAGE_SENT)) {
                red = 255;
                if (!request) {
                    blue = 255;
                }
            } else {
                green = 255;
                if (request) {
                    blue = 255;
                }
            }
            
            remote.setLocation(dx, dy);
            
            Point2D.Double corner = new Point2D.Double(
                    localhost.x + 3 * dot.width, localhost.y + 3 * dot.height);
            
            this.prxDot.setFrameFromCenter(localhost, corner);
            
            Shape shape = null;
            if (!prxDot.contains(remote)) {
                point.setLocation(cx+noise, cy+noise);
                curve.setCurve(localhost, point, remote);
                shape = curve;
            } else {
                double r = radius();
                point.setLocation(localhost.x+r, localhost.y+r);
                circle.setFrameFromCenter(localhost, point);
                shape = circle;
            }
            
            if (shape != null) {
                g2.setStroke(stroke);
                g2.setColor(new Color(red, green, blue, alpha()));
                g2.draw(shape);
            }
            
            //g2.setStroke(ONE_PIXEL_STROKE);
            //g2.setColor(Color.red);
            //g2.draw(prxDot);
            
            return System.currentTimeMillis() - timeStamp >= DURATION;
        }
    }
}
