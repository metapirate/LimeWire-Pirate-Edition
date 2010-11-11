package org.limewire.ui.swing.shell;

/**
 * Links a ShellAssociation with LimeWire settings, name & description, in order
 * to have a single object that can display the association and determine if it
 * should be registered or not.
 */
public class LimeAssociationOption {

    /** The association. */
    private final ShellAssociation association;

    /** A short name & description. */
    private final String name, description;

    /**
     * Constructs a new LimeAssociationOption linking a ShellAssociation to a
     * setting, short name & description.
     * 
     * @param association the ShellAssociation
     * @param name A short name of the association
     * @param description A description of the association
     */
    public LimeAssociationOption(ShellAssociation association, String name, String description) {
        this.association = association;
        this.name = name;
        this.description = description;
    }

    /**
     * Either links or delinks the association to this program. This does not
     * effect the setting.
     */
    public void setEnabled(boolean enabled) {
        if (enabled) {
            association.unregister();
            association.register();
        } else if (association.isRegistered()) {
            association.unregister();
        }
    }
    
    public boolean canDisassociate() {
        return association.canUnregister();
    }

    /** Determines if the association is currently registered to this program. */
    public boolean isEnabled() {
        return association.isRegistered();
    }

    /** Retrieves the short name of the association. */
    public String getName() {
        return name;
    }

    /** Retrieves the long name of the association. */
    public String getDescription() {
        return description;
    }

    /**
     * Returns true if the association is currently unhandled by any
     * application.
     */
    public boolean isAvailable() {
        return association.isAvailable();
    }
}
