package com.limegroup.gnutella.altlocs;

import org.limewire.core.settings.UploadSettings;
import org.limewire.util.Objects;

import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.downloader.RemoteFileDescFactory;

/**
 * Abstract super class that encompasses common functionality shared by most
 * implementations of {@link AlternateLocation}.
 */
public abstract class AbstractAlternateLocation implements AlternateLocation {

    /**
     * Constant for the sha1 urn for this <tt>AlternateLocation</tt> -- can be
     * <tt>null</tt>.
     */
    protected final URN SHA1_URN;

    /**
     * Constant for the string to display as the httpStringValue.
     */
    private String DISPLAY_STRING;

    /**
     * Cached hash code that is lazily initialized.
     */
    protected volatile int hashCode = 0;

    /**
     * LOCKING: obtain this' monitor while changing/accessing _count and
     * _demoted as multiple threads could be accessing them.
     */

    /**
     * Maintains a count of how many times this alternate location has been
     * seen. A value of 0 means this alternate location was failed one more time
     * that it has succeeded. Newly created AlternateLocations start out wit a
     * value of 1.
     */
    protected volatile int _count = 0;

    /**
     * Two counter objects to keep track of altloc expiration.
     */
    private final Average legacy, ping, response;

    protected AbstractAlternateLocation(URN sha1) {
        SHA1_URN = Objects.nonNull(sha1, "sha1");
        legacy = new Average();
        ping = new Average();
        response = new Average();
    }

    // ////////////////////////////accessors////////////////////////////

    /*
     * (non-Javadoc)
     * 
     * @see com.limegroup.gnutella.altlocs.AlternateLocation#getSHA1Urn()
     */
    public URN getSHA1Urn() {
        return SHA1_URN;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.limegroup.gnutella.altlocs.AlternateLocation#getCount()
     */
    public synchronized int getCount() {
        return _count;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.limegroup.gnutella.altlocs.AlternateLocation#isDemoted()
     */
    public abstract boolean isDemoted();

    // //////////////////////////Mesh utility
    // methods////////////////////////////

    public String httpStringValue() {
        if (DISPLAY_STRING == null)
            DISPLAY_STRING = generateHTTPString();
        return DISPLAY_STRING;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.limegroup.gnutella.altlocs.AlternateLocation#createRemoteFileDesc
     * (long)
     */

    public abstract RemoteFileDesc createRemoteFileDesc(long size,
            RemoteFileDescFactory remoteFileDescFactory);

    /*
     * (non-Javadoc)
     * 
     * @see com.limegroup.gnutella.altlocs.AlternateLocation#isMe()
     */
    public abstract boolean isMe();

    /*
     * (non-Javadoc)
     * 
     * @see com.limegroup.gnutella.altlocs.AlternateLocation#increment()
     */
    public synchronized void increment() {
        _count++;
    }

    /**
     * package access for demoting this.
     */
    abstract void demote();

    /**
     * package access for promoting this.
     */
    abstract void promote();

    /*
     * (non-Javadoc)
     * 
     * @see com.limegroup.gnutella.altlocs.AlternateLocation#createClone()
     */
    public abstract AlternateLocation createClone();

    /*
     * (non-Javadoc)
     * 
     * @see com.limegroup.gnutella.altlocs.AlternateLocation#send(long, int)
     */
    public synchronized void send(long now, int meshType) {
        switch (meshType) {
        case MESH_LEGACY:
            legacy.send(now);
            return;
        case MESH_PING:
            ping.send(now);
            return;
        case MESH_RESPONSE:
            response.send(now);
            return;
        default:
            throw new IllegalArgumentException("unknown mesh type");
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.limegroup.gnutella.altlocs.AlternateLocation#canBeSent(int)
     */
    public synchronized boolean canBeSent(int meshType) {
        switch (meshType) {
        case MESH_LEGACY:
            if (!UploadSettings.EXPIRE_LEGACY.getValue())
                return true;
            return legacy.canBeSent(UploadSettings.LEGACY_BIAS.getValue(),
                    UploadSettings.LEGACY_EXPIRATION_DAMPER.getValue());
        case MESH_PING:
            if (!UploadSettings.EXPIRE_PING.getValue())
                return true;
            return ping.canBeSent(UploadSettings.PING_BIAS.getValue(),
                    UploadSettings.PING_EXPIRATION_DAMPER.getValue());
        case MESH_RESPONSE:
            if (!UploadSettings.EXPIRE_RESPONSE.getValue())
                return true;
            return response.canBeSent(UploadSettings.RESPONSE_BIAS.getValue(),
                    UploadSettings.RESPONSE_EXPIRATION_DAMPER.getValue());

        default:
            throw new IllegalArgumentException("unknown mesh type");
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.limegroup.gnutella.altlocs.AlternateLocation#canBeSentAny()
     */
    public synchronized boolean canBeSentAny() {
        return canBeSent(MESH_LEGACY) || canBeSent(MESH_PING) || canBeSent(MESH_RESPONSE);
    }

    synchronized void resetSent() {
        ping.reset();
        legacy.reset();
        response.reset();
    }

    // /////////////////////////////helpers////////////////////////////////

    /**
     * Overrides the equals method to accurately compare
     * <tt>AlternateLocation</tt> instances. <tt>AlternateLocation</tt>s are
     * equal if their <tt>URL</tt>s are equal.
     * 
     * @param obj the <tt>Object</tt> instance to compare to
     * @return <tt>true</tt> if the <tt>URL</tt> of this
     *         <tt>AlternateLocation</tt> is equal to the <tt>URL</tt> of the
     *         <tt>AlternateLocation</tt> location argument, and otherwise
     *         returns <tt>false</tt>
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;
        if (!(obj instanceof AbstractAlternateLocation))
            return false;
        AbstractAlternateLocation other = (AbstractAlternateLocation) obj;

        return SHA1_URN.equals(other.SHA1_URN);

    }

    /**
     * The idea is that this is smaller than any AlternateLocation who has a
     * greater value of _count. There is one exception to this rule -- a demoted
     * AlternateLocation has a higher value irrespective of count.
     * <p>
     * This is because we want to have a sorted set of AlternateLocation where
     * any demoted AlternateLocation is put at the end of the list because it
     * probably does not work.
     * <p>
     * Further we want to get AlternateLocations with smaller counts to be
     * propogated more, since this will serve to get better load balancing of
     * uploader.
     */
    public int compareTo(AlternateLocation other) {
        int ret = _count - other.getCount();
        if (ret != 0)
            return ret;

        return ret;

    }

    protected abstract String generateHTTPString();

    /**
     * Overrides the hashCode method of Object to meet the contract of hashCode.
     * Since we override equals, it is necessary to also override hashcode to
     * ensure that two "equal" alternate locations return the same hashCode,
     * less we unleash unknown havoc on the hash-based collections.
     * 
     * @return a hash code value for this object
     */
    @Override
    public int hashCode() {

        return 17 * 37 + this.SHA1_URN.hashCode();
    }

    private static class Average {
        /** The number of times this altloc was given out */
        private int numTimes;

        /** The average time in ms between giving out the altloc */
        private double average;

        /** The last time the altloc was given out */
        private long lastSentTime;

        /** The last calculated threshold, -1 if dirty */
        private double cachedTreshold = -1;

        public void send(long now) {
            if (lastSentTime == 0)
                lastSentTime = now;

            average = ((average * numTimes) + (now - lastSentTime)) / ++numTimes;
            lastSentTime = now;
            cachedTreshold = -1;
        }

        public boolean canBeSent(float bias, float damper) {
            if (numTimes < 2 || average == 0)
                return true;

            if (cachedTreshold == -1)
                cachedTreshold = Math.abs(Math.log(average) / Math.log(damper));

            return numTimes < cachedTreshold * bias;
        }

        public void reset() {
            numTimes = 0;
            average = 0;
            lastSentTime = 0;
        }
    }
}
