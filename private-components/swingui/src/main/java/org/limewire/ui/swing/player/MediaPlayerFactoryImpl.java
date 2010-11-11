package org.limewire.ui.swing.player;

import java.awt.Canvas;
import java.awt.Container;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import javax.media.IncompatibleSourceException;
import javax.media.MediaLocator;
import javax.media.Player;
import javax.media.protocol.DataSource;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.limewire.inject.LazySingleton;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.player.api.AudioPlayer;
import org.limewire.util.ExceptionUtils;
import org.limewire.util.OSUtils;

import com.google.inject.Inject;
import com.google.inject.Provider;

@LazySingleton
class MediaPlayerFactoryImpl implements MediaPlayerFactory {

    private static final Log LOG = LogFactory.getLog(MediaPlayerFactoryImpl.class);
    
    private final Provider<AudioPlayer> player;
    
    private final List<String> handlers = new ArrayList<String>(2);
    
    private final JPanel p = new JPanel();
        
    @Inject
    public MediaPlayerFactoryImpl(Provider<AudioPlayer> player) {
        this.player = player;
        
        registerHandlers();
    }
    
    private void registerHandlers() {
        if(OSUtils.isWindows()) {
            handlers.add("net.sf.fmj.ds.media.content.unknown.Handler");
        } else if(OSUtils.isMacOSX()) {
            handlers.add("net.sf.fmj.qt.media.content.unknown.JavaToCocoaHandler");
            handlers.add("net.sf.fmj.qt.media.content.unknown.QuickTimeForJavaHandler");
        }
    }
    
    public Player createMediaPlayer(File file, final Container parentComponent) throws IncompatibleSourceException {
        if (!OSUtils.isWindows() && !OSUtils.isMacOSX()) {
            throw new IllegalStateException("Video is only supported on Windows and Mac");
        }

        Player handler = null;
        
        //attempt to cycle through the list of player handles. If one fails
        // attempt loading file with the next one
        for(String handle : handlers) {
            try {
                if(LOG.isDebugEnabled())
                    LOG.debug("loading " + handle);
                Class clazz = Class.forName(handle);
                Player player = (Player)clazz.newInstance();
                setupPlayer(player, file);
                return player;
            } catch (IncompatibleSourceException e) {
            } catch (ClassNotFoundException e) {
            } catch (InstantiationException e) {
            } catch (IllegalAccessException e) {
            }
        }
        
        if(OSUtils.isWindows7()) {
            try { 
                if(LOG.isDebugEnabled())
                    LOG.debug("loading MediaFoundationPlayer");
                Player player = createWindows7MFPlayer(file, parentComponent);
                setupPlayer(player, file);
                return player;
            } catch (IncompatibleSourceException e) {
            }
        }
        if(LOG.isDebugEnabled())
            LOG.debug("Loading default java player");
        
        // default to the java sound player if all else fails
        handler = new JavaSoundPlayer(player);
        setupPlayer(handler, file);
        parentComponent.add(p);
        
        return handler;
    }
    
    /**
     * Creates a MF Player on Windows 7. Unlike other players, a Container must created and 
     * realized before the player is created since it requires a handle to the Container. 
     * As a result we wait and add the Canvas prior to attempting to create the Player but 
     * only for MFPlayer.
     */
    private Player createWindows7MFPlayer(File file, final Container parentComponent) throws IncompatibleSourceException {
         //DS failed.  Now we try MF.
         final AtomicReference<Canvas> mfCanvas = new AtomicReference<Canvas>(); 
         try {
             //create new canvas and add to parentComponent so we can get an hwnd
             SwingUtilities.invokeAndWait(new Runnable() {
                 @Override
                 public void run() {
                     Canvas canvas = new Canvas();
                     parentComponent.add(canvas);
                     //addNotify to make sure we have a working hwnd
                     parentComponent.addNotify();
                     mfCanvas.set(canvas);
                 }
             });
         } catch (InterruptedException e) {
             throw new IncompatibleSourceException(e.toString() + " \n" + ExceptionUtils.getStackTrace(e));
         } catch (InvocationTargetException e) {
             throw new IncompatibleSourceException(e.toString() + " \n" + ExceptionUtils.getStackTrace(e));
         }
         
         final Player handler = new net.sf.fmj.mf.media.content.unknown.Handler(mfCanvas.get());
         
         SwingUtilities.invokeLater(new Runnable() {
             public void run() {
                 parentComponent.setPreferredSize(mfCanvas.get().getPreferredSize());
             }
         });
        return handler;
     
    }
    
    /**
     * Attempts to set the source on the player and realize it. After a player is realized 
     * the player is ready to use. If the realization fails an IncompatibleSourceException
     * is thrown.
     */
    private void setupPlayer(Player player, File file) throws IncompatibleSourceException {
        try {
            player.setSource(createDataSource(file));
        } catch (IOException e) {
            throw new IncompatibleSourceException(e.toString() + " \n" + ExceptionUtils.getStackTrace(e));
        } catch (UnsatisfiedLinkError e) {
            throw new IncompatibleSourceException(e.toString() + " \n" + ExceptionUtils.getStackTrace(e));
        }
        player.realize();
    }
    
    /**
     * Creates a DataSource from the file for use with the Player.
     */
    private DataSource createDataSource(File file) throws MalformedURLException{
        //FMJ's handling of files is incredibly fragile.  This works with both quicktime and directshow.
        DataSource source = new net.sf.fmj.media.protocol.file.DataSource();
        if (OSUtils.isMacOSX()) {
            // On OS-X escaping characters such as spaces, parenthesis, etc are causing the 
            // files not to be loaded by FMJ. So let's not escape any characters. 
            // NOTE: only three backslashes since absolute path returns one. Need 4 to escape
            // the string!!
            source.setLocator(new MediaLocator("file:///" + file.getAbsolutePath()));
        } else {
            // This produces a URL with the form file:/path rather than file:///path
            // This URL form is accepted on Windows but not OS X.
            String urlString = file.toURI().toURL().toExternalForm();
            source.setLocator(new MediaLocator(urlString));
        }
        return source;
    }
}
