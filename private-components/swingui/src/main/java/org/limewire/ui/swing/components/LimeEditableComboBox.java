package org.limewire.ui.swing.components;

import java.awt.Color;
import java.awt.Component;
import java.awt.Insets;
import java.awt.event.ActionListener;

import javax.swing.ComboBoxEditor;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.BasicComboPopup;
import javax.swing.plaf.basic.ComboPopup;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXPanel;
import org.jdesktop.swingx.painter.CompoundPainter;
import org.jdesktop.swingx.painter.RectanglePainter;
import org.limewire.ui.swing.painter.BorderPainter;
import org.limewire.ui.swing.painter.BorderPainter.AccentType;
import org.limewire.ui.swing.util.GuiUtils;


/**
 * Custom painted JComboBox inside JXPanel.  Provides access to ComboPopup, and text field.
 */
public class LimeEditableComboBox extends JXPanel{
    
    @Resource private int arcWidth;
    @Resource private int arcHeight;
    @Resource private Color borderColour;
    @Resource private Color bevelLeft;
    @Resource private Color bevelTop1;
    @Resource private Color bevelTop2;
    @Resource private Color bevelRight;
    @Resource private Color bevelBottom;
    
    private JTextField inputField;
    private JComboBox comboBox;
    private LimeEditableComboBoxUI comboUI;
    
    public LimeEditableComboBox(){
        GuiUtils.assignResources(this);
        
        initializeComboBox();
        initializePainter();        

        setOpaque(false);
        
        setLayout(new MigLayout("fill, ins 0 0 0 0 , gap 0! 0!, novisualpadding"));
        add(comboBox, "gapleft 6, gapright 6, aligny 50%, growx");
    }
    
    
    public BasicComboPopup getPopup(){
        return comboUI.getPopup();
    }
    
    public JTextField getTextField(){
        return inputField;
    }
    
    public JComboBox getComboBox(){
        return comboBox;
    }
     
    
    private void initializeComboBox() {
        comboBox = new JComboBox();
        
        initializeInputField();
        
        comboUI = new LimeEditableComboBoxUI();         
        comboBox.setUI(comboUI);
        
        comboBox.setEditor(new ShareComboBoxEditor());
        comboBox.setEditable(true);
        comboBox.setOpaque(false);
        comboBox.setBorder(new EmptyBorder(0, 0, 0, 0));    
    }

    //must be called after friendCombo is initialized
    private void initializeInputField() {
        inputField = new JTextField(12);
        inputField.setBorder(new EmptyBorder(0, 0, 0, 0));
        inputField.setOpaque(false);

    }
    
    private void initializePainter(){
        CompoundPainter<JXPanel> compoundPainter = new CompoundPainter<JXPanel>();

        RectanglePainter<JXPanel> painter = new RectanglePainter<JXPanel>();

        painter.setRounded(true);
        painter.setFillPaint(Color.WHITE);
        painter.setRoundWidth(this.arcWidth);
        painter.setRoundHeight(this.arcHeight);
        painter.setInsets(new Insets(2, 2, 2, 2));
        painter.setBorderPaint(null);
        painter.setFillVertical(true);
        painter.setFillHorizontal(true);
        painter.setAntialiasing(true);
        painter.setCacheable(true);
        
        compoundPainter.setPainters(painter, new BorderPainter<JXPanel>(this.arcWidth, this.arcHeight,
                borderColour,  this.bevelLeft,  this.bevelTop1,  this.bevelTop2, 
                this.bevelRight,  this.bevelBottom, AccentType.SHADOW));
        compoundPainter.setCacheable(true);
        setBackgroundPainter(compoundPainter);
    }
    
    private class ShareComboBoxEditor implements ComboBoxEditor{
        @Override
        public Component getEditorComponent() {
            return inputField;
        }

        @Override
        public void addActionListener(ActionListener l) {
            //Do nothing
        }       

        @Override
        public Object getItem() {
            //Do nothing
            return null;
        }

        @Override
        public void removeActionListener(ActionListener l) {
            //Do nothing
        }

        @Override
        public void selectAll() {
            //Do nothing
        }

        @Override
        public void setItem(Object anObject) {
            //Do nothing
        }
        
    }
    
    private static class LimeEditableComboBoxUI extends BasicComboBoxUI {
        @Resource private Icon arrowIcon;
        @Resource private Icon rolloverArrowIcon;
        @Resource private Icon downArrowIcon;
        @Resource private Color selectedBackground;
        
        private final Color selectedForeground = UIManager.getColor("MenuItem.selectionForeground");
        
        
        public LimeEditableComboBoxUI(){
            GuiUtils.assignResources(this);            
        }
        
        public BasicComboPopup getPopup(){
            return (BasicComboPopup)popup;
        }
        
        @Override 
        protected ComboPopup createPopup() {
             BasicComboPopup comboPopup = new BasicComboPopup(comboBox);
             comboPopup.getList().setSelectionBackground(selectedBackground);
             comboPopup.getList().setSelectionForeground(selectedForeground);
            return comboPopup;
        }
        
        @Override
        protected JButton createArrowButton() {
            JButton button = new IconButton(arrowIcon, rolloverArrowIcon, downArrowIcon);
            button.setName("ComboBox.arrowButton");
            return button;
        }
    }
}
