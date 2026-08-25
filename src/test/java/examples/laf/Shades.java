package examples.laf;

import java.awt.Color;

/**
 *  Derives one colour from another.
 *  <p>
 *  Presets that model light rather than pigment need shades a palette has no name for: soft UI
 *  wants "this surface, lit" and "this surface, in shadow" so it can extrude a control out of the
 *  panel it sits on, and glossy fills want a lighter and a darker step of whatever colour they were
 *  handed. Naming those in the palette would be wrong - they are not decisions a theme author
 *  makes, they are a function of a colour that already exists - and hard-coding them would break
 *  the moment the palette is swapped. So they are computed here instead, which is what lets any
 *  preset be paired with any {@link SwingTreeLookAndFeel.PalettePreset}.
 */
final class Shades
{
    private Shades() {}

    /**
     *  Moves a colour towards white.
     *
     * @param base   the colour to lighten
     * @param amount how far towards white to go, from 0 (unchanged) to 1 (white)
     * @return the lightened colour, at the original opacity
     */
    static Color lighter( Color base, double amount ) { return towards(base, 255, amount); }

    /**
     *  Moves a colour towards black.
     *
     * @param base   the colour to darken
     * @param amount how far towards black to go, from 0 (unchanged) to 1 (black)
     * @return the darkened colour, at the original opacity
     */
    static Color darker( Color base, double amount ) { return towards(base, 0, amount); }

    /**
     *  The same colour at a different opacity.
     *
     * @param base  the colour
     * @param alpha how opaque, from 0 to 255
     * @return the colour at that opacity
     */
    static Color alpha( Color base, int alpha ) {
        return new Color(base.getRed(), base.getGreen(), base.getBlue(), clamp(alpha));
    }

    /**
     *  Mixes two colours.
     *
     * @param from the colour at {@code t == 0}
     * @param to   the colour at {@code t == 1}
     * @param t    how far along the mix, from 0 to 1
     * @return the mixed colour, opacity mixed along with the channels
     */
    static Color mix( Color from, Color to, double t ) {
        double amount = Math.max(0, Math.min(1, t));
        return new Color(
            clamp((int) Math.round(from.getRed()   + (to.getRed()   - from.getRed())   * amount)),
            clamp((int) Math.round(from.getGreen() + (to.getGreen() - from.getGreen()) * amount)),
            clamp((int) Math.round(from.getBlue()  + (to.getBlue()  - from.getBlue())  * amount)),
            clamp((int) Math.round(from.getAlpha() + (to.getAlpha() - from.getAlpha()) * amount))
        );
    }

    /** @return {@code true} if the colour is dark enough that light text reads better on it. */
    static boolean isDark( Color colour ) {
        double luminance = ( 0.2126 * colour.getRed() + 0.7152 * colour.getGreen() + 0.0722 * colour.getBlue() ) / 255.0;
        return luminance < 0.5;
    }

    private static Color towards( Color base, int target, double amount ) {
        double t = Math.max(0, Math.min(1, amount));
        return new Color(
            clamp((int) Math.round(base.getRed()   + (target - base.getRed())   * t)),
            clamp((int) Math.round(base.getGreen() + (target - base.getGreen()) * t)),
            clamp((int) Math.round(base.getBlue()  + (target - base.getBlue())  * t)),
            base.getAlpha()
        );
    }

    private static int clamp( int channel ) { return Math.max(0, Math.min(255, channel)); }
}
