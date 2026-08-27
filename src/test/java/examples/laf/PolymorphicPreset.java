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
 *  <b>Polymorphism</b>: a theme that has no fixed appearance, only rules for arriving at one.
 *  <p>
 *  Every other preset here decides what it looks like and then asks the palette for the colours to
 *  do it in. This one asks the other way round. It reads three things about the context it has been
 *  dropped into and derives the whole appearance from them:
 *  <ul>
 *      <li><b>What the palette leaves it to work with</b> ({@link Mood}). Given a palette whose
 *          ground and surfaces are the same colour it separates them with light; given a dark one
 *          it rims them; given a light one with contrast to spend it uses a flat fill and a
 *          shadow. Switching the palette therefore does not re-tint this theme, it
 *          <em>rewrites</em> it.</li>
 *      <li><b>How big the control is.</b> The radius is half the control's own height up to a
 *          limit, so a one-line control comes out as a pill and a tall one as a soft rectangle,
 *          without either being told which it is.</li>
 *      <li><b>How deeply it is nested.</b> A card lying on another card is lifted further than one
 *          lying on the window, because that is the only way the two are still told apart.</li>
 *  </ul>
 *  Two consequences worth knowing. The theme has no look of its own to show in a screenshot - it
 *  has to be seen in at least two palettes to be seen at all. And every rule reads the component,
 *  so nothing here can be decided ahead of time and cached.
 *
 *  @see SwingTreeLookAndFeel.StylePreset#POLYMORPHIC
 */
final class PolymorphicPreset
{
    private PolymorphicPreset() {}

    /** The largest radius a control is rounded to, however tall it grows. */
    private static final int MAX_RADIUS = 14;
    /** The radius used before a control has been laid out and has a height to derive one from. */
    private static final int UNMEASURED = 8;
    /** How far a surface rises for each surface it is standing on. */
    private static final int STEP = 3;
    /** How far the light moves a surface in a palette that has no contrast to separate it with. */
    private static final int LIGHT_STEP = 16;
    /** The same for the shadow opposite it, which is always the deeper of the two. */
    private static final int SHADE_STEP = 20;

    private static final String LIT   = "lit";
    private static final String DROP  = "drop";

    private static final Tuple<StyleRule> RULES = Tuple.of(
        StyleRule.of(JPanel.class,         PolymorphicPreset::panel),
        StyleRule.of(AbstractButton.class, PolymorphicPreset::button),
        StyleRule.of(JCheckBox.class,      PolymorphicPreset::tickable),
        StyleRule.of(JRadioButton.class,   PolymorphicPreset::tickable),
        StyleRule.of(JMenuItem.class,      PolymorphicPreset::menuItem),
        StyleRule.of(JMenuBar.class,       PolymorphicPreset::menuBar),
        StyleRule.of(JPopupMenu.class,     PolymorphicPreset::popupMenu),
        StyleRule.of(JLabel.class,         PolymorphicPreset::label),
        StyleRule.of(JTextField.class,     PolymorphicPreset::field),
        StyleRule.of(JTextArea.class,      PolymorphicPreset::page),
        StyleRule.of(JEditorPane.class,    PolymorphicPreset::page),
        StyleRule.of(JSeparator.class,     PolymorphicPreset::separator),
        StyleRule.of(JToolTip.class,       PolymorphicPreset::toolTip),
        StyleRule.of(JProgressBar.class,   PolymorphicPreset::progressBar),
        StyleRule.of(JSlider.class,        PolymorphicPreset::bare),
        StyleRule.of(JScrollBar.class,     PolymorphicPreset::scrollBar),
        StyleRule.of(JScrollPane.class,    PolymorphicPreset::scrollPane),
        StyleRule.of(JViewport.class,      PolymorphicPreset::bare),
        StyleRule.of(JComboBox.class,      PolymorphicPreset::comboBox),
        StyleRule.of(JSpinner.class,       PolymorphicPreset::spinner),
        StyleRule.of(JTabbedPane.class,    PolymorphicPreset::bare),
        StyleRule.of(JList.class,          PolymorphicPreset::bare),
        StyleRule.of(JTable.class,         PolymorphicPreset::bare),
        StyleRule.of(JTableHeader.class,   PolymorphicPreset::tableHeader),
        StyleRule.of(JTree.class,          PolymorphicPreset::bare),
        StyleRule.of(JToolBar.class,       PolymorphicPreset::toolBar),
        StyleRule.of(JSplitPane.class,     PolymorphicPreset::bare)
    );

    /** @return the theme's style rules, one per component family. */
    static Tuple<StyleRule> rules() { return RULES; }

    // ── What the context says ────────────────────────────────────────────

    /** @return half the control's own height, so that shape follows size rather than category. */
    private static int radiusOf( JComponent c ) {
        int height = c.getHeight();
        if ( height <= 0 )
            return UNMEASURED;
        return Math.max(3, Math.min(MAX_RADIUS, height / 2));
    }

    /** @return how many tagged surfaces a component is standing on, counted up to three. */
    private static int depthOf( JComponent c ) {
        int       depth  = 0;
        Container parent = c.getParent();
        while ( parent != null && depth < 3 ) {
            if ( parent instanceof JComponent && Surface.of((JComponent) parent) == Surface.CARD )
                depth++;
            parent = parent.getParent();
        }
        return depth;
    }

    /**
     *  Lifts a surface off whatever it is lying on, by whichever of the three means the palette
     *  has left available.
     *
     * @param it     the delegate to style
     * @param fill   the colour the surface is painted in
     * @param radius how far the surface's corners are rounded
     * @param lift   how far above its ground the surface sits
     * @param <C> the component type
     * @return the styled delegate
     */
    private static <C extends JComponent> ComponentStyleDelegate<C> lift(
        ComponentStyleDelegate<C> it, Color fill, int radius, int lift
    ) {
        Palette p   = SwingTreeLookAndFeel.palette();
        int     off = Math.max(1, lift / 2);
        it = it.backgroundColor(fill).borderRadius(radius);
        switch ( Mood.of(p) ) {
            case RELIEF:
                return it
                        .borderWidth(0)
                        .shadow(LIT,  s -> s.color(LafUtilities.shadeBySteps(p.background(), LIGHT_STEP))
                                            .offset(-off, -off).blurRadius(lift)
                                            .type(UI.ShadowType.GLOW).isInset(false))
                        .shadow(DROP, s -> s.color(LafUtilities.shadeBySteps(p.background(), -SHADE_STEP))
                                            .offset(off, off).blurRadius(lift)
                                            .type(UI.ShadowType.BLUR).isInset(false));
            case LUMINOUS:
                return it
                        .border(1, LafUtilities.shadeBySteps(fill, 26))
                        .shadow(DROP, s -> s.color(LafUtilities.withOpacity(Color.BLACK, 120))
                                            .offset(0, off).blurRadius(lift)
                                            .type(UI.ShadowType.BLUR).isInset(false));
            case SHEET:
            default:
                return it
                        .borderWidth(0)
                        .shadow(DROP, s -> s.color(LafUtilities.withOpacity(p.text(), 46))
                                            .offset(0, off).blurRadius(lift + 1).spreadRadius(-1)
                                            .type(UI.ShadowType.BLUR).isInset(false));
        }
    }

    /** The same three answers for a surface that has to read as something you reach into. */
    private static <C extends JComponent> ComponentStyleDelegate<C> recess(
        ComponentStyleDelegate<C> it, Color fill, int radius, boolean focused
    ) {
        Palette p = SwingTreeLookAndFeel.palette();
        it = it.backgroundColor(fill).borderRadius(radius);
        if ( Mood.of(p) == Mood.RELIEF )
            return it
                    .borderWidth(focused ? 2 : 0)
                    .borderColor(focused ? p.accent() : Palette.TRANSPARENT)
                    .shadow(DROP, s -> s.color(LafUtilities.shadeBySteps(p.background(), -SHADE_STEP / 2))
                                        .offset(3, 3).blurRadius(6)
                                        .type(UI.ShadowType.PENUMBRA).isInset(true))
                    .shadow(LIT,  s -> s.color(LafUtilities.shadeBySteps(p.background(), LIGHT_STEP / 2))
                                        .offset(-3, -3).blurRadius(6)
                                        .type(UI.ShadowType.PENUMBRA).isInset(true));
        return it.border(focused ? 2 : 1, focused ? p.accent() : p.border());
    }

    // ── Surfaces ─────────────────────────────────────────────────────────

    @SuppressWarnings("deprecation") // component() is the documented hook for LAF state reads
    private static ComponentStyleDelegate<JPanel> panel( ComponentStyleDelegate<JPanel> it ) {
        Palette p     = SwingTreeLookAndFeel.palette();
        JPanel  panel = it.component();
        it = it.foregroundColor(p.text());
        switch ( Surface.of(panel) ) {
            case CARD: {
                int lift = STEP + STEP * depthOf(panel);
                return lift(it.margin(lift).padding(2), p.surface(), MAX_RADIUS, lift);
            }
            case RAIL:        return it.backgroundColor(p.surface()).borderWidth(0);
            case TRANSPARENT: return it.backgroundColor(Palette.TRANSPARENT);
            case WINDOW:
            default:          return it.backgroundColor(
                                        LafUtilities.isControlInternal(panel) ? Palette.TRANSPARENT
                                                                              : p.background());
        }
    }

    @SuppressWarnings("deprecation")
    private static ComponentStyleDelegate<JScrollPane> scrollPane( ComponentStyleDelegate<JScrollPane> it ) {
        Palette     p    = SwingTreeLookAndFeel.palette();
        JScrollPane pane = it.component();
        it = it.foregroundColor(p.text());
        switch ( Surface.of(pane) ) {
            case TRANSPARENT: return it.backgroundColor(Palette.TRANSPARENT).borderWidth(0).borderRadius(0).padding(0);
            case CARD: {
                int lift = STEP + STEP * depthOf(pane);
                return lift(it.margin(lift).padding(3), p.surface(), MAX_RADIUS, lift);
            }
            case RAIL:        return it.backgroundColor(p.surface()).borderWidth(0).borderRadius(0).padding(0);
            case WINDOW:
            default:          return recess(it.margin(3).padding(3), p.surfaceField(), MAX_RADIUS - 4, false);
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
        int     radius  = radiusOf(b);
        Color   fill    = fill(variant, p, enabled, sunken, rollover);

        it = it
                .margin(4)
                .padding(7, 16, 7, 16)
                .borderRadius(radius)
                .borderWidth(focused ? 2 : 0)
                .borderColor(focused ? p.accent() : Palette.TRANSPARENT)
                .backgroundColor(fill)
                .foregroundColor(ink(variant, p, enabled));

        if ( !enabled || ( variant == Variant.QUIET && !sunken && !rollover ) )
            return it;
        if ( sunken )
            return recess(it, fill, radius, focused);
        return lift(it, fill, radius, rollover ? STEP + 2 : STEP);
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
        Palette      p     = SwingTreeLookAndFeel.palette();
        JComboBox<?> combo = it.component();
        boolean      on    = combo.isEnabled();
        it = it.margin(4).padding(6, 4, 6, 10)
               .foregroundColor(on ? p.text() : p.textDisabled());
        Color fill = on ? p.surface() : p.surfaceDisabled();
        return lift(it.borderWidth(0), fill, radiusOf(combo), STEP);
    }

    @SuppressWarnings("deprecation")
    private static ComponentStyleDelegate<JSpinner> spinner( ComponentStyleDelegate<JSpinner> it ) {
        Palette  p       = SwingTreeLookAndFeel.palette();
        JSpinner spinner = it.component();
        boolean  on      = spinner.isEnabled();
        it = it.margin(4).padding(4)
               .foregroundColor(on ? p.text() : p.textDisabled());
        Color fill = on ? p.surface() : p.surfaceDisabled();
        return lift(it.borderWidth(0), fill, radiusOf(spinner), STEP);
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
        // Inside a scroll pane or a picker, whatever that made of itself is the surface here.
        if ( LafUtilities.isInsideAnotherControl(text) )
            return it
                    .margin(0).padding(padY, padX, padY, padX).borderWidth(0)
                    .backgroundColor(Palette.TRANSPARENT)
                    .foregroundColor(text.isEnabled() ? p.text() : p.textDisabled());
        it = it.margin(4).padding(padY, padX, padY, padX)
               .foregroundColor(text.isEnabled() ? p.text() : p.textDisabled());
        return recess(it, editable ? p.surfaceField() : p.surfaceDisabled(),
                      radiusOf(text), editable && text.isFocusOwner());
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
                .borderRadius(radiusOf(item))
                .borderWidth(0)
                .backgroundColor(armed ? p.accentSoft() : Palette.TRANSPARENT)
                .foregroundColor(enabled ? p.text() : p.textDisabled());
    }

    private static ComponentStyleDelegate<JMenuBar> menuBar( ComponentStyleDelegate<JMenuBar> it ) {
        Palette p = SwingTreeLookAndFeel.palette();
        return it.backgroundColor(p.background()).foregroundColor(p.text()).padding(2, 4, 2, 4).borderWidth(0);
    }

    @SuppressWarnings("deprecation")
    private static ComponentStyleDelegate<JPopupMenu> popupMenu( ComponentStyleDelegate<JPopupMenu> it ) {
        Palette p = SwingTreeLookAndFeel.palette();
        return lift(it.foregroundColor(p.text()).margin(5).padding(5, 0, 5, 0),
                    p.surface(), MAX_RADIUS - 4, STEP + 3);
    }

    @SuppressWarnings("deprecation")
    private static ComponentStyleDelegate<JToolTip> toolTip( ComponentStyleDelegate<JToolTip> it ) {
        Palette p = SwingTreeLookAndFeel.palette();
        return lift(it.foregroundColor(p.text()).margin(4).padding(5, 10, 5, 10),
                    p.surface(), radiusOf(it.component()), STEP + 2);
    }

    /** The delegate draws the hairline itself, so the rule leaves the rest of the strip alone. */
    private static ComponentStyleDelegate<JSeparator> separator( ComponentStyleDelegate<JSeparator> it ) {
        Palette p = SwingTreeLookAndFeel.palette();
        return it.backgroundColor(Palette.TRANSPARENT).foregroundColor(p.borderSoft());
    }

    @SuppressWarnings("deprecation")
    private static ComponentStyleDelegate<JProgressBar> progressBar( ComponentStyleDelegate<JProgressBar> it ) {
        Palette      p   = SwingTreeLookAndFeel.palette();
        JProgressBar bar = it.component();
        return recess(it.margin(2).foregroundColor(p.accent()),
                      Mood.of(p) == Mood.RELIEF ? p.background() : p.accentSoft(), radiusOf(bar), false);
    }

    private static ComponentStyleDelegate<JScrollBar> scrollBar( ComponentStyleDelegate<JScrollBar> it ) {
        Palette p = SwingTreeLookAndFeel.palette();
        return it.backgroundColor(p.background()).foregroundColor(p.border());
    }

    private static ComponentStyleDelegate<JTableHeader> tableHeader( ComponentStyleDelegate<JTableHeader> it ) {
        Palette p = SwingTreeLookAndFeel.palette();
        return it
                .backgroundColor(Palette.TRANSPARENT)
                .foregroundColor(p.textMuted())
                .borderAt(UI.Edge.BOTTOM, 1, p.borderSoft());
    }

    @SuppressWarnings("deprecation")
    private static ComponentStyleDelegate<JToolBar> toolBar( ComponentStyleDelegate<JToolBar> it ) {
        Palette   p   = SwingTreeLookAndFeel.palette();
        JToolBar  bar = it.component();
        return lift(it.foregroundColor(p.text()).margin(5).padding(4, 8, 4, 8),
                    p.surface(), MAX_RADIUS, STEP + STEP * depthOf(bar));
    }

    /** Everything that is only the contents of a surface somebody else already made. */
    private static <C extends JComponent> ComponentStyleDelegate<C> bare( ComponentStyleDelegate<C> it ) {
        return it.backgroundColor(Palette.TRANSPARENT).foregroundColor(SwingTreeLookAndFeel.palette().text());
    }

    // ── Variant colours ──────────────────────────────────────────────────

    private static Color fill( Variant variant, Palette p, boolean enabled, boolean sunken, boolean rollover ) {
        if ( !enabled )
            return variant == Variant.QUIET ? Palette.TRANSPARENT : p.surfaceDisabled();
        switch ( variant ) {
            case PRIMARY: return sunken ? p.primaryPressed() : rollover ? p.primaryHover() : p.primary();
            case DANGER:  return sunken ? p.dangerPressed()  : rollover ? p.dangerHover()  : p.danger();
            case QUIET:   return sunken || rollover ? p.surface() : Palette.TRANSPARENT;
            case NEUTRAL:
            default:      return sunken ? p.surfacePressed() : rollover ? p.surfaceHover() : p.surface();
        }
    }

    private static Color ink( Variant variant, Palette p, boolean enabled ) {
        if ( !enabled )
            return p.textDisabled();
        return variant.isFilled() ? p.onFilled() : p.text();
    }
}
