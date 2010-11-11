package org.limewire.net.address;

import java.io.IOException;

import org.limewire.io.Address;

import com.google.inject.Inject;

/**
 * Used to (de)serialize <code>Addresses</code> over a network connection.
 */
public interface AddressSerializer {
    /**
     * Register this <code>AddressSerializer</code> with an
     * <code>AddressFactory</code>.
     */
    @Inject
    public void register(AddressFactory factory);

    /**
     * @return a non-null String representing a human readable short
     * description of the kind of Address this serializer handles.
     */
    public String getAddressType();

    /**
     * @return true if this serializer can serialize <code>address</code>,
     * false otherwise
     */
    public boolean canSerialize(Address address);

    /**
     * Deserialize a byte [] representation of an Address, typically as
     * read from a network message, into an Address.
     * @return a non-null Address
     * @throws IOException if the input cannot be deserialized into
     * an Address
     */
    public Address deserialize(byte [] serializedAddress) throws IOException;

    /**
     * Serialize an Address into a byte [], typically to include in a
     * network message.
     * @param address a non-null address
     * @return a non-null byte []
     * @throws IOException if an error occurs serializing the
     * Address
     */
    public byte [] serialize(Address address) throws IOException;

    /**
     * Turns a user-input String into an Address.
     * @return  an address representing the String parameter; never null
     * @throws IOException if the input cannot be converted into an Address
     */
    Address deserialize(String address) throws IOException;
}
