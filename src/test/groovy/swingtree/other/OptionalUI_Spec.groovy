package swingtree.other

import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title
import swingtree.EventProcessor
import swingtree.OptionalUI
import swingtree.UI

import javax.swing.*

@Title('OptionalUI, a Swing-Tree Monad')
@Narrative('''

    The OptionalUI is a monadic container object for AWT Component types
    which may or may not contain a non-null value.
    
''')
@Subject([OptionalUI])
class OptionalUI_Spec extends Specification
{
    def setupSpec() {
        UI.SETTINGS().setEventProcessor(EventProcessor.COUPLED_STRICT)
        // In this specification we are using the strict event processor
        // which will throw exceptions if we try to perform UI operations in the test thread.
    }

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

    def 'An OptionalUI can be mapped to a regular non-empty optional.'()
    {
        given:
            var optionalUI = UI.runAndGet({UI.panel().component().map({ p -> new JButton() })})
        expect:
            optionalUI.isPresent()
    }

    def 'An empty OptionalUI will throw an exception when orElseThrow is called.'()
    {
        given:
            var optionalUI = UI.panel().component().map({ it -> null })
        when:
            optionalUI.orElseThrow({new NoSuchElementException()})
        then:
            thrown(NoSuchElementException)
    }

    def 'An empty OptionalUI will return a default value when orElse is called.'()
    {
        given:
            var optionalUI = UI.panel().component().map({ it -> null })
        expect:
            optionalUI.orElse(new JButton()) instanceof JButton
    }

    def 'An empty OptionalUI will return a default value when orElseGet is called.'()
    {
        given:
            var optionalUI = UI.panel().component().map({ it -> null })
        expect:
            optionalUI.orElseGet({ new JButton() }) instanceof JButton
    }

}
