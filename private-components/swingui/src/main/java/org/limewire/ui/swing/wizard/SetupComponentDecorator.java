package org.limewire.ui.swing.wizard;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JRadioButton;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXButton;
import org.jdesktop.swingx.JXPanel;
import org.limewire.ui.swing.components.decorators.ButtonDecorator;
import org.limewire.ui.swing.painter.GenericBarPainter;
import org.limewire.ui.swing.painter.GreenButtonBackgroundPainter;
import org.limewire.ui.swing.painter.LightButtonBackgroundPainter;
import org.limewire.ui.swing.painter.BorderPainter.AccentType;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.PainterUtils;

import com.google.inject.Inject;

/**
 * A non singleton decorator class for the special components
 *  used during the setup wizard.
 */
public class SetupComponentDecorator {
    
    private final ButtonDecorator plainButtonDecorator;
    
    @Resource private Font headingFont;
    @Resource private Color headingFontColor;
    @Resource private Font normalFont;
    @Resource private Color normalFontColor;
    @Resource private Font  subHeadingFont;
    @Resource private Color subHeadingFontColor;
    @Resource private Font  linkFont;

    @Resource private Color greenButtonForeground;
    @Resource private Font greenButtonFont;
    
    @Resource private Font backButtonFont;
    
    @Resource private Icon largeBox;
    @Resource private Icon largeBoxChecked;
    @Resource private Icon largeRadio;
    @Resource private Icon largeRadioChecked;
   
    @Resource private Color headerGradientTop;
    @Resource private Color headerGradientBottom;
    @Resource private Color headerTopBorder1 = PainterUtils.TRANSPARENT;
    @Resource private Color headerTopBorder2 = PainterUtils.TRANSPARENT;
    @Resource private Color headerBottomBorder1 = PainterUtils.TRANSPARENT;
    @Resource private Color headerBottomBorder2 = PainterUtils.TRANSPARENT;
    
    private final GenericBarPainter<JXPanel> pooledBarPainter;
    
    @Inject
    SetupComponentDecorator(ButtonDecorator plainButtonDecorator) {
        
        GuiUtils.assignResources(this);
        
        this.plainButtonDecorator = plainButtonDecorator;
        
        pooledBarPainter = new GenericBarPainter<JXPanel>(
                new GradientPaint(0,0, headerGradientTop, 0,1, headerGradientBottom, false),
                headerTopBorder1, headerTopBorder2, headerBottomBorder1, headerBottomBorder2);
    }
    
    public void decorateLargeCheckBox(JCheckBox box) {
        box.setIcon(largeBox);
        box.setSelectedIcon(largeBoxChecked);
        box.setOpaque(false);
        box.setFocusPainted(false);
    }
    
    public void decorateLargeRadioButton(JRadioButton box) {
        box.setIcon(largeRadio);
        box.setSelectedIcon(largeRadioChecked);
        box.setOpaque(false);
        box.setFocusPainted(false);
    }
    
    public void decorateGreenButton(JXButton button) {
        button.setBackgroundPainter(new GreenButtonBackgroundPainter());
        button.setForeground(greenButtonForeground);
        button.setFont(greenButtonFont);
        button.setOpaque(false);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(0,10,3,10));
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        button.setMinimumSize(new Dimension(105, 32));
    }
    
    public void decorateGreyButton(JXButton button) {
        button.setBackgroundPainter(new LightButtonBackgroundPainter());
        button.setOpaque(false);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(3,15,4,15));
    }

    public void decorateBackButton(JComponent button) {
        button.setFont(backButtonFont);
        button.setBorder(BorderFactory.createEmptyBorder(0,10,3,10));
    }
    
    public void decorateLink(JComponent link) {
        link.setOpaque(false);
        link.setFont(linkFont);
    }
    
    public void decoratePlainButton(JXButton button) {
        plainButtonDecorator.decorateDarkFullButton(button, AccentType.NONE);
    }
    
    public void decorateSetupHeader(JXPanel header) {
        header.setBackgroundPainter(pooledBarPainter);
        header.setMaximumSize(new Dimension(Integer.MAX_VALUE, (int)header.getMaximumSize().getHeight()));
    }
    
    public void decorateHeadingText(JComponent component) {
        component.setFont(headingFont);
        component.setForeground(headingFontColor);
        component.setOpaque(false);
    }
    
    public void decorateNormalText(JComponent component) {
        component.setFont(normalFont);
        component.setForeground(normalFontColor);
        component.setOpaque(false);
    }
    
    public void decorateSubHeading(JComponent component) {
        component.setFont(subHeadingFont);
        component.setForeground(subHeadingFontColor);
        component.setOpaque(false);
    }
}
