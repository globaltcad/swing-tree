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
 *  <b>Flat design</b>: no shadow, no gradient, no bevel and no rounded corner anywhere.
 *  <p>
 *  Once depth is given up, colour is the only thing left to say anything with, so it is spent
 *  freely and never mixed: a control at rest is a plain grey rectangle, under the pointer it goes
 *  pale accent, and pressed it goes the full accent with its label inverted to white. That
 *  three-step ladder is the whole vocabulary, and every control climbs the same one.
 *  <p>
 *  The idiom has one well known cost - with no relief left, a thing that can be pressed looks
 *  exactly like a thing that cannot - and it pays for it with edges instead: every input carries a
 *  hard rule around it, and focus doubles that rule rather than adding a glow, so the ring can be
 *  seen on a projector.
 *
 *  @see SwingTreeLookAndFeel.StylePreset#FLAT
 */
final class FlatDesignPreset
{
    private FlatDesignPreset() {}

    private static final Tuple<StyleRule> RULES = Tuple.of(
        StyleRule.of(JPanel.class,         FlatDesignPreset::panel),
        StyleRule.of(AbstractButton.class, FlatDesignPreset::button),
        StyleRule.of(JCheckBox.class,      FlatDesignPreset::tickable),
        StyleRule.of(JRadioButton.class,   FlatDesignPreset::tickable),
        StyleRule.of(JMenuItem.class,      FlatDesignPreset::menuItem),
        StyleRule.of(JMenuBar.class,       FlatDesignPreset::menuBar),
        StyleRule.of(JPopupMenu.class,     FlatDesignPreset::popupMenu),
        StyleRule.of(JLabel.class,         FlatDesignPreset::label),
        StyleRule.of(JTextField.class,     FlatDesignPreset::field),
        StyleRule.of(JTextArea.class,      FlatDesignPreset::page),
        StyleRule.of(JEditorPane.class,    FlatDesignPreset::page),
        StyleRule.of(JSeparator.class,     FlatDesignPreset::separator),
        StyleRule.of(JToolTip.class,       FlatDesignPreset::toolTip),
        StyleRule.of(JProgressBar.class,   FlatDesignPreset::progressBar),
        StyleRule.of(JSlider.class,        FlatDesignPreset::bare),
        StyleRule.of(JScrollBar.class,     FlatDesignPreset::scrollBar),
        StyleRule.of(JScrollPane.class,    FlatDesignPreset::scrollPane),
        StyleRule.of(JViewport.class,      FlatDesignPreset::viewport),
        StyleRule.of(JComboBox.class,      FlatDesignPreset::comboBox),
        StyleRule.of(JSpinner.class,       FlatDesignPreset::spinner),
        StyleRule.of(JTabbedPane.class,    FlatDesignPreset::tabbedPane),
        StyleRule.of(JList.class,          FlatDesignPreset::content),
        StyleRule.of(JTable.class,         FlatDesignPreset::content),
        StyleRule.of(JTableHeader.class,   FlatDesignPreset::tableHeader),
        StyleRule.of(JTree.class,          FlatDesignPreset::content),
        StyleRule.of(JToolBar.class,       FlatDesignPreset::toolBar),
        StyleRule.of(JSplitPane.class,     FlatDesignPreset::bare)
    );

    /** @return the theme's style rules, one per component family. */
    static Tuple<StyleRule> rules() { return RULES; }

    // ── Surfaces ─────────────────────────────────────────────────────────

    @SuppressWarnings("deprecation") // component() is the documented hook for LAF state reads
    private static ComponentStyleDelegate<JPanel> panel( ComponentStyleDelegate<JPanel> it ) {
        Palette p = SwingTreeLookAndFeel.palette();
        it = it.foregroundColor(p.text());
        switch ( Surface.of(it.component()) ) {
            // A card is told from the ground by the gap of ground left around it, since there is
            // no shadow left to raise it and no radius left to shape it.
            case CARD:        return it.backgroundColor(p.surface()).borderWidth(0).margin(6);
            case RAIL:        return it.backgroundColor(p.surface()).borderWidth(0);
            case TRANSPARENT: return it.backgroundColor(Palette.TRANSPARENT);
            case WINDOW:
            default:          return it.backgroundColor(
                                        LafUtilities.isControlInternal(it.component()) ? Palette.TRANSPARENT
                                                                                       : p.background());
        }
    }

    @SuppressWarnings("deprecation")
    private static ComponentStyleDelegate<JScrollPane> scrollPane( ComponentStyleDelegate<JScrollPane> it ) {
        Palette p = SwingTreeLookAndFeel.palette();
        it = it.foregroundColor(p.text()).borderRadius(0);
        switch ( Surface.of(it.component()) ) {
            case TRANSPARENT: return it.backgroundColor(Palette.TRANSPARENT).borderWidth(0).padding(0);
            case CARD:        return it.backgroundColor(p.surface()).borderWidth(0).margin(6);
            case RAIL:        return it.backgroundColor(p.surface()).borderWidth(0).padding(0);
            case WINDOW:
            default:          return it.backgroundColor(p.surfaceField()).border(1, p.borderSoft());
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
        boolean sunken   = pressed || ( enabled && m.isSelected() );
        boolean rollover = enabled && m.isRollover() && !pressed;
        boolean focused  = enabled && b.isFocusOwner();

        Variant variant = Variant.of(b);
        return it
                // The focus ring grows into the footprint the margin was holding, so a button
                // taking focus never moves the row it sits in.
                .margin(focused ? 0 : 2)
                .padding(focused ? 7 : 9, focused ? 16 : 18, focused ? 7 : 9, focused ? 16 : 18)
                .borderRadius(0)
                .borderWidth(focused ? 2 : 0)
                .borderColor(focused ? p.accent() : Palette.TRANSPARENT)
                .backgroundColor(fill(variant, p, enabled, sunken, rollover))
                .foregroundColor(ink(variant, p, enabled, sunken));
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
        JComboBox<?> combo   = it.component();
        boolean      enabled = combo.isEnabled();
        return ruled(it, enabled, enabled && LafUtilities.hasFocus(combo), 6, 10, 4);
    }

    @SuppressWarnings("deprecation")
    private static ComponentStyleDelegate<JSpinner> spinner( ComponentStyleDelegate<JSpinner> it ) {
        JSpinner spinner = it.component();
        boolean  enabled = spinner.isEnabled();
        return ruled(it, enabled, enabled && LafUtilities.hasFocus(spinner), 4, 6, 4);
    }

    // ── Inputs ───────────────────────────────────────────────────────────

    @SuppressWarnings("deprecation")
    private static ComponentStyleDelegate<JTextField> field( ComponentStyleDelegate<JTextField> it ) {
        return input(it, it.component(), 7, 10);
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
        // Inside a scroll pane or a spinner the box has already been drawn around it.
        if ( LafUtilities.isInsideAnotherControl(text) )
            return it
                    .margin(0).padding(padY, padX, padY, padX).borderWidth(0)
                    .backgroundColor(Palette.TRANSPARENT)
                    .foregroundColor(text.isEnabled() ? p.text() : p.textDisabled());
        return ruled(it, editable, editable && text.isFocusOwner(), padY, padX, padX)
                .foregroundColor(text.isEnabled() ? p.text() : p.textDisabled());
    }

    /**
     *  The hard rule every input is boxed in. Focus doubles its width and takes the accent, and
     *  the margin gives back exactly what the extra width took, so nothing shifts.
     */
    private static <C extends JComponent> ComponentStyleDelegate<C> ruled(
        ComponentStyleDelegate<C> it, boolean enabled, boolean focused, int padY, int padX, int padRight
    ) {
        Palette p = SwingTreeLookAndFeel.palette();
        return it
                .margin(focused ? 0 : 1)
                .padding(padY, padRight, padY, padX)
                .borderRadius(0)
                .border(focused ? 2 : 1, focused ? p.accent() : p.border())
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
                .backgroundColor(armed ? p.accent() : Palette.TRANSPARENT)
                .foregroundColor(!enabled ? p.textDisabled() : armed ? p.onFilled() : p.text());
    }

    private static ComponentStyleDelegate<JMenuBar> menuBar( ComponentStyleDelegate<JMenuBar> it ) {
        Palette p = SwingTreeLookAndFeel.palette();
        return it
                .backgroundColor(p.surface())
                .foregroundColor(p.text())
                .padding(2, 4, 2, 4)
                .borderAt(UI.Edge.BOTTOM, 1, p.borderSoft());
    }

    private static ComponentStyleDelegate<JPopupMenu> popupMenu( ComponentStyleDelegate<JPopupMenu> it ) {
        Palette p = SwingTreeLookAndFeel.palette();
        return it
                .backgroundColor(p.surface())
                .foregroundColor(p.text())
                .padding(4, 0, 4, 0)
                .borderRadius(0)
                .border(1, p.borderSoft());
    }

    private static ComponentStyleDelegate<JToolTip> toolTip( ComponentStyleDelegate<JToolTip> it ) {
        Palette p = SwingTreeLookAndFeel.palette();
        return it
                .padding(5, 9, 5, 9)
                .borderRadius(0)
                .borderWidth(0)
                .backgroundColor(p.text())
                .foregroundColor(p.onFilled());
    }

    /** The delegate draws the hairline itself, so the rule leaves the rest of the strip alone. */
    private static ComponentStyleDelegate<JSeparator> separator( ComponentStyleDelegate<JSeparator> it ) {
        Palette p = SwingTreeLookAndFeel.palette();
        return it.backgroundColor(Palette.TRANSPARENT).foregroundColor(p.borderSoft());
    }

    private static ComponentStyleDelegate<JProgressBar> progressBar( ComponentStyleDelegate<JProgressBar> it ) {
        Palette p = SwingTreeLookAndFeel.palette();
        return it
                .borderRadius(0)
                .borderWidth(0)
                .backgroundColor(p.accentSoft())
                .foregroundColor(p.accent());
    }

    private static ComponentStyleDelegate<JScrollBar> scrollBar( ComponentStyleDelegate<JScrollBar> it ) {
        Palette p = SwingTreeLookAndFeel.palette();
        return it.backgroundColor(p.background()).foregroundColor(p.border());
    }

    private static ComponentStyleDelegate<JTabbedPane> tabbedPane( ComponentStyleDelegate<JTabbedPane> it ) {
        Palette p = SwingTreeLookAndFeel.palette();
        return it.backgroundColor(p.background()).foregroundColor(p.text());
    }

    private static ComponentStyleDelegate<JTableHeader> tableHeader( ComponentStyleDelegate<JTableHeader> it ) {
        Palette p = SwingTreeLookAndFeel.palette();
        return it
                .backgroundColor(p.surface())
                .foregroundColor(p.textMuted())
                .borderAt(UI.Edge.BOTTOM, 1, p.border());
    }

    private static ComponentStyleDelegate<JToolBar> toolBar( ComponentStyleDelegate<JToolBar> it ) {
        Palette p = SwingTreeLookAndFeel.palette();
        return it
                .backgroundColor(p.surface())
                .foregroundColor(p.text())
                .padding(4, 8, 4, 8)
                .borderRadius(0)
                .borderAt(UI.Edge.BOTTOM, 1, p.borderSoft());
    }

    /** A list, table or tree is the content of the box around it, so it fills nothing itself. */
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
            case QUIET:   return sunken ? p.accent() : rollover ? p.accentSoft() : Palette.TRANSPARENT;
            case NEUTRAL:
            default:      return sunken ? p.accent() : rollover ? p.accentSoft() : p.surfaceHover();
        }
    }

    private static Color ink( Variant variant, Palette p, boolean enabled, boolean sunken ) {
        if ( !enabled )
            return p.textDisabled();
        // The last rung of the ladder is a saturated fill, so the label has to invert with it.
        return variant.isFilled() || sunken ? p.onFilled() : p.text();
    }
}
