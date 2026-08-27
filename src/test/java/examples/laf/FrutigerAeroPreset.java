package examples.laf;

import examples.laf.SwingTreeLookAndFeel.Palette;
import examples.laf.SwingTreeLookAndFeel.Surface;
import examples.laf.SwingTreeLookAndFeel.Variant;
import sprouts.Tuple;
import swingtree.UI;
import swingtree.style.ComponentStyleDelegate;
import swingtree.style.GradientConf;

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
 *  <b>Frutiger Aero</b>: the wet, glassy optimism of software from about 2004 to 2012.
 *  <p>
 *  Everything looks like it was moulded out of coloured glass and then rained on. The defining
 *  move is the <em>gloss</em>: a fill noticeably lighter across its top half, breaking on a hard
 *  line at the middle and continuing darker below, so the surface reads as curved rather than
 *  flat. Around it go a crisp one-pixel outline a few shades darker than the fill, a generous
 *  radius, and a small drop shadow. The page itself is a sky - a soft vertical gradient. Pressing
 *  something turns the gloss upside down and sinks the shadow inward.
 *  <p>
 *  The idiom needs a colour it can put a highlight on, which is what {@link Palettes#AERO}
 *  supplies. A flatter palette still works, it is simply quieter: all four stops of every gradient
 *  are derived from the palette, never named.
 *
 *  @see SwingTreeLookAndFeel.StylePreset#FRUTIGER_AERO
 */
final class FrutigerAeroPreset
{
    private FrutigerAeroPreset() {}

    /** Where the gloss breaks, as a fraction of the height. Just above the middle, which is what
     *  makes the highlight read as a reflection rather than as a two-tone paint job. */
    private static final double BREAK = 0.48;
    /** The far side of that break. A gradient's stops have to be strictly increasing, so the hard
     *  line the idiom is built on is the smallest step there is rather than no step at all. */
    private static final double BREAK_END = 0.482;

    private static final Tuple<StyleRule> RULES = Tuple.of(
        StyleRule.of(JPanel.class,         FrutigerAeroPreset::panel),
        StyleRule.of(AbstractButton.class, FrutigerAeroPreset::button),
        StyleRule.of(JCheckBox.class,      FrutigerAeroPreset::tickable),
        StyleRule.of(JRadioButton.class,   FrutigerAeroPreset::tickable),
        StyleRule.of(JMenuItem.class,      FrutigerAeroPreset::menuItem),
        StyleRule.of(JMenuBar.class,       FrutigerAeroPreset::menuBar),
        StyleRule.of(JPopupMenu.class,     FrutigerAeroPreset::popupMenu),
        StyleRule.of(JLabel.class,         FrutigerAeroPreset::label),
        StyleRule.of(JTextField.class,     FrutigerAeroPreset::field),
        StyleRule.of(JTextArea.class,      FrutigerAeroPreset::page),
        StyleRule.of(JEditorPane.class,    FrutigerAeroPreset::page),
        StyleRule.of(JSeparator.class,     FrutigerAeroPreset::separator),
        StyleRule.of(JToolTip.class,       FrutigerAeroPreset::toolTip),
        StyleRule.of(JProgressBar.class,   FrutigerAeroPreset::progressBar),
        StyleRule.of(JSlider.class,        FrutigerAeroPreset::slider),
        StyleRule.of(JScrollBar.class,     FrutigerAeroPreset::scrollBar),
        StyleRule.of(JScrollPane.class,    FrutigerAeroPreset::scrollPane),
        StyleRule.of(JViewport.class,      FrutigerAeroPreset::viewport),
        StyleRule.of(JComboBox.class,      FrutigerAeroPreset::comboBox),
        StyleRule.of(JSpinner.class,       FrutigerAeroPreset::spinner),
        StyleRule.of(JTabbedPane.class,    FrutigerAeroPreset::tabbedPane),
        StyleRule.of(JList.class,          FrutigerAeroPreset::flatField),
        StyleRule.of(JTable.class,         FrutigerAeroPreset::flatField),
        StyleRule.of(JTableHeader.class,   FrutigerAeroPreset::tableHeader),
        StyleRule.of(JTree.class,          FrutigerAeroPreset::flatField),
        StyleRule.of(JToolBar.class,       FrutigerAeroPreset::toolBar),
        StyleRule.of(JSplitPane.class,     FrutigerAeroPreset::splitPane)
    );

    /** @return the theme's style rules, one per component family. */
    static Tuple<StyleRule> rules() { return RULES; }

    // ── The gloss ────────────────────────────────────────────────────────

    /**
     *  The four stops that make a surface look like glass: bright at the top, dimming to the break,
     *  then a jump back up and a gentle darkening to the bottom edge.
     */
    private static GradientConf gloss( GradientConf g, Color base ) {
        return g
                .colors(LafUtilities.shadeTowardsWhite(base, 0.42), LafUtilities.shadeTowardsWhite(base, 0.14),
                        base,                        LafUtilities.shadeTowardsBlack(base, 0.12))
                .fractions(0, BREAK, BREAK_END, 1)
                .span(UI.Span.TOP_TO_BOTTOM)
                .clipTo(UI.ComponentArea.BODY);
    }

    /** The same glass, upside down, which is what a reflective thing does when pushed in. */
    private static GradientConf pressedGloss( GradientConf g, Color base ) {
        return g
                .colors(LafUtilities.shadeTowardsBlack(base, 0.16), LafUtilities.shadeTowardsBlack(base, 0.04),
                        base,                       LafUtilities.shadeTowardsWhite(base, 0.18))
                .fractions(0, BREAK, BREAK_END, 1)
                .span(UI.Span.TOP_TO_BOTTOM)
                .clipTo(UI.ComponentArea.BODY);
    }

    /** A soft vertical wash, for the large surfaces that are sky rather than glass. */
    private static GradientConf sky( GradientConf g, Color base ) {
        return g
                .colors(LafUtilities.shadeTowardsWhite(base, 0.30), base)
                .span(UI.Span.TOP_TO_BOTTOM)
                .clipTo(UI.ComponentArea.BODY);
    }

    private static <C extends JComponent> ComponentStyleDelegate<C> lifted(
        ComponentStyleDelegate<C> it, int blur, int alpha
    ) {
        return it
                .shadowColor(new Color(0, 0, 0, alpha))
                .shadowBlurRadius(blur)
                .shadowSpreadRadius(-1)
                .shadowOffset(0, 2)
                .shadowIsInset(false);
    }

    // ── Surfaces ─────────────────────────────────────────────────────────

    @SuppressWarnings("deprecation") // component() is the documented hook for LAF state reads
    private static ComponentStyleDelegate<JPanel> panel( ComponentStyleDelegate<JPanel> it ) {
        Palette p = SwingTreeLookAndFeel.palette();
        it = it.foregroundColor(p.text());
        switch ( Surface.of(it.component()) ) {
            case CARD:
                return lifted(it
                        .backgroundColor(p.surface())
                        .borderRadius(14)
                        .border(1, p.border())
                        .margin(4), 12, 46)
                        .gradient("glass", g -> g
                                .colors(LafUtilities.withOpacity(Color.WHITE, 190), LafUtilities.withOpacity(Color.WHITE, 0))
                                .fractions(0, 0.55)
                                .span(UI.Span.TOP_TO_BOTTOM)
                                .clipTo(UI.ComponentArea.BODY));
            case RAIL:
                return it.backgroundColor(p.surface()).gradient(g -> sky(g, p.surface()));
            case TRANSPARENT:
                return it.backgroundColor(Palette.TRANSPARENT);
            case WINDOW:
            default:
                return it.backgroundColor(p.background()).gradient(g -> sky(g, p.background()));
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
                return lifted(it.backgroundColor(p.surface()).borderRadius(12).border(1, p.border()).padding(2), 10, 40);
            case RAIL:
                return it.backgroundColor(p.surface()).borderWidth(0).borderRadius(0).padding(0);
            case WINDOW:
            default:
                return it
                        .backgroundColor(p.surfaceField())
                        .borderRadius(10)
                        .border(1, p.border())
                        .padding(2)
                        .shadowColor(LafUtilities.withOpacity(p.text(), 46))
                        .shadowBlurRadius(5)
                        .shadowOffset(0, 2)
                        .shadowIsInset(true);
        }
    }

    @SuppressWarnings("deprecation")
    private static ComponentStyleDelegate<JViewport> viewport( ComponentStyleDelegate<JViewport> it ) {
        Palette p = SwingTreeLookAndFeel.palette();
        it = it.foregroundColor(p.text());
        switch ( Surface.of(it.component()) ) {
            case TRANSPARENT: return it.backgroundColor(Palette.TRANSPARENT);
            case CARD:
            case RAIL:        return it.backgroundColor(p.surface());
            case WINDOW:
            default:          return it.backgroundColor(p.surfaceField());
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
        boolean selected = enabled && m.isSelected();
        boolean sunken   = pressed || selected;
        boolean rollover = enabled && m.isRollover() && !pressed;
        boolean focused  = enabled && b.isFocusOwner();

        Variant variant = Variant.of(b);
        Color   base    = fill(variant, p, enabled, sunken, rollover);

        it = it
                .margin(focused ? 1 : 2)
                .padding(7, 18, 7, 18)
                .borderRadius(12)
                .borderWidth(focused ? 2 : 1)
                .borderColor(focused ? p.accent() : outline(variant, p, enabled))
                .backgroundColor(base)
                .foregroundColor(ink(variant, p, enabled));

        if ( !enabled )
            return it;
        if ( variant == Variant.QUIET && !sunken && !rollover )
            return it.borderColor(Palette.TRANSPARENT).backgroundColor(Palette.TRANSPARENT);
        if ( sunken )
            return it
                    .gradient(g -> pressedGloss(g, base))
                    .shadowColor(new Color(0, 0, 0, 80))
                    .shadowBlurRadius(5)
                    .shadowOffset(0, 2)
                    .shadowIsInset(true);
        return lifted(it.gradient(g -> gloss(g, base)), rollover ? 9 : 6, rollover ? 70 : 50);
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
        Palette      p       = SwingTreeLookAndFeel.palette();
        JComboBox<?> combo   = it.component();
        boolean      enabled = combo.isEnabled();
        boolean      focused = enabled && LafUtilities.hasFocus(combo);
        Color        base    = enabled ? p.surfaceField() : p.surfaceDisabled();
        return it
                .margin(focused ? 1 : 2)
                .padding(5, 10, 5, 4)
                .borderRadius(10)
                .borderWidth(focused ? 2 : 1)
                .borderColor(focused ? p.accent() : p.border())
                .backgroundColor(base)
                .foregroundColor(enabled ? p.text() : p.textDisabled())
                .gradient(g -> gloss(g, base));
    }

    @SuppressWarnings("deprecation")
    private static ComponentStyleDelegate<JSpinner> spinner( ComponentStyleDelegate<JSpinner> it ) {
        Palette  p       = SwingTreeLookAndFeel.palette();
        JSpinner spinner = it.component();
        boolean  enabled = spinner.isEnabled();
        boolean  focused = enabled && LafUtilities.hasFocus(spinner);
        Color    base    = enabled ? p.surfaceField() : p.surfaceDisabled();
        return it
                .margin(focused ? 1 : 2)
                .padding(3)
                .borderRadius(10)
                .borderWidth(focused ? 2 : 1)
                .borderColor(focused ? p.accent() : p.border())
                .backgroundColor(base)
                .foregroundColor(enabled ? p.text() : p.textDisabled())
                .gradient(g -> gloss(g, base));
    }

    // ── Inputs ───────────────────────────────────────────────────────────

    @SuppressWarnings("deprecation")
    private static ComponentStyleDelegate<JTextField> field( ComponentStyleDelegate<JTextField> it ) {
        return input(it, it.component(), 5, 9);
    }

    @SuppressWarnings("deprecation")
    private static <C extends JTextComponent> ComponentStyleDelegate<C> page( ComponentStyleDelegate<C> it ) {
        return input(it, it.component(), 7, 9);
    }

    /** An input is clear glass over white: no gloss on the fill, a shadow cast inward from the
     *  top edge, and an accent ring the moment it takes focus. */
    private static <C extends JComponent> ComponentStyleDelegate<C> input(
        ComponentStyleDelegate<C> it, JTextComponent text, int padY, int padX
    ) {
        Palette p        = SwingTreeLookAndFeel.palette();
        boolean editable = text.isEnabled() && text.isEditable();
        boolean focused  = editable && text.isFocusOwner();
        it = it
                .margin(focused ? 1 : 2)
                .padding(padY, padX, padY, padX)
                .borderRadius(9)
                .borderWidth(focused ? 2 : 1)
                .borderColor(focused ? p.accent() : p.border())
                .backgroundColor(editable ? p.surfaceField() : p.surfaceDisabled())
                .foregroundColor(text.isEnabled() ? p.text() : p.textDisabled())
                .shadow("sunk", s -> s.color(LafUtilities.withOpacity(p.text(), 40)).offset(0, 2).blurRadius(4).isInset(true));
        if ( !focused )
            return it;
        return it.shadow("glow", s -> s.color(LafUtilities.withOpacity(p.accent(), 110)).offset(0, 0).blurRadius(7).isInset(false));
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
                .padding(4, 9, 4, 9)
                .borderRadius(7)
                .borderWidth(0)
                .backgroundColor(armed ? p.accent() : Palette.TRANSPARENT)
                .foregroundColor(!enabled ? p.textDisabled() : armed ? p.onFilled() : p.text());
        return armed ? it.gradient(g -> gloss(g, p.accent())) : it;
    }

    private static ComponentStyleDelegate<JMenuBar> menuBar( ComponentStyleDelegate<JMenuBar> it ) {
        Palette p = SwingTreeLookAndFeel.palette();
        return it
                .backgroundColor(p.surface())
                .foregroundColor(p.text())
                .padding(2, 4, 2, 4)
                .gradient(g -> gloss(g, p.surface()))
                .borderAt(UI.Edge.BOTTOM, 1, p.border());
    }

    private static ComponentStyleDelegate<JPopupMenu> popupMenu( ComponentStyleDelegate<JPopupMenu> it ) {
        Palette p = SwingTreeLookAndFeel.palette();
        return lifted(it
                .backgroundColor(p.surfaceField())
                .foregroundColor(p.text())
                .margin(3)
                .padding(4)
                .borderRadius(10)
                .border(1, p.border()), 12, 90);
    }

    private static ComponentStyleDelegate<JToolTip> toolTip( ComponentStyleDelegate<JToolTip> it ) {
        Palette p = SwingTreeLookAndFeel.palette();
        return lifted(it
                .margin(3)
                .padding(4, 10, 4, 10)
                .borderRadius(8)
                .border(1, p.border())
                .backgroundColor(p.surfaceField())
                .foregroundColor(p.text())
                .gradient(g -> gloss(g, p.surfaceField())), 8, 80);
    }

    private static ComponentStyleDelegate<JSeparator> separator( ComponentStyleDelegate<JSeparator> it ) {
        Palette p = SwingTreeLookAndFeel.palette();
        return it.backgroundColor(p.borderSoft()).foregroundColor(p.borderSoft());
    }

    private static ComponentStyleDelegate<JProgressBar> progressBar( ComponentStyleDelegate<JProgressBar> it ) {
        Palette p = SwingTreeLookAndFeel.palette();
        return it
                .borderRadius(8)
                .border(1, p.border())
                .backgroundColor(p.surfaceDisabled())
                .foregroundColor(p.primary())
                .shadowColor(LafUtilities.withOpacity(p.text(), 50))
                .shadowBlurRadius(4)
                .shadowOffset(0, 1)
                .shadowIsInset(true);
    }

    private static ComponentStyleDelegate<JSlider> slider( ComponentStyleDelegate<JSlider> it ) {
        Palette p = SwingTreeLookAndFeel.palette();
        return it.backgroundColor(Palette.TRANSPARENT).foregroundColor(p.text());
    }

    private static ComponentStyleDelegate<JScrollBar> scrollBar( ComponentStyleDelegate<JScrollBar> it ) {
        Palette p = SwingTreeLookAndFeel.palette();
        return it.backgroundColor(p.surfaceDisabled()).foregroundColor(p.border());
    }

    private static ComponentStyleDelegate<JTabbedPane> tabbedPane( ComponentStyleDelegate<JTabbedPane> it ) {
        Palette p = SwingTreeLookAndFeel.palette();
        return it.backgroundColor(p.background()).foregroundColor(p.text());
    }

    private static ComponentStyleDelegate<JSplitPane> splitPane( ComponentStyleDelegate<JSplitPane> it ) {
        Palette p = SwingTreeLookAndFeel.palette();
        return it.backgroundColor(Palette.TRANSPARENT).foregroundColor(p.text());
    }

    private static <C extends JComponent> ComponentStyleDelegate<C> flatField( ComponentStyleDelegate<C> it ) {
        Palette p = SwingTreeLookAndFeel.palette();
        return it.backgroundColor(p.surfaceField()).foregroundColor(p.text());
    }

    private static ComponentStyleDelegate<JTableHeader> tableHeader( ComponentStyleDelegate<JTableHeader> it ) {
        Palette p = SwingTreeLookAndFeel.palette();
        return it
                .backgroundColor(p.surface())
                .foregroundColor(p.textMuted())
                .gradient(g -> gloss(g, p.surface()))
                .borderAt(UI.Edge.BOTTOM, 1, p.border());
    }

    private static ComponentStyleDelegate<JToolBar> toolBar( ComponentStyleDelegate<JToolBar> it ) {
        Palette p = SwingTreeLookAndFeel.palette();
        return it
                .backgroundColor(p.surface())
                .foregroundColor(p.text())
                .padding(4, 8, 4, 8)
                .borderRadius(10)
                .border(1, p.borderSoft())
                .gradient(g -> gloss(g, p.surface()));
    }

    // ── Variant colours ──────────────────────────────────────────────────

    private static Color fill( Variant variant, Palette p, boolean enabled, boolean sunken, boolean rollover ) {
        if ( !enabled )
            return variant == Variant.QUIET ? Palette.TRANSPARENT : p.surfaceDisabled();
        switch ( variant ) {
            case PRIMARY: return sunken ? p.primaryPressed() : rollover ? p.primaryHover() : p.primary();
            case DANGER:  return sunken ? p.dangerPressed()  : rollover ? p.dangerHover()  : p.danger();
            case QUIET:   return sunken ? p.surfacePressed() : rollover ? p.surfaceHover() : Palette.TRANSPARENT;
            case NEUTRAL:
            default:      return sunken ? p.surfacePressed() : rollover ? p.surfaceHover() : p.surface();
        }
    }

    private static Color outline( Variant variant, Palette p, boolean enabled ) {
        if ( !enabled )
            return variant == Variant.QUIET ? Palette.TRANSPARENT : p.borderSoft();
        switch ( variant ) {
            case PRIMARY: return p.primaryPressed();
            case DANGER:  return p.dangerPressed();
            case QUIET:   return Palette.TRANSPARENT;
            case NEUTRAL:
            default:      return p.border();
        }
    }

    private static Color ink( Variant variant, Palette p, boolean enabled ) {
        if ( !enabled )
            return p.textDisabled();
        return variant.isFilled() ? p.onFilled() : p.text();
    }
}
