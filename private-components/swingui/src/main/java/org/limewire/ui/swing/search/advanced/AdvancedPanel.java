package org.limewire.ui.swing.search.advanced;

import java.awt.Component;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import javax.swing.Action;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.library.FriendAutoCompleterFactory;
import org.limewire.core.api.search.SearchCategory;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.components.BasicAutoCompleter;
import org.limewire.ui.swing.components.CollectionBackedComboBoxModel;
import org.limewire.ui.swing.components.DropDownListAutoCompleteControl;
import org.limewire.ui.swing.components.FilteredDocument;
import org.limewire.ui.swing.components.TextFieldClipboardControl;
import org.limewire.ui.swing.util.FilePropertyKeyUtils;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

/** An abstract panel for creating advanced searches. */
abstract class AdvancedPanel extends JPanel {
    
    private final Map<FilePropertyKey, JComponent> componentMap =
        new EnumMap<FilePropertyKey, JComponent>(FilePropertyKey.class);
    private final SearchCategory category;

    private final FriendAutoCompleterFactory friendAutoCompleterFactory;
    
    private final Action enterKeyAction;
    
    private Component firstComponent;

	/**
	 * Resource package since this class is abstract.
	 */
    private static class Resources {
        @Resource(key="AdvancedPanel.font") public Font font;
        public Resources() {
            GuiUtils.assignResources(this);
        }
    }
    private final Resources resources = new Resources();
    
    
    /** 
     * Constructs an AdvancedPanel that will search the given category.
     */
    public AdvancedPanel(SearchCategory category, FriendAutoCompleterFactory friendAutoCompleterFactory, Action enterKeyAction) {
        super(new MigLayout("fillx", "[]related[grow]", ""));
        
        this.category = category;
        this.friendAutoCompleterFactory = friendAutoCompleterFactory;
        this.enterKeyAction = enterKeyAction;
        
        GuiUtils.assignResources(this);
    }

    /**
     * Adds a new JTextField that will search using the given FilePropertyKey using 
     *  the default for the description text.
     */
    protected void addField(FilePropertyKey key) {
       addField(I18n.tr(FilePropertyKeyUtils.getUntraslatedDisplayName(key, category)), key);
    }
    
    /** 
     * Adds a new JTextField that will search using the given FilePropertyKey. 
     */ 
    private void addField(String description, FilePropertyKey key) {
        JLabel label = new JLabel(description);
        label.setFont(resources.font);
        add(label);
        JTextField textField = new JTextField();
        TextFieldClipboardControl.install(textField);
        FilteredDocument.configure(textField, key);
        addEnterAction(textField);
        
        textField.setFont(resources.font);
        final DropDownListAutoCompleteControl autoCompleteControl = DropDownListAutoCompleteControl.install(textField, new BasicAutoCompleter(friendAutoCompleterFactory.getDictionary(category, key)));
        autoCompleteControl.setAutoComplete(true);
        add(textField, "growx, wrap");
        componentMap.put(key, textField);
        
        setFirstComponent(textField);
    }

    private void addEnterAction(JComponent component) {
        component.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "pressed");
        component.getActionMap().put("pressed", new AbstractAction("pressed") { 
            public void actionPerformed(ActionEvent e) {
                if(enterKeyAction != null) {
                    enterKeyAction.actionPerformed(e);
                }
            }
        });
    }
    
    /**
     * Adds a new JTextField that will search using the given FilePropertyKey using 
     *  the default for the description text.
     * The contents of the combo are all the possible values.
     */
    protected void addField(FilePropertyKey key, List<String> possibleValues) {
        addField(I18n.tr(FilePropertyKeyUtils.getUntraslatedDisplayName(key, category)), key, possibleValues);
    }
    
    /**
     * Adds a new JComboBox that will search using the given FilePropertyKey.
     * The contents of the combo are all the possible values.
     */
    private void addField(String name, FilePropertyKey key, List<String> possibleValues) {
        JLabel label = new JLabel(name);
        label.setFont(resources.font);
        add(label);
        JComboBox comboBox = new JComboBox(new CollectionBackedComboBoxModel(possibleValues));
        addEnterAction(comboBox);
        comboBox.setFont(resources.font);
        add(comboBox, "growx, wrap");
        componentMap.put(key, comboBox);
        
        setFirstComponent(comboBox);
    }

    /** 
     * Gets a map between the key for each field with user data and that data.
     * 
     * <p> Describes the requested advanced search request. 
     */
    public Map<FilePropertyKey, String> getSearchData() {
        Map<FilePropertyKey, String> searchData = new EnumMap<FilePropertyKey, String>(FilePropertyKey.class);
        for(Map.Entry<FilePropertyKey, JComponent> entry : componentMap.entrySet()) {
            String value = getData(entry.getValue());
            if(value != null && !value.trim().equals("")) {
                searchData.put(entry.getKey(), value);
            }
        }
        if(searchData.size() > 0) {
            return searchData;
        } else {
            return null;
        }
    }
    
    /**
     * @return the {@link SearchCategory} that this panel represents.
     */
    public SearchCategory getSearchCategory() {
        return category;
    }
    
    private String getData(JComponent component) {
        if(component instanceof JTextField) {
            return ((JTextField)component).getText();
        } else if(component instanceof JComboBox) {
            return ((JComboBox)component).getSelectedItem().toString();
        } else {
            throw new IllegalStateException("invalid component: " + component);
        }
    }
    
    /**
     * Sets the first focusable component only if it is not already set.
     */
    private void setFirstComponent(Component component) {
        if (firstComponent == null && component.isFocusable()) {
            firstComponent = component;
        }
    }
    
    /**
     * Requests focus on the first component in the panel.
     */
    public boolean requestFocusFirstComponent() {
        if (firstComponent != null) {
            return firstComponent.requestFocusInWindow();
        } else {
            return false;
        }
    }
}
