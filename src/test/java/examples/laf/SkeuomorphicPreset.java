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
 *  <b>Skeuomorphism</b>: every control pretends to be made of something.
 *  <p>
 *  The window is a leather bench, cards are sheets of paper lying on it, and anything you can
 *  press is a milled metal plate screwed down onto the paper. Three things say so together, and
 *  none of them works without the other two: a grain, so the surface has a material; a vertical
 *  gradient, because a flat thing under a ceiling lamp is brightest at the top and darkest where
 *  it curves away at the bottom; and a bevel of one light pixel along the top edge and one dark
 *  one along the bottom, which is the plate's own thickness seen edge on.
 *  <p>
 *  Anything you type into is the opposite of a plate: a hole milled into the surface, dark along
 *  its top wall where the light cannot reach and bright along the bottom where it can. Pressing a
 *  plate turns it into exactly that hole, which is the whole trick the idiom is built on.
 *
 *  @see SwingTreeLookAndFeel.StylePreset#SKEUOMORPHIC
 */
final class SkeuomorphicPreset
{
    private SkeuomorphicPreset() {}

    /** How far the top of a lit plate sits above its nominal colour, and its bottom below. */
    private static final int SHEEN  = 16;
    /** The bevel: a plate is thick enough to catch light on one edge and hide it on the other. */
    private static final int BEVEL  = 26;
    /** The radius of a milled corner. Machined, not drawn: small and the same on everything. */
    private static final int RADIUS = 5;

    private static final String DROP  = "drop";
    private static final String WALL  = "wall";
    private static final String FLOOR = "floor";
    private static final String GRAIN = "grain";

    private static final Tuple<StyleRule> RULES = Tuple.of(
        StyleRule.of(JPanel.class,         SkeuomorphicPreset::panel),
        StyleRule.of(AbstractButton.class, SkeuomorphicPreset::button),
        StyleRule.of(JCheckBox.class,      SkeuomorphicPreset::tickable),
        StyleRule.of(JRadioButton.class,   SkeuomorphicPreset::tickable),
        StyleRule.of(JMenuItem.class,      SkeuomorphicPreset::menuItem),
        StyleRule.of(JMenuBar.class,       SkeuomorphicPreset::menuBar),
        StyleRule.of(JPopupMenu.class,     SkeuomorphicPreset::popupMenu),
        StyleRule.of(JLabel.class,         SkeuomorphicPreset::label),
        StyleRule.of(JTextField.class,     SkeuomorphicPreset::field),
        StyleRule.of(JTextArea.class,      SkeuomorphicPreset::page),
        StyleRule.of(JEditorPane.class,    SkeuomorphicPreset::page),
        StyleRule.of(JSeparator.class,     SkeuomorphicPreset::separator),
        StyleRule.of(JToolTip.class,       SkeuomorphicPreset::toolTip),
        StyleRule.of(JProgressBar.class,   SkeuomorphicPreset::progressBar),
        StyleRule.of(JSlider.class,        SkeuomorphicPreset::bare),
        StyleRule.of(JScrollBar.class,     SkeuomorphicPreset::scrollBar),
        StyleRule.of(JScrollPane.class,    SkeuomorphicPreset::scrollPane),
        StyleRule.of(JViewport.class,      SkeuomorphicPreset::viewport),
        StyleRule.of(JComboBox.class,      SkeuomorphicPreset::comboBox),
        StyleRule.of(JSpinner.class,       SkeuomorphicPreset::spinner),
        StyleRule.of(JTabbedPane.class,    SkeuomorphicPreset::tabbedPane),
        StyleRule.of(JList.class,          SkeuomorphicPreset::content),
        StyleRule.of(JTable.class,         SkeuomorphicPreset::content),
        StyleRule.of(JTableHeader.class,   SkeuomorphicPreset::tableHeader),
        StyleRule.of(JTree.class,          SkeuomorphicPreset::content),
        StyleRule.of(JToolBar.class,       SkeuomorphicPreset::toolBar),
        StyleRule.of(JSplitPane.class,     SkeuomorphicPreset::bare)
    );

    /** @return the theme's style rules, one per component family. */
    static Tuple<StyleRule> rules() { return RULES; }

    // ── Materials ────────────────────────────────────────────────────────

    /**
     *  Turns a flat fill into a milled plate: bright along the top, dark along the bottom, with a
     *  bevel of one pixel of each along the two edges and a machined grain across the face.
     *
     * @param it      the delegate to style
     * @param base    the colour the plate is nominally made of
     * @param pressed whether the plate has been pushed down into the surface it is screwed to
     * @param <C> the component type
     * @return the styled delegate
     */
    private static <C extends JComponent> ComponentStyleDelegate<C> plate(
        ComponentStyleDelegate<C> it, Color base, boolean pressed
    ) {
        int lift = pressed ? -SHEEN : SHEEN;
        it = it
                .backgroundColor(base)
                .gradient(g -> g
                        .span(UI.Span.TOP_TO_BOTTOM)
                        .colors(
                            LafUtilities.shadeBySteps(base, lift),
                            base,
                            LafUtilities.shadeBySteps(base, -lift)
                        )
                        .clipTo(UI.ComponentArea.BODY))
                .noise(GRAIN, n -> n
                        .function(UI.NoiseType.BRUSHED_METAL)
                        .colors(LafUtilities.withOpacity(Color.WHITE, 30),
                                LafUtilities.withOpacity(Color.BLACK, 22))
                        .scale(0.3, 5)
                        .clipTo(UI.ComponentArea.BODY))
                .borderColors(
                    LafUtilities.shadeBySteps(base, pressed ? -BEVEL : BEVEL),
                    LafUtilities.shadeBySteps(base, -BEVEL / 2),
                    LafUtilities.shadeBySteps(base, pressed ? BEVEL : -BEVEL),
                    LafUtilities.shadeBySteps(base, -BEVEL / 2)
                );
        if ( pressed )
            return it.shadow(WALL, s -> s.color(LafUtilities.withOpacity(Color.BLACK, 90))
                                         .offset(0, 2).blurRadius(3)
                                         .type(UI.ShadowType.PENUMBRA).isInset(true));
        return it.shadow(DROP, s -> s.color(LafUtilities.withOpacity(Color.BLACK, 60))
                                     .offset(0, 2).blurRadius(3)
                                     .type(UI.ShadowType.BLUR).isInset(false));
    }

    /**
     *  Turns a flat fill into a hole milled into whatever it sits in: its top wall in shadow, its
     *  floor catching the light that spills over the far lip.
     */
    private static <C extends JComponent> ComponentStyleDelegate<C> well(
        ComponentStyleDelegate<C> it, int depth
    ) {
        return it
                .shadow(WALL,  s -> s.color(LafUtilities.withOpacity(Color.BLACK, 105))
                                     .offset(0, depth).blurRadius(depth * 2)
                                     .type(UI.ShadowType.PENUMBRA).isInset(true))
                .shadow(FLOOR, s -> s.color(LafUtilities.withOpacity(Color.WHITE, 120))
                                     .offset(0, -depth).blurRadius(depth * 2)
                                     .type(UI.ShadowType.GLOW).isInset(true));
    }

    // ── Surfaces ─────────────────────────────────────────────────────────

    @SuppressWarnings("deprecation") // component() is the documented hook for LAF state reads
    private static ComponentStyleDelegate<JPanel> panel( ComponentStyleDelegate<JPanel> it ) {
        Palette p = SwingTreeLookAndFeel.palette();
        it = it.foregroundColor(p.text());
        switch ( Surface.of(it.component()) ) {
            case CARD:        return sheet(it, p).margin(5).padding(2);
            case RAIL:        return plate(it.borderRadius(0).border(1, p.border()), p.surface(), false);
            case TRANSPARENT: return it.backgroundColor(Palette.TRANSPARENT);
            case WINDOW:
            default:          return LafUtilities.isControlInternal(it.component())
                                        ? it.backgroundColor(Palette.TRANSPARENT)
                                        : leather(it, p);
        }
    }

    @SuppressWarnings("deprecation")
    private static ComponentStyleDelegate<JScrollPane> scrollPane( ComponentStyleDelegate<JScrollPane> it ) {
        Palette p = SwingTreeLookAndFeel.palette();
        it = it.foregroundColor(p.text());
        switch ( Surface.of(it.component()) ) {
            case TRANSPARENT: return it.backgroundColor(Palette.TRANSPARENT).borderWidth(0).borderRadius(0).padding(0);
            case CARD:        return sheet(it, p).margin(5).padding(3);
            case RAIL:        return it.backgroundColor(p.surface()).borderWidth(0).borderRadius(0).padding(0);
            case WINDOW:
            default:
                return well(it
                        .backgroundColor(p.surfaceField())
                        .borderRadius(RADIUS)
                        .border(1, p.border())
                        .margin(2)
                        .padding(2), 3);
        }
    }

    /** A viewport paints nothing: the well around it has already been milled into the card. */
    private static ComponentStyleDelegate<JViewport> viewport( ComponentStyleDelegate<JViewport> it ) {
        return it.backgroundColor(Palette.TRANSPARENT).foregroundColor(SwingTreeLookAndFeel.palette().text());
    }

    /** A sheet of paper: its own grain, a hairline edge and a shadow where it lifts off the bench. */
    private static <C extends JComponent> ComponentStyleDelegate<C> sheet( ComponentStyleDelegate<C> it, Palette p ) {
        return it
                .backgroundColor(p.surface())
                .borderRadius(RADIUS)
                .border(1, p.borderSoft())
                .noise(GRAIN, n -> n
                        .function(UI.NoiseType.PAPER)
                        .colors(LafUtilities.withOpacity(Color.WHITE, 40),
                                LafUtilities.withOpacity(p.textMuted(), 26))
                        .scale(1.4)
                        .clipTo(UI.ComponentArea.BODY))
                .shadow(DROP, s -> s.color(LafUtilities.withOpacity(Color.BLACK, 70))
                                    .offset(0, 3).blurRadius(6).spreadRadius(-1)
                                    .type(UI.ShadowType.BLUR).isInset(false));
    }

    /** The bench everything else lies on. */
    private static <C extends JComponent> ComponentStyleDelegate<C> leather( ComponentStyleDelegate<C> it, Palette p ) {
        return it
                .backgroundColor(p.background())
                .noise(GRAIN, n -> n
                        .function(UI.NoiseType.LEATHER)
                        .colors(p.textureLight(), p.textureDark())
                        .scale(0.3)
                        .clipTo(UI.ComponentArea.BODY));
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
        Color   base    = fill(variant, p, enabled, sunken, rollover);

        it = it
                .margin(3)
                .padding(7, 16, 7, 16)
                .borderRadius(RADIUS)
                .borderWidth(1)
                .backgroundColor(base)
                .foregroundColor(ink(variant, p, enabled));

        if ( focused )
            it = it.shadow("focus", s -> s.color(LafUtilities.withOpacity(p.accent(), 150))
                                          .blurRadius(4).spreadRadius(1)
                                          .type(UI.ShadowType.GLOW).isInset(false));
        if ( !enabled )
            return it.borderColor(p.border());
        if ( variant == Variant.QUIET && !sunken && !rollover )
            return it.borderColor(Palette.TRANSPARENT).backgroundColor(Palette.TRANSPARENT);
        return plate(it, base, sunken);
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
        return machined(it, p, combo.isEnabled(), LafUtilities.hasFocus(combo), 5, 10, 4);
    }

    @SuppressWarnings("deprecation")
    private static ComponentStyleDelegate<JSpinner> spinner( ComponentStyleDelegate<JSpinner> it ) {
        Palette  p       = SwingTreeLookAndFeel.palette();
        JSpinner spinner = it.component();
        return machined(it, p, spinner.isEnabled(), LafUtilities.hasFocus(spinner), 3, 6, 3);
    }

    /**
     *  A picker is milled like a hole rather than like a plate, and the stepper or drop-down
     *  button screwed into its right-hand end is what you actually press. It has to be a hole:
     *  Swing fills the strip a picker shows its value in with the {@code ComboBox.background}
     *  default rather than with the component's own colour, so a picker made of anything else
     *  would carry a rectangle of paper colour across its middle.
     */
    private static <C extends JComponent> ComponentStyleDelegate<C> machined(
        ComponentStyleDelegate<C> it, Palette p, boolean enabled, boolean focused,
        int padY, int padX, int padRight
    ) {
        it = it
                .margin(3)
                .padding(padY, padRight, padY, padX)
                .borderRadius(RADIUS)
                .border(1, focused ? p.accent() : p.border())
                .backgroundColor(enabled ? p.surfaceField() : p.surfaceDisabled())
                .foregroundColor(enabled ? p.text() : p.textDisabled());
        return enabled ? well(it, 3) : it;
    }

    // ── Inputs ───────────────────────────────────────────────────────────

    @SuppressWarnings("deprecation")
    private static ComponentStyleDelegate<JTextField> field( ComponentStyleDelegate<JTextField> it ) {
        return input(it, it.component(), 6, 10);
    }

    @SuppressWarnings("deprecation")
    private static <C extends JTextComponent> ComponentStyleDelegate<C> page( ComponentStyleDelegate<C> it ) {
        return input(it, it.component(), 8, 10);
    }

    private static <C extends JComponent> ComponentStyleDelegate<C> input(
        ComponentStyleDelegate<C> it, JTextComponent text, int padY, int padX
    ) {
        Palette p        = SwingTreeLookAndFeel.palette();
        boolean editable = text.isEnabled() && text.isEditable();
        boolean focused  = editable && text.isFocusOwner();
        // Inside a scroll pane or a picker the hole has already been milled around it.
        if ( LafUtilities.isInsideAnotherControl(text) )
            return it
                    .margin(0).padding(padY, padX, padY, padX).borderWidth(0)
                    .backgroundColor(Palette.TRANSPARENT)
                    .foregroundColor(text.isEnabled() ? p.text() : p.textDisabled());
        it = it
                .margin(3)
                .padding(padY, padX, padY, padX)
                .borderRadius(RADIUS)
                .border(1, focused ? p.accent() : p.border())
                .backgroundColor(editable ? p.surfaceField() : p.surfaceDisabled())
                .foregroundColor(text.isEnabled() ? p.text() : p.textDisabled());
        return well(it, 3);
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
        it = it
                .padding(5, 12, 5, 12)
                .borderRadius(3)
                .borderWidth(0)
                .backgroundColor(armed ? p.accent() : Palette.TRANSPARENT)
                .foregroundColor(!enabled ? p.textDisabled() : armed ? p.onFilled() : p.text());
        return armed ? plate(it, p.accent(), false) : it;
    }

    private static ComponentStyleDelegate<JMenuBar> menuBar( ComponentStyleDelegate<JMenuBar> it ) {
        Palette p = SwingTreeLookAndFeel.palette();
        return plate(it
                .foregroundColor(p.text())
                .padding(2, 4, 2, 4)
                .borderRadius(0)
                .borderWidth(1), p.surface(), false);
    }

    private static ComponentStyleDelegate<JPopupMenu> popupMenu( ComponentStyleDelegate<JPopupMenu> it ) {
        Palette p = SwingTreeLookAndFeel.palette();
        return sheet(it
                .foregroundColor(p.text())
                .margin(5)
                .padding(4, 0, 4, 0), p);
    }

    private static ComponentStyleDelegate<JToolTip> toolTip( ComponentStyleDelegate<JToolTip> it ) {
        Palette p = SwingTreeLookAndFeel.palette();
        return sheet(it
                .foregroundColor(p.text())
                .margin(4)
                .padding(4, 8, 4, 8), p);
    }

    /**
     *  An engraved line. The delegate draws the hairline itself, so all this adds is the lip of
     *  light just below it - and the rest of the strip is left alone, or a separator laid out
     *  taller than its hairline would come out as a solid brown band.
     */
    private static ComponentStyleDelegate<JSeparator> separator( ComponentStyleDelegate<JSeparator> it ) {
        Palette p = SwingTreeLookAndFeel.palette();
        return it
                .backgroundColor(Palette.TRANSPARENT)
                .foregroundColor(p.border())
                .shadow(FLOOR, s -> s.color(LafUtilities.withOpacity(Color.WHITE, 130))
                                     .offset(0, 1).blurRadius(0).isInset(false));
    }

    private static ComponentStyleDelegate<JProgressBar> progressBar( ComponentStyleDelegate<JProgressBar> it ) {
        Palette p = SwingTreeLookAndFeel.palette();
        return well(it
                .margin(2)
                .borderRadius(6)
                .border(1, p.border())
                .backgroundColor(p.surfaceDisabled())
                .foregroundColor(p.accent()), 2);
    }

    private static ComponentStyleDelegate<JScrollBar> scrollBar( ComponentStyleDelegate<JScrollBar> it ) {
        Palette p = SwingTreeLookAndFeel.palette();
        return well(it
                .backgroundColor(p.surfaceDisabled())
                .foregroundColor(p.border()), 2);
    }

    private static ComponentStyleDelegate<JTabbedPane> tabbedPane( ComponentStyleDelegate<JTabbedPane> it ) {
        Palette p = SwingTreeLookAndFeel.palette();
        return it.backgroundColor(Palette.TRANSPARENT).foregroundColor(p.text());
    }

    private static ComponentStyleDelegate<JTableHeader> tableHeader( ComponentStyleDelegate<JTableHeader> it ) {
        Palette p = SwingTreeLookAndFeel.palette();
        return plate(it
                .foregroundColor(p.textMuted())
                .borderRadius(0)
                .borderWidth(1), p.surface(), false);
    }

    private static ComponentStyleDelegate<JToolBar> toolBar( ComponentStyleDelegate<JToolBar> it ) {
        Palette p = SwingTreeLookAndFeel.palette();
        return plate(it
                .foregroundColor(p.text())
                .margin(4)
                .padding(4, 8, 4, 8)
                .borderRadius(RADIUS)
                .borderWidth(1), p.surface(), false);
    }

    /** A list, table or tree lies on the floor of the well the scroll pane milled for it. */
    private static <C extends JComponent> ComponentStyleDelegate<C> content( ComponentStyleDelegate<C> it ) {
        return it.backgroundColor(Palette.TRANSPARENT).foregroundColor(SwingTreeLookAndFeel.palette().text());
    }

    /** Structure with nothing of its own to paint: the symbol set draws all of it. */
    private static <C extends JComponent> ComponentStyleDelegate<C> bare( ComponentStyleDelegate<C> it ) {
        return it.backgroundColor(Palette.TRANSPARENT).foregroundColor(SwingTreeLookAndFeel.palette().text());
    }

    // ── Variant colours ──────────────────────────────────────────────────

    private static Color fill( Variant variant, Palette p, boolean enabled, boolean sunken, boolean rollover ) {
        if ( !enabled )
            return p.surfaceDisabled();
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
