package org.limewire.ui.swing.util;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.lang.reflect.Field;
import java.math.RoundingMode;
import java.net.URL;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.Date;
import java.util.Locale;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.KeyStroke;
import javax.swing.LookAndFeel;
import javax.swing.SwingUtilities;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.plaf.ComponentUI;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jdesktop.application.Application;
import org.jdesktop.application.SingleFrameApplication;
import org.limewire.i18n.I18nMarker;
import org.limewire.ui.swing.mainframe.AppFrame;
import org.limewire.util.OSUtils;



public class GuiUtils {

    private static final Log LOG = LogFactory.getLog(GuiUtils.class);
    
    /**
     * Localizable Number Format constant for the current default locale
     * set at init time.
     */
    private static NumberFormat NUMBER_FORMAT0; // localized "#,##0"
    private static NumberFormat NUMBER_FORMAT1; // localized "#,##0.0"
    
    /**
     * Localizable Date Format constant for the current default locale
     * set at init time.
     */
    private static DateFormat DATETIME_FORMAT;
    private static DateFormat DATE_FORMAT;
      
    private interface Unit {
        public String getFormatString();
        public long getMagnitude();
        public long getUnitValue();
    }
    
    /**
     * Shortcut for accessing the KB/s format string.  Should be avoided.
     */
    public static String KBPERSEC_FORMAT = SpeedUnit.KBSEC.getFormatString();
    
    private enum SizeUnit implements Unit {
        BYTES(I18nMarker.marktr("{0} bytes"), 1024, 1), // TODO: trn???
        KILOBYTES(I18nMarker.marktr("{0} KB"), 0x100000, 0x400),
        MEGABYTES(I18nMarker.marktr("{0} MB"), 0x40000000L, 0x100000),
        GIGABYTES(I18nMarker.marktr("{0} GB"), 0x10000000000L, 0x40000000),
        TERABYTES(I18nMarker.marktr("{0} TB"), Long.MAX_VALUE, 0x10000000000L);

        private final String text;
        private final long magnitude;
        private final long unitValue;
        
        private SizeUnit(String text, long magnitude, long unitValue) {
            this.text = text;
            this.magnitude = magnitude;
            this.unitValue = unitValue;
        }

        @Override
        public String getFormatString() {
            return text;
        }
        
        @Override
        public long getMagnitude() {
            return magnitude;
        }

        @Override 
        public long getUnitValue() {
            return unitValue;
        }
    }
    
    private enum SpeedUnit implements Unit {
        BPSEC(I18nMarker.marktr("{0} B/s"), 1024, 1), 
        KBSEC(I18nMarker.marktr("{0} KB/s"), 0x100000, 0x400),
        MBSEC(I18nMarker.marktr("{0} MB/s"), 0x40000000L, 0x100000),
        GBSEC(I18nMarker.marktr("{0} GB/s"), 0x10000000000L, 0x40000000),
        TBSEC(I18nMarker.marktr("{0} TB/s"), Long.MAX_VALUE, 0x10000000000L); // Oh yes, maybe one day...

        private final String text;
        private final long magnitude;
        private final long unitValue;
        
        private SpeedUnit(String text, long magnitude, long unitValue) {
            this.text = text;
            this.magnitude = magnitude;
            this.unitValue = unitValue;
        }

        @Override
        public String getFormatString() {
            return text;
        }
        
        @Override
        public long getMagnitude() {
            return magnitude;
        }

        @Override 
        public long getUnitValue() {
            return unitValue;
        }
    }
    
    static {       
        setLocale(Locale.getDefault());
    }
    
    static void setLocale(Locale locale) {
        NUMBER_FORMAT0 = NumberFormat.getNumberInstance(locale);
        NUMBER_FORMAT0.setMaximumFractionDigits(1);
        NUMBER_FORMAT0.setMinimumFractionDigits(0);
        NUMBER_FORMAT0.setGroupingUsed(true);
        NUMBER_FORMAT0.setRoundingMode(RoundingMode.HALF_UP);
        
        NUMBER_FORMAT1 = NumberFormat.getNumberInstance(locale);
        NUMBER_FORMAT1.setMaximumFractionDigits(2);
        NUMBER_FORMAT1.setMinimumFractionDigits(2);
        NUMBER_FORMAT1.setGroupingUsed(true);
        NUMBER_FORMAT1.setRoundingMode(RoundingMode.HALF_UP);

        DATETIME_FORMAT = DateFormat.getDateTimeInstance(DateFormat.DEFAULT, 
                DateFormat.DEFAULT, locale);
        DATE_FORMAT = DateFormat.getDateInstance(DateFormat.SHORT, locale);
    }
    
    /**
     * This static method converts the passed in number
     * into a localizable representation of an integer, with
     * digit grouping using locale dependant separators.
     *
     * @param value the number to convert to a numeric String
     *
     * @return a localized String representing the integer value
     */
    public static String toLocalizedInteger(long value) {
        return NUMBER_FORMAT0.format(value);
    }
    
    /**
     * This static method converts the passed in number of bytes into a
     * kilobyte string grouping digits with locale-dependant thousand separator
     * and with "KB" locale-dependant unit at the end.
     *
     * @param bytes the number of bytes to convert to a kilobyte String
     *
     * @return a String representing the number of kilobytes that the
     *         <code>bytes</code> argument evaluates to, with "KB" appended
     *         at the end.  If the input value is negative, the string
     *         returned will be "? KB"
     */
    public static String toKilobytes(long bytes) {
        if (bytes < 0)
            return I18n.tr(SizeUnit.KILOBYTES.getFormatString(), "?");
        long kbytes = bytes / 1024;
         // round to nearest multiple, or round up if size below 1024
        if ((bytes & 512) != 0 || (bytes > 0 && bytes < 1024)) kbytes++;
        // result formating, according to the current locale
        return I18n.tr(SizeUnit.KILOBYTES.getFormatString(), NUMBER_FORMAT0.format(kbytes));
    }
    
    /**
     * Converts the passed in number of bytes into a byte-size string.
     * Group digits with locale-dependant thousand separator if needed, but
     * with "B", or "KB", or "MB" or "GB" or "TB" locale-dependant unit at the end,
     * and a limited precision of 4 significant digits. 
     * 
     *
     * @param bytes the number of bytes to convert to a size String
     * @return a String representing the number of kilobytes that the
     *         <code>bytes</code> argument evaluates to, with
     *         "B"/"KB"/"MB"/"GB"/TB" appended at the end. If the input value is
     *         negative, the string returned will be "? KB".
     */
    public static String formatUnitFromBytes(long bytes) {
        return toUnit(bytes, SizeUnit.values());
    }
    
    private static boolean isInRange(long value, Unit unit) {
        return value < unit.getMagnitude();
    }
    
    private static String toUnit(long value, Unit[] unitSet) {
        if (value < 0) {
            return I18n.tr(SizeUnit.KILOBYTES.getFormatString(), "?");
        }
        
        long   unitValue = 1; // the multiple associated with the unit
        String unitName = "?";  // one of localizable units
        
        for ( Unit unit : unitSet ) {
            if (isInRange(value, unit)) {
                unitValue = unit.getUnitValue();
                unitName = unit.getFormatString();
                break;
            }
        }
        
        NumberFormat numberFormat; // one of localizable formats
        
        if(value <  0x100000) {
            numberFormat = NUMBER_FORMAT0;
        } else {
            // return a minimum "100.0xB", and maximum "999.9xB"
            numberFormat = NUMBER_FORMAT1; // localized "#,##0.00"
        }
        
        try {
            return I18n.tr(unitName, numberFormat.format((double)value / unitValue));
        } catch(ArithmeticException ae) {
            return I18n.tr(unitName, 0);
            // internal java error, just return 0.
        }
    }
    /**
     * Converts the passed in number of bytes into a byte-size string.
     * The returned String is always a locale-dependant thousand separated
     * String of bytes.
     */
    public static String formatBytes(long bytes) {
        if (bytes < 0) {
            return I18n.tr(SizeUnit.BYTES.getFormatString(), "?");
        }
        
        NumberFormat numberFormat = NUMBER_FORMAT0;
        try {
            return I18n.tr(SizeUnit.BYTES.getFormatString(), numberFormat.format(bytes));
        } catch(ArithmeticException ae) {
            return I18n.tr(SizeUnit.BYTES.getFormatString(), 0);
            // internal java error, just return 0.
        }
    }
    
    /**
     * Converts an rate into a human readable and localized KB/s speed.
     */
    public static String formatKilobytesPerSec(double rate) {
        return I18n.tr(SpeedUnit.KBSEC.getFormatString(), NUMBER_FORMAT0.format(rate));
    }
    
    public static String formatUnitFromBytesPerSec(long bytesPerSec) {
        return toUnit(bytesPerSec, SpeedUnit.values());
    }
    
    /**
     * Converts number of milliseconds since way back when to a
     * local-formatted date String.
     */
    public static String msec2DateTime(long milliseconds) {
        Date d = new Date(milliseconds);
        return DATETIME_FORMAT.format(d);
    }
    
    public static String msec2Date(long milliseconds) {
        Date date = new Date(milliseconds);
        return DATE_FORMAT.format(date);
    }
    
    public static String date2String(Date date) {
        return DATE_FORMAT.format(date);
    }
    
    /**
     * Returns the application's default frame.
     */
    public static JFrame getMainFrame() {
        if(AppFrame.isStarted()) {
            Application app = Application.getInstance();
            if(app instanceof SingleFrameApplication) {
                return ((SingleFrameApplication)app).getMainFrame();
            } else {
                return null;
            }
        } else {
            return null;
        }
    }
    
    /**
     * Returns the window frame containing the specified component.  If the
     * component has not been added to a window frame, then the main GUI 
     * frame is returned.
     * @param component the UI component being examined
     * @return window frame containing the component
     */
    public static Frame getParentFrame(Component component) {
        // Access top level ancestor for Swing component.
        if (component instanceof JComponent) {
            Container ancestor = ((JComponent) component).getTopLevelAncestor();
            if (ancestor instanceof Frame) {
                return (Frame) ancestor;
            }
        }

        // Return main frame if parent frame cannot be determined.
        return GuiUtils.getMainFrame();
    }
    
    /**
     * Inject fields from AppFrame.properties into object. Fields to be injected
     * should be annotated <code>@Resource</code> and defined in AppFrame.properties as
     * <code>ClassNameWithoutPackage.variableName=resource</code>
     * 
     * @param object the object whose fields will be injected
     */
    public static void assignResources(Object object) {

        Application.getInstance().getContext().getResourceMap(AppFrame.class)
            .injectFields(object);
    }
    
    /**
     * Convert a color object to a hex string.
     */
    public static String colorToHex(Color colorCode){
        int r = colorCode.getRed();
        int g = colorCode.getGreen();
        int b = colorCode.getBlue();
        
        return toHex(r) + toHex(g) + toHex(b);   
    }   

    
    /** Returns the int as a hex string. */
    public static String toHex(int i) {
        String hex = Integer.toHexString(i).toUpperCase(Locale.US);
        if (hex.length() == 1)
            return "0" + hex;
        else
            return hex;
    }
    
    /**
     * Updates the component to use the native UI resource.
     */
    public static ComponentUI getNativeUI(JComponent c) {
        ComponentUI ret = null;
        String name = UIManager.getSystemLookAndFeelClassName();
        if (name != null) {
            try {
                Class clazz = Class.forName(name);
                LookAndFeel lf = (LookAndFeel) clazz.newInstance();
                lf.initialize();
                UIDefaults def = lf.getDefaults();
                ret = def.getUI(c);
            } catch (ExceptionInInitializerError e) {
            } catch (ClassNotFoundException e) {
            } catch (LinkageError e) {
            } catch (IllegalAccessException e) {
            } catch (InstantiationException e) {
            } catch (SecurityException e) {
            } catch (ClassCastException e) {
            }
        }

        // if any of those failed, default to the current UI.
        if (ret == null)
            ret = UIManager.getUI(c);

        return ret;
    }
    
    /**
     * Returns <code>text</code> wrapped by an HTML table tag that is set to a
     * fixed width.
     * <p>
     * Note: It seems to be a possible to trigger a NullPointerException in
     * Swing when this is used in a JLabel: GUI-239.
     */
    public static String restrictWidth(String text, int width) {
        return "<html><table width=\"" + width + "\"><tr><td>" + text
                + "</td></tr></table></html>";
    }
    
    /**
     * Using a little reflection here for a lack of any better way 
     * to access locale-specific char codes for menu mnemonics.
     * We could at least defer this in the future.
     *
     * @param str the key for the locale-specific char resource to
     *  look up -- the key as it appears in the locale-specific
     *  properties file
     * @return the code for the passed-in key as defined in 
     *  <tt>java.awt.event.KeyEvent</tt>, or -1 if no key code
     *  could be found
     */
    public static int getCodeForCharKey(String str) {
        int charCode = -1;
        String charStr = str.toUpperCase(Locale.US);
        if(charStr.length()>1) return -1;
        try {
            Field charField = KeyEvent.class.getField("VK_"+charStr);
            charCode = charField.getInt(KeyEvent.class);
        } catch (NoSuchFieldException e) {
            LOG.error("can't get key for: " + charStr, e);
        } catch (SecurityException e) {
            LOG.error("can't get key for: " + charStr, e);
        } catch (IllegalAccessException e) {
            LOG.error("can't get key for: " + charStr, e);
        }
        return charCode;
    }
    
    private static int getAmpersandPosition(String text) {
        int index = -1;
        while ((index = text.indexOf('&', index + 1)) != -1) {
            if (index < text.length() - 1 && Character.isLetterOrDigit(text.charAt(index + 1))) {
                break;
            }
        }
        return index;
    }
    
    /**
     * Strips the first ampersand '&' in <code>text</code> that appears
     * before a letter or digit.
     * 
     * @return the original text if there is no such ampersand
     */
    public static String stripAmpersand(String text) {
        int index = getAmpersandPosition(text);
        if (index >= 0) {
            return text.substring(0, index) + text.substring(index + 1);
        }
        return text;
    }
    
    /**
     * Finds the first ampersand '&' in <code>text</code> that appears
     * before a letter or a digit and returns the key code for the letter
     * or digit after it.
     */
    public static int getMnemonicKeyCode(String text) {
        // parse out mnemonic key
        int index = getAmpersandPosition(text);
        if (index >= 0) {
            return getCodeForCharKey(text.substring(index + 1, index + 2));
        }
        return -1;
    }
    
    /**
     * Determines if the Start On Startup option is available.
     */
    public static boolean shouldShowStartOnStartupWindow() {
        return OSUtils.isMacOSX() ||
               WindowsUtils.isLoginStatusAvailable();
    }
    
    /**
     * Returns a string representing the quality score just like 
     * toQualityString not including the trailing word 'Quality'.
     */
    public static String toQualityStringShort(long qualityScore) {
        if (qualityScore <= 1) {
            return I18n.trc("Poor (media quality)", "Poor");
        } else if (qualityScore == 2) {
            return I18n.trc("Good (media quality)", "Good");
        } else {
           return I18n.trc("Excellent (media quality)", "Excellent");
        }
    }
    
    /**
     * Returns a quality string for the given quality score.
     * <pre>
     * <= 1  Poor Quality
     * == 2  Good Quality
     * >  2  Excellent Quality
     * </pre>
     */
    public static String toQualityString(long qualityScore) {
        if (qualityScore <= 1) {
            return I18n.tr("Poor Quality");
        } else if (qualityScore == 2) {
            return I18n.tr("Good Quality");
        } else {
           return I18n.tr("Excellent Quality");
        }
    }
    
    /** Returns a listener that opens hyperlinks in a browser. */
    public static HyperlinkListener getHyperlinkListener() {
        return new HyperlinkListener() {
            @Override
            public void hyperlinkUpdate(HyperlinkEvent e) {
                if(e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                    URL url = e.getURL();
                    if(url != null) {
                        NativeLaunchUtils.openURL(url.toExternalForm());
                    }
                }
            }
        };
    }
    
    /** Returns the ESC action. */
    public static Action getDisposeAction() {
        return new AbstractAction() {
            public void actionPerformed(ActionEvent ae) {
                Window parent;
                if(ae.getSource() instanceof Window)
                    parent = (Window)ae.getSource();
                else
                    parent = SwingUtilities.getWindowAncestor((Component)ae.getSource());

                if(parent != null)
                    parent.dispatchEvent(new WindowEvent(parent, WindowEvent.WINDOW_CLOSING));
            }
        };
    }
    
    /**
     * Adds a hide action to a JDialog.
     */
    public static void addHideAction(JDialog jd) {
        addHideAction(jd.getRootPane());
    }
    
    /**
     * Adds an action to hide a window / dialog.
     * <p>
     * On OSX, this is done by typing 'Command-W'.
     * On all other platforms, this is done by hitting 'ESC'.
     */
    public static void addHideAction(JComponent jc) {
        InputMap map = jc.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        map.put(getHideKeystroke(), "limewire.hideWindow");
        jc.getActionMap().put("limewire.hideWindow", getDisposeAction());
    }
    
    /**
     * Gets the keystroke for hiding a window according to the platform.
     */
    public static KeyStroke getHideKeystroke() {
        if(OSUtils.isMacOSX()) {
            return KeyStroke.getKeyStroke(KeyEvent.VK_W,
                            Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
        } else {
            return KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
        }
    }
    
    /**
     * Returns the point for the placing the specified component on the
     * center of the screen.
     *
     * @param comp the <tt>Component</tt> to use for getting the relative
     *             center point
     * @return the <tt>Point</tt> for centering the specified
     *         <tt>Component</tt> on the screen
     */
    public static Point getScreenCenterPoint(Component comp) {
        Dimension dimension = comp.getSize();
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int appWidth = Math.min(screenSize.width, dimension.width);
        // compare against a little bit less than the screen size,
        // as the screen size includes the taskbar
        int appHeight = Math.min(screenSize.height - 40, dimension.height);
        return new Point((screenSize.width - appWidth) / 2, (screenSize.height - appHeight) / 2);
    }

    /**
     * Determines if a font can display up to a point in the string.
     * <p>
     * Returns -1 if it can display the whole string.
     */
    public static boolean canDisplay(Font f, String s) {
        int upTo = f.canDisplayUpTo(s);
        if(upTo >= s.length() || upTo == -1)
            return true;
        else
            return false;
    }
    
    /**
     * Iterates over all the windows owned by this process then hides and 
     *  disposes them.
     */
    public static void hideAndDisposeAllWindows() {
        for(Window window : Window.getWindows()) {
            try {
                window.setVisible(false);
                window.dispose();
            } catch(Throwable ignored) {}
        }
    }

}
