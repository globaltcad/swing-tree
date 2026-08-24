package examples.laf;

import javax.swing.UIManager;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.util.Map;

/**
 *  Enables text antialiasing on the {@link Graphics2D} contexts the UI delegates hand to their
 *  inherited {@code BasicXxxUI.paint} implementations.
 *  <p>
 *  Swing's {@code SwingUtilities2.drawString} (used by every {@code BasicXxxUI}) reads its
 *  antialiasing decision from two places: the component's package-private
 *  {@code AA_TEXT_PROPERTY_KEY} client property, and — when that is absent — the rendering
 *  hints already installed on the {@link Graphics2D}. The first key is not exposed outside
 *  {@code javax.swing}, so a third-party look and feel can only influence the second. This
 *  helper does exactly that by pulling the current desktop font hints from the {@link Toolkit}
 *  (the same hints the system look and feel uses) and merging them onto the graphics context.
 *  It falls back to a conservative explicit {@code TEXT_ANTIALIAS_ON} if no desktop hints are
 *  available — when the JVM was launched headless, say, or with
 *  {@code -Dawt.useSystemAAFontSettings=off}.
 */
final class LafPaint
{
    private LafPaint() {}

    /**
     *  Adds high-quality text and geometry antialiasing hints to the given {@link Graphics2D}.
     *  Safe to call multiple times — {@link Graphics2D#addRenderingHints} merges into the
     *  existing map, it does not replace it.
     *
     * @param g the graphics context about to be handed to an inherited paint implementation
     */
    static void applyAaHints( Graphics2D g ) {
        Map<?, ?> desktopHints = desktopFontHints();
        if ( desktopHints != null && !desktopHints.isEmpty() ) {
            g.addRenderingHints(desktopHints);
        } else {
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                               RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,
                               RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        }
        // Stroke / fill antialiasing for the small amount of geometric drawing BasicXxxUI does
        // itself (focus rectangles, check marks, ...).
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                           RenderingHints.VALUE_ANTIALIAS_ON);
    }

    /**
     *  Returns the AWT desktop font hint map, or {@code null} if it is not available. The map
     *  is keyed by {@link RenderingHints} keys and describes the system's preferred subpixel
     *  layout, gamma and so on, so that the look and feel's text matches the rest of the desktop.
     */
    private static Map<?, ?> desktopFontHints() {
        // Prefer the cached value Swing already keeps in UIManager — it accounts for laptop /
        // multi-monitor differences. Fall back to the raw Toolkit property if the look and feel
        // did not install one.
        Object fromManager = UIManager.get("AwtFontDesktopHints");
        if ( fromManager instanceof Map<?, ?> )
            return (Map<?, ?>) fromManager;
        Object fromToolkit = Toolkit.getDefaultToolkit().getDesktopProperty("awt.font.desktophints");
        return ( fromToolkit instanceof Map<?, ?> ) ? (Map<?, ?>) fromToolkit : null;
    }
}
