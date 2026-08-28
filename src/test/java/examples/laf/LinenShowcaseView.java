package examples.laf;

import examples.laf.app.AtelierSheet;
import examples.laf.app.AtelierView;
import examples.laf.app.AtelierViewModel;
import sprouts.Var;
import swingtree.UI;
import swingtree.threading.EventProcessor;

import javax.swing.JPanel;

/**
 *  The showcase of the {@link SwingTreeLookAndFeel}: it installs the look and feel under its
 *  {@linkplain SwingTreeLookAndFeel.StylePreset#LINEN Linen} preset and opens <b>Flaxen</b>, the
 *  order book of a small weaving atelier, built with SwingTree in {@link examples.laf.app}.
 *  <p>
 *  A component gallery can tell you that a look and feel <em>has</em> a check box; it cannot tell
 *  you whether the thing is pleasant to work in. So this showcase is a working application:
 *  everything the look and feel ships a UI delegate for is on screen, but each one is there
 *  because the workshop needs it.
 *  <p>
 *  Run it with:
 *  <pre>{@code
 *    java -cp <classpath> examples.laf.LinenShowcaseView
 *  }</pre>
 *
 *  @see SwingTreeLookAndFeel
 *  @see AtelierView
 */
public final class LinenShowcaseView
{
    private LinenShowcaseView() {}

    /**
     *  Application entry point: installs the look and feel and opens the atelier in a
     *  SwingTree-managed window.
     *
     *  @param args ignored
     */
    public static void main( String... args ) {
        UI.show("Flaxen — Aspang Weaving Atelier · a Linen look-and-feel showcase", frame -> createView());
        EventProcessor.DECOUPLED.join();
    }

    /**
     *  Installs the look and feel and builds the atelier under its {@link AtelierSheet}. Exists
     *  so that callers outside this package — the resize benchmark among them — can build exactly
     *  the view {@link #main(String...)} shows.
     *
     *  @return the atelier, styled the way the application shows it
     */
    public static JPanel createView() {
        return createView(SwingTreeLookAndFeel.StylePreset.LINEN);
    }

    /**
     *  Installs the look and feel under the given style preset and builds the atelier under its
     *  {@link AtelierSheet}. Exists so that callers outside this package - the resize benchmark
     *  among them - can build the atelier under a preset other than the one
     *  {@link #main(String...)} shows.
     *
     *  @param preset the style preset to install the look and feel under; its
     *                {@linkplain SwingTreeLookAndFeel.StylePreset#preferredSymbols() preferred symbols}
     *                are installed with it
     *  @return the atelier, styled by the given preset
     */
    public static JPanel createView( SwingTreeLookAndFeel.StylePreset preset ) {
        SwingTreeLookAndFeel.initializeUsing( it -> it
            .stylePreset(preset)
            .symbolPreset(preset.preferredSymbols())
        );
        AtelierSheet sheet = new AtelierSheet();
        return UI.use(sheet, () -> new AtelierView(Var.of(AtelierViewModel.initial()), sheet));
    }
}
