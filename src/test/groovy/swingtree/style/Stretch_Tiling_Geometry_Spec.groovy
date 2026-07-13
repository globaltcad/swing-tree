package swingtree.style

import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title
import swingtree.SwingTree
import swingtree.UI
import swingtree.components.JBox
import swingtree.layout.Bounds
import swingtree.threading.EventProcessor
import utility.SwingTreeTestConfigurator

@Title("Stretch Tiling Geometry")
@Narrative('''

    The render cache of SwingTree is keyed by an immutable snapshot of everything
    that influences the pixels of a component layer, which includes the exact
    component size. So while a component is being resized, every paint produces
    a new cache key and consequently a cache miss, making resizing expensive
    for heavily styled components.

    The `StretchTiling` utility solves this by mapping every *eligible* render
    configuration of *every size* onto one shared, size independent cache key:
    the same configuration with its size replaced by a minimal "canonical" size.
    This works because for flat fills, borders and shadows, the rendered pixels
    are constant along the component edges, so any size can be reconstructed
    from the canonical rendering by copying the four corners and stretching
    the edge bands and the center (a "nine slice", like the classic
    9-patch images from Android or CSS border-image slicing).

    This specification covers the pure geometry and policy functions:
    which configurations are eligible, where the slice cut lines are,
    how big the canonical rendering is, and the algebraic properties
    of the canonicalization (idempotence and size independence).

''')
@Subject([StretchTiling])
class Stretch_Tiling_Geometry_Spec extends Specification
{
    def setupSpec() {
        SwingTree.initializeUsing(SwingTreeTestConfigurator.get())
        SwingTree.get().setEventProcessor(EventProcessor.COUPLED_STRICT)
    }

    def cleanupSpec() {
        SwingTree.clear()
    }

    private static LayerRenderConf renderConfOf( UI.Layer layer, int width, int height, Closure styler ) {
        var style = ComponentExtension.from(
                        UI.box().withStyle(conf -> styler(conf)).get(JBox)
                    )
                    .getStyle()
        var componentConf = new ComponentConf(style, Bounds.of(0, 0, width, height), Outline.none())
        return componentConf.renderConfFor(layer)
    }

    def 'Eligible configurations of different sizes share one and the same canonical cache key.'()
    {
        reportInfo """
            This is the central promise of stretch tiling: no matter how large
            the component currently is, the canonical form of its render
            configuration is always the same object value. A resize therefore
            does not invalidate the cache entry.
        """
        given : 'The same heavy background style at three very different sizes:'
            var styler = { it.backgroundColor("#4a8fd1").foundationColor("#20242a").borderRadius(16).margin(6) }
            var small  = renderConfOf(UI.Layer.BACKGROUND, 120,  60,  styler)
            var medium = renderConfOf(UI.Layer.BACKGROUND, 500,  300, styler)
            var large  = renderConfOf(UI.Layer.BACKGROUND, 3840, 900, styler)

        expect : 'The three configurations differ (their sizes differ):'
            small != medium
            medium != large

        when : 'We canonicalize all three:'
            var canonicalSmall  = StretchTiling.canonicalize(small)
            var canonicalMedium = StretchTiling.canonicalize(medium)
            var canonicalLarge  = StretchTiling.canonicalize(large)

        then : 'They all collapse onto one and the same size independent key:'
            canonicalSmall == canonicalMedium
            canonicalMedium == canonicalLarge

        and : 'The canonical size is the minimal size derived from the slice insets, not the component size:'
            canonicalMedium.boxModel().size() == StretchTiling.canonicalSize(StretchTiling.sliceInsets(medium))

        and : 'The canonical rendering is tiny compared to the actual components:'
            canonicalMedium.boxModel().size().widthOrElse(0f)  < 60
            canonicalMedium.boxModel().size().heightOrElse(0f) < 60
    }

    def 'Canonicalization is idempotent, so canonical configurations map onto themselves.'()
    {
        reportInfo """
            A real component may coincidentally have exactly the canonical size.
            Its configuration must then map onto itself, otherwise the cache key
            would flip-flop. More generally `canonicalize(canonicalize(x))` must
            always equal `canonicalize(x)`.
        """
        given :
            var conf = renderConfOf(UI.Layer.BACKGROUND, 400, 200, {
                            it.backgroundColor("#af4232").borderRadius(24).margin(4)
                        })
        when :
            var canonical = StretchTiling.canonicalize(conf)
        then : 'The canonical form is a fixed point of the canonicalization:'
            StretchTiling.canonicalize(canonical).is(canonical)
    }

    def 'Style content whose pixels depend on the full component size makes a configuration ineligible.'(
        UI.Layer layer, Closure styler
    ) {
        reportInfo """
            Gradients span the component bounds, noise varies per pixel position,
            images are placed and fitted relative to the component bounds, text is
            laid out within them and custom painters are completely opaque to us.
            None of these can be reconstructed by stretching a small rendering,
            so they must fall back to the classic exact-size caching, which
            means canonicalization must return the configuration unchanged.
        """
        given :
            var conf = renderConfOf(layer, 500, 300, styler)
        expect :
            !StretchTiling.isEligible(conf)
        and : 'Canonicalization is the identity, so behavior falls back to the status quo:'
            StretchTiling.canonicalize(conf).is(conf)

        where :
            layer               | styler
            UI.Layer.BACKGROUND | { it.backgroundColor("#4a8fd1").borderRadius(12).gradient(g -> g.colors("#ff0000", "#0000ff")) }
            UI.Layer.BACKGROUND | { it.backgroundColor("#4a8fd1").borderRadius(12).noise(n -> n.colors("#ff0000", "#0000ff")) }
            UI.Layer.BACKGROUND | { it.backgroundColor("#4a8fd1").borderRadius(12).image(UI.Layer.BACKGROUND, i -> i.primer(UI.color("#ff0000"))) }
            UI.Layer.CONTENT    | { it.backgroundColor("#4a8fd1").borderRadius(12).text(t -> t.content("Hello")) }
            UI.Layer.BACKGROUND | { it.backgroundColor("#4a8fd1").borderRadius(12).painter(UI.Layer.BACKGROUND, g2d -> {}) }
    }

    def 'Flat colors, borders and shadows are eligible for stretch tiling.'(
        UI.Layer layer, Closure styler
    ) {
        given :
            var conf = renderConfOf(layer, 500, 300, styler)
        expect :
            StretchTiling.isEligible(conf)
        and : 'Since the component is large enough, canonicalization produces a smaller key:'
            StretchTiling.canonicalize(conf) != conf

        where :
            layer               | styler
            UI.Layer.BACKGROUND | { it.backgroundColor("#4a8fd1").foundationColor("#20242a").borderRadius(16).margin(6) }
            UI.Layer.BORDER     | { it.border(3, "#20242a").borderRadius(16) }
            UI.Layer.BORDER     | { it.borderWidths(1, 2, 3, 4).borderColors("#ff0000", "#00ff00", "#0000ff", "#ffff00") }
            UI.Layer.CONTENT    | { it.shadowColor("#000000").shadowBlurRadius(6).shadowSpreadRadius(2).shadowOffset(2, 3).borderRadius(12) }
            UI.Layer.BACKGROUND | { it.backgroundColor("#4a8fd1")
                                      .borderRadiusAt(UI.Corner.TOP_LEFT, 0, 0)
                                      .borderRadiusAt(UI.Corner.TOP_RIGHT, 8, 8)
                                      .borderRadiusAt(UI.Corner.BOTTOM_LEFT, 16, 16)
                                      .borderRadiusAt(UI.Corner.BOTTOM_RIGHT, 24, 24) }
    }

    def 'Per edge border colors are only eligible without rounded corners.'()
    {
        reportInfo """
            The diagonal color seam between two differently colored border edges
            runs towards the component center. For square corners the seam is
            confined to the fixed corner region, but inside a *rounded* corner
            the slope of the seam (and therefore the corner pixels) depends on
            the component aspect ratio, which stretch tiling cannot reproduce.
        """
        given : 'Two per-edge colored borders, one with and one without corner arcs:'
            var square  = renderConfOf(UI.Layer.BORDER, 500, 300, {
                              it.borderWidths(1, 2, 3, 4).borderColors("#ff0000", "#00ff00", "#0000ff", "#ffff00")
                          })
            var rounded = renderConfOf(UI.Layer.BORDER, 500, 300, {
                              it.borderWidths(1, 2, 3, 4).borderColors("#ff0000", "#00ff00", "#0000ff", "#ffff00").borderRadius(16)
                          })
        expect :
            StretchTiling.isEligible(square)
            !StretchTiling.isEligible(rounded)

        and : 'A homogeneous border color remains eligible even with rounded corners:'
            StretchTiling.isEligible(renderConfOf(UI.Layer.BORDER, 500, 300, {
                it.border(3, "#20242a").borderRadius(16)
            }))
    }

    def 'The slice insets account for margins, border widths, corner arcs and the safety margin.'()
    {
        reportInfo """
            The slice insets mark how far the size dependent pixels reach into
            the component from each side. Everything between opposite insets is
            constant along the respective axis and may be stretched freely.
        """
        given : 'A style with asymmetric margins, border widths and per-corner arcs:'
            var conf = renderConfOf(UI.Layer.BACKGROUND, 500, 300, {
                            it.backgroundColor("#4a8fd1")
                              .margin(1, 2, 3, 4)             // top, right, bottom, left
                              .borderWidths(5, 6, 7, 8)       // top, right, bottom, left
                              .borderRadiusAt(UI.Corner.TOP_LEFT, 10, 11)
                              .borderRadiusAt(UI.Corner.TOP_RIGHT, 12, 13)
                              .borderRadiusAt(UI.Corner.BOTTOM_LEFT, 14, 15)
                              .borderRadiusAt(UI.Corner.BOTTOM_RIGHT, 16, 17)
                        })
        when :
            var insets = StretchTiling.sliceInsets(conf)
        then : 'Each side is margin + border width + the larger adjacent arc extent + safety margin:'
            insets.top().get()    == 1 + 5 + Math.max(11, 13) + StretchTiling.SAFETY_MARGIN
            insets.right().get()  == 2 + 6 + Math.max(12, 16) + StretchTiling.SAFETY_MARGIN
            insets.bottom().get() == 3 + 7 + Math.max(15, 17) + StretchTiling.SAFETY_MARGIN
            insets.left().get()   == 4 + 8 + Math.max(10, 14) + StretchTiling.SAFETY_MARGIN
    }

    def 'The slice insets account for the full reach of shadows, including their gradient fade.'()
    {
        reportInfo """
            A shadow's varying pixels extend beyond its geometric box: the shadow
            gradients fade over blur + spread + an internal gradient start offset,
            and the whole shadow box is displaced by the shadow offset. The insets
            must cover all of it, using the exact same gradient start offset
            formula as the shadow renderer (a shared pure function), so that
            the cut lines are guaranteed to run through constant pixels.
        """
        given : 'A configuration with an offset drop shadow:'
            var conf = renderConfOf(UI.Layer.CONTENT, 500, 300, {
                            it.shadowColor("#000000")
                              .shadowBlurRadius(6)
                              .shadowSpreadRadius(2)
                              .shadowOffset(2, 3)
                              .borderRadius(12)
                        })
        and : 'The gradient fade distance straight from the single source of truth:'
            var shadow = conf.layer().shadows().sortedByNames()[0]
            var fade = StyleRenderer.shadowGradientStartOffset(conf.boxModel(), shadow)

        when :
            var insets = StretchTiling.sliceInsets(conf)
        then : 'Horizontal sides include blur + spread + fade + horizontal offset, on top of the arc:'
            insets.left().get()  == Math.ceil(12 + (6 + 2 + fade + 2) + StretchTiling.SAFETY_MARGIN)
            insets.right().get() == insets.left().get()
        and : 'Vertical sides include the vertical offset instead:'
            insets.top().get()    == Math.ceil(12 + (6 + 2 + fade + 3) + StretchTiling.SAFETY_MARGIN)
            insets.bottom().get() == insets.top().get()
    }

    def 'The canonical size is symmetric per axis so that center-splitting render internals stay in the constant band.'()
    {
        reportInfo """
            Some rendering internals split their work at the component center
            (corner shadow clip boxes and border edge polygons both meet in the
            middle). The canonical size is therefore `2 * max(insetA, insetB) + band`
            per axis, which guarantees the canonical center line falls into the
            freely stretchable band no matter how asymmetric the insets are.
        """
        given : 'A configuration with strongly asymmetric insets:'
            var conf = renderConfOf(UI.Layer.BACKGROUND, 500, 300, {
                            it.backgroundColor("#4a8fd1").margin(0, 0, 0, 20).borderRadius(8)
                        })
        when :
            var insets = StretchTiling.sliceInsets(conf)
            var canonical = StretchTiling.canonicalSize(insets)
        then :
            insets.left().get() > insets.right().get()
        and : 'Both axes use twice the larger inset plus the stretch band:'
            canonical.widthOrElse(0f)  == 2 * insets.left().get() + StretchTiling.STRETCH_BAND
            canonical.heightOrElse(0f) == 2 * insets.top().get()  + StretchTiling.STRETCH_BAND
    }

    def 'Components which are not strictly larger than the canonical size are left alone.'()
    {
        reportInfo """
            If the component is so small that the corner regions would overlap,
            stretch tiling cannot apply. Canonicalization then returns the
            configuration unchanged and everything behaves exactly like the
            classic exact-size caching.
        """
        given : 'A small component with a comparatively heavy style:'
            var conf = renderConfOf(UI.Layer.BACKGROUND, 40, 40, {
                            it.backgroundColor("#4a8fd1").borderRadius(16).margin(6)
                        })
        expect : 'The content is eligible, but the size gate rejects it:'
            StretchTiling.isEligible(conf)
            StretchTiling.canonicalize(conf).is(conf)
    }

    def 'An empty style layer is eligible but canonicalization is inconsequential for it.'()
    {
        reportInfo """
            A completely undecorated layer contains nothing that varies with
            size, so it is trivially eligible. Whether or not it gets cached is
            a separate decision made by the cache scoring (which rejects plain
            layers as not worth caching), so eligibility alone must not matter.
        """
        given :
            var conf = renderConfOf(UI.Layer.BACKGROUND, 500, 300, { it })
        expect :
            StretchTiling.isEligible(conf)
    }
}
