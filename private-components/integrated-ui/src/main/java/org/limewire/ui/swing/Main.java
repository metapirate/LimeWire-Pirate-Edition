package org.limewire.ui.swing;

import java.awt.Frame;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.SplashScreen;
import java.awt.Toolkit;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Locale;

/** The entry point into the program using the real core. */
public class Main {
    
    private static long startTime;
    
    public static void main(String[] args) {
        startTime = System.currentTimeMillis();
        try {
            if (isMacOSX()) {
                // Register GURL to receive AppleEvents, such as magnet links.
                // Use reflection to not slow down non-OSX systems.
                // "GURLHandler.getInstance().register();"
                Class<?> clazz = Class.forName("org.limewire.ui.swing.GURLHandler");
                Method getInstance = clazz.getMethod("getInstance", new Class[0]);
                Object gurl = getInstance.invoke(null, new Object[0]);
                Method register = gurl.getClass().getMethod("register", new Class[0]);
                register.invoke(gurl, new Object[0]);
            }        
        
        
            SplashScreen splashScreen = SplashScreen.getSplashScreen();
            Frame splash = null;
            Image splashImage = null; // begin with null, assuming we can get it from SplashScreen

            // show initial splash screen only if there are no arguments
            if (args == null || args.length == 0) {
                Rectangle bounds;
                
                // If the splash screen existed & was visible,
                // then we want to take the image of the splash screen
                // and the bounds it was displayed at, so that
                // when we convert to an AWT image, the same image
                // is shown at the same place.
                if(splashScreen != null && splashScreen.isVisible()) {
                    bounds = splashScreen.getBounds();
                    splashImage = Toolkit.getDefaultToolkit().createImage(splashScreen.getImageURL());    
                } else {
                    // If no splash was already showing, then there's no pre-existing bounds
                    // and we need to find our own image.
                    bounds = null;
                    splashImage = getSplashImage();
                }
                
                // If we've discovered an image to show a splash, then show the
                // AWT splash.  Note that if there was a pre-existing splash showing
                // via SplashScreen, the showing of an AWT frame will automatically
                // close & hide that splash.
                if(splashImage != null) {
                    splash = AWTSplashWindow.splash(splashImage, bounds);
                } else if (splashScreen != null && splashScreen.isVisible()) {
                    // If we couldn't find an image to show via AWT, then just close
                    // the existing splash and be done with it.
                    splashScreen.close();                    
                }
            } else {
                // Hide the built-in splash if it was shown, since we don't want a splash
                // right now..
                if(splashScreen != null && splashScreen.isVisible()) {
                    splashScreen.close();
                }                
            }
            
            // If we couldn't find a splashImage by this point, just try to get it.
            // This is because the GuiLoader/Initializer code requires an Image,
            // even if there's no AWT splash frame for displaying it.
            if(splashImage == null) {
                splashImage = getSplashImage();
            }
            
            // load the GUI through reflection so that we don't reference classes here,
            // which would slow the speed of class-loading, causing the splash to be
            // displayed later.
            Class<?> loadClass = Class.forName("org.limewire.ui.swing.GuiLoader");
            Object loadInstance = loadClass.newInstance();
            Method loadMethod = loadClass.getMethod("load", new Class[] { String[].class, Frame.class, Image.class } );
            loadMethod.invoke(loadInstance, args, splash, splashImage);
        } catch(Throwable t) {
            t.printStackTrace();
            System.exit(1);
        }
    }
    
    /**
     * Gets the image to be used as the splash.
     */
    private static Image getSplashImage() {
        URL imageURL = ClassLoader.getSystemResource("org/limewire/ui/swing/mainframe/resources/splash.png");
        if (imageURL != null) {
            return Toolkit.getDefaultToolkit().createImage(imageURL);
        } else {
            return null;
        }
    }

    /** Determines if this is running on OS X. */
    private static boolean isMacOSX() {
        return System.getProperty("os.name", "").toLowerCase(Locale.US).startsWith("mac os x");
    }
    
    public static long getStartTime() {
        return startTime;
    }
}
