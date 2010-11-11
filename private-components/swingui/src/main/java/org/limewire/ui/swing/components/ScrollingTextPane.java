package org.limewire.ui.swing.components;

import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JEditorPane;
import javax.swing.JScrollPane;
import javax.swing.Timer;
import javax.swing.event.HyperlinkListener;

/**
 * Extend <tt>JScrollPane</tt> so that a scrolled HTML file is shown.
 */
public final class ScrollingTextPane extends JScrollPane {

    /**
     * <tt>JEditorPane</tt> to show the text.
     */
    private final JEditorPane EDITOR_PANE;

    /**
     * Timer to control scrolling.
     */
    protected Timer _timer;

    /**
     * Constructs the elements of the about window.
     * 
     * @param html the text of the HTML to load into the scrolling pane.
     */
    public ScrollingTextPane(String html) {
        if (html == null)
            throw new NullPointerException("null html");
        
         EDITOR_PANE = new JEditorPane("text/html", html);
         EDITOR_PANE.setMargin(new Insets(5, 5, 5, 5));
         // don't allow edit of editor pane - use it just as a viewer
         EDITOR_PANE.setEditable(false);
         
         // add it to the scrollpane
         getViewport().add(EDITOR_PANE);
         
         // enable double buffering for smooth scroll effect
         this.setDoubleBuffered(true);

         // create timer
         Action scrollText = new AbstractAction() {
             public void actionPerformed(ActionEvent e) {
                 scroll();
             }
         };
         _timer = new Timer(50, scrollText);
         
         // set view to beginning of pane
         EDITOR_PANE.setCaretPosition(0);
    }

    /**
     * Start scrolling.
     */
    public void startScroll() {
        _timer.start();
    }

    /**
     * Stop scrolling.
     */
    public void stopScroll() {
        _timer.stop();
    }

    /**
     * Scroll the content of the JEditorPane.
     */
    protected void scroll() {
        // calculate visible rectangle
        Rectangle rect = EDITOR_PANE.getVisibleRect();
        
        // get x / y values
        int x = rect.x;
        
        int y = this.getVerticalScrollBar().getValue(); 
        
        if((y+rect.height) >= EDITOR_PANE.getHeight()) {
            return;
        }
        else {
            y += 1;
        }
        
        Rectangle rectNew = 
            new Rectangle(x, y,(x + rect.width), 
                    (y + rect.height));
        
        
        // scroll to current position
        EDITOR_PANE.scrollRectToVisible(rectNew);
    }
    
    /**
     * Adds a <tt>HyperlinkListener</tt> instance to the underlying
     * <tt>JEditorPane</tt> instance.
     *
     * @param listener the listener for hyperlinks
     */
    public void addHyperlinkListener(HyperlinkListener listener) {
        EDITOR_PANE.addHyperlinkListener(listener);
    }
}
