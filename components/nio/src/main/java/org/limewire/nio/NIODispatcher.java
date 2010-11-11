package org.limewire.nio;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.concurrent.ThreadExecutor;
import org.limewire.inject.EagerSingleton;
import org.limewire.nio.observer.AcceptChannelObserver;
import org.limewire.nio.observer.ConnectObserver;
import org.limewire.nio.observer.IOErrorObserver;
import org.limewire.nio.observer.ReadObserver;
import org.limewire.nio.observer.ReadWriteObserver;
import org.limewire.nio.observer.Shutdownable;
import org.limewire.nio.observer.TransportListener;
import org.limewire.nio.observer.WriteObserver;
import org.limewire.nio.timeout.ReadTimeout;
import org.limewire.nio.timeout.TimeoutController;
import org.limewire.nio.timeout.Timeoutable;
import org.limewire.service.ErrorService;


/**
 * Dispatcher for <code>NIO</code>.
 * <p>
 * To register interest initially in either reading, writing, accepting, or connecting,
 * use {@link #registerRead(SelectableChannel, ReadObserver)}, 
 * {@link #registerWrite(SelectableChannel, WriteObserver)}, 
 * {@link #registerReadWrite(SelectableChannel, ReadWriteObserver)},
 * {@link #registerAccept(SelectableChannel, AcceptChannelObserver)}, or 
 * {@link #registerConnect(SelectableChannel, ConnectObserver, int)}.
 * <p>
 * A channel registering for a connect can specify a timeout. If the timeout is greater than
 * zero and a connect event hasn't happened in that length of time, the channel will be cancelled 
 * and <code>handleIOException</code> will be called on the Observer 
 * (<code>org.limewire.nio.observer</code>). 
 * <p>
 * When handling events, future interest is done different ways. A channel 
 * registered for accepting will remain registered for accepting until that 
 * channel is closed (There isn't any way to turn off interest in accepting). A 
 * channel registered for connecting will turn off all interest (for any 
 * operation) once the connect event has been handled. Channels registered for 
 * reading or writing must manually change their interest when they no longer 
 * want to receive events (and must turn it back on when events are wanted).
 * <p>
 * To change interest in reading or writing, use {@link #interestRead(SelectableChannel, boolean)}
 * or {@link #interestWrite(SelectableChannel, boolean)} with the appropriate 
 * boolean parameter. The channel must have already been registered with the 
 * dispatcher. If the channel was not registered, changing interest is a no 
 * operation (no-op).
 * <p> 
 * The attachment the channel was registered with (via 
 * {@link ThrottleListener#setAttachment(Object)}) 
 * must also implement the appropriate Observer to handle read or write events.
 * (An attachment is an object that contains additional information.)
 * If interest in an event is turned on, but the attachment does not implement 
 * that Observer, a <code>ClassCastException</code> is thrown while attempting
 * to handle that event.
 * <p>
 * If any unhandled events occur while processing an event for a specific Observer, 
 * that Observer will be shutdown and will no longer receive events. If any 
 * <code>IOExceptions</code> occur while handling events for an Observer, 
 * <code>handleIOException</code> is called on that Observer.
 */
@EagerSingleton
public class NIODispatcher implements Runnable {
    
    private static final Log LOG = LogFactory.getLog(NIODispatcher.class);
    
    private static final NIODispatcher INSTANCE = new NIODispatcher();
    public static final NIODispatcher instance() { return INSTANCE; }
    private NIODispatcher() {
        boolean failed = false;
        try {
            primarySelector = Selector.open();
        } catch(IOException iox) {
            failed = true;
        }
        
        if(!failed) {
            dispatchThread = ThreadExecutor.newManagedThread(this, "NIODispatcher");
            dispatchThread.start();
        } else {
            dispatchThread = null;
        }
        
        EXECUTOR = new NIOExecutorService(dispatchThread);
    }
    
    /**
     * Maximum number of times Selector can return quickly without having anything
     * selected.
     */
    private static final long SPIN_AMOUNT = 5000;
    
    /** Ignore up to this many non-zero selects when suspecting selector is broken */
    private static final int MAX_IGNORES = 5;
    
    /** The length of time between clearing intervals for the cache. */
    private static final long CACHE_CLEAR_INTERVAL = 30000;
    
    /** The thread this is being run on. */
    private final Thread dispatchThread;
    
    /** Queue lock. */
    private final Object Q_LOCK = new Object();
    
    /** Stats for the selector */
    private final SelectStats stats = new SelectStats();
    
    /** A listener to notify the NIO thread when a selector has a pending event. */
    private final TransportListener TRANSPORT_LISTENER = new MyTransportListener();
    
    /** An ExecutorService that invokes runnables on the NIO thread. */
    private final ScheduledExecutorService EXECUTOR;
    
    /**
     * A map of classes of SelectableChannels to the Selector that should
     * be used to register that channel with.
     */
    private final Map<Class<? extends SelectableChannel>, Selector> OTHER_SELECTORS =
        new HashMap<Class<? extends SelectableChannel>, Selector>();
    
    /** A list of other Selectors that should be polled. */
    private final List <Selector> POLLERS = new ArrayList<Selector>();
    
    /** The invokeLater queue. */
    private Collection <Runnable> LATER = new LinkedList<Runnable>();
    
    /** A queue of DelayedRunnables to process tasks. */
    private final BlockingQueue<ScheduledFutureTask> DELAYED = new DelayQueue<ScheduledFutureTask>();
    
    /** The throttle queue. */
    private final List <NBThrottle> THROTTLE = new ArrayList<NBThrottle>();
    
    /** The timeout manager. */
    private final TimeoutController TIMEOUTER = new TimeoutController();
    
    /**
     * A common ByteBufferCache that classes can use.
     * TODO: Move somewhere else.
     */
    private final ByteBufferCache BUFFER_CACHE = new ByteBufferCache();
    
    /** The selector this uses. */
    private Selector primarySelector = null;
    
    /** The current iteration of selection. */
    private long iteration = 0;
    
    /** Whether or not we've tried to wake up the selector. */
    private volatile boolean wokeup = false;
    
    /** The last time the ByteBufferCache was cleared. */
    private long lastCacheClearTime;
    
    /** Returns true if the NIODispatcher is merrily chugging along. */
    public boolean isRunning() {
        return dispatchThread != null;
    }

    /** Determine if this is the dispatch thread. */
    public boolean isDispatchThread() {
        return Thread.currentThread() == dispatchThread;
    }
    
    /** Gets the common <code>ByteBufferCache</code>. */
    public ByteBufferCache getBufferCache() {
        return BUFFER_CACHE;
    }
    
    /** Returns the number of timeouts that are pending. */
    public int getNumPendingTimeouts() {
        return TIMEOUTER.getNumPendingTimeouts();
    }

    /** Adds a <code>Throttle</code> into the throttle requesting loop. */
    // TODO: have some way to remove Throttles, or make these use WeakReferences
    public void addThrottle(final NBThrottle t) {
        if(Thread.currentThread() == dispatchThread)
            THROTTLE.add(t);
        else {
            executeLaterAlways(new Runnable() {
                public void run() {
                    THROTTLE.add(t);
                }
            });
        }
    }
    
    /** Registers a channel for nothing. */
    public void register(SelectableChannel channel, IOErrorObserver attachment) {
        register(channel, attachment, 0, 0);
    }
    
    /** Register interest in accepting. */
    public void registerAccept(SelectableChannel channel, AcceptChannelObserver attachment) {
        register(channel, attachment, SelectionKey.OP_ACCEPT, 0);
    }
    
    /** Register interest in connecting. */
    public void registerConnect(SelectableChannel channel, ConnectObserver attachment, int timeout) {
        register(channel, attachment, SelectionKey.OP_CONNECT, timeout);
    }
    
    /** Register interest in reading. */
    public void registerRead(SelectableChannel channel, ReadObserver attachment) {
        register(channel, attachment, SelectionKey.OP_READ, 0);
    }
    
    /** Register interest in writing. */
    public void registerWrite(SelectableChannel channel, WriteObserver attachment) {
        register(channel, attachment, SelectionKey.OP_WRITE, 0);
    }
    
    /** Register interest in both reading and writing. */
    public void registerReadWrite(SelectableChannel channel, ReadWriteObserver attachment) {
        register(channel, attachment, SelectionKey.OP_READ | SelectionKey.OP_WRITE, 0);
    }
    
    /** Register interest. */
    private void register(SelectableChannel channel, IOErrorObserver handler, int op, int timeout) {
        if(Thread.currentThread() == dispatchThread) {
            registerImpl(getSelectorFor(channel), channel, op, handler, timeout);
        } else {
            synchronized(Q_LOCK) {
                LATER.add(new RegisterOp(channel, handler, op, timeout));
            }
            wakeup();
        }
    }
    
    /**
     * Registers a <code>SelectableChannel</code> as being interested in a write again.
     * <p>
     * You must ensure that the attachment that handles events for this channel
     * implements <code>WriteObserver</code>. If not, a <code>ClassCastException</code> will be thrown
     * while handling write events.
     */
    public void interestWrite(SelectableChannel channel, boolean on) {
        interest(channel, SelectionKey.OP_WRITE, on);
    }
    
    /**
     * Registers a <code>SelectableChannel</code> as being interested in a read
     * again.
     * <p>
     * You must ensure that the attachment that handles events for this channel
     * implements <code>ReadObserver</code>. If not, a 
     * <code>ClassCastException</code> will be thrown
     * while handling read events.
     */
    public void interestRead(SelectableChannel channel, boolean on) {
        interest(channel, SelectionKey.OP_READ, on);
    }    
    
    /** Registers interest on the channel for the given <code>op</code> */
    private void interest(SelectableChannel channel, int op, boolean on) {
        try {
            Selector sel = getSelectorFor(channel);
            SelectionKey sk = channel.keyFor(sel);
            if(sk != null && sk.isValid()) {
                // We must synchronize on something unique to each key,
                // (but not the key itself, 'cause that'll interfere with Selector.select)
                // so that multiple threads calling interest(..) will be atomic with
                // respect to each other.  Otherwise, one thread can preempt another's
                // interest setting, and one of the interested ops may be lost.
                int oldOps;
                synchronized(sk.attachment()) {
                    if((op & SelectionKey.OP_READ) == SelectionKey.OP_READ) {
                        ((Attachment)sk.attachment()).changeReadStatus(on);
                    }
                    
                    oldOps = sk.interestOps();
                    
                    if(on)
                        sk.interestOps(oldOps | op);
                    else
                        sk.interestOps(oldOps & ~op);
                }
                
                // if we're turning it on and it wasn't on before...
                if(on && (oldOps & op) != op)
                    wakeup();
            }
        } catch(CancelledKeyException ignored) {
            // Because closing can happen in any thread, the key may be cancelled
            // between the time we check isValid & the time that interestOps are
            // set or gotten.
        }
    }
    
    /** Returns the <code>Selector</code> that should be used for the given channel. */
    private Selector getSelectorFor(SelectableChannel channel) {
        Selector sel = OTHER_SELECTORS.get(channel.getClass());
        if(sel == null)
            return primarySelector; // default selector
        else
            return sel;      // custom selector
    }
    
    /** Shuts down the handler, possibly scheduling it for shutdown in the 
     * <code>NIODispatch</code> thread. */
    public void shutdown(Shutdownable handler) {
        handler.shutdown();
    }
    
    /**
     * Registers a new <code>Selector</code> that should be used when 
     * <code>SelectableChannels</code> assignable from the given class are 
     * registered.
     */
    public void registerSelector(final Selector newSelector, final Class<? extends SelectableChannel> channelClass) {
        if(Thread.currentThread() == dispatchThread) {
            POLLERS.add(newSelector);
            OTHER_SELECTORS.put(channelClass, newSelector);
        } else {
            executeLaterAlways(new Runnable() {
                public void run() {
                    POLLERS.add(newSelector);
                    OTHER_SELECTORS.put(channelClass, newSelector);
                }
            });
        }
    }
    
    /**
     * Removes a registered Selector.
     */
    public void removeSelector(final Selector selector) {
        if(Thread.currentThread() == dispatchThread) {
            POLLERS.remove(selector);
            OTHER_SELECTORS.remove(selector);
        } else {
            executeLaterAlways(new Runnable() {
                public void run() {
                    POLLERS.remove(selector);
                    OTHER_SELECTORS.remove(selector);
                }
            });
        }
    }
    
    /**
     * Retrieves the <code>ExecutorService</code> this <code>NIODispatcher</code> uses to
     * run things on the NIO Thread.
     * If tasks are submitted for execution while already on the NIO thread,
     * the task will be immediately run. Otherwise,
     * the tasks will be scheduled for running as soon as possible on the
     * NIO Thread.
     */
    public ScheduledExecutorService getScheduledExecutorService() {
        return EXECUTOR;
    }
    
    /** Submits the runnable for execution later, even if the current thread is the NIO thread. */
    public void executeLaterAlways(Runnable runner) {
        synchronized(Q_LOCK) {
            LATER.add(runner);
        }
        wakeup();
    }
    
    /** Gets the underlying attachment for the given <code>SelectionKey</code>'s attachment. */
    public IOErrorObserver attachment(Object proxyAttachment) {
        return ((Attachment)proxyAttachment).attachment;
    }
    
    /**
     * Cancel <code>SelectionKey</code> and shuts down the handler.
     */
    private void cancel(SelectionKey sk, Shutdownable handler) {
        sk.cancel();
        if(handler != null)
            handler.shutdown();
    }
    
        
    /**
     * Accept an incoming connection.
     * 
     * @throws IOException
     */
    private void processAccept(long now, SelectionKey sk, AcceptChannelObserver handler, Attachment proxy) throws IOException {
        if(LOG.isDebugEnabled())
            LOG.debug("Handling accept: " + handler);
        
        ServerSocketChannel ssc = (ServerSocketChannel)sk.channel();
        SocketChannel channel = ssc.accept();
        
        if (channel == null)
            return;
        
        if (channel.isOpen()) {
            channel.configureBlocking(false);
            handler.handleAcceptChannel(channel);
        } else {
            try {
                channel.close();
            } catch (IOException err) {
                LOG.error("SocketChannel.close()", err);
            }
        }
    }
    
    /**
     * Process a connected channel.
     */
    private void processConnect(long now, SelectionKey sk, ConnectObserver handler, Attachment proxy)
      throws IOException {
        if (LOG.isDebugEnabled())
            LOG.debug("Handling connect: " + handler);

        SocketChannel channel = (SocketChannel) sk.channel();
        proxy.clearTimeout();

        boolean finished = channel.finishConnect();
        if (finished) {
        	sk.interestOps(0); // interested in nothing just yet.
            handler.handleConnect(channel.socket());
        } else {
            cancel(sk, handler);
        }
    }
    
    /** Process a channel read operation. */
    private void processRead(long now, ReadObserver handler, Attachment proxy) throws IOException {
        if (LOG.isDebugEnabled())
            LOG.debug("Handling read: " + handler);
        proxy.updateReadTimeout(now);
        handler.handleRead();
    }
    
    /** Process a channel write operation. */
    private void processWrite(long now, WriteObserver handler, Attachment proxy) throws IOException {
        if (LOG.isDebugEnabled())
            LOG.debug("Handling write: " + handler);
        handler.handleWrite();
    }
    
    /**
     * Does a real registration.
     */
    private void registerImpl(Selector selector, SelectableChannel channel, int op,
                              IOErrorObserver attachment, int timeout) {
        try {
            SelectionKey existing = channel.keyFor(selector);
            if(existing != null) {
                Attachment old = (Attachment)existing.attachment();
                old.discard();
            }
            
            Attachment guard = new Attachment(attachment);
            SelectionKey key = channel.register(selector, op, guard);
            guard.setKey(key);
            if(timeout != 0) 
                guard.addTimeout(System.currentTimeMillis(), timeout);
            else if((op & SelectionKey.OP_READ) != 0)
                guard.changeReadStatus(true);
        } catch(IOException iox) {
            attachment.handleIOException(iox);
        }
    }
    
    /**
     * Adds any pending actions.
     * <p>
     * This works by adding any pending actions into a local list and then replacing
     * LATER with a new list.  This is done so that actions to the outside world
     * don't need to hold Q_LOCK.
     * <p>
     * Throttle is ticked outside the lock because ticking only hits items in this
     * package and we can ensure it doesn't deadlock.
     */
    private void runPendingTasks() {
        long now = System.currentTimeMillis();
        Collection<Runnable> localLater;
        synchronized(Q_LOCK) {
            localLater = LATER;
            LATER = new LinkedList<Runnable>();
        }
        
        DELAYED.drainTo(localLater);
        
        if(now > lastCacheClearTime + CACHE_CLEAR_INTERVAL) {
            BUFFER_CACHE.clearCache();
            lastCacheClearTime = now;
        }
        
        if(!localLater.isEmpty()) {
            for(Runnable item : localLater) {
                try {
                    item.run();
                } catch(Throwable t) {
                    LOG.error(t);
                    ErrorService.error(t);
                }
            }
        }
        
        now = System.currentTimeMillis();
        for(NBThrottle t: THROTTLE)
            t.tick(now);
    }
    
    /**
     * Runs through all secondary Selectors and returns a 
     * Collection of <code>SelectionKey</code>s that they selected.
     */
    private Collection <SelectionKey> pollOtherSelectors() {
        Collection<SelectionKey> ret = null;
        boolean growable = false;
        
        // Optimized to not create collection objects unless absolutely
        // necessary.
        for(int i = 0; i < POLLERS.size(); i++) {
            Selector sel = POLLERS.get(i);
            int n = 0;
            try {
                n = sel.selectNow();
            } catch(IOException iox) {
                LOG.error("Error performing secondary select", iox);
            }
            
            if(n != 0) {
                Collection<SelectionKey> selected = sel.selectedKeys();
                if(!selected.isEmpty()) {
                    if(ret == null) {
                        ret = selected;
                    } else if(!growable) {
                        growable = true;
                        ret = new HashSet<SelectionKey>(ret);
                        ret.addAll(selected);
                    } else {
                        ret.addAll(selected);
                    }
                }
            }
        }
        
        if(ret == null)
            return Collections.emptySet();
        else
            return ret;
    }
    
    /**
     * Loops through all <code>Throttles</code> and gives them the ready keys.
     */
    private void readyThrottles(Collection<SelectionKey> keys) {
        for (int i = 0; i < THROTTLE.size(); i++)
            THROTTLE.get(i).selectableKeys(keys);
    }
    
    /**
     * Wakes up the primary selector if it wasn't already woken up,
     * and the current thread is not the dispatch thread.
     */
    void wakeup() {
        if(!wokeup && Thread.currentThread() != dispatchThread && primarySelector != null) {
            wokeup = true;
            primarySelector.wakeup();
        }
    }
    
    /**
     * The actual NIO run loop.
     */
    private void process() throws ProcessingException, SpinningException {
        boolean checkTime = false;
        long startSelect = -1;
        int zeroes = 0;
        int ignores = 0;
        
        while(true) {
            runPendingTasks();
            
            Collection<SelectionKey> polled = pollOtherSelectors();
            boolean immediate = !polled.isEmpty();
            try {
                if(!immediate && checkTime)
                    startSelect = System.currentTimeMillis();
                
                if(!immediate) {
                	long delay = nextSelectTimeout();
                	if (delay == 0) {
                		immediate = true;
                    } else {
                        long nanoNow = System.nanoTime();
                        try {
                            if (Thread.interrupted())
                                LOG.warn("interrupted?");
                            primarySelector.select(Math.min(delay, Integer.MAX_VALUE));
                        } finally {
                            stats.updateSelectTime(System.nanoTime() - nanoNow);
                        }
                    }
                }
                
                if (immediate) {
                    stats.countSelectNow();
                    primarySelector.selectNow();
                }
            } catch (NullPointerException err) {
                LOG.warn("npe", err);
                continue;
            } catch (CancelledKeyException err) {
                LOG.warn("cancelled", err);
                continue;
            } catch (IOException iox) {
                throw new ProcessingException(iox);
            }
            
            Collection<SelectionKey> keys = primarySelector.selectedKeys();
            if(!immediate && !wokeup) {
                if(keys.isEmpty()) {
                    long now = System.currentTimeMillis();
                    if(startSelect == -1) {
                        LOG.trace("No keys selected, starting spin check.");
                        checkTime = true;
                    } else if(startSelect + 30 >= now) {
                        if(LOG.isWarnEnabled())
                            LOG.warn("Spinning detected, current spins: " + zeroes+" startSelect "+startSelect+" now "+now+" keys "+primarySelector.keys());
                        if(zeroes++ > SPIN_AMOUNT)
                            throw new SpinningException();
                    } else { // waited the timeout just fine, reset everything.
                        checkTime = false;
                        startSelect = -1;
                        zeroes = 0;
                        ignores = 0;
                    }
                    TIMEOUTER.processTimeouts(now);
                    continue;                
                } else if (checkTime) {             
                    // skip up to certain number of good selects if we suspect the selector is broken
                    ignores++;
                    if (ignores > MAX_IGNORES) {
                        checkTime = false;
                        zeroes = 0;
                        startSelect = -1;
                        ignores = 0;
                    }
                }
            }
            
            if(LOG.isTraceEnabled())
                LOG.trace("Selected keys: (" + keys.size() + "), polled: (" + polled.size() + "). wokeup "+wokeup+" immediate "+immediate);
            
            Collection<SelectionKey> allKeys;
            if(!polled.isEmpty()) {
                allKeys = new HashSet<SelectionKey>(keys.size() + polled.size());
                allKeys.addAll(keys);
                allKeys.addAll(polled);
            } else {
                allKeys = keys;
            }
            
            readyThrottles(allKeys);
            
            long now = System.currentTimeMillis();
            for(SelectionKey sk : allKeys) 
				process(now, sk, sk.attachment(), 0xFFFF);
            
            keys.clear();
            iteration++;
            TIMEOUTER.processTimeouts(now);
            wokeup = false;
        }
    }
    
    /**
     * @return the timeout of the next select call. 0 if it should be immediate
     */
    private long nextSelectTimeout() {
        // first see when the next throttle should tick
        long next = Long.MAX_VALUE;
        for (Throttle t : THROTTLE)
            next = Math.min(next, t.nextTickTime());
        long now = System.currentTimeMillis();
        next -= now;
        if (next <= 0)
            return 0;

        // then check when the next timeout is due
        long timeout = TIMEOUTER.getNextExpireTime();
        if (timeout > -1)
            next = Math.min(next, timeout - now);
        if (next <= 0)
            return 0;

        // then see when the next scheduled task is due
        // Note: DelayedQueue.peek() returns the element even if not expired.
        Delayed nextScheduled = DELAYED.peek();
        if (nextScheduled != null)
            next = Math.min(next, nextScheduled.getDelay(TimeUnit.MILLISECONDS));
        return Math.max(0, next);
    }
    
    /**
     * Returns true if this channel is going to have handleRead called on its
     * attachment in this iteration of the NIODispatcher's processing.
     * <p>
     * This must be called from the NIODispatch thread to have any meaningful impact.
     */
    boolean isReadReadyThisIteration(SelectableChannel channel) {
        SelectionKey sk = channel.keyFor(getSelectorFor(channel));
        Object proxyAttachment = sk.attachment();
        if(proxyAttachment instanceof Attachment) {
            Attachment proxy = (Attachment)sk.attachment();
            if(proxy.lastMod == iteration+1) {
                if(sk.isValid()) {
                    try {
                        return (sk.readyOps() & (~proxy.handled) & SelectionKey.OP_READ) != 0;
                    } catch(CancelledKeyException ignored) {}
                }
            }
        }
        
        return false;
    }
    
    /**
     * Processes a single SelectionKey & attachment, processing only
     * ops that are in allowedOps.
     */
    void process(long now, SelectionKey sk, Object proxyAttachment, int allowedOps) {
        Attachment proxy = (Attachment)proxyAttachment;
        IOErrorObserver attachment = proxy.attachment;
        
        // NOTE: handled is updated in proxy to prevent items that were processed
        //       from throttles from being reprocessed.
        //       it is reset to 0 whenever the item is being processed for the first
        //       time in a given iteration.

        if(proxy.lastMod <= iteration)
            proxy.handled = 0;
            
        proxy.lastMod = iteration + 1;
        
        if(sk.isValid()) {
            try {
                try {
                    int notHandled = ~proxy.handled;
                    int readyOps = sk.readyOps();
                    if ((allowedOps & readyOps & notHandled & SelectionKey.OP_ACCEPT) != 0)  {
                        proxy.handled |= SelectionKey.OP_ACCEPT;
                        processAccept(now, sk, (AcceptChannelObserver)attachment, proxy);
                    } else if((allowedOps & readyOps & notHandled & SelectionKey.OP_CONNECT) != 0) {
                        proxy.handled |= SelectionKey.OP_CONNECT;
                        processConnect(now, sk, (ConnectObserver)attachment, proxy);
                    } else {
                        if ((allowedOps & readyOps & notHandled & SelectionKey.OP_READ) != 0) {
                            proxy.handled |= SelectionKey.OP_READ;
                            processRead(now, (ReadObserver)attachment, proxy);
                        }
                        if ((allowedOps & readyOps & notHandled & SelectionKey.OP_WRITE) != 0) {
                            proxy.handled |= SelectionKey.OP_WRITE;
                            processWrite(now, (WriteObserver)attachment, proxy);
                        }
                    }
                } catch (CancelledKeyException err) {
                    LOG.warn("Ignoring cancelled key", err);
                } catch(IOException iox) {
                    LOG.warn("IOX processing", iox);
                    try {
                        sk.cancel(); // make sure its cancelled.
                    } catch(Throwable ignored) {}
                    attachment.handleIOException(iox);
                }
            } catch(Throwable t) {
                ErrorService.error(t, "Unhandled exception while dispatching");
                safeCancel(sk, attachment);
            }
        } else {
            if(LOG.isErrorEnabled())
                LOG.error("SelectionKey cancelled for: " + attachment);
            // we've had too many hits in a row.  kill this attachment.
            safeCancel(sk, attachment);
        }
    }
    
    /** A very safe cancel, ignoring errors & only shutting down if possible. */
    private void safeCancel(SelectionKey sk, Shutdownable attachment) {
        try {
            cancel(sk, attachment);
        } catch(Throwable ignored) {}
    }
    
    /**
     * Swaps all channels out of the old selector & puts them in the new one.
     */
    private void swapSelector() {
        Selector oldSelector = primarySelector;
        Collection<SelectionKey> oldKeys = Collections.emptySet();
        try {
            if(oldSelector != null)
                oldKeys = oldSelector.keys();
        } catch(ClosedSelectorException ignored) {
            LOG.warn("error getting keys", ignored);
        }
        
        try {
            primarySelector = Selector.open();
        } catch(IOException iox) {
            LOG.error("Can't make a new selector!!!", iox);
            throw new RuntimeException(iox);
        }
        
        // We do not have to concern ourselves with secondary selectors,
        // because we only retrieves keys from the primary one.
        for(SelectionKey key : oldKeys ) {
            try {
                SelectableChannel channel = key.channel();
                Attachment attachment = (Attachment)key.attachment();
                int ops = key.interestOps();
                try {
                    SelectionKey newKey = channel.register(primarySelector, ops, attachment);
                    attachment.setKey(newKey);
                } catch(IOException iox) {
                    attachment.attachment.handleIOException(iox);
                }
            } catch(CancelledKeyException ignored) {
                LOG.warn("key cancelled while swapping", ignored);
            }
        }
        
        try {
            if(oldSelector != null)
                oldSelector.close();
        } catch(IOException ignored) {
            LOG.warn("error closing old selector", ignored);
        }
    }
    
    /**
     * The <code>run</code> loop.
     */
    public void run() {
        while(true) {
            try {
                if(primarySelector == null)
                    primarySelector = Selector.open();
                process();
            } catch(SpinningException spin) {
                LOG.warn("selector is spinning!", spin);
                swapSelector();
            } catch(ProcessingException uhoh) {
                LOG.warn("unknown exception while selecting", uhoh);
                swapSelector();
            } catch(IOException iox) {
                LOG.error("Unable to create a new Selector!!!", iox);
                throw new RuntimeException(iox);
            } catch(Throwable err) {
                LOG.error("Error in Selector!", err);
                ErrorService.error(err);
                
                swapSelector();
            }
        }
    }
    
    /**
     * Encapsulates an attachment.
     * Contains methods for timing out an attachment,
     * keeping track of the number of successive hits, etc...
     */
    class Attachment implements Timeoutable {        
        private final IOErrorObserver attachment;
        private long lastMod;
        private int handled;
        private SelectionKey key;

        private boolean timeoutActive = false;
        private long storedTimeoutLength = Long.MAX_VALUE;
        private long storedExpireTime = Long.MAX_VALUE;
        
        private volatile boolean discarded;
        
        Attachment(IOErrorObserver attachment) {
            this.attachment = attachment;
        }
        
        @Override
        public String toString() {
            return "Attachment for: " + attachment;
        }
        
        void discard() {
            discarded = true;
        }
        
        synchronized void clearTimeout() {
            timeoutActive = false;
        }
        
        synchronized void updateReadTimeout(long now) {
            if(!discarded) {
                if(attachment instanceof ReadTimeout) {
                    long timeoutLength = ((ReadTimeout)attachment).getReadTimeout();
                    if(timeoutLength != 0) {
                        long expireTime = now + timeoutLength;
                        // We need to add a new timeout if none is scheduled or we need
                        // to timeout before the next one.
                        if(expireTime < storedExpireTime || storedExpireTime == -1 || storedExpireTime < now) {
                            addTimeout(now, timeoutLength);
                        } else {
                            // Otherwise, store the timeout info so when we get notified
                            // we can reschedule it for the future.
                            storedExpireTime = expireTime;
                            storedTimeoutLength = timeoutLength;
                            timeoutActive = true;
                        }
                    } else {
                        clearTimeout();
                    }
                }
            }
        }
        
        synchronized void changeReadStatus(boolean reading) {
            if(!discarded) {
                if(reading)
                    updateReadTimeout(System.currentTimeMillis());
                else
                    clearTimeout();
            }
        }

        synchronized void addTimeout(long now, long timeoutLength) {
            if(!discarded) {
                timeoutActive = true;
                storedTimeoutLength = timeoutLength;
                storedExpireTime = now + timeoutLength;
                TIMEOUTER.addTimeout(this, now, timeoutLength);
            }
        }
        
        public void notifyTimeout(long now, long expireTime, long timeoutLength) {
            if(!discarded) {
                boolean cancel = false;
                long timeToUse = 0;
                synchronized(this) {
                    if(timeoutActive) {
                        if(expireTime == storedExpireTime) {
                            cancel = true;
                            timeoutActive = false;
                            timeToUse = storedTimeoutLength;
                            storedExpireTime = -1;
                        } else if(expireTime < storedExpireTime) {
                            TIMEOUTER.addTimeout(this, now, storedExpireTime - now);
                        } else { // expireTime > storedExpireTime
                            storedExpireTime = -1;
                            if(LOG.isWarnEnabled())
                                LOG.warn("Ignoring extra timeout for: " + attachment);
                        }
                    } else {
                        storedExpireTime = -1;
                        storedTimeoutLength = -1;
                    }
                }
                
                // must do cancel & IOException outside of the lock.
                if(cancel) {
                    if (LOG.isDebugEnabled())
                        LOG.debug("Closing due to read timeout: " + attachment); 
                    cancel(key, attachment);
                    attachment.handleIOException(new SocketTimeoutException("operation timed out (" + timeToUse + ")"));
                }
            }
        }
        
        public void setKey(SelectionKey key) {
            this.key = key;
        }
    }    
    
    /** Encapsulates a register op. */
    private class RegisterOp implements Runnable {
        private final SelectableChannel channel;
        private final IOErrorObserver handler;
        private final int op;
        private final int timeout;
    
        RegisterOp(SelectableChannel channel, IOErrorObserver handler, int op, int timeout) {
            this.channel = channel;
            this.handler = handler;
            this.op = op;
            this.timeout = timeout;
        }
        
        public void run() {
            registerImpl(getSelectorFor(channel), channel, op, handler, timeout);
        }
    }
    
    private static class SpinningException extends Exception {
        public SpinningException() { super(); }
    }
    
    private static class ProcessingException extends Exception {
//        public ProcessingException() { super(); }
        public ProcessingException(Throwable t) { super(t); }
    }
    
    /** An ExecutorService that runs all tasks on the NIODispatch thread. */
    private static class NIOExecutorService extends AbstractExecutorService implements ScheduledExecutorService {
        private final Thread nioThread;
        
        private NIOExecutorService(Thread nioThread) {
            this.nioThread = nioThread;
        }
        
        public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
            Thread.sleep(unit.toMillis(timeout));
            return false;
        }

        public boolean isShutdown() {
            return false;
        }

        public boolean isTerminated() {
            return false;
        }

        public void shutdown() {
            throw new UnsupportedOperationException();
        }

        public List<Runnable> shutdownNow() {
            throw new UnsupportedOperationException();
        }

        public void execute(Runnable command) {
            if(Thread.currentThread() == nioThread) {
                command.run();
            } else {
                instance().executeLaterAlways(command);
            }
        }

        public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
           ScheduledFutureTask<?> ret = new ScheduledFutureTask<Void>(command, null, unit.toNanos(delay));
           instance().DELAYED.add(ret);
           instance().wakeup();
           return ret;
        }

        public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
            ScheduledFutureTask<V> ret = new ScheduledFutureTask<V>(callable, unit.toNanos(delay));
            instance().DELAYED.add(ret);
            instance().wakeup();
            return ret;
        }

        public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
            throw new UnsupportedOperationException();
        }

        public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
            throw new UnsupportedOperationException();
        }
        
    }

    public TransportListener getTransportListener() {
    	return TRANSPORT_LISTENER;
    }
    
    /** A transport listener that wakes up the selector when an event is pending. */
    private class MyTransportListener implements TransportListener {
    	public void eventPending() {
    		wakeup();
    	}
    }
    
    /**
     * @return quick stats about the selector
     */
    public long [] getSelectStats() {
        return stats.getStats();
    }
    /**
     * Provides statistics about the {@link Selector} including the number
     * of selects, number of immediate selects and the average select time.
     * 
     */
    public static class SelectStats {
        private long numSelects, numImmediateSelects, avgSelectTime;
        public synchronized long[] getStats() {
            return new long[]{numSelects, numImmediateSelects, avgSelectTime};
        }
        
        /** Updates the counters for the select times */
        synchronized void updateSelectTime(long thisSelect) {
            // the Math.max calls are to account for overflow
            long avg = avgSelectTime;
            long num = numSelects;
            avg = Math.max(0, avg * Math.max(1, num));
            num = Math.max(1,++num);
            avg = Math.max(0, avg+thisSelect) / num;
            numSelects = num;
            avgSelectTime = avg;
        }
        
        synchronized void countSelectNow() {
            numImmediateSelects = Math.max(0, numImmediateSelects +1 );   
        }
    }
}

