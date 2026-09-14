package examples.mixer;

import sprouts.From;
import sprouts.Var;
import sprouts.Viewable;
import swingtree.UI;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Window;
import java.util.function.Supplier;

/**
 *  The content of the console's window, which follows the choice in the Look menu.
 *  <p>
 *  The choice is a {@code Var<Look>} that belongs to the application thread like every other
 *  property. When it changes, the new look and feel is installed on the UI thread, on a later turn
 *  of it, so that the menu the user chose from has closed before its delegates are swapped. Then
 *  the console is either refreshed or built anew, depending on
 *  {@link Look#canRefreshComponentsOf(Look)}.
 *  <p>
 *  This panel is also the outermost panel of the window, which matters for the SwingTree look and
 *  feel: it paints the ground of the window, and every panel inside is tagged as a card or as
 *  transparent.
 */
final class LookSwitcher
{
    private final JPanel               host = new JPanel(new BorderLayout());
    private final Supplier<JComponent> console;
    private Look installed;

    LookSwitcher( Var<Look> look, Supplier<JComponent> console ) {
        this.console   = console;
        this.installed = look.get();
        host.add(console.get(), BorderLayout.CENTER);
        Viewable.cast(look).onChange(From.ALL, it ->
            it.currentValue().ifPresent(chosen -> UI.runLater(() -> switchTo(chosen)))
        );
    }

    JComponent host() {
        return host;
    }

    private void switchTo( Look chosen ) {
        if ( chosen == installed )
            return;
        boolean refreshIsEnough = chosen.canRefreshComponentsOf(installed);
        installed = chosen;
        chosen.installLookAndFeel();
        if ( !refreshIsEnough ) {
            host.removeAll();
            host.add(console.get(), BorderLayout.CENTER);
        }
        for ( Window window : Window.getWindows() )
            SwingUtilities.updateComponentTreeUI(window);
        host.revalidate();
        host.repaint();
    }
}
