package swingtree.other

import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title
import swingtree.OptionalUI
import swingtree.UI

import javax.swing.JButton

@Title('OptionalUI, a Swing-Tree Monad')
@Narrative('''

    The OptionalUI is a monadic container object for AWT Component types
    which may or may not contain a non-null value.
    
''')
@Subject([OptionalUI])
class OptionalUI_Spec extends Specification
{
    def 'OptionalUI wraps AWT components exclusively.'()
    {
        given:
            var component = new JButton()
            var optionalUI = UI.of(component).component()
        expect:
            optionalUI instanceof OptionalUI
            optionalUI.isPresent()
        and :
            UI.runAndGet({optionalUI.orElseThrow()}) == component
    }

    def 'OptionalUI can be mapped to a regular empty optional.'()
    {
        given:
            var optionalUI = UI.panel().component().map({ it -> null })
        expect:
            !optionalUI.isPresent()
    }

}
