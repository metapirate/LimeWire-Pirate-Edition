package org.limewire.core.api.support;

import java.util.Map;

/**
 * Defines the information on an individual client machine for an individual
 * bug report.
 */
public interface LocalClientInfo {

    /**
     * Sets the user-entered comments.
     * @param comments entered by the user
     */    
    public void addUserComments(String comments);

    /**
     * Returns this bug as a bug report.
     */
    public String toBugReport();
    
    /**
     * Returns the parsed version of the stack trace, without the message
     * between the exception and the stack trace.
     */
    public String getParsedBug();
    
    /** 
     * Returns an array of map entries in this info.
     */
    public Map.Entry[] getPostRequestParams();

    /**
     * Returns compact printout of the list of parameters.
     */
    public String getShortParamList();

}
