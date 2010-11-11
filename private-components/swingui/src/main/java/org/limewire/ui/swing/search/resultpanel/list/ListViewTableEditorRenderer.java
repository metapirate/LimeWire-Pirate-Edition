package org.limewire.ui.swing.search.resultpanel.list;

import static org.limewire.ui.swing.search.resultpanel.list.ListViewRowHeightRule.RowDisplayConfig.HeadingSubHeadingAndMetadata;
import static org.limewire.ui.swing.util.I18n.tr;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.AbstractCellEditor;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.event.HyperlinkEvent.EventType;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXPanel;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.ui.swing.components.HTMLLabel;
import org.limewire.ui.swing.components.IconButton;
import org.limewire.ui.swing.components.RemoteHostWidget;
import org.limewire.ui.swing.components.RemoteHostWidgetFactory;
import org.limewire.ui.swing.components.RemoteHostWidget.RemoteWidgetType;
import org.limewire.ui.swing.library.LibraryMediator;
import org.limewire.ui.swing.listener.MousePopupListener;
import org.limewire.ui.swing.nav.Navigator;
import org.limewire.ui.swing.properties.FileInfoDialogFactory;
import org.limewire.ui.swing.properties.FileInfoDialog.FileInfoType;
import org.limewire.ui.swing.search.model.BasicDownloadState;
import org.limewire.ui.swing.search.model.VisualSearchResult;
import org.limewire.ui.swing.search.resultpanel.DownloadHandler;
import org.limewire.ui.swing.search.resultpanel.ResultsTable;
import org.limewire.ui.swing.search.resultpanel.SearchHeading;
import org.limewire.ui.swing.search.resultpanel.SearchHeadingDocumentBuilder;
import org.limewire.ui.swing.search.resultpanel.SearchResultMenu;
import org.limewire.ui.swing.search.resultpanel.SearchResultMenuFactory;
import org.limewire.ui.swing.search.resultpanel.SearchResultTruncator;
import org.limewire.ui.swing.search.resultpanel.SearchResultTruncator.FontWidthResolver;
import org.limewire.ui.swing.search.resultpanel.list.ListViewRowHeightRule.PropertyMatch;
import org.limewire.ui.swing.search.resultpanel.list.ListViewRowHeightRule.RowDisplayConfig;
import org.limewire.ui.swing.search.resultpanel.list.ListViewRowHeightRule.RowDisplayResult;
import org.limewire.ui.swing.table.TransparentCellTableRenderer;
import org.limewire.ui.swing.util.CategoryIconManager;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

import ca.odell.glazedlists.swing.DefaultEventTableModel;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.assistedinject.Assisted;

/**
 * This class is responsible for rendering an individual SearchResult
 * in "List View".
 */
public class ListViewTableEditorRenderer extends AbstractCellEditor implements TableCellEditor, TableCellRenderer {

    private static final Log LOG = LogFactory.getLog(ListViewTableEditorRenderer.class);
    
    private static final int LEFT_COLUMN_WIDTH = 450;

    private final CategoryIconManager categoryIconManager;
    
    private static final String HTML = "<html>";
    private static final String CLOSING_HTML_TAG = "</html>";
    
    @Resource private Icon similarResultsIcon;
    @Resource private Color subHeadingLabelColor;
    @Resource private Color metadataLabelColor;
    @Resource private Color downloadSourceCountColor;
    @Resource private Color similarResultsBackgroundColor;
    @Resource private Color surplusRowLimitColor;
    @Resource private String headingColor;
    @Resource private Font headingFont;
    @Resource private Font subHeadingFont;
    @Resource private Font metadataFont;
    @Resource private Font downloadSourceCountFont;
    @Resource private Font surplusRowLimitFont;
    @Resource private Icon spamIcon;
    @Resource private Icon downloadingIcon;
    @Resource private Icon libraryIcon;
    @Resource private Icon warningIcon;
    
    @Resource private Icon propertiesPressedIcon;
    @Resource private Icon propertiesHoverIcon;
    @Resource private Icon propertiesIcon;
    
    @Resource private Icon propertiesSimilarShownPressedIcon;
    @Resource private Icon propertiesSimilarShownHoverIcon;
    @Resource private Icon propertiesSimilarShownIcon;
    
    @Resource private Icon similarHiddenIcon;
    @Resource private Icon similarHiddenPressedIcon;
    @Resource private Icon similarHiddenHoverIcon;
    
    @Resource private Icon similarShownIcon;
    @Resource private Icon similarShownPressedIcon;
    @Resource private Icon similarShownHoverIcon;
    
    @Resource private Icon dividerIcon;
    
    private final Provider<SearchHeadingDocumentBuilder> headingBuilder;
    private final ListViewRowHeightRule rowHeightRule;
    private final ListViewDisplayedRowsLimit displayLimit;
    private final Provider<SearchResultTruncator> truncator;
    private final HeadingFontWidthResolver headingFontWidthResolver = new HeadingFontWidthResolver();
    private final FileInfoDialogFactory fileInfoFactory;
    private RemoteHostWidget fromWidget;
    private JButton itemIconButton;
    private IconButton similarButton = new IconButton();
    private IconButton propertiesButton = new IconButton();
    //TODO: convert this to a label
    private JEditorPane heading = new JEditorPane();
    private JLabel subheadingLabel = new NoDancingHtmlLabel();
    private JLabel metadataLabel = new NoDancingHtmlLabel();
    private JLabel downloadSourceCount = new TransparentCellTableRenderer();
    private JXPanel editorComponent;

    private VisualSearchResult vsr;
    private JTable table;
    private JComponent similarResultIndentation;
    private JPanel lastRowPanel;
    private final JPanel emptyPanel = new JPanel();
    private JXPanel searchResultTextPanel;

    private JLabel lastRowMessage;

    private DownloadHandler downloadHandler;
    
    /**
     * cached width used for text truncation
     */
    private int textPanelWidth;

    private final SearchResultMenuFactory searchResultMenuFactory;
    
    @Inject
    ListViewTableEditorRenderer(
            CategoryIconManager categoryIconManager,
            RemoteHostWidgetFactory fromWidgetFactory,
        Navigator navigator, 
        final @Assisted DownloadHandler downloadHandler,
        Provider<SearchHeadingDocumentBuilder> headingBuilder,
        @Assisted ListViewRowHeightRule rowHeightRule,
        final @Assisted ListViewDisplayedRowsLimit displayLimit,
        LibraryMediator libraryMediator,
        Provider<SearchResultTruncator> truncator, FileInfoDialogFactory fileInfoFactory,
        SearchResultMenuFactory searchResultMenuFactory) {

        this.categoryIconManager = categoryIconManager;
        this.headingBuilder = headingBuilder;
        this.rowHeightRule = rowHeightRule;
        this.displayLimit = displayLimit;
        this.truncator = truncator;
        this.downloadHandler = downloadHandler;
        this.fileInfoFactory = fileInfoFactory;
        this.searchResultMenuFactory = searchResultMenuFactory;
        
        GuiUtils.assignResources(this);
        
        fromWidget = fromWidgetFactory.create(RemoteWidgetType.SEARCH_LIST);
       
        makePanel(navigator, libraryMediator);       

        setupButtons();
        
        layoutEditorComponent();
    }

    private void makePanel(Navigator navigator, LibraryMediator libraryMediator) {
        initializeComponents();
        makeIndentation();
        setupListeners(navigator, libraryMediator);

        lastRowPanel = new JPanel(new MigLayout("insets 10 30 0 0", "[]", "[]"));
        lastRowPanel.setOpaque(false);
        lastRowMessage = new JLabel();
        lastRowMessage.setFont(surplusRowLimitFont);
        lastRowMessage.setForeground(surplusRowLimitColor);
        lastRowPanel.add(lastRowMessage);
        emptyPanel.setOpaque(false);
    }

    private void setupButtons(){
        similarButton.setFocusable(false);
        similarButton.setIcon(similarHiddenIcon);
        similarButton.setPressedIcon(similarHiddenPressedIcon);
        similarButton.setRolloverIcon(similarHiddenHoverIcon);
        similarButton.setToolTipText(I18n.tr("Show Similar Files"));
        
        propertiesButton.setFocusable(false);
        propertiesButton.setIcon(propertiesIcon);
        propertiesButton.setPressedIcon(propertiesPressedIcon);
        propertiesButton.setRolloverIcon(propertiesHoverIcon);
        propertiesButton.setToolTipText(I18n.tr("View File Info"));
        
        itemIconButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (isDownloadEligible(vsr)) {
                    downloadHandler.download(vsr);
                    table.editingStopped(new ChangeEvent(table));
                }
            }
        });   
        
        similarButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                vsr.toggleChildrenVisibility();
                if(vsr.isChildrenVisible()) {
                    similarButton.setIcon(similarShownIcon);
                    similarButton.setPressedIcon(similarShownPressedIcon);
                    similarButton.setRolloverIcon(similarShownHoverIcon);
                    similarButton.setToolTipText(I18n.tr("Hide Similar Files"));
                    propertiesButton.setIcon(propertiesSimilarShownIcon);
                    propertiesButton.setPressedIcon(propertiesSimilarShownPressedIcon);
                    propertiesButton.setRolloverIcon(propertiesSimilarShownHoverIcon);
                } else {
                    similarButton.setIcon(similarHiddenIcon);
                    similarButton.setPressedIcon(similarHiddenPressedIcon);
                    similarButton.setRolloverIcon(similarHiddenHoverIcon);
                    similarButton.setToolTipText(I18n.tr("Show Similar Files"));
                    propertiesButton.setIcon(propertiesIcon);
                    propertiesButton.setPressedIcon(propertiesPressedIcon);
                    propertiesButton.setRolloverIcon(propertiesHoverIcon);
                }
                table.editingStopped(new ChangeEvent(table));
            }
        });
        
        propertiesButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JDialog dialog = fileInfoFactory.createFileInfoDialog(vsr, FileInfoType.REMOTE_FILE);
                dialog.setVisible(true);
            }
        });
    }
    
    private void layoutEditorComponent(){
        searchResultTextPanel.setLayout(new MigLayout("nogrid, ins 0 0 0 0, gap 0! 0!, novisualpadding"));
        searchResultTextPanel.setOpaque(false);
        searchResultTextPanel.add(heading, "left, shrinkprio 200, growx, wmax pref, hidemode 3, wrap");
        searchResultTextPanel.add(subheadingLabel, "left, shrinkprio 200, growx, hidemode 3, wrap");
        searchResultTextPanel.add(metadataLabel, "left, shrinkprio 200, hidemode 3");

        editorComponent.setLayout(new MigLayout("ins 0 0 0 0, gap 0! 0!, novisualpadding"));
        editorComponent.add(similarResultIndentation, "growy, hidemode 3, shrinkprio 0");
        editorComponent.add(itemIconButton, "left, aligny 50%, gapleft 4, shrinkprio 0");
        editorComponent.add(searchResultTextPanel, "left, , aligny 50%, gapleft 4, growx, shrinkprio 200, growprio 200, push");
        editorComponent.add(downloadSourceCount, "gapbottom 3, gapright 2, shrinkprio 0");
        editorComponent.add(new JLabel(dividerIcon), "shrinkprio 0");
        //TODO: better number for wmin
        editorComponent.add(fromWidget, "wmin 90, left, shrinkprio 0");
        //Tweaked the width of the icon because display gets clipped otherwise
        editorComponent.add(similarButton, "gapright 4, hidemode 0, hmax 25, wmax 27, shrinkprio 0");
        editorComponent.add(propertiesButton, "gapright 4, hmax 25, wmax 27, shrinkprio 0");
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
        boolean isSelected, boolean hasFocus, int row, int column) {

        return getTableCellEditorComponent(table, value, isSelected, row, column);
    }

    @Override
    public Component getTableCellEditorComponent(final JTable table, Object value, boolean isSelected, int row, final int col) {
        vsr = (VisualSearchResult) value;
        this.table = table;        
        editorComponent.setBackground(table.getBackground());
        
        if (value == null) {
            return emptyPanel;
        }
                
        LOG.debugf("row: {0} shouldIndent: {1}", row, vsr.getSimilarityParent() != null);
        
        if (row == displayLimit.getLastDisplayedRow()) {
            lastRowMessage.setText(tr("Not showing {0} results.  Narrow results to see more.", 
                    (displayLimit.getTotalResultsReturned() - displayLimit.getLastDisplayedRow())));
          return lastRowPanel;
        } 

        update(vsr);
        return editorComponent;
    }  

    private void update(VisualSearchResult vsr) {
        fromWidget.setPeople(vsr.getSources());

        propertiesButton.setIcon(vsr.isChildrenVisible() ? propertiesSimilarShownIcon : propertiesIcon);

        similarButton.setVisible(vsr.getSimilarResults().size() > 0);
        similarButton.setIcon(vsr.isChildrenVisible() ? similarShownIcon : similarHiddenIcon);

        similarResultIndentation.setVisible(vsr.getSimilarityParent() != null);

        itemIconButton.setIcon(getIcon(vsr));
        itemIconButton.setCursor(getIconCursor(vsr));

        RowDisplayResult result = rowHeightRule.getDisplayResult(vsr);

        setLabelVisibility(result.getConfig());

        populateHeading(result, vsr.getDownloadState());
        populateSubheading(result);
        populateMetadata(result);
    }
    
    private Icon getIcon(VisualSearchResult vsr) {
        if (vsr.isSpam()) {
            return spamIcon;
        } 
        switch (vsr.getDownloadState()) {
        case DOWNLOADING:
            return downloadingIcon;
        case DOWNLOADED:
        case LIBRARY:
            return libraryIcon;
        case REMOVED:
            return warningIcon;
        }
        return categoryIconManager.getIcon(vsr);
    }

    private Cursor getIconCursor(VisualSearchResult vsr) {
        boolean useDefaultCursor = !isDownloadEligible(vsr);
        return Cursor.getPredefinedCursor(useDefaultCursor ? Cursor.DEFAULT_CURSOR : Cursor.HAND_CURSOR);
    }
    
    private void setLabelVisibility(RowDisplayConfig config) {
        switch(config) {
        case HeadingOnly:
            subheadingLabel.setVisible(false);
            metadataLabel.setVisible(false);
            break;
        case HeadingAndSubheading:
            subheadingLabel.setVisible(true);
            metadataLabel.setVisible(false);
            break;
        case HeadingAndMetadata:
            subheadingLabel.setVisible(false);
            metadataLabel.setVisible(true);
            break;
        case HeadingSubHeadingAndMetadata:
            subheadingLabel.setVisible(true);
            metadataLabel.setVisible(true);
        }
    }
    
    private void populateHeading(final RowDisplayResult result, BasicDownloadState downloadState) {
        //the visible rect width is always 0 for renderers so getVisibleRect() won't work here
        //Width is zero the first time editorpane is rendered - use a wide default (roughly width of left column)
        int width = textPanelWidth == 0 ? LEFT_COLUMN_WIDTH : textPanelWidth;
        //Make the width seem a little smaller than usual to trigger a more hungry truncation
        //otherwise the JEditorPane word-wrapping logic kicks in and the edge word just disappears
        final int fudgeFactorPixelWidth = width - 10;
        SearchHeading searchHeading = new SearchHeading() {
            @Override
            public String getText() {
                String headingText = result.getHeading();
                String truncatedHeading = truncator.get().truncateHeading(headingText, fudgeFactorPixelWidth, headingFontWidthResolver);
                handleHeadingTooltip(headingText, truncatedHeading);
                return truncatedHeading;
            }

            /**
             * Sets a tooltip for the heading field only if the text has been truncated. 
             */
            private void handleHeadingTooltip(String headingText, String truncatedHeading) {
                String toolTipText = HTML + headingText + CLOSING_HTML_TAG;
                editorComponent.setToolTipText(toolTipText);
                heading.setToolTipText(toolTipText);
            }

            @Override
            public String getText(String adjoiningFragment) {
                int adjoiningTextPixelWidth = headingFontWidthResolver.getPixelWidth(adjoiningFragment);
                String headingText = result.getHeading();
                String truncatedHeading = truncator.get().truncateHeading(headingText, 
                        fudgeFactorPixelWidth - adjoiningTextPixelWidth, headingFontWidthResolver);
                handleHeadingTooltip(headingText, truncatedHeading);
                return truncatedHeading;
            }
        };

        this.heading.setText(headingBuilder.get().getHeadingDocument(searchHeading, downloadState, result.isSpam()));
        this.downloadSourceCount.setText(Integer.toString(vsr.getSources().size()));
    }

    private void populateSubheading(RowDisplayResult result) {
        subheadingLabel.setText(result.getSubheading());
    }

    private void populateMetadata(RowDisplayResult result) {
        metadataLabel.setText(null);
        RowDisplayConfig config = result.getConfig();
        if (config != HeadingSubHeadingAndMetadata && config != RowDisplayConfig.HeadingAndMetadata) { 
            return;
        }
        
        PropertyMatch pm = result.getMetadata();
        
        if (pm != null) {
            String html = pm.getHighlightedValue();
            // Insert the following: the key, a colon and a space after the html start tag, then the closing tags.
            html = html.replace(HTML, "").replace(CLOSING_HTML_TAG, "");
            html = HTML + pm.getKey() + ":" + html + CLOSING_HTML_TAG;
            metadataLabel.setText(html);
        }
    }    

    private boolean isDownloadEligible(VisualSearchResult vsr) {
        return !vsr.isSpam() && vsr.getDownloadState() == BasicDownloadState.NOT_STARTED;
    }
    
    private void initializeComponents() {
        searchResultTextPanel = new JXPanel(){
            @Override
            public void paint(Graphics g){
                super.paint(g);
              //the visible rect width is always 0 for renderers so we cache the width here
                textPanelWidth = getSize().width;
            }
        };
        
        editorComponent = new JXPanel();
        
        itemIconButton = new IconButton(); 
        
        heading.setContentType("text/html");
        HTMLDocument document = new HTMLDocument();
        heading.setDocument(document);
        heading.setEditable(false);
        heading.setCaretPosition(0);
        heading.setSelectionColor(HTMLLabel.TRANSPARENT_COLOR);       
        heading.setOpaque(false);
        heading.setFocusable(false);

        StyleSheet mainStyle = document.getStyleSheet();
        String rules = "body { font-family: " + headingFont.getFamily() + "; }" +
                ".title { color: " + headingColor + "; font-size: " + headingFont.getSize() + "; }" +
                "a { color: " + headingColor + "; }";
        StyleSheet newStyle = new StyleSheet();
        newStyle.addRule(rules);
        mainStyle.addStyleSheet(newStyle); 
        heading.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
        // add an empty border around the label to keep it from dancing up and down on OS X
        heading.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
        
        subheadingLabel.setForeground(subHeadingLabelColor);
        subheadingLabel.setFont(subHeadingFont);
        // add an empty border around the label to keep it from dancing up and down on OS X
        subheadingLabel.setBorder(BorderFactory.createEmptyBorder(3, 3, 1, 3));
        
        metadataLabel.setForeground(metadataLabelColor);
        metadataLabel.setFont(metadataFont);
        metadataLabel.setBorder(BorderFactory.createEmptyBorder(0, 3, 3, 3));

        downloadSourceCount.setForeground(downloadSourceCountColor);
        downloadSourceCount.setFont(downloadSourceCountFont);
    }

    private void makeIndentation() {
        similarResultIndentation = new JPanel(new BorderLayout());
        similarResultIndentation.add(new JLabel(similarResultsIcon), BorderLayout.CENTER);
        similarResultIndentation.setBackground(similarResultsBackgroundColor);
    }

    private void setupListeners(final Navigator navigator, final LibraryMediator libraryMediator) {   
        heading.addHyperlinkListener(new HyperlinkListener(){
            public void hyperlinkUpdate(HyperlinkEvent e) {
                if (EventType.ACTIVATED == e.getEventType()) {
                    if (e.getDescription().equals("#download")) {
                        downloadHandler.download(vsr);
                        table.editingStopped(new ChangeEvent(table));
                    }
                }
            }
        });
        Component[] listenerComponents = new Component[]{editorComponent, heading, subheadingLabel, metadataLabel, 
                similarResultIndentation, searchResultTextPanel, downloadSourceCount, itemIconButton};
       
        MousePopupListener popupListener = new MousePopupListener() {
            @Override
            public void handlePopupMouseEvent(MouseEvent e) {
                // Update selection if mouse is not in selected row.
                if (table.isEditing()) {
                    int editRow = table.getEditingRow();
                    if (!table.isRowSelected(editRow)) {
                        updateSelection(e);
                    }
                }
                
                // Create list of selected results.
                List<VisualSearchResult> selectedResults = new ArrayList<VisualSearchResult>();
                DefaultEventTableModel model = ((ResultsTable) table).getEventTableModel();
                int[] selectedRows = table.getSelectedRows();
                for (int row : selectedRows) {
                    Object element = model.getElementAt(row);
                    if (element instanceof VisualSearchResult) {
                        selectedResults.add((VisualSearchResult) element);
                    }
                }
                
                // If nothing selected, use current result.
                if (selectedResults.size() == 0) {
                    selectedResults.add(vsr);
                }
                
                // Display context menu.
                SearchResultMenu searchResultMenu = searchResultMenuFactory.create(downloadHandler, selectedResults, SearchResultMenu.ViewType.List);
                searchResultMenu.show(e.getComponent(), e.getX()+3, e.getY()+3);
            }
        };
    
        addMouseListener(popupListener, listenerComponents);   
        
        // Add listener to update table selection and start downloads.
        MouseListener selectionDownloadAdapter = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    updateSelection(e);
                }
            }
            
            @Override
            public void mouseClicked(MouseEvent e) {
                if (vsr != null && !vsr.isSpam() && e.getClickCount() == 2 &&
                        SwingUtilities.isLeftMouseButton(e)) {  
                    downloadHandler.download(vsr);
                }
            }
        };
        addMouseListener(selectionDownloadAdapter, listenerComponents);   
    }
    
    private void addMouseListener(MouseListener listener, Component... components) {    
        for (Component c : components) {
            c.addMouseListener(listener);
        }
    }
        
    @Override
    public boolean shouldSelectCell(EventObject anEvent) {
        return false;
    }

    @Override
    public Object getCellEditorValue() {
        return vsr;
    }    
    
    /**
     * Updates the table row selection based on the specified mouse event.
     */
    private void updateSelection(MouseEvent e) {
        if (table.isEditing()) {
            // Get cell being edited by this editor.
            int editRow = table.getEditingRow();
            int editCol = table.getEditingColumn();
            
            // Update the selection.  We also prepare the editor to apply
            // the selection colors to the current editor component.
            if (editRow > -1 && editRow < table.getRowCount()) {
                table.changeSelection(editRow, editCol, e.isControlDown(), e.isShiftDown());
                table.prepareEditor(ListViewTableEditorRenderer.this, editRow, editCol);
            }
        }
        
        // Request focus so Enter key can be handled.
        e.getComponent().requestFocusInWindow();
    }
    
    private class HeadingFontWidthResolver implements FontWidthResolver {
        private static final String EMPTY_STRING = "";
        //finds <b>foo</b> or <a href="#foo"> or {1} patterns
        //**NOTE** This does not account for all HTML sanitizing, just for HTML
        //**NOTE** that would have been added by search result display code
        private final Pattern findHTMLTagsOrReplacementTokens = Pattern.compile("([<][/]?[\\w =\"#]*[>])|([{][\\d]*[}])");
        private final Matcher matcher = findHTMLTagsOrReplacementTokens.matcher("");
        
        @Override
        public int getPixelWidth(String text) {
            FontMetrics fontMetrics;
            if (heading.getEditorKit() instanceof HTMLEditorKit) {
                // If the EditorKit is an HTMLEditorKit, then let's get the font metrics from its style sheet.
                HTMLEditorKit editorKit = (HTMLEditorKit) heading.getEditorKit();
                StyleSheet css = editorKit.getStyleSheet();
                fontMetrics = css.getFontMetrics(headingFont);
            } else { 
                // On OS X the EditorKit might be a PlainEditorKit. So, in this case
                // get a graphics context and the accompanying font metrics from the heading component
                Graphics graphics = heading.getGraphics(); 
                fontMetrics = graphics.getFontMetrics(headingFont);
            }
            
            matcher.reset(text);
            text = matcher.replaceAll(EMPTY_STRING);
            return fontMetrics.stringWidth(text);
        }
    }
    
    /**
     * A label that does not appear to dance up and down when displaying HTML in a table.
     * 
     * The text inside JLabels wraps when displaying HTML that doesn't fit on one line. Only the first line is displayed but
     * when the label is used in a table renderer or editor, it has weird mouse over behavior where the lines dance up and down.  
     * NoDancingHtmlLabel prevents this behavior.
     */
    private static class NoDancingHtmlLabel extends TransparentCellTableRenderer {
        public NoDancingHtmlLabel(){
            //prevents strange movement on mouseover
            setVerticalAlignment(JLabel.TOP);
        }
        
        @Override
        public void setText(String text){
            super.setText(text);
            //prevent our little friend from dancing up and down on mouse over
            setMaximumSize(new Dimension(Integer.MAX_VALUE, getPreferredSize().height));
        }
    }
}