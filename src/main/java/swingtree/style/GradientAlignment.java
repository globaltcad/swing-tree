package swingtree.style;

import java.util.function.Function;

/**
 *  Use these enum instances to specify the gradient alignment for various sub styles,
 *  like for example {@link BackgroundStyle} or {@link BorderStyle} through the
 *  {@link GradientStyle#align(GradientAlignment)} method exposed by methods like
 *  {@link StyleDelegate#gradient(String, Function)} or {@link StyleDelegate#borderGradient(String, Function)}.
 */
public enum GradientAlignment
{
    TOP_LEFT_TO_BOTTOM_RIGHT, BOTTOM_LEFT_TO_TOP_RIGHT,
    TOP_RIGHT_TO_BOTTOM_LEFT, BOTTOM_RIGHT_TO_TOP_LEFT,

    TOP_TO_BOTTOM, LEFT_TO_RIGHT,
    BOTTOM_TO_TOP, RIGHT_TO_LEFT;

    /**
     * @return {@code true} if this alignment is diagonal, {@code false} otherwise.
     */
    public boolean isDiagonal() {
        return this == TOP_LEFT_TO_BOTTOM_RIGHT || this == BOTTOM_LEFT_TO_TOP_RIGHT ||
               this == TOP_RIGHT_TO_BOTTOM_LEFT || this == BOTTOM_RIGHT_TO_TOP_LEFT;
    }
}
