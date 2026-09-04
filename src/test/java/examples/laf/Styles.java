package examples.laf;

import sprouts.Tuple;
import swingtree.UI;
import swingtree.api.Painter;
import swingtree.api.Styler;
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
 *  No rule anywhere here names a colour. Every one reads {@link SwingTreeLookAndFeel#palette()}
 *  while it runs, which is on every paint, so pairing a preset with a palette it was not designed
 *  against re-tints the whole preset instead of half of it.
 */
final class Styles
{
    private Styles() {}


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

        private static <C extends JComponent> StyleRule rule( Class<C> type, Styler<C> styler ) {
            return new StyleRule(type, styler);
        }

        // ── Surfaces ─────────────────────────────────────────────────────────

        /**
         *  A panel is the ground the application stands on, or one of the cards standing on it,
         *  depending on the {@link SwingTreeLookAndFeel.Surface} it was tagged with. Only the fill and the grain are
         *  decided here; padding, spacing and per-edge accents stay free for the application.
         */
        @SuppressWarnings("deprecation") // component() is the documented hook for LAF state reads
        private static ComponentStyleDelegate<JPanel> panel(ComponentStyleDelegate<JPanel> it ) {
            SwingTreeLookAndFeel.Palette p = SwingTreeLookAndFeel.palette();
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
        private static ComponentStyleDelegate<JScrollPane> scrollPane( ComponentStyleDelegate<JScrollPane> it ) {
            SwingTreeLookAndFeel.Palette p = SwingTreeLookAndFeel.palette();
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
        private static ComponentStyleDelegate<JViewport> viewport( ComponentStyleDelegate<JViewport> it ) {
            SwingTreeLookAndFeel.Palette p = SwingTreeLookAndFeel.palette();
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
        private static ComponentStyleDelegate<AbstractButton> button( ComponentStyleDelegate<AbstractButton> it ) {
            SwingTreeLookAndFeel.Palette p = SwingTreeLookAndFeel.palette();
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
        private static <C extends AbstractButton> ComponentStyleDelegate<C> tickable( ComponentStyleDelegate<C> it ) {
            SwingTreeLookAndFeel.Palette p = SwingTreeLookAndFeel.palette();
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
        private static ComponentStyleDelegate<JMenuItem> menuItem( ComponentStyleDelegate<JMenuItem> it ) {
            SwingTreeLookAndFeel.Palette p       = SwingTreeLookAndFeel.palette();
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

        private static ComponentStyleDelegate<JMenuBar> menuBar( ComponentStyleDelegate<JMenuBar> it ) {
            SwingTreeLookAndFeel.Palette p = SwingTreeLookAndFeel.palette();
            return it
                    .backgroundColor(p.surface())
                    .foregroundColor(p.text())
                    .padding(2, 4, 2, 4)
                    .borderAt(UI.Edge.BOTTOM, 1, p.borderSoft());
        }

        private static ComponentStyleDelegate<JPopupMenu> popupMenu( ComponentStyleDelegate<JPopupMenu> it ) {
            SwingTreeLookAndFeel.Palette p = SwingTreeLookAndFeel.palette();
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
            SwingTreeLookAndFeel.Palette p = SwingTreeLookAndFeel.palette();
            return it.foregroundColor(it.component().isEnabled() ? p.text() : p.textDisabled());
        }

        /**
         *  A single-line input: a rounded field with a border that grows into the accent on focus,
         *  under a faint accent-tinted glow that marks the active field without being noisy.
         */
        @SuppressWarnings("deprecation")
        private static ComponentStyleDelegate<JTextField> field( ComponentStyleDelegate<JTextField> it ) {
            SwingTreeLookAndFeel.Palette p = SwingTreeLookAndFeel.palette();
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
        private static <C extends JTextComponent> ComponentStyleDelegate<C> page(ComponentStyleDelegate<C> it ) {
            return textSurface(it, SwingTreeLookAndFeel.palette(), it.component(), 6, 9);
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

        private static ComponentStyleDelegate<JToolTip> toolTip( ComponentStyleDelegate<JToolTip> it ) {
            SwingTreeLookAndFeel.Palette p = SwingTreeLookAndFeel.palette();
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
            SwingTreeLookAndFeel.Palette p       = SwingTreeLookAndFeel.palette();
            JComboBox<?> combo   = it.component();
            boolean      enabled = combo.isEnabled();
            boolean      focused = enabled && LafUtilities.hasFocus(combo);

            return it
                    .margin(focused ? 0 : 1)
                    .padding(4, 8, 4, 4)
                    .borderRadius(7)
                    .borderWidth(focused ? 2 : 1)
                    .borderColor(focused ? p.accent() : p.border())
                    .backgroundColor(enabled ? p.surfaceField() : p.surfaceDisabled())
                    .foregroundColor(enabled ? p.text() : p.textDisabled());
        }

        @SuppressWarnings("deprecation")
        private static ComponentStyleDelegate<JSpinner> spinner( ComponentStyleDelegate<JSpinner> it ) {
            SwingTreeLookAndFeel.Palette p       = SwingTreeLookAndFeel.palette();
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

        private static ComponentStyleDelegate<JSlider> slider( ComponentStyleDelegate<JSlider> it ) {
            SwingTreeLookAndFeel.Palette p = SwingTreeLookAndFeel.palette();
            return it.backgroundColor(p.background()).foregroundColor(p.text());
        }

        /** The trough of a progress bar; the {@linkplain Symbols symbol set} fills it. */
        private static ComponentStyleDelegate<JProgressBar> progressBar( ComponentStyleDelegate<JProgressBar> it ) {
            SwingTreeLookAndFeel.Palette p = SwingTreeLookAndFeel.palette();
            return it
                    .borderRadius(7)
                    .borderWidth(1)
                    .borderColor(p.border())
                    .backgroundColor(p.surfaceDisabled())
                    .foregroundColor(p.accent());
        }

        // ── Structure ────────────────────────────────────────────────────────

        private static ComponentStyleDelegate<JSeparator> separator( ComponentStyleDelegate<JSeparator> it ) {
            SwingTreeLookAndFeel.Palette p = SwingTreeLookAndFeel.palette();
            return it.backgroundColor(p.borderSoft()).foregroundColor(p.borderSoft());
        }

        /**
         *  A scroll bar is nothing but its groove and its thumb, and the groove is this background:
         *  the symbol set draws the thumb on top of it. Painting the groove here rather than as a
         *  symbol keeps it a flat fill the render cache can blit, instead of a rounded rectangle the
         *  rasterizer has to antialias the height of the window on every frame.
         */
        private static ComponentStyleDelegate<JScrollBar> scrollBar( ComponentStyleDelegate<JScrollBar> it ) {
            SwingTreeLookAndFeel.Palette p = SwingTreeLookAndFeel.palette();
            return it.backgroundColor(p.surfaceDisabled()).foregroundColor(p.border());
        }

        private static ComponentStyleDelegate<JTabbedPane> tabbedPane( ComponentStyleDelegate<JTabbedPane> it ) {
            SwingTreeLookAndFeel.Palette p = SwingTreeLookAndFeel.palette();
            return it.backgroundColor(p.background()).foregroundColor(p.text());
        }

        private static ComponentStyleDelegate<JSplitPane> splitPane( ComponentStyleDelegate<JSplitPane> it ) {
            SwingTreeLookAndFeel.Palette p = SwingTreeLookAndFeel.palette();
            return it.backgroundColor(p.background()).foregroundColor(p.text());
        }

        @SuppressWarnings("deprecation")
        private static ComponentStyleDelegate<JToolBar> toolBar( ComponentStyleDelegate<JToolBar> it ) {
            SwingTreeLookAndFeel.Palette p = SwingTreeLookAndFeel.palette();
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
            SwingTreeLookAndFeel.Palette p = SwingTreeLookAndFeel.palette();
            return it.backgroundColor(p.surfaceField()).foregroundColor(p.text());
        }

        private static ComponentStyleDelegate<JTable> table( ComponentStyleDelegate<JTable> it ) {
            SwingTreeLookAndFeel.Palette p = SwingTreeLookAndFeel.palette();
            return it.backgroundColor(p.surfaceField()).foregroundColor(p.text());
        }

        private static ComponentStyleDelegate<JTableHeader> tableHeader( ComponentStyleDelegate<JTableHeader> it ) {
            SwingTreeLookAndFeel.Palette p = SwingTreeLookAndFeel.palette();
            return it
                    .backgroundColor(p.surface())
                    .foregroundColor(p.textMuted())
                    .borderAt(UI.Edge.BOTTOM, 1, p.borderSoft());
        }

        private static ComponentStyleDelegate<JTree> tree( ComponentStyleDelegate<JTree> it ) {
            SwingTreeLookAndFeel.Palette p = SwingTreeLookAndFeel.palette();
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

        /**
         *  Draws the handle a floatable tool bar is dragged by, on the tool bar's content layer. It
         *  is a named painter rather than a lambda because a style rule runs on every paint, and a
         *  capturing lambda is a new object each time, which would tell the style engine the tool
         *  bar's style had changed. Two of these compare equal whenever they would draw the same
         *  thing.
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
         *  {@link UI.ShadowType#BLUR}, the profile a hard edge takes when it is convolved with a
         *  blur, so it leaves the shape at full strength and reaches nothing with no slope at either
         *  end; the surface then swells out of the panel instead of stepping up out of it. The
         *  highlight uses the bell of {@link UI.ShadowType#GLOW}, because a sheen is light bleeding
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
                ComponentStyleDelegate<C> it, SwingTreeLookAndFeel.Palette p, int depth
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
            SwingTreeLookAndFeel.Palette p = SwingTreeLookAndFeel.palette();
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
        private static ComponentStyleDelegate<JScrollPane> scrollPane( ComponentStyleDelegate<JScrollPane> it ) {
            SwingTreeLookAndFeel.Palette p = SwingTreeLookAndFeel.palette();
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
        private static ComponentStyleDelegate<JViewport> viewport( ComponentStyleDelegate<JViewport> it ) {
            return it.backgroundColor(SwingTreeLookAndFeel.Palette.TRANSPARENT).foregroundColor(SwingTreeLookAndFeel.palette().text());
        }

        // ── Controls ─────────────────────────────────────────────────────────

        @SuppressWarnings("deprecation")
        private static ComponentStyleDelegate<AbstractButton> button( ComponentStyleDelegate<AbstractButton> it ) {
            SwingTreeLookAndFeel.Palette p = SwingTreeLookAndFeel.palette();
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
        private static <C extends AbstractButton> ComponentStyleDelegate<C> tickable( ComponentStyleDelegate<C> it ) {
            SwingTreeLookAndFeel.Palette p = SwingTreeLookAndFeel.palette();
            return it
                    .backgroundColor(SwingTreeLookAndFeel.Palette.TRANSPARENT)
                    .foregroundColor(it.component().isEnabled() ? p.text() : p.textDisabled())
                    .padding(3, 5, 3, 5);
        }

        @SuppressWarnings("deprecation")
        private static ComponentStyleDelegate<JComboBox> comboBox( ComponentStyleDelegate<JComboBox> it ) {
            SwingTreeLookAndFeel.Palette p       = SwingTreeLookAndFeel.palette();
            JComboBox<?> combo   = it.component();
            boolean      enabled = combo.isEnabled();
            boolean      focused = enabled && LafUtilities.hasFocus(combo);
            int lift = focused ? 4 : 6;
            it = it
                    .margin(lift)
                    .padding(6, 10, 6, 6)
                    .borderRadius(14)
                    .borderWidth(focused ? 2 : 0)
                    .borderColor(focused ? p.accent() : SwingTreeLookAndFeel.Palette.TRANSPARENT)
                    .backgroundColor(enabled ? p.surface() : p.surfaceDisabled())
                    .foregroundColor(enabled ? p.text() : p.textDisabled());
            if ( !enabled )
                return it;
            return raised(curved(it, p.surface(), false), p, lift);
        }

        @SuppressWarnings("deprecation")
        private static ComponentStyleDelegate<JSpinner> spinner( ComponentStyleDelegate<JSpinner> it ) {
            SwingTreeLookAndFeel.Palette p       = SwingTreeLookAndFeel.palette();
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
            SwingTreeLookAndFeel.Palette p        = SwingTreeLookAndFeel.palette();
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
        private static ComponentStyleDelegate<JLabel> label( ComponentStyleDelegate<JLabel> it ) {
            SwingTreeLookAndFeel.Palette p = SwingTreeLookAndFeel.palette();
            return it.foregroundColor(it.component().isEnabled() ? p.text() : p.textDisabled());
        }

        @SuppressWarnings("deprecation")
        private static ComponentStyleDelegate<JMenuItem> menuItem( ComponentStyleDelegate<JMenuItem> it ) {
            SwingTreeLookAndFeel.Palette p       = SwingTreeLookAndFeel.palette();
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

        private static ComponentStyleDelegate<JMenuBar> menuBar( ComponentStyleDelegate<JMenuBar> it ) {
            SwingTreeLookAndFeel.Palette p = SwingTreeLookAndFeel.palette();
            return it.backgroundColor(p.background()).foregroundColor(p.text()).padding(3, 6, 3, 6).borderWidth(0);
        }

        private static ComponentStyleDelegate<JPopupMenu> popupMenu( ComponentStyleDelegate<JPopupMenu> it ) {
            SwingTreeLookAndFeel.Palette p = SwingTreeLookAndFeel.palette();
            return raised(curved(it
                    .backgroundColor(p.surface())
                    .foregroundColor(p.text())
                    .margin(6)
                    .padding(6)
                    .borderRadius(18)
                    .borderWidth(0), p.surface(), false), p, 6);
        }

        private static ComponentStyleDelegate<JToolTip> toolTip( ComponentStyleDelegate<JToolTip> it ) {
            SwingTreeLookAndFeel.Palette p = SwingTreeLookAndFeel.palette();
            return raised(curved(it
                    .margin(5)
                    .padding(5, 12, 5, 12)
                    .borderRadius(14)
                    .borderWidth(0)
                    .backgroundColor(p.surface())
                    .foregroundColor(p.text()), p.surface(), false), p, 5);
        }

        private static ComponentStyleDelegate<JSeparator> separator( ComponentStyleDelegate<JSeparator> it ) {
            SwingTreeLookAndFeel.Palette p = SwingTreeLookAndFeel.palette();
            return it.backgroundColor(p.borderSoft()).foregroundColor(p.borderSoft());
        }

        /** The trough is a groove pressed into the panel; the symbol set fills it. */
        private static ComponentStyleDelegate<JProgressBar> progressBar( ComponentStyleDelegate<JProgressBar> it ) {
            SwingTreeLookAndFeel.Palette p = SwingTreeLookAndFeel.palette();
            return sunken(it
                    .margin(3)
                    .borderRadius(8)
                    .borderWidth(0)
                    .backgroundColor(p.background())
                    .foregroundColor(p.accent()), p, 3);
        }

        private static ComponentStyleDelegate<JSlider> slider( ComponentStyleDelegate<JSlider> it ) {
            SwingTreeLookAndFeel.Palette p = SwingTreeLookAndFeel.palette();
            return it.backgroundColor(SwingTreeLookAndFeel.Palette.TRANSPARENT).foregroundColor(p.text());
        }

        private static ComponentStyleDelegate<JScrollBar> scrollBar( ComponentStyleDelegate<JScrollBar> it ) {
            SwingTreeLookAndFeel.Palette p = SwingTreeLookAndFeel.palette();
            return it.backgroundColor(p.background()).foregroundColor(p.border());
        }

        private static ComponentStyleDelegate<JTabbedPane> tabbedPane( ComponentStyleDelegate<JTabbedPane> it ) {
            SwingTreeLookAndFeel.Palette p = SwingTreeLookAndFeel.palette();
            return it.backgroundColor(p.background()).foregroundColor(p.text());
        }

        private static ComponentStyleDelegate<JSplitPane> splitPane( ComponentStyleDelegate<JSplitPane> it ) {
            SwingTreeLookAndFeel.Palette p = SwingTreeLookAndFeel.palette();
            return it.backgroundColor(SwingTreeLookAndFeel.Palette.TRANSPARENT).foregroundColor(p.text());
        }

        /**
         *  A list, table or tree is the content of a hole, not a surface of its own: it lets the
         *  scroll pane's field colour and groove through and paints only its rows.
         */
        private static <C extends JComponent> ComponentStyleDelegate<C> flatField( ComponentStyleDelegate<C> it ) {
            return it.backgroundColor(SwingTreeLookAndFeel.Palette.TRANSPARENT).foregroundColor(SwingTreeLookAndFeel.palette().text());
        }

        private static ComponentStyleDelegate<JTableHeader> tableHeader( ComponentStyleDelegate<JTableHeader> it ) {
            SwingTreeLookAndFeel.Palette p = SwingTreeLookAndFeel.palette();
            return it.backgroundColor(p.surface()).foregroundColor(p.textMuted());
        }

        private static ComponentStyleDelegate<JToolBar> toolBar( ComponentStyleDelegate<JToolBar> it ) {
            SwingTreeLookAndFeel.Palette p = SwingTreeLookAndFeel.palette();
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
        private static ComponentStyleDelegate<JPanel> panel( ComponentStyleDelegate<JPanel> it ) {
            SwingTreeLookAndFeel.Palette p = SwingTreeLookAndFeel.palette();
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
        private static ComponentStyleDelegate<JScrollPane> scrollPane( ComponentStyleDelegate<JScrollPane> it ) {
            SwingTreeLookAndFeel.Palette p = SwingTreeLookAndFeel.palette();
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
        private static ComponentStyleDelegate<JViewport> viewport( ComponentStyleDelegate<JViewport> it ) {
            SwingTreeLookAndFeel.Palette p = SwingTreeLookAndFeel.palette();
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
        private static ComponentStyleDelegate<AbstractButton> button( ComponentStyleDelegate<AbstractButton> it ) {
            SwingTreeLookAndFeel.Palette p = SwingTreeLookAndFeel.palette();
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
        private static <C extends AbstractButton> ComponentStyleDelegate<C> tickable( ComponentStyleDelegate<C> it ) {
            SwingTreeLookAndFeel.Palette p = SwingTreeLookAndFeel.palette();
            return it
                    .backgroundColor(SwingTreeLookAndFeel.Palette.TRANSPARENT)
                    .foregroundColor(it.component().isEnabled() ? p.text() : p.textDisabled())
                    .padding(2, 4, 2, 4);
        }

        @SuppressWarnings("deprecation")
        private static ComponentStyleDelegate<JComboBox> comboBox( ComponentStyleDelegate<JComboBox> it ) {
            SwingTreeLookAndFeel.Palette p       = SwingTreeLookAndFeel.palette();
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
            SwingTreeLookAndFeel.Palette p       = SwingTreeLookAndFeel.palette();
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
            SwingTreeLookAndFeel.Palette p        = SwingTreeLookAndFeel.palette();
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
            SwingTreeLookAndFeel.Palette p = SwingTreeLookAndFeel.palette();
            return it.foregroundColor(it.component().isEnabled() ? p.text() : p.textDisabled());
        }

        @SuppressWarnings("deprecation")
        private static ComponentStyleDelegate<JMenuItem> menuItem( ComponentStyleDelegate<JMenuItem> it ) {
            SwingTreeLookAndFeel.Palette p       = SwingTreeLookAndFeel.palette();
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

        private static ComponentStyleDelegate<JMenuBar> menuBar( ComponentStyleDelegate<JMenuBar> it ) {
            SwingTreeLookAndFeel.Palette p = SwingTreeLookAndFeel.palette();
            return it
                    .backgroundColor(p.surface())
                    .foregroundColor(p.text())
                    .padding(2, 4, 2, 4)
                    .gradient(g -> gloss(g, p.surface()))
                    .borderAt(UI.Edge.BOTTOM, 1, p.border());
        }

        private static ComponentStyleDelegate<JPopupMenu> popupMenu( ComponentStyleDelegate<JPopupMenu> it ) {
            SwingTreeLookAndFeel.Palette p = SwingTreeLookAndFeel.palette();
            return lifted(it
                    .backgroundColor(p.surfaceField())
                    .foregroundColor(p.text())
                    .margin(3)
                    .padding(4)
                    .borderRadius(10)
                    .border(1, p.border()), 12, 90);
        }

        private static ComponentStyleDelegate<JToolTip> toolTip( ComponentStyleDelegate<JToolTip> it ) {
            SwingTreeLookAndFeel.Palette p = SwingTreeLookAndFeel.palette();
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
            SwingTreeLookAndFeel.Palette p = SwingTreeLookAndFeel.palette();
            return it.backgroundColor(p.borderSoft()).foregroundColor(p.borderSoft());
        }

        private static ComponentStyleDelegate<JProgressBar> progressBar( ComponentStyleDelegate<JProgressBar> it ) {
            SwingTreeLookAndFeel.Palette p = SwingTreeLookAndFeel.palette();
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
            SwingTreeLookAndFeel.Palette p = SwingTreeLookAndFeel.palette();
            return it.backgroundColor(SwingTreeLookAndFeel.Palette.TRANSPARENT).foregroundColor(p.text());
        }

        private static ComponentStyleDelegate<JScrollBar> scrollBar( ComponentStyleDelegate<JScrollBar> it ) {
            SwingTreeLookAndFeel.Palette p = SwingTreeLookAndFeel.palette();
            return it.backgroundColor(p.surfaceDisabled()).foregroundColor(p.border());
        }

        private static ComponentStyleDelegate<JTabbedPane> tabbedPane( ComponentStyleDelegate<JTabbedPane> it ) {
            SwingTreeLookAndFeel.Palette p = SwingTreeLookAndFeel.palette();
            return it.backgroundColor(p.background()).foregroundColor(p.text());
        }

        private static ComponentStyleDelegate<JSplitPane> splitPane( ComponentStyleDelegate<JSplitPane> it ) {
            SwingTreeLookAndFeel.Palette p = SwingTreeLookAndFeel.palette();
            return it.backgroundColor(SwingTreeLookAndFeel.Palette.TRANSPARENT).foregroundColor(p.text());
        }

        private static <C extends JComponent> ComponentStyleDelegate<C> flatField( ComponentStyleDelegate<C> it ) {
            SwingTreeLookAndFeel.Palette p = SwingTreeLookAndFeel.palette();
            return it.backgroundColor(p.surfaceField()).foregroundColor(p.text());
        }

        private static ComponentStyleDelegate<JTableHeader> tableHeader( ComponentStyleDelegate<JTableHeader> it ) {
            SwingTreeLookAndFeel.Palette p = SwingTreeLookAndFeel.palette();
            return it
                    .backgroundColor(p.surface())
                    .foregroundColor(p.textMuted())
                    .gradient(g -> gloss(g, p.surface()))
                    .borderAt(UI.Edge.BOTTOM, 1, p.border());
        }

        private static ComponentStyleDelegate<JToolBar> toolBar( ComponentStyleDelegate<JToolBar> it ) {
            SwingTreeLookAndFeel.Palette p = SwingTreeLookAndFeel.palette();
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
        private static ComponentStyleDelegate<JPanel> panel( ComponentStyleDelegate<JPanel> it ) {
            SwingTreeLookAndFeel.Palette p = SwingTreeLookAndFeel.palette();
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
        private static ComponentStyleDelegate<JScrollPane> scrollPane( ComponentStyleDelegate<JScrollPane> it ) {
            SwingTreeLookAndFeel.Palette p = SwingTreeLookAndFeel.palette();
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
        private static ComponentStyleDelegate<JViewport> viewport( ComponentStyleDelegate<JViewport> it ) {
            SwingTreeLookAndFeel.Palette p = SwingTreeLookAndFeel.palette();
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
        private static ComponentStyleDelegate<AbstractButton> button( ComponentStyleDelegate<AbstractButton> it ) {
            SwingTreeLookAndFeel.Palette p = SwingTreeLookAndFeel.palette();
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

            it = it
                    .margin(focused ? 1 : 2)
                    .padding(8, 16, 8, 16)
                    .borderRadius(RADIUS)
                    .borderWidth(focused ? 2 : ( contained || variant == SwingTreeLookAndFeel.Variant.QUIET ? 0 : 1 ))
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
            SwingTreeLookAndFeel.Palette p = SwingTreeLookAndFeel.palette();
            return it
                    .backgroundColor(SwingTreeLookAndFeel.Palette.TRANSPARENT)
                    .foregroundColor(it.component().isEnabled() ? p.text() : p.textDisabled())
                    .padding(2, 4, 2, 4);
        }

        @SuppressWarnings("deprecation")
        private static ComponentStyleDelegate<JComboBox> comboBox( ComponentStyleDelegate<JComboBox> it ) {
            SwingTreeLookAndFeel.Palette p       = SwingTreeLookAndFeel.palette();
            JComboBox<?> combo   = it.component();
            boolean      enabled = combo.isEnabled();
            boolean      focused = enabled && LafUtilities.hasFocus(combo);
            return underlined(it, p, enabled, focused, 6, 10, 4);
        }

        @SuppressWarnings("deprecation")
        private static ComponentStyleDelegate<JSpinner> spinner( ComponentStyleDelegate<JSpinner> it ) {
            SwingTreeLookAndFeel.Palette p       = SwingTreeLookAndFeel.palette();
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
                ComponentStyleDelegate<C> it, SwingTreeLookAndFeel.Palette p, boolean enabled, boolean focused, int padY, int padX, int padRight
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
            SwingTreeLookAndFeel.Palette p = SwingTreeLookAndFeel.palette();
            return it.foregroundColor(it.component().isEnabled() ? p.text() : p.textDisabled());
        }

        @SuppressWarnings("deprecation")
        private static ComponentStyleDelegate<JMenuItem> menuItem( ComponentStyleDelegate<JMenuItem> it ) {
            SwingTreeLookAndFeel.Palette p       = SwingTreeLookAndFeel.palette();
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

        private static ComponentStyleDelegate<JMenuBar> menuBar( ComponentStyleDelegate<JMenuBar> it ) {
            SwingTreeLookAndFeel.Palette p = SwingTreeLookAndFeel.palette();
            return elevation(it
                    .backgroundColor(p.surface())
                    .foregroundColor(p.text())
                    .padding(2, 4, 2, 4)
                    .borderWidth(0), 1);
        }

        private static ComponentStyleDelegate<JPopupMenu> popupMenu( ComponentStyleDelegate<JPopupMenu> it ) {
            SwingTreeLookAndFeel.Palette p = SwingTreeLookAndFeel.palette();
            return elevation(it
                    .backgroundColor(p.surface())
                    .foregroundColor(p.text())
                    .margin(4)
                    .padding(4, 0, 4, 0)
                    .borderRadius(RADIUS)
                    .borderWidth(0), 4);
        }

        private static ComponentStyleDelegate<JToolTip> toolTip( ComponentStyleDelegate<JToolTip> it ) {
            SwingTreeLookAndFeel.Palette p = SwingTreeLookAndFeel.palette();
            return it
                    .padding(6, 10, 6, 10)
                    .borderRadius(RADIUS)
                    .borderWidth(0)
                    .backgroundColor(LafUtilities.withOpacity(p.text(), 229))
                    .foregroundColor(p.onFilled());
        }

        private static ComponentStyleDelegate<JSeparator> separator( ComponentStyleDelegate<JSeparator> it ) {
            SwingTreeLookAndFeel.Palette p = SwingTreeLookAndFeel.palette();
            return it.backgroundColor(p.borderSoft()).foregroundColor(p.borderSoft());
        }

        private static ComponentStyleDelegate<JProgressBar> progressBar( ComponentStyleDelegate<JProgressBar> it ) {
            SwingTreeLookAndFeel.Palette p = SwingTreeLookAndFeel.palette();
            return it
                    .borderRadius(3)
                    .borderWidth(0)
                    .backgroundColor(p.accentSoft())
                    .foregroundColor(p.accent());
        }

        private static ComponentStyleDelegate<JSlider> slider( ComponentStyleDelegate<JSlider> it ) {
            SwingTreeLookAndFeel.Palette p = SwingTreeLookAndFeel.palette();
            return it.backgroundColor(SwingTreeLookAndFeel.Palette.TRANSPARENT).foregroundColor(p.text());
        }

        private static ComponentStyleDelegate<JScrollBar> scrollBar( ComponentStyleDelegate<JScrollBar> it ) {
            SwingTreeLookAndFeel.Palette p = SwingTreeLookAndFeel.palette();
            return it.backgroundColor(p.background()).foregroundColor(p.border());
        }

        private static ComponentStyleDelegate<JTabbedPane> tabbedPane( ComponentStyleDelegate<JTabbedPane> it ) {
            SwingTreeLookAndFeel.Palette p = SwingTreeLookAndFeel.palette();
            return it.backgroundColor(p.background()).foregroundColor(p.text());
        }

        private static ComponentStyleDelegate<JSplitPane> splitPane( ComponentStyleDelegate<JSplitPane> it ) {
            SwingTreeLookAndFeel.Palette p = SwingTreeLookAndFeel.palette();
            return it.backgroundColor(SwingTreeLookAndFeel.Palette.TRANSPARENT).foregroundColor(p.text());
        }

        private static <C extends JComponent> ComponentStyleDelegate<C> flatField( ComponentStyleDelegate<C> it ) {
            SwingTreeLookAndFeel.Palette p = SwingTreeLookAndFeel.palette();
            return it.backgroundColor(p.surface()).foregroundColor(p.text());
        }

        private static ComponentStyleDelegate<JTableHeader> tableHeader( ComponentStyleDelegate<JTableHeader> it ) {
            SwingTreeLookAndFeel.Palette p = SwingTreeLookAndFeel.palette();
            return it
                    .backgroundColor(p.surface())
                    .foregroundColor(p.textMuted())
                    .borderAt(UI.Edge.BOTTOM, 1, p.borderSoft());
        }

        private static ComponentStyleDelegate<JToolBar> toolBar( ComponentStyleDelegate<JToolBar> it ) {
            SwingTreeLookAndFeel.Palette p = SwingTreeLookAndFeel.palette();
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
        private static ComponentStyleDelegate<JPanel> panel( ComponentStyleDelegate<JPanel> it ) {
            SwingTreeLookAndFeel.Palette p = SwingTreeLookAndFeel.palette();
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
        private static ComponentStyleDelegate<JScrollPane> scrollPane( ComponentStyleDelegate<JScrollPane> it ) {
            SwingTreeLookAndFeel.Palette p = SwingTreeLookAndFeel.palette();
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
        private static ComponentStyleDelegate<JViewport> viewport( ComponentStyleDelegate<JViewport> it ) {
            SwingTreeLookAndFeel.Palette p = SwingTreeLookAndFeel.palette();
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
        private static ComponentStyleDelegate<AbstractButton> button( ComponentStyleDelegate<AbstractButton> it ) {
            SwingTreeLookAndFeel.Palette p = SwingTreeLookAndFeel.palette();
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
                    .padding(focused ? 7 : 9, focused ? 16 : 18, focused ? 7 : 9, focused ? 16 : 18)
                    .borderRadius(0)
                    .borderWidth(focused ? 2 : 0)
                    .borderColor(focused ? p.accent() : SwingTreeLookAndFeel.Palette.TRANSPARENT)
                    .backgroundColor(fill(variant, p, enabled, sunken, rollover))
                    .foregroundColor(ink(variant, p, enabled, sunken));
        }

        @SuppressWarnings("deprecation")
        private static <C extends AbstractButton> ComponentStyleDelegate<C> tickable( ComponentStyleDelegate<C> it ) {
            SwingTreeLookAndFeel.Palette p = SwingTreeLookAndFeel.palette();
            return it
                    .backgroundColor(SwingTreeLookAndFeel.Palette.TRANSPARENT)
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
            SwingTreeLookAndFeel.Palette p        = SwingTreeLookAndFeel.palette();
            boolean editable = text.isEnabled() && text.isEditable();
            // Inside a scroll pane or a spinner the box has already been drawn around it.
            if ( LafUtilities.isInsideAnotherControl(text) )
                return it
                        .margin(0).padding(padY, padX, padY, padX).borderWidth(0)
                        .backgroundColor(SwingTreeLookAndFeel.Palette.TRANSPARENT)
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
            SwingTreeLookAndFeel.Palette p = SwingTreeLookAndFeel.palette();
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
            SwingTreeLookAndFeel.Palette p = SwingTreeLookAndFeel.palette();
            return it.foregroundColor(it.component().isEnabled() ? p.text() : p.textDisabled());
        }

        @SuppressWarnings("deprecation")
        private static ComponentStyleDelegate<JMenuItem> menuItem( ComponentStyleDelegate<JMenuItem> it ) {
            SwingTreeLookAndFeel.Palette p       = SwingTreeLookAndFeel.palette();
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

        private static ComponentStyleDelegate<JMenuBar> menuBar( ComponentStyleDelegate<JMenuBar> it ) {
            SwingTreeLookAndFeel.Palette p = SwingTreeLookAndFeel.palette();
            return it
                    .backgroundColor(p.surface())
                    .foregroundColor(p.text())
                    .padding(2, 4, 2, 4)
                    .borderAt(UI.Edge.BOTTOM, 1, p.borderSoft());
        }

        private static ComponentStyleDelegate<JPopupMenu> popupMenu( ComponentStyleDelegate<JPopupMenu> it ) {
            SwingTreeLookAndFeel.Palette p = SwingTreeLookAndFeel.palette();
            return it
                    .backgroundColor(p.surface())
                    .foregroundColor(p.text())
                    .padding(4, 0, 4, 0)
                    .borderRadius(0)
                    .border(1, p.borderSoft());
        }

        private static ComponentStyleDelegate<JToolTip> toolTip( ComponentStyleDelegate<JToolTip> it ) {
            SwingTreeLookAndFeel.Palette p = SwingTreeLookAndFeel.palette();
            return it
                    .padding(5, 9, 5, 9)
                    .borderRadius(0)
                    .borderWidth(0)
                    .backgroundColor(p.text())
                    .foregroundColor(p.onFilled());
        }

        /** The delegate draws the hairline itself, so the rule leaves the rest of the strip alone. */
        private static ComponentStyleDelegate<JSeparator> separator( ComponentStyleDelegate<JSeparator> it ) {
            SwingTreeLookAndFeel.Palette p = SwingTreeLookAndFeel.palette();
            return it.backgroundColor(SwingTreeLookAndFeel.Palette.TRANSPARENT).foregroundColor(p.borderSoft());
        }

        private static ComponentStyleDelegate<JProgressBar> progressBar( ComponentStyleDelegate<JProgressBar> it ) {
            SwingTreeLookAndFeel.Palette p = SwingTreeLookAndFeel.palette();
            return it
                    .borderRadius(0)
                    .borderWidth(0)
                    .backgroundColor(p.accentSoft())
                    .foregroundColor(p.accent());
        }

        private static ComponentStyleDelegate<JScrollBar> scrollBar( ComponentStyleDelegate<JScrollBar> it ) {
            SwingTreeLookAndFeel.Palette p = SwingTreeLookAndFeel.palette();
            return it.backgroundColor(p.background()).foregroundColor(p.border());
        }

        private static ComponentStyleDelegate<JTabbedPane> tabbedPane( ComponentStyleDelegate<JTabbedPane> it ) {
            SwingTreeLookAndFeel.Palette p = SwingTreeLookAndFeel.palette();
            return it.backgroundColor(p.background()).foregroundColor(p.text());
        }

        private static ComponentStyleDelegate<JTableHeader> tableHeader( ComponentStyleDelegate<JTableHeader> it ) {
            SwingTreeLookAndFeel.Palette p = SwingTreeLookAndFeel.palette();
            return it
                    .backgroundColor(p.surface())
                    .foregroundColor(p.textMuted())
                    .borderAt(UI.Edge.BOTTOM, 1, p.border());
        }

        private static ComponentStyleDelegate<JToolBar> toolBar( ComponentStyleDelegate<JToolBar> it ) {
            SwingTreeLookAndFeel.Palette p = SwingTreeLookAndFeel.palette();
            return it
                    .backgroundColor(p.surface())
                    .foregroundColor(p.text())
                    .padding(4, 8, 4, 8)
                    .borderRadius(0)
                    .borderAt(UI.Edge.BOTTOM, 1, p.borderSoft());
        }

        /** A list, table or tree is the content of the box around it, so it fills nothing itself. */
        private static <C extends JComponent> ComponentStyleDelegate<C> content( ComponentStyleDelegate<C> it ) {
            return it.backgroundColor(SwingTreeLookAndFeel.Palette.TRANSPARENT).foregroundColor(SwingTreeLookAndFeel.palette().text());
        }

        /** Structure with nothing of its own to paint: the symbol set draws all of it. */
        private static <C extends JComponent> ComponentStyleDelegate<C> bare( ComponentStyleDelegate<C> it ) {
            return it.backgroundColor(SwingTreeLookAndFeel.Palette.TRANSPARENT).foregroundColor(SwingTreeLookAndFeel.palette().text());
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
            SwingTreeLookAndFeel.Palette p = SwingTreeLookAndFeel.palette();
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
        private static ComponentStyleDelegate<JScrollPane> scrollPane( ComponentStyleDelegate<JScrollPane> it ) {
            SwingTreeLookAndFeel.Palette p = SwingTreeLookAndFeel.palette();
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
        private static ComponentStyleDelegate<JViewport> viewport( ComponentStyleDelegate<JViewport> it ) {
            return it.backgroundColor(SwingTreeLookAndFeel.Palette.TRANSPARENT).foregroundColor(SwingTreeLookAndFeel.palette().text());
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
                                        .type(UI.ShadowType.BLUR).isInset(false));
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
        private static ComponentStyleDelegate<AbstractButton> button( ComponentStyleDelegate<AbstractButton> it ) {
            SwingTreeLookAndFeel.Palette p = SwingTreeLookAndFeel.palette();
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
                                              .type(UI.ShadowType.GLOW).isInset(false));
            if ( !enabled )
                return it.borderColor(p.border());
            if ( variant == SwingTreeLookAndFeel.Variant.QUIET && !sunken && !rollover )
                return it.borderColor(SwingTreeLookAndFeel.Palette.TRANSPARENT).backgroundColor(SwingTreeLookAndFeel.Palette.TRANSPARENT);
            return plate(it, base, sunken);
        }

        @SuppressWarnings("deprecation")
        private static <C extends AbstractButton> ComponentStyleDelegate<C> tickable( ComponentStyleDelegate<C> it ) {
            SwingTreeLookAndFeel.Palette p = SwingTreeLookAndFeel.palette();
            return it
                    .backgroundColor(SwingTreeLookAndFeel.Palette.TRANSPARENT)
                    .foregroundColor(it.component().isEnabled() ? p.text() : p.textDisabled())
                    .padding(2, 4, 2, 4);
        }

        @SuppressWarnings("deprecation")
        private static ComponentStyleDelegate<JComboBox> comboBox( ComponentStyleDelegate<JComboBox> it ) {
            SwingTreeLookAndFeel.Palette p     = SwingTreeLookAndFeel.palette();
            JComboBox<?> combo = it.component();
            return machined(it, p, combo.isEnabled(), LafUtilities.hasFocus(combo), 5, 10, 4);
        }

        @SuppressWarnings("deprecation")
        private static ComponentStyleDelegate<JSpinner> spinner( ComponentStyleDelegate<JSpinner> it ) {
            SwingTreeLookAndFeel.Palette p       = SwingTreeLookAndFeel.palette();
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
        private static <C extends JComponent> ComponentStyleDelegate<C> machined(
                ComponentStyleDelegate<C> it, SwingTreeLookAndFeel.Palette p, boolean enabled, boolean focused,
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
            SwingTreeLookAndFeel.Palette p        = SwingTreeLookAndFeel.palette();
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
        private static ComponentStyleDelegate<JLabel> label( ComponentStyleDelegate<JLabel> it ) {
            SwingTreeLookAndFeel.Palette p = SwingTreeLookAndFeel.palette();
            return it.foregroundColor(it.component().isEnabled() ? p.text() : p.textDisabled());
        }

        @SuppressWarnings("deprecation")
        private static ComponentStyleDelegate<JMenuItem> menuItem( ComponentStyleDelegate<JMenuItem> it ) {
            SwingTreeLookAndFeel.Palette p       = SwingTreeLookAndFeel.palette();
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

        private static ComponentStyleDelegate<JMenuBar> menuBar( ComponentStyleDelegate<JMenuBar> it ) {
            SwingTreeLookAndFeel.Palette p = SwingTreeLookAndFeel.palette();
            return plate(it
                    .foregroundColor(p.text())
                    .padding(2, 4, 2, 4)
                    .borderRadius(0)
                    .borderWidth(1), p.surface(), false);
        }

        private static ComponentStyleDelegate<JPopupMenu> popupMenu( ComponentStyleDelegate<JPopupMenu> it ) {
            SwingTreeLookAndFeel.Palette p = SwingTreeLookAndFeel.palette();
            return sheet(it
                    .foregroundColor(p.text())
                    .margin(5)
                    .padding(4, 0, 4, 0), p);
        }

        private static ComponentStyleDelegate<JToolTip> toolTip( ComponentStyleDelegate<JToolTip> it ) {
            SwingTreeLookAndFeel.Palette p = SwingTreeLookAndFeel.palette();
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
            SwingTreeLookAndFeel.Palette p = SwingTreeLookAndFeel.palette();
            return it
                    .backgroundColor(SwingTreeLookAndFeel.Palette.TRANSPARENT)
                    .foregroundColor(p.border())
                    .shadow(FLOOR, s -> s.color(LafUtilities.withOpacity(Color.WHITE, 130))
                                         .offset(0, 1).blurRadius(0).isInset(false));
        }

        private static ComponentStyleDelegate<JProgressBar> progressBar( ComponentStyleDelegate<JProgressBar> it ) {
            SwingTreeLookAndFeel.Palette p = SwingTreeLookAndFeel.palette();
            return well(it
                    .margin(2)
                    .borderRadius(6)
                    .border(1, p.border())
                    .backgroundColor(p.surfaceDisabled())
                    .foregroundColor(p.accent()), 2);
        }

        private static ComponentStyleDelegate<JScrollBar> scrollBar( ComponentStyleDelegate<JScrollBar> it ) {
            SwingTreeLookAndFeel.Palette p = SwingTreeLookAndFeel.palette();
            return well(it
                    .backgroundColor(p.surfaceDisabled())
                    .foregroundColor(p.border()), 2);
        }

        private static ComponentStyleDelegate<JTabbedPane> tabbedPane( ComponentStyleDelegate<JTabbedPane> it ) {
            SwingTreeLookAndFeel.Palette p = SwingTreeLookAndFeel.palette();
            return it.backgroundColor(SwingTreeLookAndFeel.Palette.TRANSPARENT).foregroundColor(p.text());
        }

        private static ComponentStyleDelegate<JTableHeader> tableHeader( ComponentStyleDelegate<JTableHeader> it ) {
            SwingTreeLookAndFeel.Palette p = SwingTreeLookAndFeel.palette();
            return plate(it
                    .foregroundColor(p.textMuted())
                    .borderRadius(0)
                    .borderWidth(1), p.surface(), false);
        }

        private static ComponentStyleDelegate<JToolBar> toolBar( ComponentStyleDelegate<JToolBar> it ) {
            SwingTreeLookAndFeel.Palette p = SwingTreeLookAndFeel.palette();
            return plate(it
                    .foregroundColor(p.text())
                    .margin(4)
                    .padding(4, 8, 4, 8)
                    .borderRadius(RADIUS)
                    .borderWidth(1), p.surface(), false);
        }

        /** A list, table or tree lies on the floor of the well the scroll pane milled for it. */
        private static <C extends JComponent> ComponentStyleDelegate<C> content( ComponentStyleDelegate<C> it ) {
            return it.backgroundColor(SwingTreeLookAndFeel.Palette.TRANSPARENT).foregroundColor(SwingTreeLookAndFeel.palette().text());
        }

        /** Structure with nothing of its own to paint: the symbol set draws all of it. */
        private static <C extends JComponent> ComponentStyleDelegate<C> bare( ComponentStyleDelegate<C> it ) {
            return it.backgroundColor(SwingTreeLookAndFeel.Palette.TRANSPARENT).foregroundColor(SwingTreeLookAndFeel.palette().text());
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
            ComponentStyleDelegate<C> it, int wash, int lift
        ) {
            SwingTreeLookAndFeel.Palette p = SwingTreeLookAndFeel.palette();
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
            SwingTreeLookAndFeel.Palette p = SwingTreeLookAndFeel.palette();
            it = it.foregroundColor(p.text());
            switch ( SwingTreeLookAndFeel.Surface.of(it.component()) ) {
                case CARD:        return pane(it.borderRadius(RADIUS).borderWidth(1).margin(7).padding(2), PANE, 6);
                case RAIL:        return pane(it.borderRadius(0).borderWidth(0), PANE / 2, 3);
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
        private static ComponentStyleDelegate<JScrollPane> scrollPane( ComponentStyleDelegate<JScrollPane> it ) {
            SwingTreeLookAndFeel.Palette p = SwingTreeLookAndFeel.palette();
            it = it.foregroundColor(p.text());
            switch ( SwingTreeLookAndFeel.Surface.of(it.component()) ) {
                case TRANSPARENT: return it.backgroundColor(SwingTreeLookAndFeel.Palette.TRANSPARENT).borderWidth(0).borderRadius(0).padding(0);
                case CARD:        return pane(it.borderRadius(RADIUS).borderWidth(1).margin(7).padding(3), PANE, 6);
                case RAIL:        return it.backgroundColor(SwingTreeLookAndFeel.Palette.TRANSPARENT).borderWidth(0).borderRadius(0).padding(0);
                case WINDOW:
                default:          return pane(it.borderRadius(RADIUS - 4).borderWidth(1).margin(4).padding(3), WELL, 3);
            }
        }

        // ── Controls ─────────────────────────────────────────────────────────

        @SuppressWarnings("deprecation")
        private static ComponentStyleDelegate<AbstractButton> button( ComponentStyleDelegate<AbstractButton> it ) {
            SwingTreeLookAndFeel.Palette p = SwingTreeLookAndFeel.palette();
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
                return pane(it, PANE, sunken ? 2 : 5)
                        .backgroundColor(LafUtilities.withOpacity(tint(variant, p, sunken, rollover), 150));
            if ( variant == SwingTreeLookAndFeel.Variant.QUIET && !sunken && !rollover )
                return it.backgroundColor(SwingTreeLookAndFeel.Palette.TRANSPARENT).borderColor(SwingTreeLookAndFeel.Palette.TRANSPARENT);
            return pane(it, sunken ? WELL : rollover ? PANE + 22 : PANE, sunken ? 2 : 5);
        }

        @SuppressWarnings("deprecation")
        private static <C extends AbstractButton> ComponentStyleDelegate<C> tickable( ComponentStyleDelegate<C> it ) {
            SwingTreeLookAndFeel.Palette p = SwingTreeLookAndFeel.palette();
            return it
                    .backgroundColor(SwingTreeLookAndFeel.Palette.TRANSPARENT)
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
            SwingTreeLookAndFeel.Palette p        = SwingTreeLookAndFeel.palette();
            boolean editable = text.isEnabled() && text.isEditable();
            // Inside a scroll pane or a picker the pane has already been cut around it.
            if ( LafUtilities.isInsideAnotherControl(text) )
                return it
                        .margin(0).padding(padY, padX, padY, padX).borderWidth(0)
                        .backgroundColor(SwingTreeLookAndFeel.Palette.TRANSPARENT)
                        .foregroundColor(text.isEnabled() ? p.text() : p.textDisabled());
            return frosted(it, editable, editable && text.isFocusOwner(), padY, padX, padX)
                    .foregroundColor(text.isEnabled() ? p.text() : p.textDisabled());
        }

        /** A pane you reach into: darker than the ones you only look at, so text stands off it. */
        private static <C extends JComponent> ComponentStyleDelegate<C> frosted(
            ComponentStyleDelegate<C> it, boolean enabled, boolean focused, int padY, int padX, int padRight
        ) {
            SwingTreeLookAndFeel.Palette p = SwingTreeLookAndFeel.palette();
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
            SwingTreeLookAndFeel.Palette p = SwingTreeLookAndFeel.palette();
            return it.foregroundColor(it.component().isEnabled() ? p.text() : p.textDisabled());
        }

        @SuppressWarnings("deprecation")
        private static ComponentStyleDelegate<JMenuItem> menuItem( ComponentStyleDelegate<JMenuItem> it ) {
            SwingTreeLookAndFeel.Palette p       = SwingTreeLookAndFeel.palette();
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

        private static ComponentStyleDelegate<JMenuBar> menuBar( ComponentStyleDelegate<JMenuBar> it ) {
            SwingTreeLookAndFeel.Palette p = SwingTreeLookAndFeel.palette();
            return pane(it
                    .foregroundColor(p.text())
                    .padding(2, 4, 2, 4)
                    .borderRadius(0)
                    .borderWidth(0), PANE / 2, 3);
        }

        @SuppressWarnings("deprecation") // component() is the documented hook for LAF state reads
        private static ComponentStyleDelegate<JPopupMenu> popupMenu( ComponentStyleDelegate<JPopupMenu> it ) {
            SwingTreeLookAndFeel.Palette p = SwingTreeLookAndFeel.palette();
            return groundIfUnfrosted(pane(it
                    .foregroundColor(p.text())
                    .margin(7)
                    .padding(5, 0, 5, 0)
                    .borderRadius(RADIUS)
                    .borderWidth(1), PANE + 30, 7), it.component(), PANE + 30);
        }

        @SuppressWarnings("deprecation") // component() is the documented hook for LAF state reads
        private static ComponentStyleDelegate<JToolTip> toolTip( ComponentStyleDelegate<JToolTip> it ) {
            SwingTreeLookAndFeel.Palette p = SwingTreeLookAndFeel.palette();
            return groundIfUnfrosted(pane(it
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
            ComponentStyleDelegate<C> it, C popup, int wash
        ) {
            if ( SwingTreeLookAndFeel.popupWindowModeOf(popup) != SwingTreeLookAndFeel.PopupWindowMode.TRANSLUCENT )
                return it;
            SwingTreeLookAndFeel.Palette p = SwingTreeLookAndFeel.palette();
            Color grounded = LafUtilities.shadeTowards(p.background(), p.surface(), wash / 255.0);
            return it.backgroundColor(LafUtilities.withOpacity(grounded, UNFROSTED_PANE));
        }

        /** The delegate draws the hairline itself, so the rule leaves the rest of the strip alone. */
        private static ComponentStyleDelegate<JSeparator> separator( ComponentStyleDelegate<JSeparator> it ) {
            SwingTreeLookAndFeel.Palette p = SwingTreeLookAndFeel.palette();
            return it.backgroundColor(SwingTreeLookAndFeel.Palette.TRANSPARENT)
                     .foregroundColor(LafUtilities.withOpacity(p.border(), 60));
        }

        private static ComponentStyleDelegate<JProgressBar> progressBar( ComponentStyleDelegate<JProgressBar> it ) {
            SwingTreeLookAndFeel.Palette p = SwingTreeLookAndFeel.palette();
            return it
                    .margin(2)
                    .borderRadius(6)
                    .border(1, LafUtilities.withOpacity(p.border(), 50))
                    .backgroundColor(LafUtilities.withOpacity(p.surfaceField(), WELL))
                    .foregroundColor(p.accent());
        }

        private static ComponentStyleDelegate<JScrollBar> scrollBar( ComponentStyleDelegate<JScrollBar> it ) {
            SwingTreeLookAndFeel.Palette p = SwingTreeLookAndFeel.palette();
            return it.backgroundColor(SwingTreeLookAndFeel.Palette.TRANSPARENT).foregroundColor(p.border());
        }

        private static ComponentStyleDelegate<JTableHeader> tableHeader( ComponentStyleDelegate<JTableHeader> it ) {
            SwingTreeLookAndFeel.Palette p = SwingTreeLookAndFeel.palette();
            return it
                    .backgroundColor(LafUtilities.withOpacity(p.surface(), 26))
                    .foregroundColor(p.textMuted())
                    .borderAt(UI.Edge.BOTTOM, 1, LafUtilities.withOpacity(p.border(), 60));
        }

        private static ComponentStyleDelegate<JToolBar> toolBar( ComponentStyleDelegate<JToolBar> it ) {
            SwingTreeLookAndFeel.Palette p = SwingTreeLookAndFeel.palette();
            return pane(it
                    .foregroundColor(p.text())
                    .margin(6)
                    .padding(4, 8, 4, 8)
                    .borderRadius(RADIUS)
                    .borderWidth(1), PANE, 5);
        }

        /** Everything that is only the contents of a pane somebody else already cut. */
        private static <C extends JComponent> ComponentStyleDelegate<C> bare( ComponentStyleDelegate<C> it ) {
            return it.backgroundColor(SwingTreeLookAndFeel.Palette.TRANSPARENT).foregroundColor(SwingTreeLookAndFeel.palette().text());
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
     *  engine. Every raised thing is one piece of moulded plastic under one overhead light: bright
     *  along the top edge, dimming about two thirds of the way down, catching the light again on the
     *  bottom lip, and closed off underneath by a line much darker than the rest of its outline. A
     *  button, a combo box, a table heading, a tab, a check box and a scroll-bar thumb differ only in
     *  the colour that curve is laid over and in how they are outlined, so the curve is written once
     *  in {@link NimbusRelief} and most of the rules here only say which tone a control is cast in.
     *  <p>
     *  Two of its habits are easy to get wrong. Pressing something moves the colour down and leaves
     *  the light where it was, so a pressed button looks pushed into the panel rather than lit from
     *  below. Disabling something flattens the curve instead of greying it, because a control that
     *  cannot be used is one nothing is shining on.
     *  <p>
     *  No colour here is written down. Each one is a named palette colour that has been moved:
     *  {@link LafUtilities#shiftHsb} where the distance is fixed, and {@link LafUtilities#wash} where
     *  it has to be measured against the ground the colour will be seen on. The difference decides
     *  whether the theme survives a palette it was not designed for. A fixed brightness offset
     *  reproduces the original exactly and then turns
     *  {@link SwingTreeLookAndFeel.PalettePreset#MIDNIGHT}'s already bright accent white, taking every
     *  label on top of it. Washing a colour towards the surface it will be seen against is the same
     *  instruction on a light palette and on a dark one.
     *  <p>
     *  One colour would not fit. Nimbus writes a tool tip on a pale yellow it calls {@code info},
     *  deliberately not a shade of anything else and therefore not derivable from the palette either.
     *  There is no palette slot for a notice colour, so it is carried in the two grain slots, which a
     *  theme with no grain has spare - the same room {@link Palettes#AURORA} borrows for its blooms.
     *  Nothing here knows whether that colour came out light or dark, so the ink on it is picked by
     *  {@link LafUtilities#readableOn} rather than named.
     *
     *  @see SwingTreeLookAndFeel.StylePreset#NIMBUS
     */
    static final class Nimbus
    {
        private Nimbus() {}

        /** The corner radius of everything that has one, in developer pixels. */
        private static final int RADIUS = 5;

        /** The room kept around a control for its focus ring and the shadow it drops. */
        private static final int MARGIN = 2;

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
            StyleRule.of(JTextArea.class,      Nimbus::page),
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

        // ── The light ────────────────────────────────────────────────────────

        /**
         *  The colour a control would be with no light on it, which is the colour the middle of its
         *  relief reproduces. Everything else about how it looks follows from this and from
         *  {@link #surfaceEdge}.
         */
        private static Color tone(
                SwingTreeLookAndFeel.Palette p, SwingTreeLookAndFeel.Variant variant, boolean enabled, boolean sunken, boolean rollover, boolean isDefault
        ) {
            if ( !enabled )
                return p.surfaceDisabled();
            switch ( variant ) {
                case PRIMARY: return sunken ? p.primaryPressed() : rollover ? p.primaryHover() : p.primary();
                case DANGER:  return sunken ? p.dangerPressed()  : rollover ? p.dangerHover()  : p.danger();
                default:
                    if ( isDefault )
                        return accentedTone(p, sunken, rollover);
                    return surfaceTone(p, enabled, sunken, rollover);
            }
        }

        /**
         *  The unlit colour of an ordinary control, which {@link Symbols.Nimbus} reads as well: a
         *  check box has the same four states a button has and is made of the same material, so
         *  neither keeps an idea of its own about what a pressed control looks like.
         *
         * @param p the palette in force
         * @param enabled whether the control can be used
         * @param sunken whether it is held down or selected
         * @param rollover whether the pointer is over it
         * @return the colour the middle of its relief reproduces
         */
        static Color surfaceTone(SwingTreeLookAndFeel.Palette p, boolean enabled, boolean sunken, boolean rollover ) {
            if ( !enabled )
                return p.surfaceDisabled();
            return sunken ? p.surfacePressed() : rollover ? p.surfaceHover() : p.surface();
        }

        /**
         *  Which of the mouldings lights a control, given what it is cast in.
         *
         * @param enabled whether the control can be used
         * @param accented whether it is made of the accented material rather than the neutral chrome
         * @return the relief to lay over its tone
         */
        static NimbusRelief relief( boolean enabled, boolean accented ) {
            if ( !enabled )
                return NimbusRelief.UNLIT;
            return accented ? NimbusRelief.LIT_ACCENTED : NimbusRelief.LIT;
        }

        /**
         *  The material anything that is <i>on</i> is made of: the default button, a ticked check box,
         *  a filled radio, a selected tab, and the actuator a combo box or a spinner is worked by.
         *  Nimbus gives all five the same washed out accent rather than a colour each.
         *  <p>
         *  It is washed towards the palette's own surface instead of simply lightened, so that it
         *  comes out dark on a dark palette. Lightening it there would leave the label on top of it
         *  unreadable, and this method does not choose the label.
         *
         * @param p the palette in force
         * @param sunken whether the control is held down or selected
         * @param rollover whether the pointer is over it
         * @return the colour the middle of its relief reproduces
         */
        static Color accentedTone(SwingTreeLookAndFeel.Palette p, boolean sunken, boolean rollover ) {
            double towardsGround = sunken ? 0.640 : rollover ? 0.900 : 0.807;
            return LafUtilities.wash(p.accent(), p.surface(), 0.307, towardsGround);
        }

        /**
         *  The bottom of an outline, which Nimbus draws much darker than the rest of it: the line
         *  where a raised thing meets what it stands on is in its own shadow. One edge, and the most
         *  reliable single difference between a control that looks moulded and one that looks printed.
         *
         * @param edge the colour the rest of the outline is drawn in
         * @return the colour for its bottom edge
         */
        static Color contactEdge( Color edge ) {
            return LafUtilities.shiftHsb(edge, +0.050, -0.262);
        }

        /**
         *  The outline the accented material wears: the accent again, lightened just enough to read
         *  as an edge rather than as a shadow.
         *
         * @param p the palette in force
         * @return the colour to outline an accented control with
         */
        static Color accentedEdge( SwingTreeLookAndFeel.Palette p ) {
            return LafUtilities.wash(p.accent(), p.surface(), 0.456, -0.025);
        }

        /**
         *  The outline. Nimbus does not keep one border colour and tint the fill under it: the outline
         *  darkens as the pointer arrives and goes nearly black while the control is held down, which
         *  is most of what makes a pressed button read as pressed at all.
         *
         * @param p the palette in force
         * @param enabled whether the control can be used
         * @param sunken whether it is held down or selected
         * @param rollover whether the pointer is over it
         * @return the colour to outline it with
         */
        static Color surfaceEdge(SwingTreeLookAndFeel.Palette p, boolean enabled, boolean sunken, boolean rollover ) {
            if ( !enabled )
                return LafUtilities.shiftHsb(p.border(), 0, +0.200);
            if ( sunken )
                return LafUtilities.shiftHsb(p.border(), +0.050, -0.600);
            return rollover ? LafUtilities.shiftHsb(p.border(), 0, -0.100) : p.border();
        }

        /** The pale ring a focused control wears outside its outline. */
        private static Color focusRing( SwingTreeLookAndFeel.Palette p ) {
            return LafUtilities.shiftHsb(p.accent(), -0.186, +0.271);
        }

        /**
         *  Adds that ring. It is painted as a hard-edged shadow rather than as a thicker border so
         *  that focus costs no layout: the ring grows outwards into the margin every control already
         *  keeps, instead of pushing the label around inside it.
         */
        private static <C extends JComponent> ComponentStyleDelegate<C> focused(
                ComponentStyleDelegate<C> it, SwingTreeLookAndFeel.Palette p, boolean focused
        ) {
            if ( !focused )
                return it;
            return it.shadow("focus", s -> s.color(focusRing(p)).blurRadius(0).spreadRadius(2).isInset(false));
        }

        /** The soft contact shadow a raised control drops on the panel it stands on. */
        private static <C extends JComponent> ComponentStyleDelegate<C> lifted( ComponentStyleDelegate<C> it, SwingTreeLookAndFeel.Palette p ) {
            return it.shadow("lift", s -> s.color(LafUtilities.withOpacity(p.border(), 90))
                                           .offset(0, 1).blurRadius(2).isInset(false));
        }

        // ── Surfaces ─────────────────────────────────────────────────────────

        @SuppressWarnings("deprecation") // component() is the documented hook for LAF state reads
        private static ComponentStyleDelegate<JPanel> panel( ComponentStyleDelegate<JPanel> it ) {
            SwingTreeLookAndFeel.Palette p = SwingTreeLookAndFeel.palette();
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

        @SuppressWarnings("deprecation")
        private static ComponentStyleDelegate<JScrollPane> scrollPane( ComponentStyleDelegate<JScrollPane> it ) {
            SwingTreeLookAndFeel.Palette p = SwingTreeLookAndFeel.palette();
            it = it.foregroundColor(p.text());
            if ( SwingTreeLookAndFeel.Surface.of(it.component()) == SwingTreeLookAndFeel.Surface.TRANSPARENT )
                return it.backgroundColor(SwingTreeLookAndFeel.Palette.TRANSPARENT).borderWidth(0).borderRadius(0).padding(0);
            return it
                    .backgroundColor(p.surfaceField())
                    .border(1, p.border())
                    .borderRadius(RADIUS)
                    .padding(3);
        }

        @SuppressWarnings("deprecation")
        private static ComponentStyleDelegate<JViewport> viewport( ComponentStyleDelegate<JViewport> it ) {
            SwingTreeLookAndFeel.Palette p = SwingTreeLookAndFeel.palette();
            it = it.foregroundColor(p.text());
            return it.backgroundColor(
                        SwingTreeLookAndFeel.Surface.of(it.component()) == SwingTreeLookAndFeel.Surface.TRANSPARENT ? SwingTreeLookAndFeel.Palette.TRANSPARENT : p.surfaceField()
                    );
        }

        /** A list, a table or a tree: a plain white page, because the scroll pane around it is the frame. */
        private static <C extends JComponent> ComponentStyleDelegate<C> sheet( ComponentStyleDelegate<C> it ) {
            SwingTreeLookAndFeel.Palette p = SwingTreeLookAndFeel.palette();
            return it.backgroundColor(p.surfaceField()).foregroundColor(p.text()).borderWidth(0);
        }

        // ── Controls ─────────────────────────────────────────────────────────

        @SuppressWarnings("deprecation")
        private static ComponentStyleDelegate<AbstractButton> button( ComponentStyleDelegate<AbstractButton> it ) {
            SwingTreeLookAndFeel.Palette p = SwingTreeLookAndFeel.palette();
            AbstractButton b = it.component();
            ButtonModel    m = b.getModel();

            boolean enabled   = b.isEnabled();
            boolean sunken    = enabled && ( ( m.isArmed() && m.isPressed() ) || m.isSelected() );
            boolean rollover  = enabled && m.isRollover() && !sunken;
            boolean focused   = enabled && b.isFocusOwner();
            boolean isDefault = b instanceof JButton && ((JButton) b).isDefaultButton();

            SwingTreeLookAndFeel.Variant variant = SwingTreeLookAndFeel.Variant.of(b);
            if ( variant == SwingTreeLookAndFeel.Variant.QUIET && !sunken && !rollover )
                return it
                        .margin(MARGIN)
                        .padding(6, 14, 6, 14)
                        .backgroundColor(SwingTreeLookAndFeel.Palette.TRANSPARENT)
                        .borderWidth(0)
                        .foregroundColor(enabled ? p.text() : p.textDisabled());

            Color        tone   = tone(p, variant, enabled, sunken, rollover, isDefault);
            NimbusRelief relief = relief(enabled, isDefault || variant.isFilled());

            Color edge = isDefault && enabled ? accentedEdge(p)
                                              : surfaceEdge(p, enabled, sunken, rollover);
            it = it
                    .margin(MARGIN)
                    .padding(6, 14, 6, 14)
                    .borderRadius(RADIUS)
                    .border(1, edge)
                    .borderAt(UI.Edge.BOTTOM, 1, enabled ? contactEdge(edge) : edge)
                    .backgroundColor(tone)
                    .gradient("relief", g -> relief.over(g, tone))
                    .foregroundColor(ink(p, variant, enabled));

            it = focused(it, p, focused);
            return enabled && !sunken ? lifted(it, p) : it;
        }

        private static Color ink(SwingTreeLookAndFeel.Palette p, SwingTreeLookAndFeel.Variant variant, boolean enabled ) {
            if ( !enabled )
                return p.textDisabled();
            return variant.isFilled() ? p.onFilled() : p.text();
        }

        /**
         *  A check box or a radio button. The box itself is a symbol rather than a style, so all that
         *  is left here is to keep the label's own ground out of the way of the panel behind it.
         */
        @SuppressWarnings("deprecation")
        private static <C extends AbstractButton> ComponentStyleDelegate<C> tickable( ComponentStyleDelegate<C> it ) {
            SwingTreeLookAndFeel.Palette p = SwingTreeLookAndFeel.palette();
            C       b = it.component();
            return focused(it, p, b.isEnabled() && b.isFocusOwner())
                    .margin(MARGIN)
                    .padding(2, 3, 2, 3)
                    .borderRadius(RADIUS)
                    .borderWidth(0)
                    .backgroundColor(SwingTreeLookAndFeel.Palette.TRANSPARENT)
                    .foregroundColor(b.isEnabled() ? p.text() : p.textDisabled());
        }

        @SuppressWarnings("deprecation")
        private static ComponentStyleDelegate<JComboBox> comboBox( ComponentStyleDelegate<JComboBox> it ) {
            SwingTreeLookAndFeel.Palette p       = SwingTreeLookAndFeel.palette();
            JComboBox<?> combo   = it.component();
            boolean      enabled = combo.isEnabled();
            boolean      focused = enabled && LafUtilities.hasFocus(combo);

            if ( combo.isEditable() )
                return inset(it, p, enabled, focused).padding(0, 3, 0, 6);

            Color        tone   = enabled ? p.surface() : p.surfaceDisabled();
            NimbusRelief relief = relief(enabled, false);
            Color        edge   = surfaceEdge(p, enabled, false, false);
            return focused(it
                    .margin(MARGIN)
                    .padding(4, 6, 4, 3)
                    .borderRadius(RADIUS)
                    .border(1, edge)
                    .borderAt(UI.Edge.BOTTOM, 1, enabled ? contactEdge(edge) : edge)
                    .backgroundColor(tone)
                    .gradient("relief", g -> relief.over(g, tone))
                    .foregroundColor(enabled ? p.text() : p.textDisabled()), p, focused);
        }

        @SuppressWarnings("deprecation")
        private static ComponentStyleDelegate<JSpinner> spinner( ComponentStyleDelegate<JSpinner> it ) {
            SwingTreeLookAndFeel.Palette p       = SwingTreeLookAndFeel.palette();
            JSpinner spinner = it.component();
            boolean  enabled = spinner.isEnabled();
            return inset(it, p, enabled, enabled && LafUtilities.hasFocus(spinner)).padding(0, 3, 0, 6);
        }

        // ── Inputs ───────────────────────────────────────────────────────────

        @SuppressWarnings("deprecation")
        private static ComponentStyleDelegate<JTextField> field( ComponentStyleDelegate<JTextField> it ) {
            return input(it, it.component());
        }

        @SuppressWarnings("deprecation")
        private static <C extends JTextComponent> ComponentStyleDelegate<C> page( ComponentStyleDelegate<C> it ) {
            return input(it, it.component());
        }

        private static <C extends JComponent> ComponentStyleDelegate<C> input(
            ComponentStyleDelegate<C> it, JTextComponent text
        ) {
            SwingTreeLookAndFeel.Palette p  = SwingTreeLookAndFeel.palette();
            boolean on = text.isEnabled() && text.isEditable();
            // Inside a scroll pane or a spinner the cut has already been made around it.
            if ( LafUtilities.isInsideAnotherControl(text) )
                return it
                        .margin(0).padding(2, 6, 2, 6).borderWidth(0)
                        .backgroundColor(SwingTreeLookAndFeel.Palette.TRANSPARENT)
                        .foregroundColor(text.isEnabled() ? p.text() : p.textDisabled());
            return inset(it, p, on, on && text.isFocusOwner()).padding(6, 6, 6, 6);
        }

        /**
         *  Anything you can type into: a white sheet cut into the panel rather than standing on it. The
         *  relief is therefore upside down - a shade along the top edge where the wall of the cut
         *  catches no light - and it is an inset shadow rather than a gradient so that it stays one
         *  pixel deep however tall the field is.
         */
        private static <C extends JComponent> ComponentStyleDelegate<C> inset(
                ComponentStyleDelegate<C> it, SwingTreeLookAndFeel.Palette p, boolean enabled, boolean focused
        ) {
            return focused(it
                    .margin(MARGIN)
                    .borderRadius(RADIUS)
                    .border(1, enabled ? p.border() : LafUtilities.shiftHsb(p.border(), 0, +0.200))
                    .backgroundColor(enabled ? p.surfaceField() : p.surfaceDisabled())
                    .foregroundColor(enabled ? p.text() : p.textDisabled())
                    .shadow("cut", s -> s.color(LafUtilities.withOpacity(p.border(), enabled ? 150 : 60))
                                         .offset(0, 1).blurRadius(2)
                                         .type(UI.ShadowType.PENUMBRA).isInset(true)), p, focused);
        }

        // ── Menus ────────────────────────────────────────────────────────────

        @SuppressWarnings("deprecation")
        private static ComponentStyleDelegate<JMenuItem> menuItem( ComponentStyleDelegate<JMenuItem> it ) {
            SwingTreeLookAndFeel.Palette p       = SwingTreeLookAndFeel.palette();
            JMenuItem   item    = it.component();
            ButtonModel m       = item.getModel();
            boolean     enabled = item.isEnabled();
            boolean     armed   = enabled && ( m.isArmed() || m.isSelected() );
            Color       tone    = p.accent();
            return it
                    .padding(3, 12, 4, 13)
                    .borderRadius(0)
                    .borderWidth(0)
                    .backgroundColor(armed ? tone : SwingTreeLookAndFeel.Palette.TRANSPARENT)
                    .gradient("relief", g -> armed ? NimbusRelief.LIT.over(g, tone) : g)
                    .foregroundColor(!enabled ? p.textDisabled() : armed ? p.onFilled() : p.text());
        }

        private static ComponentStyleDelegate<JMenuBar> menuBar( ComponentStyleDelegate<JMenuBar> it ) {
            SwingTreeLookAndFeel.Palette p    = SwingTreeLookAndFeel.palette();
            Color   tone = p.surface();
            return it
                    .padding(2, 6, 2, 6)
                    .borderWidth(0)
                    .borderAt(UI.Edge.BOTTOM, 1, p.border())
                    .backgroundColor(tone)
                    .gradient("relief", g -> NimbusRelief.STRIP.over(g, tone))
                    .foregroundColor(p.text());
        }

        private static ComponentStyleDelegate<JPopupMenu> popupMenu( ComponentStyleDelegate<JPopupMenu> it ) {
            SwingTreeLookAndFeel.Palette p = SwingTreeLookAndFeel.palette();
            return it
                    .margin(3)
                    .padding(6, 1, 6, 1)
                    .borderRadius(RADIUS)
                    .border(1, p.border())
                    .backgroundColor(p.surfaceDisabled())
                    .foregroundColor(p.text())
                    .shadow("lift", s -> s.color(LafUtilities.withOpacity(p.border(), 110))
                                          .offset(0, 2).blurRadius(4).isInset(false));
        }

        private static ComponentStyleDelegate<JToolTip> toolTip( ComponentStyleDelegate<JToolTip> it ) {
            SwingTreeLookAndFeel.Palette p = SwingTreeLookAndFeel.palette();
            return it
                    .margin(2)
                    .padding(4, 4, 4, 4)
                    .borderRadius(0)
                    .border(1, p.textureDark())
                    .backgroundColor(p.textureLight())
                    // The notice colour is chosen, not derived, so nothing here knows whether it came
                    // out light or dark.
                    .foregroundColor(LafUtilities.readableOn(p.textureLight(), p.text(), p.onFilled()))
                    .shadow("lift", s -> s.color(LafUtilities.withOpacity(p.border(), 90))
                                          .offset(0, 1).blurRadius(3).isInset(false));
        }

        // ── The rest ─────────────────────────────────────────────────────────

        @SuppressWarnings("deprecation")
        private static ComponentStyleDelegate<JLabel> label( ComponentStyleDelegate<JLabel> it ) {
            SwingTreeLookAndFeel.Palette p = SwingTreeLookAndFeel.palette();
            return it.foregroundColor(it.component().isEnabled() ? p.text() : p.textDisabled());
        }

        private static ComponentStyleDelegate<JSeparator> separator( ComponentStyleDelegate<JSeparator> it ) {
            SwingTreeLookAndFeel.Palette p = SwingTreeLookAndFeel.palette();
            return it.backgroundColor(p.borderSoft()).foregroundColor(p.borderSoft());
        }

        /**
         *  The trough a progress bar fills. The bar itself is a symbol, because its gloss has to be
         *  clipped to however much of the trough has been filled rather than to the component.
         */
        private static ComponentStyleDelegate<JProgressBar> progressBar( ComponentStyleDelegate<JProgressBar> it ) {
            SwingTreeLookAndFeel.Palette p    = SwingTreeLookAndFeel.palette();
            Color   tone = LafUtilities.shiftHsb(p.surface(), 0, -0.030);
            return it
                    .margin(MARGIN)
                    .borderRadius(3)
                    .border(1, p.border())
                    .backgroundColor(tone)
                    .gradient("trough", g -> NimbusRelief.UNLIT.over(g, tone))
                    .foregroundColor(p.primary());
        }

        private static ComponentStyleDelegate<JSlider> slider( ComponentStyleDelegate<JSlider> it ) {
            SwingTreeLookAndFeel.Palette p = SwingTreeLookAndFeel.palette();
            return it.backgroundColor(SwingTreeLookAndFeel.Palette.TRANSPARENT).foregroundColor(p.text());
        }

        private static ComponentStyleDelegate<JScrollBar> scrollBar( ComponentStyleDelegate<JScrollBar> it ) {
            SwingTreeLookAndFeel.Palette p = SwingTreeLookAndFeel.palette();
            return it.backgroundColor(p.surfaceHover()).foregroundColor(p.border());
        }

        private static ComponentStyleDelegate<JTabbedPane> tabbedPane( ComponentStyleDelegate<JTabbedPane> it ) {
            SwingTreeLookAndFeel.Palette p = SwingTreeLookAndFeel.palette();
            return it.backgroundColor(p.background()).foregroundColor(p.text());
        }

        private static ComponentStyleDelegate<JSplitPane> splitPane( ComponentStyleDelegate<JSplitPane> it ) {
            SwingTreeLookAndFeel.Palette p = SwingTreeLookAndFeel.palette();
            return it.backgroundColor(SwingTreeLookAndFeel.Palette.TRANSPARENT).foregroundColor(p.text());
        }

        private static ComponentStyleDelegate<JTableHeader> tableHeader( ComponentStyleDelegate<JTableHeader> it ) {
            SwingTreeLookAndFeel.Palette p    = SwingTreeLookAndFeel.palette();
            Color   tone = p.surface();
            return it
                    .padding(2, 5, 4, 5)
                    .backgroundColor(tone)
                    .gradient("relief", g -> NimbusRelief.LIT.over(g, tone))
                    .foregroundColor(p.text())
                    .borderAt(UI.Edge.BOTTOM, 1, LafUtilities.shiftHsb(p.border(), 0, -0.130));
        }

        private static ComponentStyleDelegate<JToolBar> toolBar( ComponentStyleDelegate<JToolBar> it ) {
            SwingTreeLookAndFeel.Palette p    = SwingTreeLookAndFeel.palette();
            Color   tone = p.surface();
            return it
                    .padding(2)
                    .borderWidth(0)
                    .backgroundColor(tone)
                    .gradient("relief", g -> NimbusRelief.STRIP.over(g, tone))
                    .foregroundColor(p.text());
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
            ComponentStyleDelegate<C> it, Color fill, int radius, int lift
        ) {
            SwingTreeLookAndFeel.Palette p   = SwingTreeLookAndFeel.palette();
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
            SwingTreeLookAndFeel.Palette p = SwingTreeLookAndFeel.palette();
            it = it.backgroundColor(fill).borderRadius(radius);
            if ( Mood.of(p) == Mood.RELIEF )
                return it
                        .borderWidth(focused ? 2 : 0)
                        .borderColor(focused ? p.accent() : SwingTreeLookAndFeel.Palette.TRANSPARENT)
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
            SwingTreeLookAndFeel.Palette p     = SwingTreeLookAndFeel.palette();
            JPanel  panel = it.component();
            it = it.foregroundColor(p.text());
            switch ( SwingTreeLookAndFeel.Surface.of(panel) ) {
                case CARD: {
                    int lift = STEP + STEP * depthOf(panel);
                    return lift(it.margin(lift).padding(2), p.surface(), MAX_RADIUS, lift);
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
        private static ComponentStyleDelegate<JScrollPane> scrollPane( ComponentStyleDelegate<JScrollPane> it ) {
            SwingTreeLookAndFeel.Palette p    = SwingTreeLookAndFeel.palette();
            JScrollPane pane = it.component();
            it = it.foregroundColor(p.text());
            switch ( SwingTreeLookAndFeel.Surface.of(pane) ) {
                case TRANSPARENT: return it.backgroundColor(SwingTreeLookAndFeel.Palette.TRANSPARENT).borderWidth(0).borderRadius(0).padding(0);
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
            SwingTreeLookAndFeel.Palette p = SwingTreeLookAndFeel.palette();
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

            it = it
                    .margin(4)
                    .padding(7, 16, 7, 16)
                    .borderRadius(radius)
                    .borderWidth(focused ? 2 : 0)
                    .borderColor(focused ? p.accent() : SwingTreeLookAndFeel.Palette.TRANSPARENT)
                    .backgroundColor(fill)
                    .foregroundColor(ink(variant, p, enabled));

            if ( !enabled || ( variant == SwingTreeLookAndFeel.Variant.QUIET && !sunken && !rollover ) )
                return it;
            if ( sunken )
                return recess(it, fill, radius, focused);
            return lift(it, fill, radius, rollover ? STEP + 2 : STEP);
        }

        @SuppressWarnings("deprecation")
        private static <C extends AbstractButton> ComponentStyleDelegate<C> tickable( ComponentStyleDelegate<C> it ) {
            SwingTreeLookAndFeel.Palette p = SwingTreeLookAndFeel.palette();
            return it
                    .backgroundColor(SwingTreeLookAndFeel.Palette.TRANSPARENT)
                    .foregroundColor(it.component().isEnabled() ? p.text() : p.textDisabled())
                    .padding(2, 4, 2, 4);
        }

        @SuppressWarnings("deprecation")
        private static ComponentStyleDelegate<JComboBox> comboBox( ComponentStyleDelegate<JComboBox> it ) {
            SwingTreeLookAndFeel.Palette p     = SwingTreeLookAndFeel.palette();
            JComboBox<?> combo = it.component();
            boolean      on    = combo.isEnabled();
            it = it.margin(4).padding(6, 4, 6, 10)
                   .foregroundColor(on ? p.text() : p.textDisabled());
            Color fill = on ? p.surface() : p.surfaceDisabled();
            return lift(it.borderWidth(0), fill, radiusOf(combo), STEP);
        }

        @SuppressWarnings("deprecation")
        private static ComponentStyleDelegate<JSpinner> spinner( ComponentStyleDelegate<JSpinner> it ) {
            SwingTreeLookAndFeel.Palette p       = SwingTreeLookAndFeel.palette();
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
            SwingTreeLookAndFeel.Palette p        = SwingTreeLookAndFeel.palette();
            boolean editable = text.isEnabled() && text.isEditable();
            // Inside a scroll pane or a picker, whatever that made of itself is the surface here.
            if ( LafUtilities.isInsideAnotherControl(text) )
                return it
                        .margin(0).padding(padY, padX, padY, padX).borderWidth(0)
                        .backgroundColor(SwingTreeLookAndFeel.Palette.TRANSPARENT)
                        .foregroundColor(text.isEnabled() ? p.text() : p.textDisabled());
            it = it.margin(4).padding(padY, padX, padY, padX)
                   .foregroundColor(text.isEnabled() ? p.text() : p.textDisabled());
            return recess(it, editable ? p.surfaceField() : p.surfaceDisabled(),
                          radiusOf(text), editable && text.isFocusOwner());
        }

        // ── The rest ─────────────────────────────────────────────────────────

        @SuppressWarnings("deprecation")
        private static ComponentStyleDelegate<JLabel> label( ComponentStyleDelegate<JLabel> it ) {
            SwingTreeLookAndFeel.Palette p = SwingTreeLookAndFeel.palette();
            return it.foregroundColor(it.component().isEnabled() ? p.text() : p.textDisabled());
        }

        @SuppressWarnings("deprecation")
        private static ComponentStyleDelegate<JMenuItem> menuItem( ComponentStyleDelegate<JMenuItem> it ) {
            SwingTreeLookAndFeel.Palette p       = SwingTreeLookAndFeel.palette();
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

        private static ComponentStyleDelegate<JMenuBar> menuBar( ComponentStyleDelegate<JMenuBar> it ) {
            SwingTreeLookAndFeel.Palette p = SwingTreeLookAndFeel.palette();
            return it.backgroundColor(p.background()).foregroundColor(p.text()).padding(2, 4, 2, 4).borderWidth(0);
        }

        @SuppressWarnings("deprecation")
        private static ComponentStyleDelegate<JPopupMenu> popupMenu( ComponentStyleDelegate<JPopupMenu> it ) {
            SwingTreeLookAndFeel.Palette p = SwingTreeLookAndFeel.palette();
            return lift(it.foregroundColor(p.text()).margin(5).padding(5, 0, 5, 0),
                        p.surface(), MAX_RADIUS - 4, STEP + 3);
        }

        @SuppressWarnings("deprecation")
        private static ComponentStyleDelegate<JToolTip> toolTip( ComponentStyleDelegate<JToolTip> it ) {
            SwingTreeLookAndFeel.Palette p = SwingTreeLookAndFeel.palette();
            return lift(it.foregroundColor(p.text()).margin(4).padding(5, 10, 5, 10),
                        p.surface(), radiusOf(it.component()), STEP + 2);
        }

        /** The delegate draws the hairline itself, so the rule leaves the rest of the strip alone. */
        private static ComponentStyleDelegate<JSeparator> separator( ComponentStyleDelegate<JSeparator> it ) {
            SwingTreeLookAndFeel.Palette p = SwingTreeLookAndFeel.palette();
            return it.backgroundColor(SwingTreeLookAndFeel.Palette.TRANSPARENT).foregroundColor(p.borderSoft());
        }

        @SuppressWarnings("deprecation")
        private static ComponentStyleDelegate<JProgressBar> progressBar( ComponentStyleDelegate<JProgressBar> it ) {
            SwingTreeLookAndFeel.Palette p   = SwingTreeLookAndFeel.palette();
            JProgressBar bar = it.component();
            return recess(it.margin(2).foregroundColor(p.accent()),
                          Mood.of(p) == Mood.RELIEF ? p.background() : p.accentSoft(), radiusOf(bar), false);
        }

        private static ComponentStyleDelegate<JScrollBar> scrollBar( ComponentStyleDelegate<JScrollBar> it ) {
            SwingTreeLookAndFeel.Palette p = SwingTreeLookAndFeel.palette();
            return it.backgroundColor(p.background()).foregroundColor(p.border());
        }

        private static ComponentStyleDelegate<JTableHeader> tableHeader( ComponentStyleDelegate<JTableHeader> it ) {
            SwingTreeLookAndFeel.Palette p = SwingTreeLookAndFeel.palette();
            return it
                    .backgroundColor(SwingTreeLookAndFeel.Palette.TRANSPARENT)
                    .foregroundColor(p.textMuted())
                    .borderAt(UI.Edge.BOTTOM, 1, p.borderSoft());
        }

        @SuppressWarnings("deprecation")
        private static ComponentStyleDelegate<JToolBar> toolBar( ComponentStyleDelegate<JToolBar> it ) {
            SwingTreeLookAndFeel.Palette p   = SwingTreeLookAndFeel.palette();
            JToolBar  bar = it.component();
            return lift(it.foregroundColor(p.text()).margin(5).padding(4, 8, 4, 8),
                        p.surface(), MAX_RADIUS, STEP + STEP * depthOf(bar));
        }

        /** Everything that is only the contents of a surface somebody else already made. */
        private static <C extends JComponent> ComponentStyleDelegate<C> bare( ComponentStyleDelegate<C> it ) {
            return it.backgroundColor(SwingTreeLookAndFeel.Palette.TRANSPARENT).foregroundColor(SwingTreeLookAndFeel.palette().text());
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
