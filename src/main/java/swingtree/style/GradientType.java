package swingtree.style;

import java.util.function.Function;

/**
 *  Use these enum instances to specify the gradient type for various sub styles,
 *  like for example {@link BackgroundStyle} or {@link BorderStyle} through the
 *  {@link GradientStyle#type(GradientType)} method exposed by methods like
 *  {@link StyleDelegate#gradient(String, Function)} or {@link StyleDelegate#borderGradient(String, Function)}.
 */
public enum GradientType
{
    LINEAR, RADIAL
}
