package swingtree.style

import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title
import swingtree.SwingTree
import swingtree.UI
import swingtree.components.JBox
import swingtree.layout.Bounds
import swingtree.layout.Size
import swingtree.threading.EventProcessor
import utility.SwingTreeTestConfigurator
import utility.Utility

import javax.swing.JButton
import java.awt.Color
import java.awt.image.BufferedImage

@Title("Stretch Tiling Pixel Equivalence")
@Narrative('''

    Stretch tiling promises that the rendering of an eligible style at *any*
    component size can be reconstructed from one small canonical rendering
    by copying the four corner tiles and stretching the edge bands and the
    center. That promise is only worth anything if the reconstruction is
    (practically) pixel identical to rendering the style directly at the
    target size — otherwise a resize would visibly change the component.

    This specification renders a matrix of styles both ways — directly at the
    actual size, and via canonical rendering plus nine tile blits — and
    compares the resulting images. It also verifies the two prerequisites the
    scheme rests on: that the canonical rendering really is constant inside
    its stretch bands, and that the device-space snapping of the tile cut
    lines never produces seams under fractional and integer scaling
    transforms (as used by HiDPI displays).

''')
@Subject([StretchTiling, StyleRenderer])
class Stretch_Tiling_Equivalence_Spec extends Specification
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

    private static BufferedImage renderDirectly( UI.Layer layer, LayerRenderConf conf ) {
        var size = conf.boxModel().size()
        var image = Utility.createDeterministicImage((int) size.widthOrElse(0f), (int) size.heightOrElse(0f))
        var g = Utility.createDeterministicGraphics(image)
        StyleRenderer.renderStyleOn(layer, conf, g)
        g.dispose()
        return image
    }

    private static BufferedImage renderTiled( UI.Layer layer, LayerRenderConf conf ) {
        var canonical = StretchTiling.canonicalize(conf)
        assert !canonical.is(conf) : "The test style must be eligible for stretch tiling!"
        var canonicalImage = renderDirectly(layer, canonical)
        var size = conf.boxModel().size()
        var image = Utility.createDeterministicImage((int) size.widthOrElse(0f), (int) size.heightOrElse(0f))
        var g = Utility.createDeterministicGraphics(image)
        StretchTiling.blit(g, canonical, canonicalImage, size)
        g.dispose()
        return image
    }

    def 'Nine tile reconstruction is pixel equivalent to direct rendering. (#description)'(
        String description, UI.Layer layer, int width, int height, Closure styler
    ) {
        given : 'The style rendered directly at the actual size, and reconstructed from the canonical rendering:'
            var conf = renderConfOf(layer, width, height, styler)
            var direct = renderDirectly(layer, conf)
            var tiled  = renderTiled(layer, conf)
        expect : 'The two images are practically identical:'
            Utility.similarityBetween(direct, tiled) >= 99.9

        where :
            description                       | layer               | width | height | styler
            "rounded background + foundation" | UI.Layer.BACKGROUND | 400   | 80     | { it.backgroundColor("#d14a4a").foundationColor("#1a1d22").borderRadius(12).margin(5) }
            "per-corner arcs, tall"           | UI.Layer.BACKGROUND | 80    | 400    | { it.backgroundColor("#4ad1a1")
                                                                                            .borderRadiusAt(UI.Corner.TOP_LEFT, 0, 0)
                                                                                            .borderRadiusAt(UI.Corner.TOP_RIGHT, 8, 8)
                                                                                            .borderRadiusAt(UI.Corner.BOTTOM_LEFT, 16, 16)
                                                                                            .borderRadiusAt(UI.Corner.BOTTOM_RIGHT, 24, 24) }
            "arcless flat, asymmetric margin" | UI.Layer.BACKGROUND | 300   | 200    | { it.backgroundColor("#4a8fd1").foundationColor("#303030").margin(1, 2, 3, 4) }
            "uniform rounded border"          | UI.Layer.BORDER     | 400   | 100    | { it.border(3, "#202430").borderRadius(16) }
            "per-edge border colors, square"  | UI.Layer.BORDER     | 300   | 150    | { it.borderWidths(1, 2, 3, 4).borderColors("#a03030", "#30a030", "#3030a0", "#a0a030") }
            "outset drop shadow with offset"  | UI.Layer.CONTENT    | 350   | 120    | { it.shadowColor("#101010").shadowBlurRadius(6).shadowSpreadRadius(2).shadowOffset(2, 3).borderRadius(12) }
            "inset shadow"                    | UI.Layer.CONTENT    | 350   | 120    | { it.shadowColor("#242424").shadowBlurRadius(4).shadowIsInset(true).borderRadius(10) }
            "two named shadows, in and out"   | UI.Layer.CONTENT    | 320   | 300    | { it.borderRadius(14)
                                                                                            .shadow("halo",  s -> s.color("#3a3a5c").blurRadius(8).spreadRadius(1).isOutset(true))
                                                                                            .shadow("inner", s -> s.color("#14141c").blurRadius(5).isInset(true)) }
            "big margin, huge radius"         | UI.Layer.BACKGROUND | 500   | 300    | { it.backgroundColor("#c19a3f").borderRadius(32).margin(10) }
    }

    def 'The arcless flat reconstruction is exactly pixel identical to direct rendering.'()
    {
        reportInfo """
            Without corner arcs and shadows there is no antialiasing anywhere,
            so we can demand bit-exact equality instead of a similarity
            threshold, which pins the blit math (no off-by-one cut lines,
            no interpolation artifacts) much harder than any threshold.
        """
        given :
            var conf = renderConfOf(UI.Layer.BACKGROUND, 300, 200, {
                            it.backgroundColor("#4a6fb1").foundationColor("#404040").margin(1, 2, 3, 4)
                        })
            var direct = renderDirectly(UI.Layer.BACKGROUND, conf)
            var tiled  = renderTiled(UI.Layer.BACKGROUND, conf)
        expect :
            direct.getWidth() == tiled.getWidth()
            direct.getHeight() == tiled.getHeight()
        and : 'Every single pixel is identical:'
            for ( int y = 0; y < direct.getHeight(); y++ )
                for ( int x = 0; x < direct.getWidth(); x++ )
                    assert direct.getRGB(x, y) == tiled.getRGB(x, y)
    }

    def 'Reconstruction also works when the component is just one pixel larger than the canonical rendering.'()
    {
        reportInfo """
            The smallest size gate for stretch tiling is 'strictly larger than
            the canonical size in both dimensions'. Right at that boundary the
            stretched bands are only a single pixel wide, which is where sloppy
            region math would fail first.
        """
        given : 'A style and its canonical size:'
            var styler = { it.backgroundColor("#7a4ab1").borderRadius(16).margin(4) }
            var probe = renderConfOf(UI.Layer.BACKGROUND, 500, 300, styler)
            var canonicalSize = StretchTiling.canonicalize(probe).boxModel().size()
        and : 'The same style at exactly one pixel more than the canonical size:'
            var conf = renderConfOf(
                            UI.Layer.BACKGROUND,
                            (int) canonicalSize.widthOrElse(0f) + 1,
                            (int) canonicalSize.heightOrElse(0f) + 1,
                            styler
                        )
        expect :
            Utility.similarityBetween(
                renderDirectly(UI.Layer.BACKGROUND, conf),
                renderTiled(UI.Layer.BACKGROUND, conf)
            ) >= 99.9
    }

    def 'The canonical rendering is constant inside its stretch bands.'(
        String description, UI.Layer layer, Closure styler
    ) {
        reportInfo """
            The entire scheme rests on the slice insets being a safe
            overestimation of where the size dependent pixels end. If the
            reach formula ever underestimates (for example by forgetting a
            shadow's gradient fade), varying pixels leak into the stretch band
            and get smeared across the component. We catch that here directly:
            inside the horizontal band all pixel columns must be identical,
            inside the vertical band all pixel rows must be identical.
        """
        given :
            var conf = renderConfOf(layer, 500, 300, styler)
            var canonical = StretchTiling.canonicalize(conf)
            var image = renderDirectly(layer, canonical)
            var insets = StretchTiling.sliceInsets(canonical)
            int left   = insets.left().get() as int
            int right  = insets.right().get() as int
            int top    = insets.top().get() as int
            int bottom = insets.bottom().get() as int

        expect : 'All columns of the horizontal stretch band are identical:'
            for ( int x = left + 1; x < image.getWidth() - right; x++ )
                for ( int y = 0; y < image.getHeight(); y++ )
                    assert image.getRGB(x, y) == image.getRGB(left, y)
        and : 'All rows of the vertical stretch band are identical:'
            for ( int y = top + 1; y < image.getHeight() - bottom; y++ )
                for ( int x = 0; x < image.getWidth(); x++ )
                    assert image.getRGB(x, y) == image.getRGB(x, top)

        where :
            description             | layer               | styler
            "rounded background"    | UI.Layer.BACKGROUND | { it.backgroundColor("#b14a8f").foundationColor("#222222").borderRadius(20).margin(6) }
            "rounded border"        | UI.Layer.BORDER     | { it.border(4, "#454545").borderRadius(18) }
            "offset outset shadow"  | UI.Layer.CONTENT    | { it.shadowColor("#050505").shadowBlurRadius(7).shadowSpreadRadius(3).shadowOffset(3, 2).borderRadius(12) }
            "inset shadow"          | UI.Layer.CONTENT    | { it.shadowColor("#333344").shadowBlurRadius(5).shadowIsInset(true).borderRadius(16) }
    }

    def 'The regular component paint pipeline with stretch tiled caching reproduces the uncached painting.'()
    {
        reportInfo """
            The previous features exercise the renderer and the blit in
            isolation. This one goes through the entire public paint pipeline
            (JComponent.paint) twice: once with all caching disabled, and once
            with the cache warm - where eligible layers are keyed canonically
            and reconstructed through tile blits on every paint. Whatever the
            machinery does internally, the pixels that reach the screen must
            be the same.
        """
        given : 'Two identical, heavily styled buttons (flat colors + shadow: fully stretch tileable).'
            def styler = { it
                .size(260, 90)
                .borderRadius(18)
                .backgroundColor(new Color(90, 140, 40))
                .foundationColor(new Color(24, 24, 28))
                .shadowColor(new Color(0, 0, 0, 120))
                .shadowBlurRadius(5)
            }
            var uncachedButton = UI.button("Same").withStyle(styler as swingtree.api.Styler).get(JButton)
            var cachedButton   = UI.button("Same").withStyle(styler as swingtree.api.Styler).get(JButton)

        when : 'One is painted with caching disabled entirely...'
            CacheBudget.UNITS_OVERRIDE = 0
            var uncachedImage = Utility.renderSingleComponent(uncachedButton)
        and : '...the other with caching enabled, twice, so the second paint is served from the cache.'
            CacheBudget.UNITS_OVERRIDE = 10
            Utility.renderSingleComponent(cachedButton)
            var cachedImage = Utility.renderSingleComponent(cachedButton)

        then : 'The cached button really is being served from the cache.'
            ComponentExtension.from(cachedButton).hasCachedRendering(UI.Layer.BACKGROUND)
            ComponentExtension.from(cachedButton).cacheHitCount(UI.Layer.BACKGROUND) >= 1
        and : 'Both pipelines produced practically identical pixels.'
            Utility.similarityBetween(uncachedImage, cachedImage) >= 99.9

        cleanup :
            CacheBudget.UNITS_OVERRIDE = -1
    }

    def 'Device space snapping of the tile cut lines produces no seams under scaling transforms. (scale #scale)'(
        double scale
    ) {
        reportInfo """
            On HiDPI displays the destination graphics carries a scaling
            transform (typically 1.25x, 1.5x or 2x). If the nine tiles were
            drawn as user space rectangles, each would round its device pixel
            edges independently, producing one pixel gaps or double blended
            overlaps at fractional scales. The blit therefore snaps all cut
            lines to integer device pixels once and shares them between
            adjacent tiles. Here we scan the reconstruction for such seams:
            with an opaque background and no margin, every interior device
            pixel along the horizontal and vertical center lines (which cross
            all four cut lines) must be fully opaque.
        """
        given : 'An opaque rounded style, rendered canonically:'
            int width = 300, height = 140
            var conf = renderConfOf(UI.Layer.BACKGROUND, width, height, {
                            it.backgroundColor("#3f6fa1").borderRadius(12)
                        })
            var canonical = StretchTiling.canonicalize(conf)
            var canonicalImage = renderDirectly(UI.Layer.BACKGROUND, canonical)

        and : 'A device buffer with a scaling transform, reconstructed via tile blits:'
            int deviceWidth  = Math.ceil(width * scale) as int
            int deviceHeight = Math.ceil(height * scale) as int
            var image = Utility.createDeterministicImage(deviceWidth, deviceHeight)
            var g = Utility.createDeterministicGraphics(image)
            g.scale(scale, scale)
            StretchTiling.blit(g, canonical, canonicalImage, Size.of(width, height))
            g.dispose()

        expect : 'No transparent seam anywhere along the horizontal center line:'
            int centerY = (int) (deviceHeight / 2)
            for ( int x = 1; x < (int) Math.floor(width * scale) - 1; x++ )
                assert ((image.getRGB(x, centerY) >> 24) & 0xFF) == 255
        and : 'No transparent seam anywhere along the vertical center line:'
            int centerX = (int) (deviceWidth / 2)
            for ( int y = 1; y < (int) Math.floor(height * scale) - 1; y++ )
                assert ((image.getRGB(centerX, y) >> 24) & 0xFF) == 255

        and : 'The scaled reconstruction matches a scaled blit of a directly rendered full size image:'
            var reference = Utility.createDeterministicImage(deviceWidth, deviceHeight)
            var g2 = Utility.createDeterministicGraphics(reference)
            g2.scale(scale, scale)
            g2.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR)
            g2.drawImage(renderDirectly(UI.Layer.BACKGROUND, conf), 0, 0, null)
            g2.dispose()
            Utility.similarityBetween(reference, image) >= 99.5

        where :
            scale << [1.0d, 1.25d, 1.5d, 2.0d]
    }
}
