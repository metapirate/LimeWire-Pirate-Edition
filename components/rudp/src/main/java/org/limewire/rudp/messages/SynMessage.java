package org.limewire.rudp.messages;

/**
 * Defines an interface to begin a reliable UDP connection. 
 */
public interface SynMessage extends RUDPMessage {

    /**
     * Enum that defines the role the connection plays from which this
     * SynMessage stems.
     */
    public static enum Role {
        /**
         * Requestor of a UDP connection after having send a request to open a 
         * connection to the acceptor.
         */
        REQUESTOR(0x01),
        /**
         * Acceptor of a UDP connection after having received a request for 
         * opening a connection from the requestor.
         */
        ACCEPTOR(0x02),
        /**
         * Undefined role, used for messages with protocol version < 1. 
         */
        UNDEFINED(0x00);
        
        private static final Role[] ROLES;
        
        private final int value;

        static {
            Role[] roles = values();
            ROLES = new Role[roles.length];
            for (Role role : roles) {
                int index = (role.value & 0xFF) % ROLES.length;
                assert (ROLES[index] == null);
                ROLES[index] = role;
            }
        }
        
        Role(int value) {
            this.value = value;
        }
        
        /**
         * Returns byte value. 
         */
        public byte byteValue() {
            return (byte) (value & 0xFF);
        }
        
        /**
         * Returns the Role with <code>value</code> or null if not found. 
         */
        public static Role valueOf(int value) {
            int index = (value & 0xFF) % ROLES.length;
            Role role = ROLES[index];
            if (role.value == value) {
                return role;
            }
            return null;
        }
        
        /**
         * Returns true if a connection with this role can connect accept
         * an incoming {@link SynMessage} with role <code>role</code>. 
         */
        public boolean canConnectTo(Role role) {
            return role != this || role == UNDEFINED;
        }

    }
    
    public byte getSenderConnectionID();

    public int getProtocolVersionNumber();
    
    /**
     * Returns the role of the sender of the syn message. 
     */
    public Role getRole();

}