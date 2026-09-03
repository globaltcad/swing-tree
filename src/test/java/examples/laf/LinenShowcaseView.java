package examples.laf;

import examples.laf.app.AtelierSheet;
import examples.laf.app.AtelierView;
import examples.laf.app.AtelierViewModel;
import sprouts.Var;
import swingtree.UI;
import swingtree.threading.EventProcessor;

import javax.swing.JPanel;

/**
 *  The showcase of {@link SwingTreeLookAndFeel}: it installs the look and feel under its
 *  {@linkplain SwingTreeLookAndFeel.StylePreset#LINEN Linen} preset and opens <b>Flaxen</b>, the
 *  order book of a small weaving atelier, built with SwingTree in {@link examples.laf.app}.
 *  <p>
 *  It is a working application rather than a gallery of controls, because a gallery shows that the
 *  look and feel has a check box without showing whether it is pleasant to work in. Every
 *  component the look and feel ships a UI delegate for is on screen, and each one is there because
 *  the atelier needs it.
 *
 *  @see AtelierView
 */
public final class LinenShowcaseView
{
    private LinenShowcaseView() {}

    /** @param args ignored */
    public static void main( String... args ) {
        UI.show("Flaxen — Aspang Weaving Atelier · a Linen look-and-feel showcase", frame -> createView());
        EventProcessor.DECOUPLED.join();
    }

    /** @return the atelier under the Linen preset, which is the view {@link #main(String...)} shows. */
    public static JPanel createView() {
        return createView(SwingTreeLookAndFeel.StylePreset.LINEN);
    }

    /**
     *  Installs the look and feel under {@code preset} and its
     *  {@linkplain SwingTreeLookAndFeel.StylePreset#preferredSymbols() preferred symbols}, then
     *  builds the atelier under its {@link AtelierSheet}. The resize benchmark calls this to build
     *  the atelier under a preset other than Linen.
     *
     *  @param preset the style preset to install
     *  @return the atelier, styled by that preset
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
