package examples.laf;

import examples.laf.SwingTreeLookAndFeel.Theme;
import examples.laf.SwingTreeLookAndFeel.ThemedStyler;
import sprouts.Tuple;
import swingtree.UI;
import swingtree.api.Painter;
import swingtree.layout.Bounds;
import swingtree.style.ComponentStyleDelegate;
import swingtree.style.GradientConf;

import javax.swing.*;
import javax.swing.table.JTableHeader;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.util.Objects;

/**
 *  The tables of style rules behind {@link SwingTreeLookAndFeel.StylePreset}, one nested class per
 *  preset and one rule per component family.
 *  <p>
 *  No rule anywhere here names a colour. Every one is a {@link ThemedStyler} and takes its colours
 *  from the palette of the {@link Theme} it is handed, so pairing a preset with a palette it was
 *  not designed against re-tints the whole preset instead of half of it.
 */
final class Styles
{
    private Styles() {}


    /**
     *  Draws the handle a floatable tool bar is dragged by, on the tool bar's content layer. It
     *  is a named painter rather than a lambda because a style rule runs on every paint, and a
     *  capturing lambda is a new object each time, which would tell the style engine the tool
     *  bar's style had changed. Two of these compare equal whenever they would draw the same
     *  thing.
     *  <p>
     *  The symbol is handed the strip the tool bar's leading inset leaves free, not the whole
     *  bar, because every symbol set draws the handle within a few pixels of that edge. A box
     *  as long as the bar is keyed on the window's width and too large to keep as a tile, so it
     *  would be rasterized again on every paint.
     */
    private static final class DragHandlePainter implements Painter
    {
        private final Theme     _theme;
        /** Where the grip goes: the room the border and the padding leave inside the tool bar's box,
         *  before its first button, in the tool bar's own pixels. */
        private final Rectangle _grip;
        private final boolean   _floatable;
        private final int       _orientation;

        DragHandlePainter( Theme theme, ComponentStyleDelegate<JToolBar> it ) {
            @SuppressWarnings("deprecation") // component() is the documented hook for LAF state reads
            JToolBar  bar    = it.component();
            Rectangle box    = LafUtilities.marginBoxOf(bar);
            Insets    insets = bar.getInsets();
            _theme       = theme;
            _floatable   = bar.isFloatable();
            _orientation = bar.getOrientation();
            _grip        = _orientation == JToolBar.HORIZONTAL
                             ? new Rectangle(box.x, box.y, Math.max(0, insets.left - box.x), box.height)
                             : new Rectangle(box.x, box.y, box.width, Math.max(0, insets.top - box.y));
        }

        @Override
        public void paint( Graphics2D g ) {
            if ( !_floatable || _grip.isEmpty() )
                return;
            Graphics2D scratch = (Graphics2D) g.create();
            try {
                float scale = UI.scale();
                scratch.scale(1 / scale, 1 / scale); // A symbol set draws in the component's own pixels.
                scratch.translate(_grip.x, _grip.y);
                _theme.symbols().paintDragHandle(
                        scratch, _theme.palette(), _grip.width, _grip.height,
                        _orientation == JToolBar.HORIZONTAL
                );
            } finally {
                scratch.dispose();
            }
        }

        @Override
        public boolean canBeCached() {
            return true;
        }

        @Override
        public boolean equals( Object other ) {
            if ( this == other ) return true;
            if ( !(other instanceof DragHandlePainter) ) return false;
            DragHandlePainter that = (DragHandlePainter) other;
            return this._theme == that._theme
                && this._grip.equals(that._grip)
                && this._floatable == that._floatable
                && this._orientation == that._orientation;
        }

        @Override
        public int hashCode() {
            return Objects.hash(System.identityHashCode(_theme), _grip, _floatable, _orientation);
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "[grip=" + _grip + ", floatable=" + _floatable +
                   ", orientation=" + _orientation + "]";
        }
    }

    /**
     *  <b>Linen</b>: a calm, paper-like theme of cream surfaces, taupe borders and a woven grain on
     *  the window. A control that takes focus grows its border and gives the same amount back from
     *  its margin, so tabbing through a form never shifts the layout around it.
     *
     *  @see SwingTreeLookAndFeel.StylePreset#LINEN
     */
    static final class Linen
    {
        private Linen() {}

        // The four shadows the theme casts, deepening in that order. They are constants because a
        // style rule runs on every paint, and a colour that never changes should not be allocated
        // there. A floating surface is a popup or a tool tip.
        private static final Color RESTING_SHADOW  = new Color(0, 0, 0, 25);
        private static final Color HOVERED_SHADOW  = new Color(0, 0, 0, 55);
        private static final Color PRESSED_SHADOW  = new Color(0, 0, 0, 55);
        private static final Color FLOATING_SHADOW = new Color(0, 0, 0, 70);

        private static final Tuple<StyleRule> RULES = buildRules();

        static Tuple<StyleRule> rules() { return RULES; }

        private static Tuple<StyleRule> buildRules() {
            return Tuple.of(
                rule(JPanel.class, Linen::panel),
                rule(AbstractButton.class, Linen::button),
                rule(JCheckBox.class, Linen::tickable),
                rule(JRadioButton.class, Linen::tickable),
                rule(JMenuItem.class, Linen::menuItem),
                rule(JMenuBar.class, Linen::menuBar),
                rule(JPopupMenu.class, Linen::popupMenu),
                rule(JLabel.class, Linen::label),
                rule(JTextField.class, Linen::field),
                rule(JTextArea.class, Linen::page),
                rule(JEditorPane.class, Linen::page),
                rule(JSeparator.class, Linen::separator),
                rule(JToolTip.class, Linen::toolTip),
                rule(JProgressBar.class, Linen::progressBar),
                rule(JSlider.class, Linen::slider),
                rule(JScrollBar.class, Linen::scrollBar),
                rule(JScrollPane.class, Linen::scrollPane),
                rule(JViewport.class, Linen::viewport),
                rule(JComboBox.class, Linen::comboBox),
                rule(JSpinner.class, Linen::spinner),
                rule(JTabbedPane.class, Linen::tabbedPane),
                rule(JList.class, Linen::list),
                rule(JTable.class, Linen::table),
                rule(JTableHeader.class, Linen::tableHeader),
                rule(JTree.class, Linen::tree),
                rule(JToolBar.class, Linen::toolBar),
                rule(JSplitPane.class, Linen::splitPane)
            );
        }

        private static <C extends JComponent> StyleRule rule( Class<C> type, ThemedStyler<C> styler ) {
            return new StyleRule(type, styler);
        }

        // ── Surfaces ─────────────────────────────────────────────────────────

        /**
         *  A panel is the ground the application stands on, or one of the cards standing on it,
         *  depending on the {@link SwingTreeLookAndFeel.Surface} it was tagged with. Only the fill and the grain are
         *  decided here; padding, spacing and per-edge accents stay free for the application.
         */
        @SuppressWarnings("deprecation") // component() is the documented hook for LAF state reads
        private static ComponentStyleDelegate<JPanel> panel( Theme theme, ComponentStyleDelegate<JPanel> it ) {
            SwingTreeLookAndFeel.Palette p = theme.palette();
            it = it.foregroundColor(p.text());
            switch ( SwingTreeLookAndFeel.Surface.of(it.component()) ) {
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
                    return it.backgroundColor(SwingTreeLookAndFeel.Palette.TRANSPARENT);
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
        private static ComponentStyleDelegate<JScrollPane> scrollPane( Theme theme, ComponentStyleDelegate<JScrollPane> it ) {
            SwingTreeLookAndFeel.Palette p = theme.palette();
            it = it.foregroundColor(p.text());
            switch ( SwingTreeLookAndFeel.Surface.of(it.component()) ) {
                case TRANSPARENT:
                    // A page-level scroll region: it *is* the page, so it must not draw a field
                    // around itself.
                    return it
                            .backgroundColor(SwingTreeLookAndFeel.Palette.TRANSPARENT)
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
        private static ComponentStyleDelegate<JViewport> viewport( Theme theme, ComponentStyleDelegate<JViewport> it ) {
            SwingTreeLookAndFeel.Palette p = theme.palette();
            it = it.foregroundColor(p.text());
            switch ( SwingTreeLookAndFeel.Surface.of(it.component()) ) {
                case TRANSPARENT: return it.backgroundColor(SwingTreeLookAndFeel.Palette.TRANSPARENT);
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
         *  {@link SwingTreeLookAndFeel.Variant} the button was tagged with; the radius, the padding and the focus border
         *  that grows while the margin shrinks to absorb it are shared across all of them.
         */
        @SuppressWarnings("deprecation")
        private static ComponentStyleDelegate<AbstractButton> button( Theme theme, ComponentStyleDelegate<AbstractButton> it ) {
            SwingTreeLookAndFeel.Palette p = theme.palette();
            AbstractButton b = it.component();
            ButtonModel    m = b.getModel();

            boolean enabled  = b.isEnabled();
            boolean pressed  = enabled && m.isArmed() && m.isPressed();
            boolean selected = enabled && m.isSelected();
            boolean sunken   = pressed || selected;
            boolean rollover = enabled && m.isRollover() && !pressed;
            boolean focused  = enabled && b.isFocusOwner();

            SwingTreeLookAndFeel.Variant variant = SwingTreeLookAndFeel.Variant.of(b);
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
        private static <C extends AbstractButton> ComponentStyleDelegate<C> tickable( Theme theme, ComponentStyleDelegate<C> it ) {
            SwingTreeLookAndFeel.Palette p = theme.palette();
            return it
                    .backgroundColor(SwingTreeLookAndFeel.Palette.TRANSPARENT)
                    .foregroundColor(it.component().isEnabled() ? p.text() : p.textDisabled())
                    .padding(2, 4, 2, 4);
        }

        // ── Menus ────────────────────────────────────────────────────────────

        /**
         *  Every flavour of menu entry — plain, submenu, check and radio — shares one rule: a
         *  transparent row that picks up the popup's fill, and a soft accent pill once it is armed.
         */
        @SuppressWarnings("deprecation")
        private static ComponentStyleDelegate<JMenuItem> menuItem( Theme theme, ComponentStyleDelegate<JMenuItem> it ) {
            SwingTreeLookAndFeel.Palette p       = theme.palette();
            JMenuItem   item    = it.component();
            ButtonModel m       = item.getModel();
            boolean     enabled = item.isEnabled();
            boolean     armed   = enabled && ( m.isArmed() || m.isSelected() );

            return it
                    .padding(4, 8, 4, 8)
                    .borderRadius(6)
                    .borderWidth(0)
                    .backgroundColor(armed ? p.accentSoft() : SwingTreeLookAndFeel.Palette.TRANSPARENT)
                    .foregroundColor(enabled ? p.text() : p.textDisabled());
        }

        private static ComponentStyleDelegate<JMenuBar> menuBar( Theme theme, ComponentStyleDelegate<JMenuBar> it ) {
            SwingTreeLookAndFeel.Palette p = theme.palette();
            return it
                    .backgroundColor(p.surface())
                    .foregroundColor(p.text())
                    .padding(2, 4, 2, 4)
                    .borderAt(UI.Edge.BOTTOM, 1, p.borderSoft());
        }

        private static ComponentStyleDelegate<JPopupMenu> popupMenu( Theme theme, ComponentStyleDelegate<JPopupMenu> it ) {
            SwingTreeLookAndFeel.Palette p = theme.palette();
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
        private static ComponentStyleDelegate<JLabel> label( Theme theme, ComponentStyleDelegate<JLabel> it ) {
            SwingTreeLookAndFeel.Palette p = theme.palette();
            return it.foregroundColor(it.component().isEnabled() ? p.text() : p.textDisabled());
        }

        /**
         *  A single-line input: a rounded field with a border that grows into the accent on focus,
         *  under a faint accent-tinted glow that marks the active field without being noisy.
         */
        @SuppressWarnings("deprecation")
        private static ComponentStyleDelegate<JTextField> field( Theme theme, ComponentStyleDelegate<JTextField> it ) {
            SwingTreeLookAndFeel.Palette p = theme.palette();
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
        private static <C extends JTextComponent> ComponentStyleDelegate<C> page( Theme theme, ComponentStyleDelegate<C> it ) {
            return textSurface(it, theme.palette(), it.component(), 6, 9);
        }

        private static <C extends JComponent> ComponentStyleDelegate<C> textSurface(
                ComponentStyleDelegate<C> it, SwingTreeLookAndFeel.Palette p, JTextComponent text, int padY, int padX
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

        private static ComponentStyleDelegate<JToolTip> toolTip( Theme theme, ComponentStyleDelegate<JToolTip> it ) {
            SwingTreeLookAndFeel.Palette p = theme.palette();
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
        private static ComponentStyleDelegate<JComboBox> comboBox( Theme theme, ComponentStyleDelegate<JComboBox> it ) {
            SwingTreeLookAndFeel.Palette p       = theme.palette();
            JComboBox<?> combo   = it.component();
            boolean      enabled = combo.isEnabled();
            boolean      focused = enabled && LafUtilities.hasFocus(combo);

            Color resting = enabled ? p.surfaceField() : p.surfaceDisabled();
            return it
                    .margin(focused ? 0 : 1)
                    .padding(4, 8, 4, 4)
                    .borderRadius(7)
                    .borderWidth(focused ? 2 : 1)
                    .borderColor(focused ? p.accent() : p.border())
                    .backgroundColor(LafUtilities.underPointer(p, resting, combo))
                    .foregroundColor(enabled ? p.text() : p.textDisabled());
        }

        @SuppressWarnings("deprecation")
        private static ComponentStyleDelegate<JSpinner> spinner( Theme theme, ComponentStyleDelegate<JSpinner> it ) {
            SwingTreeLookAndFeel.Palette p       = theme.palette();
            JSpinner spinner = it.component();
            boolean  enabled = spinner.isEnabled();
            boolean  focused = enabled && LafUtilities.hasFocus(spinner);

            return it
                    .margin(focused ? 0 : 1)
                    .padding(3)
                    .borderRadius(7)
                    .borderWidth(focused ? 2 : 1)
                    .borderColor(focused ? p.accent() : p.border())
                    .backgroundColor(enabled ? p.surfaceField() : p.surfaceDisabled())
                    .foregroundColor(enabled ? p.text() : p.textDisabled());
        }

        private static ComponentStyleDelegate<JSlider> slider( Theme theme, ComponentStyleDelegate<JSlider> it ) {
            SwingTreeLookAndFeel.Palette p = theme.palette();
            return it.backgroundColor(p.background()).foregroundColor(p.text());
        }

        /** The trough of a progress bar; the {@linkplain Symbols symbol set} fills it. */
        private static ComponentStyleDelegate<JProgressBar> progressBar( Theme theme, ComponentStyleDelegate<JProgressBar> it ) {
            SwingTreeLookAndFeel.Palette p = theme.palette();
            return it
                    .borderRadius(7)
                    .borderWidth(1)
                    .borderColor(p.border())
                    .backgroundColor(p.surfaceDisabled())
                    .foregroundColor(p.accent());
        }

        // ── Structure ────────────────────────────────────────────────────────

        private static ComponentStyleDelegate<JSeparator> separator( Theme theme, ComponentStyleDelegate<JSeparator> it ) {
            SwingTreeLookAndFeel.Palette p = theme.palette();
            return it.backgroundColor(p.borderSoft()).foregroundColor(p.borderSoft());
        }

        /**
         *  A scroll bar is nothing but its groove and its thumb, and the groove is this background:
         *  the symbol set draws the thumb on top of it. Painting the groove here rather than as a
         *  symbol keeps it a flat fill the render cache can blit, instead of a rounded rectangle the
         *  rasterizer has to antialias the height of the window on every frame.
         */
        private static ComponentStyleDelegate<JScrollBar> scrollBar( Theme theme, ComponentStyleDelegate<JScrollBar> it ) {
            SwingTreeLookAndFeel.Palette p = theme.palette();
            return it.backgroundColor(p.surfaceDisabled()).foregroundColor(p.border());
        }

        private static ComponentStyleDelegate<JTabbedPane> tabbedPane( Theme theme, ComponentStyleDelegate<JTabbedPane> it ) {
            SwingTreeLookAndFeel.Palette p = theme.palette();
            return it.backgroundColor(p.background()).foregroundColor(p.text());
        }

        private static ComponentStyleDelegate<JSplitPane> splitPane( Theme theme, ComponentStyleDelegate<JSplitPane> it ) {
            SwingTreeLookAndFeel.Palette p = theme.palette();
            return it.backgroundColor(p.background()).foregroundColor(p.text());
        }

        @SuppressWarnings("deprecation")
        private static ComponentStyleDelegate<JToolBar> toolBar( Theme theme, ComponentStyleDelegate<JToolBar> it ) {
            SwingTreeLookAndFeel.Palette p = theme.palette();
            return it
                    .backgroundColor(p.surface())
                    .foregroundColor(p.text())
                    .padding(4, 8, 4, 8)
                    .borderRadius(6)
                    .borderWidth(1)
                    .borderColor(p.borderSoft())
                    .painter(UI.Layer.CONTENT, new DragHandlePainter(theme, it));
        }

        private static ComponentStyleDelegate<JList> list( Theme theme, ComponentStyleDelegate<JList> it ) {
            SwingTreeLookAndFeel.Palette p = theme.palette();
            return it.backgroundColor(p.surfaceField()).foregroundColor(p.text());
        }

        private static ComponentStyleDelegate<JTable> table( Theme theme, ComponentStyleDelegate<JTable> it ) {
            SwingTreeLookAndFeel.Palette p = theme.palette();
            return it.backgroundColor(p.surfaceField()).foregroundColor(p.text());
        }

        private static ComponentStyleDelegate<JTableHeader> tableHeader( Theme theme, ComponentStyleDelegate<JTableHeader> it ) {
            SwingTreeLookAndFeel.Palette p = theme.palette();
            return it
                    .backgroundColor(p.surface())
                    .foregroundColor(p.textMuted())
                    .borderAt(UI.Edge.BOTTOM, 1, p.borderSoft());
        }

        private static ComponentStyleDelegate<JTree> tree( Theme theme, ComponentStyleDelegate<JTree> it ) {
            SwingTreeLookAndFeel.Palette p = theme.palette();
            return it.backgroundColor(p.surfaceField()).foregroundColor(p.text());
        }

        // ── Variant colours ──────────────────────────────────────────────────

        private static Color surfaceOf(SwingTreeLookAndFeel.Variant variant, SwingTreeLookAndFeel.Palette p, boolean enabled, boolean sunken, boolean rollover ) {
            if ( !enabled )
                return variant == SwingTreeLookAndFeel.Variant.QUIET ? SwingTreeLookAndFeel.Palette.TRANSPARENT : p.surfaceDisabled();
            switch ( variant ) {
                case PRIMARY: return sunken ? p.primaryPressed() : rollover ? p.primaryHover() : p.primary();
                case DANGER:  return sunken ? p.dangerPressed()  : rollover ? p.dangerHover()  : p.danger();
                case QUIET:   return sunken ? p.surfacePressed() : rollover ? p.surfaceHover() : SwingTreeLookAndFeel.Palette.TRANSPARENT;
                case NEUTRAL:
                default:      return sunken ? p.surfacePressed() : rollover ? p.surfaceHover() : p.surface();
            }
        }

        /** The label colour that stays legible on {@link #surfaceOf}. */
        private static Color foregroundOf(SwingTreeLookAndFeel.Variant variant, SwingTreeLookAndFeel.Palette p, boolean enabled ) {
            if ( !enabled )
                return p.textDisabled();
            return variant.isFilled() ? p.onFilled() : p.text();
        }

        /**
         *  The border colour. A filled variant borders itself in its own fill so the edge
         *  disappears, and every variant switches to the accent while focused — the border
         *  <i>is</i> the focus indicator.
         */
        private static Color borderOf(SwingTreeLookAndFeel.Variant variant, SwingTreeLookAndFeel.Palette p, boolean enabled, boolean focused, boolean rollover ) {
            if ( focused && enabled )
                return p.accent();
            if ( !enabled )
                return variant == SwingTreeLookAndFeel.Variant.QUIET ? SwingTreeLookAndFeel.Palette.TRANSPARENT : p.borderSoft();
            switch ( variant ) {
                case PRIMARY: return p.primaryPressed();
                case DANGER:  return p.dangerPressed();
                case QUIET:   return rollover ? p.border() : SwingTreeLookAndFeel.Palette.TRANSPARENT;
                case NEUTRAL:
                default:      return p.border();
            }
        }

        private static Color shadowOf( Color base, int alpha ) {
            return new Color(base.getRed(), base.getGreen(), base.getBlue(), alpha);
        }
    }

    /**
     *  <b>Soft UI</b>, or neumorphism: every surface is the colour of the window it sits on, and only
     *  the light tells them apart. A highlight up and to the left, a shadow down and to the right,
     *  and a wash across the face along the same diagonal, so a control reads as moulded rather than
     *  as a rectangle with a halo. Pressing one turns all three around.
     *  <p>
     *  Three rules keep the light believable across palettes: every shade is a fixed number of
     *  channel steps from the palette colour ({@link #LIGHT_STEP}), each fade follows the falloff
     *  curve of the thing it stands for rather than a straight ramp ({@link #raised},
     *  {@link #sunken}), and a shadow always reaches further than the highlight facing it
     *  ({@link #SHEEN_REACH}).
     *  <p>
     *  There are almost no borders, because an outline would do the work the light is there to do.
     *  Focus is the exception: a resting control already casts a shadow, so a focused one grows an
     *  accent ring and gives the same amount back from its margin.
     *
     *  @see SwingTreeLookAndFeel.StylePreset#SOFT_UI
     */
    static final class SoftUi
    {
        private SoftUi() {}

        /**
         *  How far the two sides of an extrusion move, in channel steps rather than as a fraction of
         *  the way to white or black. A fraction is the wrong unit for light: the 0.4 that lifts a
         *  pale clay grey by thirteen steps lifts a midnight blue by ninety.
         */
        private static final int LIGHT_STEP = 18;
        /** Deeper than the highlight, so the pair reads as a room lit from one corner rather than
         *  as an outline drawn twice in two colours. */
        private static final int SHADE_STEP = 22;
        /** How much dimmer the light inside a groove is. A groove is only a few pixels wide, and a
         *  step that reads as a soft halo outside reads as a hard line inside. */
        private static final double INSET_DIMMING = 0.55;
        /** How far the two ends of a surface's own diagonal wash sit from its fill. */
        private static final int CURVE_STEP = 5;
        /**
         *  How far a highlight reaches compared with the shadow opposite it. A shadow is an umbra
         *  widened by the angular width of the lamp, so it spreads; a highlight is only the band
         *  where the surface has turned far enough to face the lamp, so it stays near the edge.
         */
        private static final double SHEEN_REACH = 0.75;

        // The names of the two shadow layers every extrusion is built from: the corner the light
        // comes from, and the corner opposite it.
        private static final String LIT    = "lit";
        private static final String SHADED = "shaded";

        private static final Tuple<StyleRule> RULES = Tuple.of(
            StyleRule.of(JPanel.class,       SoftUi::panel),
            StyleRule.of(AbstractButton.class, SoftUi::button),
            StyleRule.of(JCheckBox.class,    SoftUi::tickable),
            StyleRule.of(JRadioButton.class, SoftUi::tickable),
            StyleRule.of(JMenuItem.class,    SoftUi::menuItem),
            StyleRule.of(JMenuBar.class,     SoftUi::menuBar),
            StyleRule.of(JPopupMenu.class,   SoftUi::popupMenu),
            StyleRule.of(JLabel.class,       SoftUi::label),
            StyleRule.of(JTextField.class,   SoftUi::field),
            StyleRule.of(JTextArea.class,    SoftUi::page),
            StyleRule.of(JEditorPane.class,  SoftUi::page),
            StyleRule.of(JSeparator.class,   SoftUi::separator),
            StyleRule.of(JToolTip.class,     SoftUi::toolTip),
            StyleRule.of(JProgressBar.class, SoftUi::progressBar),
            StyleRule.of(JSlider.class,      SoftUi::slider),
            StyleRule.of(JScrollBar.class,   SoftUi::scrollBar),
            StyleRule.of(JScrollPane.class,  SoftUi::scrollPane),
            StyleRule.of(JViewport.class,    SoftUi::viewport),
            StyleRule.of(JComboBox.class,    SoftUi::comboBox),
            StyleRule.of(JSpinner.class,     SoftUi::spinner),
            StyleRule.of(JTabbedPane.class,  SoftUi::tabbedPane),
            StyleRule.of(JList.class,        SoftUi::flatField),
            StyleRule.of(JTable.class,       SoftUi::flatField),
            StyleRule.of(JTableHeader.class, SoftUi::tableHeader),
            StyleRule.of(JTree.class,        SoftUi::flatField),
            StyleRule.of(JToolBar.class,     SoftUi::toolBar),
            StyleRule.of(JSplitPane.class,   SoftUi::splitPane)
        );

        static Tuple<StyleRule> rules() { return RULES; }

        // ── The light ────────────────────────────────────────────────────────

        private static Color lit( SwingTreeLookAndFeel.Palette p ) { return LafUtilities.shadeBySteps(p.background(), LIGHT_STEP); }

        private static Color shade( SwingTreeLookAndFeel.Palette p ) { return LafUtilities.shadeBySteps(p.background(), -SHADE_STEP); }

        /** @return {@link #lit}, dimmed to what it reads as inside a groove. */
        private static Color litInside( SwingTreeLookAndFeel.Palette p ) {
            return LafUtilities.shadeBySteps(p.background(), (int) Math.round(LIGHT_STEP * INSET_DIMMING));
        }

        /** @return {@link #shade}, dimmed to what it reads as inside a groove. */
        private static Color shadeInside( SwingTreeLookAndFeel.Palette p ) {
            return LafUtilities.shadeBySteps(p.background(), -(int) Math.round(SHADE_STEP * INSET_DIMMING));
        }

        /**
         *  Curves a flat fill. One lamp falling on a rounded thing does not leave it one colour: the
         *  shoulder facing the lamp is brighter than the face, and the far edge is already turning
         *  away. That wash along the diagonal is the difference between a control that looks moulded
         *  and one that looks like a coloured rectangle with a halo around it.
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
            // Most of a rounded rectangle's face points straight at the viewer and is one even
            // colour; only the two shoulders turn far enough to catch or lose the light. So the wash
            // is a plateau with a roll-off at each end rather than a constant slope, which is what a
            // curved face does and a flat one held at an angle does not.
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
         *  Each of the two fades along the curve that matches what it stands for. The shadow uses
         *  {@link UI.ShadowFalloff#BLUR}, the profile a hard edge takes when it is convolved with a
         *  blur, so it leaves the shape at full strength and reaches nothing with no slope at either
         *  end; the surface then swells out of the panel instead of stepping up out of it. The
         *  highlight uses the bell of {@link UI.ShadowFalloff#GLOW}, because a sheen is light bleeding
         *  off a shoulder rather than an edge being cast.
         *  <p>
         *  An outer shadow is drawn in the component's own margin and cut off at the component
         *  bounds, so {@code reach} - the offset and the blur together - must not exceed the margin
         *  the caller set, or the glow ends in a hard rectangular edge.
         */
        private static <C extends JComponent> ComponentStyleDelegate<C> raised(
                ComponentStyleDelegate<C> it, SwingTreeLookAndFeel.Palette p, int reach
        ) {
            int sheen = Math.max(2, (int) Math.round(reach * SHEEN_REACH));
            return it
                    .shadow(LIT,    s -> s.color(lit(p))
                                          .offset(-offsetOf(sheen), -offsetOf(sheen)).blurRadius(blurOf(sheen))
                                          .falloff(UI.ShadowFalloff.GLOW).isInset(false))
                    .shadow(SHADED, s -> s.color(shade(p))
                                          .offset(offsetOf(reach), offsetOf(reach)).blurRadius(blurOf(reach))
                                          .falloff(UI.ShadowFalloff.BLUR).isInset(false));
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
         *  {@link UI.ShadowFalloff#PENUMBRA} S-curve, and the wall rounds off into the floor instead of
         *  meeting it at a line.
         */
        private static <C extends JComponent> ComponentStyleDelegate<C> sunken(
                ComponentStyleDelegate<C> it, SwingTreeLookAndFeel.Palette p, int depth
        ) {
            return it
                    .shadow(SHADED, s -> s.color(shadeInside(p))
                                          .offset(depth, depth).blurRadius(depth * 2)
                                          .falloff(UI.ShadowFalloff.PENUMBRA).isInset(true))
                    .shadow(LIT,    s -> s.color(litInside(p))
                                          .offset(-depth, -depth).blurRadius(depth * 2)
                                          .falloff(UI.ShadowFalloff.PENUMBRA).isInset(true));
        }

        // ── Surfaces ─────────────────────────────────────────────────────────

        @SuppressWarnings("deprecation") // component() is the documented hook for LAF state reads
        private static ComponentStyleDelegate<JPanel> panel( Theme theme, ComponentStyleDelegate<JPanel> it ) {
            SwingTreeLookAndFeel.Palette p = theme.palette();
            it = it.foregroundColor(p.text());
            switch ( SwingTreeLookAndFeel.Surface.of(it.component()) ) {
                case CARD:
                    return raised(curved(it.backgroundColor(p.surface()), p.surface(), false)
                                    .borderRadius(22).borderWidth(0).margin(8), p, 8);
                case RAIL:
                    return it.backgroundColor(p.surface()).borderWidth(0);
                case TRANSPARENT:
                    return it.backgroundColor(SwingTreeLookAndFeel.Palette.TRANSPARENT);
                case WINDOW:
                default:
                    return it.backgroundColor(p.background());
            }
        }

        @SuppressWarnings("deprecation")
        private static ComponentStyleDelegate<JScrollPane> scrollPane( Theme theme, ComponentStyleDelegate<JScrollPane> it ) {
            SwingTreeLookAndFeel.Palette p = theme.palette();
            it = it.foregroundColor(p.text());
            switch ( SwingTreeLookAndFeel.Surface.of(it.component()) ) {
                case TRANSPARENT:
                    return it.backgroundColor(SwingTreeLookAndFeel.Palette.TRANSPARENT).borderWidth(0).borderRadius(0).padding(0);
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
        private static ComponentStyleDelegate<JViewport> viewport( Theme theme, ComponentStyleDelegate<JViewport> it ) {
            return it.backgroundColor(SwingTreeLookAndFeel.Palette.TRANSPARENT).foregroundColor(theme.palette().text());
        }

        // ── Controls ─────────────────────────────────────────────────────────

        @SuppressWarnings("deprecation")
        private static ComponentStyleDelegate<AbstractButton> button( Theme theme, ComponentStyleDelegate<AbstractButton> it ) {
            SwingTreeLookAndFeel.Palette p = theme.palette();
            AbstractButton b = it.component();
            ButtonModel    m = b.getModel();

            boolean enabled  = b.isEnabled();
            boolean pressed  = enabled && m.isArmed() && m.isPressed();
            boolean selected = enabled && m.isSelected();
            boolean sunkenIn = pressed || selected;
            boolean rollover = enabled && m.isRollover() && !pressed;
            boolean focused  = enabled && b.isFocusOwner();

            SwingTreeLookAndFeel.Variant variant = SwingTreeLookAndFeel.Variant.of(b);

            // The margin is where the light and the shadow live, so it is never zero; a focus ring
            // is grown out of it rather than added to the footprint.
            int lift = focused ? 5 : 7;

            Color surface = fill(variant, p, enabled, sunkenIn, rollover);

            it = it
                    .margin(lift)
                    .padding(9, 20, 9, 20)
                    .borderRadius(16)
                    .borderWidth(focused ? 2 : 0)
                    .borderColor(focused ? p.accent() : SwingTreeLookAndFeel.Palette.TRANSPARENT)
                    .backgroundColor(surface)
                    .foregroundColor(ink(variant, p, enabled));

            if ( !enabled )
                return it;
            if ( variant == SwingTreeLookAndFeel.Variant.QUIET && !sunkenIn && !rollover )
                return it; // lies flat in the panel until it is reached for
            if ( sunkenIn )
                return sunken(curved(it, surface, true), p, 3);
            return raised(curved(it, surface, false), p, rollover ? lift : lift - 2);
        }

        @SuppressWarnings("deprecation")
        private static <C extends AbstractButton> ComponentStyleDelegate<C> tickable( Theme theme, ComponentStyleDelegate<C> it ) {
            SwingTreeLookAndFeel.Palette p = theme.palette();
            return it
                    .backgroundColor(SwingTreeLookAndFeel.Palette.TRANSPARENT)
                    .foregroundColor(it.component().isEnabled() ? p.text() : p.textDisabled())
                    .padding(3, 5, 3, 5);
        }

        @SuppressWarnings("deprecation")
        private static ComponentStyleDelegate<JComboBox> comboBox( Theme theme, ComponentStyleDelegate<JComboBox> it ) {
            SwingTreeLookAndFeel.Palette p       = theme.palette();
            JComboBox<?> combo   = it.component();
            boolean      enabled = combo.isEnabled();
            boolean      focused = enabled && LafUtilities.hasFocus(combo);
            int   lift    = focused ? 4 : 6;
            Color resting = enabled ? p.surface() : p.surfaceDisabled();
            Color fill    = LafUtilities.underPointer(p, resting, combo);
            it = it
                    .margin(lift)
                    .padding(6, 10, 6, 6)
                    .borderRadius(14)
                    .borderWidth(focused ? 2 : 0)
                    .borderColor(focused ? p.accent() : SwingTreeLookAndFeel.Palette.TRANSPARENT)
                    .backgroundColor(fill)
                    .foregroundColor(enabled ? p.text() : p.textDisabled());
            if ( !enabled )
                return it;
            return raised(curved(it, fill, false), p, lift);
        }

        @SuppressWarnings("deprecation")
        private static ComponentStyleDelegate<JSpinner> spinner( Theme theme, ComponentStyleDelegate<JSpinner> it ) {
            SwingTreeLookAndFeel.Palette p       = theme.palette();
            JSpinner spinner = it.component();
            boolean  enabled = spinner.isEnabled();
            boolean  focused = enabled && LafUtilities.hasFocus(spinner);
            int lift = focused ? 4 : 6;
            it = it
                    .margin(lift)
                    .padding(4)
                    .borderRadius(14)
                    .borderWidth(focused ? 2 : 0)
                    .borderColor(focused ? p.accent() : SwingTreeLookAndFeel.Palette.TRANSPARENT)
                    .backgroundColor(enabled ? p.surface() : p.surfaceDisabled())
                    .foregroundColor(enabled ? p.text() : p.textDisabled());
            if ( !enabled )
                return it;
            return raised(curved(it, p.surface(), false), p, lift);
        }

        // ── Inputs ───────────────────────────────────────────────────────────

        @SuppressWarnings("deprecation")
        private static ComponentStyleDelegate<JTextField> field( Theme theme, ComponentStyleDelegate<JTextField> it ) {
            return input(theme, it, it.component(), 7, 14);
        }

        @SuppressWarnings("deprecation")
        private static <C extends JTextComponent> ComponentStyleDelegate<C> page( Theme theme, ComponentStyleDelegate<C> it ) {
            return input(theme, it, it.component(), 9, 14);
        }

        /**
         *  An input is a hole: the light is on the inside, and focus lights its rim.
         *  <p>
         *  Unless it is scrolled, in which case the hole belongs to the scroll pane around it and this
         *  is only the paper inside. A second sunken box here would fill over the groove the scroll
         *  pane just pressed into its own surface, and leave the document sitting on a hard edge.
         */
        private static <C extends JComponent> ComponentStyleDelegate<C> input(
            Theme theme, ComponentStyleDelegate<C> it, JTextComponent text, int padY, int padX
        ) {
            SwingTreeLookAndFeel.Palette p        = theme.palette();
            boolean editable = text.isEnabled() && text.isEditable();
            boolean focused  = editable && text.isFocusOwner();

            if ( text.getParent() instanceof JViewport )
                return it
                        .margin(0)
                        .padding(padY, padX, padY, padX)
                        .borderWidth(0)
                        .backgroundColor(SwingTreeLookAndFeel.Palette.TRANSPARENT)
                        .foregroundColor(text.isEnabled() ? p.text() : p.textDisabled());

            it = it
                    .margin(focused ? 3 : 5)
                    .padding(padY, padX, padY, padX)
                    .borderRadius(14)
                    .borderWidth(focused ? 2 : 0)
                    .borderColor(focused ? p.accent() : SwingTreeLookAndFeel.Palette.TRANSPARENT)
                    .backgroundColor(editable ? p.surfaceField() : p.surfaceDisabled())
                    .foregroundColor(text.isEnabled() ? p.text() : p.textDisabled());
            return sunken(it, p, 4);
        }

        // ── The rest ─────────────────────────────────────────────────────────

        @SuppressWarnings("deprecation")
        private static ComponentStyleDelegate<JLabel> label( Theme theme, ComponentStyleDelegate<JLabel> it ) {
            SwingTreeLookAndFeel.Palette p = theme.palette();
            return it.foregroundColor(it.component().isEnabled() ? p.text() : p.textDisabled());
        }

        @SuppressWarnings("deprecation")
        private static ComponentStyleDelegate<JMenuItem> menuItem( Theme theme, ComponentStyleDelegate<JMenuItem> it ) {
            SwingTreeLookAndFeel.Palette p       = theme.palette();
            JMenuItem   item    = it.component();
            ButtonModel m       = item.getModel();
            boolean     enabled = item.isEnabled();
            boolean     armed   = enabled && ( m.isArmed() || m.isSelected() );
            return it
                    .padding(5, 10, 5, 10)
                    .borderRadius(12)
                    .borderWidth(0)
                    .backgroundColor(armed ? p.accentSoft() : SwingTreeLookAndFeel.Palette.TRANSPARENT)
                    .foregroundColor(enabled ? p.text() : p.textDisabled());
        }

        private static ComponentStyleDelegate<JMenuBar> menuBar( Theme theme, ComponentStyleDelegate<JMenuBar> it ) {
            SwingTreeLookAndFeel.Palette p = theme.palette();
            return it.backgroundColor(p.background()).foregroundColor(p.text()).padding(3, 6, 3, 6).borderWidth(0);
        }

        private static ComponentStyleDelegate<JPopupMenu> popupMenu( Theme theme, ComponentStyleDelegate<JPopupMenu> it ) {
            SwingTreeLookAndFeel.Palette p = theme.palette();
            return raised(curved(it
                    .backgroundColor(p.surface())
                    .foregroundColor(p.text())
                    .margin(6)
                    .padding(6)
                    .borderRadius(18)
                    .borderWidth(0), p.surface(), false), p, 6);
        }

        private static ComponentStyleDelegate<JToolTip> toolTip( Theme theme, ComponentStyleDelegate<JToolTip> it ) {
            SwingTreeLookAndFeel.Palette p = theme.palette();
            return raised(curved(it
                    .margin(5)
                    .padding(5, 12, 5, 12)
                    .borderRadius(14)
                    .borderWidth(0)
                    .backgroundColor(p.surface())
                    .foregroundColor(p.text()), p.surface(), false), p, 5);
        }

        private static ComponentStyleDelegate<JSeparator> separator( Theme theme, ComponentStyleDelegate<JSeparator> it ) {
            SwingTreeLookAndFeel.Palette p = theme.palette();
            return it.backgroundColor(p.borderSoft()).foregroundColor(p.borderSoft());
        }

        /** The trough is a groove pressed into the panel; the symbol set fills it. */
        private static ComponentStyleDelegate<JProgressBar> progressBar( Theme theme, ComponentStyleDelegate<JProgressBar> it ) {
            SwingTreeLookAndFeel.Palette p = theme.palette();
            return sunken(it
                    .margin(3)
                    .borderRadius(8)
                    .borderWidth(0)
                    .backgroundColor(p.background())
                    .foregroundColor(p.accent()), p, 3);
        }

        private static ComponentStyleDelegate<JSlider> slider( Theme theme, ComponentStyleDelegate<JSlider> it ) {
            SwingTreeLookAndFeel.Palette p = theme.palette();
            return it.backgroundColor(SwingTreeLookAndFeel.Palette.TRANSPARENT).foregroundColor(p.text());
        }

        private static ComponentStyleDelegate<JScrollBar> scrollBar( Theme theme, ComponentStyleDelegate<JScrollBar> it ) {
            SwingTreeLookAndFeel.Palette p = theme.palette();
            return it.backgroundColor(p.background()).foregroundColor(p.border());
        }

        private static ComponentStyleDelegate<JTabbedPane> tabbedPane( Theme theme, ComponentStyleDelegate<JTabbedPane> it ) {
            SwingTreeLookAndFeel.Palette p = theme.palette();
            return it.backgroundColor(p.background()).foregroundColor(p.text());
        }

        private static ComponentStyleDelegate<JSplitPane> splitPane( Theme theme, ComponentStyleDelegate<JSplitPane> it ) {
            SwingTreeLookAndFeel.Palette p = theme.palette();
            return it.backgroundColor(SwingTreeLookAndFeel.Palette.TRANSPARENT).foregroundColor(p.text());
        }

        /**
         *  A list, table or tree is the content of a hole, not a surface of its own: it lets the
         *  scroll pane's field colour and groove through and paints only its rows.
         */
        private static <C extends JComponent> ComponentStyleDelegate<C> flatField( Theme theme, ComponentStyleDelegate<C> it ) {
            return it.backgroundColor(SwingTreeLookAndFeel.Palette.TRANSPARENT).foregroundColor(theme.palette().text());
        }

        private static ComponentStyleDelegate<JTableHeader> tableHeader( Theme theme, ComponentStyleDelegate<JTableHeader> it ) {
            SwingTreeLookAndFeel.Palette p = theme.palette();
            return it.backgroundColor(p.surface()).foregroundColor(p.textMuted());
        }

        private static ComponentStyleDelegate<JToolBar> toolBar( Theme theme, ComponentStyleDelegate<JToolBar> it ) {
            SwingTreeLookAndFeel.Palette p = theme.palette();
            return raised(curved(it
                    .backgroundColor(p.surface())
                    .foregroundColor(p.text())
                    .margin(6)
                    .padding(6, 10, 6, 10)
                    .borderRadius(18)
                    .borderWidth(0), p.surface(), false), p, 6);
        }

        // ── Variant colours ──────────────────────────────────────────────────

        private static Color fill(SwingTreeLookAndFeel.Variant variant, SwingTreeLookAndFeel.Palette p, boolean enabled, boolean sunkenIn, boolean rollover ) {
            if ( !enabled )
                return variant == SwingTreeLookAndFeel.Variant.QUIET ? SwingTreeLookAndFeel.Palette.TRANSPARENT : p.surfaceDisabled();
            switch ( variant ) {
                case PRIMARY: return sunkenIn ? p.primaryPressed() : rollover ? p.primaryHover() : p.primary();
                case DANGER:  return sunkenIn ? p.dangerPressed()  : rollover ? p.dangerHover()  : p.danger();
                case QUIET:   return sunkenIn || rollover ? p.surface() : SwingTreeLookAndFeel.Palette.TRANSPARENT;
                case NEUTRAL:
                default:      return sunkenIn ? p.surfacePressed() : rollover ? p.surfaceHover() : p.surface();
            }
        }

        private static Color ink(SwingTreeLookAndFeel.Variant variant, SwingTreeLookAndFeel.Palette p, boolean enabled ) {
            if ( !enabled )
                return p.textDisabled();
            return variant.isFilled() ? p.onFilled() : p.text();
        }
    }

    /**
     *  <b>Frutiger Aero</b>: the wet, glassy optimism of software from about 2004 to 2012. The move
     *  the whole idiom rests on is the gloss - a fill lighter across its top half, breaking on a
     *  hard line at the middle and darker below, so a surface reads as curved. Around it go a crisp
     *  one-pixel outline, a generous radius and a small drop shadow, and behind it a sky. Pressing
     *  something turns the gloss upside down and sinks the shadow inward.
     *  <p>
     *  All four stops of every gradient are derived from the palette, so a flatter palette than
     *  {@link Palettes#AERO} gives a quieter version rather than a broken one.
     *
     *  @see SwingTreeLookAndFeel.StylePreset#FRUTIGER_AERO
     */
    static final class FrutigerAero
    {
        private FrutigerAero() {}

        /** Where the gloss breaks, as a fraction of the height. Above the middle rather than on it,
         *  which is what makes the highlight read as a reflection and not as a two-tone paint job. */
        private static final double BREAK = 0.48;
        /** The far side of that break. A gradient's stops have to increase, so the hard line is the
         *  smallest step there is rather than no step at all. */
        private static final double BREAK_END = 0.482;

        private static final Tuple<StyleRule> RULES = Tuple.of(
            StyleRule.of(JPanel.class,         FrutigerAero::panel),
            StyleRule.of(AbstractButton.class, FrutigerAero::button),
            StyleRule.of(JCheckBox.class,      FrutigerAero::tickable),
            StyleRule.of(JRadioButton.class,   FrutigerAero::tickable),
            StyleRule.of(JMenuItem.class,      FrutigerAero::menuItem),
            StyleRule.of(JMenuBar.class,       FrutigerAero::menuBar),
            StyleRule.of(JPopupMenu.class,     FrutigerAero::popupMenu),
            StyleRule.of(JLabel.class,         FrutigerAero::label),
            StyleRule.of(JTextField.class,     FrutigerAero::field),
            StyleRule.of(JTextArea.class,      FrutigerAero::page),
            StyleRule.of(JEditorPane.class,    FrutigerAero::page),
            StyleRule.of(JSeparator.class,     FrutigerAero::separator),
            StyleRule.of(JToolTip.class,       FrutigerAero::toolTip),
            StyleRule.of(JProgressBar.class,   FrutigerAero::progressBar),
            StyleRule.of(JSlider.class,        FrutigerAero::slider),
            StyleRule.of(JScrollBar.class,     FrutigerAero::scrollBar),
            StyleRule.of(JScrollPane.class,    FrutigerAero::scrollPane),
            StyleRule.of(JViewport.class,      FrutigerAero::viewport),
            StyleRule.of(JComboBox.class,      FrutigerAero::comboBox),
            StyleRule.of(JSpinner.class,       FrutigerAero::spinner),
            StyleRule.of(JTabbedPane.class,    FrutigerAero::tabbedPane),
            StyleRule.of(JList.class,          FrutigerAero::flatField),
            StyleRule.of(JTable.class,         FrutigerAero::flatField),
            StyleRule.of(JTableHeader.class,   FrutigerAero::tableHeader),
            StyleRule.of(JTree.class,          FrutigerAero::flatField),
            StyleRule.of(JToolBar.class,       FrutigerAero::toolBar),
            StyleRule.of(JSplitPane.class,     FrutigerAero::splitPane)
        );

        static Tuple<StyleRule> rules() { return RULES; }

        // ── The gloss ────────────────────────────────────────────────────────

        /**
         *  The four stops that make a surface look like glass: bright at the top, dimming to the break,
         *  then a jump back up and a gentle darkening to the bottom edge.
         */
        private static GradientConf gloss(GradientConf g, Color base ) {
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
        private static ComponentStyleDelegate<JPanel> panel( Theme theme, ComponentStyleDelegate<JPanel> it ) {
            SwingTreeLookAndFeel.Palette p = theme.palette();
            it = it.foregroundColor(p.text());
            switch ( SwingTreeLookAndFeel.Surface.of(it.component()) ) {
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
                    return it.backgroundColor(SwingTreeLookAndFeel.Palette.TRANSPARENT);
                case WINDOW:
                default:
                    return it.backgroundColor(p.background()).gradient(g -> sky(g, p.background()));
            }
        }

        @SuppressWarnings("deprecation")
        private static ComponentStyleDelegate<JScrollPane> scrollPane( Theme theme, ComponentStyleDelegate<JScrollPane> it ) {
            SwingTreeLookAndFeel.Palette p = theme.palette();
            it = it.foregroundColor(p.text());
            switch ( SwingTreeLookAndFeel.Surface.of(it.component()) ) {
                case TRANSPARENT:
                    return it.backgroundColor(SwingTreeLookAndFeel.Palette.TRANSPARENT).borderWidth(0).borderRadius(0).padding(0);
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
        private static ComponentStyleDelegate<JViewport> viewport( Theme theme, ComponentStyleDelegate<JViewport> it ) {
            SwingTreeLookAndFeel.Palette p = theme.palette();
            it = it.foregroundColor(p.text());
            switch ( SwingTreeLookAndFeel.Surface.of(it.component()) ) {
                case TRANSPARENT: return it.backgroundColor(SwingTreeLookAndFeel.Palette.TRANSPARENT);
                case CARD:
                case RAIL:        return it.backgroundColor(p.surface());
                case WINDOW:
                default:          return it.backgroundColor(p.surfaceField());
            }
        }

        // ── Controls ─────────────────────────────────────────────────────────

        @SuppressWarnings("deprecation")
        private static ComponentStyleDelegate<AbstractButton> button( Theme theme, ComponentStyleDelegate<AbstractButton> it ) {
            SwingTreeLookAndFeel.Palette p = theme.palette();
            AbstractButton b = it.component();
            ButtonModel    m = b.getModel();

            boolean enabled  = b.isEnabled();
            boolean pressed  = enabled && m.isArmed() && m.isPressed();
            boolean selected = enabled && m.isSelected();
            boolean sunken   = pressed || selected;
            boolean rollover = enabled && m.isRollover() && !pressed;
            boolean focused  = enabled && b.isFocusOwner();

            SwingTreeLookAndFeel.Variant variant = SwingTreeLookAndFeel.Variant.of(b);
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
            if ( variant == SwingTreeLookAndFeel.Variant.QUIET && !sunken && !rollover )
                return it.borderColor(SwingTreeLookAndFeel.Palette.TRANSPARENT).backgroundColor(SwingTreeLookAndFeel.Palette.TRANSPARENT);
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
        private static <C extends AbstractButton> ComponentStyleDelegate<C> tickable( Theme theme, ComponentStyleDelegate<C> it ) {
            SwingTreeLookAndFeel.Palette p = theme.palette();
            return it
                    .backgroundColor(SwingTreeLookAndFeel.Palette.TRANSPARENT)
                    .foregroundColor(it.component().isEnabled() ? p.text() : p.textDisabled())
                    .padding(2, 4, 2, 4);
        }

        @SuppressWarnings("deprecation")
        private static ComponentStyleDelegate<JComboBox> comboBox( Theme theme, ComponentStyleDelegate<JComboBox> it ) {
            SwingTreeLookAndFeel.Palette p       = theme.palette();
            JComboBox<?> combo   = it.component();
            boolean      enabled = combo.isEnabled();
            boolean      focused = enabled && LafUtilities.hasFocus(combo);
            Color        base    = LafUtilities.underPointer(
                                        p, enabled ? p.surfaceField() : p.surfaceDisabled(), combo);
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
        private static ComponentStyleDelegate<JSpinner> spinner( Theme theme, ComponentStyleDelegate<JSpinner> it ) {
            SwingTreeLookAndFeel.Palette p       = theme.palette();
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
        private static ComponentStyleDelegate<JTextField> field( Theme theme, ComponentStyleDelegate<JTextField> it ) {
            return input(theme, it, it.component(), 5, 9);
        }

        @SuppressWarnings("deprecation")
        private static <C extends JTextComponent> ComponentStyleDelegate<C> page( Theme theme, ComponentStyleDelegate<C> it ) {
            return input(theme, it, it.component(), 7, 9);
        }

        /** An input is clear glass over white: no gloss on the fill, a shadow cast inward from the
         *  top edge, and an accent ring the moment it takes focus. */
        private static <C extends JComponent> ComponentStyleDelegate<C> input(
            Theme theme, ComponentStyleDelegate<C> it, JTextComponent text, int padY, int padX
        ) {
            SwingTreeLookAndFeel.Palette p        = theme.palette();
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
        private static ComponentStyleDelegate<JLabel> label( Theme theme, ComponentStyleDelegate<JLabel> it ) {
            SwingTreeLookAndFeel.Palette p = theme.palette();
            return it.foregroundColor(it.component().isEnabled() ? p.text() : p.textDisabled());
        }

        @SuppressWarnings("deprecation")
        private static ComponentStyleDelegate<JMenuItem> menuItem( Theme theme, ComponentStyleDelegate<JMenuItem> it ) {
            SwingTreeLookAndFeel.Palette p       = theme.palette();
            JMenuItem   item    = it.component();
            ButtonModel m       = item.getModel();
            boolean     enabled = item.isEnabled();
            boolean     armed   = enabled && ( m.isArmed() || m.isSelected() );
            it = it
                    .padding(4, 9, 4, 9)
                    .borderRadius(7)
                    .borderWidth(0)
                    .backgroundColor(armed ? p.accent() : SwingTreeLookAndFeel.Palette.TRANSPARENT)
                    .foregroundColor(!enabled ? p.textDisabled() : armed ? p.onFilled() : p.text());
            return armed ? it.gradient(g -> gloss(g, p.accent())) : it;
        }

        private static ComponentStyleDelegate<JMenuBar> menuBar( Theme theme, ComponentStyleDelegate<JMenuBar> it ) {
            SwingTreeLookAndFeel.Palette p = theme.palette();
            return it
                    .backgroundColor(p.surface())
                    .foregroundColor(p.text())
                    .padding(2, 4, 2, 4)
                    .gradient(g -> gloss(g, p.surface()))
                    .borderAt(UI.Edge.BOTTOM, 1, p.border());
        }

        private static ComponentStyleDelegate<JPopupMenu> popupMenu( Theme theme, ComponentStyleDelegate<JPopupMenu> it ) {
            SwingTreeLookAndFeel.Palette p = theme.palette();
            return lifted(it
                    .backgroundColor(p.surfaceField())
                    .foregroundColor(p.text())
                    .margin(3)
                    .padding(4)
                    .borderRadius(10)
                    .border(1, p.border()), 12, 90);
        }

        private static ComponentStyleDelegate<JToolTip> toolTip( Theme theme, ComponentStyleDelegate<JToolTip> it ) {
            SwingTreeLookAndFeel.Palette p = theme.palette();
            return lifted(it
                    .margin(3)
                    .padding(4, 10, 4, 10)
                    .borderRadius(8)
                    .border(1, p.border())
                    .backgroundColor(p.surfaceField())
                    .foregroundColor(p.text())
                    .gradient(g -> gloss(g, p.surfaceField())), 8, 80);
        }

        private static ComponentStyleDelegate<JSeparator> separator( Theme theme, ComponentStyleDelegate<JSeparator> it ) {
            SwingTreeLookAndFeel.Palette p = theme.palette();
            return it.backgroundColor(p.borderSoft()).foregroundColor(p.borderSoft());
        }

        private static ComponentStyleDelegate<JProgressBar> progressBar( Theme theme, ComponentStyleDelegate<JProgressBar> it ) {
            SwingTreeLookAndFeel.Palette p = theme.palette();
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

        private static ComponentStyleDelegate<JSlider> slider( Theme theme, ComponentStyleDelegate<JSlider> it ) {
            SwingTreeLookAndFeel.Palette p = theme.palette();
            return it.backgroundColor(SwingTreeLookAndFeel.Palette.TRANSPARENT).foregroundColor(p.text());
        }

        private static ComponentStyleDelegate<JScrollBar> scrollBar( Theme theme, ComponentStyleDelegate<JScrollBar> it ) {
            SwingTreeLookAndFeel.Palette p = theme.palette();
            return it.backgroundColor(p.surfaceDisabled()).foregroundColor(p.border());
        }

        private static ComponentStyleDelegate<JTabbedPane> tabbedPane( Theme theme, ComponentStyleDelegate<JTabbedPane> it ) {
            SwingTreeLookAndFeel.Palette p = theme.palette();
            return it.backgroundColor(p.background()).foregroundColor(p.text());
        }

        private static ComponentStyleDelegate<JSplitPane> splitPane( Theme theme, ComponentStyleDelegate<JSplitPane> it ) {
            SwingTreeLookAndFeel.Palette p = theme.palette();
            return it.backgroundColor(SwingTreeLookAndFeel.Palette.TRANSPARENT).foregroundColor(p.text());
        }

        private static <C extends JComponent> ComponentStyleDelegate<C> flatField( Theme theme, ComponentStyleDelegate<C> it ) {
            SwingTreeLookAndFeel.Palette p = theme.palette();
            return it.backgroundColor(p.surfaceField()).foregroundColor(p.text());
        }

        private static ComponentStyleDelegate<JTableHeader> tableHeader( Theme theme, ComponentStyleDelegate<JTableHeader> it ) {
            SwingTreeLookAndFeel.Palette p = theme.palette();
            return it
                    .backgroundColor(p.surface())
                    .foregroundColor(p.textMuted())
                    .gradient(g -> gloss(g, p.surface()))
                    .borderAt(UI.Edge.BOTTOM, 1, p.border());
        }

        private static ComponentStyleDelegate<JToolBar> toolBar( Theme theme, ComponentStyleDelegate<JToolBar> it ) {
            SwingTreeLookAndFeel.Palette p = theme.palette();
            return it
                    .backgroundColor(p.surface())
                    .foregroundColor(p.text())
                    .padding(4, 8, 4, 8)
                    .borderRadius(10)
                    .border(1, p.borderSoft())
                    .gradient(g -> gloss(g, p.surface()));
        }

        // ── Variant colours ──────────────────────────────────────────────────

        private static Color fill(SwingTreeLookAndFeel.Variant variant, SwingTreeLookAndFeel.Palette p, boolean enabled, boolean sunken, boolean rollover ) {
            if ( !enabled )
                return variant == SwingTreeLookAndFeel.Variant.QUIET ? SwingTreeLookAndFeel.Palette.TRANSPARENT : p.surfaceDisabled();
            switch ( variant ) {
                case PRIMARY: return sunken ? p.primaryPressed() : rollover ? p.primaryHover() : p.primary();
                case DANGER:  return sunken ? p.dangerPressed()  : rollover ? p.dangerHover()  : p.danger();
                case QUIET:   return sunken ? p.surfacePressed() : rollover ? p.surfaceHover() : SwingTreeLookAndFeel.Palette.TRANSPARENT;
                case NEUTRAL:
                default:      return sunken ? p.surfacePressed() : rollover ? p.surfaceHover() : p.surface();
            }
        }

        private static Color outline(SwingTreeLookAndFeel.Variant variant, SwingTreeLookAndFeel.Palette p, boolean enabled ) {
            if ( !enabled )
                return variant == SwingTreeLookAndFeel.Variant.QUIET ? SwingTreeLookAndFeel.Palette.TRANSPARENT : p.borderSoft();
            switch ( variant ) {
                case PRIMARY: return p.primaryPressed();
                case DANGER:  return p.dangerPressed();
                case QUIET:   return SwingTreeLookAndFeel.Palette.TRANSPARENT;
                case NEUTRAL:
                default:      return p.border();
            }
        }

        private static Color ink(SwingTreeLookAndFeel.Variant variant, SwingTreeLookAndFeel.Palette p, boolean enabled ) {
            if ( !enabled )
                return p.textDisabled();
            return variant.isFilled() ? p.onFilled() : p.text();
        }
    }

    /**
     *  <b>Material</b>: flat surfaces at different heights above the page. Nothing is shaded,
     *  bevelled or glossy, and the only thing saying one surface is above another is the shadow it
     *  casts, so the shadows come in named steps ({@link #elevation}) instead of being tuned per
     *  component. A card sits one step up, a menu three, a pressed button one more than it was.
     *  <p>
     *  Buttons come in the idiom's three kinds rather than in three colours: an ordinary one is
     *  outlined, the one affirmative or destructive one is filled and is the only thing casting a
     *  shadow at rest, and an in-place one has no box at all until the pointer arrives.
     *
     *  @see SwingTreeLookAndFeel.StylePreset#MATERIAL
     */
    static final class Material
    {
        private Material() {}

        /** The one radius the whole theme uses, in developer pixels. */
        private static final int RADIUS = 4;

        private static final Tuple<StyleRule> RULES = Tuple.of(
            StyleRule.of(JPanel.class,         Material::panel),
            StyleRule.of(AbstractButton.class, Material::button),
            StyleRule.of(JCheckBox.class,      Material::tickable),
            StyleRule.of(JRadioButton.class,   Material::tickable),
            StyleRule.of(JMenuItem.class,      Material::menuItem),
            StyleRule.of(JMenuBar.class,       Material::menuBar),
            StyleRule.of(JPopupMenu.class,     Material::popupMenu),
            StyleRule.of(JLabel.class,         Material::label),
            StyleRule.of(JTextField.class,     Material::field),
            StyleRule.of(JTextArea.class,      Material::page),
            StyleRule.of(JEditorPane.class,    Material::page),
            StyleRule.of(JSeparator.class,     Material::separator),
            StyleRule.of(JToolTip.class,       Material::toolTip),
            StyleRule.of(JProgressBar.class,   Material::progressBar),
            StyleRule.of(JSlider.class,        Material::slider),
            StyleRule.of(JScrollBar.class,     Material::scrollBar),
            StyleRule.of(JScrollPane.class,    Material::scrollPane),
            StyleRule.of(JViewport.class,      Material::viewport),
            StyleRule.of(JComboBox.class,      Material::comboBox),
            StyleRule.of(JSpinner.class,       Material::spinner),
            StyleRule.of(JTabbedPane.class,    Material::tabbedPane),
            StyleRule.of(JList.class,          Material::flatField),
            StyleRule.of(JTable.class,         Material::flatField),
            StyleRule.of(JTableHeader.class,   Material::tableHeader),
            StyleRule.of(JTree.class,          Material::flatField),
            StyleRule.of(JToolBar.class,       Material::toolBar),
            StyleRule.of(JSplitPane.class,     Material::splitPane)
        );

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
        private static ComponentStyleDelegate<JPanel> panel( Theme theme, ComponentStyleDelegate<JPanel> it ) {
            SwingTreeLookAndFeel.Palette p = theme.palette();
            it = it.foregroundColor(p.text());
            switch ( SwingTreeLookAndFeel.Surface.of(it.component()) ) {
                case CARD:
                    return elevation(it.backgroundColor(p.surface()).borderRadius(RADIUS).borderWidth(0).margin(3), 2);
                case RAIL:
                    return elevation(it.backgroundColor(p.surface()).borderWidth(0), 1);
                case TRANSPARENT:
                    return it.backgroundColor(SwingTreeLookAndFeel.Palette.TRANSPARENT);
                case WINDOW:
                default:
                    return it.backgroundColor(p.background());
            }
        }

        @SuppressWarnings("deprecation")
        private static ComponentStyleDelegate<JScrollPane> scrollPane( Theme theme, ComponentStyleDelegate<JScrollPane> it ) {
            SwingTreeLookAndFeel.Palette p = theme.palette();
            it = it.foregroundColor(p.text());
            switch ( SwingTreeLookAndFeel.Surface.of(it.component()) ) {
                case TRANSPARENT:
                    return it.backgroundColor(SwingTreeLookAndFeel.Palette.TRANSPARENT).borderWidth(0).borderRadius(0).padding(0);
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
        private static ComponentStyleDelegate<JViewport> viewport( Theme theme, ComponentStyleDelegate<JViewport> it ) {
            SwingTreeLookAndFeel.Palette p = theme.palette();
            it = it.foregroundColor(p.text());
            switch ( SwingTreeLookAndFeel.Surface.of(it.component()) ) {
                case TRANSPARENT: return it.backgroundColor(SwingTreeLookAndFeel.Palette.TRANSPARENT);
                case WINDOW:
                case CARD:
                case RAIL:
                default:          return it.backgroundColor(p.surface());
            }
        }

        // ── Controls ─────────────────────────────────────────────────────────

        /**
         *  The three kinds of button, told apart by the {@link SwingTreeLookAndFeel.Variant} the application tagged:
         *  contained for the affirmative and the destructive one, text for an in-place command,
         *  outlined for everything else.
         */
        @SuppressWarnings("deprecation")
        private static ComponentStyleDelegate<AbstractButton> button( Theme theme, ComponentStyleDelegate<AbstractButton> it ) {
            SwingTreeLookAndFeel.Palette p = theme.palette();
            AbstractButton b = it.component();
            ButtonModel    m = b.getModel();

            boolean enabled  = b.isEnabled();
            boolean pressed  = enabled && m.isArmed() && m.isPressed();
            boolean selected = enabled && m.isSelected();
            boolean sunken   = pressed || selected;
            boolean rollover = enabled && m.isRollover() && !pressed;
            boolean focused  = enabled && b.isFocusOwner();

            SwingTreeLookAndFeel.Variant variant   = SwingTreeLookAndFeel.Variant.of(b);
            boolean contained = variant.isFilled();

            boolean outlined = !contained && variant != SwingTreeLookAndFeel.Variant.QUIET;
            it = it
                    // The ring grows into the margin, so taking focus never moves the row.
                    .margin(focused ? 1 : 2)
                    .padding(8, 16, 8, 16)
                    .borderRadius(RADIUS)
                    .borderWidth(focused ? 2 : 1)
                    .borderColor(focused ? p.accent()
                                         : outlined ? p.border() : SwingTreeLookAndFeel.Palette.TRANSPARENT)
                    .backgroundColor(fill(variant, p, enabled, sunken, rollover))
                    .foregroundColor(ink(variant, p, enabled));

            if ( !enabled || !contained )
                return it;
            // Only a contained button is above the page, and pressing it lifts it further.
            return elevation(it, sunken ? 4 : rollover ? 3 : 2);
        }

        @SuppressWarnings("deprecation")
        private static <C extends AbstractButton> ComponentStyleDelegate<C> tickable( Theme theme, ComponentStyleDelegate<C> it ) {
            SwingTreeLookAndFeel.Palette p = theme.palette();
            return it
                    .backgroundColor(SwingTreeLookAndFeel.Palette.TRANSPARENT)
                    .foregroundColor(it.component().isEnabled() ? p.text() : p.textDisabled())
                    .padding(2, 4, 2, 4);
        }

        @SuppressWarnings("deprecation")
        private static ComponentStyleDelegate<JComboBox> comboBox( Theme theme, ComponentStyleDelegate<JComboBox> it ) {
            SwingTreeLookAndFeel.Palette p       = theme.palette();
            JComboBox<?> combo   = it.component();
            boolean      enabled = combo.isEnabled();
            boolean      focused = enabled && LafUtilities.hasFocus(combo);
            return underlined(it, p, enabled, focused, 6, 10, 4);
        }

        @SuppressWarnings("deprecation")
        private static ComponentStyleDelegate<JSpinner> spinner( Theme theme, ComponentStyleDelegate<JSpinner> it ) {
            SwingTreeLookAndFeel.Palette p       = theme.palette();
            JSpinner spinner = it.component();
            boolean  enabled = spinner.isEnabled();
            boolean  focused = enabled && LafUtilities.hasFocus(spinner);
            return underlined(it, p, enabled, focused, 4, 6, 4);
        }

        // ── Inputs ───────────────────────────────────────────────────────────

        @SuppressWarnings("deprecation")
        private static ComponentStyleDelegate<JTextField> field( Theme theme, ComponentStyleDelegate<JTextField> it ) {
            JTextField f        = it.component();
            boolean    editable = f.isEnabled() && f.isEditable();
            return underlined(it, theme.palette(), editable, editable && f.isFocusOwner(), 8, 12, 12)
                    .foregroundColor(f.isEnabled() ? theme.palette().text()
                                                   : theme.palette().textDisabled());
        }

        @SuppressWarnings("deprecation")
        private static <C extends JTextComponent> ComponentStyleDelegate<C> page( Theme theme, ComponentStyleDelegate<C> it ) {
            JTextComponent t        = it.component();
            boolean        editable = t.isEnabled() && t.isEditable();
            return underlined(it, theme.palette(), editable, editable && t.isFocusOwner(), 9, 12, 12)
                    .foregroundColor(t.isEnabled() ? theme.palette().text()
                                                   : theme.palette().textDisabled());
        }

        /**
         *  The filled box every input in the idiom sits in: rounded at the top only, because it stands
         *  on the rule underneath rather than floating, and that rule thickens into the accent colour
         *  the moment the field takes focus.
         */
        @SuppressWarnings("deprecation") // component() is the documented hook for LAF state reads
        private static <C extends JComponent> ComponentStyleDelegate<C> underlined(
                ComponentStyleDelegate<C> it, SwingTreeLookAndFeel.Palette p, boolean enabled, boolean focused, int padY, int padX, int padRight
        ) {
            Color resting = enabled ? p.surfaceField() : p.surfaceDisabled();
            return it
                    // The rule is a border, so it grows downwards; the margin gives back what it takes.
                    .margin(0, 0, focused ? 0 : 1, 0)
                    .padding(padY, padRight, padY, padX)
                    .borderRadiusAt(UI.Corner.TOP_LEFT, RADIUS, RADIUS)
                    .borderRadiusAt(UI.Corner.TOP_RIGHT, RADIUS, RADIUS)
                    .borderAt(UI.Edge.BOTTOM, focused ? 2 : 1, focused ? p.accent() : p.border())
                    .backgroundColor(LafUtilities.underPointer(p, resting, it.component()))
                    .foregroundColor(enabled ? p.text() : p.textDisabled());
        }

        // ── The rest ─────────────────────────────────────────────────────────

        @SuppressWarnings("deprecation")
        private static ComponentStyleDelegate<JLabel> label( Theme theme, ComponentStyleDelegate<JLabel> it ) {
            SwingTreeLookAndFeel.Palette p = theme.palette();
            return it.foregroundColor(it.component().isEnabled() ? p.text() : p.textDisabled());
        }

        @SuppressWarnings("deprecation")
        private static ComponentStyleDelegate<JMenuItem> menuItem( Theme theme, ComponentStyleDelegate<JMenuItem> it ) {
            SwingTreeLookAndFeel.Palette p       = theme.palette();
            JMenuItem   item    = it.component();
            ButtonModel m       = item.getModel();
            boolean     enabled = item.isEnabled();
            boolean     armed   = enabled && ( m.isArmed() || m.isSelected() );
            return it
                    .padding(6, 12, 6, 12)
                    .borderRadius(0)
                    .borderWidth(0)
                    .backgroundColor(armed ? p.accentSoft() : SwingTreeLookAndFeel.Palette.TRANSPARENT)
                    .foregroundColor(enabled ? p.text() : p.textDisabled());
        }

        private static ComponentStyleDelegate<JMenuBar> menuBar( Theme theme, ComponentStyleDelegate<JMenuBar> it ) {
            SwingTreeLookAndFeel.Palette p = theme.palette();
            return elevation(it
                    .backgroundColor(p.surface())
                    .foregroundColor(p.text())
                    .padding(2, 4, 2, 4)
                    .borderWidth(0), 1);
        }

        private static ComponentStyleDelegate<JPopupMenu> popupMenu( Theme theme, ComponentStyleDelegate<JPopupMenu> it ) {
            SwingTreeLookAndFeel.Palette p = theme.palette();
            return elevation(it
                    .backgroundColor(p.surface())
                    .foregroundColor(p.text())
                    .margin(4)
                    .padding(4, 0, 4, 0)
                    .borderRadius(RADIUS)
                    .borderWidth(0), 4);
        }

        private static ComponentStyleDelegate<JToolTip> toolTip( Theme theme, ComponentStyleDelegate<JToolTip> it ) {
            SwingTreeLookAndFeel.Palette p = theme.palette();
            return it
                    .padding(6, 10, 6, 10)
                    .borderRadius(RADIUS)
                    .borderWidth(0)
                    .backgroundColor(LafUtilities.withOpacity(p.text(), 229))
                    .foregroundColor(p.onFilled());
        }

        private static ComponentStyleDelegate<JSeparator> separator( Theme theme, ComponentStyleDelegate<JSeparator> it ) {
            SwingTreeLookAndFeel.Palette p = theme.palette();
            return it.backgroundColor(p.borderSoft()).foregroundColor(p.borderSoft());
        }

        private static ComponentStyleDelegate<JProgressBar> progressBar( Theme theme, ComponentStyleDelegate<JProgressBar> it ) {
            SwingTreeLookAndFeel.Palette p = theme.palette();
            return it
                    .borderRadius(3)
                    .borderWidth(0)
                    .backgroundColor(p.accentSoft())
                    .foregroundColor(p.accent());
        }

        private static ComponentStyleDelegate<JSlider> slider( Theme theme, ComponentStyleDelegate<JSlider> it ) {
            SwingTreeLookAndFeel.Palette p = theme.palette();
            return it.backgroundColor(SwingTreeLookAndFeel.Palette.TRANSPARENT).foregroundColor(p.text());
        }

        private static ComponentStyleDelegate<JScrollBar> scrollBar( Theme theme, ComponentStyleDelegate<JScrollBar> it ) {
            SwingTreeLookAndFeel.Palette p = theme.palette();
            return it.backgroundColor(p.background()).foregroundColor(p.border());
        }

        private static ComponentStyleDelegate<JTabbedPane> tabbedPane( Theme theme, ComponentStyleDelegate<JTabbedPane> it ) {
            SwingTreeLookAndFeel.Palette p = theme.palette();
            return it.backgroundColor(p.background()).foregroundColor(p.text());
        }

        private static ComponentStyleDelegate<JSplitPane> splitPane( Theme theme, ComponentStyleDelegate<JSplitPane> it ) {
            SwingTreeLookAndFeel.Palette p = theme.palette();
            return it.backgroundColor(SwingTreeLookAndFeel.Palette.TRANSPARENT).foregroundColor(p.text());
        }

        private static <C extends JComponent> ComponentStyleDelegate<C> flatField( Theme theme, ComponentStyleDelegate<C> it ) {
            SwingTreeLookAndFeel.Palette p = theme.palette();
            return it.backgroundColor(p.surface()).foregroundColor(p.text());
        }

        private static ComponentStyleDelegate<JTableHeader> tableHeader( Theme theme, ComponentStyleDelegate<JTableHeader> it ) {
            SwingTreeLookAndFeel.Palette p = theme.palette();
            return it
                    .backgroundColor(p.surface())
                    .foregroundColor(p.textMuted())
                    .borderAt(UI.Edge.BOTTOM, 1, p.borderSoft());
        }

        private static ComponentStyleDelegate<JToolBar> toolBar( Theme theme, ComponentStyleDelegate<JToolBar> it ) {
            SwingTreeLookAndFeel.Palette p = theme.palette();
            return elevation(it
                    .backgroundColor(p.surface())
                    .foregroundColor(p.text())
                    .padding(4, 8, 4, 8)
                    .borderRadius(RADIUS)
                    .borderWidth(0), 1);
        }

        // ── Variant colours ──────────────────────────────────────────────────

        private static Color fill(SwingTreeLookAndFeel.Variant variant, SwingTreeLookAndFeel.Palette p, boolean enabled, boolean sunken, boolean rollover ) {
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
                                   : SwingTreeLookAndFeel.Palette.TRANSPARENT;
            }
        }

        private static Color ink(SwingTreeLookAndFeel.Variant variant, SwingTreeLookAndFeel.Palette p, boolean enabled ) {
            if ( !enabled )
                return p.textDisabled();
            // An outlined or text button carries the accent as its label; a contained one has to be
            // legible on top of a saturated fill instead.
            return variant.isFilled() ? p.onFilled() : p.accent();
        }
    }

    /**
     *  <b>Flat design</b>: no shadow, no gradient, no bevel and no rounded corner anywhere. With
     *  depth given up, colour carries everything and is never mixed: a control at rest is a plain
     *  grey rectangle, pale accent under the pointer, and full accent with an inverted label when
     *  pressed. Every control climbs that same three-step ladder.
     *  <p>
     *  With no relief left, a thing that can be pressed looks like a thing that cannot, so the idiom
     *  pays for it with edges: every input carries a hard rule around it, and focus doubles that rule
     *  rather than adding a glow.
     *
     *  @see SwingTreeLookAndFeel.StylePreset#FLAT
     */
    static final class FlatDesign
    {
        private FlatDesign() {}

        private static final Tuple<StyleRule> RULES = Tuple.of(
            StyleRule.of(JPanel.class,         FlatDesign::panel),
            StyleRule.of(AbstractButton.class, FlatDesign::button),
            StyleRule.of(JCheckBox.class,      FlatDesign::tickable),
            StyleRule.of(JRadioButton.class,   FlatDesign::tickable),
            StyleRule.of(JMenuItem.class,      FlatDesign::menuItem),
            StyleRule.of(JMenuBar.class,       FlatDesign::menuBar),
            StyleRule.of(JPopupMenu.class,     FlatDesign::popupMenu),
            StyleRule.of(JLabel.class,         FlatDesign::label),
            StyleRule.of(JTextField.class,     FlatDesign::field),
            StyleRule.of(JTextArea.class,      FlatDesign::page),
            StyleRule.of(JEditorPane.class,    FlatDesign::page),
            StyleRule.of(JSeparator.class,     FlatDesign::separator),
            StyleRule.of(JToolTip.class,       FlatDesign::toolTip),
            StyleRule.of(JProgressBar.class,   FlatDesign::progressBar),
            StyleRule.of(JSlider.class,        FlatDesign::bare),
            StyleRule.of(JScrollBar.class,     FlatDesign::scrollBar),
            StyleRule.of(JScrollPane.class,    FlatDesign::scrollPane),
            StyleRule.of(JViewport.class,      FlatDesign::viewport),
            StyleRule.of(JComboBox.class,      FlatDesign::comboBox),
            StyleRule.of(JSpinner.class,       FlatDesign::spinner),
            StyleRule.of(JTabbedPane.class,    FlatDesign::tabbedPane),
            StyleRule.of(JList.class,          FlatDesign::content),
            StyleRule.of(JTable.class,         FlatDesign::content),
            StyleRule.of(JTableHeader.class,   FlatDesign::tableHeader),
            StyleRule.of(JTree.class,          FlatDesign::content),
            StyleRule.of(JToolBar.class,       FlatDesign::toolBar),
            StyleRule.of(JSplitPane.class,     FlatDesign::bare)
        );

        static Tuple<StyleRule> rules() { return RULES; }

        // ── Surfaces ─────────────────────────────────────────────────────────

        @SuppressWarnings("deprecation") // component() is the documented hook for LAF state reads
        private static ComponentStyleDelegate<JPanel> panel( Theme theme, ComponentStyleDelegate<JPanel> it ) {
            SwingTreeLookAndFeel.Palette p = theme.palette();
            it = it.foregroundColor(p.text());
            switch ( SwingTreeLookAndFeel.Surface.of(it.component()) ) {
                // A card is told from the ground by the gap of ground left around it, since there is
                // no shadow left to raise it and no radius left to shape it.
                case CARD:        return it.backgroundColor(p.surface()).borderWidth(0).margin(6);
                case RAIL:        return it.backgroundColor(p.surface()).borderWidth(0);
                case TRANSPARENT: return it.backgroundColor(SwingTreeLookAndFeel.Palette.TRANSPARENT);
                case WINDOW:
                default:          return it.backgroundColor(
                                            LafUtilities.isControlInternal(it.component()) ? SwingTreeLookAndFeel.Palette.TRANSPARENT
                                                                                           : p.background());
            }
        }

        @SuppressWarnings("deprecation")
        private static ComponentStyleDelegate<JScrollPane> scrollPane( Theme theme, ComponentStyleDelegate<JScrollPane> it ) {
            SwingTreeLookAndFeel.Palette p = theme.palette();
            it = it.foregroundColor(p.text()).borderRadius(0);
            switch ( SwingTreeLookAndFeel.Surface.of(it.component()) ) {
                case TRANSPARENT: return it.backgroundColor(SwingTreeLookAndFeel.Palette.TRANSPARENT).borderWidth(0).padding(0);
                case CARD:        return it.backgroundColor(p.surface()).borderWidth(0).margin(6);
                case RAIL:        return it.backgroundColor(p.surface()).borderWidth(0).padding(0);
                case WINDOW:
                default:          return it.backgroundColor(p.surfaceField()).border(1, p.borderSoft());
            }
        }

        @SuppressWarnings("deprecation")
        private static ComponentStyleDelegate<JViewport> viewport( Theme theme, ComponentStyleDelegate<JViewport> it ) {
            SwingTreeLookAndFeel.Palette p = theme.palette();
            it = it.foregroundColor(p.text());
            switch ( SwingTreeLookAndFeel.Surface.of(it.component()) ) {
                case TRANSPARENT: return it.backgroundColor(SwingTreeLookAndFeel.Palette.TRANSPARENT);
                case CARD:
                case RAIL:        return it.backgroundColor(p.surface());
                case WINDOW:
                default:          return it.backgroundColor(p.surfaceField());
            }
        }

        // ── Controls ─────────────────────────────────────────────────────────

        @SuppressWarnings("deprecation")
        private static ComponentStyleDelegate<AbstractButton> button( Theme theme, ComponentStyleDelegate<AbstractButton> it ) {
            SwingTreeLookAndFeel.Palette p = theme.palette();
            AbstractButton b = it.component();
            ButtonModel    m = b.getModel();

            boolean enabled  = b.isEnabled();
            boolean pressed  = enabled && m.isArmed() && m.isPressed();
            boolean sunken   = pressed || ( enabled && m.isSelected() );
            boolean rollover = enabled && m.isRollover() && !pressed;
            boolean focused  = enabled && b.isFocusOwner();

            SwingTreeLookAndFeel.Variant variant = SwingTreeLookAndFeel.Variant.of(b);
            return it
                    // The focus ring grows into the footprint the margin was holding, so a button
                    // taking focus never moves the row it sits in.
                    .margin(focused ? 0 : 2)
                    .padding(9, 18, 9, 18)
                    .borderRadius(0)
                    .borderWidth(focused ? 2 : 0)
                    .borderColor(focused ? p.accent() : SwingTreeLookAndFeel.Palette.TRANSPARENT)
                    .backgroundColor(fill(variant, p, enabled, sunken, rollover))
                    .foregroundColor(ink(variant, p, enabled, sunken));
        }

        @SuppressWarnings("deprecation")
        private static <C extends AbstractButton> ComponentStyleDelegate<C> tickable( Theme theme, ComponentStyleDelegate<C> it ) {
            SwingTreeLookAndFeel.Palette p = theme.palette();
            return it
                    .backgroundColor(SwingTreeLookAndFeel.Palette.TRANSPARENT)
                    .foregroundColor(it.component().isEnabled() ? p.text() : p.textDisabled())
                    .padding(2, 4, 2, 4);
        }

        @SuppressWarnings("deprecation")
        private static ComponentStyleDelegate<JComboBox> comboBox( Theme theme, ComponentStyleDelegate<JComboBox> it ) {
            JComboBox<?> combo   = it.component();
            boolean      enabled = combo.isEnabled();
            return ruled(theme, it, enabled, enabled && LafUtilities.hasFocus(combo), 6, 10, 4);
        }

        @SuppressWarnings("deprecation")
        private static ComponentStyleDelegate<JSpinner> spinner( Theme theme, ComponentStyleDelegate<JSpinner> it ) {
            JSpinner spinner = it.component();
            boolean  enabled = spinner.isEnabled();
            return ruled(theme, it, enabled, enabled && LafUtilities.hasFocus(spinner), 4, 6, 4);
        }

        // ── Inputs ───────────────────────────────────────────────────────────

        @SuppressWarnings("deprecation")
        private static ComponentStyleDelegate<JTextField> field( Theme theme, ComponentStyleDelegate<JTextField> it ) {
            return input(theme, it, it.component(), 7, 10);
        }

        @SuppressWarnings("deprecation")
        private static <C extends JTextComponent> ComponentStyleDelegate<C> page( Theme theme, ComponentStyleDelegate<C> it ) {
            return input(theme, it, it.component(), 8, 10);
        }

        private static <C extends JComponent> ComponentStyleDelegate<C> input(
            Theme theme, ComponentStyleDelegate<C> it, JTextComponent text, int padY, int padX
        ) {
            SwingTreeLookAndFeel.Palette p        = theme.palette();
            boolean editable = text.isEnabled() && text.isEditable();
            // Inside a scroll pane or a spinner the box has already been drawn around it.
            if ( LafUtilities.isInsideAnotherControl(text) )
                return it
                        .margin(0).padding(padY, padX, padY, padX).borderWidth(0)
                        .backgroundColor(SwingTreeLookAndFeel.Palette.TRANSPARENT)
                        .foregroundColor(text.isEnabled() ? p.text() : p.textDisabled());
            return ruled(theme, it, editable, editable && text.isFocusOwner(), padY, padX, padX)
                    .foregroundColor(text.isEnabled() ? p.text() : p.textDisabled());
        }

        /**
         *  The hard rule every input is boxed in. Focus doubles its width and takes the accent, and
         *  the margin gives back exactly what the extra width took, so nothing shifts.
         */
        @SuppressWarnings("deprecation") // component() is the documented hook for LAF state reads
        private static <C extends JComponent> ComponentStyleDelegate<C> ruled(
            Theme theme, ComponentStyleDelegate<C> it, boolean enabled, boolean focused, int padY, int padX, int padRight
        ) {
            SwingTreeLookAndFeel.Palette p = theme.palette();
            Color resting = enabled ? p.surfaceField() : p.surfaceDisabled();
            return it
                    .margin(focused ? 0 : 1)
                    .padding(padY, padRight, padY, padX)
                    .borderRadius(0)
                    .border(focused ? 2 : 1, focused ? p.accent() : p.border())
                    .backgroundColor(LafUtilities.underPointer(p, resting, it.component()))
                    .foregroundColor(enabled ? p.text() : p.textDisabled());
        }

        // ── The rest ─────────────────────────────────────────────────────────

        @SuppressWarnings("deprecation")
        private static ComponentStyleDelegate<JLabel> label( Theme theme, ComponentStyleDelegate<JLabel> it ) {
            SwingTreeLookAndFeel.Palette p = theme.palette();
            return it.foregroundColor(it.component().isEnabled() ? p.text() : p.textDisabled());
        }

        @SuppressWarnings("deprecation")
        private static ComponentStyleDelegate<JMenuItem> menuItem( Theme theme, ComponentStyleDelegate<JMenuItem> it ) {
            SwingTreeLookAndFeel.Palette p       = theme.palette();
            JMenuItem   item    = it.component();
            ButtonModel m       = item.getModel();
            boolean     enabled = item.isEnabled();
            boolean     armed   = enabled && ( m.isArmed() || m.isSelected() );
            return it
                    .padding(6, 12, 6, 12)
                    .borderRadius(0)
                    .borderWidth(0)
                    .backgroundColor(armed ? p.accent() : SwingTreeLookAndFeel.Palette.TRANSPARENT)
                    .foregroundColor(!enabled ? p.textDisabled() : armed ? p.onFilled() : p.text());
        }

        private static ComponentStyleDelegate<JMenuBar> menuBar( Theme theme, ComponentStyleDelegate<JMenuBar> it ) {
            SwingTreeLookAndFeel.Palette p = theme.palette();
            return it
                    .backgroundColor(p.surface())
                    .foregroundColor(p.text())
                    .padding(2, 4, 2, 4)
                    .borderAt(UI.Edge.BOTTOM, 1, p.borderSoft());
        }

        private static ComponentStyleDelegate<JPopupMenu> popupMenu( Theme theme, ComponentStyleDelegate<JPopupMenu> it ) {
            SwingTreeLookAndFeel.Palette p = theme.palette();
            return it
                    .backgroundColor(p.surface())
                    .foregroundColor(p.text())
                    .padding(4, 0, 4, 0)
                    .borderRadius(0)
                    .border(1, p.borderSoft());
        }

        private static ComponentStyleDelegate<JToolTip> toolTip( Theme theme, ComponentStyleDelegate<JToolTip> it ) {
            SwingTreeLookAndFeel.Palette p = theme.palette();
            return it
                    .padding(5, 9, 5, 9)
                    .borderRadius(0)
                    .borderWidth(0)
                    .backgroundColor(p.text())
                    .foregroundColor(p.onFilled());
        }

        /** The delegate draws the hairline itself, so the rule leaves the rest of the strip alone. */
        private static ComponentStyleDelegate<JSeparator> separator( Theme theme, ComponentStyleDelegate<JSeparator> it ) {
            SwingTreeLookAndFeel.Palette p = theme.palette();
            return it.backgroundColor(SwingTreeLookAndFeel.Palette.TRANSPARENT).foregroundColor(p.borderSoft());
        }

        private static ComponentStyleDelegate<JProgressBar> progressBar( Theme theme, ComponentStyleDelegate<JProgressBar> it ) {
            SwingTreeLookAndFeel.Palette p = theme.palette();
            return it
                    .borderRadius(0)
                    .borderWidth(0)
                    .backgroundColor(p.accentSoft())
                    .foregroundColor(p.accent());
        }

        private static ComponentStyleDelegate<JScrollBar> scrollBar( Theme theme, ComponentStyleDelegate<JScrollBar> it ) {
            SwingTreeLookAndFeel.Palette p = theme.palette();
            return it.backgroundColor(p.background()).foregroundColor(p.border());
        }

        private static ComponentStyleDelegate<JTabbedPane> tabbedPane( Theme theme, ComponentStyleDelegate<JTabbedPane> it ) {
            SwingTreeLookAndFeel.Palette p = theme.palette();
            return it.backgroundColor(p.background()).foregroundColor(p.text());
        }

        private static ComponentStyleDelegate<JTableHeader> tableHeader( Theme theme, ComponentStyleDelegate<JTableHeader> it ) {
            SwingTreeLookAndFeel.Palette p = theme.palette();
            return it
                    .backgroundColor(p.surface())
                    .foregroundColor(p.textMuted())
                    .borderAt(UI.Edge.BOTTOM, 1, p.border());
        }

        private static ComponentStyleDelegate<JToolBar> toolBar( Theme theme, ComponentStyleDelegate<JToolBar> it ) {
            SwingTreeLookAndFeel.Palette p = theme.palette();
            return it
                    .backgroundColor(p.surface())
                    .foregroundColor(p.text())
                    .padding(4, 8, 4, 8)
                    .borderRadius(0)
                    .borderAt(UI.Edge.BOTTOM, 1, p.borderSoft());
        }

        /** A list, table or tree is the content of the box around it, so it fills nothing itself. */
        private static <C extends JComponent> ComponentStyleDelegate<C> content( Theme theme, ComponentStyleDelegate<C> it ) {
            return it.backgroundColor(SwingTreeLookAndFeel.Palette.TRANSPARENT).foregroundColor(theme.palette().text());
        }

        /** Structure with nothing of its own to paint: the symbol set draws all of it. */
        private static <C extends JComponent> ComponentStyleDelegate<C> bare( Theme theme, ComponentStyleDelegate<C> it ) {
            return it.backgroundColor(SwingTreeLookAndFeel.Palette.TRANSPARENT).foregroundColor(theme.palette().text());
        }

        // ── Variant colours ──────────────────────────────────────────────────

        private static Color fill(SwingTreeLookAndFeel.Variant variant, SwingTreeLookAndFeel.Palette p, boolean enabled, boolean sunken, boolean rollover ) {
            if ( !enabled )
                return p.surfaceDisabled();
            switch ( variant ) {
                case PRIMARY: return sunken ? p.primaryPressed() : rollover ? p.primaryHover() : p.primary();
                case DANGER:  return sunken ? p.dangerPressed()  : rollover ? p.dangerHover()  : p.danger();
                case QUIET:   return sunken ? p.accent() : rollover ? p.accentSoft() : SwingTreeLookAndFeel.Palette.TRANSPARENT;
                case NEUTRAL:
                default:      return sunken ? p.accent() : rollover ? p.accentSoft() : p.surfaceHover();
            }
        }

        private static Color ink(SwingTreeLookAndFeel.Variant variant, SwingTreeLookAndFeel.Palette p, boolean enabled, boolean sunken ) {
            if ( !enabled )
                return p.textDisabled();
            // The last rung of the ladder is a saturated fill, so the label has to invert with it.
            return variant.isFilled() || sunken ? p.onFilled() : p.text();
        }
    }

    /**
     *  <b>Skeuomorphism</b>: the window is a leather bench, cards are sheets of paper lying on it,
     *  and anything you can press is a milled metal plate screwed down onto the paper. A plate needs
     *  all three of a grain, so the surface has a material; a vertical gradient, because a flat thing
     *  under a ceiling lamp is brightest at the top; and a bevel of one light pixel along the top
     *  edge and one dark pixel along the bottom, which is its own thickness seen edge on.
     *  <p>
     *  Anything you type into is the opposite: a hole milled into the surface, dark along the top
     *  wall the light cannot reach and bright along the bottom it can. Pressing a plate turns it into
     *  that same hole.
     *
     *  @see SwingTreeLookAndFeel.StylePreset#SKEUOMORPHIC
     */
    static final class Skeuomorphic
    {
        private Skeuomorphic() {}

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
            StyleRule.of(JPanel.class,         Skeuomorphic::panel),
            StyleRule.of(AbstractButton.class, Skeuomorphic::button),
            StyleRule.of(JCheckBox.class,      Skeuomorphic::tickable),
            StyleRule.of(JRadioButton.class,   Skeuomorphic::tickable),
            StyleRule.of(JMenuItem.class,      Skeuomorphic::menuItem),
            StyleRule.of(JMenuBar.class,       Skeuomorphic::menuBar),
            StyleRule.of(JPopupMenu.class,     Skeuomorphic::popupMenu),
            StyleRule.of(JLabel.class,         Skeuomorphic::label),
            StyleRule.of(JTextField.class,     Skeuomorphic::field),
            StyleRule.of(JTextArea.class,      Skeuomorphic::page),
            StyleRule.of(JEditorPane.class,    Skeuomorphic::page),
            StyleRule.of(JSeparator.class,     Skeuomorphic::separator),
            StyleRule.of(JToolTip.class,       Skeuomorphic::toolTip),
            StyleRule.of(JProgressBar.class,   Skeuomorphic::progressBar),
            StyleRule.of(JSlider.class,        Skeuomorphic::bare),
            StyleRule.of(JScrollBar.class,     Skeuomorphic::scrollBar),
            StyleRule.of(JScrollPane.class,    Skeuomorphic::scrollPane),
            StyleRule.of(JViewport.class,      Skeuomorphic::viewport),
            StyleRule.of(JComboBox.class,      Skeuomorphic::comboBox),
            StyleRule.of(JSpinner.class,       Skeuomorphic::spinner),
            StyleRule.of(JTabbedPane.class,    Skeuomorphic::tabbedPane),
            StyleRule.of(JList.class,          Skeuomorphic::content),
            StyleRule.of(JTable.class,         Skeuomorphic::content),
            StyleRule.of(JTableHeader.class,   Skeuomorphic::tableHeader),
            StyleRule.of(JTree.class,          Skeuomorphic::content),
            StyleRule.of(JToolBar.class,       Skeuomorphic::toolBar),
            StyleRule.of(JSplitPane.class,     Skeuomorphic::bare)
        );

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
                                             .falloff(UI.ShadowFalloff.PENUMBRA).isInset(true));
            return it.shadow(DROP, s -> s.color(LafUtilities.withOpacity(Color.BLACK, 60))
                                         .offset(0, 2).blurRadius(3)
                                         .falloff(UI.ShadowFalloff.BLUR).isInset(false));
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
                                         .falloff(UI.ShadowFalloff.PENUMBRA).isInset(true))
                    .shadow(FLOOR, s -> s.color(LafUtilities.withOpacity(Color.WHITE, 120))
                                         .offset(0, -depth).blurRadius(depth * 2)
                                         .falloff(UI.ShadowFalloff.GLOW).isInset(true));
        }

        // ── Surfaces ─────────────────────────────────────────────────────────

        @SuppressWarnings("deprecation") // component() is the documented hook for LAF state reads
        private static ComponentStyleDelegate<JPanel> panel( Theme theme, ComponentStyleDelegate<JPanel> it ) {
            SwingTreeLookAndFeel.Palette p = theme.palette();
            it = it.foregroundColor(p.text());
            switch ( SwingTreeLookAndFeel.Surface.of(it.component()) ) {
                case CARD:        return sheet(it, p).margin(5).padding(2);
                case RAIL:        return plate(it.borderRadius(0).border(1, p.border()), p.surface(), false);
                case TRANSPARENT: return it.backgroundColor(SwingTreeLookAndFeel.Palette.TRANSPARENT);
                case WINDOW:
                default:          return LafUtilities.isControlInternal(it.component())
                                            ? it.backgroundColor(SwingTreeLookAndFeel.Palette.TRANSPARENT)
                                            : leather(it, p);
            }
        }

        @SuppressWarnings("deprecation")
        private static ComponentStyleDelegate<JScrollPane> scrollPane( Theme theme, ComponentStyleDelegate<JScrollPane> it ) {
            SwingTreeLookAndFeel.Palette p = theme.palette();
            it = it.foregroundColor(p.text());
            switch ( SwingTreeLookAndFeel.Surface.of(it.component()) ) {
                case TRANSPARENT: return it.backgroundColor(SwingTreeLookAndFeel.Palette.TRANSPARENT).borderWidth(0).borderRadius(0).padding(0);
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
        private static ComponentStyleDelegate<JViewport> viewport( Theme theme, ComponentStyleDelegate<JViewport> it ) {
            return it.backgroundColor(SwingTreeLookAndFeel.Palette.TRANSPARENT).foregroundColor(theme.palette().text());
        }

        /** A sheet of paper: its own grain, a hairline edge and a shadow where it lifts off the bench. */
        private static <C extends JComponent> ComponentStyleDelegate<C> sheet( ComponentStyleDelegate<C> it, SwingTreeLookAndFeel.Palette p ) {
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
                                        .falloff(UI.ShadowFalloff.BLUR).isInset(false));
        }

        /** The bench everything else lies on. */
        private static <C extends JComponent> ComponentStyleDelegate<C> leather( ComponentStyleDelegate<C> it, SwingTreeLookAndFeel.Palette p ) {
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
        private static ComponentStyleDelegate<AbstractButton> button( Theme theme, ComponentStyleDelegate<AbstractButton> it ) {
            SwingTreeLookAndFeel.Palette p = theme.palette();
            AbstractButton b = it.component();
            ButtonModel    m = b.getModel();

            boolean enabled  = b.isEnabled();
            boolean pressed  = enabled && m.isArmed() && m.isPressed();
            boolean sunken   = pressed || ( enabled && m.isSelected() );
            boolean rollover = enabled && m.isRollover() && !pressed;
            boolean focused  = enabled && b.isFocusOwner();

            SwingTreeLookAndFeel.Variant variant = SwingTreeLookAndFeel.Variant.of(b);
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
                                              .falloff(UI.ShadowFalloff.GLOW).isInset(false));
            if ( !enabled )
                return it.borderColor(p.border());
            if ( variant == SwingTreeLookAndFeel.Variant.QUIET && !sunken && !rollover )
                return it.borderColor(SwingTreeLookAndFeel.Palette.TRANSPARENT).backgroundColor(SwingTreeLookAndFeel.Palette.TRANSPARENT);
            return plate(it, base, sunken);
        }

        @SuppressWarnings("deprecation")
        private static <C extends AbstractButton> ComponentStyleDelegate<C> tickable( Theme theme, ComponentStyleDelegate<C> it ) {
            SwingTreeLookAndFeel.Palette p = theme.palette();
            return it
                    .backgroundColor(SwingTreeLookAndFeel.Palette.TRANSPARENT)
                    .foregroundColor(it.component().isEnabled() ? p.text() : p.textDisabled())
                    .padding(2, 4, 2, 4);
        }

        @SuppressWarnings("deprecation")
        private static ComponentStyleDelegate<JComboBox> comboBox( Theme theme, ComponentStyleDelegate<JComboBox> it ) {
            SwingTreeLookAndFeel.Palette p     = theme.palette();
            JComboBox<?> combo = it.component();
            return machined(it, p, combo.isEnabled(), LafUtilities.hasFocus(combo), 5, 10, 4);
        }

        @SuppressWarnings("deprecation")
        private static ComponentStyleDelegate<JSpinner> spinner( Theme theme, ComponentStyleDelegate<JSpinner> it ) {
            SwingTreeLookAndFeel.Palette p       = theme.palette();
            JSpinner spinner = it.component();
            return machined(it, p, spinner.isEnabled(), LafUtilities.hasFocus(spinner), 3, 6, 3);
        }

        /**
         *  A combo box or a spinner is milled like a hole rather than like a plate, and the button
         *  screwed into its right-hand end is what you press. It has to be a hole, because Swing
         *  fills the strip showing the current value from the {@code ComboBox.background} default
         *  rather than from the component, and anything else would carry a rectangle of paper colour
         *  across its middle.
         */
        @SuppressWarnings("deprecation") // component() is the documented hook for LAF state reads
        private static <C extends JComponent> ComponentStyleDelegate<C> machined(
                ComponentStyleDelegate<C> it, SwingTreeLookAndFeel.Palette p, boolean enabled, boolean focused,
                int padY, int padX, int padRight
        ) {
            Color resting = enabled ? p.surfaceField() : p.surfaceDisabled();
            it = it
                    .margin(3)
                    .padding(padY, padRight, padY, padX)
                    .borderRadius(RADIUS)
                    .border(1, focused ? p.accent() : p.border())
                    .backgroundColor(LafUtilities.underPointer(p, resting, it.component()))
                    .foregroundColor(enabled ? p.text() : p.textDisabled());
            return enabled ? well(it, 3) : it;
        }

        // ── Inputs ───────────────────────────────────────────────────────────

        @SuppressWarnings("deprecation")
        private static ComponentStyleDelegate<JTextField> field( Theme theme, ComponentStyleDelegate<JTextField> it ) {
            return input(theme, it, it.component(), 6, 10);
        }

        @SuppressWarnings("deprecation")
        private static <C extends JTextComponent> ComponentStyleDelegate<C> page( Theme theme, ComponentStyleDelegate<C> it ) {
            return input(theme, it, it.component(), 8, 10);
        }

        private static <C extends JComponent> ComponentStyleDelegate<C> input(
            Theme theme, ComponentStyleDelegate<C> it, JTextComponent text, int padY, int padX
        ) {
            SwingTreeLookAndFeel.Palette p        = theme.palette();
            boolean editable = text.isEnabled() && text.isEditable();
            boolean focused  = editable && text.isFocusOwner();
            // Inside a scroll pane or a picker the hole has already been milled around it.
            if ( LafUtilities.isInsideAnotherControl(text) )
                return it
                        .margin(0).padding(padY, padX, padY, padX).borderWidth(0)
                        .backgroundColor(SwingTreeLookAndFeel.Palette.TRANSPARENT)
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
        private static ComponentStyleDelegate<JLabel> label( Theme theme, ComponentStyleDelegate<JLabel> it ) {
            SwingTreeLookAndFeel.Palette p = theme.palette();
            return it.foregroundColor(it.component().isEnabled() ? p.text() : p.textDisabled());
        }

        @SuppressWarnings("deprecation")
        private static ComponentStyleDelegate<JMenuItem> menuItem( Theme theme, ComponentStyleDelegate<JMenuItem> it ) {
            SwingTreeLookAndFeel.Palette p       = theme.palette();
            JMenuItem   item    = it.component();
            ButtonModel m       = item.getModel();
            boolean     enabled = item.isEnabled();
            boolean     armed   = enabled && ( m.isArmed() || m.isSelected() );
            it = it
                    .padding(5, 12, 5, 12)
                    .borderRadius(3)
                    .borderWidth(0)
                    .backgroundColor(armed ? p.accent() : SwingTreeLookAndFeel.Palette.TRANSPARENT)
                    .foregroundColor(!enabled ? p.textDisabled() : armed ? p.onFilled() : p.text());
            return armed ? plate(it, p.accent(), false) : it;
        }

        private static ComponentStyleDelegate<JMenuBar> menuBar( Theme theme, ComponentStyleDelegate<JMenuBar> it ) {
            SwingTreeLookAndFeel.Palette p = theme.palette();
            return plate(it
                    .foregroundColor(p.text())
                    .padding(2, 4, 2, 4)
                    .borderRadius(0)
                    .borderWidth(1), p.surface(), false);
        }

        private static ComponentStyleDelegate<JPopupMenu> popupMenu( Theme theme, ComponentStyleDelegate<JPopupMenu> it ) {
            SwingTreeLookAndFeel.Palette p = theme.palette();
            return sheet(it
                    .foregroundColor(p.text())
                    .margin(5)
                    .padding(4, 0, 4, 0), p);
        }

        private static ComponentStyleDelegate<JToolTip> toolTip( Theme theme, ComponentStyleDelegate<JToolTip> it ) {
            SwingTreeLookAndFeel.Palette p = theme.palette();
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
        private static ComponentStyleDelegate<JSeparator> separator( Theme theme, ComponentStyleDelegate<JSeparator> it ) {
            SwingTreeLookAndFeel.Palette p = theme.palette();
            return it
                    .backgroundColor(SwingTreeLookAndFeel.Palette.TRANSPARENT)
                    .foregroundColor(p.border())
                    .shadow(FLOOR, s -> s.color(LafUtilities.withOpacity(Color.WHITE, 130))
                                         .offset(0, 1).blurRadius(0).isInset(false));
        }

        private static ComponentStyleDelegate<JProgressBar> progressBar( Theme theme, ComponentStyleDelegate<JProgressBar> it ) {
            SwingTreeLookAndFeel.Palette p = theme.palette();
            return well(it
                    .margin(2)
                    .borderRadius(6)
                    .border(1, p.border())
                    .backgroundColor(p.surfaceDisabled())
                    .foregroundColor(p.accent()), 2);
        }

        private static ComponentStyleDelegate<JScrollBar> scrollBar( Theme theme, ComponentStyleDelegate<JScrollBar> it ) {
            SwingTreeLookAndFeel.Palette p = theme.palette();
            return well(it
                    .backgroundColor(p.surfaceDisabled())
                    .foregroundColor(p.border()), 2);
        }

        private static ComponentStyleDelegate<JTabbedPane> tabbedPane( Theme theme, ComponentStyleDelegate<JTabbedPane> it ) {
            SwingTreeLookAndFeel.Palette p = theme.palette();
            return it.backgroundColor(SwingTreeLookAndFeel.Palette.TRANSPARENT).foregroundColor(p.text());
        }

        private static ComponentStyleDelegate<JTableHeader> tableHeader( Theme theme, ComponentStyleDelegate<JTableHeader> it ) {
            SwingTreeLookAndFeel.Palette p = theme.palette();
            return plate(it
                    .foregroundColor(p.textMuted())
                    .borderRadius(0)
                    .borderWidth(1), p.surface(), false);
        }

        private static ComponentStyleDelegate<JToolBar> toolBar( Theme theme, ComponentStyleDelegate<JToolBar> it ) {
            SwingTreeLookAndFeel.Palette p = theme.palette();
            return plate(it
                    .foregroundColor(p.text())
                    .margin(4)
                    .padding(4, 8, 4, 8)
                    .borderRadius(RADIUS)
                    .borderWidth(1), p.surface(), false);
        }

        /** A list, table or tree lies on the floor of the well the scroll pane milled for it. */
        private static <C extends JComponent> ComponentStyleDelegate<C> content( Theme theme, ComponentStyleDelegate<C> it ) {
            return it.backgroundColor(SwingTreeLookAndFeel.Palette.TRANSPARENT).foregroundColor(theme.palette().text());
        }

        /** Structure with nothing of its own to paint: the symbol set draws all of it. */
        private static <C extends JComponent> ComponentStyleDelegate<C> bare( Theme theme, ComponentStyleDelegate<C> it ) {
            return it.backgroundColor(SwingTreeLookAndFeel.Palette.TRANSPARENT).foregroundColor(theme.palette().text());
        }

        // ── Variant colours ──────────────────────────────────────────────────

        private static Color fill(SwingTreeLookAndFeel.Variant variant, SwingTreeLookAndFeel.Palette p, boolean enabled, boolean sunken, boolean rollover ) {
            if ( !enabled )
                return p.surfaceDisabled();
            switch ( variant ) {
                case PRIMARY: return sunken ? p.primaryPressed() : rollover ? p.primaryHover() : p.primary();
                case DANGER:  return sunken ? p.dangerPressed()  : rollover ? p.dangerHover()  : p.danger();
                case QUIET:   return sunken || rollover ? p.surface() : SwingTreeLookAndFeel.Palette.TRANSPARENT;
                case NEUTRAL:
                default:      return sunken ? p.surfacePressed() : rollover ? p.surfaceHover() : p.surface();
            }
        }

        private static Color ink(SwingTreeLookAndFeel.Variant variant, SwingTreeLookAndFeel.Palette p, boolean enabled ) {
            if ( !enabled )
                return p.textDisabled();
            return variant.isFilled() ? p.onFilled() : p.text();
        }
    }

    /**
     *  <b>Glassmorphism</b>: frosted panes floating over something vivid. Nothing is opaque. A
     *  surface is a wash of white at about a tenth of full strength, and what makes it read as glass
     *  rather than as a pale rectangle is that the window behind it is really blurred where it shows
     *  through: {@link ComponentStyleDelegate#parentFilter} convolves the parent's rendering rather
     *  than imitating it. A hairline of brighter white along the edge is the bevel catching the
     *  light, and a wide soft shadow underneath says the pane is floating.
     *  <p>
     *  Glass needs something worth blurring, so the window is a gradient and not a colour. Under a
     *  flat palette the panes still behave, they simply have nothing to show.
     *
     *  @see SwingTreeLookAndFeel.StylePreset#GLASSMORPHIC
     */
    static final class Glassmorphic
    {
        private Glassmorphic() {}

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
            StyleRule.of(JPanel.class,         Glassmorphic::panel),
            StyleRule.of(AbstractButton.class, Glassmorphic::button),
            StyleRule.of(JCheckBox.class,      Glassmorphic::tickable),
            StyleRule.of(JRadioButton.class,   Glassmorphic::tickable),
            StyleRule.of(JMenuItem.class,      Glassmorphic::menuItem),
            StyleRule.of(JMenuBar.class,       Glassmorphic::menuBar),
            StyleRule.of(JPopupMenu.class,     Glassmorphic::popupMenu),
            StyleRule.of(JLabel.class,         Glassmorphic::label),
            StyleRule.of(JTextField.class,     Glassmorphic::field),
            StyleRule.of(JTextArea.class,      Glassmorphic::page),
            StyleRule.of(JEditorPane.class,    Glassmorphic::page),
            StyleRule.of(JSeparator.class,     Glassmorphic::separator),
            StyleRule.of(JToolTip.class,       Glassmorphic::toolTip),
            StyleRule.of(JProgressBar.class,   Glassmorphic::progressBar),
            StyleRule.of(JSlider.class,        Glassmorphic::bare),
            StyleRule.of(JScrollBar.class,     Glassmorphic::scrollBar),
            StyleRule.of(JScrollPane.class,    Glassmorphic::scrollPane),
            StyleRule.of(JViewport.class,      Glassmorphic::bare),
            StyleRule.of(JComboBox.class,      Glassmorphic::comboBox),
            StyleRule.of(JSpinner.class,       Glassmorphic::spinner),
            StyleRule.of(JTabbedPane.class,    Glassmorphic::bare),
            StyleRule.of(JList.class,          Glassmorphic::bare),
            StyleRule.of(JTable.class,         Glassmorphic::bare),
            StyleRule.of(JTableHeader.class,   Glassmorphic::tableHeader),
            StyleRule.of(JTree.class,          Glassmorphic::bare),
            StyleRule.of(JToolBar.class,       Glassmorphic::toolBar),
            StyleRule.of(JSplitPane.class,     Glassmorphic::bare)
        );

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
            Theme theme, ComponentStyleDelegate<C> it, int wash, int lift
        ) {
            SwingTreeLookAndFeel.Palette p = theme.palette();
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
                                        .falloff(UI.ShadowFalloff.BLUR).isInset(false));
        }

        // ── Surfaces ─────────────────────────────────────────────────────────

        @SuppressWarnings("deprecation") // component() is the documented hook for LAF state reads
        private static ComponentStyleDelegate<JPanel> panel( Theme theme, ComponentStyleDelegate<JPanel> it ) {
            SwingTreeLookAndFeel.Palette p = theme.palette();
            it = it.foregroundColor(p.text());
            switch ( SwingTreeLookAndFeel.Surface.of(it.component()) ) {
                case CARD:        return pane(theme, it.borderRadius(RADIUS).borderWidth(1).margin(7).padding(2), PANE, 6);
                case RAIL:        return pane(theme, it.borderRadius(0).borderWidth(0), PANE / 2, 3);
                case TRANSPARENT: return it.backgroundColor(SwingTreeLookAndFeel.Palette.TRANSPARENT);
                case WINDOW:
                default:          return LafUtilities.isControlInternal(it.component()) || _standsOnTheGround(it.component())
                                            ? it.backgroundColor(SwingTreeLookAndFeel.Palette.TRANSPARENT)
                                            : aurora(it, p);
            }
        }

        /**
         *  The vivid ground the whole idiom needs there to be. Only the outermost panel paints it,
         *  because a gradient is laid out across the bounds of whatever draws it and an untagged
         *  panel inside another untagged panel would start the sweep again inside its own corner.
         */
        private static <C extends JComponent> ComponentStyleDelegate<C> aurora( ComponentStyleDelegate<C> it, SwingTreeLookAndFeel.Palette p ) {
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
                if ( parent instanceof JPanel && SwingTreeLookAndFeel.Surface.of((JPanel) parent) == SwingTreeLookAndFeel.Surface.WINDOW )
                    return true;
                parent = parent.getParent();
            }
            return false;
        }

        @SuppressWarnings("deprecation")
        private static ComponentStyleDelegate<JScrollPane> scrollPane( Theme theme, ComponentStyleDelegate<JScrollPane> it ) {
            SwingTreeLookAndFeel.Palette p = theme.palette();
            it = it.foregroundColor(p.text());
            switch ( SwingTreeLookAndFeel.Surface.of(it.component()) ) {
                case TRANSPARENT: return it.backgroundColor(SwingTreeLookAndFeel.Palette.TRANSPARENT).borderWidth(0).borderRadius(0).padding(0);
                case CARD:        return pane(theme, it.borderRadius(RADIUS).borderWidth(1).margin(7).padding(3), PANE, 6);
                case RAIL:        return it.backgroundColor(SwingTreeLookAndFeel.Palette.TRANSPARENT).borderWidth(0).borderRadius(0).padding(0);
                case WINDOW:
                default:          return pane(theme, it.borderRadius(RADIUS - 4).borderWidth(1).margin(4).padding(3), WELL, 3);
            }
        }

        // ── Controls ─────────────────────────────────────────────────────────

        @SuppressWarnings("deprecation")
        private static ComponentStyleDelegate<AbstractButton> button( Theme theme, ComponentStyleDelegate<AbstractButton> it ) {
            SwingTreeLookAndFeel.Palette p = theme.palette();
            AbstractButton b = it.component();
            ButtonModel    m = b.getModel();

            boolean enabled  = b.isEnabled();
            boolean pressed  = enabled && m.isArmed() && m.isPressed();
            boolean sunken   = pressed || ( enabled && m.isSelected() );
            boolean rollover = enabled && m.isRollover() && !pressed;
            boolean focused  = enabled && b.isFocusOwner();

            SwingTreeLookAndFeel.Variant variant = SwingTreeLookAndFeel.Variant.of(b);
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
                return pane(theme, it, PANE, sunken ? 2 : 5)
                        .backgroundColor(LafUtilities.withOpacity(tint(variant, p, sunken, rollover), 150));
            if ( variant == SwingTreeLookAndFeel.Variant.QUIET && !sunken && !rollover )
                return it.backgroundColor(SwingTreeLookAndFeel.Palette.TRANSPARENT).borderColor(SwingTreeLookAndFeel.Palette.TRANSPARENT);
            return pane(theme, it, sunken ? WELL : rollover ? PANE + 22 : PANE, sunken ? 2 : 5);
        }

        @SuppressWarnings("deprecation")
        private static <C extends AbstractButton> ComponentStyleDelegate<C> tickable( Theme theme, ComponentStyleDelegate<C> it ) {
            SwingTreeLookAndFeel.Palette p = theme.palette();
            return it
                    .backgroundColor(SwingTreeLookAndFeel.Palette.TRANSPARENT)
                    .foregroundColor(it.component().isEnabled() ? p.text() : p.textDisabled())
                    .padding(2, 4, 2, 4);
        }

        @SuppressWarnings("deprecation")
        private static ComponentStyleDelegate<JComboBox> comboBox( Theme theme, ComponentStyleDelegate<JComboBox> it ) {
            JComboBox<?> combo = it.component();
            return frosted(theme, it, combo.isEnabled(), LafUtilities.hasFocus(combo), 6, 10, 4);
        }

        @SuppressWarnings("deprecation")
        private static ComponentStyleDelegate<JSpinner> spinner( Theme theme, ComponentStyleDelegate<JSpinner> it ) {
            JSpinner spinner = it.component();
            return frosted(theme, it, spinner.isEnabled(), LafUtilities.hasFocus(spinner), 4, 6, 4);
        }

        // ── Inputs ───────────────────────────────────────────────────────────

        @SuppressWarnings("deprecation")
        private static ComponentStyleDelegate<JTextField> field( Theme theme, ComponentStyleDelegate<JTextField> it ) {
            return input(theme, it, it.component(), 7, 12);
        }

        @SuppressWarnings("deprecation")
        private static <C extends JTextComponent> ComponentStyleDelegate<C> page( Theme theme, ComponentStyleDelegate<C> it ) {
            return input(theme, it, it.component(), 8, 12);
        }

        private static <C extends JComponent> ComponentStyleDelegate<C> input(
            Theme theme, ComponentStyleDelegate<C> it, JTextComponent text, int padY, int padX
        ) {
            SwingTreeLookAndFeel.Palette p        = theme.palette();
            boolean editable = text.isEnabled() && text.isEditable();
            // Inside a scroll pane or a picker the pane has already been cut around it.
            if ( LafUtilities.isInsideAnotherControl(text) )
                return it
                        .margin(0).padding(padY, padX, padY, padX).borderWidth(0)
                        .backgroundColor(SwingTreeLookAndFeel.Palette.TRANSPARENT)
                        .foregroundColor(text.isEnabled() ? p.text() : p.textDisabled());
            return frosted(theme, it, editable, editable && text.isFocusOwner(), padY, padX, padX)
                    .foregroundColor(text.isEnabled() ? p.text() : p.textDisabled());
        }

        /** A pane you reach into: darker than the ones you only look at, so text stands off it. */
        @SuppressWarnings("deprecation") // component() is the documented hook for LAF state reads
        private static <C extends JComponent> ComponentStyleDelegate<C> frosted(
            Theme theme, ComponentStyleDelegate<C> it, boolean enabled, boolean focused, int padY, int padX, int padRight
        ) {
            SwingTreeLookAndFeel.Palette p = theme.palette();
            it = it
                    // The thicker focused edge is taken out of the margin, so a field keeps its
                    // footprint when you click into it.
                    .margin(focused ? 2 : 3)
                    .padding(padY, padRight, padY, padX)
                    .borderRadius(RADIUS - 5)
                    .borderWidth(focused ? 2 : 1)
                    .foregroundColor(enabled ? p.text() : p.textDisabled());
            if ( !enabled )
                return it.backgroundColor(LafUtilities.withOpacity(p.surfaceDisabled(), 30))
                         .borderColor(LafUtilities.withOpacity(p.border(), 40));
            // This idiom answers the pointer the way its buttons do, by letting more of the surface
            // through the glass rather than by moving the colour behind it.
            int veil = WELL + 40 + ( LafUtilities.isUnderPointer(it.component()) ? 22 : 0 );
            return pane(theme, it, WELL, 2)
                    .backgroundColor(LafUtilities.withOpacity(p.surfaceField(), veil))
                    .borderColor(LafUtilities.withOpacity(focused ? p.accent() : p.border(), focused ? 220 : RIM));
        }

        // ── The rest ─────────────────────────────────────────────────────────

        @SuppressWarnings("deprecation")
        private static ComponentStyleDelegate<JLabel> label( Theme theme, ComponentStyleDelegate<JLabel> it ) {
            SwingTreeLookAndFeel.Palette p = theme.palette();
            return it.foregroundColor(it.component().isEnabled() ? p.text() : p.textDisabled());
        }

        @SuppressWarnings("deprecation")
        private static ComponentStyleDelegate<JMenuItem> menuItem( Theme theme, ComponentStyleDelegate<JMenuItem> it ) {
            SwingTreeLookAndFeel.Palette p       = theme.palette();
            JMenuItem   item    = it.component();
            ButtonModel m       = item.getModel();
            boolean     enabled = item.isEnabled();
            boolean     armed   = enabled && ( m.isArmed() || m.isSelected() );
            return it
                    .padding(6, 12, 6, 12)
                    .borderRadius(RADIUS - 6)
                    .borderWidth(0)
                    .backgroundColor(armed ? LafUtilities.withOpacity(p.surface(), 62) : SwingTreeLookAndFeel.Palette.TRANSPARENT)
                    .foregroundColor(enabled ? p.text() : p.textDisabled());
        }

        private static ComponentStyleDelegate<JMenuBar> menuBar( Theme theme, ComponentStyleDelegate<JMenuBar> it ) {
            SwingTreeLookAndFeel.Palette p = theme.palette();
            return pane(theme, it
                    .foregroundColor(p.text())
                    .padding(2, 4, 2, 4)
                    .borderRadius(0)
                    .borderWidth(0), PANE / 2, 3);
        }

        @SuppressWarnings("deprecation") // component() is the documented hook for LAF state reads
        private static ComponentStyleDelegate<JPopupMenu> popupMenu( Theme theme, ComponentStyleDelegate<JPopupMenu> it ) {
            SwingTreeLookAndFeel.Palette p = theme.palette();
            return groundIfUnfrosted(theme, pane(theme, it
                    .foregroundColor(p.text())
                    .margin(7)
                    .padding(5, 0, 5, 0)
                    .borderRadius(RADIUS)
                    .borderWidth(1), PANE + 30, 7), it.component(), PANE + 30);
        }

        @SuppressWarnings("deprecation") // component() is the documented hook for LAF state reads
        private static ComponentStyleDelegate<JToolTip> toolTip( Theme theme, ComponentStyleDelegate<JToolTip> it ) {
            SwingTreeLookAndFeel.Palette p = theme.palette();
            return groundIfUnfrosted(theme, pane(theme, it
                    .foregroundColor(p.text())
                    .margin(5)
                    .padding(5, 10, 5, 10)
                    .borderRadius(RADIUS - 6)
                    .borderWidth(1), PANE + 40, 5), it.component(), PANE + 40);
        }

        /**
         *  Repaints a popup's pane in a colour that does not need the frost, for a popup in a
         *  per-pixel translucent window of its own.
         *  <p>
         *  Every other pane here is frosted, and that blur of what lies behind is what separates the
         *  pane's text from it. A popup Swing had to put in a window of its own has nothing behind
         *  it: {@code parentFilter} reads the parent's rendering, and the parent is that window's
         *  empty content pane, so the frost is missing exactly where the pane is most transparent and
         *  the menu text would stand on the bare desktop. The two opaque
         *  {@link SwingTreeLookAndFeel.PopupWindowMode}s need no repaint, because
         *  {@link SwingTreePopupFactory} fills their window with the palette ground.
         *  <p>
         *  Raising the wash alone would make it worse. The wash tints towards
         *  {@link SwingTreeLookAndFeel.Palette#surface()}, so on a palette whose text is lighter than
         *  its surface a thicker wash moves the pane towards the colour of its own letters. The pane
         *  is therefore mixed down onto {@link SwingTreeLookAndFeel.Palette#background()} first,
         *  which is the colour it would have been composited against in-frame, and only then made
         *  nearly opaque. The window's alpha still buys the margin ring: the corners stay antialiased
         *  and the drop shadow still falls on the desktop.
         *
         * @param it    the pane as {@link #pane} left it
         * @param popup the popup menu or tool tip being styled
         * @param wash  the wash {@link #pane} was given, out of 255
         * @param <C> the component type
         * @return the delegate, repainted only for a popup which cannot be frosted
         */
        private static <C extends JComponent> ComponentStyleDelegate<C> groundIfUnfrosted(
            Theme theme, ComponentStyleDelegate<C> it, C popup, int wash
        ) {
            if ( SwingTreeLookAndFeel.popupWindowModeOf(popup) != SwingTreeLookAndFeel.PopupWindowMode.TRANSLUCENT )
                return it;
            SwingTreeLookAndFeel.Palette p = theme.palette();
            Color grounded = LafUtilities.shadeTowards(p.background(), p.surface(), wash / 255.0);
            return it.backgroundColor(LafUtilities.withOpacity(grounded, UNFROSTED_PANE));
        }

        /** The delegate draws the hairline itself, so the rule leaves the rest of the strip alone. */
        private static ComponentStyleDelegate<JSeparator> separator( Theme theme, ComponentStyleDelegate<JSeparator> it ) {
            SwingTreeLookAndFeel.Palette p = theme.palette();
            return it.backgroundColor(SwingTreeLookAndFeel.Palette.TRANSPARENT)
                     .foregroundColor(LafUtilities.withOpacity(p.border(), 60));
        }

        private static ComponentStyleDelegate<JProgressBar> progressBar( Theme theme, ComponentStyleDelegate<JProgressBar> it ) {
            SwingTreeLookAndFeel.Palette p = theme.palette();
            return it
                    .margin(2)
                    .borderRadius(6)
                    .border(1, LafUtilities.withOpacity(p.border(), 50))
                    .backgroundColor(LafUtilities.withOpacity(p.surfaceField(), WELL))
                    .foregroundColor(p.accent());
        }

        private static ComponentStyleDelegate<JScrollBar> scrollBar( Theme theme, ComponentStyleDelegate<JScrollBar> it ) {
            SwingTreeLookAndFeel.Palette p = theme.palette();
            return it.backgroundColor(SwingTreeLookAndFeel.Palette.TRANSPARENT).foregroundColor(p.border());
        }

        private static ComponentStyleDelegate<JTableHeader> tableHeader( Theme theme, ComponentStyleDelegate<JTableHeader> it ) {
            SwingTreeLookAndFeel.Palette p = theme.palette();
            return it
                    .backgroundColor(LafUtilities.withOpacity(p.surface(), 26))
                    .foregroundColor(p.textMuted())
                    .borderAt(UI.Edge.BOTTOM, 1, LafUtilities.withOpacity(p.border(), 60));
        }

        private static ComponentStyleDelegate<JToolBar> toolBar( Theme theme, ComponentStyleDelegate<JToolBar> it ) {
            SwingTreeLookAndFeel.Palette p = theme.palette();
            return pane(theme, it
                    .foregroundColor(p.text())
                    .margin(6)
                    .padding(4, 8, 4, 8)
                    .borderRadius(RADIUS)
                    .borderWidth(1), PANE, 5);
        }

        /** Everything that is only the contents of a pane somebody else already cut. */
        private static <C extends JComponent> ComponentStyleDelegate<C> bare( Theme theme, ComponentStyleDelegate<C> it ) {
            return it.backgroundColor(SwingTreeLookAndFeel.Palette.TRANSPARENT).foregroundColor(theme.palette().text());
        }

        // ── Variant colours ──────────────────────────────────────────────────

        private static Color tint(SwingTreeLookAndFeel.Variant variant, SwingTreeLookAndFeel.Palette p, boolean sunken, boolean rollover ) {
            switch ( variant ) {
                case DANGER: return sunken ? p.dangerPressed()  : rollover ? p.dangerHover()  : p.danger();
                case PRIMARY:
                default:     return sunken ? p.primaryPressed() : rollover ? p.primaryHover() : p.primary();
            }
        }

        private static Color ink(SwingTreeLookAndFeel.Variant variant, SwingTreeLookAndFeel.Palette p, boolean enabled ) {
            if ( !enabled )
                return p.textDisabled();
            return variant.isFilled() ? p.onFilled() : p.text();
        }
    }

    /**
     *  <b>Nimbus</b>: the look and feel Sun shipped with Java 6 update 10, rebuilt on the style
     *  engine from the painters the JDK generates for it.
     *  <p>
     *  Nothing here was measured off a screenshot. Every shape, every gradient stop and every colour
     *  was read out of Nimbus's own painters, so a button is filled exactly as Nimbus fills one: a
     *  lip one pixel below it, an edge filling its whole shape and a face one pixel in from that,
     *  which leaves the edge as an outline. Those three are a {@link NimbusMould}, and the style
     *  engine paints them as two gradients, one clipped to the body and one to the interior, with
     *  the lip or the focus ring in the two pixel margin around them.
     *  <p>
     *  Nimbus writes each colour as an offset from one of a few named colours, and so does this
     *  preset: {@link NimbusScheme} reads {@code nimbusBase}, {@code nimbusBlueGrey},
     *  {@code control} and the rest from {@link UIManager}, where an application re-tints Nimbus,
     *  and takes whatever it does not find there from the installed palette. The keys Nimbus is laid
     *  out through - the room around a table heading, a list cell or a tab, a titled border's title
     *  above its line - are installed as {@link UIManager} defaults too, see {@link #installDefaults}.
     *  <p>
     *  Two things are this preset's own, because Nimbus has nothing to say about them. A
     *  {@link SwingTreeLookAndFeel.Variant#PRIMARY primary} or {@link SwingTreeLookAndFeel.Variant#DANGER
     *  dangerous} button is a Nimbus button whose background is {@code nimbusOrange} or
     *  {@code nimbusRed}, which is how Nimbus paints a button an application has given a background.
     *  And a {@link SwingTreeLookAndFeel.Surface#CARD card} is lifted off the window by a paler
     *  ground and a soft outline.
     *
     *  @see SwingTreeLookAndFeel.StylePreset#NIMBUS
     */
    static final class Nimbus
    {
        private Nimbus() {}

        /** The corner radius of everything that has one, in developer pixels. {@link Symbols.Nimbus}
         *  reads it too, so that an actuator standing against a rounded outline is cut to the same
         *  curve rather than squared off across it. */
        static final int RADIUS = 5;

        /** How round a button's edge is, as the width of the arc its corners are cut along: nine
         *  pixels, which is what Nimbus fills it with and what the style engine's border radius
         *  means. */
        private static final double BUTTON_ARC = 9;

        /** A button on a tool bar is rounder than one on a form, and less round again while it is
         *  switched on. */
        private static final double TOOL_ARC          = 12;
        private static final double TOOL_SELECTED_ARC = 10;

        /** What a tool bar button keeps between its label and its edge, from Nimbus's
         *  {@code ToolBar:Button.contentMargins} of four all round, less the margin and the edge. */
        private static final int TOOL_PAD = 4 - 2 - 1;

        /**
         *  The room kept outside a control's edge. Nimbus draws its focus ring from six tenths of a
         *  pixel inside the component's bounds to the edge two pixels in, and the shadow a raised
         *  control casts in the row under the edge, so both live in these two pixels.
         */
        private static final int MARGIN = 2;

        /**
         *  How far the focus ring of a square control - a text field, a scroll pane - stands out
         *  from its edge on each of the four sides. Nimbus draws that ring from six tenths of a
         *  pixel inside the bounds of a control inset by the whole {@link #MARGIN}, which leaves
         *  the one and two fifths of a pixel that the ring keeps whatever margin it ends up in.
         */
        private static final float[] SQUARE_RING = { MARGIN - 0.6f, MARGIN - 0.6f, MARGIN - 0.6f, MARGIN - 0.6f };

        /**
         *  What a button keeps between its label and its edge, vertically and then horizontally.
         *  Nimbus lays a button out to its {@code Button.contentMargins} of six by fourteen, which
         *  is measured from the component's bounds and therefore has to lose the {@link #MARGIN} and
         *  the pixel of edge that stand outside the padding here.
         */
        private static final int PAD_Y = 6 - MARGIN - 1;
        private static final int PAD_X = 14 - MARGIN - 1;

        /** What anything you can type into keeps between its text and its bounds: Nimbus's
         *  {@code TextField.contentMargins} of six all round. An editor pane and a text pane keep
         *  four above and below instead. */
        private static final int FIELD_PAD_Y = 6;
        private static final int FIELD_PAD_X = 6;
        private static final int PAGE_PAD_Y  = 4;

        /** How round a combo box is, as the width of the arc its corners are cut along. */
        private static final double COMBO_ARC = 10;

        /** What a combo box keeps around the label showing its value, inside its edge. The label
         *  brings a pixel of its own, and Nimbus puts the text seven pixels in from the left and
         *  five down from the top. */
        private static final int COMBO_PAD_Y = 1;
        private static final int COMBO_PAD_X = 3;

        // The colours of a text field, from TextFieldPainter.
        private static final NimbusScheme.Gradient FIELD_EDGE_TOP = NimbusScheme.gradient(new double[]{ 0.1, 0.5, 0.9 },
                NimbusScheme.shade(NimbusScheme.Key.BLUE_GREY, -0.027777791, -0.0965403, -0.18431371), NimbusScheme.MID,
                NimbusScheme.shade(NimbusScheme.Key.BLUE_GREY, 0.055555582, -0.1048766, -0.05098039));
        private static final NimbusScheme.Gradient FIELD_SHADE = NimbusScheme.gradient(new double[]{ 0.1, 0.5, 0.9 },
                NimbusScheme.shade(NimbusScheme.Key.LIGHT_BACKGROUND, 0.6666667, 0.004901961, -0.19999999), NimbusScheme.MID,
                NimbusScheme.shade(NimbusScheme.Key.LIGHT_BACKGROUND, 0, 0, 0));
        private static final NimbusScheme.Shade FIELD_SIDE   = NimbusScheme.shade(NimbusScheme.Key.BLUE_GREY, 0.055555582, -0.10512091, -0.019607842);
        private static final NimbusScheme.Shade FIELD_BOTTOM = NimbusScheme.shade(NimbusScheme.Key.BLUE_GREY, 0.055555582, -0.105344966, 0.011764705);
        private static final NimbusScheme.Shade FIELD_DISABLED = NimbusScheme.shade(NimbusScheme.Key.BLUE_GREY, -0.015872955, -0.07995863, 0.15294117);
        private static final NimbusScheme.Gradient FIELD_DISABLED_EDGE_TOP = NimbusScheme.gradient(new double[]{ 0, 0.5, 1 },
                NimbusScheme.shade(NimbusScheme.Key.BLUE_GREY, -0.006944418, -0.07187897, 0.06666666), NimbusScheme.MID,
                NimbusScheme.shade(NimbusScheme.Key.BLUE_GREY, 0.007936537, -0.07826825, 0.10588235));
        private static final NimbusScheme.Gradient FIELD_DISABLED_SHADE = NimbusScheme.gradient(new double[]{ 0, 0.5, 1 },
                NimbusScheme.shade(NimbusScheme.Key.BLUE_GREY, 0.007936537, -0.07856284, 0.11372548), NimbusScheme.MID,
                NimbusScheme.shade(NimbusScheme.Key.BLUE_GREY, -0.015872955, -0.07995863, 0.15294117));
        private static final NimbusScheme.Shade FIELD_DISABLED_LEFT  = NimbusScheme.shade(NimbusScheme.Key.BLUE_GREY, 0.007936537, -0.07796818, 0.09803921);
        private static final NimbusScheme.Shade FIELD_DISABLED_RIGHT = NimbusScheme.shade(NimbusScheme.Key.BLUE_GREY, 0.007936537, -0.07826825, 0.10588235);

        // The text field of a spinner or of an editable combo box, from SpinnerPanelSpinnerFormattedTextFieldPainter.
        private static final NimbusScheme.Gradient OPEN_FIELD_EDGE_TOP = NimbusScheme.gradient(new double[]{ 0, 0.496, 0.991 },
                NimbusScheme.shade(NimbusScheme.Key.BLUE_GREY, -0.027777791, -0.0965403, -0.18431371), NimbusScheme.MID,
                NimbusScheme.shade(NimbusScheme.Key.BLUE_GREY, 0.055555582, -0.1048766, -0.08));
        private static final NimbusScheme.Gradient SPINNER_FIELD_SHADE = NimbusScheme.gradient(new double[]{ 0, 0.168, 1 },
                NimbusScheme.shade(NimbusScheme.Key.BLUE_GREY, 0.055555582, -0.105624355, 0.054901958), NimbusScheme.MID,
                NimbusScheme.shade(NimbusScheme.Key.BLUE_GREY, 0, -0.110526316, 0.25490195));
        private static final NimbusScheme.Shade OPEN_FIELD_EDGE = FIELD_BOTTOM;
        private static final NimbusScheme.Shade OPEN_FIELD_LIP  = NimbusScheme.shade(NimbusScheme.Key.BLUE_GREY, -0.6111111, -0.110526316, -0.74509805, -237);
        private static final NimbusScheme.Shade OPEN_FIELD_DISABLED_EDGE = NimbusScheme.shade(NimbusScheme.Key.BASE, 0.040395975, -0.60315615, 0.29411763);
        private static final NimbusScheme.Shade OPEN_FIELD_DISABLED_FACE = NimbusScheme.shade(NimbusScheme.Key.BASE, 0.016586483, -0.6051466, 0.3490196);

        private static final Tuple<StyleRule> RULES = Tuple.of(
            StyleRule.of(JPanel.class,         Nimbus::panel),
            StyleRule.of(AbstractButton.class, Nimbus::button),
            StyleRule.of(JCheckBox.class,      Nimbus::tickable),
            StyleRule.of(JRadioButton.class,   Nimbus::tickable),
            StyleRule.of(JMenuItem.class,      Nimbus::menuItem),
            StyleRule.of(JMenuBar.class,       Nimbus::menuBar),
            StyleRule.of(JPopupMenu.class,     Nimbus::popupMenu),
            StyleRule.of(JLabel.class,         Nimbus::label),
            StyleRule.of(JTextField.class,     Nimbus::field),
            StyleRule.of(JTextArea.class,      Nimbus::area),
            StyleRule.of(JEditorPane.class,    Nimbus::page),
            StyleRule.of(JSeparator.class,     Nimbus::separator),
            StyleRule.of(JToolTip.class,       Nimbus::toolTip),
            StyleRule.of(JProgressBar.class,   Nimbus::progressBar),
            StyleRule.of(JSlider.class,        Nimbus::slider),
            StyleRule.of(JScrollBar.class,     Nimbus::scrollBar),
            StyleRule.of(JScrollPane.class,    Nimbus::scrollPane),
            StyleRule.of(JViewport.class,      Nimbus::viewport),
            StyleRule.of(JComboBox.class,      Nimbus::comboBox),
            StyleRule.of(JSpinner.class,       Nimbus::spinner),
            StyleRule.of(JTabbedPane.class,    Nimbus::tabbedPane),
            StyleRule.of(JList.class,          Nimbus::sheet),
            StyleRule.of(JTable.class,         Nimbus::sheet),
            StyleRule.of(JTableHeader.class,   Nimbus::tableHeader),
            StyleRule.of(JTree.class,          Nimbus::sheet),
            StyleRule.of(JToolBar.class,       Nimbus::toolBar),
            StyleRule.of(JSplitPane.class,     Nimbus::splitPane)
        );

        static Tuple<StyleRule> rules() { return RULES; }

        // ── Surfaces ─────────────────────────────────────────────────────────

        @SuppressWarnings("deprecation") // component() is the documented hook for LAF state reads
        private static ComponentStyleDelegate<JPanel> panel( Theme theme, ComponentStyleDelegate<JPanel> it ) {
            SwingTreeLookAndFeel.Palette p = theme.palette();
            it = it.foregroundColor(p.text());
            switch ( SwingTreeLookAndFeel.Surface.of(it.component()) ) {
                case CARD:
                    // Nimbus paints a panel and the window it lies on the same colour, so a card has
                    // to be lifted off the ground rather than tinted away from it.
                    return it
                            .backgroundColor(LafUtilities.shiftHsb(p.surface(), -0.020, +0.045))
                            .borderRadius(RADIUS)
                            .border(1, p.borderSoft())
                            .margin(MARGIN);
                case RAIL:
                    return it.backgroundColor(p.surface()).borderWidth(0).borderRadius(0);
                case TRANSPARENT:
                    return it.backgroundColor(SwingTreeLookAndFeel.Palette.TRANSPARENT);
                case WINDOW:
                default:
                    // A spinner's editor is a panel the application never declared, sitting inside a
                    // box the spinner has already been given.
                    return it.backgroundColor(
                                LafUtilities.isControlInternal(it.component()) ? SwingTreeLookAndFeel.Palette.TRANSPARENT
                                                                              : p.background()
                            );
            }
        }

        /**
         *  A scroll pane: a square one pixel outline two pixels inside its bounds, which is the box of
         *  whatever it scrolls, and the focus ring around it while a text component inside it has
         *  the focus.
         */
        @SuppressWarnings("deprecation")
        private static ComponentStyleDelegate<JScrollPane> scrollPane( Theme theme, ComponentStyleDelegate<JScrollPane> it ) {
            NimbusScheme s    = theme.nimbusScheme();
            JScrollPane  pane = it.component();
            it = it.foregroundColor(s.get(NimbusScheme.Key.TEXT));
            if ( SwingTreeLookAndFeel.Surface.of(pane) == SwingTreeLookAndFeel.Surface.TRANSPARENT )
                return it.backgroundColor(SwingTreeLookAndFeel.Palette.TRANSPARENT).margin(0).borderWidth(0).borderRadius(0).padding(0);
            Component view    = pane.getViewport() == null ? null : pane.getViewport().getView();
            boolean   focused = view instanceof JTextComponent && view.isFocusOwner();
            return it
                    .margin(2)
                    .padding(0)
                    .borderRadius(0)
                    .border(1, s.get(NimbusScheme.Key.BORDER))
                    .backgroundColor(SwingTreeLookAndFeel.Palette.TRANSPARENT)
                    .painter(UI.Layer.BACKGROUND, UI.ComponentArea.ALL, "ring", NimbusRing.of(
                        it, focused ? s.get(NimbusScheme.Key.FOCUS) : SwingTreeLookAndFeel.Palette.TRANSPARENT,
                        SQUARE_RING, 0, 0
                    ));
        }

        @SuppressWarnings("deprecation")
        private static ComponentStyleDelegate<JViewport> viewport( Theme theme, ComponentStyleDelegate<JViewport> it ) {
            NimbusScheme s = theme.nimbusScheme();
            return it
                    .foregroundColor(s.get(NimbusScheme.Key.TEXT))
                    .backgroundColor(
                        SwingTreeLookAndFeel.Surface.of(it.component()) == SwingTreeLookAndFeel.Surface.TRANSPARENT
                            ? SwingTreeLookAndFeel.Palette.TRANSPARENT : s.get(NimbusScheme.Key.CONTROL)
                    );
        }

        /** A list, a table or a tree: a plain white page, because the scroll pane around it is the frame. */
        private static <C extends JComponent> ComponentStyleDelegate<C> sheet( Theme theme, ComponentStyleDelegate<C> it ) {
            SwingTreeLookAndFeel.Palette p = theme.palette();
            return it.backgroundColor(p.surfaceField()).foregroundColor(p.text()).borderWidth(0);
        }

        // ── Controls ─────────────────────────────────────────────────────────

        /**
         *  A button, a toggle button, or either of them on a tool bar.
         *  <p>
         *  Nimbus has no filled buttons, only the one blue default button and buttons whose
         *  background the application has set, which it paints in the same light over that
         *  colour. So a {@link SwingTreeLookAndFeel.Variant#PRIMARY primary} button is a Nimbus
         *  button laid over {@code nimbusOrange} and a {@link SwingTreeLookAndFeel.Variant#DANGER
         *  dangerous} one a Nimbus button laid over {@code nimbusRed}, and a
         *  {@link SwingTreeLookAndFeel.Variant#QUIET quiet} one is painted the way Nimbus paints
         *  every button on a tool bar: not at all until the pointer arrives.
         */
        @SuppressWarnings("deprecation")
        private static ComponentStyleDelegate<AbstractButton> button( Theme theme, ComponentStyleDelegate<AbstractButton> it ) {
            NimbusScheme   s = theme.nimbusScheme();
            AbstractButton b = it.component();
            ButtonModel    m = b.getModel();

            boolean enabled   = b.isEnabled();
            boolean pressed   = enabled && m.isArmed() && m.isPressed();
            boolean selected  = m.isSelected();
            boolean rollover  = enabled && m.isRollover();
            boolean focused   = enabled && b.isFocusOwner();
            boolean isDefault = b instanceof JButton && ((JButton) b).isDefaultButton();

            SwingTreeLookAndFeel.Variant variant = SwingTreeLookAndFeel.Variant.of(b);
            boolean onToolBar = b.getParent() instanceof JToolBar;
            boolean quiet     = onToolBar || variant == SwingTreeLookAndFeel.Variant.QUIET;
            Color   tint      = variant == SwingTreeLookAndFeel.Variant.PRIMARY ? s.get(NimbusScheme.Key.ORANGE)
                              : variant == SwingTreeLookAndFeel.Variant.DANGER  ? s.get(NimbusScheme.Key.RED)
                              : null;

            it = it.foregroundColor(ink(s, enabled, isDefault && pressed));
            it = onToolBar ? it.padding(TOOL_PAD, TOOL_PAD, TOOL_PAD, TOOL_PAD)
                           : it.padding(PAD_Y, PAD_X, PAD_Y, PAD_X);

            NimbusMould mould = selected ? ( !enabled ? NimbusMould.TOGGLE_SELECTED_DISABLED
                                           : pressed  ? NimbusMould.TOGGLE_SELECTED_PRESSED
                                           : rollover ? NimbusMould.TOGGLE_SELECTED_MOUSE_OVER
                                           :            NimbusMould.TOGGLE_SELECTED )
                              : quiet    ? ( pressed  ? NimbusMould.TOOL_BAR_BUTTON_PRESSED
                                           : rollover ? NimbusMould.TOOL_BAR_BUTTON_MOUSE_OVER
                                           :            null )
                              : !enabled ? NimbusMould.BUTTON_DISABLED
                              : b instanceof JToggleButton && pressed ? NimbusMould.TOGGLE_SELECTED_MOUSE_OVER
                              : isDefault ? ( pressed  ? NimbusMould.DEFAULT_BUTTON_PRESSED
                                            : rollover ? NimbusMould.DEFAULT_BUTTON_MOUSE_OVER
                                            :            NimbusMould.DEFAULT_BUTTON )
                              : pressed  ? NimbusMould.BUTTON_PRESSED
                              : rollover ? NimbusMould.BUTTON_MOUSE_OVER
                              :            NimbusMould.BUTTON;

            double arc = !onToolBar ? BUTTON_ARC : selected ? TOOL_SELECTED_ARC : TOOL_ARC;
            if ( mould == null )
                return it
                        .margin(MARGIN)
                        .borderRadius(arc)
                        .border(1, SwingTreeLookAndFeel.Palette.TRANSPARENT)
                        .backgroundColor(SwingTreeLookAndFeel.Palette.TRANSPARENT)
                        .painter(UI.Layer.BACKGROUND, UI.ComponentArea.ALL, "ring", NimbusRing.aroundEdge(
                            it, focused ? s.get(NimbusScheme.Key.FOCUS) : SwingTreeLookAndFeel.Palette.TRANSPARENT, (float) arc
                        ));

            return mould.style(it, s, arc, tint, focused);
        }

        /**
         *  The ink of a button's label. Nimbus writes black on every button, including a blue or a
         *  tinted one, and only turns it white on the default button while it is held down, where
         *  the face goes to the full base colour.
         */
        private static Color ink( NimbusScheme s, boolean enabled, boolean onPressedDefault ) {
            if ( !enabled )
                return s.get(NimbusScheme.Key.DISABLED_TEXT);
            return s.get(onPressedDefault ? NimbusScheme.Key.SELECTED_TEXT : NimbusScheme.Key.TEXT);
        }

        /**
         *  A check box or a radio button. The box itself is a symbol rather than a style, and so is
         *  its focus ring, which Nimbus draws around the box and not around the label, so all that
         *  is left here is to keep the label's own ground out of the way of the panel behind it.
         */
        @SuppressWarnings("deprecation")
        private static <C extends AbstractButton> ComponentStyleDelegate<C> tickable( Theme theme, ComponentStyleDelegate<C> it ) {
            NimbusScheme s = theme.nimbusScheme();
            return it
                    .margin(0)
                    .padding(0)
                    .borderWidth(0)
                    .backgroundColor(SwingTreeLookAndFeel.Palette.TRANSPARENT)
                    .foregroundColor(s.get(it.component().isEnabled() ? NimbusScheme.Key.TEXT : NimbusScheme.Key.DISABLED_TEXT));
        }

        // ── Inputs ───────────────────────────────────────────────────────────

        /**
         *  A combo box. One that cannot be edited is a raised control like a button, whose blue end
         *  {@link Symbols.Nimbus#paintComboArrow} draws over the combo box's own edge. One that can
         *  be edited is a text field and that blue end side by side, and paints nothing itself.
         */
        @SuppressWarnings("deprecation")
        private static ComponentStyleDelegate<JComboBox> comboBox( Theme theme, ComponentStyleDelegate<JComboBox> it ) {
            NimbusScheme s       = theme.nimbusScheme();
            JComboBox<?> combo   = it.component();
            boolean      enabled = combo.isEnabled();
            it = it.foregroundColor(s.get(enabled ? NimbusScheme.Key.TEXT : NimbusScheme.Key.DISABLED_TEXT));
            if ( combo.isEditable() )
                return it.margin(0).padding(0).borderWidth(0).backgroundColor(SwingTreeLookAndFeel.Palette.TRANSPARENT);
            NimbusMould mould = !enabled                           ? NimbusMould.COMBO_BOX_DISABLED
                              : combo.isPopupVisible()             ? NimbusMould.COMBO_BOX_PRESSED
                              : LafUtilities.isUnderPointer(combo) ? NimbusMould.COMBO_BOX_MOUSE_OVER
                              :                                      NimbusMould.COMBO_BOX;
            return mould.style(it, s, COMBO_ARC, null, enabled && LafUtilities.hasFocus(combo))
                        .padding(COMBO_PAD_Y, 0, COMBO_PAD_Y, COMBO_PAD_X);
        }

        /** A spinner paints nothing: its text field is the box, and its two buttons stand beside it. */
        private static ComponentStyleDelegate<JSpinner> spinner( Theme theme, ComponentStyleDelegate<JSpinner> it ) {
            return it.margin(0).padding(0).borderWidth(0).backgroundColor(SwingTreeLookAndFeel.Palette.TRANSPARENT);
        }

        @SuppressWarnings("deprecation")
        private static ComponentStyleDelegate<JTextField> field( Theme theme, ComponentStyleDelegate<JTextField> it ) {
            return input(theme, it, it.component(), FIELD_PAD_Y, FIELD_PAD_X);
        }

        @SuppressWarnings("deprecation")
        private static ComponentStyleDelegate<JTextArea> area( Theme theme, ComponentStyleDelegate<JTextArea> it ) {
            return input(theme, it, it.component(), FIELD_PAD_Y, FIELD_PAD_X);
        }

        /** An editor pane or a text pane, which Nimbus keeps two pixels tighter above and below. */
        @SuppressWarnings("deprecation")
        private static <C extends JTextComponent> ComponentStyleDelegate<C> page( Theme theme, ComponentStyleDelegate<C> it ) {
            return input(theme, it, it.component(), PAGE_PAD_Y, FIELD_PAD_X);
        }

        /**
         *  Anything you can type into. Nimbus draws four kinds of box for it, all square: a text field
         *  on its own, cut into the panel with a dark top edge and a soft shade under it; the same box
         *  open on its right where a spinner's or an editable combo box's buttons continue it; no box at
         *  all inside a scroll pane, whose own outline is the box; and the ground behind a text
         *  component that stands inside some other control, which is none of the above.
         */
        private static <C extends JComponent> ComponentStyleDelegate<C> input(
            Theme theme, ComponentStyleDelegate<C> it, JTextComponent text, int padY, int padX
        ) {
            NimbusScheme s       = theme.nimbusScheme();
            boolean      enabled = text.isEnabled();
            it = it.foregroundColor(s.get(enabled ? NimbusScheme.Key.TEXT : NimbusScheme.Key.DISABLED_TEXT));
            Color page = enabled ? s.get(NimbusScheme.Key.LIGHT_BACKGROUND) : FIELD_DISABLED.in(s);
            if ( text.getParent() instanceof JViewport )
                return it.margin(0).padding(padY, padX, padY, padX).borderWidth(0).backgroundColor(page);
            boolean inSpinner = SwingUtilities.getAncestorOfClass(JSpinner.class, text) != null;
            boolean inCombo   = !inSpinner && SwingUtilities.getAncestorOfClass(JComboBox.class, text) != null;
            if ( inSpinner || inCombo )
                return openField(it, s, text, enabled, page, inSpinner);

            boolean focused = enabled && text.isFocusOwner();
            return it
                    .margin(2)
                    .padding(padY - 3, padX - 3, padY - 3, padX - 3)
                    .border(1, SwingTreeLookAndFeel.Palette.TRANSPARENT)
                    .borderColors(
                        enabled ? topEdge(s, FIELD_EDGE_TOP, 0.1f, 0.5f) : topEdge(s, FIELD_DISABLED_EDGE_TOP, 0f, 0.5f),
                        ( enabled ? FIELD_SIDE : FIELD_DISABLED_RIGHT ).in(s),
                        enabled ? FIELD_BOTTOM.in(s) : page,
                        ( enabled ? FIELD_SIDE : FIELD_DISABLED_LEFT ).in(s)
                    )
                    .backgroundColor(page)
                    .gradient(UI.Layer.BACKGROUND, "shade", g -> innerShade(g, text, enabled ? FIELD_SHADE : FIELD_DISABLED_SHADE, s,
                                                                            enabled ? 0.1f : 0f, enabled ? 0.9f : 1f))
                    .painter(UI.Layer.BACKGROUND, UI.ComponentArea.ALL, "ring", NimbusRing.of(
                        it, focused ? s.get(NimbusScheme.Key.FOCUS) : SwingTreeLookAndFeel.Palette.TRANSPARENT,
                        SQUARE_RING, 0, 0
                    ));
        }

        /**
         *  The text field of a spinner or of an editable combo box: Nimbus's text field without its
         *  right edge, so that the buttons beside it close the box, and with a faint lip under it
         *  that the buttons continue.
         */
        private static <C extends JComponent> ComponentStyleDelegate<C> openField(
            ComponentStyleDelegate<C> it, NimbusScheme s, JTextComponent text, boolean enabled, Color page, boolean inSpinner
        ) {
            boolean   focused = enabled && text.isFocusOwner();
            Color     edge    = ( enabled ? OPEN_FIELD_EDGE : OPEN_FIELD_DISABLED_EDGE ).in(s);
            Color     ground  = enabled ? page : OPEN_FIELD_DISABLED_FACE.in(s);
            NimbusScheme.Gradient top = enabled ? OPEN_FIELD_EDGE_TOP : FIELD_DISABLED_EDGE_TOP;
            Color     lip     = focused ? s.get(NimbusScheme.Key.FOCUS) : OPEN_FIELD_LIP.in(s);
            // The focus ring stands out from the box on the three sides that have one, and the lip
            // is the single row under the box; on the right the buttons continue the box, so
            // neither reaches past it there.
            float[]   outsets = focused ? new float[]{ MARGIN - 0.67f, MARGIN - 0.67f, 0, MARGIN - 0.75f }
                                        : new float[]{ 0, -1, 0, 1 };
            return it
                    .margin(MARGIN, 0, MARGIN, MARGIN)
                    .padding(inSpinner ? 3 : 2, inSpinner ? 6 : 3, 2, 3)
                    .borderWidths(1, 0, 1, 1)
                    .borderColors(topEdge(s, top, 0f, enabled ? 0.496f : 0.5f), edge, edge, edge)
                    .backgroundColor(ground)
                    .gradient(UI.Layer.BACKGROUND, "shade", g -> enabled && inSpinner
                                    ? innerShade(g, text, SPINNER_FIELD_SHADE, s, 0f, 1f).fractions(0, 0.168 * shadeFraction(text), shadeFraction(text))
                                    : innerShade(g, text, enabled ? FIELD_SHADE : FIELD_DISABLED_SHADE, s, enabled ? 0.1f : 0f, enabled ? 0.9f : 1f))
                    .painter(UI.Layer.BACKGROUND, UI.ComponentArea.ALL, "ring",
                             NimbusRing.of(it, lip, outsets, 0, 0));
        }

        /**
         *  The colour of the top row of a text field's edge. Nimbus fills the top three pixels of the
         *  box with one gradient and lets the face cover all of it but the outermost row and the two
         *  side columns, so the row is that gradient sampled half a pixel down.
         */
        private static Color topEdge( NimbusScheme s, NimbusScheme.Gradient gradient, float first, float middle ) {
            Color[] stops = gradient.colors(s);
            float   t     = ( 0.5f / 3f - first ) / ( middle - first );
            return NimbusScheme.mix(stops[0], stops[1], Math.max(0, Math.min(1, t)));
        }

        /**
         *  The soft shade along the top of a text field's face, two pixels deep whatever the height of
         *  the field, which is why its stops are worked out from the height rather than written down.
         */
        private static swingtree.style.GradientConf innerShade(
            swingtree.style.GradientConf g, JComponent c, NimbusScheme.Gradient shade, NimbusScheme s, float first, float last
        ) {
            float depth = shadeFraction(c);
            return g.colors(shade.colors(s))
                    .fractions(first * depth, 0.5 * depth, last * depth)
                    .span(UI.Span.TOP_TO_BOTTOM)
                    .boundary(UI.ComponentBoundary.BORDER_TO_INTERIOR)
                    .clipTo(UI.ComponentArea.INTERIOR);
        }

        /** Two pixels as a fraction of the height of a text field's face. */
        private static float shadeFraction( JComponent c ) {
            float inner = c.getHeight() / UI.scale() - 6;
            return inner > 2 ? 2f / inner : 1f;
        }

        // ── Menus ────────────────────────────────────────────────────────────

        /**
         *  A menu on a menu bar or an item in a popup: nothing at all at rest, and a flat band of
         *  {@code nimbusSelection} with white ink while it is armed. Nimbus writes a menu's label
         *  in a grey only just off black.
         */
        @SuppressWarnings("deprecation")
        private static ComponentStyleDelegate<JMenuItem> menuItem( Theme theme, ComponentStyleDelegate<JMenuItem> it ) {
            NimbusScheme s       = theme.nimbusScheme();
            JMenuItem    item    = it.component();
            ButtonModel  m       = item.getModel();
            boolean      enabled = item.isEnabled();
            boolean      armed   = enabled && ( m.isArmed() || ( item instanceof JMenu && m.isSelected() ) );
            boolean      onBar   = item.getParent() instanceof JMenuBar;
            Color        ink     = !enabled ? s.get(NimbusScheme.Key.DISABLED_TEXT)
                                 : armed    ? s.get(NimbusScheme.Key.SELECTED_TEXT)
                                 :            MENU_TEXT.in(s);
            return it
                    .padding(1, onBar ? 4 : item instanceof JMenu ? 5 : 13, 2, onBar ? 4 : 12)
                    .borderRadius(0)
                    .borderWidth(0)
                    .backgroundColor(armed ? s.get(NimbusScheme.Key.SELECTION) : SwingTreeLookAndFeel.Palette.TRANSPARENT)
                    .foregroundColor(ink);
        }

        /** A menu bar: the control colour with a white sheen fading out down its top quarter, and a rule under it. */
        private static ComponentStyleDelegate<JMenuBar> menuBar( Theme theme, ComponentStyleDelegate<JMenuBar> it ) {
            NimbusScheme s = theme.nimbusScheme();
            return it
                    .padding(2, 6, 1, 6)
                    .borderWidths(0, 0, 1, 0)
                    .borderColor(s.get(NimbusScheme.Key.BORDER))
                    .backgroundColor(s.get(NimbusScheme.Key.CONTROL))
                    .gradient(UI.Layer.BACKGROUND, "sheen", g -> MENU_BAR_SHEEN.over(g, s, null).clipTo(UI.ComponentArea.BODY))
                    .foregroundColor(MENU_TEXT.in(s));
        }

        /** A popup menu: a square grey outline around a sheet shading from white at its ends to the pale {@code menu} colour. */
        private static ComponentStyleDelegate<JPopupMenu> popupMenu( Theme theme, ComponentStyleDelegate<JPopupMenu> it ) {
            NimbusScheme s = theme.nimbusScheme();
            return it
                    .margin(0)
                    .padding(5, 0, 5, 0)
                    .borderRadius(0)
                    .border(1, POPUP_OUTLINE.in(s))
                    .backgroundColor(s.get(NimbusScheme.Key.LIGHT_BACKGROUND))
                    .gradient(UI.Layer.BACKGROUND, "sheet", g -> POPUP_SHEET.over(g, s, null)
                                                                    .boundary(UI.ComponentBoundary.BORDER_TO_INTERIOR)
                                                                    .clipTo(UI.ComponentArea.INTERIOR))
                    .foregroundColor(MENU_TEXT.in(s));
        }

        /** A tool tip: {@code info} in a square outline of {@code nimbusBorder}. */
        private static ComponentStyleDelegate<JToolTip> toolTip( Theme theme, ComponentStyleDelegate<JToolTip> it ) {
            NimbusScheme s = theme.nimbusScheme();
            return it
                    .margin(0)
                    .padding(3, 3, 3, 3)
                    .borderRadius(0)
                    .border(1, s.get(NimbusScheme.Key.BORDER))
                    .backgroundColor(s.get(NimbusScheme.Key.INFO))
                    .foregroundColor(s.get(NimbusScheme.Key.TEXT));
        }

        /** The ink of a menu's label, which Nimbus writes down as a value rather than deriving it. */
        private static final NimbusScheme.Shade MENU_TEXT = NimbusScheme.constant(35, 35, 36, 255);

        private static final NimbusScheme.Gradient MENU_BAR_SHEEN = NimbusScheme.gradient(new double[]{ 0, 0.015, 0.03, 0.234, 0.757 },
                NimbusScheme.shade(NimbusScheme.Key.BLUE_GREY, -0.027777791, -0.10255819, 0.23921567), NimbusScheme.MID,
                NimbusScheme.shade(NimbusScheme.Key.BLUE_GREY, -0.111111104, -0.10654225, 0.23921567, -29), NimbusScheme.MID,
                NimbusScheme.shade(NimbusScheme.Key.BLUE_GREY, 0, -0.110526316, 0.25490195, -255));
        private static final NimbusScheme.Shade POPUP_OUTLINE = NimbusScheme.shade(NimbusScheme.Key.BLUE_GREY, -0.6111111, -0.110526316, -0.39607844);
        private static final NimbusScheme.Gradient POPUP_SHEET = NimbusScheme.gradient(new double[]{ 0, 0.003, 0.02, 0.5, 0.98, 0.996, 1 },
                NimbusScheme.shade(NimbusScheme.Key.BASE, 0, -0.6357143, 0.45098037), NimbusScheme.MID,
                NimbusScheme.shade(NimbusScheme.Key.BASE, 0.021348298, -0.6150531, 0.39999998), NimbusScheme.MID,
                NimbusScheme.shade(NimbusScheme.Key.BASE, 0.021348298, -0.6150531, 0.39999998), NimbusScheme.MID,
                NimbusScheme.shade(NimbusScheme.Key.BASE, 0, -0.6357143, 0.45098037));

        // ── The rest ─────────────────────────────────────────────────────────

        @SuppressWarnings("deprecation")
        private static ComponentStyleDelegate<JLabel> label( Theme theme, ComponentStyleDelegate<JLabel> it ) {
            SwingTreeLookAndFeel.Palette p = theme.palette();
            return it.foregroundColor(it.component().isEnabled() ? p.text() : p.textDisabled());
        }

        /** A hairline the delegate draws across the middle: a ground would make it a bar as tall
         *  as whatever box a layout gave it. */
        private static ComponentStyleDelegate<JSeparator> separator( Theme theme, ComponentStyleDelegate<JSeparator> it ) {
            SwingTreeLookAndFeel.Palette p = theme.palette();
            return it.backgroundColor(SwingTreeLookAndFeel.Palette.TRANSPARENT).foregroundColor(p.borderSoft());
        }

        /**
         *  The trough a progress bar fills: a square sunk channel two pixels inside the bar's bounds.
         *  The bar itself is a symbol, because it runs over the trough's edge and its glow reaches
         *  the bounds.
         */
        private static ComponentStyleDelegate<JProgressBar> progressBar( Theme theme, ComponentStyleDelegate<JProgressBar> it ) {
            NimbusScheme s = theme.nimbusScheme();
            boolean enabled = it.component().isEnabled();
            NimbusScheme.Gradient edge = enabled ? TROUGH_EDGE : TROUGH_EDGE_DISABLED;
            NimbusScheme.Gradient face = enabled ? TROUGH_FACE : TROUGH_FACE_DISABLED;
            return it
                    .margin(2)
                    .borderRadius(0)
                    .border(1, SwingTreeLookAndFeel.Palette.TRANSPARENT)
                    .backgroundColor(SwingTreeLookAndFeel.Palette.TRANSPARENT)
                    .gradient(UI.Layer.BACKGROUND, "edge", g -> edge.over(g, s, null)
                                                                   .boundary(UI.ComponentBoundary.EXTERIOR_TO_BORDER)
                                                                   .clipTo(UI.ComponentArea.BODY))
                    .gradient(UI.Layer.BACKGROUND, "face", g -> face.over(g, s, null)
                                                                   .boundary(UI.ComponentBoundary.BORDER_TO_INTERIOR)
                                                                   .clipTo(UI.ComponentArea.INTERIOR))
                    .painter(UI.Layer.BACKGROUND, UI.ComponentArea.ALL, "glow", new ProgressGlow(theme, it))
                    .foregroundColor(s.get(NimbusScheme.Key.TEXT));
        }

        /**
         *  The glow around the filled part of a progress bar, which lies in the bar's margin, outside
         *  the area the look and feel paints the bar itself into. It is a value holding the fill it was
         *  made for and the theme it is drawn in, so that the style changes, and the bar repaints,
         *  when either does.
         */
        private static final class ProgressGlow implements Painter
        {
            private final Theme   _theme;
            /** The trough the glow runs along, in "developer pixel" from the component's corner. */
            private final Bounds  _box;
            private final double  _ratio;
            private final boolean _horizontal;
            private final boolean _enabled;

            ProgressGlow( Theme theme, ComponentStyleDelegate<JProgressBar> it ) {
                @SuppressWarnings("deprecation") // component() is the documented hook for LAF state reads
                JProgressBar bar = it.component();
                int range   = Math.max(1, bar.getMaximum() - bar.getMinimum());
                _theme      = theme;
                _box        = LafUtilities.marginBoxOf(it);
                _ratio      = bar.isIndeterminate() ? 0 : Math.max(0, Math.min(1, ( bar.getValue() - bar.getMinimum() ) / (double) range));
                _horizontal = bar.getOrientation() == SwingConstants.HORIZONTAL;
                _enabled    = bar.isEnabled();
            }

            @Override
            public void paint( Graphics2D g ) {
                Graphics2D g2 = (Graphics2D) g.create();
                try {
                    g2.translate(_box.location().x(), _box.location().y());
                    Symbols.Nimbus.paintProgressGlow(g2, _theme.nimbusScheme(),
                            _box.size().widthOrElse(0f), _box.size().heightOrElse(0f), _ratio, _horizontal, _enabled);
                } finally {
                    g2.dispose();
                }
            }

            @Override
            public boolean canBeCached() {
                return true;
            }

            @Override
            public boolean equals( Object other ) {
                if ( !(other instanceof ProgressGlow) ) return false;
                ProgressGlow that = (ProgressGlow) other;
                return _theme == that._theme && _box.equals(that._box) && _ratio == that._ratio
                    && _horizontal == that._horizontal && _enabled == that._enabled;
            }

            @Override
            public int hashCode() {
                return Objects.hash(System.identityHashCode(_theme), _box, _ratio, _horizontal, _enabled);
            }

            @Override
            public String toString() {
                return getClass().getSimpleName() + "[box=" + _box + ", ratio=" + _ratio + "]";
            }
        }

        private static final NimbusScheme.Gradient TROUGH_EDGE = NimbusScheme.gradient(new double[]{ 0, 0.5, 1 },
                NimbusScheme.shade(NimbusScheme.Key.BLUE_GREY, 0, -0.04845735, -0.17647058), NimbusScheme.MID,
                NimbusScheme.shade(NimbusScheme.Key.BLUE_GREY, 0, -0.061345987, -0.027450979));
        private static final NimbusScheme.Gradient TROUGH_FACE = NimbusScheme.gradient(new double[]{ 0.039, 0.06, 0.081, 0.237, 0.394, 0.416, 0.439, 0.674, 0.91, 0.915, 0.919 },
                NimbusScheme.shade(NimbusScheme.Key.BLUE_GREY, 0, -0.110526316, 0.25490195), NimbusScheme.MID,
                NimbusScheme.shade(NimbusScheme.Key.BLUE_GREY, 0, -0.097921275, 0.18823528), NimbusScheme.MID,
                NimbusScheme.shade(NimbusScheme.Key.BLUE_GREY, 0.0138888955, -0.0925083, 0.12549019), NimbusScheme.MID,
                NimbusScheme.shade(NimbusScheme.Key.BLUE_GREY, 0, -0.08222443, 0.086274505), NimbusScheme.MID,
                NimbusScheme.shade(NimbusScheme.Key.BLUE_GREY, 0, -0.08477524, 0.16862744), NimbusScheme.MID,
                NimbusScheme.shade(NimbusScheme.Key.BLUE_GREY, 0, -0.086996906, 0.25490195));
        private static final NimbusScheme.Gradient TROUGH_EDGE_DISABLED = NimbusScheme.gradient(new double[]{ 0.055, 0.503, 0.952 },
                NimbusScheme.shade(NimbusScheme.Key.BLUE_GREY, 0, -0.061613273, -0.02352941), NimbusScheme.MID,
                NimbusScheme.shade(NimbusScheme.Key.BLUE_GREY, -0.01111114, -0.061265234, 0.05098039));
        private static final NimbusScheme.Gradient TROUGH_FACE_DISABLED = NimbusScheme.gradient(new double[]{ 0.039, 0.06, 0.081, 0.237, 0.394, 0.416, 0.439, 0.674, 0.91, 0.916, 0.923 },
                NimbusScheme.shade(NimbusScheme.Key.BLUE_GREY, 0.0138888955, -0.09378991, 0.19215685), NimbusScheme.MID,
                NimbusScheme.shade(NimbusScheme.Key.BLUE_GREY, 0, -0.08455229, 0.1607843), NimbusScheme.MID,
                NimbusScheme.shade(NimbusScheme.Key.BLUE_GREY, -0.027777791, -0.08362049, 0.12941176), NimbusScheme.MID,
                NimbusScheme.shade(NimbusScheme.Key.BLUE_GREY, 0.007936537, -0.07826825, 0.10588235), NimbusScheme.MID,
                NimbusScheme.shade(NimbusScheme.Key.BLUE_GREY, 0.007936537, -0.07982456, 0.1490196), NimbusScheme.MID,
                NimbusScheme.shade(NimbusScheme.Key.BLUE_GREY, 0.007936537, -0.08099045, 0.18431371));

        private static ComponentStyleDelegate<JSlider> slider( Theme theme, ComponentStyleDelegate<JSlider> it ) {
            SwingTreeLookAndFeel.Palette p = theme.palette();
            return it.backgroundColor(SwingTreeLookAndFeel.Palette.TRANSPARENT).foregroundColor(p.text());
        }

        /**
         *  The groove of a scroll bar, shading across the bar from dark along the side next to what
         *  it scrolls. Its buttons reach eight pixels into it, see {@link #installDefaults}.
         */
        @SuppressWarnings("deprecation")
        private static ComponentStyleDelegate<JScrollBar> scrollBar( Theme theme, ComponentStyleDelegate<JScrollBar> it ) {
            NimbusScheme s   = theme.nimbusScheme();
            JScrollBar   bar = it.component();
            UI.Span span = bar.getOrientation() == JScrollBar.VERTICAL ? UI.Span.LEFT_TO_RIGHT : UI.Span.TOP_TO_BOTTOM;
            NimbusScheme.Gradient groove = bar.isEnabled() ? GROOVE : GROOVE_DISABLED;
            return it
                    .backgroundColor(s.get(NimbusScheme.Key.CONTROL))
                    .foregroundColor(s.get(NimbusScheme.Key.TEXT))
                    .gradient(UI.Layer.BACKGROUND, "groove", g -> groove.over(g, s, null).span(span));
        }

        private static final NimbusScheme.Gradient GROOVE = NimbusScheme.gradient(new double[]{ 0, 0.031, 0.061, 0.097, 0.132, 0.221, 0.31, 0.474, 0.823 },
                NimbusScheme.shade(NimbusScheme.Key.BLUE_GREY, 0.02222228, -0.06465475, -0.31764707), NimbusScheme.MID,
                NimbusScheme.shade(NimbusScheme.Key.BLUE_GREY, 0, -0.06766917, -0.19607842), NimbusScheme.MID,
                NimbusScheme.shade(NimbusScheme.Key.BLUE_GREY, -0.006944418, -0.0655825, -0.04705882), NimbusScheme.MID,
                NimbusScheme.shade(NimbusScheme.Key.BLUE_GREY, 0.0138888955, -0.071117446, 0.05098039), NimbusScheme.MID,
                NimbusScheme.shade(NimbusScheme.Key.BLUE_GREY, 0, -0.07016757, 0.12941176));
        private static final NimbusScheme.Gradient GROOVE_DISABLED = NimbusScheme.gradient(new double[]{ 0.016, 0.039, 0.061, 0.161, 0.265, 0.438, 0.884 },
                NimbusScheme.shade(NimbusScheme.Key.BLUE_GREY, -0.027777791, -0.10016362, 0.011764705), NimbusScheme.MID,
                NimbusScheme.shade(NimbusScheme.Key.BLUE_GREY, -0.027777791, -0.100476064, 0.035294116), NimbusScheme.MID,
                NimbusScheme.shade(NimbusScheme.Key.BLUE_GREY, 0.055555582, -0.10606203, 0.13333333), NimbusScheme.MID,
                NimbusScheme.shade(NimbusScheme.Key.BLUE_GREY, -0.6111111, -0.110526316, 0.24705881));

        private static ComponentStyleDelegate<JTabbedPane> tabbedPane( Theme theme, ComponentStyleDelegate<JTabbedPane> it ) {
            SwingTreeLookAndFeel.Palette p = theme.palette();
            return it.backgroundColor(p.background()).foregroundColor(p.text());
        }

        private static ComponentStyleDelegate<JSplitPane> splitPane( Theme theme, ComponentStyleDelegate<JSplitPane> it ) {
            SwingTreeLookAndFeel.Palette p = theme.palette();
            return it.backgroundColor(SwingTreeLookAndFeel.Palette.TRANSPARENT).foregroundColor(p.text());
        }

        /**
         *  The heading row of a table: one lit strip with a dark rule under it. The rules between the
         *  headings are {@link Symbols.Nimbus#tableHeaderDivider}, and the room around each heading's
         *  text is Nimbus's own renderer margin, see {@link #installDefaults}.
         */
        private static ComponentStyleDelegate<JTableHeader> tableHeader( Theme theme, ComponentStyleDelegate<JTableHeader> it ) {
            NimbusScheme s = theme.nimbusScheme();
            return it
                    .padding(0)
                    .borderWidths(0, 0, 1, 0)
                    .borderColor(HEADER_RULE.in(s))
                    .backgroundColor(s.get(NimbusScheme.Key.CONTROL))
                    .gradient(UI.Layer.BACKGROUND, "face", g -> HEADER_FACE.over(g, s, null)
                                                                    .boundary(UI.ComponentBoundary.BORDER_TO_INTERIOR)
                                                                    .clipTo(UI.ComponentArea.INTERIOR))
                    .foregroundColor(s.get(NimbusScheme.Key.TEXT));
        }

        private static final NimbusScheme.Gradient HEADER_FACE = NimbusScheme.gradient(new double[]{ 0, 0.071, 0.289, 0.549, 0.704, 0.852, 1 },
                NimbusScheme.shade(NimbusScheme.Key.BLUE_GREY, 0.055555582, -0.10655806, 0.24313724), NimbusScheme.MID,
                NimbusScheme.shade(NimbusScheme.Key.BLUE_GREY, 0, -0.08455229, 0.1607843), NimbusScheme.MID,
                NimbusScheme.shade(NimbusScheme.Key.BLUE_GREY, 0, -0.07016757, 0.12941176), NimbusScheme.MID,
                NimbusScheme.shade(NimbusScheme.Key.BLUE_GREY, 0, -0.07466974, 0.23921567));
        private static final NimbusScheme.Shade HEADER_RULE = NimbusScheme.shade(NimbusScheme.Key.BORDER, -0.013888836, 0.0005823001, -0.12941176);

        /**
         *  The {@link UIManager} keys Nimbus is read through beyond its colours: the room around the
         *  text of a list cell, a table cell and a table heading, a combo box that looks held down
         *  while its list is open, and a titled border's title above its line rather than on it.
         *
         * @param table the defaults of the look and feel being installed
         * @param palette its palette
         */
        static void installDefaults( UIDefaults table, Theme theme ) {
            NimbusScheme.install(table, theme.palette());
            table.put("ComboBox.pressedWhenPopupVisible", Boolean.TRUE);
            table.put("TableHeader:\"TableHeader.renderer\".contentMargins", new javax.swing.plaf.InsetsUIResource(2, 5, 4, 5));
            javax.swing.border.Border cell = new javax.swing.plaf.BorderUIResource(
                    BorderFactory.createEmptyBorder(UI.scale(2), UI.scale(5), UI.scale(2), UI.scale(5)));
            table.put("List.cellNoFocusBorder",        cell);
            table.put("List.focusCellHighlightBorder", cell);
            table.put("Table.cellNoFocusBorder",        cell);
            table.put("Table.focusCellHighlightBorder", cell);
            table.put("TitledBorder.position", "ABOVE_TOP");
            table.put("TitledBorder.border", new NimbusLoweredBorder());
            table.put("TitledBorder.titleColor", new javax.swing.plaf.ColorUIResource(
                    NimbusScheme.derive(theme.nimbusScheme().get(NimbusScheme.Key.TEXT), 0f, 0f, 0.23f, 0)));
            java.awt.Font font = swingtree.SwingTree.get().getScaledDefaultFont();
            table.put("TitledBorder.font", new javax.swing.plaf.FontUIResource(font.deriveFont(java.awt.Font.BOLD)));
            table.put("Slider.tickColor", new javax.swing.plaf.ColorUIResource(35, 40, 48));
            table.put("ScrollBar.incrementButtonGap", UI.scale(-8));
            table.put("ScrollBar.decrementButtonGap", UI.scale(-8));
            table.put("TabbedPane.selectedTabPadInsets", new javax.swing.plaf.InsetsUIResource(0, 0, 0, 0));
            table.put("TabbedPane.labelShift", 0);
            table.put("TabbedPane.selectedLabelShift", 0);
            table.put("TabbedPane:TabbedPaneTab.contentMargins", new javax.swing.plaf.InsetsUIResource(2, 8, 3, 8));
            table.put("TabbedPane:TabbedPaneTabArea.contentMargins", new javax.swing.plaf.InsetsUIResource(3, 10, 4, 10));
            // A plain menu item reserves no column for a tick nor room for a submenu arrow in Nimbus.
            table.remove("MenuItem.checkIcon");
            table.remove("MenuItem.arrowIcon");
            for ( String menu : new String[]{ "Menu", "MenuItem", "CheckBoxMenuItem", "RadioButtonMenuItem" } )
                table.put(menu + ".textIconGap", 5);
            table.put("Tree.leftChildIndent",  12);
            table.put("Tree.rightChildIndent", 4);
        }

        /**
         *  A tool bar: the control colour with a rule under it, and room on its left for the handle
         *  {@link Symbols.Nimbus#paintDragHandle} draws, which is eleven pixels wide.
         */
        @SuppressWarnings("deprecation")
        private static ComponentStyleDelegate<JToolBar> toolBar( Theme theme, ComponentStyleDelegate<JToolBar> it ) {
            NimbusScheme s   = theme.nimbusScheme();
            JToolBar     bar = it.component();
            int handle = bar.isFloatable() ? 11 : 0;
            it = bar.getOrientation() == JToolBar.HORIZONTAL
                    ? it.padding(2, 2, 1, 2 + handle).borderWidths(0, 0, 1, 0)
                    : it.padding(2 + handle, 1, 2, 2).borderWidths(0, 1, 0, 0);
            return it
                    .borderColor(s.get(NimbusScheme.Key.BORDER))
                    .backgroundColor(s.get(NimbusScheme.Key.CONTROL))
                    .foregroundColor(s.get(NimbusScheme.Key.TEXT))
                    .painter(UI.Layer.CONTENT, new DragHandlePainter(theme, it));
        }
    }

    /**
     *  <b>Polymorphism</b>: a theme with no fixed appearance, only rules for arriving at one. Every
     *  other preset here decides how it looks and then asks the palette for the colours; this one
     *  asks the other way round, and derives its whole appearance from three readings:
     *  <ul>
     *      <li><b>What the palette leaves it to work with</b> ({@link Mood}). A palette whose ground
     *          and surfaces are one colour has them separated by light, a dark one has them rimmed,
     *          and a light one with contrast to spend gets a flat fill and a shadow.</li>
     *      <li><b>How big the control is.</b> The radius is half the control's own height up to a
     *          limit, so a one-line control comes out a pill and a tall one a soft rectangle.</li>
     *      <li><b>How deeply it is nested.</b> A card lying on another card is lifted further than
     *          one lying on the window, which is the only thing still telling the two apart.</li>
     *  </ul>
     *  So switching the palette rewrites this theme rather than re-tinting it, and it has to be seen
     *  in two palettes to be seen at all. Every rule reads its component, so none of it can be
     *  decided ahead of time.
     *
     *  @see SwingTreeLookAndFeel.StylePreset#POLYMORPHIC
     */
    static final class Polymorphic
    {
        private Polymorphic() {}

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
            StyleRule.of(JPanel.class,         Polymorphic::panel),
            StyleRule.of(AbstractButton.class, Polymorphic::button),
            StyleRule.of(JCheckBox.class,      Polymorphic::tickable),
            StyleRule.of(JRadioButton.class,   Polymorphic::tickable),
            StyleRule.of(JMenuItem.class,      Polymorphic::menuItem),
            StyleRule.of(JMenuBar.class,       Polymorphic::menuBar),
            StyleRule.of(JPopupMenu.class,     Polymorphic::popupMenu),
            StyleRule.of(JLabel.class,         Polymorphic::label),
            StyleRule.of(JTextField.class,     Polymorphic::field),
            StyleRule.of(JTextArea.class,      Polymorphic::page),
            StyleRule.of(JEditorPane.class,    Polymorphic::page),
            StyleRule.of(JSeparator.class,     Polymorphic::separator),
            StyleRule.of(JToolTip.class,       Polymorphic::toolTip),
            StyleRule.of(JProgressBar.class,   Polymorphic::progressBar),
            StyleRule.of(JSlider.class,        Polymorphic::bare),
            StyleRule.of(JScrollBar.class,     Polymorphic::scrollBar),
            StyleRule.of(JScrollPane.class,    Polymorphic::scrollPane),
            StyleRule.of(JViewport.class,      Polymorphic::bare),
            StyleRule.of(JComboBox.class,      Polymorphic::comboBox),
            StyleRule.of(JSpinner.class,       Polymorphic::spinner),
            StyleRule.of(JTabbedPane.class,    Polymorphic::bare),
            StyleRule.of(JList.class,          Polymorphic::bare),
            StyleRule.of(JTable.class,         Polymorphic::bare),
            StyleRule.of(JTableHeader.class,   Polymorphic::tableHeader),
            StyleRule.of(JTree.class,          Polymorphic::bare),
            StyleRule.of(JToolBar.class,       Polymorphic::toolBar),
            StyleRule.of(JSplitPane.class,     Polymorphic::bare)
        );

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
                if ( parent instanceof JComponent && SwingTreeLookAndFeel.Surface.of((JComponent) parent) == SwingTreeLookAndFeel.Surface.CARD )
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
            Theme theme, ComponentStyleDelegate<C> it, Color fill, int radius, int lift
        ) {
            SwingTreeLookAndFeel.Palette p   = theme.palette();
            int     off = Math.max(1, lift / 2);
            it = it.backgroundColor(fill).borderRadius(radius);
            switch ( Mood.of(p) ) {
                case RELIEF:
                    return it
                            .border(1, SwingTreeLookAndFeel.Palette.TRANSPARENT)
                            .shadow(LIT,  s -> s.color(LafUtilities.shadeBySteps(p.background(), LIGHT_STEP))
                                                .offset(-off, -off).blurRadius(lift)
                                                .falloff(UI.ShadowFalloff.GLOW).isInset(false))
                            .shadow(DROP, s -> s.color(LafUtilities.shadeBySteps(p.background(), -SHADE_STEP))
                                                .offset(off, off).blurRadius(lift)
                                                .falloff(UI.ShadowFalloff.BLUR).isInset(false));
                case LUMINOUS:
                    return it
                            .border(1, LafUtilities.shadeBySteps(fill, 26))
                            .shadow(DROP, s -> s.color(LafUtilities.withOpacity(Color.BLACK, 120))
                                                .offset(0, off).blurRadius(lift)
                                                .falloff(UI.ShadowFalloff.BLUR).isInset(false));
                case SHEET:
                default:
                    return it
                            .border(1, SwingTreeLookAndFeel.Palette.TRANSPARENT)
                            .shadow(DROP, s -> s.color(LafUtilities.withOpacity(p.text(), 46))
                                                .offset(0, off).blurRadius(lift + 1).spreadRadius(-1)
                                                .falloff(UI.ShadowFalloff.BLUR).isInset(false));
            }
        }

        /** The same three answers for a surface that has to read as something you reach into. */
        private static <C extends JComponent> ComponentStyleDelegate<C> recess(
            Theme theme, ComponentStyleDelegate<C> it, Color fill, int radius
        ) {
            SwingTreeLookAndFeel.Palette p = theme.palette();
            it = it.backgroundColor(fill).borderRadius(radius);
            if ( Mood.of(p) == Mood.RELIEF )
                return it
                        .border(1, SwingTreeLookAndFeel.Palette.TRANSPARENT)
                        .shadow(DROP, s -> s.color(LafUtilities.shadeBySteps(p.background(), -SHADE_STEP / 2))
                                            .offset(3, 3).blurRadius(6)
                                            .falloff(UI.ShadowFalloff.PENUMBRA).isInset(true))
                        .shadow(LIT,  s -> s.color(LafUtilities.shadeBySteps(p.background(), LIGHT_STEP / 2))
                                            .offset(-3, -3).blurRadius(6)
                                            .falloff(UI.ShadowFalloff.PENUMBRA).isInset(true));
            return it.border(1, p.border());
        }

        /**
         *  Rings a control that has the keyboard. It is a hard-edged shadow rather than a thicker
         *  border, so the ring grows outwards into the margin every control here already keeps and
         *  the label underneath it never moves.
         */
        private static <C extends JComponent> ComponentStyleDelegate<C> focused(
            ComponentStyleDelegate<C> it, SwingTreeLookAndFeel.Palette p, boolean focused
        ) {
            if ( !focused )
                return it;
            return it.shadow("focus", s -> s.color(LafUtilities.withOpacity(p.accent(), 190))
                                            .blurRadius(0).spreadRadius(2).isInset(false));
        }

        // ── Surfaces ─────────────────────────────────────────────────────────

        @SuppressWarnings("deprecation") // component() is the documented hook for LAF state reads
        private static ComponentStyleDelegate<JPanel> panel( Theme theme, ComponentStyleDelegate<JPanel> it ) {
            SwingTreeLookAndFeel.Palette p     = theme.palette();
            JPanel  panel = it.component();
            it = it.foregroundColor(p.text());
            switch ( SwingTreeLookAndFeel.Surface.of(panel) ) {
                case CARD: {
                    int lift = STEP + STEP * depthOf(panel);
                    return lift(theme, it.margin(lift).padding(2), p.surface(), MAX_RADIUS, lift);
                }
                case RAIL:        return it.backgroundColor(p.surface()).borderWidth(0);
                case TRANSPARENT: return it.backgroundColor(SwingTreeLookAndFeel.Palette.TRANSPARENT);
                case WINDOW:
                default:          return it.backgroundColor(
                                            LafUtilities.isControlInternal(panel) ? SwingTreeLookAndFeel.Palette.TRANSPARENT
                                                                                  : p.background());
            }
        }

        @SuppressWarnings("deprecation")
        private static ComponentStyleDelegate<JScrollPane> scrollPane( Theme theme, ComponentStyleDelegate<JScrollPane> it ) {
            SwingTreeLookAndFeel.Palette p    = theme.palette();
            JScrollPane pane = it.component();
            it = it.foregroundColor(p.text());
            switch ( SwingTreeLookAndFeel.Surface.of(pane) ) {
                case TRANSPARENT: return it.backgroundColor(SwingTreeLookAndFeel.Palette.TRANSPARENT).borderWidth(0).borderRadius(0).padding(0);
                case CARD: {
                    int lift = STEP + STEP * depthOf(pane);
                    return lift(theme, it.margin(lift).padding(3), p.surface(), MAX_RADIUS, lift);
                }
                case RAIL:        return it.backgroundColor(p.surface()).borderWidth(0).borderRadius(0).padding(0);
                case WINDOW:
                default:          return recess(theme, it.margin(3).padding(3), p.surfaceField(), MAX_RADIUS - 4);
            }
        }

        // ── Controls ─────────────────────────────────────────────────────────

        @SuppressWarnings("deprecation")
        private static ComponentStyleDelegate<AbstractButton> button( Theme theme, ComponentStyleDelegate<AbstractButton> it ) {
            SwingTreeLookAndFeel.Palette p = theme.palette();
            AbstractButton b = it.component();
            ButtonModel    m = b.getModel();

            boolean enabled  = b.isEnabled();
            boolean pressed  = enabled && m.isArmed() && m.isPressed();
            boolean sunken   = pressed || ( enabled && m.isSelected() );
            boolean rollover = enabled && m.isRollover() && !pressed;
            boolean focused  = enabled && b.isFocusOwner();

            SwingTreeLookAndFeel.Variant variant = SwingTreeLookAndFeel.Variant.of(b);
            int     radius  = radiusOf(b);
            Color   fill    = fill(variant, p, enabled, sunken, rollover);

            it = focused(it
                    .margin(4)
                    .padding(7, 16, 7, 16)
                    .borderRadius(radius)
                    .border(1, SwingTreeLookAndFeel.Palette.TRANSPARENT)
                    .backgroundColor(fill)
                    .foregroundColor(ink(variant, p, enabled)), p, focused);

            if ( !enabled || ( variant == SwingTreeLookAndFeel.Variant.QUIET && !sunken && !rollover ) )
                return it;
            if ( sunken )
                return recess(theme, it, fill, radius);
            return lift(theme, it, fill, radius, rollover ? STEP + 2 : STEP);
        }

        @SuppressWarnings("deprecation")
        private static <C extends AbstractButton> ComponentStyleDelegate<C> tickable( Theme theme, ComponentStyleDelegate<C> it ) {
            SwingTreeLookAndFeel.Palette p = theme.palette();
            return it
                    .backgroundColor(SwingTreeLookAndFeel.Palette.TRANSPARENT)
                    .foregroundColor(it.component().isEnabled() ? p.text() : p.textDisabled())
                    .padding(2, 4, 2, 4);
        }

        @SuppressWarnings("deprecation")
        private static ComponentStyleDelegate<JComboBox> comboBox( Theme theme, ComponentStyleDelegate<JComboBox> it ) {
            SwingTreeLookAndFeel.Palette p     = theme.palette();
            JComboBox<?> combo = it.component();
            boolean      on    = combo.isEnabled();
            it = it.margin(4).padding(6, 4, 6, 10)
                   .foregroundColor(on ? p.text() : p.textDisabled());
            Color fill = LafUtilities.underPointer(p, on ? p.surface() : p.surfaceDisabled(), combo);
            return lift(theme, it, fill, radiusOf(combo), STEP);
        }

        @SuppressWarnings("deprecation")
        private static ComponentStyleDelegate<JSpinner> spinner( Theme theme, ComponentStyleDelegate<JSpinner> it ) {
            SwingTreeLookAndFeel.Palette p       = theme.palette();
            JSpinner spinner = it.component();
            boolean  on      = spinner.isEnabled();
            it = it.margin(4).padding(4)
                   .foregroundColor(on ? p.text() : p.textDisabled());
            Color fill = on ? p.surface() : p.surfaceDisabled();
            return lift(theme, it.borderWidth(0), fill, radiusOf(spinner), STEP);
        }

        // ── Inputs ───────────────────────────────────────────────────────────

        @SuppressWarnings("deprecation")
        private static ComponentStyleDelegate<JTextField> field( Theme theme, ComponentStyleDelegate<JTextField> it ) {
            return input(theme, it, it.component(), 7, 12);
        }

        @SuppressWarnings("deprecation")
        private static <C extends JTextComponent> ComponentStyleDelegate<C> page( Theme theme, ComponentStyleDelegate<C> it ) {
            return input(theme, it, it.component(), 8, 12);
        }

        private static <C extends JComponent> ComponentStyleDelegate<C> input(
            Theme theme, ComponentStyleDelegate<C> it, JTextComponent text, int padY, int padX
        ) {
            SwingTreeLookAndFeel.Palette p        = theme.palette();
            boolean editable = text.isEnabled() && text.isEditable();
            // Inside a scroll pane or a picker, whatever that made of itself is the surface here.
            if ( LafUtilities.isInsideAnotherControl(text) )
                return it
                        .margin(0).padding(padY, padX, padY, padX).borderWidth(0)
                        .backgroundColor(SwingTreeLookAndFeel.Palette.TRANSPARENT)
                        .foregroundColor(text.isEnabled() ? p.text() : p.textDisabled());
            it = it.margin(4).padding(padY, padX, padY, padX)
                   .foregroundColor(text.isEnabled() ? p.text() : p.textDisabled());
            return focused(recess(theme, it, editable ? p.surfaceField() : p.surfaceDisabled(), radiusOf(text)),
                           p, editable && text.isFocusOwner());
        }

        // ── The rest ─────────────────────────────────────────────────────────

        @SuppressWarnings("deprecation")
        private static ComponentStyleDelegate<JLabel> label( Theme theme, ComponentStyleDelegate<JLabel> it ) {
            SwingTreeLookAndFeel.Palette p = theme.palette();
            return it.foregroundColor(it.component().isEnabled() ? p.text() : p.textDisabled());
        }

        @SuppressWarnings("deprecation")
        private static ComponentStyleDelegate<JMenuItem> menuItem( Theme theme, ComponentStyleDelegate<JMenuItem> it ) {
            SwingTreeLookAndFeel.Palette p       = theme.palette();
            JMenuItem   item    = it.component();
            ButtonModel m       = item.getModel();
            boolean     enabled = item.isEnabled();
            boolean     armed   = enabled && ( m.isArmed() || m.isSelected() );
            return it
                    .padding(6, 12, 6, 12)
                    .borderRadius(radiusOf(item))
                    .borderWidth(0)
                    .backgroundColor(armed ? p.accentSoft() : SwingTreeLookAndFeel.Palette.TRANSPARENT)
                    .foregroundColor(enabled ? p.text() : p.textDisabled());
        }

        private static ComponentStyleDelegate<JMenuBar> menuBar( Theme theme, ComponentStyleDelegate<JMenuBar> it ) {
            SwingTreeLookAndFeel.Palette p = theme.palette();
            return it.backgroundColor(p.background()).foregroundColor(p.text()).padding(2, 4, 2, 4).borderWidth(0);
        }

        @SuppressWarnings("deprecation")
        private static ComponentStyleDelegate<JPopupMenu> popupMenu( Theme theme, ComponentStyleDelegate<JPopupMenu> it ) {
            SwingTreeLookAndFeel.Palette p = theme.palette();
            return lift(theme, it.foregroundColor(p.text()).margin(5).padding(5, 0, 5, 0),
                        p.surface(), MAX_RADIUS - 4, STEP + 3);
        }

        @SuppressWarnings("deprecation")
        private static ComponentStyleDelegate<JToolTip> toolTip( Theme theme, ComponentStyleDelegate<JToolTip> it ) {
            SwingTreeLookAndFeel.Palette p = theme.palette();
            return lift(theme, it.foregroundColor(p.text()).margin(4).padding(5, 10, 5, 10),
                        p.surface(), radiusOf(it.component()), STEP + 2);
        }

        /** The delegate draws the hairline itself, so the rule leaves the rest of the strip alone. */
        private static ComponentStyleDelegate<JSeparator> separator( Theme theme, ComponentStyleDelegate<JSeparator> it ) {
            SwingTreeLookAndFeel.Palette p = theme.palette();
            return it.backgroundColor(SwingTreeLookAndFeel.Palette.TRANSPARENT).foregroundColor(p.borderSoft());
        }

        @SuppressWarnings("deprecation")
        private static ComponentStyleDelegate<JProgressBar> progressBar( Theme theme, ComponentStyleDelegate<JProgressBar> it ) {
            SwingTreeLookAndFeel.Palette p   = theme.palette();
            JProgressBar bar = it.component();
            return recess(theme, it.margin(2).foregroundColor(p.accent()),
                          Mood.of(p) == Mood.RELIEF ? p.background() : p.accentSoft(), radiusOf(bar));
        }

        private static ComponentStyleDelegate<JScrollBar> scrollBar( Theme theme, ComponentStyleDelegate<JScrollBar> it ) {
            SwingTreeLookAndFeel.Palette p = theme.palette();
            return it.backgroundColor(p.background()).foregroundColor(p.border());
        }

        private static ComponentStyleDelegate<JTableHeader> tableHeader( Theme theme, ComponentStyleDelegate<JTableHeader> it ) {
            SwingTreeLookAndFeel.Palette p = theme.palette();
            return it
                    .backgroundColor(SwingTreeLookAndFeel.Palette.TRANSPARENT)
                    .foregroundColor(p.textMuted())
                    .borderAt(UI.Edge.BOTTOM, 1, p.borderSoft());
        }

        @SuppressWarnings("deprecation")
        private static ComponentStyleDelegate<JToolBar> toolBar( Theme theme, ComponentStyleDelegate<JToolBar> it ) {
            SwingTreeLookAndFeel.Palette p   = theme.palette();
            JToolBar  bar = it.component();
            return lift(theme, it.foregroundColor(p.text()).margin(5).padding(4, 8, 4, 8),
                        p.surface(), MAX_RADIUS, STEP + STEP * depthOf(bar));
        }

        /** Everything that is only the contents of a surface somebody else already made. */
        private static <C extends JComponent> ComponentStyleDelegate<C> bare( Theme theme, ComponentStyleDelegate<C> it ) {
            return it.backgroundColor(SwingTreeLookAndFeel.Palette.TRANSPARENT).foregroundColor(theme.palette().text());
        }

        // ── Variant colours ──────────────────────────────────────────────────

        private static Color fill(SwingTreeLookAndFeel.Variant variant, SwingTreeLookAndFeel.Palette p, boolean enabled, boolean sunken, boolean rollover ) {
            if ( !enabled )
                return variant == SwingTreeLookAndFeel.Variant.QUIET ? SwingTreeLookAndFeel.Palette.TRANSPARENT : p.surfaceDisabled();
            switch ( variant ) {
                case PRIMARY: return sunken ? p.primaryPressed() : rollover ? p.primaryHover() : p.primary();
                case DANGER:  return sunken ? p.dangerPressed()  : rollover ? p.dangerHover()  : p.danger();
                case QUIET:   return sunken || rollover ? p.surface() : SwingTreeLookAndFeel.Palette.TRANSPARENT;
                case NEUTRAL:
                default:      return sunken ? p.surfacePressed() : rollover ? p.surfaceHover() : p.surface();
            }
        }

        private static Color ink(SwingTreeLookAndFeel.Variant variant, SwingTreeLookAndFeel.Palette p, boolean enabled ) {
            if ( !enabled )
                return p.textDisabled();
            return variant.isFilled() ? p.onFilled() : p.text();
        }
    }
}
