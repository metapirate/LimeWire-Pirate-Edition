package com.limegroup.gnutella.filters;

import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.QueryRequest;

/** 
 * Stops queries that are bound to match too many files.  
 * <p>
 * Currently, queries that are blocked include "a.asf, d.mp3, etc." or
 * single-character searches.  Additionally, queries such as "*.mp3" or 
 * "mpg" or "*.*" are to be blocked, are at least set to travel less than
 * other queries.
 */
public class GreedyQueryFilter implements SpamFilter {
    private static final int GREEDY_QUERY_MAX = 3;

    /**
     * has a side effect of changing the TTL in some conditions!!
     */
    public boolean allow(Message m) {
        if (! (m instanceof QueryRequest))
            return true;

		QueryRequest qr = (QueryRequest)m;
        String query = qr.getQuery();
        int n=query.length();
        if (n==1 && !qr.hasQueryUrns())
            return false;
        if ((n==5 || n==6) 
               && query.charAt(1)=='.' 
               && Character.isLetter(query.charAt(0)) )
            return false; 
        if (this.isVeryGeneralSearch(query) ||
            this.isObfuscatedGeneralSearch(query)) {

            int hops = m.getHops();
            int ttl = m.getTTL();
            
            if (hops >= GreedyQueryFilter.GREEDY_QUERY_MAX)
                return false;
            if ( (hops + ttl) > GreedyQueryFilter.GREEDY_QUERY_MAX) 
                m.setTTL((byte)(GreedyQueryFilter.GREEDY_QUERY_MAX - hops));
        }

        return true;
    }

    /**
     * Search through a query string and see if matches a very general search
     * such as "*.*", "*.mp3", or "*.mpg" and check for uppercase also
     */
    private boolean isVeryGeneralSearch(String queryString) {
        int length = queryString.length();

        if ((length == 3) && 
            ( (queryString.charAt(1) == '.') ||
              (queryString.equalsIgnoreCase("mp3")) ||
              (queryString.equalsIgnoreCase("mpg")) ) ) 
            return true;

        if (length == 5) { //could by of type "*.mp3" or "*.mpg"
            String fileFormat = queryString.substring(2,5);
            if ((queryString.charAt(1) == '.') &&
                ( (fileFormat.equalsIgnoreCase("mp3")) ||
                  (fileFormat.equalsIgnoreCase("mpg")) ) )
                return true;
        }
        
        return false; //not a general search
    }


    /** To combat system-wide gnutella overflow, this method checks for
     *  permutations of "*.*".
     */
    private boolean isObfuscatedGeneralSearch(final String queryString) {
        final String unacceptable = "*.- ";
        for (int i = 0; i < queryString.length(); i++) 
            // if a character is not one of the unacceptable strings, the query
            // is ok.
            if (unacceptable.indexOf(queryString.charAt(i)) == -1)
                return false;

        return true;
    }
}
