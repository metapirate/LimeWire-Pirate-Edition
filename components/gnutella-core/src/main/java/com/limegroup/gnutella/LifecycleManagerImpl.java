package com.limegroup.gnutella;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.concurrent.ThreadExecutor;
import org.limewire.core.api.lifecycle.LifeCycleEvent;
import org.limewire.core.settings.ApplicationSettings;
import org.limewire.inject.EagerSingleton;
import org.limewire.lifecycle.ServiceRegistry;
import org.limewire.listener.EventListener;
import org.limewire.listener.EventListenerList;
import org.limewire.service.ErrorService;
import org.limewire.setting.SettingsGroupManager;
import org.limewire.util.OSUtils;
import org.limewire.util.SystemUtils;

import com.google.inject.Inject;

/**
 * Provides an implementation of the <code>LifecycleManager</code> to start and
 * stop the LimeWire components.
 */
@EagerSingleton
public class LifecycleManagerImpl implements LifecycleManager {
    
    private static final Log LOG = LogFactory.getLog(LifecycleManagerImpl.class);
   
    private final AtomicBoolean preinitializeBegin = new AtomicBoolean(false);
    private final AtomicBoolean preinitializeDone = new AtomicBoolean(false);
    private final AtomicBoolean backgroundBegin = new AtomicBoolean(false);
    private final AtomicBoolean backgroundDone = new AtomicBoolean(false);
    private final AtomicBoolean startBegin = new AtomicBoolean(false);
    private final AtomicBoolean startDone = new AtomicBoolean(false);
    private final AtomicBoolean shutdownBegin = new AtomicBoolean(false);
    private final AtomicBoolean shutdownDone = new AtomicBoolean(false);
    
    private final CountDownLatch startLatch = new CountDownLatch(1);
    
    private final EventListenerList<LifeCycleEvent> listenerList;
    
    private static enum State { NONE, STARTING, STARTED, STOPPED }
    
    /** The time when this finished starting. */
    private long startFinishedTime;

    private final ServiceRegistry serviceRegistry;
    
    @Inject
    public LifecycleManagerImpl(ServiceRegistry serviceRegistry) { 
        this.serviceRegistry = serviceRegistry;
        this.listenerList = new EventListenerList<LifeCycleEvent>();
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.LifecycleManager#isLoaded()
     */
    public boolean isLoaded() {
        State state = getCurrentState();
        return state == State.STARTED || state == State.STARTING;
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.LifecycleManager#isStarted()
     */
    public boolean isStarted() {
        State state = getCurrentState();
        return state == State.STARTED || state == State.STOPPED;
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.LifecycleManager#isShutdown()
     */
    public boolean isShutdown() {
        return getCurrentState() == State.STOPPED;
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.LifecycleManager#installListeners()
     */
    private void installListeners() {
        if(preinitializeBegin.getAndSet(true))
            return;
        
        LimeCoreGlue.preinstall();        
        serviceRegistry.initialize();        
        preinitializeDone.set(true);
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.LifecycleManager#loadBackgroundTasks()
     */
    public void loadBackgroundTasks() {
        if(backgroundBegin.getAndSet(true))
            return;

        installListeners();
        
        ThreadExecutor.startThread(new Runnable() {
            public void run() {
                doBackgroundTasks();
            }
        }, "BackgroundTasks");
    }
    
    private void loadBackgroundTasksBlocking() {
        if(backgroundBegin.getAndSet(true))
            return;

        installListeners();
        
        doBackgroundTasks();
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.LifecycleManager#start()
     */
    public void start() {
        if(startBegin.getAndSet(true))
            return;
        
        try {
            listenerList.broadcast(LifeCycleEvent.STARTING);
            doStart();
            listenerList.broadcast(LifeCycleEvent.STARTED);
        } finally {
            startLatch.countDown();
        }
    }
    
    /*
     * Package private only for testing purposes, no methods outside this 
     * class should try to access this method.
     */
    void doStart() {                                
        //if previous instance of LimeWire was shutdown properly
        if(ApplicationSettings.CURRENTLY_RUNNING.getValue()) {
            ApplicationSettings.PREVIOUS_SHUTDOWN_WAS_GRACEFUL.setValue(false);             
        } else {
            ApplicationSettings.PREVIOUS_SHUTDOWN_WAS_GRACEFUL.setValue(true); 
        }
        //sets that an instance of LimeWire is running.
        ApplicationSettings.CURRENTLY_RUNNING.setValue(true);        
        
        // save limewire.props & other settings
        SettingsGroupManager.instance().save();
        
        loadBackgroundTasksBlocking();

        serviceRegistry.start();

        if(ApplicationSettings.AUTOMATIC_MANUAL_GC.getValue())
            startManualGCThread();

        startDone.set(true);
        startFinishedTime = System.currentTimeMillis();
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.LifecycleManager#shutdown()
     */
    public void shutdown() {
        try {
            listenerList.broadcast(LifeCycleEvent.SHUTINGDOWN);
            doShutdown();
            listenerList.broadcast(LifeCycleEvent.SHUTDOWN);
        } catch(Throwable t) {
            ErrorService.error(t);
        }
    }
    
    private void doShutdown() {
        if(!startBegin.get() || shutdownBegin.getAndSet(true))
            return;
        
        try {
            // TODO: should we have a time limit on how long we wait?
            startLatch.await(); // wait for starting to finish...
        } catch(InterruptedException ie) {
            LOG.error("Interrupted while waiting to finish starting", ie);
            return;
        }
        

        serviceRegistry.stop();     
        
        //records that LimeWire is shutting down properly
        ApplicationSettings.CURRENTLY_RUNNING.setValue(false);
        
        // save limewire.props & other settings
        SettingsGroupManager.instance().save();
        
        shutdownDone.set(true);
    }

    
    private static String parseCommand(String toCall) {
        if (toCall.startsWith("\"")) {
            int end;
            if ((end = toCall.indexOf("\"", 1)) > -1) {
                return toCall.substring(0,end+1);
            }
            else {
                return toCall+"\"";
            }
        }
        int space;
        if ((space = toCall.indexOf(" ")) > -1) {
            return toCall.substring(0, space);
        }
        
        return toCall;
    }
    
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.LifecycleManager#shutdown(java.lang.String)
     */
    public void shutdown(String toExecute) {
        shutdown();
        if (toExecute != null) {
            try {
                if (OSUtils.isWindowsVista()) {
                    String cmd = parseCommand(toExecute).trim();
                    String params = toExecute.substring(cmd.length()).trim();
                    SystemUtils.openFile(cmd, params);
                }
                else {
                    Runtime.getRuntime().exec(toExecute);
                }
            } catch (IOException tooBad) {}
        }
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.LifecycleManager#getStartFinishedTime()
     */
    public long getStartFinishedTime() {
        return startFinishedTime;
    }

    /** Runs all tasks that can be done in the background while the gui inits. */
    private void doBackgroundTasks() {
        serviceRegistry.start("SuperEarly"); // Load settings and simpp message
        serviceRegistry.start("EarlyBackground"); // Open listening ports and load spam filter
        backgroundDone.set(true);
    }

    /** Gets the current state of the lifecycle. */
    private State getCurrentState() {
        if(shutdownBegin.get())
            return State.STOPPED;
        else if(startDone.get())
            return State.STARTED;
        else if(startBegin.get())
            return State.STARTING;
        else
            return State.NONE;
    }

    /** Starts a manual GC thread. */
    private void startManualGCThread() {
        Thread t = ThreadExecutor.newManagedThread(new Runnable() {
            public void run() {
                while(true) {
                    try {
                        Thread.sleep(5 * 60 * 1000);
                    } catch(InterruptedException ignored) {}
                    LOG.trace("Running GC");
                    System.gc();
                    LOG.trace("GC finished, running finalizers");
                    System.runFinalization();
                    LOG.trace("Finalizers finished.");
                }
            }
        }, "ManualGC");
        t.setDaemon(true);
        t.start();
        LOG.trace("Started manual GC thread.");
    }
    
    public void addListener(EventListener<LifeCycleEvent> listener) {
        listenerList.addListener(listener);
    }
    
    public boolean removeListener(EventListener<LifeCycleEvent> listener) {
        return listenerList.removeListener(listener);
    }
}
