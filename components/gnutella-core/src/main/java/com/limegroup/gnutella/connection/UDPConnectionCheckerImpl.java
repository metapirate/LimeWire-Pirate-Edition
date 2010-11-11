package com.limegroup.gnutella.connection;

import java.util.Collection;

import org.limewire.collection.Cancellable;
import org.limewire.io.IpPort;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.limegroup.gnutella.ConnectionServices;
import com.limegroup.gnutella.MessageListener;
import com.limegroup.gnutella.ReplyHandler;
import com.limegroup.gnutella.UniqueHostPinger;
import com.limegroup.gnutella.UDPService;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.PingRequest;
import com.limegroup.gnutella.messages.PingRequestFactory;

public class UDPConnectionCheckerImpl implements UDPConnectionChecker {

    private final ConnectionServices connectionServices;
    private final Provider<UDPService> udpService;
    private final Provider<UniqueHostPinger> uniqueHostPinger;
    private final PingRequestFactory pingRequestFactory;

    @Inject
    public UDPConnectionCheckerImpl(ConnectionServices connectionServices,
            Provider<UDPService> udpService,
            Provider<UniqueHostPinger> uniqueHostPinger,
            PingRequestFactory pingRequestFactory) {
        this.connectionServices = connectionServices;
        this.udpService = udpService;
        this.uniqueHostPinger = uniqueHostPinger;
        this.pingRequestFactory = pingRequestFactory;
    }

    public boolean udpIsDead() {
        PingRequest ping = pingRequestFactory.createUDPPing();
        Collection<IpPort> hosts = connectionServices.getPreferencedHosts(false, "en", 50);
        UDPChecker checker = new UDPChecker();

        // send some hosts to be ranked
        uniqueHostPinger.get().rank(hosts, checker, checker, ping);
        long now = System.currentTimeMillis();
        synchronized (checker) {
            try {
                // since there may be other udp packets backed up to be sent,
                // check every second if we have received something, and if so
                // cancel the hosts we sent.
                for (int i = 0; i < 5; i++) {
                    checker.wait(1000);
                    if (udpService.get().getLastReceivedTime() > now) {
                        checker.received = true;
                        return false;
                    }
                }
            } catch (InterruptedException ignored) {
            }
        }
        return !checker.received;
    }

    private static class UDPChecker implements MessageListener, Cancellable {
        volatile boolean received;

        public boolean isCancelled() {
            return received;
        }

        public void processMessage(Message m, ReplyHandler handler) {
            received = true;
            synchronized (this) {
                notify();
            }
        }

        public void registered(byte[] guid) {
        }

        public void unregistered(byte[] guid) {
        }
    }
}
