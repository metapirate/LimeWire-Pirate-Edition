package org.limewire.ui.swing.util;

import java.awt.Font;
import java.util.Locale;
import java.util.Map;

import javax.swing.UIManager;

import org.limewire.core.settings.ApplicationSettings;
import org.limewire.util.OSUtils;
import org.limewire.util.StringUtils;

public class LocaleUtils {
    
    private LocaleUtils() {}
    
    /** Returns the current locale in use. */
    public static Locale getCurrentLocale() {
        return new Locale(ApplicationSettings.LANGUAGE.get(),
                ApplicationSettings.COUNTRY.get(),
                ApplicationSettings.LOCALE_VARIANT.get());        
    }

    /** Sets the locale based on whats in the preferences. */
    public static void setLocaleFromPreferences() {
        if (ApplicationSettings.LANGUAGE.get().equals("")) {
            ApplicationSettings.LANGUAGE.set("en");
        }
        
        Locale locale = new Locale(ApplicationSettings.LANGUAGE.get(),
                ApplicationSettings.COUNTRY.get(),
                ApplicationSettings.LOCALE_VARIANT.get());
        Locale.setDefault(locale);
        StringUtils.setLocale(locale);
        GuiUtils.setLocale(locale);
        I18n.setLocale(locale);
    }

    /**
     * Validates the locale, determining if the current locale's resources can
     * be displayed using the current fonts. If not, then the locale is reset to
     * English.
     * 
     * This prevents the UI from appearing as all boxes.
     */
    public static void validateLocaleAndFonts() {
        // OSX can always display everything, and if it can't,
        // we have no way of correcting things 'cause canDisplayUpTo
        // is broken on it.
        if (OSUtils.isMacOSX()) {
            return;
        }

        String s = getCurrentLocale().getDisplayName();
        if (!checkUIFonts("dialog", s)) {
            // if it couldn't display, revert the locale to english.
            ApplicationSettings.LANGUAGE.set("en");
            ApplicationSettings.COUNTRY.set("");
            ApplicationSettings.LOCALE_VARIANT.set("");
            setLocaleFromPreferences();
        }
    }

    /**
     * Alters all Fonts in UIManager to use Dialog, to correctly display foreign
     * strings.
     */
    private static boolean checkUIFonts(String newFont, String testString) {
        String[] comps = new String[] { "TextField.font", "PasswordField.font",
                "TextArea.font", "TextPane.font", "EditorPane.font",
                "FormattedTextField.font", "Button.font", "CheckBox.font",
                "RadioButton.font", "ToggleButton.font", "ProgressBar.font",
                "ComboBox.font", "InternalFrame.titleFont", "DesktopIcon.font",
                "TitledBorder.font", "Label.font", "List.font",
                "TabbedPane.font", "Table.font", "TableHeader.font",
                "MenuBar.font", "Menu.font", "Menu.acceleratorFont",
                "MenuItem.font", "MenuItem.acceleratorFont", "PopupMenu.font",
                "CheckBoxMenuItem.font", "CheckBoxMenuItem.acceleratorFont",
                "RadioButtonMenuItem.font",
                "RadioButtonMenuItem.acceleratorFont", "Spinner.font",
                "Tree.font", "ToolBar.font", "OptionPane.messageFont",
                "OptionPane.buttonFont", "ToolTip.font", };

        boolean displayable = false;
        for (int i = 0; i < comps.length; i++)
            displayable |= checkFont(comps[i], newFont, testString, false);

        // Then do it the automagic way.
        // note that this could work all the time (without requiring the above)
        // if Java 1.4 didn't introduce Locales, and it could even still work
        // if they offered a way to get all the keys of possible resources.
        for (Map.Entry<Object, Object> next : UIManager.getDefaults().entrySet()) {
            if (next.getValue() instanceof Font) {
                Font f = (Font) next.getValue();
                if (f != null && !newFont.equalsIgnoreCase(f.getName())) {
                    if (!FontUtils.canDisplay(f, testString)) {
                        f = new Font(newFont, f.getStyle(), f.getSize());
                        if (FontUtils.canDisplay(f, testString)) {
                            next.setValue(f);
                            displayable = true;
                        }
                    }
                }
            }
        }

        return displayable;
    }

    /** Updates the font of a given fontName to be newName. */
    private static boolean checkFont(String fontName, String newName,
            String testString, boolean force) {
        boolean displayable = true;
        Font f = UIManager.getFont(fontName);
        if (f != null && !newName.equalsIgnoreCase(f.getName())) {
            if (!FontUtils.canDisplay(f, testString) || force) {
                f = new Font(newName, f.getStyle(), f.getSize());
                if (FontUtils.canDisplay(f, testString))
                    UIManager.put(fontName, f);
                else
                    displayable = false;
            }
        } else if (f != null) {
            displayable = FontUtils.canDisplay(f, testString);
        } else {
            displayable = false;
        }
        return displayable;
    }
    
}
