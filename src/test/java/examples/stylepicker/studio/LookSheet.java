package examples.stylepicker.studio;

import swingtree.UI;
import swingtree.style.ComponentStyleDelegate;
import swingtree.style.StyleSheet;

import javax.swing.JComponent;
import java.awt.Color;

/**
 *  The live, hot-swappable {@link StyleSheet} that turns an applied
 *  {@link StyleConfig} into real SwingTree styling for every {@link Look} group.
 *
 *  <p>This is the same hot-swap idiom as {@code examples.zen.ThemedStyleSheet}
 *  and {@code examples.trains.mvi.TrainStyle}: hold a mutable config, and on
 *  {@link #setConfig(StyleConfig)} call {@link #reconfigure()} so everything
 *  built inside the surrounding {@code UI.use(sheet, ...)} scope instantly
 *  repaints with the new rules. Because {@link StyleStudioView} builds its own
 *  chrome <i>and</i> a showcase inside that scope, pressing Apply restyles the
 *  tool itself — the "design your own Look-and-Feel" payoff.</p>
 *
 *  <p>The {@link #apply(GroupStyle, ComponentStyleDelegate)} mapper is the single
 *  source of truth for "what a {@link GroupStyle} means", and is intentionally
 *  mirrored field-for-field by {@code StyleStudioView}'s code generator so the
 *  copied source produces a visually identical result.</p>
 */
public final class LookSheet extends StyleSheet {

    private StyleConfig applied = StyleConfig.blank();

    public StyleConfig config() { return applied; }

    /** Swap in a new applied config and repaint the whole {@code UI.use} scope. */
    public void setConfig(StyleConfig newConfig) {
        this.applied = newConfig;
        reconfigure();
    }

    @Override
    protected void configure() {
        StyleConfig cfg = this.applied;
        // Emit one rule per target. Type rules are added before group rules, but
        // group rules win regardless because a group selector is more specific
        // than a type selector in SwingTree's style sheet resolution.
        for (StyleTarget t : StyleTarget.all()) {
            GroupStyle gs = cfg.styleFor(t);
            if (t.isGroup()) add(group(t.group()), it -> apply(gs, it));
            else             addType(t.type(), gs);
        }
    }

    /** Helper that captures the component type so {@code add(type(..), ..)} type-checks cleanly. */
    private <C extends JComponent> void addType(Class<C> type, GroupStyle gs) {
        add(type(type), it -> apply(gs, it));
    }

    /**
     *  Maps a {@link GroupStyle} onto a style delegate. A {@code null} colour or
     *  a disabled overlay simply contributes nothing, so untouched aspects fall
     *  through to the underlying Look-and-Feel.
     */
    public static <C extends JComponent> ComponentStyleDelegate<C> apply(
        GroupStyle s, ComponentStyleDelegate<C> it
    ) {
        // ── Fills ────────────────────────────────────────────────────────────
        ColorSet c = s.colors();
        if (c.background() != null) it = it.backgroundColor(c.background());
        if (c.foundation() != null) it = it.foundationColor(c.foundation());
        if (c.foreground() != null) it = it.foregroundColor(c.foreground());

        // ── Box: padding / margin / border / radius ──────────────────────────
        Pad p = s.padding();
        it = it.padding(p.top(), p.right(), p.bottom(), p.left());
        Pad m = s.margin();
        it = it.margin(m.top(), m.right(), m.bottom(), m.left());

        BorderConf b = s.border();
        if (b.anyArc())
            it = it
                .borderRadiusAt(UI.Corner.TOP_LEFT,     b.arcWidthAt(UI.Corner.TOP_LEFT),     b.arcHeightAt(UI.Corner.TOP_LEFT))
                .borderRadiusAt(UI.Corner.TOP_RIGHT,    b.arcWidthAt(UI.Corner.TOP_RIGHT),    b.arcHeightAt(UI.Corner.TOP_RIGHT))
                .borderRadiusAt(UI.Corner.BOTTOM_RIGHT, b.arcWidthAt(UI.Corner.BOTTOM_RIGHT), b.arcHeightAt(UI.Corner.BOTTOM_RIGHT))
                .borderRadiusAt(UI.Corner.BOTTOM_LEFT,  b.arcWidthAt(UI.Corner.BOTTOM_LEFT),  b.arcHeightAt(UI.Corner.BOTTOM_LEFT));
        if (b.anyWidth())
            it = it
                .borderWidths(b.widthAt(UI.Edge.TOP), b.widthAt(UI.Edge.RIGHT), b.widthAt(UI.Edge.BOTTOM), b.widthAt(UI.Edge.LEFT))
                .borderColors(nz(b.colorAt(UI.Edge.TOP)), nz(b.colorAt(UI.Edge.RIGHT)), nz(b.colorAt(UI.Edge.BOTTOM)), nz(b.colorAt(UI.Edge.LEFT)));

        // ── Typography ───────────────────────────────────────────────────────
        Typo t = s.typo();
        if (t.isSet()) {
            it = it.componentFont(f -> {
                if (!t.family().isBlank()) f = f.family(t.family());
                if (t.size() > 0)          f = f.size(t.size());
                f = f.weight((float) t.weight())
                     .posture((float) t.posture())
                     .spacing((float) t.spacing());
                if (t.color() != null)     f = f.color(t.color());
                return f;
            });
        }

        // ── Gradient overlay ─────────────────────────────────────────────────
        Grad g = s.gradient();
        if (g.on())
            it = it.gradient("look", gc -> gc
                .type(g.type())
                .span(g.span())
                .colors(g.color1(), g.color2())
                .clipTo(UI.ComponentArea.BODY)
            );

        // ── Shadow ───────────────────────────────────────────────────────────
        Shade sh = s.shadow();
        if (sh.on())
            it = it.shadow("look", sc -> sc
                .color(sh.color())
                .blurRadius(sh.blur())
                .spreadRadius(sh.spread())
                .offset(sh.offsetX(), sh.offsetY())
                .isInset(sh.inset())
            );

        // ── Noise overlay ────────────────────────────────────────────────────
        Grain n = s.noise();
        if (n.on())
            it = it.noise("look", nc -> nc
                .function(n.function())
                .colors(n.color1(), n.color2())
                .scale(n.scale())
                .clipTo(UI.ComponentArea.BODY)
            );

        return it;
    }

    /** Non-null colour fallback for a border edge whose colour was left unset. */
    private static Color nz(Color c) { return c != null ? c : Color.GRAY; }
}