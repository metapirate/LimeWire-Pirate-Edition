package org.limewire.core.impl.mozilla;

import com.limegroup.gnutella.BandwidthTracker;
import com.limegroup.gnutella.InsufficientDataException;

/**
 * A simple implementation of the BandwidthTracker interface.
 */
class SimpleBandwidthTracker implements BandwidthTracker {
	private static final int DEFAULT_INTERVAL = 500;

	private final int _interval;

	private long _lastAmount = 0;

	private long _lastTimeMeasured = 0;

	private long _firstTimeMeasured = 0;

	private long _amount = 0;

	private float _measuredBandwidth = -1f;

	public SimpleBandwidthTracker() {
		this(DEFAULT_INTERVAL);
	}

	public SimpleBandwidthTracker(int interval) {
		_interval = interval;
	}

	/**
	 * Adds the given amount to the tracked bandwidth usage since the last time 
	 * measureBandwidth was called.
	 * @param added non negative value to count twoards bandwidth
	 * @throws IllegalArgumentException if value supplied is < 0
	 */
	public synchronized void count(long added) {
		if( added < 0) {
		    throw new IllegalArgumentException("Count values must be positive: " + added);
		}
	    startCounting(-1);
		_amount += added;
	}

	private void startCounting(long now) {
		if (_firstTimeMeasured == 0) {
			if (now == -1)
				now = System.currentTimeMillis();
			_firstTimeMeasured = now;
			_lastTimeMeasured = _firstTimeMeasured;
		}
	}
	
	public synchronized long getTotalAmount() {
		return _amount;
	}

	public synchronized void measureBandwidth() {
		long now = System.currentTimeMillis();
		startCounting(now);
		if (now - _lastTimeMeasured < _interval)
			return;

		_measuredBandwidth = (_amount - _lastAmount)
				/ (now - _lastTimeMeasured) ;
		_lastAmount = _amount;
		_lastTimeMeasured = now;
	}

	public synchronized float getMeasuredBandwidth() throws InsufficientDataException {
		if (_measuredBandwidth < 0)
			throw new InsufficientDataException();
		return _measuredBandwidth;
	}

	public synchronized float getAverageBandwidth() {
		long time = System.currentTimeMillis() - _firstTimeMeasured; 
		return time <= 0 ? 0 :_amount / time ;
	}
}
