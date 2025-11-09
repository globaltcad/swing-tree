package swingtree

import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title
import swingtree.threading.EventProcessor
import utility.SwingTreeTestConfigurator

import java.awt.Font

@Title("Fonts")
@Narrative('''

    As part of the `UI` namespace, SwingTree ships with a custom `Font` 
    sub-type that exposes a set of additional convenience methods for 
    working with fonts in Swing applications.

''')
@Subject([UI.Font, UI, UIFactoryMethods])
class Font_Spec extends Specification
{
    def setupSpec() {
        SwingTree.initialiseUsing(SwingTreeTestConfigurator.get())
        SwingTree.get().setEventProcessor(EventProcessor.COUPLED)
        // This is so that the test thread is also allowed to perform UI operations
    }

    def 'Use the `UI.font(String)` method to parse a font string.'()
    {
        reportInfo """
            The `UI.font(String)` decodes a font string based on the following
            format: `family-style-size`. The `style` part is optional and can
            be one of the following: PLAIN, BOLD, ITALIC, or BOLD_ITALIC.
            You can also omit the size, in which case the default size of 12
            multiplied by the current look and feel's scaling factor is used.
        """
        given:
            var font1 = UI.font('Ubuntu-PLAIN-14').toAwtFont()
            var font2 = UI.font('Dancing Script-ITALIC-24').toAwtFont()
        expect :
            font1.family == 'Ubuntu'
            font1.style == Font.PLAIN
            font1.size == 14
        and :
            font2.family == 'Dancing Script'
            font2.style == Font.ITALIC
            font2.size == 24
    }

    def 'The `UI.font(String)` method allows you to access fonts from a system property.'()
    {
        given: 'First we setup a system property'
            System.setProperty('my.fonts.AlloyInk', 'Alloy Ink-ITALIC-42')
        when: 'We use the `UI.font(String)` method to access the font'
            var font = UI.font('my.fonts.AlloyInk').toAwtFont()
        then: 'The font is correctly parsed'
            font.family == 'a Alloy Ink'
            font.style == Font.ITALIC
            font.size == 42
    }

    def 'Simply pass the desired font name to the `UI.font(String)` to get a properly sized font object.'(
        float scalingFactor
    ) {
        reportInfo """
            The `UI.font(String)` method allows you to quickly instantiate a `Font` object
            with the given font family name that has a default size of 12 multiplied by the
            current look and feel's scaling factor.
        """
        given: 'We first initialise SwingTree using the given scaling factor'
            SwingTree.initialiseUsing(it -> it.uiScaleFactor(scalingFactor))
        and : 'A new font object based on the Buggie font family.'
            var font = UI.font('Buggie').toAwtFont()
        expect: 'The font object has the correct family and size.'
            font.family == 'Buggie'
            font.size == (int)(12 * scalingFactor)
            font.style == Font.PLAIN
        where :
            scalingFactor << [1f, 1.25f, 1.5f, 1.75f, 2f]
    }

    def 'The `UI.font(String)` factory method can fetch fonts from system properties and apply the correct scaling factor.'(
        float scalingFactor
    ) {
        reportInfo """
            The `UI.font(String)` not only parses font strings, but it can also fetch font
            definitions from system properties if parsing fails. When fetching fonts from
            system properties, the correct scaling factor is also applied to the font size.
        """
        given: 'We first initialise SwingTree using the given scaling factor'
            SwingTree.initialiseUsing(it -> it.uiScaleFactor(scalingFactor))
        and : 'We setup a system property for a font'
            System.setProperty('my.fonts.TestFont', 'Ubuntu-BOLD-42')
        when : 'We fetch the font using the UI.font factory method'
            var font = UI.font('my.fonts.TestFont').toAwtFont()
        then : 'The font has the correct family, style, and scaled size'
            font.family == 'Ubuntu'
            font.style == Font.BOLD
            font.size == Math.round(42 * scalingFactor)
        where :
            scalingFactor << [1f, 1.25f, 1.5f, 1.75f, 2f]
    }

    def 'The `UI.font(String)` factory method can create a font merely bz specifying the family.'(
        float scalingFactor
    ) {
        reportInfo """
            The `UI.font(String)` factory method can create a font merely by specifying
            the font family name. In this case, the default size of 12 multiplied by
            the current look and feel's scaling factor is used.
        """
        given: 'We first initialise SwingTree using the given scaling factor'
            SwingTree.initialiseUsing(it -> it.uiScaleFactor(scalingFactor))
        when : 'We create a font using only the family name'
            var font = UI.font('Dancing Script').toAwtFont()
        then : 'The font has the correct family and scaled size'
            font.family == 'Dancing Script'
            font.style == Font.PLAIN
            font.size == Math.round(12 * scalingFactor)
        where :
            scalingFactor << [1f, 1.25f, 1.5f, 1.75f, 2f]
    }
}
