package org.limewire.ui.swing.util;

public enum EnabledType {

    ENABLED(true),
    NOT_ENABLED(false);

    private final boolean isEnabled;

    EnabledType(boolean isEnabled) {
        this.isEnabled = isEnabled;
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public static EnabledType valueOf(boolean isEnabled) {
        return isEnabled ? ENABLED : NOT_ENABLED;
    }
}
