package org.limewire.ui.swing.mainframe;

import java.awt.Color;
import java.awt.Frame;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.lang.reflect.Method;
import java.util.Enumeration;
import java.util.EventObject;

import javax.swing.Icon;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPopupMenu;
import javax.swing.ToolTipManager;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.plaf.ColorUIResource;

import org.jdesktop.application.Action;
import org.jdesktop.application.ApplicationContext;
import org.jdesktop.application.Resource;
import org.jdesktop.application.SessionStorage;
import org.jdesktop.application.SingleFrameApplication;
import org.jdesktop.swingx.JXPanel;
import org.jdesktop.swingx.painter.AbstractPainter;
import org.limewire.core.api.Application;
import org.limewire.core.settings.InstallSettings;
import org.limewire.inject.GuiceUtils;
import org.limewire.inject.LimeWireInjectModule;
import org.limewire.inject.Modules;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.ui.swing.LimeWireSwingUiModule;
import org.limewire.ui.swing.browser.LimeMozillaInitializer;
import org.limewire.ui.swing.components.LimeJFrame;
import org.limewire.ui.swing.components.PlainCheckBoxMenuItemUI;
import org.limewire.ui.swing.components.PlainMenuItemUI;
import org.limewire.ui.swing.components.PlainMenuUI;
import org.limewire.ui.swing.components.PlainWindowsCheckBoxMenuItemUI;
import org.limewire.ui.swing.components.PlainWindowsMenuItemUI;
import org.limewire.ui.swing.components.PlainWindowsMenuUI;
import org.limewire.ui.swing.home.HomeMediator;
import org.limewire.ui.swing.menu.LimeMenuBar;
import org.limewire.ui.swing.options.OptionsDialog;
import org.limewire.ui.swing.settings.SwingUiSettings;
import org.limewire.ui.swing.shell.ShellAssociationManager;
import org.limewire.ui.swing.tray.TrayExitListener;
import org.limewire.ui.swing.tray.TrayNotifier;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.SaveDirectoryHandler;
import org.limewire.ui.swing.wizard.SetupWizard;
import org.limewire.util.OSUtils;
import org.mozilla.browser.MozillaInitialization;
import org.mozilla.browser.MozillaInitialization.InitStatus;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Provider;
import com.google.inject.Stage;

/**
 * The entry point for the Swing UI.  If the real core is desired,
 * start from integrated-ui/../Main.  The main method in this class
 * uses the mock-core.
 */
public class AppFrame extends SingleFrameApplication {
    private static final Log LOG = LogFactory.getLog(AppFrame.class);    
    public static final String STARTUP = "startup";
    private boolean isStartup = false;

    @Inject private static volatile Injector injector;

    private static volatile boolean started;

    /** Default background color for panels. */
    @Resource private Color bgColor;
    @Resource private Color glassPaneColor;
    @Resource private Color selectionBackgroundColor;
    
    // Icons for JFileChooser bug workaround on Vista. 
    @Resource private Icon upFolderVistaFixIcon;
    @Resource private Icon detailsViewVistaFixIcon;
    @Resource private Icon listViewVistaFixIcon;
    @Resource private Icon newFolderVistaFixIcon;
    
    @Inject private Application application;
    @Inject private LimeWireSwingUI ui;
    @Inject private Provider<SetupWizard> setupWizardProvider;
    @Inject private Provider<OptionsDialog> options;
    @Inject private FramePositioner framePositioner;
    @Inject private TrayNotifier trayNotifier;
    @Inject private LimeMenuBar limeMenuBar;
    @Inject private DelayedShutdownHandler delayedShutdownHandler;
    @Inject private HomeMediator homeMediator;
    
    private OptionsDialog lastOptionsDialog;
    
    /** Starts with the mock core. */
    public static void main(String[] args) {
        launch(AppFrame.class, args);
    }

    /** Returns true if the UI has initialized & successfully been shown. */
    public static boolean isStarted() {
        return started;
    }
    
    /**
     * JDesktop's built-in storage isn't what we want -- don't use it.
     * There's issues with it restoring sizes/position if you closed
     * while minimized or other weirdness.  Unfortunately, changing
     * it is package-private.
     */
    private void changeSessionStorage(ApplicationContext ctx, SessionStorage storage) {
        try {
            Method m = ctx.getClass().getDeclaredMethod("setSessionStorage", SessionStorage.class);
            m.setAccessible(true);
            m.invoke(ctx, storage);
        } catch(Throwable oops) {}
    }
    
    @Override
    protected void initialize(String[] args) {
        changeSessionStorage(getContext(), new NullSessionStorage(getContext()));
        
        GuiUtils.assignResources(this);

        initUIDefaults();
        
        // Because we use a browser heavily, which is heavyweight,
        // we must disable all lightweight popups.
        if(MozillaInitialization.getStatus() != InitStatus.FAILED) {
            JPopupMenu.setDefaultLightWeightPopupEnabled(false);
            ToolTipManager.sharedInstance().setLightWeightPopupEnabled(false);
        }
        
        isStartup = args.length > 0 && STARTUP.equals(args[0]);
    }
   
    @Override
    protected void startup() {         
        String title = getContext().getResourceMap().getString("Application.title");
        JFrame frame = new FirstVizIgnorer(title);
        frame.setName("mainFrame");
        getMainView().setFrame(frame);

        // Create the Injector for the UI.
        assert ui == null;
        createUiInjector();
        assert ui != null;
        
        if(isStartup || SwingUiSettings.MINIMIZE_TO_TRAY.getValue()) {
            trayNotifier.showTrayIcon();            
        } else {
            trayNotifier.hideTrayIcon();
        }
        getMainFrame().setJMenuBar(limeMenuBar);

        // Install handler for shutdown after transfers.
        delayedShutdownHandler.install(this);
        
        addExitListener(new TrayExitListener(trayNotifier));
        // On OS X clicking the x in a window doesn't normally close an application.  It just closes the 
        // window while the application sits in the dock with a light under it showing that it's still running.
        if (OSUtils.isMacOSX()) {
            addExitListener(new ExitListener() {
                @Override
                public boolean canExit(EventObject event) {
                    if (event.getSource() instanceof JFrame) {
                        hide(getMainView());
                        return false;
                    } else {
                        return true;
                    }
                }

                @Override
                public void willExit(EventObject event) {
                }
                
            });
        }
        addExitListener(new ShutdownListener(getMainFrame(), application));
        
        framePositioner.initialize(getMainFrame());
       
        // This will NOT actually show it -- we're purposely
        // doing this to more explicitly control when it goes visible.
        // Unfortunately, we have to call show in order to setup things
        // on the ui.
        show(ui);
        
        // Set the window position just before we become visible.
        framePositioner.setWindowPosition();
        
        // Set visible if this isn't being run from startup --
        // otherwise, minimize.
        if (isStartup) {
            minimizeToTray();
        } else {
            getMainFrame().setVisible(true);
        }
     // Reset the browser page to blank before continuing.  Necessary here so that Mac won't freeze.
        homeMediator.getComponent().loadBlank();
        started = true;
    }
    
    @Override
    protected void ready() {
        if (application.isNewInstall()) {
            initializeNewInstall();
        }
        else if (SetupWizard.shouldShowWizard()) {
            showSetupWizard();
        }       

        ui.goHome();
        ui.focusOnSearch();

        // Make absolutely positively certain that we've set this to true.
        InstallSettings.UPGRADED_TO_5.setValue(true);
        
        validateSaveDirectory();

        // Now that the UI is ready to use, update it's priority a bit.
        Thread eventThread = Thread.currentThread();
        eventThread.setPriority(eventThread.getPriority() + 1);

        new ShellAssociationManager().validateFileAssociations(getMainFrame());
    }
    
    /**
     * Run iff this is a new install of LW.
     */
    private void initializeNewInstall() {
        //setup wizard is always shown on new installs
        showSetupWizard();
        
        // Sets the upgraded flag after the setup wizard completes
        InstallSettings.UPGRADED_TO_5.setValue(true);
        InstallSettings.LAST_VERSION_RUN.set(application.getVersion());
        InstallSettings.PREVIOUS_RAN_VERSIONS.add(application.getVersion());
    }
    
    private void showSetupWizard() {
        JXPanel glassPane = new JXPanel();
        glassPane.setOpaque(false);
        glassPane.setBackgroundPainter(new AbstractPainter<JComponent>() {
            @Override
            protected void doPaint(Graphics2D g, JComponent object, int width, int height) {
                g.setPaint(glassPaneColor);
                g.fillRect(0, 0, width, height);
            }
        });
        getMainFrame().setGlassPane(glassPane);
        
        ui.hideMainPanel();
        glassPane.setVisible(true);
        setupWizardProvider.get().showDialog(getMainFrame());
        glassPane.setVisible(false);
        glassPane.setBackgroundPainter(null);
        ui.showMainPanel();
    }
    
    @Action
    public void showOptionsDialog(ActionEvent actionEvent) { // DO NOT CHANGE THIS METHOD NAME!  
        if(lastOptionsDialog == null) {
            lastOptionsDialog = options.get();
        }
        
        if (!lastOptionsDialog.isVisible()) {
            lastOptionsDialog.initOptions();
            // if the main frame is not visible on OS X when preferences is opened, then a zombie window popups up...
            GuiUtils.getMainFrame().setVisible(true);
            lastOptionsDialog.setLocationRelativeTo(GuiUtils.getMainFrame());
            if(actionEvent.getID() == ActionEvent.ACTION_PERFORMED && actionEvent.getActionCommand() != null){
                lastOptionsDialog.select(actionEvent.getActionCommand());
            }
            lastOptionsDialog.setVisible(true);
        }
    }
    
    @Action
    public void exitApplication(ActionEvent actionEvent) { // DO NOT CHANGE THIS METHOD NAME!
        exit(actionEvent);
    }
    
    @Action
    public void minimizeToTray() { // DO NOT CHANGE THIS METHOD NAME!  
        getMainFrame().setState(Frame.ICONIFIED);
        getMainFrame().setVisible(!trayNotifier.supportsSystemTray());
    }    

    @Action
    public void restoreView() { // DO NOT CHANGE THIS METHOD NAME! 
        if(!SwingUiSettings.MINIMIZE_TO_TRAY.getValue()) {
            trayNotifier.hideTrayIcon();
        }
        getMainFrame().setVisible(true);
        getMainFrame().setState(Frame.NORMAL);
        getMainFrame().toFront();
    }

    /**
     * Action method to exit the application after all transfers are completed.
     */
    @Action
    public void shutdownAfterTransfers() { // DO NOT CHANGE THIS METHOD NAME!
        trayNotifier.showTrayIcon();
        delayedShutdownHandler.shutdownAfterTransfers();
    }
    
    private Injector createUiInjector() {
        Module thiz = new AbstractModule() {
            @Override
            protected void configure() {
                bind(AppFrame.class).toInstance(AppFrame.this);
            }
        };
        Injector childInjector;
        if (injector == null) {
            LimeMozillaInitializer.initialize();
            childInjector = Guice.createInjector(Stage.DEVELOPMENT,
                    new LimeWireInjectModule(),
                    new LimeWireSwingUiModule(),
                    thiz);
        } else {
            // TODO: We want to use child injectors, but weird things happen
            //       with circular dependencies...
            childInjector = Guice.createInjector(Stage.DEVELOPMENT,
                    Modules.providersFrom(injector),
                    new LimeWireInjectModule(),
                    thiz,
                    new LimeWireSwingUiModule());
        }        
        GuiceUtils.loadEagerSingletons(childInjector);
        return childInjector;
    }
    
    /**
     * Sets the custom default UI color and behavior properties
     */
    private void initUIDefaults() {       
        if (OSUtils.isMacOSX()) {
            initMacUIDefaults();
        }
        
        if (OSUtils.isWindows()) {
            verifyWindowsLAF();
        }
        
        initBackgrounds();

        Color originalSelectionBackground  
            = (Color) UIManager.get("MenuItem.selectionBackground");
        
        Color originalSelectionForeground 
            = (Color) UIManager.get("MenuItem.selectionForeground");

        if (originalSelectionForeground != null && originalSelectionBackground != null) {
            PlainMenuUI.overrideDefaults(originalSelectionForeground, originalSelectionBackground);
            PlainMenuItemUI.overrideDefaults(originalSelectionForeground, originalSelectionBackground);
            PlainCheckBoxMenuItemUI.overrideDefaults(originalSelectionForeground, originalSelectionBackground);
            
            PlainWindowsMenuUI.overrideDefaults(originalSelectionForeground, originalSelectionBackground);
            PlainWindowsMenuItemUI.overrideDefaults(originalSelectionForeground, originalSelectionBackground);
            PlainWindowsCheckBoxMenuItemUI.overrideDefaults(originalSelectionForeground, originalSelectionBackground);
            
        }
        
        // Set default selection colours
        UIManager.put("TextField.selectionBackground", selectionBackgroundColor);
        UIManager.put("PasswordField.selectionBackground", selectionBackgroundColor);
        UIManager.put("EditorPane.selectionBackground", selectionBackgroundColor);
        UIManager.put("TextArea.selectionBackground", selectionBackgroundColor);
        UIManager.put("Menu.selectionBackground", selectionBackgroundColor);
        UIManager.put("MenuItem.selectionBackground", selectionBackgroundColor);
        UIManager.put("CheckBoxMenuItem.selectionBackground", selectionBackgroundColor);
        UIManager.put("RadioButtonMenuItem.selectionBackground", selectionBackgroundColor);
        
        // Set the menu item highlight colors to avoid contrast issues with
        //  new highlight background in default XP theme
        Color selectionForeground = Color.BLACK;
        UIManager.put("TextField.selectionForeground", selectionForeground);
        UIManager.put("PasswordField.selectionForeground", selectionForeground);
        UIManager.put("EditorPane.selectionForeground", selectionForeground);
        UIManager.put("TextArea.selectionForeground", selectionForeground);
        UIManager.put("Menu.selectionForeground", selectionForeground);
        UIManager.put("MenuItem.selectionForeground", selectionForeground);
        UIManager.put("CheckBoxMenuItem.selectionForeground", selectionForeground);
        UIManager.put("RadioButtonMenuItem.selectionForeground", selectionForeground);
        
        
        // Necessary to allow popups to behave normally.
        UIManager.put("PopupMenu.consumeEventOnClose", false);
        
        // FIX FOR SUN BUG 6449933: On Windows sometimes, JFileChooser cannot 
        // display because some icons throw an AIOOBE when they are retrieved.
        // To workaround this, we install our own version of those icons.
        if (OSUtils.isWindows()) {
            replaceIconIfFailing("FileChooser.upFolderIcon", upFolderVistaFixIcon);
            replaceIconIfFailing("FileChooser.detailsViewIcon", detailsViewVistaFixIcon);
            replaceIconIfFailing("FileChooser.listViewIcon", listViewVistaFixIcon);
            replaceIconIfFailing("FileChooser.newFolderIcon", newFolderVistaFixIcon);
        }
    }
    
    /**
     * Sets some mac only UI settings.
     */
    private void initMacUIDefaults() {
        UIManager.put("MenuItemUI", "javax.swing.plaf.basic.BasicMenuItemUI");
        UIManager.put("CheckBoxMenuItemUI", "javax.swing.plaf.basic.BasicCheckBoxMenuItemUI");
        UIManager.put("RadioButtonMenuItemUI", "javax.swing.plaf.basic.BasicRadioButtonMenuItemUI");
        UIManager.put("MenuUI", "javax.swing.plaf.basic.BasicMenuUI");
        // these two lines have to be here in the mac UI settings method.
        // if they're put under initUIDefaults, then they turn the application menus in Windows green as well...
        // On OSX they have no effect on the application menu color.
        UIManager.put("Menu.selectionBackground", selectionBackgroundColor);
        UIManager.put("Menu.selectionForeground", Color.BLACK);
    }
        
    /**
     * Changes all default background colors that are equal to Panel.background to the
     * bgColor set in properties. Also sets Table.background.
     */
    private void initBackgrounds() {
        ColorUIResource bgColorResource = new ColorUIResource(bgColor);
        Color oldBgColor = UIManager.getDefaults().getColor("Panel.background");
        UIDefaults uiDefaults = UIManager.getDefaults();
        Enumeration<?> enumeration = uiDefaults.keys();
        while (enumeration.hasMoreElements()) {
            Object key = enumeration.nextElement();
            if (key.toString().indexOf("background") != -1) {
                if (uiDefaults.get(key).equals(oldBgColor)) {
                    UIManager.getDefaults().put(key, bgColorResource);
                }
            }
        }

        uiDefaults.put("Table.background", bgColorResource);
    }
    
    /**
     * Replaces an icon resource in UIManager with the specified replacement
     * icon if the original resource cannot be retrieved correctly.
     */
    private void replaceIconIfFailing(String resource, Icon replacementIcon) {
        try {
            UIManager.getIcon(resource);
        } catch (ArrayIndexOutOfBoundsException aioobe) {
            UIManager.put(resource, replacementIcon);
        }
    }
        
    /** Ensures the save directory is valid. */
    private void validateSaveDirectory() {        
        // Make sure the save directory is valid.
        SaveDirectoryHandler.validateSaveDirectoryAndPromptForNewOne();
    }
    
    /**
     * Verify that the Windows LAF will load properly.  If not, then this 
     * method installs the cross-platform LAF.  This is a workaround for Sun
     * Bug IDs 6629522, 6588271, 6622760, 6637885, which can cause an NPE when 
     * retrieving the checkbox icon width. 
     */
    private void verifyWindowsLAF() {
        // Skip if installed LAF is not Windows LAF.
        String lafName = UIManager.getLookAndFeel().getClass().getName();
        if (!UIManager.getSystemLookAndFeelClassName().equals(lafName)) {
            return;
        }
        
        boolean lafValid = true;

        try {
            // Create checkbox with sample text, and get its preferred size.  
            // This should force a call to get the width of the checkbox icon 
            // in the Windows LAF.  The icon class is 
            // WindowsIconFactory$CheckBoxIcon.  The icon can also be retrieved
            // by a call to WindowsCheckBoxUI.getDefaultIcon(). (JIRA LWC-2302)
            JCheckBox checkBox = new JCheckBox("Verify");
            checkBox.getPreferredSize();
            
        } catch (NullPointerException npe) {
            LOG.error("Windows XP LAF error", npe);
            lafValid = false;
        }
        
        // Install cross-platform LAF if Windows LAF is not valid.
        if (!lafValid) {
            try {
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            } catch (Exception ex) {
                LOG.error("Unable to install LAF", ex);
            }
        }
    }
    
    private static class ShutdownListener implements ExitListener {
        private final Application application;
        private final JFrame mainFrame;
        
        public ShutdownListener(JFrame mainFrame, Application application) {
            this.mainFrame = mainFrame;
            this.application = application;
        }        
        
        @Override
        public boolean canExit(EventObject event) {
            return true;
        }
        
        @Override
        public void willExit(EventObject event) {
            mainFrame.setVisible(false);
            System.out.println("Shutting down...");
            application.stopCore();
            System.out.println("Shut down");
        }
        
    }
    
    private static class FirstVizIgnorer extends LimeJFrame {
        /** false if we haven't shown once yet. */
        private boolean shownOnce = false;

        public FirstVizIgnorer(String title) {
            super(title);
        }

        //this is a bit hacky and ugly but necessary because SingleFrameApplication.show(Component) calls setVisible
        //and we don't want that to happen.  Unfortunately show() also calls private methods so we can't just override it.
        @Override
        public void setVisible(boolean visible) {
            // Ignore the first call.
            if(!shownOnce) {
                shownOnce = true;
            } else {
                super.setVisible(visible);
            }
        }
        
        @SuppressWarnings("deprecation")
        @Override
        public void hide() {
            try {
                super.hide();
            } catch(Throwable t) {
                // Ignored... Internal JDK bugs causing this to error
                // out, which ends up stopping us from closing LW.
            }
        }

    }
}
