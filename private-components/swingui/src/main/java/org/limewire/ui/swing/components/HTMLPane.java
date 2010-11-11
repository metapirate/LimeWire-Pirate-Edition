package org.limewire.ui.swing.components;

import java.awt.EventQueue;
import java.awt.Insets;
import java.awt.Rectangle;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.JEditorPane;
import javax.swing.SwingUtilities;
import javax.swing.text.Document;
import javax.swing.text.EditorKit;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;

import org.limewire.concurrent.ExecutorsHelper;
import org.limewire.concurrent.ListeningExecutorService;
import org.limewire.concurrent.ListeningFuture;
import org.limewire.ui.swing.util.SwingUtils;
import org.limewire.util.ExceptionUtils;

/**
 * An editor pane that forces synchronous page loading unless you use the
 * {@link #setPageAsynchronous(String, String)} method.
 * <p>
 * Much of this comes from JEditorPane and had to be copied out because of package-private problems.
 */
public class HTMLPane extends JEditorPane {
    
    public static enum LoadResult {
        SERVER_PAGE,
        OFFLINE_PAGE;
    }
    
    private static final ListeningExecutorService QUEUE = ExecutorsHelper.newProcessingQueue("HTMLPane Queue");

    private final SynchronousEditorKit kit = new SynchronousEditorKit();    
    private HashMap<Object, Object> pageProperties;
    
    private volatile boolean pageLoaded;
    private ListeningFuture<LoadResult> currentLoad;
    
    public HTMLPane() {
        setEditorKit(kit);
        setEditorKitForContentType(kit.getContentType(), kit);
        setContentType("text/html");
        setEditable(false);
        kit.setAutoFormSubmission(false);
        setMargin(new Insets(0, 0, 0, 0));
    }
    
    /** Loads the given URL, loading the backup page if it fails to load. */
    public ListeningFuture<LoadResult> setPageAsynchronous(final String url, final String backupPage) {        
        assert SwingUtilities.isEventDispatchThread();        
        if(currentLoad != null) {
            currentLoad.cancel(true);
        }        
        currentLoad = QUEUE.submit(new Callable<LoadResult>() {
            @Override
            public LoadResult call() {
                try {
                    setPageImpl(new URL(url));
                    return LoadResult.SERVER_PAGE;
                } catch(IOException iox) {
                    setBackup();
                    return LoadResult.OFFLINE_PAGE;
                } catch(RuntimeInterruptedException rie) {
                    setBackup();
                    return LoadResult.OFFLINE_PAGE;
                }
            }
            
            private void setBackup() {
                SwingUtils.invokeNowOrLater(new Runnable() {
                    public void run() {
                        setContentType("text/html");
                        setText(backupPage);
                        setCaretPosition(0);
                    }
                });
            }
        });
        return currentLoad;
    }
    
    public boolean isLastRequestSuccessful() {
        return pageLoaded;
    }

    public boolean isRequestInProgress() {
        return currentLoad != null && !currentLoad.isDone();
    }    
    
    @Override
    public void setPage(final URL page) throws IOException {
        setPageImpl(page);
    }
    
    // Note the use of SwingUtils vs SwingUtilities & also a custom invokeAndWait
    // SwingUtils will invoke immediately if already in the dispatch thread,
    // SwingUtilities will always force to the end of the queue --
    // The uses here (of both) are very deliberate.
    // invokeAndWait is special because we want to rethrow InterruptedException
    // as IOException, because we're expecting to be interrupted occasionally
    // and do not want to report the error.
    private void setPageImpl(final URL page) throws IOException {
        if (page == null) {
            throw new IOException("invalid url");
        }

        pageLoaded = false;
        final AtomicReference<URL> loaded = new AtomicReference<URL>();
        invokeAndWait(new Runnable() {
            public void run() {
                loaded.set(getPage());
                // reset scrollbar
                if (!page.equals(loaded.get()) && page.getRef() == null) {
                    scrollRectToVisible(new Rectangle(0, 0, 1, 1));
                }
            }
        });
        
        final AtomicBoolean reloaded = new AtomicBoolean(false);
        InputStream in = new InterruptableStream(getStream(page));
        if (kit != null) {
            final AtomicReference<Document> doc = new AtomicReference<Document>();
            invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    doc.set(initializeModel(kit, page));
                    setDocument(doc.get());
                }
            });
            read(in, doc.get());
            reloaded.set(true);
        }
        
        SwingUtils.invokeNowOrLater(new Runnable() {
            public void run() {
                // Start at the top, then move if necessary.
                setCaretPosition(0);
                final String reference = page.getRef();
                if (reference != null) {
                    if (!reloaded.get()) {
                        scrollToReference(reference);
                    } else {
                        // Force this to the back of the queue.
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                scrollToReference(reference);
                            }
                        });
                    }
                }
                getDocument().putProperty(Document.StreamDescriptionProperty, page);
                firePropertyChange("page", loaded.get(), page);
            }            
        });        
        pageLoaded = true; // purposely not in a finally -- if we fail it didn't load
    }
    
    @Override
    public void setDocument(final Document doc) {
        // Ensure the document is set on the EDT thread,
        // because it triggers a repaint (and it should be).
        invokeAndWait(new Runnable() {
            @Override
            public void run() {
                HTMLPane.super.setDocument(doc);
            }
        });
    }

    // Copied from {@link JEditorPane#getStream(URL)} because of package-private problems.
    @Override
    protected InputStream getStream(URL page) throws IOException {
        final URLConnection conn = page.openConnection();
        if (conn instanceof HttpURLConnection) {
            HttpURLConnection hconn = (HttpURLConnection) conn;
            hconn.setInstanceFollowRedirects(false);
            int response = hconn.getResponseCode();
            boolean redirect = (response >= 300 && response <= 399);

            /*
             * In the case of a redirect, we want to actually change the URL
             * that was input to the new, redirected URL
             */
            if (redirect) {
                String loc = conn.getHeaderField("Location");
                if (loc.startsWith("http", 0)) {
                    page = new URL(loc);
                } else {
                    page = new URL(page, loc);
                }
                return getStream(page);
            }
        }

        invokeAndWait(new Runnable() {
            @Override
            public void run() {
                handleConnectionProperties(conn);
            }
        });
        return conn.getInputStream();
    }
    
    // This rethrows InterruptedException as RuntimeInterruptedException,
    // so that we can catch it in the setPageImpl call and respond to it.
    private void invokeAndWait(final Runnable runnable) throws RuntimeInterruptedException {
        if(EventQueue.isDispatchThread()) {
            runnable.run();
        } else {
            try {
                SwingUtilities.invokeAndWait(new Runnable() {
                    @Override
                    public void run() {
                        runnable.run();
                    }
                });
            } catch (InterruptedException e) {
                throw new RuntimeInterruptedException(e);
            } catch (InvocationTargetException e) {
                ExceptionUtils.rethrow(e);
            }
        }
    }
    
    private static class RuntimeInterruptedException extends RuntimeException {
        public RuntimeInterruptedException(Throwable t) {
            super(t);
        }
    }

    // Copied from {@link JEditorPane#handleConnectionProperties(URLConnection)} because of package-private problems.
    private void handleConnectionProperties(URLConnection conn) {
        if (pageProperties == null) {
            pageProperties = new HashMap<Object, Object>();
        }
        String type = conn.getContentType();
        if (type != null) {
            setContentType(type);
            pageProperties.put("content-type", type);
        }
        pageProperties.put(Document.StreamDescriptionProperty, conn.getURL());
        String enc = conn.getContentEncoding();
        if (enc != null) {
            pageProperties.put("content-encoding", enc);
        }
    }
    
    // Copied from {@link JEditorPane#initializeModel(EditorKit, URL)} because of package-private problems.
    private Document initializeModel(EditorKit kit, URL page) {
        Document doc = kit.createDefaultDocument();
        if (pageProperties != null) {
            // transfer properties discovered in stream to the
            // document property collection.
            for (Iterator<Object> iter = pageProperties.keySet().iterator(); iter.hasNext() ;) {
                Object key = iter.next();
                doc.putProperty(key, pageProperties.get(key));
            }
            pageProperties.clear();
        }
        if (doc.getProperty(Document.StreamDescriptionProperty) == null) {
            doc.putProperty(Document.StreamDescriptionProperty, page);
        }
        return doc;
    }

    /**
     * Updated to set the asynchronous priority to -1, although it shouldn't
     * matter because we explicitly load documents ourselves.
     */
    private static class SynchronousEditorKit extends HTMLEditorKit {
        @Override
        public Document createDefaultDocument() {
            Document doc = super.createDefaultDocument();
            ((HTMLDocument)doc).setAsynchronousLoadPriority(-1);
            return doc;
        }
    }
    
    /** An extension to {@link InputStream} that fails if the thread was interrupted. */
    private static class InterruptableStream extends FilterInputStream {
        public InterruptableStream(InputStream i) {
            super(i);
        }

        protected void checkInterrupted() throws IOException {
            if(Thread.interrupted()) {
                throw new IOException("read interrupted");
            }
        }
        
        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            checkInterrupted();
            return super.read(b, off, len);
        }

        @Override
        public int read() throws IOException {
            checkInterrupted();
            return super.read();
        }

        @Override
        public long skip(long n) throws IOException {
            checkInterrupted();
            return super.skip(n);
        }

        @Override
        public int available() throws IOException {
            checkInterrupted();
            return super.available();
        }

        @Override
        public void reset() throws IOException {
            checkInterrupted();
            super.reset();
        }
    }
    
}
