package examples.laf;

import examples.laf.SwingTreeLookAndFeel.Palette;
import org.jspecify.annotations.Nullable;
import swingtree.UI;
import swingtree.style.GradientConf;

import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.plaf.ColorUIResource;
import java.awt.Color;
import java.awt.LinearGradientPaint;
import java.awt.MultipleGradientPaint;
import java.awt.Paint;
import java.awt.geom.Point2D;
import java.util.Arrays;
import java.util.Objects;

/**
 *  The colours of Nimbus, worked out the way Nimbus works them out, for {@link Styles.Nimbus} and
 *  {@link Symbols.Nimbus}.
 *  <p>
 *  Nimbus names a handful of colours in {@link UIManager} - {@code nimbusBase},
 *  {@code nimbusBlueGrey}, {@code control}, {@code nimbusFocus} and the rest of {@link Key} - and
 *  writes every other colour it paints as an offset from one of them: "{@code nimbusBlueGrey},
 *  with 0.07 less saturation and 0.13 more brightness". So an application re-tints the whole of
 *  Nimbus by putting a new {@code nimbusBase} into {@link UIManager}, and it re-tints this preset
 *  the same way. Each of those keys is looked up in {@link UIManager} first, where an application's
 *  own {@code UIManager.put(..)} wins, and only then taken from the installed {@link Palette}, so
 *  the palette presets re-tint Nimbus too.
 *  <p>
 *  A {@link Shade} is one such offset and a {@link Gradient} is a row of them. Both were copied
 *  out of the painters the JDK generates for Nimbus, so the default palette reproduces Nimbus to
 *  the byte, and both remember what they resolved to, so that a style rule reading forty of them
 *  on every repaint pays for the colour arithmetic once per scheme.
 */
final class NimbusScheme
{
    /**
     *  The colours Nimbus derives every other colour from, each with the {@link UIManager} key an
     *  application overrides it through. The order matters: a key is resolved after the keys it is
     *  derived from.
     */
    enum Key
    {
        /** The ink of ordinary text. */
        TEXT("text"),
        /** The ground of a window and of every panel on it. */
        CONTROL("control"),
        /** The one saturated colour: the default button, a ticked box, a selected tab, a scroll thumb. */
        BASE("nimbusBase"),
        /** The neutral every unlit control is made of. Nimbus derives it from {@link #BASE}. */
        BLUE_GREY("nimbusBlueGrey"),
        /** The outline of a scroll pane and of a text area inside one. */
        BORDER("nimbusBorder"),
        /** The ring around the focused control. */
        FOCUS("nimbusFocus"),
        /** The page of a text field, a list, a table and a tree. */
        LIGHT_BACKGROUND("nimbusLightBackground"),
        /** The band behind a selected row, a selected menu item and selected text. */
        SELECTION_BACKGROUND("nimbusSelectionBackground"),
        /** The darker blue a combo box's list and a tree's selected row are drawn in. */
        SELECTION("nimbusSelection"),
        /** The ink of a label lying on {@link #SELECTION_BACKGROUND}. */
        SELECTED_TEXT("nimbusSelectedText"),
        /** The ink of a label that cannot be used. */
        DISABLED_TEXT("nimbusDisabledText"),
        /** The ground of a tool tip. */
        INFO("info"),
        /** The fill of a progress bar. */
        ORANGE("nimbusOrange"),
        /** The colour of something that went well. */
        GREEN("nimbusGreen"),
        /** The colour of something that went wrong. */
        RED("nimbusRed"),
        /** The colour of an informative icon. */
        INFO_BLUE("nimbusInfoBlue"),
        /** The colour of a warning icon. */
        ALERT_YELLOW("nimbusAlertYellow");

        private final String _uiKey;

        Key( String uiKey ) { _uiKey = uiKey; }

        /** @return the name Nimbus files this colour under in {@link UIManager} */
        String uiKey() { return _uiKey; }
    }

    /** Nimbus derives {@code nimbusBlueGrey} from {@code nimbusBase} by this offset. */
    private static final float[] BLUE_GREY_FROM_BASE = { 0.032459438f, -0.52518797f, 0.19607842f };

    /**
     *  A button at rest is {@code nimbusBlueGrey} lifted by this offset, which lands exactly on
     *  {@code control}. Undoing it recovers the blue-grey a palette's own surface colour implies,
     *  which for the Nimbus palette is {@code nimbusBlueGrey} itself, to the byte.
     */
    private static final float[] BLUE_GREY_FROM_SURFACE = { 0f, 0.07016757f, -0.12941176f };

    /** {@code nimbusFocus} measured against {@code nimbusBase}, so that a re-tinted accent moves
     *  the focus ring with it. */
    private static final float[] FOCUS_FROM_BASE = { 0.00113559f, -0.18595353f, 0.27058822f };

    private static volatile @Nullable NimbusScheme _last = null;

    /** Moves on whenever something is put into {@link UIManager} or a look and feel is installed,
     *  which is the only way an application reaches the keys this scheme reads. */
    private static volatile int _generation = 0;

    static {
        UIManager.getDefaults().addPropertyChangeListener(event -> _generation++);
        UIManager.addPropertyChangeListener(event -> _generation++);
    }

    private final Palette _palette;
    private final int     _generationSeen;
    private final Color[] _colors;

    private NimbusScheme( Palette palette, int generation, Color[] colors ) {
        _palette        = palette;
        _generationSeen = generation;
        _colors         = colors;
    }

    /**
     *  The scheme in force for a palette. It is the same instance for as long as neither the palette
     *  nor any of the {@link Key} colours in {@link UIManager} changes, which is what lets a
     *  {@link Shade} remember what it resolved to.
     *
     * @param palette the palette of the installed look and feel
     * @return the colours Nimbus would paint with
     */
    static NimbusScheme of( Palette palette ) {
        NimbusScheme last       = _last;
        int          generation = _generation;
        if ( last != null && last._palette == palette && last._generationSeen == generation )
            return last;
        Color[] colors = resolve(palette, true);
        // Equal colours are kept as the same array, which is what a cached shade is checked against.
        if ( last != null && Arrays.equals(last._colors, colors) )
            colors = last._colors;
        NimbusScheme scheme = new NimbusScheme(palette, generation, colors);
        _last = scheme;
        return scheme;
    }

    /** @return one of the named colours */
    Color get( Key key ) { return _colors[key.ordinal()]; }

    /** @return whether two schemes resolve every colour identically, so that a colour cached for
     *          one is right for the other */
    boolean isSameAs( NimbusScheme other ) { return other._colors == _colors; }

    /**
     *  Puts every {@link Key} colour the palette implies into a look and feel's defaults, together
     *  with the system colours Nimbus redefines in terms of them, so that an application which reads
     *  {@code UIManager.getColor("nimbusBase")} to paint something of its own finds the colour this
     *  preset is painting with. An application's own {@code UIManager.put(..)} still wins, because
     *  {@link UIManager} looks there before it looks at a look and feel's defaults.
     *
     * @param table the defaults of the look and feel being installed
     * @param palette its palette
     */
    static void install( UIDefaults table, Palette palette ) {
        Color[] colors = resolve(palette, false);
        for ( Key key : Key.values() )
            table.put(key.uiKey(), new ColorUIResource(colors[key.ordinal()]));
        Color text     = colors[Key.TEXT.ordinal()];
        Color base     = colors[Key.BASE.ordinal()];
        Color blueGrey = colors[Key.BLUE_GREY.ordinal()];
        put(table, "infoText",          text);
        put(table, "menuText",          text);
        put(table, "controlText",       text);
        put(table, "textForeground",    text);
        put(table, "menu",              derive(base, 0.021348298f, -0.6150531f, 0.39999998f, 0));
        put(table, "scrollbar",         derive(blueGrey, -0.006944418f, -0.07296763f, 0.09019607f, 0));
        put(table, "controlHighlight",  derive(blueGrey, 0f, -0.07333623f, 0.20392156f, 0));
        put(table, "controlLHighlight", derive(blueGrey, 0f, -0.098526314f, 0.2352941f, 0));
        put(table, "controlShadow",     derive(blueGrey, -0.0027777553f, -0.0212406f, 0.13333333f, 0));
        put(table, "controlDkShadow",   derive(blueGrey, -0.0027777553f, -0.0018306673f, -0.02352941f, 0));
        put(table, "textHighlight",     colors[Key.SELECTION_BACKGROUND.ordinal()]);
        put(table, "textHighlightText", colors[Key.SELECTED_TEXT.ordinal()]);
        put(table, "textInactiveText",  colors[Key.DISABLED_TEXT.ordinal()]);
        put(table, "textBackground",    colors[Key.SELECTION_BACKGROUND.ordinal()]);
        put(table, "background",        colors[Key.CONTROL.ordinal()]);
        put(table, "desktop",           derive(base, -0.009207249f, -0.13984653f, -0.07450983f, 0));
        put(table, "activeCaption",     derive(blueGrey, 0f, -0.049920253f, 0.031372547f, 0));
        put(table, "inactiveCaption",   derive(blueGrey, -0.00505054f, -0.055526316f, 0.039215684f, 0));
    }

    private static void put( UIDefaults table, String key, Color color ) {
        table.put(key, new ColorUIResource(color));
    }

    /**
     *  Works out every {@link Key} colour in order.
     *
     * @param palette where a colour comes from that {@link UIManager} does not override
     * @param readOverrides whether to look in {@link UIManager} at all; installing the defaults
     *                      must not, or an override would be copied into the defaults it shadows
     * @return the colours, indexed by {@link Key#ordinal()}
     */
    private static Color[] resolve( Palette palette, boolean readOverrides ) {
        Key[]   keys   = Key.values();
        Color[] colors = new Color[keys.length];
        for ( Key key : keys ) {
            Color override = readOverrides ? overrideOf(key) : null;
            colors[key.ordinal()] = override != null ? override : fromPalette(key, palette, colors, readOverrides);
        }
        return colors;
    }

    private static Color fromPalette( Key key, Palette p, Color[] resolved, boolean readOverrides ) {
        switch ( key ) {
            case TEXT:                 return p.text();
            case CONTROL:              return p.background();
            case BASE:                 return p.accent();
            case BLUE_GREY:
                // Nimbus derives the blue-grey from the base, so an application that re-tints only
                // the base expects the greys to follow it. Otherwise the palette's own surface says
                // what the greys are.
                if ( readOverrides && overrideOf(Key.BASE) != null )
                    return derive(resolved[Key.BASE.ordinal()], BLUE_GREY_FROM_BASE);
                return derive(p.surface(), BLUE_GREY_FROM_SURFACE);
            case BORDER:               return derive(resolved[Key.BLUE_GREY.ordinal()], 0f, -0.017358616f, -0.11372548f, 0);
            case FOCUS:                return derive(p.accent(), FOCUS_FROM_BASE);
            case LIGHT_BACKGROUND:     return p.surfaceField();
            case SELECTION_BACKGROUND: return p.accentSoft();
            case SELECTION:            return derive(resolved[Key.BASE.ordinal()], -0.010750473f, -0.04875779f, -0.007843137f, 0);
            case SELECTED_TEXT:        return p.onFilled();
            case DISABLED_TEXT:        return p.textDisabled();
            case INFO:                 return p.textureLight();
            case ORANGE:               return p.primary();
            case GREEN:                return new Color(176, 179, 50);
            case RED:                  return p.danger();
            case INFO_BLUE:            return new Color(47, 92, 180);
            case ALERT_YELLOW:         return new Color(255, 220, 35);
            default: throw new IllegalArgumentException(key.name());
        }
    }

    /**
     *  The colour an application put under a key, or {@code null} if the key holds what the look
     *  and feel put there itself. A colour resource is copied into a plain colour, because the style
     *  engine installs the colours it is handed and Swing replaces an installed resource on the
     *  next {@code updateUI()}.
     */
    private static @Nullable Color overrideOf( Key key ) {
        Object value = UIManager.get(key.uiKey());
        if ( !(value instanceof Color) || value == UIManager.getLookAndFeelDefaults().get(key.uiKey()) )
            return null;
        return new Color(((Color) value).getRGB(), true);
    }

    // ── The arithmetic ───────────────────────────────────────────────────

    private static Color derive( Color source, float[] offset ) {
        return derive(source, offset[0], offset[1], offset[2], 0);
    }

    /**
     *  Moves a colour by an offset in hue, saturation, brightness and opacity, with exactly the
     *  rounding and clamping Nimbus's own derived colours use. The hue wraps around; the rest stop
     *  at their ends.
     *
     * @param source the colour to move
     * @param hue how far to turn the hue, as a fraction of the colour wheel
     * @param saturation how much saturation to add
     * @param brightness how much brightness to add
     * @param alpha how much opacity to add, in steps of 1/255
     * @return the moved colour
     */
    static Color derive( Color source, float hue, float saturation, float brightness, int alpha ) {
        float[] hsb = Color.RGBtoHSB(source.getRed(), source.getGreen(), source.getBlue(), null);
        float   s   = Math.max(0f, Math.min(1f, hsb[1] + saturation));
        float   b   = Math.max(0f, Math.min(1f, hsb[2] + brightness));
        int     a   = Math.max(0, Math.min(255, source.getAlpha() + alpha));
        return new Color(( Color.HSBtoRGB(hsb[0] + hue, s, b) & 0xFFFFFF ) | ( a << 24 ), true);
    }

    /** The colour halfway between two others, rounded the way Nimbus rounds the stops it inserts
     *  between the colours of a gradient. */
    static Color midpoint( Color from, Color to ) {
        return new Color(
                    from.getRed()   + Math.round(( to.getRed()   - from.getRed()   ) * 0.5f),
                    from.getGreen() + Math.round(( to.getGreen() - from.getGreen() ) * 0.5f),
                    from.getBlue()  + Math.round(( to.getBlue()  - from.getBlue()  ) * 0.5f),
                    from.getAlpha() + Math.round(( to.getAlpha() - from.getAlpha() ) * 0.5f)
                );
    }

    /**
     *  A colour part of the way from one colour to another, which is what a gradient paints at that
     *  point between two of its stops.
     *
     * @param from the colour at the start
     * @param to the colour at the end
     * @param t how far along, from 0 to 1
     * @return the colour in between
     */
    static Color mix( Color from, Color to, float t ) {
        return new Color(
                    Math.round(from.getRed()   + ( to.getRed()   - from.getRed()   ) * t),
                    Math.round(from.getGreen() + ( to.getGreen() - from.getGreen() ) * t),
                    Math.round(from.getBlue()  + ( to.getBlue()  - from.getBlue()  ) * t),
                    Math.round(from.getAlpha() + ( to.getAlpha() - from.getAlpha() ) * t)
                );
    }

    // ── Shades and gradients ─────────────────────────────────────────────

    /**
     *  Declares a shade of one of the named colours.
     *
     * @param key the colour it is derived from
     * @param hue the hue offset
     * @param saturation the saturation offset
     * @param brightness the brightness offset
     * @return the shade
     */
    static Shade shade( Key key, double hue, double saturation, double brightness ) {
        return new Shade(key, (float) hue, (float) saturation, (float) brightness, 0);
    }

    /**
     *  Declares a translucent shade of one of the named colours.
     *
     * @param alpha how much opacity to add to the named colour, which is opaque, so a negative number
     * @return the shade
     */
    static Shade shade( Key key, double hue, double saturation, double brightness, int alpha ) {
        return new Shade(key, (float) hue, (float) saturation, (float) brightness, alpha);
    }

    /**
     *  Declares a colour Nimbus writes down as a value rather than deriving it, which is rare and
     *  always a highlight: the pale lip under a pressed button, for one.
     *
     * @return a shade that is this colour in every scheme
     */
    static Shade constant( int red, int green, int blue, int alpha ) {
        return new Shade(new Color(red, green, blue, alpha));
    }

    /** Stands in a {@link Gradient}'s stops for the colour halfway between the stops either side of it. */
    static final Object MID = new Object();

    /**
     *  Declares a gradient the way Nimbus writes one down: where each stop sits, as a fraction of
     *  the height of the shape it fills, and what each stop is - a {@link Shade}, or {@link #MID}
     *  for the colour halfway between its neighbours.
     *
     * @param fractions where the stops sit, increasing
     * @param stops one {@link Shade} or {@link #MID} per fraction
     * @return the gradient
     */
    static Gradient gradient( double[] fractions, Object... stops ) {
        return new Gradient(fractions, stops);
    }

    /** One colour of the scheme: a named colour and the offset Nimbus moves it by. */
    static final class Shade
    {
        private final @Nullable Key   _key;
        private final @Nullable Color _constant;
        private final float _hue, _saturation, _brightness;
        private final int   _alpha;

        private volatile @Nullable Resolved _resolved = null;

        private Shade( Key key, float hue, float saturation, float brightness, int alpha ) {
            _key = key; _constant = null;
            _hue = hue; _saturation = saturation; _brightness = brightness; _alpha = alpha;
        }

        private Shade( Color constant ) {
            _key = null; _constant = constant;
            _hue = 0; _saturation = 0; _brightness = 0; _alpha = 0;
        }

        /**
         * @param scheme the scheme in force
         * @return this shade in that scheme
         */
        Color in( NimbusScheme scheme ) {
            if ( _constant != null || _key == null )
                return Objects.requireNonNull(_constant);
            Resolved resolved = _resolved;
            if ( resolved != null && resolved.scheme.isSameAs(scheme) )
                return resolved.colors[0];
            Color color = derive(scheme.get(_key), _hue, _saturation, _brightness, _alpha);
            _resolved = new Resolved(scheme, new Color[]{ color });
            return color;
        }

        /**
         *  This shade laid over a colour of the application's choosing instead of over its named
         *  colour. It is how Nimbus paints a button whose background an application has set: the
         *  saturation and brightness of each stop move the same way, the hue offset is dropped and
         *  the hue is the chosen colour's own.
         *
         * @param tint the colour to derive from
         * @return this shade of it
         */
        Color over( Color tint ) {
            if ( _constant != null )
                return _constant;
            return derive(tint, 0f, _saturation, _brightness, _alpha);
        }
    }

    /** A vertical gradient of {@link Shade}s, with Nimbus's stops and the halfway colours between them. */
    static final class Gradient
    {
        private final float[]  _fractions;
        private final Object[] _stops;

        private volatile @Nullable Resolved _resolved = null;

        private Gradient( double[] fractions, Object[] stops ) {
            if ( fractions.length != stops.length )
                throw new IllegalArgumentException("A gradient needs one stop per fraction.");
            _fractions = new float[fractions.length];
            for ( int i = 0; i < fractions.length; i++ )
                _fractions[i] = (float) fractions[i];
            _stops = stops.clone();
        }

        /**
         * @param scheme the scheme in force
         * @return the colour of every stop, a new array the caller may keep
         */
        Color[] colors( NimbusScheme scheme ) {
            Resolved resolved = _resolved;
            if ( resolved == null || !resolved.scheme.isSameAs(scheme) ) {
                resolved = new Resolved(scheme, _resolve(scheme, null));
                _resolved = resolved;
            }
            return resolved.colors.clone();
        }

        /**
         * @param scheme the scheme in force
         * @param tint a colour chosen by the application to lay the gradient over, see
         *             {@link Shade#over(Color)}; {@code null} for the named colours
         * @return the colour of every stop
         */
        Color[] colors( NimbusScheme scheme, @Nullable Color tint ) {
            return tint == null ? colors(scheme) : _resolve(scheme, tint);
        }

        private Color[] _resolve( NimbusScheme scheme, @Nullable Color tint ) {
            Color[] colors = new Color[_stops.length];
            for ( int i = 0; i < _stops.length; i++ )
                if ( _stops[i] instanceof Shade )
                    colors[i] = tint == null ? ((Shade) _stops[i]).in(scheme) : ((Shade) _stops[i]).over(tint);
            for ( int i = 0; i < _stops.length; i++ )
                if ( _stops[i] == MID )
                    colors[i] = midpoint(colors[i - 1], colors[i + 1]);
            return colors;
        }

        /** @return where the stops sit, a new array the caller may keep */
        float[] fractions() { return _fractions.clone(); }

        /**
         *  Lays this gradient down a style's component area, top to bottom.
         *
         * @param g the gradient being configured
         * @param scheme the scheme in force
         * @param tint see {@link #colors(NimbusScheme, Color)}
         * @return the configured gradient
         */
        GradientConf over( GradientConf g, NimbusScheme scheme, @Nullable Color tint ) {
            float[]  f         = _fractions;
            double[] fractions = new double[f.length];
            for ( int i = 0; i < f.length; i++ )
                fractions[i] = f[i];
            return g.colors(colors(scheme, tint)).fractions(fractions).span(UI.Span.TOP_TO_BOTTOM);
        }

        /**
         *  The same gradient as a paint running from one height to another, for a symbol.
         *
         * @param scheme the scheme in force
         * @param top where the first stop is measured from
         * @param bottom where the last stop is measured to
         * @return the paint
         */
        Paint paint( NimbusScheme scheme, float top, float bottom ) {
            return paint(scheme, 0, top, 0, bottom);
        }

        /**
         *  The same gradient as a paint running between two points.
         *
         * @return the paint
         */
        Paint paint( NimbusScheme scheme, float x1, float y1, float x2, float y2 ) {
            if ( x1 == x2 && y1 == y2 )
                y2 += 0.00001f;
            return new LinearGradientPaint(
                        new Point2D.Float(x1, y1), new Point2D.Float(x2, y2),
                        _fractions, colors(scheme), MultipleGradientPaint.CycleMethod.NO_CYCLE
                    );
        }
    }

    /** What a shade or a gradient last resolved to, and in which scheme. */
    private static final class Resolved
    {
        final NimbusScheme scheme;
        final Color[]      colors;

        Resolved( NimbusScheme scheme, Color[] colors ) { this.scheme = scheme; this.colors = colors; }
    }
}
