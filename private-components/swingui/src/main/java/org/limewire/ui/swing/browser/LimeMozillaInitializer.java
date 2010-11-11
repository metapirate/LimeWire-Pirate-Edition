package org.limewire.ui.swing.browser;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.core.settings.MozillaSettings;
import org.limewire.io.Expand;
import org.limewire.io.IOUtils;
import org.limewire.util.CommonUtils;
import org.limewire.util.FileUtils;
import org.limewire.util.OSUtils;
import org.mozilla.browser.IMozillaWindow;
import org.mozilla.browser.IMozillaWindowFactory;
import org.mozilla.browser.MozillaConfig;
import org.mozilla.browser.MozillaInitialization;
import org.mozilla.browser.MozillaWindow;
import org.mozilla.browser.impl.WindowCreator;

public class LimeMozillaInitializer {

    private static final Log LOG = LogFactory.getLog(LimeMozillaInitializer.class);

    private LimeMozillaInitializer() {
    }

    public static boolean shouldInitialize() {
        return (OSUtils.isWindows() || OSUtils.isLinux() || OSUtils.isMacOSX())
                && is32BitProcess() && MozillaSettings.USE_MOZILLA.getValue();
    }

    private static boolean is32BitProcess() {
        return "i386".equals(OSUtils.getOSArch()) || "x86".equals(OSUtils.getOSArch());
    }

    public static void initialize() {
        if (!shouldInitialize()) {
            return;
        }

        File xulInstallPath = new File(CommonUtils.getUserSettingsDir(), "/browser");
        // Check to see if the correct version of XUL exists.
        File xulFile = new File(xulInstallPath, "xul-v2.0b2.5-do-not-remove");
        if (!xulFile.exists()) {
            if (LOG.isDebugEnabled())
                LOG.debug("unzip xulrunner to " + xulInstallPath);
            FileUtils.deleteRecursive(xulInstallPath);
            InputStream in = null;
            try {
                in = new BufferedInputStream(CommonUtils.getResourceStream(getResourceName()));
                Expand.expandFile(in, xulInstallPath, true, null);
                xulFile.createNewFile();
            } catch (IOException e) {
                // This causes errors if the zip file can't be expanded for whatever reason.
                // It sucks, but no need to report it to us... we fallback to using
                // JEditorPane where we can.
//                ErrorService.error(e);
            } finally {
                IOUtils.close(in);
            }
        }

        installFlashLinux(xulInstallPath);

        String newLibraryPath = System.getProperty("java.library.path") + File.pathSeparator
                + xulInstallPath.getAbsolutePath();
        System.setProperty("java.library.path", newLibraryPath);

        MozillaConfig.setXULRunnerHome(xulInstallPath);
        File profileDir = new File(CommonUtils.getUserSettingsDir(), "/mozilla-profile");
        profileDir.mkdirs();
        MozillaConfig.setProfileDir(profileDir);
        WindowCreator.setWindowFactory(new IMozillaWindowFactory() {
            @Override
            public IMozillaWindow create(boolean attachNewBrowserOnCreation) {
                MozillaPopupPanel popupPanel = new MozillaPopupPanel(attachNewBrowserOnCreation);
                MozillaWindow popupWindow = new MozillaWindow(popupPanel);
                return popupWindow;
            }
        });
        MozillaInitialization.initialize();

        if (LOG.isDebugEnabled())
            LOG.debug("Moz Summary: " + MozillaConfig.getConfigSummary());
    }

    private static void installFlashLinux(File xulInstallPath) {
        //TODO move logic to .deb? or prompt before doing, this can be a security risk
        File pluginsDir = new File(xulInstallPath, "/xulrunner/plugins");

        if (OSUtils.isLinux()) {
            for (File file : pluginsDir.listFiles()) {
                if (file.isFile() && file.getName().contains("flash")) {
                    return;// flash already installed
                }
            }

            File[] possibleFlashLocations = new File[] { new File("/usr/lib/flash-plugin/"), new File("/usr/lib/firefox/plugins"),
                    new File("/usr/lib/mozilla/plugins"), new File("/usr/lib/iceweasle"),
                    new File("/usr/lib/xulrunner"), new File(CommonUtils.getUserHomeDir(), "/.mozilla/plugins") };
            for (File flashLocation : possibleFlashLocations) {
                if (flashLocation.exists() && flashLocation.isDirectory()) {
                    boolean foundFlash = false;
                    for (File flashFile : flashLocation.listFiles()) {
                        if (flashFile.getName().contains("flash")) {
                            // TODO make sure we are not using a file we cannot
                            // support, ie 64 bit running in 32 bit jvm

                            File linkTarget = new File(pluginsDir, "/" + flashFile.getName());
                            // using a symlink instead of copying, debian had
                            // issues with copying the library.
                            try {
                                FileUtils.createSymbolicLink(flashFile, linkTarget);
                                foundFlash = true;
                            } catch (IOException e) {
                                LOG.debug(e.getMessage(), e);
                            } catch (InterruptedException e) {
                                LOG.debug(e.getMessage(), e);
                            }
                            // continue looping because there might be more than
                            // 1 file at this location for flash to work
                        }
                    }
                    if (foundFlash) {
                        // flash was found at another location and copied
                        break;
                    }
                }
            }
        }
    }

    private static String getResourceName() {
        if (OSUtils.isWindows()) {
            return "xulrunner-win32.zip";
        } else if (OSUtils.isLinux()) {
            return "xulrunner-linux.zip";
        } else if (OSUtils.isMacOSX()) {
            return "xulrunner-macosx-i386.zip";
        } else {
            throw new IllegalStateException("no resource for OS: " + OSUtils.getOS());
        }
    }
}
