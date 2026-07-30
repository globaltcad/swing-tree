package examples.chat.mvi;

import java.awt.Color;

/**
 *  The two skins of the Treehouse, each carrying a complete {@link Palette}.
 *  <p>
 *  The interesting part is {@link Palette#hue(int)}: rooms and members store a
 *  hue <b>angle</b> rather than a colour, and the palette turns that angle into
 *  paint. So one member is a soft pastel on paper and a luminous neon at night,
 *  from a single number in the view model — and adding a third skin later would
 *  not touch a single model class.
 */
public enum Theme {

    LIGHT(new Palette(
        /* page       */ new Color(0xF2, 0xF4, 0xF1),
        /* card       */ new Color(0xFF, 0xFF, 0xFF),
        /* raised     */ new Color(0xF6, 0xF8, 0xF5),
        /* border     */ new Color(0xE1, 0xE6, 0xDE),
        /* text       */ new Color(0x1C, 0x24, 0x1E),
        /* subtext    */ new Color(0x69, 0x74, 0x6C),
        /* accent     */ new Color(0x2E, 0x8B, 0x57),
        /* onAccent   */ new Color(0xFF, 0xFF, 0xFF),
        /* mine       */ new Color(0xDE, 0xF3, 0xE4),
        /* theirs     */ new Color(0xF4, 0xF6, 0xF3),
        /* online     */ new Color(0x2E, 0x9E, 0x5B),
        /* away       */ new Color(0xC8, 0x8A, 0x18),
        /* offline    */ new Color(0xA3, 0xAC, 0xA6),
        /* shadow     */ new Color(0x20, 0x30, 0x22, 30),
        /* grain      */ new Color(0x3A, 0x4A, 0x36, 13),
        /* glow       */ new Color(0x8F, 0xD6, 0xA8, 70),
        false
    )),

    DARK(new Palette(
        /* page       */ new Color(0x12, 0x16, 0x14),
        /* card       */ new Color(0x1B, 0x21, 0x1D),
        /* raised     */ new Color(0x23, 0x2A, 0x25),
        /* border     */ new Color(0x2E, 0x37, 0x30),
        /* text       */ new Color(0xE7, 0xEE, 0xE8),
        /* subtext    */ new Color(0x94, 0xA2, 0x99),
        /* accent     */ new Color(0x5D, 0xD6, 0x92),
        /* onAccent   */ new Color(0x0D, 0x1A, 0x12),
        /* mine       */ new Color(0x1E, 0x3A, 0x2A),
        /* theirs     */ new Color(0x22, 0x29, 0x24),
        /* online     */ new Color(0x63, 0xDD, 0x95),
        /* away       */ new Color(0xE3, 0xB3, 0x4A),
        /* offline    */ new Color(0x63, 0x6F, 0x67),
        /* shadow     */ new Color(0x00, 0x00, 0x00, 130),
        /* grain      */ new Color(0xC9, 0xF7, 0xD8, 12),
        /* glow       */ new Color(0x2A, 0x7E, 0x55, 110),
        true
    ));

    // 'Palette' is a final class holding only final fields, so it is effectively immutable.
    @SuppressWarnings("ImmutableEnumChecker")
    private final Palette palette;

    Theme( Palette palette ) { this.palette = palette; }

    public Palette palette() { return palette; }

    public Theme toggled() { return this == LIGHT ? DARK : LIGHT; }

    public boolean isDark() { return this == DARK; }

    /** A flat colour bundle shared by the style sheet and every inline {@code withStyle}. */
    public static final class Palette {

        public final Color page, card, raised, border,
                           text, subtext,
                           accent, onAccent,
                           mine, theirs,
                           online, away, offline,
                           shadow, grain, glow;
        public final boolean dark;

        Palette(
            Color page, Color card, Color raised, Color border,
            Color text, Color subtext, Color accent, Color onAccent,
            Color mine, Color theirs,
            Color online, Color away, Color offline,
            Color shadow, Color grain, Color glow, boolean dark
        ) {
            this.page = page;       this.card = card;       this.raised = raised;
            this.border = border;   this.text = text;       this.subtext = subtext;
            this.accent = accent;   this.onAccent = onAccent;
            this.mine = mine;       this.theirs = theirs;
            this.online = online;   this.away = away;       this.offline = offline;
            this.shadow = shadow;   this.grain = grain;     this.glow = glow;
            this.dark = dark;
        }

        /** Turns a stored hue angle (0..359) into a readable accent for this skin. */
        public Color hue( int hueAngle ) {
            float h = ((hueAngle % 360) + 360) % 360 / 360f;
            return dark ? Color.getHSBColor(h, 0.55f, 0.92f)
                        : Color.getHSBColor(h, 0.68f, 0.68f);
        }

        /** The same hue, dialled down to something you can lay text on top of. */
        public Color hueWash( int hueAngle ) {
            float h = ((hueAngle % 360) + 360) % 360 / 360f;
            return dark ? Color.getHSBColor(h, 0.42f, 0.24f)
                        : Color.getHSBColor(h, 0.14f, 0.98f);
        }

        public Color presence( Presence presence ) {
            switch ( presence ) {
                case ONLINE: return online;
                case AWAY:   return away;
                default:     return offline;
            }
        }

        /** Blend towards another colour; {@code t} in [0,1] moves from this to {@code b}. */
        public static Color mix( Color a, Color b, double t ) {
            double f = Math.max(0, Math.min(1, t));
            return new Color(
                (int) Math.round(a.getRed()   + (b.getRed()   - a.getRed())   * f),
                (int) Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * f),
                (int) Math.round(a.getBlue()  + (b.getBlue()  - a.getBlue())  * f),
                a.getAlpha());
        }

        public Color withAlpha( Color base, int alpha ) {
            return new Color(base.getRed(), base.getGreen(), base.getBlue(), alpha);
        }
    }
}
