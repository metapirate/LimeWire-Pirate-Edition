package org.limewire.ui.swing.filter;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import org.jdesktop.swingx.JXList;
import org.jdesktop.swingx.decorator.ColorHighlighter;
import org.jdesktop.swingx.decorator.HighlightPredicate;
import org.limewire.ui.swing.components.RolloverCursorListener;
import org.limewire.ui.swing.filter.AbstractFilter.FilterResources;
import org.limewire.ui.swing.util.I18n;

/**
 * Display panel for filter popup window.  This is used by PropertyFilter to 
 * display the "more" popup, and SourceFilter to display the "friend" popup.
 */
class FilterPopupPanel extends JPanel {
    private static final int MAX_VISIBLE_ROWS = 18;
    
    private final JPanel titlePanel = new JPanel();
    private final JLabel titleLabel = new JLabel();
    private final JButton closeButton = new JButton();
    private final JXList list = new JXList();
    private final JScrollPane scrollPane = new JScrollPane();
    private final JPopupMenu popupMenu = new JPopupMenu();
    
    private boolean popupReady;
    private boolean popupTriggered;
    
    /**
     * Constructs a FilterPopupPanel with the specified resources and title
     * text.
     */
    public FilterPopupPanel(FilterResources resources, String titleText) {
        
        setBorder(BorderFactory.createLineBorder(resources.getPopupBorderColor(), 2));
        setLayout(new BorderLayout());
        
        titlePanel.setBackground(resources.getPopupHeaderBackground());
        titlePanel.setBorder(BorderFactory.createEmptyBorder(1, 3, 1, 1));
        titlePanel.setLayout(new BorderLayout());
        
        titleLabel.setForeground(resources.getPopupHeaderForeground());
        titleLabel.setFont(resources.getPopupHeaderFont());
        titleLabel.setText(I18n.tr("All {0}", titleText));
        
        closeButton.setBorder(BorderFactory.createEmptyBorder(0, 3, 3, 3));
        closeButton.setContentAreaFilled(false);
        closeButton.setForeground(resources.getPopupHeaderForeground());
        closeButton.setIcon(resources.getPopupCloseIcon());
        closeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                hidePopup();
            }
        });
        
        // Add listener to show cursor on mouse over.
        new RolloverCursorListener().install(closeButton);
        
        list.setFont(resources.getRowFont());
        list.setForeground(resources.getRowColor());
        list.setOpaque(false);
        list.setRolloverEnabled(true);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        // Add highlighter for rollover.
        list.setHighlighters(new ColorHighlighter(HighlightPredicate.ROLLOVER_ROW,
                resources.getHighlightBackground(), resources.getHighlightForeground()));
        
        // Add listener to show cursor on mouse over.
        new RolloverCursorListener().install(list);
        
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setViewportView(list);
        
        popupMenu.setBorder(BorderFactory.createEmptyBorder());
        popupMenu.setFocusable(false);
        popupMenu.addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {
            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                if (popupTriggered) {
                    popupReady = false;
                } else {
                    popupReady = true;
                }
            }

            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                popupTriggered = true;
            }
        });
        
        add(titlePanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        titlePanel.add(titleLabel, BorderLayout.CENTER);
        titlePanel.add(closeButton, BorderLayout.EAST);
        popupMenu.add(this);
    }
    
    /**
     * Sets the cell renderer on the list.
     */
    public void setListCellRenderer(ListCellRenderer renderer) {
        list.setCellRenderer(renderer);
    }
    
    /**
     * Sets the data model on the list.
     */
    public void setListModel(ListModel model) {
        list.setModel(model);
    }
    
    /**
     * Sets the selection model on the list.
     */
    public void setListSelectionModel(ListSelectionModel selectionModel) {
        list.setSelectionModel(selectionModel);
    }
    
    /**
     * Returns true if the popup is ready to be displayed.  The return 
     * value will be false if the popup is about to be hidden due to a 
     * triggering event.  
     */
    public boolean isPopupReady() {
        return popupReady;
    }
    
    /**
     * Sets an indicator that determines whether the popup is ready to be 
     * displayed.
     */
    public void setPopupReady(boolean popupReady) {
        this.popupReady = popupReady;
    }
    
    /**
     * Sets an indicator that determines whether a triggering event is
     * about to occur that affects popup visibility.
     */
    public void setPopupTriggered(boolean popupTriggered) {
        this.popupTriggered = popupTriggered;
    }
    
    /**
     * Displays this panel in a popup window at the x,y position relative to
     * the specified invoker.
     */
    public void showPopup(Component invoker, int x, int y) {
        // Adjust popup list height.
        list.setVisibleRowCount(Math.min(list.getModel().getSize(), MAX_VISIBLE_ROWS));
        
        // Limit popup width.
        if (popupMenu.getPreferredSize().width > 275) {
            popupMenu.setPreferredSize(new Dimension(275, popupMenu.getPreferredSize().height));
        }
        
        // Display popup next to property label.  Coordinates are relative
        // to the invoker, so we adjust the horizontal position to align
        // with the list, and the vertical position to align with the 
        // filter label.
        popupMenu.show(invoker, x, y);
    }
    
    /**
     * Hides the popup window.
     */
    public void hidePopup() {
        popupMenu.setVisible(false);
    }
}
