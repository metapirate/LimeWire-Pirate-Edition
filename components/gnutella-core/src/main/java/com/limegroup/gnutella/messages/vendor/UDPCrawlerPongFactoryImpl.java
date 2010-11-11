package com.limegroup.gnutella.messages.vendor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.GZIPOutputStream;

import org.limewire.core.settings.ApplicationSettings;
import org.limewire.io.NetworkUtils;
import org.limewire.service.ErrorService;
import org.limewire.util.ByteUtils;
import org.limewire.util.StringUtils;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.limegroup.gnutella.ConnectionManager;
import com.limegroup.gnutella.Constants;
import com.limegroup.gnutella.connection.Connection;
import com.limegroup.gnutella.connection.RoutedConnection;
import com.limegroup.gnutella.dht.DHTManager;
import com.limegroup.gnutella.util.LimeWireUtils;

@Singleton
public class UDPCrawlerPongFactoryImpl implements UDPCrawlerPongFactory {

    private final Provider<DHTManager> dhtManager;
    private final Provider<ConnectionManager> connectionManager;

    @Inject
    public UDPCrawlerPongFactoryImpl(Provider<DHTManager> dhtManager, Provider<ConnectionManager> connectionManager) {
        this.dhtManager = dhtManager;
        this.connectionManager = connectionManager;
    }
    
    public UDPCrawlerPong createUDPCrawlerPong(UDPCrawlerPing request) {
        return new UDPCrawlerPong(request, derivePayload(request));
    }

    private byte [] derivePayload(UDPCrawlerPing request) {
        
        //local copy of the requested format
        byte format = (byte)(request.getFormat() & UDPCrawlerPing.FEATURE_MASK);
        
        //get a list of all ultrapeers and leafs we have connections to
        List<RoutedConnection> endpointsUP = new LinkedList<RoutedConnection>();
        List<RoutedConnection> endpointsLeaf = new LinkedList<RoutedConnection>();
        
        //add only good ultrapeers or just those who support UDP pinging
        //(they support UDP ponging, obviously)
        boolean newOnly = request.hasNewOnly();
        
        for(RoutedConnection c : connectionManager.get().getInitializedConnections()) {
            if (newOnly) {  
                if (c.getConnectionCapabilities().remoteHostSupportsUDPCrawling() >= 1)
                    endpointsUP.add(c);
            } else if (c.isGoodUltrapeer())  {
                endpointsUP.add(c);
            }
        }
        
        //add all leaves.. or not?
        for(RoutedConnection c : connectionManager.get().getInitializedClientConnections()) {
            //if (c.isGoodLeaf()) //uncomment if you decide you want only good leafs 
                endpointsLeaf.add(c);
        }
        
        //the ping does not carry info about which locale to preference to, so we'll just
        //preference any locale.  In reality we will probably have only connections only to 
        //this host's pref'd locale so they will end up in the pong.
        
        if (!request.hasLocaleInfo()) {
        //do a randomized trim.
            if (request.getNumberUP() != UDPCrawlerPing.ALL && 
                request.getNumberUP() < endpointsUP.size()) {
                //randomized trim
                int index = (int) Math.floor(Math.random()*
                    (endpointsUP.size()-request.getNumberUP()));
                endpointsUP = endpointsUP.subList(index,index+request.getNumberUP());
            }
            if (request.getNumberLeaves() != UDPCrawlerPing.ALL && 
                    request.getNumberLeaves() < endpointsLeaf.size()) {
                //randomized trim
                int index = (int) Math.floor(Math.random()*
                    (endpointsLeaf.size()-request.getNumberLeaves()));
                endpointsLeaf = endpointsLeaf.subList(index,index+request.getNumberLeaves());
            }
        } else {
            String myLocale = ApplicationSettings.LANGUAGE.get();
            
            //move the connections with the locale pref to the head of the lists
            //we prioritize these disregarding the other criteria (such as isGoodUltrapeer, etc.)
            List<RoutedConnection> prefedcons =
                connectionManager.get().getInitializedConnectionsMatchLocale(myLocale);
            for(RoutedConnection c : prefedcons) {
                endpointsUP.remove(c);
                endpointsUP.add(0, c);
            }
            
            prefedcons =
                connectionManager.get().getInitializedClientConnectionsMatchLocale(myLocale);
            for(RoutedConnection c : prefedcons) {
                endpointsLeaf.remove(c);
                endpointsLeaf.add(0, c);
            }
            
            //then trim down to the requested number
            if (request.getNumberUP() != UDPCrawlerPing.ALL && 
                    request.getNumberUP() < endpointsUP.size())
                endpointsUP = endpointsUP.subList(0,request.getNumberUP());
            if (request.getNumberLeaves() != UDPCrawlerPing.ALL && 
                    request.getNumberLeaves() < endpointsLeaf.size())
                endpointsLeaf = endpointsLeaf.subList(0,request.getNumberLeaves());
        }
        
        //serialize the Endpoints to a byte []
        int bytesPerResult = 6;
        if (request.hasConnectionTime())
            bytesPerResult+=2;
        if (request.hasLocaleInfo())
            bytesPerResult+=2;
        if (request.hasReplies())
            bytesPerResult += 4;

        int index = 3;
        if(request.hasNodeUptime()) {
            index += 4;
        }
        
        if(request.hasDHTStatus()) {
            index++;
        }
        
        byte [] result = new byte[(endpointsUP.size()+endpointsLeaf.size())*
                                  bytesPerResult+index];
        
        //write out metainfo
        result[0] = (byte)endpointsUP.size();
        result[1] = (byte)endpointsLeaf.size();
        result[2] = format;
        
        if(request.hasNodeUptime()) {
            long currentAverage = connectionManager.get().getCurrentAverageUptime()/1000L;//in sec
            if(currentAverage > Integer.MAX_VALUE)
                currentAverage = Integer.MAX_VALUE;
            ByteUtils.int2leb((int)currentAverage, result, 3);
        }
        
        if(request.hasDHTStatus()) {
            byte dhtStatus = 0x00;
            DHTManager manager = dhtManager.get();
            if(manager.isRunning()) {
                switch (manager.getDHTMode()) {
                    case ACTIVE:
                        dhtStatus |= UDPCrawlerPong.DHT_ACTIVE_MASK;
                        break;
                    case PASSIVE:
                        dhtStatus |= UDPCrawlerPong.DHT_PASSIVE_MASK;
                        break;
                    case PASSIVE_LEAF:
                        dhtStatus |= UDPCrawlerPong.DHT_PASSIVE_LEAF_MASK;
                        break;        
                }
                
                if(!manager.isMemberOfDHT()) {
                    dhtStatus |= UDPCrawlerPong.DHT_WAITING_MASK;
                }
            }
            result[index-1] = dhtStatus;
        }
        
        //cat the two lists
        endpointsUP.addAll(endpointsLeaf);
        
        //cache the call to currentTimeMillis() cause its not always cheap
        long now = System.currentTimeMillis();
        
        for(RoutedConnection c : endpointsUP) {
            //pack each entry into a 6 byte array and add it to the result.
            System.arraycopy(
                    NetworkUtils.getBytes(c.getInetAddress(), c.getPort(), java.nio.ByteOrder.LITTLE_ENDIAN),
                    0,
                    result,
                    index,
                    6);
            index+=6;
            //add connection time if asked for
            //represent it as a short with the # of minutes
            if (request.hasConnectionTime()) {
                long uptime = now - c.getConnectionTime();
                short packed = (short) ( uptime / Constants.MINUTE);
                ByteUtils.short2leb(packed, result, index);
                index+=2;
            }
                
            if (request.hasLocaleInfo()){
                //I'm assuming the language code is always 2 bytes, no?
                System.arraycopy(StringUtils.toAsciiBytes(c.getLocalePref()),0,result,index,2);
                index+=2;
            }
            
            if (request.hasReplies()) {
                // pack the # of replies as reported up to Integer.MAX_VALUE
                ByteUtils.int2leb(ByteUtils.long2int(c.getConnectionMessageStatistics().getNumQueryReplies()),
                        result,index);
                index += 4;
            }           
        }
        
        //if the ping asked for user agents, copy the reported strings verbatim
        //in the same order as the results.
        if (request.hasUserAgent()) {
            StringBuilder agents = new StringBuilder();
            for(Connection c : endpointsUP) {
                String agent = c.getConnectionCapabilities().getUserAgent();
                agent = StringUtils.replace(agent,UDPCrawlerPong.AGENT_SEP,"\\"+UDPCrawlerPong.AGENT_SEP);
                agents.append(agent).append(UDPCrawlerPong.AGENT_SEP);
            }
            
            // append myself at the end
            agents.append(LimeWireUtils.getHttpServer());
            
            //zip the string
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try {
                GZIPOutputStream zout = new GZIPOutputStream(baos);
                byte [] length = new byte[2];
                ByteUtils.short2leb((short)agents.length(),length,0);
                zout.write(length);
                zout.write(StringUtils.toAsciiBytes(agents.toString()));
                zout.flush();
                zout.close();
            }catch(IOException huh) {
                ErrorService.error(huh);
            }
            
            //put in the return payload.
            byte [] agentsB = baos.toByteArray();
            byte [] resTemp = result;
            result = new byte[result.length+agentsB.length+2];
            
            System.arraycopy(resTemp,0,result,0,resTemp.length);
            ByteUtils.short2leb((short)agentsB.length,result,resTemp.length);
            System.arraycopy(agentsB,0,result,resTemp.length+2,agentsB.length);
        }
        return result;
    }

}
