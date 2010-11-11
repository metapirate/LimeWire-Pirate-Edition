package org.limewire.ui.swing.library.table;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.dnd.DropTarget;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.Comparator;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.DropMode;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.jdesktop.jxlayer.JXLayer;
import org.jdesktop.jxlayer.plaf.AbstractLayerUI;
import org.limewire.collection.glazedlists.GlazedListsFactory;
import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.inject.LazySingleton;
import org.limewire.ui.swing.images.ImageCellEditor;
import org.limewire.ui.swing.images.ImageCellRenderer;
import org.limewire.ui.swing.images.ImageList;
import org.limewire.ui.swing.library.navigator.LibraryNavItem;
import org.limewire.ui.swing.library.navigator.LibraryNavigatorPanel;
import org.limewire.ui.swing.table.TablePopupHandler;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.SwingHacks;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.SortedList;

import com.google.inject.Inject;

@LazySingleton
public class LibraryImageTable extends JPanel implements Scrollable {

    @Resource private Color backgroundColor;
    
    private final ImageList imageList;    
    private final JXLayer<JComponent> layer;
    private final ImageCellEditor imageEditor;
    
    @Inject
    public LibraryImageTable(final ImageList imageList, final ImageCellEditor imageEditor, 
            final LibraryNavigatorPanel navigatorPanel, DeletionKeyListener deletionKeyListener) {
        super(new MigLayout("insets 0 0 0 0, fill"));
        
        this.imageEditor = imageEditor;
        imageEditor.setVisible(false);
        
        GuiUtils.assignResources(this); 
        
        setBackground(backgroundColor);
        
        this.imageList = imageList;
        imageList.setDragEnabled(true);
        imageList.setDropMode(DropMode.ON);
        
        imageList.setBorder(BorderFactory.createEmptyBorder(0,7,0,7));
        JScrollPane imageScrollPane = new JScrollPane(imageList);
        imageScrollPane.setBorder(BorderFactory.createEmptyBorder(0,0,0,0));
        
        layer = new JXLayer<JComponent>(imageScrollPane, new AbstractLayerUI<JComponent>());
        layer.getGlassPane().setLayout(null);       

        imageList.addKeyListener(deletionKeyListener);

        new MouseReaction(imageList, imageEditor, imageScrollPane);
        layer.getGlassPane().add(imageEditor);
        
        // this should really be an actionlistener but point conversion from
        // editor to list index isn't working for some reason
        imageEditor.getRemoveButton().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                Point p2 = SwingUtilities.convertPoint((Component) e.getSource(), e.getPoint(),
                        imageList);
                int popupRow = imageList.locationToIndex(p2);
                if (popupRow < 0)
                    return;
                imageList.setSelectedIndex(popupRow);

                List<LocalFileItem> items = imageList.getSelectedItems();
                if (items.size() > 0) {
                    LibraryNavItem item = navigatorPanel.getSelectedNavItem();
                    item.getLocalFileList().removeFile(items.get(0).getFile());
                }
                imageEditor.setVisible(false);
            }
        });
        
        add(layer, "grow");
    }
    
    
    public void setShowButtons(boolean value) {
        imageList.getImageCellRenderer().setShowButtons(value);
        imageEditor.setShowButtons(value);
    }
    
    @Override
    public DropTarget getDropTarget() {
        return imageList.getDropTarget();
    }
    
    @Override
    public void setTransferHandler(TransferHandler newHandler) {
        imageList.setTransferHandler(newHandler);
        SwingHacks.fixDnDforKDE(imageList);
    }
    
    public ImageList getImageList() {
        return imageList;
    }
    
    public void setEventList(EventList<LocalFileItem> localFileList) {
        SortedList<LocalFileItem> sortedList = GlazedListsFactory.sortedList(localFileList, new LocationComparator());
        imageList.setModel(sortedList);
    }
    
    public void setPopupHandler(TablePopupHandler popupHandler) {
        imageList.setPopupHandler(popupHandler);
    }
    
    /**
     * Overrides getPreferredSize. getPreferredSize is used by scrollable to 
     * determine how big to make the scrollableViewPort. Uses the parent 
     * component to set the appropriate size thats currently visible. This
     * prevents the image list from growing too wide and never shrinking again.
     */
    @Override
    public Dimension getPreferredSize() {
        //ensure viewport is filled so dnd will work
        Dimension dimension = super.getPreferredSize();
        if (getParent() == null)
            return dimension;
        if (dimension.height > getParent().getSize().height){
            return new Dimension(getParent().getWidth(), dimension.height);
        } else {
            return getParent().getSize(); 
        }
    }
    
    @Override
    public Dimension getPreferredScrollableViewportSize() {
        return getPreferredSize();
    }

    @Override
    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
        return getPosition(visibleRect, orientation, direction);
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
        return false;
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        return false;
    }

    @Override
    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
        return getPosition(visibleRect, orientation, direction);
    }
    
    /**
     * Scrolls to the appropriate location based on the height of a thumbnail image.
     */
    private int getPosition(Rectangle visibleRect, int orientation, int direction) {
        int currentPosition = 0;
        if (orientation == SwingConstants.HORIZONTAL)
            currentPosition = visibleRect.x;
        else
            currentPosition = visibleRect.y;
    
        int height = imageList.getFixedCellHeight();
        
        if (direction < 0) {
            int newPosition = currentPosition - (currentPosition / height) * height;
            return (newPosition == 0) ? height : newPosition;
        } else {
            return ((currentPosition / height) + 1) * height - currentPosition;
        }
    }
    
    public static class MouseReaction implements MouseListener, MouseMotionListener {

        private final ImageList imageList;
        private final JComponent editor;
        private final JScrollPane scrollPane;
        
        public MouseReaction(ImageList imageList, JComponent editor, JScrollPane scrollPane) {
            this.imageList = imageList;      
            this.editor = editor;
            this.scrollPane = scrollPane;

            imageList.addMouseListener(this);
            imageList.addMouseMotionListener(this);
        }
        
        @Override
        public void mouseEntered(MouseEvent e) {
            update(e.getPoint());
        }

        @Override
        public void mouseExited(MouseEvent e) {
            if(!editor.getBounds().contains(e.getPoint())) {
                editor.setVisible(false);
            }
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            int index = imageList.locationToIndex(e.getPoint());
            if(!e.isPopupTrigger() && !e.isShiftDown() && !e.isControlDown() && !e.isMetaDown() && index > -1)
                imageList.setSelectedIndex(index);
        }
        @Override
        public void mousePressed(MouseEvent e) {}
        @Override
        public void mouseReleased(MouseEvent e) {}
        @Override
        public void mouseDragged(MouseEvent e) {}

        @Override
        public void mouseMoved(MouseEvent e) {
            update(e.getPoint());
        }
        
        private void update(Point point){
            if (imageList.getModel().getSize() > 0) {
                int index = imageList.locationToIndex(point);
                if (index > -1) { 
                    editor.setVisible(true);
                    Rectangle bounds = imageList.getCellBounds(index, index);
                    ImageCellRenderer renderer = imageList.getImageCellRenderer();
                    Point convertedPoint = SwingUtilities.convertPoint(imageList, new Point(bounds.x, bounds.y), scrollPane);
                    editor.setLocation(convertedPoint.x + renderer.getPaddingInsets().left, convertedPoint.y + renderer.getPaddingInsets().top);
                }
            }
        }
    }

    /** Returns all currently selected LocalFileItems. */
    public List<LocalFileItem> getSelection() {
        return imageList.getSelectedItems();
    }
    
    /**
     * Sorts images based on folder and then filename if they exist in the 
     * same folder.
     */
    private static class LocationComparator implements Comparator<LocalFileItem> {
        @Override
        public int compare(LocalFileItem item1, LocalFileItem item2) {
            String location1 = item1.getPropertyString(FilePropertyKey.LOCATION);
            String location2 = item2.getPropertyString(FilePropertyKey.LOCATION);
            
            int value = location1.compareToIgnoreCase(location2);
            if(value == 0) {
                return item1.getFileName().compareToIgnoreCase(item2.getFileName());
            } else {
                return value;
            }
        }
    }
}
