package org.limewire.ui.swing.animate;

import java.awt.Point;

import org.jdesktop.animation.timing.Animator;
import org.jdesktop.animation.timing.interpolation.PropertySetter;
import org.jdesktop.animation.transitions.Effect;
import org.jdesktop.animation.transitions.effects.CompositeEffect;
import org.jdesktop.animation.transitions.effects.FadeIn;
import org.jdesktop.animation.transitions.effects.FadeOut;

/**
 * Utility methods for animated transition effects.
 * 
 * <p>The Effect interface is part of the Animated Transitions library.</p>
 */
public class EffectsUtils {

    /**
     * Creates an effect where the component moves in from the specified start
     * position, and either grows or fades into view.
     */
    public static Effect createMoveInEffect(int startX, int startY, boolean grow) {
        CompositeEffect effect = new CompositeEffect();
        effect.addEffect(new MoveIn(startX, startY));
        if (grow) {
            effect.addEffect(new Grow());
        } else {
            effect.addEffect(new FadeIn());
        }
        return effect;
    }
    
    /**
     * Creates an effect where the component moves out to the specified end
     * position, and either shrinks or fades out of view.
     */
    public static Effect createMoveOutEffect(int endX, int endY, boolean shrink) {
        CompositeEffect effect = new CompositeEffect();
        effect.addEffect(new MoveOut(endX, endY));
        if (shrink) {
            effect.addEffect(new Shrink());
        } else {
            effect.addEffect(new FadeOut());
        }
        return effect;
    }
    
    /**
     * A custom Effect to move a component from a specified starting point
     * into its end location.
     */
    public static class MoveIn extends Effect {
        private final Point startLocation = new Point();
        private PropertySetter ps;
        
        public MoveIn(int x, int y) {
            startLocation.x = x;
            startLocation.y = y;
        }

        /**
         * Initializes animation to vary the location during the transition.
         */
        @Override
        public void init(Animator animator, Effect parentEffect) {
            Effect targetEffect = (parentEffect == null) ? this : parentEffect;
            ps = new PropertySetter(targetEffect, "location", 
                    startLocation, new Point(getEnd().getX(), getEnd().getY()));
            animator.addTarget(ps);
            super.init(animator, parentEffect);
        }
        
        @Override
        public void cleanup(Animator animator) {
            animator.removeTarget(ps);
        }
    }
    
    /**
     * A custom Effect to move a component from its start location out to a
     * specified end point.
     */
    public static class MoveOut extends Effect {
        private final Point endLocation = new Point();
        private PropertySetter ps;
        
        public MoveOut(int x, int y) {
            endLocation.x = x;
            endLocation.y = y;
        }

        /**
         * Initializes animation to vary the location during the transition.
         */
        @Override
        public void init(Animator animator, Effect parentEffect) {
            Effect targetEffect = (parentEffect == null) ? this : parentEffect;
            ps = new PropertySetter(targetEffect, "location", 
                    new Point(getStart().getX(), getStart().getY()), endLocation);
            animator.addTarget(ps);
            super.init(animator, parentEffect);
        }
        
        @Override
        public void cleanup(Animator animator) {
            animator.removeTarget(ps);
        }
    }
    
    /**
     * A custom Effect to expand a component.
     */
    public static class Grow extends Effect {
        private PropertySetter psWidth;
        private PropertySetter psHeight;

        /**
         * Initializes animation to vary the size during the transition.
         */
        @Override
        public void init(Animator animator, Effect parentEffect) {
            Effect targetEffect = (parentEffect == null) ? this : parentEffect;
            psWidth = new PropertySetter(targetEffect, "width", 0, getEnd().getWidth());
            animator.addTarget(psWidth);
            psHeight = new PropertySetter(targetEffect, "height", 0, getEnd().getHeight());
            animator.addTarget(psHeight);
            super.init(animator, parentEffect);
        }
        
        @Override
        public void cleanup(Animator animator) {
            animator.removeTarget(psWidth);
            animator.removeTarget(psHeight);
        }
    }
    
    /**
     * A custom Effect to shrink a component.
     */
    public static class Shrink extends Effect {
        private PropertySetter psWidth;
        private PropertySetter psHeight;

        /**
         * Initializes animation to vary the size during the transition.
         */
        @Override
        public void init(Animator animator, Effect parentEffect) {
            Effect targetEffect = (parentEffect == null) ? this : parentEffect;
            psWidth = new PropertySetter(targetEffect, "width", getStart().getWidth(), 0);
            animator.addTarget(psWidth);
            psHeight = new PropertySetter(targetEffect, "height", getStart().getHeight(), 0);
            animator.addTarget(psHeight);
            super.init(animator, parentEffect);
        }
        
        @Override
        public void cleanup(Animator animator) {
            animator.removeTarget(psWidth);
            animator.removeTarget(psHeight);
        }
    }
}
