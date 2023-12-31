package swingtree.animation;

/**
 *  Defines either an animation progresses from 0 to 1 or regresses from 1 to 0.
 *  This is used to determine the direction of the animation, which
 *  is important for animations that end with a fade out.
 */
public enum Stride
{
    /**
     *  The animation progress defined by {@link AnimationState#progress()}
     *  will start at 0 and end at 1.
     */
    PROGRESSIVE,
    /**
     *  The animation progress defined by {@link AnimationState#progress()}
     *  will start at 1 and end at 0.
     */
    REGRESSIVE
}
