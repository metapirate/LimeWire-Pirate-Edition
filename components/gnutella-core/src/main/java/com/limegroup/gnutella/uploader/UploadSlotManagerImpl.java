package com.limegroup.gnutella.uploader;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.collection.MultiIterable;
import org.limewire.collection.NumericBuffer;
import org.limewire.collection.QueueCounter;
import org.limewire.core.settings.UploadSettings;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.InsufficientDataException;


/**
 * This class implements the logic of managing BT uploads and HTTP Uploads.
 * More information available see  
 * http://wiki.limewire.org/index.php?title=UploadSlotsAndBT
 */
@Singleton
public class UploadSlotManagerImpl implements UploadSlotManager {
    
    private static final Log LOG = LogFactory.getLog(UploadSlotManagerImpl.class);
    
    /**
     * The three priority levels
     */
    private static final int BT_SEED = 0; // low priority
    private static final int HTTP = 1; // medium priority
    private static final int HIGH = 2; // torrent downloads and metafiles.
    
    /** 
     * The desired minimum quality of service to provide for uploads, in
     *  B/ms
     */
    private static final float MINIMUM_UPLOAD_SPEED = 3.0f;
    
    /**
     * The list of active upload slot requests
     * INVARIANT: sorted by priority and contains only 
     * requests of the highest priority or non-preemptible requests
     */
    private final CountingList <UploadSlotRequest> active;
    
    /**
     * The list of queued non-resumable requests
     */
    private final CountingList <HTTPSlotRequest> queued;
    
    /**
     * The list of queued resumable requests
     * (currently only Seeding BT Uploaders)
     */
    private final CountingList<BTSlotRequest> queuedResumable;
    
    private final MultiIterable<UploadSlotRequest> allRequests;
    
    private final NumericBuffer<Float> bandwidth = new NumericBuffer<Float>(10);
    
    private float sessionAverage;
    private int numMeasures;
    
    @Inject
    public UploadSlotManagerImpl() {
        active = new CountingList<UploadSlotRequest>();
        queued = new CountingList<HTTPSlotRequest>();
        queuedResumable = new CountingList<BTSlotRequest>();
        allRequests = new MultiIterable<UploadSlotRequest>(active, queued, queuedResumable);
    }

    /**
     * Polls for an available upload slot. (HTTP-style)
     * 
     * @param user the user that will use the upload slot
     * @param queue if the user can enter the queue 
     * @param highPriority if the user needs an upload slot now or never 
     * @return the position in the queue if queued, -1 if rejected,
     * 0 if it can proceed immediately
     */
    public int pollForSlot(UploadSlotUser user, boolean queue, boolean highPriority) {
        if (LOG.isDebugEnabled())
            LOG.debug(user+" polling for slot, queuable "+queue);
        return requestSlot(new HTTPSlotRequest(user, queue, highPriority));
    }
    
    /**
     * Requests an upload slot. (BT-style)
     * 
     * @param listener the listener that should be notified when a slot
     * becomes available
     * @param highPriority if the user needs an upload slot now or never
     * @return the position of the upload if queued, -1 if rejected, 0 if 
     * it can proceed immediately.
     */
    public int requestSlot(UploadSlotListener listener, boolean highPriority) {
        if (LOG.isDebugEnabled())
            LOG.debug(listener+" requesting slot, high priority "+highPriority);
        return requestSlot(new BTSlotRequest(listener, highPriority));
    }

    private synchronized int requestSlot(UploadSlotRequest request) {
        // see if there exists an uploader with higher priority
        boolean existHigherPriority = existActiveHigherPriority(request.getPriority());
        
        // see if this is already in the queue
        int positionInQueue = positionInQueue(request);
        
        // see if there are any uploaders with lower priority
        int freeableSlots = getPreemptible(request.getPriority());
        
        // if there is a higher priority upload or not enough free slots, queue.
        if (existHigherPriority || 
                !hasFreeSlot(active.size() + 
                        Math.max(0,positionInQueue) - 
                        freeableSlots)) {
            
            if (!request.isQueuable()) 
                return -1;
            
            if (positionInQueue >= 0)
                return ++positionInQueue;
            else
                return queueRequest(request);
        }
        
        // free any freeable slots
        if (freeableSlots > 0)
            killPreemptible(request.getPriority());

        // remove from queue if it was there
        if (positionInQueue > -1)
            removeIfQueued(request.getUser());
        
        addActiveRequest(request);
        return 0;
    }
    
    /**
     * @return the position in the appropriate queue of the request
     *    0 if not in the queue
     */
    private int positionInQueue(UploadSlotRequest request) {
        return getQueue(request.getUser()).indexOf(request);
    }
    
    public synchronized int positionInQueue(UploadSlotUser user) {
        List<? extends UploadSlotRequest> queue = getQueue(user);
        for(int i = 0; i < queue.size();i++) {
            UploadSlotRequest request = queue.get(i);
            if (request.getUser() == user)
                return i;
        }
        return -1;
    }
    /**
     * @return the queue where requests from the user would be found.
     */
    private List<? extends UploadSlotRequest> getQueue(UploadSlotUser user) {
        return user instanceof UploadSlotListener ? queuedResumable : queued;
    }
    
    /**
     * @return if there are any active users with higher priority
     */
    private boolean existActiveHigherPriority(int priority) {
        if (priority == HIGH) 
            return false;
        
        if (!active.isEmpty()) {
            UploadSlotRequest max = active.get(0);
            if (max.getPriority() > priority)
                return true;
        }
        return false;
    }
    
    /**
     * @return the number of active uploaders with lower priority
     * that can be preempted.
     */
    private int getPreemptible(int priority) {
        if (priority == BT_SEED)
            return 0;
        
        // iterate backwards
        int ret = 0;
        for(int i = active.size() - 1; i >= 0; i--) {
            UploadSlotRequest request = active.get(i);
            if (request.getPriority() < priority && request.isPreemptible())
                ret++;
        }
        return ret;
    }
    
    /**
     * Kills any active uploaders that can be preempted and have lower priority.
     */
    private void killPreemptible(int priority) {
        for(int i = active.size() - 1; i >= 0; i--) {
            UploadSlotRequest request = active.get(i);
            if (request.getPriority() < priority && request.isPreemptible()) {
                if (LOG.isDebugEnabled())
                    LOG.debug("freeing slot from "+request.getUser());
                active.remove(i);
                request.getUser().releaseSlot();
            }
        }
    }

    /**
     * @return whether there is a free slot for an HTTP uploader.
     */
    public synchronized boolean hasHTTPSlot(int current) {
        // This ignores currently active BT_SEED uploaders since they 
        // can be preempted.
        if (existActiveHigherPriority(HTTP))
            return false;
        
        return hasFreeSlot(current);
    }
    
    /**
     * @return true if there may be a free slot for an HTTP uploader.
     */
    public synchronized boolean hasHTTPSlotForMeta(int current) {
        
        return hasFreeSlot(current);
    }
    
    /**
     * @return whether there would be a free slot if current many were taken.
     */
    private boolean hasFreeSlot(int current) {
        //Allow another upload if (a) we currently have fewer than
        //SOFT_MAX_UPLOADS uploads or (b) some upload has more than
        //MINIMUM_UPLOAD_SPEED KB/s.  But never allow more than MAX_UPLOADS.
        //
        //In other words, we continue to allow uploads until everyone's
        //bandwidth is diluted.  The assumption is that with MAX_UPLOADS
        //uploads, the probability that all just happen to have low capacity
        //(e.g., modems) is small.  This reduces "Try Again Later"'s at the
        //expensive of quality, making swarmed downloads work better.
        
        if (current >= UploadSettings.HARD_MAX_UPLOADS.getValue()) 
            return false;
        else if (current < UploadSettings.SOFT_MAX_UPLOADS.getValue()) 
            return true;
        else {
            float fastest = 0f;
            for (UploadSlotRequest request : active) {
                UploadSlotUser user = request.getUser();
                float speed = 0;
                user.measureBandwidth();
                try {
                    speed = user.getMeasuredBandwidth();
                } catch (InsufficientDataException ide) {}
                fastest = Math.max(fastest,speed);
                if (fastest > MINIMUM_UPLOAD_SPEED) 
                    return true;
                
            }
            return false;
        }
    }
    
    public synchronized void measureBandwidth() {
        float bw = getTotalBandwidth();
        sessionAverage = ((sessionAverage * numMeasures) + bw) / (++numMeasures);
        bandwidth.add(bw);
    }
    
    public synchronized float getMeasuredBandwidth() throws InsufficientDataException {
        if (bandwidth.size() < bandwidth.getCapacity())
            throw new InsufficientDataException();
        return bandwidth.average().floatValue();
    }
    
    public synchronized float getAverageBandwidth() {
        return sessionAverage;
    }
    
    private float getTotalBandwidth() {
        float ret = 0;
        for (UploadSlotRequest request : active) {
            UploadSlotUser user = request.getUser();
            user.measureBandwidth();
            try {
                ret += user.getMeasuredBandwidth();
            } catch (InsufficientDataException ide) {}
        }
        return ret;
    }
    
    /**
     * Adds a request to the appropriate queue if not already there.
     * @return the position in the queue (>= 1)
     */
    @SuppressWarnings("unchecked")
    private <Request_t extends UploadSlotRequest>int queueRequest(Request_t request) {
        List<Request_t> queue = (List<Request_t>)getQueue(request.user);
        if (queue.size() == UploadSettings.UPLOAD_QUEUE_SIZE.getValue())
            return -1;
        queue.add(request);
        
        if (LOG.isDebugEnabled())
            LOG.debug("queued "+request.getUser()+" at position "+queue.size());
        
        return queue.size();
    }
    
    /**
     * Adds an active request.  
     */
    private void addActiveRequest(UploadSlotRequest request) {
        int i = 0;
        for(; i < active.size(); i++) {
            UploadSlotRequest current = active.get(i);
            if (current.getPriority() < request.getPriority()) 
                break;
        }
        
        if (LOG.isDebugEnabled())
            LOG.debug("added active request "+request.getUser()+" at position "+i);
        
        active.add(i,request);
    }
    
    /**
     * Cancels the request issued by this UploadSlotListener.
     */
    public synchronized void cancelRequest(UploadSlotUser user) {
        if (LOG.isDebugEnabled())
            LOG.debug(user +" cancelling request");
        if (!removeIfQueued(user))
            requestDone(user);
    }

    /**
     * Removes an UploadSlotUser from the queue. 
     * @return if the user was in the queue.
     */
    private boolean removeIfQueued(UploadSlotUser user) {
        List<? extends UploadSlotRequest> queue = getQueue(user);
        for (Iterator<? extends UploadSlotRequest> iter = queue.iterator(); iter.hasNext();) {
            UploadSlotRequest request = iter.next();
            if (request.getUser() == user) {
                iter.remove();
                if (LOG.isDebugEnabled())
                    LOG.debug("remove queued request by "+user);
                return true;
            }
        }
        return false;
    }

    /**
     * Notification that the UploadSlotUser is done with its request.
     */
    public synchronized void requestDone(UploadSlotUser user) {
        for (Iterator<? extends UploadSlotRequest> iter = active.iterator(); iter.hasNext();) {
            UploadSlotRequest request = iter.next();
            if (request.getUser() == user) {
                if (LOG.isDebugEnabled())
                    LOG.debug("request finished for "+user);
                iter.remove();
                resumeQueued();
                return;
            }
        }
    }
    
    /**
     * Resumes an uploader from the resumable queue
     * (in this specific case a Seeding BT uploader).
     */
    private void resumeQueued() {
        // can't resume if someone is still active
        if (existActiveHigherPriority(BT_SEED))
            return;
        
        // consider moving this to an external collection
        for(Iterator<BTSlotRequest> iter = queuedResumable.iterator(); iter.hasNext() && hasFreeSlot(active.size());) {
            BTSlotRequest queuedRequest = iter.next();
            iter.remove();
            
            if (LOG.isDebugEnabled())
                LOG.debug("resuming queued request "+queuedRequest.getUser());
            
            active.add(queuedRequest);
            queuedRequest.getListener().slotAvailable();
        }
    }
    
    public synchronized int getNumActive() {
        return active.size();
    }
    
    public synchronized int getNumQueued() {
        return queued.size();
    }
    
    public synchronized int getNumQueuedResumable() {
        return queuedResumable.size();
    }
    
    public synchronized int getNumUsersForHost(String host) {
        int ret = 0;
        for(UploadSlotRequest request : allRequests) {
            if (host.equals(request.getUser().getHost()))
                ret++;
        }
        return ret;
    }
    
    @Override
    public synchronized String toString() {
        StringBuilder ret = new StringBuilder();
        ret.append("active:");
        appendPriorities(active, ret);
        ret.append("queued:");
        appendPriorities(queued, ret);
        ret.append("resumable:");
        appendPriorities(queuedResumable, ret);
        ret.append("bw now:").append(getTotalBandwidth());
        ret.append(" session avg:").append(sessionAverage);
        return ret.toString();
    }
    
    private void appendPriorities(List<? extends UploadSlotRequest> l, StringBuilder dest) {
        int [] priorities = countPriorities(l);
        for (int i = 0; i < priorities.length; i++)
            dest.append(i).append(":").append(priorities[i]).append(" ");
    }
    
    private int[] countPriorities(List<? extends UploadSlotRequest> l) {
        int [] ret = new int[3];
        for (UploadSlotRequest r : l) 
            ret[r.getPriority()]++;
        return ret;
    }
    
    /**
     * A request for an upload slot.
     */
    private abstract class UploadSlotRequest {
        private final UploadSlotUser user;
        private final boolean preempt;
        private final int priority;
        
        boolean isPreemptible() {
            return preempt;
        }
        
        int getPriority() {
            return priority;
        }
        
        UploadSlotUser getUser() {
            return user;
        }
        
        abstract boolean isQueuable();
        
        protected UploadSlotRequest(UploadSlotUser listener,
                boolean preempt,
                int priority) {
            this.user = listener;
            this.preempt = preempt;
            this.priority = priority;
        }
        
        @Override
        public boolean equals(Object o) {
            if (! (o instanceof UploadSlotRequest))
                return false;
            UploadSlotRequest other = (UploadSlotRequest) o;
            
            // one request per user at a time.
            return getUser() == other.getUser();
        }
        
        @Override
        public String toString() {
            return getClass().getName() + "[user=" + user + "]";
        }

    }

    /**
     * An HTTP request for an upload slot.
     */
    private class HTTPSlotRequest extends UploadSlotRequest {
        
        private final boolean queuable;
        
        HTTPSlotRequest (UploadSlotUser user, boolean queuable, boolean highPriority) {
            super(user, false, highPriority ? HIGH : HTTP);
            this.queuable = queuable;
        }
        
        @Override
        boolean isQueuable() {
            return queuable;
        }
        
    }

    /**
     * A BT request for an upload slot.
     */
    private class BTSlotRequest extends UploadSlotRequest {
        BTSlotRequest(UploadSlotListener listener, boolean highPriority) {
            super(listener, !highPriority, highPriority ? HIGH : BT_SEED);
        }
        
        UploadSlotListener getListener() {
            return (UploadSlotListener) getUser();
        }
        
        @Override
        boolean isQueuable() {
            return getPriority() == BT_SEED;
        }
    }
    
    /**
     * An ArrayList that keeps some stats on its elements. 
     */
    private static class CountingList<E> extends ArrayList<E> {
        private final QueueCounter counter = new QueueCounter(10);
        private volatile int maxSize;
        
        @Override
        public boolean add(E e) {
            counter.recordArrival();
            maxSize = Math.max(maxSize, 1 + size());
            return super.add(e);
        }
        
        @Override
        public void add(int index, E e) {
            counter.recordArrival();
            maxSize = Math.max(maxSize, 1 + size());
            super.add(index, e);
        }
        
        @Override
        public E remove(int index) {
            counter.recordDeparture();
            return super.remove(index);
        }
        
        @Override
        public boolean remove(Object e) {
            boolean ret = super.remove(e);
            if (ret) {
                counter.recordDeparture();
            }
            return ret;
        }
    }

    public synchronized void cleanup() {
        active.clear();
        queued.clear();
        queuedResumable.clear();        
    }
}
