package examples.laf.app;

import java.awt.Color;
import java.util.Locale;

/**
 *  Turns a commission into two pictures of itself, both as plain text values so
 *  that neither needs a component, a cache or a repaint to stay current.
 *  <ul>
 *    <li>{@link #swatchSvg} draws the cloth: a real interlacing of warp and weft
 *        derived from {@link Weave#floats()} and coloured from
 *        {@link Fibre#shade()}. It is handed to a component as
 *        {@code withStyle(svg, (text, it) -> it.image(img -> img.svg(text)))},
 *        so it re-renders crisply at any DPI whenever the order changes, and a
 *        different weave really does look different.</li>
 *    <li>{@link #docketHtml} writes the delivery note the Docket tab shows in a
 *        {@code JEditorPane}. Deliberately plain HTML — Swing's renderer is an
 *        HTML 3.2 engine with a little CSS, not a browser.</li>
 *  </ul>
 *  The class is {@code final} and not instantiable.
 */
final class AtelierArt
{
    private AtelierArt() {}

    /** Threads drawn across the swatch in each direction. */
    private static final int THREADS = 22;

    /**
     *  Weaves a square of cloth as an SVG document: one rectangle per thread
     *  crossing, warp-coloured where the warp thread lies on top and
     *  weft-coloured where the weft does.
     *
     *  @param order the commission whose cloth to draw
     *  @return a standalone SVG document as text
     */
    static String swatchSvg( Order order ) {
        Color  warp   = order.fibre().shade();
        Color  weft   = shade(warp, order.rush() ? 0.74 : 0.84);
        Color  ground = shade(warp, 0.94);
        int    floats = order.weave().floats();
        double cell   = 100.0 / THREADS;

        StringBuilder svg = new StringBuilder(4096);
        svg.append("<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 100 100'>");
        svg.append("<rect x='0' y='0' width='100' height='100' rx='4' fill='").append(hex(ground)).append("'/>");
        for ( int y = 0; y < THREADS; y++ )
            for ( int x = 0; x < THREADS; x++ )
                svg.append(String.format(Locale.US,
                    "<rect x='%.3f' y='%.3f' width='%.3f' height='%.3f' fill='%s'/>",
                    x * cell, y * cell, cell + 0.08, cell + 0.08,
                    hex(warpIsOnTop(order.weave(), floats, x, y) ? warp : weft)));
        // A slub every so often: the flaw that tells hand-woven from machine-made.
        for ( int y = 3; y < THREADS; y += 7 )
            svg.append(String.format(Locale.US,
                "<rect x='0' y='%.3f' width='100' height='%.3f' fill='%s' opacity='0.35'/>",
                y * cell, cell, hex(shade(warp, 1.08))));
        svg.append("<rect x='0.5' y='0.5' width='99' height='99' rx='4' fill='none' stroke='")
           .append(hex(shade(warp, 0.7))).append("' stroke-width='1'/>");
        return svg.append("</svg>").toString();
    }

    /**
     *  Decides which thread lies on top at one crossing. A plain weave simply
     *  alternates; a twill steps the float sideways by one thread per row, which
     *  is what produces its diagonal; a herringbone is a twill that changes its
     *  mind every six threads.
     */
    private static boolean warpIsOnTop( Weave weave, int floats, int x, int y ) {
        if ( floats <= 1 )
            return ( x + y ) % 2 == 0;
        int run = floats + 1;
        if ( weave == Weave.HERRINGBONE ) {
            boolean mirrored = ( y / 6 ) % 2 == 1;
            int     stepped  = mirrored ? x + y : x - y;
            return Math.floorMod(stepped, run) < floats;
        }
        return Math.floorMod(x - y, run) < floats;
    }

    /**
     *  Writes the delivery note that travels with the cloth.
     *
     *  @param order the commission to write up
     *  @return an HTML document Swing's editor kit can render
     */
    static String docketHtml( Order order ) {
        if ( !order.exists() )
            return "<html><body style='font-family:serif;color:#8A7F6A;padding:18px'>"
                 + "<i>Select a commission in the order book to see its delivery note.</i>"
                 + "</body></html>";

        StringBuilder html = new StringBuilder(1024);
        html.append("<html><body style='font-family:serif;color:#3D352A;padding:16px'>");
        html.append("<div style='font-size:9px;letter-spacing:2px;color:#8A7F6A'>FLAXEN · ASPANG WEAVING ATELIER</div>");
        html.append("<h2 style='margin:6px 0 2px 0'>Delivery note #").append(order.ref()).append("</h2>");
        html.append("<div style='color:#8A7F6A'>for ").append(order.client())
            .append(" &middot; due ").append(order.due()).append("</div>");
        html.append("<hr>");
        html.append("<table cellpadding='3' cellspacing='0'>");
        row(html, "Cloth",  order.fibre().label() + " &middot; " + order.weave().label().replace("é", "&eacute;"));
        row(html, "Sett",   order.sett() + " warp threads per cm");
        html.append("<tr><td colspan='2' style='height:4px'></td></tr>");
        row(html, "Length", order.metres() + " m");
        row(html, "Width",  order.widthCm() + " cm");
        row(html, "Edge",   order.finish().label());
        html.append("<tr><td colspan='2' style='height:4px'></td></tr>");
        row(html, "Stage",  order.stage().label());
        row(html, "Total",  "<b>" + order.totalAsMoney().replace("€", "&euro;") + "</b>"
                          + ( order.rush() ? " <i>(rush, +20%)</i>" : "" ));
        html.append("</table>");
        if ( !order.notes().trim().isEmpty() ) {
            html.append("<hr><div style='color:#8A7F6A;font-size:11px'>WEAVER'S NOTES</div>");
            html.append("<div>").append(escape(order.notes()).replace("\n", "<br>")).append("</div>");
        }
        html.append("<hr><div style='color:#8A7F6A;font-size:11px'>")
            .append("Woven by hand. Wash cold, dry flat, press while damp.</div>");
        return html.append("</body></html>").toString();
    }

    private static void row( StringBuilder html, String label, String value ) {
        html.append("<tr><td style='color:#8A7F6A'>").append(label)
            .append("</td><td>").append(value).append("</td></tr>");
    }

    /** Multiplies a colour's brightness, clamping each channel. */
    private static Color shade( Color base, double factor ) {
        return new Color(
            clamp(base.getRed()   * factor),
            clamp(base.getGreen() * factor),
            clamp(base.getBlue()  * factor)
        );
    }

    private static int clamp( double channel ) {
        return (int) Math.max(0, Math.min(255, Math.round(channel)));
    }

    private static String hex( Color color ) {
        return String.format("#%02X%02X%02X", color.getRed(), color.getGreen(), color.getBlue());
    }

    private static String escape( String text ) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
