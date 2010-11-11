package org.limewire.ui.swing.animate;

import javax.swing.JWindow;

import org.limewire.listener.EventListener;
import org.limewire.ui.swing.util.WindowTransparencyUtils;

/**
 * Fades the given component out from 100 to 0% opacity over the given time
 * period.
 */
public class FadeOutAnimator extends AbstractAnimator<JWindow> {

    public FadeOutAnimator(final JWindow window, int totalTime) {
        super(window, totalTime);
        addListener(new EventListener<AnimatorEvent<JWindow>>() {
            @Override
            public void handleEvent(AnimatorEvent event) {
                if (event.getType() == AnimatorEvent.Type.STEP) {
                    int currentStep = event.getAnimator().getCurrentStepNumber();
                    float alpha = (float) (getTotalNumberOfSteps() - currentStep)
                            / getTotalNumberOfSteps();
                    WindowTransparencyUtils.setAlpha(window, alpha);
                } else if (event.getType() == AnimatorEvent.Type.STOPPED) {
                    window.setVisible(false);
                }
            }
        });
    }
}
