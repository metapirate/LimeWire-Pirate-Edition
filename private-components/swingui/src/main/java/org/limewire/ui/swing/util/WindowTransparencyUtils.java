package org.limewire.ui.swing.util;

import java.awt.Window;
import java.lang.reflect.Method;

import javax.swing.JWindow;

import org.limewire.util.OSUtils;

public class WindowTransparencyUtils {

    /**
     * Sets the alpha transparency for a component. com.sun.awt.AWTUtilities
     * exists for java 1.6 update 10. Calling it for reflection to support older
     * 1.6 clients which it will not exist. Mac does not yet have a 1.6 update
     * 10 client yet, but they support a client property Window.alpha to get the
     * same effect.
     */
    @SuppressWarnings("unchecked")
    public static void setAlpha(JWindow window, float alpha) {
        if (alpha >= .99f) {
            alpha = .99f;// prevents flash when component switches from opaque
            // to transparent
        }

        try {
            // windows, linux, solaris java 1.6 update 10
            Class awtutil = Class.forName("com.sun.awt.AWTUtilities");
            Method setWindowOpaque = awtutil.getMethod("setWindowOpacity", Window.class,
                    float.class);
            setWindowOpaque.invoke(null, window, alpha);
        } catch (Exception ex) {
            if (OSUtils.isMacOSX()) {
                window.getRootPane().putClientProperty("Window.alpha", new Float(alpha));// mac
            }
        }
    }
}
