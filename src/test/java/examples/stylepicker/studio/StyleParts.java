package examples.stylepicker.studio;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.With;
import lombok.experimental.Accessors;
import org.jspecify.annotations.Nullable;
import swingtree.UI;

import java.awt.Color;

/*
 *  The immutable "style parts" — one tiny value class per styling aspect.
 *
 *  Splitting GroupStyle into these aspects is what keeps the lens chain in the
 *  view short and readable: the editor zooms
 *      currentStyle → colors → background
 *      currentStyle → padding → top
 *  and every edit produces a brand-new, structurally-shared GroupStyle.
 *
 *  These are package-private top-level classes (legal to bundle many per file
 *  as long as none is public) so the view can name them without the noise of a
 *  long enclosing-class prefix.
 *
 *  A {@code null} colour always means "leave this to the underlying
 *  Look-and-Feel" — neither applied to the live sheet nor emitted into the
 *  generated code. Numeric values are always meaningful (0 is a real value).
 */

/** Background / foundation / foreground fills of a group. */
@With @Getter @Accessors(fluent = true) @AllArgsConstructor
final class ColorSet {
    private final @Nullable Color background;
    private final @Nullable Color foundation;
    private final @Nullable Color foreground;

    static ColorSet none() { return new ColorSet(null, null, null); }
}

/** A group's typography (maps onto {@code componentFont(..)}). */
@With @Getter @Accessors(fluent = true) @AllArgsConstructor
final class Typo {
    private final String         family;   // "" → leave to L&F
    private final int            size;     // 0  → leave to L&F
    private final double         weight;   // 1 = normal, 2 = bold-ish
    private final double         posture;  // 0 = upright, >0 = italic
    private final double         spacing;  // letter spacing
    private final @Nullable Color color;

    static Typo none() { return new Typo("", 0, 1, 0, 0, null); }

    boolean isSet() {
        return !family.isBlank() || size > 0 || weight != 1 || posture != 0
            || spacing != 0 || color != null;
    }
}

/** Four-sided spacing in pixels (padding or margin). */
@With @Getter @Accessors(fluent = true) @AllArgsConstructor
final class Pad {
    private final int top;
    private final int right;
    private final int bottom;
    private final int left;

    static Pad of(int n) { return new Pad(n, n, n, n); }
    static Pad none()    { return of(0); }

    boolean isZero()    { return top == 0 && right == 0 && bottom == 0 && left == 0; }
    boolean isUniform() { return top == right && right == bottom && bottom == left; }
}

/** One border edge: a line width plus its colour. */
@With @Getter @Accessors(fluent = true) @AllArgsConstructor
final class EdgeLine {
    private final int            width;   // 0 → no line on this edge
    private final @Nullable Color color;

    static EdgeLine none() { return new EdgeLine(0, null); }
}

/** One rounded corner: an elliptical arc (width × height). */
@With @Getter @Accessors(fluent = true) @AllArgsConstructor
final class CornerArc {
    private final int width;
    private final int height;

    static CornerArc none() { return new CornerArc(0, 0); }
    static CornerArc of(int r) { return new CornerArc(r, r); }
}

/** A single two-stop gradient overlay. */
@With @Getter @Accessors(fluent = true) @AllArgsConstructor
final class Grad {
    private final boolean           on;
    private final UI.GradientType   type;
    private final UI.Span           span;
    private final Color             color1;
    private final Color             color2;

    static Grad none() {
        return new Grad(false, UI.GradientType.LINEAR, UI.Span.TOP_TO_BOTTOM,
                        new Color(255, 255, 255, 60), new Color(0, 0, 0, 60));
    }
}

/** A single drop / inset shadow. */
@With @Getter @Accessors(fluent = true) @AllArgsConstructor
final class Shade {
    private final boolean on;
    private final Color   color;
    private final int     blur;
    private final int     spread;
    private final int     offsetX;
    private final int     offsetY;
    private final boolean inset;

    static Shade none() {
        return new Shade(false, new Color(0, 0, 0, 70), 8, 0, 0, 3, false);
    }
}

/** A single procedural-noise overlay. */
@With @Getter @Accessors(fluent = true) @AllArgsConstructor
final class Grain {
    private final boolean        on;
    private final UI.NoiseType   function;
    private final Color          color1;
    private final Color          color2;
    private final double         scale;

    static Grain none() {
        return new Grain(false, UI.NoiseType.TISSUE,
                         new Color(0, 0, 0, 0), new Color(0, 0, 0, 40), 1.0);
    }
}