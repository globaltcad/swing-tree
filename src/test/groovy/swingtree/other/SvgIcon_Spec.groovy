package swingtree.other

import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title
import swingtree.UI
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

    def 'The `SvgIcon` is immutable, and its size must be specified through wither methods.'()
    {
        reportInfo """
            The reason why the `SvgIcon` is immutable is because
            it makes caching of the icon easier and safer.
            So when you want to change the size of the icon,
            you must use its various wither methods.
        """
        given : 'We create a basic `SvgIcon` of a funnel.'
            var icon = new SvgIcon("/img/funnel.svg")
        when : 'We use the various wither methods to create differently sized icons.'
        var icon2 = icon.withIconWidth(12)
            var icon1 = icon.withIconHeight(13)
            var icon3 = icon.withIconSize(27, 16)
            var icon5 = icon.withIconSizeFromWidth(31)
            var icon4 = icon.withIconSizeFromHeight(24)
        then : 'These icons have different sizes.'
            icon1.getIconWidth()  == -1
            icon1.getIconHeight() == 13
            icon2.getIconWidth()  == 12
            icon2.getIconHeight() == -1
            icon3.getIconWidth()  == 27
            icon3.getIconHeight() == 16
            icon4.getIconWidth()  == 24
            icon4.getIconHeight() == 24
            icon5.getIconWidth()  == 31
            icon5.getIconHeight() == 31
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
}
