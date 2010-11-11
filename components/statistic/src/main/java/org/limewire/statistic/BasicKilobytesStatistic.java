package org.limewire.statistic;

/**
 * Abstract class for recording basic data in kilobytes instead of bytes.
 * In order to preserve data accuracy, data is stored in bytes and converted to 
 * kilobytes; otherwise, data would be lost.
 * <p>
 * A sample, aka a cycle of data, is all the data collected between calls to 
 * {@link #storeCurrentStat()}. 
 * Therefore, the {@link #getMax()} is the largest sample and the 
 * {@link #getAverage()} is the total size / the number of samples.
 * <p>
 * An example of using <code>BasicKilobytesStatistic</code>:
<pre>
    class Stats extends BasicKilobytesStatistic {}
    Statistic s = new Stats();
    
    for(int i = 0; i < 1024; i++)
        s.incrementStat();
    s.storeCurrentStat();
    
    s.addData(1024 * 2);
    s.storeCurrentStat();
    
    s.addData(1024 * 3);
    s.storeCurrentStat();
    
    s.addData(1024 * 4);
    s.storeCurrentStat();
    
    s.addData(1024 * 5);
    s.storeCurrentStat();
    
    System.out.println("Total: " + s.getTotal());
    System.out.println("Max: " + s.getMax());
    System.out.println("Average: " + s.getAverage());
    
    Output:
        Total: 15.0
        Max: 5.0
        Average: 3.0

</pre>
 */
public abstract class BasicKilobytesStatistic extends AbstractKilobytesStatistic {

    public BasicKilobytesStatistic(StatisticAccumulator statisticAccumulator) {
        statisticAccumulator.addBasicStatistic(this);
	}
}
