package org.limewire.ui.swing.animate;

import javax.swing.JWindow;

import org.limewire.listener.EventListener;
import org.limewire.listener.EventListenerList;

/**
 * Fades the given component in over the given time period, pauses for a given
 * time, then fades the component out or the given time period.
 */
public class FadeInOutAnimator implements Animator<JWindow>, EventListener<AnimatorEvent<JWindow>> {

    private final EventListenerList<AnimatorEvent<JWindow>> eventListenerList;

    private final FadeInAnimator fadeInAnimator;

    private final FadeOutAnimator fadeOutAnimator;

    private final JWindow window;

    public FadeInOutAnimator(final JWindow window, int fadeIn, int pauseTime, int fadeOut) {
        this.window = window;
        eventListenerList = new EventListenerList<AnimatorEvent<JWindow>>();
        fadeInAnimator = new FadeInAnimator(window, fadeIn);
        fadeInAnimator.addListener(this);

        fadeOutAnimator = new FadeOutAnimator(window, fadeOut);
        fadeOutAnimator.setInitialDelay(pauseTime);
        fadeOutAnimator.addListener(this);
    }

    @Override
    public int getTotalNumberOfSteps() {
        return 0;
    }

    @Override
    public void setInitialDelay(int initialDelay) {
        throw new UnsupportedOperationException();

    }

    @Override
    public void start() {
        fadeInAnimator.start();
        eventListenerList.broadcast(new AnimatorEvent<JWindow>(this, AnimatorEvent.Type.STARTED));
    }

    @Override
    public void stop() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addListener(EventListener<AnimatorEvent<JWindow>> listener) {
        eventListenerList.addListener(listener);
    }

    @Override
    public boolean removeListener(EventListener<AnimatorEvent<JWindow>> listener) {
        return eventListenerList.removeListener(listener);
    }

    @Override
    public void handleEvent(AnimatorEvent<JWindow> event) {
        if (event.getType() == AnimatorEvent.Type.STOPPED && event.getAnimator() == fadeInAnimator) {
            fadeOutAnimator.start();
        } else if (event.getType() == AnimatorEvent.Type.STOPPED
                && event.getAnimator() == fadeOutAnimator) {
            eventListenerList
                    .broadcast(new AnimatorEvent<JWindow>(this, AnimatorEvent.Type.STOPPED));
        }
    }

    @Override
    public JWindow getComponent() {
        return window;
    }

    @Override
    public int getCurrentStepNumber() {
        return 0;
    }

}
