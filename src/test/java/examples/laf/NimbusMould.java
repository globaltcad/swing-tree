package examples.laf;

import org.jspecify.annotations.Nullable;
import swingtree.UI;
import swingtree.style.ComponentStyleDelegate;

import javax.swing.JComponent;
import java.awt.Color;

import static examples.laf.NimbusScheme.Key.BASE;
import static examples.laf.NimbusScheme.Key.BLUE_GREY;
import static examples.laf.NimbusScheme.MID;
import static examples.laf.NimbusScheme.constant;
import static examples.laf.NimbusScheme.gradient;
import static examples.laf.NimbusScheme.shade;

/**
 *  How Nimbus fills one state of a raised control, read by {@link Styles.Nimbus} and
 *  {@link Symbols.Nimbus}.
 *  <p>
 *  Every raised control in Nimbus is painted the same three times over, each time a pixel further
 *  in: a <b>lip</b> one pixel below the control, which is a shadow while the control stands up and a
 *  highlight while it is held down; an <b>edge</b>, filled as a whole shape and darkening towards
 *  the bottom; and a <b>face</b> filled over the edge one pixel in from it, which leaves the edge
 *  showing as an outline. A button, a toggle button, a tool bar button and a combo box differ only in
 *  which colours they fill those three shapes with and how round the shapes are, so a mould is those
 *  colours and nothing else.
 *  <p>
 *  The values are copied from the painters the JDK generates for Nimbus, stop for stop, so the
 *  names read like Nimbus's own state names.
 */
enum NimbusMould
{
    BUTTON( lip(-190),
            edge(Stops.E_RESTING, bg(-0.055555522, -0.05356429, -0.12549019), bg(0, -0.0147816315, -0.3764706)),
            face(Stops.F_RESTING, bg(0.055555582, -0.10655806, 0.24313724), bg(0, -0.09823123, 0.2117647),
                            bg(0, -0.07016757, 0.12941176), bg(0, -0.0749532, 0.24705881), bg(0, -0.110526316, 0.25490195)) ),

    BUTTON_MOUSE_OVER( lip(-190),
            edge(Stops.E_RESTING, bg(0, -0.020974077, -0.21960783), bg(0, 0.11169591, -0.53333336)),
            face(Stops.F_MOUSE_OVER, bg(0.055555582, -0.10658931, 0.25098038), bg(0, -0.098526314, 0.2352941),
                               bg(0, -0.07333623, 0.20392156), bg(0, -0.110526316, 0.25490195), bg(0, -0.110526316, 0.25490195)) ),

    BUTTON_PRESSED( constant(245, 250, 255, 160),
            edge(Stops.E_PRESSED, bg(0.055555582, 0.8894737, -0.7176471), bg(0, 0.0005847961, -0.32156864)),
            face(Stops.F_RESTING, bg(-0.00505054, -0.05960039, 0.10196078), bg(-0.008547008, -0.04772438, 0.06666666),
                            bg(-0.0027777553, -0.0018306673, -0.02352941), bg(-0.0027777553, -0.0212406, 0.13333333),
                            bg(0.0055555105, -0.030845039, 0.23921567)) ),

    BUTTON_DISABLED( lip(-232),
            edge(Stops.E_RESTING, bg(0, -0.06766917, 0.07843137), bg(0, -0.06484103, 0.027450979)),
            face(Stops.F_FLAT, bg(0, -0.08477524, 0.16862744), bg(-0.015872955, -0.080091536, 0.15686274),
                         bg(0, -0.07016757, 0.12941176), bg(0, -0.07052632, 0.1372549), bg(0, -0.070878744, 0.14509803)) ),

    /** The button a root pane presses for Enter, which is the one blue button on a form. */
    DEFAULT_BUTTON( lip(-190),
            edge(Stops.E_PRESSED, base(0.00051498413, -0.34585923, -0.007843137), base(0.00051498413, -0.095173776, -0.25882354)),
            face(Stops.F_RESTING, base(0.004681647, -0.6197143, 0.43137252), base(0.004681647, -0.5766426, 0.38039213),
                            base(0.00051498413, -0.43866998, 0.24705881), base(0.00051498413, -0.46404046, 0.36470586),
                            base(0.00051498413, -0.47761154, 0.44313723)) ),

    DEFAULT_BUTTON_MOUSE_OVER( lip(-190),
            edge(Stops.E_PRESSED, base(0.0013483167, -0.1769987, -0.12156865), base(0.059279382, 0.3642857, -0.43529415)),
            face(Stops.F_RESTING, base(0.004681647, -0.6198413, 0.43921566), base(-0.0017285943, -0.5822163, 0.40392154),
                            base(0.00051498413, -0.4555341, 0.3215686), base(0.00051498413, -0.47698414, 0.43921566),
                            base(-0.06415892, -0.5455182, 0.45098037)) ),

    DEFAULT_BUTTON_PRESSED( shade(BLUE_GREY, 0, -0.110526316, 0.25490195, -95),
            edge(Stops.E_PRESSED, base(-0.57865167, -0.6357143, -0.54901963), base(-0.0000352859, 0.018606722, -0.23137257)),
            face(Stops.F_RESTING, base(-0.00042033195, -0.38050595, 0.20392156), base(0.001903832, -0.29863563, 0.1490196),
                            base(0, 0, 0), base(0.0018727183, -0.14126986, 0.15686274), base(0.00089377165, -0.20852983, 0.2588235)) ),

    /** A toggle button that is on: sunk into the panel rather than standing on it, so its lip is a highlight. */
    TOGGLE_SELECTED( selectedLip(-86),
            edge(Stops.E_SUNK, bg(0, -0.06472479, -0.23137254), bg(0.007936537, -0.06959064, -0.0745098)),
            sunkFace(bg(0.0138888955, -0.06401469, -0.07058823), bg(0, -0.06530018, 0.035294116), bg(0, -0.06507177, 0.031372547)) ),

    /** A toggle button that is on with the pointer over it, which is also how one that is off looks while held down. */
    TOGGLE_SELECTED_MOUSE_OVER( selectedLip(-86),
            edge(Stops.E_SUNK, bg(-0.01111114, -0.060526315, -0.3529412), bg(0, -0.064372465, -0.2352941)),
            sunkFace(bg(-0.006944418, -0.0595709, -0.12941176), bg(0, -0.061075766, -0.031372547), bg(0, -0.06080256, -0.035294116)) ),

    TOGGLE_SELECTED_PRESSED( selectedLip(-86),
            edge(Stops.E_SUNK, bg(-0.027777791, -0.05338346, -0.47058824), bg(0, -0.049301825, -0.36078432)),
            sunkFace(bg(-0.018518567, -0.03909774, -0.2509804), bg(-0.00505054, -0.040013492, -0.13333333), bg(0.01010108, -0.039558575, -0.1372549)) ),

    TOGGLE_SELECTED_DISABLED( selectedLip(-220),
            edge(Stops.E_SUNK, bg(0, -0.066408664, 0.054901958), bg(0, -0.06807348, 0.086274505)),
            sunkFace(bg(0, -0.06807348, 0.086274505), bg(0, -0.06924191, 0.109803915), bg(0, -0.06924191, 0.109803915)) ),

    /** A button on a tool bar, which Nimbus paints only while the pointer is over it. */
    TOOL_BAR_BUTTON_MOUSE_OVER( lip(-153),
            mouseOverEdge(),
            face(Stops.F_FLAT, bg(0.055555582, -0.10658931, 0.25098038), bg(0, -0.098526314, 0.2352941),
                         bg(0, -0.07333623, 0.20392156), bg(0, -0.110526316, 0.25490195), bg(0, -0.110526316, 0.25490195)) ),

    TOOL_BAR_BUTTON_PRESSED( lip(-153),
            mouseOverEdge(),
            face(Stops.F_FLAT, bg(-0.00505054, -0.05960039, 0.10196078), bg(-0.008547008, -0.04772438, 0.06666666),
                         bg(-0.0027777553, -0.0018306673, -0.02352941), bg(-0.0027777553, -0.0212406, 0.13333333),
                         bg(0.0055555105, -0.030845039, 0.23921567)) ),

    /** The part of a combo box showing its value. Its actuator is {@link #COMBO_BOX_ACTUATOR}. */
    COMBO_BOX( shade(BLUE_GREY, 0, 0, -0.22, -176),
            edge(Stops.E_SUNK, base(0.032459438, -0.5787523, 0.07058823), base(0.032459438, -0.5399696, -0.18039218)),
            comboFace(Stops.F_COMBO, base(0.08801502, -0.63174605, 0.43921566), base(0.040395975, -0.6054113, 0.35686272),
                                     base(0.032459438, -0.5953556, 0.32549018), base(0.032459438, -0.5998577, 0.4352941)) ),

    COMBO_BOX_MOUSE_OVER( shade(BLUE_GREY, 0, 0, -0.22, -176),
            edge(Stops.E_SUNK, base(0.032459438, -0.54616207, -0.02352941), base(0.032459438, -0.41349208, -0.33725494)),
            comboFace(Stops.F_COMBO, base(0.08801502, -0.6317773, 0.4470588), base(0.032459438, -0.6113241, 0.41568625),
                                     base(0.032459438, -0.5985242, 0.39999998), base(0, -0.6357143, 0.45098037)) ),

    /** A combo box that is held down or has its list open. */
    COMBO_BOX_PRESSED( selectedLip(-83),
            edge(Stops.E_SUNK, base(0.08801502, 0.3642857, -0.52156866), base(0.032459438, -0.5246032, -0.12549022)),
            comboFace(Stops.F_COMBO, base(0.027408898, -0.5847884, 0.2980392), base(0.026611507, -0.53623784, 0.19999999),
                                     base(0.029681683, -0.52701867, 0.17254901), base(0.03801495, -0.5456242, 0.3215686)) ),

    COMBO_BOX_DISABLED( shade(BLUE_GREY, -0.6111111, -0.110526316, -0.74509805, -247),
            edge(Stops.E_SUNK, base(0.032459438, -0.5928571, 0.2745098), base(0.032459438, -0.590029, 0.2235294)),
            comboFace(Stops.F_COMBO, base(0.032459438, -0.60996324, 0.36470586), base(0.040395975, -0.60474086, 0.33725488),
                                     base(0.032459438, -0.5953556, 0.32549018), base(0.032459438, -0.5957143, 0.3333333)) ),

    /** The blue end of a combo box the arrow stands on, which is the default button's material. */
    COMBO_BOX_ACTUATOR( shade(BLUE_GREY, 0, 0, -0.22, -176),
            edge(Stops.E_SUNK, base(0.00051498413, -0.34585923, -0.007843137), base(0.00051498413, -0.095173776, -0.25882354)),
            comboFace(Stops.F_ACTUATOR, base(0.004681647, -0.6197143, 0.43137252), base(-0.0028941035, -0.4800539, 0.28235292),
                                        base(0.00051498413, -0.43866998, 0.24705881), base(0.00051498413, -0.4625541, 0.35686272)) ),

    COMBO_BOX_ACTUATOR_MOUSE_OVER( shade(BLUE_GREY, 0, 0, -0.22, -176),
            edge(Stops.E_SUNK, base(0.0013483167, -0.1769987, -0.12156865), base(0.059279382, 0.3642857, -0.43529415)),
            comboFace(Stops.F_ACTUATOR, base(0.004681647, -0.6198413, 0.43921566), base(-0.0008738637, -0.50527954, 0.35294116),
                                        base(0.00051498413, -0.4555341, 0.3215686), base(0.00051498413, -0.4625541, 0.35686272)) ),

    COMBO_BOX_ACTUATOR_PRESSED( selectedLip(-83),
            edge(Stops.E_SUNK, base(-0.57865167, -0.6357143, -0.54901963), base(-0.0000352859, 0.018606722, -0.23137257)),
            comboFace(Stops.F_ACTUATOR, base(-0.00042033195, -0.38050595, 0.20392156), base(0.0004081726, -0.12922078, 0.054901958),
                                        base(0, -0.00895375, 0.007843137), base(-0.0015907288, -0.1436508, 0.19215685)) ),

    COMBO_BOX_ACTUATOR_DISABLED( shade(BLUE_GREY, -0.6111111, -0.110526316, -0.74509805, -247),
            edge(Stops.E_SUNK, base(0.021348298, -0.56289876, 0.2588235), base(0.010237217, -0.55799407, 0.20784312)),
            comboFace(Stops.F_ACTUATOR, base(0.021348298, -0.59223604, 0.35294116), base(0.02391243, -0.5774183, 0.32549018),
                                        base(0.021348298, -0.56722116, 0.3098039), base(0.021348298, -0.567841, 0.31764704)) );

    /** Where the stops of the gradients sit. A nested class, because an enum's own constants are
     *  built before its static fields are. */
    private static final class Stops
    {
        static final double[] E_RESTING    = { 0.09, 0.52, 0.95 };
        static final double[] E_PRESSED    = { 0.05, 0.5, 0.95 };
        static final double[] E_SUNK       = { 0, 0.5, 1 };
        static final double[] F_RESTING    = { 0, 0.024, 0.06, 0.276, 0.6, 0.65, 0.7, 0.856, 0.96, 0.984, 1 };
        static final double[] F_MOUSE_OVER = { 0, 0.024, 0.06, 0.276, 0.6, 0.65, 0.7, 0.856, 0.96, 0.98, 1 };
        static final double[] F_FLAT       = { 0, 0.03, 0.06, 0.33, 0.6, 0.65, 0.7, 0.825, 0.95, 0.975, 1 };
        static final double[] F_SUNK       = { 0, 0.067, 0.134, 0.567, 1 };
        static final double[] F_COMBO      = { 0, 0.2, 0.401, 0.533, 0.665, 0.832, 1 };
        static final double[] F_ACTUATOR   = { 0, 0.172, 0.344, 0.482, 0.619, 0.81, 1 };
    }

    private final NimbusScheme.Shade    _lip;
    private final NimbusScheme.Gradient _edge;
    private final NimbusScheme.Gradient _face;

    NimbusMould( NimbusScheme.Shade lip, NimbusScheme.Gradient edge, NimbusScheme.Gradient face ) {
        _lip  = lip;
        _edge = edge;
        _face = face;
    }

    /** @return the pixel row below the control */
    NimbusScheme.Shade lip() { return _lip; }

    /** @return the whole shape of the control, of which only a one pixel outline stays visible */
    NimbusScheme.Gradient edge() { return _edge; }

    /** @return the inside of the control, one pixel in from its edge */
    NimbusScheme.Gradient face() { return _face; }

    /**
     *  Styles a component as this mould: a two pixel margin for the focus ring and the lip to live
     *  in, the edge filling the body, the face filling the interior one pixel in, and either the
     *  focus ring or the lip in the margin, never both.
     *
     * @param it the style being built
     * @param scheme the colours in force
     * @param arc how round the corners of the edge are, as the width of the arc a corner is cut
     *            along, which is how Nimbus writes it down; the face is cut along an arc two
     *            pixels narrower, which is the engine's own rule for an interior
     * @param tint a colour the application chose for this control, laid under the face the way
     *             Nimbus lays a button's own background under it; {@code null} for Nimbus's colours
     * @param focused whether to draw the focus ring in place of the lip
     * @param <C> the type of the component
     * @return the style
     */
    @SuppressWarnings("deprecation") // component() is the documented hook for LAF state reads
    <C extends JComponent> ComponentStyleDelegate<C> style(
        ComponentStyleDelegate<C> it, NimbusScheme scheme, double arc, @Nullable Color tint, boolean focused
    ) {
        NimbusRing ring = focused ? NimbusRing.aroundEdge(it.component(), scheme.get(NimbusScheme.Key.FOCUS), (float) arc)
                                  : NimbusRing.underEdge(it.component(), _lip.in(scheme), (float) arc);
        return it
                .margin(2)
                .borderRadius(arc)
                .border(1, SwingTreeLookAndFeel.Palette.TRANSPARENT)
                .backgroundColor(SwingTreeLookAndFeel.Palette.TRANSPARENT)
                .gradient(UI.Layer.BACKGROUND, "edge", g -> _edge.over(g, scheme, null)
                                                                .boundary(UI.ComponentBoundary.EXTERIOR_TO_BORDER)
                                                                .clipTo(UI.ComponentArea.BODY))
                .gradient(UI.Layer.BACKGROUND, "face", g -> _face.over(g, scheme, tint)
                                                                .boundary(UI.ComponentBoundary.BORDER_TO_INTERIOR)
                                                                .clipTo(UI.ComponentArea.INTERIOR))
                .painter(UI.Layer.BACKGROUND, UI.ComponentArea.ALL, "ring", ring);
    }

    // ── Shorthand for the tables above ───────────────────────────────────

    private static NimbusScheme.Shade bg( double hue, double saturation, double brightness ) {
        return shade(BLUE_GREY, hue, saturation, brightness);
    }

    private static NimbusScheme.Shade base( double hue, double saturation, double brightness ) {
        return shade(BASE, hue, saturation, brightness);
    }

    /** The shadow a standing control casts, at a given transparency. */
    private static NimbusScheme.Shade lip( int alpha ) {
        return shade(BLUE_GREY, -0.027777791, -0.06885965, -0.36862746, alpha);
    }

    /** The highlight along the bottom of a control sunk into the panel, at a given transparency. */
    private static NimbusScheme.Shade selectedLip( int alpha ) {
        return shade(BLUE_GREY, 0, -0.110526316, 0.25490195, alpha);
    }

    private static NimbusScheme.Gradient edge( double[] fractions, NimbusScheme.Shade top, NimbusScheme.Shade bottom ) {
        return gradient(fractions, top, MID, bottom);
    }

    /** Nimbus lays a face out of five colours, with the colour halfway between each pair inserted
     *  between them and the middle one held flat across the widest part of the control. */
    private static NimbusScheme.Gradient face(
        double[] fractions, NimbusScheme.Shade top, NimbusScheme.Shade upper, NimbusScheme.Shade middle,
        NimbusScheme.Shade lower, NimbusScheme.Shade bottom
    ) {
        return gradient(fractions, top, MID, upper, MID, middle, MID, middle, MID, lower, MID, bottom);
    }

    /** A combo box's face has four colours rather than five, the lower two a little closer together. */
    private static NimbusScheme.Gradient comboFace(
        double[] fractions, NimbusScheme.Shade top, NimbusScheme.Shade upper, NimbusScheme.Shade middle, NimbusScheme.Shade bottom
    ) {
        return gradient(fractions, top, MID, upper, MID, middle, MID, bottom);
    }

    /** A sunk face has no highlight at its bottom, only the three colours of its upper half. */
    private static NimbusScheme.Gradient sunkFace( NimbusScheme.Shade top, NimbusScheme.Shade middle, NimbusScheme.Shade bottom ) {
        return gradient(Stops.F_SUNK, top, MID, middle, MID, bottom);
    }

    private static NimbusScheme.Gradient mouseOverEdge() {
        return edge(Stops.E_RESTING, bg(0, -0.020974077, -0.21960783), bg(0, 0.11169591, -0.53333336));
    }
}
