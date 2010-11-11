package org.limewire.bittorrent.bencoding;

import java.io.IOException;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.List;

/**
 * A token used to parse a bencoded list of elements.
 */
class BEList extends BEAbstractCollection<List<Object>> {
    
    BEList(ReadableByteChannel chan) {
        super(chan);
    }
    
    @Override
    protected List<Object> createCollection() {
        return new ArrayList<Object>();
    }
    
    @Override
    protected void add(Object o) {
        result.add(o);
    }
    
    @Override
    protected Token<?> getNewElement() throws IOException {
        return getNextToken(chan);
    }
    
    @Override
    public int getType() {
        return LIST;
    }
}
