package com.limegroup.gnutella.connection;

import org.limewire.nio.channel.InterestReadableByteChannel;

public interface MessageReaderFactory {

    public MessageReader createMessageReader(MessageReceiver receiver);

    public MessageReader createMessageReader(
            InterestReadableByteChannel channel, MessageReceiver receiver);

}