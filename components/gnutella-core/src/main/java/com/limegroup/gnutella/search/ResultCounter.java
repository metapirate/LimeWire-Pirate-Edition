package com.limegroup.gnutella.search;

/**
 * Interface for a class that counts the number of results for a given query. 
 * This can easily be used to add a result counting mixin to any class choosing
 * to add this functionality.
 */
public interface ResultCounter {
	
	/**
	 * Accessor for the number of results for a query.
	 *
	 * @return the number of results returned
	 */
	int getNumResults();
}
