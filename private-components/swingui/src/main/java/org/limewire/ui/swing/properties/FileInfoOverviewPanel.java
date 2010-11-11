package org.limewire.ui.swing.properties;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXPanel;
import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.MagnetLinkFactory;
import org.limewire.core.api.library.PropertiableFile;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.action.BitziLookupAction;
import org.limewire.ui.swing.components.HyperlinkButton;
import org.limewire.ui.swing.components.TextFieldClipboardControl;
import org.limewire.ui.swing.painter.factories.BarPainterFactory;
import org.limewire.ui.swing.properties.FileInfoDialog.FileInfoType;
import org.limewire.ui.swing.search.model.VisualSearchResult;
import org.limewire.ui.swing.search.resultpanel.CopyMagnetLinkToClipboardAction;
import org.limewire.ui.swing.util.CategoryIconManager;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.IconManager;
import org.limewire.ui.swing.util.ResizeUtils;
import org.limewire.util.CommonUtils;
import org.limewire.util.FileUtils;
import org.limewire.util.OSUtils;

import com.google.inject.Provider;

/**
 * Displays general information about a PropertiableFile.
 */
class FileInfoOverviewPanel implements FileInfoPanel {

    @Resource private Color foreground;
    @Resource private Font smallFont;
    @Resource private Font headerFont;
    @Resource private Font headerFont2;
    
    private final FileInfoType type;
    private PropertiableFile propertiableFile;
    private final MagnetLinkFactory magnetLinkFactory;
    private final CategoryIconManager categoryIconManager;
    private final LibraryManager libraryManager;
    private final RenameAction renameAction;
    
    private final JXPanel component;
    private JButton renameButton;
    private JTextField nameLabel;
    private JTextField renameTextField;
    
    public FileInfoOverviewPanel(FileInfoType type, PropertiableFile propertiableFile, 
            Provider<IconManager> iconManager, MagnetLinkFactory magnetLinkFactory, 
            CategoryIconManager categoryIconManager,
            LibraryManager libraryManager,
            BarPainterFactory barPainterFactory) {
        this.type = type;
        this.propertiableFile = propertiableFile;
        this.magnetLinkFactory = magnetLinkFactory;
        this.categoryIconManager = categoryIconManager;
        this.libraryManager = libraryManager;
        this.renameAction = new RenameAction();
        
        GuiUtils.assignResources(this);
        
        component = new JXPanel(new MigLayout("fill, gap 0, insets 10 10 16 14"));
        component.setBackgroundPainter(barPainterFactory.createPopUpBarPainter());
        
        init();
    }
    
    public JComponent getComponent() {
        return component;
    }
    
    @Override
    public boolean hasChanged() {
        return false;
    }

    @Override
    public void save() {
        // if file is currently being renamed, attempt to save the changes
        // before exiting
        if(renameTextField != null && renameTextField.isVisible())
            saveFileName();
    }
    
    @Override
    public void dispose() {
        //no listeners registered
    }
    

    @Override
    public void updatePropertiableFile(PropertiableFile file) {
        this.propertiableFile = file;
        nameLabel.setText(propertiableFile.getFileName());
    }
    
    public void enableRename() {
        renameAction.actionPerformed(null);
    }
    
    private void init() {
        component.setOpaque(false);

        addOverviewCategory();
        
        HyperlinkButton copyToClipboard = null;
        if(type == FileInfoType.LOCAL_FILE){
            if(propertiableFile instanceof LocalFileItem && ((LocalFileItem)propertiableFile).isShareable()) {
                copyToClipboard = new HyperlinkButton();
                copyToClipboard.setFont(smallFont);
                copyToClipboard.setAction(new AbstractAction(I18n.tr("Copy Magnet Link")) {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        StringSelection sel = new StringSelection(magnetLinkFactory.createMagnetLink((LocalFileItem)propertiableFile));
                        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(sel, sel);
                    }
                });
            }
        } else if(type == FileInfoType.REMOTE_FILE) {
            if(propertiableFile instanceof VisualSearchResult) {
                copyToClipboard = new HyperlinkButton();
                copyToClipboard.setFont(smallFont);
                copyToClipboard.setAction(new CopyMagnetLinkToClipboardAction((VisualSearchResult)propertiableFile));
            }
        } 
        HyperlinkButton moreFileInfo = new HyperlinkButton(new BitziLookupAction(propertiableFile));
        moreFileInfo.setFont(smallFont);
      
        if(type == FileInfoType.LOCAL_FILE && propertiableFile instanceof LocalFileItem) {
            renameButton = new HyperlinkButton(renameAction);
            renameButton.setFont(smallFont);
            component.add(renameButton, "cell 1 1, alignx right");
            
        }
        component.add(moreFileInfo, "cell 1 2, alignx right");
        if(copyToClipboard != null)
            component.add(copyToClipboard, "cell 1 3, alignx right");
    }
    

    /**
     * Loads values for the Overview Panel. If a given field is null or 
     * doesn't exist, the field is ignored.
     */
    private void addOverviewCategory() {
        Icon icon = getIcon(propertiableFile);
        JPanel iconDock = new JPanel();
        iconDock.setOpaque(false);
        iconDock.add(new JLabel(icon));
        
        String title = propertiableFile.getPropertyString(FilePropertyKey.TITLE);
        
        component.add(iconDock, "split, aligny top, growy, gapafter 0");
        if (title != null) {
            nameLabel = createLabelField(title);
        } 
        else {
            nameLabel = createLabelField(propertiableFile.getFileName());
        }
        nameLabel.setFont(headerFont);
        nameLabel.setPreferredSize(new Dimension(440, 26));
        component.add(nameLabel, "growx, span, wrap");
                
        if (title != null) {
            JTextField fileNameField = createLabelField(propertiableFile.getFileName());
            fileNameField.setFont(headerFont2);
            component.add(fileNameField, "gapbefore 4, growx, wrap");
        }
        
        String info = "";
        if (!("".equals(FileInfoUtils.getFileSize(propertiableFile)))) {
            info = FileInfoUtils.getFileSize(propertiableFile) + " (" + FileInfoUtils.getFileSizeBytes(propertiableFile)+ ")";
        }
            
        switch(propertiableFile.getCategory()) {
            case AUDIO:
                String time = FileInfoUtils.getLength(propertiableFile);
                String bitrate = propertiableFile.getPropertyString(FilePropertyKey.BITRATE);
            
                if(time != null) {
                    if (!("".equals(info))) {
                        info += ", ";
                    }

                    info += time;
                }
            
                if(bitrate != null) {
                    String quality = FileInfoUtils.getQuality(propertiableFile);
                    
                    if (!("".equals(info))) {
                        info += ", ";
                    }
                    
                    if(quality != null) {
                        info += bitrate + " kbps (" + quality + ")";
                    }
                    else {
                        info += bitrate + " kbps";
                    }
                }
                break;

        }
        
        if (!("".equals(info))) {
            component.add(createLabelField(info), "gapbefore 4, growx, wrap");
        }
    }
   
    private JTextField createLabelField(String text) {
        JTextField field = new JTextField(text);
        TextFieldClipboardControl.install(field);
        field.setCaretPosition(0);
        field.setEditable(false);
        field.setOpaque(false);
        field.setFont(smallFont);
        field.setForeground(foreground);
        field.setBorder(BorderFactory.createEmptyBorder(0,1,0,1));
        return field;
    }
    
    /**
     * Returns the appropriate Icon for the file type.
     */
    private Icon getIcon(PropertiableFile propertiableFile) {
        switch(type){
        case LOCAL_FILE:
            return categoryIconManager.getIcon(propertiableFile);
        case DOWNLOADING_FILE:
        case REMOTE_FILE:
        default:
            return categoryIconManager.getIcon(propertiableFile);
        }
    }
    
    /**
     * Creates a textfield used exclusively while renaming the file.
     */
    private void createRenameTextField() {
        renameTextField = new JTextField();
        renameTextField.setFont(headerFont);
        renameTextField.setForeground(foreground);
        renameTextField.setVisible(false);

        TextFieldClipboardControl.install(renameTextField);
        
        // close when enter key is typed
        renameTextField.addKeyListener(new KeyAdapter(){
            @Override
            public void keyTyped(KeyEvent e) {
                if(e.getKeyChar() == KeyEvent.VK_ENTER) {
                    enableRenameMode(false);
                }
            }
        });
        
        // close when mouse is clicked somewhere else
        component.addMouseListener(new MouseAdapter(){
            @Override
            public void mouseClicked(MouseEvent e) {
                enableRenameMode(false);
            }            
        });

        component.add(renameTextField, "hidemode 3, pos " + (nameLabel.getLocation().x-3) + " " + (nameLabel.getLocation().y-3));
        component.revalidate();
    }
       
    /**
     * Updates components and saves states depending on whether 
     * rename mode is activated or deactivated.
     */
    private void enableRenameMode(boolean enable) {
        if(enable) {
            setRenameText();
            setRenameEnabled(true);        
            renameTextField.requestFocusInWindow();
            renameTextField.selectAll();
        } else {
            saveFileName();
            setRenameEnabled(false);
            component.repaint();
        }
    }
    
    /**
     * Updates the text of the editable renameTextField. Once the text is updated,
     * the editable textfield is made visible, allowing the user to change the 
     * filename.
     */
    private void setRenameText() {
        if(propertiableFile instanceof LocalFileItem) {
            int maxFileLength = getMaxFileSize(((LocalFileItem)propertiableFile).getFile());
            renameTextField.setDocument(new FileNameDocument(maxFileLength));
        }
        String fileName = FileUtils.getFilenameNoExtension(nameLabel.getText());
        renameTextField.setText(fileName);
        ResizeUtils.forceWidth(renameTextField, nameLabel.getWidth());
    }
    
    
    /**
     * Updates components that change when in edit mode.
     * 
     * @isRenaming is true when the mode will be switched to
     * edit mode, false when mode is switched out of edit mode.
     */
    private void setRenameEnabled(boolean isRenaming) {
        renameTextField.setVisible(isRenaming);
        nameLabel.setVisible(!isRenaming);
        renameButton.setEnabled(!isRenaming);
    }
    
    /**
     * Attempts to save the filename changes to disk.
     */
    private void saveFileName() {
        String newFileName = renameTextField.getText().trim();
        LocalFileItem oldFileItem = (LocalFileItem) propertiableFile;
        // check the new file name is valid
        if(!isValidFileName(newFileName)) {
            renameTextField.setText(oldFileItem.getName());
            return;
        }
      
        // check if the name hasn't changed, just return
        if(newFileName.equals(oldFileItem.getName())) {
            return;
        }

        File oldFile = oldFileItem.getFile();
      
        newFileName = newFileName + "." + FileUtils.getFileExtension(oldFile);
        File newFile = new File(oldFile.getParentFile(), newFileName);
      
        // try performing the file rename, if something goes wrong, revert textfield.
        if(FileUtils.forceRename(oldFile, newFile)) {
            updateFileNameInLibrary(oldFile, newFile);
        } else {
            newFile.delete();
            renameTextField.setText(oldFileItem.getName());
        }
    }
    
    /**
     * Notifies the library that the fileName has been changed.
     */
    private void updateFileNameInLibrary(File oldFile, File newFile) {
        libraryManager.getLibraryManagedList().fileRenamed(oldFile, newFile);
    }
    
    /**
     * Returns true if the text is a valid file name.
     */
    private boolean isValidFileName(String fileName) {
        return fileName != null && fileName.length() > 0 && CommonUtils.santizeString(fileName).equals(fileName);
    }
    
    /**
     * Handles renaming of the file name. This is handled using
     * a popup menu to get the support of loosing focus and
     * properlly disappearing.
     */
    private class RenameAction extends AbstractAction {
        public RenameAction() {
            super(I18n.tr("Rename File"));
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            if(renameTextField == null)
                createRenameTextField();
            enableRenameMode(true);
        }
    }
    
    /**
     * Returns the maximum length the filename can be.
     */
    private int getMaxFileSize(File file) {
        String fileName = FileUtils.getFilenameNoExtension(file.getName());
        return OSUtils.getMaxPathLength() - file.getAbsolutePath().length() + fileName.length();
    }
    
    /**
     * Prevents a TextField from having a String beyond a certain length. Also
     * prevents non-valid filename characters from being entered.
     */
    private class FileNameDocument extends PlainDocument {
        
        private int maxLength = Integer.MAX_VALUE;
        
        public FileNameDocument(int length) {
            this.maxLength = length;
        }
        
        @Override
        public void insertString(int offset, String str, AttributeSet attr) throws BadLocationException {
            if(str == null)
                return;
            String santize = CommonUtils.santizeString(str);
            // only add the string if it contains valid characters and is below the max length
            if(santize.equals(str) && getLength() + str.length() <= maxLength) {
                super.insertString(offset, str, attr);
            } else {
                Toolkit.getDefaultToolkit().beep();
            }
        }
    }
}
