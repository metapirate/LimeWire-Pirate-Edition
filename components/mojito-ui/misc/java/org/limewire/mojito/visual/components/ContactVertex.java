package org.limewire.mojito.visual.components;

import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.routing.impl.LocalContact;


public class ContactVertex extends RouteTableVertex<Contact>{
    
    private Contact contact;
    private final boolean isLocal;
    private int depth;
    
    public ContactVertex(Contact contact, int depth) {
        super();
        this.contact = contact;
        this.depth = depth;
        isLocal = contact instanceof LocalContact;
    } 
    
    
    @Override
    public Contact getNode() {
        return contact;
    }

    @Override
    public boolean isLocal() {
        return isLocal;
    }

    @Override
    public String toString() {
        return contact.getNodeID().toBinString().substring(0, depth);
    }
}
