package org.limewire.concurrent;

/**
 * A synchronized reference of a given type that allows custom lock
 * and exposes it for more flexible synchronization.
 */
public class SyncWrapper<T> {

	private final Object lock;
	private T value;
	
	public SyncWrapper(T value, Object lock) {
		this.lock = lock;
		this.value = value;
	}
	
	public SyncWrapper(T value) {
		this.value = value;
		this.lock = this;
	}
	
	public SyncWrapper(){
		this(null);
	}

	/**
	 * @return the current value of the reference
	 */
	public T get() {
		synchronized(getLock()) {
			return value;
		}
	}
	
	/**
	 * @param newValue sets the reference to this value
	 */
	public void set(T newValue) {
		synchronized(getLock()) {
			value = newValue;
		}
	}
	
	/**
	 * @return the lock used to synchronize the setting & getting.
	 */
	public Object getLock() {
		return lock;
	}
}
