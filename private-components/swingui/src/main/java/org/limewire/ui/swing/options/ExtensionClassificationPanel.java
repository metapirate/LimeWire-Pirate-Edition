package org.limewire.ui.swing.options;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.swingx.JXButton;
import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.table.TableColumnExt;
import org.limewire.core.api.Category;
import org.limewire.core.api.file.CategoryManager;
import org.limewire.ui.swing.components.FocusJOptionPane;
import org.limewire.ui.swing.components.MultiLineLabel;
import org.limewire.ui.swing.components.decorators.ButtonDecorator;
import org.limewire.ui.swing.components.decorators.TableDecorator;
import org.limewire.ui.swing.options.actions.OKDialogAction;
import org.limewire.ui.swing.table.DefaultLimeTableCellRenderer;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.IconManager;
import org.limewire.util.OSUtils;

import com.google.inject.Inject;

/**
 * A panel that simply displays the current file to category associations being used.
 */
public class ExtensionClassificationPanel extends JPanel {

    private static final Category[] ALL_CATEGORIES = Category.values();
    
    private static final String TABLE_VIEW = "TABLE_VIEW";
    private static final String OTHER_VIEW = "OTHER_VIEW";
    
    private final CategoryManager categoryManager;
    private final IconManager iconManager;
    private final ButtonDecorator buttonDecorator;
    
    private final JXTable table;
    private final JPanel contentPanel;
    private final CardLayout contentSwitcher;
    
    private Category[] currentCategories = null;
    
    private final JPanel switchPanel;
    
    @Inject
    public ExtensionClassificationPanel(CategoryManager categoryManager, IconManager iconManager,
            TableDecorator tableDecorator, ButtonDecorator buttonDecorator) {

        super(new BorderLayout());
        
        this.categoryManager = categoryManager;
        this.iconManager = iconManager;
        this.buttonDecorator = buttonDecorator;
                
        setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
        setOpaque(false);
        
        add(new JLabel("<html>"+I18n.tr("Below are the file extensions LimeWire knows about and how they are classified throughout the program")+"</html>"),
            BorderLayout.NORTH);
        
        
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setOpaque(false);
        
        switchPanel = new JPanel(new MigLayout("insets 10, gap 5"));
        switchPanel.setOpaque(false);
        
        centerPanel.add(switchPanel, BorderLayout.NORTH);
        
        table = new JXTable(); 
        tableDecorator.decorate(table);        
        
        table.setShowGrid(false, false);
        table.setColumnSelectionAllowed(false);
        table.setSelectionMode(0);
        table.setDefaultRenderer(Object.class, new DefaultLimeTableCellRenderer());
        
        JScrollPane scrollPane = new JScrollPane(table, 
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, 
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setOpaque(false);
        
        contentSwitcher = new CardLayout();
        contentPanel = new JPanel(contentSwitcher);
        contentPanel.setOpaque(false);
        
        contentPanel.add(scrollPane, TABLE_VIEW);
        
        JPanel otherPanel = new JPanel(new GridBagLayout());
        otherPanel.setOpaque(false);
        
        JLabel otherLabel = new MultiLineLabel(I18n.tr("File types that LimeWire doesn't know about are " +
                "classified as \"Other\". Add these files to a List by dragging them, or " +
                "when adding a folder, click \"Advanced\" and choose specific file " +
                "extensions"), 450);
        otherLabel.setOpaque(false);
        
        otherPanel.add(otherLabel);
        
        contentPanel.add(otherPanel, OTHER_VIEW);
        
        centerPanel.add(contentPanel, BorderLayout.CENTER);
        add(centerPanel, BorderLayout.CENTER);

    }

    /**
     * Builds a table model containing the extension association information for a given
     *  set of categories. 
     */
    private TableModel createTableModel(Category... categories) {
        
        Collection<String> extensions = new HashSet<String>();
        for ( Category category : categories ) {
            extensions.addAll(categoryManager.getExtensionsForCategory(category));
        }
        
        List<String> sortedList = new ArrayList<String>(extensions);
        Collections.sort(sortedList);
        extensions = sortedList;
        
        List<String> headingList = new ArrayList<String>();
        headingList.add(""); // Icon
        headingList.add(I18n.tr("Extension"));
        if (categories.length > 1) {
            headingList.add(I18n.tr("Category"));
        }
        if (OSUtils.isWindows()) {
            headingList.add(I18n.tr("Type"));
        }
        
        Object[][] data = new Object[extensions.size()][headingList.size()];
        
        int y = 0;
        for ( String ext : extensions ) {
            int x = 0;
            
            Icon icon = iconManager.getIconForExtension(ext);
            
            data[y][x++] = icon;
            data[y][x++] = ext;
            
            if (categories.length > 1) {
                data[y][x++] = I18n.tr(categoryManager.getCategoryForExtension(ext).getSingularName());
            }
            if (OSUtils.isWindows() && icon != null) {
                if (!icon.toString().startsWith("file:/")) {
                    data[y][x++] = icon.toString();
                }
            }
               
            y++;
        }
        
        return new DefaultTableModel(data, headingList.toArray()) {
            @Override
            public Class<?> getColumnClass(int column) {
                if (column == 0) {
                    return Icon.class;
                }
                else {
                    return String.class;
                }
            }
        };
    }
    
    private class CategorySwitchAction extends AbstractAction {
        
        private final Category[] categoriesForSwitch;
        
        public CategorySwitchAction(String name, Category... categoriesForSwitch) {
            super(name);
            this.categoriesForSwitch = categoriesForSwitch;
        }
        
        public CategorySwitchAction(Category... categoriesForSwitch) {
            super(I18n.tr(categoriesForSwitch[0].getPluralName()));
            this.categoriesForSwitch = categoriesForSwitch;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            switchCategory(categoriesForSwitch);
            updateSelection((JButton)e.getSource());
        }
        
        public Category[] getCategoriesForSwitch() {
            return categoriesForSwitch;
        }
    }
    
    /**
     * Switch the visible tab to the one containing the category.
     * 
     * <p> Null category implies the all category as a shortcut if
     *      ever needed
     */
    public void switchCategory(Category category) {
        if (category == null) {
            switchCategory(ALL_CATEGORIES);
        } 
        else {
            switchCategory(new Category[] {category});
        }
        updateSelection(category);
    }
    
    private void switchCategory(Category... categories) {
        
        if (categories == currentCategories) {
            return;
        }
        
        if (categories.length == 1 && categories[0] == Category.OTHER) {
            contentSwitcher.show(contentPanel, OTHER_VIEW);
        }
        else {
            contentSwitcher.show(contentPanel, TABLE_VIEW);
        }
        
        currentCategories = categories;
        TableModel model = createTableModel(currentCategories);

        table.setModel(model);
        TableColumnExt iconColumn = table.getColumnExt(model.getColumnName(0));
        iconColumn.setResizable(false);
        iconColumn.setMinWidth(16);
        iconColumn.setMaxWidth(16);
        iconColumn.setWidth(16);
        iconColumn.setComparator(new Comparator<ImageIcon>() {
            @Override
            public int compare(ImageIcon o1, ImageIcon o2) {
                return o1.getImage().toString().compareTo(o2.getImage().toString());
            }
        });
    }
    
    private void updateSelection(JButton button) {
        button.setSelected(true);
        for ( Component comp : switchPanel.getComponents() ) {
            if (comp instanceof JButton && button != comp) {
                ((JButton)comp).setSelected(false);
            }
        }
    }
    
    private void updateSelection(Category category) {
        for ( Component comp : switchPanel.getComponents() ) {
            if (comp instanceof JButton) {
                JButton button = ((JButton)comp);
                Category[] categoriesForSwitch = ((CategorySwitchAction)button.getAction()).getCategoriesForSwitch();
                if (category == null) {
                    button.setSelected(categoriesForSwitch.length > 1);
                } else {
                    button.setSelected(categoriesForSwitch.length != 0 && categoriesForSwitch[0] == category);
                }
            }
        }
    }
    
    private JButton createSelectionButton(Action action) {
        JXButton button = new JXButton(action);
        button.setModel(new JToggleButton.ToggleButtonModel());
        buttonDecorator.decorateLinkButton(button);
        return button;
    }
    
    /**
     * Loads the panel for use.  Builds the table and lays out the component.
     */
    public void init() {
        if (currentCategories == null) { 
            switchCategory(ALL_CATEGORIES);
        }
        
        switchPanel.removeAll();
        switchPanel.invalidate();
        
        switchPanel.add(new JLabel(I18n.tr("Show:")));
        
        JButton allButton = createSelectionButton(new CategorySwitchAction(I18n.tr("All"), ALL_CATEGORIES));
        allButton.setSelected(currentCategories == ALL_CATEGORIES);
        
        switchPanel.add(allButton);
        
        for ( Category category : ALL_CATEGORIES ) {
            JButton categoryButton = createSelectionButton(new CategorySwitchAction(category));
            categoryButton.setSelected(currentCategories.length == 1 && category == currentCategories[0]);
            switchPanel.add(categoryButton);
        }
    }

    public void showDialogue() {
        init();
        JDialog dialog = FocusJOptionPane.createDialog(I18n.tr("File extension Classification"), null, this);
        
        JPanel buttonPanel = new JPanel(new MigLayout("insets 0 0 10 10, gap 4, fill"));
        buttonPanel.setOpaque(false);
        buttonPanel.add(new JButton(new OKDialogAction()), "tag ok");
        dialog.getContentPane().add(buttonPanel, BorderLayout.SOUTH);
        
        dialog.pack();
        
        table.requestFocusInWindow();
        dialog.setVisible(true);
        
    }
}
