package examples.stylepicker.studio;

import java.util.HashMap;
import java.util.Map;

/**
 *  An immutable snapshot of the style of <i>every</i> {@link StyleTarget} — both
 *  the high-level {@link Look} groups and the Swing component types. This is one
 *  complete "theme", and it is the value that travels through the
 *  {@link History}: editing a single field produces a brand-new
 *  {@code StyleConfig}, and {@link LookSheet} renders whichever one is applied.
 *
 *  <p>The backing map is never mutated after construction; every wither copies
 *  it, so instances are safe to share across history entries and checkpoints.</p>
 */
public final class StyleConfig {

    private final Map<StyleTarget, GroupStyle> styles;

    private StyleConfig(Map<StyleTarget, GroupStyle> styles) {
        this.styles = styles; // private + copy-on-write everywhere → effectively immutable
    }

    /** The handsome opinionated starting theme: groups themed, types left neutral. */
    public static StyleConfig starter() {
        Map<StyleTarget, GroupStyle> m = new HashMap<>();
        for (StyleTarget t : StyleTarget.all())
            m.put(t, t.isGroup() ? GroupStyle.defaultFor(t.group()) : GroupStyle.neutral());
        return new StyleConfig(m);
    }

    /** A blank theme that overrides nothing of the underlying Look-and-Feel. */
    public static StyleConfig blank() {
        Map<StyleTarget, GroupStyle> m = new HashMap<>();
        for (StyleTarget t : StyleTarget.all())
            m.put(t, GroupStyle.neutral());
        return new StyleConfig(m);
    }

    public GroupStyle styleFor(StyleTarget target) {
        return styles.getOrDefault(target, GroupStyle.neutral());
    }

    public StyleConfig withStyleFor(StyleTarget target, GroupStyle style) {
        Map<StyleTarget, GroupStyle> m = new HashMap<>(styles);
        m.put(target, style);
        return new StyleConfig(m);
    }
}