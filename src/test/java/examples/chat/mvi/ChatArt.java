package examples.chat.mvi;

import sprouts.Tuple;

import java.awt.Color;
import java.util.Locale;

/**
 *  Vector art, generated from the model as plain <b>SVG text</b>.
 *  <p>
 *  This is the trick the budget example uses for its donut chart, and it is
 *  worth stating plainly: an SVG is just a {@link String}, so a picture derived
 *  from your data is an ordinary <em>value</em>. That means it can live in a
 *  {@code Val<String>}, be handed to a component with
 *  {@code withStyle(svgVal, (svg, it) -> it.image(img -> img.svg(svg)))}, and be
 *  re-rendered — crisply, at whatever DPI the screen has — every single time the
 *  data behind it changes. No image files, no rasterisation, no cache to
 *  invalidate.
 *  <p>
 *  Two pictures are generated here:
 *  <ul>
 *    <li>{@link #roomSigil} — a small emblem per room, tinted with the room's
 *        hue and given a different glyph per room, and</li>
 *    <li>{@link #conversationRibbon} — the <i>shape</i> of a conversation: one
 *        bar per recent message, as tall as the message is long, in the colour
 *        of whoever wrote it. It grows while you chat.</li>
 *  </ul>
 */
public final class ChatArt {

    private ChatArt() {}

    // ════════════════════════════════════════════════════════════════════════
    //  Room sigils
    // ════════════════════════════════════════════════════════════════════════

    /**
     *  A 24×24 emblem for a room: a soft disc in the room's washed-out hue with
     *  a glyph on top in the full hue. Which glyph is picked is a pure function
     *  of the room name, so a room keeps its emblem forever.
     *
     * @param room  The room to draw an emblem for.
     * @param p     The palette of the currently active skin.
     * @return Standalone SVG document text, ready for {@code img.svg(..)}.
     */
    public static String roomSigil( Room room, Theme.Palette p ) {
        String tint  = hex(p.hue(room.hue()));
        String wash  = hex(p.hueWash(room.hue()));
        String glyph = glyphFor(Math.abs(room.name().hashCode()) % 4, tint);
        return "<svg xmlns='http://www.w3.org/2000/svg' width='24' height='24' viewBox='0 0 24 24'>"
             +   "<rect x='0' y='0' width='24' height='24' rx='8' fill='" + wash + "'/>"
             +   glyph
             + "</svg>";
    }

    private static String glyphFor( int which, String tint ) {
        switch ( which ) {
            case 0:  // a speech bubble — the generic room
                return "<path fill='" + tint + "' d='M6 5h12a2 2 0 0 1 2 2v7a2 2 0 0 1-2 2h-6l-4 3v-3H6a2 2 0 0 1-2-2V7a2 2 0 0 1 2-2z'/>";
            case 1:  // a leaf — things that grow
                return "<path fill='" + tint + "' d='M19 4c-8 0-13 4-13 10 0 1.6.5 3 1.3 4.1C8.4 12.6 11.6 9.4 16 8c-3.8 2.2-6.2 5.5-6.9 10.4C16.9 19.2 21 13.6 21 6.6c0-1 0-1.8-2-2.6z'/>";
            case 2:  // a spark — announcements and loud opinions
                return "<path fill='" + tint + "' d='M12 3l2 6.6 6.6 2-6.6 2L12 21l-2-7.4-6.6-2 6.6-2z'/>";
            default: // a beetle — the bug hollow
                return "<g fill='none' stroke='" + tint + "' stroke-width='1.7' stroke-linecap='round'>"
                     +   "<path d='M9 6.5 10.6 8M15 6.5 13.4 8'/>"
                     +   "<path d='M5 10h2M17 10h2M4.5 14h2.5M17 14h2.5M6 18l2-1.6M18 18l-2-1.6'/>"
                     + "</g>"
                     + "<ellipse cx='12' cy='13' rx='4.2' ry='5.4' fill='" + tint + "'/>";
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  The conversation ribbon
    // ════════════════════════════════════════════════════════════════════════

    private static final int RIBBON_BARS = 30;
    private static final int RIBBON_W    = 300;
    private static final int RIBBON_H    = 74;

    /**
     *  Draws the last {@value #RIBBON_BARS} messages of a room as a ribbon of
     *  bars: one bar per message, its height set by how much was said and its
     *  colour by who said it. Sending a message visibly extends the ribbon,
     *  which is the point — it is a picture of a live value, not a static asset.
     *
     * @param room  The room whose recent traffic should be drawn.
     * @param p     The palette of the currently active skin.
     * @return Standalone SVG document text, ready for {@code img.svg(..)}.
     */
    public static String conversationRibbon( Room room, Theme.Palette p ) {
        Tuple<Message> all    = room.messages();
        int            from   = Math.max(0, all.size() - RIBBON_BARS);
        int            count  = all.size() - from;
        StringBuilder  svg    = new StringBuilder(1024);

        svg.append("<svg xmlns='http://www.w3.org/2000/svg' width='").append(RIBBON_W)
           .append("' height='").append(RIBBON_H)
           .append("' viewBox='0 0 ").append(RIBBON_W).append(' ').append(RIBBON_H).append("'>");

        // The baseline the bars stand on, in the room's own hue.
        svg.append("<rect x='0' y='").append(RIBBON_H - 3)
           .append("' width='").append(RIBBON_W).append("' height='2' rx='1' opacity='0.35' fill='")
           .append(hex(p.hue(room.hue()))).append("'/>");

        if ( count == 0 ) {
            svg.append("<text x='").append(RIBBON_W / 2).append("' y='").append(RIBBON_H / 2)
               .append("' text-anchor='middle' font-family='sans-serif' font-size='11' fill='")
               .append(hex(p.subtext)).append("'>no traffic yet</text></svg>");
            return svg.toString();
        }

        // The bars always span the full width, however few of them there are —
        // a three-message room gets three wide bars, not three slivers hugging
        // the left edge.
        double slot = (double) RIBBON_W / count;
        double barW = Math.min(16.0, Math.max(3.0, slot - 4.0));
        for ( int i = 0; i < count; i++ ) {
            Message m      = all.get(from + i);
            Member  author = room.author(m.authorId());
            // Longer messages make taller bars, but a shout never leaves the frame.
            double  weight = Math.min(1.0, Math.sqrt(m.text().length()) / 11.0);
            double  h      = 8 + weight * (RIBBON_H - 16);
            double  x      = i * slot + (slot - barW) / 2;
            double  y      = RIBBON_H - 4 - h;
            svg.append("<rect x='").append(round(x)).append("' y='").append(round(y))
               .append("' width='").append(round(barW)).append("' height='").append(round(h))
               .append("' rx='").append(round(barW / 2))
               .append("' fill='").append(hex(p.hue(author.hue())))
               .append("' opacity='").append(round(0.45 + 0.55 * (i + 1.0) / count))
               .append("'/>");
        }
        svg.append("</svg>");
        return svg.toString();
    }

    // ════════════════════════════════════════════════════════════════════════

    /** SVG wants {@code #rrggbb}; transparency is carried separately by an {@code opacity} attribute. */
    private static String hex( Color c ) {
        return String.format(Locale.US, "#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
    }

    private static String round( double value ) {
        return String.format(Locale.US, "%.1f", value);
    }
}
