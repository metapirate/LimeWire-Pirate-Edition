package org.limewire.statistic;

/**
 * Abstract class for recording data in kilobytes instead of bytes. In order to 
 * preserve data accuracy, data is stored in bytes and converted to kilobytes; 
 * otherwise, data would be lost.
 * <p>
 * A sample, aka a cycle of data, is all the data collected between calls to 
 * {@link #storeCurrentStat()}. 
 * Therefore, the {@link #getMax()} is the largest sample and the 
 * {@link #getAverage()} is the total size / the number of samples.
 * <p>
 * An example of using <code>AbstractKilobytesStatistic</code>:
 <pre>
    class Stats extends AbstractKilobytesStatistic {}
    Statistic s = new Stats();
            
    for(int i = 0; i < 1024; i++)
        s.incrementStat();
    s.storeCurrentStat();
    
    s.addData(2 * 1024);
    s.storeCurrentStat();
    
    s.addData(3 * 1024);
    s.storeCurrentStat();
    
    s.addData(4 * 1024);
    s.storeCurrentStat();

    s.addData(5 * 1024);
    s.storeCurrentStat();
            
    System.out.println("Total: " + s.getTotal());
    System.out.println("Max: " + s.getMax());
    System.out.println("Average: " + s.getAverage());
    
    Output:
        Total: 15.0
        Max: 5.0
        Average: 3.0
 </pre> 
 * 
 * 
 */
public abstract class AbstractKilobytesStatistic extends AbstractStatistic {

	/**
	 * Bytes per kilobyte for conversion convenience.
	 */
	private static final int BYTES_PER_KILOBYTE = 1024;

	/** 
	 * Overridden to report the average for this statistic in kilobyes.
	 *
	 * @return the average for this statistic in kilobytes per unit of
	 *  measurement (KB/s)
	 */
	@Override
    public double getAverage() {
        if (_totalStatsRecorded == 0)
            return 0;
        return (_total / _totalStatsRecorded) / BYTES_PER_KILOBYTE;
    }

	/** 
	 * Overridden to report the maximum for this statistic in kilobyes.
	 *
	 * @return the maximum for a recorded time period for this statistic 
	 *  in kilobytes 
	 */
	@Override
	public double getMax() {
		return _max/BYTES_PER_KILOBYTE;
	}

	/** 
	 * Overridden to report the total for this statistic in kilobytes.
	 *
	 * @return the total for this statistic in kilobytes 
	 */
	@Override
	public double getTotal() {
		return _total/BYTES_PER_KILOBYTE;
	}
}
