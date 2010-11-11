package org.limewire.ui.swing.warnings;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileFilter;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.limewire.core.api.Category;
import org.limewire.core.api.file.CategoryManager;
import org.limewire.core.api.library.LocalFileList;
import org.limewire.core.api.library.SharedFileList;
import org.limewire.core.settings.LibrarySettings;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.components.HorizonalCheckBoxListPanel;
import org.limewire.ui.swing.components.HyperlinkButton;
import org.limewire.ui.swing.components.Line;
import org.limewire.ui.swing.components.OverlayPopupPanel;
import org.limewire.ui.swing.components.PopupHeaderBar;
import org.limewire.ui.swing.mainframe.GlobalLayeredPane;
import org.limewire.ui.swing.options.ExtensionClassificationPanel;
import org.limewire.ui.swing.settings.SwingUiSettings;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.util.FileUtils;

import com.google.common.collect.ImmutableSortedSet;
import com.google.inject.Inject;
import com.google.inject.Provider;

class LibraryWarningDialog extends OverlayPopupPanel {

    private static int HEIGHT_COLLAPSED = 185;
    private static int HEIGHT_OPEN = 240;
    
    private final CategoryManager categoryManager;
    private final LibraryFileAdder libraryFileAdder;
    private final Provider<ExtensionClassificationPanel> classificationPanelProvider;

    @Resource private Color border;
    @Resource private Font normalFont;
    @Resource private Color fontColor;
    @Resource private Icon upIcon;
    @Resource private Icon downIcon;

    
    private boolean isShared;
    private CancelAction cancelAction;
    private LocalFileList localFileList;
    private List<File> files;
    private JRadioButton recursiveButton;
    private JRadioButton nonRecursiveButton;
    private Line horizontalLine;
    private JToggleButton advancedToggleButton;
    private JTextField extensionTextField;
    private JLabel advancedLabel;
    private HorizonalCheckBoxListPanel<Category> categories;
    
    @Inject
    public LibraryWarningDialog(
            @GlobalLayeredPane JLayeredPane layeredPane,
            LibraryFileAdder libraryFileAdder,
            CategoryManager categoryManager,
            Provider<ExtensionClassificationPanel> classificationPanelProvider) {
        
        super(layeredPane);
        
        this.libraryFileAdder = libraryFileAdder;
        this.categoryManager = categoryManager;
        this.classificationPanelProvider = classificationPanelProvider;
        
        GuiUtils.assignResources(this);
        
        setLayout(new BorderLayout());
    }

    public void initialize(final LocalFileList fileList, final List<File> files) {
        this.localFileList = fileList;
        this.files = files;
        cancelAction = new CancelAction();
        
        //this must be called prior to any ui components being created
        setIsShared(fileList);
        
        PopupHeaderBar header = new PopupHeaderBar(getHeaderString(), cancelAction);
        add(header, BorderLayout.NORTH);       
        add(createBody(), BorderLayout.CENTER);
        
        repaint();
        validate();
    }
    
    private JPanel createBody() {
        JPanel p = new JPanel(new MigLayout("fill, gap 0"));
        p.setBorder(BorderFactory.createMatteBorder(0, 1, 1, 1, border));
        
        JLabel messageLabel = new JLabel(getMessageString(localFileList));
        decorateComponent(messageLabel);
        HyperlinkButton learnMoreLink = new HyperlinkButton(new AbstractAction("(" + I18n.tr("Learn More") + ")"){
            @Override
            public void actionPerformed(ActionEvent e) {
                classificationPanelProvider.get().showDialogue();
            }
        });
        
        p.add(messageLabel, "span, split 2");
        p.add(learnMoreLink, "wrap");
        
        categories = createCheckBoxes();
        
        p.add(categories, "span, alignx center, gapbottom 5, wrap");

        File folder = getFolderBeingAdded(files);
        int folderCount = getFolderCount(files);
        String recursiveText = toHTML(getRecursiveLabel(folder, folderCount));
        recursiveButton = new JRadioButton(recursiveText);
        recursiveButton.setBorder(BorderFactory.createEmptyBorder());
        String nonRecursiveText = toHTML(getNonRecursiveLabel(folder, folderCount));
        nonRecursiveButton = new JRadioButton(nonRecursiveText);
        nonRecursiveButton.setBorder(BorderFactory.createEmptyBorder());
        ButtonGroup radioButtonGroup = new ButtonGroup();
        radioButtonGroup.add(recursiveButton);
        radioButtonGroup.add(nonRecursiveButton);
        recursiveButton.setSelected(LibrarySettings.DEFAULT_RECURSIVELY_ADD_FOLDERS_OPTION.getValue());
        
        JLabel fromLabel = new JLabel(I18n.tr("From:"));
        decorateComponent(fromLabel);
        p.add(fromLabel, "span, wrap");
        p.add(recursiveButton, "span, growy, wrap, gapleft 10");
        p.add(nonRecursiveButton, "span, growy, wrap, gapleft 10");
        
        horizontalLine = Line.createHorizontalLine(border, 1);
        horizontalLine.setVisible(false);
        
        p.add(horizontalLine, "span, growx, gapleft 10, gapright 10, gaptop 10, hidemode 3, wrap");
        p.add(createAdvancedPanel(), "gaptop 5");
        
        p.add(new JButton(new OKAction()), "split, aligny bottom, tag ok");
        p.add(new JButton(cancelAction), "tag cancel, aligny bottom, wrap");
        
        return p;
    }
    
    private HorizonalCheckBoxListPanel<Category> createCheckBoxes() {
        List<Category> categoryList = Arrays.asList(Category.AUDIO, Category.VIDEO, Category.IMAGE, Category.DOCUMENT, Category.PROGRAM);
        HorizonalCheckBoxListPanel<Category> categoriesPanel = new HorizonalCheckBoxListPanel<Category>(categoryList);
        categoriesPanel.setSelected(SwingUiSettings.getDefaultSelectedCategories());
        categoriesPanel.setForeground(fontColor);
        categoriesPanel.setFont(normalFont);
        
        //disable program check box is programs is disabled in settings
        if(!LibrarySettings.ALLOW_PROGRAMS.get()) {
            categoriesPanel.getCheckBox(Category.PROGRAM).setEnabled(false);
            categoriesPanel.getCheckBox(Category.PROGRAM).setSelected(false);
        }
        //disable documents check box if documents setting is disabled and 
        //this is the public share list.
        if(isShared && localFileList instanceof SharedFileList && ((SharedFileList)localFileList).isPublic() && !LibrarySettings.ALLOW_DOCUMENT_GNUTELLA_SHARING.get()) {
            categoriesPanel.getCheckBox(Category.DOCUMENT).setEnabled(false);
            categoriesPanel.getCheckBox(Category.DOCUMENT).setSelected(false);
        }
        
        return categoriesPanel;
    }
    
    /**
     * Creates the collapsable advanced panel for adding extensions.
     */
    private JPanel createAdvancedPanel() {
        JPanel p = new JPanel(new MigLayout("fill, insets 0, gap 0, hidemode 3"));
        
        advancedToggleButton = new JToggleButton(I18n.tr("Advanced"));
        advancedToggleButton.setSelectedIcon(upIcon);
        advancedToggleButton.setIcon(downIcon);
        advancedToggleButton.setBorder(BorderFactory.createEmptyBorder());
        advancedToggleButton.setOpaque(false);
        advancedToggleButton.setContentAreaFilled(false);
        advancedToggleButton.setFocusPainted(false);
        advancedToggleButton.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                horizontalLine.setVisible(advancedToggleButton.isSelected());
                extensionTextField.setVisible(advancedToggleButton.isSelected());
                advancedLabel.setVisible(advancedToggleButton.isSelected());
                LibraryWarningDialog.this.resize();
            }
        });
        
        advancedLabel = new JLabel(I18n.tr("Include files with the following extensions(abc, xyz)"));
        decorateComponent(advancedLabel);
        advancedLabel.setVisible(false);
        extensionTextField = new JTextField();
        extensionTextField.getDocument().addDocumentListener(new DocumentListener(){
            @Override
            public void changedUpdate(DocumentEvent e) {}

            @Override
            public void insertUpdate(DocumentEvent e) {
                if(extensionTextField.getText().trim().length() > 0)
                    advancedToggleButton.setEnabled(false);
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                if(extensionTextField.getText().trim().length() == 0)
                    advancedToggleButton.setEnabled(true);
            }
        });

        decorateComponent(extensionTextField);
        extensionTextField.setVisible(false);
        
        p.add(advancedToggleButton, "span, wrap, aligny bottom");
        p.add(advancedLabel, "span, gaptop 5, wrap");
        p.add(extensionTextField, "gaptop 5, growx");
        
        return p;
    }
    
    private void decorateComponent(JComponent component) {
        component.setFont(normalFont);
        component.setForeground(fontColor);
    }
    
    private String toHTML(String text){
        return "<HTML><HEAD>" + createFontCSS(normalFont) + "</HEAD>" + text + "</HTML>";
    }
    
    private String createFontCSS(Font font) {
        return "<style TYPE=\"text/css\"><!--" +
        "body {font-family: " + font.getFamily() + ";" +
        		"font-size: " + font.getSize() + "pt;" +
        		"font-weight: lighter;}-->" +
        		"</style>";     
    }
    
    /**
     * Sets the boolean value for whether the list the files being added
     * to is shared or not.
     */
    private void setIsShared(LocalFileList fileList) {
        if(fileList instanceof SharedFileList && ( ((SharedFileList)fileList).isPublic() || 
                ((SharedFileList)fileList).getFriendIds().size() > 0)) {
            isShared = true;
        } else {
            isShared = false;
        }
    }
    
    /**
     * Returns the String to display in the header.
     */
    private String getHeaderString() {
        if(isShared) 
            return I18n.tr("Choose What to Share");
        else
            return I18n.tr("Choose What to Add");
    }
    
    /**
     * Returns the main Message in the panel.
     */
    private String getMessageString(LocalFileList fileList) {
        if(isShared) {
            if(fileList instanceof SharedFileList && ((SharedFileList)fileList).isPublic()) {
                return I18n.tr("Choose the type of files to share with the world");
            } else {
                return I18n.trn("Choose the types of files to share with {0} friend", "Choose the types of files to share with {0} friends", ((SharedFileList)fileList).getFriendIds().size());
            }
        } else {
            return I18n.tr("Add these types of files");
        }
    }
    
    /**
     * If and only if one folder was added in the drop, that 
     * file will be returned, otherwise returns null.
     */
    private File getFolderBeingAdded(List<File> files) {
        int directoryCount = 0;
        File folder = null;
        for (File file : files) {
            if (file.isDirectory()) {
                directoryCount++;
                folder = file;
                if (directoryCount > 1) {
                    // short circuit just need to know if there is more than 1
                    // null folder when more than 1 folder.
                    folder = null;
                    break;
                }
            }
        }
        return folder;
    }
    
    private int getFolderCount(List<File> files) {
        int folderCount = 0;
        for(File file : files) {
            if(file.isDirectory())
                folderCount += 1;
        }
        return folderCount;
    }
    
    /**
     * Returns the text for the recursive radio button.
     */
    private String getRecursiveLabel(File folder, int folderCount) {
        if(folder != null) 
            return I18n.tr("\"{0}\" and all its subfolders", folder.getName());
        else
            return I18n.tr("{0} folders and all their subfolders", folderCount);
    }
    
    /**
     * Returns the text for the non-recursive radio button.
     */
    private String getNonRecursiveLabel(File folder, int folderCount) {        
        if(folder != null) 
            return I18n.tr("\"{0}\" only", folder.getName());
        else
            return I18n.tr("{0} folders only", folderCount);
    }

    @Override
    public void resize() {
        Rectangle parentBounds = getParent().getBounds();
        int w = 520;
        int h = HEIGHT_COLLAPSED;
        if(advancedToggleButton != null && advancedToggleButton.isSelected()) {
            h = HEIGHT_OPEN;
        } else {
            h = HEIGHT_COLLAPSED;
        }
        setBounds((int)parentBounds.getWidth()/2-w/2,
                (int)parentBounds.getHeight()/2-h/2,
                w, h);
    }
    
    /**
     * Returns all extensions separated by spaces or comma separated.
     * It ignores any preceding "." to an extension.
     */
    private List<String> getAdvancedExtensions() {
        String text = extensionTextField.getText();
        String[] parsed = text.split(",\\s*\\.?|\\s*\\.|\\s+");
        return Arrays.asList(parsed);
    }
    
    /**
     * Accepts the drop.
     */
    private class OKAction extends AbstractAction {
        public OKAction() {
            if(isShared) {
                putValue(Action.NAME, I18n.tr("Share"));
            } else {
                putValue(Action.NAME, I18n.tr("Add"));
            }
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            List<String> includedExtensions = getAdvancedExtensions();
            libraryFileAdder.addFilesInner(localFileList, files, new Filter(categories.getSelected(), includedExtensions, recursiveButton.isSelected()));
            LibraryWarningDialog.this.dispose();
        }
    }
    
    /**
     * Disposes of the dialog and cancels the drop.
     */
    private class CancelAction extends AbstractAction {
        public CancelAction() {
            super(I18n.tr("Cancel"));
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            LibraryWarningDialog.this.dispose();
        }
    }
    
    /**
     * Filter which accepts a file if its a folder or the extension matches one of the
     * selected categories or is added to the advanced textfield.
     */
    private class Filter implements FileFilter {
        private final Set<String> extensionSet;
        private final boolean isRecursive;
  
        /** Constructs the filter with the given set of allowed extensions. */
        public Filter(Collection<Category> categories, List<String> advancedExtensions, boolean isRecursive) {
            this.isRecursive = isRecursive;
            
            ImmutableSortedSet.Builder<String> builder =
                ImmutableSortedSet.orderedBy(String.CASE_INSENSITIVE_ORDER);
            
            builder.addAll(advancedExtensions);
            for(Category category : categories) {
                builder.addAll(categoryManager.getExtensionsForCategory(category));
            }
            
            extensionSet = builder.build();
        }
  
        /**
         * Return true if the file is a directory and recursion is allowed or 
         * the file is not a directory and the extension is allowed.
         */
        @Override
        public boolean accept(File file) {
            if(file.isDirectory()) {
                return isRecursive;
            } else { 
                return extensionSet.contains(FileUtils.getFileExtension(file));
            }
        }
    }
}
