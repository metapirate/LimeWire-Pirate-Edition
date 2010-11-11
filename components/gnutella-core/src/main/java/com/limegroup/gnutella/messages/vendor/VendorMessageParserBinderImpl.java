package com.limegroup.gnutella.messages.vendor;

import org.limewire.security.MACCalculatorRepositoryManager;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.Message.Network;
import com.limegroup.gnutella.messages.vendor.VendorMessageFactory.VendorMessageParser;

@Singleton
public class VendorMessageParserBinderImpl implements VendorMessageParserBinder {

    private final ReplyNumberVendorMessageFactory replyNumberVendorMessageFactory;
    private final HeadPongFactory headPongFactory;
    private final MACCalculatorRepositoryManager macManager;

    @Inject
    public VendorMessageParserBinderImpl(ReplyNumberVendorMessageFactory replyNumberVendorMessageFactory,
            HeadPongFactory headPongFactory,
            MACCalculatorRepositoryManager macManager) {
        this.replyNumberVendorMessageFactory = replyNumberVendorMessageFactory;
        this.headPongFactory = headPongFactory;
        this.macManager = macManager;
    }
    
    public void bind(VendorMessageFactory vendorMessageFactory) {
        vendorMessageFactory.setParser(VendorMessage.F_HOPS_FLOW, VendorMessage.F_BEAR_VENDOR_ID, new HopsFlowVendorMessageParser());
        vendorMessageFactory.setParser(VendorMessage.F_LIME_ACK, VendorMessage.F_LIME_VENDOR_ID, new LimeACKVendorMessageParser());
        vendorMessageFactory.setParser(VendorMessage.F_REPLY_NUMBER, VendorMessage.F_LIME_VENDOR_ID, new ReplyNumberVendorMessageParser());
        vendorMessageFactory.setParser(VendorMessage.F_TCP_CONNECT_BACK, VendorMessage.F_BEAR_VENDOR_ID, new TCPConnectBackVendorMessageParser());
        vendorMessageFactory.setParser(VendorMessage.F_MESSAGES_SUPPORTED, VendorMessage.F_NULL_VENDOR_ID, new MessagesSupportedVendorMessageParser());
        vendorMessageFactory.setParser(VendorMessage.F_UDP_CONNECT_BACK, VendorMessage.F_GTKG_VENDOR_ID, new UDPConnectBackVendorMessageParser());
        vendorMessageFactory.setParser(VendorMessage.F_PUSH_PROXY_REQ, VendorMessage.F_LIME_VENDOR_ID, new PushProxyRequestParser());
        vendorMessageFactory.setParser(VendorMessage.F_PUSH_PROXY_ACK, VendorMessage.F_LIME_VENDOR_ID, new PushProxyAcknowledgementParser());
        vendorMessageFactory.setParser(VendorMessage.F_LIME_ACK, VendorMessage.F_BEAR_VENDOR_ID, new QueryStatusRequestParser());
        vendorMessageFactory.setParser(VendorMessage.F_REPLY_NUMBER, VendorMessage.F_BEAR_VENDOR_ID, new QueryStatusResponseParser());
        vendorMessageFactory.setParser(VendorMessage.F_TCP_CONNECT_BACK, VendorMessage.F_LIME_VENDOR_ID, new TCPConnectBackRedirectParser());
        vendorMessageFactory.setParser(VendorMessage.F_UDP_CONNECT_BACK_REDIR, VendorMessage.F_LIME_VENDOR_ID, new UDPConnectBackRedirectParser());
        vendorMessageFactory.setParser(VendorMessage.F_CAPABILITIES, VendorMessage.F_NULL_VENDOR_ID, new CapabilitiesVMParser());
        vendorMessageFactory.setParser(VendorMessage.F_CRAWLER_PING, VendorMessage.F_LIME_VENDOR_ID, new UDPCrawlerPingParser());
        vendorMessageFactory.setParser(VendorMessage.F_UDP_HEAD_PING, VendorMessage.F_LIME_VENDOR_ID, new HeadPingParser());
        vendorMessageFactory.setParser(VendorMessage.F_UDP_HEAD_PONG, VendorMessage.F_LIME_VENDOR_ID, new HeadPongParser());
        vendorMessageFactory.setParser(VendorMessage.F_CONTENT_REQ, VendorMessage.F_LIME_VENDOR_ID, new ContentRequestParser());
        vendorMessageFactory.setParser(VendorMessage.F_CONTENT_RESP, VendorMessage.F_LIME_VENDOR_ID, new ContentResponseParser());
        vendorMessageFactory.setParser(VendorMessage.F_HEADER_UPDATE, VendorMessage.F_LIME_VENDOR_ID, new HeaderUpdateVendorMessageParser());
        vendorMessageFactory.setParser(VendorMessage.F_OOB_PROXYING_CONTROL, VendorMessage.F_LIME_VENDOR_ID, new OOBProxyControlVendorMessageParser());
        vendorMessageFactory.setParser(VendorMessage.F_ADVANCED_TOGGLE, VendorMessage.F_LIME_VENDOR_ID, new AdvancedStatsToggleVendorMessageParser());
        vendorMessageFactory.setParser(VendorMessage.F_DHT_CONTACTS, VendorMessage.F_LIME_VENDOR_ID, new DHTContactsMessageParser(macManager));
    }

    
    // HOPS FLOW MESSAGE
    private static class HopsFlowVendorMessageParser implements VendorMessageParser {
        public VendorMessage parse(byte[] guid, byte ttl, byte hops, int version, 
                byte[] restOf, Network network) throws BadPacketException {
            return new HopsFlowVendorMessage(guid, ttl, hops, version, restOf, network);
        }
    }
    
    // LIME ACK MESSAGE
    private static class LimeACKVendorMessageParser implements VendorMessageParser {
        public VendorMessage parse(byte[] guid, byte ttl, byte hops, int version, 
                byte[] restOf, Network network) throws BadPacketException {
            return new LimeACKVendorMessage(guid, ttl, hops, version, restOf, network);
        }
    }
    
    // REPLY NUMBER MESSAGE
    private class ReplyNumberVendorMessageParser implements VendorMessageParser {
        public VendorMessage parse(byte[] guid, byte ttl, byte hops, int version, 
                byte[] restOf, Network network) throws BadPacketException {
            return replyNumberVendorMessageFactory.createFromNetwork(guid, ttl,
                    hops, version, restOf, network);
        }
    }

    // TCP CONNECT BACK
    private static class TCPConnectBackVendorMessageParser implements VendorMessageParser {
        public VendorMessage parse(byte[] guid, byte ttl, byte hops, int version, 
                byte[] restOf, Network network) throws BadPacketException {
            return new TCPConnectBackVendorMessage(guid, ttl, hops, version, restOf, network);
        }
    }
    
    // Messages Supported Message
    private static class MessagesSupportedVendorMessageParser implements VendorMessageParser {
        public VendorMessage parse(byte[] guid, byte ttl, byte hops, int version, 
                byte[] restOf, Network network) throws BadPacketException {
            return new MessagesSupportedVendorMessage(guid, ttl, hops, version, restOf, network);
        }
    }

    // UDP CONNECT BACK
    private static class UDPConnectBackVendorMessageParser implements VendorMessageParser {
        public VendorMessage parse(byte[] guid, byte ttl, byte hops, int version, 
                byte[] restOf, Network network) throws BadPacketException {
            return new UDPConnectBackVendorMessage(guid, ttl, hops, version, restOf, network);
        }
    }
    
    // Push Proxy Request
    private static class PushProxyRequestParser implements VendorMessageParser {
        public VendorMessage parse(byte[] guid, byte ttl, byte hops, int version, 
                byte[] restOf, Network network) throws BadPacketException {
            return new PushProxyRequest(guid, ttl, hops, version, restOf, network);
        }
    }
    
    // Push Proxy Acknowledgement
    private static class PushProxyAcknowledgementParser implements VendorMessageParser {
        public VendorMessage parse(byte[] guid, byte ttl, byte hops, int version, 
                byte[] restOf, Network network) throws BadPacketException {
            return new PushProxyAcknowledgement(guid, ttl, hops, version, restOf, network);
        }
    }
    
    // Query Status Request
    private static class QueryStatusRequestParser implements VendorMessageParser {
        public VendorMessage parse(byte[] guid, byte ttl, byte hops, int version, 
                byte[] restOf, Network network) throws BadPacketException {
            return new QueryStatusRequest(guid, ttl, hops, version, restOf, network);
        }
    }
    
    // Query Status Response
    private static class QueryStatusResponseParser implements VendorMessageParser {
        public VendorMessage parse(byte[] guid, byte ttl, byte hops, int version, 
                byte[] restOf, Network network) throws BadPacketException {
            return new QueryStatusResponse(guid, ttl, hops, version, restOf, network);
        }
    }
    
    private static class TCPConnectBackRedirectParser implements VendorMessageParser {
        public VendorMessage parse(byte[] guid, byte ttl, byte hops, int version, 
                byte[] restOf, Network network) throws BadPacketException {
            return new TCPConnectBackRedirect(guid, ttl, hops, version, restOf, network);
        }
    }
    
    private static class UDPConnectBackRedirectParser implements VendorMessageParser {
        public VendorMessage parse(byte[] guid, byte ttl, byte hops, int version, 
                byte[] restOf, Network network) throws BadPacketException {
            return new UDPConnectBackRedirect(guid, ttl, hops, version, restOf, network);
        }
    }
    
    private static class CapabilitiesVMParser implements VendorMessageParser {
        public VendorMessage parse(byte[] guid, byte ttl, byte hops, int version, 
                byte[] restOf, Network network) throws BadPacketException {
            return new CapabilitiesVMImpl(guid, ttl, hops, version, restOf, network);
        }
    }
    
    private static class UDPCrawlerPingParser implements VendorMessageParser {
        public VendorMessage parse(byte[] guid, byte ttl, byte hops, int version, 
                byte[] restOf, Network network) throws BadPacketException {
            return new UDPCrawlerPing(guid, ttl, hops, version, restOf, network);
        }
    }
    
    private static class HeadPingParser implements VendorMessageParser {
        public VendorMessage parse(byte[] guid, byte ttl, byte hops, int version, 
                byte[] restOf, Network network) throws BadPacketException {
            return new HeadPing(guid, ttl, hops, version, restOf, network);
        }
    }
    
    private class HeadPongParser implements VendorMessageParser {
        public VendorMessage parse(byte[] guid, byte ttl, byte hops, int version, 
                byte[] restOf, Network network) throws BadPacketException {
            return headPongFactory.createFromNetwork(guid,
                    ttl, hops, version, restOf, network);
        }
    }
    
    private static class ContentRequestParser implements VendorMessageParser {
        public VendorMessage parse(byte[] guid, byte ttl, byte hops, int version, 
                byte[] restOf, Network network) throws BadPacketException {
            return new ContentRequest(guid, ttl, hops, version, restOf, network);
        }
    }
    
    private static class ContentResponseParser implements VendorMessageParser {
        public VendorMessage parse(byte[] guid, byte ttl, byte hops, int version, 
                byte[] restOf, Network network) throws BadPacketException {
            return new ContentResponse(guid, ttl, hops, version, restOf, network);
        }
    }
    
    private static class HeaderUpdateVendorMessageParser implements VendorMessageParser {
        public VendorMessage parse(byte[] guid, byte ttl, byte hops, int version, 
                byte[] restOf, Network network) throws BadPacketException {
            return new HeaderUpdateVendorMessage(guid, ttl, hops, version, restOf, network);
        }
    }
    
    private static class OOBProxyControlVendorMessageParser implements VendorMessageParser {
        public VendorMessage parse(byte[] guid, byte ttl, byte hops, int version,
                byte[] restOf, Network network) throws BadPacketException {
            return new OOBProxyControlVendorMessage(guid, ttl, hops, version, restOf, network);
        }
    }
    
    private static class AdvancedStatsToggleVendorMessageParser implements VendorMessageParser {
        public VendorMessage parse(byte[] guid, byte ttl, byte hops, int version,
                byte[] restOf, Network network) throws BadPacketException {
            return new AdvancedStatsToggle(guid, ttl, hops, version, restOf, network);
        }
    }
    
    private static class DHTContactsMessageParser implements VendorMessageParser {
        private final MACCalculatorRepositoryManager macManager;
        private DHTContactsMessageParser(MACCalculatorRepositoryManager macManager) {
            this.macManager = macManager;
        }
        public VendorMessage parse(byte[] guid, byte ttl, byte hops, int version, 
                byte[] restOf, Network network) throws BadPacketException {
            return new DHTContactsMessage(guid, ttl, hops, version, restOf, network, macManager);
        }
    }
      
}
