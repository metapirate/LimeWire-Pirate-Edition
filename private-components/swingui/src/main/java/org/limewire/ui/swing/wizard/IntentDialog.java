package org.limewire.ui.swing.wizard;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Paint;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Locale;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.event.HyperlinkEvent.EventType;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.swingx.JXButton;
import org.jdesktop.swingx.JXPanel;
import org.limewire.core.api.malware.VirusEngine;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.action.UrlAction;
import org.limewire.ui.swing.components.HTMLLabel;
import org.limewire.ui.swing.components.HyperlinkButton;
import org.limewire.ui.swing.components.LanguageComboBox;
import org.limewire.ui.swing.components.LimeJDialog;
import org.limewire.ui.swing.components.MultiLineLabel;
import org.limewire.ui.swing.components.NonNullJComboBox;
import org.limewire.ui.swing.painter.GenericBarPainter;
import org.limewire.ui.swing.util.FontUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.LanguageUtils;
import org.limewire.ui.swing.util.PainterUtils;
import org.limewire.ui.swing.util.ResizeUtils;
import org.limewire.util.SystemUtils;

public class IntentDialog extends LimeJDialog {

    private final VirusEngine virusEngine;
    private final Color backgroundColor = Color.WHITE;
    private final Font headingFont = new Font("Dialog", Font.BOLD, 14);
    private final Font smallFont = new Font("Dialog", Font.PLAIN, 12);
    private final Font normalFont = new Font("Dialog", Font.PLAIN, 14);
    private final Font buttonFont = new Font("Dialog", Font.BOLD, 14);
    
    private final Color headerGradientTop = new Color(0xf4f4f4);
    private final Color headerGradientBottom = new Color(0xd7d7d7);
    private final Paint headerTopBorder1 = new Color(0xffffff);
    private final Paint headerTopBorder2 = PainterUtils.TRANSPARENT; 
    private final Paint headerBottomBorder1 = new Color(0x696969);
    private final Paint headerBottomBorder2 = new Color(0xffffff);
    
    private final JLabel headingLabel;
    private final HTMLLabel bodyLabel;
    private final JLabel agreeLabel;
    private final JButton licenseButton;
    private final JButton avgLicenseButton;
    private final JButton privacyButton;
    private final JLabel languageLabel;
    private final JXButton agreeButton;
    private final JXButton exitButton;
    private final JLabel copyrightLabel;
    private final JLabel policiesLabel;
    
    private final String copyrightURL = "http://client-data.limewire.com/client_startup/docs/?page=copyright&is_client=true";
    private final String licenseURL = "http://client-data.limewire.com/client_startup/docs/?page=agreement&is_client=true";
    private final String avgLicenseURL = "http://client-data.limewire.com/client_startup/docs/?page=avg_agreement&is_client=true";
    private final String privacyURL = "http://client-data.limewire.com/client_startup/docs/?page=privacy&is_client=true";
    
    private boolean agreed = false;
    
    public IntentDialog(String version, VirusEngine virusEngine){
        super();
        this.virusEngine = virusEngine;
        ResizeUtils.forceSize(this, new Dimension(514,402));
              
        final String title = "LimeWire " + version; 
        
        setTitle(title);
        setModal(true);
        setResizable(false);
        setAlwaysOnTop(true);
       
        JPanel contentPane = new JPanel(new BorderLayout());
        contentPane.setBackground(backgroundColor);
        add(contentPane);
        JPanel panel = new JPanel(new MigLayout("nogrid, insets 0, gap 0"));
        panel.setOpaque(false);
        
        headingLabel = new JLabel();
        headingLabel.setFont(headingFont);
        
        copyrightLabel = new JLabel();
        copyrightLabel.setFont(headingFont);
                
        policiesLabel = new JLabel();
        policiesLabel.setFont(headingFont);
        
        final JComboBox languageDropDown = createLanguageDropDown(normalFont);
        
        final UrlAction.GetParamAppender languageAppender = new UrlAction.GetParamAppender() {
            @Override
            public String appendParams(String original) {
                // TODO: should language be encoded like LimeWireUtils.addLWInfoToUrl() ??
                return original + "&lang=" + languageDropDown.getSelectedItem().toString();
            }
        };
        
        agreeLabel = new MultiLineLabel("", 500);
        agreeLabel.setFont(smallFont);
        //FontUtils.bold(agreeLabel);
        licenseButton = new HyperlinkButton(new UrlAction(licenseURL, title, languageAppender));
        licenseButton.setFocusPainted(false);
        licenseButton.setFont(smallFont);
        FontUtils.underline(licenseButton);
        licenseButton.setForeground(new Color(0x2152a6));
        privacyButton = new HyperlinkButton(new UrlAction(privacyURL, title, languageAppender));
        privacyButton.setFocusPainted(false);
        privacyButton.setFont(smallFont);
        FontUtils.underline(privacyButton);
        privacyButton.setForeground(new Color(0x2152a6));
        
        avgLicenseButton = new HyperlinkButton(new UrlAction(avgLicenseURL, title, languageAppender));
        avgLicenseButton.setFocusPainted(false);
        avgLicenseButton.setFont(smallFont);
        FontUtils.underline(avgLicenseButton);
        avgLicenseButton.setForeground(new Color(0x2152a6));
        
        bodyLabel = new HTMLLabel("");
        bodyLabel.setHtmlFont(smallFont);
        bodyLabel.setHtmlLinkForeground(new Color(0x2152a6));
        bodyLabel.setEditable(false);
        bodyLabel.setOpaque(false);
        bodyLabel.addHyperlinkListener(new HyperlinkListener() {
            private final Action urlAction = new UrlAction(copyrightURL, title, languageAppender);
            @Override
            public void hyperlinkUpdate(HyperlinkEvent e) {
                if (e.getEventType() == EventType.ACTIVATED) {
                    urlAction.actionPerformed(null);
                }
            }
            
        });
        
        languageLabel = new JLabel();
        languageLabel.setFont(smallFont);
        agreeButton = new JXButton();
        decorateButton(agreeButton);
        agreeButton.setBackgroundPainter(new IntentGreenButtonBackgroundPainter());
        exitButton = new JXButton();
        decorateButton(exitButton);
        exitButton.setBackgroundPainter(new IntentLightButtonBackgroundPainter());

        JPanel bottomPanel = new JPanel(new BorderLayout());
        JPanel outerPanel = new JPanel(new BorderLayout());
        JPanel innerPanel = new JPanel(new MigLayout("gap 6, insets 0, fill"));
        JPanel langInnerPanel = new JPanel(new MigLayout("flowy, gap 4, insets 0, fill"));
        
        bottomPanel.setOpaque(false);
        outerPanel.setOpaque(false);
        innerPanel.setOpaque(false);
        langInnerPanel.setOpaque(false);
        
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(0,14,14,14));
        
        JXPanel headerBar = new JXPanel(new MigLayout("insets 14, gap 0, fill"));
        headerBar.setBackgroundPainter(new GenericBarPainter<JXPanel>(
                new GradientPaint(0,0, headerGradientTop, 0,1, headerGradientBottom, false),
                headerTopBorder1, headerTopBorder2, headerBottomBorder1, headerBottomBorder2));
        
        int indent = 14;
        headerBar.add(headingLabel, "grow, wrap");
        panel.add(copyrightLabel, "gapleft " + indent + ", gaptop 10, wrap");
        panel.add(bodyLabel, "gapleft " + (indent + indent) + ", gaptop 10, wrap");
        panel.add(policiesLabel, "gapleft " + indent + ", gaptop 20, wrap");
        panel.add(licenseButton, "gaptop 10, gapleft " + (indent + indent) +  ", wrap");
        panel.add(privacyButton, "gapleft " + (indent + indent) +  ", wrap");
        
        if(virusEngine.isSupported()) {
            panel.add(avgLicenseButton, "gapleft " + (indent + indent) +  ", wrap");
        }
        
        panel.add(agreeLabel, "gapleft " + indent +  ", gaptop 30, wrap");

        langInnerPanel.add(languageLabel);
        langInnerPanel.add(languageDropDown);
        bottomPanel.add(langInnerPanel, BorderLayout.WEST);
        
        innerPanel.add(agreeButton);
        innerPanel.add(exitButton);
        outerPanel.add(innerPanel, BorderLayout.SOUTH);
        bottomPanel.add(outerPanel, BorderLayout.EAST);
        
        setTextContents();
        
        contentPane.add(headerBar, BorderLayout.NORTH);
        contentPane.add(panel, BorderLayout.CENTER);
        contentPane.add(bottomPanel, BorderLayout.SOUTH);
        
        pack();        
    }
    
    /**
     * Helper method to prep (size/unskin) the agree and exit buttons for painting
     */
    private void decorateButton(JXButton button) {
        button.setFont(buttonFont);
        button.setOpaque(false);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(0,10,3,10));
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        button.setMinimumSize(new Dimension(105, 32));
    }
    
    
    @Override
    public void addNotify() {
        super.addNotify();
        setAlwaysOnTop(true);
        SystemUtils.setWindowTopMost(this);
    }
    
    private void finish(boolean agreed){
        this.agreed = agreed;
        setVisible(false);
        dispose();
    }
    
    public boolean confirmLegal(){
        setLocationRelativeTo(null);
        setVisible(true);
        return agreed;
    }
    
    /**
     * Creates the language combo box, attempts to guess an appropriate *available* locale to select,
     *  and sets the application locale to that match.
     */
    private JComboBox createLanguageDropDown(Font normalFont) {
        final JComboBox languageDropDown = new NonNullJComboBox();
        Locale[] locales = LanguageUtils.getLocales(normalFont);
        languageDropDown.setRenderer(new LanguageComboBox.LocaleRenderer());
        languageDropDown.setFont(smallFont);
        languageDropDown.setModel(new DefaultComboBoxModel(locales));
        
        // Attempt to guess the default locale and set accordingly
        languageDropDown.setSelectedItem(LanguageUtils.guessBestAvailableLocale(locales));
        
        // Make sure the drop down and the set locale match.  This may cause the default OS
        //  language to be overridden to English in the case of a bad guess.  This has always been
        //  a problem but in this case at least it will be obvious that the users language is being
        //  overridden.
        LanguageUtils.setLocale((Locale)languageDropDown.getSelectedItem());
                
        languageDropDown.addItemListener(new ItemListener() {

            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    Locale locale = (Locale) languageDropDown.getSelectedItem();
                    LanguageUtils.setLocale(locale);
                    setTextContents();
                }
            }
            
        });

        return languageDropDown;
    }
    
    /**
     * Allows a soft localised refresh of the text within the panel based
     *  on the language selected in the combo box.
     */
    private void setTextContents() {
        //TODO validate new text changes with legal, and nathan.
        String heading  = I18n.tr("Some Legal Stuff");
        String bodyText1
        = I18n.tr("<html><body>LimeWire Basic and LimeWire PRO are peer-to-peer programs for sharing authorized files only. Copyright laws may forbid obtaining or distributing certain copyrighted content. Learn more information about <a href=\"{0}\">Copyright</a></body></html>.", 0);
        String copyInfringementText = I18n.tr("Copyright Infringement");
        String privacyText = I18n.tr("LimeWire Privacy Policy");
        String licenseText = I18n.tr("LimeWire License Agreement");
        String avgLicenseText = I18n.tr("AVG Anti-Virus License Agreement");
        String agreementText = virusEngine.isSupported() ? I18n.tr("By clicking \"I Agree\", you agree that you have read, understand and assent to the terms of the LimeWire and AVG Anti-Virus License Agreements and LimeWire Privacy Policy. You also agree that you will not use LimeWire for copyright infringement.") : I18n.tr("By clicking \"I Agree\", you agree that you have read, understand and assent to the terms of the LimeWire License Agreement and LimeWire Privacy Policy. You also agree that you will not use LimeWire for copyright infringement.");
        String languageText = I18n.tr("Choose your language");
        String policiesText = I18n.tr("Policies Governing Your Use:");
        
        Action exitAction = new AbstractAction(I18n.tr("Exit")){
            @Override
            public void actionPerformed(ActionEvent e) {
                finish(false);
            }
        };
        
        Action agreeAction = new AbstractAction(I18n.tr("I Agree")){
            @Override
            public void actionPerformed(ActionEvent e) {
                finish(true);
            }
        };    
        
        headingLabel.setText(heading);
        bodyLabel.setText(bodyText1);
        copyrightLabel.setText(copyInfringementText);
        policiesLabel.setText(policiesText);
        licenseButton.setText(licenseText);
        avgLicenseButton.setText(avgLicenseText);
        privacyButton.setText(privacyText);
        agreeLabel.setText(agreementText);
        languageLabel.setText(languageText);
        exitButton.setAction(exitAction);
        agreeButton.setAction(agreeAction);
    }
}
