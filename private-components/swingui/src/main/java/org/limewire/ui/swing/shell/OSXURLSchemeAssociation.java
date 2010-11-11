package org.limewire.ui.swing.shell;

import org.limewire.ui.swing.util.MacOSXUtils;

class OSXURLSchemeAssociation implements ShellAssociation {
	private final String urlScheme;
	
	/**
	 * 
	 * @param urlScheme - the URL scheme denoting a particular prototype, e.g. magnet.
	 * (Do not include the ":" / colon as part of the URL scheme.) 
	 */
	protected OSXURLSchemeAssociation(String urlScheme) {
	    this.urlScheme = urlScheme;
	}
	
	public static boolean isNativeLibraryLoadedCorrectly() {
	    return MacOSXUtils.isNativeLibraryLoadedCorrectly();
	}
	
	public boolean isAvailable() {
	    return !MacOSXUtils.isURLSchemeHandled(urlScheme);
	}

	public boolean isRegistered() {
        return MacOSXUtils.isLimewireDefaultURLSchemeHandler(urlScheme);
	}

    @Override
    public void register() {
        MacOSXUtils.setLimewireAsDefaultURLSchemeHandler(urlScheme);
    }

    public boolean canUnregister() {
        return MacOSXUtils.canChangeDefaultURLSchemeHandler(urlScheme);
    }

    @Override
    public void unregister() {
        // There is no way to unregister a URL scheme association on OS-X. 
        // So, we'll try to set another registered handler for this file
        // type as the default handler.
        MacOSXUtils.tryChangingDefaultURLSchemeHandler(urlScheme);
    }

}
