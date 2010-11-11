package org.limewire.friend.api;

import org.limewire.listener.DefaultDataTypeEvent;

public class FileOfferEvent extends DefaultDataTypeEvent<FileOffer, FileOfferEvent.Type> {

    public FileOfferEvent(FileOffer data, Type event) {
        super(data, event);
    }

    public enum Type {OFFER}
}
