package swingtree

import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title
import swingtree.threading.EventProcessor

import javax.swing.*

@Title('OptionalUI, a Swing-Tree Monad')
@Narrative('''

    The OptionalUI is a monadic container object for AWT Component types
    which may or may not contain a non-null value.

    In a SwingTree application you typically encounter it as the return type
    of the `find(Class, id)` methods on the delegates of your event handlers,
    where a component you search for may or may not exist in the component tree.

    Its API is intentionally modelled after `java.util.Optional`, but with an
    important twist: UI components may only be accessed by the UI thread
    (the AWT event dispatch thread). So instead of just handing you the
    component, the OptionalUI either performs your actions on the UI thread
    for you, or it refuses to unpack the component when you are on the
    wrong thread. This way GUI state cannot accidentally leak into
    application threads where it would be unsafe to touch it.

''')
@Subject([OptionalUI])
class OptionalUI_Spec extends Specification
{
    def setupSpec() {
        SwingTree.get().setEventProcessor(EventProcessor.COUPLED_STRICT)
        // In this specification we are using the strict event processor
        // which will throw exceptions if we try to perform UI operations in the test thread.
    }

    def 'OptionalUI wraps AWT components exclusively.'()
    {
        given:
            var component = new JButton()
            var optionalUI = OptionalUI.of(()->UI.of(component))
        expect:
            optionalUI instanceof OptionalUI
            optionalUI.isPresent()
        and :
            UI.runAndGet({optionalUI.orElseThrow()}) == component
    }

    def 'OptionalUI can be mapped to a regular empty optional.'()
    {
        given:
            var optionalUI = OptionalUI.of(()->UI.panel()).map({ it -> null })
        expect:
            !optionalUI.isPresent()
    }

    def 'An OptionalUI can be mapped to a regular non-empty optional.'()
    {
        given:
            var optionalUI = OptionalUI.of(()->UI.panel()).map({ p -> new JButton() })
        expect:
            optionalUI.isPresent()
    }

    def 'An empty OptionalUI will throw an exception when orElseThrow is called.'()
    {
        given:
            var optionalUI = OptionalUI.of(()->UI.panel()).map({ it -> null })
        when:
            optionalUI.orElseThrow({new NoSuchElementException()})
        then:
            thrown(NoSuchElementException)
    }

    def 'An empty OptionalUI will return a default value when orElse is called.'()
    {
        given:
            var optionalUI = OptionalUI.of(()->UI.panel()).map({ it -> null })
        expect:
            optionalUI.orElse(new JButton()) instanceof JButton
    }

    def 'An empty OptionalUI will return a default value when orElseGet is called.'()
    {
        given:
            var optionalUI = OptionalUI.of(()->UI.panel()).map({ it -> null })
        expect:
            optionalUI.orElseGet({ new JButton() }) instanceof JButton
    }

    def 'Note that `map(Function)` takes you from the `OptionalUI` to a regular `Optional`.'()
    {
        reportInfo """
            The mapping function of the `map(Function)` method may produce
            any kind of object, not just UI components. So its return type
            is a plain `java.util.Optional` which no longer guards its
            contents against access from threads other than the UI thread.
            Use it to extract plain values, like a text or a size, out of
            the UI component, which you can then safely pass
            to other application threads.
            If you want to stay in the UI world and continue
            chaining component-related operations, use `update(Function)`
            or `updateIf(..)` instead!
        """
        given : 'An OptionalUI wrapping a text field declaration.'
            var optionalUI = OptionalUI.of(()->UI.textField().withText("Hello!"))
        when : 'We map the component to its text, a plain string.'
            var text = optionalUI.map({ c -> c.text })
        then : 'The result is a regular `java.util.Optional` holding the extracted value.'
            text instanceof Optional
            text.get() == "Hello!"
    }

    def 'Use `isEmpty()` to check if an `OptionalUI` has no component, it is the opposite of `isPresent()`.'()
    {
        given : 'A present and an empty OptionalUI.'
            var present = OptionalUI.of(()->UI.label("Hi!"))
            var empty   = OptionalUI.ofNullable(null)
        expect :
            !present.isEmpty() &&  present.isPresent()
             empty.isEmpty()   && !empty.isPresent()
    }

    def 'The `ifPresent(Consumer)` method performs an action with the component on the UI thread.'()
    {
        reportInfo """
            The consumer passed to `ifPresent(Consumer)` is executed
            on the UI thread, no matter which thread calls the method.
            This is because UI components may only ever be touched
            by the UI thread, which is the AWT event dispatch thread (EDT).
            If the OptionalUI is empty on the other hand, the consumer
            is simply never invoked.
        """
        given : 'A trace list for recording what happens and on which thread.'
            var trace = []
        and : 'A present and an empty OptionalUI.'
            var present = OptionalUI.of(()->UI.textField().withText("Guten Tag!"))
            var empty   = OptionalUI.ofNullable(null)

        when : 'We ask both of them to perform an action with their component.'
            present.ifPresent( c -> trace << "found '${c.text}' (on EDT: ${UI.thisIsUIThread()})" )
            empty.ifPresent( c -> trace << "how did this happen?" )
        and : 'We process all pending UI events to make sure the actions completed.'
            UI.sync()
        then : 'Only the present OptionalUI executed its action, and it did so on the UI thread.'
            trace == ["found 'Guten Tag!' (on EDT: true)"]
    }

    def 'The `ifPresentOrElse(Consumer, Runnable)` method also covers the empty case.'()
    {
        given : 'A trace list, a present and an empty OptionalUI.'
            var trace = []
            var present = OptionalUI.of(()->UI.label("Hi!"))
            var empty   = OptionalUI.ofNullable(null)

        when : 'Both are asked to either use their component or run a fallback action.'
            present.ifPresentOrElse( c -> trace << "text=${c.text}", () -> trace << "no label found" )
            empty.ifPresentOrElse( c -> trace << "text=${c.text}", () -> trace << "no label found" )
        and : 'We wait for the UI thread to complete the actions.'
            UI.sync()
        then : 'The present one used its component and the empty one ran the fallback.'
            trace == ["text=Hi!", "no label found"]
    }

    def 'Use `filter(Predicate)` to conditionally clear an `OptionalUI`.'()
    {
        reportInfo """
            Just like with a regular `Optional`, the `filter(Predicate)` method
            keeps the component only if it matches the given predicate,
            and otherwise returns an empty `OptionalUI`.
            You may call this from any thread, because internally the
            predicate is dispatched to the UI thread for you.
        """
        given : 'An OptionalUI holding a button with a text.'
            var optionalUI = OptionalUI.of(()->UI.button("Click me!"))
        expect : 'A matching predicate keeps the component.'
            optionalUI.filter( c -> c.text == "Click me!" ).isPresent()
        and : 'A non-matching predicate produces an empty OptionalUI.'
            optionalUI.filter( c -> c.text == "Do not click me!" ).isEmpty()
        and : 'Filtering an already empty OptionalUI simply stays empty.'
            OptionalUI.ofNullable(null).filter( c -> true ).isEmpty()
    }

    def 'The `update(Function)` method transforms the component without leaving the UI world.'()
    {
        reportInfo """
            Whereas `map(Function)` takes you from the `OptionalUI` monad
            to a regular `Optional`, the `update(Function)` method stays
            in the `OptionalUI` world. Its mapping function is executed
            on the UI thread, receives the current component, and its
            (usually modified) return value is wrapped in a new `OptionalUI`.
            This is the canonical way to safely modify a component
            you found through a `find(..)` call in an event handler.
        """
        given : 'An OptionalUI wrapping a text field.'
            var optionalUI = OptionalUI.of(()->UI.textField().withText("What is 6 x 7?"))
        when : 'We update the text of the wrapped component.'
            var updated = optionalUI.update( c -> { c.text = "42"; return c } )
        then : 'The result is again a present OptionalUI, wrapping the modified component.'
            updated.isPresent()
            UI.runAndGet({ updated.orElseThrow() }).text == "42"

        when : 'An empty OptionalUI is updated on the other hand...'
            var trace = []
            var stillEmpty = OptionalUI.ofNullable(null).update( c -> { trace << "never happens"; return c } )
        then : '...the mapping function is never invoked and the result stays empty.'
            trace.isEmpty()
            stillEmpty.isEmpty()
    }

    def 'The `updateIf(Class, Function)` method only transforms components of a particular type.'()
    {
        reportInfo """
            When searching for components you often end up with a rather
            general component type. The `updateIf(Class, Function)` method
            allows you to apply a transformation only if the component
            is actually an instance of a more specific type, in which case
            the mapping function receives it conveniently cast to that type.
            Otherwise the OptionalUI is returned unchanged.
        """
        given : 'An OptionalUI wrapping a humble button.'
            var optionalUI = OptionalUI.of(()->UI.button("I am a button"))
        when : 'We update it conditioned on it being a `JButton`, which it is.'
            optionalUI = optionalUI.updateIf(JButton.class, b -> { b.text = "I knew it!"; return b })
        then : 'The mapping function was applied.'
            UI.runAndGet({ optionalUI.orElseThrow() }).text == "I knew it!"

        when : 'We try the same conditioned on it being a `JTextField`, which it is not.'
            var trace = []
            optionalUI = optionalUI.updateIf(JTextField.class, t -> { trace << "never happens"; return t })
        then : 'The mapping function was ignored and the component is unchanged.'
            trace.isEmpty()
            UI.runAndGet({ optionalUI.orElseThrow() }).text == "I knew it!"
    }

    def 'The `updateIf(boolean, Function)` method transforms the component only if a condition is true.'()
    {
        given : 'An OptionalUI wrapping a label, and a flag from some business logic.'
            var optionalUI = OptionalUI.of(()->UI.label("Rock"))
        when : 'We apply two conditional updates, one active and one inactive.'
            optionalUI = optionalUI
                        .updateIf(true,  l -> { l.text = l.text + " and Roll"; return l })
                        .updateIf(false, l -> { l.text = "Silence";            return l })
        then : 'Only the update with the true condition was applied.'
            UI.runAndGet({ optionalUI.orElseThrow() }).text == "Rock and Roll"
    }

    def 'Supply alternative components for empty `OptionalUI`s through `or`, `orGet` and `orGetUi`.'()
    {
        reportInfo """
            The `or(Supplier<OptionalUI>)`, `orGet(Supplier<Component>)` and
            `orGetUi(Supplier<UIForAnything>)` methods all serve the same purpose:
            they provide an alternative component in case the OptionalUI is empty.
            They only differ in what kind of alternative you hand them,
            another `OptionalUI`, a raw component, or a full SwingTree declaration.
            If a component is already present, the supplier is simply ignored.
        """
        given : 'An empty and a present OptionalUI.'
            var empty   = OptionalUI.ofNullable(null)
            var present = OptionalUI.of(()->UI.label("original"))
        expect : 'All three methods fill the empty OptionalUI with the alternative:'
            empty.or({ OptionalUI.of(()->UI.label("alternative")) }).isPresent()
            empty.orGet({ new JButton("alternative") }).isPresent()
            empty.orGetUi({ UI.button("alternative") }).isPresent()
        and : 'For the present OptionalUI on the other hand, the original component always wins.'
            UI.runAndGet({ present.or({ OptionalUI.of(()->UI.label("alternative")) }).orElseThrow() }).text == "original"
            UI.runAndGet({ present.orGet({ new JButton("alternative") }).orElseThrow() }).text == "original"
            UI.runAndGet({ present.orGetUi({ UI.button("alternative") }).orElseThrow() }).text == "original"
    }

    def 'The `orGetIf(boolean, Supplier)` method only supplies an alternative if a condition is met.'()
    {
        given : 'An empty OptionalUI.'
            var empty = OptionalUI.ofNullable(null)
        expect : 'With a true condition, the alternative component is used...'
            empty.orGetIf(true, { new JButton("plan B") }).isPresent()
        and : '...whereas a false condition leaves the OptionalUI empty.'
            empty.orGetIf(false, { new JButton("plan B") }).isEmpty()
    }

    def 'Terminal accessors like `orNull()` and `orElseThrow()` may only be called on the UI thread.'()
    {
        reportInfo """
            All methods which hand the raw component out of the `OptionalUI`,
            like `orNull()`, `orElse(..)`, `orElseGet(..)` and `orElseThrow(..)`,
            guard the component against being accessed from any thread
            other than the UI thread, in which case they fail fast
            with an exception.
            This is the core promise of the `OptionalUI` design:
            a component can never accidentally wander off
            into application threads.

            So when you need the component itself, ask for it inside
            a `UI.run(..)` or `UI.runAndGet(..)` block.
        """
        given : 'A present OptionalUI.'
            var optionalUI = OptionalUI.of(()->UI.button("Sensitive Component"))
        and : 'We ensure that this specification does not run on the UI thread.'
            assert !UI.thisIsUIThread()

        when : 'We try to unpack the component directly from the test thread...'
            optionalUI.orNull()
        then : '...the OptionalUI refuses.'
            thrown(RuntimeException)

        when : 'The same request is dispatched to the UI thread instead...'
            var component = UI.runAndGet({ optionalUI.orNull() })
        then : '...it succeeds.'
            component instanceof JButton
            component.text == "Sensitive Component"
    }

    def 'An `OptionalUI` has value based equality and a debug friendly string representation.'()
    {
        given : 'A component and various OptionalUI instances.'
            var button = UI.runAndGet({ new JButton("Hi!") })
            var optional1 = OptionalUI.ofNullable(button)
            var optional2 = OptionalUI.ofNullable(button)
            var empty1    = OptionalUI.ofNullable(null)
            var empty2    = OptionalUI.ofNullable(null)
        expect : 'Two OptionalUIs are equal if they wrap the same component, or are both empty.'
            optional1 == optional2
            empty1 == empty2
            optional1 != empty1
        and : 'Their hash codes are consistent with equality.'
            optional1.hashCode() == optional2.hashCode()
            empty1.hashCode() == empty2.hashCode()
        and : 'Present and empty instances are unambiguously distinguishable in their string representations.'
            empty1.toString() == "OptionalUI.empty"
            optional1.toString().startsWith("OptionalUI[")
    }

}
