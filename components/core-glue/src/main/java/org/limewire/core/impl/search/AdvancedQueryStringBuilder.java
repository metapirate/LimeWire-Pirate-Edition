package org.limewire.core.impl.search;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.limewire.core.api.Category;
import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.impl.util.FilePropertyKeyPopulator;
import org.limewire.util.NameValue;

import com.google.inject.Inject;
import com.limegroup.gnutella.util.QueryUtils;
import com.limegroup.gnutella.xml.LimeXMLDocumentFactory;

/**
 * A utility class to manage generation of query strings (XML and plain text) for advanced searches. 
 */
public class AdvancedQueryStringBuilder {
    
    private final LimeXMLDocumentFactory xmlDocumentFactory;
    
    @Inject
    AdvancedQueryStringBuilder(LimeXMLDocumentFactory xmlDocumentFactory) {
         this.xmlDocumentFactory = xmlDocumentFactory;
    }
    
    /**
     * @return a simple composite query String that only includes the values for the properties
     *         being searched on without their association.
     */
    public String createSimpleCompositeQuery(Map<FilePropertyKey, String> advancedSearch) {
        StringBuilder sb = new StringBuilder();
        for(String value : advancedSearch.values()) {
            if (value != null && value.trim().length() > 0) {
                sb.append(value);
                sb.append(' ');
            }
        }        
        
        int len = sb.length();
        if (len > 0) {
            sb.deleteCharAt(len-1);
        }
        
        return QueryUtils.createQueryString(sb.toString());
    }
    
    /**
     * @return the full XML query String to be used in a search based on a category and a map of properties
     *         to search terms.
     */
    public String createXMLQueryString(Map<FilePropertyKey, String> advancedSearch, Category category) {
        List<NameValue<String>> nvs = new ArrayList<NameValue<String>>();
        for(Map.Entry<FilePropertyKey, String> entry : advancedSearch.entrySet()) {
            String xmlName = FilePropertyKeyPopulator.getLimeXmlName(category, entry.getKey());
            if(xmlName != null) {
                nvs.add(new NameValue<String>(xmlName, entry.getValue()));
            }
        }
        if(nvs.isEmpty()) {
            return "";
        } else {
            return xmlDocumentFactory.createLimeXMLDocument(nvs,
                    FilePropertyKeyPopulator.getLimeXmlSchemaUri(category)).getXMLString();
        }
    }
}
