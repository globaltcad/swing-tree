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

/**
 *  <b>Soft UI</b>, or neumorphism: a theme in which nothing has a colour of its own.
 *  <p>
 *  Every surface is the same colour as the window it sits on. What tells a button from the panel
 *  behind it is not a fill or a border but the <em>light</em>: a pale highlight up and to the left,
 *  a soft shadow down and to the right, and across the face between them a wash along the same
 *  diagonal, so the thing reads as moulded rather than as a rectangle with a halo. Press it and all
 *  three turn around and it sinks back in. Text inputs are sunken from the start, because a hole is
 *  what you type into.
 *  <p>
 *  Every one of those shades is computed from the palette by a fixed number of channel steps
 *  ({@link #LIGHT_STEP}), never named and never a fraction, which is what lets the same theme be
 *  lit on {@link Palettes#CLAY} and on a midnight blue without glaring on one of them. Each fade
 *  also follows the falloff curve of the thing it stands for rather than a straight ramp - see
 *  {@link #raised} and {@link #sunken} - and a shadow always reaches further than the highlight
 *  facing it ({@link #SHEEN_REACH}), because in a real room it does.
 *  <p>
 *  Borders are given up almost entirely, since an outline would do the job the light is there to
 *  do. The exception is focus, which cannot be said with a shadow a resting control already has -
 *  so a focused control grows an accent ring and gives the same amount back from its margin.
 *
 *  @see SwingTreeLookAndFeel.StylePreset#SOFT_UI
 */
final class SoftUiPreset
{
    private SoftUiPreset() {}

    /**
     *  How far the two sides of an extrusion move, in channel steps rather than as a fraction of
     *  the way to white or black.
     *  <p>
     *  A fraction is the wrong unit for light. The same 0.4 that lifts a pale clay grey by
     *  thirteen steps lifts a midnight blue by ninety, so a relief built out of fractions glares
     *  on a dark palette and washes out on a light one. A fixed step is what one lamp falling on
     *  one material actually does, and it is why this preset survives being paired with any
     *  {@link SwingTreeLookAndFeel.PalettePreset}.
     */
    private static final int LIGHT_STEP = 18;
    /** Slightly deeper than the highlight, so the effect reads as a room lit from one corner
     *  rather than as an outline drawn twice. */
    private static final int SHADE_STEP = 22;
    /**
     *  An inset groove is packed into the few pixels along an edge, where the same step that
     *  reads as a soft halo around a raised thing reads as a hard dark line pressed into a sunken
     *  one. Inset light is therefore dimmer than the light outside.
     */
    private static final double INSET_DIMMING = 0.55;
    /** How far the two ends of a surface's own diagonal wash sit from its fill. */
    private static final int CURVE_STEP = 5;
    /**
     *  How far a highlight reaches compared with the shadow opposite it.
     *  <p>
     *  In a real room the two are never the same size. A shadow is an umbra widened by the whole
     *  angular width of the light, so it spreads; a highlight is only the band where the surface
     *  has turned far enough to face the lamp, so it stays near the edge. Giving both the same
     *  reach is what makes a relief read as an outline drawn twice in two colours.
     */
    private static final double SHEEN_REACH = 0.75;

    /** The corner the light comes from, said once so every extrusion agrees. */
    private static final String LIT    = "lit";
    /** The opposite corner. */
    private static final String SHADED = "shaded";

    private static final Tuple<StyleRule> RULES = Tuple.of(
        StyleRule.of(JPanel.class,       SoftUiPreset::panel),
        StyleRule.of(AbstractButton.class, SoftUiPreset::button),
        StyleRule.of(JCheckBox.class,    SoftUiPreset::tickable),
        StyleRule.of(JRadioButton.class, SoftUiPreset::tickable),
        StyleRule.of(JMenuItem.class,    SoftUiPreset::menuItem),
        StyleRule.of(JMenuBar.class,     SoftUiPreset::menuBar),
        StyleRule.of(JPopupMenu.class,   SoftUiPreset::popupMenu),
        StyleRule.of(JLabel.class,       SoftUiPreset::label),
        StyleRule.of(JTextField.class,   SoftUiPreset::field),
        StyleRule.of(JTextArea.class,    SoftUiPreset::page),
        StyleRule.of(JEditorPane.class,  SoftUiPreset::page),
        StyleRule.of(JSeparator.class,   SoftUiPreset::separator),
        StyleRule.of(JToolTip.class,     SoftUiPreset::toolTip),
        StyleRule.of(JProgressBar.class, SoftUiPreset::progressBar),
        StyleRule.of(JSlider.class,      SoftUiPreset::slider),
        StyleRule.of(JScrollBar.class,   SoftUiPreset::scrollBar),
        StyleRule.of(JScrollPane.class,  SoftUiPreset::scrollPane),
        StyleRule.of(JViewport.class,    SoftUiPreset::viewport),
        StyleRule.of(JComboBox.class,    SoftUiPreset::comboBox),
        StyleRule.of(JSpinner.class,     SoftUiPreset::spinner),
        StyleRule.of(JTabbedPane.class,  SoftUiPreset::tabbedPane),
        StyleRule.of(JList.class,        SoftUiPreset::flatField),
        StyleRule.of(JTable.class,       SoftUiPreset::flatField),
        StyleRule.of(JTableHeader.class, SoftUiPreset::tableHeader),
        StyleRule.of(JTree.class,        SoftUiPreset::flatField),
        StyleRule.of(JToolBar.class,     SoftUiPreset::toolBar),
        StyleRule.of(JSplitPane.class,   SoftUiPreset::splitPane)
    );

    /** @return the theme's style rules, one per component family. */
    static Tuple<StyleRule> rules() { return RULES; }

    // ── The light ────────────────────────────────────────────────────────

    /** @return the surface as it looks where the light falls on it. */
    private static Color lit( Palette p ) { return LafUtilities.shadeBySteps(p.background(), LIGHT_STEP); }

    /** @return the surface as it looks in its own shadow. */
    private static Color shade( Palette p ) { return LafUtilities.shadeBySteps(p.background(), -SHADE_STEP); }

    /** @return {@link #lit} as it reads inside a groove. */
    private static Color litInside( Palette p ) {
        return LafUtilities.shadeBySteps(p.background(), (int) Math.round(LIGHT_STEP * INSET_DIMMING));
    }

    /** @return {@link #shade} as it reads inside a groove. */
    private static Color shadeInside( Palette p ) {
        return LafUtilities.shadeBySteps(p.background(), -(int) Math.round(SHADE_STEP * INSET_DIMMING));
    }

    /**
     *  Curves a flat fill. One light source falling on a rounded thing does not leave it one
     *  colour: the shoulder facing the lamp is brighter than the face, and the far edge is already
     *  turning away. A gentle wash along that diagonal is the whole difference between a control
     *  that looks moulded and one that looks like a coloured rectangle with a halo around it.
     *
     * @param it     the delegate to style
     * @param fill   the colour the surface is nominally painted in
     * @param inward whether the surface curves away from the light instead of towards it, which
     *               is what a pressed control does
     * @param <C> the component type
     * @return the styled delegate
     */
    private static <C extends JComponent> ComponentStyleDelegate<C> curved(
        ComponentStyleDelegate<C> it, Color fill, boolean inward
    ) {
        int step = inward ? -CURVE_STEP : CURVE_STEP;
        // Most of a rounded rectangle's face points straight at the viewer and is therefore one
        // even colour; only near the two shoulders does it turn far enough to catch or lose the
        // light. So the wash is a plateau with a roll-off at each end, not a constant slope,
        // which is the difference between a curved face and a flat one held at an angle.
        return it.gradient( g -> g
                .span(UI.Span.TOP_LEFT_TO_BOTTOM_RIGHT)
                .fractions(0, 0.38, 0.62, 1)
                .colors(
                    LafUtilities.shadeBySteps(fill, step),
                    fill,
                    fill,
                    LafUtilities.shadeBySteps(fill, -step)
                )
        );
    }

    /**
     *  Extrudes a surface out of the panel behind it: light from the top left, shadow to the
     *  bottom right, both outside the shape.
     *  <p>
     *  Each of the two fades along the curve that matches what it is. The shadow uses
     *  {@link UI.ShadowType#BLUR}, the profile a hard edge takes on when it is convolved with a
     *  blur, so it leaves the shape at full strength and arrives at nothing with no slope at
     *  either end - which is what stops the surface looking as though it steps up out of the
     *  panel rather than swelling out of it. The highlight uses {@link UI.ShadowType#GLOW}, a
     *  bell, because a sheen is light bleeding off a shoulder and not an edge being cast.
     *  <p>
     *  An outer shadow is drawn in the component's own margin and is cut off at the component
     *  bounds, so {@code reach} - the offset and the blur together - must never exceed the margin
     *  the caller set, or the soft glow ends in a hard rectangular edge.
     */
    private static <C extends JComponent> ComponentStyleDelegate<C> raised(
        ComponentStyleDelegate<C> it, Palette p, int reach
    ) {
        int sheen = Math.max(2, (int) Math.round(reach * SHEEN_REACH));
        return it
                .shadow(LIT,    s -> s.color(lit(p))
                                      .offset(-offsetOf(sheen), -offsetOf(sheen)).blurRadius(blurOf(sheen))
                                      .type(UI.ShadowType.GLOW).isInset(false))
                .shadow(SHADED, s -> s.color(shade(p))
                                      .offset(offsetOf(reach), offsetOf(reach)).blurRadius(blurOf(reach))
                                      .type(UI.ShadowType.BLUR).isInset(false));
    }

    /** How far a shadow of the given reach is displaced from the shape casting it. */
    private static int offsetOf( int reach ) { return Math.max(1, reach / 3); }

    /** How much of a shadow of the given reach is spent fading out. */
    private static int blurOf( int reach ) { return Math.max(1, reach - offsetOf(reach)); }

    /**
     *  The same light, turned around and moved inside, which is how a control says it is pressed
     *  and how an input says it is a hole.
     *  <p>
     *  Nothing is cast inside a groove: both sides of it are the same wall, turning towards the
     *  light on one edge and away from it on the other. So both fades take the same symmetric
     *  {@link UI.ShadowType#PENUMBRA} S-curve, and the wall rounds off into the floor instead of
     *  meeting it at a line.
     */
    private static <C extends JComponent> ComponentStyleDelegate<C> sunken(
        ComponentStyleDelegate<C> it, Palette p, int depth
    ) {
        return it
                .shadow(SHADED, s -> s.color(shadeInside(p))
                                      .offset(depth, depth).blurRadius(depth * 2)
                                      .type(UI.ShadowType.PENUMBRA).isInset(true))
                .shadow(LIT,    s -> s.color(litInside(p))
                                      .offset(-depth, -depth).blurRadius(depth * 2)
                                      .type(UI.ShadowType.PENUMBRA).isInset(true));
    }

    // ── Surfaces ─────────────────────────────────────────────────────────

    @SuppressWarnings("deprecation") // component() is the documented hook for LAF state reads
    private static ComponentStyleDelegate<JPanel> panel( ComponentStyleDelegate<JPanel> it ) {
        Palette p = SwingTreeLookAndFeel.palette();
        it = it.foregroundColor(p.text());
        switch ( Surface.of(it.component()) ) {
            case CARD:
                return raised(curved(it.backgroundColor(p.surface()), p.surface(), false)
                                .borderRadius(22).borderWidth(0).margin(8), p, 8);
            case RAIL:
                return it.backgroundColor(p.surface()).borderWidth(0);
            case TRANSPARENT:
                return it.backgroundColor(Palette.TRANSPARENT);
            case WINDOW:
            default:
                return it.backgroundColor(p.background());
        }
    }

    @SuppressWarnings("deprecation")
    private static ComponentStyleDelegate<JScrollPane> scrollPane( ComponentStyleDelegate<JScrollPane> it ) {
        Palette p = SwingTreeLookAndFeel.palette();
        it = it.foregroundColor(p.text());
        switch ( Surface.of(it.component()) ) {
            case TRANSPARENT:
                return it.backgroundColor(Palette.TRANSPARENT).borderWidth(0).borderRadius(0).padding(0);
            case CARD:
                return raised(curved(it.backgroundColor(p.surface()), p.surface(), false)
                                .borderRadius(20).borderWidth(0).margin(6).padding(4), p, 6);
            case RAIL:
                return it.backgroundColor(p.surface()).borderWidth(0).borderRadius(0).padding(0);
            case WINDOW:
            default:
                return sunken(it.backgroundColor(p.surfaceField()).borderRadius(18).borderWidth(0).margin(5).padding(4), p, 4);
        }
    }

    /**
     *  A viewport paints nothing of its own. The scroll pane around it has already painted the
     *  surface, and in the sunken case the groove pressed into that surface as well - a viewport
     *  filling the same colour over the top would erase exactly the edges the groove is made of.
     */
    private static ComponentStyleDelegate<JViewport> viewport( ComponentStyleDelegate<JViewport> it ) {
        return it.backgroundColor(Palette.TRANSPARENT).foregroundColor(SwingTreeLookAndFeel.palette().text());
    }

    // ── Controls ─────────────────────────────────────────────────────────

    @SuppressWarnings("deprecation")
    private static ComponentStyleDelegate<AbstractButton> button( ComponentStyleDelegate<AbstractButton> it ) {
        Palette        p = SwingTreeLookAndFeel.palette();
        AbstractButton b = it.component();
        ButtonModel    m = b.getModel();

        boolean enabled  = b.isEnabled();
        boolean pressed  = enabled && m.isArmed() && m.isPressed();
        boolean selected = enabled && m.isSelected();
        boolean sunkenIn = pressed || selected;
        boolean rollover = enabled && m.isRollover() && !pressed;
        boolean focused  = enabled && b.isFocusOwner();

        Variant variant = Variant.of(b);

        // The margin is where the light and the shadow live, so it is never zero; a focus ring
        // is grown out of it rather than added to the footprint.
        int lift = focused ? 5 : 7;

        Color surface = fill(variant, p, enabled, sunkenIn, rollover);

        it = it
                .margin(lift)
                .padding(9, 20, 9, 20)
                .borderRadius(16)
                .borderWidth(focused ? 2 : 0)
                .borderColor(focused ? p.accent() : Palette.TRANSPARENT)
                .backgroundColor(surface)
                .foregroundColor(ink(variant, p, enabled));

        if ( !enabled )
            return it;
        if ( variant == Variant.QUIET && !sunkenIn && !rollover )
            return it; // lies flat in the panel until it is reached for
        if ( sunkenIn )
            return sunken(curved(it, surface, true), p, 3);
        return raised(curved(it, surface, false), p, rollover ? lift : lift - 2);
    }

    @SuppressWarnings("deprecation")
    private static <C extends AbstractButton> ComponentStyleDelegate<C> tickable( ComponentStyleDelegate<C> it ) {
        Palette p = SwingTreeLookAndFeel.palette();
        return it
                .backgroundColor(Palette.TRANSPARENT)
                .foregroundColor(it.component().isEnabled() ? p.text() : p.textDisabled())
                .padding(3, 5, 3, 5);
    }

    @SuppressWarnings("deprecation")
    private static ComponentStyleDelegate<JComboBox> comboBox( ComponentStyleDelegate<JComboBox> it ) {
        Palette      p       = SwingTreeLookAndFeel.palette();
        JComboBox<?> combo   = it.component();
        boolean      enabled = combo.isEnabled();
        boolean      focused = enabled && LafUtilities.hasFocus(combo);
        int lift = focused ? 4 : 6;
        it = it
                .margin(lift)
                .padding(6, 10, 6, 6)
                .borderRadius(14)
                .borderWidth(focused ? 2 : 0)
                .borderColor(focused ? p.accent() : Palette.TRANSPARENT)
                .backgroundColor(enabled ? p.surface() : p.surfaceDisabled())
                .foregroundColor(enabled ? p.text() : p.textDisabled());
        if ( !enabled )
            return it;
        return raised(curved(it, p.surface(), false), p, lift);
    }

    @SuppressWarnings("deprecation")
    private static ComponentStyleDelegate<JSpinner> spinner( ComponentStyleDelegate<JSpinner> it ) {
        Palette  p       = SwingTreeLookAndFeel.palette();
        JSpinner spinner = it.component();
        boolean  enabled = spinner.isEnabled();
        boolean  focused = enabled && LafUtilities.hasFocus(spinner);
        int lift = focused ? 4 : 6;
        it = it
                .margin(lift)
                .padding(4)
                .borderRadius(14)
                .borderWidth(focused ? 2 : 0)
                .borderColor(focused ? p.accent() : Palette.TRANSPARENT)
                .backgroundColor(enabled ? p.surface() : p.surfaceDisabled())
                .foregroundColor(enabled ? p.text() : p.textDisabled());
        if ( !enabled )
            return it;
        return raised(curved(it, p.surface(), false), p, lift);
    }

    // ── Inputs ───────────────────────────────────────────────────────────

    @SuppressWarnings("deprecation")
    private static ComponentStyleDelegate<JTextField> field( ComponentStyleDelegate<JTextField> it ) {
        return input(it, it.component(), 7, 14);
    }

    @SuppressWarnings("deprecation")
    private static <C extends JTextComponent> ComponentStyleDelegate<C> page( ComponentStyleDelegate<C> it ) {
        return input(it, it.component(), 9, 14);
    }

    /**
     *  An input is a hole: the light is on the inside, and focus lights its rim.
     *  <p>
     *  Unless it is scrolled, in which case the hole belongs to the scroll pane around it and this
     *  is only the paper inside. A second sunken box here would fill over the groove the scroll
     *  pane just pressed into its own surface, and leave the document sitting on a hard edge.
     */
    private static <C extends JComponent> ComponentStyleDelegate<C> input(
        ComponentStyleDelegate<C> it, JTextComponent text, int padY, int padX
    ) {
        Palette p        = SwingTreeLookAndFeel.palette();
        boolean editable = text.isEnabled() && text.isEditable();
        boolean focused  = editable && text.isFocusOwner();

        if ( text.getParent() instanceof JViewport )
            return it
                    .margin(0)
                    .padding(padY, padX, padY, padX)
                    .borderWidth(0)
                    .backgroundColor(Palette.TRANSPARENT)
                    .foregroundColor(text.isEnabled() ? p.text() : p.textDisabled());

        it = it
                .margin(focused ? 3 : 5)
                .padding(padY, padX, padY, padX)
                .borderRadius(14)
                .borderWidth(focused ? 2 : 0)
                .borderColor(focused ? p.accent() : Palette.TRANSPARENT)
                .backgroundColor(editable ? p.surfaceField() : p.surfaceDisabled())
                .foregroundColor(text.isEnabled() ? p.text() : p.textDisabled());
        return sunken(it, p, 4);
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
                .padding(5, 10, 5, 10)
                .borderRadius(12)
                .borderWidth(0)
                .backgroundColor(armed ? p.accentSoft() : Palette.TRANSPARENT)
                .foregroundColor(enabled ? p.text() : p.textDisabled());
    }

    private static ComponentStyleDelegate<JMenuBar> menuBar( ComponentStyleDelegate<JMenuBar> it ) {
        Palette p = SwingTreeLookAndFeel.palette();
        return it.backgroundColor(p.background()).foregroundColor(p.text()).padding(3, 6, 3, 6).borderWidth(0);
    }

    private static ComponentStyleDelegate<JPopupMenu> popupMenu( ComponentStyleDelegate<JPopupMenu> it ) {
        Palette p = SwingTreeLookAndFeel.palette();
        return raised(curved(it
                .backgroundColor(p.surface())
                .foregroundColor(p.text())
                .margin(6)
                .padding(6)
                .borderRadius(18)
                .borderWidth(0), p.surface(), false), p, 6);
    }

    private static ComponentStyleDelegate<JToolTip> toolTip( ComponentStyleDelegate<JToolTip> it ) {
        Palette p = SwingTreeLookAndFeel.palette();
        return raised(curved(it
                .margin(5)
                .padding(5, 12, 5, 12)
                .borderRadius(14)
                .borderWidth(0)
                .backgroundColor(p.surface())
                .foregroundColor(p.text()), p.surface(), false), p, 5);
    }

    private static ComponentStyleDelegate<JSeparator> separator( ComponentStyleDelegate<JSeparator> it ) {
        Palette p = SwingTreeLookAndFeel.palette();
        return it.backgroundColor(p.borderSoft()).foregroundColor(p.borderSoft());
    }

    /** The trough is a groove pressed into the panel; the symbol set fills it. */
    private static ComponentStyleDelegate<JProgressBar> progressBar( ComponentStyleDelegate<JProgressBar> it ) {
        Palette p = SwingTreeLookAndFeel.palette();
        return sunken(it
                .margin(3)
                .borderRadius(8)
                .borderWidth(0)
                .backgroundColor(p.background())
                .foregroundColor(p.accent()), p, 3);
    }

    private static ComponentStyleDelegate<JSlider> slider( ComponentStyleDelegate<JSlider> it ) {
        Palette p = SwingTreeLookAndFeel.palette();
        return it.backgroundColor(Palette.TRANSPARENT).foregroundColor(p.text());
    }

    private static ComponentStyleDelegate<JScrollBar> scrollBar( ComponentStyleDelegate<JScrollBar> it ) {
        Palette p = SwingTreeLookAndFeel.palette();
        return it.backgroundColor(p.background()).foregroundColor(p.border());
    }

    private static ComponentStyleDelegate<JTabbedPane> tabbedPane( ComponentStyleDelegate<JTabbedPane> it ) {
        Palette p = SwingTreeLookAndFeel.palette();
        return it.backgroundColor(p.background()).foregroundColor(p.text());
    }

    private static ComponentStyleDelegate<JSplitPane> splitPane( ComponentStyleDelegate<JSplitPane> it ) {
        Palette p = SwingTreeLookAndFeel.palette();
        return it.backgroundColor(Palette.TRANSPARENT).foregroundColor(p.text());
    }

    /**
     *  A list, table or tree is the content of a hole, not a surface of its own: it lets the
     *  scroll pane's field colour and groove through and paints only its rows.
     */
    private static <C extends JComponent> ComponentStyleDelegate<C> flatField( ComponentStyleDelegate<C> it ) {
        return it.backgroundColor(Palette.TRANSPARENT).foregroundColor(SwingTreeLookAndFeel.palette().text());
    }

    private static ComponentStyleDelegate<JTableHeader> tableHeader( ComponentStyleDelegate<JTableHeader> it ) {
        Palette p = SwingTreeLookAndFeel.palette();
        return it.backgroundColor(p.surface()).foregroundColor(p.textMuted());
    }

    private static ComponentStyleDelegate<JToolBar> toolBar( ComponentStyleDelegate<JToolBar> it ) {
        Palette p = SwingTreeLookAndFeel.palette();
        return raised(curved(it
                .backgroundColor(p.surface())
                .foregroundColor(p.text())
                .margin(6)
                .padding(6, 10, 6, 10)
                .borderRadius(18)
                .borderWidth(0), p.surface(), false), p, 6);
    }

    // ── Variant colours ──────────────────────────────────────────────────

    private static Color fill( Variant variant, Palette p, boolean enabled, boolean sunkenIn, boolean rollover ) {
        if ( !enabled )
            return variant == Variant.QUIET ? Palette.TRANSPARENT : p.surfaceDisabled();
        switch ( variant ) {
            case PRIMARY: return sunkenIn ? p.primaryPressed() : rollover ? p.primaryHover() : p.primary();
            case DANGER:  return sunkenIn ? p.dangerPressed()  : rollover ? p.dangerHover()  : p.danger();
            case QUIET:   return sunkenIn || rollover ? p.surface() : Palette.TRANSPARENT;
            case NEUTRAL:
            default:      return sunkenIn ? p.surfacePressed() : rollover ? p.surfaceHover() : p.surface();
        }
    }

    private static Color ink( Variant variant, Palette p, boolean enabled ) {
        if ( !enabled )
            return p.textDisabled();
        return variant.isFilled() ? p.onFilled() : p.text();
    }
}
