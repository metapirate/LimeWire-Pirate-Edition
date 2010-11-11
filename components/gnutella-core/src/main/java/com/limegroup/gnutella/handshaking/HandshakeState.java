package com.limegroup.gnutella.handshaking;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.limewire.nio.statemachine.IOState;


/** Outlines all the states an asynchronous handshake can be in. */
class HandshakeState {
    
    /**
     * Returns a new set of HandshakeStates for doing an asynchronous incoming handshake.
     * 
     * @param support The HandshakeSupport supporter that'll keep track of what we're doing.
     * @param responder The responder to use when we write a response.
     * @return
     */
    static List<IOState> getIncomingHandshakeStates(HandshakeSupport support,
                                                    HandshakeResponder responder) {
        List<IOState> list = new ArrayList<IOState>(3);
        list.add(new ReadHandshakeState.ReadRequestState(support));
        list.add(new WriteHandshakeState.WriteResponseState(support, responder, false));
        list.add(new ReadHandshakeState.ReadResponseState(support));
        return list;
    }
    
    /**
     * Returns a new set of HandshakeStates for doing an asynchronous outgoing handshake.
     * 
     * @param support The HandshakeSupport supporter that'll keep track of what we're doing.
     * @param request The initial set of request headers to send.
     * @param responder The responder to use when we write a response.
     * @return
     */
    static List<IOState> getOutgoingHandshakeStates(HandshakeSupport support,
                                                    Properties request,
                                                    HandshakeResponder responder) {
        List<IOState> list = new ArrayList<IOState>(3);
        list.add(new WriteHandshakeState.WriteRequestState(support, request));
        list.add(new ReadHandshakeState.ReadResponseState(support));
        list.add(new WriteHandshakeState.WriteResponseState(support, responder, true));
        return list;
    }
    
    private HandshakeState() {}
}
