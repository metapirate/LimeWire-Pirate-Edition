package org.limewire.ui.swing.components;

import java.awt.Component;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.text.JTextComponent;
import javax.swing.text.Document;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;

import org.limewire.i18n.I18nMarker;
import org.limewire.ui.swing.util.I18n;

/**
 * Enables clipboard controls on a JTextComponent.
 */
public class TextFieldClipboardControl {
    
    private static String UNDO_MANAGER_FIELD = "limewire.text.undoManager";
    
    /**
     * The undo action.
     */
    private static Action UNDO_ACTION = new TextFieldAction(I18nMarker.marktr("Undo")) {
        public void actionPerformed(ActionEvent e) {
            JTextComponent textField = getTextField(e);
            UndoManager undoManager = getUndoManager(textField);
            try {
                if(undoManager != null)
                    undoManager.undoOrRedo();
            } catch(CannotUndoException ignored) {
            } catch(CannotRedoException ignored) {
            }
        }
    };
    
    /**
     * The cut action
     */
    private static Action CUT_ACTION = new TextFieldAction(I18nMarker.marktr("Cut")) {
        public void actionPerformed(ActionEvent e) {
            getTextField(e).cut();
        }
    };
    
    /**
     * The copy action.
     */
    private static Action COPY_ACTION = new TextFieldAction(I18nMarker.marktr("Copy")) {
        public void actionPerformed(ActionEvent e) {
            getTextField(e).copy();
        }
    };
    
    /**
     * The paste action.
     */
    private static Action PASTE_ACTION = new TextFieldAction(I18nMarker.marktr("Paste")) {
        public void actionPerformed(ActionEvent e) {
            getTextField(e).paste();
        }
    };
    
    /**
     * The delete action.
     */
    private static Action DELETE_ACTION = new TextFieldAction(I18nMarker.marktr("Delete")) {
        public void actionPerformed(ActionEvent e) {
            getTextField(e).replaceSelection("");
        }
    };
      
    /**
     * The select all action.
     */      
    private static Action SELECT_ALL_ACTION = new TextFieldAction(I18nMarker.marktr("Select All")) {
        public void actionPerformed(ActionEvent e) {
            getTextField(e).selectAll();
        }
    };    
    
    /**
     * The sole JPopupMenu that's shared among all the text fields.
     */
    private static final JPopupMenu POPUP = createPopup();
    
    public static void install(JTextComponent textField) {
        textField.setComponentPopupMenu(POPUP);
        
        UndoManager undoManager = new UndoManager();
        undoManager.setLimit(1);
        installUndoManager(textField, undoManager);
    }
    
    protected static UndoManager getUndoManager(JTextComponent textField) {
        return (UndoManager)textField.getClientProperty(UNDO_MANAGER_FIELD);
    }

    private static void installUndoManager(JTextComponent textField, final UndoManager undoManager) {
        textField.getDocument().addUndoableEditListener(undoManager);
        textField.putClientProperty(UNDO_MANAGER_FIELD, undoManager);
        textField.addPropertyChangeListener("document", new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                Document oldDoc = (Document)evt.getOldValue();
                if(oldDoc != null) {
                    oldDoc.removeUndoableEditListener(undoManager);
                }
                JTextComponent textField = (JTextComponent)evt.getSource();
                textField.removePropertyChangeListener("document", this);
            }
        });
    }
    
    /**
     * Creates the JPopupMenu that all LimeTextFields will share.
     */
    private static JPopupMenu createPopup() {
        JPopupMenu popup;

        // initialize the JPopupMenu with necessary stuff.
        popup = new JPopupMenu() {
            @Override
            public void show(Component invoker, int x, int y) {
                updateActions((JTextComponent)invoker);
                super.show(invoker, x, y);
            }
        };
        
        popup.add(new JMenuItem(UNDO_ACTION));
        popup.addSeparator();
        popup.add(new JMenuItem(CUT_ACTION));
        popup.add(new JMenuItem(COPY_ACTION));
        popup.add(new JMenuItem(PASTE_ACTION));
        popup.add(new JMenuItem(DELETE_ACTION));
        popup.addSeparator();
        popup.add(new JMenuItem(SELECT_ALL_ACTION));
        return popup;
    }
    
    /**
     * Updates the actions in each text just before showing the popup menu.
     */
    private static void updateActions(JTextComponent textField) {
        String selectedText = textField.getSelectedText();
        if(selectedText == null)
            selectedText = "";
        
        boolean stuffSelected = !selectedText.equals("");
        boolean allSelected = selectedText.equals(textField.getText());
        
        UNDO_ACTION.setEnabled(textField.isEnabled() && textField.isEditable() && isUndoAvailable(textField));
        CUT_ACTION.setEnabled(textField.isEnabled() && textField.isEditable() && stuffSelected);
        COPY_ACTION.setEnabled(textField.isEnabled() && stuffSelected);
        PASTE_ACTION.setEnabled(textField.isEnabled() && textField.isEditable() && isPasteAvailable(textField));
        DELETE_ACTION.setEnabled(textField.isEnabled() && textField.isEditable() && stuffSelected);
        SELECT_ALL_ACTION.setEnabled(textField.isEnabled() && !allSelected);
    }
    
    /**
     * Determines if an Undo is available.
     */
    private static boolean isUndoAvailable(JTextComponent textField) {
        UndoManager undoManager = getUndoManager(textField);
        return undoManager != null && undoManager.canUndoOrRedo();
    }
    
    /**
     * Determines if paste is currently available.
     */
    private static boolean isPasteAvailable(JTextComponent textField) {
        try {
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            return clipboard.isDataFlavorAvailable(DataFlavor.stringFlavor);
        } catch(UnsupportedOperationException he) {
            return false;
        } catch(IllegalStateException ise) {
            return false;
        }
    }
    
    private static abstract class TextFieldAction extends AbstractAction {
        
        /**
         * Constructs a new FieldAction looking up the name from the MessagesBundles.
         */
        public TextFieldAction(String name) {
            super(I18n.tr(name));
        }
        
        /**
         * Gets the JTextComponent for the given ActionEvent.
         */
        protected JTextComponent getTextField(ActionEvent e) {
            JMenuItem source = (JMenuItem)e.getSource();
            JPopupMenu menu = (JPopupMenu)source.getParent();
            return (JTextComponent)menu.getInvoker();
        }

    }
}
