package org.limewire.nio.channel;


/**
 * Defines the interface of a class that can handle delegating 
 * <code>ChannelReadObserver</code> or <code>ChannelWriter</code> events to 
 * other <code>ChannelReadObservers</code> or <code>ChannelWriters</code>.
 */
public interface NIOMultiplexor {
    
    /**
     * Sets the new <code>ReadObserver</code>. A <code>ChannelReadObserver</code> 
     * is required so that the multiplexor can set the appropriate source channel
     * for reading. The source channel is set on the deepest 
     * <code>ChannelReader</code> in the chain. For example, given the chain:
     * <pre>
     *      ChannelReadObserver a = new ProtocolReader();
     *      ChannelReader b = new DeObfuscator();
     *      ChannelReader c = new DataInflater();
     *      a.setReadChannel(b);
     *      b.setReadChannel(c);
     *      setReadObserver(a);
     *      
     * the deepest ChannelReader is 'c', so the muliplexor would call
     *
     *      c.setReadChannel(ultimateSource);
     *
     * The deepest ChannelReader is found with code equivalent to:
     *
     *      ChannelReader deepest = initial;
     *      while(deepest.getReadChannel() instanceof ChannelReader)
     *          deepest = (ChannelReader)deepest.getReadChannel();
     *  </pre>
     */
    public void setReadObserver(ChannelReadObserver reader);
    
    /**
     * Sets the new <code>ChannelWriter</code>. A <code>ChannelWriter</code> is 
     * necessary (instead of a <code>WriteObserver</code>) because the actual 
     * <code>WriteObserver</code> that listens for write events from the 
     * ultimate source will be installed at the deepest
     * <code>InterestWriteChannel</code> in the chain. For example, given the chain:
     * <pre>
     *      ChannelWriter a = new ProtocolWriter();
     *      ChannelWriter b = new Obfuscator();
     *      ChannelWriter c = new DataDeflater();
     *      a.setWriteChannel(b);
     *      b.setWriteChannel(c);
     *      setWriteObserver(a);
     *      
     * the deepest ChannelWriter is 'c', so the multiplexor would call
     *      c.setWriteChannel(networkSink);
     * where networkSink is the network channel from where data is actually read.
     * 
     * The deepest ChannelWriter is found with code equivalent to:
     * 
     *      ChannelWriter deepest = initial;
     *      while(deepest.getWriteChannel() instanceof ChannelWriter)
     *          deepest = (ChannelWriter)deepest.getWriteChannel();
     *
     * </pre>
     * When write events are generated, <code>ultimateSource.handleWrite</code> 
     * will forward the event to the last channel that was interested in it ('c'),
     * which will cause 'c' to either write data immediately or forward the event
     * to 'b', etc.
     */
    public void setWriteObserver(ChannelWriter writer);
}