package examples.laf;

import examples.laf.SwingTreeLookAndFeel.Palette;
import examples.laf.SwingTreeLookAndFeel.Surface;
import examples.laf.SwingTreeLookAndFeel.Variant;
import sprouts.Tuple;
import swingtree.UI;
import swingtree.style.ComponentStyleDelegate;

import javax.swing.AbstractButton;
import javax.swing.ButtonModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JProgressBar;
import javax.swing.JRadioButton;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.JToolTip;
import javax.swing.JTree;
import javax.swing.JViewport;
import javax.swing.table.JTableHeader;
import javax.swing.text.JTextComponent;
import java.awt.Color;
import java.awt.Container;

/**
 *  <b>Glassmorphism</b>: frosted panes floating over something vivid.
 *  <p>
 *  Nothing here is opaque. A surface is a wash of white at about a tenth of full strength, and what
 *  makes it read as glass rather than as a pale rectangle is that the window behind it is
 *  <em>blurred</em> where it shows through - which the style engine does with
 *  {@link ComponentStyleDelegate#parentFilter}, an actual convolution of the parent's rendering
 *  rather than a painted imitation of one. A hairline of brighter white along the edge is the pane
 *  catching the light on its bevel, and a wide soft shadow underneath is what says it is floating
 *  rather than lying flat.
 *  <p>
 *  The idiom only works if there is something worth blurring, so the window is not a colour but a
 *  gradient: an indigo ground with a violet and a magenta bloom washed diagonally across it. Pair
 *  the preset with a flat palette and the glass still behaves, it simply has nothing to show.
 *
 *  @see SwingTreeLookAndFeel.StylePreset#GLASSMORPHIC
 */
final class GlassmorphicPreset
{
    private GlassmorphicPreset() {}

    /** How far the frosting reaches into whatever is behind a pane. */
    private static final int FROST = 12;
    /** How much white a pane is washed with, out of 255. */
    private static final int PANE  = 34;
    /** The same for a pane you type into, which has to be darker than what it is cut out of. */
    private static final int WELL  = 52;
    /** How bright the bevel along a pane's edge is. */
    private static final int RIM   = 90;
    /** The radius every pane is cut to. */
    private static final int RADIUS = 16;
    /** How opaque a pane is when nothing behind it can be frosted, out of 255. */
    private static final int UNFROSTED_PANE = 232;

    private static final String DROP  = "drop";
    private static final String SHEEN = "sheen";
    private static final String BLOOM = "bloom";

    private static final Tuple<StyleRule> RULES = Tuple.of(
        StyleRule.of(JPanel.class,         GlassmorphicPreset::panel),
        StyleRule.of(AbstractButton.class, GlassmorphicPreset::button),
        StyleRule.of(JCheckBox.class,      GlassmorphicPreset::tickable),
        StyleRule.of(JRadioButton.class,   GlassmorphicPreset::tickable),
        StyleRule.of(JMenuItem.class,      GlassmorphicPreset::menuItem),
        StyleRule.of(JMenuBar.class,       GlassmorphicPreset::menuBar),
        StyleRule.of(JPopupMenu.class,     GlassmorphicPreset::popupMenu),
        StyleRule.of(JLabel.class,         GlassmorphicPreset::label),
        StyleRule.of(JTextField.class,     GlassmorphicPreset::field),
        StyleRule.of(JTextArea.class,      GlassmorphicPreset::page),
        StyleRule.of(JEditorPane.class,    GlassmorphicPreset::page),
        StyleRule.of(JSeparator.class,     GlassmorphicPreset::separator),
        StyleRule.of(JToolTip.class,       GlassmorphicPreset::toolTip),
        StyleRule.of(JProgressBar.class,   GlassmorphicPreset::progressBar),
        StyleRule.of(JSlider.class,        GlassmorphicPreset::bare),
        StyleRule.of(JScrollBar.class,     GlassmorphicPreset::scrollBar),
        StyleRule.of(JScrollPane.class,    GlassmorphicPreset::scrollPane),
        StyleRule.of(JViewport.class,      GlassmorphicPreset::bare),
        StyleRule.of(JComboBox.class,      GlassmorphicPreset::comboBox),
        StyleRule.of(JSpinner.class,       GlassmorphicPreset::spinner),
        StyleRule.of(JTabbedPane.class,    GlassmorphicPreset::bare),
        StyleRule.of(JList.class,          GlassmorphicPreset::bare),
        StyleRule.of(JTable.class,         GlassmorphicPreset::bare),
        StyleRule.of(JTableHeader.class,   GlassmorphicPreset::tableHeader),
        StyleRule.of(JTree.class,          GlassmorphicPreset::bare),
        StyleRule.of(JToolBar.class,       GlassmorphicPreset::toolBar),
        StyleRule.of(JSplitPane.class,     GlassmorphicPreset::bare)
    );

    /** @return the theme's style rules, one per component family. */
    static Tuple<StyleRule> rules() { return RULES; }

    // ── Glass ────────────────────────────────────────────────────────────

    /**
     *  Cuts a pane of frosted glass: a wash of white, the window behind it blurred where it shows
     *  through, a bright hairline along the edge and a wide soft shadow underneath.
     *
     * @param it    the delegate to style
     * @param wash  how much white the pane is tinted with, out of 255
     * @param lift  how far the pane floats above what is behind it
     * @param <C> the component type
     * @return the styled delegate
     */
    private static <C extends JComponent> ComponentStyleDelegate<C> pane(
        ComponentStyleDelegate<C> it, int wash, int lift
    ) {
        Palette p = SwingTreeLookAndFeel.palette();
        return it
                .backgroundColor(LafUtilities.withOpacity(p.surface(), wash))
                .parentFilter(f -> f.blur(FROST).area(UI.ComponentArea.BODY))
                .borderColor(LafUtilities.withOpacity(p.border(), RIM))
                // The light falls on the top left corner of a bevel and runs out well before the
                // opposite one, so the sheen is a short gradient rather than a fill.
                .gradient(SHEEN, g -> g
                        .span(UI.Span.TOP_LEFT_TO_BOTTOM_RIGHT)
                        .colors(LafUtilities.withOpacity(p.surface(), 46),
                                LafUtilities.withOpacity(p.surface(), 0))
                        .fractions(0, 0.55)
                        .clipTo(UI.ComponentArea.BODY))
                .shadow(DROP, s -> s.color(LafUtilities.withOpacity(Color.BLACK, 90))
                                    .offset(0, lift).blurRadius(lift * 2)
                                    .type(UI.ShadowType.BLUR).isInset(false));
    }

    // ── Surfaces ─────────────────────────────────────────────────────────

    @SuppressWarnings("deprecation") // component() is the documented hook for LAF state reads
    private static ComponentStyleDelegate<JPanel> panel( ComponentStyleDelegate<JPanel> it ) {
        Palette p = SwingTreeLookAndFeel.palette();
        it = it.foregroundColor(p.text());
        switch ( Surface.of(it.component()) ) {
            case CARD:        return pane(it.borderRadius(RADIUS).borderWidth(1).margin(7).padding(2), PANE, 6);
            case RAIL:        return pane(it.borderRadius(0).borderWidth(0), PANE / 2, 3);
            case TRANSPARENT: return it.backgroundColor(Palette.TRANSPARENT);
            case WINDOW:
            default:          return LafUtilities.isControlInternal(it.component()) || _standsOnTheGround(it.component())
                                        ? it.backgroundColor(Palette.TRANSPARENT)
                                        : aurora(it, p);
        }
    }

    /**
     *  The vivid ground the whole idiom depends on there being.
     *  <p>
     *  Only the outermost panel paints it. A gradient is laid out across the bounds of whatever
     *  draws it, so an untagged panel inside another untagged panel would start the whole sweep
     *  again inside its own corner of it.
     */
    private static <C extends JComponent> ComponentStyleDelegate<C> aurora( ComponentStyleDelegate<C> it, Palette p ) {
        return it
                .backgroundColor(p.background())
                .gradient(BLOOM, g -> g
                        .span(UI.Span.TOP_LEFT_TO_BOTTOM_RIGHT)
                        .colors(
                            LafUtilities.shadeTowards(p.background(), p.textureLight(), 0.55),
                            p.background(),
                            LafUtilities.shadeTowards(p.background(), p.textureDark(), 0.42),
                            p.background()
                        )
                        .fractions(0, 0.34, 0.68, 1)
                        .clipTo(UI.ComponentArea.BODY))
                // Blurring a smooth gradient gives back the same smooth gradient, so the frosting
                // would be invisible without something on the ground that has an edge to lose.
                .noise("aurora", n -> n
                        .function(UI.NoiseType.CLOUDS)
                        .colors(LafUtilities.withOpacity(p.textureLight(), 120),
                                LafUtilities.withOpacity(p.textureDark(), 90),
                                LafUtilities.withOpacity(p.background(), 0))
                        .scale(7)
                        .clipTo(UI.ComponentArea.BODY));
    }

    /** @return whether some panel further out has already painted the ground this one is on. */
    private static boolean _standsOnTheGround( JComponent component ) {
        Container parent = component.getParent();
        while ( parent != null ) {
            if ( parent instanceof JPanel && Surface.of((JPanel) parent) == Surface.WINDOW )
                return true;
            parent = parent.getParent();
        }
        return false;
    }

    @SuppressWarnings("deprecation")
    private static ComponentStyleDelegate<JScrollPane> scrollPane( ComponentStyleDelegate<JScrollPane> it ) {
        Palette p = SwingTreeLookAndFeel.palette();
        it = it.foregroundColor(p.text());
        switch ( Surface.of(it.component()) ) {
            case TRANSPARENT: return it.backgroundColor(Palette.TRANSPARENT).borderWidth(0).borderRadius(0).padding(0);
            case CARD:        return pane(it.borderRadius(RADIUS).borderWidth(1).margin(7).padding(3), PANE, 6);
            case RAIL:        return it.backgroundColor(Palette.TRANSPARENT).borderWidth(0).borderRadius(0).padding(0);
            case WINDOW:
            default:          return pane(it.borderRadius(RADIUS - 4).borderWidth(1).margin(4).padding(3), WELL, 3);
        }
    }

    // ── Controls ─────────────────────────────────────────────────────────

    @SuppressWarnings("deprecation")
    private static ComponentStyleDelegate<AbstractButton> button( ComponentStyleDelegate<AbstractButton> it ) {
        Palette        p = SwingTreeLookAndFeel.palette();
        AbstractButton b = it.component();
        ButtonModel    m = b.getModel();

        boolean enabled  = b.isEnabled();
        boolean pressed  = enabled && m.isArmed() && m.isPressed();
        boolean sunken   = pressed || ( enabled && m.isSelected() );
        boolean rollover = enabled && m.isRollover() && !pressed;
        boolean focused  = enabled && b.isFocusOwner();

        Variant variant = Variant.of(b);
        it = it
                .margin(4)
                .padding(8, 18, 8, 18)
                .borderRadius(RADIUS - 4)
                .borderWidth(1)
                .foregroundColor(ink(variant, p, enabled))
                .borderColor(LafUtilities.withOpacity(focused ? p.accent() : p.border(), focused ? 220 : RIM));

        if ( !enabled )
            return it.backgroundColor(LafUtilities.withOpacity(p.surfaceDisabled(), 30));
        if ( variant.isFilled() )
            // A tinted pane rather than a white one: the colour is what says which button this is,
            // and it still has to let the ground through or it stops being glass.
            return pane(it, PANE, sunken ? 2 : 5)
                    .backgroundColor(LafUtilities.withOpacity(tint(variant, p, sunken, rollover), 150));
        if ( variant == Variant.QUIET && !sunken && !rollover )
            return it.backgroundColor(Palette.TRANSPARENT).borderColor(Palette.TRANSPARENT);
        return pane(it, sunken ? WELL : rollover ? PANE + 22 : PANE, sunken ? 2 : 5);
    }

    @SuppressWarnings("deprecation")
    private static <C extends AbstractButton> ComponentStyleDelegate<C> tickable( ComponentStyleDelegate<C> it ) {
        Palette p = SwingTreeLookAndFeel.palette();
        return it
                .backgroundColor(Palette.TRANSPARENT)
                .foregroundColor(it.component().isEnabled() ? p.text() : p.textDisabled())
                .padding(2, 4, 2, 4);
    }

    @SuppressWarnings("deprecation")
    private static ComponentStyleDelegate<JComboBox> comboBox( ComponentStyleDelegate<JComboBox> it ) {
        JComboBox<?> combo = it.component();
        return frosted(it, combo.isEnabled(), LafUtilities.hasFocus(combo), 6, 10, 4);
    }

    @SuppressWarnings("deprecation")
    private static ComponentStyleDelegate<JSpinner> spinner( ComponentStyleDelegate<JSpinner> it ) {
        JSpinner spinner = it.component();
        return frosted(it, spinner.isEnabled(), LafUtilities.hasFocus(spinner), 4, 6, 4);
    }

    // ── Inputs ───────────────────────────────────────────────────────────

    @SuppressWarnings("deprecation")
    private static ComponentStyleDelegate<JTextField> field( ComponentStyleDelegate<JTextField> it ) {
        return input(it, it.component(), 7, 12);
    }

    @SuppressWarnings("deprecation")
    private static <C extends JTextComponent> ComponentStyleDelegate<C> page( ComponentStyleDelegate<C> it ) {
        return input(it, it.component(), 8, 12);
    }

    private static <C extends JComponent> ComponentStyleDelegate<C> input(
        ComponentStyleDelegate<C> it, JTextComponent text, int padY, int padX
    ) {
        Palette p        = SwingTreeLookAndFeel.palette();
        boolean editable = text.isEnabled() && text.isEditable();
        // Inside a scroll pane or a picker the pane has already been cut around it.
        if ( LafUtilities.isInsideAnotherControl(text) )
            return it
                    .margin(0).padding(padY, padX, padY, padX).borderWidth(0)
                    .backgroundColor(Palette.TRANSPARENT)
                    .foregroundColor(text.isEnabled() ? p.text() : p.textDisabled());
        return frosted(it, editable, editable && text.isFocusOwner(), padY, padX, padX)
                .foregroundColor(text.isEnabled() ? p.text() : p.textDisabled());
    }

    /** A pane you reach into: darker than the ones you only look at, so text stands off it. */
    private static <C extends JComponent> ComponentStyleDelegate<C> frosted(
        ComponentStyleDelegate<C> it, boolean enabled, boolean focused, int padY, int padX, int padRight
    ) {
        Palette p = SwingTreeLookAndFeel.palette();
        it = it
                .margin(3)
                .padding(padY, padRight, padY, padX)
                .borderRadius(RADIUS - 5)
                .borderWidth(focused ? 2 : 1)
                .foregroundColor(enabled ? p.text() : p.textDisabled());
        if ( !enabled )
            return it.backgroundColor(LafUtilities.withOpacity(p.surfaceDisabled(), 30))
                     .borderColor(LafUtilities.withOpacity(p.border(), 40));
        return pane(it, WELL, 2)
                .backgroundColor(LafUtilities.withOpacity(p.surfaceField(), WELL + 40))
                .borderColor(LafUtilities.withOpacity(focused ? p.accent() : p.border(), focused ? 220 : RIM));
    }

    // ── The rest ─────────────────────────────────────────────────────────

    @SuppressWarnings("deprecation")
    private static ComponentStyleDelegate<JLabel> label( ComponentStyleDelegate<JLabel> it ) {
        Palette p = SwingTreeLookAndFeel.palette();
        return it.foregroundColor(it.component().isEnabled() ? p.text() : p.textDisabled());
    }

    @SuppressWarnings("deprecation")
    private static ComponentStyleDelegate<JMenuItem> menuItem( ComponentStyleDelegate<JMenuItem> it ) {
        Palette     p       = SwingTreeLookAndFeel.palette();
        JMenuItem   item    = it.component();
        ButtonModel m       = item.getModel();
        boolean     enabled = item.isEnabled();
        boolean     armed   = enabled && ( m.isArmed() || m.isSelected() );
        return it
                .padding(6, 12, 6, 12)
                .borderRadius(RADIUS - 6)
                .borderWidth(0)
                .backgroundColor(armed ? LafUtilities.withOpacity(p.surface(), 62) : Palette.TRANSPARENT)
                .foregroundColor(enabled ? p.text() : p.textDisabled());
    }

    private static ComponentStyleDelegate<JMenuBar> menuBar( ComponentStyleDelegate<JMenuBar> it ) {
        Palette p = SwingTreeLookAndFeel.palette();
        return pane(it
                .foregroundColor(p.text())
                .padding(2, 4, 2, 4)
                .borderRadius(0)
                .borderWidth(0), PANE / 2, 3);
    }

    @SuppressWarnings("deprecation") // component() is the documented hook for LAF state reads
    private static ComponentStyleDelegate<JPopupMenu> popupMenu( ComponentStyleDelegate<JPopupMenu> it ) {
        Palette p = SwingTreeLookAndFeel.palette();
        return groundIfUnfrosted(pane(it
                .foregroundColor(p.text())
                .margin(7)
                .padding(5, 0, 5, 0)
                .borderRadius(RADIUS)
                .borderWidth(1), PANE + 30, 7), it.component(), PANE + 30);
    }

    @SuppressWarnings("deprecation") // component() is the documented hook for LAF state reads
    private static ComponentStyleDelegate<JToolTip> toolTip( ComponentStyleDelegate<JToolTip> it ) {
        Palette p = SwingTreeLookAndFeel.palette();
        return groundIfUnfrosted(pane(it
                .foregroundColor(p.text())
                .margin(5)
                .padding(5, 10, 5, 10)
                .borderRadius(RADIUS - 6)
                .borderWidth(1), PANE + 40, 5), it.component(), PANE + 40);
    }

    /**
     *  Repaints a popup's pane in a colour that does not need the frost, when the popup is in a
     *  per-pixel translucent window of its own.
     *  <p>
     *  Every other pane in this preset is frosted: {@code parentFilter} blurs the component behind
     *  it, and that blur is what separates the pane's text from whatever it covers. A popup which
     *  Swing had to put in a window of its own has no component behind it - the blur reads the
     *  parent's rendering, and the parent is that window's own empty content pane - so the frost
     *  is absent exactly where the pane is most transparent, and the menu text would stand on the
     *  bare desktop. The two opaque {@link SwingTreeLookAndFeel.PopupWindowMode}s need no repaint,
     *  because {@link SwingTreePopupFactory} fills their window with the palette ground.
     *  <p>
     *  Raising the wash alone would make this worse rather than better: the wash tints towards
     *  {@link SwingTreeLookAndFeel.Palette#surface()}, and on a palette whose text is lighter than
     *  its surface a thicker wash moves the pane towards the colour of its own letters. So the
     *  pane is mixed down onto {@link SwingTreeLookAndFeel.Palette#background()} first - which is
     *  the colour it would have been composited against inside the application window - and only
     *  then made nearly opaque. What the window's alpha still buys is the margin ring: the rounded
     *  corners stay antialiased and the drop shadow still falls on the desktop.
     *
     * @param it    the pane as {@link #pane} left it
     * @param popup the popup menu or tool tip being styled
     * @param wash  the wash {@link #pane} was given, out of 255
     * @param <C> the component type
     * @return the delegate, repainted only for a popup which cannot be frosted
     */
    private static <C extends JComponent> ComponentStyleDelegate<C> groundIfUnfrosted(
        ComponentStyleDelegate<C> it, C popup, int wash
    ) {
        if ( SwingTreeLookAndFeel.popupWindowModeOf(popup) != SwingTreeLookAndFeel.PopupWindowMode.TRANSLUCENT )
            return it;
        Palette p = SwingTreeLookAndFeel.palette();
        Color grounded = LafUtilities.shadeTowards(p.background(), p.surface(), wash / 255.0);
        return it.backgroundColor(LafUtilities.withOpacity(grounded, UNFROSTED_PANE));
    }

    /** The delegate draws the hairline itself, so the rule leaves the rest of the strip alone. */
    private static ComponentStyleDelegate<JSeparator> separator( ComponentStyleDelegate<JSeparator> it ) {
        Palette p = SwingTreeLookAndFeel.palette();
        return it.backgroundColor(Palette.TRANSPARENT)
                 .foregroundColor(LafUtilities.withOpacity(p.border(), 60));
    }

    private static ComponentStyleDelegate<JProgressBar> progressBar( ComponentStyleDelegate<JProgressBar> it ) {
        Palette p = SwingTreeLookAndFeel.palette();
        return it
                .margin(2)
                .borderRadius(6)
                .border(1, LafUtilities.withOpacity(p.border(), 50))
                .backgroundColor(LafUtilities.withOpacity(p.surfaceField(), WELL))
                .foregroundColor(p.accent());
    }

    private static ComponentStyleDelegate<JScrollBar> scrollBar( ComponentStyleDelegate<JScrollBar> it ) {
        Palette p = SwingTreeLookAndFeel.palette();
        return it.backgroundColor(Palette.TRANSPARENT).foregroundColor(p.border());
    }

    private static ComponentStyleDelegate<JTableHeader> tableHeader( ComponentStyleDelegate<JTableHeader> it ) {
        Palette p = SwingTreeLookAndFeel.palette();
        return it
                .backgroundColor(LafUtilities.withOpacity(p.surface(), 26))
                .foregroundColor(p.textMuted())
                .borderAt(UI.Edge.BOTTOM, 1, LafUtilities.withOpacity(p.border(), 60));
    }

    private static ComponentStyleDelegate<JToolBar> toolBar( ComponentStyleDelegate<JToolBar> it ) {
        Palette p = SwingTreeLookAndFeel.palette();
        return pane(it
                .foregroundColor(p.text())
                .margin(6)
                .padding(4, 8, 4, 8)
                .borderRadius(RADIUS)
                .borderWidth(1), PANE, 5);
    }

    /** Everything that is only the contents of a pane somebody else already cut. */
    private static <C extends JComponent> ComponentStyleDelegate<C> bare( ComponentStyleDelegate<C> it ) {
        return it.backgroundColor(Palette.TRANSPARENT).foregroundColor(SwingTreeLookAndFeel.palette().text());
    }

    // ── Variant colours ──────────────────────────────────────────────────

    private static Color tint( Variant variant, Palette p, boolean sunken, boolean rollover ) {
        switch ( variant ) {
            case DANGER: return sunken ? p.dangerPressed()  : rollover ? p.dangerHover()  : p.danger();
            case PRIMARY:
            default:     return sunken ? p.primaryPressed() : rollover ? p.primaryHover() : p.primary();
        }
    }

    private static Color ink( Variant variant, Palette p, boolean enabled ) {
        if ( !enabled )
            return p.textDisabled();
        return variant.isFilled() ? p.onFilled() : p.text();
    }
}
