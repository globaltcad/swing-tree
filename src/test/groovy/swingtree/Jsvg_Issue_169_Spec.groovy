package swingtree

import com.github.weisj.jsvg.SVGDocument
import com.github.weisj.jsvg.parser.LoaderContext
import com.github.weisj.jsvg.parser.SVGLoader
import com.github.weisj.jsvg.view.ViewBox
import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Title
import swingtree.components.JBox
import utility.Utility

import java.awt.Color
import java.awt.Rectangle
import java.awt.image.BufferedImage
import java.nio.charset.StandardCharsets

@Title("The jsvg ViewBox Rendering Contract (jsvg issue 169)")
@Narrative('''
    This specification pins down the rendering contract which SwingTree's `SvgIcon`
    relies on: `SVGDocument.render(component, graphics, viewBox)` has to map the
    document onto the passed view box, so that the document fills exactly that
    rectangle.

    The jsvg library broke this contract in version 2.1.0: whenever a document
    declared explicit width/height attributes whose size differed from the passed
    view box, the declared size was first fitted *into* the view box using a
    hardcoded "xMidYMid meet" policy. The content was then rendered into that
    smaller, centered viewport, which made it shrink. This is reported here:

    https://github.com/weisJ/jsvg/issues/169

    SwingTree worked around it by rewriting the width/height attributes of loaded
    documents to "100%" at load time (issue #556 of SwingTree). This branch reverts
    that workaround so that the specification below talks to jsvg directly, without
    anything in between:

     - with jsvg 2.0.0 it passes (the contract held before the regression),
     - with jsvg 2.1.0 it fails (the regression),
     - with a jsvg version containing the fix it passes again.

    The related SwingTree issues are:
    https://github.com/globaltcad/swing-tree/issues/383 and
    https://github.com/globaltcad/swing-tree/issues/556
''')
class Jsvg_Issue_169_Spec extends Specification
{
    def 'A document fills the view box passed to `render(component, graphics, viewBox)`.'(
        String description, String svg, int renderWidth, int renderHeight, Rectangle expectedShapeBounds
    ) {
        reportInfo """
            Every case below renders an SVG document containing a single green shape
            into a light gray image and then measures the bounding box of everything
            which is not light gray, in other words: the bounding box of the shape.

            The shape always spans the document's view box except for a 10% margin
            on each side, so a correctly mapped document produces a shape bounding
            box which is inset by 10% of the render target on each side.

            The case named '$description' is rendered into a
            ${renderWidth}x${renderHeight} view box and the shape is expected
            at $expectedShapeBounds.

            Under the jsvg 2.1.0 regression, the cases whose declared width/height
            differ from the render target shrink towards the center of the target
            instead. The very first case for example, a document declaring
            width="200" height="100" and rendered into a 100x100 view box,
            produces a 40x40 bounding box at (30,30) instead of the 80x80
            bounding box at (10,10) which it produced up to jsvg 2.0.0.
        """
        given : 'A document parsed by jsvg exactly the way `SvgIcon` parses it:'
            SVGDocument document = new SVGLoader().load(
                    new ByteArrayInputStream(svg.getBytes(StandardCharsets.UTF_8)),
                    null,
                    LoaderContext.createDefault()
                )
        and : 'A light gray image serving as the render target:'
            var image = new BufferedImage(renderWidth, renderHeight, BufferedImage.TYPE_INT_ARGB)
            var g = image.createGraphics()
            g.setColor(Color.LIGHT_GRAY)
            g.fillRect(0, 0, renderWidth, renderHeight)

        when : 'We render the document onto a view box covering the whole image...'
            document.render(null, g, new ViewBox(0, 0, renderWidth, renderHeight))
            g.dispose()
        then : 'The shape covers the expected part of the image.'
            _boundsOfShapeIn(image, Color.LIGHT_GRAY) == expectedShapeBounds

        where :
            description                    | svg                                              | renderWidth | renderHeight || expectedShapeBounds
            'wide document, square target' | _svgCircle('200', '100', '0 0 100 100', '')      | 100         | 100          || new Rectangle(10, 10, 80, 80)
            'tall document, square target' | _svgCircle('100', '200', '0 0 100 100', '')      | 100         | 100          || new Rectangle(10, 10, 80, 80)
            'wide document, no stretching' | _svgCircle('200', '100', '0 0 100 100', 'none')  | 100         | 100          || new Rectangle(10, 10, 80, 80)
            'sizeless document'            | _svgCircle('',    '',    '0 0 100 100', '')      | 100         | 100          || new Rectangle(10, 10, 80, 80)
            'target matching the document' | _svgCircle('200', '100', '0 0 100 100', 'none')  | 200         | 100          || new Rectangle(20, 10, 160, 80)
    }

    def 'An SVG whose declared aspect ratio differs from its view box is drawn at its declared size.'()
    {
        reportInfo """
            This is the same contract as in the specification above, but seen through
            the SwingTree style API, which is where the jsvg 2.1.0 regression surfaced
            for users of this library.

            A 90x60 pixel box is styled with an SVG document declaring a size of
            60x20 pixels and a square view box of 100x100 units. The document contains
            an orange rectangle covering its view box except for a 5% margin on each
            side, so the rectangle is squashed to the declared 3:1 aspect ratio and
            drawn 54x18 pixels big, centered inside the box.

            Under the jsvg 2.1.0 regression it is drawn smaller than that.
        """
        given : 'A SwingTree context with a UI scale of 1, so that pixels are comparable:'
            SwingTree.initializeUsing(it -> it.uiScaleFactor(1f) )
        and : 'An SVG document declaring a size whose aspect ratio differs from its view box:'
            var svg = "<svg width=\"60\" height=\"20\" viewBox=\"0 0 100 100\">\n" +
                      "  <rect x=\"5\" y=\"5\" width=\"90\" height=\"90\" fill=\"orange\"/>\n" +
                      "</svg>"
        and : 'A box using the SVG as its background image, centered and not fitted to the box:'
            var ui =
                    UI.box().withStyle( it -> it
                        .size(90, 60)
                        .backgroundColor(UI.Color.LIGHTGRAY)
                        .image( conf -> conf
                            .svg(svg)
                            .placement(UI.Placement.CENTER)
                            .fitMode(UI.FitComponent.NO)
                        )
                    )

        when : 'We render the box into an image...'
            var image = Utility.renderSingleComponent(ui.get(JBox))
        then : 'The orange rectangle is 54x18 pixels big and centered in the 90x60 box.'
            _boundsOfShapeIn(image, UI.Color.LIGHTGRAY) == new Rectangle(18, 21, 54, 18)
            _boundsOfShapeIn(image, Color.LIGHT_GRAY) == new Rectangle(18, 21, 54, 18)

        cleanup :
            SwingTree.clear()
    }

    private static String _svgCircle( String width, String height, String viewBox, String preserveAspectRatio ) {
        String widthAttribute  = ( width.isEmpty()  ? "" : " width=\"$width\"" )
        String heightAttribute = ( height.isEmpty() ? "" : " height=\"$height\"" )
        String ratioAttribute  = ( preserveAspectRatio.isEmpty() ? "" : " preserveAspectRatio=\"$preserveAspectRatio\"" )
        return "<svg$widthAttribute$heightAttribute viewBox=\"$viewBox\"$ratioAttribute>\n" +
               "  <circle cx=\"50\" cy=\"50\" r=\"40\" fill=\"green\"/>\n" +
               "</svg>"
    }

    /**
     *  Determines the bounding box of everything in the supplied image which is
     *  not the supplied background color, using a tolerance which ignores the
     *  faint anti aliasing fringe around the edges of a shape.
     */
    private static Rectangle _boundsOfShapeIn( BufferedImage image, Color background ) {
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, maxX = -1, maxY = -1
        for ( int y = 0; y < image.getHeight(); y++ ) {
            for ( int x = 0; x < image.getWidth(); x++ ) {
                var pixel = new Color(image.getRGB(x, y), true)
                int difference = ( Math.abs(pixel.getRed()   - background.getRed()  ) +
                                   Math.abs(pixel.getGreen() - background.getGreen()) +
                                   Math.abs(pixel.getBlue()  - background.getBlue() ) )
                if ( difference > 64 ) {
                    minX = Math.min(minX, x); maxX = Math.max(maxX, x)
                    minY = Math.min(minY, y); maxY = Math.max(maxY, y)
                }
            }
        }
        if ( maxX < 0 )
            return new Rectangle(0, 0, 0, 0)
        return new Rectangle(minX, minY, maxX - minX + 1, maxY - minY + 1)
    }
}
