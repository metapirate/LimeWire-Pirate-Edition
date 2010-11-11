package org.limewire.ui.swing.wizard;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.LineBorder;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXButton;
import org.jdesktop.swingx.JXPanel;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.components.HyperlinkButton;
import org.limewire.ui.swing.components.LimeJDialog;
import org.limewire.ui.swing.components.MultiLineLabel;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.ResizeUtils;

public class Wizard extends JPanel {
    
    @Resource private Dimension size;
    @Resource private Color background;
    @Resource private Color border;
    @Resource private Color titleBarBackground;
    @Resource private Color titleBarForeground;
    @Resource private Color titleBarBorder;
    @Resource private Font titleBarFont;
    @Resource private Font footerFont;
    @Resource private Color footerFontColor;
    
    private JDialog dialog;    
 
    private final JLabel titleBarLabel;
    private final JLabel headerLine1;
    private final JLabel headerLine2;
    private final JLabel footer;
    
    private int currentPage;

    private final JXButton continueButton;
    
    private Action continueAction = new AbstractAction(I18n.tr("Continue")) {
        @Override
        public void actionPerformed(ActionEvent e) {
            next();
        }
    };
    
    private JButton backButton;
    private Action backAction = new AbstractAction(I18n.tr("Go back")) {
        @Override
        public void actionPerformed(ActionEvent e) {
            back();
        }
    };
    
    private JXButton finishButton;    
    private Action finishAction = new AbstractAction(I18n.tr("Continue")) {
        @Override
        public void actionPerformed(ActionEvent e) {
            finish();
        }
    };
    
    private List<WizardPage> pageList;
    
    private JPanel mainPanel;
    private CardLayout cardLayout;
    
    public Wizard(SetupComponentDecorator decorator){
        super(new BorderLayout());
        
        GuiUtils.assignResources(this);
        
        setBackground(background);
        setBorder(new LineBorder(border, 3));
        
        pageList = new ArrayList<WizardPage>();
        
        continueButton = new JXButton(continueAction);
        decorator.decorateGreenButton(continueButton);
        
        backButton = new HyperlinkButton((String)backAction.getValue(Action.NAME), backAction);
        decorator.decorateBackButton(backButton);
        
        finishButton = new JXButton(finishAction);
        decorator.decorateGreenButton(finishButton);
        
        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);
        mainPanel.setBackground(getBackground());
        
        titleBarLabel = new JLabel();
        titleBarLabel.setOpaque(true);
        titleBarLabel.setFont(titleBarFont);
        titleBarLabel.setBackground(titleBarBackground);
        titleBarLabel.setForeground(titleBarForeground);
        titleBarLabel.setBorder(new LineBorder(titleBarBorder,3));
        
        headerLine1 = new JLabel();
        decorator.decorateHeadingText(headerLine1);
        
        headerLine2 = new MultiLineLabel();
        decorator.decorateNormalText(headerLine2);
        
        footer = new JLabel();
        footer.setOpaque(false);
        footer.setForeground(footerFontColor);
        footer.setFont(footerFont);
        footer.setBorder(BorderFactory.createEmptyBorder(0,14,0,0));
        
        JXPanel headerBar = new JXPanel(new MigLayout("insets 14, gap 0, fill"));
        decorator.decorateSetupHeader(headerBar);

        headerBar.add(titleBarLabel, "dock north, growx");
        headerBar.add(headerLine1, "growx, wrap");
        headerBar.add(headerLine2, "growx");

        JPanel bottomBar = new JPanel(new BorderLayout());
        JPanel bottomBarInner = new JPanel(new MigLayout("gap 10, insets 0 0 10 10, nogrid, hidemode 3"));
        bottomBarInner.add(backButton);
        bottomBarInner.add(continueButton);
        bottomBarInner.add(finishButton);
        bottomBar.add(bottomBarInner, BorderLayout.EAST);
        bottomBar.add(footer, BorderLayout.WEST);
        
        add(headerBar, BorderLayout.NORTH);
        add(mainPanel, BorderLayout.CENTER);
        add(bottomBar, BorderLayout.SOUTH);
    }
    
    public void addPage(WizardPage page){
        pageList.add(page);
        mainPanel.add(page, pageList.indexOf(page) + "");
    }
    
    public int getPageCount(){
        return pageList.size();
    }
    
    public void showDialog(Frame owner){
        if (getPageCount() > 0) {
            setCurrentPage(0);

            dialog = new LimeJDialog(owner, true);
            
            ResizeUtils.forceSize(dialog, size);

            // Focus must be set after the window becomes visible
            dialog.addWindowListener(new WindowAdapter() {
                @Override
                public void windowOpened(WindowEvent e) {
                    setFocus();
                }
            });
            
            dialog.setUndecorated(true);
            dialog.add(this);
            dialog.pack();
            dialog.setLocationRelativeTo(owner);
            dialog.setVisible(true);
        }
    }
    
    protected void finish() {
        for (WizardPage page : pageList) {
            page.applySettings();
        }
        if(dialog != null){
            dialog.setVisible(false);
            dialog.dispose();
            dialog = null;
        }
    }

    private void next(){
        setCurrentPage(currentPage + 1);
        setFocus();
    }
    
    private void back() {
        setCurrentPage(currentPage - 1);
        setFocus();
    }

    private void updateTitle(WizardPage page) {
        if (pageList.size() > 1) {
            titleBarLabel.setText(I18n.tr("Setup - step {0} of {1}", currentPage + 1, pageList.size()));
        } else {
            titleBarLabel.setText(I18n.tr("Setup"));
        }
        
        String line2 = page.getLine2();
        if (line2 != null) {
            headerLine1.setText(page.getLine1());
            headerLine2.setText(line2);
            headerLine1.setVisible(true);
        }
        else {
            headerLine1.setVisible(false);
            headerLine2.setText(page.getLine1());
        }
        
        footer.setText(page.getFooter());
    }
    
    public void updateControls() {
        updateForwardButton(pageList.get(currentPage));
        updateBackButton(pageList.get(currentPage));
        updateFooter(pageList.get(currentPage));
    }
    
    private void updateForwardButton(WizardPage page) {
        if (finishButton.isVisible() && !finishButton.getText().equals(page.getForwardButtonText()))
            finishButton.setText(page.getForwardButtonText());
        if (continueButton.isVisible() && !continueButton.getText().equals(page.getForwardButtonText()))
            continueButton.setText(page.getForwardButtonText());
    }
    
    private void updateBackButton(WizardPage page) {
        backButton.setVisible(currentPage == pageList.size() - 1 && page.hasBackButton());
    }
    
    private void updateFooter(WizardPage page) {
        footer.setText(page.getFooter());
    }

    public void setCurrentPage(int step){
        if(step >= 0 && step < pageList.size()) {
            currentPage = step;
            cardLayout.show(mainPanel, currentPage + "");
            finishButton.setVisible(currentPage == pageList.size() - 1);
            continueButton.setVisible(!finishButton.isVisible());
            updateTitle(pageList.get(currentPage));
            updateForwardButton(pageList.get(currentPage));
            updateBackButton(pageList.get(currentPage));
        }
    }
 
    /**
     * Sets the window focus to the visible of either the continue or finish button.
     * 
     * NOTE: This method will have no effect unless the window is visible.  It can not be
     *        called once on setCurrentPage() because this method is used before the dialogue
     *        is shown. 
     *  
     */
    private void setFocus() {
        if (finishButton.isVisible()) {
            finishButton.requestFocusInWindow();
        } else {
            continueButton.requestFocusInWindow();
        }
    }
}
