package swingtree

import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title
import swingtree.threading.EventProcessor
import utility.Utility

import javax.swing.JFrame
import javax.swing.JPanel
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
        and : 'Then we build the scroll pane component:'
            var scrollPane = ui.get(JScrollPane)

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
            var scrollPane = ui.get(JScrollPane)

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
            var scrollPane = ui.get(JScrollPane)
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
            var scrollPane = ui.get(JScrollPane)

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
            var scrollPane = ui.get(JScrollPane)
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
            var scrollPane = ui.get(JScrollPane)
        expect : 'The scroll pane has the expected block scroll increments, both vertical and horizontally.'
            scrollPane.getHorizontalScrollBar().getBlockIncrement() == 42
            scrollPane.getVerticalScrollBar().getBlockIncrement() == 42
    }

    def 'Use a declarative configurator lambda instead implementing the `Scrollable` interface manually.'()
    {
        reportInfo """
            Classical Swing has the `Scrollable` interface, which is an optional
            interface the scroll pane content component may implement in order
            to configure how the component should be scrolled in the
            viewport of the scroll pane.
            
            This is a bit cumbersome to implement, and it prevents you from keeping your
            UI declarative, as you have to use inheritance instead of composition.
            
            Swing-Tree offers a solution to this through a declarative configurator lambda
            passed to the ´UI.scrollPane(Configurator)´ factory method.
            In this lambda, you can configure the scroll pane content component
            behavior in the viewport as you would with the `Scrollable` interface.
        """
        given : 'We create a scroll pane with a custom scrollable configurator.'
            var ui =
                    UI.scrollPane( it -> it
                        .prefSize(160, 130)
                        .blockIncrement(7)
                        .unitIncrement(5)
                        .fitHeight(false)
                        .fitWidth(true)
                    )
                    .add(
                        UI.panel().withSize(140, 100)
                        .add(
                            UI.html("<p> This is a long text that should be scrollable. </p>")
                        )
                    )
        and : 'We then build the component:'
            var scrollPane = ui.get(JScrollPane)
        expect : 'The scroll pane has the expected scrollable behavior.'
            scrollPane.getViewport().getView().getPreferredScrollableViewportSize() == new java.awt.Dimension(160, 130)
            scrollPane.getViewport().getView().getScrollableBlockIncrement(null, 0,0) == 7
            scrollPane.getViewport().getView().getScrollableUnitIncrement(null, 0,0) == 5
            scrollPane.getViewport().getView().getScrollableTracksViewportHeight() == false
            scrollPane.getViewport().getView().getScrollableTracksViewportWidth() == true
    }

    def 'The scroll configuration API produces a scroll pane whose content layout is calculated correctly.'()
    {
        reportInfo """
            In this little test we check if the layout of the content
            of a scroll pane is calculated correctly, for both the
            case where we use the scroll conf API to fit the viewport
            and for the case where we do not.
        """
        given : 'A bit of content for the scroll pane content layout test.'
            var TEXT =  "This is a little story about a long sentence which is unfortunately too long to fit horizontally " +
                        "placed on a single line of text in a panel inside a scroll pane. This is why it is a good idea " +
                        "to place me in a scroll pane.";

        and : 'We create a UI with a scroll pane layout.'
            var ui =
                UI.frame("Scroll Pane Layout Test").add(
                    UI.panel("wrap, fill").withPrefSize(350, 550)
                    .add("shrink",UI.label("Not implementing Scrollable:"))
                    .add("grow, push",
                        UI.scrollPane()
                        .add(
                            UI.panel("wrap", "", "[]push[]").id("content-1")
                            .withBackground(UI.Color.LIGHT_GRAY)
                            .add(UI.html(TEXT))
                            .add(UI.html("END"))
                        )
                    )
                    .add("shrink",UI.label("Using Scroll Conf:"))
                    .add("grow, push",
                        UI.scrollPane(it -> it.fitWidth(true))
                        .add(
                            UI.panel("wrap", "", "[]push[]").id("content-2")
                            .withBackground(UI.Color.LIGHT_GRAY)
                            .add(UI.html(TEXT))
                            .add(UI.html("END"))
                        )
                    )
                );
        and : 'We build the UI:'
            var frame = ui.get(JFrame)

        when : 'We do the layout of the component...'
            UI.runNow(()->frame.pack())

        then : 'The layout is calculated correctly.'
            frame.getWidth() == 360
            frame.getHeight() == 580

        when : 'We filter out the content panels...'
            var content1 = new Utility.Query(frame).find(JPanel, "content-1").orElseThrow(NoSuchElementException::new)
            var content2 = new Utility.Query(frame).find(JPanel, "content-2").orElseThrow(NoSuchElementException::new)

        then : 'The content panels have the expected size.'
            content1.getWidth() > 900
            content1.getHeight() == 227
            content2.getWidth() == 335
            content2.getHeight() == 242
    }
}
