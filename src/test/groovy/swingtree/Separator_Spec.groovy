package swingtree

import groovy.transform.CompileDynamic
import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title
import sprouts.Var
import swingtree.threading.EventProcessor
import utility.SwingTreeTestConfigurator

import javax.swing.JSeparator
import javax.swing.SwingConstants

@Title("Separators")
@Narrative('''

    A separator is a simple component that draws a thin line
    which is used to visually separate groups of components
    from each other, in menus, toolbars or plain panels.

    SwingTree exposes the `JSeparator` through the
    `UI.separator()` factory methods and the `UIForSeparator`
    builder, which allow you to declare the orientation
    and the length of the separation line, and even bind
    these aspects of its state to properties in your view model.

''')
@Subject([UIForSeparator, UI, UIFactoryMethods])
@CompileDynamic
class Separator_Spec extends Specification
{
    def setupSpec() {
        SwingTree.initializeUsing(SwingTreeTestConfigurator.get())
        SwingTree.get().setEventProcessor(EventProcessor.COUPLED_STRICT)
        // In this specification we are using the strict event processor
        // which will throw exceptions if we try to perform UI operations in the test thread.
    }

    def cleanupSpec() {
        SwingTree.clear()
    }

    def 'Use the `UI.separator()` factory method to create a horizontal separator.'()
    {
        reportInfo """
            The plain `UI.separator()` factory method wraps a `JSeparator`
            with its default orientation, which is horizontal,
            just like the line drawn between two menu items.
        """
        given : 'We declare and then build a plain separator component.'
            var separator = UI.separator().get(JSeparator)

        expect : 'It has the default horizontal orientation of the underlying `JSeparator`.'
            separator.orientation == SwingConstants.HORIZONTAL
    }

    def 'Pass an `UI.Axis` constant to the `separator(UI.Axis)` factory method to choose the orientation.'()
    {
        reportInfo """
            Instead of the integer constants that raw Swing expects
            in `JSeparator.setOrientation(int)`, SwingTree uses the
            type safe `UI.Axis` enum to declare whether the separation
            line runs horizontally or vertically.
        """
        given : 'A vertical and a horizontal separator.'
            var vertical   = UI.separator(UI.Axis.VERTICAL).get(JSeparator)
            var horizontal = UI.separator(UI.Axis.HORIZONTAL).get(JSeparator)

        expect : 'The orientation of each component matches its declaration.'
            vertical.orientation   == SwingConstants.VERTICAL
            horizontal.orientation == SwingConstants.HORIZONTAL
    }

    def 'The `withOrientation(UI.Axis)` method configures the orientation as part of a builder chain.'()
    {
        given : 'We declare a separator and set its orientation in the middle of the builder chain.'
            var separator =
                    UI.separator()
                    .withOrientation(UI.Axis.VERTICAL)
                    .get(JSeparator)

        expect :
            separator.orientation == SwingConstants.VERTICAL
    }

    def 'Bind an `UI.Axis` property to a separator to control its orientation dynamically.'()
    {
        reportInfo """
            The `separator(Val<UI.Axis>)` factory method, and equally the
            `withOrientation(Val<UI.Axis>)` builder method, bind an alignment
            property to the orientation of the separator.
            Whenever the property changes in your view model, the separator
            reorients itself automatically.
            This is useful for layouts which rearrange themselves
            dynamically, for example when the user resizes a window
            from a wide to a narrow shape.
        """
        given : 'An alignment property, as it would exist in a view model.'
            var alignment = Var.of(UI.Axis.HORIZONTAL)
        and : 'A separator bound to the property.'
            var separator = UI.separator(alignment).get(JSeparator)

        expect : 'The separator starts out with the initial orientation of the property.'
            separator.orientation == SwingConstants.HORIZONTAL

        when : 'The view model changes the alignment.'
            UI.runNow({ alignment.set(UI.Axis.VERTICAL) })
        then : 'The separator followed along.'
            separator.orientation == SwingConstants.VERTICAL
    }

    def 'Use `withLength(int)` to declare how long the separation line should be.'()
    {
        reportInfo """
            A `JSeparator` does not have a dedicated "length" attribute,
            instead its size is determined by its preferred size.
            The `withLength(int)` method is an orientation aware
            convenience method which sets the preferred width
            for a horizontal separator, and the preferred height
            for a vertical one, while leaving the respective
            other dimension, the thickness of the line, untouched.

            Note that the length is interpreted as an abstract pixel size,
            which is scaled according to the current DPI scaling factor
            of the SwingTree context. This is why the assertions below
            compare against `UI.scale(int)`.
        """
        given : 'A horizontal and a vertical separator, both with a length of 42.'
            var horizontal =
                    UI.separator(UI.Axis.HORIZONTAL)
                    .withLength(42)
                    .get(JSeparator)
            var vertical =
                    UI.separator(UI.Axis.VERTICAL)
                    .withLength(42)
                    .get(JSeparator)

        expect : 'For the horizontal separator the length became the preferred width.'
            horizontal.preferredSize.width == UI.scale(42)
        and : 'For the vertical separator on the other hand, it became the preferred height.'
            vertical.preferredSize.height == UI.scale(42)
    }

    def 'The length of a separator can be bound to an integer property.'()
    {
        reportInfo """
            Just like most other aspects of a component, the length of the
            separation line can be modelled dynamically by a property
            in your view model using the `withLength(Val<Integer>)` method.
            Whenever the property changes, the preferred size of the
            separator is updated according to its orientation.
        """
        given : 'An integer property modelling the length of the separator.'
            var length = Var.of(50)
        and : 'A horizontal separator bound to it.'
            var separator =
                    UI.separator(UI.Axis.HORIZONTAL)
                    .withLength(length)
                    .get(JSeparator)

        expect : 'The initial preferred width comes from the initial property value.'
            separator.preferredSize.width == UI.scale(50)

        when : 'The view model grows the separator.'
            UI.runNow({ length.set(200) })
        then : 'The preferred width of the component was updated.'
            separator.preferredSize.width == UI.scale(200)
    }

    def 'Null is not a valid axis for a separator.'()
    {
        reportInfo """
            The separator factory and builder methods reject null
            arguments, as well as nullable axis properties, eagerly
            with an exception, so that programming errors surface
            right where the UI is declared.
        """
        when : 'We try to declare a separator with a null axis...'
            UI.separator((UI.Axis) null)
        then : '...the declaration fails immediately.'
            thrown(IllegalArgumentException)

        when : 'The same happens for a property which permits null in it:'
            UI.separator().withOrientation(Var.ofNullable(UI.Axis, null))
        then :
            thrown(IllegalArgumentException)
    }
}
