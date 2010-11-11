package org.limewire.ui.swing.components;

import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JToolTip;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;

public class MarqueeButton extends JButton {
    private JToolTip toolTip = new JToolTip();
    private int position;
    private int maxPosition;
    private Timer timer;

    private int repeatCount = 5;
    private int initialDelay = 30;
    private String marqueeString;
    //private int maxDelay = 50;

    public MarqueeButton(String text, int maxCharsShown) {
        super(text);
        setHorizontalAlignment(SwingConstants.LEFT);
        toolTip.setComponent(this);
        setMargin(new Insets(0, 0, 0, 0));
        setBorderPainted(false);
        setRolloverEnabled(true);
        setContentAreaFilled(false);
        setOpaque(false);
        setFocusPainted(false);
        setMaxChars(maxCharsShown);
        setToolTipText(getText());
        addAncestorListener(new AncestorListener() {            
            @Override
            public void ancestorRemoved(AncestorEvent event) {
                if (!event.getAncestor().isAncestorOf(MarqueeButton.this)) {
                    //we've been removed from the ancestor
                    stop();
                }
            }
            
            @Override
            public void ancestorMoved(AncestorEvent event) {}
            
            @Override
            public void ancestorAdded(AncestorEvent event) {}
        });
    }
    
    public void start() {
        stop();


        StringBuilder marqueeBuilder = new StringBuilder();
        for (int i = 0; i < repeatCount; i++) {
            marqueeBuilder.append(getText());
            if (i != repeatCount - 1) {
                marqueeBuilder.append("          ");
            }
        }
        
        marqueeString = marqueeBuilder.toString();

        JLabel marqueeLabel = new JLabel(marqueeString);
        marqueeLabel.setFont(getFont());
        
        JLabel fullLabel = new JLabel(getText());
        fullLabel.setFont(getFont());
        
        maxPosition = marqueeLabel.getPreferredSize().width - fullLabel.getPreferredSize().width;
        position = 0;
        
        timer = new Timer(initialDelay, new MarqueeAction());
        timer.start();
    }
    
    public void stop() {
        if (timer != null) {
            timer.stop();
            timer = null;
            position = 0;
            repaint();
        }
    }
    
    @Override
    public void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D)g.create();

        g2.setFont(getFont());
        FontMetrics metrics = g2.getFontMetrics();
        g2.setColor(getForeground());
        
        if (timer != null && timer.isRunning()) {
            //TODO: do this properly, ie not - 1
            g2.drawString(marqueeString, -position, getHeight() / 2 + metrics.getAscent()/2 -1);
        } else {
            g2.drawString(getText(), 0, getHeight() / 2 + metrics.getAscent()/2 -1);
        }
        
        g2.dispose();
    }
    
    public void setMaxChars(int maxCharsShown){
        StringBuilder fillerBuilder = new StringBuilder();
        for(int i = 0; i < maxCharsShown; i++){
            fillerBuilder.append('X');
        }
        String oldText = getText();
        setText(fillerBuilder.toString());
        setMaximumSize(getPreferredSize());
        setPreferredSize(getMaximumSize());
        setText(oldText);
    }

    @Override
    public JToolTip createToolTip(){
        return toolTip;
    }
    
    public JToolTip getToolTip(){
        return toolTip;
    }
    
    
    private class MarqueeAction implements ActionListener {
        
        @Override
        public void actionPerformed(ActionEvent e) {
            position += 1;
            repaint();
            
            if (position >= maxPosition){
                position = 0;
            }
        }       
    }
    
    //not using variable speed for now
//    private class MarqueeAction implements ActionListener {
//        
//        @Override
//        public void actionPerformed(ActionEvent e) {
//            if (repeatCount <= 0) {
//                timer.stop();
//            }
//            
//            if (position >= maxPosition) {
//                position = -getWidth();
//                repeatCount--;
//                timer.setDelay(initialDelay);
//            }
//            position += 1;
//            repaint();
//            if (position % 5 == 0 && timer.getDelay() < maxDelay) {
//                timer.setDelay(timer.getDelay() + 3);
//            }
//        }       
//    }
   
}