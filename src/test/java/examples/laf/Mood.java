package examples.laf;

import examples.laf.SwingTreeLookAndFeel.Palette;

import java.awt.Color;

/**
 *  What a palette leaves a theme to tell a surface from the ground it lies on with, which is what
 *  {@link SwingTreeLookAndFeel.StylePreset#POLYMORPHIC} builds its whole appearance out of. It is
 *  a property of the palette, so it is worked out once here and every rule then asks for it.
 */
enum Mood
{
    /** The ground and the things standing on it are the same colour, so only light can tell
     *  them apart: a highlight on one side, a shadow on the other. */
    RELIEF,
    /** A dark room. A surface is told by the rim of light along its edge and by being a shade
     *  nearer the light than the wall behind it. */
    LUMINOUS,
    /** A light ground with real contrast to spend: a flat fill and a soft shadow are enough,
     *  and anything more would be decoration. */
    SHEET;

    /** How far apart two colours have to be, summed over the three channels, before a flat fill in
     *  one can be seen against the other. Below this the light has to do the work instead. */
    private static final int SEPARABLE = 12;

    static Mood of( Palette p ) {
        if ( distance(p.background(), p.surface()) < SEPARABLE )
            return RELIEF;
        return luminance(p.background()) < 0.4 ? LUMINOUS : SHEET;
    }

    private static int distance( Color a, Color b ) {
        return Math.abs(a.getRed()   - b.getRed())
             + Math.abs(a.getGreen() - b.getGreen())
             + Math.abs(a.getBlue()  - b.getBlue());
    }

    private static double luminance( Color c ) {
        return ( 0.2126 * c.getRed() + 0.7152 * c.getGreen() + 0.0722 * c.getBlue() ) / 255.0;
    }
}
