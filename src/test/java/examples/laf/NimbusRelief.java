package examples.laf;

import swingtree.UI;
import swingtree.style.GradientConf;

import java.awt.Color;
import java.awt.LinearGradientPaint;
import java.awt.MultipleGradientPaint;
import java.awt.Paint;
import java.awt.geom.Point2D;

/**
 *  The light {@link NimbusPreset} is built out of, in the one place both the style rules and the
 *  {@link Symbols.Nimbus} read it from.
 *  <p>
 *  A relief is a set of gradient stops measured against the surface's own colour rather than
 *  written down: a stop says "this far down, this much brighter and this much less saturated than
 *  the colour handed in". One relief therefore describes a button, a tab, a table heading and a
 *  check box, and describes them in whatever palette is installed - which is the property the
 *  original look and feel has and a theme made of literal colours does not.
 *  <p>
 *  The offsets are the ones Nimbus paints with, read off its output rather than copied from it.
 *  They are given to three decimal places because that is where they stop changing any byte of
 *  the result.
 *
 *  @see LafUtilities#shiftHsb(Color, double, double)
 */
// The stop tables are arrays because that is the shape the gradient APIs on both sides take. They
// are written once here and never handed out: every method below builds a fresh array to return.
@SuppressWarnings("ImmutableEnumChecker")
enum NimbusRelief
{
    /**
     *  A moulded surface under an overhead light: a bright top edge falling away quickly, a long
     *  dim stretch through the middle, and the bottom lip catching the light again. Buttons, combo
     *  boxes, tabs, table headings, check boxes, scroll thumbs - everything that is meant to look
     *  liftable.
     */
    LIT( new double[]  {  0.00,   0.08,   0.62,  0.70,  1.00 },
         new double[][]{ {-0.036, 0.115}, {-0.028, 0.078}, {0, 0}, {0, 0}, {-0.009, 0.120} } ),

    /**
     *  The same surface with the light off: the curve is still there, at a fifth of the depth.
     *  That is what tells a disabled control from a flat one, and it is why disabling something
     *  here does not simply grey it - a control that cannot be used is one nothing is shining on.
     */
    UNLIT( new double[]  {  0.00,   0.11,   0.62,  0.70,  1.00 },
           new double[][]{ {-0.015, 0.039}, {-0.010, 0.024}, {0, 0}, {0, 0}, {-0.001, 0.012} } ),

    /**
     *  The same moulding, cast in a colour that has some saturation in it.
     *  <p>
     *  There have to be two, because the top of a lit moulding is close to white whatever the body
     *  is made of: a body that is already nearly grey is washed out by a small change, and a body
     *  carrying real colour needs a much larger one to reach the same highlight. So the neutral
     *  chrome and the accented material - the default button, a ticked box, a selected tab, the
     *  actuator on a combo box - are lit by the same lamp described twice.
     */
    LIT_ACCENTED( new double[]  {  0.00,   0.08,   0.62,   0.70,  1.00 },
                  new double[][]{ {-0.179, 0.184}, {-0.107, 0.129}, {-0.004, 0.004}, {0, 0}, {-0.025, 0.122} } ),

    /**
     *  A strip rather than a moulding: lit at the top and shading evenly to its own colour at the
     *  bottom, with no lip to catch the light. Menu bars, tool bars and anything else that runs
     *  the width of the window and is not meant to look liftable.
     */
    STRIP( new double[]  {  0.00,   0.23,   1.00 },
           new double[][]{ {-0.036, 0.098}, {-0.040, 0.114}, {0, 0} } ),

    /**
     *  A groove cut into the panel rather than a moulding standing on it, so the light runs the
     *  other way: the top wall of the cut is in shadow and the floor at the bottom catches what
     *  gets in. Laid over the border colour rather than over a surface, since a groove is a line
     *  before it is a shape.
     */
    CUT( new double[]  {  0.00,  0.80,   1.00 },
         new double[][]{ {0, 0}, {-0.019, 0.161}, {-0.077, 0.314} } ),

    /**
     *  A saturated bar under a hard sheen: the top washes almost to white while keeping its hue,
     *  the middle stays the colour itself, and the bottom lip flares. This is the one relief that
     *  changes the saturation more than the brightness, which is what makes a progress bar read as
     *  wet plastic next to the dry moulding around it.
     */
    GLOSS( new double[]  {  0.00,   0.08,   0.46,  0.62,   1.00 },
           new double[][]{ {-0.759, 0.176}, {-0.558, 0.129}, {-0.240, 0.051}, {-0.025, 0.024}, {-0.227, 0.200} } );

    private final double[]   _fractions;
    private final double[][] _offsets;

    NimbusRelief( double[] fractions, double[][] offsets ) {
        _fractions = fractions;
        _offsets   = offsets;
    }

    /**
     *  Lays this relief over one colour, as a style rule's gradient.
     *
     * @param g the gradient being configured
     * @param tone the colour the surface would be with no light on it, which the middle of the
     *             relief reproduces exactly
     * @return the configured gradient, running down the component and clipped to its body
     */
    GradientConf over( GradientConf g, Color tone ) {
        return g.colors(stops(tone))
                .fractions(_fractions.clone())
                .span(UI.Span.TOP_TO_BOTTOM)
                .clipTo(UI.ComponentArea.BODY);
    }

    /**
     *  The same relief as a paint, for the pieces of geometry no style rule can express. A symbol
     *  is handed a scratch graphics context and a rectangle, so it needs the gradient as something
     *  it can fill a shape with rather than as a style.
     *
     * @param y the top of the span the relief runs over, in component pixels
     * @param height how tall that span is; a span of no height is painted in the middle tone,
     *               since a gradient needs two distinct ends
     * @param tone the colour the middle of the relief reproduces
     * @return a paint for that span
     */
    Paint paint( float y, float height, Color tone ) {
        Color[] stops = stops(tone);
        if ( height < 1 )
            return stops[stops.length / 2];
        float[] fractions = new float[_fractions.length];
        for ( int i = 0; i < fractions.length; i++ )
            fractions[i] = (float) _fractions[i];
        return new LinearGradientPaint(
                    new Point2D.Float(0, y), new Point2D.Float(0, y + height),
                    fractions, stops,
                    MultipleGradientPaint.CycleMethod.NO_CYCLE
                );
    }

    private Color[] stops( Color tone ) {
        Color[] stops = new Color[_offsets.length];
        for ( int i = 0; i < stops.length; i++ )
            stops[i] = LafUtilities.shiftHsb(tone, _offsets[i][0], _offsets[i][1]);
        return stops;
    }
}
