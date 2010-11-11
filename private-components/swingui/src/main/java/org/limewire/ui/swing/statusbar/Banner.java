package org.limewire.ui.swing.statusbar;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Class that keeps track of several ad messages and displays a single
 * one based on their probability. 
 */
class Banner {
    
    private final List<Ad> ads;
    
    /**
     * @param source the encoded representation of the messages:
     * <message1, url1, probability1, message2, url2, probability2...>
     * the probabilites must add up to 1.0.
     */
    public Banner(String... source) {
        if (source.length % 3 != 0)
            throw new IllegalArgumentException();
        
        ads = new ArrayList<Ad>(source.length / 3);
        for (int i = 0; i < source.length; i+=3) {
            ads.add(new Ad(source[i], source[i+1], Float.valueOf(source[i+2])));
        }
        
        Collections.sort(ads);
        
        float total = 0;
        for (Ad ad : ads)
            total += ad.getProbability();
        if (total < 0.9999 || total > 1.0)
            throw new IllegalArgumentException("wrong probabilities: "+total);
    }
    
    /**
     * @return the next message that should be displayed.
     */
    public Ad getAd() {
        float dice = (float)Math.random();
        float current = 0; 
        for (Ad ad : ads) {
            current += ad.getProbability();
            if (current >= dice)
                return ad;
        }
        return ads.get(ads.size() - 1);
    }
    
    public Collection<Ad> getAllAds() {
        return Collections.unmodifiableCollection(ads);
    }
}
