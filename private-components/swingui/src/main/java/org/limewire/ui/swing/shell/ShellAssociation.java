package org.limewire.ui.swing.shell;

/**
 * A registration in the platform shell that sets a program as the default viewer for a protocol link or file type.
 */
public interface ShellAssociation {

	/**
	 * @return true if we are currently handling this association
	 */
	public boolean isRegistered();
	
	/**
	 * @return true if nobody is handling this association
	 */
	public boolean isAvailable();

	/**
	 * Associates this running program with this protocol or file type in the shell.
	 */
	public void register();

    /**
     * Checks whether we can clear the shell association.
     */
	public boolean canUnregister();
	
	/**
	 * Clears this shell association, leaving another program or no program registered.
	 */
	public void unregister();

}
