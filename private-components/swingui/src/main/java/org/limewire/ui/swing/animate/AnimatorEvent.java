package org.limewire.ui.swing.animate;

/**
 * Represents an animation starting, stopping, or currently stepping.
 */
public class AnimatorEvent<T> {
    public enum Type {
        STARTED, STEP, STOPPED
    }

    private final Animator<T> animator;

    private final AnimatorEvent.Type type;

    public AnimatorEvent(Animator<T> animator, Type type) {
        this.animator = animator;
        this.type = type;
    }

    public AnimatorEvent.Type getType() {
        return type;
    }

    public Animator<T> getAnimator() {
        return animator;
    }
}
