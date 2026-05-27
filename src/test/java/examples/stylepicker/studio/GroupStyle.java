package examples.stylepicker.studio;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.With;
import lombok.experimental.Accessors;

import java.awt.Color;

/**
 *  The full style configuration of a single {@link Look} group — an immutable
 *  aggregate of the individual {@link ColorSet}, {@link Typo}, {@link Pad},
 *  {@link BorderConf}, {@link Grad}, {@link Shade} and {@link Grain} parts.
 *
 *  <p>This is the unit the editor zooms into: the studio always edits the
 *  {@code GroupStyle} of the currently-selected group, and the
 *  {@link LookSheet} turns each group's {@code GroupStyle} into live style-API
 *  calls.</p>
 */
@With @Getter @Accessors(fluent = true) @AllArgsConstructor
public final class GroupStyle {

    private final ColorSet   colors;
    private final Typo       typo;
    private final Pad        padding;
    private final Pad        margin;
    private final BorderConf border;
    private final Grad       gradient;
    private final Shade      shadow;
    private final Grain      noise;

    /** A completely neutral style that changes nothing about the L&F default. */
    public static GroupStyle neutral() {
        return new GroupStyle(
            ColorSet.none(), Typo.none(), Pad.none(), Pad.none(),
            BorderConf.none(), Grad.none(), Shade.none(), Grain.none()
        );
    }

    // ── Pleasant, opinionated starting defaults — one per group ──────────────
    //    These give the very first "Apply" something handsome to show and a
    //    sensible base for the user to tweak.

    static GroupStyle defaultFor(Look look) {
        Color page    = new Color(244, 246, 250);
        Color surface = new Color(255, 255, 255);
        Color ink     = new Color( 30,  41,  59);
        Color subtle  = new Color(100, 116, 139);
        Color accent  = new Color( 59, 130, 246);
        Color border  = new Color(226, 232, 240);

        switch (look) {
            case FRAME:
                return neutral()
                    .withColors(new ColorSet(page, page, ink))
                    .withPadding(Pad.of(0))
                    .withTypo(Typo.none().withFamily("SansSerif").withSize(13).withColor(ink));
            case SURFACE:
                return neutral()
                    .withColors(ColorSet.none().withBackground(surface))
                    .withBorder(BorderConf.uniform(0, border, 10))
                    .withPadding(Pad.of(12))
                    .withMargin(Pad.of(6));
            case CARD:
                return neutral()
                    .withColors(ColorSet.none().withBackground(surface))
                    .withBorder(BorderConf.uniform(1, border, 14))
                    .withPadding(Pad.of(16))
                    .withMargin(Pad.of(8))
                    .withShadow(new Shade(true, new Color(0, 0, 0, 22), 16, 0, 0, 4, false));
            case HEADER:
                return neutral()
                    .withColors(ColorSet.none().withBackground(new Color(30, 41, 59)).withForeground(Color.WHITE))
                    .withPadding(new Pad(12, 20, 12, 20))
                    .withTypo(Typo.none().withFamily("SansSerif").withSize(15).withWeight(2).withColor(Color.WHITE));
            case FOOTER:
                return neutral()
                    .withColors(new ColorSet(new Color(241, 245, 249), null, subtle))
                    .withBorder(BorderConf.edge(swingtree.UI.Edge.TOP, 1, border))
                    .withPadding(new Pad(6, 16, 6, 16))
                    .withTypo(Typo.none().withFamily("SansSerif").withSize(11).withColor(subtle));
            case HEADING:
                return neutral()
                    .withTypo(Typo.none().withFamily("SansSerif").withSize(18).withWeight(2).withColor(ink));
            case TEXT:
                return neutral()
                    .withTypo(Typo.none().withFamily("SansSerif").withSize(13).withColor(ink));
            case CAPTION:
                return neutral()
                    .withTypo(Typo.none().withFamily("SansSerif").withSize(11).withPosture(0.12).withColor(subtle));
            case BUTTON:
                return neutral()
                    .withColors(new ColorSet(new Color(241, 245, 249), null, ink))
                    .withBorder(BorderConf.uniform(1, border, 9))
                    .withPadding(new Pad(7, 14, 7, 14))
                    .withMargin(Pad.of(3))
                    .withTypo(Typo.none().withFamily("SansSerif").withSize(13).withWeight(2).withColor(ink));
            case PRIMARY_BUTTON:
                return neutral()
                    .withColors(new ColorSet(accent, null, Color.WHITE))
                    .withBorder(BorderConf.uniform(0, accent, 9))
                    .withPadding(new Pad(7, 16, 7, 16))
                    .withMargin(Pad.of(3))
                    .withTypo(Typo.none().withFamily("SansSerif").withSize(13).withWeight(2).withColor(Color.WHITE))
                    .withShadow(new Shade(true, new Color(59, 130, 246, 90), 10, 0, 0, 3, false));
            case INPUT:
                return neutral()
                    .withColors(ColorSet.none().withBackground(surface).withForeground(ink))
                    .withBorder(BorderConf.uniform(1, border, 8))
                    .withPadding(new Pad(5, 8, 5, 8))
                    .withMargin(Pad.of(2));
            case LIST_ROW:
                return neutral()
                    .withColors(ColorSet.none().withForeground(ink))
                    .withBorder(BorderConf.edge(swingtree.UI.Edge.BOTTOM, 1, new Color(226, 232, 240)))
                    .withPadding(new Pad(8, 12, 8, 12))
                    .withTypo(Typo.none().withFamily("SansSerif").withSize(13).withColor(ink));
            case SEPARATOR:
                return neutral()
                    .withColors(ColorSet.none().withBackground(border))
                    .withMargin(new Pad(6, 0, 6, 0));
            default:
                return neutral();
        }
    }
}