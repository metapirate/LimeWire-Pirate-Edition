package org.limewire.core.impl.friend;

import java.net.InetAddress;
import java.util.Collections;
import java.util.Map;

import org.limewire.core.settings.SearchSettings;
import org.limewire.friend.api.Friend;
import org.limewire.friend.api.FriendPresence;
import org.limewire.friend.api.IncomingChatListener;
import org.limewire.friend.api.MessageReader;
import org.limewire.friend.api.MessageWriter;
import org.limewire.friend.api.Network;
import org.limewire.friend.api.PresenceEvent;
import org.limewire.io.Address;
import org.limewire.io.Connectable;
import org.limewire.io.IpPort;
import org.limewire.listener.EventListener;
import org.limewire.util.ByteUtils;
import org.limewire.util.Objects;

import com.limegroup.gnutella.PushEndpoint;

class GnutellaFriend implements Friend {

    private final Address address;
    private final FriendPresence presence;
    
    private static final String UNKNOWN_ADDRESS_DESCRIPTION = "Unknown";
    
    private static final String[] adjs = { "Almond", "Brass", "Apricot", "Aqua", "Asparagus", "Tangerine",
            "Awesome", "Banana", "Bear", "Bittersweet", "Fast", "Blue", "Bell", "Gray", "Green",
            "Violet", "Red", "Pink", "Orange", "Sienna", "Cool", "Earthy", "Caribbean", "Elder",
            "Pink", "Cerise", "Cerulean", "Chestnut", "Copper", "Better", "Candy", "Cranberry",
            "Dandelion", "Denim", "Gray", "Sand", "Desert", "Eggplant", "Lime", "Electric",
            "Famous", "Fern", "Forest", "Fuchsia", "Fuzzy", "Tree", "Gold", "Apple", "Smith",
            "Magenta", "Indigo", "Jazz", "Berry", "Jam", "Jungle", "Lemon", "Cold", "Lavender",
            "Hot", "New", "Ordinary", "Magenta", "Frowning", "Mint", "Mahogany", "Pretty", "Strange",
            "Grumpy", "Itchy", "Maroon", "Melon", "Midnight", "Clumsy", "Better", "Smiling",
            "Navy", "Neon", "Olive", "Orchid", "Outer", "Tame", "Cheerful", "Peach", "Periwinkle",
            "Pig", "Pine", "Nutty", "Plum", "Purple", "Rose", "Salmon", "Scarlet", "Nice", "Jolly",
            "Great", "Silver", "Sky", "Spring", "Long", "Glow", "Set", "Happy", "Tan", "Thistle",
            "Timber", "Tough", "Torch", "Smart", "Funny", "Tropical", "Tumble", "Ultra", "White",
            "Wild", "Yellow", "Eager", "Joyous", "Jumpy", "Kind", "Lucky", "Meek", "Nifty",
            "Adorable", "Aggressive", "Alert", "Attractive", "Average", "Bright", "Fragile",
            "Graceful", "Handsome", "Light", "Long", "Misty", "Muddy", "Plain", "Poised",
            "Precious", "Shiny", "Sparkling", "Stormy", "Wide", "Alive", "Annoying", "Better",
            "Brainy", "Busy", "Clever", "Clumsy", "Crazy", "Curious", "Easy", "Famous", "Frail",
            "Gifted", "Important", "Innocent", "Modern", "Mushy", "Odd", "Open", "Powerful",
            "Real", "Shy", "Sleepy", "Super", "Tame", "Tough", "Vast", "Wild", "Wrong", "Annoyed",
            "Anxious", "Crazy", "Dizzy", "Dull", "Evil", "Foolish", "Frantic", "Grieving",
            "Grumpy", "Helpful", "Hungry", "Lazy", "Lonely", "Scary", "Tense", "Weary", "Worried",
            "Brave", "Calm", "Charming", "Magic", "Easer", "Elated", "Enchanting", "Excited",
            "Fair", "Fine", "Friendly", "Funny", "Gentle", "Good", "Happy", "Healthy", "Jolly",
            "Kind", "Lovely", "Nice", "Perfect", "Proud", "Silly", "Smiling", "Thankful", "Witty",
            "Zany", "Big", "Fat", "Great", "Huge", "Immense", "Puny", "Scrawny", "Short", "Small",
            "Tall", "Teeny", "Tiny", "Faint", "Harsh", "Loud", "Melodic", "Mute", "Noisy", "Quiet",
            "Raspy", "Soft", "Whispering", "Ancient", "Fast", "Late", "Long", "Modern", "Old",
            "Quick", "Rapid", "Short", "Slow", "Swift", "Bitter", "Fresh", "Ripe", "Rotten",
            "Salty", "Sour", "Spicy" };

    private static final String[] nouns = { "Alligator", "Alpaca", "Antelope", "Badger", "Armadillo",
            "Bat", "Bear", "Bee", "Bird", "Bison", "Buffalo", "Boar", "Butterfly", "Camel", "Cat",
            "Cattle", "Cow", "Chicken", "Clam", "Cockroach", "Codfish", "Coyote", "Crane", "Crow",
            "Deer", "Dinosaur", "Velociraptor", "Dog", "Dolphin", "Donkey", "Dove", "Duck",
            "Eagle", "Eel", "Elephant", "Elk", "Emu", "Falcon", "Ferret", "Fish", "Finch", "Fly",
            "Fox", "Frog", "Gerbil", "Giraffe", "Gnat", "Gnu", "Goat", "Goose", "Gorilla",
            "Grasshopper", "Grouse", "Gull", "Hamster", "Hare", "Hawk", "Hedgehog", "Heron",
            "Hornet", "Hog", "Horse", "Hound", "Hummingbird", "Hyena", "Jay", "Jellyfish",
            "Kangaroo", "Koala", "Lark", "Leopard", "Lion", "Llama", "Mallard", "Mole", "Monkey",
            "Moose", "Mosquito", "Mouse", "Mule", "Nightingale", "Opossum", "Ostrich", "Otter",
            "Owl", "Ox", "Oyster", "Panda", "Parrot", "Peafowl", "Penguin", "Pheasant", "Pig",
            "Pigeon", "Platypus", "Porpoise", "PrarieDog", "Pronghorn", "Quail", "Rabbit",
            "Raccoon", "Rat", "Raven", "Reindeer", "Rhinoceros", "Seal", "Seastar", "Serval",
            "Shark", "Sheep", "Skunk", "Snake", "Snipe", "Sparrow", "Spider", "Squirrel", "Swallow",
            "Swan", "Termite", "Tiger", "Toad", "Trout", "Turkey", "Turtle", "Wallaby", "Walrus",
            "Wasp", "Weasel", "Whale", "Wolf", "Wombat", "Woodpecker", "Wren", "Yak", "Zebra",
            "Ball", "Bed", "Book", "Bun", "Can", "Cake", "Cap", "Car", "Cat", "Day", "Fan", "Feet",
            "Hall", "Hat", "Hen", "Jar", "Kite", "Man", "Map", "Men", "Panda", "Pet", "Pie", "Pig",
            "Pot", "Sun", "Toe", "Apple", "Armadillo", "Banana", "Bike", "Book", "Clam", "Mushroom",
            "Clover", "Club", "Corn", "Crayon", "Crown", "Crib", "Desk", "Dress", "Flower", "Fog",
            "Game", "Hill", "Home", "Hornet", "Hose", "Joke", "Juice", "Mask", "Mice", "Alarm",
            "Bath", "Bean", "Beam", "Camp", "Crook", "Deer", "Dock", "Doctor", "Frog", "Good",
            "Jam", "Face", "Honey", "Kitten", "Fruit", "Fuel", "Cable", "Calculator", "Circle",
            "Guitar", "Bomb", "Border", "Apparel", "Activity", "Desk", "Art", "Colt", "Cyclist",
            "Biker", "Blogger", "Anchoby", "Carp", "Glassfish", "Clownfish", "Barracuda", "Eel",
            "Moray", "Stingray", "Flounder", "Swordfish", "Marlin", "Pipefish", "Grunter",
            "Grunion", "Grouper", "Guppy", "Gulper", "Crab", "Lobster", "Halibut", "Hagfish",
            "Horsefish", "Seahorse", "Jellyfish", "Killifish", "Trout", "Pike", "Ray", "Razorfish",
            "Ragfish", "Hamster", "Gerbil", "Mouse", "Gnome", "Shark", "Snail", "Skilfish" };    

    public GnutellaFriend(Address address, FriendPresence presence) {
        this.address = Objects.nonNull(address, "address");
        this.presence = presence;
    }
    
    Address getAddress() {
        return address;
    }
    
    private String describe(Address address) {
        if(address instanceof Connectable || address instanceof PushEndpoint) {
            IpPort ipp = (IpPort)address;
            InetAddress inetAddr = ipp.getInetAddress();
            return inetAddr == null ? ipp.getAddress() : inetAddr.getHostAddress();
        } else {
            return address.getAddressDescription();
        }
    }
    
    private String describeFriendly(Address address) {
        if(!SearchSettings.FRIENDLY_ADDRESS_DESCRIPTIONS.getValue()) {
            return describe(address);
        } else if(address instanceof Connectable || address instanceof PushEndpoint) {
            // Convert IP addr into a #.
            IpPort ipp = (IpPort)address;
            InetAddress inetAddr = ipp.getInetAddress();
            if(inetAddr == null) {
                return UNKNOWN_ADDRESS_DESCRIPTION;
            }
            byte[] addr = inetAddr.getAddress();
            
            if (addr.length != 4) {
                return UNKNOWN_ADDRESS_DESCRIPTION;
            }

            //create a fake name
            int i1 = ByteUtils.ubyte2int(addr[0]);
            int i2 = ByteUtils.ubyte2int(addr[1]);
            int i3 = ByteUtils.ubyte2int(addr[2]);
            int i4 = ByteUtils.ubyte2int(addr[3]);
            return adjs[i1] + nouns[i2] + "-" + i3 + "-" + i4;
        } else {
            return address.getAddressDescription();
        }
    }

    @Override
    public boolean isAnonymous() {
        return true;
    }

    @Override
    public String getId() {
        return presence.getPresenceId();
    }

    @Override
    public String getName() {
        return describe(address);
    }

    @Override
    public String getRenderName() {
        return describeFriendly(address);
    }

    @Override
    public void setName(String name) {
    }

    @Override
    public Network getNetwork() {
        return null;
    }

    @Override
    public String getFirstName() {
        return getName();
    }

    @Override
    public void addPresenceListener(EventListener<PresenceEvent> presenceListener) {
    }

    @Override
    public MessageWriter createChat(MessageReader reader) {
        return null;
    }

    @Override
    public void setChatListenerIfNecessary(IncomingChatListener listener) {
    }

    @Override
    public void removeChatListener() {
    }

    @Override
    public FriendPresence getActivePresence() {
        return null;
    }

    @Override
    public boolean hasActivePresence() {
        return false;
    }

    @Override
    public boolean isSignedIn() {
        return false;
    }

    @Override
    public Map<String, FriendPresence> getPresences() {
        return Collections.singletonMap(presence.getPresenceId(), presence);
    }

    @Override
    public boolean isSubscribed() {
        return false;
    }
    
    @Override
    public String toString() {
        return "renderName[" + getRenderName() + "], name[" + getName() + "], id[" + getId() + "]"; 
    }
}
