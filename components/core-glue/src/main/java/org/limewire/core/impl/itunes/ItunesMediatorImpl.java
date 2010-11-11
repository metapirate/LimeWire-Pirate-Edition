package org.limewire.core.impl.itunes;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.concurrent.ExecutorsHelper;
import org.limewire.core.settings.iTunesSettings;
import org.limewire.external.itunes.windows.com.IITLibraryPlaylist;
import org.limewire.external.itunes.windows.com.IITOperationStatus;
import org.limewire.external.itunes.windows.com.IiTunes;
import org.limewire.util.FileUtils;
import org.limewire.util.OSUtils;

import com.google.inject.Singleton;
import com.jacob.activeX.ActiveXComponent;
import com.jacob.com.ComFailException;
import com.jacob.com.ComThread;
import com.jacob.com.Dispatch;

/**
 * Handles sending completed downloads into iTunes.
 */
@Singleton
public final class ItunesMediatorImpl implements ItunesMediator {

    private static final Log LOG = LogFactory.getLog(ItunesMediatorImpl.class);

    /**
     * The queue that will process the tunes to add.
     */
    private final ExecutorService QUEUE = ExecutorsHelper.newProcessingQueue("iTunesAdderThread");

    @Override
    public void addSong(File file) {

        // If not on Windows or OSX don't do anything.
        if (!OSUtils.isWindows() && !OSUtils.isMacOSX())
            return;

        // Make sure we convert any uppercase to lowercase or vice versa.
        try {
            file = FileUtils.getCanonicalFile(file);
        } catch (IOException ignored) {
        }

        // Verify that we're adding a real file.
        if (!file.exists()) {
            if (LOG.isDebugEnabled())
                LOG.debug("File: '" + file + "' does not exist");
            return;
        }

        File[] files;
        if (file.isDirectory()) {
        	files = FileUtils.getFilesRecursive(file, 
        			iTunesSettings.ITUNES_SUPPORTED_FILE_TYPES.get());
        } else if (file.isFile() && isSupported(FileUtils.getFileExtension(file)))
            files = new File[] { file };
        else
            return;

        for (File toAdd : files) {
            if (LOG.isTraceEnabled())
                LOG.trace("Will add '" + toAdd + "' to Playlist");

            if (OSUtils.isWindows())
                QUEUE.execute(new AddFileToWindowsITunes(toAdd));
            else if (OSUtils.isMacOSX())
                QUEUE.execute(new ExecOSAScriptCommand(toAdd));
        }
    }

    /**
     * Returns true if the extension of name is a supported file type.
     */
    private static boolean isSupported(String extension) {
        if (extension.isEmpty())
            return false;
        String[] types = iTunesSettings.ITUNES_SUPPORTED_FILE_TYPES.get();
        for (int i = 0; i < types.length; i++)
            if (extension.equalsIgnoreCase(types[i]))
                return true;
        return false;
    }

    /** Uses JACOB and the iTunes COM SDK to add files to iTunes. */
    private static class AddFileToWindowsITunes implements Runnable {
        private static final String ITUNES_ACTIVEX_NAME = "iTunes.Application";

        /**
         * Set to a value the first time this class executes an add operation.
         * If false, all subsequent adds will be ignored.
         */
        private static Boolean ready = null;

        private final File file;

        /**
         * Constructs a new AddFileToWindowsITunes for the specified file.
         */
        public AddFileToWindowsITunes(File file) {
            this.file = file;
        }

        public void run() {
            try {
                addFileToLibrary(file);
            } catch (com.jacob.com.ComFailException e) {
                LOG.warn("Error adding file to itunes library", e);
            } catch (IllegalStateException e) {
                LOG.warn("Error adding file to itunes library", e);
            }
        }

        /**
         * @return true if:
         *         <li>The JACOB library is correctly linked
         *         <li>iTunes seems to be responsive
         */
        public synchronized boolean isReady() {
            if (ready != null)
                return ready;
            // Check for JACOB & iTunes
            try {
                ComThread.InitMTA();
                try {
                    // Check for iTunes
                    new ActiveXComponent(ITUNES_ACTIVEX_NAME);
                } finally {
                    ComThread.Release();
                }
            } catch (UnsatisfiedLinkError ex) {
                // JACOB is not in the java.library.path
                LOG.error("JACOB is not in the java.library.path.", ex);
                ready = Boolean.FALSE;
                return false;
            } catch (ComFailException ex) {
                // iTunes ActiveX wasn't available...
                LOG.error("iTunes ActiveX component not found.", ex);
                ready = Boolean.FALSE;
                return false;
            }

            ready = Boolean.TRUE;
            return true;
        }

        /**
         * Does the heavy lifting, activating COM and inserting the file into
         * the iTunes DB. Does not add duplicates.
         */
        private boolean addFileToLibrary(File newFile) {
            if (!isReady())
                return false;
            ComThread.InitMTA();
            try {
                ActiveXComponent iTunesCom = new ActiveXComponent(ITUNES_ACTIVEX_NAME);
                Dispatch iTunesController = iTunesCom.getObject();
                IiTunes it = new IiTunes(iTunesController);
                IITLibraryPlaylist pl = it.getLibraryPlaylist();
                // Add the file
                IITOperationStatus status = pl.addFile(newFile.getAbsolutePath());
                if (status == null)
                    return false;
                while (status.getInProgress()) {
                    // Waiting for add operation to complete (This is usually
                    // instantaneous)...
                    try {
                        TimeUnit.MILLISECONDS.sleep(10);
                    } catch (InterruptedException ignored) {
                        break;
                    }
                }
            } finally {
                ComThread.Release();
            }
            return true;
        }
    }

    /**
     * Executes the osascript CLI command (Mac OSX)
     */
    private static class ExecOSAScriptCommand implements Runnable {
        /**
         * The file to add.
         */
        private final File file;

        /**
         * Constructs a new ExecOSAScriptCommand for the specified file.
         */
        public ExecOSAScriptCommand(File file) {
            this.file = file;
        }

        /**
         * Constructs and returns a osascript command.
         */
        private String[] createOSAScriptCommand(File file) {
            String path = file.getAbsolutePath();
            String playlist = iTunesSettings.ITUNES_PLAYLIST.get();

            String[] command = new String[] { "osascript", "-e", "tell application \"Finder\"",
                    "-e",
                    "set hfsFile to (POSIX file \"" + path + "\")",
                    "-e",
                    "set thePlaylist to \"" + playlist + "\"",
                    "-e",
                    "tell application \"iTunes\"",
                    // "-e", "activate", // launch and bring to front
                    "-e",
                    "launch", // launch in background
                    "-e", "if not (exists playlist thePlaylist) then", "-e",
                    "set thisPlaylist to make new playlist", "-e",
                    "set name of thisPlaylist to thePlaylist", "-e", "end if", "-e",
                    "add hfsFile to playlist thePlaylist", "-e", "end tell", "-e", "end tell" };

            return command;
        }

        /**
         * Runs the osascript command
         */
        public void run() {
            try {
                Runtime.getRuntime().exec(createOSAScriptCommand(file));
            } catch (IOException err) {
                LOG.debug(err);
            }
        }
    }
}
