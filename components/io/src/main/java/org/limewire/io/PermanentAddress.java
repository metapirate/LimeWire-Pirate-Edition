package org.limewire.io;

/**
 * Interface to mark an address a permanent address that can be serialized
 * between sessions. These addresses usually need address resolution, examples
 * could be a friend id, or a client guid.
 */
public interface PermanentAddress extends Address {

}
