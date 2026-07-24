package examples.budget.mvi;

import java.awt.Color;

/**
 *  A spending category. Each carries a human label and a distinct brand colour
 *  so a row, a donut slice and a legend entry all read as the same thing at a
 *  glance. The colours are the single source of truth for the whole
 *  infographic: the {@link Budget#donutSvg} chart paints its slices from them,
 *  and {@link BudgetView} tints its legend and combo box from them too.
 *  <p>
 *  Categories are stored in the table as their {@link #label()} string (the
 *  cell value the user actually sees and edits), so {@link #fromLabel(String)}
 *  maps a free-typed or edited cell back to a known category, falling back to
 *  {@link #OTHER} for anything unrecognised.
 */
public enum Category {

    HOUSING  ("Housing",   new Color(0x4C, 0x6F, 0xB1)),   // blue
    FOOD     ("Food",      new Color(0xE0, 0x8A, 0x3C)),   // orange
    TRANSPORT("Transport", new Color(0x2F, 0xA5, 0xA9)),   // teal
    LEISURE  ("Leisure",   new Color(0x8E, 0x6C, 0xC0)),   // violet
    HEALTH   ("Health",    new Color(0xD4, 0x6A, 0x7E)),   // rose
    OTHER    ("Other",     new Color(0x8A, 0x94, 0xA6));   // slate

    private final String label;

    // 'Color' is effectively immutable here (never mutated), so the enum stays a value.
    @SuppressWarnings("ImmutableEnumChecker")
    private final Color color;

    Category(String label, Color color) {
        this.label = label;
        this.color = color;
    }

    public String label() { return label; }
    public Color  color() { return color; }

    /** Resolve a (possibly hand-edited) cell string back to a category, defaulting to {@link #OTHER}. */
    public static Category fromLabel(String label) {
        if (label != null)
            for (Category c : values())
                if (c.label.equalsIgnoreCase(label.trim()))
                    return c;
        return OTHER;
    }
}
