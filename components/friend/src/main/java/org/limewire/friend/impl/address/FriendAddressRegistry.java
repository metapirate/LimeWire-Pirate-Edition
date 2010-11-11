package org.limewire.friend.impl.address;

import java.util.concurrent.ConcurrentHashMap;

import org.limewire.friend.impl.address.FriendAddress;
import org.limewire.io.Address;
import org.limewire.util.StringUtils;

import com.google.inject.Singleton;

/**
 * Maintains the currently known addresse for each {@link FriendAddress}.
 * 
 * Used for address resolution.
 */
@Singleton
public class FriendAddressRegistry {
    
    private final ConcurrentHashMap<FriendAddress, Address> addressMap;
    
    public FriendAddressRegistry() {
        this.addressMap = new ConcurrentHashMap<FriendAddress, Address>();
    }

    public void put(FriendAddress friendAddress, Address address){
        addressMap.put(friendAddress, address);
    }
    
    public Address get(FriendAddress friendAddress) {
       return addressMap.get(friendAddress); 
    }
    
    public void remove(FriendAddress friendAddress) {
        addressMap.remove(friendAddress);
    }
    
    @Override
    public String toString() {
        return StringUtils.toString(this);
    }
}
