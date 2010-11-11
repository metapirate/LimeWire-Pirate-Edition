package org.limewire.ui.swing.animate;

import org.limewire.listener.EventListener;
import org.limewire.listener.ListenerSupport;

/**
 * Interface for the general concept of animating some gui changes.
 */
interface Animator<T> extends ListenerSupport<AnimatorEvent<T>> {

    /**
     * The delay before starting the event after start gets called.
     */
    public abstract void setInitialDelay(int initialDelay);

    /**
     * Starts animating the event until finished.
     */
    public abstract void start();

    /**
     * Prematurely stops animating the gui event.
     */
    public abstract void stop();

    /**
     * Add a listener to deal with animating the event.
     */
    public abstract void addListener(EventListener<AnimatorEvent<T>> listener);

    /**
     * Removes the listener.
     */
    public abstract boolean removeListener(EventListener<AnimatorEvent<T>> listener);

    /**
     * Returns the total number of steps this animator will run before
     * finishing.
     */
    public abstract int getTotalNumberOfSteps();

    /**
     * Returns the step number that is currently being processed.
     */
    public int getCurrentStepNumber();

    /**
     * Returns the component that is currently being animated.
     */
    public T getComponent();

}
