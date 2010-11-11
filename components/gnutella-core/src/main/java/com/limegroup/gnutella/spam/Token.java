package com.limegroup.gnutella.spam;

import java.io.Serializable;

/**
 * An abstract Token class that uses an exponential moving average to update
 * its spam rating. Each subclass can have a different weight that determines
 * how sensitive its spam rating is to updates. 
 */
public abstract class Token implements Serializable {

    private static final long serialVersionUID = -1519906405576105402L;

    /**
     * The spam rating of this token, between 0 (not spam) and 1 (spam).
     * Initialised to 0.
     */
    protected float rating = 0;

    /**
     * Returns a weight between 0 and 1 that determines how sensitive this
     * token's spam rating is to updates - 0 means an update has no effect on
     * the rating, while 1 means an update completely changes the rating.
     * 
     * @return a weight between 0 (not sensitive) and 1 (sensitive)
     */
    abstract protected float getWeight();

    /**
     * Gets this token's spam rating
     * 
     * @return a rating between 0 (not spam) and 1 (spam)
     */
    protected float getRating() {
        return rating;
    }

    /** Sets this token's spam rating
     * 
     * @param rating a rating between 0 (not spam) and 1 (spam)
     */
    protected void setRating(float rating) {
        this.rating = rating;
    }

    /**
     * Updates this token's spam rating by using an exponential moving average
     * to combine the new rating with the current rating. Subclasses must
     * supply the weight of the exponential moving average (between 0 and 1).
     *  
     * @param update the new rating
     */
    protected void updateRating(float update) {
        float weight = getWeight();
        rating = rating * (1 - weight) + update * weight;
    }
}
