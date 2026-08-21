package examples.laf;

import examples.laf.app.AtelierSheet;
import examples.laf.app.AtelierView;
import examples.laf.app.AtelierViewModel;
import sprouts.Var;
import swingtree.UI;
import swingtree.threading.EventProcessor;

import javax.swing.JPanel;
import javax.swing.UIManager;

/**
 *  The showcase of the {@link LinenLookAndFeel}: it installs the look-and-feel
 *  and opens <b>Flaxen</b>, the order book of a small weaving atelier, built
 *  with SwingTree in {@link examples.laf.app}.
 *  <p>
 *  A component gallery — one tab per delegate, a row of buttons, a table of
 *  invented rows — can tell you that a look-and-feel <em>has</em> a check box.
 *  It cannot tell you whether the thing is pleasant to work in: whether a card
 *  reads as raised, whether a tool bar of six commands is calm or noisy, whether
 *  the focus border shifts the layout when you tab through a form, whether a
 *  table of real data is legible at a glance. So this showcase is an
 *  application. Everything Linen ships a UI delegate for is on screen, but each
 *  one is there because the workshop needs it, and it can be used for as long as
 *  you like.
 *  <p>
 *  Run it with:
 *  <pre>{@code
 *    java -cp <classpath> examples.laf.LinenShowcaseView
 *  }</pre>
 *
 *  @see LinenLookAndFeel
 *  @see AtelierView
 */
public final class LinenShowcaseView
{
    private LinenShowcaseView() {}

    /**
     *  Application entry point: installs the {@link LinenLookAndFeel} and opens
     *  the atelier in a SwingTree-managed window.
     *
     *  @param args ignored
     */
    public static void main( String... args ) {
        UI.show("Flaxen — Aspang Weaving Atelier · a Linen look-and-feel showcase", frame -> createView());
        EventProcessor.DECOUPLED.join();
    }

    /**
     *  Installs the {@link LinenLookAndFeel} and builds the atelier under its
     *  {@link AtelierSheet}. Exists so that callers outside this package — the
     *  resize benchmark among them — can build exactly the view
     *  {@link #main(String...)} shows.
     *
     *  @return the atelier, styled the way the application shows it
     */
    public static JPanel createView() {
        try {
            UIManager.setLookAndFeel(new LinenLookAndFeel());
        } catch ( Exception e ) {
            throw new RuntimeException("Could not install LinenLookAndFeel", e);
        }
        AtelierSheet sheet = new AtelierSheet();
        return UI.use(sheet, () -> new AtelierView(Var.of(AtelierViewModel.initial()), sheet));
    }
}
