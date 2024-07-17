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
        given:
            var font = UI.font('Ubuntu-PLAIN-14')
        expect :
            font.family == 'Ubuntu'
            font.style == Font.PLAIN
            font.size == 14
    }

    def 'The `UI.font(String)` method allows you to access fonts from a system property.'()
    {
        given: 'First we setup a system property'
            System.setProperty('my.fonts.Ubuntu', 'Ubuntu-ITALIC-42')
        when: 'We use the `UI.font(String)` method to access the font'
            var font = UI.font('my.fonts.Ubuntu')
        then: 'The font is correctly parsed'
            font.family == 'Ubuntu'
            font.style == Font.ITALIC
            font.size == 42
    }
}
