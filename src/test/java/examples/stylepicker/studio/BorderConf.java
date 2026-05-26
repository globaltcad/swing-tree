package examples.stylepicker.studio;

import org.jspecify.annotations.Nullable;
import swingtree.UI;

import java.awt.Color;
import java.util.EnumMap;
import java.util.Map;

/**
 *  A border configuration with <b>independent width &amp; colour per
 *  {@link UI.Edge}</b> and an <b>independent arc per {@link UI.Corner}</b> —
 *  the level of control the original {@code BoxShadowPicker} had and the flat
 *  first-draft border was missing.
 *
 *  <p>Like that picker, editing is mediated by a <i>focus</i>: pick
 *  {@code EVERY} to change all four sides (or corners) at once, or pick a single
 *  side/corner to fine-tune it. The {@code focusedXxx} getters/withers below are
 *  what the editor's lenses bind to, so one slider transparently edits "all" or
 *  "just this one" depending on the focus.</p>
 */
public final class BorderConf {

    private static final UI.Edge[]   EDGES   = { UI.Edge.TOP, UI.Edge.RIGHT, UI.Edge.BOTTOM, UI.Edge.LEFT };
    private static final UI.Corner[] CORNERS = { UI.Corner.TOP_LEFT, UI.Corner.TOP_RIGHT, UI.Corner.BOTTOM_LEFT, UI.Corner.BOTTOM_RIGHT };

    private final UI.Edge                  edgeFocus;
    private final UI.Corner                cornerFocus;
    private final Map<UI.Edge, EdgeLine>   edges;
    private final Map<UI.Corner, CornerArc> corners;

    private BorderConf(UI.Edge edgeFocus, UI.Corner cornerFocus,
                       Map<UI.Edge, EdgeLine> edges, Map<UI.Corner, CornerArc> corners) {
        this.edgeFocus = edgeFocus;
        this.cornerFocus = cornerFocus;
        this.edges = edges;       // private + copy-on-write → effectively immutable
        this.corners = corners;
    }

    // ── Factories ────────────────────────────────────────────────────────────

    public static BorderConf none() {
        EnumMap<UI.Edge, EdgeLine> e = new EnumMap<>(UI.Edge.class);
        for (UI.Edge edge : EDGES) e.put(edge, EdgeLine.none());
        EnumMap<UI.Corner, CornerArc> c = new EnumMap<>(UI.Corner.class);
        for (UI.Corner corner : CORNERS) c.put(corner, CornerArc.none());
        return new BorderConf(UI.Edge.EVERY, UI.Corner.EVERY, e, c);
    }

    public static BorderConf uniform(int width, @Nullable Color color, int radius) {
        EnumMap<UI.Edge, EdgeLine> e = new EnumMap<>(UI.Edge.class);
        for (UI.Edge edge : EDGES) e.put(edge, new EdgeLine(width, color));
        EnumMap<UI.Corner, CornerArc> c = new EnumMap<>(UI.Corner.class);
        for (UI.Corner corner : CORNERS) c.put(corner, CornerArc.of(radius));
        return new BorderConf(UI.Edge.EVERY, UI.Corner.EVERY, e, c);
    }

    /** A border touching only one edge (used e.g. for a list-row bottom divider). */
    public static BorderConf edge(UI.Edge which, int width, @Nullable Color color) {
        BorderConf b = none();
        Map<UI.Edge, EdgeLine> e = new EnumMap<>(b.edges);
        e.put(which, new EdgeLine(width, color));
        return new BorderConf(which, UI.Corner.EVERY, e, b.corners);
    }

    // ── Focus ─────────────────────────────────────────────────────────────────

    public UI.Edge   edgeFocus()   { return edgeFocus; }
    public UI.Corner cornerFocus() { return cornerFocus; }
    public BorderConf withEdgeFocus(UI.Edge f)   { return new BorderConf(f, cornerFocus, edges, corners); }
    public BorderConf withCornerFocus(UI.Corner f) { return new BorderConf(edgeFocus, f, edges, corners); }

    // ── Focused edge width / colour (EVERY ⇒ all four) ──────────────────────────

    public int focusedWidth() {
        return lineAt(edgeFocus == UI.Edge.EVERY ? UI.Edge.TOP : edgeFocus).width();
    }
    public BorderConf withFocusedWidth(int w) {
        EnumMap<UI.Edge, EdgeLine> e = new EnumMap<>(edges);
        for (UI.Edge edge : targetEdges()) e.put(edge, lineAt(edge).withWidth(w));
        return new BorderConf(edgeFocus, cornerFocus, e, corners);
    }

    public @Nullable Color focusedColor() {
        return lineAt(edgeFocus == UI.Edge.EVERY ? UI.Edge.TOP : edgeFocus).color();
    }
    public BorderConf withFocusedColor(@Nullable Color c) {
        EnumMap<UI.Edge, EdgeLine> e = new EnumMap<>(edges);
        for (UI.Edge edge : targetEdges()) e.put(edge, lineAt(edge).withColor(c));
        return new BorderConf(edgeFocus, cornerFocus, e, corners);
    }

    // ── Focused corner arc (EVERY ⇒ all four) ───────────────────────────────────

    public int focusedArcWidth() {
        return arcAt(cornerFocus == UI.Corner.EVERY ? UI.Corner.TOP_LEFT : cornerFocus).width();
    }
    public BorderConf withFocusedArcWidth(int w) {
        EnumMap<UI.Corner, CornerArc> c = new EnumMap<>(corners);
        for (UI.Corner corner : targetCorners()) c.put(corner, arcAt(corner).withWidth(w));
        return new BorderConf(edgeFocus, cornerFocus, edges, c);
    }

    public int focusedArcHeight() {
        return arcAt(cornerFocus == UI.Corner.EVERY ? UI.Corner.TOP_LEFT : cornerFocus).height();
    }
    public BorderConf withFocusedArcHeight(int h) {
        EnumMap<UI.Corner, CornerArc> c = new EnumMap<>(corners);
        for (UI.Corner corner : targetCorners()) c.put(corner, arcAt(corner).withHeight(h));
        return new BorderConf(edgeFocus, cornerFocus, edges, c);
    }

    // ── Raw accessors used by the applier / code generator ─────────────────────

    public int   widthAt(UI.Edge e)    { return lineAt(e).width(); }
    public Color colorAt(UI.Edge e)    { return lineAt(e).color(); }
    public int   arcWidthAt(UI.Corner c)  { return arcAt(c).width(); }
    public int   arcHeightAt(UI.Corner c) { return arcAt(c).height(); }

    public boolean anyWidth()  { for (UI.Edge e : EDGES)   if (lineAt(e).width() > 0) return true; return false; }
    public boolean anyArc()    { for (UI.Corner c : CORNERS) if (arcAt(c).width() > 0 || arcAt(c).height() > 0) return true; return false; }
    public boolean isSet()     { return anyWidth() || anyArc(); }

    public boolean widthsUniform() {
        int w = widthAt(UI.Edge.TOP);
        for (UI.Edge e : EDGES) if (widthAt(e) != w) return false;
        return true;
    }
    public boolean colorsUniform() {
        Color c = colorAt(UI.Edge.TOP);
        for (UI.Edge e : EDGES) if (!java.util.Objects.equals(colorAt(e), c)) return false;
        return true;
    }
    public boolean cornersUniform() {
        CornerArc a = arcAt(UI.Corner.TOP_LEFT);
        for (UI.Corner c : CORNERS) if (arcWidthAt(c) != a.width() || arcHeightAt(c) != a.height()) return false;
        return true;
    }

    // ── internals ──────────────────────────────────────────────────────────────

    private EdgeLine lineAt(UI.Edge e)   { return edges.getOrDefault(e, EdgeLine.none()); }
    private CornerArc arcAt(UI.Corner c) { return corners.getOrDefault(c, CornerArc.none()); }

    private UI.Edge[] targetEdges() {
        return edgeFocus == UI.Edge.EVERY ? EDGES : new UI.Edge[]{ edgeFocus };
    }
    private UI.Corner[] targetCorners() {
        return cornerFocus == UI.Corner.EVERY ? CORNERS : new UI.Corner[]{ cornerFocus };
    }
}