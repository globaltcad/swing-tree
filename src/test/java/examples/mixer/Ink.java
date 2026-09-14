package examples.mixer;

import examples.laf.SwingTreeLookAndFeel;

import javax.swing.UIManager;
import java.awt.Color;

/**
 *  The colours of the console, read from whatever look and feel is installed at the moment of
 *  painting.
 *  <p>
 *  The console can switch its look and feel while it runs, so no style may remember a colour.
 *  Every style asks this class when it is evaluated, which SwingTree does before every paint,
 *  and gets the colour of the look and feel that is active right then: the palette of the
 *  {@link SwingTreeLookAndFeel} when that is installed, and the {@link UIManager} defaults
 *  of FlatLaf, Metal or Nimbus otherwise. The three meter colours are the exception. Green,
 *  amber and red mean the same thing on every mixing desk in the world.
 *  <p>
 *  A colour read from the {@link UIManager} is a {@link javax.swing.plaf.ColorUIResource}, and it
 *  must not be handed to a style as it is. The style engine installs the colours of a style on the
 *  component, and the next {@code updateUI()} replaces every installed colour that is marked as a
 *  {@code UIResource} with the look and feel's default. A heading styled in the accent colour of
 *  FlatLaf would turn black the first time the component tree is refreshed, and stay black,
 *  because its style did not change. So every colour leaves this class as a plain {@link Color}.
 */
final class Ink
{
    static final Color METER_GREEN = new Color(0x2F, 0xBF, 0x71);
    static final Color METER_AMBER = new Color(0xF2, 0xB1, 0x38);
    static final Color METER_RED   = new Color(0xE5, 0x48, 0x4D);

    private Ink() {}

    static boolean swingTreeIsActive() {
        return UIManager.getLookAndFeel() instanceof SwingTreeLookAndFeel;
    }

    static Color background() {
        if ( swingTreeIsActive() )
            return _plain(SwingTreeLookAndFeel.palette().background());
        return _ui("Panel.background", new Color(0xEE, 0xEE, 0xEE));
    }

    static Color text() {
        if ( swingTreeIsActive() )
            return _plain(SwingTreeLookAndFeel.palette().text());
        return _ui("Label.foreground", new Color(0x22, 0x22, 0x22));
    }

    static Color muted() {
        if ( swingTreeIsActive() )
            return _plain(SwingTreeLookAndFeel.palette().textMuted());
        return mix(text(), background(), 0.42);
    }

    /**
     *  The accent of the look and feel, darkened or lightened towards the text colour when it would
     *  be hard to read on the background. Metal has no accent colour of its own, and its closest
     *  candidate, the pale blue of {@code textHighlight}, nearly disappears on Metal's light grey.
     */
    static Color accent() {
        if ( swingTreeIsActive() )
            return _plain(SwingTreeLookAndFeel.palette().accent());
        Color accent = _ui("Component.accentColor", _ui("nimbusFocus", _ui("textHighlight", new Color(0x3D, 0x8B, 0xFD))));
        return Math.abs(_luma(accent) - _luma(background())) >= 60 ? accent : mix(accent, text(), 0.45);
    }

    static Color card() {
        return mix(background(), text(), isDark() ? 0.05 : 0.025);
    }

    static Color hairline() {
        if ( swingTreeIsActive() )
            return _plain(SwingTreeLookAndFeel.palette().borderSoft());
        return mix(background(), text(), 0.16);
    }

    static Color well() {
        return mix(background(), Color.BLACK, isDark() ? 0.35 : 0.10);
    }

    static boolean isDark() {
        return _luma(background()) < 128;
    }

    private static double _luma( Color c ) {
        return 0.299 * c.getRed() + 0.587 * c.getGreen() + 0.114 * c.getBlue();
    }

    static Color mix( Color a, Color b, double t ) {
        double f = Math.max(0, Math.min(1, t));
        return new Color(
                (int) Math.round(a.getRed()   + (b.getRed()   - a.getRed())   * f),
                (int) Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * f),
                (int) Math.round(a.getBlue()  + (b.getBlue()  - a.getBlue())  * f)
            );
    }

    static Color withAlpha( Color c, int alpha ) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.max(0, Math.min(255, alpha)));
    }

    private static Color _ui( String key, Color fallback ) {
        Color color = UIManager.getColor(key);
        return color != null ? _plain(color) : fallback;
    }

    private static Color _plain( Color color ) {
        return color.getClass() == Color.class ? color : new Color(color.getRGB(), true);
    }
}
