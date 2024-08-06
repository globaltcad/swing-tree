package swingtree.animation;

/**
 *  Defines either an animation progresses from 0 to 1 or regresses from 1 to 0.
 *  This is used to determine the direction of the animation, which
 *  is important for animations that end with a fade out.
 */
public enum Stride
{
    /**
     *  The animation progress defined by {@link AnimationStatus#progress()}
     *  will start at 0 and end at 1.
     */
    PROGRESSIVE,
    /**
     *  The animation progress defined by {@link AnimationStatus#progress()}
     *  will start at 1 and end at 0.
     */
    REGRESSIVE;

    /**
     *  There are only two possible values for this enum,
     *  which are {@link #PROGRESSIVE} and {@link #REGRESSIVE}.
     *  So this returns the inverse of this stride, which is either
     *  {@link #PROGRESSIVE} if this is {@link #REGRESSIVE}
     *  or {@link #REGRESSIVE} if this is {@link #PROGRESSIVE}.
     *
     * @return The inverse of this stride.
     */
    public Stride inverse() {
        return this == PROGRESSIVE ? REGRESSIVE : PROGRESSIVE;
    }

    /**
     *  Applies this stride to the supplied progress value.
     *  If this is {@link #PROGRESSIVE}, the progress is returned as is.
     *  If this is {@link #REGRESSIVE}, the progress is inverted.
     *
     * @param progress The progress value to apply this stride to.
     * @return The progress value after applying this stride.
     */
    public double applyTo( double progress ) {
        return this == PROGRESSIVE ? progress : 1 - progress;
    }
}
