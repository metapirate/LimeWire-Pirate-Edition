package org.limewire.ui.swing.animate;

import javax.swing.JWindow;

import org.limewire.listener.EventListener;
import org.limewire.ui.swing.util.WindowTransparencyUtils;

/**
 * Fades the given component in from 0 to 100% opacity over the given time
 * period.
 */
public class FadeInAnimator extends AbstractAnimator<JWindow> {

    public FadeInAnimator(final JWindow window, int totalTime) {
        super(window, totalTime);
        addListener(new EventListener<AnimatorEvent<JWindow>>() {
            @Override
            public void handleEvent(AnimatorEvent event) {
                if (event.getType() == AnimatorEvent.Type.STARTED) {
                    WindowTransparencyUtils.setAlpha(window, 0);
                    window.setVisible(true);
                } else if (event.getType() == AnimatorEvent.Type.STEP) {
                    int currentStep = event.getAnimator().getCurrentStepNumber();
                    float alpha = (float) currentStep / getTotalNumberOfSteps();
                    WindowTransparencyUtils.setAlpha(window, alpha);
                } else if (event.getType() == AnimatorEvent.Type.STOPPED) {
                    WindowTransparencyUtils.setAlpha(window, 1);
                }
            }
        });
    }
}
