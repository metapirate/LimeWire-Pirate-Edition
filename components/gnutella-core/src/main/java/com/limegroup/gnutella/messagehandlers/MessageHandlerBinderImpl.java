package com.limegroup.gnutella.messagehandlers;

import com.google.inject.Singleton;
import com.limegroup.gnutella.MessageHandlerBinder;
import com.limegroup.gnutella.MessageRouter;
import com.limegroup.gnutella.messages.PushRequest;

@Singleton
public class MessageHandlerBinderImpl implements MessageHandlerBinder {

    public void bind(MessageRouter messageRouter) {
        AllNetworkPushRequestHandler pushRequestHandler = new AllNetworkPushRequestHandler(messageRouter);
        messageRouter.setMessageHandler(PushRequest.class, pushRequestHandler);
        messageRouter.setUDPMessageHandler(PushRequest.class, pushRequestHandler);
        messageRouter.setMulticastMessageHandler(PushRequest.class, pushRequestHandler);
    }

}
