package org.limewire.ui.swing.filter;

import org.limewire.ui.swing.util.I18n;
import org.limewire.util.Objects;

/**
 * An item in the SourceFilter list.
 */
class SourceItem {
    /** Source types. */
    public static enum Type {
        ANONYMOUS, ANY_FRIEND, FRIEND;
    }
    
    public static final SourceItem ANONYMOUS_SOURCE = new SourceItem(Type.ANONYMOUS, I18n.tr("P2P Users"));
    public static final SourceItem ANY_FRIEND_SOURCE = new SourceItem(Type.ANY_FRIEND, I18n.tr("Any Friend"));
   
    private final Type type;
    private final String name;
    
    /**
     * Constructs a SourceItem with the specified type and name.
     */
    public SourceItem(Type type, String name) {
        this.type = type;
        this.name = name;
    }
    
    /**
     * Returns the display name.
     */
    public String getName() {
        return name;
    }
    
    /**
     * Returns the source type.
     */
    public Type getType() {
        return type;
    }
    
    @Override
    public String toString() {
        return getName();
    }
    
    @Override
    public boolean equals(Object obj) {
        if(obj == this) {
            return true; 
        }
        
        if (obj instanceof SourceItem) {
            SourceItem item2 = (SourceItem) obj;
            if (type == item2.type) {
                return (Objects.compareToNullIgnoreCase(name, item2.name, false) == 0);
            }
        }
        
        return false;
    }
    
    @Override
    public int hashCode() {
        int result = 17;
        result = 31 * result + type.hashCode();
        result = 31 * result + ((name != null) ? name.hashCode() : 0);
        return result;
    }
}
