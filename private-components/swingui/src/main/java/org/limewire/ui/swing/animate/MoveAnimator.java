package org.limewire.ui.swing.animate;

import java.awt.Point;

import javax.swing.JWindow;

import org.limewire.listener.EventListener;

/**
 * Moves the component from its current position to its new postion, over
 * the given amount of time.
 */
public class MoveAnimator extends AbstractAnimator<JWindow> {

    public MoveAnimator(final JWindow window, int totalTime, final Point newPoint) {
        super(window, totalTime);

        double xDifference = Math.ceil(newPoint.getX() - window.getLocation().getX());
        double yDifference = Math.ceil(newPoint.getY() - window.getY());

        final double xStep = xDifference / getTotalNumberOfSteps();
        final double yStep = yDifference / getTotalNumberOfSteps();

        addListener(new EventListener<AnimatorEvent<JWindow>>() {
            @Override
            public void handleEvent(AnimatorEvent event) {
                if (event.getType() == AnimatorEvent.Type.STEP) {
                    Point currentLocation = window.getLocation();
                    Point newLocation = new Point((int) Math.ceil(currentLocation.getX() + xStep),
                            (int) Math.ceil(currentLocation.getY() + yStep));
                    window.setLocation(newLocation);
                } else if (event.getType() == AnimatorEvent.Type.STOPPED) {
                    window.setLocation(newPoint);
                }
            }
        });
    }
}
