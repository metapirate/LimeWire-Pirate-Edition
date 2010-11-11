package org.limewire.ui.swing.util;

import java.awt.Component;
import java.util.HashMap;
import java.util.Map;

import org.limewire.util.OSUtils;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.win32.W32APIFunctionMapper;
import com.sun.jna.win32.W32APITypeMapper;

import foxtrot.Job;
import foxtrot.Worker;

/**
 * A modal Folder chooser dialog box. This uses native calls in JNA
 * to load a native folder chooser on Windows systems. This class will not
 * run on other platforms.
 */
public class WindowsFolderChooser {

    //see http://msdn.microsoft.com/en-us/library/bb773205(VS.85).aspx 
    //for documentation on these flags and the BROWSEINFO struct
    private final static int BIF_RETURNONLYFSDIRS = 1;
    private final static int BIF_EDITBOX = 16;
    private final static int BIF_NEWDIALOGSTYLE = 64;
    
    //flags for events within the Folder Dialog
    private static final int BFFM_INITIALIZED = 1;
    private static final int BFFM_SETSELECTION = 1024 + 103;
    
    /**
     * Handle to the window shell.
     */
    private Shell32 shell32;
    
    /**
     * Handle to the Structure that contains information about the Folder Chooser.
     */
    private Shell32.BROWSEINFO info;
    
    /**
     * Creates a native Folder Chooser structure.
     * 
     * @param msg the message to show the user.
     * @param showEditBox true shows the edit box below the tree
     * @param allowNewFolder true allows users to create a new folder in this dialog
     * @param currentDirectory the folder to show selected
     */
    public WindowsFolderChooser(Component component, String msg, boolean showEditBox, boolean allowNewFolder, final String currentDirectory){
        
        // listens for callback events from changes within the Folder Chooser.
        BrowseInfoCallback proc = new BrowseInfoCallback() {

            /**
             * When the Folder Chooser is initialized, expand and select the given
             * directory.
             */
            @Override
            public int callback(Pointer wnd, int msg, int param, int lpData) {
                if(msg == BFFM_INITIALIZED && currentDirectory != null) {
                    User32.INSTANCE.PostMessage(wnd, BFFM_SETSELECTION, 1, currentDirectory);
                }
                return 0;
            }
        };

        shell32 = Shell32.INSTANCE;
        
        // set visiblity flags for various components within the dialog
        int flags = showEditBox ? BIF_EDITBOX : 0;
        flags = flags | (allowNewFolder ? BIF_NEWDIALOGSTYLE : 0);
        flags = flags | BIF_RETURNONLYFSDIRS;

        // create the structure that holds the Folder Chooser information
        info = new Shell32.BROWSEINFO();
        info.lpszTitle = msg;
        info.hwndOwner = Native.getComponentPointer(component);
        info.lpfn = proc;
        info.ulFlags = flags;
    }
    
    /**
     * Makes the native Folder Chooser visible.
     * 
     * @return a string containing the absolute path the user choose
     */
    public String showWidget() {
        String returnPath = (String)Worker.post(new Job() {
            @Override
            public Object run() {
                byte[] path = new byte[OSUtils.getMaxPathLength()];
                
                Pointer ptr = shell32.SHBrowseForFolder(info);
                
                // get path selected by the user
                shell32.SHGetPathFromIDList(ptr, path);
                String returnPath = Native.toString(path);
                
                // Dispose of the return path structure
                Ole32.INSTANCE.CoTaskMemFree(ptr);
                
                return returnPath;
            }
        });

        return returnPath;
    }

    /**
     * An Java interface for the Shell32.dll library.
     */
    private interface Shell32 extends Library  {
        Shell32 INSTANCE = (Shell32) Native.loadLibrary("shell32", Shell32.class);

        // A Java version of the C++ struct used for this dialog
        // http://msdn.microsoft.com/en-us/library/bb773205(VS.85).aspx
        class BROWSEINFO extends Structure {
            @SuppressWarnings("unused") public Pointer hwndOwner;
            @SuppressWarnings("unused") public Pointer pidlRoot;
            @SuppressWarnings("unused") public Pointer pszDisplayName;
            @SuppressWarnings("unused") public String lpszTitle;
            @SuppressWarnings("unused") public int ulFlags;
            @SuppressWarnings("unused") public BrowseInfoCallback lpfn;
            @SuppressWarnings("unused") public Pointer lParam;
            @SuppressWarnings("unused") public int iImage;
        }
    
        /**
         * Displays a dialog box that enables the user to select a folder.
         */
        Pointer SHBrowseForFolder(BROWSEINFO info);
        
        /**
         * Converts an item identifier list to a file system path.
         */
        Boolean SHGetPathFromIDList(Pointer idl, byte[] path);
    }
    
    /**
     * A Java Interface for the Ole32 dll library.
     */
    private interface Ole32 extends Library {
        Ole32 INSTANCE = (Ole32) Native.loadLibrary("ole32", Ole32.class);
       
        /**
         * Frees memory used by the given pointer
         */
        void CoTaskMemFree(Pointer pointer);
    }

    /**
     * A Java interface for User32 dll library.
     */
    private interface User32 extends StdCallLibrary {
        /** Standard options to use the unicode version of a w32 API. */
        @SuppressWarnings("unchecked")
        Map UNICODE_OPTIONS = new HashMap() {
            {
                put(OPTION_TYPE_MAPPER, W32APITypeMapper.UNICODE);
                put(OPTION_FUNCTION_MAPPER, W32APIFunctionMapper.UNICODE);
            }
        };
        
        /** Standard options to use the ASCII/MBCS version of a w32 API. */
        @SuppressWarnings("unchecked")
        Map ASCII_OPTIONS = new HashMap() {
            {
                put(OPTION_TYPE_MAPPER, W32APITypeMapper.ASCII);
                put(OPTION_FUNCTION_MAPPER, W32APIFunctionMapper.ASCII);
            }
        };
        Map DEFAULT_OPTIONS = Boolean.getBoolean("w32.ascii") ? ASCII_OPTIONS : UNICODE_OPTIONS;

        
        User32 INSTANCE = (User32) Native.loadLibrary("user32", User32.class, DEFAULT_OPTIONS);

        /**
         * Posts a message within the message queue of the associated thread that
         * created the given window and returns immediately.
         */
        void PostMessage(Pointer pointer, int msg, int wParam, String lParam) ;
    }
    
    
    /**
     * A callback function for receiving change events from a Dialog.
     */
    private interface BrowseInfoCallback extends StdCallLibrary.StdCallCallback {
        
        /**
         * The dialog box calls this function when an event occurs
         */
        int callback(Pointer hWnd , int uMsg, int lParam, int lpData);        
    }
    
}
