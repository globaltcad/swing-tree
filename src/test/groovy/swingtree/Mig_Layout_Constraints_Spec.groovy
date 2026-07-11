package swingtree

import groovy.transform.CompileDynamic
import net.miginfocom.swing.MigLayout
import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title
import swingtree.layout.LayoutConstraint
import swingtree.layout.MigAddConstraint
import swingtree.threading.EventProcessor

import javax.swing.JLabel
import javax.swing.JPanel

@Title("Type Safe MigLayout Constraints")
@Narrative('''

    The default layout manager of SwingTree is the `MigLayout`,
    a very powerful grid based layout manager which is
    configured through constraint strings like "fill, insets 10"
    or "span 2, grow".

    Strings are however inherently brittle, typos only surface
    at runtime and your IDE cannot help you with autocompletion.
    This is why SwingTree also exposes the most common MigLayout
    constraints in the form of type safe constants and factory
    methods, like `UI.FILL`, `UI.WRAP(int)`, `UI.SPAN(int)` or `UI.GROW`,
    which can be composed using their `and(..)` methods and then
    passed to `withLayout(..)` and `add(..)` in your UI declarations.

    Note that this is merely a additional option for you.
    Using plain constraint strings is fine too, and both approaches
    can be mixed freely.

''')
@Subject([UILayoutConstants, LayoutConstraint, MigAddConstraint, UI])
@CompileDynamic
class Mig_Layout_Constraints_Spec extends Specification
{
    def setupSpec() {
        SwingTree.get().setEventProcessor(EventProcessor.COUPLED_STRICT)
        // In this specification we are using the strict event processor
        // which will throw exceptions if we try to perform UI operations in the test thread.
    }

    def cleanupSpec() {
        SwingTree.clear()
    }

    def 'The layout constraint constants and factory methods translate to their MigLayout string counterparts.'(
        Object constraint, String expected
    ) {
        reportInfo """
            Constants and factory methods for constraints targeting the
            whole layout, like `UI.FILL` or `UI.INSETS(int)`, produce
            `LayoutConstraint` instances, whereas those targeting an
            individual component, like `UI.SPAN(int)` or `UI.GROW`,
            produce `MigAddConstraint` instances.
            Both are just type safe wrappers around the corresponding
            MigLayout constraint strings, as their `toString()`
            representations reveal.
        """
        expect :
            constraint.toString() == expected

        where :
            constraint             || expected
            UI.FILL                || "fill"
            UI.FILL_X              || "fillx"
            UI.FILL_Y              || "filly"
            UI.INSETS(10)          || "insets 10"
            UI.INSETS(1, 2, 3, 4)  || "insets 1 2 3 4"
            UI.INS(10)             || "insets 10"
            UI.WRAP(3)             || "wrap 3"
            UI.FLOW_X              || "flowx"
            UI.NO_GRID             || "nogrid"
            UI.DEBUG               || "debug"

            UI.WRAP                || "wrap"
            UI.SPAN(2)             || "span 2"
            UI.SPAN(2, 3)          || "span 2 3"
            UI.SPAN_X(4)           || "spanx 4"
            UI.SPAN_Y(5)           || "spany 5"
            UI.GROW                || "grow"
            UI.GROW(50)            || "grow 50"
            UI.GROW_X              || "growx"
            UI.SHRINK              || "shrink"
            UI.PUSH                || "push"
            UI.PUSH_Y(200)         || "pushy 200"
            UI.SKIP(2)             || "skip 2"
            UI.SPLIT(3)            || "split 3"
            UI.WIDTH(10, 20, 30)   || "width 10:20:30"
            UI.HEIGHT(10, 20, 30)  || "height 10:20:30"
            UI.PAD(5)              || "pad 5 5 5 5"
            UI.ALIGN_CENTER        || "align center"
    }

    def 'Use the `and(..)` method to compose multiple constraints into one.'()
    {
        reportInfo """
            The `and(..)` methods of both the `LayoutConstraint` and the
            `MigAddConstraint` types merge two constraints into a new one,
            leaving the merged instances untouched, they are immutable.

            Note that the merged constraints form a *set*, so duplicates
            are collapsed and the order in which the individual constraints
            appear in the final constraint string is not defined.
            This is fine, because MigLayout does not care about
            the order of its comma separated constraints either.
        """
        given : 'We compose a layout constraint from two constants and a factory method.'
            var constraint = UI.FILL_X.and(UI.NO_GRID).and(UI.INSETS(12))
        expect : 'The result contains all three constraints in the form of a comma separated string.'
            constraint.toString().split(", ").toList().toSorted() == ["fillx", "insets 12", "nogrid"]

        when : 'We merge a constraint with itself.'
            var deduplicated = UI.FILL.and(UI.FILL)
        then : 'The duplicate collapses, because the composition is set based.'
            deduplicated.toString() == "fill"
    }

    def 'A composed `LayoutConstraint` installs a fully configured `MigLayout` on a component.'()
    {
        reportInfo """
            Passing a `LayoutConstraint` to the `withLayout(..)` method
            is equivalent to passing the corresponding constraint string.
            SwingTree creates a `MigLayout` from it and installs
            it on the component being declared.
        """
        given : 'A panel declared with type safe layout constraints.'
            var panel =
                    UI.panel().withLayout(UI.FILL_X.and(UI.INS(24)))
                    .get(JPanel)

        expect : 'The panel has a `MigLayout` installed.'
            panel.layout instanceof MigLayout
        and : 'Its layout constraints contain the MigLayout representation of our type safe declaration.'
            panel.layout.getLayoutConstraints().contains("fillx")
            panel.layout.getLayoutConstraints().contains("insets 24")
    }

    def 'A composed `MigAddConstraint` tells the `MigLayout` how to place an individual component.'()
    {
        reportInfo """
            The `add(..)` methods of the SwingTree builders accept
            `MigAddConstraint` composites for the component being added,
            just like they accept plain component constraint strings.
        """
        given : 'A panel with a MigLayout based grid, containing a label with type safe add constraints.'
            var panel =
                    UI.panel().withLayout(UI.FILL)
                    .add(UI.GROW.and(UI.SPAN(2)),
                        UI.label("I grow across two cells!")
                    )
                    .get(JPanel)
        and : 'We fetch the layout manager and the added label.'
            var layout = (MigLayout) panel.layout
            var label = panel.getComponent(0)

        expect : 'The label was added to the panel...'
            label instanceof JLabel
        and : '...and the layout manager knows it by our composed component constraints.'
            String.valueOf(layout.getComponentConstraints(label)).contains("grow")
            String.valueOf(layout.getComponentConstraints(label)).contains("span 2")
    }
}
