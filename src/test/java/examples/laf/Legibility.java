package examples.laf;

import examples.laf.SwingTreeLookAndFeel.Palette;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.function.Function;

/**
 *  Keeps a palette readable under a style preset it was not designed against, by measuring the
 *  colours it is given rather than recognising them.
 *  <p>
 *  Every palette slot names what a colour is <i>for</i>, so any palette can be paired with any
 *  preset, but nothing stops the pairing from putting pale text on a pale surface: Aurora fills
 *  its surfaces with white because Glassmorphic lays them over a dark ground at thirteen percent
 *  opacity, and Linen paints the same white at full opacity underneath Aurora's near-white text.
 *  This class decides how far apart two colours are with the contrast ratio of the Web Content
 *  Accessibility Guidelines, which is what a reader's eye responds to, and moves a colour that
 *  falls short only along its lightness in the Oklab colour space, which is the one axis a
 *  reader perceives as "lighter" or "darker" without the hue shifting. Hue and saturation are
 *  kept, a colour already readable is returned unchanged, and a colour that has to move moves
 *  exactly as far as it has to.
 *  <p>
 *  Shadows and highlights have the opposite problem: a black shadow at a fixed opacity takes
 *  a lot of light away from a white ground and almost none from a near-black one, so a preset
 *  designed on paper casts no visible shadow on a dark palette, and one designed in the dark
 *  casts a smudge on paper. {@link #shade(Color, Color, Color)} measures how much lightness a
 *  shadow took away from the ground the preset was designed on and takes the same amount from
 *  the ground it is actually painted on.
 */
final class Legibility
{
    private Legibility() {}

    /** The contrast ratio body text needs against its ground: level AA of the Web Content
     *  Accessibility Guidelines for text under 18 points. */
    static final double TEXT = 4.5;

    /** The contrast ratio a caption, a heading in small capitals or a hint needs: less than
     *  {@link #TEXT}, so that a palette can still make it recede, and far enough above the 3.0 of
     *  a non-text outline that a reader does not have to lean in to read it. */
    static final double SECONDARY = 3.5;

    /** The contrast ratio the label of something that cannot be used needs. It is meant to look
     *  unavailable, so it stays well below {@link #SECONDARY}, but a reader still has to be able
     *  to see what it is that is unavailable. */
    static final double DISABLED = 2.0;

    /** How far, as a distance in Oklab, a speck of grain may lie from the ground it is scattered
     *  over. The Linen and Workshop palettes put theirs between 0.006 and 0.05 away; a speck
     *  further out than this stops reading as the texture of a material and starts reading as
     *  a pattern printed over it. */
    static final double GRAIN = 0.06;

    /** How far past its floor a colour that has to move is moved. A colour placed exactly on the
     *  floor would drop back under it wherever a preset lays a gradient, a grain or a sheen over
     *  the ground it was measured against, so a colour that moves at all clears the floor by ten
     *  percent, where the far end of its lightness range allows it. */
    static final double CLEARANCE = 1.1;

    /** Rules run on every paint, and most of what they ask here is the same question with the
     *  same colours, so answers are remembered. The cache is dropped whole once it holds this many,
     *  which no palette comes near, because a theme asks a few dozen questions at most. */
    private static final int REMEMBERED = 2048;
    private static final Map<List<Object>, Color> ANSWERS = new ConcurrentHashMap<>();


    // ── Measuring ────────────────────────────────────────────────────────

    /**
     * @param a one colour, its opacity ignored
     * @param b the other colour, its opacity ignored
     * @return the contrast ratio between the two, from 1 (identical) to 21 (black on white)
     */
    static double contrast( Color a, Color b ) {
        double la = luminance(a), lb = luminance(b);
        return ( Math.max(la, lb) + 0.05 ) / ( Math.min(la, lb) + 0.05 );
    }

    /** @return the relative luminance of the colour, 0 for black and 1 for white, opacity ignored */
    private static double luminance( Color c ) {
        return 0.2126 * linear(c.getRed()) + 0.7152 * linear(c.getGreen()) + 0.0722 * linear(c.getBlue());
    }

    /** @return the Oklab lightness of the colour, 0 for black and 1 for white, opacity ignored */
    private static double lightness( Color c ) { return oklab(c)[0]; }

    /**
     *  What a translucent colour looks like once it is painted over an opaque one, blended the
     *  way Java 2D blends it: channel by channel, in the gamma-encoded values.
     *
     * @param top   the colour painted, at its own opacity
     * @param under the colour it is painted over, taken as opaque
     * @return the opaque colour a reader sees
     */
    static Color over( Color top, Color under ) {
        double a = top.getAlpha() / 255.0;
        return new Color(
            (int) Math.round(top.getRed()   * a + under.getRed()   * ( 1 - a )),
            (int) Math.round(top.getGreen() * a + under.getGreen() * ( 1 - a )),
            (int) Math.round(top.getBlue()  * a + under.getBlue()  * ( 1 - a ))
        );
    }


    // ── Moving one colour ────────────────────────────────────────────────

    /**
     *  The ink closest in lightness to {@code ink} which reads on every one of {@code grounds}
     *  at {@code floor} or better. An ink that already does is returned as it is.
     *  <p>
     *  Both directions are tried, and the smaller move wins, so dark text on a ground that came
     *  out a little too dark gets darker rather than turning white. Only when neither direction
     *  reaches the floor, which happens when the grounds lie on both sides of the ink, is the
     *  lightness chosen that comes closest.
     *
     * @param ink     the colour the text is meant to have
     * @param floor   the contrast ratio it needs, such as {@link #TEXT}
     * @param grounds every opaque colour the text may be written on
     * @return the ink, at the same hue and opacity, moved as little as its lightness allows
     */
    static Color ink( Color ink, double floor, Color... grounds ) {
        if ( worstContrast(ink, grounds) >= floor )
            return ink;
        return remembered(() -> _ink(ink, floor, grounds), "ink", ink, floor, grounds);
    }

    private static Color _ink( Color ink, double floor, Color... grounds ) {
        double from = lightness(ink);
        double up   = firstLightnessReaching(ink, from, 1, floor, grounds);
        double down = firstLightnessReaching(ink, from, 0, floor, grounds);
        if ( !Double.isNaN(up) || !Double.isNaN(down) ) {
            if ( Double.isNaN(down) || ( !Double.isNaN(up) && up - from <= from - down ) )
                return withLightness(ink, up);
            return withLightness(ink, down);
        }
        double best = from, bestContrast = -1;
        for ( int i = 0; i <= 100; i++ ) {
            double contrast = worstContrast(withLightness(ink, i / 100.0), grounds);
            if ( contrast > bestContrast ) {
                bestContrast = contrast;
                best         = i / 100.0;
            }
        }
        return withLightness(ink, best);
    }

    /**
     *  The ground closest in lightness to {@code ground} on which {@code ink} reads at
     *  {@code floor} or better, found by moving the ground away from the ink. This is what a
     *  rule asks when the text colour is part of the design and the fill behind it is not - a
     *  white label on a gloss that lifted the fill it was given towards white.
     *
     * @param ground the fill the rule means to paint
     * @param ink    the colour of the text written on it
     * @param floor  the contrast ratio the text needs
     * @return the fill, at the same hue and opacity, as close to {@code ground} as that allows
     */
    static Color ground( Color ground, Color ink, double floor ) {
        if ( contrast(ground, ink) >= floor )
            return ground;
        return remembered(() -> _ground(ground, ink, floor), "ground", ground, ink, floor);
    }

    private static Color _ground( Color ground, Color ink, double floor ) {
        return groundUnderMix(ground, ink, floor, ink, 0);
    }

    /**
     *  The same colour at another Oklab lightness, with its hue and opacity kept. Where the
     *  lightness leaves no room for the colour's saturation inside the sRGB gamut - a saturated
     *  blue cannot be made nearly white and stay as saturated - the saturation is reduced until
     *  it fits, rather than a channel being clipped, which would shift the hue.
     *
     * @param c         the colour
     * @param lightness the Oklab lightness it should have, 0 to 1
     * @return the colour at that lightness
     */
    private static Color withLightness( Color c, double lightness ) {
        double[] lab = oklab(c);
        double   L   = Math.max(0, Math.min(1, lightness));
        double[] rgb = fromOklab(L, lab[1], lab[2]);
        if ( !inGamut(rgb) ) {
            double low = 0, high = 1;
            for ( int i = 0; i < 24; i++ ) {
                double k = ( low + high ) / 2;
                if ( inGamut(fromOklab(L, lab[1] * k, lab[2] * k)) ) low = k; else high = k;
            }
            rgb = fromOklab(L, lab[1] * low, lab[2] * low);
        }
        return new Color(encode(rgb[0]), encode(rgb[1]), encode(rgb[2]), c.getAlpha());
    }

    /**
     *  Mixes two colours in Oklab, where halfway between them looks halfway to a reader, which
     *  mixing gamma-encoded channels does not. The opacity of {@code from} is kept.
     *
     * @param from the colour at {@code t == 0}
     * @param to   the colour at {@code t == 1}
     * @param t    how far to go, 0 to 1
     * @return the mixed colour
     */
    private static Color towards( Color from, Color to, double t ) {
        if ( t <= 0 )
            return from;
        double[] a = oklab(from), b = oklab(to);
        double[] rgb = fromOklab(
                            a[0] + ( b[0] - a[0] ) * t,
                            a[1] + ( b[1] - a[1] ) * t,
                            a[2] + ( b[2] - a[2] ) * t
                        );
        return new Color(encode(rgb[0]), encode(rgb[1]), encode(rgb[2]), from.getAlpha());
    }

    /**
     *  A speck of grain pulled towards the ground it is scattered over until it lies no further
     *  than {@link #GRAIN} from it. A palette which uses its two grain slots for something else
     *  entirely - Aurora keeps a violet and a magenta bloom in them - would otherwise have every
     *  preset that draws grain sprinkle that bloom over the whole window.
     *
     * @param speck  the colour of the speck, as the palette names it
     * @param ground the colour the grain lies on
     * @return the speck, unchanged if it already lies close enough
     */
    static Color speck( Color speck, Color ground ) {
        double distance = distance(speck, ground);
        if ( distance <= GRAIN )
            return speck;
        return towards(speck, ground, 1 - GRAIN / distance);
    }


    // ── Shadows and highlights ───────────────────────────────────────────

    /**
     *  A shadow as deep on {@code ground} as {@code designed} was on the ground the preset was
     *  designed on.
     *  <p>
     *  The depth of a shadow is how much Oklab lightness it takes away from the ground it falls
     *  on. A black shadow at 25 of 255 takes 0.033 away from a cream page and 0.009 from a
     *  near-black one, which a reader cannot see, so the opacity is raised on a darker ground and
     *  lowered on a lighter one until the lightness taken away is the same. On the ground the
     *  preset was designed on, the shadow comes back unchanged.
     *  <p>
     *  A shadow a preset tints with its text colour - so that a shadow on cream paper is a warm
     *  brown rather than a cold grey - would turn into a pale glow wherever the text is lighter
     *  than the ground, which is on every dark palette. A tint lighter than the ground is
     *  therefore replaced by a very dark shade of the ground's own hue.
     *
     * @param designed         the shadow as the preset wrote it, opacity included
     * @param ground           the opaque colour the shadow falls on
     * @param designedGround   the same ground in the palette the preset was designed against
     * @return the shadow to paint
     */
    static Color shade( Color designed, Color ground, Color designedGround ) {
        boolean darker = lightness(designed) < lightness(ground);
        if ( darker && sameRgb(ground, designedGround) )
            return designed;
        return remembered(() -> {
            Color  tint  = darker ? designed : withLightness(ground, lightness(ground) * 0.2);
            double depth = lightness(designedGround) - lightness(over(designed, designedGround));
            return withOpacity(tint, opacityFor(tint, ground, depth, designed.getAlpha()));
        }, "shade", designed, ground, designedGround);
    }

    /**
     *  A highlight as bright on {@code ground} as {@code designed} was on the ground the preset
     *  was designed on, measured the same way as {@link #shade(Color, Color, Color)} but as
     *  lightness added. A white sheen designed on a mid-tone would glare on a black ground and
     *  disappear on a white one; this keeps the lift it gives the same.
     *
     * @param designed       the highlight as the preset wrote it, opacity included
     * @param ground         the opaque colour the highlight lies over
     * @param designedGround the same ground in the palette the preset was designed against
     * @return the highlight to paint
     */
    static Color light( Color designed, Color ground, Color designedGround ) {
        if ( sameRgb(ground, designedGround) || lightness(designed) <= lightness(ground) )
            return designed;
        return remembered(() -> {
            double lift = lightness(over(designed, designedGround)) - lightness(designedGround);
            return withOpacity(designed, opacityFor(designed, ground, -lift, designed.getAlpha()));
        }, "light", designed, ground, designedGround);
    }

    /**
     *  Finds the opacity at which {@code tint} over {@code ground} moves its lightness by
     *  {@code depth}, positive for darker. The opacity never rises above three times the one the
     *  preset designed, because a shadow or a highlight that has to be made much stronger than
     *  that is being asked for more than its ground can give. When even that is not enough, a
     *  shadow stays at the ceiling, as dark as it is allowed to be, while a highlight keeps the
     *  opacity it was designed with: a white sheen on a ground already close to white cannot be
     *  seen whatever its opacity, and a nearly opaque one would blend into a grey wherever a
     *  grain mixes it with its dark counterpart.
     */
    private static int opacityFor( Color tint, Color ground, double depth, int designedAlpha ) {
        double base    = lightness(ground);
        int    ceiling = Math.min(230, Math.max(designedAlpha, designedAlpha * 3));
        if ( Math.abs(lightness(over(withOpacity(tint, ceiling), ground)) - base) < Math.abs(depth) )
            return depth > 0 ? ceiling : designedAlpha;
        int low = 0, high = ceiling;
        while ( low < high ) {
            int mid = ( low + high ) / 2;
            double moved = Math.abs(lightness(over(withOpacity(tint, mid), ground)) - base);
            if ( moved < Math.abs(depth) ) low = mid + 1; else high = mid;
        }
        return low;
    }


    // ── Adapting a palette ───────────────────────────────────────────────

    /**
     *  A palette in which the three text colours read on every ground they are written on.
     *  <p>
     *  The <b>background</b> is taken as given, because it is most of the window and is what a
     *  reader recognises the palette by. The surfaces come next, because they are what gives a
     *  preset its look, and the text colours are the cheapest thing to move. So, for each of the
     *  text, the muted text and the disabled text:
     *  <ol>
     *    <li>The ink is first made readable on the background, in whichever direction is shorter,
     *        and on the background as the preset shows it: a glass preset shows it through its
     *        blooms.</li>
     *    <li>It is then moved further, but only away from the background - dark text darker on a
     *        light palette, light text lighter on a dark one - until it also reads on every
     *        surface it is written on.</li>
     *    <li>Only when no ink on that side can read on some surface is the surface moved, towards
     *        the background, as far as the ink needs. That is the case of a palette whose
     *        surfaces lie on the other side of the text from its background: Aurora's white
     *        surfaces under its near-white text. The resting, the hovered and the pressed surface
     *        move together, by the same fraction of the way, so the pointer still lifts a
     *        control and a press still sinks it.</li>
     *  </ol>
     *  Body text is measured against every surface; muted text only against the background, the
     *  resting surface and the field, which are where captions and hints are written; disabled
     *  text against the background, the resting surface and the disabled surface.
     *  <p>
     *  The fills of filled controls and of a selection are not touched here, because presets
     *  differ in what they write there: Linen writes {@code onFilled} on {@code primary}, Nimbus
     *  writes its own ink on a moulded gradient of it. A rule that writes on such a fill asks
     *  {@link #fill} or {@link #ink} with the colour it actually paints. The one thing settled
     *  here is that {@code onFilled} can be written on <i>some</i> fill: a mid-tone such as a
     *  khaki reads at {@link #TEXT} neither on black nor on white, so no fill a rule could move
     *  to would carry it. Such an ink is moved towards the end of the range it is nearer to,
     *  until it reads on that end with room to spare.
     *  <p>
     *  {@code seenAs} is what makes this preset-aware without the palette knowing any preset: it
     *  maps a surface colour to every colour a reader may see where that surface is painted, and
     *  a text has to read on all of them. For a preset that paints surfaces opaque, that is the
     *  colour itself. For one that paints them as a translucent wash, it is the wash laid over
     *  each of the colours the window shows through it.
     *
     * @param p      the palette an application or a palette preset chose
     * @param seenAs every colour a reader may see where a surface colour is painted
     * @return a palette in which nothing is changed that was already readable
     */
    static Palette adapt( Palette p, Function<Color, Color[]> seenAs ) {
        return adapt(p, seenAs, seenAs);
    }

    /**
     *  The same, for a preset which paints the field colour differently from the other surfaces.
     *  A preset made of glass lays its surfaces over the window as a thin wash, but Swing fills a
     *  combo box's value strip, a list and a table straight from the field colour, at whatever
     *  opacity the palette gives it, and so does an application that paints something of its own
     *  with {@link Palette#surfaceField()}. So the field is judged by {@code fieldSeenAs}.
     *
     * @param p           the palette an application or a palette preset chose
     * @param seenAs      every colour a reader may see where a surface colour is painted
     * @param fieldSeenAs every colour a reader may see where the field colour is painted
     * @return a palette in which nothing is changed that was already readable
     */
    static Palette adapt( Palette p, Function<Color, Color[]> seenAs, Function<Color, Color[]> fieldSeenAs ) {
        Color   background = p.background();
        Color[] window     = with(background, seenAs.apply(background));
        Color text     = ink(p.text(), TEXT, window);
        Color muted    = ink(p.textMuted(), SECONDARY, window);
        Color disabled = ink(p.textDisabled(), DISABLED, window);

        Color[] raised = { p.surface(), p.surfaceHover(), p.surfacePressed() };
        Color field    = p.surfaceField();
        Color idle     = p.surfaceDisabled();

        if ( !readsOnItsSide(text, TEXT, background, grounds(seenAs, fieldSeenAs, raised, field, idle, background)) ) {
            raised = pulledUntilReadable(raised, background, seenAs, text, TEXT);
            field  = pulledUntilReadable(new Color[]{field}, background, fieldSeenAs, text, TEXT)[0];
            idle   = pulledUntilReadable(new Color[]{idle},  background, seenAs, text, TEXT)[0];
        }
        if ( !readsOnItsSide(muted, SECONDARY, background, grounds(seenAs, fieldSeenAs, new Color[]{raised[0]}, field, null, background)) ) {
            Color resting = pulledUntilReadable(new Color[]{raised[0]}, background, seenAs, muted, SECONDARY)[0];
            double t = fractionMoved(raised[0], resting, background);
            raised = new Color[]{ resting, towards(raised[1], background, t), towards(raised[2], background, t) };
            field  = pulledUntilReadable(new Color[]{field}, background, fieldSeenAs, muted, SECONDARY)[0];
        }

        text     = inkOnItsSide(text,     TEXT,      background, grounds(seenAs, fieldSeenAs, raised, field, idle, background));
        muted    = inkOnItsSide(muted,    SECONDARY, background, grounds(seenAs, fieldSeenAs, new Color[]{raised[0]}, field, null, background));
        disabled = ink(disabled, DISABLED, seen(seenAs, background, raised[0], idle));

        Color onFilled = p.onFilled();
        double carried = Math.max(contrast(onFilled, Color.WHITE), contrast(onFilled, Color.BLACK));
        if ( carried < TEXT * CLEARANCE )
            onFilled = inkOnItsSide(onFilled, TEXT * CLEARANCE,
                                    lightness(onFilled) >= 0.5 ? Color.BLACK : Color.WHITE,
                                    lightness(onFilled) >= 0.5 ? Color.BLACK : Color.WHITE);

        return p.text(text)
                .onFilled(onFilled)
                .textMuted(muted)
                .textDisabled(disabled)
                .surface(raised[0])
                .surfaceHover(raised[1])
                .surfacePressed(raised[2])
                .surfaceField(field)
                .surfaceDisabled(idle);
    }

    private static Color[] with( Color first, Color[] rest ) {
        Color[] all = new Color[rest.length + 1];
        all[0] = first;
        System.arraycopy(rest, 0, all, 1, rest.length);
        return all;
    }

    /** Every ground a text is measured against: the background, the raised surfaces, the field
     *  seen the way the field is painted, and the disabled surface where one is given. */
    private static Color[] grounds(
        Function<Color, Color[]> seenAs, Function<Color, Color[]> fieldSeenAs, Color[] raised, Color field, Color idle, Color background
    ) {
        List<Color> all = new ArrayList<>();
        all.add(background);
        for ( Color surface : raised )
            all.addAll(Arrays.asList(seenAs.apply(surface)));
        all.addAll(Arrays.asList(fieldSeenAs.apply(field)));
        if ( idle != null )
            all.addAll(Arrays.asList(seenAs.apply(idle)));
        return all.toArray(new Color[0]);
    }

    private static Color[] seen( Function<Color, Color[]> seenAs, Color... surfaces ) {
        List<Color> seen = new ArrayList<>();
        for ( Color surface : surfaces )
            seen.addAll(Arrays.asList(seenAs.apply(surface)));
        return seen.toArray(new Color[0]);
    }

    /** @return whether some ink on the far side of {@code ink} from the background reads on all grounds */
    private static boolean readsOnItsSide( Color ink, double floor, Color background, Color... grounds ) {
        double end = lightness(ink) >= lightness(background) ? 1 : 0;
        return worstContrast(withLightness(ink, end), grounds) >= floor;
    }

    /** Moves an ink only away from the background, and only as far as it needs to go or can go. */
    private static Color inkOnItsSide( Color ink, double floor, Color background, Color... grounds ) {
        if ( worstContrast(ink, grounds) >= floor )
            return ink;
        double end   = lightness(ink) >= lightness(background) ? 1 : 0;
        double found = firstLightnessReaching(ink, lightness(ink), end, floor, grounds);
        return withLightness(ink, Double.isNaN(found) ? end : found);
    }

    /**
     *  Moves a set of surfaces towards the background by one common fraction of the way, the
     *  smallest at which {@code ink} reads on each of them at {@code floor}. A set that already
     *  reads is returned as it is, and so is one that the background itself cannot help.
     */
    private static Color[] pulledUntilReadable(
        Color[] surfaces, Color background, Function<Color, Color[]> seenAs, Color ink, double floor
    ) {
        if ( readsOnAll(0, surfaces, background, seenAs, ink, floor) || !readsOnAll(1, surfaces, background, seenAs, ink, floor) )
            return surfaces;
        if ( readsOnAll(1, surfaces, background, seenAs, ink, floor * CLEARANCE) )
            floor *= CLEARANCE;
        double low = 0, high = 1;
        for ( int i = 0; i < 20; i++ ) {
            double mid = ( low + high ) / 2;
            if ( readsOnAll(mid, surfaces, background, seenAs, ink, floor) ) high = mid; else low = mid;
        }
        Color[] pulled = new Color[surfaces.length];
        for ( int i = 0; i < surfaces.length; i++ )
            pulled[i] = towards(surfaces[i], background, high);
        return pulled;
    }

    private static boolean readsOnAll(
        double t, Color[] surfaces, Color background, Function<Color, Color[]> seenAs, Color ink, double floor
    ) {
        for ( Color surface : surfaces )
            for ( Color seen : seenAs.apply(towards(surface, background, t)) )
                if ( contrast(ink, seen) < floor )
                    return false;
        return true;
    }

    /** @return how far along the Oklab line from {@code from} to {@code to} the colour {@code moved} lies */
    private static double fractionMoved( Color from, Color moved, Color to ) {
        double whole = distance(from, to);
        return whole == 0 ? 0 : Math.min(1, distance(from, moved) / whole);
    }


    /**
     *  One state of a filled control - resting, under the pointer, pressed - moved until
     *  {@code ink} reads on it, keeping the lightness step the palette puts between that state
     *  and the resting fill.
     *  <p>
     *  Moving each state on its own would land a hovered fill that was lighter than the resting
     *  one on exactly the same colour, which a reader sees as the control not reacting to the
     *  pointer. So the resting fill is moved first, as little as it can be, and every other state
     *  keeps its distance from it. Where that distance would carry the state back across the
     *  floor, the state goes the same distance the other way: a hovered green that was lighter
     *  than the resting one becomes darker by the same amount, and still changes under the pointer.
     *  <p>
     *  A preset rarely paints the fill exactly as the palette names it. A gloss lifts it part of
     *  the way to white behind the label, and a pane of glass lets part of the window through it.
     *  Both are a mix of the fill with another colour, so the caller names that colour and how
     *  much of it is mixed in, and the label is measured against the mix.
     *
     * @param state      the fill of the state being painted, as the palette names it
     * @param resting    the fill of the same control at rest, as the palette names it
     * @param ink        the colour of the label written on it
     * @param floor      the contrast ratio the label needs
     * @param mixedWith  the colour the preset mixes into the fill behind the label
     * @param mixed      how much of it, from 0 for a fill painted as it is to 1
     * @return the fill to hand the preset in that state, {@code state} itself if nothing had to move
     */
    static Color fill( Color state, Color resting, Color ink, double floor, Color mixedWith, double mixed ) {
        if ( contrast(mix(resting, mixedWith, mixed), ink) >= floor && contrast(mix(state, mixedWith, mixed), ink) >= floor )
            return state;
        return remembered(() -> {
            Color moved = groundUnderMix(resting, ink, floor, mixedWith, mixed);
            if ( state.equals(resting) )
                return moved;
            double step = lightness(state) - lightness(resting);
            Color  same = withLightness(state, lightness(moved) + step);
            if ( contrast(mix(same, mixedWith, mixed), ink) >= floor )
                return same;
            Color other = withLightness(state, lightness(moved) - step);
            return contrast(mix(other, mixedWith, mixed), ink) >= floor
                    ? other
                    : groundUnderMix(state, ink, floor, mixedWith, mixed);
        }, "fill", state, resting, ink, floor, mixedWith, mixed);
    }

    /**
     *  Moves a fill, which is mixed with another colour before the reader sees it, until
     *  {@code ink} reads on the mix. It moves away from the ink first. Only where that end of the
     *  lightness range cannot carry the ink - a khaki label reads on black but not on white, so a
     *  fill lighter than it cannot become light enough - does the fill cross over to the other
     *  side of the ink.
     */
    private static Color groundUnderMix( Color fill, Color ink, double floor, Color mixedWith, double mixed ) {
        double natural = lightness(ink) >= lightness(mix(fill, mixedWith, mixed)) ? 0 : 1;
        double found   = lightnessUnderMix(fill, ink, floor, mixedWith, mixed, natural);
        if ( Double.isNaN(found) )
            found = lightnessUnderMix(fill, ink, floor, mixedWith, mixed, 1 - natural);
        if ( Double.isNaN(found) ) {
            double towardsBlack = contrast(mix(withLightness(fill, 0), mixedWith, mixed), ink);
            double towardsWhite = contrast(mix(withLightness(fill, 1), mixedWith, mixed), ink);
            found = towardsBlack >= towardsWhite ? 0 : 1;
        }
        return withLightness(fill, found);
    }

    /** The lightness closest to the fill's own, on the way to {@code end}, at which the ink reads
     *  on the mix, or {@code NaN} if even {@code end} is not enough. */
    private static double lightnessUnderMix( Color fill, Color ink, double floor, Color mixedWith, double mixed, double end ) {
        double best = contrast(mix(withLightness(fill, end), mixedWith, mixed), ink);
        if ( best < floor )
            return Double.NaN;
        double aim  = best >= floor * CLEARANCE ? floor * CLEARANCE : floor;
        double miss = lightness(fill), hit = end;
        for ( int i = 0; i < 24; i++ ) {
            double mid = ( miss + hit ) / 2;
            if ( contrast(mix(withLightness(fill, mid), mixedWith, mixed), ink) >= aim ) hit = mid; else miss = mid;
        }
        return hit;
    }

    /** Mixes {@code amount} of {@code other} into {@code c} the way Java 2D composites, opaque. */
    private static Color mix( Color c, Color other, double amount ) {
        if ( amount <= 0 )
            return over(c, c);
        return over(withOpacity(other, (int) Math.round(amount * 255)), over(c, c));
    }

    /**
     *  A translucent highlight laid over a ground, made fainter until {@code ink} still reads
     *  where it is brightest. A white gloss at three quarters opacity is a sheen on a pale blue
     *  card and turns a dark card pale grey, under text that was chosen for the dark card.
     *
     * @param highlight the sheen as the preset wrote it, opacity included
     * @param ground    the opaque colour it is laid over
     * @param ink       the colour of text written through it
     * @param floor     the contrast ratio that text needs
     * @return the highlight at the highest opacity, up to its own, that keeps the text readable
     */
    static Color sheen( Color highlight, Color ground, Color ink, double floor ) {
        if ( contrast(over(highlight, ground), ink) >= floor )
            return highlight;
        return remembered(() -> {
            int low = 0, high = highlight.getAlpha();
            while ( low < high ) {
                int mid = ( low + high + 1 ) / 2;
                if ( contrast(over(withOpacity(highlight, mid), ground), ink) >= floor ) low = mid; else high = mid - 1;
            }
            return withOpacity(highlight, low);
        }, "sheen", highlight, ground, ink, floor);
    }

    /**
     *  A translucent wash made smokier until {@code ink} reads through it over every one of
     *  {@code grounds}: its colour is moved towards {@code smoke} and its opacity raised
     *  together, by the smallest fraction of the way that is enough. A pane of frosted glass
     *  over a bright bloom shows the bloom, and the text on the pane was chosen for the dark
     *  night around it; tinting the glass is what a designer would do, and it keeps the glass.
     *
     * @param wash    the fill as the preset wrote it, opacity included
     * @param ink     the colour of text written on it
     * @param floor   the contrast ratio that text needs
     * @param smoke   the colour the wash is tinted towards, normally the window's own background
     * @param grounds every opaque colour that may show through the wash
     * @return the wash to paint, {@code wash} itself if the text already reads
     */
    static Color veil( Color wash, Color ink, double floor, Color smoke, Color... grounds ) {
        if ( readsThrough(wash, ink, floor, grounds) )
            return wash;
        return remembered(() -> {
            double aim = readsThrough(veiled(wash, smoke, 1), ink, floor * CLEARANCE, grounds) ? floor * CLEARANCE : floor;
            double low = 0, high = 1;
            for ( int i = 0; i < 20; i++ ) {
                double mid = ( low + high ) / 2;
                if ( readsThrough(veiled(wash, smoke, mid), ink, aim, grounds) ) high = mid; else low = mid;
            }
            return veiled(wash, smoke, high);
        }, "veil", wash, ink, floor, smoke, grounds);
    }

    private static Color veiled( Color wash, Color smoke, double t ) {
        return withOpacity(towards(wash, smoke, t), (int) Math.round(wash.getAlpha() + ( 230 - wash.getAlpha() ) * t));
    }

    private static boolean readsThrough( Color wash, Color ink, double floor, Color... grounds ) {
        for ( Color ground : grounds )
            if ( contrast(over(wash, ground), ink) < floor )
                return false;
        return true;
    }

    private static Color remembered( Supplier<Color> answer, Object... question ) {
        List<Object> key = new ArrayList<>();
        for ( Object part : question )
            if ( part instanceof Color[] ) key.addAll(Arrays.asList((Color[]) part)); else key.add(part);
        Color known = ANSWERS.get(key);
        if ( known != null )
            return known;
        if ( ANSWERS.size() > REMEMBERED )
            ANSWERS.clear();
        Color computed = answer.get();
        ANSWERS.put(key, computed);
        return computed;
    }


    // ── Internals ────────────────────────────────────────────────────────

    /** Walks the lightness of {@code c} from {@code from} towards {@code to} and returns the
     *  first lightness at which it reads on every ground, or {@code NaN} if none does. */
    private static double firstLightnessReaching( Color c, double from, double to, double floor, Color... grounds ) {
        double aim = worstContrast(withLightness(c, to), grounds) >= floor * CLEARANCE ? floor * CLEARANCE : floor;
        if ( worstContrast(withLightness(c, to), grounds) < aim )
            return Double.NaN;
        double miss = from, hit = to;
        for ( int i = 0; i < 24; i++ ) {
            double mid = ( miss + hit ) / 2;
            if ( worstContrast(withLightness(c, mid), grounds) >= aim ) hit = mid; else miss = mid;
        }
        return hit;
    }

    private static double worstContrast( Color ink, Color... grounds ) {
        double worst = Double.MAX_VALUE;
        for ( Color ground : grounds )
            worst = Math.min(worst, contrast(ink, ground));
        return worst;
    }

    private static double distance( Color a, Color b ) {
        double[] x = oklab(a), y = oklab(b);
        return Math.sqrt(sq(x[0] - y[0]) + sq(x[1] - y[1]) + sq(x[2] - y[2]));
    }

    private static boolean sameRgb( Color a, Color b ) {
        return ( a.getRGB() & 0xFFFFFF ) == ( b.getRGB() & 0xFFFFFF );
    }

    private static Color withOpacity( Color c, int alpha ) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.max(0, Math.min(255, alpha)));
    }

    private static double sq( double v ) { return v * v; }

    /** The linear light of each of the 256 values a gamma-encoded channel can take, worked out
     *  once, because a style rule asks for a contrast ratio on every paint. */
    private static final double[] LINEAR = new double[256];
    static {
        for ( int i = 0; i < 256; i++ ) {
            double v = i / 255.0;
            LINEAR[i] = v <= 0.04045 ? v / 12.92 : Math.pow(( v + 0.055 ) / 1.055, 2.4);
        }
    }

    private static double linear( int channel ) { return LINEAR[channel]; }

    private static int encode( double linear ) {
        double v = Math.max(0, Math.min(1, linear));
        double s = v <= 0.0031308 ? v * 12.92 : 1.055 * Math.pow(v, 1 / 2.4) - 0.055;
        return (int) Math.round(s * 255);
    }

    private static boolean inGamut( double[] rgb ) {
        final double e = 1e-4;
        return rgb[0] >= -e && rgb[0] <= 1 + e && rgb[1] >= -e && rgb[1] <= 1 + e && rgb[2] >= -e && rgb[2] <= 1 + e;
    }

    /** sRGB to Oklab, as Björn Ottosson published it. */
    private static double[] oklab( Color c ) {
        double r = linear(c.getRed()), g = linear(c.getGreen()), b = linear(c.getBlue());
        double l = Math.cbrt(0.4122214708 * r + 0.5363325363 * g + 0.0514459929 * b);
        double m = Math.cbrt(0.2119034982 * r + 0.6806995451 * g + 0.1073969566 * b);
        double s = Math.cbrt(0.0883024619 * r + 0.2817188376 * g + 0.6299787005 * b);
        return new double[]{
            0.2104542553 * l + 0.7936177850 * m - 0.0040720468 * s,
            1.9779984951 * l - 2.4285922050 * m + 0.4505937099 * s,
            0.0259040371 * l + 0.7827717662 * m - 0.8086757660 * s
        };
    }

    /** Oklab to linear sRGB, which may lie outside 0 to 1 for a colour outside the gamut. */
    private static double[] fromOklab( double L, double a, double b ) {
        double l = cube(L + 0.3963377774 * a + 0.2158037573 * b);
        double m = cube(L - 0.1055613458 * a - 0.0638541728 * b);
        double s = cube(L - 0.0894841775 * a - 1.2914855480 * b);
        return new double[]{
             4.0767416621 * l - 3.3077115913 * m + 0.2309699292 * s,
            -1.2684380046 * l + 2.6097574011 * m - 0.3413193965 * s,
            -0.0041960863 * l - 0.7034186147 * m + 1.7076147010 * s
        };
    }

    private static double cube( double v ) { return v * v * v; }
}
