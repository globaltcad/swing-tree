package swingtree

import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title
import swingtree.api.IconDeclaration
import swingtree.components.JBox
import swingtree.layout.Bounds
import swingtree.layout.Size
import swingtree.style.SvgIcon
import utility.Utility

import javax.swing.BorderFactory
import javax.swing.JLabel
import javax.swing.JPanel
import java.awt.Color
import java.awt.ComponentOrientation
import java.awt.image.BufferedImage
import java.nio.charset.StandardCharsets

@Title("SVG Support through SvgIcon")
@Narrative('''
    Swing-Tree supports SVG icons through the `SvgIcon` class,
    which is a subclass of `javax.swing.ImageIcon`.
    This allows for smooth integration of SVG icons into regular
    Swing components, like buttons, labels, etc.
    
    In this specification we will see how to use the `SvgIcon` class.
''')
@Subject([SvgIcon])
class SvgIcon_Spec extends Specification
{
    def 'A basic `SvgIcon` does not have a size.'()
    {
        reportInfo """
            The nature of SVG icons is that they are scalable,
            which means that they do not have a fixed size.
            The size is dependent on the component that uses the icon
            or the specified size of the icon.
        """
        given : 'We create a basic `SvgIcon` of a funnel.'
            var icon = SvgIcon.at("/img/funnel.svg")
        expect : 'The icon does not have a size.'
            icon.getSvgDocument() != null
            icon.getIconHeight() == -1
            icon.getIconWidth()  == -1
    }

    def 'The `SvgIcon` is immutable, and its size must be specified through wither methods.'(float uiScale) {
        reportInfo """
            The reason why the `SvgIcon` is immutable is because
            it makes caching of the icon easier and safer.
            So when you want to change the size of the icon,
            you must use its various wither methods.
        """
        given : """
            We first set a scaling factor to simulate a platform with higher DPI.
            So when your screen has a higher pixel density then this factor
            is used by SwingTree to ensure that the SVG is scaled from developer
            pixel space to component pixel space accordingly! 
            Please note that the line below only exists for testing purposes, 
            SwingTree will compute a suitable 
            scaling factor for the current system automatically for you,
            so you do not have to specify this factor manually. 
        """
            SwingTree.get().setUiScaleFactor(uiScale)
        and : 'We create a basic `SvgIcon` of a funnel.'
            var icon = SvgIcon.at("/img/funnel.svg")
        when : 'We use the various wither methods to create differently sized icons.'
            var icon2 = icon.withIconWidth(12)
            var icon1 = icon.withIconHeight(13)
            var icon3 = icon.withIconSize(27, 16)
            var icon5 = icon.withIconSizeFromWidth(31)
            var icon4 = icon.withIconSizeFromHeight(24)
        then : 'These icons have different sizes.'
            icon1.getIconWidth()  == (13 * uiScale) as int
            icon1.getIconHeight() == (13 * uiScale) as int
            icon1.getBaseWidth()  == -1
            icon1.getBaseHeight() == 13

            icon2.getIconWidth()  == (12 * uiScale) as int
            icon2.getIconHeight() == (12 * uiScale) as int
            icon2.getBaseWidth()  == 12
            icon2.getBaseHeight() == -1

            icon3.getIconWidth()  == (27 * uiScale) as int
            icon3.getIconHeight() == (16 * uiScale) as int
            icon3.getBaseWidth()  == 27
            icon3.getBaseHeight() == 16

            icon4.getIconWidth()  == (24 * uiScale) as int
            icon4.getIconHeight() == (24 * uiScale) as int
            icon4.getBaseWidth()  == 24
            icon4.getBaseHeight() == 24

            icon5.getIconWidth()  == (31 * uiScale) as int
            icon5.getIconHeight() == (31 * uiScale) as int
            icon5.getBaseWidth()  == 31
            icon5.getBaseHeight() == 31
        cleanup :
            SwingTree.clear()

        where :
            uiScale << [1f, 2f, 3f]
    }

    def 'The `String` representation of the `SvgIcon` shows its properties.'()
    {
        reportInfo """
            The `SvgIcon` consists of various properties,
            which are shown in the `String` representation of the icon.
            This includes the size of the icon and how it is scaled
            and preferably placed.
        """
        given : 'We create a set of various `SvgIcon`s.'
            var icon  = SvgIcon.at("/img/funnel.svg")
            var icon1 = icon.withIconHeight(13)
            var icon2 = icon.withIconWidth(12).withFitComponent(UI.FitComponent.NO)
            var icon3 = icon.withIconSize(27, 16)
            var icon4 = icon.withIconSizeFromWidth(31).withPreferredPlacement(UI.Placement.BOTTOM_RIGHT)
            var icon5 = icon.withIconSizeFromHeight(24)
            var icon6  = icon.withOpacity(0.123f)
        and : 'We turn each icon into its String representation:'
            icon  = icon .toString()
            icon1 = icon1.toString()
            icon2 = icon2.toString()
            icon3 = icon3.toString()
            icon4 = icon4.toString()
            icon5 = icon5.toString()
            icon6 = icon6.toString()

        expect : 'They all have the expected String representations:'
            icon.matches( /SvgIcon\[width=\?, height=\?, fitComponent=UNDEFINED, preferredPlacement=UNDEFINED, opacity=1\.0, doc=.*]/ )
            icon1.matches( /SvgIcon\[width=\?, height=13px, fitComponent=UNDEFINED, preferredPlacement=UNDEFINED, opacity=1\.0, doc=.*]/ )
            icon2.matches( /SvgIcon\[width=12px, height=\?, fitComponent=NO, preferredPlacement=UNDEFINED, opacity=1\.0, doc=.*]/ )
            icon3.matches( /SvgIcon\[width=27px, height=16px, fitComponent=UNDEFINED, preferredPlacement=UNDEFINED, opacity=1\.0, doc=.*]/ )
            icon4.matches( /SvgIcon\[width=31px, height=31px, fitComponent=UNDEFINED, preferredPlacement=BOTTOM_RIGHT, opacity=1\.0, doc=.*]/ )
            icon5.matches( /SvgIcon\[width=24px, height=24px, fitComponent=UNDEFINED, preferredPlacement=UNDEFINED, opacity=1\.0, doc=.*]/ )
            icon6.matches( /SvgIcon\[width=\?, height=\?, fitComponent=UNDEFINED, preferredPlacement=UNDEFINED, opacity=0\.123, doc=.*]/ )
    }

    def 'Use `UI.findSvgIcon(IconDeclaration)` to load an SVG icon from a file.'()
    {
        reportInfo """
            The `UI.findSvgIcon(IconDeclaration)` method is a convenience method
            that allows you to load an icon from a file in the form of an `SvgIcon`.
        """
        given : 'We load an icon from a icon declaration.'
            var declaration = IconDeclaration.of("/img/bubble-tree.svg")
            var icon = UI.findSvgIcon(declaration)
        expect : 'The icon is loaded correctly.'
            icon.isPresent()
        and : 'The icon has the expected size.'
            icon.get().getIconWidth()  == -1
            icon.get().getIconHeight() == -1
    }

    def 'The `UI.findSvgIcon(IconDeclaration)` method will not fail when the icon is not found.'()
    {
        reportInfo """
            The `UI.findSvgIcon(IconDeclaration)` method is a convenience method
            that allows you to load an icon from a file in the form of an `SvgIcon`.
            When it fails to load the icon, it will return an empty `Optional`.
            Any errors that occur during loading are logged.
            This behaviour ensures that the frontend of the application
            will not crash when an icon is not found.
        """
        given : 'We load an icon from a file.'
            var declaration = IconDeclaration.of("/img/my-name-is-so-idiotic-that-it-will-probably-never-exist.svg")
            var icon = UI.findSvgIcon(declaration)
        expect : 'The icon is not loaded.'
            !icon.isPresent()
    }

    def 'The `UI.findIcon(IconDeclaration)` method will not fail when the icon is not found.'()
    {
        reportInfo """
            The `UI.findIcon(IconDeclaration)` method is a convenience method
            that allows you to load an icon from a file in the form of an `SvgIcon`
            if the file is an SVG file.
            When it fails to load the icon, it will return an empty `Optional`.
            Any errors that occur during loading are logged.
            This behaviour ensures that the frontend of the application
            will not crash when an icon is not found.
        """
        given : 'We load an icon from a file.'
            var declaration = IconDeclaration.of("/img/my-name-is-so-idiotic-that-it-will-probably-never-exist.svg")
            var icon = UI.findIcon(declaration)
        expect : 'The icon is not loaded.'
            !icon.isPresent()
    }

    def 'The `SvgIcon` will determine missing image size dimensions through the aspect ratio of the SVG document.'()
    {
        reportInfo """
            The `SvgIcon` is a special type of icon that can load SVG documents
            and then render them as icons in the UI.
            When loading such an icon using a `Size` object with 
            one of the dimensions set to -1, the icon will determine the missing
            dimension through the aspect ratio of the SVG document.
            
            But note that this is calculated dynamically
            for the `getIconWidth()` and `getIconHeight()` methods.
            If you want the actual size of the icon, you 
            can call `getBaseWidth()` and `getBaseHeight()`.
        """
        given : 'We start with an initial scale of 1.'
            SwingTree.initializeUsing(it -> it.uiScaleFactor(1f) )
        and : 'Then we load two SVG icons with different `Size` objects.'
            var icon1 = UI.findIcon(IconDeclaration.of(Size.of(-1, 17), "img/dandelion.svg"))
            var icon2 = UI.findIcon(IconDeclaration.of(Size.of(17, -1), "img/dandelion.svg"))
        expect : 'The icons should have been loaded.'
            icon1.isPresent() && icon2.isPresent()
        and : 'They are both instances of `SvgIcon`.'
            (icon1.get() instanceof SvgIcon)
            (icon2.get() instanceof SvgIcon)
        and : 'The icons should have the correct size.'
            icon1.get().getIconWidth() == 17
            icon1.get().getIconHeight() == 17
            icon2.get().getIconWidth() == 17
            icon2.get().getIconHeight() == 17
        and : 'Their base size is what we specified.'
            icon1.get().getBaseWidth() == -1
            icon1.get().getBaseHeight() == 17
            icon2.get().getBaseWidth() == 17
            icon2.get().getBaseHeight() == -1
    }

    def 'An `SvgIcon` has the correct dimensions when parsed from a string.'(
        float uiScale, int expectedWidth, int expectedHeight, String svg
    ) {
        given : 'We start with an initial UI scale.'
            SwingTree.initializeUsing(it -> it.uiScaleFactor(uiScale) )
        and :
            var svgIcon = SvgIcon.of(svg)
        expect :
            svgIcon.getIconWidth() == expectedWidth
        and :
            svgIcon.getIconHeight() == expectedHeight

        where :
            uiScale | expectedWidth | expectedHeight || svg

            1       |   -1          |   100          || "<svg width=\"100%\" height=\"100\" viewBox=\"0 0 100 100\" xmlns=\"http://www.w3.org/2000/svg\">\n" + "  <circle cx=\"50\" cy=\"50\" r=\"40\" fill=\"red\"/>\n" + "</svg>"
            1       |   -1          |    90          || "<svg width=\"100%\" height=\"90\" viewBox=\"0 0 100 100\" xmlns=\"http://www.w3.org/2000/svg\">\n" + "  <circle cx=\"50\" cy=\"50\" r=\"40\" fill=\"red\"/>\n" + "</svg>"
            1       |   100         |   -1           || "<svg width=\"100\" height=\"100%\" viewBox=\"0 0 100 100\" xmlns=\"http://www.w3.org/2000/svg\">\n" + "  <circle cx=\"50\" cy=\"50\" r=\"40\" fill=\"red\"/>\n" + "</svg>"
            1       |   75          |   100          || "<svg width=\"75\" height=\"100\" viewBox=\"0 0 100 100\" xmlns=\"http://www.w3.org/2000/svg\">\n" + "  <circle cx=\"50\" cy=\"50\" r=\"40\" fill=\"red\"/>\n" + "</svg>"
            1       |   -1          |    12          || "<svg width=\"100%\" height=\"12\" viewBox=\"0 0 100 100\" xmlns=\"http://www.w3.org/2000/svg\">\n" + "  <circle cx=\"50\" cy=\"50\" r=\"40\" fill=\"red\"/>\n" + "</svg>"
            1       |    8          |    16          || "<svg width=\"8\" height=\"16\" viewBox=\"0 0 100 100\" xmlns=\"http://www.w3.org/2000/svg\">\n" + "  <circle cx=\"50\" cy=\"50\" r=\"40\" fill=\"red\"/>\n" + "</svg>"
            1       |    22         |    22          || "<svg width=\"22\" height=\"22\" viewBox=\"0 0 100 100\" xmlns=\"http://www.w3.org/2000/svg\">\n" + "  <circle cx=\"50\" cy=\"50\" r=\"40\" fill=\"red\"/>\n" + "</svg>"

            1       |   -1          |   100          || "<svg width=\"100%\" height=\"100px\" viewBox=\"0 0 100 100\" xmlns=\"http://www.w3.org/2000/svg\">\n" + "  <circle cx=\"50\" cy=\"50\" r=\"40\" fill=\"red\"/>\n" + "</svg>"
            1       |   -1          |    90          || "<svg width=\"100%\" height=\"90px\" viewBox=\"0 0 100 100\" xmlns=\"http://www.w3.org/2000/svg\">\n" + "  <circle cx=\"50\" cy=\"50\" r=\"40\" fill=\"red\"/>\n" + "</svg>"
            1       |   100         |   -1           || "<svg width=\"100px\" height=\"100%\" viewBox=\"0 0 100 100\" xmlns=\"http://www.w3.org/2000/svg\">\n" + "  <circle cx=\"50\" cy=\"50\" r=\"40\" fill=\"red\"/>\n" + "</svg>"
            1       |   75          |   100          || "<svg width=\"75px\" height=\"100px\" viewBox=\"0 0 100 100\" xmlns=\"http://www.w3.org/2000/svg\">\n" + "  <circle cx=\"50\" cy=\"50\" r=\"40\" fill=\"red\"/>\n" + "</svg>"
            1       |   -1          |    12          || "<svg width=\"100%\" height=\"12px\" viewBox=\"0 0 100 100\" xmlns=\"http://www.w3.org/2000/svg\">\n" + "  <circle cx=\"50\" cy=\"50\" r=\"40\" fill=\"red\"/>\n" + "</svg>"
            1       |    8          |    16          || "<svg width=\"8px\" height=\"16px\" viewBox=\"0 0 100 100\" xmlns=\"http://www.w3.org/2000/svg\">\n" + "  <circle cx=\"50\" cy=\"50\" r=\"40\" fill=\"red\"/>\n" + "</svg>"
            1       |    22         |    22          || "<svg width=\"22px\" height=\"22px\" viewBox=\"0 0 100 100\" xmlns=\"http://www.w3.org/2000/svg\">\n" + "  <circle cx=\"50\" cy=\"50\" r=\"40\" fill=\"red\"/>\n" + "</svg>"

            2       |   -1          |   200          || "<svg width=\"100%\" height=\"100\" viewBox=\"0 0 100 100\" xmlns=\"http://www.w3.org/2000/svg\">\n" + "  <circle cx=\"50\" cy=\"50\" r=\"40\" fill=\"red\"/>\n" + "</svg>"
            2       |   -1          |   180          || "<svg width=\"100%\" height=\"90\" viewBox=\"0 0 100 100\" xmlns=\"http://www.w3.org/2000/svg\">\n" + "  <circle cx=\"50\" cy=\"50\" r=\"40\" fill=\"red\"/>\n" + "</svg>"
            2       |   200         |   -1           || "<svg width=\"100\" height=\"100%\" viewBox=\"0 0 100 100\" xmlns=\"http://www.w3.org/2000/svg\">\n" + "  <circle cx=\"50\" cy=\"50\" r=\"40\" fill=\"red\"/>\n" + "</svg>"
            2       |   150         |   200          || "<svg width=\"75\" height=\"100\" viewBox=\"0 0 100 100\" xmlns=\"http://www.w3.org/2000/svg\">\n" + "  <circle cx=\"50\" cy=\"50\" r=\"40\" fill=\"red\"/>\n" + "</svg>"
            2       |   -1          |    24          || "<svg width=\"100%\" height=\"12\" viewBox=\"0 0 100 100\" xmlns=\"http://www.w3.org/2000/svg\">\n" + "  <circle cx=\"50\" cy=\"50\" r=\"40\" fill=\"red\"/>\n" + "</svg>"
            2       |    16         |    32          || "<svg width=\"8\" height=\"16\" viewBox=\"0 0 100 100\" xmlns=\"http://www.w3.org/2000/svg\">\n" + "  <circle cx=\"50\" cy=\"50\" r=\"40\" fill=\"red\"/>\n" + "</svg>"
            2       |    44         |    44          || "<svg width=\"22\" height=\"22\" viewBox=\"0 0 100 100\" xmlns=\"http://www.w3.org/2000/svg\">\n" + "  <circle cx=\"50\" cy=\"50\" r=\"40\" fill=\"red\"/>\n" + "</svg>"

            2       |   -1          |   200          || "<svg width=\"100%\" height=\"100px\" viewBox=\"0 0 100 100\" xmlns=\"http://www.w3.org/2000/svg\">\n" + "  <circle cx=\"50\" cy=\"50\" r=\"40\" fill=\"red\"/>\n" + "</svg>"
            2       |   -1          |   180          || "<svg width=\"100%\" height=\"90px\" viewBox=\"0 0 100 100\" xmlns=\"http://www.w3.org/2000/svg\">\n" + "  <circle cx=\"50\" cy=\"50\" r=\"40\" fill=\"red\"/>\n" + "</svg>"
            2       |   200         |   -1           || "<svg width=\"100px\" height=\"100%\" viewBox=\"0 0 100 100\" xmlns=\"http://www.w3.org/2000/svg\">\n" + "  <circle cx=\"50\" cy=\"50\" r=\"40\" fill=\"red\"/>\n" + "</svg>"
            2       |   150         |   200          || "<svg width=\"75px\" height=\"100px\" viewBox=\"0 0 100 100\" xmlns=\"http://www.w3.org/2000/svg\">\n" + "  <circle cx=\"50\" cy=\"50\" r=\"40\" fill=\"red\"/>\n" + "</svg>"
            2       |   -1          |    24          || "<svg width=\"100%\" height=\"12px\" viewBox=\"0 0 100 100\" xmlns=\"http://www.w3.org/2000/svg\">\n" + "  <circle cx=\"50\" cy=\"50\" r=\"40\" fill=\"red\"/>\n" + "</svg>"
            2       |    16         |    32          || "<svg width=\"8px\" height=\"16px\" viewBox=\"0 0 100 100\" xmlns=\"http://www.w3.org/2000/svg\">\n" + "  <circle cx=\"50\" cy=\"50\" r=\"40\" fill=\"red\"/>\n" + "</svg>"
            2       |    44         |    44          || "<svg width=\"22px\" height=\"22px\" viewBox=\"0 0 100 100\" xmlns=\"http://www.w3.org/2000/svg\">\n" + "  <circle cx=\"50\" cy=\"50\" r=\"40\" fill=\"red\"/>\n" + "</svg>"
    }

    def 'An `SvgIcon` can resolve percentage-based dimensions to pixel-based dimensions using the view box as reference.'(
        float uiScale, Size initialSize, Size resultingBaseSize, Size resultingIconSize, String svg
    ) {
        reportInfo """
            The `SvgIcon.withPercentageSizeResolvedAsPixels()` method is specifically designed for SVG documents 
            with percentage-based dimensions. It converts these percentages to pixel values using the 
            SVG document's view box as reference dimensions.
            
            For example, an SVG with width="100%" height="50%" and viewBox="0 0 24 24" would resolve to:
            - Width: 24 pixels (100% of viewBox width)
            - Height: 12 pixels (50% of viewBox height)
            
            Icons without percentage-based dimensions are returned unchanged.
        """
        given : 'We start with an initial UI scale.'
            SwingTree.initializeUsing(it -> it.uiScaleFactor(uiScale) )
        and : 'We create an SvgIcon from the provided SVG string.'
            var originalIcon = SvgIcon.of(svg)
        expect : 'The initial icon has the expected dimensions:'
            originalIcon.getIconWidth() == initialSize.width().map(UI::scale).map(Math::round).orElse(-1f)
            originalIcon.getIconHeight() == initialSize.height().map(UI::scale).map(Math::round).orElse(-1f)
        and :
            originalIcon.getBaseWidth() == initialSize.width().map(Math::round).orElse(-1f)
            originalIcon.getBaseHeight() == initialSize.height().map(Math::round).orElse(-1f)

        when : 'We call withPercentageSizeResolvedAsPixels() to convert percentage dimensions.'
            var resolvedIcon = originalIcon.withPercentageSizeResolvedAsPixels()
        then :
            resolvedIcon.widthUnitString() == "px"
            resolvedIcon.heightUnitString() == "px"
        then : 'The icon has the expected dimensions after resolution.'
            resolvedIcon.getIconWidth() == resultingIconSize.width().map(UI::scale).map(Math::round).orElse(-1f)
            resolvedIcon.getIconHeight() == resultingIconSize.height().map(UI::scale).map(Math::round).orElse(-1f)
        and :
            resolvedIcon.getBaseWidth() == resultingBaseSize.width().map(Math::round).orElse(-1f)
            resolvedIcon.getBaseHeight() == resultingBaseSize.height().map(Math::round).orElse(-1f)

        where :
            uiScale |  initialSize      | resultingBaseSize | resultingIconSize || svg
            // Basic percentage conversions with viewBox 100x100
            1       | Size.of(-1, -1)   | Size.of(100,  50) | Size.of(100,  50) || "<svg width=\"100%\" height=\"50%\" viewBox=\"0 0 100 100\">\n<circle cx=\"50\" cy=\"50\" r=\"40\" fill=\"red\"/>\n</svg>"
            1       | Size.of(-1, -1)   | Size.of( 50, 100) | Size.of( 50, 100) || "<svg width=\"50%\" height=\"100%\" viewBox=\"0 0 100 100\">\n<circle cx=\"50\" cy=\"50\" r=\"40\" fill=\"green\"/>\n</svg>"
            1       | Size.of(-1, -1)   | Size.of( 75,  25) | Size.of( 75,  25) || "<svg width=\"75%\" height=\"25%\" viewBox=\"0 0 100 100\">\n<circle cx=\"50\" cy=\"50\" r=\"40\" fill=\"blue\"/>\n</svg>"
            1       | Size.of(-1, -1)   | Size.of(200, 100) | Size.of(200, 100) || "<svg width=\"100%\" height=\"50%\" viewBox=\"0 0 200 200\">\n<circle cx=\"50\" cy=\"50\" r=\"40\" fill=\"red\"/>\n</svg>"
            1       | Size.of(-1, -1)   | Size.of(100, 200) | Size.of(100, 200) || "<svg width=\"50%\" height=\"100%\" viewBox=\"0 0 200 200\">\n<circle cx=\"50\" cy=\"50\" r=\"40\" fill=\"green\"/>\n</svg>"
            1       | Size.of(-1, -1)   | Size.of(150,  50) | Size.of(150,  50) || "<svg width=\"75%\" height=\"25%\" viewBox=\"0 0 200 200\">\n<circle cx=\"50\" cy=\"50\" r=\"40\" fill=\"blue\"/>\n</svg>"

            // Mixed units (percentage and pixels)
            1       | Size.of(24, -1)   | Size.of(24, -1)   | Size.of(24, 24)   || "<svg width=\"24\" height=\"50%\" viewBox=\"0 0 100 100\">\n<circle cx=\"50\" cy=\"50\" r=\"40\" fill=\"white\"/>\n</svg>"
            1       | Size.of(-1, 16)   | Size.of(-1, 16)   | Size.of(16, 16)   || "<svg width=\"50%\" height=\"16\" viewBox=\"0 0 100 100\">\n<circle cx=\"50\" cy=\"50\" r=\"40\" fill=\"red\"/>\n</svg>"

            // Different viewBox dimensions
            1       | Size.of(-1, -1)   | Size.of(48, 24)   | Size.of(48, 24)   || "<svg width=\"100%\" height=\"50%\" viewBox=\"0 0 48 48\">\n<circle cx=\"24\" cy=\"24\" r=\"20\" fill=\"red\"/>\n</svg>"
            1       | Size.of(-1, -1)   | Size.of(32, 64)   | Size.of(32, 64)   || "<svg width=\"50%\" height=\"100%\" viewBox=\"0 0 64 64\">\n<circle cx=\"32\" cy=\"32\" r=\"30\" fill=\"salmon\"/>\n</svg>"

            // Only one percentage dimension
            1       | Size.of(-1, 60)   | Size.of(-1, 60 )  | Size.of(60, 60 )  || "<svg width=\"100%\" height=\"60\" viewBox=\"0 0 100 100\">\n<circle cx=\"50\" cy=\"50\" r=\"40\" fill=\"red\"/>\n</svg>"
            1       | Size.of(30, -1)   | Size.of( 30, -1)  | Size.of( 30, 30)  || "<svg width=\"30\" height=\"50%\" viewBox=\"0 0 100 100\">\n<circle cx=\"50\" cy=\"50\" r=\"40\" fill=\"lime\"/>\n</svg>"

            // No percentages (should remain unchanged)
            1       | Size.of(100, 100) | Size.of(100, 100) | Size.of(100, 100) || "<svg width=\"100\" height=\"100\" viewBox=\"0 0 100 100\">\n<circle cx=\"50\" cy=\"50\" r=\"40\" fill=\"red\"/>\n</svg>"
            1       | Size.of(50, 75)   | Size.of( 50,  75) | Size.of( 50,  75) || "<svg width=\"50\" height=\"75\" viewBox=\"0 0 100 100\">\n<circle cx=\"50\" cy=\"50\" r=\"40\" fill=\"red\"/>\n</svg>"

            // View boxes that are not square, where each percentage is measured against its own dimension
            1       | Size.of(-1, -1)   | Size.of(200,  50) | Size.of(200,  50) || "<svg width=\"100%\" height=\"50%\" viewBox=\"0 0 200 100\">\n<circle cx=\"50\" cy=\"50\" r=\"40\" fill=\"red\"/>\n</svg>"
            1       | Size.of(-1, -1)   | Size.of(100, 100) | Size.of(100, 100) || "<svg width=\"50%\" height=\"100%\" viewBox=\"0 0 200 100\">\n<circle cx=\"50\" cy=\"50\" r=\"40\" fill=\"red\"/>\n</svg>"
            1       | Size.of(-1, -1)   | Size.of( 20,  30) | Size.of( 20,  30) || "<svg width=\"25%\" height=\"75%\" viewBox=\"0 0 80 40\">\n<circle cx=\"20\" cy=\"20\" r=\"15\" fill=\"red\"/>\n</svg>"
            2       | Size.of(-1, -1)   | Size.of( 60,  10) | Size.of( 60,  10) || "<svg width=\"20%\" height=\"10%\" viewBox=\"0 0 300 100\">\n<circle cx=\"50\" cy=\"50\" r=\"40\" fill=\"red\"/>\n</svg>"

            // With UI scaling
            2       | Size.of(-1, -1)   | Size.of(100,  50) | Size.of(100,  50) || "<svg width=\"100%\" height=\"50%\" viewBox=\"0 0 100 100\">\n<circle cx=\"50\" cy=\"50\" r=\"40\" fill=\"red\"/>\n</svg>"
            3       | Size.of(-1, -1)   | Size.of( 50, 100) | Size.of( 50, 100) || "<svg width=\"50%\" height=\"100%\" viewBox=\"0 0 100 100\">\n<circle cx=\"50\" cy=\"50\" r=\"40\" fill=\"red\"/>\n</svg>"
            2       | Size.of(-1, -1)   | Size.of( 50,  25) | Size.of( 50,  25) || "<svg width=\"50%\" height=\"25%\" viewBox=\"0 0 100 100\">\n<circle cx=\"50\" cy=\"50\" r=\"40\" fill=\"red\"/>\n</svg>"
            3       | Size.of(24, -1)   | Size.of(24, -1)   | Size.of(24, 24)   || "<svg width=\"24\" height=\"50%\" viewBox=\"0 0 100 100\">\n<circle cx=\"50\" cy=\"50\" r=\"40\" fill=\"white\"/>\n</svg>"
            2       | Size.of(-1, 16)   | Size.of(-1, 16)   | Size.of(16, 16)   || "<svg width=\"50%\" height=\"16\" viewBox=\"0 0 100 100\">\n<circle cx=\"50\" cy=\"50\" r=\"40\" fill=\"red\"/>\n</svg>"
    }

    def 'The `withIconSizeFromWidth` method converts percentage-based icons to pixel-based icons and maintains aspect ratio.'(
        float uiScale, int newWidth, Size expectedSize, String svg
    ) {
        reportInfo """
            The `withIconSizeFromWidth` method should convert percentage-based dimensions
            to pixel-based dimensions while maintaining the aspect ratio of the SVG document.
            
            When called on an SVG with percentage dimensions, the resulting icon should:
            1. Have pixel-based units (PX) instead of percentage units
            2. Calculate the height based on the aspect ratio of the SVG's viewBox
            3. Apply the UI scaling factor correctly
        """
        given : 'We start with an initial UI scale.'
            SwingTree.initializeUsing(it -> it.uiScaleFactor(uiScale) )
        and : 'We create an SvgIcon from the provided SVG string.'
            var originalIcon = SvgIcon.of(svg)
        expect : 'The original icon has percentage-based dimensions.'
            originalIcon.getIconWidth() == -1
            originalIcon.getIconHeight() == -1

        when : 'We call withIconSizeFromWidth with a new width.'
            var modifiedIcon = originalIcon.withIconSizeFromWidth(newWidth)
        then : 'The modified icon has pixel-based dimensions with correct aspect ratio.'
            modifiedIcon.getIconWidth() == expectedSize.width().map(UI::scale).map(Math::round).orElse(-1f)
            modifiedIcon.getIconHeight() == expectedSize.height().map(UI::scale).map(Math::round).orElse(-1f)
        and : 'The base dimensions are as expected.'
            modifiedIcon.getBaseWidth() == expectedSize.width().map(Math::round).orElse(-1f)
            modifiedIcon.getBaseHeight() == expectedSize.height().map(Math::round).orElse(-1f)

        cleanup :
            SwingTree.clear()

        where :
            uiScale | newWidth | expectedSize || svg

            // Square aspect ratio (1:1)
            1       | 100      | Size.of(100, 100) || "<svg width=\"100%\" height=\"100%\" viewBox=\"0 0 100 100\">\n<circle cx=\"50\" cy=\"50\" r=\"40\" fill=\"red\"/>\n</svg>"
            1       | 50       | Size.of(50, 50)   || "<svg width=\"100%\" height=\"100%\" viewBox=\"0 0 100 100\">\n<circle cx=\"50\" cy=\"50\" r=\"40\" fill=\"red\"/>\n</svg>"
            2       | 100      | Size.of(100, 100) || "<svg width=\"100%\" height=\"100%\" viewBox=\"0 0 100 100\">\n<circle cx=\"50\" cy=\"50\" r=\"40\" fill=\"red\"/>\n</svg>"

            // Rectangular aspect ratio (2:1)
            1       | 200      | Size.of(200, 100) || "<svg width=\"100%\" height=\"100%\" viewBox=\"0 0 200 100\">\n<rect x=\"0\" y=\"0\" width=\"200\" height=\"100\" fill=\"blue\"/>\n</svg>"
            1       | 100      | Size.of(100, 50)  || "<svg width=\"100%\" height=\"100%\" viewBox=\"0 0 200 100\">\n<rect x=\"0\" y=\"0\" width=\"200\" height=\"100\" fill=\"blue\"/>\n</svg>"
            2       | 200      | Size.of(200, 100) || "<svg width=\"100%\" height=\"100%\" viewBox=\"0 0 200 100\">\n<rect x=\"0\" y=\"0\" width=\"200\" height=\"100\" fill=\"blue\"/>\n</svg>"

            // Different percentage values
            1       | 75       | Size.of(75, 75)   || "<svg width=\"50%\" height=\"25%\" viewBox=\"0 0 100 100\">\n<circle cx=\"50\" cy=\"50\" r=\"40\" fill=\"green\"/>\n</svg>"
            2       | 6        | Size.of(6,   3)   || "<svg width=\"60%\" height=\"75%\" viewBox=\"0 0 200 100\">\n<circle cx=\"50\" cy=\"50\" r=\"40\" fill=\"green\"/>\n</svg>"
    }

    def 'An `SvgIcon` can be converted to a buffered image.'(
            float uiScale, String imgToMatch, String svg
    ) {
        reportInfo """
            You can easily convert an `SvgIcon` into a `BufferedImage` using the `getImage()` method.
            This will return a buffered image which has the same DPI dimensions as the `SvgIcon`
            reported by `getIconWidth()` and `getIconHeight()`.
            
            ${Utility.linkSnapshot("svgAsPng/${imgToMatch}.png")}
            
            Note that if the SVG icon has a distorted aspect ratio, which happens when the
            view box aspect ratio does not match the width and height aspect ratio, then
            the resulting image will be rendered with the expected distortion, exactly
            like the `SvgIcon` would render itself onto a component for example...
        """
        given :
            SwingTree.initializeUsing(it -> it.uiScaleFactor(uiScale) )
        and :
            var declaration = IconDeclaration.ofSvg(svg)
            var svgIcon = declaration.find().orElseThrow(IllegalStateException::new)
        when : 'We create a buffered image from the SVG based icon...'
            var img = svgIcon.getImage() as BufferedImage
        then : 'It matched the PNGs stored in the test snapshots folder!'
            Utility.similarityBetween(img, "svgAsPng/${imgToMatch}.png", 99.5) > 99.5

        cleanup :
            SwingTree.clear()

        where :
          uiScale | imgToMatch                ||  svg
              1   | 'blue-circle-1'           || "<svg width=\"100%\" height=\"100%\" viewBox=\"0 0 100 100\">\n<circle cx=\"50\" cy=\"50\" r=\"40\" fill=\"blue\"/>\n</svg>"
              2   | 'blue-circle-2'           || "<svg width=\"100%\" height=\"100%\" viewBox=\"0 0 100 100\">\n<circle cx=\"50\" cy=\"50\" r=\"40\" fill=\"blue\"/>\n</svg>"
              1   | 'green-stretched-circle-1'|| "<svg width=\"200px\" height=\"100px\" viewBox=\"0 0 100 100\">\n<circle cx=\"50\" cy=\"50\" r=\"40\" fill=\"green\"/>\n</svg>"
              2   | 'green-stretched-circle-2'|| "<svg width=\"100px\" height=\"200px\" viewBox=\"0 0 100 100\">\n<circle cx=\"50\" cy=\"50\" r=\"40\" fill=\"green\"/>\n</svg>"
    }

    def 'An SVG passed to the style API is rendered onto the component according to placement, fit mode and its declared size.'(
        float uiScale, String imgToMatch, String svgWidth, String svgHeight, String viewBox, UI.Placement placement, UI.FitComponent fitMode
    ) {
        reportInfo """
            Instead of creating an `SvgIcon` yourself, you can also pass raw SVG document code
            directly to the style API using the `svg(String)` method of the "image" sub-style.
            There you can also configure how the SVG is placed and stretched
            inside the component through the `placement(UI.Placement)`
            and `fitMode(UI.FitComponent)` methods.

            In this example we render an SVG document declaring a single orange rectangle
            which covers the entire SVG viewport except for a 5 pixel wide margin on each side.
            The SVG is rendered onto a light gray `JBox` sized 90x60 pixels,
            which produces the following image:

            ${Utility.linkSnapshot("svgInBox/${imgToMatch}.png")}

            Note that when the aspect ratio formed by the SVG's width and height
            differs from the aspect ratio of its view box, then the rectangle
            is distorted accordingly, exactly like in a browser.
            The fit mode based scaling on the other hand, stretches the
            SVG viewport relative to the size of the component.
        """
        given : 'We initialize SwingTree with the UI scaling factor of the current data table row:'
            SwingTree.initializeUsing(it -> it.uiScaleFactor(uiScale) )
        and : """
            We construct the SVG document from the width, height and view box parameters.
            The rectangle is derived from the view box so that it always covers the
            entire viewport except for a margin of 5 pixels on each of its sides.
        """
            var viewBoxParts = viewBox.split(" ")
            var rectWidth  = (viewBoxParts[2] as int) - 10
            var rectHeight = (viewBoxParts[3] as int) - 10
            var svg = "<svg width=\"$svgWidth\" height=\"$svgHeight\" viewBox=\"$viewBox\">\n" +
                      "  <rect x=\"5\" y=\"5\" width=\"$rectWidth\" height=\"$rectHeight\" fill=\"orange\"/>\n" +
                      "</svg>"
        and : 'A `JBox` which receives the SVG code together with the current placement and fit mode through the style API:'
            var ui =
                    UI.box().withStyle( it -> it
                        .size(90, 60)
                        .backgroundColor(UI.Color.LIGHTGRAY)
                        .image( conf -> conf
                            .svg(svg)
                            .placement(placement)
                            .fitMode(fitMode)
                        )
                    )

        when : 'We render the box into a buffered image...'
            var img = Utility.renderSingleComponent(ui.get(JBox))
        then : 'It matches the PNG stored in the test snapshots folder!'
            Utility.similarityBetween(img, "svgInBox/${imgToMatch}.png", 99.9) > 99.9

        cleanup :
            SwingTree.clear()

        where :
            uiScale | imgToMatch               | svgWidth | svgHeight | viewBox       | placement                 | fitMode
            // A small square SVG placed at the various placement locations:
            1       | 'square-center-1'        | "30"     | "30"      | "0 0 100 100" | UI.Placement.CENTER       | UI.FitComponent.NO
            1       | 'square-top-left-1'      | "30"     | "30"      | "0 0 100 100" | UI.Placement.TOP_LEFT     | UI.FitComponent.NO
            1       | 'square-top-1'           | "30"     | "30"      | "0 0 100 100" | UI.Placement.TOP          | UI.FitComponent.NO
            1       | 'square-top-right-1'     | "30"     | "30"      | "0 0 100 100" | UI.Placement.TOP_RIGHT    | UI.FitComponent.NO
            1       | 'square-right-1'         | "30"     | "30"      | "0 0 100 100" | UI.Placement.RIGHT        | UI.FitComponent.NO
            1       | 'square-bottom-right-1'  | "30"     | "30"      | "0 0 100 100" | UI.Placement.BOTTOM_RIGHT | UI.FitComponent.NO
            1       | 'square-bottom-1'        | "30"     | "30"      | "0 0 100 100" | UI.Placement.BOTTOM       | UI.FitComponent.NO
            1       | 'square-bottom-left-1'   | "30"     | "30"      | "0 0 100 100" | UI.Placement.BOTTOM_LEFT  | UI.FitComponent.NO
            1       | 'square-left-1'          | "30"     | "30"      | "0 0 100 100" | UI.Placement.LEFT         | UI.FitComponent.NO
            2       | 'square-center-2'        | "30"     | "30"      | "0 0 100 100" | UI.Placement.CENTER       | UI.FitComponent.NO
            // A width/height aspect ratio different from the view box aspect ratio distorts the rectangle:
            1       | 'squashed-flat-1'        | "60"     | "20"      | "0 0 100 100" | UI.Placement.CENTER       | UI.FitComponent.NO
            1       | 'stretched-tall-1'       | "20"     | "50"      | "0 0 100 100" | UI.Placement.CENTER       | UI.FitComponent.NO
            1       | 'stretched-wide-1'       | "40"     | "40"      | "0 0 50 100"  | UI.Placement.CENTER       | UI.FitComponent.NO
            2       | 'squashed-flat-2'        | "60"     | "20"      | "0 0 100 100" | UI.Placement.CENTER       | UI.FitComponent.NO
            // Distorted rectangles can of course also be placed anywhere in the component:
            1       | 'squashed-flat-top-left-1'      | "40" | "14"   | "0 0 100 100" | UI.Placement.TOP_LEFT     | UI.FitComponent.NO
            1       | 'squashed-flat-top-right-1'     | "40" | "14"   | "0 0 100 100" | UI.Placement.TOP_RIGHT    | UI.FitComponent.NO
            1       | 'squashed-flat-bottom-1'        | "40" | "14"   | "0 0 100 100" | UI.Placement.BOTTOM       | UI.FitComponent.NO
            1       | 'stretched-tall-left-1'         | "14" | "40"   | "0 0 100 100" | UI.Placement.LEFT         | UI.FitComponent.NO
            1       | 'stretched-tall-top-1'          | "14" | "40"   | "0 0 100 100" | UI.Placement.TOP          | UI.FitComponent.NO
            1       | 'stretched-tall-bottom-right-1' | "14" | "40"   | "0 0 100 100" | UI.Placement.BOTTOM_RIGHT | UI.FitComponent.NO
            2       | 'squashed-flat-bottom-left-2'   | "40" | "14"   | "0 0 100 100" | UI.Placement.BOTTOM_LEFT  | UI.FitComponent.NO
            // The fit mode determines how the SVG viewport is stretched onto the 90x60 component:
            1       | 'fit-width-and-height-1' | "30"     | "30"      | "0 0 100 100" | UI.Placement.CENTER       | UI.FitComponent.WIDTH_AND_HEIGHT
            1       | 'fit-min-dim-1'          | "30"     | "30"      | "0 0 100 100" | UI.Placement.CENTER       | UI.FitComponent.MIN_DIM
            1       | 'fit-max-dim-1'          | "30"     | "30"      | "0 0 100 100" | UI.Placement.CENTER       | UI.FitComponent.MAX_DIM
            1       | 'fit-width-1'            | "30"     | "30"      | "0 0 100 100" | UI.Placement.CENTER       | UI.FitComponent.WIDTH
            1       | 'fit-height-1'           | "30"     | "30"      | "0 0 100 100" | UI.Placement.CENTER       | UI.FitComponent.HEIGHT
            2       | 'fit-min-dim-2'          | "30"     | "30"      | "0 0 100 100" | UI.Placement.CENTER       | UI.FitComponent.MIN_DIM
            // An SVG with percentage based dimensions has no inherent size and so it always stretches over the component:
            1       | 'percent-size-no-fit-1'  | "100%"   | "100%"    | "0 0 100 100" | UI.Placement.CENTER       | UI.FitComponent.NO
    }

    def 'The `withIconSizeFromHeight` method converts percentage-based icons to pixel-based icons and maintains aspect ratio.'(
        float uiScale, int newHeight, Size expectedSize, String svg
    ) {
        reportInfo """
            The `withIconSizeFromHeight` method should convert percentage-based dimensions
            to pixel-based dimensions while maintaining the view box based aspect ratio of the SVG document.
            
            When called on an SVG with percentage dimensions, the resulting icon should:
            1. Have pixel-based units (PX) instead of percentage units
            2. Calculate the width based on the aspect ratio of the SVG's viewBox
            3. Apply the UI scaling factor correctly
        """
        given : 'We start with an initial UI scale.'
            SwingTree.initializeUsing(it -> it.uiScaleFactor(uiScale) )
        and : 'We create an SvgIcon from the provided SVG string.'
            var originalIcon = SvgIcon.of(svg)
        expect : 'The original icon has percentage-based dimensions.'
            originalIcon.getIconWidth() == -1
            originalIcon.getIconHeight() == -1

        when : 'We call withIconSizeFromHeight with a new height.'
            var modifiedIcon = originalIcon.withIconSizeFromHeight(newHeight)
        then :
            modifiedIcon.widthUnitString() == "px"
            modifiedIcon.heightUnitString() == "px"
        and : 'The modified icon has pixel-based dimensions with correct aspect ratio.'
            modifiedIcon.getIconWidth() == expectedSize.width().map(UI::scale).map(Math::round).orElse(-1f)
            modifiedIcon.getIconHeight() == expectedSize.height().map(UI::scale).map(Math::round).orElse(-1f)
        and : 'The base dimensions are as expected.'
            modifiedIcon.getBaseWidth() == expectedSize.width().map(Math::round).orElse(-1f)
            modifiedIcon.getBaseHeight() == expectedSize.height().map(Math::round).orElse(-1f)

        cleanup :
            SwingTree.clear()

        where :
            uiScale | newHeight | expectedSize || svg

            // Square aspect ratio (1:1)
            1       | 100       | Size.of(100, 100) || "<svg width=\"100%\" height=\"100%\" viewBox=\"0 0 100 100\">\n<circle cx=\"50\" cy=\"50\" r=\"40\" fill=\"red\"/>\n</svg>"
            2       | 50        | Size.of(50, 50)   || "<svg width=\"100%\" height=\"100%\" viewBox=\"0 0 100 100\">\n<circle cx=\"50\" cy=\"50\" r=\"40\" fill=\"red\"/>\n</svg>"
            3       | 100       | Size.of(100, 100) || "<svg width=\"100%\" height=\"100%\" viewBox=\"0 0 100 100\">\n<circle cx=\"50\" cy=\"50\" r=\"40\" fill=\"red\"/>\n</svg>"

            // Percentage based square aspect ratio (1:1)
            1       | 200       | Size.of(400, 200) || "<svg width=\"50%\" height=\"100%\" viewBox=\"0 0 200 100\">\n<rect x=\"0\" y=\"0\" width=\"100\" height=\"200\" fill=\"blue\"/>\n</svg>"
            2       | 100       | Size.of(200, 100) || "<svg width=\"50%\" height=\"100%\" viewBox=\"0 0 200 100\">\n<rect x=\"0\" y=\"0\" width=\"100\" height=\"200\" fill=\"yellow\"/>\n</svg>"
            3       | 200       | Size.of(400, 200) || "<svg width=\"50%\" height=\"100%\" viewBox=\"0 0 200 100\">\n<rect x=\"0\" y=\"0\" width=\"100\" height=\"200\" fill=\"blue\"/>\n</svg>"

            // Rectangular aspect ratio (1:2)
            1       | 200       | Size.of(200, 200) || "<svg width=\"100%\" height=\"200%\" viewBox=\"0 0 100 100\">\n<rect x=\"0\" y=\"0\" width=\"100\" height=\"200\" fill=\"blue\"/>\n</svg>"
            2       | 100       | Size.of(100,100)  || "<svg width=\"100%\" height=\"200%\" viewBox=\"0 0 100 100\">\n<rect x=\"0\" y=\"0\" width=\"100\" height=\"200\" fill=\"black\"/>\n</svg>"
            3       | 200       | Size.of(200, 200) || "<svg width=\"100%\" height=\"200%\" viewBox=\"0 0 100 100\">\n<rect x=\"0\" y=\"0\" width=\"100\" height=\"200\" fill=\"blue\"/>\n</svg>"

            // Rectangular aspect ratio (1:2)
            1       | 200       | Size.of(100,200)  || "<svg width=\"100%\" height=\"200%\" viewBox=\"0 0 100 200\">\n<rect x=\"0\" y=\"0\" width=\"100\" height=\"200\" fill=\"blue\"/>\n</svg>"
            2       | 100       | Size.of(50, 100)  || "<svg width=\"100%\" height=\"200%\" viewBox=\"0 0 100 200\">\n<rect x=\"0\" y=\"0\" width=\"100\" height=\"200\" fill=\"black\"/>\n</svg>"
            3       | 200       | Size.of(100, 200) || "<svg width=\"100%\" height=\"200%\" viewBox=\"0 0 100 200\">\n<rect x=\"0\" y=\"0\" width=\"100\" height=\"200\" fill=\"blue\"/>\n</svg>"

            // Percentage based aspect ratio (1:2)
            1       | 200       | Size.of(200, 200) || "<svg width=\"50%\" height=\"100%\" viewBox=\"0 0 100 100\">\n<rect x=\"0\" y=\"0\" width=\"100\" height=\"200\" fill=\"blue\"/>\n</svg>"
            2       | 100       | Size.of(100, 100) || "<svg width=\"50%\" height=\"100%\" viewBox=\"0 0 100 100\">\n<rect x=\"0\" y=\"0\" width=\"100\" height=\"200\" fill=\"yellow\"/>\n</svg>"
            3       | 200       | Size.of(200, 200) || "<svg width=\"50%\" height=\"100%\" viewBox=\"0 0 100 100\">\n<rect x=\"0\" y=\"0\" width=\"100\" height=\"200\" fill=\"blue\"/>\n</svg>"

            // Different percentage values
            1       | 60        | Size.of(120, 60)  || "<svg width=\"75%\" height=\"50%\" viewBox=\"0 0 200 100\">\n<circle cx=\"100\" cy=\"50\" r=\"40\" fill=\"green\"/>\n</svg>"
            2       | 60        | Size.of(120, 60)  || "<svg width=\"75%\" height=\"50%\" viewBox=\"0 0 200 100\">\n<circle cx=\"100\" cy=\"50\" r=\"40\" fill=\"green\"/>\n</svg>"
            3       | 60        | Size.of(120, 60)  || "<svg width=\"75%\" height=\"50%\" viewBox=\"0 0 200 100\">\n<circle cx=\"100\" cy=\"50\" r=\"40\" fill=\"green\"/>\n</svg>"
    }

    def 'The `withIconSize` method may convert percentage-based icons to pixel-based icons and maintains aspect ratio.'(
        float uiScale,
        Size initSize,
        Tuple2<String,String> initUnits,
        Size newSize,
        Size expectedSize,
        Tuple2<String,String> expectedUnits,
        String svg
    ) {
        given : 'We start with an initial UI scale.'
            SwingTree.initializeUsing(it -> it.uiScaleFactor(uiScale) )
        and : 'We create an SvgIcon from the provided SVG string.'
            var originalIcon = SvgIcon.of(svg)
        expect : 'The original icon has percentage-based dimensions.'
            originalIcon.getIconWidth() == initSize.width().map(UI::scale).map(Math::round).orElse(-1)
            originalIcon.getIconHeight() == initSize.height().map(UI::scale).map(Math::round).orElse(-1)
            originalIcon.getBaseWidth() == initSize.width().map(Math::round).orElse(-1)
            originalIcon.getBaseHeight() == initSize.height().map(Math::round).orElse(-1)
        and : 'The units are as expected:'
            originalIcon.widthUnitString() == initUnits.v1
            originalIcon.heightUnitString() == initUnits.v2

        when : 'We call withIconSizeFromHeight with a new height.'
            var modifiedIcon = originalIcon.withIconSize(newSize)
        then : 'The modified icon has pixel-based dimensions with correct aspect ratio.'
            modifiedIcon.getIconWidth() == expectedSize.width().map(UI::scale).map(Math::round).orElse(-1)
            modifiedIcon.getIconHeight() == expectedSize.height().map(UI::scale).map(Math::round).orElse(-1)
        and : 'The base dimensions are as expected.'
            modifiedIcon.getBaseWidth() == (!newSize.width().isPresent() ? -1 : expectedSize.width().map(Math::round).orElse(-1))
            modifiedIcon.getBaseHeight() == (!newSize.height().isPresent() ? -1 : expectedSize.height().map(Math::round).orElse(-1))
        and : 'The units are as expected:'
            modifiedIcon.widthUnitString() == expectedUnits.v1
            modifiedIcon.heightUnitString() == expectedUnits.v2

        cleanup :
            SwingTree.clear()

        where :
            uiScale |  initSize        | initUnits  | newSize          | expectedSize      | expectedUnits || svg
            1       | Size.of(-1,-1)   | ["%","%"]  | Size.of(100, 100)| Size.of(100, 100) | ["px","px"]   || "<svg width=\"100%\" height=\"100%\" viewBox=\"0 0 100 100\">\n<circle cx=\"50\" cy=\"50\" r=\"40\" fill=\"red\"/>\n</svg>"
            2       | Size.of(-1,-1)   | ["%","%"]  | Size.of(50, 50)  | Size.of(50, 50)   | ["px","px"]   || "<svg width=\"100%\" height=\"100%\" viewBox=\"0 0 100 100\">\n<circle cx=\"50\" cy=\"50\" r=\"40\" fill=\"red\"/>\n</svg>"
            3       | Size.of(-1,-1)   | ["%","%"]  | Size.of(100, 100)| Size.of(100, 100) | ["px","px"]   || "<svg width=\"100%\" height=\"100%\" viewBox=\"0 0 100 100\">\n<circle cx=\"50\" cy=\"50\" r=\"40\" fill=\"red\"/>\n</svg>"
            1       | Size.of(-1,-1)   | ["%","%"]  | Size.of(-1, -1)  | Size.of(-1, -1)   | ["px","px"]   || "<svg width=\"100%\" height=\"100%\" viewBox=\"0 0 100 100\">\n<circle cx=\"50\" cy=\"50\" r=\"40\" fill=\"red\"/>\n</svg>"
            2       | Size.of(-1,-1)   | ["%","%"]  | Size.of(2,  -1)  | Size.of( 2,  2)   | ["px","px"]   || "<svg width=\"100%\" height=\"100%\" viewBox=\"0 0 100 100\">\n<circle cx=\"50\" cy=\"50\" r=\"40\" fill=\"red\"/>\n</svg>"
            3       | Size.of(-1,-1)   | ["%","%"]  | Size.of(-1,  4)  | Size.of( 4,  4)   | ["px","px"]   || "<svg width=\"100%\" height=\"100%\" viewBox=\"0 0 100 100\">\n<circle cx=\"50\" cy=\"50\" r=\"40\" fill=\"red\"/>\n</svg>"
            1       | Size.of(100,100) | ["px","px"]| Size.of(100, 100)| Size.of(100, 100) | ["px","px"]   || "<svg width=\"100px\" height=\"100px\" viewBox=\"0 0 100 100\">\n<circle cx=\"50\" cy=\"50\" r=\"40\" fill=\"red\"/>\n</svg>"
            2       | Size.of(100,-1)  | ["px","%"] | Size.of(50, 50)  | Size.of(50, 50)   | ["px","px"]   || "<svg width=\"100px\" height=\"100%\" viewBox=\"0 0 100 100\">\n<circle cx=\"50\" cy=\"50\" r=\"40\" fill=\"red\"/>\n</svg>"
            3       | Size.of(-1,100)  | ["%","px"] | Size.of(100, 100)| Size.of(100, 100) | ["px","px"]   || "<svg width=\"100%\" height=\"100px\" viewBox=\"0 0 100 100\">\n<circle cx=\"50\" cy=\"50\" r=\"40\" fill=\"red\"/>\n</svg>"
            1       | Size.of(100,100) | ["px","px"]| Size.of(-1, -1)  | Size.of(-1, -1)   | ["px","px"]   || "<svg width=\"100px\" height=\"100px\" viewBox=\"0 0 100 100\">\n<circle cx=\"50\" cy=\"50\" r=\"40\" fill=\"red\"/>\n</svg>"
            2       | Size.of(100,-1)  | ["px","%"] | Size.of(2,  -1)  | Size.of( 2,  2)   | ["px","px"]   || "<svg width=\"100px\" height=\"100%\" viewBox=\"0 0 100 100\">\n<circle cx=\"50\" cy=\"50\" r=\"40\" fill=\"red\"/>\n</svg>"
            3       | Size.of(-1,100)  | ["%","px"] | Size.of(-1,  4)  | Size.of( 4,  4)   | ["px","px"]   || "<svg width=\"100%\" height=\"100px\" viewBox=\"0 0 100 100\">\n<circle cx=\"50\" cy=\"50\" r=\"40\" fill=\"red\"/>\n</svg>"
            1       | Size.of(-1,100)  | ["%","px"] | Size.of(2,  -1)  | Size.of( 2,  2)   | ["px","px"]   || "<svg width=\"100%\" height=\"100px\" viewBox=\"0 0 100 100\">\n<circle cx=\"50\" cy=\"50\" r=\"40\" fill=\"red\"/>\n</svg>"
            2       | Size.of(100,-2)  | ["px","%"] | Size.of(-1,  4)  | Size.of( 4,  4)   | ["px","px"]   || "<svg width=\"100px\" height=\"100%\" viewBox=\"0 0 100 100\">\n<circle cx=\"50\" cy=\"50\" r=\"40\" fill=\"red\"/>\n</svg>"
    }

    def 'The `withIconSizeFromWidth` derives SVG icons with complete size.'(
        float uiScale,
        Size initSize,
        Tuple2<String,String> initUnits,
        int newWidth,
        Size expectedSize,
        Tuple2<String,String> expectedUnits,
        String svg
    ) {
        given : 'We start with an initial UI scale.'
            SwingTree.initializeUsing(it -> it.uiScaleFactor(uiScale) )
        and : 'We create an SvgIcon from the provided SVG string.'
            var originalIcon = SvgIcon.of(svg)
        expect : 'The original icon has percentage-based dimensions.'
            originalIcon.getIconWidth() == initSize.width().map(UI::scale).map(Math::round).orElse(-1)
            originalIcon.getIconHeight() == initSize.height().map(UI::scale).map(Math::round).orElse(-1)
            originalIcon.getBaseWidth() == initSize.width().map(Math::round).orElse(-1)
            originalIcon.getBaseHeight() == initSize.height().map(Math::round).orElse(-1)
        and : 'The units are as expected:'
            originalIcon.widthUnitString() == initUnits.v1
            originalIcon.heightUnitString() == initUnits.v2

        when : 'We call withIconSizeFromHeight with a new height.'
            var modifiedIcon = originalIcon.withIconSizeFromWidth(newWidth)
        then : 'The modified icon has pixel-based dimensions with correct aspect ratio.'
            modifiedIcon.getIconWidth() == expectedSize.width().map(UI::scale).map(Math::round).orElse(-1)
            modifiedIcon.getIconHeight() == expectedSize.height().map(UI::scale).map(Math::round).orElse(-1)
        and : 'The base dimensions are as expected.'
            modifiedIcon.getBaseWidth() == (newWidth < 0 ? -1 : expectedSize.width().map(Math::round).orElse(-1))
            modifiedIcon.getBaseHeight() == (expectedUnits.v2 == "%" ? -1 : expectedSize.height().map(Math::round).orElse(-1))
        and : 'The units are as expected:'
            modifiedIcon.widthUnitString() == expectedUnits.v1
            modifiedIcon.heightUnitString() == expectedUnits.v2

        cleanup :
            SwingTree.clear()

        where :
            uiScale |  initSize        | initUnits  | newWidth | expectedSize      | expectedUnits || svg
            1       | Size.of(-1,-1)   | ["%","%"]  | 100      | Size.of(100, 100) | ["px","px"]   || "<svg width=\"100%\" height=\"100%\" viewBox=\"0 0 100 100\">\n<circle cx=\"50\" cy=\"50\" r=\"40\" fill=\"red\"/>\n</svg>"
            2       | Size.of(-1,-1)   | ["%","%"]  | 50       | Size.of(50, 50)   | ["px","px"]   || "<svg width=\"100%\" height=\"100%\" viewBox=\"0 0 100 100\">\n<circle cx=\"50\" cy=\"50\" r=\"40\" fill=\"red\"/>\n</svg>"
            3       | Size.of(-1,-1)   | ["%","%"]  | 100      | Size.of(100, 100) | ["px","px"]   || "<svg width=\"100%\" height=\"100%\" viewBox=\"0 0 100 100\">\n<circle cx=\"50\" cy=\"50\" r=\"40\" fill=\"red\"/>\n</svg>"
            1       | Size.of(-1,-1)   | ["%","%"]  | -1       | Size.of(-1, -1)   | ["px","px"]   || "<svg width=\"100%\" height=\"100%\" viewBox=\"0 0 100 100\">\n<circle cx=\"50\" cy=\"50\" r=\"40\" fill=\"red\"/>\n</svg>"
            2       | Size.of(-1,-1)   | ["%","%"]  | 2        | Size.of( 2,  2)   | ["px","px"]   || "<svg width=\"100%\" height=\"100%\" viewBox=\"0 0 100 100\">\n<circle cx=\"50\" cy=\"50\" r=\"40\" fill=\"red\"/>\n</svg>"
            3       | Size.of(-1,-1)   | ["%","%"]  | -1       | Size.of(-1, -1)   | ["px","px"]   || "<svg width=\"100%\" height=\"100%\" viewBox=\"0 0 100 100\">\n<circle cx=\"50\" cy=\"50\" r=\"40\" fill=\"red\"/>\n</svg>"
            1       | Size.of(100,100) | ["px","px"]| 100      | Size.of(100, 100) | ["px","px"]   || "<svg width=\"100px\" height=\"100px\" viewBox=\"0 0 100 100\">\n<circle cx=\"50\" cy=\"50\" r=\"40\" fill=\"red\"/>\n</svg>"
            2       | Size.of(100,-1)  | ["px","%"] | 50       | Size.of(50, 50)   | ["px","px"]   || "<svg width=\"100px\" height=\"100%\" viewBox=\"0 0 100 100\">\n<circle cx=\"50\" cy=\"50\" r=\"40\" fill=\"red\"/>\n</svg>"
            3       | Size.of(-1,100)  | ["%","px"] | 100      | Size.of(100, 100) | ["px","px"]   || "<svg width=\"100%\" height=\"100px\" viewBox=\"0 0 100 100\">\n<circle cx=\"50\" cy=\"50\" r=\"40\" fill=\"red\"/>\n</svg>"
            1       | Size.of(100,100) | ["px","px"]| -1       | Size.of(-1, -1)   | ["px","px"]   || "<svg width=\"100px\" height=\"100px\" viewBox=\"0 0 100 100\">\n<circle cx=\"50\" cy=\"50\" r=\"40\" fill=\"red\"/>\n</svg>"
            2       | Size.of(100,-1)  | ["px","%"] | 2        | Size.of( 2,  2)   | ["px","px"]   || "<svg width=\"100px\" height=\"100%\" viewBox=\"0 0 100 100\">\n<circle cx=\"50\" cy=\"50\" r=\"40\" fill=\"red\"/>\n</svg>"
            3       | Size.of(-1,100)  | ["%","px"] | -1       | Size.of(-1, -1)   | ["px","px"]   || "<svg width=\"100%\" height=\"100px\" viewBox=\"0 0 100 100\">\n<circle cx=\"50\" cy=\"50\" r=\"40\" fill=\"red\"/>\n</svg>"
            1       | Size.of(-1,100)  | ["%","px"] | 2        | Size.of( 2,  2)   | ["px","px"]   || "<svg width=\"100%\" height=\"100px\" viewBox=\"0 0 100 100\">\n<circle cx=\"50\" cy=\"50\" r=\"40\" fill=\"red\"/>\n</svg>"
            2       | Size.of(100,-1)  | ["px","%"] | -1       | Size.of(-1, -1)   | ["px","px"]   || "<svg width=\"100px\" height=\"100%\" viewBox=\"0 0 100 100\">\n<circle cx=\"50\" cy=\"50\" r=\"40\" fill=\"red\"/>\n</svg>"

            1       | Size.of(-1,-1)   | ["%","%"]  | 100      | Size.of(100, 50)  | ["px","px"]   || "<svg width=\"100%\" height=\"100%\" viewBox=\"0 0 100 50\">\n<circle cx=\"50\" cy=\"50\" r=\"40\" fill=\"red\"/>\n</svg>"
            2       | Size.of(-1,-1)   | ["%","%"]  | 50       | Size.of(50, 25)   | ["px","px"]   || "<svg width=\"100%\" height=\"100%\" viewBox=\"0 0 100 50\">\n<circle cx=\"50\" cy=\"50\" r=\"40\" fill=\"red\"/>\n</svg>"
            3       | Size.of(-1,-1)   | ["%","%"]  | 100      | Size.of(100, 50)  | ["px","px"]   || "<svg width=\"100%\" height=\"100%\" viewBox=\"0 0 100 50\">\n<circle cx=\"50\" cy=\"50\" r=\"40\" fill=\"red\"/>\n</svg>"
            1       | Size.of(-1,-1)   | ["%","%"]  | -1       | Size.of(-1, -1)   | ["px","px"]   || "<svg width=\"100%\" height=\"100%\" viewBox=\"0 0 100 50\">\n<circle cx=\"50\" cy=\"50\" r=\"40\" fill=\"red\"/>\n</svg>"
            2       | Size.of(-1,-1)   | ["%","%"]  | 2        | Size.of( 2,  1)   | ["px","px"]   || "<svg width=\"100%\" height=\"100%\" viewBox=\"0 0 100 50\">\n<circle cx=\"50\" cy=\"50\" r=\"40\" fill=\"red\"/>\n</svg>"
            3       | Size.of(-1,-1)   | ["%","%"]  | -1       | Size.of(-1, -1)   | ["px","px"]   || "<svg width=\"100%\" height=\"100%\" viewBox=\"0 0 100 50\">\n<circle cx=\"50\" cy=\"50\" r=\"40\" fill=\"red\"/>\n</svg>"
            1       | Size.of(100,100) | ["px","px"]| 100      | Size.of(100, 100) | ["px","px"]   || "<svg width=\"100px\" height=\"100px\" viewBox=\"0 0 100 50\">\n<circle cx=\"50\" cy=\"50\" r=\"40\" fill=\"red\"/>\n</svg>"
            2       | Size.of(100,-1)  | ["px","%"] | 50       | Size.of(50, 25)   | ["px","px"]   || "<svg width=\"100px\" height=\"100%\" viewBox=\"0 0 100 50\">\n<circle cx=\"50\" cy=\"50\" r=\"40\" fill=\"red\"/>\n</svg>"
            3       | Size.of(-1,100)  | ["%","px"] | 100      | Size.of(100,  50) | ["px","px"]   || "<svg width=\"100%\" height=\"100px\" viewBox=\"0 0 100 50\">\n<circle cx=\"50\" cy=\"50\" r=\"40\" fill=\"red\"/>\n</svg>"
            1       | Size.of(100,100) | ["px","px"]| -1       | Size.of(-1, -1)   | ["px","px"]   || "<svg width=\"100px\" height=\"100px\" viewBox=\"0 0 100 50\">\n<circle cx=\"50\" cy=\"50\" r=\"40\" fill=\"red\"/>\n</svg>"
            2       | Size.of(100,-1)  | ["px","%"] | 2        | Size.of( 2,  1)   | ["px","px"]   || "<svg width=\"100px\" height=\"100%\" viewBox=\"0 0 100 50\">\n<circle cx=\"50\" cy=\"50\" r=\"40\" fill=\"red\"/>\n</svg>"
            3       | Size.of(-1,100)  | ["%","px"] | -1       | Size.of(-1, -1)   | ["px","px"]   || "<svg width=\"100%\" height=\"100px\" viewBox=\"0 0 100 50\">\n<circle cx=\"50\" cy=\"50\" r=\"40\" fill=\"red\"/>\n</svg>"
            1       | Size.of(-1,100)  | ["%","px"] | 2        | Size.of( 2,  1)   | ["px","px"]   || "<svg width=\"100%\" height=\"100px\" viewBox=\"0 0 100 50\">\n<circle cx=\"50\" cy=\"50\" r=\"40\" fill=\"red\"/>\n</svg>"
            2       | Size.of(100,-1)  | ["px","%"] | -1       | Size.of(-1, -1)   | ["px","px"]   || "<svg width=\"100px\" height=\"100%\" viewBox=\"0 0 100 50\">\n<circle cx=\"50\" cy=\"50\" r=\"40\" fill=\"red\"/>\n</svg>"
    }

    def 'The `withIconSizeFromHeight` derives SVG icons with complete size.'(
        float uiScale,
        Size initSize,
        Tuple2<String,String> initUnits,
        int newHeight,
        Size expectedSize,
        Tuple2<String,String> expectedUnits,
        String svg
    ) {
        given : 'We start with an initial UI scale.'
            SwingTree.initializeUsing(it -> it.uiScaleFactor(uiScale) )
        and : 'We create an SvgIcon from the provided SVG string.'
            var originalIcon = SvgIcon.of(svg)
        expect : 'The original icon has the expected dimensions.'
            originalIcon.getIconWidth() == initSize.width().map(UI::scale).map(Math::round).orElse(-1)
            originalIcon.getIconHeight() == initSize.height().map(UI::scale).map(Math::round).orElse(-1)
            originalIcon.getBaseWidth() == initSize.width().map(Math::round).orElse(-1)
            originalIcon.getBaseHeight() == initSize.height().map(Math::round).orElse(-1)
        and : 'The units are as expected:'
            originalIcon.widthUnitString() == initUnits.v1
            originalIcon.heightUnitString() == initUnits.v2

        when : 'We call withIconSizeFromHeight with a new height.'
            var modifiedIcon = originalIcon.withIconSizeFromHeight(newHeight)
        then : 'The modified icon has pixel-based dimensions with correct aspect ratio.'
            modifiedIcon.getIconWidth() == expectedSize.width().map(UI::scale).map(Math::round).orElse(-1)
            modifiedIcon.getIconHeight() == expectedSize.height().map(UI::scale).map(Math::round).orElse(-1)
        and : 'The base dimensions are as expected.'
            modifiedIcon.getBaseWidth() == (expectedUnits.v1 == "%" ? -1 : expectedSize.width().map(Math::round).orElse(-1))
            modifiedIcon.getBaseHeight() == (newHeight < 0 ? -1 : expectedSize.height().map(Math::round).orElse(-1))
        and : 'The units are as expected:'
            modifiedIcon.widthUnitString() == expectedUnits.v1
            modifiedIcon.heightUnitString() == expectedUnits.v2

        cleanup :
            SwingTree.clear()

        where :
            uiScale |  initSize        | initUnits  | newHeight | expectedSize      | expectedUnits || svg
            1       | Size.of(-1,-1)   | ["%","%"]  | 100       | Size.of(100, 100) | ["px","px"]   || "<svg width=\"100%\" height=\"100%\" viewBox=\"0 0 100 100\">\n<circle cx=\"50\" cy=\"50\" r=\"40\" fill=\"red\"/>\n</svg>"
            2       | Size.of(-1,-1)   | ["%","%"]  | 50        | Size.of(50, 50)   | ["px","px"]   || "<svg width=\"100%\" height=\"100%\" viewBox=\"0 0 100 100\">\n<circle cx=\"50\" cy=\"50\" r=\"40\" fill=\"red\"/>\n</svg>"
            3       | Size.of(-1,-1)   | ["%","%"]  | 100       | Size.of(100, 100) | ["px","px"]   || "<svg width=\"100%\" height=\"100%\" viewBox=\"0 0 100 100\">\n<circle cx=\"50\" cy=\"50\" r=\"40\" fill=\"red\"/>\n</svg>"
            1       | Size.of(-1,-1)   | ["%","%"]  | -1        | Size.of(-1, -1)   | ["px","px"]   || "<svg width=\"100%\" height=\"100%\" viewBox=\"0 0 100 100\">\n<circle cx=\"50\" cy=\"50\" r=\"40\" fill=\"red\"/>\n</svg>"
            2       | Size.of(-1,-1)   | ["%","%"]  | 2         | Size.of(2, 2)     | ["px","px"]   || "<svg width=\"100%\" height=\"100%\" viewBox=\"0 0 100 100\">\n<circle cx=\"50\" cy=\"50\" r=\"40\" fill=\"red\"/>\n</svg>"
            3       | Size.of(-1,-1)   | ["%","%"]  | -1        | Size.of(-1, -1)   | ["px","px"]   || "<svg width=\"100%\" height=\"100%\" viewBox=\"0 0 100 100\">\n<circle cx=\"50\" cy=\"50\" r=\"40\" fill=\"red\"/>\n</svg>"
            1       | Size.of(100,100) | ["px","px"]| 100       | Size.of(100, 100) | ["px","px"]   || "<svg width=\"100px\" height=\"100px\" viewBox=\"0 0 100 100\">\n<circle cx=\"50\" cy=\"50\" r=\"40\" fill=\"red\"/>\n</svg>"
            2       | Size.of(-1,100)  | ["%","px"] | 50        | Size.of(50, 50)   | ["px","px"]   || "<svg width=\"100%\" height=\"100px\" viewBox=\"0 0 100 100\">\n<circle cx=\"50\" cy=\"50\" r=\"40\" fill=\"red\"/>\n</svg>"
            3       | Size.of(100,-1)  | ["px","%"] | 100       | Size.of(100, 100) | ["px","px"]   || "<svg width=\"100px\" height=\"100%\" viewBox=\"0 0 100 100\">\n<circle cx=\"50\" cy=\"50\" r=\"40\" fill=\"red\"/>\n</svg>"
            1       | Size.of(100,100) | ["px","px"]| -1        | Size.of(-1, -1)   | ["px","px"]   || "<svg width=\"100px\" height=\"100px\" viewBox=\"0 0 100 100\">\n<circle cx=\"50\" cy=\"50\" r=\"40\" fill=\"red\"/>\n</svg>"
            2       | Size.of(-1,100)  | ["%","px"] | 2         | Size.of(2, 2)     | ["px","px"]   || "<svg width=\"100%\" height=\"100px\" viewBox=\"0 0 100 100\">\n<circle cx=\"50\" cy=\"50\" r=\"40\" fill=\"red\"/>\n</svg>"
            3       | Size.of(100,-1)  | ["px","%"] | -1        | Size.of(-1, -1)   | ["px","px"]   || "<svg width=\"100px\" height=\"100%\" viewBox=\"0 0 100 100\">\n<circle cx=\"50\" cy=\"50\" r=\"40\" fill=\"red\"/>\n</svg>"
            2       | Size.of(-1,100)  | ["%","px"] | 2         | Size.of(2, 2)     | ["px","px"]   || "<svg width=\"100%\" height=\"100px\" viewBox=\"0 0 100 100\">\n<circle cx=\"50\" cy=\"50\" r=\"40\" fill=\"red\"/>\n</svg>"
            3       | Size.of(100,-1)  | ["px","%"] | -1        | Size.of(-1, -1)   | ["px","px"]   || "<svg width=\"100px\" height=\"100%\" viewBox=\"0 0 100 100\">\n<circle cx=\"50\" cy=\"50\" r=\"40\" fill=\"red\"/>\n</svg>"

            1       | Size.of(-1,-1)   | ["%","%"]  | 50        | Size.of(100, 50)  | ["px","px"]   || "<svg width=\"100%\" height=\"100%\" viewBox=\"0 0 100 50\">\n<circle cx=\"50\" cy=\"25\" r=\"20\" fill=\"red\"/>\n</svg>"
            2       | Size.of(-1,-1)   | ["%","%"]  | 25        | Size.of(50, 25)   | ["px","px"]   || "<svg width=\"100%\" height=\"100%\" viewBox=\"0 0 100 50\">\n<circle cx=\"50\" cy=\"25\" r=\"20\" fill=\"red\"/>\n</svg>"
            3       | Size.of(-1,-1)   | ["%","%"]  | 50        | Size.of(100, 50)  | ["px","px"]   || "<svg width=\"100%\" height=\"100%\" viewBox=\"0 0 100 50\">\n<circle cx=\"50\" cy=\"25\" r=\"20\" fill=\"red\"/>\n</svg>"
            1       | Size.of(-1,-1)   | ["%","%"]  | -1        | Size.of(-1, -1)   | ["px","px"]   || "<svg width=\"100%\" height=\"100%\" viewBox=\"0 0 100 50\">\n<circle cx=\"50\" cy=\"25\" r=\"20\" fill=\"red\"/>\n</svg>"
            2       | Size.of(-1,-1)   | ["%","%"]  | 1         | Size.of(2, 1)     | ["px","px"]   || "<svg width=\"100%\" height=\"100%\" viewBox=\"0 0 100 50\">\n<circle cx=\"50\" cy=\"25\" r=\"20\" fill=\"red\"/>\n</svg>"
            3       | Size.of(-1,-1)   | ["%","%"]  | -1        | Size.of(-1, -1)   | ["px","px"]   || "<svg width=\"100%\" height=\"100%\" viewBox=\"0 0 100 50\">\n<circle cx=\"50\" cy=\"25\" r=\"20\" fill=\"red\"/>\n</svg>"
            1       | Size.of(100,100) | ["px","px"]| 100       | Size.of(100, 100) | ["px","px"]   || "<svg width=\"100px\" height=\"100px\" viewBox=\"0 0 100 50\">\n<circle cx=\"50\" cy=\"25\" r=\"20\" fill=\"red\"/>\n</svg>"
            2       | Size.of(-1,100)  | ["%","px"] | 50        | Size.of(100, 50)  | ["px","px"]   || "<svg width=\"100%\" height=\"100px\" viewBox=\"0 0 100 50\">\n<circle cx=\"50\" cy=\"25\" r=\"20\" fill=\"red\"/>\n</svg>"
            3       | Size.of(100,-1)  | ["px","%"] | 25        | Size.of(50, 25)   | ["px","px"]   || "<svg width=\"100px\" height=\"100%\" viewBox=\"0 0 100 50\">\n<circle cx=\"50\" cy=\"25\" r=\"20\" fill=\"red\"/>\n</svg>"
            1       | Size.of(100,100) | ["px","px"]| -1        | Size.of(-1, -1)   | ["px","px"]   || "<svg width=\"100px\" height=\"100px\" viewBox=\"0 0 100 50\">\n<circle cx=\"50\" cy=\"25\" r=\"20\" fill=\"red\"/>\n</svg>"
            2       | Size.of(-1,100)  | ["%","px"] | 1         | Size.of(2, 1)     | ["px","px"]   || "<svg width=\"100%\" height=\"100px\" viewBox=\"0 0 100 50\">\n<circle cx=\"50\" cy=\"25\" r=\"20\" fill=\"red\"/>\n</svg>"
            3       | Size.of(100,-1)  | ["px","%"] | -1        | Size.of(-1, -1)   | ["px","px"]   || "<svg width=\"100px\" height=\"100%\" viewBox=\"0 0 100 50\">\n<circle cx=\"50\" cy=\"25\" r=\"20\" fill=\"red\"/>\n</svg>"
            2       | Size.of(-1,100)  | ["%","px"] | 1         | Size.of(2, 1)     | ["px","px"]   || "<svg width=\"100%\" height=\"100px\" viewBox=\"0 0 100 50\">\n<circle cx=\"50\" cy=\"25\" r=\"20\" fill=\"red\"/>\n</svg>"
            3       | Size.of(100,-1)  | ["px","%"] | -1        | Size.of(-1, -1)   | ["px","px"]   || "<svg width=\"100px\" height=\"100%\" viewBox=\"0 0 100 50\">\n<circle cx=\"50\" cy=\"25\" r=\"20\" fill=\"red\"/>\n</svg>"
    }

    def 'The `String` representation of `SvgIcon` correctly formats different configurations with proper units and properties.'(
        Size size, String widthUnit, String heightUnit,
        UI.FitComponent fitComponent, UI.Placement placement, String expectedPattern
    ) {
        reportInfo """
            The `toString()` method of `SvgIcon` should accurately represent the icon's properties,
            including dimensions with their proper units (px, %, or ? for unknown),
            fit component policy, and preferred placement.
            
            This test verifies that the string representation correctly formats all combinations
            of these properties.
        """
        given : 'We start with a consistent UI scale.'
            SwingTree.initializeUsing(it -> it.uiScaleFactor(1f) )
        and :
            int width = size.width().map(Math::round).orElse(-1) as int
            int height = size.height().map(Math::round).orElse(-1) as int
        and : 'We create an SVG string with the specified dimensions and units.'
            String svgString = """
                <svg ${widthUnit == 'px' ? "width=\"${width}px\"" : widthUnit == '%' ? "width=\"${width}%\"" : ''} 
                     ${heightUnit == 'px' ? "height=\"${height}px\"" : heightUnit == '%' ? "height=\"${height}%\"" : ''}
                     viewBox="0 0 100 100" xmlns="http://www.w3.org/2000/svg">
                    <circle cx="50" cy="50" r="40" fill="red"/>
                </svg>
            """.trim()
        and : 'We create an SvgIcon from the string and apply the specified properties.'
            var icon = SvgIcon.of(svgString)
                        .withFitComponent(fitComponent)
                        .withPreferredPlacement(placement)
        expect :
            icon.widthUnitString() == widthUnit
            icon.heightUnitString() == heightUnit

        when : 'We get the string representation.'
            String result = icon.toString()
        then : 'The string matches the expected pattern.'
            result.matches(expectedPattern)
        cleanup :
            SwingTree.clear()

        where :
            size             | widthUnit | heightUnit | fitComponent                     | placement                  || expectedPattern

            // Unknown dimensions (represented as ?)
            Size.of(-1, -1)  | ''        | ''         | UI.FitComponent.UNDEFINED        | UI.Placement.UNDEFINED    || /SvgIcon\[width=\?, height=\?, fitComponent=UNDEFINED, preferredPlacement=UNDEFINED, opacity=1\.0, doc=SVGDocument\[width=100\.0, height=100\.0\]\]/
            Size.of(-1, -1)  | ''        | ''         | UI.FitComponent.NO               | UI.Placement.TOP_LEFT     || /SvgIcon\[width=\?, height=\?, fitComponent=NO, preferredPlacement=TOP_LEFT, opacity=1\.0, doc=SVGDocument\[width=100\.0, height=100\.0\]\]/

            // Pixel dimensions
            Size.of(100, 50 )| 'px'      | 'px'       | UI.FitComponent.WIDTH            | UI.Placement.CENTER       || /SvgIcon\[width=100px, height=50px, fitComponent=WIDTH, preferredPlacement=CENTER, opacity=1\.0, doc=SVGDocument\[width=100\.0, height=50\.0\]\]/
            Size.of(75 , 75 )| 'px'      | 'px'       | UI.FitComponent.HEIGHT           | UI.Placement.TOP_RIGHT    || /SvgIcon\[width=75px, height=75px, fitComponent=HEIGHT, preferredPlacement=TOP_RIGHT, opacity=1\.0, doc=SVGDocument\[width=75\.0, height=75\.0\]\]/
            Size.of(200, 100)| 'px'      | 'px'       | UI.FitComponent.WIDTH_AND_HEIGHT | UI.Placement.BOTTOM_LEFT  || /SvgIcon\[width=200px, height=100px, fitComponent=WIDTH_AND_HEIGHT, preferredPlacement=BOTTOM_LEFT, opacity=1\.0, doc=SVGDocument\[width=200\.0, height=100\.0\]\]/

            // Percentage dimensions
            Size.of(100, 50 )| '%'       | '%'        | UI.FitComponent.MAX_DIM          | UI.Placement.RIGHT        || /SvgIcon\[width=100%, height=50%, fitComponent=MAX_DIM, preferredPlacement=RIGHT, opacity=1\.0, doc=SVGDocument\[width=100\.0, height=50\.0\]\]/
            Size.of(75 , 100)| '%'       | '%'        | UI.FitComponent.MIN_DIM          | UI.Placement.BOTTOM       || /SvgIcon\[width=75%, height=100%, fitComponent=MIN_DIM, preferredPlacement=BOTTOM, opacity=1\.0, doc=SVGDocument\[width=75\.0, height=100\.0\]\]/

            // Mixed units (one pixel, one percentage)
            Size.of(200,50)  | 'px'      | '%'        | UI.FitComponent.NO               | UI.Placement.TOP          || /SvgIcon\[width=200px, height=50%, fitComponent=NO, preferredPlacement=TOP, opacity=1\.0, doc=SVGDocument\[width=200\.0, height=50\.0\]\]/
            Size.of(100,75)  | '%'       | 'px'       | UI.FitComponent.WIDTH            | UI.Placement.LEFT         || /SvgIcon\[width=100%, height=75px, fitComponent=WIDTH, preferredPlacement=LEFT, opacity=1\.0, doc=SVGDocument\[width=100\.0, height=75\.0\]\]/

            // Mixed: one known, one unknown
            Size.of(32, -1)  | 'px'      | ''         | UI.FitComponent.HEIGHT           | UI.Placement.BOTTOM_RIGHT || /SvgIcon\[width=32px, height=\?, fitComponent=HEIGHT, preferredPlacement=BOTTOM_RIGHT, opacity=1\.0, doc=SVGDocument\[width=32\.0, height=100\.0\]\]/
            Size.of(-1, 24)  | ''        | 'px'       | UI.FitComponent.WIDTH_AND_HEIGHT | UI.Placement.UNDEFINED    || /SvgIcon\[width=\?, height=24px, fitComponent=WIDTH_AND_HEIGHT, preferredPlacement=UNDEFINED, opacity=1\.0, doc=SVGDocument\[width=100\.0, height=24\.0\]\]/
            Size.of(32, 3 )  | 'px'      | ''         | UI.FitComponent.HEIGHT           | UI.Placement.BOTTOM_RIGHT || /SvgIcon\[width=32px, height=\?, fitComponent=HEIGHT, preferredPlacement=BOTTOM_RIGHT, opacity=1\.0, doc=SVGDocument\[width=32\.0, height=100\.0\]\]/
            Size.of(4 , 24)  | ''        | 'px'       | UI.FitComponent.WIDTH_AND_HEIGHT | UI.Placement.UNDEFINED    || /SvgIcon\[width=\?, height=24px, fitComponent=WIDTH_AND_HEIGHT, preferredPlacement=UNDEFINED, opacity=1\.0, doc=SVGDocument\[width=100\.0, height=24\.0\]\]/

            // All placement options
            Size.of(64,64)   | 'px'      | 'px'       | UI.FitComponent.NO               | UI.Placement.TOP          || /SvgIcon\[width=64px, height=64px, fitComponent=NO, preferredPlacement=TOP, opacity=1\.0, doc=SVGDocument\[width=64\.0, height=64\.0\]\]/
            Size.of(64,64)   | 'px'      | 'px'       | UI.FitComponent.NO               | UI.Placement.BOTTOM       || /SvgIcon\[width=64px, height=64px, fitComponent=NO, preferredPlacement=BOTTOM, opacity=1\.0, doc=SVGDocument\[width=64\.0, height=64\.0\]\]/
            Size.of(64,64)   | 'px'      | 'px'       | UI.FitComponent.NO               | UI.Placement.LEFT         || /SvgIcon\[width=64px, height=64px, fitComponent=NO, preferredPlacement=LEFT, opacity=1\.0, doc=SVGDocument\[width=64\.0, height=64\.0\]\]/
            Size.of(64,64)   | 'px'      | 'px'       | UI.FitComponent.NO               | UI.Placement.RIGHT        || /SvgIcon\[width=64px, height=64px, fitComponent=NO, preferredPlacement=RIGHT, opacity=1\.0, doc=SVGDocument\[width=64\.0, height=64\.0\]\]/

            // All fit component options
            Size.of(128,96)  | 'px'      | 'px'       | UI.FitComponent.NO               | UI.Placement.CENTER       || /SvgIcon\[width=128px, height=96px, fitComponent=NO, preferredPlacement=CENTER, opacity=1\.0, doc=SVGDocument\[width=128\.0, height=96\.0\]\]/
            Size.of(128,96)  | 'px'      | 'px'       | UI.FitComponent.WIDTH            | UI.Placement.CENTER       || /SvgIcon\[width=128px, height=96px, fitComponent=WIDTH, preferredPlacement=CENTER, opacity=1\.0, doc=SVGDocument\[width=128\.0, height=96\.0\]\]/
            Size.of(128,96)  | 'px'      | 'px'       | UI.FitComponent.HEIGHT           | UI.Placement.CENTER       || /SvgIcon\[width=128px, height=96px, fitComponent=HEIGHT, preferredPlacement=CENTER, opacity=1\.0, doc=SVGDocument\[width=128\.0, height=96\.0\]\]/
            Size.of(128,96)  | 'px'      | 'px'       | UI.FitComponent.WIDTH_AND_HEIGHT | UI.Placement.CENTER       || /SvgIcon\[width=128px, height=96px, fitComponent=WIDTH_AND_HEIGHT, preferredPlacement=CENTER, opacity=1\.0, doc=SVGDocument\[width=128\.0, height=96\.0\]\]/
            Size.of(128,96)  | 'px'      | 'px'       | UI.FitComponent.MAX_DIM          | UI.Placement.CENTER       || /SvgIcon\[width=128px, height=96px, fitComponent=MAX_DIM, preferredPlacement=CENTER, opacity=1\.0, doc=SVGDocument\[width=128\.0, height=96\.0\]\]/
            Size.of(128,96)  | 'px'      | 'px'       | UI.FitComponent.MIN_DIM          | UI.Placement.CENTER       || /SvgIcon\[width=128px, height=96px, fitComponent=MIN_DIM, preferredPlacement=CENTER, opacity=1\.0, doc=SVGDocument\[width=128\.0, height=96\.0\]\]/
    }

    def 'The `SvgIcon` has value semantics.'()
    {
        reportInfo """
            In this unit test we both ensure that the `SvgIcon`
            expresses value semantics and we also introduce you
            to the various of its properties.
            
            You can derive new icons from an existing one
            using the `withXXX` methods, which return a new instance
            with the specified property changed.
        """
        given : 'We start off with two identical SVG icons created from the same SVG string.'
            String svg = """
                <svg width="100px" height="100px" viewBox="0 0 100 100" xmlns="http://www.w3.org/2000/svg">
                    <circle cx="50" cy="50" r="40" fill="red"/>
                </svg>
            """.trim()
            var icon1 = UI.findSvgIcon(IconDeclaration.ofSvg(svg)).get()
            var icon2 = UI.findSvgIcon(IconDeclaration.ofSvg(svg)).get()
        expect : 'The two icons are equal and have the same hash code.'
            icon1 == icon2
            icon1.hashCode() == icon2.hashCode()

        when : """
            We now derive both icons to have different opacities
            and then check that they are no longer equal.
            The opacity is a property that can be set on an `SvgIcon` to control its transparency when rendered.
             An opacity of 1.0 means fully opaque, while 0.0 means fully transparent.
        """
            var modifiedIcon1 = icon1.withOpacity(0.15f)
            var modifiedIcon2 = icon2.withOpacity(0.8f)
        then : 'The modified icons are not equal to each other or to the original.'
            modifiedIcon1 != modifiedIcon2
            modifiedIcon1 != icon1
            modifiedIcon2 != icon2
        and : 'The modified icons have the expected opacities.'
            modifiedIcon1.getOpacity() == 0.15f
            modifiedIcon2.getOpacity() == 0.8f

        when : """
            We now re-derive the initial icons by
            setting the `UI.FitComponent` property.
            
            This property controls how the icon should be scaled to 
            fit within a component when rendered.
        """
            modifiedIcon1 = icon1.withFitComponent(UI.FitComponent.WIDTH)
            modifiedIcon2 = icon2.withFitComponent(UI.FitComponent.HEIGHT)
        then : 'The modified icons are not equal to each other or to the original.'
            modifiedIcon1 != modifiedIcon2
            modifiedIcon1 != icon1
            modifiedIcon2 != icon2
        and : 'The modified icons have the expected fit component policies.'
            modifiedIcon1.getFitComponent() == UI.FitComponent.WIDTH
            modifiedIcon2.getFitComponent() == UI.FitComponent.HEIGHT

        when : """
            We now re-derive the initial icons by
            setting the `UI.Placement` property.

            This property controls the preferred placement of the icon within a component when rendered.
        """
            modifiedIcon1 = icon1.withPreferredPlacement(UI.Placement.BOTTOM)
            modifiedIcon2 = icon2.withPreferredPlacement(UI.Placement.RIGHT)
        then : 'The modified icons are not equal to each other or to the original.'
            modifiedIcon1 != modifiedIcon2
            modifiedIcon1 != icon1
            modifiedIcon2 != icon2
        and : 'The modified icons have the expected preferred placements.'
            modifiedIcon1.getPreferredPlacement() == UI.Placement.BOTTOM
            modifiedIcon2.getPreferredPlacement() == UI.Placement.RIGHT

        when : """
            We now derive icons that differ only in their size. An icon is the description of
            something to paint, and two descriptions that name different sizes describe different
            things, no matter that they were parsed from the same SVG document.
        """
            var narrowIcon = icon1.withIconWidth(10)
            var wideIcon   = icon1.withIconWidth(20)
        then : 'Icons of different size are neither equal nor do they share a hash code.'
            narrowIcon != wideIcon
            narrowIcon.hashCode() != wideIcon.hashCode()
        and : 'Icons of the same size are, even when they were derived separately.'
            narrowIcon == icon1.withIconWidth(10)
            narrowIcon.hashCode() == icon1.withIconWidth(10).hashCode()

        when : 'We derive two icons that differ only in their opacity...'
            var faintIcon = icon1.withOpacity(0.15f)
            var boldIcon  = icon1.withOpacity(0.8f)
        then : 'They too are told apart by both equality and hash code.'
            faintIcon != boldIcon
            faintIcon.hashCode() != boldIcon.hashCode()

        when : """
            We finally derive two icons from one whose width is pixel based while its height is
            percentage based. The two units are held separately by the icon, so a value object
            has to weigh each of them against its counterpart in the other icon.
        """
            var mixedUnitIcon = UI.findSvgIcon(IconDeclaration.ofSvg(
                    "<svg width=\"100px\" height=\"50%\" viewBox=\"0 0 100 100\">" +
                    "<circle cx=\"50\" cy=\"50\" r=\"40\" fill=\"red\"/></svg>"
                )).get()
            var derived1 = mixedUnitIcon.withFitComponent(UI.FitComponent.WIDTH)
            var derived2 = mixedUnitIcon.withFitComponent(UI.FitComponent.WIDTH)
        then : 'The two derivations are distinct objects describing the same icon, so they are equal.'
            !derived1.is(derived2)
            derived1 == derived2
            derived1.hashCode() == derived2.hashCode()
        and : """
            An icon whose height was turned into the same number of pixels is a different icon,
            even though both describe a size of 100 by 50, because a percentage and a pixel count
            answer differently when the icon is measured against a component.
        """
            derived1 != mixedUnitIcon.withIconHeight(50).withFitComponent(UI.FitComponent.WIDTH)
    }

    def 'The `getSvgSize()` method reports the width and height the SVG document declared for itself.'(
        String declaredWidth, String declaredHeight, String viewBox,
        Size declaredSize, String widthUnit, String heightUnit,
        Size baseSize, Size parsedDocumentSize
    ) {
        reportInfo """
            An SVG document may declare a `width` and a `height` of its own, and it may declare a
            `viewBox` which gives its coordinate system a size. The `getSvgSize()` method reports
            the `width` and `height` written in the source document, which is deliberately not the
            same as the size of the parsed document reachable through `getSvgDocument()`:
            whenever a document declares a usable `viewBox` together with pixel or percentage based
            dimensions, SwingTree rewrites those two attributes to `100%` while parsing, so that the
            document fills whatever area it is rendered into instead of being fitted inside it.
            The parsed document therefore reports the size of the view box, while `getSvgSize()`
            still reports the numbers the source declared.

            A `viewBox` which does not consist of exactly four numbers, or whose width or height is
            zero or negative, cannot serve as a reference frame, so the rewrite is skipped and the
            parsed document keeps the dimensions the source declared.

            Only pixel and percentage dimensions are resolved by SwingTree itself. A dimension in
            any other unit, like `pt` or `em`, is left to the SVG parser, which is why
            `widthUnitString()` and `heightUnitString()` report an empty `String` for it and why the
            icon then has no size of its own. The same holds for a percentage dimension, which only
            becomes a number once it is measured against something (see
            `withPercentageSizeResolvedAsPixels()`).
        """
        given : 'A UI scale factor of 1, so that the icon reports its size in developer pixels.'
            SwingTree.initializeUsing(it -> it.uiScaleFactor(1f) )
        and : 'An SVG document declaring the width, height and view box of the current data table row.'
            var svg = "<svg" +
                      ( declaredWidth.isEmpty()  ? "" : " width=\"${declaredWidth}\"" ) +
                      ( declaredHeight.isEmpty() ? "" : " height=\"${declaredHeight}\"" ) +
                      ( viewBox.isEmpty()        ? "" : " viewBox=\"${viewBox}\"" ) +
                      "><circle cx=\"5\" cy=\"5\" r=\"4\" fill=\"red\"/></svg>"

        when : 'We parse the document into an `SvgIcon`...'
            var icon = SvgIcon.of(svg)
        then : 'The icon reports the size the source document declared for itself.'
            icon.getSvgSize() == declaredSize
        and : 'The two unit strings name what SwingTree recognized in the dimension attributes.'
            icon.widthUnitString() == widthUnit
            icon.heightUnitString() == heightUnit
        and : 'The icon only adopts a declared dimension whose unit it resolves itself.'
            icon.getBaseWidth()  == baseSize.width().map(Math::round).orElse(-1)
            icon.getBaseHeight() == baseSize.height().map(Math::round).orElse(-1)
        and : 'The parsed document reports the view box size wherever SwingTree rewrote its dimensions.'
            icon.getSvgDocument().size().width  == parsedDocumentSize.widthOrElse(-1f)
            icon.getSvgDocument().size().height == parsedDocumentSize.heightOrElse(-1f)

        cleanup :
            SwingTree.clear()

        where :
            declaredWidth | declaredHeight | viewBox                 || declaredSize        | widthUnit | heightUnit | baseSize          | parsedDocumentSize

            // A pixel size next to a view box is rewritten to 100%, so the parsed document reports the view box:
            '75'          | '100'          | '0 0 100 100'           || Size.of(75, 100)    | 'px'      | 'px'       | Size.of(75, 100)  | Size.of(100, 100)
            '100%'        | '50%'          | '0 0 100 100'           || Size.of(100, 50)    | '%'       | '%'        | Size.of(-1, -1)   | Size.of(100, 100)
            ''            | ''             | '0 0 40 20'             || Size.of(40, 20)     | ''        | ''         | Size.of(-1, -1)   | Size.of(100, 100)
            '12.5'        | '7.5'          | '0 0 100 60'            || Size.of(12.5, 7.5)  | 'px'      | 'px'       | Size.of(13, 8)    | Size.of(100, 100)
            ' 40 px'      | ' 20 px'       | '0 0 80 40'             || Size.of(40, 20)     | 'px'      | 'px'       | Size.of(40, 20)   | Size.of(100, 100)
            '+30'         | '-20'          | '0 0 90 60'             || Size.of(30, -20)    | 'px'      | 'px'       | Size.of(30, -1)   | Size.of(100, 100)
            '50%'         | ''             | '0 0 60 40'             || Size.of(50, 40)     | '%'       | ''         | Size.of(-1, -1)   | Size.of(100, 100)
            ''            | '40'           | '0 0 60 40'             || Size.of(60, 40)     | ''        | 'px'       | Size.of(-1, 40)   | Size.of(100, 100)
            '0'           | '0'            | '0 0 60 40'             || Size.of(0, 0)       | 'px'      | 'px'       | Size.of(0, 0)     | Size.of(100, 100)

            // The four view box numbers may be separated by commas, by spaces, or by both:
            '50'          | '25'           | '0,0,100,50'            || Size.of(50, 25)     | 'px'      | 'px'       | Size.of(50, 25)   | Size.of(100, 100)
            '50'          | '25'           | '  0 ,  0,  100 , 50  ' || Size.of(50, 25)     | 'px'      | 'px'       | Size.of(50, 25)   | Size.of(100, 100)

            // A view box that cannot serve as a reference frame leaves the declared dimensions alone:
            '50'          | '25'           | '0 0 100'               || Size.of(50, 25)     | 'px'      | 'px'       | Size.of(50, 25)   | Size.of(50, 25)
            '50'          | '25'           | '0 0 100 50 7'          || Size.of(50, 25)     | 'px'      | 'px'       | Size.of(50, 25)   | Size.of(50, 25)
            '50'          | '25'           | '0 0 0 0'               || Size.of(50, 25)     | 'px'      | 'px'       | Size.of(50, 25)   | Size.of(50, 25)
            '50'          | '25'           | '0 0 -100 50'           || Size.of(50, 25)     | 'px'      | 'px'       | Size.of(50, 25)   | Size.of(50, 25)
            '50'          | '25'           | 'garbage'               || Size.of(50, 25)     | 'px'      | 'px'       | Size.of(50, 25)   | Size.of(50, 25)
            '60'          | '30'           | ''                      || Size.of(60, 30)     | 'px'      | 'px'       | Size.of(60, 30)   | Size.of(60, 30)

            // Units SwingTree does not resolve are left to the SVG parser, and the icon gets no size:
            '12pt'        | '12pt'         | '0 0 24 24'             || Size.of(16, 16)     | ''        | ''         | Size.of(-1, -1)   | Size.of(16, 16)
            '2em'         | '3em'          | '0 0 24 12'             || Size.of(20, 30)     | ''        | ''         | Size.of(-1, -1)   | Size.of(20, 30)
    }

    def 'The `SvgIcon` factory methods read the same icon from a resource path, a URL, an input stream or a parsed document.'()
    {
        reportInfo """
            The `SvgIcon` class has a factory method for every way an SVG document can reach your
            application: `SvgIcon.at(String)` and `SvgIcon.at(URL)` read it from a resource,
            `SvgIcon.of(String)` and `SvgIcon.of(InputStream)` read it from text you already hold,
            and `SvgIcon.of(SVGDocument)` adopts a document somebody else parsed.

            The four that parse the document themselves also read its `width` and `height`
            attributes, which is why the icons they create can report a size and a unit.
            `SvgIcon.of(SVGDocument)` never sees those attributes, so the icon it creates has no
            size and no unit of its own and is sized by whatever it is rendered into.
            Note also that a document handed out by `getSvgDocument()` has had its dimensions
            rewritten to `100%` during parsing, so feeding it back into `SvgIcon.of(SVGDocument)`
            reports the view box size as the size of the SVG.
        """
        given : 'A UI scale factor of 1, so that the icons report their size in developer pixels.'
            SwingTree.initializeUsing(it -> it.uiScaleFactor(1f) )
        and : 'An SVG document declaring a width of 75 and a height of 100 pixels.'
            var svg = "<svg width=\"75\" height=\"100\" viewBox=\"0 0 100 100\">" +
                      "<circle cx=\"50\" cy=\"50\" r=\"40\" fill=\"red\"/></svg>"

        when : 'We read that document once from a `String` and once from an `InputStream`...'
            var fromString = SvgIcon.of(svg)
            var fromStream = SvgIcon.of(new ByteArrayInputStream(svg.getBytes(StandardCharsets.UTF_8)))
        then : 'Both icons adopted the width and height the document declared.'
            fromString.getBaseWidth()  == 75 && fromString.getBaseHeight()  == 100
            fromStream.getBaseWidth()  == 75 && fromStream.getBaseHeight()  == 100
            fromString.widthUnitString() == "px" && fromString.heightUnitString() == "px"
            fromStream.widthUnitString() == "px" && fromStream.heightUnitString() == "px"
        and : 'Both report the size the document declared for itself.'
            fromString.getSvgSize() == Size.of(75, 100)
            fromStream.getSvgSize() == Size.of(75, 100)

        when : 'We read a resource declaring a width and height of 800 pixels, once by path and once by URL...'
            var fromPath = SvgIcon.at("/img/chat-bubble.svg")
            var fromUrl  = SvgIcon.at(SvgIcon.class.getResource("/img/chat-bubble.svg"))
        then : 'The two ways of naming the same resource produce the same icon.'
            fromPath.getBaseWidth()  == 800 && fromPath.getBaseHeight()  == 800
            fromUrl.getBaseWidth()   == 800 && fromUrl.getBaseHeight()   == 800
            fromPath.getSvgSize() == Size.of(800, 800)
            fromUrl.getSvgSize()  == Size.of(800, 800)

        when : 'We adopt the already parsed document of the first icon...'
            var fromDocument = SvgIcon.of(fromString.getSvgDocument())
        then : 'The new icon has no size and no unit of its own.'
            fromDocument.getBaseWidth()  == -1
            fromDocument.getBaseHeight() == -1
            fromDocument.widthUnitString() == ""
            fromDocument.heightUnitString() == ""
        and : 'It reports the view box size as the size of the SVG, because the parsed dimensions are `100%`.'
            fromDocument.getSvgSize() == Size.of(100, 100)

        when : 'We name a resource path which does not lead to an SVG document...'
            var missing = SvgIcon.at("/img/there-is-no-such-icon-here.svg")
        then : 'We get an empty icon instead of an exception.'
            missing.getSvgDocument() == null
            missing.getSvgSize() == Size.unknown()
            missing.getBaseWidth()  == -1
            missing.getBaseHeight() == -1

        cleanup :
            SwingTree.clear()
    }

    def 'A `Size` supplied to an `SvgIcon` factory method overrides the size the SVG document declares.'(
        float uiScale, Size suppliedSize, Size expectedBaseSize
    ) {
        reportInfo """
            Every `SvgIcon` factory method has an overload taking a `Size`, and a dimension supplied
            there wins over the same dimension declared inside the SVG document. The two sources are
            weighed per dimension, so a `Size` which only names a width lets the document supply the
            height. A `Size` with neither dimension, like `Size.unknown()`, leaves both to the
            document.

            The supplied size is measured in developer pixels, which is why `getBaseWidth()` reports
            it unchanged while `getIconWidth()` reports it scaled by the current DPI factor.
            The size the document declared for itself is not affected by any of this and stays
            readable through `getSvgSize()`.
        """
        given : 'We start with the UI scale factor of the current data table row.'
            SwingTree.initializeUsing(it -> it.uiScaleFactor(uiScale) )
        and : 'An SVG document declaring a width of 75 and a height of 100 pixels.'
            var svg = "<svg width=\"75\" height=\"100\" viewBox=\"0 0 100 100\">" +
                      "<circle cx=\"50\" cy=\"50\" r=\"40\" fill=\"red\"/></svg>"

        when : 'We parse it with the size of the current data table row...'
            var icon = SvgIcon.of(svg, suppliedSize)
        then : 'The icon takes each dimension from the supplied size and falls back to the document.'
            icon.getBaseWidth()  == expectedBaseSize.width().map(Math::round).orElse(-1)
            icon.getBaseHeight() == expectedBaseSize.height().map(Math::round).orElse(-1)
        and : 'The very same size, scaled to component pixel space, is what the icon renders at.'
            icon.getIconWidth()  == expectedBaseSize.width().map(UI::scale).map(Math::round).orElse(-1)
            icon.getIconHeight() == expectedBaseSize.height().map(UI::scale).map(Math::round).orElse(-1)
        and : 'What the document declared for itself is unchanged by the supplied size.'
            icon.getSvgSize() == Size.of(75, 100)

        when : 'We supply the same size to the input stream based factory method...'
            var fromStream = SvgIcon.of(new ByteArrayInputStream(svg.getBytes(StandardCharsets.UTF_8)), suppliedSize)
        then : 'It resolves the size exactly like the `String` based one.'
            fromStream.getBaseWidth()  == icon.getBaseWidth()
            fromStream.getBaseHeight() == icon.getBaseHeight()

        cleanup :
            SwingTree.clear()

        where :
            uiScale | suppliedSize      || expectedBaseSize
            1       | Size.unknown()    || Size.of(75, 100)
            1       | Size.of(40, 20)   || Size.of(40, 20)
            1       | Size.of(40, -1)   || Size.of(40, 100)
            1       | Size.of(-1, 20)   || Size.of(75, 20)
            2       | Size.unknown()    || Size.of(75, 100)
            2       | Size.of(40, 20)   || Size.of(40, 20)
            2       | Size.of(40, -1)   || Size.of(40, 100)
            2       | Size.of(-1, 20)   || Size.of(75, 20)
            3       | Size.of(13, 7)    || Size.of(13, 7)
    }

    def 'A `Size` supplied to `SvgIcon.at(..)` overrides the size declared by the SVG resource.'(
        Size suppliedSize, Size expectedBaseSize
    ) {
        reportInfo """
            The resource based factory methods `SvgIcon.at(String, Size)` and
            `SvgIcon.at(URL, Size)` weigh the supplied size against the declared one exactly like
            the text based factory methods do. A resource which cannot be found still honours the
            supplied size, because the size does not come from the document in the first place.
        """
        given : 'A UI scale factor of 1, so that the icons report their size in developer pixels.'
            SwingTree.initializeUsing(it -> it.uiScaleFactor(1f) )

        when : 'We read a resource declaring 800 by 800 pixels with the size of the current data table row...'
            var fromPath = SvgIcon.at("/img/chat-bubble.svg", suppliedSize)
            var fromUrl  = SvgIcon.at(SvgIcon.class.getResource("/img/chat-bubble.svg"), suppliedSize)
        then : 'Both icons resolve to the same expected size.'
            fromPath.getBaseWidth()  == expectedBaseSize.width().map(Math::round).orElse(-1)
            fromPath.getBaseHeight() == expectedBaseSize.height().map(Math::round).orElse(-1)
            fromUrl.getBaseWidth()   == expectedBaseSize.width().map(Math::round).orElse(-1)
            fromUrl.getBaseHeight()  == expectedBaseSize.height().map(Math::round).orElse(-1)

        when : 'We name a resource path which does not lead to an SVG document...'
            var missing = SvgIcon.at("/img/there-is-no-such-icon-here.svg", suppliedSize)
        then : 'The icon is empty, yet it still reports the size we supplied.'
            missing.getSvgDocument() == null
            missing.getBaseWidth()  == suppliedSize.width().map(Math::round).orElse(-1)
            missing.getBaseHeight() == suppliedSize.height().map(Math::round).orElse(-1)

        cleanup :
            SwingTree.clear()

        where :
            suppliedSize      || expectedBaseSize
            Size.unknown()    || Size.of(800, 800)
            Size.of(33, 44)   || Size.of(33, 44)
            Size.of(33, -1)   || Size.of(33, 800)
            Size.of(-1, 44)   || Size.of(800, 44)
    }

    def 'The `withIconWidth` method drops a percentage based height, because it cannot stand next to a pixel width.'(
        String declaredWidth, String declaredHeight, int newWidth,
        Size expectedBaseSize, Size expectedIconSize, Tuple2<String,String> expectedUnits
    ) {
        reportInfo """
            A number handed to `withIconWidth(int)` is measured in developer pixels, so the derived
            icon is pixel based in its width. A percentage height cannot stand next to it, because
            a percentage and a pixel count are measured against different things and the icon would
            no longer describe a single size. The wither therefore resets such a height to `-1`,
            which lets the aspect ratio of the SVG document supply it again whenever the icon is
            measured. A height that was already pixel based is carried over untouched.

            Passing `-1` clears the width instead of setting it. The one case where nothing changes
            is a width that already holds exactly the requested number of pixels: the wither then
            hands back the very same icon, percentage height and all.
        """
        given : 'A UI scale factor of 1, so that developer pixels and component pixels agree.'
            SwingTree.initializeUsing(it -> it.uiScaleFactor(1f) )
        and : 'An SVG document with a view box twice as wide as it is tall, declaring the dimensions of the current data table row.'
            var svg = "<svg" +
                      ( declaredWidth.isEmpty()  ? "" : " width=\"${declaredWidth}\"" ) +
                      ( declaredHeight.isEmpty() ? "" : " height=\"${declaredHeight}\"" ) +
                      " viewBox=\"0 0 100 50\"><circle cx=\"50\" cy=\"25\" r=\"20\" fill=\"red\"/></svg>"
            var icon = SvgIcon.of(svg)

        when : 'We derive an icon with the width of the current data table row...'
            var derived = icon.withIconWidth(newWidth)
        then : 'The derived icon has the expected size in developer pixels.'
            derived.getBaseWidth()  == expectedBaseSize.width().map(Math::round).orElse(-1)
            derived.getBaseHeight() == expectedBaseSize.height().map(Math::round).orElse(-1)
        and : 'A dimension left undefined is filled in from the aspect ratio when the icon is measured.'
            derived.getIconWidth()  == expectedIconSize.width().map(Math::round).orElse(-1)
            derived.getIconHeight() == expectedIconSize.height().map(Math::round).orElse(-1)
        and : 'The units of the derived icon are the expected ones.'
            derived.widthUnitString()  == expectedUnits.v1
            derived.heightUnitString() == expectedUnits.v2

        cleanup :
            SwingTree.clear()

        where :
            declaredWidth | declaredHeight | newWidth || expectedBaseSize | expectedIconSize  | expectedUnits

            // Two pixel dimensions: only the width moves.
            '100px'       | '50px'         | 30       || Size.of(30, 50)  | Size.of(30, 50)   | ["px","px"]
            '100px'       | '50px'         | 100      || Size.of(100, 50) | Size.of(100, 50)  | ["px","px"]
            '100px'       | '50px'         | -1       || Size.of(-1, 50)  | Size.of(100, 50)  | ["px","px"]

            // A percentage height is dropped, unless the wither finds nothing to change at all:
            '100px'       | '50%'          | 30       || Size.of(30, -1)  | Size.of(30, 15)   | ["px","px"]
            '100px'       | '50%'          | 100      || Size.of(100, -1) | Size.of(100, -1)  | ["px","%"]
            '100px'       | '50%'          | -1       || Size.of(-1, -1)  | Size.of(-1, -1)   | ["px","px"]

            // A percentage width simply becomes pixel based:
            '100%'        | '50px'         | 30       || Size.of(30, 50)  | Size.of(30, 50)   | ["px","px"]
            '100%'        | '50px'         | 100      || Size.of(100, 50) | Size.of(100, 50)  | ["px","px"]
            '100%'        | '50px'         | -1       || Size.of(-1, 50)  | Size.of(100, 50)  | ["px","px"]

            // Two percentage dimensions: the height is dropped and the width becomes pixel based:
            '100%'        | '50%'          | 30       || Size.of(30, -1)  | Size.of(30, 15)   | ["px","px"]
            '100%'        | '50%'          | 100      || Size.of(100, -1) | Size.of(100, 50)  | ["px","px"]
            '100%'        | '50%'          | -1       || Size.of(-1, -1)  | Size.of(-1, -1)   | ["px","px"]

            // No declared dimensions at all: there is nothing to carry over or to drop.
            ''            | ''             | 30       || Size.of(30, -1)  | Size.of(30, 15)   | ["px","px"]
            ''            | ''             | 100      || Size.of(100, -1) | Size.of(100, 50)  | ["px","px"]
            ''            | ''             | -1       || Size.of(-1, -1)  | Size.of(-1, -1)   | ["",""]
    }

    def 'The `withIconHeight` method drops a percentage based width, because it cannot stand next to a pixel height.'(
        String declaredWidth, String declaredHeight, int newHeight,
        Size expectedBaseSize, Size expectedIconSize, Tuple2<String,String> expectedUnits
    ) {
        reportInfo """
            `withIconHeight(int)` is the mirror image of `withIconWidth(int)`: the number it takes
            is measured in developer pixels, so a percentage width next to it is reset to `-1` and
            supplied again by the aspect ratio of the SVG document whenever the icon is measured.
            A width that was already pixel based is carried over untouched, `-1` clears the height,
            and a height that already holds exactly the requested number of pixels leaves the icon
            as it is, percentage width and all.
        """
        given : 'A UI scale factor of 1, so that developer pixels and component pixels agree.'
            SwingTree.initializeUsing(it -> it.uiScaleFactor(1f) )
        and : 'An SVG document with a view box twice as wide as it is tall, declaring the dimensions of the current data table row.'
            var svg = "<svg" +
                      ( declaredWidth.isEmpty()  ? "" : " width=\"${declaredWidth}\"" ) +
                      ( declaredHeight.isEmpty() ? "" : " height=\"${declaredHeight}\"" ) +
                      " viewBox=\"0 0 100 50\"><circle cx=\"50\" cy=\"25\" r=\"20\" fill=\"red\"/></svg>"
            var icon = SvgIcon.of(svg)

        when : 'We derive an icon with the height of the current data table row...'
            var derived = icon.withIconHeight(newHeight)
        then : 'The derived icon has the expected size in developer pixels.'
            derived.getBaseWidth()  == expectedBaseSize.width().map(Math::round).orElse(-1)
            derived.getBaseHeight() == expectedBaseSize.height().map(Math::round).orElse(-1)
        and : 'A dimension left undefined is filled in from the aspect ratio when the icon is measured.'
            derived.getIconWidth()  == expectedIconSize.width().map(Math::round).orElse(-1)
            derived.getIconHeight() == expectedIconSize.height().map(Math::round).orElse(-1)
        and : 'The units of the derived icon are the expected ones.'
            derived.widthUnitString()  == expectedUnits.v1
            derived.heightUnitString() == expectedUnits.v2

        cleanup :
            SwingTree.clear()

        where :
            declaredWidth | declaredHeight | newHeight || expectedBaseSize | expectedIconSize  | expectedUnits

            // Two pixel dimensions: only the height moves.
            '100px'       | '50px'         | 30        || Size.of(100, 30) | Size.of(100, 30)  | ["px","px"]
            '100px'       | '50px'         | 50        || Size.of(100, 50) | Size.of(100, 50)  | ["px","px"]
            '100px'       | '50px'         | -1        || Size.of(100, -1) | Size.of(100, 50)  | ["px","px"]

            // A percentage height simply becomes pixel based:
            '100px'       | '50%'          | 30        || Size.of(100, 30) | Size.of(100, 30)  | ["px","px"]
            '100px'       | '50%'          | 50        || Size.of(100, 50) | Size.of(100, 50)  | ["px","px"]
            '100px'       | '50%'          | -1        || Size.of(100, -1) | Size.of(100, 50)  | ["px","px"]

            // A percentage width is dropped, unless the wither finds nothing to change at all:
            '100%'        | '50px'         | 30        || Size.of(-1, 30)  | Size.of(60, 30)   | ["px","px"]
            '100%'        | '50px'         | 50        || Size.of(-1, 50)  | Size.of(-1, 50)   | ["%","px"]
            '100%'        | '50px'         | -1        || Size.of(-1, -1)  | Size.of(-1, -1)   | ["px","px"]

            // Two percentage dimensions: the width is dropped and the height becomes pixel based:
            '100%'        | '50%'          | 30        || Size.of(-1, 30)  | Size.of(60, 30)   | ["px","px"]
            '100%'        | '50%'          | 50        || Size.of(-1, 50)  | Size.of(100, 50)  | ["px","px"]
            '100%'        | '50%'          | -1        || Size.of(-1, -1)  | Size.of(-1, -1)   | ["px","px"]

            // No declared dimensions at all: there is nothing to carry over or to drop.
            ''            | ''             | 30        || Size.of(-1, 30)  | Size.of(60, 30)   | ["px","px"]
            ''            | ''             | 50        || Size.of(-1, 50)  | Size.of(100, 50)  | ["px","px"]
            ''            | ''             | -1        || Size.of(-1, -1)  | Size.of(-1, -1)   | ["",""]
    }

    def 'The `getImage()` method renders the icon into a new image of the size the icon reports.'(
        float uiScale, String declaredWidth, String declaredHeight, String viewBox,
        Size expectedIconSize, Size expectedImageSize
    ) {
        reportInfo """
            `SvgIcon` extends `ImageIcon`, so it has to be able to hand out an `Image`.
            It produces one by rendering the SVG document into a fresh buffer whose size is
            what `getIconWidth()` and `getIconHeight()` report, which means the image lives in
            component pixel space and grows with the current DPI scaling factor.

            An icon without a size of its own has no such numbers to use, so the image falls back
            to the size the document declared for itself, scaled the same way. For a document
            declaring percentages that is the percentage number itself, because a percentage of an
            area nobody named is not a pixel count.

            The document is stretched onto the whole image, so an icon whose width and height
            disagree with the aspect ratio of its view box produces a distorted image on purpose,
            exactly like it would when painted onto a component of that shape.
        """
        given : 'We start with the UI scale factor of the current data table row.'
            SwingTree.initializeUsing(it -> it.uiScaleFactor(uiScale) )
        and : 'An SVG document declaring the dimensions and view box of the current data table row.'
            var svg = "<svg" +
                      ( declaredWidth.isEmpty()  ? "" : " width=\"${declaredWidth}\"" ) +
                      ( declaredHeight.isEmpty() ? "" : " height=\"${declaredHeight}\"" ) +
                      ( viewBox.isEmpty()        ? "" : " viewBox=\"${viewBox}\"" ) +
                      "><circle cx=\"30\" cy=\"15\" r=\"10\" fill=\"red\"/></svg>"
            var icon = SvgIcon.of(svg)
        expect : 'The icon reports the size we expect in component pixel space.'
            icon.getIconWidth()  == expectedIconSize.width().map(Math::round).orElse(-1)
            icon.getIconHeight() == expectedIconSize.height().map(Math::round).orElse(-1)

        when : 'We ask the icon for an image...'
            var image = icon.getImage()
        then : 'The image has the expected size.'
            image.getWidth(null)  == expectedImageSize.width().map(Math::round).orElse(-1)
            image.getHeight(null) == expectedImageSize.height().map(Math::round).orElse(-1)

        when : 'We ask a second time...'
            var secondImage = icon.getImage()
        then : 'We get a freshly rendered image of the same size rather than the first one again.'
            !secondImage.is(image)
            secondImage.getWidth(null)  == image.getWidth(null)
            secondImage.getHeight(null) == image.getHeight(null)

        when : """
            We try to push an image into the icon. An `SvgIcon` renders a vector graphic,
            it does not wrap a raster image, so `setImage(Image)` has nothing to store.
        """
            icon.setImage(new BufferedImage(3, 3, BufferedImage.TYPE_INT_ARGB))
        then : 'The icon keeps rendering its SVG document at the same size.'
            icon.getImage().getWidth(null)  == expectedImageSize.width().map(Math::round).orElse(-1)
            icon.getImage().getHeight(null) == expectedImageSize.height().map(Math::round).orElse(-1)

        cleanup :
            SwingTree.clear()

        where :
            uiScale | declaredWidth | declaredHeight | viewBox       || expectedIconSize  | expectedImageSize

            // A declared pixel size is the size of the image, scaled to component pixel space:
            1       | '75'          | '100'          | '0 0 100 100' || Size.of(75, 100)  | Size.of(75, 100)
            2       | '75'          | '100'          | '0 0 100 100' || Size.of(150, 200) | Size.of(150, 200)
            1       | '60'          | '30'           | ''            || Size.of(60, 30)   | Size.of(60, 30)

            // Without a size of its own the icon falls back to what the document declared:
            1       | '100%'        | '100%'         | '0 0 60 30'   || Size.of(-1, -1)   | Size.of(100, 100)
            2       | '100%'        | '100%'         | '0 0 60 30'   || Size.of(-1, -1)   | Size.of(200, 200)
            1       | '50%'         | '25%'          | '0 0 60 30'   || Size.of(-1, -1)   | Size.of(50, 25)
            1       | ''            | ''             | '0 0 60 30'   || Size.of(-1, -1)   | Size.of(60, 30)
            2       | ''            | ''             | '0 0 60 30'   || Size.of(-1, -1)   | Size.of(120, 60)
            1       | '12pt'        | '12pt'         | '0 0 24 24'   || Size.of(-1, -1)   | Size.of(16, 16)
    }

    def 'The `withOpacity` method clamps an opacity outside the 0 to 1 range into it.'(
        float suppliedOpacity, float expectedOpacity
    ) {
        reportInfo """
            The opacity of an `SvgIcon` is a fraction between 0 and 1, where 0 paints nothing at
            all and 1 paints the SVG document as it is. A number outside that range has no meaning,
            so `withOpacity(float)` pulls it back to the nearest end of the range and logs a warning
            naming the number it was given, instead of carrying a value the renderer cannot use.
        """
        given : 'An SVG document with an opaque red circle in it.'
            var icon = SvgIcon.of(
                    "<svg width=\"20\" height=\"20\" viewBox=\"0 0 20 20\">" +
                    "<circle cx=\"10\" cy=\"10\" r=\"8\" fill=\"red\"/></svg>"
                )
        expect : 'An icon is fully opaque until told otherwise.'
            icon.getOpacity() == 1f

        when : 'We derive an icon with the opacity of the current data table row...'
            var derived = icon.withOpacity(suppliedOpacity)
        then : 'The derived icon carries the clamped opacity.'
            derived.getOpacity() == expectedOpacity

        where :
            suppliedOpacity || expectedOpacity
            1f              || 1f
            0.5f            || 0.5f
            0f              || 0f
            2f              || 1f
            1.0001f         || 1f
            -0.5f           || 0f
            -1000f          || 0f
            Float.MAX_VALUE || 1f
    }

    def 'The `getImage()` method of an empty `SvgIcon` hands out a transparent image instead of failing.'()
    {
        reportInfo """
            An `SvgIcon` whose source could not be parsed paints nothing, and it has no size of its
            own to fall back to either. Asking such an icon for an image still has to produce one,
            because `getImage()` is inherited from `ImageIcon` and the caller has no way to tell an
            empty icon apart from a loaded one before calling it. The answer is a single fully
            transparent pixel, which paints nothing wherever it is drawn.

            An empty icon that was given a size does report that size, so the transparent image it
            hands out has that size too.
        """
        given : 'A UI scale factor of 1, so that the icon reports its size in developer pixels.'
            SwingTree.initializeUsing(it -> it.uiScaleFactor(1f) )
        and : 'An icon created from a resource path which does not lead to an SVG document.'
            var empty = SvgIcon.at("/img/there-is-no-such-icon-here.svg")
        expect : 'The icon has neither a document nor a size of its own.'
            empty.getSvgDocument() == null
            empty.getIconWidth()  == -1
            empty.getIconHeight() == -1

        when : 'We ask the empty icon for an image...'
            var image = empty.getImage()
        then : 'We get a single pixel rather than an exception.'
            image.getWidth(null)  == 1
            image.getHeight(null) == 1
        and : 'That pixel is fully transparent, so painting the image changes nothing.'
            new Color(((BufferedImage) image).getRGB(0, 0), true).getAlpha() == 0

        when : 'We give the same empty icon a size and ask again...'
            var sizedImage = empty.withIconSize(4, 5).getImage()
        then : 'The image has the size we asked for and is transparent throughout.'
            sizedImage.getWidth(null)  == 4
            sizedImage.getHeight(null) == 5
            new Color(((BufferedImage) sizedImage).getRGB(2, 3), true).getAlpha() == 0

        cleanup :
            SwingTree.clear()
    }

    def 'A dimension derived from the aspect ratio is rounded up, so that the icon never crops its document.'(
        String viewBox, int newDimension, Size expectedFromWidth, Size expectedFromHeight
    ) {
        reportInfo """
            When only one of the two dimensions of an `SvgIcon` is known, the other one is derived
            from the aspect ratio of the SVG document whenever the icon is measured. That division
            rarely lands on a whole pixel, and the leftover fraction is rounded up rather than
            down: a document given one pixel too many still shows all of itself, while one given a
            pixel too few loses a row of its content.

            The same rounding decides the size that `withIconSizeFromWidth(int)` and
            `withIconSizeFromHeight(int)` write into the icon, which is why the derived size is
            readable through `getBaseHeight()` and `getBaseWidth()` there.
        """
        given : 'A UI scale factor of 1, so that developer pixels and component pixels agree.'
            SwingTree.initializeUsing(it -> it.uiScaleFactor(1f) )
        and : 'An SVG document without a size of its own, whose view box is the one of the current data table row.'
            var icon = SvgIcon.of(
                    "<svg width=\"100%\" height=\"100%\" viewBox=\"${viewBox}\">" +
                    "<circle cx=\"1\" cy=\"1\" r=\"1\" fill=\"red\"/></svg>"
                )

        when : 'We fix the width and let the height be derived...'
            var byWidth = icon.withIconWidth(newDimension)
        then : 'The derived height is the fraction rounded up.'
            byWidth.getIconWidth()  == expectedFromWidth.width().map(Math::round).orElse(-1)
            byWidth.getIconHeight() == expectedFromWidth.height().map(Math::round).orElse(-1)
        and : 'Deriving the whole size from that width writes the very same numbers into the icon.'
            icon.withIconSizeFromWidth(newDimension).getBaseWidth()  == expectedFromWidth.width().map(Math::round).orElse(-1)
            icon.withIconSizeFromWidth(newDimension).getBaseHeight() == expectedFromWidth.height().map(Math::round).orElse(-1)

        when : 'We fix the height and let the width be derived...'
            var byHeight = icon.withIconHeight(newDimension)
        then : 'The derived width is the fraction rounded up.'
            byHeight.getIconWidth()  == expectedFromHeight.width().map(Math::round).orElse(-1)
            byHeight.getIconHeight() == expectedFromHeight.height().map(Math::round).orElse(-1)
        and : 'Deriving the whole size from that height writes the very same numbers into the icon.'
            icon.withIconSizeFromHeight(newDimension).getBaseWidth()  == expectedFromHeight.width().map(Math::round).orElse(-1)
            icon.withIconSizeFromHeight(newDimension).getBaseHeight() == expectedFromHeight.height().map(Math::round).orElse(-1)

        cleanup :
            SwingTree.clear()

        where :
            viewBox      | newDimension || expectedFromWidth | expectedFromHeight

            // A view box twice as wide as it is tall halves and doubles without a remainder:
            '0 0 100 50' | 30           || Size.of(30, 15)   | Size.of(60, 30)
            '0 0 100 50' | 20           || Size.of(20, 10)   | Size.of(40, 20)
            // ...until the number to halve is odd, where the derived height gains the half pixel:
            '0 0 100 50' | 7            || Size.of(7, 4)     | Size.of(14, 7)

            // A view box of three by two has a ratio of 1.5, which divides evenly only sometimes:
            '0 0 3 2'    | 30           || Size.of(30, 20)   | Size.of(45, 30)
            '0 0 3 2'    | 10           || Size.of(10, 7)    | Size.of(15, 10)
            '0 0 3 2'    | 20           || Size.of(20, 14)   | Size.of(30, 20)
            '0 0 3 2'    | 7            || Size.of(7, 5)     | Size.of(11, 7)

            // A view box of seven by three divides evenly almost never:
            '0 0 7 3'    | 30           || Size.of(30, 13)   | Size.of(70, 30)
            '0 0 7 3'    | 20           || Size.of(20, 9)    | Size.of(47, 20)
            '0 0 7 3'    | 10           || Size.of(10, 5)    | Size.of(24, 10)
            '0 0 7 3'    | 7            || Size.of(7, 4)     | Size.of(17, 7)
    }

    def 'An `SvgIcon` painted onto a component covers the area its fit policy and placement name.'(
        UI.FitComponent fitMode, UI.Placement placement, Bounds expectedCoverage
    ) {
        reportInfo """
            `SvgIcon` implements the `Icon` interface, so any Swing component that shows an icon
            paints it by calling `paintIcon(Component, Graphics, int, int)`. What that call covers
            is decided by two policies the icon carries, and they answer two different questions:
            `UI.FitComponent` says how large the document is drawn, and `UI.Placement` says where
            inside the component that drawing sits.

            Leaving *both* policies undefined is the one case that is not SwingTree's own layout at
            all: the document is then drawn at the position the caller passed in, at the size the
            icon reports, which is what a plain `ImageIcon` would have done. As soon as either
            policy is named, SwingTree measures the icon against the component instead, and an
            undefined fit policy then means `MIN_DIM`, which is the largest size that still fits
            inside both dimensions of the component.

            In this specification an SVG document declaring 30 by 30 pixels, whose whole view box
            is one orange rectangle, is painted at the origin of a 90 by 60 pixel component, and
            the rectangle of orange pixels this produces is what the data table below names.
        """
        given : 'A UI scale factor of 1, so that developer pixels and component pixels agree.'
            SwingTree.initializeUsing(it -> it.uiScaleFactor(1f) )
        and : 'An SVG document declaring 30 by 30 pixels, filled edge to edge with orange.'
            var icon = SvgIcon.of(
                        "<svg width=\"30\" height=\"30\" viewBox=\"0 0 100 100\">" +
                        "<rect x=\"0\" y=\"0\" width=\"100\" height=\"100\" fill=\"orange\"/></svg>"
                    )
                    .withFitComponent(fitMode)
                    .withPreferredPlacement(placement)
        and : 'A panel of 90 by 60 pixels, and an equally large image to paint into.'
            var panel = UI.panel().get(JPanel)
            panel.setSize(90, 60)
            var image = new BufferedImage(90, 60, BufferedImage.TYPE_INT_ARGB)
        and : 'A way to measure the rectangle of pixels the orange of the document ended up covering.'
            var orangeCoverage = { BufferedImage img ->
                int left = Integer.MAX_VALUE, top = Integer.MAX_VALUE, right = -1, bottom = -1
                for ( int y = 0; y < img.getHeight(); y++ )
                    for ( int x = 0; x < img.getWidth(); x++ ) {
                        var pixel = new Color(img.getRGB(x, y), true)
                        if ( pixel.getAlpha() >= 128 && pixel.getRed() >= 128 && pixel.getBlue() < 128 ) {
                            left = Math.min(left, x) ; top    = Math.min(top, y)
                            right = Math.max(right, x); bottom = Math.max(bottom, y)
                        }
                    }
                return right < 0 ? Bounds.none() : Bounds.of(left, top, right - left + 1, bottom - top + 1)
            }

        when : 'We let the icon paint itself into the top left corner of the image...'
            var graphics = image.createGraphics()
            icon.paintIcon(panel, graphics, 0, 0)
            graphics.dispose()
        then : 'The orange covers exactly the area we expect.'
            orangeCoverage(image) == expectedCoverage

        cleanup :
            SwingTree.clear()

        where :
            fitMode                          | placement                 || expectedCoverage

            // Both policies undefined is the plain Swing case: the icon draws itself where it was told to.
            UI.FitComponent.UNDEFINED        | UI.Placement.UNDEFINED    || Bounds.of( 0,  0, 30, 30)
            // Naming a placement hands the layout to SwingTree, whose default fit policy is MIN_DIM:
            UI.FitComponent.UNDEFINED        | UI.Placement.CENTER       || Bounds.of(15,  0, 60, 60)
            UI.FitComponent.UNDEFINED        | UI.Placement.TOP_LEFT     || Bounds.of( 0,  0, 60, 60)
            UI.FitComponent.UNDEFINED        | UI.Placement.BOTTOM_RIGHT || Bounds.of(30,  0, 60, 60)

            // `NO` keeps the size the icon declares and moves it to the named corner or edge:
            UI.FitComponent.NO               | UI.Placement.UNDEFINED    || Bounds.of(30, 15, 30, 30)
            UI.FitComponent.NO               | UI.Placement.CENTER       || Bounds.of(30, 15, 30, 30)
            UI.FitComponent.NO               | UI.Placement.TOP_LEFT     || Bounds.of( 0,  0, 30, 30)
            UI.FitComponent.NO               | UI.Placement.TOP          || Bounds.of(30,  0, 30, 30)
            UI.FitComponent.NO               | UI.Placement.TOP_RIGHT    || Bounds.of(60,  0, 30, 30)
            UI.FitComponent.NO               | UI.Placement.LEFT         || Bounds.of( 0, 15, 30, 30)
            UI.FitComponent.NO               | UI.Placement.RIGHT        || Bounds.of(60, 15, 30, 30)
            UI.FitComponent.NO               | UI.Placement.BOTTOM_LEFT  || Bounds.of( 0, 30, 30, 30)
            UI.FitComponent.NO               | UI.Placement.BOTTOM       || Bounds.of(30, 30, 30, 30)
            UI.FitComponent.NO               | UI.Placement.BOTTOM_RIGHT || Bounds.of(60, 30, 30, 30)

            // `WIDTH` stretches the document across the component and leaves its height alone:
            UI.FitComponent.WIDTH            | UI.Placement.UNDEFINED    || Bounds.of( 0, 15, 90, 30)
            UI.FitComponent.WIDTH            | UI.Placement.TOP_LEFT     || Bounds.of( 0,  0, 90, 30)
            UI.FitComponent.WIDTH            | UI.Placement.BOTTOM       || Bounds.of( 0, 30, 90, 30)

            // `HEIGHT` is its mirror image:
            UI.FitComponent.HEIGHT           | UI.Placement.UNDEFINED    || Bounds.of(30,  0, 30, 60)
            UI.FitComponent.HEIGHT           | UI.Placement.TOP_LEFT     || Bounds.of( 0,  0, 30, 60)
            UI.FitComponent.HEIGHT           | UI.Placement.RIGHT        || Bounds.of(60,  0, 30, 60)

            // `WIDTH_AND_HEIGHT` fills the component, which leaves the placement nothing to decide:
            UI.FitComponent.WIDTH_AND_HEIGHT | UI.Placement.UNDEFINED    || Bounds.of( 0,  0, 90, 60)
            UI.FitComponent.WIDTH_AND_HEIGHT | UI.Placement.BOTTOM_RIGHT || Bounds.of( 0,  0, 90, 60)

            // `MIN_DIM` grows the document until the smaller component dimension is full:
            UI.FitComponent.MIN_DIM          | UI.Placement.UNDEFINED    || Bounds.of(15,  0, 60, 60)
            UI.FitComponent.MIN_DIM          | UI.Placement.TOP_LEFT     || Bounds.of( 0,  0, 60, 60)
            UI.FitComponent.MIN_DIM          | UI.Placement.TOP_RIGHT    || Bounds.of(30,  0, 60, 60)
            UI.FitComponent.MIN_DIM          | UI.Placement.BOTTOM_LEFT  || Bounds.of( 0,  0, 60, 60)

            // `MAX_DIM` grows it until the larger one is full, so it fills the component here:
            UI.FitComponent.MAX_DIM          | UI.Placement.UNDEFINED    || Bounds.of( 0,  0, 90, 60)
            UI.FitComponent.MAX_DIM          | UI.Placement.TOP_LEFT     || Bounds.of( 0,  0, 90, 60)
    }

    def 'An `SvgIcon` painted onto a bordered component keeps out of the border.'(
        UI.FitComponent fitMode, UI.Placement placement, Bounds expectedCoverage
    ) {
        reportInfo """
            A component that carries a border has less room for its icon than its bounds suggest,
            and `SvgIcon` accounts for that: it starts at the inner edge of the border and measures
            the icon against what is left. Without it, an icon fitted to the component would paint
            over the border it is supposed to sit inside.

            The one case that keeps painting where it was told to is the plain Swing case, where
            neither a fit policy nor a placement was named. There the caller decided the position,
            and the caller is the component that knows about its own border.

            The component below is the same 90 by 60 pixels as before, with a 5 pixel border on
            every side, leaving an 80 by 50 pixel area for an icon declaring 30 by 30 pixels.
        """
        given : 'A UI scale factor of 1, so that developer pixels and component pixels agree.'
            SwingTree.initializeUsing(it -> it.uiScaleFactor(1f) )
        and : 'An SVG document declaring 30 by 30 pixels, filled edge to edge with orange.'
            var icon = SvgIcon.of(
                        "<svg width=\"30\" height=\"30\" viewBox=\"0 0 100 100\">" +
                        "<rect x=\"0\" y=\"0\" width=\"100\" height=\"100\" fill=\"orange\"/></svg>"
                    )
                    .withFitComponent(fitMode)
                    .withPreferredPlacement(placement)
        and : 'A panel of 90 by 60 pixels with a 5 pixel border, and an equally large image to paint into.'
            var panel = UI.panel().get(JPanel)
            panel.setSize(90, 60)
            panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5))
            var image = new BufferedImage(90, 60, BufferedImage.TYPE_INT_ARGB)
        and : 'A way to measure the rectangle of pixels the orange of the document ended up covering.'
            var orangeCoverage = { BufferedImage img ->
                int left = Integer.MAX_VALUE, top = Integer.MAX_VALUE, right = -1, bottom = -1
                for ( int y = 0; y < img.getHeight(); y++ )
                    for ( int x = 0; x < img.getWidth(); x++ ) {
                        var pixel = new Color(img.getRGB(x, y), true)
                        if ( pixel.getAlpha() >= 128 && pixel.getRed() >= 128 && pixel.getBlue() < 128 ) {
                            left = Math.min(left, x) ; top    = Math.min(top, y)
                            right = Math.max(right, x); bottom = Math.max(bottom, y)
                        }
                    }
                return right < 0 ? Bounds.none() : Bounds.of(left, top, right - left + 1, bottom - top + 1)
            }

        when : 'We let the icon paint itself into the top left corner of the image...'
            var graphics = image.createGraphics()
            icon.paintIcon(panel, graphics, 0, 0)
            graphics.dispose()
        then : 'The orange covers exactly the area we expect.'
            orangeCoverage(image) == expectedCoverage

        cleanup :
            SwingTree.clear()

        where :
            fitMode                          | placement              || expectedCoverage

            // The plain Swing case paints where the caller asked, border or no border:
            UI.FitComponent.UNDEFINED        | UI.Placement.UNDEFINED || Bounds.of( 0, 0, 30, 30)
            // Everything else starts at the inner edge of the border and measures what is left:
            UI.FitComponent.UNDEFINED        | UI.Placement.CENTER    || Bounds.of(20, 5, 50, 50)
            UI.FitComponent.UNDEFINED        | UI.Placement.TOP_LEFT  || Bounds.of( 5, 5, 50, 50)
            UI.FitComponent.NO               | UI.Placement.UNDEFINED || Bounds.of(30, 15, 30, 30)
            UI.FitComponent.NO               | UI.Placement.TOP_LEFT  || Bounds.of( 5, 5, 30, 30)
            UI.FitComponent.WIDTH_AND_HEIGHT | UI.Placement.UNDEFINED || Bounds.of( 5, 5, 80, 50)
            UI.FitComponent.WIDTH_AND_HEIGHT | UI.Placement.TOP_LEFT  || Bounds.of( 5, 5, 80, 50)
            UI.FitComponent.MIN_DIM          | UI.Placement.UNDEFINED || Bounds.of(20, 5, 50, 50)
            UI.FitComponent.MIN_DIM          | UI.Placement.TOP_LEFT  || Bounds.of( 5, 5, 50, 50)
    }

    def 'An `SvgIcon` without a size of its own fills whatever component it is painted into.'(
        UI.FitComponent fitMode, UI.Placement placement
    ) {
        reportInfo """
            An SVG document declaring its dimensions in percent has no size of its own, which is
            what makes it scalable in the first place. Painted onto a component, such an icon has
            nothing to be placed *within*: it covers the whole component, and neither the fit
            policy nor the placement has anything left to decide.
        """
        given : 'A UI scale factor of 1, so that developer pixels and component pixels agree.'
            SwingTree.initializeUsing(it -> it.uiScaleFactor(1f) )
        and : 'An SVG document sized in percent, filled edge to edge with orange.'
            var icon = SvgIcon.of(
                        "<svg width=\"100%\" height=\"100%\" viewBox=\"0 0 100 100\">" +
                        "<rect x=\"0\" y=\"0\" width=\"100\" height=\"100\" fill=\"orange\"/></svg>"
                    )
                    .withFitComponent(fitMode)
                    .withPreferredPlacement(placement)
        expect : 'The icon reports no size of its own.'
            icon.getIconWidth()  == -1
            icon.getIconHeight() == -1

        when : 'We let it paint itself into the top left corner of a 90 by 60 pixel image...'
            var panel = UI.panel().get(JPanel)
            panel.setSize(90, 60)
            var image = new BufferedImage(90, 60, BufferedImage.TYPE_INT_ARGB)
            var graphics = image.createGraphics()
            icon.paintIcon(panel, graphics, 0, 0)
            graphics.dispose()
        then : 'Every corner of the image is orange, because the document covers all of it.'
            new Color(image.getRGB( 0,  0), true).getRed()   >= 128
            new Color(image.getRGB(89,  0), true).getRed()   >= 128
            new Color(image.getRGB( 0, 59), true).getRed()   >= 128
            new Color(image.getRGB(89, 59), true).getRed()   >= 128
            new Color(image.getRGB(89, 59), true).getAlpha() >= 128

        cleanup :
            SwingTree.clear()

        where :
            fitMode                          | placement
            UI.FitComponent.UNDEFINED        | UI.Placement.UNDEFINED
            UI.FitComponent.NO               | UI.Placement.CENTER
            UI.FitComponent.WIDTH            | UI.Placement.TOP_LEFT
            UI.FitComponent.HEIGHT           | UI.Placement.BOTTOM_RIGHT
            UI.FitComponent.WIDTH_AND_HEIGHT | UI.Placement.UNDEFINED
            UI.FitComponent.MIN_DIM          | UI.Placement.CENTER
            UI.FitComponent.MAX_DIM          | UI.Placement.TOP_LEFT
    }

    def 'A component which also shows text pulls an undefined icon placement to its leading side.'(
        String labelText, ComponentOrientation orientation, Bounds expectedCoverage
    ) {
        reportInfo """
            When an `SvgIcon` has no placement of its own, it asks the component it is painted onto
            where it would like its icon. A component that shows text answers with the side its
            text reads away from, so that icon and text do not fight over the same pixels, and a
            component with an explicit orientation answers with the leading side of that
            orientation. A component with neither has no opinion, and the icon ends up centered.
        """
        given : 'A UI scale factor of 1, so that developer pixels and component pixels agree.'
            SwingTree.initializeUsing(it -> it.uiScaleFactor(1f) )
        and : 'An SVG document declaring 30 by 30 pixels, filled edge to edge with orange, which keeps that size.'
            var icon = SvgIcon.of(
                        "<svg width=\"30\" height=\"30\" viewBox=\"0 0 100 100\">" +
                        "<rect x=\"0\" y=\"0\" width=\"100\" height=\"100\" fill=\"orange\"/></svg>"
                    )
                    .withFitComponent(UI.FitComponent.NO)
        and : 'A label of 90 by 60 pixels carrying the text and orientation of the current data table row.'
            var label = UI.label(labelText).get(JLabel)
            label.setSize(90, 60)
            label.setComponentOrientation(orientation)
        and : 'A way to measure the rectangle of pixels the orange of the document ended up covering.'
            var orangeCoverage = { BufferedImage img ->
                int left = Integer.MAX_VALUE, top = Integer.MAX_VALUE, right = -1, bottom = -1
                for ( int y = 0; y < img.getHeight(); y++ )
                    for ( int x = 0; x < img.getWidth(); x++ ) {
                        var pixel = new Color(img.getRGB(x, y), true)
                        if ( pixel.getAlpha() >= 128 && pixel.getRed() >= 128 && pixel.getBlue() < 128 ) {
                            left = Math.min(left, x) ; top    = Math.min(top, y)
                            right = Math.max(right, x); bottom = Math.max(bottom, y)
                        }
                    }
                return right < 0 ? Bounds.none() : Bounds.of(left, top, right - left + 1, bottom - top + 1)
            }

        when : 'We let the icon paint itself onto that label...'
            var image = new BufferedImage(90, 60, BufferedImage.TYPE_INT_ARGB)
            var graphics = image.createGraphics()
            icon.paintIcon(label, graphics, 0, 0)
            graphics.dispose()
        then : 'The orange sits where the label wanted its icon.'
            orangeCoverage(image) == expectedCoverage

        cleanup :
            SwingTree.clear()

        where :
            labelText   | orientation                          || expectedCoverage
            ''          | ComponentOrientation.UNKNOWN         || Bounds.of(30, 15, 30, 30)
            'some text' | ComponentOrientation.UNKNOWN         || Bounds.of( 0, 15, 30, 30)
            'some text' | ComponentOrientation.LEFT_TO_RIGHT   || Bounds.of( 0, 15, 30, 30)
            'some text' | ComponentOrientation.RIGHT_TO_LEFT   || Bounds.of(60, 15, 30, 30)
            ''          | ComponentOrientation.LEFT_TO_RIGHT   || Bounds.of( 0, 15, 30, 30)
            ''          | ComponentOrientation.RIGHT_TO_LEFT   || Bounds.of(60, 15, 30, 30)
    }

    def 'The padding of the image sub style is measured in pixels of the component, not of the SVG document.'(
        UI.FitComponent fitMode, int padding, Bounds expectedCoverage
    ) {
        reportInfo """
            An SVG handed to the style API through `image(conf -> conf.svg(..))` can be given a
            padding, and that padding is a number of component pixels. It is taken off the drawing
            *after* the fit mode has decided how large the document is drawn, so the same number
            always frees the same number of pixels, whether the fit mode blew the document up to
            fill the component or left it at the size it declares.

            A padding measured in the document's own coordinates instead would be stretched along
            with the document: the same `padding(10)` would free 60 pixels under a fit mode that
            triples the document and 3 pixels under one that shrinks it to a third, so the number
            would mean something different in every component it is used in.
        """
        given : 'A UI scale factor of 1, so that developer pixels and component pixels agree.'
            SwingTree.initializeUsing(it -> it.uiScaleFactor(1f) )
        and : 'A way to measure the rectangle of pixels the orange of the document ended up covering.'
            var orangeCoverage = { BufferedImage img ->
                int left = Integer.MAX_VALUE, top = Integer.MAX_VALUE, right = -1, bottom = -1
                for ( int y = 0; y < img.getHeight(); y++ )
                    for ( int x = 0; x < img.getWidth(); x++ ) {
                        var pixel = new Color(img.getRGB(x, y), true)
                        if ( pixel.getAlpha() >= 128 && pixel.getRed() >= 128 && pixel.getBlue() < 128 ) {
                            left = Math.min(left, x) ; top    = Math.min(top, y)
                            right = Math.max(right, x); bottom = Math.max(bottom, y)
                        }
                    }
                return right < 0 ? Bounds.none() : Bounds.of(left, top, right - left + 1, bottom - top + 1)
            }
        and : 'An SVG document declaring 30 by 30 pixels, filled edge to edge with orange.'
            var svg = "<svg width=\"30\" height=\"30\" viewBox=\"0 0 100 100\">" +
                      "<rect x=\"0\" y=\"0\" width=\"100\" height=\"100\" fill=\"orange\"/></svg>"

        when : 'We render a 90 by 60 pixel box showing that document with the padding of the current data table row...'
            var padded = Utility.renderSingleComponent(
                    UI.box().withStyle( it -> it
                        .size(90, 60)
                        .image( conf -> conf.svg(svg).placement(UI.Placement.TOP_LEFT).fitMode(fitMode).padding(padding) )
                    ).get(JBox)
                )
        then : 'The document covers exactly the rectangle we expect.'
            orangeCoverage(padded) == expectedCoverage

        when : 'We render the very same box once more, without any padding...'
            var unpadded = orangeCoverage(Utility.renderSingleComponent(
                    UI.box().withStyle( it -> it
                        .size(90, 60)
                        .image( conf -> conf.svg(svg).placement(UI.Placement.TOP_LEFT).fitMode(fitMode) )
                    ).get(JBox)
                ))
        then : """
            The padded drawing is the unpadded one moved in by the padding and shortened by twice
            it, which is the whole promise: the padding costs the drawing the same pixels here as
            it does under every other fit mode.
        """
            orangeCoverage(padded) == Bounds.of(
                                            padding,
                                            padding,
                                            unpadded.size().widthOrElse(0f)  - 2 * padding,
                                            unpadded.size().heightOrElse(0f) - 2 * padding
                                        )

        cleanup :
            SwingTree.clear()

        where :
            fitMode                          | padding || expectedCoverage

            // The document keeps the size it declares, so the padding takes 20 off 30:
            UI.FitComponent.NO               | 0       || Bounds.of( 0,  0, 30, 30)
            UI.FitComponent.NO               | 5       || Bounds.of( 5,  5, 20, 20)
            UI.FitComponent.NO               | 10      || Bounds.of(10, 10, 10, 10)

            // The document is stretched across the component, and the padding still takes 20:
            UI.FitComponent.WIDTH            | 0       || Bounds.of( 0,  0, 90, 30)
            UI.FitComponent.WIDTH            | 5       || Bounds.of( 5,  5, 80, 20)
            UI.FitComponent.WIDTH            | 10      || Bounds.of(10, 10, 70, 10)

            UI.FitComponent.HEIGHT           | 0       || Bounds.of( 0,  0, 30, 60)
            UI.FitComponent.HEIGHT           | 5       || Bounds.of( 5,  5, 20, 50)
            UI.FitComponent.HEIGHT           | 10      || Bounds.of(10, 10, 10, 40)

            UI.FitComponent.WIDTH_AND_HEIGHT | 0       || Bounds.of( 0,  0, 90, 60)
            UI.FitComponent.WIDTH_AND_HEIGHT | 5       || Bounds.of( 5,  5, 80, 50)
            UI.FitComponent.WIDTH_AND_HEIGHT | 10      || Bounds.of(10, 10, 70, 40)

            UI.FitComponent.MIN_DIM          | 0       || Bounds.of( 0,  0, 60, 60)
            UI.FitComponent.MIN_DIM          | 5       || Bounds.of( 5,  5, 50, 50)
            UI.FitComponent.MIN_DIM          | 10      || Bounds.of(10, 10, 40, 40)
    }

    def 'A padding that differs per side decides where inside the component the SVG document sits.'(
        UI.Placement placement, Bounds expectedCoverage
    ) {
        reportInfo """
            A padding names four numbers, and they need not be the same. Together they cut a
            smaller rectangle out of the component, and it is that rectangle, not the component,
            that the placement then works within: `TOP_LEFT` puts the document in its top left
            corner, `BOTTOM_RIGHT` in its bottom right one, and `CENTER` in its middle.

            A placement that reached for the component instead would ignore the difference between
            the four numbers entirely, because it would recompute the position from edges the
            padding never moved.

            The component below is 90 by 60 pixels and the padding is 1 at the top, 2 on the right,
            3 at the bottom and 8 on the left. That leaves a rectangle of 80 by 56 pixels at (8, 1),
            and the document, which declares 30 by 30 pixels and keeps that size, is drawn 20 by 26
            pixels large inside it.
        """
        given : 'A UI scale factor of 1, so that developer pixels and component pixels agree.'
            SwingTree.initializeUsing(it -> it.uiScaleFactor(1f) )
        and : 'A way to measure the rectangle of pixels the orange of the document ended up covering.'
            var orangeCoverage = { BufferedImage img ->
                int left = Integer.MAX_VALUE, top = Integer.MAX_VALUE, right = -1, bottom = -1
                for ( int y = 0; y < img.getHeight(); y++ )
                    for ( int x = 0; x < img.getWidth(); x++ ) {
                        var pixel = new Color(img.getRGB(x, y), true)
                        if ( pixel.getAlpha() >= 128 && pixel.getRed() >= 128 && pixel.getBlue() < 128 ) {
                            left = Math.min(left, x) ; top    = Math.min(top, y)
                            right = Math.max(right, x); bottom = Math.max(bottom, y)
                        }
                    }
                return right < 0 ? Bounds.none() : Bounds.of(left, top, right - left + 1, bottom - top + 1)
            }
        and : 'A 90 by 60 pixel box showing a 30 by 30 pixel document with a different padding on every side.'
            var box =
                    UI.box().withStyle( it -> it
                        .size(90, 60)
                        .image( conf -> conf
                            .svg("<svg width=\"30\" height=\"30\" viewBox=\"0 0 100 100\">" +
                                 "<rect x=\"0\" y=\"0\" width=\"100\" height=\"100\" fill=\"orange\"/></svg>")
                            .placement(placement)
                            .fitMode(UI.FitComponent.NO)
                            .padding(1, 2, 3, 8)
                        )
                    )

        when : 'We render the box into an image...'
            var image = Utility.renderSingleComponent(box.get(JBox))
        then : 'The document sits where the placement puts it inside the padded rectangle.'
            orangeCoverage(image) == expectedCoverage

        cleanup :
            SwingTree.clear()

        where :
            placement                 || expectedCoverage
            UI.Placement.TOP_LEFT     || Bounds.of( 8,  1, 20, 26)
            UI.Placement.TOP          || Bounds.of(38,  1, 20, 26)
            UI.Placement.TOP_RIGHT    || Bounds.of(68,  1, 20, 26)
            UI.Placement.LEFT         || Bounds.of( 8, 16, 20, 26)
            UI.Placement.CENTER       || Bounds.of(38, 16, 20, 26)
            UI.Placement.RIGHT        || Bounds.of(68, 16, 20, 26)
            UI.Placement.BOTTOM_LEFT  || Bounds.of( 8, 31, 20, 26)
            UI.Placement.BOTTOM       || Bounds.of(38, 31, 20, 26)
            UI.Placement.BOTTOM_RIGHT || Bounds.of(68, 31, 20, 26)
            UI.Placement.UNDEFINED    || Bounds.of(38, 16, 20, 26)
    }

    def 'The padding of the image sub style grows with the DPI scaling factor, like every other style dimension.'(
        float uiScale, Bounds expectedCoverage
    ) {
        reportInfo """
            The numbers you hand to the style API are developer pixels, and SwingTree turns them
            into component pixels using the DPI scaling factor of the machine the UI runs on. The
            padding of the image sub style is no exception: on a screen that scales everything by
            two, a `padding(10)` frees twenty component pixels, so it keeps looking like the same
            gap next to a component and a document that grew by the same factor.
        """
        given : 'We start with the UI scale factor of the current data table row.'
            SwingTree.initializeUsing(it -> it.uiScaleFactor(uiScale) )
        and : 'A way to measure the rectangle of pixels the orange of the document ended up covering.'
            var orangeCoverage = { BufferedImage img ->
                int left = Integer.MAX_VALUE, top = Integer.MAX_VALUE, right = -1, bottom = -1
                for ( int y = 0; y < img.getHeight(); y++ )
                    for ( int x = 0; x < img.getWidth(); x++ ) {
                        var pixel = new Color(img.getRGB(x, y), true)
                        if ( pixel.getAlpha() >= 128 && pixel.getRed() >= 128 && pixel.getBlue() < 128 ) {
                            left = Math.min(left, x) ; top    = Math.min(top, y)
                            right = Math.max(right, x); bottom = Math.max(bottom, y)
                        }
                    }
                return right < 0 ? Bounds.none() : Bounds.of(left, top, right - left + 1, bottom - top + 1)
            }
        and : 'A 90 by 60 pixel box stretching a 30 by 30 pixel document across itself, with a padding of 10.'
            var box =
                    UI.box().withStyle( it -> it
                        .size(90, 60)
                        .image( conf -> conf
                            .svg("<svg width=\"30\" height=\"30\" viewBox=\"0 0 100 100\">" +
                                 "<rect x=\"0\" y=\"0\" width=\"100\" height=\"100\" fill=\"orange\"/></svg>")
                            .placement(UI.Placement.TOP_LEFT)
                            .fitMode(UI.FitComponent.WIDTH_AND_HEIGHT)
                            .padding(10)
                        )
                    )

        when : 'We render the box into an image...'
            var image = Utility.renderSingleComponent(box.get(JBox))
        then : 'Both the component and the padding have grown by the scaling factor.'
            orangeCoverage(image) == expectedCoverage
        and : 'Which is the scaled component minus twice the scaled padding.'
            orangeCoverage(image) == Bounds.of(
                                            UI.scale(10), UI.scale(10),
                                            UI.scale(90) - 2 * UI.scale(10),
                                            UI.scale(60) - 2 * UI.scale(10)
                                        )

        cleanup :
            SwingTree.clear()

        where :
            uiScale || expectedCoverage
            1       || Bounds.of(10, 10,  70,  40)
            2       || Bounds.of(20, 20, 140,  80)
            3       || Bounds.of(30, 30, 210, 120)
    }

    def 'An `SvgIcon` without a size of its own is measured against the shape of the area it is painted into.'(
        String viewBox, UI.FitComponent fitMode, int areaWidth, int areaHeight, Bounds expectedCoverage
    ) {
        reportInfo """
            An SVG document that declares no `width` and no `height` gives the icon nothing to be
            sized by, so the size has to come from the area it is painted into. Which of the two
            area dimensions decides that size depends on the fit policy and on whether the area is
            taller than it is wide: `MIN_DIM` grows the document until the smaller area dimension
            is full, `MAX_DIM` until the larger one is, and `NO` and `UNDEFINED` behave like
            `MIN_DIM` for such an icon, because there is no declared size for them to keep.

            The aspect ratio of the view box is preserved throughout, and it wins over the area:
            a document wider than it is tall, grown until the smaller area dimension is full, is
            then wider than the area and paints past its edge.
        """
        given : 'A UI scale factor of 1, so that developer pixels and component pixels agree.'
            SwingTree.initializeUsing(it -> it.uiScaleFactor(1f) )
        and : 'An SVG document without declared dimensions, whose view box is filled edge to edge with orange.'
            var viewBoxParts = viewBox.split(" ")
            var icon = SvgIcon.of(
                        "<svg viewBox=\"${viewBox}\"><rect x=\"0\" y=\"0\" " +
                        "width=\"${viewBoxParts[2]}\" height=\"${viewBoxParts[3]}\" fill=\"orange\"/></svg>"
                    )
                    .withFitComponent(fitMode)
                    .withPreferredPlacement(UI.Placement.TOP_LEFT)
        and : 'A way to measure the rectangle of pixels the orange of the document ended up covering.'
            var orangeCoverage = { BufferedImage img ->
                int left = Integer.MAX_VALUE, top = Integer.MAX_VALUE, right = -1, bottom = -1
                for ( int y = 0; y < img.getHeight(); y++ )
                    for ( int x = 0; x < img.getWidth(); x++ ) {
                        var pixel = new Color(img.getRGB(x, y), true)
                        if ( pixel.getAlpha() >= 128 && pixel.getRed() >= 128 && pixel.getBlue() < 128 ) {
                            left = Math.min(left, x) ; top    = Math.min(top, y)
                            right = Math.max(right, x); bottom = Math.max(bottom, y)
                        }
                    }
                return right < 0 ? Bounds.none() : Bounds.of(left, top, right - left + 1, bottom - top + 1)
            }

        when : 'We paint the icon into the top left corner of an area of the given shape...'
            var panel = UI.panel().get(JPanel)
            panel.setSize(areaWidth, areaHeight)
            var image = new BufferedImage(200, 200, BufferedImage.TYPE_INT_ARGB)
            var graphics = image.createGraphics()
            icon.paintIcon(panel, graphics, 0, 0)
            graphics.dispose()
        then : 'The orange covers exactly the area we expect.'
            orangeCoverage(image) == expectedCoverage

        cleanup :
            SwingTree.clear()

        where :
            viewBox      | fitMode                   | areaWidth | areaHeight || expectedCoverage

            // A square document: the smaller area dimension decides, except under MAX_DIM.
            '0 0 100 100'| UI.FitComponent.UNDEFINED | 90        | 60         || Bounds.of(0, 0, 60, 60)
            '0 0 100 100'| UI.FitComponent.UNDEFINED | 60        | 90         || Bounds.of(0, 0, 60, 60)
            '0 0 100 100'| UI.FitComponent.UNDEFINED | 70        | 70         || Bounds.of(0, 0, 70, 70)
            '0 0 100 100'| UI.FitComponent.MIN_DIM   | 90        | 60         || Bounds.of(0, 0, 60, 60)
            '0 0 100 100'| UI.FitComponent.MIN_DIM   | 60        | 90         || Bounds.of(0, 0, 60, 60)
            '0 0 100 100'| UI.FitComponent.MAX_DIM   | 90        | 60         || Bounds.of(0, 0, 90, 90)
            '0 0 100 100'| UI.FitComponent.MAX_DIM   | 60        | 90         || Bounds.of(0, 0, 90, 90)
            // `NO` has no declared size to keep, so it reaches for the area as well:
            '0 0 100 100'| UI.FitComponent.NO        | 90        | 60         || Bounds.of(0, 0, 60, 60)
            '0 0 100 100'| UI.FitComponent.NO        | 60        | 90         || Bounds.of(0, 0, 90, 90)

            // A view box twice as wide as it is tall keeps that ratio in every area shape:
            '0 0 200 100'| UI.FitComponent.UNDEFINED | 90        | 60         || Bounds.of(0, 0, 120, 60)
            '0 0 200 100'| UI.FitComponent.UNDEFINED | 60        | 90         || Bounds.of(0, 0,  60, 30)
            '0 0 200 100'| UI.FitComponent.UNDEFINED | 70        | 70         || Bounds.of(0, 0, 140, 70)
            '0 0 200 100'| UI.FitComponent.NO        | 90        | 60         || Bounds.of(0, 0, 120, 60)
            '0 0 200 100'| UI.FitComponent.MIN_DIM   | 90        | 60         || Bounds.of(0, 0, 120, 60)
            '0 0 200 100'| UI.FitComponent.MIN_DIM   | 60        | 90         || Bounds.of(0, 0,  60, 30)
            '0 0 200 100'| UI.FitComponent.MAX_DIM   | 90        | 60         || Bounds.of(0, 0,  90, 45)
            '0 0 200 100'| UI.FitComponent.MAX_DIM   | 60        | 90         || Bounds.of(0, 0, 180, 90)
    }

    def 'An `SvgIcon` larger than the component it is painted onto is not cropped to it.'(
        UI.FitComponent fitMode, UI.Placement placement
    ) {
        reportInfo """
            An icon that declares a size larger than the component it is painted onto keeps that
            size: the component decides where there is room, not how large an explicitly sized icon
            is allowed to be. It therefore paints beyond the component, and it is up to whoever
            draws it to clip the result if that is not wanted.
        """
        given : 'A UI scale factor of 1, so that developer pixels and component pixels agree.'
            SwingTree.initializeUsing(it -> it.uiScaleFactor(1f) )
        and : 'An SVG document declaring 100 by 100 pixels, filled edge to edge with orange.'
            var icon = SvgIcon.of(
                        "<svg width=\"100\" height=\"100\" viewBox=\"0 0 100 100\">" +
                        "<rect x=\"0\" y=\"0\" width=\"100\" height=\"100\" fill=\"orange\"/></svg>"
                    )
                    .withFitComponent(fitMode)
                    .withPreferredPlacement(placement)
        and : 'A component of only 40 by 30 pixels, and a much larger image to paint into.'
            var panel = UI.panel().get(JPanel)
            panel.setSize(40, 30)
            var image = new BufferedImage(120, 120, BufferedImage.TYPE_INT_ARGB)

        when : 'We let the icon paint itself into the top left corner of the image...'
            var graphics = image.createGraphics()
            icon.paintIcon(panel, graphics, 0, 0)
            graphics.dispose()
        then : 'The orange reaches all the way to 100 by 100 pixels, far past the component.'
            new Color(image.getRGB(99, 99), true).getAlpha() >= 128
            new Color(image.getRGB(99, 99), true).getRed()   >= 128
        and : 'It stops there, because the icon is 100 by 100 pixels and nothing stretched it.'
            new Color(image.getRGB(101, 101), true).getAlpha() == 0

        cleanup :
            SwingTree.clear()

        where :
            fitMode                   | placement
            UI.FitComponent.UNDEFINED | UI.Placement.UNDEFINED
            UI.FitComponent.UNDEFINED | UI.Placement.TOP_LEFT
            UI.FitComponent.NO        | UI.Placement.UNDEFINED
            UI.FitComponent.NO        | UI.Placement.TOP_LEFT
            UI.FitComponent.MIN_DIM   | UI.Placement.UNDEFINED
            UI.FitComponent.MIN_DIM   | UI.Placement.TOP_LEFT
    }

    def 'An `SvgIcon` is painted with antialiasing, so a curved edge is not a staircase.'()
    {
        reportInfo """
            SwingTree turns antialiasing on before handing the document to the SVG renderer, which
            is what lets a curve end in pixels that are only partly covered instead of a hard step
            from painted to unpainted. A shape whose edges follow the pixel grid has no such
            partly covered pixels, which is how the two cases can be told apart.
        """
        given : 'A UI scale factor of 1, so that developer pixels and component pixels agree.'
            SwingTree.initializeUsing(it -> it.uiScaleFactor(1f) )
        and : 'A way to count the pixels that are neither fully transparent nor fully opaque.'
            var partlyCoveredPixels = { BufferedImage img ->
                int found = 0
                for ( int y = 0; y < img.getHeight(); y++ )
                    for ( int x = 0; x < img.getWidth(); x++ ) {
                        int alpha = new Color(img.getRGB(x, y), true).getAlpha()
                        if ( alpha > 0 && alpha < 255 )
                            found++
                    }
                return found
            }
        and : 'A component of 60 by 60 pixels to paint the icons onto.'
            var panel = UI.panel().get(JPanel)
            panel.setSize(60, 60)

        when : 'We paint an icon whose document is a circle...'
            var circleImage = new BufferedImage(60, 60, BufferedImage.TYPE_INT_ARGB)
            var circleGraphics = circleImage.createGraphics()
            SvgIcon.of("<svg width=\"60\" height=\"60\" viewBox=\"0 0 60 60\">" +
                       "<circle cx=\"30\" cy=\"30\" r=\"28\" fill=\"orange\"/></svg>")
                   .withFitComponent(UI.FitComponent.NO)
                   .paintIcon(panel, circleGraphics, 0, 0)
            circleGraphics.dispose()
        then : 'Its edge is made of many partly covered pixels.'
            partlyCoveredPixels(circleImage) > 50

        when : 'We paint an icon whose document is a rectangle along the pixel grid...'
            var squareImage = new BufferedImage(60, 60, BufferedImage.TYPE_INT_ARGB)
            var squareGraphics = squareImage.createGraphics()
            SvgIcon.of("<svg width=\"60\" height=\"60\" viewBox=\"0 0 60 60\">" +
                       "<rect x=\"0\" y=\"0\" width=\"60\" height=\"60\" fill=\"orange\"/></svg>")
                   .withFitComponent(UI.FitComponent.NO)
                   .paintIcon(panel, squareGraphics, 0, 0)
            squareGraphics.dispose()
        then : 'There is nothing for antialiasing to soften, so every pixel is fully covered or not at all.'
            partlyCoveredPixels(squareImage) == 0

        cleanup :
            SwingTree.clear()
    }

    def 'The opacity of an `SvgIcon` decides how much of what is behind it shows through.'(
        float opacity, int expectedCenterAlpha
    ) {
        reportInfo """
            An `SvgIcon` with an opacity below 1 is rendered into a buffer of its own first and
            that buffer is then drawn at the requested opacity. Doing it in one pass instead would
            let overlapping shapes inside the document show through each other, which is not what
            a partly transparent picture looks like. An opacity of 0 skips the rendering entirely,
            so nothing at all reaches the component.
        """
        given : 'A UI scale factor of 1, so that developer pixels and component pixels agree.'
            SwingTree.initializeUsing(it -> it.uiScaleFactor(1f) )
        and : 'An icon of an opaque orange circle, carrying the opacity of the current data table row.'
            var icon = SvgIcon.of("<svg width=\"60\" height=\"60\" viewBox=\"0 0 60 60\">" +
                                  "<circle cx=\"30\" cy=\"30\" r=\"28\" fill=\"orange\"/></svg>")
                               .withFitComponent(UI.FitComponent.NO)
                               .withOpacity(opacity)

        when : 'We paint it onto a 60 by 60 pixel component...'
            var panel = UI.panel().get(JPanel)
            panel.setSize(60, 60)
            var image = new BufferedImage(60, 60, BufferedImage.TYPE_INT_ARGB)
            var graphics = image.createGraphics()
            icon.paintIcon(panel, graphics, 0, 0)
            graphics.dispose()
        then : 'The middle of the circle carries the alpha the opacity asked for.'
            new Color(image.getRGB(30, 30), true).getAlpha() == expectedCenterAlpha

        cleanup :
            SwingTree.clear()

        where :
            opacity || expectedCenterAlpha
            1f      || 255
            0.75f   || 191
            0.5f    || 128
            0.25f   || 64
            0f      || 0
    }

    def 'The image an `SvgIcon` keeps while painting follows the size of the area it is painted into.'()
    {
        reportInfo """
            An `SvgIcon` remembers the image it last painted, so that a component repainting at an
            unchanged size does not make it rasterize the same document again. That memory is only
            usable while the area keeps its size: painting the same icon into a differently sized
            area has to rasterize it anew, or the second component would show the first one's
            picture stretched to fit.
        """
        given : 'A UI scale factor of 1, so that developer pixels and component pixels agree.'
            SwingTree.initializeUsing(it -> it.uiScaleFactor(1f) )
        and : 'One single icon instance, which is the one that does the remembering.'
            var icon = SvgIcon.of(
                        "<svg width=\"30\" height=\"30\" viewBox=\"0 0 100 100\">" +
                        "<rect x=\"0\" y=\"0\" width=\"100\" height=\"100\" fill=\"orange\"/></svg>"
                    )
                    .withFitComponent(UI.FitComponent.NO)
        and : 'A way to paint it onto a component of a given size and measure what the orange covered.'
            var coverageOn = { int areaWidth, int areaHeight ->
                var panel = UI.panel().get(JPanel)
                panel.setSize(areaWidth, areaHeight)
                var img = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB)
                var graphics = img.createGraphics()
                icon.paintIcon(panel, graphics, 0, 0)
                graphics.dispose()
                int left = Integer.MAX_VALUE, top = Integer.MAX_VALUE, right = -1, bottom = -1
                for ( int y = 0; y < img.getHeight(); y++ )
                    for ( int x = 0; x < img.getWidth(); x++ ) {
                        var pixel = new Color(img.getRGB(x, y), true)
                        if ( pixel.getAlpha() >= 128 && pixel.getRed() >= 128 && pixel.getBlue() < 128 ) {
                            left = Math.min(left, x) ; top    = Math.min(top, y)
                            right = Math.max(right, x); bottom = Math.max(bottom, y)
                        }
                    }
                return right < 0 ? Bounds.none() : Bounds.of(left, top, right - left + 1, bottom - top + 1)
            }

        when : 'We paint the icon onto a 90 by 60 pixel component...'
            var first = coverageOn(90, 60)
        then : 'The 30 by 30 pixel document sits in the middle of that area.'
            first == Bounds.of(30, 15, 30, 30)

        when : 'We paint the very same icon instance onto a 40 by 40 pixel component...'
            var second = coverageOn(40, 40)
        then : 'It is centered in the smaller area at its own size, not the stretched first picture.'
            second == Bounds.of(5, 5, 30, 30)

        when : 'And we go back to the first size...'
            var third = coverageOn(90, 60)
        then : 'We see the very same thing we saw the first time.'
            third == first

        cleanup :
            SwingTree.clear()
    }
}
