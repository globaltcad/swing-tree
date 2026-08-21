package examples.laf;

import java.awt.Color;

/**
 *  Central palette of the {@link LinenLookAndFeel} — a small set of warm,
 *  neutral colours sampled to evoke aged paper, raw linen and weathered
 *  taupe stone. All UI delegates in this package read their colours from
 *  here so that an application can re-skin Linen by referencing the same
 *  constants in its own {@link swingtree.style.StyleSheet}.
 *  <p>
 *  The colours form three groups:
 *  <ul>
 *      <li><b>Surfaces</b> — {@link #BACKGROUND}, {@link #SURFACE},
 *          {@link #SURFACE_HOVER}, {@link #SURFACE_PRESSED},
 *          {@link #SURFACE_DISABLED}, {@link #SURFACE_FIELD}.</li>
 *      <li><b>Borders &amp; text</b> — {@link #BORDER}, {@link #BORDER_SOFT},
 *          {@link #TEXT}, {@link #TEXT_MUTED}, {@link #TEXT_DISABLED}.</li>
 *      <li><b>Accent &amp; texture</b> — {@link #ACCENT}, {@link #ACCENT_SOFT},
 *          {@link #TEXTURE_LIGHT}, {@link #TEXTURE_DARK}.</li>
 *  </ul>
 *  <p>
 *  The class is {@code final} and not instantiable — treat it as a
 *  namespaced bundle of constants.
 */
public final class LinenPalette
{
    private LinenPalette() {}

    /** Window background — warm cream, slightly darker than raised surfaces. */
    public static final Color BACKGROUND       = new Color(0xF5, 0xF1, 0xE8);

    /** Default surface colour for buttons and other raised elements. */
    public static final Color SURFACE          = new Color(0xFB, 0xF8, 0xF0);

    /** Surface colour when the pointer is hovering over a component. */
    public static final Color SURFACE_HOVER    = new Color(0xFF, 0xFC, 0xF5);

    /** Surface colour while a component is being pressed or is selected. */
    public static final Color SURFACE_PRESSED  = new Color(0xE8, 0xE2, 0xD4);

    /** Surface colour for disabled components. */
    public static final Color SURFACE_DISABLED = new Color(0xEF, 0xEB, 0xE0);

    /** Surface colour for editable fields — slightly cooler than {@link #SURFACE}. */
    public static final Color SURFACE_FIELD    = new Color(0xFC, 0xFA, 0xF3);

    /** Default border colour — warm taupe. */
    public static final Color BORDER           = new Color(0xC9, 0xC0, 0xAB);

    /** Subtle inner divider colour, lighter than {@link #BORDER}. */
    public static final Color BORDER_SOFT      = new Color(0xDC, 0xD4, 0xBE);

    /** Primary text colour — dark warm brown for high contrast on cream. */
    public static final Color TEXT             = new Color(0x3D, 0x35, 0x2A);

    /** Secondary / muted text colour. */
    public static final Color TEXT_MUTED       = new Color(0x8A, 0x7F, 0x6A);

    /** Disabled text colour. */
    public static final Color TEXT_DISABLED    = new Color(0xB5, 0xAC, 0x9B);

    /** Primary accent — used for focus rings, carets and selected borders. */
    public static final Color ACCENT           = new Color(0x7A, 0x6E, 0x55);

    /** Soft accent — used as a selection background colour. */
    public static final Color ACCENT_SOFT      = new Color(0xD8, 0xCC, 0xAE);

    /** Light noise speck — pairs with {@link #TEXTURE_DARK} to produce the
     *  faint, fabric-like graininess on panels. */
    public static final Color TEXTURE_LIGHT    = new Color(0xF9, 0xF5, 0xEC);

    /** Dark noise speck — see {@link #TEXTURE_LIGHT}. */
    public static final Color TEXTURE_DARK     = new Color(0xF0, 0xEC, 0xE3);

    /** Filled surface of a {@link LinenVariant#PRIMARY} control — deep moss. */
    public static final Color PRIMARY          = new Color(0x36, 0x5C, 0x3B);

    /** {@link #PRIMARY} with the pointer over it. */
    public static final Color PRIMARY_HOVER    = new Color(0x41, 0x6B, 0x46);

    /** {@link #PRIMARY} while pressed or selected. */
    public static final Color PRIMARY_PRESSED  = new Color(0x2B, 0x4A, 0x30);

    /** Filled surface of a {@link LinenVariant#DANGER} control — faded brick. */
    public static final Color DANGER           = new Color(0x8B, 0x3A, 0x3A);

    /** {@link #DANGER} with the pointer over it. */
    public static final Color DANGER_HOVER     = new Color(0x9C, 0x45, 0x45);

    /** {@link #DANGER} while pressed or selected. */
    public static final Color DANGER_PRESSED   = new Color(0x74, 0x2E, 0x2E);

    /** Text laid over {@link #PRIMARY} or {@link #DANGER} — the cream of the
     *  window background, so a filled control reads as a hole punched in it. */
    public static final Color ON_FILLED        = new Color(0xFA, 0xF6, 0xEC);

    /** Fully transparent — what a {@link LinenVariant#QUIET} control paints
     *  instead of a surface, so whatever it sits on shows through untouched. */
    public static final Color TRANSPARENT      = new Color(0, 0, 0, 0);
}