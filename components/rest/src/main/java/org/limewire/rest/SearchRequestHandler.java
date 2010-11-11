package org.limewire.rest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.protocol.HttpContext;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.limewire.core.api.search.GroupedSearchResult;
import org.limewire.core.api.search.Search;
import org.limewire.core.api.search.SearchDetails;
import org.limewire.core.api.search.SearchFactory;
import org.limewire.core.api.search.SearchManager;
import org.limewire.core.api.search.SearchResultList;
import org.limewire.io.GUID;

import com.google.inject.Inject;

/**
 * Request handler for Search services.
 */
class SearchRequestHandler extends AbstractRestRequestHandler {

    private static final String ALL = "";
    private static final String FILES = "/files";
    private static final String START = "";
    private static final int MAX_LIMIT = 1000;
    
    private final SearchManager searchManager;
    private final SearchFactory searchFactory;
    
    @Inject
    public SearchRequestHandler(SearchManager searchManager, SearchFactory searchFactory) {
        this.searchManager = searchManager;
        this.searchFactory = searchFactory;
    }
    
    @Override
    public void handle(HttpRequest request, HttpResponse response, HttpContext context)
            throws HttpException, IOException {
        
        // Get request method and uri target.
        String method = request.getRequestLine().getMethod();
        String uriTarget = RestUtils.getUriTarget(request, RestPrefix.SEARCH.pattern());
        Map<String, String> queryParams = RestUtils.getQueryParams(request);
        
        if (RestUtils.GET.equals(method) && ALL.equals(uriTarget)) {
            // Get metadata for all searches.
            doGetAll(uriTarget, queryParams, response);

        } else if (RestUtils.GET.equals(method) && (uriTarget.length() > 0)) {
            // Get data for single search.
            doGetSearch(uriTarget, queryParams, response);
            
        } else if (RestUtils.POST.equals(method) && START.equals(uriTarget)) {
            // Start search.
            doStart(uriTarget, queryParams, response);

        } else if (RestUtils.DELETE.equals(method) && (uriTarget.length() > 0)) {
            // Delete search.
            doDelete(uriTarget, response);
            
        } else {
            response.setStatusCode(HttpStatus.SC_NOT_IMPLEMENTED);
        }
    }

    /**
     * Processes request to retrieve metadata for all searches.
     */
    private void doGetAll(String uriTarget, Map<String, String> queryParams,
            HttpResponse response) throws IOException {

        // Get active search lists.
        List<SearchResultList> searchLists = searchManager.getActiveSearchLists();

        try {
            // Create JSON result.
            JSONArray jsonArr = new JSONArray();
            for (SearchResultList searchList : searchLists) {
                jsonArr.put(RestUtils.createSearchJson(searchList));
            }

            // Set response entity and status.
            HttpEntity entity = RestUtils.createStringEntity(jsonArr.toString());
            response.setEntity(entity);
            response.setStatusCode(HttpStatus.SC_OK);

        } catch (JSONException ex) {
            throw new IOException(ex);
        }
    }
    
    /**
     * Processes request to retrieve data for a single search.
     */
    private void doGetSearch(String uriTarget, Map<String, String> queryParams,
            HttpResponse response) throws IOException {

        // Use GUID to get search result list.
        SearchResultList searchList;
        try {
            String guidStr = parseGuid(uriTarget);
            searchList = searchManager.getSearchResultList(new GUID(guidStr));
        } catch (Exception ex) {
            searchList = null;
        }
        
        // Return empty object if null.
        if (searchList == null) {
            response.setEntity(RestUtils.createStringEntity("{}"));
            response.setStatusCode(HttpStatus.SC_OK);
            return;
        }

        try {
            if (uriTarget.indexOf(FILES) < 0) {
                // Return search metadata.
                JSONObject jsonObj = RestUtils.createSearchJson(searchList);
                HttpEntity entity = RestUtils.createStringEntity(jsonObj.toString());
                response.setEntity(entity);
                response.setStatusCode(HttpStatus.SC_OK);

            } else {
                // Get query parameters.
                String offsetStr = queryParams.get("offset");
                String limitStr = queryParams.get("limit");
                int offset = (offsetStr != null) ? Integer.parseInt(offsetStr) : 0;
                int limit = (limitStr != null) ? Math.min(Integer.parseInt(limitStr), MAX_LIMIT) : MAX_LIMIT;

                // Create search result array.
                List<GroupedSearchResult> resultList = new ArrayList<GroupedSearchResult>(searchList.getGroupedResults());
                JSONArray jsonArr = new JSONArray();
                for (int i = offset, max = Math.min(offset + limit, resultList.size()); i < max; i++) {
                    GroupedSearchResult result = resultList.get(i);
                    jsonArr.put(RestUtils.createSearchResultJson(result));
                }

                // Set response entity and status.
                HttpEntity entity = RestUtils.createStringEntity(jsonArr.toString());
                response.setEntity(entity);
                response.setStatusCode(HttpStatus.SC_OK);
            }

        } catch (JSONException ex) {
            throw new IOException(ex);
        }
    }
    
    /**
     * Starts a new search using the specified uri target and query parameters.
     */
    private void doStart(String uriTarget, Map<String, String> queryParams, 
            HttpResponse response) throws IOException {

        // Get parameters to start search.
        String searchQuery = queryParams.get("q");

        // Create search.
        SearchDetails searchDetails = RestSearchDetails.createKeywordSearch(searchQuery);
        Search search = searchFactory.createSearch(searchDetails);

        // Add search to search manager.  The search is monitored so it will
        // be cancelled if we stop polling for its results.
        SearchResultList searchList = searchManager.addMonitoredSearch(search, searchDetails);
        
        // Start search.
        search.start();

        try {
            // Return search metadata.
            JSONObject jsonObj = RestUtils.createSearchJson(searchList);
            HttpEntity entity = RestUtils.createStringEntity(jsonObj.toString());
            response.setEntity(entity);
            response.setStatusCode(HttpStatus.SC_OK);

        } catch (JSONException ex) {
            throw new IOException(ex);
        }
    }
    
    /**
     * Deletes a search using the specified uri target.
     */
    private void doDelete(String uriTarget, HttpResponse response) throws IOException {
        // Use GUID to get search result list.
        SearchResultList searchList;
        try {
            String guidStr = parseGuid(uriTarget);
            searchList = searchManager.getSearchResultList(new GUID(guidStr));
        } catch (Exception ex) {
            searchList = null;
        }
        
        if (searchList != null) {
            Search search = searchList.getSearch();
            
            // Stop search.
            search.stop();
            
            // Remove search from core management.
            searchManager.removeSearch(search);
            
            // Set OK status.
            response.setStatusCode(HttpStatus.SC_OK);
            
        } else {
            response.setStatusCode(HttpStatus.SC_NOT_FOUND);
        }
    }
    
    /**
     * Returns the GUID string from the specified URI target.  The GUID string
     * should be between the first and second slash characters.
     */
    private String parseGuid(String uriTarget) {
        int pos1 = uriTarget.indexOf("/");
        String guid = (pos1 < 0) ? uriTarget : uriTarget.substring(pos1 + 1);
        
        int pos2 = guid.indexOf("/");
        return (pos2 < 0) ? guid : guid.substring(0, pos2);
    }
}
