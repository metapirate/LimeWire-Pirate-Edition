package org.limewire.collection;

/**
 * Provides a circular buffer for {@link Number Numbers}. Includes a summation
 * and average (arithmetic mean) of the elements in the buffer.
<pre>
    NumericBuffer&lt;Float&gt; nb = new NumericBuffer&lt;Float&gt;(10);
    
    nb.add(1f);
    nb.add(2f);
    nb.add(3f);
    nb.add(4f);
    System.out.println(nb);
    System.out.println("Arithmetic mean (average) is: " + nb.average());
    System.out.println("Sum is: " + nb.sum());  

    Output:
        [4.0, 3.0, 2.0, 1.0]
        Arithmetic mean (average) is: 2.5
        Sum is: 10.0
</pre>
 * 
 */
public class NumericBuffer<T extends Number> extends Buffer<T> {

	public NumericBuffer(int size) {
		super(size);
	}

	public NumericBuffer(Buffer<? extends T> other) {
		super(other);
	}
	
	@Override
    @SuppressWarnings("unchecked")
	protected T[] createArray(int size) {
		return (T[])new Number[size + 1];
	}
	
	/**
	 * @return the average (arithmetic mean) of the elements in this buffer 
     * with the best accuracy possible - double if the elements are float or 
     * double and long otherwise.  
	 */
	public Number average() {
		if (isEmpty())
			return 0;
		Number sum = sum();
		if (sum instanceof Double)
			return sum().doubleValue() / size();
		else
			return sum().longValue() / size();
	}
	
	/**
	 * @return the sum of the elements in this buffer with the
	 * best accuracy possible - double if the elements are float or double
	 * and long otherwise.  
	 */
	public Number sum() {
		if (isEmpty())
			return 0;
		Number n = first();
		if (n instanceof Float || n instanceof Double)
			return doubleSum();
		else
			return longSum();
	}
	
	private double doubleSum() {
		double ret = 0;
		for (Number n : buf) {
			if (n != null) 
				ret += n.doubleValue();
		}
		return ret;
	}
	
	private long longSum() {
		long ret = 0;
		for (Number n : buf) {
			if (n != null)
				ret += n.longValue();
		}
		return ret;
	}
}
