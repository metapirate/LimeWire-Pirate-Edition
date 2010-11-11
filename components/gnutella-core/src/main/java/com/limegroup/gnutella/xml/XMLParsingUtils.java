package com.limegroup.gnutella.xml;


import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.service.ErrorService;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;


/**
 * Provides just enough functionality for our simple schemas,
 * based on SAX.
 * @author  tjones
 */
public class XMLParsingUtils {
    
    private static final Log LOG = LogFactory.getLog(XMLParsingUtils.class);
    

    static final private String XML_START = "<?xml";
    
    /**
     * A ThreadLocal to contain the instance of the Lime parser.
     */
    private static ThreadLocal<LimeParser> _parserContainer = new ThreadLocal<LimeParser>() {
        @Override
        protected LimeParser initialValue() {
            return new LimeParser();
        }
    };
    
    /**
     * Parses our simplified XML.
     */
    public static ParseResult parse(String xml, int responseCount) 
      throws IOException, SAXException {
        xml = LimeXMLUtils.scanForBadCharacters(xml);
        return parse(new InputSource(new StringReader(xml)),responseCount);
    }
    
    public static ParseResult parse(InputSource inputSource) 
      throws IOException,SAXException {
        return parse(inputSource, 8);
    }
    
    /**
     * Parses our simplified XML.
     */
    public static ParseResult parse(InputSource inputSource, int responseCount) 
      throws IOException, SAXException {
        ParseResult result = new ParseResult(responseCount);
        LimeParser parser = _parserContainer.get();
        parser.parse(result,inputSource);
        return result;
    }

    /**
     * Splits an aggregated XML string into individual XML strings.
     * @return List of Strings
     */    
    public static List<String> split(String aggregatedXmlDocuments) {
        List<String> results = new ArrayList<String>();
        
        int begin=aggregatedXmlDocuments.indexOf(XML_START);
        int end=aggregatedXmlDocuments.indexOf(XML_START,begin+1);
        
        while(end!=-1) {
            results.add(aggregatedXmlDocuments.substring(begin,end));
            begin = end;
            end = aggregatedXmlDocuments.indexOf(XML_START,begin+1);
        }
        
        if(begin!=-1) 
            results.add(aggregatedXmlDocuments.substring(begin));
        
        return results;
    }
    
    /**
     * A list of maps, also containing the Schema URI, the type and
     * the canonical key prefix.
     */
    public static class ParseResult extends ArrayList<Map<String, String>> {
        
        public ParseResult(int size) {
            super(size*2/3);
        }
        
        public String schemaURI;            //like http://www.limewire.com/schemas/audio.xsd
        public String type;                 //e.g. audio, video, etc.
        public String canonicalKeyPrefix;   //like audios__audio__
    }
    
    /**
     * This class does the actual parsing of the document.  It is a reusable
     * DocumentHandler.
     */
    private static class LimeParser extends DefaultHandler {
        private final XMLReader _reader;
        private ParseResult _result;
        
        boolean _isFirstElement=true;
        
        LimeParser() {
            XMLReader reader; 
            try {
                reader = SAXParserFactory.newInstance().newSAXParser().getXMLReader();
                reader.setContentHandler(this);
                reader.setErrorHandler(this);
                reader.setFeature("http://xml.org/sax/features/namespaces", false);
            }catch(SAXException bad) {
                ErrorService.error(bad);
                reader = null; 
            } catch (ParserConfigurationException bad) {
            	ErrorService.error(bad);
            	reader = null;
            }
            _reader=reader;
        }
        
        /**
         * Parses the given document input.  Any state from previous parsing is
         * discarded.
         */
        public void parse(ParseResult dest, InputSource input) 
        	throws SAXException, IOException {
            
            //if parser creation failed, do not try to parse.
            if (_reader==null)
                return;
            
            _isFirstElement=true;
            _result = dest;

            _reader.parse(input);
        }
        
        @Override
        public void startElement(String namespaceUri, String localName, 
                                 String qualifiedName, Attributes attributes) {
            
            int attributesLength;
            String qName;
            
            if(_isFirstElement) {
                _isFirstElement=false; 
                _result.canonicalKeyPrefix = qualifiedName;
                return;
            }
            
            if(_result.type==null) {
                _result.type = qualifiedName;
                _result.schemaURI = "http://www.limewire.com/schemas/"+_result.type+".xsd";
                _result.canonicalKeyPrefix += "__"+qualifiedName+"__";
            } 
            
            attributesLength = attributes.getLength();            
            
            //convert prefix to lower case to prevent capitalized tags from appearing
            _result.canonicalKeyPrefix = _result.canonicalKeyPrefix.toLowerCase(Locale.US);
            
            if(attributesLength > 0) {
                Map<String, String> attributeMap = new HashMap<String, String>(attributesLength);
                for(int i = 0; i < attributesLength; i++) {
                    //everything goes to lowercase
                    qName = attributes.getQName(i).toLowerCase(Locale.US); 
                    if(!qName.equals("comments")) { // ignore comments!
                        attributeMap.put((_result.canonicalKeyPrefix + 
                                qName + "__").intern(),
                                attributes.getValue(i).trim().intern());
                    }                                        
                }
                _result.add(attributeMap);
            } else {
                Map<String, String> empty = Collections.emptyMap();
                _result.add(empty);
            }
        }

        @Override
        public void fatalError(SAXParseException e) throws SAXException {
            LOG.fatal("Fatal parsing error", e);
            throw e;
        }

        @Override
        public void warning(SAXParseException e) throws SAXException {
            LOG.warn("parse warning", e);
        }
        
        @Override
        public void error(SAXParseException e) throws SAXException {
            LOG.error("Parsing error", e);
        }
    }
}
