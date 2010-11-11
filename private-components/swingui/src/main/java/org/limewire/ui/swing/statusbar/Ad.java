package org.limewire.ui.swing.statusbar;

/**
 * A short message with an associated URL and probability of being displayed.
 */
class Ad implements Comparable<Ad>{
    private final String text;
    private final String uri;
    private final float probability;
    
    Ad(String text, String uri, float probability) {
        this.text = text;
        this.uri = uri;
        this.probability = probability;
    }
    
    public String getText() {
        return text;
    }
    
    public String getURI() {
        return uri;
    }
    
    float getProbability() {
        return probability;
    }
    
    /**
     * sorts by probability, highest first.
     */
    public int compareTo(Ad other) {
        return -Float.compare(getProbability(), other.getProbability());
    }
}
