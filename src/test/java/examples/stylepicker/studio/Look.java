package examples.stylepicker.studio;
import java.util.Locale;

/**
 *  A small, deliberately <b>application-agnostic</b> vocabulary of semantic
 *  styling groups — the SwingTree equivalent of a CSS reset's base selectors.
 *
 *  <p>The whole idea behind {@link StyleStudioView} is that <i>any</i> SwingTree
 *  app can tag its components with these groups
 *  ({@code .group(Look.CARD)}, {@code .group(Look.PRIMARY_BUTTON)}, …) and then
 *  hand the user a {@link LookSheet} configured through this studio. Because the
 *  studio's <i>own</i> skeleton is tagged with exactly the same groups, the user
 *  literally designs the look-and-feel of the tool from inside the tool.</p>
 *
 *  <p>The names map onto the higher-level structural roles found in almost every
 *  desktop UI: a window surface, raised containers, chrome bars, a small type
 *  scale, two button weights, inputs and list rows. Anything that is really just
 *  "a kind of Swing widget" (a slider, a check box, a combo, …) is <i>not</i> a
 *  group here — those are addressed by their {@code type(..)} instead (see
 *  {@link StyleTarget}). Keeping the group set generic is what makes a
 *  {@code StyleSheet} produced here reusable in a completely different app.</p>
 */
public enum Look {
    /** The window / root page surface. */
    FRAME,
    /** A plain raised container panel. */
    SURFACE,
    /** An elevated content card (rounded, shadowed). */
    CARD,
    /** A top chrome bar (title / toolbar region). */
    HEADER,
    /** A bottom chrome bar (status / footer region). */
    FOOTER,
    /** Prominent heading text. */
    HEADING,
    /** Ordinary body text. */
    TEXT,
    /** Muted secondary / caption text. */
    CAPTION,
    /** A default (secondary) push button. */
    BUTTON,
    /** A primary call-to-action button. */
    PRIMARY_BUTTON,
    /** A text input field / area. */
    INPUT,
    /** A row inside a list / table / feed. */
    LIST_ROW,
    /** A thin divider between sections. */
    SEPARATOR;

    /** {@code PRIMARY_BUTTON} → {@code "Primary Button"} for combo boxes and code. */
    public String pretty() {
        String[] parts = name().split("_", -1);
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            sb.append(Character.toUpperCase(p.charAt(0)))
              .append(p.substring(1).toLowerCase(Locale.ROOT))
              .append(' ');
        }
        return sb.toString().trim();
    }
}