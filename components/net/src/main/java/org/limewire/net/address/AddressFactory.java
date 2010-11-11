package org.limewire.net.address;

import java.io.IOException;

import org.limewire.io.Address;

/**
 * A collection of <code>AddressSerializer</code>s.  <code>Address</code>s should register
 * themselves with this factory via the <code>addSerializer()</code> method at injection time.
 */
public interface AddressFactory {
    /**
     * Registers an AddressSerializer with this AddressFactory.
     */
    public void registerSerializer(AddressSerializer serializer);

    /**
     * @param address cannot be null
     * @return the AddressSerializer for a particular class
     * @throws IllegalArgumentException if an AddressSerializer does not
     * exist for the specified address
     */
    public AddressSerializer getSerializer(Address address) throws IllegalArgumentException;
    /**
     * Looks up serializer by {@link AddressSerializer#getAddressType()}. 
     * @return null if no serializer is registered for that type
     */
    public AddressSerializer getSerializer(String addressType);

    /**
     * Deserialize an address, typically as read from a network message
     * @param type the type of message contained in the byte array.  Will match
     * AddressSerializer.getType() for the AddressSerialzer for the Address contained
     * in the byte []
     * @return a non-null Address
     * @throws IOException if there is an error deserializing the Address
     */
    public Address deserialize(String type, byte [] serializedAddress) throws IOException;

    /**
     * Turns a user-input String into an Address.
     * @return  an address representing the String parameter; never null
     * @throws IOException if the input cannot be converted into an Address
     */
    public Address deserialize(String address) throws IOException;
}
