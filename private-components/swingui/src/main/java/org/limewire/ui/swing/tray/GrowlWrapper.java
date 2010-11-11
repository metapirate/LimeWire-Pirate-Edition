package org.limewire.ui.swing.tray;

import java.io.IOException;
import java.util.HashSet;

/** 
 * Wrapper for the <a href="http://www.growl.info/">Growl</a> system which can 
 * be used on Mac OS X to display popup notifications.
 * <p>
 * The system is based on each application having a number of named 'notification'
 * types. Each type can be enabled or disabled by default, but the user can
 * change this via the Growl interface - individual application programs don't
 * need to provide an interface.
 * <p>
 * This pure Java implementation compiles, links, and runs on all platforms. 
 * Where Growl is not supported or installed, it does nothing. You can find
 * out whether Growl is in use via the {@link #getState()} method.
 * <p>
 * It uses AppleScript via the command-line 'osascript' utility which is 
 * available in all supported OS X versions. Each notification requires
 * a separate invocation of this process, meaning that it isn't very efficient
 * - but by the nature of these popups, you shouldn't be generating many each
 * second, so in practice any delay is no issue.
 * <p>
 * Example usage:
 * <pre>
 * String FUN_NOTIFICATION="Fun notification",
 *   BORING_NOTIFICATION="Boring notification";
 * GrowlWrapper gw=new GrowlWrapper("MyApp","Finder",
 *   new String[] {FUN_NOTIFICATION,BORING_NOTIFICATION},
 *   new String[] {FUN_NOTIFICATION});
 * gw.notify(FUN_NOTIFICATION,
 *   "Fun stuff","I bet you're glad you can see this!");
 * gw.notify(BORING_NOTIFICATION,
 *   "Boring stuff","I bet you regret seeing this!");
 * </pre>
 * Released publicly under the BSD license.
 * @author Samuel Marshall, <a target="_top" href="http://www.leafdigital.com/software/">leafdigital.com</a>
 */
class GrowlWrapper
{
    /** Growl application name */
    private String application;
    
    /** Available Growl notifications */
    private HashSet<String> notifications=new HashSet<String>();
    
    /** Wrapper state; one of the GROWL_xx constants */
    private int state;
    
    /** State: Growl is available and working */
    public final static int GROWL_OK=0;
    /** State: This computer is not running Mac OS X */
    public final static int GROWL_NOT_MAC=1;
    /** State: The 'osascript' program used to run AppleScript could not be found */
    public final static int GROWL_NO_APPLESCRIPT=2;
    /** State: An AppleScript error occurred, indicating that Growl is not available */
    public final static int GROWL_UNAVAILABLE=3;
    /** State: An unexpected error occurred during registration */
    public final static int GROWL_UNEXPECTED_ERROR=4;
    
    /**
     * Constructs a GrowlWrapper. (Note that you need to construct a new object
     * if you want to change any of these settings.)
     * @param application Name of application (for use in Growl settings)
     * @param applicationForIcon Name of application whose icon will be included
     *   in the popup (as defined by Apple; it's looking for the name part of a
     *   .app file you've run before) - may be null for no icon
     * @param allNotifications Array of strings corresponding to events that
     *   can be notified (as used in Growl settings)
     * @param defaultNotifications Array (must be a subset of the former) 
     *   of events that should be turned on by default
     * @throws IllegalArgumentException If a required parameter is null, or
     *   if one of the default notifications is not included in the 'all
     *   notifications' list
     */
    public GrowlWrapper(String application,String applicationForIcon,
        String[] allNotifications,String[] defaultNotifications)
      throws IllegalArgumentException
    {
        if(application==null || allNotifications==null || defaultNotifications==null)
            throw new IllegalArgumentException(
                "application,allNotifications and defaultNotifications are required parameters");
        if(allNotifications.length<1)
            throw new IllegalArgumentException(
                "Must specify at least one notification type");
        this.application=application;
        for(int i=0;i<allNotifications.length;i++)
        {
            notifications.add(allNotifications[i]);
        }
        for(int i=0;i<defaultNotifications.length;i++)
        {
            if(!notifications.contains(defaultNotifications[i]))
                throw new IllegalArgumentException("Default notifications must be "+
                    "included in the allNotifications array too");
        }
        
        state=GROWL_OK;
        
        // Test for Mac
        // Code from http://developer.apple.com/technotes/tn2002/tn2110.html
        String lcOSName = System.getProperty("os.name").toLowerCase();
        if(!lcOSName.startsWith("mac os x"))
        {
            state=GROWL_NOT_MAC;
        }
        else
        {
            // Create AppleScript process
            Process p=null;
            try
            {
                p=Runtime.getRuntime().exec("osascript");
            }
            catch(IOException e)
            {
                state=GROWL_NO_APPLESCRIPT;
            }
            
            if(state==GROWL_OK)
            {
                // Send the register script
                try
                {
                    if (p == null) {
                        return;
                    }
                    // Send script
                    p.getOutputStream().write(getScript(application,applicationForIcon,
                        allNotifications,defaultNotifications,
                    null,null,null).getBytes("UTF-8"));
                    // Close stdin and wait for process to complete
                    p.getOutputStream().close();
                    try
                    {
                        p.waitFor();
                    }
                    catch(InterruptedException e)
                    {
                    }
                    // Check if there is anything on stderr (shouldn't be)
                    if(p.getErrorStream().read()!=-1)
                        state=GROWL_UNAVAILABLE;
                }
                catch(IOException e)
                {
                    // By the time it gets to here, not really expecting problems of this
                    // type as they should have shown up earlier
                    state=GROWL_UNEXPECTED_ERROR;
                }
            }
        }       
    }
    
    /**
     * Obtains the state of the wrapper.
     * @return One of the GROWL_xx constants
     */
    public int getState()
    {
        return state;
    }
    
    /**
     * Generates a notification popup. 
     * @param notification Notification event ID, as previously supplied to the 
     *   constructor  
     * @param title Title for popup
     * @param description Text for popup (can include line breaks)
     * @throws IllegalArgumentException If the notification wasn't defined,
     *   or if a parameter is null
     */
    public void notify(String notification,String title,String description)
      throws IllegalArgumentException
    {
        // Check parameters
        if(notification==null || title==null || description==null)
            throw new IllegalArgumentException("Parameters may not be null");
        if(!notifications.contains(notification))
            throw new IllegalArgumentException("Unknown notification '"+notification+"'");
        
        // Do nothing if Growl is not running
        if(state!=GROWL_OK) return;
        
        try
        {
            // Run process and send script
            Process p=Runtime.getRuntime().exec("osascript");
            p.getOutputStream().write(getScript(application,null,null,null,
                notification,title,description).getBytes("UTF-8"));         
            p.getOutputStream().close();
            
            // No need to wait for process to exit as we don't care what the
            // response is
        }
        catch(IOException e)
        {
            // Ignore errors - any likely problems should have been detected
            // in constructor
        }
    }
    
    /**
     * Quotes a string for inclusion in AppleScript.
     * @param input String
     * @return String surrounded by double quotes and with necessasry backslashes
     */
    private static String appleScriptQuote(String input) 
    {
        StringBuffer sb=new StringBuffer("\"");
        for(int i=0;i<input.length();i++)
        {
            char c=input.charAt(i);
            switch(c)
            {
            case '"' : sb.append("\\\""); break;
            case '\n' : sb.append("\\n"); break;
            case '\\' : sb.append("\\\\"); break;
            default: sb.append(c);
            }
        }
        sb.append('"');
        return sb.toString();
    }

    /**
     * Internal code which builds up a suitable AppleScript based on the
     * example on the Growl site. This can both register and generate an
     * event.
     * @param application Name of application (for Growl prefs)
     * @param applicationForIcon Name of application to use for icon, or null if none
     *   (may be null if not registering)
     * @param allNotifications Array of notification names or null if not registering
     * @param defaultNotifications Array of notification names or null if not registering
     * @param notification Name of notification to generate or null if not generating
     * @param title Title of popup or null if not generating
     * @param description Text of popup or null if not generating
     * @return AppleScript in a string
     */
  private static String getScript(
    String application,String applicationForIcon,
    String[] allNotifications,String[] defaultNotifications,
    String notification,String title,String description)
  {
    StringBuffer sb=new StringBuffer();
    sb.append("tell application \"GrowlHelperApp\"\n");
    if(allNotifications!=null)
    {
        sb.append("register as application ");
        sb.append(appleScriptQuote(application));
        sb.append(" all notifications {");
        for(int i=0;i<allNotifications.length;i++)
        {
            if(i>0) sb.append(',');
            sb.append(appleScriptQuote(allNotifications[i]));
        }
        sb.append("} default notifications {");
        for(int i=0;i<defaultNotifications.length;i++)
        {
            if(i>0) sb.append(',');
            sb.append(appleScriptQuote(defaultNotifications[i]));
        }
        if(applicationForIcon!=null)
        {
            sb.append("} icon of application ");
            sb.append(appleScriptQuote(applicationForIcon));
        }
        else
        {
            sb.append("}");
        }
    }
    if(notification!=null)
    {
        sb.append("\nnotify with name");
        sb.append(appleScriptQuote(notification));
        sb.append(" title ");
        sb.append(appleScriptQuote(title));
        sb.append(" description ");
        sb.append(appleScriptQuote(description));
        sb.append(" application name ");
        sb.append(appleScriptQuote(application));
    }
    sb.append("\nend tell\n");
    return sb.toString();
  }
}
