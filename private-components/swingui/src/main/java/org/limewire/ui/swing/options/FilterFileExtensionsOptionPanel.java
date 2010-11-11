package org.limewire.ui.swing.options;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.swingx.JXTable;
import org.limewire.core.api.Category;
import org.limewire.core.api.file.CategoryManager;
import org.limewire.core.api.spam.SpamManager;
import org.limewire.core.settings.FilterSettings;
import org.limewire.core.settings.LibrarySettings;
import org.limewire.setting.Setting;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.components.MultiLineLabel;
import org.limewire.ui.swing.components.TextFieldClipboardControl;
import org.limewire.ui.swing.options.actions.OKDialogAction;
import org.limewire.ui.swing.table.MouseableTable;
import org.limewire.ui.swing.util.BackgroundExecutorService;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.IconManager;
import org.limewire.util.OSUtils;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.CompositeList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.gui.TableFormat;
import ca.odell.glazedlists.swing.DefaultEventTableModel;

import com.google.inject.Inject;

/**
 * Creates a table to manage which file extensions will not show up in search results.
 */
public class FilterFileExtensionsOptionPanel extends AbstractFilterOptionPanel {
    
    private final SpamManager spamManager;
    private final CategoryManager categoryManager;
    private final UnsafeTypeOptionPanelStateManager settingStateManager;
    
    private JButton defaultButton;
    private JButton okButton;
    private JTextField keywordTextField;
    private JButton addKeywordButton;
    private JXTable filterTable;

    private final EventList<String> immutableProgramsList;
        
    @Inject
    public FilterFileExtensionsOptionPanel(IconManager iconManager, SpamManager spamManager,
            CategoryManager categoryManager,
            UnsafeTypeOptionPanelStateManager settingStateManager) {
        
        this.spamManager = spamManager;
        this.categoryManager = categoryManager;
        this.settingStateManager = settingStateManager;
        
        setLayout(new MigLayout("gapy 10, nogrid"));
        
        keywordTextField = new JTextField(30);
        addKeywordButton = new JButton(I18n.tr("Add Extension"));
        
        TextFieldClipboardControl.install(keywordTextField);
     
        CompositeList<String> allExtensions = new CompositeList<String>(eventList.getPublisher(),
                eventList.getReadWriteLock());
        allExtensions.addMemberList(eventList);

        immutableProgramsList = new BasicEventList<String>(eventList.getPublisher(),
                eventList.getReadWriteLock());
        allExtensions.addMemberList(immutableProgramsList);
        
                
        filterTable = new MouseableTable(new DefaultEventTableModel<String>(
                allExtensions, new FileFilterTableFormat(categoryManager, iconManager)));
        
        filterTable.setShowGrid(false, false);
        filterTable.setColumnSelectionAllowed(false);
        filterTable.setSelectionMode(0);
        TableColumn iconColumn = filterTable.getColumn(0); 
        iconColumn.setCellRenderer(new IconRenderer());
        iconColumn.setWidth(16);
        iconColumn.setMaxWidth(16);
        iconColumn.setMinWidth(16);
        
        TableColumn removeColumn = filterTable.getColumn(FileFilterTableFormat.columnNames.length-1);
        
        removeColumn.setCellRenderer(new RemoveButtonRenderer(filterTable));
        removeColumn.setCellEditor(new RemoveButtonRenderer(filterTable));
        
        okButton = new JButton(new OKDialogAction());
        addKeywordButton.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                String text = keywordTextField.getText();
                
                if(text == null || text.trim().length() == 0) {
                    return;
                }
                
                text = text.trim();
                if(text.charAt(0) != '.') {
                    text = "." + text;
                }
                
                if(!eventList.contains(text) && !immutableProgramsList.contains(text)) {
                    eventList.add(text);
                }
                
                keywordTextField.setText("");
            }
        });
        
        defaultButton = new JButton(new DefaultAction());
        
        add(new MultiLineLabel(I18n.tr("LimeWire will not show files with the following extensions in your search results"), 300), "span, wrap");
        add(keywordTextField, "gapright 10");
        add(addKeywordButton,"wrap");
        add(new JScrollPane(filterTable), "span 2, grow, wrap");
        
        add(defaultButton, "alignx left");
        add(okButton, "tag ok, alignx right");
    }
    
    @Override
    ApplyOptionResult applyOptions() {
        String[] values = eventList.toArray(new String[eventList.size()]);
        FilterSettings.BANNED_EXTENSIONS.set(values);
        BackgroundExecutorService.execute(new Runnable() {
            @Override
            public void run() {
                spamManager.adjustSpamFilters();
            }
        });
        return new ApplyOptionResult(false,true);
    }

    @Override
    boolean hasChanged() {
        List model = Arrays.asList(FilterSettings.BANNED_EXTENSIONS.get());
        String[] values = eventList.toArray(new String[eventList.size()]);
        
        return model.equals(new ArrayList<String>(Arrays.asList(values)));
    }

    private void updateItems() {
        eventList.clear();
        immutableProgramsList.clear();
    
        // Adds or removes the banned programs to list with non deletable elements to be appended at the end
        //  based on the allow programs setting.
        if (settingStateManager.getValue(LibrarySettings.ALLOW_PROGRAMS) == Boolean.FALSE) {
            for ( String extension : categoryManager.getExtensionsForCategory(Category.PROGRAM) ) {
                immutableProgramsList.add("." + extension);
            }
        }
        
        // Add all the banned extensions that are not already accounted for by 
        //  banned programs
        for ( String extension : FilterSettings.BANNED_EXTENSIONS.get() ) {
            if (!immutableProgramsList.contains(extension)) {
                eventList.add(extension);
            }
        }
    }
    
    @Override
    public void initOptions() {
        
        
        updateItems();
        settingStateManager.addSettingChangedListener(new OptionPanelStateManager.SettingChangedListener() {
            @Override
            public void settingChanged(Setting setting) {
                if (setting == LibrarySettings.ALLOW_PROGRAMS) {
                    updateItems();
                }
            }
        });
    }
    
    /**
     * Reverts the extensions not shown in search results to the default setting.
     */
    private class DefaultAction extends AbstractAction {
        public DefaultAction() {
            putValue(Action.NAME, I18n.tr("Use Default"));
            putValue(Action.SHORT_DESCRIPTION, I18n.tr("Revert to default settings"));
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            FilterSettings.BANNED_EXTENSIONS.revertToDefault();
            updateItems();
        }
    }
    
    private static class FileFilterTableFormat implements TableFormat<String> {

        private final CategoryManager categoryManager;
        private final IconManager iconManager;
        
        final int ICON_COLUMN = 0;
        final int CATEGORY_COLUMN = 2;
        final int TYPE_COLUMN = 3;
        
        static final String[] columnNames = OSUtils.isWindows() ?
                new String[] {"", I18n.tr("Extension"), I18n.tr("Category"), I18n.tr("Type"), ""} 
              : new String[] {"", I18n.tr("Extension"), I18n.tr("Category"), ""};
        
        public FileFilterTableFormat(CategoryManager cagetoryManager, IconManager iconManager) {
            this.categoryManager = cagetoryManager;
            this.iconManager = iconManager;
        }
                
        @Override
        public Object getColumnValue(String baseObject, int column) {
            
            switch (column) {
                case ICON_COLUMN :
                    return iconManager.getIconForExtension(baseObject.substring(1));
            
                case CATEGORY_COLUMN :
                    return categoryManager.getCategoryForExtension(baseObject.substring(1));
            
                case TYPE_COLUMN :
                    if (OSUtils.isWindows()) {
                        return iconManager.getIconForExtension(baseObject.substring(1)).toString();
                    } // ELSE PASS-THROUGH
                    
                default:
                    return baseObject;
            }
        }

        @Override
        public int getColumnCount() {
            return columnNames.length;
        }

        @Override
        public String getColumnName(int column) {
            return columnNames[column];
        }
    }
    
    private static class IconRenderer implements TableCellRenderer {

        private final JLabel component = new JLabel(); 
        

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            if (value == null) {
                return null;
            }
            component.setIcon((Icon) value);
            
            return component;
        }
    }
}
