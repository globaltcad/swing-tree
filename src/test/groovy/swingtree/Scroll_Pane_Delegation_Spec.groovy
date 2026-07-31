package swingtree

import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title
import swingtree.threading.EventProcessor
import utility.SwingTreeTestConfigurator
import utility.Utility

import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JScrollPane
import java.awt.Dimension
import java.beans.PropertyChangeListener

@Title("The Box a Scroll Pane puts around your Content")
@Narrative('''

    A viewport holds exactly one view and has no layout manager to interpret a constraint
    with, and the `Scrollable` interface can only be implemented by inheriting from it. So
    when you configure a scroll pane declaratively, SwingTree slips a thin box between the
    viewport and your content: the box carries the layout manager, and it answers the
    viewport's `Scrollable` questions from your configurator.

    The box is meant to be invisible in every other respect. Whatever your content says
    about how small, how large and how big it would like to be, the box says the same, and
    asking it any of those three questions must leave the other two exactly as they were.

    That last sentence is the one worth writing tests for, and it is not as easy to test as
    it sounds. Each of the three size methods of the box answers from the content but also
    writes the answer back into itself, because several places in SwingTree and in the look
    and feels ask a component whether a size was explicitly set on it. A mistake in one of
    those write-backs is invisible to a test that only reads the answers - the answer comes
    from the content either way - and shows up only as a component that keeps changing
    underneath everyone listening to it. This specification therefore watches what the box
    does to itself, not just what it says.

''')
@Subject([UIForScrollPane, JScrollPane])
class Scroll_Pane_Delegation_Spec extends Specification
{
    def setupSpec() {
        SwingTree.initializeUsing(SwingTreeTestConfigurator.get())
        SwingTree.get().setEventProcessor(EventProcessor.COUPLED_STRICT)
    }

    def 'Asking the box for one size never changes another.'()
    {
        reportInfo """
            The three size questions are independent of each other, so answering one of them
            has no business touching the answer to the others. Note that we cannot check this
            by simply reading the other two back, because the box answers all three from the
            content and would go on doing so even if it had corrupted its own record. What
            gives it away is the component telling everyone that a size changed, so that is
            what we listen for.
        """
        given : 'A scroll pane with some content in it, and the box SwingTree put in between.'
            var view = boxOf(UI.panel("fill").withMinSize(40, 20).withMaxSize(400, 300))
        and : 'A record of every size the box announces as changed.'
            var announced = announcementsOf(view)

        when : 'We ask it one of the three questions, a few times over.'
            UI.runNow({ 3.times { question.call(view) } })

        then : 'The only size it can possibly have changed is the one we asked about.'
            announced.every { it == expectedProperty }

        where : 'We ask each of the three questions in turn.'
            expectedProperty | question
            "minimumSize"    | { JComponent v -> v.getMinimumSize()   }
            "maximumSize"    | { JComponent v -> v.getMaximumSize()   }
            "preferredSize"  | { JComponent v -> v.getPreferredSize() }
    }

    def 'The box settles down: asking it the same things again changes nothing.'()
    {
        reportInfo """
            Every write-back the box performs is a `PropertyChangeEvent`, and a component
            that fires those forever is a component that keeps waking up whatever listens to
            it. So the box has to reach a fixed point: once it has recorded what its content
            says, asking it again has to be silent.

            This is the shape a mix-up between two of the sizes takes. Writing one size into
            another's field makes the two disagree by construction, so each question undoes
            what the previous one recorded and the box never stops announcing changes - even
            though every single answer it gives is correct.
        """
        given : 'A scroll pane with some content in it, and the box SwingTree put in between.'
            var view = boxOf(UI.panel("fill").withMinSize(40, 20).withMaxSize(400, 300))
            var announced = announcementsOf(view)

        when : 'We ask the box all three questions, over and over, the way a layout pass does.'
            UI.runNow({
                20.times { view.getMinimumSize(); view.getMaximumSize(); view.getPreferredSize() }
            })

        then : """
            It announced a change at most once per size - the first time it recorded what its
            content said - and then went quiet.
        """
            announced.count("minimumSize")   <= 1
            announced.count("maximumSize")   <= 1
            announced.count("preferredSize") <= 1
    }

    def 'The box reports exactly the sizes of the content it wraps.'()
    {
        reportInfo """
            And of course the answers themselves have to be the content's answers. This is
            the part that is easy to test, and it is here so that the two tests above cannot
            be satisfied by a box that has simply stopped answering.
        """
        given : 'A content component with a mind of its own about how it wants to be sized.'
            var content = UI.panel("fill").withMinSize(40, 20).withMaxSize(400, 300)
            var view = boxOf(content)
            var contentComponent = new Utility.Query(view).find(JPanel, "content").orElseThrow(NoSuchElementException::new)

        expect : 'The box says exactly what the content says.'
            UI.runAndGet({ view.getMinimumSize() }) == UI.runAndGet({ contentComponent.getMinimumSize() })
            UI.runAndGet({ view.getMaximumSize() }) == UI.runAndGet({ contentComponent.getMaximumSize() })
            UI.runAndGet({ view.getPreferredSize() }) == UI.runAndGet({ contentComponent.getPreferredSize() })

        when : 'The content changes its mind,'
            UI.runNow({ contentComponent.setMinimumSize(new Dimension(123, 45)) })

        then : 'the box changes its mind with it.'
            UI.runAndGet({ view.getMinimumSize() }) == new Dimension(123, 45)
    }

    // ────────────────────────────────────────────────────────────────────────

    /** Builds a configured scroll pane around the given content and returns the box in between. */
    private static JComponent boxOf( UIForAnySwing<?,?> content ) {
        var scrollPane = UI.scrollPane(conf -> conf.fitWidth(true) )
                           .add(content.id("content"))
                           .get(JScrollPane)
        return UI.runAndGet({ (JComponent) scrollPane.getViewport().getView() })
    }

    /** Records the name of every size the given component announces as changed. */
    private static List<String> announcementsOf( JComponent component ) {
        List<String> announced = []
        UI.runNow({
            component.addPropertyChangeListener({ event ->
                if ( event.propertyName in ["minimumSize", "maximumSize", "preferredSize"] )
                    announced.add(event.propertyName)
            } as PropertyChangeListener)
        })
        return announced
    }
}
