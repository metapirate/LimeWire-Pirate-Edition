package com.limegroup.gnutella.search;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import com.limegroup.gnutella.connection.RoutedConnection;
import com.limegroup.gnutella.messages.QueryRequest;

/**
 * This class handles query "probes."  Probe queries are the initial queries
 * that are sent out to determine the popularity of the file.  This allows 
 * queries down new connections to have more information for choosing the TTL.
 */
final class ProbeQuery {
    
    /**
     * Constant list of hosts to probe query at ttl=1.
     */
    private final List<RoutedConnection> TTL_1_PROBES;
    
    /**
     * Constant list of hosts to probe query at ttl=2.
     */
    private final List<RoutedConnection> TTL_2_PROBES;

    /**
     * Constant reference to the query handler instance.
     */
    private final QueryHandler QUERY_HANDLER;


    /**
     * Constructs a new <tt>ProbeQuery</tt> instance with the specified
     * list of connections to query and with the data enclosed in the
     * <tt>QueryHandler</tt>.
     *
     * @param connections the <tt>List</tt> of connections to query
     * @param qh the <tt>QueryHandler</tt> instance containing data
     *  for the probe
     */
    ProbeQuery(List<? extends RoutedConnection> connections, QueryHandler qh) {
        QUERY_HANDLER = qh;
        List<List<RoutedConnection>> lists = createProbeLists(connections, qh.getTemplateQueryRequest());

        TTL_1_PROBES = lists.get(0);
        TTL_2_PROBES = lists.get(1);        
    }
    

    /**
     * Obtains the time to wait for probe results to return.
     *
     * @return the time to wait for this probe to complete, in
     *  milliseconds
     */
    long getTimeToWait() {

        // determine the wait time.  we wait a little longer per
        // hop for probes to give them more time -- also weight
        // this depending on how many TTL=1 probes we're sending
        if(!TTL_2_PROBES.isEmpty()) 
            return (long)(QUERY_HANDLER.getTimeToWaitPerHop()*1.3);
        if(!TTL_1_PROBES.isEmpty()) 
            return (long)(QUERY_HANDLER.getTimeToWaitPerHop()*
                (TTL_1_PROBES.size()/2.0));
        return 0L;
    }
    
    /**
     * Sends the next probe query out on the network if there 
     * are more to send.
     *
     * @return the number of hosts theoretically hit by this new 
     *  probe
     */
    int sendProbe() {
        int hosts = 0;
        QueryRequest query = QUERY_HANDLER.createQuery((byte)1);
        for(RoutedConnection mc : TTL_1_PROBES)
            hosts += QUERY_HANDLER.sendQueryToHost(query, mc);
        
        query = QUERY_HANDLER.createQuery((byte)2);
        for(RoutedConnection mc : TTL_2_PROBES)
            hosts += QUERY_HANDLER.sendQueryToHost(query, mc);
        
        TTL_1_PROBES.clear();
        TTL_2_PROBES.clear();

        return hosts;
    }

    /**
     * Helper method that creates the list of nodes to query for the probe.
     * This list will vary in size depending on how popular the content appears
     * to be.
     */
    private static List<List<RoutedConnection>> createProbeLists(
            List<? extends RoutedConnection> connections, QueryRequest query) {
        
        LinkedList<RoutedConnection> missConnections = new LinkedList<RoutedConnection>();
        LinkedList<RoutedConnection> oldConnections  = new LinkedList<RoutedConnection>();
        LinkedList<RoutedConnection> hitConnections  = new LinkedList<RoutedConnection>();

        // iterate through our connections, adding them to the hit, miss, or
        // old connections list
        for(RoutedConnection mc : connections) {
            if(mc.getConnectionCapabilities().isUltrapeerQueryRoutingConnection()) {
                if(mc.shouldForwardQuery(query)) { 
                    hitConnections.add(mc);
                } else {
                    missConnections.add(mc);
                }
            } else {
                oldConnections.add(mc);
            }
        }

        // final list of connections to query
        List<List<RoutedConnection>> returnLists = new ArrayList<List<RoutedConnection>>(2);
        LinkedList<RoutedConnection> ttl1List = new LinkedList<RoutedConnection>();
        LinkedList<RoutedConnection> ttl2List = new LinkedList<RoutedConnection>();
        returnLists.add(ttl1List);
        returnLists.add(ttl2List);        

        // do we have adequate data to determine some measure of the file's 
        // popularity?
        boolean adequateData = 
            (missConnections.size()+hitConnections.size()) > 8;

        // if we don't have enough data from QRP tables, just send out a 
        // traditional probe also, if we don't have an adequate number of QRP 
        // tables to access the popularity of the file, just send out an 
        // old-style probe at TTL=2
        if(hitConnections.size() == 0 || !adequateData) {
            return createAggressiveProbe(oldConnections, missConnections, 
                                         hitConnections, returnLists);
        } 

        int numHitConnections = hitConnections.size();
        double popularity = 
            (numHitConnections/
                     ((double)missConnections.size()+numHitConnections));
        
        // if the file appears to be very popular, send it to only one host
        if(popularity == 1.0) {
            ttl1List.add(hitConnections.removeFirst());
            return returnLists;
        }

        if(numHitConnections > 3) {
            // TTL=1 queries are cheap -- send a lot of them if we can
            int numToTry = Math.min(9, numHitConnections);

            int startIndex = numHitConnections-numToTry;
            int endIndex   = numHitConnections;
            ttl1List.addAll(hitConnections.subList(startIndex, endIndex));
            return returnLists;
        }

        // otherwise, it's not very widely distributed content -- send
        // the query to all hit connections plus 3 TTL=2 connections
        ttl1List.addAll(hitConnections);        
        addToList(ttl2List, oldConnections, missConnections, 3);

        return returnLists;        
    }


    /**
     * Helper method that adds as many elements as possible up to the
     * desired number from two lists into a third list.  This method
     * takes as many elements as possible from <tt>list1</tt>, only
     * using elements from <tt>list2</tt> if the desired number of
     * elements to add cannot be fulfilled from <tt>list1</tt> alone.
     *
     * @param listToAddTo the list that elements should be added to
     * @param list1 the first list to add elements from, with priority 
     *  given to this list
     * @param list2 the second list to add elements from -- only used
     *  in the case where <tt>list1</tt> is smaller than <tt>numElements</tt>
     * @param numElements the desired number of elements to add to 
     *  <tt>listToAddTo</tt> -- note that this number will not be reached
     *  if the list1.size()+list2.size() < numElements
     */
    private static <T> void addToList(List<T> listToAddTo,
                                      List<? extends T> list1,
                                      List<? extends T> list2, 
                                      int numElements) {
        if(list1.size() >= numElements) {
            listToAddTo.addAll(list1.subList(0, numElements));
            return;
        } else {
            listToAddTo.addAll(list1);
        }

        numElements = numElements - list1.size();

        if(list2.size() >= numElements) {
            listToAddTo.addAll(list2.subList(0, numElements));
        } else {
            listToAddTo.addAll(list2);
        }
    }
       

    /**
     * Helper method that creates lists of TTL=1 and TTL=2 connections to query
     * for an aggressive probe.  This is desired, for example, when the desired
     * file appears to be rare or when there is not enough data to determine
     * the file's popularity.
     *
     * @param oldConnections the <tt>List</tt> of old-style connections
     * @param missConnections the <tt>List</tt> of new connections that did
     *  not have a hit for this query
     * @param hitConnections the <tt>List</tt> of connections with hits
     * @param returnLists the array of TTL=1 and TTL=2 connections to query
     */
    private static <T> List<List<T>>
        createAggressiveProbe(List<? extends T> oldConnections,
                              List<? extends T> missConnections,
                              List<? extends T> hitConnections,
                              List<List<T>> returnLists) {
        
        // add as many connections as possible from first the old connections
        // list, then the connections that did not have hits
        addToList(returnLists.get(1), oldConnections, missConnections, 3);

        // add any hits there are to the TTL=1 list
        int maxIndex = Math.min(4, hitConnections.size());
        returnLists.get(0).addAll(hitConnections.subList(0, maxIndex));

        return returnLists;        
    }
}













