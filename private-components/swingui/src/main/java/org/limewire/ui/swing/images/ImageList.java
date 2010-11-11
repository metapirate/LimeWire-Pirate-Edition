package org.limewire.ui.swing.images;

import java.awt.Color;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JList;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXList;
import org.limewire.core.api.file.CategoryManager;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.ui.swing.components.Disposable;
import org.limewire.ui.swing.library.table.LibraryPopupMenu;
import org.limewire.ui.swing.table.TablePopupHandler;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.NativeLaunchUtils;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.ListSelection;
import ca.odell.glazedlists.swing.DefaultEventListModel;
import ca.odell.glazedlists.swing.DefaultEventSelectionModel;

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 *  Draws a list of images. Images are displayed in a horizontal left
 *  to right space before wrapping to a new line. Spaces between the
 *  images are injected with the inset values list below.
 */
public class ImageList extends JXList implements Disposable {

    @Resource
    private Color backgroundListcolor;
    
    private final ImageCellRenderer imageCellRenderer;
    private final CategoryManager categoryManager;
    
    private DefaultEventListModel cachedEventListModel;
    private DefaultEventSelectionModel<LocalFileItem> cachedEventSelectionModel;
    
    @Inject
    public ImageList(final ImageCellRenderer imageCellRenderer, 
            Provider<LibraryPopupMenu> libraryPopupMenu,
            CategoryManager categoryManager) {
        this.imageCellRenderer = imageCellRenderer;
        this.categoryManager = categoryManager;
        
        GuiUtils.assignResources(this); 
        
        setBackground(backgroundListcolor);
        setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        setLayoutOrientation(JList.HORIZONTAL_WRAP);
        //this must be set to negative 1 to get horizontal line wrap
        setVisibleRowCount(-1);

        setCellRenderer(imageCellRenderer);
        //TODO: fix this, component dimensions not beign created yet without setting this
        imageCellRenderer.setBorder(BorderFactory.createEmptyBorder(15,7,0,7));
        // add in inset size when calculated fixed cell dimensions
        // inset spacing is the white space you will see between images
        Insets insets = imageCellRenderer.getBorder().getBorderInsets(imageCellRenderer);
        setFixedCellHeight(imageCellRenderer.getHeight() + insets.top + insets.bottom);
        setFixedCellWidth(imageCellRenderer.getWidth() + insets.left + insets.right);
        
        //enable double click launching of image files.
        addMouseListener(new ImageDoubleClickMouseListener());
        setPopupHandler(new ImagePopupHandler(this, libraryPopupMenu));
    }
    
    public void setModel(EventList<LocalFileItem> eventList) {
        DefaultEventListModel newEventListModel = new DefaultEventListModel<LocalFileItem>(eventList);
        DefaultEventSelectionModel<LocalFileItem> newEventSelectionModel = new DefaultEventSelectionModel<LocalFileItem>(eventList);

        setSelectionModel(newEventSelectionModel);
        setModel(newEventListModel);
        newEventSelectionModel.setSelectionMode(ListSelection.MULTIPLE_INTERVAL_SELECTION_DEFENSIVE);
        
        if(cachedEventListModel != null) {
            cachedEventSelectionModel.dispose();
            cachedEventListModel.dispose();
        }
        
        cachedEventListModel = newEventListModel;
        cachedEventSelectionModel = newEventSelectionModel;
    }
    
    /** Returns all currently selected LocalFileItems. */
    public List<LocalFileItem> getSelectedItems() {
        return cachedEventSelectionModel.getSelected();
    }

    @Override
    public void dispose() {
        if(cachedEventListModel != null) {
            cachedEventSelectionModel.dispose();
            cachedEventListModel.dispose();
        }
    }
    
    /**
     * Sets the popup Handler for this List. 
     */
    public void setPopupHandler(final TablePopupHandler popupHandler) {
        addMouseListener(new MouseAdapter() {
            
            @Override
            public void mouseReleased(MouseEvent e) {
                maybeShowPopup(e);
            }

            @Override
            public void mousePressed(MouseEvent e) {
                maybeShowPopup(e);
            }

            private void maybeShowPopup(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    popupHandler.maybeShowPopup(e.getComponent(), e.getX(), e.getY());
                }
            }
        });
    }
    
    /**
     * Returns the CellRenderer for this list.
     */
    public ImageCellRenderer getImageCellRenderer() {
        return imageCellRenderer;
    }
    
    /**
     * This class listens for double clicks inside of the ImageList.
     * When a double click is detected, the relevant item in the list is launched.
     */
    private final class ImageDoubleClickMouseListener extends MouseAdapter {
        @Override
        public void mouseClicked(MouseEvent e) {
            if(SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2) {
                ImageList imageList = (ImageList)e.getComponent();
                int index = imageList.locationToIndex(e.getPoint());
                if(index >= 0) {
                    LocalFileItem val = (LocalFileItem) imageList.getElementAt(index);
                    File file = val.getFile();
                    NativeLaunchUtils.safeLaunchFile(file, categoryManager);
                }
            }
        }
    }
}
