package examples.laf.app;

import examples.laf.SwingTreeLookAndFeel;
import examples.laf.SwingTreeLookAndFeel.PalettePreset;
import examples.laf.SwingTreeLookAndFeel.StylePreset;
import examples.laf.SwingTreeLookAndFeel.SymbolPreset;
import sprouts.From;
import sprouts.Var;
import sprouts.Viewable;
import swingtree.UI;
import swingtree.UIForLabel;
import swingtree.UIForMenu;
import swingtree.UIForRadioButtonMenuItem;
import swingtree.style.StyleSheet;

import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JRadioButtonMenuItem;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 *  The three choices that decide what the atelier looks like, and the menu the user makes them in.
 *
 *  <h2>Why the application owns this at all</h2>
 *  A look and feel is installed into {@link javax.swing.UIManager}, which is process-wide, so
 *  changing it is not something a view model has an opinion about - no commission in the order book
 *  depends on whether the window is made of paper or of glass. It is a preference about this
 *  program's chrome, so it lives here, next to the menu that sets it, as three plain properties.
 *
 *  <h2>What happens on a change</h2>
 *  {@link SwingTreeLookAndFeel#initializeUsing(swingtree.api.Configurator)} re-installs the look
 *  and feel and refreshes every open window, which rebuilds each component's UI delegate and
 *  re-gathers its style. Nothing in the component tree is replaced, so the caret, the selection,
 *  the scroll offsets and every property binding survive the switch. The application's own
 *  {@link StyleSheet} is reconfigured afterwards, because its type scale reads colours out of the
 *  palette and would otherwise still be carrying the previous one.
 *  <p>
 *  Choosing a <b>style</b> also moves the symbols and the palette to the ones that style was
 *  designed against, which is what makes the menu feel like a list of themes rather than three
 *  unrelated knobs. Choosing either of the other two afterwards leaves the style alone, so every
 *  combination is still reachable: Linen on a dark palette, Material with glassy symbols.
 */
final class Appearance
{
    private final Var<StylePreset>   _style   = Var.of(StylePreset.LINEN);
    private final Var<SymbolPreset>  _symbols = Var.of(StylePreset.LINEN.preferredSymbols());
    private final Var<PalettePreset> _palette = Var.of(StylePreset.LINEN.preferredPalette());

    private final StyleSheet _sheet;

    /**
     *  The boolean lenses the radio items are bound through, kept in a field because a lens is
     *  observed only weakly by the property it was zoomed out of: one that nothing holds quietly
     *  stops reporting.
     */
    private final List<Var<Boolean>> _bindings = new ArrayList<>();

    /** Set while a style change is re-pointing the other two axes, so the look and feel is
     *  installed once for the three writes rather than three times. */
    private boolean _switching = false;

    Appearance( StyleSheet sheet ) {
        _sheet = Objects.requireNonNull(sheet);
        Viewable.cast(_style).onChange(From.ALL, it -> _chooseStyle(_style.get()));
        Viewable.cast(_symbols).onChange(From.ALL, it -> _apply());
        Viewable.cast(_palette).onChange(From.ALL, it -> _apply());
    }

    /**
     *  The menu the user chooses in: three groups of radio items, one per axis, each labelled with
     *  the preset's own {@code toString()} so that adding a preset puts it in this menu without
     *  this class knowing anything about it.
     *
     * @return the "Appearance" submenu
     */
    UIForMenu<JMenu> menu() {
        // A SwingTree builder is immutable and every add(..) hands back a new one, so the result
        // has to be carried through the loops rather than discarded.
        UIForMenu<JMenu> appearance = UI.menu("Appearance").add(_heading("Style"));
        ButtonGroup styles = new ButtonGroup();
        for ( StylePreset preset : StylePreset.values() )
            appearance = appearance.add(_choice(preset.toString(), _style, preset, styles));

        appearance = appearance.add(UI.separator()).add(_heading("Symbols"));
        ButtonGroup symbols = new ButtonGroup();
        for ( SymbolPreset preset : SymbolPreset.values() )
            appearance = appearance.add(_choice(preset.toString(), _symbols, preset, symbols));

        appearance = appearance.add(UI.separator()).add(_heading("Palette"));
        ButtonGroup palettes = new ButtonGroup();
        for ( PalettePreset preset : PalettePreset.values() )
            appearance = appearance.add(_choice(preset.toString(), _palette, preset, palettes));

        return appearance;
    }

    /**
     *  One radio item, bound through a boolean lens rather than through the enum property directly.
     *  <p>
     *  {@code UI.radioButtonMenuItem(anEnum, aVar)} looks like the right call and is half of one:
     *  the write-back it needs lives on the <i>toggle button</i> builder, and a menu item is not a
     *  toggle button, so the enum overload reaches the read-only version - a click moves the tick
     *  and never reaches the property. Zooming the property into "is it this one?" reaches the
     *  boolean overload instead, which does write back, and is the same shape as the material
     *  filter a few lines up in {@link AtelierView}.
     *
     * @param label    what the item says
     * @param property the property the group as a whole selects into
     * @param value    the value this one item stands for
     * @param group    the button group the item belongs to
     * @param <E> the type of the choice
     * @return the item
     */
    private <E> UIForRadioButtonMenuItem<JRadioButtonMenuItem> _choice(
        String label, Var<E> property, E value, ButtonGroup group
    ) {
        Var<Boolean> isChosen = property.zoomTo(current -> current.equals(value),
                                                (current, chosen) -> chosen ? value : current);
        _bindings.add(isChosen);
        return UI.radioButtonMenuItem(label, isChosen).peek(group::add);
    }

    /** A non-interactive caption telling the three groups apart inside the one menu. */
    private static UIForLabel<JLabel> _heading( String text ) {
        return UI.label(text).withStyle( it -> it
                .padding(6, 12, 2, 12)
                .componentFont( f -> f.size(11).weight(2f).spacing(0.16f)
                                      .color(SwingTreeLookAndFeel.palette().textMuted()) )
        );
    }

    /** Moves the other two axes to what this style was designed against, then applies all three. */
    private void _chooseStyle( StylePreset style ) {
        if ( _switching )
            return;
        _switching = true;
        try {
            _symbols.set(From.VIEW_MODEL, style.preferredSymbols());
            _palette.set(From.VIEW_MODEL, style.preferredPalette());
        } finally {
            _switching = false;
        }
        _apply();
    }

    private void _apply() {
        if ( _switching )
            return;
        StylePreset   style   = _style.get();
        SymbolPreset  symbols = _symbols.get();
        PalettePreset palette = _palette.get();
        // The look and feel is Swing-wide state, so this has to happen on the event thread - and on
        // a later turn of it, so the menu that triggered the change is closed before every delegate
        // underneath it is swapped.
        UI.runLater(() -> {
            SwingTreeLookAndFeel.initializeUsing( it -> it
                .stylePreset(style)
                .symbolPreset(symbols)
                .palettePreset(palette)
            );
            _sheet.reconfigure();
        });
    }
}
