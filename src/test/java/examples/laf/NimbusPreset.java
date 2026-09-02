package examples.laf;

import examples.laf.SwingTreeLookAndFeel.Palette;
import examples.laf.SwingTreeLookAndFeel.Surface;
import examples.laf.SwingTreeLookAndFeel.Variant;
import sprouts.Tuple;
import swingtree.UI;
import swingtree.style.ComponentStyleDelegate;

import javax.swing.AbstractButton;
import javax.swing.ButtonModel;
import javax.swing.JButton;
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
 *  <b>Nimbus</b>: the look and feel Sun shipped with Java 6 update 10, rebuilt on the style engine.
 *
 *  <h2>What the idiom actually is</h2>
 *  Every raised thing is the same piece of moulded plastic seen under one overhead light: bright
 *  along the top edge, dimming to about two thirds of the way down, then catching the light again
 *  on the bottom lip, and closed off underneath by a line much darker than the rest of its outline.
 *  A button, a combo box, a table heading, a tab, a check box and a scroll-bar thumb differ only in
 *  which colour that curve is laid over and how they are outlined, so the curve is written once, in
 *  {@link NimbusRelief}, and this file is mostly a list of which tone each control is cast in.
 *  <p>
 *  Two things follow from that which are easy to get wrong. Pressing something does not turn the
 *  light around: it moves the colour down and leaves the light where it was, which is why a pressed
 *  Nimbus button looks pushed into the panel rather than lit from below. And disabling something
 *  flattens the curve rather than greying it, because a control that cannot be used is one nothing
 *  is shining on.
 *
 *  <h2>Why no colour here is written down</h2>
 *  Nimbus's colour schemes are not repaints. Its whole appearance hangs off a handful of chosen
 *  colours, and every shade of chrome is stated as a distance from one of them, so replacing a
 *  chosen colour moves all of them together. This preset keeps that property: it asks the palette
 *  for a named colour and then moves it, with {@link LafUtilities#shiftHsb} where the distance is
 *  fixed and with {@link LafUtilities#wash} where it has to be read relative to the ground.
 *  <p>
 *  The distinction is what lets the theme survive a palette it was not designed for. A fixed
 *  brightness offset reproduces the original exactly and then turns
 *  {@link SwingTreeLookAndFeel.PalettePreset#MIDNIGHT}'s already-bright accent white, taking every
 *  label on top of it with it. Washing a colour <em>towards the surface it will be seen against</em>
 *  is the same instruction on a light palette and a dark one. The numbers used were measured off
 *  the colours Nimbus itself paints, so
 *  {@link SwingTreeLookAndFeel.PalettePreset#NIMBUS} reproduces it and
 *  {@link SwingTreeLookAndFeel.Palette#nimbus} re-tints it.
 *
 *  <h2>The one thing that did not fit</h2>
 *  Nimbus writes a tool tip on a pale yellow it calls {@code info}, deliberately not a shade of
 *  anything else in the theme and therefore not derivable from the palette either. There is no
 *  palette slot for a notice colour, so it is carried in the two grain slots, which a theme with no
 *  grain has spare - the same accommodation {@link Palettes#AURORA} makes for its blooms. Because
 *  it is chosen rather than derived, nothing here knows whether it came out light or dark, and the
 *  ink on it has to be picked by {@link LafUtilities#readableOn} rather than named.
 *
 *  @see SwingTreeLookAndFeel.StylePreset#NIMBUS
 */
final class NimbusPreset
{
    private NimbusPreset() {}

    /** The corner radius of everything that has one, in developer pixels. */
    private static final int RADIUS = 5;

    /** The room kept around a control for its focus ring and the shadow it drops. */
    private static final int MARGIN = 2;

    private static final Tuple<StyleRule> RULES = Tuple.of(
        StyleRule.of(JPanel.class,         NimbusPreset::panel),
        StyleRule.of(AbstractButton.class, NimbusPreset::button),
        StyleRule.of(JCheckBox.class,      NimbusPreset::tickable),
        StyleRule.of(JRadioButton.class,   NimbusPreset::tickable),
        StyleRule.of(JMenuItem.class,      NimbusPreset::menuItem),
        StyleRule.of(JMenuBar.class,       NimbusPreset::menuBar),
        StyleRule.of(JPopupMenu.class,     NimbusPreset::popupMenu),
        StyleRule.of(JLabel.class,         NimbusPreset::label),
        StyleRule.of(JTextField.class,     NimbusPreset::field),
        StyleRule.of(JTextArea.class,      NimbusPreset::page),
        StyleRule.of(JEditorPane.class,    NimbusPreset::page),
        StyleRule.of(JSeparator.class,     NimbusPreset::separator),
        StyleRule.of(JToolTip.class,       NimbusPreset::toolTip),
        StyleRule.of(JProgressBar.class,   NimbusPreset::progressBar),
        StyleRule.of(JSlider.class,        NimbusPreset::slider),
        StyleRule.of(JScrollBar.class,     NimbusPreset::scrollBar),
        StyleRule.of(JScrollPane.class,    NimbusPreset::scrollPane),
        StyleRule.of(JViewport.class,      NimbusPreset::viewport),
        StyleRule.of(JComboBox.class,      NimbusPreset::comboBox),
        StyleRule.of(JSpinner.class,       NimbusPreset::spinner),
        StyleRule.of(JTabbedPane.class,    NimbusPreset::tabbedPane),
        StyleRule.of(JList.class,          NimbusPreset::sheet),
        StyleRule.of(JTable.class,         NimbusPreset::sheet),
        StyleRule.of(JTableHeader.class,   NimbusPreset::tableHeader),
        StyleRule.of(JTree.class,          NimbusPreset::sheet),
        StyleRule.of(JToolBar.class,       NimbusPreset::toolBar),
        StyleRule.of(JSplitPane.class,     NimbusPreset::splitPane)
    );

    /** @return the theme's style rules, one per component family. */
    static Tuple<StyleRule> rules() { return RULES; }

    // ── The light ────────────────────────────────────────────────────────

    /**
     *  The colour a control would be with no light on it, which is the colour the middle of its
     *  relief reproduces. Everything else about how it looks follows from this and from
     *  {@link #surfaceEdge}.
     */
    private static Color tone(
        Palette p, Variant variant, boolean enabled, boolean sunken, boolean rollover, boolean isDefault
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
     *  The unlit colour of an ordinary control, which {@link NimbusSymbols} needs too: a check box
     *  has the same four states a button has and has to be made of the same material, so both read
     *  it from here rather than each keeping its own idea of what "pressed" looks like.
     *
     * @param p the palette in force
     * @param enabled whether the control can be used
     * @param sunken whether it is held down or selected
     * @param rollover whether the pointer is over it
     * @return the colour the middle of its relief reproduces
     */
    static Color surfaceTone( Palette p, boolean enabled, boolean sunken, boolean rollover ) {
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
     *  a filled radio, a selected tab, and the little actuator a combo box or a spinner is worked
     *  by. Nimbus gives all five the same washed-out accent rather than a colour each, which is
     *  what makes them read as one family of "this one, then".
     *  <p>
     *  It is washed towards the palette's own surface rather than simply lightened, so that on a
     *  dark palette it comes out dark. Lightening it there would leave the label on top of it
     *  unreadable, and the label is not this method's to choose.
     *
     * @param p the palette in force
     * @param sunken whether the control is held down or selected
     * @param rollover whether the pointer is over it
     * @return the colour the middle of its relief reproduces
     */
    static Color accentedTone( Palette p, boolean sunken, boolean rollover ) {
        double towardsGround = sunken ? 0.640 : rollover ? 0.900 : 0.807;
        return LafUtilities.wash(p.accent(), p.surface(), 0.307, towardsGround);
    }

    /**
     *  The outline that material wears: the accent again, lightened just enough to read as an edge
     *  rather than as a shadow.
     *
     * @param p the palette in force
     * @return the colour to outline an accented control with
     */
    /**
     *  The bottom of an outline, which Nimbus draws a good deal darker than the rest of it: the
     *  line where a raised thing meets what it is standing on is in its own shadow. It is the most
     *  consistent single difference between a control that looks moulded and one that looks
     *  printed, and it costs one edge.
     *
     * @param edge the colour the rest of the outline is drawn in
     * @return the colour for its bottom edge
     */
    static Color contactEdge( Color edge ) {
        return LafUtilities.shiftHsb(edge, +0.050, -0.262);
    }

    static Color accentedEdge( Palette p ) {
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
    static Color surfaceEdge( Palette p, boolean enabled, boolean sunken, boolean rollover ) {
        if ( !enabled )
            return LafUtilities.shiftHsb(p.border(), 0, +0.200);
        if ( sunken )
            return LafUtilities.shiftHsb(p.border(), +0.050, -0.600);
        return rollover ? LafUtilities.shiftHsb(p.border(), 0, -0.100) : p.border();
    }

    /** The pale ring a focused control wears outside its outline. */
    private static Color focusRing( Palette p ) {
        return LafUtilities.shiftHsb(p.accent(), -0.186, +0.271);
    }

    /**
     *  Adds that ring. It is painted as a hard-edged shadow rather than as a thicker border so
     *  that focus costs no layout: the ring grows outwards into the margin every control already
     *  keeps, instead of pushing the label around inside it.
     */
    private static <C extends JComponent> ComponentStyleDelegate<C> focused(
        ComponentStyleDelegate<C> it, Palette p, boolean focused
    ) {
        if ( !focused )
            return it;
        return it.shadow("focus", s -> s.color(focusRing(p)).blurRadius(0).spreadRadius(2).isInset(false));
    }

    /** The soft contact shadow a raised control drops on the panel it stands on. */
    private static <C extends JComponent> ComponentStyleDelegate<C> lifted( ComponentStyleDelegate<C> it, Palette p ) {
        return it.shadow("lift", s -> s.color(LafUtilities.withOpacity(p.border(), 90))
                                       .offset(0, 1).blurRadius(2).isInset(false));
    }

    // ── Surfaces ─────────────────────────────────────────────────────────

    @SuppressWarnings("deprecation") // component() is the documented hook for LAF state reads
    private static ComponentStyleDelegate<JPanel> panel( ComponentStyleDelegate<JPanel> it ) {
        Palette p = SwingTreeLookAndFeel.palette();
        it = it.foregroundColor(p.text());
        switch ( Surface.of(it.component()) ) {
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
                return it.backgroundColor(Palette.TRANSPARENT);
            case WINDOW:
            default:
                // A spinner's editor is a panel the application never declared, sitting inside a
                // box the spinner has already been given.
                return it.backgroundColor(
                            LafUtilities.isControlInternal(it.component()) ? Palette.TRANSPARENT
                                                                          : p.background()
                        );
        }
    }

    @SuppressWarnings("deprecation")
    private static ComponentStyleDelegate<JScrollPane> scrollPane( ComponentStyleDelegate<JScrollPane> it ) {
        Palette p = SwingTreeLookAndFeel.palette();
        it = it.foregroundColor(p.text());
        if ( Surface.of(it.component()) == Surface.TRANSPARENT )
            return it.backgroundColor(Palette.TRANSPARENT).borderWidth(0).borderRadius(0).padding(0);
        return it
                .backgroundColor(p.surfaceField())
                .border(1, p.border())
                .borderRadius(RADIUS)
                .padding(3);
    }

    @SuppressWarnings("deprecation")
    private static ComponentStyleDelegate<JViewport> viewport( ComponentStyleDelegate<JViewport> it ) {
        Palette p = SwingTreeLookAndFeel.palette();
        it = it.foregroundColor(p.text());
        return it.backgroundColor(
                    Surface.of(it.component()) == Surface.TRANSPARENT ? Palette.TRANSPARENT : p.surfaceField()
                );
    }

    /** A list, a table or a tree: a plain white page, because the scroll pane around it is the frame. */
    private static <C extends JComponent> ComponentStyleDelegate<C> sheet( ComponentStyleDelegate<C> it ) {
        Palette p = SwingTreeLookAndFeel.palette();
        return it.backgroundColor(p.surfaceField()).foregroundColor(p.text()).borderWidth(0);
    }

    // ── Controls ─────────────────────────────────────────────────────────

    @SuppressWarnings("deprecation")
    private static ComponentStyleDelegate<AbstractButton> button( ComponentStyleDelegate<AbstractButton> it ) {
        Palette        p = SwingTreeLookAndFeel.palette();
        AbstractButton b = it.component();
        ButtonModel    m = b.getModel();

        boolean enabled   = b.isEnabled();
        boolean sunken    = enabled && ( ( m.isArmed() && m.isPressed() ) || m.isSelected() );
        boolean rollover  = enabled && m.isRollover() && !sunken;
        boolean focused   = enabled && b.isFocusOwner();
        boolean isDefault = b instanceof JButton && ((JButton) b).isDefaultButton();

        Variant variant = Variant.of(b);
        if ( variant == Variant.QUIET && !sunken && !rollover )
            return it
                    .margin(MARGIN)
                    .padding(6, 14, 6, 14)
                    .backgroundColor(Palette.TRANSPARENT)
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

    private static Color ink( Palette p, Variant variant, boolean enabled ) {
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
        Palette p = SwingTreeLookAndFeel.palette();
        C       b = it.component();
        return focused(it, p, b.isEnabled() && b.isFocusOwner())
                .margin(MARGIN)
                .padding(2, 3, 2, 3)
                .borderRadius(RADIUS)
                .borderWidth(0)
                .backgroundColor(Palette.TRANSPARENT)
                .foregroundColor(b.isEnabled() ? p.text() : p.textDisabled());
    }

    @SuppressWarnings("deprecation")
    private static ComponentStyleDelegate<JComboBox> comboBox( ComponentStyleDelegate<JComboBox> it ) {
        Palette      p       = SwingTreeLookAndFeel.palette();
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
        Palette  p       = SwingTreeLookAndFeel.palette();
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
        Palette p  = SwingTreeLookAndFeel.palette();
        boolean on = text.isEnabled() && text.isEditable();
        // Inside a scroll pane or a spinner the cut has already been made around it.
        if ( LafUtilities.isInsideAnotherControl(text) )
            return it
                    .margin(0).padding(2, 6, 2, 6).borderWidth(0)
                    .backgroundColor(Palette.TRANSPARENT)
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
        ComponentStyleDelegate<C> it, Palette p, boolean enabled, boolean focused
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
        Palette     p       = SwingTreeLookAndFeel.palette();
        JMenuItem   item    = it.component();
        ButtonModel m       = item.getModel();
        boolean     enabled = item.isEnabled();
        boolean     armed   = enabled && ( m.isArmed() || m.isSelected() );
        Color       tone    = p.accent();
        return it
                .padding(3, 12, 4, 13)
                .borderRadius(0)
                .borderWidth(0)
                .backgroundColor(armed ? tone : Palette.TRANSPARENT)
                .gradient("relief", g -> armed ? NimbusRelief.LIT.over(g, tone) : g)
                .foregroundColor(!enabled ? p.textDisabled() : armed ? p.onFilled() : p.text());
    }

    private static ComponentStyleDelegate<JMenuBar> menuBar( ComponentStyleDelegate<JMenuBar> it ) {
        Palette p    = SwingTreeLookAndFeel.palette();
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
        Palette p = SwingTreeLookAndFeel.palette();
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
        Palette p = SwingTreeLookAndFeel.palette();
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
        Palette p = SwingTreeLookAndFeel.palette();
        return it.foregroundColor(it.component().isEnabled() ? p.text() : p.textDisabled());
    }

    private static ComponentStyleDelegate<JSeparator> separator( ComponentStyleDelegate<JSeparator> it ) {
        Palette p = SwingTreeLookAndFeel.palette();
        return it.backgroundColor(p.borderSoft()).foregroundColor(p.borderSoft());
    }

    /**
     *  The trough a progress bar fills. The bar itself is a symbol, because its gloss has to be
     *  clipped to however much of the trough has been filled rather than to the component.
     */
    private static ComponentStyleDelegate<JProgressBar> progressBar( ComponentStyleDelegate<JProgressBar> it ) {
        Palette p    = SwingTreeLookAndFeel.palette();
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
        Palette p = SwingTreeLookAndFeel.palette();
        return it.backgroundColor(Palette.TRANSPARENT).foregroundColor(p.text());
    }

    private static ComponentStyleDelegate<JScrollBar> scrollBar( ComponentStyleDelegate<JScrollBar> it ) {
        Palette p = SwingTreeLookAndFeel.palette();
        return it.backgroundColor(p.surfaceHover()).foregroundColor(p.border());
    }

    private static ComponentStyleDelegate<JTabbedPane> tabbedPane( ComponentStyleDelegate<JTabbedPane> it ) {
        Palette p = SwingTreeLookAndFeel.palette();
        return it.backgroundColor(p.background()).foregroundColor(p.text());
    }

    private static ComponentStyleDelegate<JSplitPane> splitPane( ComponentStyleDelegate<JSplitPane> it ) {
        Palette p = SwingTreeLookAndFeel.palette();
        return it.backgroundColor(Palette.TRANSPARENT).foregroundColor(p.text());
    }

    private static ComponentStyleDelegate<JTableHeader> tableHeader( ComponentStyleDelegate<JTableHeader> it ) {
        Palette p    = SwingTreeLookAndFeel.palette();
        Color   tone = p.surface();
        return it
                .padding(2, 5, 4, 5)
                .backgroundColor(tone)
                .gradient("relief", g -> NimbusRelief.LIT.over(g, tone))
                .foregroundColor(p.text())
                .borderAt(UI.Edge.BOTTOM, 1, LafUtilities.shiftHsb(p.border(), 0, -0.130));
    }

    private static ComponentStyleDelegate<JToolBar> toolBar( ComponentStyleDelegate<JToolBar> it ) {
        Palette p    = SwingTreeLookAndFeel.palette();
        Color   tone = p.surface();
        return it
                .padding(2)
                .borderWidth(0)
                .backgroundColor(tone)
                .gradient("relief", g -> NimbusRelief.STRIP.over(g, tone))
                .foregroundColor(p.text());
    }
}
