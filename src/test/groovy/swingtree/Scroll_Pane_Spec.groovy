package swingtree

import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title
import swingtree.threading.EventProcessor
import utility.SwingTreeTestConfigurator
import utility.Utility

import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.SwingConstants
import java.awt.Dimension
import java.awt.Font

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
        SwingTree.initializeUsing(SwingTreeTestConfigurator.get())
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
                UI.frame("Scroll Pane Layout Test")
                .peek(it->it.setPreferredSize(new Dimension(350, 550)))
                .add(
                    UI.panel("wrap, fill").withPrefSize(350, 550)
                    .add("shrink",UI.label("Not implementing Scrollable:"))
                    .add("grow, push",
                        UI.scrollPane().id("scroll-1")
                        .add(
                            UI.panel("wrap", "", "[]push[]").id("content-1")
                            .withBackground(UI.Color.LIGHT_GRAY)
                            .add(UI.html(TEXT).withFont(new Font("Ubuntu", Font.BOLD, 12)))
                            .add(UI.html("END").withFont(new Font("Ubuntu", Font.BOLD, 12)))
                        )
                    )
                    .add("shrink",UI.label("Using Scroll Conf:"))
                    .add("grow, push",
                        UI.scrollPane(it -> it.fitWidth(true))
                        .id("scroll-2")
                        .add(
                            UI.panel("wrap", "", "[]push[]").id("content-2")
                            .withBackground(UI.Color.LIGHT_GRAY)
                            .add(UI.html(TEXT).withFont(new Font("Ubuntu", Font.BOLD, 12)))
                            .add(UI.html("END").withFont(new Font("Ubuntu", Font.BOLD, 12)))
                        )
                    )
                );
        and : 'We build the UI:'
            var frame = ui.get(JFrame)

        when : 'We do the layout of the component...'
            UI.runNow(()->frame.pack())

        then : 'The layout is calculated correctly.'
            frame.getWidth() == 350
            frame.getHeight() == 550

        when : 'We filter out the content panels...'
            var scroll1 = new Utility.Query(frame).find(JScrollPane, "scroll-1").orElseThrow(NoSuchElementException::new)
            var scroll2 = new Utility.Query(frame).find(JScrollPane, "scroll-2").orElseThrow(NoSuchElementException::new)
            var content1 = new Utility.Query(frame).find(JPanel, "content-1").orElseThrow(NoSuchElementException::new)
            var content2 = new Utility.Query(frame).find(JPanel, "content-2").orElseThrow(NoSuchElementException::new)

        then : 'The content panels have the covariance relative to their viewport.'
            content1.getSize() != scroll1.getViewport().getSize()
            content2.getSize() == scroll2.getViewport().getSize()
        and :
            content1.getWidth() > scroll1.getViewport().getWidth()
            content1.getHeight() == scroll1.getViewport().getHeight()
        and : 'The content panels have the expected size.'
            content1.getWidth() > 910
            200 <= content1.getHeight() && content1.getHeight() <= 230
            315 <= content2.getWidth()  && content2.getWidth()  <= 335
            215 <= content2.getHeight() && content2.getHeight() <= 245
    }

    def 'The view of a configured scroll pane reports a preferred size which tracks its content.'()
    {
        reportInfo """
            When you configure a scroll pane through the `UI.scrollPane(Configurator)` factory
            method, then SwingTree places a thin delegation box between the viewport and your
            content component. The box exists so that the `Scrollable` behaviour can be supplied
            declaratively instead of through inheritance, and it is meant to be completely
            transparent with respect to sizing: whatever the content component says it needs,
            the box says too.

            This little test pins that transparency down for the case that tends to break it,
            namely a content component which changes size after the box has already been
            measured once. Note that we deliberately measure the box while it is in the
            invalidated state, because that is the state it spends every layout pass in, and
            because an invalid component does not receive `invalidate()` from its children a
            second time. So a box which remembered its last measurement would answer with a
            stale size here, and the scroll pane would then lay out and scroll a view that
            does not exist anymore.
        """
        given : 'A label whose text we are going to grow later on, placed inside a configured scroll pane.'
            var label = new JLabel("short")
            var ui =
                UI.frame("Scroll Pane Content Growth Test")
                .peek(it -> it.setPreferredSize(new Dimension(400, 200)))
                .add(
                    UI.scrollPane( conf -> conf.fitWidth(true) ).id("scroll")
                    .add(
                        UI.panel("fill, ins 0").id("content")
                        .add("grow", UI.of(label))
                    )
                )
        and : 'We build the UI and let it lay itself out:'
            var frame = ui.get(JFrame)
            UI.runNow( () -> frame.pack() )
        and : 'We look up the scroll pane as well as the delegation box acting as its view.'
            var scrollPane = new Utility.Query(frame).find(JScrollPane, "scroll").orElseThrow(NoSuchElementException::new)
            var content    = new Utility.Query(frame).find(JPanel, "content").orElseThrow(NoSuchElementException::new)
            var view       = UI.runAndGet( () -> scrollPane.getViewport().getView() )

        expect : 'Right after the first layout the box already agrees with its content.'
            UI.runAndGet( () -> view.getPreferredSize() ) == UI.runAndGet( () -> content.getPreferredSize() )

        when : 'The box enters the invalidated state it is in throughout every layout pass,'
            UI.runNow( () -> view.invalidate() )
        and : 'and it is measured while being in that state, just like a layout pass would do,'
            var widthBefore = UI.runAndGet( () -> view.getPreferredSize() ).width
        and : 'and the content then becomes substantially wider.'
            UI.runNow( () -> label.setText("a piece of text which is a great deal longer than the previous one, by a wide margin indeed") )

        then : 'The box grew together with its content instead of reporting the size it had before.'
            UI.runAndGet( () -> view.getPreferredSize() ).width > widthBefore
        and : 'It still reports exactly what the content component itself reports.'
            UI.runAndGet( () -> view.getPreferredSize() ) == UI.runAndGet( () -> content.getPreferredSize() )

        when : 'We lay the whole frame out again and shrink the content back down,'
            UI.runNow( () -> frame.validate() )
            var widthOfWideContent = UI.runAndGet( () -> view.getPreferredSize() ).width
            UI.runNow( () -> label.setText("short") )

        then : 'the box shrinks along with it.'
            UI.runAndGet( () -> view.getPreferredSize() ).width < widthOfWideContent
            UI.runAndGet( () -> view.getPreferredSize() ) == UI.runAndGet( () -> content.getPreferredSize() )
    }

    def 'Asking a configured scroll pane for a scroll increment does not measure its content.'()
    {
        reportInfo """
            A scroll pane configured through `UI.scrollPane(Configurator)` builds a fresh
            configuration object for every single question the scroll pane asks it, and Swing
            asks a handful of those per layout pass. Most of them are cheap questions whose
            answer does not depend on how big the content is, like "how far is one scroll
            unit?", and the same goes for every value the configurator supplies itself.

            None of those should measure the content component, because measuring a densely
            populated view is by far the most expensive thing a scroll pane ever does.
        """
        given : 'A content component whose layout manager counts how often it is measured.'
            var layout  = new CountingLayout()
            var ui =
                UI.frame("Scroll Increment Measurement Test")
                .peek(it -> it.setPreferredSize(new Dimension(400, 200)))
                .add(
                    UI.scrollPane( conf -> conf
                        .unitIncrement(7)
                        .blockIncrement(13)
                        .fitWidth(true)
                        .fitHeight(false)
                    )
                    .id("scroll")
                    .add(UI.of(new JPanel(layout)).id("content"))
                )
        and : 'We build the UI and let it lay itself out:'
            var frame = ui.get(JFrame)
            UI.runNow( () -> frame.pack() )
        and : 'We look up the content as well as the delegation box acting as the view.'
            var scrollPane = new Utility.Query(frame).find(JScrollPane, "scroll").orElseThrow(NoSuchElementException::new)
            var content    = new Utility.Query(frame).find(JPanel, "content").orElseThrow(NoSuchElementException::new)
            var view       = UI.runAndGet( () -> scrollPane.getViewport().getView() )

        when : 'We invalidate the content so that no cached measurement can answer on its behalf,'
            UI.runNow( () -> content.invalidate() )
        and : 'we start counting from here,'
            var measurementsBefore = layout.measurements
        and : 'and then ask the view every question whose answer the configurator already supplied.'
            var unitIncrement  = UI.runAndGet( () -> view.getScrollableUnitIncrement(null, SwingConstants.VERTICAL, 1) )
            var blockIncrement = UI.runAndGet( () -> view.getScrollableBlockIncrement(null, SwingConstants.VERTICAL, 1) )
            var tracksWidth    = UI.runAndGet( () -> view.getScrollableTracksViewportWidth() )
            var tracksHeight   = UI.runAndGet( () -> view.getScrollableTracksViewportHeight() )

        then : 'We receive exactly what was configured,'
            unitIncrement  == 7
            blockIncrement == 13
            tracksWidth    == true
            tracksHeight   == false
        and : 'and the content was not measured a single time for any of it.'
            layout.measurements == measurementsBefore

        when : 'We ask for the preferred viewport size, which the configurator did not supply,'
            var preferredViewportSize = UI.runAndGet( () -> view.getPreferredScrollableViewportSize() )

        then : 'the content is measured after all, because now the answer does depend on it.'
            preferredViewportSize == new Dimension(300, 200)
            layout.measurements > measurementsBefore
    }

    def 'A scroll pane still derives the viewport fitting defaults from the content it was given.'()
    {
        reportInfo """
            The `fitWidth` and `fitHeight` flags of the scroll configuration API have defaults
            which are derived from the content: a content component which is already smaller
            than the viewport has nothing to scroll and may just as well be stretched to fill
            it, whereas one which is larger must keep its own size so that it can be scrolled.

            These defaults are computed only when they are actually read, so this test makes
            sure that deferring them did not quietly change what they are.
        """
        given : 'A scroll pane with an identity configurator, so that all defaults survive.'
            var content = new JPanel(new CountingLayout(width: contentWidth, height: contentHeight))
            var ui =
                UI.frame("Scroll Fitting Defaults Test")
                .peek(it -> it.setPreferredSize(new Dimension(400, 300)))
                .add(
                    UI.scrollPane( conf -> conf ).id("scroll")
                    .withScrollBarPolicy(UI.Active.NEVER)
                    .add(UI.of(content))
                )
        and : 'We build the UI and let it lay itself out:'
            var frame = ui.get(JFrame)
            UI.runNow( () -> frame.pack() )
            var scrollPane = new Utility.Query(frame).find(JScrollPane, "scroll").orElseThrow(NoSuchElementException::new)
            var view       = UI.runAndGet( () -> scrollPane.getViewport().getView() )

        expect : 'The viewport only takes over a dimension the content does not need for itself.'
            UI.runAndGet( () -> view.getScrollableTracksViewportWidth()  ) == expectedToFitWidth
            UI.runAndGet( () -> view.getScrollableTracksViewportHeight() ) == expectedToFitHeight

        where :
            contentWidth | contentHeight || expectedToFitWidth | expectedToFitHeight
            50           | 50            || true               | true
            5000         | 50            || false              | true
            50           | 5000          || true               | false
            5000         | 5000          || false              | false
    }

    def 'You can add a custom ´Scrollable´ component to a scroll pane with layout constraints that work.'()
    {
        reportInfo """
            In this little test we check if the usage of layout constraints
            when adding a custom Scrollable component to a scroll pane works.
            Note that this is not something supported in regular Swing, 
            SwingTree however, will wrap and delegate your custom Scrollable
            component in a JPanel, with a ´MigLayout´ instance that
            respects the layout constraints you set.
        """
        given : """
            We create a UI with 2 different scroll panes each containing a custom
            ´Scrollable´ component. The first one however receives no layout constraints,
            while the second one does.
        """
            var ui =
                UI.frame("Scrollable Pane Layout Test")
                .peek(it->it.setPreferredSize(new Dimension(500, 300)))
                .add(
                    UI.panel("fill", "[grow][grow]").withPrefSize(500, 300)
                    .add("shrink",UI.label("Without Constraints:"))
                    .add("grow, push, wrap",
                        UI.scrollPane().id("scroll-1")
                        .add(
                            UI.of(new CustomScrollablePanel()).withLayout("wrap 2").id("content-1")
                            .add(UI.label("Forename: "))
                            .add(UI.textField("Joey"))
                            .add(UI.label("Surname: "))
                            .add(UI.textField("Carbstrong"))
                        )
                    )
                    .add("shrink",UI.label("With Constraints:"))
                    .add("grow, push, wrap",
                        UI.scrollPane().id("scroll-2")
                        .id("scroll-2")
                        .add("grow, push",
                            UI.of(new CustomScrollablePanel()).id("content-2")
                            .add(UI.label("Forename: "))
                            .add(UI.textField("Joey"))
                            .add(UI.label("Surname: "))
                            .add(UI.textField("Carbstrong"))
                        )
                    )
                );
        and : 'Then we just build the UI:'
            var frame = ui.get(JFrame)

        when : 'We do the layout of the component...'
            UI.runNow( () -> frame.pack() )
        and : 'We fetch the content panels so we can check their layout.'
            var scroll1 = new Utility.Query(frame).find(JScrollPane, "scroll-1" ).orElseThrow(NoSuchElementException::new)
            var scroll2 = new Utility.Query(frame).find(JScrollPane, "scroll-2" ).orElseThrow(NoSuchElementException::new)
            var content1    = new Utility.Query(frame).find(JPanel,      "content-1").orElseThrow(NoSuchElementException::new)
            var content2    = new Utility.Query(frame).find(JPanel,      "content-2").orElseThrow(NoSuchElementException::new)

        then : 'The content panels have the covariance relative to their viewport.'
            content1.getSize().getWidth()  == scroll1.getViewport().getSize().getWidth()
            content1.getSize().getHeight() <  scroll1.getViewport().getSize().getHeight()
            content2.getSize().getWidth()  == scroll2.getViewport().getSize().getWidth()
            content2.getSize().getHeight() <  scroll2.getViewport().getSize().getHeight()
        and : 'The content components are in fact of the custom scrollable type we use for testing.'
            content1 instanceof CustomScrollablePanel
            content2 instanceof CustomScrollablePanel
    }


    def 'The layout constraints of a custom ´Scrollable´ component are actually applied to it.'()
    {
        reportInfo """
            Adding a component to a scroll pane together with layout constraints is something
            regular Swing has no answer for, because a viewport holds exactly one view and no
            layout manager to interpret a constraint with. SwingTree supplies the missing
            layout manager by slipping a thin box in between, and this has to happen for a
            content component implementing `Scrollable` just as much as for any other one --
            the box then simply passes the scroll behaviour of its child on to the scroll pane,
            so that nothing is lost by the indirection.

            Here we give the very same custom `Scrollable` component to two scroll panes, one
            of them with a size constraint, and then check that the constraint made a
            difference. Note that the component in question reports that it wants to track the
            width of its viewport, so without the box in between it would be stretched to the
            full viewport width and the constraint would simply evaporate.
        """
        given : 'Two scroll panes with the same custom `Scrollable` content, one of them constrained.'
            var ui =
                UI.frame("Scrollable Constraints Test")
                .peek(it -> it.setPreferredSize(new Dimension(600, 400)))
                .add(
                    UI.panel("fill, wrap 1", "[grow]", "[grow][grow]").withPrefSize(600, 400)
                    .add("grow, push",
                        UI.scrollPane().id("scroll-unconstrained")
                        .add(
                            UI.of(new CustomScrollablePanel()).id("content-unconstrained")
                            .add(UI.label("Hello"))
                        )
                    )
                    .add("grow, push",
                        UI.scrollPane().id("scroll-constrained")
                        .add("width 250px",
                            UI.of(new CustomScrollablePanel()).id("content-constrained")
                            .add(UI.label("Hello"))
                        )
                    )
                )
        and : 'We build the UI and let it lay itself out:'
            var frame = ui.get(JFrame)
            UI.runNow( () -> frame.pack() )
        and : 'We look up the two scroll panes and their content components.'
            var unconstrainedScroll  = new Utility.Query(frame).find(JScrollPane, "scroll-unconstrained").orElseThrow(NoSuchElementException::new)
            var constrainedScroll    = new Utility.Query(frame).find(JScrollPane, "scroll-constrained").orElseThrow(NoSuchElementException::new)
            var unconstrainedContent = new Utility.Query(frame).find(JPanel, "content-unconstrained").orElseThrow(NoSuchElementException::new)
            var constrainedContent   = new Utility.Query(frame).find(JPanel, "content-constrained").orElseThrow(NoSuchElementException::new)

        expect : 'The unconstrained content does what its own `Scrollable` implementation asks for.'
            unconstrainedContent.getWidth() == unconstrainedScroll.getViewport().getWidth()
        and : 'The constrained content has the width we demanded of it instead.'
            constrainedContent.getWidth()  == 250
        and : 'Its viewport is in fact wider than that, which is what makes the constraint visible at all.'
            constrainedScroll.getViewport().getWidth() > 250
        and : """
            The scroll behaviour of the content survived the indirection: the view of the
            constrained scroll pane answers the very same questions its content would.
        """
            UI.runAndGet( () -> constrainedScroll.getViewport().getView().getScrollableTracksViewportWidth()  ) == true
            UI.runAndGet( () -> constrainedScroll.getViewport().getView().getScrollableTracksViewportHeight() ) == false
            UI.runAndGet( () -> constrainedScroll.getViewport().getView().getScrollableUnitIncrement(null, SwingConstants.VERTICAL, 1) ) == 10
            UI.runAndGet( () -> constrainedScroll.getViewport().getView().getScrollableBlockIncrement(null, SwingConstants.VERTICAL, 1) ) == 10
    }

    /**
     *  A layout manager of a fixed opinion which keeps track of how often it was
     *  asked for it, so that a test can tell whether a component was measured or not.
     */
    static class CountingLayout implements java.awt.LayoutManager {
        int measurements = 0
        int width  = 300
        int height = 200
        @Override void addLayoutComponent(String name, java.awt.Component comp) {}
        @Override void removeLayoutComponent(java.awt.Component comp) {}
        @Override Dimension preferredLayoutSize(java.awt.Container parent) { measurements++; return new Dimension(width, height) }
        @Override Dimension minimumLayoutSize(java.awt.Container parent) { return new Dimension(10, 10) }
        @Override void layoutContainer(java.awt.Container parent) {}
    }

    static class CustomScrollablePanel extends JPanel implements javax.swing.Scrollable {
        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return null;
        }
        @Override
        public int getScrollableUnitIncrement(java.awt.Rectangle visibleRect, int orientation, int direction) {
            return 10;
        }
        @Override
        public int getScrollableBlockIncrement(java.awt.Rectangle visibleRect, int orientation, int direction) {
            return 10;
        }
        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }
        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }

}
