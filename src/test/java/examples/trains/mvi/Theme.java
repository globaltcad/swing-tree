package examples.trains.mvi;

import java.awt.Color;

/**
 *  The two visual themes. Each carries a full {@link Palette} so the
 *  {@link TrainStyle} style sheet (and the route/board cards) can paint
 *  themselves purely as a function of the selected theme, while a matching
 *  FlatLaf base look-and-feel handles the stock widgets underneath.
 */
public enum Theme {

    LIGHT(new Palette(
        new Color(0xF1, 0xF3, 0xF6),   // page
        new Color(0xFF, 0xFF, 0xFF),   // card
        new Color(0xF7, 0xF8, 0xFA),   // row
        new Color(0xD4, 0x00, 0x2A),   // accent  (ÖBB-ish red)
        new Color(0xFF, 0xFF, 0xFF),   // onAccent
        new Color(0x1A, 0x1D, 0x24),   // text
        new Color(0x6B, 0x72, 0x80),   // subtext
        new Color(0xE3, 0xE6, 0xEB),   // border
        new Color(0xFF, 0xF3, 0xCF),   // currentStop highlight
        new Color(0x14, 0x7A, 0x4B)    // onTime green
    )),

    DARK(new Palette(
        new Color(0x15, 0x17, 0x1C),   // page
        new Color(0x21, 0x24, 0x2C),   // card
        new Color(0x1A, 0x1D, 0x24),   // row
        new Color(0xFF, 0x5A, 0x76),   // accent
        new Color(0x16, 0x18, 0x1D),   // onAccent
        new Color(0xE9, 0xEC, 0xF1),   // text
        new Color(0x9A, 0xA1, 0xAD),   // subtext
        new Color(0x2E, 0x33, 0x3D),   // border
        new Color(0x3D, 0x35, 0x1C),   // currentStop highlight
        new Color(0x49, 0xC9, 0x83)    // onTime green
    ));

    // 'Palette' is a final class holding only final 'Color' fields, so it is effectively immutable.
    @SuppressWarnings("ImmutableEnumChecker")
    private final Palette palette;
    Theme(Palette palette) { this.palette = palette; }
    public Palette palette() { return palette; }

    public Theme toggled() { return this == LIGHT ? DARK : LIGHT; }
    public boolean isDark() { return this == DARK; }

    /** A flat colour bundle shared by the style sheet and the per-card styling. */
    public static final class Palette {
        public final Color page, card, row, accent, onAccent, text, subtext, border, currentStop, onTime;
        Palette(Color page, Color card, Color row, Color accent, Color onAccent,
                Color text, Color subtext, Color border, Color currentStop, Color onTime) {
            this.page = page; this.card = card; this.row = row;
            this.accent = accent; this.onAccent = onAccent;
            this.text = text; this.subtext = subtext; this.border = border;
            this.currentStop = currentStop; this.onTime = onTime;
        }

        /** Brand colour for a train line, by MOTIS mode, so different services read at a glance. */
        public Color modeColor(String mode) {
            switch (mode) {
                case "HIGHSPEED_RAIL":
                case "LONG_DISTANCE":   return new Color(0xD4, 0x00, 0x2A); // RJ / IC – red
                case "NIGHT_RAIL":      return new Color(0x6A, 0x4C, 0xC0); // NJ – violet
                case "REGIONAL_RAIL":   return new Color(0x16, 0x8A, 0x5E); // REX / R – green
                case "SUBURBAN":        return new Color(0x1C, 0x6F, 0xC9); // S-Bahn – blue
                case "BUS":             return new Color(0xB5, 0x6A, 0x00); // bus – amber
                default:                return new Color(0x55, 0x5C, 0x66);
            }
        }
    }
}