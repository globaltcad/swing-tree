package swingtree

import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title
import swingtree.api.IconDeclaration
import swingtree.layout.Size
import swingtree.style.SvgIcon

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
            var icon = new SvgIcon("/img/funnel.svg")
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
            is used by SwingTree to ensure that the SVG is upscaled accordingly! 
            Please note that the line below only exists for testing purposes, 
            SwingTree will determine a suitable 
            scaling factor for the current system automatically for you,
            so you do not have to specify this factor manually. 
        """
            SwingTree.get().setUiScaleFactor(uiScale)
        and : 'We create a basic `SvgIcon` of a funnel.'
            var icon = new SvgIcon("/img/funnel.svg")
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
            var icon  = new SvgIcon("/img/funnel.svg")
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
            icon.matches( /SvgIcon\[width=\?, height=\?, fitComponent=MIN_DIM, preferredPlacement=UNDEFINED, doc=.*\]/ )
            icon1.matches( /SvgIcon\[width=\?, height=13, fitComponent=MIN_DIM, preferredPlacement=UNDEFINED, doc=.*\]/ )
            icon2.matches( /SvgIcon\[width=12, height=\?, fitComponent=NO, preferredPlacement=UNDEFINED, doc=.*\]/ )
            icon3.matches( /SvgIcon\[width=27, height=16, fitComponent=MIN_DIM, preferredPlacement=UNDEFINED, doc=.*\]/ )
            icon4.matches( /SvgIcon\[width=31, height=31, fitComponent=MIN_DIM, preferredPlacement=BOTTOM_RIGHT, doc=.*\]/ )
            icon5.matches( /SvgIcon\[width=24, height=24, fitComponent=MIN_DIM, preferredPlacement=UNDEFINED, doc=.*\]/ )

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
            icon1.get() instanceof SvgIcon
            icon2.get() instanceof SvgIcon
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
}
