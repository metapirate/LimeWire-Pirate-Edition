package org.limewire.ui.swing.util;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileSystemView;
import javax.swing.filechooser.FileView;
import javax.swing.plaf.FileChooserUI;

import org.limewire.collection.FixedsizeForgetfulHashMap;
import org.limewire.collection.FixedsizeForgetfulHashSet;
import org.limewire.concurrent.ExecutorsHelper;
import org.limewire.core.api.Category;
import org.limewire.core.api.file.CategoryManager;
import org.limewire.core.settings.SharingSettings;
import org.limewire.ui.swing.settings.SwingUiSettings;
import org.limewire.util.FileUtils;
import org.limewire.util.OSUtils;
import org.limewire.util.SystemUtils;
import org.limewire.util.SystemUtils.SpecialLocations;

import com.google.inject.Inject;


/** A FileIconController that attempts to return native icons. */
public class NativeFileIconController implements FileIconController {
    
    /** The view that retrieves the icon from the filesystem. */
    private final SmartFileView VIEW;
    
    /** A mapping from String (extension) to Icon. */
    private final Map<String, Icon> EXTENSIONS = new HashMap<String, Icon>();
    
    /**
     * A marker null icon so we don't create a file everytime
     * if the icon can't be found.
     */
    private final Icon NULL = new ImageIcon();
    
    private final CategoryManager categoryManager;
    
    /**
     * Constructs the NativeFileIconController.
     * This constructor may block as the JFileChooser
     * is constructed.
     */
    @Inject
    NativeFileIconController(CategoryManager categoryManager) {
        this.categoryManager = categoryManager;
        SmartFileView view = getNativeFileView();
        if(view == null) {
            VIEW = null;
        } else {
            VIEW = new DelegateFileView(view);
            if(SwingUiSettings.PRELOAD_NATIVE_ICONS.getValue())
                preload();
        }
    }
    
    /** Returns true if loading succeeded and the view hasn't screwed up. */
    public boolean isValid() {
        return VIEW != null && VIEW.isViewAvailable();
    }
    
    /** Returns true if we have requested this file recently. */
    public boolean isIconForFileAvailable(File f) {
        return VIEW.isIconCached(f);
    }
    
    /**
     * Retrieves the native FileView.
     */
    private SmartFileView getNativeFileView() {
        // Deadlocks happen on Windows when using file-chooser based view.
        if(OSUtils.isWindows())
            return constructFSVView();
        else 
            return constructFileChooserView();
    }
    
    /**
     * Constructs a JFileChooser-based FileView.
     * This doesn't work consistently on Java 1.5.0_10 on Windows,
     * because it deadlocks if called outside of the Swing thread.
     * @return
     */
    private SmartFileView constructFileChooserView() {
        // This roundabout way of getting the FileView is necessary for the
        // following reasons:
        // 1) We need the native UI's FileView to get the correct icons,
        //    because the Metal UI's icons are terrible.
        // 2) We cannot just call getFileView(chooser) once retrieving the
        //    native UI, because FileChooserUI tends to delegate calls
        //    to the JFileChooser, and it seems to require that it
        //    the UI be set on the chooser.
        // 3) setUI is a protected method of JFileChooser (of JComponent),
        //    so we need to have the anonymous class with an extended
        //    constructor.
        // 4) Even after constructing the JFileChooser, using getIcon on it
        //    doesn't work well, so we need to do it directly on the FileView.
        // 5) In order to get the correct file view, it needs to be explicitly
        //    set, otherwise it reverts to the UI's FileView, using UIManager,
        //    which may actually be a different UI.
        // 6) The NullPointerException must be caught because sometimes
        //    the Windows JFileChooser throws an NPE while constructing.
        //    The ArrayIndexOutOfBoundsException is a workaround also,
        //    as that error seems to be thrown occasionally in Java 1.6.
        
        JFileChooser chooser = null;        
        // If after 10 times we still can't set it just give up.
        for(int i = 0; i < 10; i++) {
            try {
                chooser = new JFileChooser() {
                    {
                        FileChooserUI ui =
                            (FileChooserUI)GuiUtils.getNativeUI(this);
                        setUI(ui);
                        setFileView(ui.getFileView(this));
                    }
                };
            } catch(NullPointerException ignored) {
            } catch(ArrayIndexOutOfBoundsException ignored) {
            }
            
            if(chooser != null)
                break;
        }
        
        if(chooser == null) {
            return null;
        } else {
            return new SmartChooserView(chooser.getFileView());
        }        
    }
    
    /**
     * Constructs a FileSystemView-based FileView.
     * Just to be safe, we do this on the Swing thread, since we've seen
     * deadlocks when constructing JFileChooser ones outside the Swing thread.
     */
    public SmartFileView constructFSVView() {
        final AtomicReference<SmartFileView> ref = new AtomicReference<SmartFileView>();
        SwingUtils.invokeNowOrWait(new Runnable() {
            public void run() {
                try {
                    ref.set(new FSVFileView());
                } catch(Throwable err) {
                    // If an error constructing FSF view, ignore.
                    ref.set(null);
                }
            }
        });
        return ref.get();
    }
    
    /**
     * Returns the native file icon for a file, if it exists.
     * If it doesn't exist, returns the icon for the extension of the file.
     */
    public Icon getIconForFile(File f) {
        if (f == null)
            return null;

        // We return the icon if it was previously cached,
        // or if it exists.  We cannot get a nonexistant (non-cached)
        // file from the view, otherwise they'll be spurious exceptions.
        if(VIEW.isIconCached(f) || f.exists()) {
            return VIEW.getIcon(f);
        } else {
            String extension = FileUtils.getFileExtension(f);
            if (!extension.isEmpty())
                return getIconForExtension(extension);
            else
                return null;
        }
    }
     
    /**
     * Returns the icon associated with the extension.
     */ 
     /* TODO: Implement better.
     */
    public Icon getIconForExtension(String ext) {
        ext = ext.trim().toLowerCase();
        Icon icon = EXTENSIONS.get(ext);
        // If we already had a cached icon..
        if(icon != null) {
            // So long as it wasn't the NULL marker,
            // return that icon.
            if(icon != NULL)
                return icon;
            else 
                return null;
        }

        // If we don't know the icon for this extension yet,
        // then create a temporary file, get icon, cache it,
        // and return it.
        File dir = SharingSettings.INCOMPLETE_DIRECTORY.get();
        File tmp = new File(dir, ".LimeWireIconFinder." + ext);
        if(tmp.exists()) {
            icon = VIEW.getIcon(tmp);
        } else {
            try {
                FileUtils.touch(tmp);
                icon = VIEW.getIcon(tmp);
                if(icon == null)
                    icon = NULL;
            } catch(IOException fnfe) {
                icon = NULL;
            }
        }
        
        tmp.delete();
        EXTENSIONS.put(ext, icon);
        VIEW.removeFromCache(tmp); // do not need to keep it around.
        
        return icon;
    }
    
    
    /**
     * Preloads a bunch of icons.
     */
    private void preload() {
        ExecutorService queue = ExecutorsHelper.newProcessingQueue("IconLoader");
        Category[] categories = Category.values();
        final AtomicBoolean continueLoading = new AtomicBoolean(true);
        for(int i = 0; i < categories.length && continueLoading.get(); i++) {
            final Collection<String> exts = categoryManager.getExtensionsForCategory(categories[i]);
            for(Iterator<String> j = exts.iterator(); j.hasNext() && continueLoading.get(); ) {
                final String next = j.next();
                queue.execute(new Runnable() {
                    public void run() {
                        SwingUtils.invokeNowOrWait(new Runnable() {
                            public void run() {
                                getIconForExtension(next);
                                if(!VIEW.isViewAvailable())
                                    continueLoading.set(false);
                            }
                        });
                    }
                });
            }
        }
    }
    
    /** A smarter FileView. */
    private static abstract class SmartFileView extends FileView {
        
        /** Checks to see if the given icon is cached. */
        public abstract boolean isIconCached(File f);
        
        /** Removes a file from the cache, if possible. */
        public abstract boolean removeFromCache(File f);
        
        /** Determines if this view is working.  By default, returns true. */
        public boolean isViewAvailable() {
            return true;
        }
    }
    
    /**
     * Delegates to another FileView, catching NPEs & UnsatisfiedLinkErrors.
     *
     * The NPE catching is required because of poorly built methods in
     * javax.swing.filechooser.FileSystemView that print true
     * exceptions to System.err and return null, instead of
     * letting the exception propagate.
     * <p>
     * The ULE catching is required because of strange Swing errors
     * that can't find the native code to:
     *  sun.awt.shell.Win32ShellFolder2.getFileSystemPath(I)Ljava/lang/String;     * 
     * See: LWC-1174.
     */
    private static class DelegateFileView extends SmartFileView {
        private final SmartFileView DELEGATE;
        private boolean linkFailed = false;
        
        DelegateFileView(SmartFileView real) {
            DELEGATE = real;
        }
        
        @Override
        public boolean isViewAvailable() {
            return !linkFailed;
        }
        
        @Override
        public Icon getIcon(final File f) {
            try {
                Icon icon = DELEGATE.getIcon(f);
                return icon;
            } catch(NullPointerException npe) {
                return null;
            } catch(UnsatisfiedLinkError ule) {
                linkFailed = true;
                return null;
            }
        }
        
        @Override
        public String getDescription(File f) {
            try {
                return DELEGATE.getDescription(f);
            } catch(NullPointerException npe) {
                return null;
            } catch(UnsatisfiedLinkError ule) {
                linkFailed = true;
                return null;
            }
        }
        
        @Override
        public String getName(File f) {
            try {
                return DELEGATE.getName(f);
            } catch (NullPointerException npe) {
                return null;
            } catch(UnsatisfiedLinkError ule) {
                linkFailed = true;
                return null;
            }
        }

        @Override
        public String getTypeDescription(File f) {
            try {
                return DELEGATE.getTypeDescription(f);
            } catch (NullPointerException npe) {
                return null;
            } catch(UnsatisfiedLinkError ule) {
                linkFailed = true;
                return null;
            }
        }

        @Override
        public Boolean isTraversable(File f) {
            try {
                return DELEGATE.isTraversable(f);
            } catch (NullPointerException npe) {
                return null;
            } catch(UnsatisfiedLinkError ule) {
                linkFailed = true;
                return null;
            }
        }

        @Override
        public boolean isIconCached(File f) {
            return DELEGATE.isIconCached(f);
        }

        /** Does nothing. */
        @Override
        public boolean removeFromCache(File f) {
            return DELEGATE.removeFromCache(f);
        }
    }
    
    /**
     * A FileSystemView FileView.
     */
    private static class FSVFileView extends SmartFileView {
        private final FileSystemView VIEW = FileSystemView.getFileSystemView();        
        private final Map<File, Icon> CACHE = new FixedsizeForgetfulHashMap<File, Icon>(50000);
        
        FSVFileView() {
            // Explicitly put the icon for roots as gotten from their roots description.
            // For some reason, getting the icon for the desktop requires that you pass
            // the File as retrieved from getRoots instead of a file at the desktop's
            // location on FS.
            File[] roots = VIEW.getRoots();
            for(int i = 0; i < roots.length; i++) {
                CACHE.put(new File(roots[i].getPath()), VIEW.getSystemIcon(roots[i]));
            }
            
            // Similarly, with the My Documents folder, the icon is only valid if it's
            // retrieved as the child from the root.
            // Do not execute this code on windows 7. The default logic works fine. 
            // On windows 7 this breaks because View.getRoots only returns one root,
            // the path to the Desktop folder.
            // We bypass this code on Windows Vista.  It fails because the 
            // Documents folder no longer has a desktop icon on Vista.
            if(OSUtils.isWindows() && !OSUtils.isWindowsVista() && !OSUtils.isWindows7() && roots.length == 1) {
                String path = SystemUtils.getSpecialPath(SpecialLocations.DOCUMENTS);
                if(path != null) {
                    File documents = new File(path);
                    File child = VIEW.getChild(roots[0], documents.getName());
                    if(child != null) {
                        CACHE.put(documents, VIEW.getSystemIcon(child));
                    }
                }
            }
        }
        
        @Override
        public String getDescription(File f) {
            return VIEW.getSystemTypeDescription(f);
        }

        @Override
        public Icon getIcon(final File f) {
            Icon icon = CACHE.get(f);
            if(icon == null) {
                icon = VIEW.getSystemIcon(f);
                CACHE.put(f, icon);
            }
            return icon;
        }

        @Override
        public String getName(File f) {
            return VIEW.getSystemDisplayName(f);
        }

        @Override
        public String getTypeDescription(File f) {
            return VIEW.getSystemTypeDescription(f);
        }

        @Override
        public Boolean isTraversable(File f) {
            return VIEW.isTraversable(f);
        }

        @Override
        public boolean isIconCached(File f) {
            return CACHE.containsKey(f);
        }

        /** Removes the given file from the cache. */
        @Override
        public boolean removeFromCache(File f) {
            return CACHE.remove(f) != null;
        }
    }
    
    /**
     * A wrapper around JFileChooser's view that returns true for caching
     * once a file has been looked up.
     */
    private static class SmartChooserView extends SmartFileView {
        /** The view this uses. */
        private final FileView DELEGATE;
        
        /** A set of the most recently requested Files. */
        private final Set<File> CACHE = new FixedsizeForgetfulHashSet<File>(5000, 1000); 

        public SmartChooserView(FileView delegate) {
            DELEGATE = delegate;
        }

        @Override
        public String getDescription(File f) {
            return DELEGATE.getDescription(f);
        }

        @Override
        public Icon getIcon(File f) {
            CACHE.add(f);
            return DELEGATE.getIcon(f);
        }

        @Override
        public String getName(File f) {
            return DELEGATE.getName(f);
        }

        @Override
        public String getTypeDescription(File f) {
            return DELEGATE.getTypeDescription(f);
        }

        @Override
        public Boolean isTraversable(File f) {
            return DELEGATE.isTraversable(f);
        }

        @Override
        public boolean isIconCached(File f) {
            return CACHE.contains(f);
        }

        @Override
        public boolean removeFromCache(File f) {
            return CACHE.remove(f);
        }
    }
}
