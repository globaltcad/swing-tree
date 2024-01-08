package swingtree.scrollpane

import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title
import swingtree.SwingTree
import swingtree.threading.EventProcessor
import swingtree.UI
import swingtree.UIForScrollPane

import javax.swing.JScrollPane

@Title("The Scroll Pane")
@Narrative('''

    Just like for any other main component in Swing,
    Swing-Tree also supports a nice API for 
    building UIs with scroll panes.
    
    A scroll pane is a component that allows
    the user to scroll through a larger view
    of a component. It is a container that
    contains a single component, called the
    viewport. The viewport is the area that
    is actually visible to the user. 
    
    The scroll pane also contains a set of
    scrollbars that allow the user to scroll
    the viewport. 
    
    In this specification, we will see how
    to build a scroll pane with Swing-Tree.

''')
@Subject([UIForScrollPane, JScrollPane])
class Scroll_Pane_Spec extends Specification
{
    def setupSpec() {
        SwingTree.get().setEventProcessor(EventProcessor.COUPLED_STRICT)
        // In this specification we are using the strict event processor
        // which will throw exceptions if we try to perform UI operations in the test thread.
    }

    def 'Use the `UI.ScrollBarPolicy` enum to configure the scroll pane scroll bars.'()
    {
        reportInfo """
            Note that this is based on the rather non-desciptive `with` method.
            We are using it because the type and name of the enum instance
            already describe the scroll bar policy.
            You will find this pattern in other places in Swing-Tree,
            where the `with` method is used to configure a component
            using an enum instance. 
        """
        given : 'We create a scroll pane with a custom scroll bar policy.'
            var ui =
                    UI.scrollPane()
                    .withScrollBarPolicy(UI.Active.NEVER)
        and : 'We actually build the component:'
            var scrollPane = ui.component

        expect : 'The scroll pane has the expected scroll bar policies.'
            scrollPane.getHorizontalScrollBarPolicy() == JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
            scrollPane.getVerticalScrollBarPolicy() == JScrollPane.VERTICAL_SCROLLBAR_NEVER
    }

    def 'Configure both the horizontal and vertical scroll policy individually.'()
    {
        reportInfo """
            Note that this is based on the rather non-desciptive `withHorizontal` 
            and `withVertical` methods.
            We are using them because the type and name of the enum instance
            already describe the scroll bar policy.
            You will find this pattern in other places in Swing-Tree,
            where the `with` method, or variations of it, are used to configure a component
            in a fluent way.
        """
        given : 'We create a scroll pane with a custom scroll bar policy.'
            var ui =
                    UI.scrollPane()
                    .withHorizontalScrollBarPolicy(UI.Active.NEVER)
                    .withVerticalScrollBarPolicy(UI.Active.ALWAYS)
        and : 'We actually build the component:'
            var scrollPane = ui.component

        expect : 'The scroll pane has the expected scroll bar policies.'
            scrollPane.getHorizontalScrollBarPolicy() == JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
            scrollPane.getVerticalScrollBarPolicy() == JScrollPane.VERTICAL_SCROLLBAR_ALWAYS
    }

    def 'We can configure the vertical and horizontal scroll bar scroll increment of a scroll pane.'()
    {
        given : 'We create a scroll pane with a custom scroll increment.'
            var ui =
                    UI.scrollPane()
                    .withHorizontalScrollIncrement(42)
                    .withVerticalScrollIncrement(24)
        and : 'We actually build the component:'
            var scrollPane = ui.component
        expect : 'The scroll pane has the expected scroll increments.'
            scrollPane.getHorizontalScrollBar().getUnitIncrement() == 42
            scrollPane.getVerticalScrollBar().getUnitIncrement() == 24
    }

    def 'We can configure the general scroll increment of the scroll pane scroll bars.'()
    {
        reportInfo """
            Note that this sets the scroll increment unit for both
            the vertical and horizontal scroll bars.
            So any previously set scroll increment unit for the
            vertical or horizontal scroll bar will be overwritten.
        """
        given : 'We create a scroll pane with a custom scroll increment.'
            var ui =
                    UI.scrollPane()
                    .withScrollIncrement(42)
        and : 'We actually build the component:'
            var scrollPane = ui.component

        expect : 'The scroll pane has the expected scroll increments, both vertical and horizontally.'
            scrollPane.getHorizontalScrollBar().getUnitIncrement() == 42
            scrollPane.getVerticalScrollBar().getUnitIncrement() == 42
    }

    def 'The horizontal as well as vertical block scroll increment can be configured easily.'()
    {
        given : 'We create a scroll pane with a custom block scroll increment.'
            var ui =
                    UI.scrollPane()
                    .withHorizontalBlockScrollIncrement(42)
                    .withVerticalBlockScrollIncrement(24)
        and : 'We actually build the component:'
            var scrollPane = ui.component
        expect : 'The scroll pane has the expected block scroll increments.'
            scrollPane.getHorizontalScrollBar().getBlockIncrement() == 42
            scrollPane.getVerticalScrollBar().getBlockIncrement() == 24
    }

    def 'Configure the block scroll increment for both scroll bars in one line.'()
    {
        reportInfo """
            Note that this sets the block scroll increment unit for both
            the vertical and horizontal scroll bars.
            So any previously set block scroll increment unit for the
            vertical or horizontal scroll bar will be overwritten.
        """
        given : 'We create a scroll pane with a custom block scroll increment.'
            var ui =
                    UI.scrollPane()
                    .withBlockScrollIncrement(42)
        and : 'We actually build the component:'
            var scrollPane = ui.component
        expect : 'The scroll pane has the expected block scroll increments, both vertical and horizontally.'
            scrollPane.getHorizontalScrollBar().getBlockIncrement() == 42
            scrollPane.getVerticalScrollBar().getBlockIncrement() == 42
    }
}
