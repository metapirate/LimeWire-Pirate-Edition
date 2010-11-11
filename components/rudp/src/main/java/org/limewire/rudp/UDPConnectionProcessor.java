package org.limewire.rudp;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AlreadyConnectedException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ConnectionPendingException;
import java.nio.channels.SelectionKey;

import org.limewire.listener.EventBroadcaster;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.rudp.messages.AckMessage;
import org.limewire.rudp.messages.DataMessage;
import org.limewire.rudp.messages.FinMessage;
import org.limewire.rudp.messages.KeepAliveMessage;
import org.limewire.rudp.messages.RUDPMessage;
import org.limewire.rudp.messages.SynMessage;
import org.limewire.rudp.messages.SynMessage.Role;
import org.limewire.service.ErrorService;

/** 
 *  Manages a reliable UDP connection to transfer data.
 */
public class UDPConnectionProcessor {

    private static final Log LOG = LogFactory.getLog(UDPConnectionProcessor.class);

    /** Define the chunk size used for data bytes. */
    public static final int   DATA_CHUNK_SIZE         = 512;

    /** Define the maximum chunk size read for data bytes
        before we will blow out the connection. */
    public  static final int   MAX_DATA_SIZE           = 4096;
    
    /** Define the size of the data window. */
    public static final int  DATA_WINDOW_SIZE        = 20;

    /** Define the maximum accepted write ahead packet. */
    private static final int  DATA_WRITE_AHEAD_MAX    = DATA_WINDOW_SIZE + 5;

    /** The maximum number of times to try and send a data message. */
    private static final int  MAX_SEND_TRIES          = 8;

	/** Define the wait time between SYN messages. */
	private static final long SYN_WAIT_TIME           = 400;

    /** Define the maximum wait time to connect. */
    private static final long MAX_CONNECT_WAIT_TIME   = 20*1000;
    
    /** Define the maximum time that we'll allow a connection to remain
     *  open through keep-alives alone. */
    private static final long MAX_KEEPALIVE_TIME      = 60 * 1000;

	/** Define the maximum wait time before sending a message in order to
        keep the connection alive (and firewalls open).  */
	private static final long KEEPALIVE_WAIT_TIME     = (3*1000 - 500);

    /** Define the default time to check for an ack to a data message. */
    private static final long DEFAULT_RTO_WAIT_TIME   = 400;

    /** Define the maximum time that a connection will stay open without 
		a message being received */
    private static final long MAX_MESSAGE_WAIT_TIME   = 20 * 1000;

    /** Define the minimum wait time between ack timeout events. */
    private static final long MIN_ACK_WAIT_TIME       = 5;

    /** Define the size of a small send window for increasing wait time .*/
            static final long SMALL_SEND_WINDOW       = 2;

    /** Ensure that writing takes a break every 4 writes so other 
        synchronized activity can take place. */
    private static final long MAX_WRITE_WITHOUT_SLEEP = 4;

    /** Delay the write wakeup event a little so that it isn't constantly
        firing - This should achieve part of nagles algorithm.  */
    private static final long WRITE_WAKEUP_DELAY_TIME = 10;

    /** Delay the write events by one second if there is nothing to do. */
    private static final long NOTHING_TO_DO_DELAY     = 1000;

    /** Time to wait after a close before everything is totally shutdown. */
    private static final long SHUTDOWN_DELAY_TIME     = 400;
    
    /**
     *  Time to wait after the connection is closed to completely and totally
     *  shut down the connection (and its channel).
     */
    private static final long CHANNEL_SHUTDOWN_DELAY  = 30000;
    
    // Handle to various singleton objects in our architecture
    private UDPScheduler      _scheduler;
    
    /** The UDPSocketChannel backing this processor. */
    private UDPSocketChannel  _channel;
    
    /** The address we're connected to. */
    private InetSocketAddress _connectedTo;
    
    /** The limit on space for data to be written out. */
    private volatile int      _chunkLimit;

    /** The receivers windowSpace defining amount of data that receiver can
        accept. */
    private volatile int      _receiverWindowSpace;    
    
    /** Whether or not we've received an ack to our syn. */
    private boolean           _receivedSynAck;

    /** The Window for sending and acking data. */
	private DataWindow        _sendWindow;

    /** The WriteRegulator controls the amount of waiting time between writes. */
    private WriteRegulator    _writeRegulator;

    /** The Window for receiving data. */
    private DataWindow        _receiveWindow;

    /** The connection id of this end of the connection. Used for routing. */
    private byte              _myConnectionID;
    
    /** The connectionID of the other end of the connection.  Used for routing. */
	private volatile byte     _theirConnectionID;

    /** The status of the connection */
	private ConnectionState   _connectionState;

    /** Scheduled event for keeping connection alive.  */
    private UDPTimerEvent     _keepaliveEvent;

    /** Scheduled event for writing data appropriately over time.  */
    private UDPTimerEvent     _writeDataEvent;

    /** Scheduled event for cleaning up at end of connection life.  */
    private UDPTimerEvent     _closedCleanupEvent;

    /** Flag that the writeEvent is shutdown waiting for space to write. */
	private boolean           _waitingForDataSpace;

    /** Flag that the writeEvent is shutdown waiting for data to write. */
	private volatile boolean  _waitingForDataAvailable;

    /** Flag saying that a Fin packet has been acked on shutdown. */
    private boolean           _waitingForFinAck;
    
    /** The time we started connecting. */
    private long              _startedConnecting;
    
    /** Scheduled event for connecting. */
    private UDPTimerEvent     _connectEvent;

    /** Scheduled event for ensuring that data is acked or resent. */
    private UDPTimerEvent     _ackTimeoutEvent;

    /** Adhoc event for waking up the writing of data. */
    private UDPTimerEvent     _safeWriteWakeup;

    /** The current sequence number of messages originated here. */
    private long              _sequenceNumber;

    /** The sequence number of a pending fin message */
	private long              _finSeqNo;

	/** Transformer for mapping 2 byte sequenceNumbers of incoming ACK 
        messages to 8 byte longs of essentially infinite size - note Acks 
        echo our seqNo. */
	private SequenceNumberExtender _localExtender;

    /** Transformer for mapping 2 byte sequenceNumbers of incoming messages to
        8 byte longs of essentially infinite size. */
    private SequenceNumberExtender _extender;

    /** The last time that a message was sent to other host. */
    private long              _lastSendTime;

    /** The last time that data was sent to other host. */
    private long              _lastDataSendTime;

    /** The last time that a message was received from the other host. */
	private long              _lastReceivedTime;
    
    /** The last time we received a non-keepalive message. */
    private long              _lastDataOrAckTime;

    /** The number of resends to take into account when scheduling ack wait. */
    private int               _ackResendCount;

    /** Skip a Data Write if this flag is true. */
    private boolean           _skipADataWrite;

    /** Keep track of the reason for shutting down. */
    private byte              _closeReasonCode;

    ////////////////////////////////////////////
    // Some settings related to skipping acks
    ///////////////////////////////////////////
    
    /** Whether to skip any acks at all. */
    private final boolean _skipAcks;
    
    /** How long each measuring period is. */
    private final int _period;
    
    /** How many periods to keep track of. */
    private final int _periodHistory;
    
    /** 
     * By how much does the current period need to deviate from the average
     * before we start acking.
     */
    private final float _deviation;
    
    /** Do not skip more than this many acks in a row. */
    private final int _maxSkipAck;
    
    /** how many data packets we got each second. */
    private final int [] _periods;
    
    /** index within that array, points to the last period. */
    private int _currentPeriodId;
    
    /** How many data packets we received this period. */
    private int _packetsThisPeriod;
    
    /** whether we have enough data. */
    private boolean _enoughData;
    
    /** when the current second started. */
    private long _lastPeriod;
    
    /** how many acks we skipped in a row vs. total. */
    private int _skippedAcks, _skippedAcksTotal;
    
    /** how many packets we got in total.*/
    private int _totalDataPackets;
   
    /** The context containing various aspects required for RUDP. */
    private final RUDPContext _context;

    private final Role role;
        
    private final EventBroadcaster<UDPSocketChannelConnectionEvent> _connectionStateEventBroadcaster;

        /** Creates a new unconnected UDPConnectionProcessor. 
     * @param role defines the role it plays in the communication, either 
     * requestor or acceptor. 
     */
    protected UDPConnectionProcessor(UDPSocketChannel channel,
                                     RUDPContext context,
                                     Role role,
                                     EventBroadcaster<UDPSocketChannelConnectionEvent> connectionStateEventBroadcaster) {
        // Init default state
        _context                 = context;
        this.role = role;
        this._connectionStateEventBroadcaster = connectionStateEventBroadcaster;
        _theirConnectionID       = UDPMultiplexor.UNASSIGNED_SLOT; 
        _lastSendTime            = 0l;
        _lastDataSendTime        = 0l;
        _chunkLimit              = DATA_WINDOW_SIZE;
        _receiverWindowSpace     = DATA_WINDOW_SIZE; 
        _waitingForDataSpace     = false;
        _waitingForDataAvailable = false;
        _waitingForFinAck        = false;  
        _skipADataWrite          = false;
        _ackResendCount          = 0;
        _closeReasonCode         = FinMessage.REASON_NORMAL_CLOSE;
        _channel                 = channel;
        setConnectionState(ConnectionState.PRECONNECT);

        _scheduler         = UDPScheduler.instance();

        // Precreate the receive window for response reporting
        _receiveWindow   = new DataWindow(DATA_WINDOW_SIZE, 1);

        // All incoming seqNo and windowStarts get extended
        // Acks seqNo need to be extended separately
        _localExtender     = new SequenceNumberExtender();
        _extender          = new SequenceNumberExtender();
        
        _skipAcks = _context.getRUDPSettings().isSkipAcksEnabled();
        _maxSkipAck = _context.getRUDPSettings().getMaxSkipAcks();
        _deviation = _context.getRUDPSettings().getMaxSkipDeviation();
        _period = _context.getRUDPSettings().getSkipAckPeriodLength();
        _periodHistory = _context.getRUDPSettings().getSkipAckHistorySize();
        _periods = new int[_periodHistory];
    }
        
    private void setConnectionState(ConnectionState newState) {
        _connectionState = newState;
        _connectionStateEventBroadcaster.broadcast(new UDPSocketChannelConnectionEvent(_channel, newState));
    }
    
    /**
     * Attempts to connect to the given IP/port.
     * 
     * @throws IOException
     */
    protected void connect(InetSocketAddress addr) throws IOException {
        // If UDP is not running or not workable, barf
        if (!_context.getUDPService().isListening() || !_context.getUDPService().isNATTraversalCapable()) {
            throw new IOException("udp isn't working");
        }        
        
        synchronized(this) {
            if(_connectionState != ConnectionState.PRECONNECT) {
                if(isConnected())
                    throw new AlreadyConnectedException();
                else if(isClosed())
                    throw new ClosedChannelException();
                else
                    throw new ConnectionPendingException();
            }
            setConnectionState(ConnectionState.CONNECTING);
            // Record their address in synchronized block, to avoid close() seeing
            // seeing a state != preconnecting with a null address
            _connectedTo = addr;
        }
        
        
        _startedConnecting = System.currentTimeMillis();
        _sequenceNumber = 0;

        if (LOG.isDebugEnabled())
            LOG.debug("Connecting to: " + addr);

        // See if you can establish a pseudo connection 
        // which means each side can send/receive a SYN and ACK
		tryToConnect();
    }

    public byte getTheirConnectionID() {
        return _theirConnectionID;
    }

    /** Sets the connection id this is using. */
    protected void setConnectionId(byte id) {
        this._myConnectionID = id;
    }
    
    /** Retrieves the InetSocketAddress this is connecting to. */
    protected InetSocketAddress getSocketAddress() {
        return _connectedTo;
    }
    
    /** Retrieves the DataWindow used for reading data. */
    protected DataWindow getReadWindow() {
        return _receiveWindow;
    }

    /** Returns the ready ops of this processor. */
    protected synchronized int readyOps() {
        if(isClosed())
            return 0xFF;
        else
            return (isConnectReady() ? SelectionKey.OP_CONNECT : 0) 
                 | (isReadReady()    ? SelectionKey.OP_READ    : 0)
                 | (isWriteReady()   ? SelectionKey.OP_WRITE   : 0);
    }
    
    /** Gets the connect readiness. */
    private boolean isConnectReady() {
        return _receivedSynAck
               && isConnecting()
               && _theirConnectionID != UDPMultiplexor.UNASSIGNED_SLOT;
    }
    
    /** Gets the read-readiness of this processor. */
    private boolean isReadReady() {
        return  _receiveWindow.hasReadableData();
    }
    
    /** Gets the write-readiness of this processor. */
    private boolean isWriteReady() {
        return isConnected() && _channel.getNumberOfPendingChunks() < getChunkLimit();
    }
    
    /**
     * Closes the connection.
     * 
     * @throws IOException
     */
    protected synchronized void close() throws IOException {
        // If closed then done
        if ( _connectionState == ConnectionState.FIN )
            throw new IOException("already closed");
        
        if(_connectEvent != null)
            _connectEvent.unregister();

        // Shutdown keepalive event callbacks
        if ( _keepaliveEvent  != null ) 
        	_keepaliveEvent.unregister();

        // Shutdown write event callbacks
        if ( _writeDataEvent != null ) 
            _writeDataEvent.unregister();

        // Shutdown ack timeout event callbacks
        if ( _ackTimeoutEvent != null ) 
            _ackTimeoutEvent.unregister();

        // Unregister the safeWriteWakeup handler
        if ( _safeWriteWakeup != null ) 
            _safeWriteWakeup.unregister();
        
        // Store the old state.
        ConnectionState oldState = _connectionState;

        // Register that the connection is closed
        setConnectionState(ConnectionState.FIN);

        // Track incoming ACKS for an ack of FinMessage
        _waitingForFinAck = true;  

        // Tell the receiver that we are shutting down
        if(oldState != ConnectionState.PRECONNECT) {
            safeSendFin();

            // Register for a full cleanup after a slight delay
            if (_closedCleanupEvent==null) {
                _closedCleanupEvent = new ClosedConnectionCleanupTimerEvent(
                        System.currentTimeMillis() + SHUTDOWN_DELAY_TIME,this);
                LOG.debug("registering a closedCleanupEvent");
                _scheduler.register(_closedCleanupEvent);
            }
        }
    }

    private synchronized void finalClose() {

        // Send one final Fin message if not acked.
        if (_waitingForFinAck)
            safeSendFin();

        // Clean up my caller
        _closedCleanupEvent.unregister();

        _scheduler.register(new ChannelCloseTimerEvent(System.currentTimeMillis() +
                                                       CHANNEL_SHUTDOWN_DELAY, this));
    }
    
    /**
     *  Prepare for handling an open connection.
     */
    protected synchronized boolean prepareOpenConnection() throws IOException {
        if(isClosed())
            throw new ClosedChannelException();
        if(isConnected())
            return true;
        
        if(!_receivedSynAck || _theirConnectionID == UDPMultiplexor.UNASSIGNED_SLOT)
            return false;
            
        setConnectionState(ConnectionState.CONNECTED);
        _sequenceNumber = 1;
        scheduleKeepAlive();

        // Create the delayed connection components
        _sendWindow = new DataWindow(DATA_WINDOW_SIZE, 1);
        _writeRegulator = new WriteRegulator(_sendWindow);

        // Precreate the event for rescheduling writing to allow
        // thread safety and faster writing
        _safeWriteWakeup = new SafeWriteWakeupTimerEvent(Long.MAX_VALUE, this);
        _scheduler.register(_safeWriteWakeup);

        // Keep chunkLimit in sync with window space
        _chunkLimit      = _sendWindow.getWindowSpace();
        return true;
    
    }

    /**
     *  Make sure any firewall or NAT (Network Address Translation) stays open by scheduling a keepalive 
     *  message before the connection should close.
     *
     *  This just fires and reschedules itself appropriately so that we 
     *  don't need to worry about rescheduling as every new message is sent.
     */
    private synchronized void scheduleKeepAlive() {
        // Create event with initial time
        _keepaliveEvent  = 
          new KeepAliveTimerEvent(_lastSendTime + KEEPALIVE_WAIT_TIME,this);

        // Register keepalive event for future event callbacks
        _scheduler.register(_keepaliveEvent);

        // Schedule the first keepalive event callback
        _scheduler.scheduleEvent(_keepaliveEvent);
    }

    /**
     *  Setup and schedule the callback event for writing data.
     */
    private synchronized void scheduleWriteDataEvent(long time) {
        if ( isConnected() ) {
            if ( _writeDataEvent == null ) {
                _writeDataEvent  = 
                    new WriteDataTimerEvent(time,this);

                // Register writeData event for future use
                _scheduler.register(_writeDataEvent);
            } else {
                _writeDataEvent.updateTime(time);
            }

            // Notify the scheduler that there is a new write event/time
            _scheduler.scheduleEvent(_writeDataEvent);
            if(LOG.isDebugEnabled())  {
                LOG.debug("scheduleWriteDataEvent :"+time);
            }
        }
    }

    /**
     *  Activate writing if we were waiting for space
     */
    private synchronized void writeSpaceActivation() {
        if ( _waitingForDataSpace ) {
            _waitingForDataSpace = false;

            // Schedule immediately
            scheduleWriteDataEvent(0);
        }
    }

    /**
     *  Activate writing if we were waiting for data to write
     */
    private synchronized void writeDataActivation() {
        // Schedule at a reasonable time
        long rto = _sendWindow.getRTO();
        scheduleWriteDataEvent( _lastDataSendTime + (rto/4) );
	}

    /**
     *  Hand off the wakeup of data writing to the scheduler
     */
    protected void wakeupWriteEvent(boolean force) {
        if (force || _waitingForDataAvailable ) {
            LOG.debug("wakupWriteEvent");
            if (_safeWriteWakeup.getEventTime() == Long.MAX_VALUE) {
                _safeWriteWakeup.updateTime(System.currentTimeMillis()+
                  WRITE_WAKEUP_DELAY_TIME);
                _scheduler.scheduleEvent(_safeWriteWakeup);
            }
        }
    }
    
    /**
     * Setup and schedule the callback event for ensuring we resend SYNs on Connect.
     */
    private synchronized void scheduleConnectEvent(long time) {
        if(_connectEvent == null) {
            _connectEvent = new ConnectSynEvent(time, this);
            _scheduler.register(_connectEvent);
        } else {
            _connectEvent.updateTime(time);
        }
        _scheduler.scheduleEvent(_connectEvent);
    }
    
    /**
     *  Setup and schedule the callback event for ensuring data gets acked.
     */
    private synchronized void scheduleAckTimeoutEvent(long time) {
        if ( isConnected() ) {
            if ( _ackTimeoutEvent == null ) {
                _ackTimeoutEvent  = 
                    new AckTimeoutTimerEvent(time,this);

                // Register ackTimout event for future use
                _scheduler.register(_ackTimeoutEvent);
            } else {
                _ackTimeoutEvent.updateTime(time);
            }

            // Notify the scheduler that there is a new ack timeout event
            _scheduler.scheduleEvent(_ackTimeoutEvent);
        }
    }

    /**
     *  Suppress ack timeout events for now.
     */
    private synchronized void unscheduleAckTimeoutEvent() {
        // Nothing required if not initialized
        if ( _ackTimeoutEvent == null )
            return;

        // Set an existing event to an infinite wait
        // Note: No need to explicitly inform scheduler.
        _ackTimeoutEvent.updateTime(Long.MAX_VALUE);
    }

    /**
     *  Determine if an ackTimeout should be rescheduled.  
     */
    private synchronized boolean isAckTimeoutUpdateRequired() {
        // If ack timeout not yet created then yes.
        if ( _ackTimeoutEvent == null ) 
            return true;

        // If ack timeout exists but is infinite then yes an update is required.
        return (_ackTimeoutEvent.getEventTime() == Long.MAX_VALUE);
    }

    /**
     *  Test whether the connection is in connecting mode.
     */
    protected synchronized boolean isConnected() {
        return (_connectionState == ConnectionState.CONNECTED && 
                _theirConnectionID != UDPMultiplexor.UNASSIGNED_SLOT);
    }

    /**
     *  Test whether the connection is closed.
     */
    protected synchronized boolean isClosed() {
        return (_connectionState == ConnectionState.FIN);
    }

    /**
     *  Test whether the connection is not fully setup.
     */
    protected synchronized boolean isConnecting() {
        // It is important to check for either CONNECTING_STATE
        // or UNASSIGNED_SLOT because the state is advanced when
        // an ACK to our syn is received, and the connectionId is
        // changed when a SYN is received.  Either of these events
        // can happen in any order, and only when both happen
        // do we consider ourselves connected.
        
        return !isClosed() && 
            (_connectionState == ConnectionState.CONNECTING ||
                    (_connectionState != ConnectionState.PRECONNECT &&
                     _theirConnectionID == UDPMultiplexor.UNASSIGNED_SLOT)
            );
    }

    /**
     *  Return the room for new local incoming data in chunks. This should 
     *  remain equal to the space available in the sender and receiver 
     *  data window.
     */
    protected int getChunkLimit() {
        return Math.min(_chunkLimit, _receiverWindowSpace);
    }

    /**
     *  Convenience method for sending keepalive message since we might fire 
     *  these off before waiting
     */
    protected void sendKeepAlive() {
        KeepAliveMessage keepalive = null;
        try {  
            keepalive = _context.getMessageFactory().createKeepAliveMessage(
                 _theirConnectionID, 
                _receiveWindow.getWindowStart(), 
                _receiveWindow.getWindowSpace());
            send(keepalive);
        } catch(IllegalArgumentException iae) {
            // Report an error since this shouldn't ever happen
            ErrorService.error(iae);
            closeAndCleanup(FinMessage.REASON_SEND_EXCEPTION); 
        }
    }

    /**
     *  Convenience method for sending data.  
     */
    private synchronized void sendData(ByteBuffer chunk) {
        try {
            assert chunk.position() == 0;
            DataMessage dm = _context.getMessageFactory().createDataMessage(_theirConnectionID, _sequenceNumber, chunk); 
            send(dm);
            DataRecord drec   = _sendWindow.addData(dm);  
            drec.sentTime     = _lastSendTime;
            drec.sends++;

            if( LOG.isDebugEnabled() && 
               (_lastSendTime - _lastDataSendTime) > 2000)  {
                LOG.debug("SendData lag = "+
                  (_lastSendTime - _lastDataSendTime));
            }

            // Record when data was sent for future scheduling
            _lastDataSendTime = _lastSendTime;

            // Update the chunk limit for fast (nonlocking) access
            _chunkLimit = _sendWindow.getWindowSpace();

            _sequenceNumber++;

            // If Acking check needs to be woken up then do it
            if ( isAckTimeoutUpdateRequired()) 
                scheduleAckTimeoutIfNeeded();

            // Predecrement the other sides window until I here otherwise.
            // This prevents a cascade of sends before an Ack
            if ( _receiverWindowSpace > 0 )
                _receiverWindowSpace--;

        } catch(IllegalArgumentException iae) {
            // Report an error since this shouldn't ever happen
            ErrorService.error(iae);
            closeAndCleanup(FinMessage.REASON_SEND_EXCEPTION);
        }
    }

    /**
     *  Build and send an ack with default error handling with
     *  the messages sequenceNumber, receive window start and 
     *  receive window space.
     */
    private synchronized void safeSendAck(RUDPMessage msg) {
        // Ack the message
        AckMessage ack = null;
        try {
          ack = _context.getMessageFactory().createAckMessage(_theirConnectionID, msg.getSequenceNumber(), _receiveWindow.getWindowStart(), _receiveWindow.getWindowSpace());
          
          	if (LOG.isDebugEnabled()) {
          	    LOG.debug("total data packets "+_totalDataPackets+
          	            " total acks skipped "+_skippedAcksTotal+
          	            " skipped this session "+ _skippedAcks);
          	}
          	_skippedAcks=0;
            send(ack);
        } catch(IllegalArgumentException iae) {
            // Report an error since this shouldn't ever happen
            ErrorService.error(iae);
            closeAndCleanup(FinMessage.REASON_SEND_EXCEPTION);
        }
    }

    /**
     *  Build and send a fin message with default error handling.
     */
    private synchronized void safeSendFin() {
        // Ack the message
        FinMessage fin = null;
        try {
            // Record sequence number for ack monitoring
            // Not that it should increment anymore anyways
            _finSeqNo = _sequenceNumber;

            // Send the FinMessage
            fin = _context.getMessageFactory().createFinMessage(_theirConnectionID, _sequenceNumber, _closeReasonCode);
            send(fin);
        } catch(IllegalArgumentException iae) {
            // Report an error since this shouldn't ever happen
            ErrorService.error(iae);
            LOG.warn("calling recursively closeAndCleanup");
            closeAndCleanup(FinMessage.REASON_SEND_EXCEPTION);
        }
    }


    /**
     *  Send a message on to the UDPService.
     */
    private synchronized void safeSend(RUDPMessage msg) {
        try {
            send(msg); 
        } catch(IllegalArgumentException iae) {
            // Report an error since this shouldn't ever happen
            ErrorService.error(iae);
            closeAndCleanup(FinMessage.REASON_SEND_EXCEPTION);
        }
    }


    /**
     *  Send a message on to the UDPService.
     */
	private synchronized void send(RUDPMessage msg) 
      throws IllegalArgumentException {
		_lastSendTime = System.currentTimeMillis();
        if(msg instanceof DataMessage || msg instanceof AckMessage)
            _lastDataOrAckTime = _lastSendTime;
        
        if(LOG.isDebugEnabled())  {
            LOG.debug("send:" + msg + " to: " + _connectedTo + ", t:" + _lastSendTime);
            if ( msg instanceof FinMessage ) { 
            	Exception ex = new Exception();
            	LOG.debug("", ex);
            }
        }
        _context.getUDPService().send(msg, _connectedTo);  
	}

    /**
     *  Schedule an ack timeout for the oldest unacked data.
     *  If no acks are pending, then do nothing.
     */
    private synchronized void scheduleAckTimeoutIfNeeded() {
        DataRecord drec = _sendWindow.getOldestUnackedBlock();
        if ( drec != null ) {
            int rto         = _sendWindow.getRTO();
            if (rto == 0) 
                rto = (int) DEFAULT_RTO_WAIT_TIME;
            long waitTime    = drec.sentTime + rto;

            // If there was a resend then base the wait off of current time
            if ( _ackResendCount > 0 ) {
                waitTime    = _lastSendTime + rto;
               _ackResendCount = 0;
            }

            // Enforce a minimum waitTime from now
            long minTime = System.currentTimeMillis() + MIN_ACK_WAIT_TIME;
            waitTime = Math.max(waitTime, minTime);

            scheduleAckTimeoutEvent(waitTime);
        } else {
            unscheduleAckTimeoutEvent();
        }
    }

    /**
     *  Ensure that data is getting acked.  If not within an appropriate time, 
     *  then resend.
     */
    private synchronized void validateAckedData() {
        long currTime = System.currentTimeMillis();

        if (_sendWindow.acksAppearToBeMissing(currTime, 1)) {

            // if the older blocks ack have been missing for a while
            // resend them.

            // Calculate a good maximum time to wait
            int rto      = _sendWindow.getRTO();

            long start   = _sendWindow.getWindowStart();

            if(LOG.isDebugEnabled())  
              LOG.debug("Soft resend check:"+ start+ " rto:"+rto+
                " uS:"+_sendWindow.getUsedSpots()+" localSeq:"+_sequenceNumber);

            DataRecord drec;
      //      DataRecord drecNext;
            int        numResent = 0;

            // Resend up to 1 packet at a time
            resend: {

                // Get the oldest unacked block out of storage
                drec     = _sendWindow.getOldestUnackedBlock();
                int expRTO = (rto * (int)Math.pow(2,drec.sends-1));
                if (LOG.isDebugEnabled())
                	LOG.debug(" exponential backoff is now "+expRTO);

                // Check if the next drec is acked
                if(_sendWindow.countHigherAckBlocks() >0){
                    expRTO*=0.75;
                    if (LOG.isDebugEnabled())
                        LOG.debug(" higher acked blocks, adjusting exponential backoff is now "+
                            expRTO);
                }

                // The assumption is that this record has not been acked
                // FIXME this condition is never true
                if ( drec.acks > 0) 
                	break resend;
                

                // If too many sends then abort connection
                if ( drec.sends > MAX_SEND_TRIES+1 ) {
                    if(LOG.isDebugEnabled())  
                        LOG.debug("Tried too many send on:"+
                          drec.msg.getSequenceNumber());
                    closeAndCleanup(FinMessage.REASON_TOO_MANY_RESENDS);
                    return;
                }

                int currentWait = (int)(currTime - drec.sentTime);

                // If it looks like we waited too long then speculatively resend
                // Case 1: We waited 150% of RTO and next packet had been acked
                // Case 2: We waited 200% of RTO 
                if ( currentWait  > expRTO) {
                    if(LOG.isDebugEnabled())  
                        LOG.debug("Soft resending message:"+
                          drec.msg.getSequenceNumber());
                    safeSend(drec.msg);

                    // Scale back on the writing speed if you are hitting limits
                    _writeRegulator.addMessageFailure();
                    _writeRegulator.hitResendTimeout();

                    currTime      = _lastSendTime;
                    drec.sentTime = currTime;
                    drec.sends++;
                    numResent++;
                } else 
                    LOG.debug(" not resending message ");
                
            }
            
            // Delay subsequent resends of data based on number resent
            _ackResendCount = numResent;
            if ( numResent > 0 )
                _skipADataWrite          = true;
        } 
        scheduleAckTimeoutIfNeeded();
    }

    /**
     *  Close and cleanup by unregistering this connection and sending a Fin.
     */
    private synchronized void closeAndCleanup(byte reasonCode) {
        _closeReasonCode = reasonCode;
        try {
           close();
        } catch (IOException ioe) {}
    }


    // ------------------  Connection Handling Logic -------------------
    //
    /**
     *  Send SYN messages to desired host and wait for Acks and their 
     *  SYN message.  Schedules an event to periodically resend a Syn.
     */
    private synchronized void tryToConnect() {
        if (!isConnecting()) {
            LOG.debug("Already connected");
            if(_connectEvent != null)
                _connectEvent.unregister();
            return;
        }
        
        // Keep track of how long you are waiting on connection
        long now = System.currentTimeMillis();
        long waitTime = now - _startedConnecting;
        if (waitTime > MAX_CONNECT_WAIT_TIME) {
            LOG.debug("Timed out, waited for: " + waitTime);
            setConnectionState(ConnectionState.FIN);
            _channel.eventPending();
        } else {
            // We cannot send the SYN until we've registered in the Multiplexor.
            if(_myConnectionID != 0) {
                // Build SYN message with my connectionID in it
                SynMessage synMsg;
                if (_theirConnectionID != UDPMultiplexor.UNASSIGNED_SLOT)
                    synMsg = _context.getMessageFactory().createSynMessage(_myConnectionID, _theirConnectionID, role);
                else
                    synMsg = _context.getMessageFactory().createSynMessage(_myConnectionID, role);
    
                LOG.debug("Sending SYN: " + synMsg);
                // Send a SYN packet with our connectionID
                send(synMsg);
            }
            
            scheduleConnectEvent(now + SYN_WAIT_TIME);
        }
	}
    
    /**
     * Handles an incoming SYN message. All initial and/or duplicate SYNs are acked.
     * We set theirConnectionID once we see the first SYN. If a subsequent SYN has a different ID,
     * that SYN is ignored.
     */
    private void handleSynMessage(SynMessage smsg) {
        // Extend the msgs sequenceNumber to 8 bytes based on past state
        smsg.extendSequenceNumber(
          _extender.extendSequenceNumber(
            smsg.getSequenceNumber()) );

        // First Message from other host - get his connectionID.
        byte       theirConnID = smsg.getSenderConnectionID();
        LOG.debugf("our id: {0}, their id: {1}, their sender id: {2}", _theirConnectionID, theirConnID, smsg.getSenderConnectionID());
        if ( _theirConnectionID == UDPMultiplexor.UNASSIGNED_SLOT ) {
            // Keep track of their connectionID
            _theirConnectionID = theirConnID;
        } else if ( _theirConnectionID == theirConnID ) {
            // Getting a duplicate SYN so just ack it again.
        } else {
            // Unmatching SYN so just ignore it
            return;
        }

        // Ack their SYN message
        safeSendAck(smsg);
    }
    
    /**
     * Handles an ACK message.
     * <p>
     * If we're connecting, the first received ACK will advance the state
     * to CONNECTED_STATE.  Duplicate ACKs while connecting will be ignored.
     * (Even though the state is moved to CONNECTED_STATE, we may not be 
     *  be isConnected() until we also receive a SYN from them, informing
     *  us of their connection id.)
     * <p>
     * ACKs received in response to a FIN are tracked so that another FIN may
     * be sent if the first was not acked (allowing the remote side to see that
     * the connection was shutdown).
     * <p>
     * ACKs received while connected update the appropriate window & regulator
     * structures.
     */
    private void handleAckMessage(AckMessage amsg) {
        // Extend the msgs sequenceNumber to 8 bytes based on past state
        // Note that this sequence number is of local origin
        amsg.extendSequenceNumber(
          _localExtender.extendSequenceNumber(
            amsg.getSequenceNumber()) );

        // Extend the windowStart to 8 bytes the same as the 
        // sequenceNumber 
        amsg.extendWindowStart(
          _localExtender.extendSequenceNumber(amsg.getWindowStart()) );

        long          seqNo  = amsg.getSequenceNumber();
        long          wStart = amsg.getWindowStart();
        int           priorR = _receiverWindowSpace;
        _receiverWindowSpace = amsg.getWindowSpace();

        // Adjust the receivers window space with knowledge of
        // how many extra messages we have sent since this ack
        if ( _sequenceNumber > wStart ) 
            _receiverWindowSpace = 
              DATA_WINDOW_SIZE + (int) (wStart - _sequenceNumber);
            //_receiverWindowSpace += (wStart - _sequenceNumber);

        // Reactivate writing if required
        if ( (priorR == 0 || _waitingForDataSpace) && 
             _receiverWindowSpace > 0 ) {
            if(LOG.isDebugEnabled())  
                LOG.debug(" -- ACK wakeup");
            writeSpaceActivation();
        }


        if ( seqNo == 0 && isConnecting()) {
            _receivedSynAck = true;
        } else if ( _waitingForFinAck && seqNo == _finSeqNo ) { 
            // A fin message has been acked on shutdown
            _waitingForFinAck = false;
        } else if (_connectionState == ConnectionState.CONNECTED) {
            // Record the ack
            _sendWindow.ackBlock(seqNo);
            _writeRegulator.addMessageSuccess();

            // Ensure that all messages up to sent windowStart are acked
            _sendWindow.pseudoAckToReceiverWindow(amsg.getWindowStart());
            
            // Clear out the acked blocks at window start
            _sendWindow.clearLowAckedBlocks(_channel);  

            // Update the chunk limit for fast (nonlocking) access
            _chunkLimit = _sendWindow.getWindowSpace();
        }
    }
    
    /**
     * Handles a DataMessage.
     * <p>
     * This will close the connection if the data size is too large.
     * <p>
     * If the sequence of the msg is below our start window
     * (meaning we've already gobbled up this data), then the msg is
     * ignored (but may be acked).
     * <p>
     * If the msg fits in our window space, we'll add to the incoming
     * DataWindow.
     * <p>
     * An ack may be sent out to signify that we successfully received
     * the message.  
     * 
     */
    private void handleDataMessage(DataMessage dmsg) {
        
        // Extend the msgs sequenceNumber to 8 bytes based on past state
        dmsg.extendSequenceNumber(
          _extender.extendSequenceNumber(
            dmsg.getSequenceNumber()) );

        // Pass the data message to the output window

        // If message is more than limit beyond window, 
        // then throw it away
        long seqNo     = dmsg.getSequenceNumber();
        long baseSeqNo = _receiveWindow.getWindowStart();

        // If data is too large then blow out the connection
        // before any damage is done
        if (dmsg.getDataLength() > MAX_DATA_SIZE) {
            closeAndCleanup(FinMessage.REASON_LARGE_PACKET);
            return;
        }

        if ( seqNo > (baseSeqNo + DATA_WRITE_AHEAD_MAX) ) {
            if(LOG.isDebugEnabled())  
                LOG.debug("Received block num too far ahead: "+ seqNo);
           return;
        }
        
        // Make sure the data is not before the window start
        DataRecord drec = null;
        if ( seqNo >= baseSeqNo ) {
            // Record the receipt of the data in the receive window
            drec = _receiveWindow.addData(dmsg);  
        } else {
            if(LOG.isDebugEnabled())  
                LOG.debug("Received duplicate block num: "+ 
                  dmsg.getSequenceNumber());
        }

        //if this is the first data message we get, start the period now
        if (_lastPeriod == 0)
            _lastPeriod = _lastReceivedTime;
        
        _packetsThisPeriod++;
        _totalDataPackets++;
        
        if (shouldSendAck()) {
            if (drec != null) {
                drec.ackTime = System.currentTimeMillis();
                drec.acks++;
            }
            safeSendAck(dmsg);
        }
        
        // if this is the end of a period, record how many data packets we got
        if (_lastReceivedTime - _lastPeriod >= _period) {
            _lastPeriod = _lastReceivedTime;
            _currentPeriodId++;
            if (_currentPeriodId >= _periodHistory) {
                _currentPeriodId=0;
                _enoughData=true;
            }
            _periods[_currentPeriodId]=_packetsThisPeriod;
            _packetsThisPeriod=0;
        }
    }

    private boolean shouldSendAck() {
        //if we have enough history, see if we should skip an ack
        if (_skipAcks && _enoughData && _skippedAcks < _maxSkipAck) {
            float average = 0;
            for (int i = 0;i < _periodHistory;i++)
                average+=_periods[i];
            
            average /= _periodHistory;
            
            // skip an ack if the rate at which we receive data has not dropped sharply
            if (_periods[_currentPeriodId] > average / _deviation) {
                _skippedAcks++;
                _skippedAcksTotal++;
                return false;
            }
        }
         
        return true;
    }
    
    /**
     * Handles a KeepAliveMessage.
     * <p>
     * If we're closed, a Fin message is sent in reply.
     * All sent messages up to the keep alive's window-start are acked,
     * and the window space for the remote side is updated.
     */
    private void handleKeepAliveMessage(KeepAliveMessage kmsg) {
        // No need to extend seqNo on KeepAliveMessage since it is zero
        // Extend the windowStart to 8 bytes the same 
        // as the Ack
        kmsg.extendWindowStart(
          _localExtender.extendSequenceNumber(kmsg.getWindowStart()) );

    //    long             seqNo  = kmsg.getSequenceNumber();
        long             wStart = kmsg.getWindowStart(); 
        int              priorR = _receiverWindowSpace;
        _receiverWindowSpace    = kmsg.getWindowSpace();

        //System.out.println("Keep alive: remote.windowStart=" + kmsg.getWindowStart() + ", remote.windowSpace=" + kmsg.getWindowSpace() + " local.sequenceNumber=" + _sequenceNumber + ", local.receiverWindowSpace=" + _receiverWindowSpace + ", local.chunkLimit=" + _chunkLimit + ", sendWindow.windowStart=" + _sendWindow.getWindowStart() + ", sendWindow.windowSize=" + _sendWindow.getWindowSize() + ", sendWindow.windowSpace=" + _sendWindow.getWindowSpace());
        
        // Adjust the receivers window space with knowledge of
        // how many extra messages we have sent since this ack
        if ( _sequenceNumber > wStart ) 
            _receiverWindowSpace = 
              DATA_WINDOW_SIZE + (int) (wStart - _sequenceNumber);
            //_receiverWindowSpace += (wStart - _sequenceNumber);

        // If receiving KeepAlives when closed, send another FinMessage
        if ( isClosed() ) {
            safeSendFin();
        }

        // Ensure that all messages up to sent windowStart are acked
        // Note, you could get here preinitialization - in which case,
        // do nothing.
        if ( _sendWindow != null ) {  
            _sendWindow.pseudoAckToReceiverWindow(wStart);
            
            // Clear out the acked blocks at window start
            _sendWindow.clearLowAckedBlocks(_channel);
            
            // Update the chunk limit for fast (nonlocking) access
            _chunkLimit = _sendWindow.getWindowSpace();

            // Reactivate writing if required
            if ( (priorR == 0 || _waitingForDataSpace) && 
                 _receiverWindowSpace > 0 ) {
                if(LOG.isDebugEnabled()) 
                    LOG.debug(" -- KA wakeup");
                writeSpaceActivation();
            }
        }

    }
    
    /**
     * Handles a Fin message.
     * <p>
     * This will close the connection.
     */
    private void handleFinMessage(FinMessage msg) {
        // Extend the msgs sequenceNumber to 8 bytes based on past state
        msg.extendSequenceNumber(
          _extender.extendSequenceNumber(
            msg.getSequenceNumber()) );

        // Stop sending data
        _receiverWindowSpace    = 0;

        // Ack the Fin message
        safeSendAck(msg);

        // If a fin message is received then close connection
        if ( !isClosed() )
            closeAndCleanup(FinMessage.REASON_YOU_CLOSED);
    }


    /**
     *  Take action on a received message.
     */
    protected synchronized void handleMessage(RUDPMessage msg) {
        // Record when the last message was received
        _lastReceivedTime = System.currentTimeMillis();
        if (LOG.isDebugEnabled())
            LOG.debug("handleMessage :" + msg + " t:" + _lastReceivedTime);

        if (msg instanceof SynMessage) {
            handleSynMessage((SynMessage) msg);
        } else if (msg instanceof AckMessage) {
            _lastDataOrAckTime = _lastReceivedTime;
            handleAckMessage((AckMessage) msg);
        } else if (msg instanceof DataMessage) {
            _lastDataOrAckTime = _lastReceivedTime;
            handleDataMessage((DataMessage) msg);
        } else if (msg instanceof KeepAliveMessage) {
            handleKeepAliveMessage((KeepAliveMessage) msg);
        } else if (msg instanceof FinMessage) {
            handleFinMessage((FinMessage) msg);
        }
    }

    /**
     * If there is data to be written then write it and schedule next write time.
     */
    private synchronized void writeData() {
        // Make sure we don't write without a break for too long
        int noSleepCount = 0;
        
        while (true) {
            // Reset special flags for long wait times
            _waitingForDataAvailable = false;
            _waitingForDataSpace = false;

            // If someone wanted us to wait a bit then don't send data now
            if ( _skipADataWrite ) {
                _skipADataWrite = false;
            } else {  // Otherwise, it is safe to send some data
            
                // If there is room to send something then send data 
                // if available
                if ( getChunkLimit() > 0 ) {
                    // Get data and send it
                    ByteBuffer chunk = _channel.getNextChunk();
                    if(chunk != null)
                        sendData(chunk);
                } else {
                    //System.out.println("Waiting: sequenceNumber=" + _sequenceNumber + ", receiverWindowSpace=" + _receiverWindowSpace + ", chunkLimit=" + _chunkLimit + ", sendWindow.windowStart=" + _sendWindow.getWindowStart() + ", sendWindow.windowSize=" + _sendWindow.getWindowSize() + ", sendWindow.windowSpace=" + _sendWindow.getWindowSpace());

                    // if no room to send data then wait for the window to Open
                    // Don't wait more than 1 second for sanity checking 
                    scheduleWriteDataEvent(
                      System.currentTimeMillis() + NOTHING_TO_DO_DELAY);
                    _waitingForDataSpace = true;

                    if(LOG.isDebugEnabled())  
                        LOG.debug("Shutdown SendData cL:"+_chunkLimit+
                                " rWS:"+ _receiverWindowSpace);
                    return;
                }
            }

            // Don't wait for next write if there is no chunk available.
            // Writes will get rescheduled if a chunk becomes available.
            synchronized(_channel.writeLock()) {
                if (_channel.getNumberOfPendingChunks() == 0 ) {
                    // Don't wait more than 1 second for sanity checking 
                    scheduleWriteDataEvent(
                      System.currentTimeMillis() + NOTHING_TO_DO_DELAY);
                    _waitingForDataAvailable = true;
                    if(LOG.isDebugEnabled())  
                        LOG.debug("Shutdown SendData no pending");
                    return;
                }
            }
            
            // Compute how long to wait
            // TODO: Simplify experimental algorithm and plug it in
            //long waitTime = (long)_sendWindow.getRTO() / 6l;
            long currTime = System.currentTimeMillis();
            long waitTime = _writeRegulator.getSleepTime(currTime, 
              _receiverWindowSpace);

            // If we are getting too close to the end of window, make a note
            if ( _receiverWindowSpace <= SMALL_SEND_WINDOW ) { 

                // Scale back on the writing speed if you are hitting limits
                if ( _receiverWindowSpace <= 1 ) 
                    _writeRegulator.hitZeroWindow();
            }

            // Initially ensure waitTime is not too low
            if (waitTime == 0 && _sequenceNumber < 10 ) 
                waitTime = DEFAULT_RTO_WAIT_TIME;

            // Enforce some minimal sleep time if we have been in tight loop
            // This will allow handleMessages to get done if pending
            if (noSleepCount >= MAX_WRITE_WITHOUT_SLEEP) {
                waitTime += 1;
            }

            // Only wait if the waitTime is more than zero
            if ( waitTime > 0 ) {
                long time = System.currentTimeMillis() + waitTime;
                //System.out.println("Write: sequenceNumber=" + _sequenceNumber + ", receiverWindowSpace=" + _receiverWindowSpace + ", chunkLimit=" + _chunkLimit + ", sendWindow.windowStart=" + _sendWindow.getWindowStart() + ", sendWindow.windowSize=" + _sendWindow.getWindowSize() + ", sendWindow.windowSpace=" + _sendWindow.getWindowSpace());
                scheduleWriteDataEvent(time);
                break;
            }

            // Count how long we are sending without a sleep
            noSleepCount++;
        }
    }

    /** 
     *  Define what happens when a keepalive timer fires.
     */
    static class KeepAliveTimerEvent extends UDPTimerEvent {
        
        public KeepAliveTimerEvent(long time,UDPConnectionProcessor proc) {
            super(time,proc);
        }

        @Override
        protected void doActualEvent(UDPConnectionProcessor udpCon) {
            long time = System.currentTimeMillis();
            
            if(LOG.isDebugEnabled())  
                LOG.debug("keepalive: "+ time);

            // If connection closed, then make sure that keepalives have ended

            if (udpCon.isClosed() ) {
                udpCon._keepaliveEvent.unregister();
                return;
            }
            
            if (udpCon.isConnected()
                    && (udpCon._lastDataOrAckTime + MAX_KEEPALIVE_TIME < time ||
                        udpCon._lastReceivedTime  + MAX_MESSAGE_WAIT_TIME < time)
               ) {
                LOG.debug("Keepalive generated shutdown");
                udpCon.closeAndCleanup(FinMessage.REASON_TIMEOUT);
                return;
            }
            
            // If reevaluation of the time still requires a keepalive then send
            if ( time+1 >= (udpCon._lastSendTime + KEEPALIVE_WAIT_TIME) ) {
                if ( udpCon.isConnected() ) {
                    udpCon.sendKeepAlive();
                } else {
                    return;
                }
            }

            // Reschedule keepalive timer
            _eventTime = udpCon._lastSendTime + KEEPALIVE_WAIT_TIME;
            udpCon._scheduler.scheduleEvent(this);
            if(LOG.isDebugEnabled())  
                LOG.debug("end keepalive: "+ System.currentTimeMillis());
        }
    }
    /** 
     *  Define what happens when a WriteData timer event fires.
     */
    static class WriteDataTimerEvent extends UDPTimerEvent {
        public WriteDataTimerEvent(long time,UDPConnectionProcessor proc) {
            super(time,proc);
        }

        @Override
        protected void doActualEvent(UDPConnectionProcessor udpCon) {        	
        	
            if(LOG.isDebugEnabled())  
                LOG.debug("data timeout :"+ System.currentTimeMillis());
            long time = System.currentTimeMillis();

            // Make sure that some messages are received within timeframe
            if ( udpCon.isConnected() && 
                    udpCon._lastReceivedTime + MAX_MESSAGE_WAIT_TIME < time ) {
                // If no incoming messages for very long time then 
                // close connection
                udpCon.closeAndCleanup(FinMessage.REASON_TIMEOUT);
                return;
            }

            // If still connected then handle then try to write some data
            if ( udpCon.isConnected() ) {
                udpCon.writeData();
            }
            if(LOG.isDebugEnabled())  
                LOG.debug("end data timeout: "+ System.currentTimeMillis());
        }
    }

    /** 
     *  Define what happens when an ack timeout occurs
     */
    static class AckTimeoutTimerEvent extends UDPTimerEvent {

        public AckTimeoutTimerEvent(long time,UDPConnectionProcessor proc) {
            super(time,proc);
        }

        @Override
        protected void doActualEvent(UDPConnectionProcessor udpCon) {

            if(LOG.isDebugEnabled())  
                LOG.debug("ack timeout: "+ System.currentTimeMillis());
            if ( udpCon.isConnected() ) {
                udpCon.validateAckedData();
            }
            if(LOG.isDebugEnabled())  
                LOG.debug("end ack timeout: "+ System.currentTimeMillis());
        }
    }

    /** 
     *  This is an event that wakes up writing with a given delay.
     */
    static class SafeWriteWakeupTimerEvent extends UDPTimerEvent {

        public SafeWriteWakeupTimerEvent(long time,UDPConnectionProcessor proc) {
            super(time,proc);
        }

        @Override
        protected void doActualEvent(UDPConnectionProcessor udpCon) {
        	
            if(LOG.isDebugEnabled())  
                LOG.debug("write wakeup timeout: "+ System.currentTimeMillis());
            if ( udpCon.isConnected() ) {
                udpCon.writeDataActivation();
            }
            _eventTime = Long.MAX_VALUE;
            udpCon._scheduler.scheduleEvent(this);
            if(LOG.isDebugEnabled())  
                LOG.debug("write wakeup timeout: "+ System.currentTimeMillis());
        }
    }
    
    /** 
     *  Event that will resend a Syn if we need to while connecting.
     */
    static class ConnectSynEvent extends UDPTimerEvent {

        public ConnectSynEvent(long time,UDPConnectionProcessor proc) {
            super(time,proc);
        }

        @Override
        protected void doActualEvent(UDPConnectionProcessor udpCon) {
            _eventTime = Long.MAX_VALUE;
            LOG.debug("Running SYN Event");
            udpCon.tryToConnect();
        }
    }    

    /** 
     *  Do final cleanup and shutdown after connection is closed.
     */
    static class ClosedConnectionCleanupTimerEvent extends UDPTimerEvent {

        public ClosedConnectionCleanupTimerEvent(long time, UDPConnectionProcessor proc) {
            super(time,proc );
        }

        @Override
        protected void doActualEvent(UDPConnectionProcessor udpCon) {

            if(LOG.isDebugEnabled())  
                LOG.debug("Closed connection timeout: "+ 
                  System.currentTimeMillis());

            udpCon.finalClose();


            if(LOG.isDebugEnabled())  
                LOG.debug("Closed connection done: "+ System.currentTimeMillis());
            
            unregister();
        }
    }
    

    /** 
     *  Makes sure the channel is closed after a certain amount of time.
     */
    static class ChannelCloseTimerEvent extends UDPTimerEvent {

        public ChannelCloseTimerEvent(long time, UDPConnectionProcessor proc) {
            super(time,proc );
        }

        @Override
        protected void doActualEvent(UDPConnectionProcessor udpCon) {
            try {
                udpCon._channel.close();
            } catch(IOException ignored) {}
            unregister();
        }
    }
}
