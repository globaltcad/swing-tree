package swingtree

import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title
import swingtree.api.IconDeclaration
import swingtree.layout.Size
import swingtree.style.SvgIcon
import utility.Utility

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
            uiScale << [1, 2, 3]
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
        and : 'We turn each icon into its String representation:'
            icon  = icon .toString()
            icon1 = icon1.toString()
            icon2 = icon2.toString()
            icon3 = icon3.toString()
            icon4 = icon4.toString()
            icon5 = icon5.toString()

        expect : 'They all have the expected String representations:'
            icon.matches( /SvgIcon\[width=\?, height=\?, fitComponent=UNDEFINED, preferredPlacement=UNDEFINED, doc=.*\]/ )
            icon1.matches( /SvgIcon\[width=\?, height=13px, fitComponent=UNDEFINED, preferredPlacement=UNDEFINED, doc=.*\]/ )
            icon2.matches( /SvgIcon\[width=12px, height=\?, fitComponent=NO, preferredPlacement=UNDEFINED, doc=.*\]/ )
            icon3.matches( /SvgIcon\[width=27px, height=16px, fitComponent=UNDEFINED, preferredPlacement=UNDEFINED, doc=.*\]/ )
            icon4.matches( /SvgIcon\[width=31px, height=31px, fitComponent=UNDEFINED, preferredPlacement=BOTTOM_RIGHT, doc=.*\]/ )
            icon5.matches( /SvgIcon\[width=24px, height=24px, fitComponent=UNDEFINED, preferredPlacement=UNDEFINED, doc=.*\]/ )
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
            SwingTree.initialiseUsing(it -> it.uiScaleFactor(1f) )
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
            SwingTree.initialiseUsing(it -> it.uiScaleFactor(uiScale) )
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
            SwingTree.initialiseUsing(it -> it.uiScaleFactor(uiScale) )
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
            SwingTree.initialiseUsing(it -> it.uiScaleFactor(uiScale) )
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
            SwingTree.initialiseUsing( it -> it.uiScaleFactor(uiScale) )
        and :
            var declaration = IconDeclaration.ofSvg(svg)
            var svgIcon = declaration.find().orElseThrow(IllegalStateException::new)
        when : 'We create a buffered image from the SVG based icon...'
            var img = svgIcon.getImage()
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
            SwingTree.initialiseUsing(it -> it.uiScaleFactor(uiScale) )
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
            SwingTree.initialiseUsing(it -> it.uiScaleFactor(uiScale) )
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
            SwingTree.initialiseUsing(it -> it.uiScaleFactor(uiScale) )
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
            SwingTree.initialiseUsing(it -> it.uiScaleFactor(uiScale) )
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
            SwingTree.initialiseUsing(it -> it.uiScaleFactor(1f) )
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
            Size.of(-1, -1)  | ''        | ''         | UI.FitComponent.UNDEFINED        | UI.Placement.UNDEFINED    || /SvgIcon\[width=\?, height=\?, fitComponent=UNDEFINED, preferredPlacement=UNDEFINED, doc=SVGDocument\[width=100\.0, height=100\.0\]\]/
            Size.of(-1, -1)  | ''        | ''         | UI.FitComponent.NO               | UI.Placement.TOP_LEFT     || /SvgIcon\[width=\?, height=\?, fitComponent=NO, preferredPlacement=TOP_LEFT, doc=SVGDocument\[width=100\.0, height=100\.0\]\]/

            // Pixel dimensions
            Size.of(100, 50 )| 'px'      | 'px'       | UI.FitComponent.WIDTH            | UI.Placement.CENTER       || /SvgIcon\[width=100px, height=50px, fitComponent=WIDTH, preferredPlacement=CENTER, doc=SVGDocument\[width=100\.0, height=50\.0\]\]/
            Size.of(75 , 75 )| 'px'      | 'px'       | UI.FitComponent.HEIGHT           | UI.Placement.TOP_RIGHT    || /SvgIcon\[width=75px, height=75px, fitComponent=HEIGHT, preferredPlacement=TOP_RIGHT, doc=SVGDocument\[width=75\.0, height=75\.0\]\]/
            Size.of(200, 100)| 'px'      | 'px'       | UI.FitComponent.WIDTH_AND_HEIGHT | UI.Placement.BOTTOM_LEFT || /SvgIcon\[width=200px, height=100px, fitComponent=WIDTH_AND_HEIGHT, preferredPlacement=BOTTOM_LEFT, doc=SVGDocument\[width=200\.0, height=100\.0\]\]/

            // Percentage dimensions
            Size.of(100, 50 )| '%'       | '%'        | UI.FitComponent.MAX_DIM          | UI.Placement.RIGHT        || /SvgIcon\[width=100%, height=50%, fitComponent=MAX_DIM, preferredPlacement=RIGHT, doc=SVGDocument\[width=100\.0, height=50\.0\]\]/
            Size.of(75 , 100)| '%'       | '%'        | UI.FitComponent.MIN_DIM          | UI.Placement.BOTTOM       || /SvgIcon\[width=75%, height=100%, fitComponent=MIN_DIM, preferredPlacement=BOTTOM, doc=SVGDocument\[width=75\.0, height=100\.0\]\]/

            // Mixed units (one pixel, one percentage)
            Size.of(200,50)  | 'px'      | '%'        | UI.FitComponent.NO               | UI.Placement.TOP          || /SvgIcon\[width=200px, height=50%, fitComponent=NO, preferredPlacement=TOP, doc=SVGDocument\[width=200\.0, height=50\.0\]\]/
            Size.of(100,75)  | '%'       | 'px'       | UI.FitComponent.WIDTH            | UI.Placement.LEFT         || /SvgIcon\[width=100%, height=75px, fitComponent=WIDTH, preferredPlacement=LEFT, doc=SVGDocument\[width=100\.0, height=75\.0\]\]/

            // Mixed: one known, one unknown
            Size.of(32, -1)  | 'px'      | ''         | UI.FitComponent.HEIGHT           | UI.Placement.BOTTOM_RIGHT || /SvgIcon\[width=32px, height=\?, fitComponent=HEIGHT, preferredPlacement=BOTTOM_RIGHT, doc=SVGDocument\[width=32\.0, height=100\.0\]\]/
            Size.of(-1, 24)  | ''        | 'px'       | UI.FitComponent.WIDTH_AND_HEIGHT | UI.Placement.UNDEFINED    || /SvgIcon\[width=\?, height=24px, fitComponent=WIDTH_AND_HEIGHT, preferredPlacement=UNDEFINED, doc=SVGDocument\[width=100\.0, height=24\.0\]\]/
            Size.of(32, 3 )  | 'px'      | ''         | UI.FitComponent.HEIGHT           | UI.Placement.BOTTOM_RIGHT || /SvgIcon\[width=32px, height=\?, fitComponent=HEIGHT, preferredPlacement=BOTTOM_RIGHT, doc=SVGDocument\[width=32\.0, height=100\.0\]\]/
            Size.of(4 , 24)  | ''        | 'px'       | UI.FitComponent.WIDTH_AND_HEIGHT | UI.Placement.UNDEFINED    || /SvgIcon\[width=\?, height=24px, fitComponent=WIDTH_AND_HEIGHT, preferredPlacement=UNDEFINED, doc=SVGDocument\[width=100\.0, height=24\.0\]\]/

            // All placement options
            Size.of(64,64)   | 'px'      | 'px'       | UI.FitComponent.NO               | UI.Placement.TOP          || /SvgIcon\[width=64px, height=64px, fitComponent=NO, preferredPlacement=TOP, doc=SVGDocument\[width=64\.0, height=64\.0\]\]/
            Size.of(64,64)   | 'px'      | 'px'       | UI.FitComponent.NO               | UI.Placement.BOTTOM       || /SvgIcon\[width=64px, height=64px, fitComponent=NO, preferredPlacement=BOTTOM, doc=SVGDocument\[width=64\.0, height=64\.0\]\]/
            Size.of(64,64)   | 'px'      | 'px'       | UI.FitComponent.NO               | UI.Placement.LEFT         || /SvgIcon\[width=64px, height=64px, fitComponent=NO, preferredPlacement=LEFT, doc=SVGDocument\[width=64\.0, height=64\.0\]\]/
            Size.of(64,64)   | 'px'      | 'px'       | UI.FitComponent.NO               | UI.Placement.RIGHT        || /SvgIcon\[width=64px, height=64px, fitComponent=NO, preferredPlacement=RIGHT, doc=SVGDocument\[width=64\.0, height=64\.0\]\]/

            // All fit component options
            Size.of(128,96)  | 'px'      | 'px'       | UI.FitComponent.NO               | UI.Placement.CENTER       || /SvgIcon\[width=128px, height=96px, fitComponent=NO, preferredPlacement=CENTER, doc=SVGDocument\[width=128\.0, height=96\.0\]\]/
            Size.of(128,96)  | 'px'      | 'px'       | UI.FitComponent.WIDTH            | UI.Placement.CENTER       || /SvgIcon\[width=128px, height=96px, fitComponent=WIDTH, preferredPlacement=CENTER, doc=SVGDocument\[width=128\.0, height=96\.0\]\]/
            Size.of(128,96)  | 'px'      | 'px'       | UI.FitComponent.HEIGHT           | UI.Placement.CENTER       || /SvgIcon\[width=128px, height=96px, fitComponent=HEIGHT, preferredPlacement=CENTER, doc=SVGDocument\[width=128\.0, height=96\.0\]\]/
            Size.of(128,96)  | 'px'      | 'px'       | UI.FitComponent.WIDTH_AND_HEIGHT | UI.Placement.CENTER       || /SvgIcon\[width=128px, height=96px, fitComponent=WIDTH_AND_HEIGHT, preferredPlacement=CENTER, doc=SVGDocument\[width=128\.0, height=96\.0\]\]/
            Size.of(128,96)  | 'px'      | 'px'       | UI.FitComponent.MAX_DIM          | UI.Placement.CENTER       || /SvgIcon\[width=128px, height=96px, fitComponent=MAX_DIM, preferredPlacement=CENTER, doc=SVGDocument\[width=128\.0, height=96\.0\]\]/
            Size.of(128,96)  | 'px'      | 'px'       | UI.FitComponent.MIN_DIM          | UI.Placement.CENTER       || /SvgIcon\[width=128px, height=96px, fitComponent=MIN_DIM, preferredPlacement=CENTER, doc=SVGDocument\[width=128\.0, height=96\.0\]\]/
    }
}

