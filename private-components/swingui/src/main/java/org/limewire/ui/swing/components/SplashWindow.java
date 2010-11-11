package org.limewire.ui.swing.components;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Image;
import java.text.NumberFormat;
import java.util.Locale;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JWindow;

import org.limewire.ui.swing.components.decorators.ProgressBarDecorator;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.SwingUtils;

/**
 * Window that displays the splash screen.  This loads the splash screen
 * image, places it on the center of the screen, and allows dynamic
 * updating of the status text for loading the application.
 */
public class SplashWindow {
        
    /** The number format this will use to show percentages. */
    private final NumberFormat numberFormat;
    
    /** The panel that text & progressbar is drawn on. */
    private final JPanel textAndProgressPanel;
    
    /** The label that status text is written on. */
    private final JLabel textLabel;
    
    /** The progressbar. */
    private final LimeProgressBar progressBar;

    /**  Constant handle to the label that represents the splash image. */
    private final JLabel splashLabel;
    
    /** The JWindow the splash uses. */
    private final JWindow splashWindow;
    
    public SplashWindow(Image splashImage, boolean isPro, Locale locale, int steps) {
        this.splashWindow = new JWindow();
        this.splashLabel = new JLabel();
        this.textAndProgressPanel = new JPanel();
        this.numberFormat = NumberFormat.getInstance(locale);
        
        numberFormat.setMaximumIntegerDigits(3);
        numberFormat.setMaximumFractionDigits(0);
        
        textLabel = new JLabel();
        textLabel.setOpaque(false);
        textLabel.setFont(new Font("Dialog", Font.PLAIN, 12));
        textLabel.setForeground(Color.WHITE);
        
        progressBar = new LimeProgressBar();
        
        if (isPro) {
            ProgressBarDecorator.decorateStaticPro(progressBar);
        } else {
            ProgressBarDecorator.decorateStaticBasic(progressBar);
        }
        
        int width = progressBar.getPreferredSize().width;
        progressBar.setPreferredSize(new Dimension(width, 8));
        progressBar.setMinimumSize(new Dimension(width, 8));
        progressBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 8));
        progressBar.setMaximum(steps+1);
        progressBar.setMinimum(0);
        progressBar.setValue(0);
        
        textAndProgressPanel.setLayout(new BoxLayout(textAndProgressPanel, BoxLayout.Y_AXIS));
        textAndProgressPanel.setOpaque(false);
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.add(textLabel, BorderLayout.SOUTH);
        textAndProgressPanel.add(Box.createVerticalGlue());
        textAndProgressPanel.add(panel);
        textAndProgressPanel.add(Box.createVerticalStrut(2));
        textAndProgressPanel.add(progressBar);
        textAndProgressPanel.setBorder(BorderFactory.createEmptyBorder(0, 8, 8, 8));

        int imgWidth = splashImage.getWidth(null);
        if(imgWidth < 1)
            imgWidth = 1;
        int imgHeight = splashImage.getHeight(null);
        if(imgHeight < 1)
            imgHeight = 1;
        Dimension size = new Dimension(imgWidth + 2, imgHeight + 2);
        splashWindow.setSize(size);        
  
        splashLabel.setIcon(new ImageIcon(splashImage));
        splashWindow.getContentPane().add(splashLabel, BorderLayout.CENTER);

        splashWindow.setGlassPane(textAndProgressPanel);
        splashWindow.pack();
        // for some reason if you place this call before the pack, then the splash screen isn't centered correctly
        splashWindow.setLocationRelativeTo(null);
    }
    
    /**
     * Sets the Splash Window to be visible.
     */
    public void begin() {
        SwingUtils.invokeNowOrLater(new Runnable() {
            public void run() {
                splashWindow.toFront();
                splashWindow.setVisible(true);
                textAndProgressPanel.setVisible(true);
                setStatusText(I18n.tr("Loading LimeWire..."));
            }
        });
    }

    /**
     * Sets the loading status text to display in the splash 
     * screen window.
     *
     * @param text the text to display
     */
    public void setStatusText(final String text) {
        SwingUtils.invokeNowOrLater(new Runnable() {
            public void run() {
                textLabel.setText(text);
                progressBar.setValue(progressBar.getValue() + 1);
                // force a redraw so the status is shown immediately,
                // even if we're currently in the Swing thread.
                textAndProgressPanel.paintImmediately(0, 0, textAndProgressPanel.getWidth(), textAndProgressPanel.getHeight());
            }
        });
    }

    public void dispose() {
        SwingUtils.invokeNowOrLater(new Runnable() {
            public void run() {
                splashWindow.dispose();
            }
        });
    }
}

