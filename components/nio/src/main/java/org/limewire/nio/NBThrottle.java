package org.limewire.nio;

import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectionKey;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;


/**
 * A throttle that can be applied to non-blocking reads and writes.
 * <p>
 * Throttles work by giving amounts to interested parties in a First in, First 
 * out (FIFO) queue, ensuring that no one party uses all of the available 
 * bandwidth on every tick.
 * <p>
 * Listeners must adhere to the following contract in order for the 
 * <code>Throttle</code> to be effective.
  <p>
 * In order: 
 <ol>
 * <li>When a listener wants to write, it must interest ONLY the Throttle.</li>
 *<p>
 *    Call: Throttle.interest(ThrottleListener)
 *<p>
 * <li>When the Throttle informs the listener that bandwidth is available, it must interest
 *    the next party in the chain (ultimately the Socket).</li>
 *<p>
 *    Callback: {@link ThrottleListener#bandwidthAvailable()}
 *<p>
 * <li>The listener must request data prior to writing (in response to requestBandwidth), 
 *    and write out only the amount that it requested.</li>
 *<p>    
 *    Callback: ThrottleListener.requestBandwidth<br>
 *    Call: Throttle.request()
 *<p>
 * <li>The listener must release data (in response to releaseBandwidth) that it was given
 *    from a request but did not write.</li>
 *<p>
 *    Callback: ThrottleListener.releaseBandwidth<br>
 *    Call: Throttle.release(amount)
 *<p>    
 *</ol>
 * <b>Extraneous</b>: The ThrottleListener must have an 'attachment' set that 
 * is the same attachment as the one used on the {@link SelectionKey} for the 
 * {@link SelectableChannel}. This is necessary so that <code>Throttle</code> 
 * can match up <code>SelectionKey</code> ready events with 
 * <code>ThrottleListener</code> interest.
 * <p>            
 * The flow of a <code>Throttle</code> works like:
<pre>
 * 
 *      <b>Throttle</b>                           <b>ThrottleListener</b>                   <b>NIODispatcher</b>
 *
 * 1)                                      Throttle.interest               
 * 2)  &lt;adds to request list&gt;
 * 3)                                                                         Throttle.tick
 * 4)  ThrottleListener.bandwidthAvailable
 * 5)                                      SocketChannel.interest
 * 6)  &lt;moves from request to interest list&gt;
 * 7)                                                                         Selector.select
 * 8)                                                                         Throttle.selectableKeys 
 * 9)  ThrottleListener.requestBandwidth
 * 10)                                     Throttle.request
 * 11)                                     SocketChannel.write [or SocketChannel.read]
 * 12) ThrottleListener.releaseBandwidth
 * 13)                                     Throttle.release
 * 14) &lt;remove from interest&gt;
 </pre>
 * If there are multiple listeners, steps 4 and 5 are repeated for each request, and steps 9 through 14
 * are performed on interested parties until there is no bandwidth available. Another tick will
 * generate more bandwidth, which will allow previously interested parties to request/write/release.
 * Because interested parties are processed in FIFO order, all parties receive equal access to
 * the bandwidth.
 * <p>
 * Note that due to the nature of <code>Throttle</code> and {@link NIODispatcher},
 * ready parties may be told to <code>WriteObserver.handleWrite()</code> twice during each 
 * selection event. The latter will always return 0 to a request.
 */
public class NBThrottle implements Throttle {
    
    //private static final Log LOG = LogFactory.getLog(NBThrottle.class);
    

    private static final int DEFAULT_TICK_TIME = 100;
    
    /** The number of milliseconds in each tick. */
    private final int MILLIS_PER_TICK;

    /** The maximum amount to ever give anyone. */
    private final int MAXIMUM_TO_GIVE;
    /** The minimum amount to ever give anyone. */
    private final int MINIMUM_TO_GIVE;
    
    /** Whether or not this throttle is for writing. (If false, it's for reading.) */
    private final boolean _write;
    
    /** The op that this uses when processing. */
    private final int _processOp;
    
    /** The amount that is available every tick. */
    private volatile int _bytesPerTick;
    
    /** The amount currently available in this tick. */
    private volatile int _available;
    
    /** The next time a tick should occur. */
    private long _nextTickTime = -1;
    
    /**
     * A list of ThrottleListeners that are interested in bandwidthAvailable events.
     * <p>
     * As ThrottleListeners interest themselves interest themselves for writing, 
     * the requests are queued up here.  When bandwidth is available the request is
     * moved over to 'interested' after informing the ThrottleListener that bandwidth
     * is available.  New ThrottleListeners should not be added to this if they are
     * already in interested.
     */
    private Set<ThrottleListener> _requests = new HashSet<ThrottleListener>();
    
    /**
     * Attachments that are interested -> ThrottleListener that owns the attachment.
     * <p>
     * As new items become interested, they are added to the bottom of the set.
     * When something is written, so long as it writes > 0, it is removed from the
     * list (and put back at the bottom).
     */
    private Map<Object, ThrottleListener> _interested = new LinkedHashMap<Object, ThrottleListener>();
    
    /**
     * Attachments that are ready-op'd.
     * <p>
     * This is temporary per each selectableKeys call, but is cached to avoid regenerating
     * each time.
     */
    private Map<Object, SelectionKey> _ready = new HashMap<Object, SelectionKey>();
    
    /** Whether or not we're currently active in the selectableKeys portion. */
    private boolean _active = false;
    
    /**
     * Constructs a throttle using the default values for latency and availability.
     */
    public NBThrottle(boolean forWriting, float bytesPerSecond) {
        this(forWriting, bytesPerSecond, true, DEFAULT_TICK_TIME);
    }

    /**
     * Constructs a throttle that is either for reading or reading with the 
     * maximum <code>bytesPerSecond</code>.
     * <p>
     * The <code>Throttle</code> is tuned to expect 'maxRequestors' requesting 
     * data, allowing only the 'maxLatency' delay between serviced requests 
     * for any given requestor.
     * <p>
     * The values are only recommendations and may be ignored (within limits) by the 
     * <code>Throttle</code> in order to ensure that the <code>Throttle</code> 
     * behaves correctly.
     */
    public NBThrottle(boolean forWriting, float bytesPerSecond, int maxRequestors, int maxLatency) {
        this(forWriting, bytesPerSecond, true,  maxRequestors == 0 ? DEFAULT_TICK_TIME : maxLatency / maxRequestors);
    }
    
    /**
     * Constructs a new Throttle that is either for writing or reading, allowing
     * the given bytesPerSecond.
     * <p>
     * Use 'true' for writing, 'false' for reading.
     * <p>
     * If addToDispatcher is false, NIODispatcher is not notified about the Throttle,
     * so it will not be automatically ticked or told of selectable keys.
     * <p>
     * The throttle will allow bandwidth spreading every millisPerTick, after
     * enforcing it's between 50 & 100.
     */
    protected NBThrottle(boolean forWriting, float bytesPerSecond, 
                         boolean addToDispatcher, int millisPerTick) {
        MILLIS_PER_TICK = Math.min(100, Math.max(50,millisPerTick));
        int ticksPerSecond = 1000 / MILLIS_PER_TICK;
        _write = forWriting;
        _processOp = forWriting ? SelectionKey.OP_WRITE : SelectionKey.OP_READ;
        _bytesPerTick = (int)(bytesPerSecond / ticksPerSecond);
        if(addToDispatcher)
            NIODispatcher.instance().addThrottle(this);
        
        if(forWriting) {
            MAXIMUM_TO_GIVE = 1400;
            MINIMUM_TO_GIVE = 30;
        } else {
            MAXIMUM_TO_GIVE = Integer.MAX_VALUE;
            MINIMUM_TO_GIVE = 1;
        }
    }
    
    public void setRate(float bytesPerSecond) {
        int ticksPerSecond = 1000 / MILLIS_PER_TICK;
        _bytesPerTick = (int)(bytesPerSecond / ticksPerSecond);
    }
    
    /**
     * Notification from the NIODispatcher that a bunch of keys are now selectable.
     */
    void selectableKeys(Collection<? extends SelectionKey> keys) {
        if(_available >= MINIMUM_TO_GIVE && !_interested.isEmpty()) {
            for(Iterator<? extends SelectionKey> i = keys.iterator(); i.hasNext(); ) {
                SelectionKey key = i.next();
                try {
                    if(key.isValid() && (_write ? key.isWritable() : key.isReadable())) {
                        Object attachment = NIODispatcher.instance().attachment(key.attachment());
                        if(_interested.containsKey(attachment)) {
                            //LOG.debug("Adding: " + attachment + " to ready");
                            _ready.put(attachment, key);
                        }
                    }
                } catch(CancelledKeyException ignored) {
                    i.remove(); // it's cancelled, we can ignore it now & forever.
                }
            }
            
            //LOG.trace("Interested: " + _interested.size() + ", ready: " + _ready.size());
            
            _active = true;
            long now = System.currentTimeMillis();
            Iterator<Map.Entry<Object, ThrottleListener>> i = _interested.entrySet().iterator();
            for(; i.hasNext(); ) {
                Map.Entry<Object, ThrottleListener> next = i.next();
                ThrottleListener listener = next.getValue();
                Object attachment = next.getKey();
                SelectionKey key = _ready.remove(attachment);
                if(!listener.isOpen()) {
                    //LOG.trace("Removing closed but interested party: " + next.getKey());
                    i.remove();
                } else if(key != null) {
                    i.remove();
                    //LOG.debug("Processing: " + key.attachment());
                    listener.requestBandwidth();
                    try {
                        NIODispatcher.instance().process(now, key, key.attachment(), _processOp);
                    } finally {
                        listener.releaseBandwidth();
                    }
                    if (_available < MINIMUM_TO_GIVE)
                        break;
                }
            }
            _active = false;
        }
    }
    
    /**
     * Interests this <code>ThrottleListener</code> in being notified when 
     * bandwidth is available.
     */
    public void interest(ThrottleListener writer) {
        boolean wakeup;
        synchronized(_requests) {
            //LOG.debug("Adding: " + writer + " to requests");
            wakeup = _requests.isEmpty();
            _requests.add(writer);
        }
        if (wakeup || _available >= MINIMUM_TO_GIVE)
            NIODispatcher.instance().wakeup();
    }
    
    /**
     * Requests some bytes to write.
     */
    public int request() {
        if(!_active) // failsafe to ensure request only occurs when we want it
            return 0;
        
        int ret = Math.min(_available, MAXIMUM_TO_GIVE);
        _available -= ret;
        return ret;
    }
    
    /**
     * Releases some unwritten bytes back to the available pool.
     */
    public void release(int amount) {
        if(_active) // failsafe to ensure releasing only occurs when we want it
            _available += amount;
        //LOG.trace("RETR: " + amount + ", REMAINING: " + _available + ", ALL: " + wroteAll + ", FROM: " + attachment);
    }
    
    /**
     * Notification from <code>NIODispatcher</code> that some time has passed.
     * <p>
     * Returns <code>true</code> if all requests were satisfied. Returns 
     * <code>false</code> if there are still some requests that require further 
     * tick notifications.
     */
    void tick(long currentTime) {
        if(currentTime >= _nextTickTime) {
            _available = _bytesPerTick;
            _nextTickTime = currentTime + MILLIS_PER_TICK;
            spreadBandwidth();
        } else if(_available >= MINIMUM_TO_GIVE) {
            spreadBandwidth();
        }
    }
    
    public long nextTickTime() {
        synchronized(_requests) {
            if (_requests.isEmpty() && _interested.isEmpty())
                return Long.MAX_VALUE;
        }
        return _nextTickTime;
    }
    
    /**
     * Notifies all requestors that bandwidth is available.
     */
    private void spreadBandwidth() {
        synchronized(_requests) {
            if(!_requests.isEmpty()) {
                for(ThrottleListener req : _requests) {
                    Object attachment = req.getAttachment();
                    if(attachment == null)
                        throw new IllegalStateException("must have an attachment - listener: " + req);
                    
                    //LOG.debug("Moving: " + attachment + " from requests to interested");
                    if(req.bandwidthAvailable())
                    	_interested.put(attachment, req);
                    // else it'll be cleared when we loop later on.
                }
                _requests.clear();
            }
        }
    }
}
    
    
