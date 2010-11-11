package org.limewire.ui.swing.transfer;

import java.awt.Component;
import java.awt.Dimension;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.border.LineBorder;
import javax.swing.table.TableCellRenderer;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.swingx.JXPanel;
import org.limewire.ui.swing.components.LimeProgressBar;
import org.limewire.ui.swing.components.decorators.ProgressBarDecorator;

/**
 * Cell renderer for the progress column in the transfer tables.
 */
public class TransferProgressRenderer extends JXPanel implements TableCellRenderer {

    protected final TransferRendererResources resources;
    private final ProgressBarDecorator progressBarDecorator;
    
    protected final LimeProgressBar progressBar;
    protected final JLabel timeLabel;
    
    /**
     * Constructs a TransferProgressRenderer.
     */
    public TransferProgressRenderer(ProgressBarDecorator progressBarDecorator) {
        super(new MigLayout("insets 0, gap 0, novisualpadding, nogrid, aligny center"));
        
        this.progressBarDecorator = progressBarDecorator;
        resources = new TransferRendererResources();
        
        progressBar = new LimeProgressBar(0, 100);
        updateColor(); 
        progressBar.setBorder(new LineBorder(resources.getProgressBarBorderColor()));
        Dimension size = new Dimension(resources.getProgressBarWidth(), resources.getProgressBarHeight());
        progressBar.setMaximumSize(size);
        progressBar.setPreferredSize(size);
        
        timeLabel = new JLabel();
        resources.decorateComponent(timeLabel);
        
        add(progressBar);
        add(timeLabel);
    }
    
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
            boolean hasFocus, int row, int column) {
        int columnWidth = table.getColumnModel().getColumn(column).getWidth();
        
        updateProgress(value, columnWidth);
        updateTime(value);
        
        return this;
    }

    /**
     * Updates the progress bar display.  Subclasses should override this
     * method to provide a meaningful display.
     */
    protected void updateProgress(Object value, int columnWidth) {
        progressBar.setValue(0);
        progressBar.setVisible(true);
    }

    public void updateColor() {
        progressBarDecorator.decoratePlain(progressBar);
    }

    /**
     * Updates the time remaining display.  By default, this is invisible.
     * Subclasses may override this method to provide a meaningful display.
     */
    protected void updateTime(Object value) {
        timeLabel.setVisible(false);
    }
}
