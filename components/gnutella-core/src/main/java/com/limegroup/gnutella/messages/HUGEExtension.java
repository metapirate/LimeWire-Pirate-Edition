package com.limegroup.gnutella.messages;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.limewire.io.BadGGEPBlockException;
import org.limewire.io.GGEP;

import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.UrnSet;

/** 
 * Encapsulation of a HUGE block.  Offers various get methods to retrieve its
 * contents, and handles parsing, etc.
 */
public class HUGEExtension {

    // the disparate types of objects encoded in a HUGE extension - one set per
    // (lazily constructed)
    // -----------------------------------------
    private GGEP _ggep = null;
    private Set<URN> _urns = null;
    private Set<URN.Type> _urnTypes = null;
    private Set<String> _miscBlocks = null;
    // -----------------------------------------
    
    private List<GGEPBlock> _ggepBlocks = null;

    /**
     *  @return the merged GGEP of all GGEPs in this HUGE extension or null
     *  if no GGEPs were found
     */
    public GGEP getGGEP() {
        return _ggep;
    }
    
    /**
     * Returns unmodifiable list of GGEP blocks.
     * @return empty list if there no GGEP blocks were found
     */
    public List<GGEPBlock> getGGEPBlocks() {
        if (_ggepBlocks == null) {
            return Collections.emptyList();
        }
        else {
            return Collections.unmodifiableList(_ggepBlocks);
        }
    }
    
    /** @return the set of URN Objects in this HUGE extension.
     */
    public Set<URN> getURNS() {
        if (_urns == null)
            return Collections.emptySet();
        else
            return _urns;
    }
    /** @return the set of URN Type Objects in this HUGE extension.
     */
    public Set<URN.Type> getURNTypes() {
        if (_urnTypes == null)
            return Collections.emptySet();
        else
            return _urnTypes;
    }
    /** @return the set of miscellaneous blocks (Strings) in this extension.
     */
    public Set<String> getMiscBlocks() {
        if (_miscBlocks == null)
            return Collections.emptySet();
        else 
            return _miscBlocks;
    }

    public HUGEExtension(byte[] extsBytes) {
        int currIndex = 0;
        // while we don't encounter a null....
        while ((currIndex < extsBytes.length) && 
               (extsBytes[currIndex] != (byte)0x00)) {
            
            // HANDLE GGEP STUFF
            if (extsBytes[currIndex] == GGEP.GGEP_PREFIX_MAGIC_NUMBER) {
                int start = currIndex;
                int[] endIndex = new int[1];
                endIndex[0] = currIndex+1;
                try {
                    GGEP ggep = new GGEP(extsBytes, currIndex, endIndex);
                    if (_ggep == null) {
                        _ggep = new GGEP();
                    }
                    _ggep.merge(ggep);
                    if (_ggepBlocks == null) {
                        _ggepBlocks = new ArrayList<GGEPBlock>(2);
                    }
                    _ggepBlocks.add(new GGEPBlock(ggep, start, endIndex[0]));
                } catch (BadGGEPBlockException ignored) {}
                currIndex = endIndex[0];
            } else { // HANDLE HUGE STUFF
                int delimIndex = currIndex;
                while ((delimIndex < extsBytes.length) 
                       && (extsBytes[delimIndex] != (byte)0x1c))
                    delimIndex++;
                if (delimIndex <= extsBytes.length) {
                    try {
                        // another GEM extension
                        String curExtStr = new String(extsBytes, currIndex,
                                                      delimIndex - currIndex,
                                                      "UTF-8");
                        if (URN.isUrn(curExtStr)) {
                            // it's an URN to match, of form "urn:namespace:etc"
                            URN urn = URN.createSHA1Urn(curExtStr);
                            if(_urns == null) 
                                _urns = new UrnSet();
                            _urns.add(urn);
                        } else if (URN.Type.isSupportedUrnType(curExtStr)) {
                            if(_urnTypes == null)
                                _urnTypes = EnumSet.noneOf(URN.Type.class);
                            _urnTypes.add(URN.Type.createUrnType(curExtStr));
                        } else {
                            // miscellaneous, but in the case of queries, xml
                            if (_miscBlocks == null)
                                _miscBlocks = new HashSet<String>(1);
                            _miscBlocks.add(curExtStr);
                        }
                    } catch (IOException bad) {}
                } // else we've overflown and not encounted a 0x1c - discard
                currIndex = delimIndex+1;
            }
        }        
    }

    /**
     * Represents a single ggep block in the HUGE extension block.
     */
    public static class GGEPBlock {

        private int start;
        
        private int end;
        
        private GGEP ggep;
        
        public GGEPBlock(GGEP ggep, int start, int end) {
            this.ggep = ggep;
            this.start = start;
            this.end = end;
        }
        
        public GGEP getGGEP() {
            return ggep;
        }
        
        public int getStartPos() {
            return start;
        }
        
        public int getEndPos() {
            return end;
        }
        
    }
    
}
