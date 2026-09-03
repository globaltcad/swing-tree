package examples.laf;

import swingtree.UI;
import swingtree.style.GradientConf;

import java.awt.Color;
import java.awt.LinearGradientPaint;
import java.awt.MultipleGradientPaint;
import java.awt.Paint;
import java.awt.geom.Point2D;

/**
 *  The light {@link Styles.Nimbus} is built out of, read by both its style rules and
 *  {@link Symbols.Nimbus}.
 *  <p>
 *  A relief is a list of gradient stops measured against the surface's own colour rather than
 *  written down: a stop says "this far down, this much brighter and this much less saturated than
 *  the colour handed in". So one relief describes a button, a tab, a table heading and a check box
 *  at once, and describes them in whatever palette is installed.
 *  <p>
 *  The offsets were measured off the colours Nimbus itself paints. Three decimal places is where a
 *  further digit stops changing any byte of the result.
 *
 *  @see LafUtilities#shiftHsb(Color, double, double)
 */
// The stop tables are arrays because both gradient APIs take arrays. No method hands one out.
@SuppressWarnings("ImmutableEnumChecker")
enum NimbusRelief
{
    /**
     *  A moulded surface under an overhead light: a bright top edge falling away quickly, a long
     *  dim stretch through the middle, and the bottom lip catching the light again. Worn by
     *  buttons, combo boxes, tabs, table headings, check boxes and scroll thumbs.
     */
    LIT( new double[]  {  0.00,   0.08,   0.62,  0.70,  1.00 },
         new double[][]{ {-0.036, 0.115}, {-0.028, 0.078}, {0, 0}, {0, 0}, {-0.009, 0.120} } ),

    /**
     *  The same surface with the light off: the same curve at a fifth of the depth. A control that
     *  cannot be used is not greyed here, it is unlit.
     */
    UNLIT( new double[]  {  0.00,   0.11,   0.62,  0.70,  1.00 },
           new double[][]{ {-0.015, 0.039}, {-0.010, 0.024}, {0, 0}, {0, 0}, {-0.001, 0.012} } ),

    /**
     *  The same moulding cast in a saturated colour. It needs its own offsets because the top of a
     *  lit moulding is close to white whatever the body is made of: a nearly grey body reaches that
     *  highlight after a small change, a saturated one only after a much larger one. Worn by the
     *  default button, a ticked box, a selected tab and a combo box's actuator.
     */
    LIT_ACCENTED( new double[]  {  0.00,   0.08,   0.62,   0.70,  1.00 },
                  new double[][]{ {-0.179, 0.184}, {-0.107, 0.129}, {-0.004, 0.004}, {0, 0}, {-0.025, 0.122} } ),

    /**
     *  A strip rather than a moulding: lit at the top, shading evenly to its own colour at the
     *  bottom, with no lip to catch the light. Worn by menu bars and tool bars.
     */
    STRIP( new double[]  {  0.00,   0.23,   1.00 },
           new double[][]{ {-0.036, 0.098}, {-0.040, 0.114}, {0, 0} } ),

    /**
     *  A groove cut into the panel instead of a moulding standing on it, so the light runs the
     *  other way: the top wall of the cut is in shadow and the floor at the bottom catches what
     *  reaches it. It is laid over the border colour, not over a surface colour.
     */
    CUT( new double[]  {  0.00,  0.80,   1.00 },
         new double[][]{ {0, 0}, {-0.019, 0.161}, {-0.077, 0.314} } ),

    /**
     *  A saturated bar under a hard sheen: the top washes almost to white while keeping its hue,
     *  the middle stays the colour itself, and the bottom lip flares. It is the one relief that
     *  moves the saturation further than the brightness, which is what makes a progress bar look
     *  wet next to the dry moulding around it.
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
     * @param tone the colour the middle of the relief reproduces exactly
     * @return the configured gradient, running down the component and clipped to its body
     */
    GradientConf over( GradientConf g, Color tone ) {
        return g.colors(stops(tone))
                .fractions(_fractions.clone())
                .span(UI.Span.TOP_TO_BOTTOM)
                .clipTo(UI.ComponentArea.BODY);
    }

    /**
     *  The same relief as a {@link Paint}, which is what a {@link Symbols} glyph needs: a glyph is
     *  handed a graphics context and a rectangle, and fills a shape rather than declaring a style.
     *
     * @param y the top of the span the relief runs over, in component pixels
     * @param height how tall that span is; a span of no height gets the middle tone, because a
     *               gradient needs two ends that differ
     * @param tone the colour the middle of the relief reproduces exactly
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
