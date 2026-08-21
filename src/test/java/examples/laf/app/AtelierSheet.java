package examples.laf.app;

import examples.laf.LinenPalette;
import swingtree.style.StyleSheet;

/**
 *  The atelier's own styling, layered on top of the Linen look-and-feel.
 *
 *  <h2>What a style sheet may and may not do under a SwingTree-backed LAF</h2>
 *  SwingTree resolves three layers in a fixed order — <b>style sheet, then
 *  look-and-feel, then inline {@code withStyle(..)}</b> — and each one may
 *  overwrite the one before it. A style sheet therefore sits <em>below</em> the
 *  LAF, which has two consequences worth knowing before writing a single rule:
 *  <ul>
 *    <li>Anything the LAF declares unconditionally — a button's fill, a panel's
 *        background, a scroll pane's border — <b>wins</b> against a sheet rule
 *        for the same property. Asking for it here would silently do nothing.
 *        That is why the atelier asks Linen for the surface and the button role
 *        it wants, through {@code .group(LinenSurface.CARD)} and
 *        {@code .group(LinenVariant.PRIMARY)}, instead of painting them itself.</li>
 *    <li>Everything the LAF leaves alone is the sheet's. Linen is careful about
 *        this: {@code LinenLabelUI} sets only a foreground colour, so typography
 *        — family, size, weight, tracking, and the colour carried <i>by the
 *        font</i> — is entirely ours. Which is what this sheet is: a type scale
 *        for the whole application, in one place, so that no fragment of the
 *        view has to remember what a section heading looks like.</li>
 *  </ul>
 *  Spacing is not here either, but for a different reason: in SwingTree the
 *  natural place for it is the MigLayout constraint that already positions the
 *  child ({@code "ins 14 16 14 16, gap 8"}), not a padding declaration
 *  competing with it from a second file.
 */
public final class AtelierSheet extends StyleSheet
{
    private static final String SANS  = "SansSerif";
    private static final String SERIF = "Serif";

    @Override
    protected void configure() {
        add(group(Skin.APP_TITLE), it -> it
            .componentFont(f -> f.family(SERIF).size(23).weight(2f).color(LinenPalette.TEXT))
        );
        add(group(Skin.APP_SUBTITLE), it -> it
            .componentFont(f -> f.family(SANS).size(12).color(LinenPalette.TEXT_MUTED))
        );
        add(group(Skin.CARD_TITLE), it -> it
            .componentFont(f -> f.family(SERIF).size(16).weight(2f).color(LinenPalette.TEXT))
        );
        add(group(Skin.CARD_SUB), it -> it
            .componentFont(f -> f.family(SANS).size(11).color(LinenPalette.TEXT_MUTED))
        );
        // Small caps by way of tracking: the letters are pushed apart far enough
        // that an all-upper-case heading stops shouting.
        add(group(Skin.SECTION), it -> it
            .componentFont(f -> f.family(SANS).size(10).weight(2f).spacing(0.18f).color(LinenPalette.TEXT_MUTED))
        );
        add(group(Skin.FIELD_LABEL), it -> it
            .componentFont(f -> f.family(SANS).size(12).color(LinenPalette.TEXT_MUTED))
        );
        add(group(Skin.META), it -> it
            .componentFont(f -> f.family(SANS).size(11).color(LinenPalette.TEXT_MUTED))
        );
        add(group(Skin.EMPTY), it -> it
            .componentFont(f -> f.family(SERIF).size(14).posture(0.14f).color(LinenPalette.TEXT_MUTED))
        );
        add(group(Skin.STATUS), it -> it
            .componentFont(f -> f.family(SANS).size(11).color(LinenPalette.TEXT_MUTED))
        );
        add(group(Skin.FIGURE), it -> it
            .componentFont(f -> f.family(SERIF).size(19).weight(2f).color(LinenPalette.TEXT))
        );
        add(group(Skin.DOCUMENT), it -> it
            .componentFont(f -> f.family(SERIF).size(13).color(LinenPalette.TEXT))
        );
    }
}
