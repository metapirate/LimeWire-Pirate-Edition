package org.limewire.ui.swing.components;

import java.awt.event.ActionListener;
import java.awt.event.MouseListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

import org.limewire.ui.swing.action.ActionKeys;
import org.limewire.ui.swing.listener.ActionHandListener;
import org.limewire.ui.swing.listener.MouseActionListener;


/**
 * A label that has a clickable text.
 */
public class ActionLabel extends JLabel  {
    
    private MouseListener urlListener;
    
    private PropertyChangeListener listener = null;

    private Action currentAction;
    
    private final List<ActionListener> actionListeners = new CopyOnWriteArrayList<ActionListener>();
   

    public ActionLabel(Action action) {
        this(action, false);
    }
    
    /**
     * Constructs a new clickable label.
     */
    public ActionLabel(Action action, boolean showHand) {
        setAction(action, showHand);
        setHorizontalTextPosition(SwingConstants.RIGHT);
        setHorizontalAlignment(SwingConstants.LEFT);
    }
    
    public void addActionListener(ActionListener listener) {
        actionListeners.add(listener);
    }
    
    public void removeActionListener(ActionListener listener) {
        actionListeners.remove(listener);
    }
    
    public void setAction(Action action, boolean showHand) {
        // remove old listener
        Action oldAction = getAction();
        if (oldAction != null) {
            oldAction.removePropertyChangeListener(getListener());
        }

        // add listener
        currentAction = action;
        currentAction.addPropertyChangeListener(getListener());
        if(showHand) {
            installListener(new ActionHandListener(new ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    currentAction.actionPerformed(e);
                    for(ActionListener listener : actionListeners) {
                        listener.actionPerformed(e);
                    }
                }
            }));
        } else {
            installListener(new MouseActionListener(new ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    currentAction.actionPerformed(e);
                    for(ActionListener listener : actionListeners) {
                        listener.actionPerformed(e);
                    }
                }
            }));
        }
        updateLabel(currentAction, null);
    }
    
    public Action getAction(){
        return currentAction;
    }
       
    private PropertyChangeListener getListener() {
        if (listener == null) {
            listener = new PropertyChangeListener() {
                public void propertyChange(PropertyChangeEvent evt) { 
                    updateLabel(null, evt);                    
                }
            };
        }
        return listener;
    }
    
    /*
     * Update label text based on action event
     */
    public void updateLabel(Action action, PropertyChangeEvent evt) {
        if(evt == null) {
            assert action != null;
            String display = (String) currentAction.getValue(Action.NAME);
            setIcon((Icon) currentAction.getValue(Action.SMALL_ICON));
            setToolTipText((String) currentAction.getValue(Action.SHORT_DESCRIPTION));
            setVisible(!Boolean.FALSE.equals(currentAction.getValue(ActionKeys.VISIBLE)));
            if(display != null) {
                setText(display);
            }
        } else {
            assert action == null;
            String id = evt.getPropertyName();
            if(id.equals(Action.NAME)) {
                setText((String)evt.getNewValue());
            } else if(id.equals(Action.SMALL_ICON)) {
                setIcon((Icon)evt.getNewValue());
            } else if(id.equals(Action.SHORT_DESCRIPTION)) {
                setToolTipText((String)evt.getNewValue());
            } else if(id.equals(ActionKeys.VISIBLE)) {
                setVisible(!Boolean.FALSE.equals(evt.getNewValue()));
            }
        }   
    }
   
    private void installListener(MouseListener listener) {
        if (urlListener != null) {
            removeMouseListener(urlListener);
        }
        urlListener = listener;
        addMouseListener(urlListener);
    }
}