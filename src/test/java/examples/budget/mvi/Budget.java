package examples.budget.mvi;

import swingtree.api.model.TableData;

import java.awt.Color;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Locale;

/**
 *  The Swing-free domain layer of the budget planner: pure functions that turn
 *  the table's {@link TableData} value into totals, and — most notably — into a
 *  complete <b>SVG donut chart</b> as a plain {@link String}.
 *  <p>
 *  Nothing here knows about Swing or SwingTree components. {@link #donutSvg}
 *  simply builds an SVG document from the numbers, and {@link BudgetView} feeds
 *  the resulting text straight into a component through the value-capturing
 *  {@code withStyle(svgText, (svg, it) -> it.image(img -> img.svg(svg)))} style —
 *  so the entire infographic is regenerated and repainted, crisply at any DPI,
 *  every time an expense is edited or added.
 */
public final class Budget {

    public static final String COL_CATEGORY = "Category";
    public static final String COL_DETAIL   = "Detail";
    public static final String COL_AMOUNT   = "Amount (€)";

    private Budget() {}

    // ── Totals ─────────────────────────────────────────────────────────────

    /** Sum of every expense amount in the table. */
    public static double totalOf(TableData data) {
        int col = data.indexOfColumn(COL_AMOUNT);
        double sum = 0;
        for (int r = 0; r < data.getRowCount(); r++)
            sum += toDouble(data.getValueAt(r, col));
        return sum;
    }

    /** Spending per category, in enum order, including categories with a zero total. */
    public static Map<Category, Double> totalsByCategory(TableData data) {
        Map<Category, Double> totals = new LinkedHashMap<>();
        for (Category c : Category.values())
            totals.put(c, 0.0);
        int catCol = data.indexOfColumn(COL_CATEGORY);
        int amtCol = data.indexOfColumn(COL_AMOUNT);
        for (int r = 0; r < data.getRowCount(); r++) {
            Category c = Category.fromLabel(String.valueOf(data.getValueAt(r, catCol)));
            totals.merge(c, toDouble(data.getValueAt(r, amtCol)), Double::sum);
        }
        return totals;
    }

    /** A cell value coerced to a double, tolerating both {@link Number}s and edited strings. */
    public static double toDouble(Object cell) {
        if (cell instanceof Number) return ((Number) cell).doubleValue();
        if (cell == null) return 0;
        String s = cell.toString().trim().replace("€", "").replace(",", "").trim();
        if (s.isEmpty()) return 0;
        try { return Double.parseDouble(s); }
        catch (NumberFormatException e) { return 0; }
    }

    /** A euro amount, rounded, with a thousands separator, e.g. {@code €1,234} — for the chart and banner. */
    public static String money(double v) {
        return String.format(Locale.US, "€%,.0f", v);
    }

    /** A euro amount with cents, e.g. {@code €68.40} — for the table's Amount column. */
    public static String moneyCents(double v) {
        return String.format(Locale.US, "€%,.2f", v);
    }

    // ── The SVG donut chart, generated from the numbers ────────────────────

    private static final int    W = 300, H = 380;     // view box
    private static final double  CX = 150, CY = 140;   // donut centre
    private static final double  R = 92;               // stroke-centre radius
    private static final double  THICK = 40;           // ring thickness
    private static final double  C = 2 * Math.PI * R;  // circumference

    private static final Color TRACK   = new Color(0xE8, 0xEC, 0xF2);
    private static final Color INK     = new Color(0x1E, 0x24, 0x30);
    private static final Color SUBTEXT = new Color(0x6B, 0x74, 0x86);

    /** Build a full SVG donut-plus-legend document describing the given table's spending. */
    public static String donutSvg(TableData data) {
        Map<Category, Double> totals = totalsByCategory(data);
        double total = 0;
        for (double v : totals.values()) total += v;

        StringBuilder svg = new StringBuilder(2048);
        svg.append("<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 ").append(W).append(' ').append(H).append("'>");

        if (total <= 0) {
            // Empty state: a single muted ring and a gentle hint.
            ring(svg, TRACK);
            centre(svg, money(0), "nothing spent yet");
            text(svg, W / 2.0, 268, 13, SUBTEXT, "middle", "Add an expense below to");
            text(svg, W / 2.0, 288, 13, SUBTEXT, "middle", "see where your money goes.");
            return svg.append("</svg>").toString();
        }

        // Track ring underneath, then one dashed arc per non-empty category.
        ring(svg, TRACK);
        double startFraction = 0;
        for (Map.Entry<Category, Double> e : totals.entrySet()) {
            double value = e.getValue();
            if (value <= 0) continue;
            double fraction = value / total;
            double dash = Math.max(0, fraction * C - 2.5);   // tiny gap between slices
            arc(svg, e.getKey().color(), dash, startFraction * C);
            startFraction += fraction;
        }
        centre(svg, money(total), "spent this month");

        // Legend rows, one per non-empty category, aligned like a receipt.
        double y = 252;
        for (Map.Entry<Category, Double> e : totals.entrySet()) {
            double value = e.getValue();
            if (value <= 0) continue;
            Category c = e.getKey();
            svg.append(String.format(Locale.US,
                "<rect x='40' y='%.1f' width='13' height='13' rx='3' fill='%s'/>",
                y, hex(c.color())));
            text(svg, 64, y + 11, 13, INK, "start", c.label());
            text(svg, 260, y + 11, 13, SUBTEXT, "end",
                money(value) + "  ·  " + Math.round(value / total * 100) + "%");
            y += 25;
        }
        return svg.append("</svg>").toString();
    }

    /** A full background ring in one colour. */
    private static void ring(StringBuilder svg, Color color) {
        svg.append(String.format(Locale.US,
            "<circle cx='%.1f' cy='%.1f' r='%.1f' fill='none' stroke='%s' stroke-width='%.1f'/>",
            CX, CY, R, hex(color), THICK));
    }

    /** One donut slice, drawn as a dashed circle rotated so 0° sits at the top. */
    private static void arc(StringBuilder svg, Color color, double dash, double startLen) {
        svg.append(String.format(Locale.US,
            "<circle cx='%.1f' cy='%.1f' r='%.1f' fill='none' stroke='%s' stroke-width='%.1f' " +
            "stroke-dasharray='%.2f %.2f' stroke-dashoffset='%.2f' " +
            "transform='rotate(-90 %.1f %.1f)'/>",
            CX, CY, R, hex(color), THICK, dash, C - dash, -startLen, CX, CY));
    }

    /** The two lines of text in the hole of the donut. */
    private static void centre(StringBuilder svg, String big, String small) {
        text(svg, CX, CY + 4, 30, INK, "middle", big);
        text(svg, CX, CY + 26, 12, SUBTEXT, "middle", small);
    }

    private static void text(StringBuilder svg, double x, double y, int size, Color color, String anchor, String content) {
        svg.append(String.format(Locale.US,
            "<text x='%.1f' y='%.1f' font-family='sans-serif' font-size='%d' fill='%s' " +
            "text-anchor='%s'>%s</text>",
            x, y, size, hex(color), anchor, content));
    }

    private static String hex(Color c) {
        return String.format("#%02X%02X%02X", c.getRed(), c.getGreen(), c.getBlue());
    }
}
