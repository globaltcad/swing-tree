package examples.laf;

import examples.laf.SwingTreeLookAndFeel.Palette;
import examples.laf.SwingTreeLookAndFeel.Surface;
import examples.laf.SwingTreeLookAndFeel.Variant;
import swingtree.UI;
import swingtree.api.Painter;
import swingtree.api.Styler;
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
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 *  <b>Linen</b>: a calm, paper-like theme for {@link SwingTreeLookAndFeel}.
 *
 *  <h2>Character</h2>
 *  Cream surfaces, taupe borders, a deep olive accent for focus and selection, and a barely
 *  perceptible noise grain on the window background that suggests woven fabric. A control that
 *  takes focus grows its border and gives the same amount back from its margin, so tabbing
 *  through a form never shifts the layout around it.
 *
 *  <h2>What is in here</h2>
 *  Two things, and nothing else: the {@linkplain #PALETTE colours} the theme was designed
 *  against, and the {@linkplain #rules() table of style rules} that paints with them. Every
 *  rule reads its colours from {@link SwingTreeLookAndFeel#palette()} rather than from
 *  {@link #PALETTE} directly, so an application which re-tints the palette through
 *  {@link SwingTreeLookAndFeel.Conf#palette(swingtree.api.Configurator)} gets a re-tinted
 *  Linen rather than a half-changed one.
 *
 *  @see SwingTreeLookAndFeel.StylePreset#LINEN
 */
final class LinenPreset
{
    private LinenPreset() {}

    /** The colours Linen was designed against: aged paper, raw linen and weathered taupe stone. */
    static final Palette PALETTE = Palette.neutral()
            .background     (new Color(0xF5, 0xF1, 0xE8))
            .surface        (new Color(0xFB, 0xF8, 0xF0))
            .surfaceHover   (new Color(0xFF, 0xFC, 0xF5))
            .surfacePressed (new Color(0xE8, 0xE2, 0xD4))
            .surfaceDisabled(new Color(0xEF, 0xEB, 0xE0))
            .surfaceField   (new Color(0xFC, 0xFA, 0xF3))
            .border         (new Color(0xC9, 0xC0, 0xAB))
            .borderSoft     (new Color(0xDC, 0xD4, 0xBE))
            .text           (new Color(0x3D, 0x35, 0x2A))
            .textMuted      (new Color(0x8A, 0x7F, 0x6A))
            .textDisabled   (new Color(0xB5, 0xAC, 0x9B))
            .accent         (new Color(0x7A, 0x6E, 0x55))
            .accentSoft     (new Color(0xD8, 0xCC, 0xAE))
            .textureLight   (new Color(0xF9, 0xF5, 0xEC))
            .textureDark    (new Color(0xF0, 0xEC, 0xE3))
            .primary        (new Color(0x36, 0x5C, 0x3B))
            .primaryHover   (new Color(0x41, 0x6B, 0x46))
            .primaryPressed (new Color(0x2B, 0x4A, 0x30))
            .danger         (new Color(0x8B, 0x3A, 0x3A))
            .dangerHover    (new Color(0x9C, 0x45, 0x45))
            .dangerPressed  (new Color(0x74, 0x2E, 0x2E))
            .onFilled       (new Color(0xFA, 0xF6, 0xEC));

    /** The shadow a pressed control sinks into. Hoisted because a style rule runs on every
     *  paint, and a colour that never changes has no business being allocated there. */
    private static final Color PRESSED_SHADOW  = new Color(0, 0, 0, 55);
    /** The shadow a resting raised control casts. */
    private static final Color RESTING_SHADOW  = new Color(0, 0, 0, 25);
    /** The deeper shadow the same control casts once the pointer is over it. */
    private static final Color HOVERED_SHADOW  = new Color(0, 0, 0, 55);
    /** The shadow a floating surface - a popup, a tooltip - casts onto the window. */
    private static final Color FLOATING_SHADOW = new Color(0, 0, 0, 70);

    private static final List<StyleRule> RULES = Collections.unmodifiableList(buildRules());

    /** @return the theme's style rules, one per component family. */
    static List<StyleRule> rules() { return RULES; }

    private static List<StyleRule> buildRules() {
        List<StyleRule> rules = new ArrayList<>();

        rules.add(rule(JPanel.class, LinenPreset::panel));
        rules.add(rule(AbstractButton.class, LinenPreset::button));
        rules.add(rule(JCheckBox.class, LinenPreset::tickable));
        rules.add(rule(JRadioButton.class, LinenPreset::tickable));
        rules.add(rule(JMenuItem.class, LinenPreset::menuItem));
        rules.add(rule(JMenuBar.class, LinenPreset::menuBar));
        rules.add(rule(JPopupMenu.class, LinenPreset::popupMenu));
        rules.add(rule(JLabel.class, LinenPreset::label));
        rules.add(rule(JTextField.class, LinenPreset::field));
        rules.add(rule(JTextArea.class, LinenPreset::page));
        rules.add(rule(JEditorPane.class, LinenPreset::page));
        rules.add(rule(JSeparator.class, LinenPreset::separator));
        rules.add(rule(JToolTip.class, LinenPreset::toolTip));
        rules.add(rule(JProgressBar.class, LinenPreset::progressBar));
        rules.add(rule(JSlider.class, LinenPreset::slider));
        rules.add(rule(JScrollBar.class, LinenPreset::scrollBar));
        rules.add(rule(JScrollPane.class, LinenPreset::scrollPane));
        rules.add(rule(JViewport.class, LinenPreset::viewport));
        rules.add(rule(JComboBox.class, LinenPreset::comboBox));
        rules.add(rule(JSpinner.class, LinenPreset::spinner));
        rules.add(rule(JTabbedPane.class, LinenPreset::tabbedPane));
        rules.add(rule(JList.class, LinenPreset::list));
        rules.add(rule(JTable.class, LinenPreset::table));
        rules.add(rule(JTableHeader.class, LinenPreset::tableHeader));
        rules.add(rule(JTree.class, LinenPreset::tree));
        rules.add(rule(JToolBar.class, LinenPreset::toolBar));
        rules.add(rule(JSplitPane.class, LinenPreset::splitPane));

        return rules;
    }

    private static <C extends JComponent> StyleRule rule( Class<C> type, Styler<C> styler ) {
        return new StyleRule(type, styler);
    }

    // ── Surfaces ─────────────────────────────────────────────────────────

    /**
     *  A panel is the ground the application stands on, or one of the cards standing on it,
     *  depending on the {@link Surface} it was tagged with. Only the fill and the grain are
     *  decided here; padding, spacing and per-edge accents stay free for the application.
     */
    @SuppressWarnings("deprecation") // component() is the documented hook for LAF state reads
    private static ComponentStyleDelegate<JPanel> panel( ComponentStyleDelegate<JPanel> it ) {
        Palette p = SwingTreeLookAndFeel.palette();
        it = it.foregroundColor(p.text());
        switch ( Surface.of(it.component()) ) {
            case CARD:
                return it
                        .backgroundColor(p.surface())
                        .borderRadius(14)
                        .border(1, p.borderSoft())
                        .shadowColor(shadowOf(p.text(), 28))
                        .shadowBlurRadius(14)
                        .shadowSpreadRadius(-2)
                        .shadowOffset(0, 3);
            case RAIL:
                return it.backgroundColor(p.surface());
            case TRANSPARENT:
                return it.backgroundColor(Palette.TRANSPARENT);
            case WINDOW:
            default:
                return it
                        .backgroundColor(p.background())
                        .noise(n -> n
                                .function(UI.NoiseType.STOCHASTIC)
                                .colors(p.textureLight(), p.textureDark())
                                .scale(0.6)
                                .clipTo(UI.ComponentArea.BODY)
                        );
        }
    }

    @SuppressWarnings("deprecation")
    private static ComponentStyleDelegate<JScrollPane> scrollPane( ComponentStyleDelegate<JScrollPane> it ) {
        Palette p = SwingTreeLookAndFeel.palette();
        it = it.foregroundColor(p.text());
        switch ( Surface.of(it.component()) ) {
            case TRANSPARENT:
                // A page-level scroll region: it *is* the page, so it must not draw a field
                // around itself.
                return it
                        .backgroundColor(Palette.TRANSPARENT)
                        .borderWidth(0)
                        .borderRadius(0)
                        .padding(0);
            case CARD:
                return it
                        .backgroundColor(p.surface())
                        .borderRadius(8)
                        .borderWidth(1)
                        .borderColor(p.borderSoft())
                        .padding(2);
            case RAIL:
                return it
                        .backgroundColor(p.surface())
                        .borderWidth(0)
                        .borderRadius(0)
                        .padding(0);
            case WINDOW:
            default:
                return it
                        .backgroundColor(p.surfaceField())
                        .borderRadius(8)
                        .borderWidth(1)
                        .borderColor(p.border())
                        .padding(2);
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

    // ── Buttons ──────────────────────────────────────────────────────────

    /**
     *  A raised sheet with a faint top-to-bottom highlight, deepening on hover and sinking into
     *  an inset shadow while pressed. Which colours those states resolve to is decided by the
     *  {@link Variant} the button was tagged with; the radius, the padding and the focus border
     *  that grows while the margin shrinks to absorb it are shared across all of them.
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

        Variant variant = Variant.of(b);
        Color   surface = surfaceOf(variant, p, enabled, sunken, rollover);

        it = it
                .margin(focused ? 0 : 1)   // the margin gives back exactly what the border takes
                .padding(6, 16, 6, 16)
                .borderRadius(9)
                .borderWidth(focused ? 2 : 1)
                .borderColor(borderOf(variant, p, enabled, focused, rollover))
                .backgroundColor(surface)
                .foregroundColor(foregroundOf(variant, p, enabled));

        if ( !enabled )
            return it;

        if ( sunken )
            return it
                    .shadowColor(PRESSED_SHADOW)
                    .shadowBlurRadius(4)
                    .shadowSpreadRadius(0)
                    .shadowOffset(0, 1)
                    .shadowIsInset(true);

        if ( !variant.isRaised() )
            // A quiet control lies flat in whatever it sits on: no shadow to lift it off the
            // surface, and no gradient over a fill it may not even have.
            return it;

        Color top = surfaceOf(variant, p, true, false, true);
        return it
                .shadowColor(rollover ? HOVERED_SHADOW : RESTING_SHADOW)
                .shadowBlurRadius(rollover ? 8 : 3)
                .shadowSpreadRadius(-1)
                .shadowOffset(0, rollover ? 2 : 1)
                .shadowIsInset(false)
                .gradient(g -> g
                        .colors(top, surface)
                        .span(UI.Span.TOP_TO_BOTTOM)
                        .clipTo(UI.ComponentArea.BODY)
                );
    }

    /**
     *  A check box or a radio button reserves all of its visual identity for the glyph next to
     *  its label, which the {@linkplain Symbols symbol set} draws; the surrounding surface stays
     *  transparent so the parent's texture shows through unbroken.
     */
    @SuppressWarnings("deprecation")
    private static <C extends AbstractButton> ComponentStyleDelegate<C> tickable( ComponentStyleDelegate<C> it ) {
        Palette p = SwingTreeLookAndFeel.palette();
        return it
                .backgroundColor(Palette.TRANSPARENT)
                .foregroundColor(it.component().isEnabled() ? p.text() : p.textDisabled())
                .padding(2, 4, 2, 4);
    }

    // ── Menus ────────────────────────────────────────────────────────────

    /**
     *  Every flavour of menu entry — plain, submenu, check and radio — shares one rule: a
     *  transparent row that picks up the popup's fill, and a soft accent pill once it is armed.
     */
    @SuppressWarnings("deprecation")
    private static ComponentStyleDelegate<JMenuItem> menuItem( ComponentStyleDelegate<JMenuItem> it ) {
        Palette     p       = SwingTreeLookAndFeel.palette();
        JMenuItem   item    = it.component();
        ButtonModel m       = item.getModel();
        boolean     enabled = item.isEnabled();
        boolean     armed   = enabled && ( m.isArmed() || m.isSelected() );

        return it
                .padding(4, 8, 4, 8)
                .borderRadius(6)
                .borderWidth(0)
                .backgroundColor(armed ? p.accentSoft() : Palette.TRANSPARENT)
                .foregroundColor(enabled ? p.text() : p.textDisabled());
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
                .backgroundColor(p.surfaceField())
                .foregroundColor(p.text())
                .padding(4)
                .borderRadius(8)
                .borderWidth(1)
                .borderColor(p.border())
                .shadowColor(FLOATING_SHADOW)
                .shadowBlurRadius(10)
                .shadowSpreadRadius(-2)
                .shadowOffset(0, 3)
                .shadowIsInset(false);
    }

    // ── Text ─────────────────────────────────────────────────────────────

    @SuppressWarnings("deprecation")
    private static ComponentStyleDelegate<JLabel> label( ComponentStyleDelegate<JLabel> it ) {
        Palette p = SwingTreeLookAndFeel.palette();
        return it.foregroundColor(it.component().isEnabled() ? p.text() : p.textDisabled());
    }

    /**
     *  A single-line input: a rounded field with a border that grows into the accent on focus,
     *  under a faint accent-tinted glow that marks the active field without being noisy.
     */
    @SuppressWarnings("deprecation")
    private static ComponentStyleDelegate<JTextField> field( ComponentStyleDelegate<JTextField> it ) {
        Palette    p = SwingTreeLookAndFeel.palette();
        JTextField f = it.component();

        boolean focused = isEditable(f) && f.isFocusOwner();
        it = textSurface(it, p, f, 5, 9);
        if ( !focused )
            return it;
        return it
                .shadowColor(p.accentAt(70))
                .shadowBlurRadius(6)
                .shadowSpreadRadius(0)
                .shadowOffset(0, 0)
                .shadowIsInset(false);
    }

    /** A multi-line input: the same field, with a little more room around the text and no glow. */
    @SuppressWarnings("deprecation")
    private static <C extends JTextComponent> ComponentStyleDelegate<C> page( ComponentStyleDelegate<C> it ) {
        return textSurface(it, SwingTreeLookAndFeel.palette(), it.component(), 6, 9);
    }

    private static <C extends JComponent> ComponentStyleDelegate<C> textSurface(
        ComponentStyleDelegate<C> it, Palette p, JTextComponent text, int padY, int padX
    ) {
        boolean editable = isEditable(text);
        boolean focused  = editable && text.isFocusOwner();
        return it
                // See the button rule: shrinking the margin by the same amount the border grows
                // keeps the overall footprint constant on focus.
                .margin(focused ? 0 : 1)
                .padding(padY, padX, padY, padX)
                .borderRadius(7)
                .borderWidth(focused ? 2 : 1)
                .borderColor(focused ? p.accent() : p.border())
                .backgroundColor(editable ? p.surfaceField() : p.surfaceDisabled())
                .foregroundColor(text.isEnabled() ? p.text() : p.textDisabled());
    }

    private static boolean isEditable( JTextComponent text ) {
        return text.isEnabled() && text.isEditable();
    }

    private static ComponentStyleDelegate<JToolTip> toolTip( ComponentStyleDelegate<JToolTip> it ) {
        Palette p = SwingTreeLookAndFeel.palette();
        return it
                .padding(4, 10, 4, 10)
                .borderRadius(6)
                .borderWidth(0)
                .backgroundColor(p.accent())
                .foregroundColor(p.onFilled())
                .shadowColor(FLOATING_SHADOW)
                .shadowBlurRadius(8)
                .shadowSpreadRadius(-2)
                .shadowOffset(0, 3)
                .shadowIsInset(false);
    }

    // ── Value pickers ────────────────────────────────────────────────────

    @SuppressWarnings("deprecation")
    private static ComponentStyleDelegate<JComboBox> comboBox( ComponentStyleDelegate<JComboBox> it ) {
        Palette      p       = SwingTreeLookAndFeel.palette();
        JComboBox<?> combo   = it.component();
        boolean      enabled = combo.isEnabled();
        boolean      focused = enabled && comboHasFocus(combo);

        return it
                .margin(focused ? 0 : 1)
                .padding(4, 8, 4, 4)
                .borderRadius(7)
                .borderWidth(focused ? 2 : 1)
                .borderColor(focused ? p.accent() : p.border())
                .backgroundColor(enabled ? p.surfaceField() : p.surfaceDisabled())
                .foregroundColor(enabled ? p.text() : p.textDisabled());
    }

    /**
     *  Reads the right focus flag for a combo box's outer border. A non-editable combo is
     *  itself the focus owner; an editable one delegates focus to its editor component, so
     *  {@code combo.isFocusOwner()} would never be true for it.
     */
    private static boolean comboHasFocus( JComboBox<?> combo ) {
        if ( combo.isFocusOwner() )
            return true;
        if ( combo.isEditable() && combo.getEditor() != null ) {
            java.awt.Component editor = combo.getEditor().getEditorComponent();
            return editor != null && editor.isFocusOwner();
        }
        return false;
    }

    @SuppressWarnings("deprecation")
    private static ComponentStyleDelegate<JSpinner> spinner( ComponentStyleDelegate<JSpinner> it ) {
        Palette  p       = SwingTreeLookAndFeel.palette();
        JSpinner spinner = it.component();
        boolean  enabled = spinner.isEnabled();
        boolean  focused = enabled && spinnerEditorHasFocus(spinner);

        return it
                .margin(focused ? 0 : 1)
                .padding(3)
                .borderRadius(7)
                .borderWidth(focused ? 2 : 1)
                .borderColor(focused ? p.accent() : p.border())
                .backgroundColor(enabled ? p.surfaceField() : p.surfaceDisabled())
                .foregroundColor(enabled ? p.text() : p.textDisabled());
    }

    /**
     *  A {@link JSpinner}'s editor is a wrapper component — a {@link JSpinner.DefaultEditor} by
     *  default — that contains the actual input control, usually a formatted text field, as a
     *  child. Focus therefore lives on the descendant, not on the wrapper, so
     *  {@code editor.isFocusOwner()} would never return {@code true}. This walks one level in to
     *  read the right focus flag for the spinner's outer border.
     */
    private static boolean spinnerEditorHasFocus( JSpinner spinner ) {
        java.awt.Component editor = spinner.getEditor();
        if ( editor instanceof JSpinner.DefaultEditor )
            return ((JSpinner.DefaultEditor) editor).getTextField().isFocusOwner();
        return editor != null && editor.isFocusOwner();
    }

    private static ComponentStyleDelegate<JSlider> slider( ComponentStyleDelegate<JSlider> it ) {
        Palette p = SwingTreeLookAndFeel.palette();
        return it.backgroundColor(p.background()).foregroundColor(p.text());
    }

    /** The trough of a progress bar; the {@linkplain Symbols symbol set} fills it. */
    private static ComponentStyleDelegate<JProgressBar> progressBar( ComponentStyleDelegate<JProgressBar> it ) {
        Palette p = SwingTreeLookAndFeel.palette();
        return it
                .borderRadius(7)
                .borderWidth(1)
                .borderColor(p.border())
                .backgroundColor(p.surfaceDisabled())
                .foregroundColor(p.accent());
    }

    // ── Structure ────────────────────────────────────────────────────────

    private static ComponentStyleDelegate<JSeparator> separator( ComponentStyleDelegate<JSeparator> it ) {
        Palette p = SwingTreeLookAndFeel.palette();
        return it.backgroundColor(p.borderSoft()).foregroundColor(p.borderSoft());
    }

    /**
     *  A scroll bar is nothing but its groove and its thumb, and the groove is this background:
     *  the symbol set draws the thumb on top of it. Painting the groove here rather than as a
     *  symbol keeps it a flat fill the render cache can blit, instead of a rounded rectangle the
     *  rasterizer has to antialias the height of the window on every frame.
     */
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
        return it.backgroundColor(p.background()).foregroundColor(p.text());
    }

    @SuppressWarnings("deprecation")
    private static ComponentStyleDelegate<JToolBar> toolBar( ComponentStyleDelegate<JToolBar> it ) {
        Palette p = SwingTreeLookAndFeel.palette();
        return it
                .backgroundColor(p.surface())
                .foregroundColor(p.text())
                .padding(4, 8, 4, 8)
                .borderRadius(6)
                .borderWidth(1)
                .borderColor(p.borderSoft())
                .painter(UI.Layer.CONTENT, new DragHandlePainter(it.component()));
    }

    private static ComponentStyleDelegate<JList> list( ComponentStyleDelegate<JList> it ) {
        Palette p = SwingTreeLookAndFeel.palette();
        return it.backgroundColor(p.surfaceField()).foregroundColor(p.text());
    }

    private static ComponentStyleDelegate<JTable> table( ComponentStyleDelegate<JTable> it ) {
        Palette p = SwingTreeLookAndFeel.palette();
        return it.backgroundColor(p.surfaceField()).foregroundColor(p.text());
    }

    private static ComponentStyleDelegate<JTableHeader> tableHeader( ComponentStyleDelegate<JTableHeader> it ) {
        Palette p = SwingTreeLookAndFeel.palette();
        return it
                .backgroundColor(p.surface())
                .foregroundColor(p.textMuted())
                .borderAt(UI.Edge.BOTTOM, 1, p.borderSoft());
    }

    private static ComponentStyleDelegate<JTree> tree( ComponentStyleDelegate<JTree> it ) {
        Palette p = SwingTreeLookAndFeel.palette();
        return it.backgroundColor(p.surfaceField()).foregroundColor(p.text());
    }

    // ── Variant colours ──────────────────────────────────────────────────

    /**
     *  The fill of one combination of button states.
     *
     * @param variant  the semantic role the button was tagged with
     * @param p        the palette to take colours from
     * @param enabled  whether the button can be pressed at all
     * @param sunken   whether it is pressed or selected
     * @param rollover whether the pointer is over it
     * @return the surface colour to paint
     */
    private static Color surfaceOf( Variant variant, Palette p, boolean enabled, boolean sunken, boolean rollover ) {
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

    /** The label colour that stays legible on {@link #surfaceOf}. */
    private static Color foregroundOf( Variant variant, Palette p, boolean enabled ) {
        if ( !enabled )
            return p.textDisabled();
        return variant.isFilled() ? p.onFilled() : p.text();
    }

    /**
     *  The border colour. A filled variant borders itself in its own fill so the edge
     *  disappears, and every variant switches to the accent while focused — the border
     *  <i>is</i> the focus indicator.
     */
    private static Color borderOf( Variant variant, Palette p, boolean enabled, boolean focused, boolean rollover ) {
        if ( focused && enabled )
            return p.accent();
        if ( !enabled )
            return variant == Variant.QUIET ? Palette.TRANSPARENT : p.borderSoft();
        switch ( variant ) {
            case PRIMARY: return p.primaryPressed();
            case DANGER:  return p.dangerPressed();
            case QUIET:   return rollover ? p.border() : Palette.TRANSPARENT;
            case NEUTRAL:
            default:      return p.border();
        }
    }

    private static Color shadowOf( Color base, int alpha ) {
        return new Color(base.getRed(), base.getGreen(), base.getBlue(), alpha);
    }

    /**
     *  Draws the handle a floatable tool bar is dragged by, on the tool bar's content layer.
     *  <p>
     *  A named painter rather than a lambda: a style rule is re-evaluated on every paint, and a
     *  capturing lambda is a fresh object each time, which would make the style engine conclude
     *  that the tool bar's style had changed when nothing had. Two of these compare equal
     *  whenever they would draw the same thing.
     */
    private static final class DragHandlePainter implements Painter
    {
        private final JToolBar _bar;
        private final boolean  _floatable;
        private final int      _orientation;

        DragHandlePainter( JToolBar bar ) {
            _bar         = bar;
            _floatable   = bar.isFloatable();
            _orientation = bar.getOrientation();
        }

        @Override
        public void paint( Graphics2D g ) {
            if ( !_floatable )
                return;
            Graphics2D scratch = (Graphics2D) g.create();
            try {
                SwingTreeLookAndFeel.symbols().paintDragHandle(
                        scratch, SwingTreeLookAndFeel.palette(),
                        _bar.getWidth(), _bar.getHeight(),
                        _orientation == JToolBar.HORIZONTAL
                );
            } finally {
                scratch.dispose();
            }
        }

        @Override
        public boolean equals( Object other ) {
            if ( this == other ) return true;
            if ( !(other instanceof DragHandlePainter) ) return false;
            DragHandlePainter that = (DragHandlePainter) other;
            return this._bar == that._bar
                && this._floatable == that._floatable
                && this._orientation == that._orientation;
        }

        @Override
        public int hashCode() {
            return Objects.hash(System.identityHashCode(_bar), _floatable, _orientation);
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "[floatable=" + _floatable + ", orientation=" + _orientation + "]";
        }
    }
}
