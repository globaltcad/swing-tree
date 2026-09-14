package examples.mixer;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import examples.laf.SwingTreeLookAndFeel;

import javax.swing.LookAndFeel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.plaf.metal.MetalLookAndFeel;
import javax.swing.plaf.nimbus.NimbusLookAndFeel;
import java.awt.Component;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

/**
 *  One entry of the Look menu: a look and feel the console can be switched to while it runs.
 *  <p>
 *  Tick marks, labels, the track and the knob of a slider are all painted by the look and feel,
 *  so the menu exists to check every scale on the console under several painters: two FlatLaf
 *  themes, Swing's own Metal and Nimbus, and every style preset of the example
 *  {@link SwingTreeLookAndFeel}, whose delegates are painted by the SwingTree style engine.
 *  <p>
 *  A look and feel is Swing-wide state, so {@link #installLookAndFeel()} must run on the UI thread.
 */
final class Look
{
    static final Look FLATLAF_DARK  = swing("FlatLaf Dark",  FlatDarkLaf::new);
    static final Look FLATLAF_LIGHT = swing("FlatLaf Light", FlatLightLaf::new);
    static final Look METAL         = swing("Metal",         MetalLookAndFeel::new);
    static final Look NIMBUS        = swing("Nimbus",        NimbusLookAndFeel::new);

    private static final List<Look> ALL = _all();

    private final String   name;
    private final boolean  isSwingTree;
    private final Runnable installation;

    private Look( String name, boolean isSwingTree, Runnable installation ) {
        this.name         = name;
        this.isSwingTree  = isSwingTree;
        this.installation = installation;
    }

    private static Look swing( String name, Supplier<LookAndFeel> lookAndFeel ) {
        return new Look(name, false, () -> {
            try {
                UIManager.setLookAndFeel(lookAndFeel.get());
            } catch ( UnsupportedLookAndFeelException e ) {
                throw new IllegalStateException("Cannot install " + name + ".", e);
            }
        });
    }

    private static Look swingTree( SwingTreeLookAndFeel.StylePreset preset ) {
        return new Look("SwingTree · " + preset, true, () ->
            SwingTreeLookAndFeel.initializeUsing( it -> it
                .stylePreset(preset)
                .symbolPreset(preset.preferredSymbols())
                .palettePreset(preset.preferredPalette())
            )
        );
    }

    private static List<Look> _all() {
        List<Look> looks = new ArrayList<>();
        looks.add(FLATLAF_DARK);
        looks.add(FLATLAF_LIGHT);
        looks.add(METAL);
        looks.add(NIMBUS);
        for ( SwingTreeLookAndFeel.StylePreset preset : SwingTreeLookAndFeel.StylePreset.values() )
            looks.add(swingTree(preset));
        return Collections.unmodifiableList(looks);
    }

    static List<Look> all() { return ALL; }

    boolean isSwingTree() { return isSwingTree; }

    /**
     *  Installs this look and feel into the {@link UIManager}, without touching any component.
     *  {@link LookSwitcher} decides what happens to the components afterwards.
     */
    void installLookAndFeel() {
        installation.run();
    }

    /**
     *  Tells whether components can be carried over from the other look and feel to this one by
     *  refreshing their UI delegates, or whether they have to be built anew.
     *  <p>
     *  Between two plain Swing look and feels, and between two presets of the SwingTree look and
     *  feel, a refresh through {@link SwingUtilities#updateComponentTreeUI(Component)} is enough.
     *  From the SwingTree look and feel to a plain Swing one it is not. The SwingTree delegates and
     *  the style engine call setters like {@code setOpaque(..)}, {@code setContentAreaFilled(..)} or
     *  {@code setBorder(..)} on the components, and Swing marks every property set that way as the
     *  application's own choice, which no later look and feel may overwrite. FlatLaf buttons would
     *  keep an empty content area and no border. Nothing can clear that mark, so the console is
     *  built anew instead. It loses nothing by that, because all of its state lives in the view
     *  model and not in the components.
     */
    boolean canRefreshComponentsOf( Look other ) {
        return isSwingTree == other.isSwingTree;
    }

    @Override
    public String toString() { return name; }
}
