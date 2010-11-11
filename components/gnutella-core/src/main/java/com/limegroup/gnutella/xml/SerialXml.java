package com.limegroup.gnutella.xml;

import java.io.Serializable;
import java.util.Map;

import com.limegroup.gnutella.xml.LimeXMLDocumentHelper;
import com.limegroup.gnutella.xml.LimeXMLSchema;
import com.limegroup.gnutella.xml.LimeXMLUtils;
import com.limegroup.gnutella.xml.XMLStringUtils;

public class SerialXml implements Serializable {

    private static final long serialVersionUID = 7396170507085078485L;
    
    private Map<String, String> fieldToValue;
    
    private String schemaUri;
    
    private int version;
    
    public String getXml(boolean includeVersion) {
        if(fieldToValue != null && schemaUri != null && !fieldToValue.isEmpty()) {                
            StringBuilder tag = new StringBuilder(100);
            tag.append(LimeXMLDocumentHelper.XML_HEADER)
               .append("<");            
            
            String type = LimeXMLSchema.getDisplayString(schemaUri);
            String root = type + "s";
            tag.append(root)
               .append(" ")
               .append(LimeXMLDocumentHelper.XML_NAMESPACE)
               .append(schemaUri)
               .append("\">");
            
            String canonicalKey = root + "__" + type + "__";
            tag.append("<")
               .append(type);

            for(Map.Entry<String, String> entry : fieldToValue.entrySet()) {
                String name = XMLStringUtils.getLastField(canonicalKey, entry.getKey());
                if(name == null)
                    continue;
                // Construct: ' attribute="value"'
                tag.append(" ")
                   .append(name)
                   .append("=\"")
                   .append(LimeXMLUtils.encodeXML(entry.getValue()))
                   .append("\"");
            }

            if(includeVersion) {
                tag.append(" internal_version=\"")
                   .append(version)
                   .append("\"");
            }
            
            tag.append("/></")
               .append(root)
               .append(">");
            
            return tag.toString();
        } else {
            return null;
        }
    }
}
