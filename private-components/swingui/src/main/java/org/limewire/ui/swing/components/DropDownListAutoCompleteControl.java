package org.limewire.ui.swing.components;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import org.limewire.concurrent.FutureEvent;
import org.limewire.concurrent.ListeningFuture;
import org.limewire.listener.EventListener;
import org.limewire.listener.SwingEDTEvent;
import org.limewire.ui.swing.components.AutoCompleter.AutoCompleterCallback;


/** A DropDown list of autocompletable items for a JTextField. */
public class DropDownListAutoCompleteControl {
    
    private static final String PROPERTY = "limewire.text.autocompleteControl";

    /** The text field this is working on. */
    private final JTextField textField;
    
    /** The autocompleter. */
    private final AutoCompleter autoCompleter;

    /** The popup the scroll pane is in. */
    protected JPopupMenu popup;

    /** Whether or not we tried to show a popup while this wasn't showing */
    protected boolean showPending;
    
    /** Whether or not this control should try to autocomplete input. */
    private boolean autoComplete = true;
    
    /** Installs a dropdown list for autocompletion on the given text field. */
    public static DropDownListAutoCompleteControl install(final JTextField textField) {
        final BasicAutoCompleter basicAutoCompleter = new BasicAutoCompleter();
        textField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                basicAutoCompleter.addAutoCompleteSuggestion(textField.getText());
            }
        });
        return install(textField, basicAutoCompleter);
    }

    /** Installs a dropdown list using the given autocompleter. */
    public static DropDownListAutoCompleteControl install(JTextField textField,
            AutoCompleter autoCompleter) {
        DropDownListAutoCompleteControl control = new DropDownListAutoCompleteControl(textField, autoCompleter);
        textField.putClientProperty(PROPERTY, control);
        Listener listener = control.new Listener();
        textField.addKeyListener(listener);
        textField.addHierarchyListener(listener);
        textField.addFocusListener(listener);
        textField.addActionListener(listener);
        autoCompleter.setAutoCompleterCallback(listener);
        return control;
    }
    
    
    /** Returns the control for the text field. */
    public static DropDownListAutoCompleteControl getDropDownListAutoCompleteControl(JTextField textField) {
        return (DropDownListAutoCompleteControl)textField.getClientProperty(PROPERTY);
    }
    
    private DropDownListAutoCompleteControl(JTextField textField, AutoCompleter autoCompleter) {
        this.textField = textField;
        this.autoCompleter = autoCompleter;
    }    
    
    /**
    * Sets whether the component is currently performing autocomplete lookups as
    * keystrokes are performed.
    *
    * @param autoComplete true or false.
    */
    public void setAutoComplete(boolean autoComplete) {
        this.autoComplete = autoComplete;
    }
    
    /**
    * Gets whether the component is currently performing autocomplete lookups as
    * keystrokes are performed. 
    *
    * @return true or false.
    */
    public boolean isAutoCompleting() {
        return autoComplete;
    }
    
    /**
     * Displays the popup window with a list of auto-completable choices,
     * if any exist.
     */
    private void autoCompleteInput() {
        // Shove this into an invokeLater to force us seeing the proper text.
        if(isAutoCompleting()) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    String input = textField.getText();
                    if (input != null && input.length() > 0) {
                        ListeningFuture<Boolean> future = autoCompleter.setInput(input);
                        // If it finished immediately, don't hide it!
                        if(!future.isDone()) {
                            hidePopup();
                        }
                        future.addFutureListener(new EventListener<FutureEvent<Boolean>>() {
                            @Override
                            @SwingEDTEvent
                            public void handleEvent(FutureEvent<Boolean> event) {
                                // == Boolean.TRUE to prevent NPE on unbox if result is null
                                if(event.getResult() == Boolean.TRUE) {
                                    showPopup();
                                } else {
                                    hidePopup();
                                }
                            }
                        });
                    } else {
                        hidePopup();
                    }
                }
            });
        }
    }

    /**
     * Creates a new popup containing the specified component. 
     */
    private JPopupMenu createPopup(Component component) {
        // Create popup.  We only display the border for the popup component,
        // so we remove it from the popup container.  Also, we want to keep the 
        // focus within the text field, so the popup should not be focusable.
        JPopupMenu popupMenu = new JPopupMenu();
        popupMenu.setBorder(BorderFactory.createEmptyBorder());
        popupMenu.setFocusable(false);
        popupMenu.add(component);

        // Add listener to reset popup reference when hidden.
        popupMenu.addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {
            }
            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                resetPopup();
            }
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
            }
        });
        
        return popupMenu;
    }
    
    /** Shows the popup. */
    private void showPopup() {
        if(autoCompleter.isAutoCompleteAvailable()) {
            if(textField.isShowing()) {
                Component parent = textField;
                JComponent component = autoCompleter.getRenderComponent();

                // Adjust popup position for text field with painted border.
                int leftInset = 0;
                int bottomInset = 0;
                int widthInset = 0;
                if (textField instanceof Paintable) {
                    Insets paintedInsets = ((Paintable) textField).getPaintedInsets();
                    leftInset = paintedInsets.left;
                    bottomInset = paintedInsets.bottom;
                    widthInset = paintedInsets.left + paintedInsets.right;
                }

                // Null out our prior preferred size, then set a new one
                // that overrides the width to be the size we want it, but
                // preserves the height.
                Dimension priorPref = component.getPreferredSize();
                component.setPreferredSize(null);
                Dimension pref = component.getPreferredSize();
                pref = new Dimension(textField.getWidth() - widthInset, pref.height+10);
                component.setPreferredSize(pref);
                if(popup != null && priorPref.equals(pref)) {
                    return; // no need to change if sizes are same.
                }
                
                // If the popup exists already, hide it & reshow it to make it the right size.
                if (popup != null) {
                    hidePopup();
                }

                popup = createPopup(component);
                showPending = false;
                popup.show(parent, leftInset, textField.getHeight() - bottomInset);
                
            } else {
                showPending = true;
            }
        }
    }

    /** Hides the popup window. */
    private void hidePopup() {
        if (popup != null) {
            popup.setVisible(false);
        }
        resetPopup();
    }

    /** Resets popup references.  Called when popup is hidden. */
    private void resetPopup() {
        showPending = false;
        popup = null;
    }
    
    private class Listener implements ActionListener, KeyListener, HierarchyListener, FocusListener, AutoCompleterCallback {
        
        @Override
        public void itemSuggested(String autoCompleteString, boolean keepPopupVisible, boolean triggerAction) {
            textField.setText(autoCompleteString);
            textField.setCaretPosition(textField.getDocument().getLength());
            if(triggerAction) {
                textField.postActionEvent();
            } else if(!keepPopupVisible) {
                hidePopup();
            }
        }
        
        /**
         * Fires an action event.
         * <p>
         * If the popup is visible, this resets the current
         * text to be the selection on the popup (if something was selected)
         * prior to firing the event.
         */
        @Override
        public void actionPerformed(ActionEvent e) {
            if(popup != null) {
                String selection = autoCompleter.getSelectedAutoCompleteString();
                hidePopup();
                if(selection != null) {
                    textField.setText(selection);
                }
            }
        }        
        
        /** Forwards necessary events to the AutoCompleteList. */
        @Override
        public void keyPressed(KeyEvent evt) {
            if(evt.getKeyCode() == KeyEvent.VK_UP || evt.getKeyCode() == KeyEvent.VK_DOWN) {
                evt.consume();    
            }
            
            if(autoCompleter != null) {
                switch(evt.getKeyCode()) {
                case KeyEvent.VK_UP:
                    if(popup != null) {
                        autoCompleter.decrementSelection();
                    } else {
                        String input = textField.getText();
                        autoCompleter.setInput(input).addFutureListener(new EventListener<FutureEvent<Boolean>>() {
                            @Override
                            @SwingEDTEvent
                            public void handleEvent(FutureEvent<Boolean> event) {
                                // == Boolean.TRUE to prevent NPE on unbox if result is null
                                if(event.getResult() == Boolean.TRUE) {
                                    showPopup();
                                }
                            }
                        });
                    }
                    break;
                case KeyEvent.VK_DOWN:
                    if(popup != null) {
                        autoCompleter.incrementSelection();
                    } else { 
                        String input = textField.getText();
                        autoCompleter.setInput(input).addFutureListener(new EventListener<FutureEvent<Boolean>>() {
                            @Override
                            @SwingEDTEvent
                            public void handleEvent(FutureEvent<Boolean> event) {
                                // == Boolean.TRUE to prevent NPE on unbox if result is null
                                if(event.getResult() == Boolean.TRUE) {
                                    showPopup();
                                }
                            }
                        });
                    }
                    break;
                case KeyEvent.VK_LEFT:
                case KeyEvent.VK_RIGHT:
                    if(popup != null) {
                        String selection = autoCompleter.getSelectedAutoCompleteString();
                        if(selection != null) {
                            hidePopup();
                        }
                    }
                    break;
                }
            }
        }
        
        /** Forwards necessary events to the AutoCompleteList. */
        @Override
        public void keyReleased(KeyEvent evt) {
            if(evt.getKeyCode() == KeyEvent.VK_UP || evt.getKeyCode() == KeyEvent.VK_DOWN)
                evt.consume();       
        }
        
        /** Forwards necessary events to the AutoCompleteList. */
        @Override
        public void keyTyped(KeyEvent evt) {
            if(evt.getKeyCode() == KeyEvent.VK_UP || evt.getKeyCode() == KeyEvent.VK_DOWN) {
                evt.consume();                
            }
            
            if(autoCompleter != null) {
                switch(evt.getKeyChar()) {
                case KeyEvent.VK_ESCAPE:
                    if (popup != null) {
                        hidePopup();
                        textField.selectAll();
                    }
                    break;
                case KeyEvent.VK_ENTER:
                    break;
                default:
                    autoCompleteInput();
                }
            }
        }
        
        @Override
        public void hierarchyChanged(HierarchyEvent evt) {            
            if((evt.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) == HierarchyEvent.SHOWING_CHANGED) {
                boolean showing = textField.isShowing();
                if(!showing && popup != null) {
                    hidePopup();
                } else if(showing && popup == null && showPending) {
                    autoCompleteInput();
                }
            }
        }
        
        @Override
        public void focusGained(FocusEvent e) {
        }

        @Override
        public void focusLost(FocusEvent evt) {
            if (evt.getID() == FocusEvent.FOCUS_LOST && popup != null) {
                hidePopup();
            }
        }
    }
}
