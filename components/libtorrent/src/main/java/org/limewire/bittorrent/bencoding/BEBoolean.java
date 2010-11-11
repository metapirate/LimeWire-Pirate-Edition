package org.limewire.bittorrent.bencoding;

import java.io.IOException;

/**
 * A token representing a boolean value.
 */
class BEBoolean extends Token<Boolean> {
    public static final BEBoolean TRUE = new BEBoolean(true);
    public static final BEBoolean FALSE = new BEBoolean(false);
    public BEBoolean(boolean value) {
        super(null);
        result = value;
    }
    @Override
    public int getType() {
        return Token.BOOLEAN;
    }
    @Override
    public boolean isDone() {
        return true;
    }

    @Override
    public void handleRead() throws IOException {
    }
}