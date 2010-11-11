package org.limewire.rudp;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
/** 
 *  Calculates and controls the timing for data writing.
 */
public class WriteRegulator {

    private static final Log LOG =
      LogFactory.getLog(WriteRegulator.class);

    /** Don't adjust the skipping of sleeps until the window has initialized. */
    private static final int   MIN_START_WINDOW     = 40;

    /** When the window space hits this size, it is low. */
    private static final int   LOW_WINDOW_SPACE     = 4;

    /** Cap the quick sending of blocks at this number. */
    private static final int   MAX_SKIP_LIMIT       = 14;

    /** The low failure rate at optimal throughput. */
    private static final float LOW_FAILURE_RATE     = 3f / 100f;

    /** The high failure rate at optimal throughput. */
    private static final float HIGH_FAILURE_RATE    = 4f / 100f;


    private DataWindow _sendWindow;
    private int        _skipCount  = 0;
    private int        _skipLimit  = 2;
    private boolean    _limitHit   = false;
    private int        _limitCount = 0;
    private int        _limitReset = 200;  
    private int        _zeroCount  = 0;


    /** Keep track of how many successes/failures there are in 
        writing messages. */
    private FailureTracker _tracker;
        

    public WriteRegulator( DataWindow sendWindow ) {
        _sendWindow = sendWindow;
        _tracker    = new FailureTracker();
    }

    /** 
     *  When a resend is required and the failure rate is too high, 
     *  scale down activity.
     */
    public void hitResendTimeout() {
        if ( (!_limitHit || _limitCount >= 10) &&
              _tracker.failureRate() > HIGH_FAILURE_RATE ) {
            _limitHit = true;
            _skipLimit /= 2;
            _limitCount = 0;
            if(LOG.isDebugEnabled())  
                LOG.debug("hitResendTimeout _skipLimit = "+_skipLimit+
                " fR="+_tracker.failureRateAsString());
            _tracker.clearOldFailures();
        }
    }

    /** 
     *  When the send window keeps getting hit, slow down activity.
     */
    public void hitZeroWindow() {
        _zeroCount++;
        if ( (!_limitHit || _limitCount >= 10) && _zeroCount > 4) { 
            // Doing nothing for now since this is irrelevant to the skipping
            //

            //_limitHit = true;
            //_skipLimit /= 2;
            //_limitCount = 0;
            _zeroCount = 0;
            if(LOG.isDebugEnabled())  
                LOG.debug("hitZeroWindow _skipLimit = "+_skipLimit+
                  " fR="+_tracker.failureRateAsString());
        }
    }

    /** 
     *  Compute how long the sleep time should be before the next write.
     */
    public long getSleepTime(long currTime, int receiverWindowSpace) {

        //------------- Sleep ------------------------
        //rtt = Round Trip Time
        // Sleep a fraction of rtt for specified window increment
        int  usedSpots   = _sendWindow.getUsedSpots(); 
        int  windowSize  = _sendWindow.getWindowSize(); 
        long windowStart = _sendWindow.getWindowStart(); 

        int   rto        = _sendWindow.getRTO();
        float rttvar     = _sendWindow.getRTTVar();
        float srtt       = _sendWindow.getSRTT();
        int   isrtt      = (int) srtt;

        int  rtt;
        int  realRTT     = isrtt;//_sendWindow.averageRoundTripTime();
        int  lowRTT      = _sendWindow.lowRoundTripTime();
        int  smoothRTT   = isrtt;//_sendWindow.smoothRoundTripTime();
        int  sentWait    = isrtt;//_sendWindow.calculateWaitTime( currTime, 3);
        rtt = sentWait + 1;
        if  (rtt == 0) 
            rtt = 10;
        int baseWait   = Math.min(realRTT, 2000)/4;  
        //
        // Want to ideally achieve a steady state location in writing and 
        // reading window.  Don't want to get too far ahead or too far behind
        //
        int sleepTime    = ((usedSpots+1) * baseWait);
        int minTime      = 0;
        int gettingSlow  = 0;


        // Ensure the sleep time is fairly distributed in the normal case
        if ( sleepTime < windowSize ) {
            double pct = (double) sleepTime / (double) windowSize;
            if ( Math.random() < pct )
                sleepTime      = 1;
            else
                sleepTime      = 0;
        } else {
            sleepTime      = sleepTime / windowSize;
        }

        // Create a sleeptime specific to having almost no room left to send
        // more data
        if ( receiverWindowSpace <= LOW_WINDOW_SPACE ) {
            // Scale up the sleep time to a full timeout as you approach 
            // zero space for writing
            int multiple = LOW_WINDOW_SPACE / Math.max(1, receiverWindowSpace);
            sleepTime = (((int)srtt) * multiple) / (LOW_WINDOW_SPACE + 1);

            if ( receiverWindowSpace <= (LOW_WINDOW_SPACE/2) ) {
                sleepTime = rto;
                if(LOG.isDebugEnabled())  
                    LOG.debug("LOW_WINDOW sT:"+sleepTime);
            }
            minTime = sleepTime;
        }

        if(LOG.isDebugEnabled())  
            LOG.debug(
              "sleepTime:"+sleepTime+
              " uS:"+usedSpots+ 
              " RWS:"+receiverWindowSpace+
              " smoothRTT:"+smoothRTT+
              " realRTT:"+realRTT+
              " rtt:"+rtt+
              " RTO:"+rto+
              " RTTVar:"+rttvar+
              " srtt:"+srtt+
              " sL:"+_skipLimit +
              " fR="+_tracker.failureRateAsString());

        if ( _skipLimit < 1 )
            _skipLimit = 1;

        // Reset Timing if you are going to wait less than rtt or
        // RTT has elevated too much

        // Compute a max target RTT given the bandwidth capacity
        int maxRTT;
        if ( smoothRTT > ((5*lowRTT)/2) ) {  // If avg much greater than low
            // Capacity is limited so kick in quickly
            maxRTT      = ((lowRTT*7) / 5);  
        } else {
            // Capacity doesn't seem to be limited so only kick in if extreme
            maxRTT      = ((lowRTT*25) / 5);
        }

        // We want at least 2 round trips per full window time
        // so find out how much you would wait for half a window
        int windowDelay = 
          (((baseWait * windowSize) / _skipLimit) * 2) / 4;

        // If our RTT time is going up, figure out what to do
        if ( rtt != 0 && baseWait != 0 && 
             receiverWindowSpace <= LOW_WINDOW_SPACE &&
             (windowDelay < rtt || rtt > maxRTT) ) {
            if(LOG.isDebugEnabled())  
                LOG.debug(
                  " -- MAX EXCEED "+
                  " RTT sL:"+_skipLimit + " w:"+ windowStart+
                  " Rrtt:"+realRTT+ " base :"+baseWait+
                  " uS:"+usedSpots+" RWS:"+receiverWindowSpace+
                  " lRTT:"+_sendWindow.lowRoundTripTime()+
                  " sWait:"+sentWait+
                  " mRTT:"+maxRTT+
                  " wDelay:"+windowDelay+
                  " sT:"+sleepTime);


            // If we are starting to affect the RTT, 
            // then ratchet down the accelerator
            /*
            if ( realRTT > ((3*lowRTT)) || rtt > (3*lowRTT) ) {
                _limitHit = true;
                _skipLimit /= 2;
                if(LOG.isDebugEnabled())  
                    LOG.debug(
                      " -- LOWER SL "+
                      " rRTT:"+realRTT+
                      " lRTT:"+lowRTT+
                      " rtt:"+rtt+ 
                      " sL:"+_skipLimit);
            }
            */

            // If we are majorly affecting the RTT, then slow down right now
            if ( rtt > maxRTT || realRTT > maxRTT ) {
                minTime = lowRTT / 4;
                if ( gettingSlow == 0 )
                    _skipLimit--;
                gettingSlow = 50;
                //sleepTime = (16*rtt) / 7;
                if(LOG.isDebugEnabled())  
                    LOG.debug(
                      " -- UP SLEEP "+ 
                      " rtt:"+rtt+ 
                      " mRTT:"+maxRTT+
                      " rRTT:"+realRTT+
                      " lRTT:"+lowRTT+
                      " sT:"+sleepTime);
            }
        }

        // Cycle through the accelerator states and enforced backoff
        if ( _skipLimit < 1 )
            _skipLimit = 1;
        _skipCount = (_skipCount + 1) % _skipLimit;

        if ( !_limitHit ) {
            // Bump up the skipLimit occasionally to see if we can handle it
            if (_skipLimit < MAX_SKIP_LIMIT    &&
                windowStart%windowSize == 0    &&
                gettingSlow == 0               &&
                windowStart > MIN_START_WINDOW &&
                _tracker.failureRate() < LOW_FAILURE_RATE ) {
                if(LOG.isDebugEnabled())  
                    LOG.debug("up _skipLimit = "+_skipLimit);
                _skipLimit++;
                if(LOG.isDebugEnabled())  
                    LOG.debug(" -- UPP sL:"+_skipLimit);
            }
        } else {
            // Wait before trying to be aggressive again
            _limitCount++;
            if (_limitCount >= _limitReset) {
                if(LOG.isDebugEnabled())  
                    LOG.debug(" -- UPP reset:"+_skipLimit);
                _limitCount = 0;
                _limitHit = false;
            }
        }

        // Readjust the sleepTime to zero if the connection can handle it
        if ( _skipCount != 0 && 
             rtt < maxRTT && 
             receiverWindowSpace > LOW_WINDOW_SPACE )  {
             if(LOG.isDebugEnabled())  
                 LOG.debug("_skipLimit = "+_skipLimit);
            sleepTime = 0;
        }

        // Ensure that any minimum sleep time is enforced
        sleepTime = Math.max(sleepTime, minTime);

        // Reduce the gettingSlow indicator over time
        if ( gettingSlow > 0 )
            gettingSlow--;

        return sleepTime;
        //------------- Sleep ------------------------
    }


    /** 
     * Record a message success.
     */
    public void addMessageSuccess() {
        _tracker.addSuccess();
    }

    /** 
     * Record a message failure.
     */
    public void addMessageFailure() {
        _tracker.addFailure();
    }


    /**
     *  Keep track of overall successes and failures. 
     */
    private static class FailureTracker {

        private static final int HISTORY_SIZE=100;
        
        private final byte [] _data = new byte[HISTORY_SIZE];
        
        private boolean _rollover =false;
        private int _index;


        /**
         * Add one to the successful count.
         */
        public void addSuccess() {

            _data[_index++]=1;
            if (_index>=HISTORY_SIZE-1){
                LOG.debug("rolled over");
                _index=0;
                _rollover=true;
            }
        }

        /**
         * Add one to the failure count.
         */
        public void addFailure() {
            _data[_index++]=0;
            if (_index>=HISTORY_SIZE-1){
                LOG.debug("rolled over");
                _index=0;
                _rollover=true;
            }
        }

        /**
         * Clear out old failures to give new rate a chance. This should clear
         * out a clump of failures more quickly.
         */
        public void clearOldFailures() {
            for (int i = 0; i < HISTORY_SIZE/2; i++)
                addSuccess();
        }

        /**
         * Compute the failure rate of last HISTORY_SIZE blocks once up and running.
         */
        public float failureRate() {

            int total=0;
            for (int i=0;i < (_rollover ? HISTORY_SIZE : _index);i++)
                total+=_data[i];
            
            if (LOG.isDebugEnabled()) {
                LOG.debug("failure rate from "+_index+ 
                        " measurements and rollover "+_rollover+
                        " total is "+total+
                        " and rate "+ 
                        (1- (float)total / (float)(_rollover ? HISTORY_SIZE : _index)));
            }
            
            return 1- ((float)total / (float)(_rollover ? HISTORY_SIZE : _index));
        }

        
        /**
         * Report the failure rate as string for debugging.
         */
        public String failureRateAsString() {

           float rate  = failureRate() * 1000; 
           int   irate = ((int)rate) / 10 ;
           int   drate = (((int)rate) - (irate * 10));
           return "" + irate + "." + drate;
        }
        
    }
}
