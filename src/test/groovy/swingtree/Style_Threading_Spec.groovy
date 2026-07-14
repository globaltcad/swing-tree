package swingtree

import groovy.transform.CompileDynamic
import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Timeout
import spock.lang.Title
import sprouts.Var
import swingtree.animation.LifeTime
import swingtree.api.AnimatedItemStyler
import swingtree.api.ItemStyler
import swingtree.api.Styler
import swingtree.style.ComponentExtension
import swingtree.threading.EventProcessor
import utility.SwingTreeTestConfigurator
import utility.Wait

import javax.swing.JLabel
import javax.swing.SwingUtilities
import java.awt.Color
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@Title("Styles Across Threads")
@Narrative('''

    Style gathering is owned by the UI thread: every `Styler` lambda is
    evaluated as part of the style and paint cycle of its component.
    This is a deliberate design decision, because a style is a function
    of not only your own inputs, but also of the component itself
    (its parent, its children, its geometry), which belongs to the UI thread.

    This has an important consequence in the decoupled threading mode,
    where properties are owned by the application thread: a `Styler`
    lambda must not read a property from its enclosing scope, because
    that would leak the UI thread into application thread owned state.

    The solution is the property bound `withStyle(property, styler)`
    family of methods, whose `ItemStyler` receives the current property
    item as an explicit argument: captured from the property change event
    on the property's owning thread, published to a UI thread owned copy,
    and only then handed to the style function on the UI thread.
    The style is recalculated and the component repainted automatically
    on every change, so no separate `withRepaintOn` binding is needed.

    This specification documents that threading contract for the plain,
    the property bound and the animated style APIs.

''')
@Subject([UIForAnySwing, Styler, ItemStyler, AnimatedItemStyler, EventProcessor])
@Timeout(60)
@CompileDynamic
class Style_Threading_Spec extends Specification
{
    def setupSpec() {
        SwingTree.initializeUsing(SwingTreeTestConfigurator.get())
        SwingTree.get().setEventProcessor(EventProcessor.COUPLED_STRICT)
        // The global default remains strict, the decoupled mode is
        // activated per UI declaration through `UI.use(EventProcessor.DECOUPLED, ...)`.
    }

    def cleanupSpec() {
        SwingTree.clear()
    }

    def setup() {
        // We make sure that every feature starts with an empty application event queue,
        // so that no leftovers from previous tests can interfere.
        try {
            EventProcessor.DECOUPLED.joinUntilDoneOrException()
        } catch (Exception ignored) {}
    }

    def 'Plain `Styler` lambdas are always evaluated by the UI thread.'()
    {
        reportInfo """
            The `withStyle(Styler)` lambda is part of the style and paint
            cycle of its component, which is owned by the UI thread.
            This is exactly why such a lambda must not read application
            thread owned properties from its enclosing scope in the
            decoupled threading mode: use `withStyle(property, styler)`
            for that instead, which captures the item safely.
        """
        given : 'A trace recording the thread of every styler evaluation.'
            var evaluationThreads = new CopyOnWriteArrayList<Boolean>()
        and : 'A label with a plain style lambda, built in decoupled mode.'
            var label = UI.runAndGet({
                UI.use(EventProcessor.DECOUPLED, ()->
                    UI.label("styled").withStyle( conf -> {
                        evaluationThreads.add(SwingUtilities.isEventDispatchThread())
                        return conf.foregroundColor(Color.PINK)
                    })
                ).get(JLabel)
            })
        expect : 'The styler ran at least once, while the component was being built and styled.'
            !evaluationThreads.isEmpty()

        when : 'We force a fresh style gathering, the way a new paint cycle would.'
            UI.runNow({ ComponentExtension.from(label).gatherApplyAndInstallStyle(true) })
        then : 'Every single evaluation of the styler happened on the UI thread.'
            evaluationThreads.every( { it == true } )
    }

    def 'An `ItemStyler` receives the items of its property as captured event values, replayed in their original order.'()
    {
        reportInfo """
            This is the heart of the thread safety contract of the property
            bound `withStyle(property, styler)`: the styler does not read
            the property (which would yield whatever the application thread
            has written most recently), it receives the item belonging to
            each individual change event, captured on the property's owning
            thread.

            To prove this, we park the UI thread and fire two property
            changes from this thread. When the UI thread catches up, the
            styler must observe *both* intermediate items, in their original
            order - a live property read would only ever see the final one.
        """
        given : 'A color name property, and a trace of the items observed by the styler.'
            var itemsObserved = new CopyOnWriteArrayList<String>()
            var evaluationThreads = new CopyOnWriteArrayList<Boolean>()
            var colorName = Var.of("red")
        and : 'A label whose background is styled from the property item, built in decoupled mode.'
            var label = UI.runAndGet({
                UI.use(EventProcessor.DECOUPLED, ()->
                    UI.label("colorful").withStyle( colorName, (name, conf) -> {
                        itemsObserved.add(name)
                        evaluationThreads.add(SwingUtilities.isEventDispatchThread())
                        switch ( name ) {
                            case "green": return conf.foregroundColor(Color.GREEN)
                            case "blue" : return conf.foregroundColor(Color.BLUE)
                            default     : return conf.foregroundColor(Color.RED)
                        }
                    })
                ).get(JLabel)
            })
        expect : 'The initial item was already used to style the label.'
            itemsObserved.contains("red")
            UI.runAndGet({ label.foreground }) == Color.RED

        when : 'We park the UI thread and change the property twice from this thread.'
            var gate = new CountDownLatch(1)
            UI.run({ gate.await() })
            var foregroundWhileParked = null
            try {
                colorName.set("green")
                colorName.set("blue")
                foregroundWhileParked = label.foreground
            } finally {
                gate.countDown()
            }
            UI.sync()
        then : 'While the UI thread was parked, the label still wore its old style.'
            foregroundWhileParked == Color.RED
        and : 'After the UI thread replayed the events, the styler observed both captured items, in order.'
            itemsObserved.contains("green")
            itemsObserved.contains("blue")
            itemsObserved.indexOf("green") < itemsObserved.indexOf("blue")
        and : 'The label was restyled and repainted automatically, no `withRepaintOn` was needed.'
            UI.runAndGet({ label.foreground }) == Color.BLUE
        and : 'Every single evaluation of the styler happened on the UI thread.'
            evaluationThreads.every( { it == true } )
    }

    def 'A transitional style receives its toggle through captured event values and is evaluated by the UI thread.'()
    {
        reportInfo """
            The `withTransitionalStyle(toggle, lifetime, styler)` binding
            follows the same threading convention: the boolean toggle is
            captured from the property change event and handed to the UI
            thread, where the `AnimatedStyler` interpolates the style
            between the two states over the given lifetime.
        """
        given : 'A trace recording the thread of every styler evaluation.'
            var evaluationThreads = new CopyOnWriteArrayList<Boolean>()
        and : 'A boolean property and a label with a transitional style, built in decoupled mode.'
            var isOn = Var.of(false)
            var label = UI.runAndGet({
                UI.use(EventProcessor.DECOUPLED, ()->
                    UI.label("transitional").withTransitionalStyle( isOn, LifeTime.of(1, TimeUnit.MILLISECONDS), (status, conf) -> {
                        evaluationThreads.add(SwingUtilities.isEventDispatchThread())
                        return conf.foregroundColor( status.progress() > 0.5 ? Color.GREEN : Color.RED )
                    })
                ).get(JLabel)
            })
        expect : 'The label starts out in the off state.'
            UI.runAndGet({ label.foreground }) == Color.RED

        when : 'The application thread flips the toggle, and we wait for the transition to complete.'
            isOn.set(true)
            Wait.until({
                UI.sync()
                UI.runAndGet({
                    ComponentExtension.from(label).gatherApplyAndInstallStyle(true)
                    label.foreground
                }) == Color.GREEN
            }, 5_000)
        then : 'The label transitioned to the on state.'
            UI.runAndGet({ label.foreground }) == Color.GREEN
        and : 'Every single evaluation of the styler happened on the UI thread.'
            evaluationThreads.every( { it == true } )
    }

    def 'An `AnimatedItemStyler` animates towards each new property item, on the UI thread.'()
    {
        reportInfo """
            The animated flavour `withStyle(property, lifetime, styler)`
            restarts a transition animation on every property change, whose
            progress runs from 0 to 1 over the given lifetime. The styler
            receives both the captured item and the animation status, so a
            style can not only follow the application state, but smoothly
            animate towards it. The same thread safety contract applies:
            the item is captured on the property's owning thread and the
            styler is evaluated by the UI thread.
        """
        given : 'Traces for the observed items, progresses and evaluation threads.'
            var itemsObserved = new CopyOnWriteArrayList<String>()
            var progresses = new CopyOnWriteArrayList<Double>()
            var evaluationThreads = new CopyOnWriteArrayList<Boolean>()
        and : 'A mood property and a label with an animated item style, built in decoupled mode.'
            var mood = Var.of("calm")
            var label = UI.runAndGet({
                UI.use(EventProcessor.DECOUPLED, ()->
                    UI.label("animated").withStyle( mood, LifeTime.of(1, TimeUnit.MILLISECONDS), (m, status, conf) -> {
                        itemsObserved.add(m)
                        progresses.add(status.progress())
                        evaluationThreads.add(SwingUtilities.isEventDispatchThread())
                        return conf.foregroundColor( m == "excited" && status.progress() >= 1 ? Color.ORANGE : Color.GRAY )
                    })
                ).get(JLabel)
            })
        expect : 'The initial item is considered fully settled, so the styler saw it with a progress of 1.'
            itemsObserved.contains("calm")
            progresses.first() == 1.0d

        when : 'The application thread changes the mood, and we wait for the transition towards it to complete.'
            mood.set("excited")
            Wait.until({
                UI.sync()
                UI.runAndGet({
                    ComponentExtension.from(label).gatherApplyAndInstallStyle(true)
                    label.foreground
                }) == Color.ORANGE
            }, 5_000)
        then : 'The styler observed the new captured item and completed the transition towards it.'
            itemsObserved.contains("excited")
            UI.runAndGet({ label.foreground }) == Color.ORANGE
        and : 'Every single evaluation of the styler happened on the UI thread.'
            evaluationThreads.every( { it == true } )
    }
}
