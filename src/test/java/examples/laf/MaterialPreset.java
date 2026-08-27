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
 *  <b>Material</b>: flat surfaces at different heights above the page.
 *  <p>
 *  Nothing is shaded, bevelled or glossy. Every surface is one flat colour with a small four-pixel
 *  radius, and the only thing that says one is above another is the shadow it casts - which is why
 *  the shadows here come in named steps ({@link #elevation}) rather than being tuned per component.
 *  A card sits one step up, a menu three, a button that has just been pressed one more than it was.
 *  <p>
 *  Buttons follow the idiom's three kinds rather than being one kind in three colours: an ordinary
 *  one is <i>outlined</i>, the single affirmative or destructive one is <i>contained</i> - filled,
 *  and the only thing on the page casting a shadow at rest - and an in-place one is a <i>text</i>
 *  button with no box at all until you reach for it. Text fields are filled boxes, rounded only at
 *  the top, standing on a rule that thickens into the accent colour when the field takes focus.
 *
 *  @see SwingTreeLookAndFeel.StylePreset#MATERIAL
 */
final class MaterialPreset
{
    private MaterialPreset() {}

    /** The one radius the whole theme uses, in developer pixels. */
    private static final int RADIUS = 4;

    private static final Tuple<StyleRule> RULES = Tuple.of(
        StyleRule.of(JPanel.class,         MaterialPreset::panel),
        StyleRule.of(AbstractButton.class, MaterialPreset::button),
        StyleRule.of(JCheckBox.class,      MaterialPreset::tickable),
        StyleRule.of(JRadioButton.class,   MaterialPreset::tickable),
        StyleRule.of(JMenuItem.class,      MaterialPreset::menuItem),
        StyleRule.of(JMenuBar.class,       MaterialPreset::menuBar),
        StyleRule.of(JPopupMenu.class,     MaterialPreset::popupMenu),
        StyleRule.of(JLabel.class,         MaterialPreset::label),
        StyleRule.of(JTextField.class,     MaterialPreset::field),
        StyleRule.of(JTextArea.class,      MaterialPreset::page),
        StyleRule.of(JEditorPane.class,    MaterialPreset::page),
        StyleRule.of(JSeparator.class,     MaterialPreset::separator),
        StyleRule.of(JToolTip.class,       MaterialPreset::toolTip),
        StyleRule.of(JProgressBar.class,   MaterialPreset::progressBar),
        StyleRule.of(JSlider.class,        MaterialPreset::slider),
        StyleRule.of(JScrollBar.class,     MaterialPreset::scrollBar),
        StyleRule.of(JScrollPane.class,    MaterialPreset::scrollPane),
        StyleRule.of(JViewport.class,      MaterialPreset::viewport),
        StyleRule.of(JComboBox.class,      MaterialPreset::comboBox),
        StyleRule.of(JSpinner.class,       MaterialPreset::spinner),
        StyleRule.of(JTabbedPane.class,    MaterialPreset::tabbedPane),
        StyleRule.of(JList.class,          MaterialPreset::flatField),
        StyleRule.of(JTable.class,         MaterialPreset::flatField),
        StyleRule.of(JTableHeader.class,   MaterialPreset::tableHeader),
        StyleRule.of(JTree.class,          MaterialPreset::flatField),
        StyleRule.of(JToolBar.class,       MaterialPreset::toolBar),
        StyleRule.of(JSplitPane.class,     MaterialPreset::splitPane)
    );

    /** @return the theme's style rules, one per component family. */
    static Tuple<StyleRule> rules() { return RULES; }

    /**
     *  How high above the page a surface sits. The idiom says depth with one thing only, so this is
     *  the one thing: a wide soft shadow for the distance and a tight dark one for the contact edge,
     *  both growing with the step.
     */
    private static <C extends JComponent> ComponentStyleDelegate<C> elevation( ComponentStyleDelegate<C> it, int step ) {
        if ( step <= 0 )
            return it;
        int spread = step;
        return it
                .shadow("ambient", s -> s.color(new Color(0, 0, 0, 30))
                                         .offset(0, spread).blurRadius(spread * 3).spreadRadius(-1).isInset(false))
                .shadow("contact", s -> s.color(new Color(0, 0, 0, 38))
                                         .offset(0, Math.max(1, spread / 2)).blurRadius(spread).isInset(false));
    }

    // ── Surfaces ─────────────────────────────────────────────────────────

    @SuppressWarnings("deprecation") // component() is the documented hook for LAF state reads
    private static ComponentStyleDelegate<JPanel> panel( ComponentStyleDelegate<JPanel> it ) {
        Palette p = SwingTreeLookAndFeel.palette();
        it = it.foregroundColor(p.text());
        switch ( Surface.of(it.component()) ) {
            case CARD:
                return elevation(it.backgroundColor(p.surface()).borderRadius(RADIUS).borderWidth(0).margin(3), 2);
            case RAIL:
                return elevation(it.backgroundColor(p.surface()).borderWidth(0), 1);
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
                return elevation(it.backgroundColor(p.surface()).borderRadius(RADIUS).borderWidth(0).margin(3), 2);
            case RAIL:
                return it.backgroundColor(p.surface()).borderWidth(0).borderRadius(0).padding(0);
            case WINDOW:
            default:
                return it
                        .backgroundColor(p.surface())
                        .borderRadius(RADIUS)
                        .border(1, p.borderSoft())
                        .padding(1);
        }
    }

    @SuppressWarnings("deprecation")
    private static ComponentStyleDelegate<JViewport> viewport( ComponentStyleDelegate<JViewport> it ) {
        Palette p = SwingTreeLookAndFeel.palette();
        it = it.foregroundColor(p.text());
        switch ( Surface.of(it.component()) ) {
            case TRANSPARENT: return it.backgroundColor(Palette.TRANSPARENT);
            case WINDOW:
            case CARD:
            case RAIL:
            default:          return it.backgroundColor(p.surface());
        }
    }

    // ── Controls ─────────────────────────────────────────────────────────

    /**
     *  The three kinds of button, told apart by the {@link Variant} the application tagged:
     *  contained for the affirmative and the destructive one, text for an in-place command,
     *  outlined for everything else.
     */
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

        Variant variant   = Variant.of(b);
        boolean contained = variant.isFilled();

        it = it
                .margin(focused ? 1 : 2)
                .padding(8, 16, 8, 16)
                .borderRadius(RADIUS)
                .borderWidth(focused ? 2 : ( contained || variant == Variant.QUIET ? 0 : 1 ))
                .borderColor(focused ? p.accent() : p.border())
                .backgroundColor(fill(variant, p, enabled, sunken, rollover))
                .foregroundColor(ink(variant, p, enabled));

        if ( !enabled || !contained )
            return it;
        // Only a contained button is above the page, and pressing it lifts it further.
        return elevation(it, sunken ? 4 : rollover ? 3 : 2);
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
        return underlined(it, p, enabled, focused, 6, 10, 4);
    }

    @SuppressWarnings("deprecation")
    private static ComponentStyleDelegate<JSpinner> spinner( ComponentStyleDelegate<JSpinner> it ) {
        Palette  p       = SwingTreeLookAndFeel.palette();
        JSpinner spinner = it.component();
        boolean  enabled = spinner.isEnabled();
        boolean  focused = enabled && LafUtilities.hasFocus(spinner);
        return underlined(it, p, enabled, focused, 4, 6, 4);
    }

    // ── Inputs ───────────────────────────────────────────────────────────

    @SuppressWarnings("deprecation")
    private static ComponentStyleDelegate<JTextField> field( ComponentStyleDelegate<JTextField> it ) {
        JTextField f        = it.component();
        boolean    editable = f.isEnabled() && f.isEditable();
        return underlined(it, SwingTreeLookAndFeel.palette(), editable, editable && f.isFocusOwner(), 8, 12, 12)
                .foregroundColor(f.isEnabled() ? SwingTreeLookAndFeel.palette().text()
                                               : SwingTreeLookAndFeel.palette().textDisabled());
    }

    @SuppressWarnings("deprecation")
    private static <C extends JTextComponent> ComponentStyleDelegate<C> page( ComponentStyleDelegate<C> it ) {
        JTextComponent t        = it.component();
        boolean        editable = t.isEnabled() && t.isEditable();
        return underlined(it, SwingTreeLookAndFeel.palette(), editable, editable && t.isFocusOwner(), 9, 12, 12)
                .foregroundColor(t.isEnabled() ? SwingTreeLookAndFeel.palette().text()
                                               : SwingTreeLookAndFeel.palette().textDisabled());
    }

    /**
     *  The filled box every input in the idiom sits in: rounded at the top only, because it stands
     *  on the rule underneath rather than floating, and that rule thickens into the accent colour
     *  the moment the field takes focus.
     */
    private static <C extends JComponent> ComponentStyleDelegate<C> underlined(
        ComponentStyleDelegate<C> it, Palette p, boolean enabled, boolean focused, int padY, int padX, int padRight
    ) {
        return it
                // The rule is a border, so it grows downwards; the margin gives back what it takes.
                .margin(0, 0, focused ? 0 : 1, 0)
                .padding(padY, padRight, padY, padX)
                .borderRadiusAt(UI.Corner.TOP_LEFT, RADIUS, RADIUS)
                .borderRadiusAt(UI.Corner.TOP_RIGHT, RADIUS, RADIUS)
                .borderAt(UI.Edge.BOTTOM, focused ? 2 : 1, focused ? p.accent() : p.border())
                .backgroundColor(enabled ? p.surfaceField() : p.surfaceDisabled())
                .foregroundColor(enabled ? p.text() : p.textDisabled());
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
                .borderRadius(0)
                .borderWidth(0)
                .backgroundColor(armed ? p.accentSoft() : Palette.TRANSPARENT)
                .foregroundColor(enabled ? p.text() : p.textDisabled());
    }

    private static ComponentStyleDelegate<JMenuBar> menuBar( ComponentStyleDelegate<JMenuBar> it ) {
        Palette p = SwingTreeLookAndFeel.palette();
        return elevation(it
                .backgroundColor(p.surface())
                .foregroundColor(p.text())
                .padding(2, 4, 2, 4)
                .borderWidth(0), 1);
    }

    private static ComponentStyleDelegate<JPopupMenu> popupMenu( ComponentStyleDelegate<JPopupMenu> it ) {
        Palette p = SwingTreeLookAndFeel.palette();
        return elevation(it
                .backgroundColor(p.surface())
                .foregroundColor(p.text())
                .margin(4)
                .padding(4, 0, 4, 0)
                .borderRadius(RADIUS)
                .borderWidth(0), 4);
    }

    private static ComponentStyleDelegate<JToolTip> toolTip( ComponentStyleDelegate<JToolTip> it ) {
        Palette p = SwingTreeLookAndFeel.palette();
        return it
                .padding(6, 10, 6, 10)
                .borderRadius(RADIUS)
                .borderWidth(0)
                .backgroundColor(LafUtilities.withOpacity(p.text(), 229))
                .foregroundColor(p.onFilled());
    }

    private static ComponentStyleDelegate<JSeparator> separator( ComponentStyleDelegate<JSeparator> it ) {
        Palette p = SwingTreeLookAndFeel.palette();
        return it.backgroundColor(p.borderSoft()).foregroundColor(p.borderSoft());
    }

    private static ComponentStyleDelegate<JProgressBar> progressBar( ComponentStyleDelegate<JProgressBar> it ) {
        Palette p = SwingTreeLookAndFeel.palette();
        return it
                .borderRadius(3)
                .borderWidth(0)
                .backgroundColor(p.accentSoft())
                .foregroundColor(p.accent());
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

    private static <C extends JComponent> ComponentStyleDelegate<C> flatField( ComponentStyleDelegate<C> it ) {
        Palette p = SwingTreeLookAndFeel.palette();
        return it.backgroundColor(p.surface()).foregroundColor(p.text());
    }

    private static ComponentStyleDelegate<JTableHeader> tableHeader( ComponentStyleDelegate<JTableHeader> it ) {
        Palette p = SwingTreeLookAndFeel.palette();
        return it
                .backgroundColor(p.surface())
                .foregroundColor(p.textMuted())
                .borderAt(UI.Edge.BOTTOM, 1, p.borderSoft());
    }

    private static ComponentStyleDelegate<JToolBar> toolBar( ComponentStyleDelegate<JToolBar> it ) {
        Palette p = SwingTreeLookAndFeel.palette();
        return elevation(it
                .backgroundColor(p.surface())
                .foregroundColor(p.text())
                .padding(4, 8, 4, 8)
                .borderRadius(RADIUS)
                .borderWidth(0), 1);
    }

    // ── Variant colours ──────────────────────────────────────────────────

    private static Color fill( Variant variant, Palette p, boolean enabled, boolean sunken, boolean rollover ) {
        // Not transparent, even for a button that has no fill when it works: BasicButtonUI derives
        // a disabled label's colour from the background it is drawn on, and darkening a fully
        // transparent colour leaves it transparent - the label would simply disappear.
        if ( !enabled )
            return p.surfaceDisabled();
        switch ( variant ) {
            case PRIMARY: return sunken ? p.primaryPressed() : rollover ? p.primaryHover() : p.primary();
            case DANGER:  return sunken ? p.dangerPressed()  : rollover ? p.dangerHover()  : p.danger();
            case QUIET:
            case NEUTRAL:
            default:      return sunken ? p.accentSoft()
                               : rollover ? LafUtilities.withOpacity(p.accent(), 28)
                               : Palette.TRANSPARENT;
        }
    }

    private static Color ink( Variant variant, Palette p, boolean enabled ) {
        if ( !enabled )
            return p.textDisabled();
        // An outlined or text button carries the accent as its label; a contained one has to be
        // legible on top of a saturated fill instead.
        return variant.isFilled() ? p.onFilled() : p.accent();
    }
}
