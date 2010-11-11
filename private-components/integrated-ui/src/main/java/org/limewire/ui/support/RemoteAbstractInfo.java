package org.limewire.ui.support;

/**
 * This class maintains protected constants and variables for 
 * <tt>RemoteServletInfo</tt> and <tt>RemoteClientInfo</tt>,
 * the classes that contain the data for the client machine 
 * reporting the bug.  This class simply ensures that they are
 * using the same values.
 */
//2345678|012345678|012345678|012345678|012345678|012345678|012345678|012345678|
public abstract class RemoteAbstractInfo {
	    
    /** 
	 * Key for the next time to report this bug.
	 */
	protected static final String NEXT_THIS_BUG_TIME = "1";
	
	/**
	 * Key for the next time to report any bug.
	 */
	protected static final String NEXT_ANY_BUG_TIME = "2";
}
