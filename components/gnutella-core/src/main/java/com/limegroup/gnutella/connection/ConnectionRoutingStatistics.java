package com.limegroup.gnutella.connection;

import com.limegroup.gnutella.routing.QueryRouteTable;

/**
 * Defines the interface to retrieve statistics about
 * {@link QueryRouteTable QueryRouteTables} that are sent and retrieved over a
 * {@link RoutedConnection}.
 */
public interface ConnectionRoutingStatistics {

    /**
     * Accessor for the <tt>QueryRouteTable</tt> received along this
     * connection. Can be <tt>null</tt> if no query routing table has been
     * received yet.
     * 
     * @return the last <tt>QueryRouteTable</tt> received along this
     *         connection
     */
    public QueryRouteTable getQueryRouteTableReceived();

    /**
     * Accessor for the last QueryRouteTable's percent full.
     */
    public double getQueryRouteTablePercentFull();

    /**
     * Accessor for the last QueryRouteTable's size.
     */
    public int getQueryRouteTableSize();

    /**
     * Accessor for the last QueryRouteTable's Empty Units.
     */
    public int getQueryRouteTableEmptyUnits();

    /**
     * Accessor for the last QueryRouteTable's Units In Use.
     */
    public int getQueryRouteTableUnitsInUse();

    /**
     * Returns the system time that we should next forward a query route table
     * along this connection. Only valid if isClientSupernodeConnection() is
     * true.
     */
    public long getNextQRPForwardTime();

    /**
     * Increments the next time we should forward query route tables for this
     * connection. This depends on whether or not this is a connection to a leaf
     * or to an Ultrapeer.
     * 
     * @param curTime the current time in milliseconds, used to calculate the
     *        next update time
     */
    public void incrementNextQRPForwardTime(long curTime);

    /**
     * Accessor for the query route table associated with this. This is
     * guaranteed to be non-null, but it may not yet contain any data.
     * 
     * @return the <tt>QueryRouteTable</tt> instance containing query route
     *         table data sent along this connection, or <tt>null</tt> if no
     *         data has yet been sent
     */
    public QueryRouteTable getQueryRouteTableSent();

    /**
     * Mutator for the last query route table that was sent along this
     * connection.
     * 
     * @param qrt the last query route table that was sent along this connection
     */
    public void setQueryRouteTableSent(QueryRouteTable qrt);

}
