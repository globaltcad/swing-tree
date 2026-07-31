package swingtree

import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title
import swingtree.layout.ResponsiveGridFlowLayout
import swingtree.threading.EventProcessor

import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.border.EmptyBorder
import java.awt.Dimension
import java.awt.Font

@Title("Responsive Layout Size Invalidation")
@Narrative('''

    The `ResponsiveGridFlowLayout` must always report the size its container
    wants *now*. This specification pins that down, and it exists because the
    obvious way to speed the layout up is to stop it from being true.

    Measuring is by far the most expensive thing this layout does. A single
    `validate()` of the chat example produced 138 calls to
    `preferredLayoutSizeAtWidth` and more than a thousand `getPreferredSize()`
    calls for the ten containers that were actually laid out, and each of those
    walks down to the leaves, where measuring a label means laying its text out
    through the font engine. Neither AWT nor Swing absorbs any of it:
    `Container.preferredSize()` recomputes whenever the container is invalid,
    which is exactly the state a container is in while it is being validated,
    and `JComponent.getPreferredSize()` asks its ui delegate every single time.

    So remembering the answers is tempting, and clearing them from
    `invalidateLayout` looks like the natural way to keep them honest. It is
    not. `Component.invalidate()` only propagates to a parent which is still
    *valid*, so once a container has been invalidated, a **second** child
    changing size never reaches its layout manager at all. A cache cleared that
    way keeps describing a tree that no longer exists.

    That failure is quiet - nothing throws, the window is simply laid out as if
    its content were still the size it was a moment ago - so it is pinned down
    here instead. The features below all pass today and are meant to fail
    loudly against any cache which outlives a single measurement.

''')
@Subject([ResponsiveGridFlowLayout])
class Responsive_Layout_Size_Invalidation_Spec extends Specification
{
    def setup() {
        SwingTree.get().setEventProcessor(EventProcessor.COUPLED)
        SwingTree.get().setUiScaleFactor(1f)
    }

    def cleanup() {
        SwingTree.clear()
    }

    private static JPanel _panelWithLabel( JLabel label ) {
        return UI.panel().withFlowLayout()
                .apply({ ui -> ui.add(label) })
                .get(JPanel)
    }

    /** A plain fixed size child, so that a feature measures the layout and not a look and feel. */
    private static JPanel _box( int width, int height ) {
        var box = new JPanel()
        box.setPreferredSize(new Dimension(width, height))
        return box
    }

    private static JPanel _panelWithBoxes( int count ) {
        var panel = UI.panel().withFlowLayout().get(JPanel)
        count.times { panel.add(_box(20, 20)) }
        return panel
    }

    def 'A label growing its text grows the preferred size reported by its parent.'()
    {
        reportInfo """
            The requirement in its most direct form. Setting the text of a
            label changes what it wants to be, so the panel above it has to
            report a different width from then on, and not the width of the
            text it measured last time.
        """
        given : 'A panel whose only child is a label with a short text.'
            var label = new JLabel("x")
            var panel = _panelWithLabel(label)
        and : 'We ask for the preferred size once, so that there is something to remember.'
            var before = panel.getLayout().preferredLayoutSize(panel)

        when : 'The label is given a very much longer text,'
            label.setText("a considerably longer piece of text than before")
            var after = panel.getLayout().preferredLayoutSize(panel)

        then : 'the panel reports a wider preferred size than it did before.'
            after.width > before.width
    }

    def 'A label shrinking its text shrinks the preferred size reported by its parent.'()
    {
        reportInfo """
            The other direction of the same requirement. Something which only
            ever grows the reported size would pass the feature above while
            still being wrong.
        """
        given : 'A panel whose only child is a label with a long text.'
            var label = new JLabel("a considerably longer piece of text than before")
            var panel = _panelWithLabel(label)
            var before = panel.getLayout().preferredLayoutSize(panel)

        when : 'The label is given a much shorter text,'
            label.setText("x")
            var after = panel.getLayout().preferredLayoutSize(panel)

        then : 'the panel reports a narrower preferred size than it did before.'
            after.width < before.width
    }

    def 'Adding and removing children is reflected in the preferred size.'()
    {
        given : 'A panel with a single box in it.'
            var panel = _panelWithBoxes(1)
            var withOne = panel.getLayout().preferredLayoutSize(panel)

        when : 'A second box of the same size is added,'
            panel.add(_box(20, 20))
            panel.invalidate()
            var withTwo = panel.getLayout().preferredLayoutSize(panel)
        then : 'the panel grew wider.'
            withTwo.width > withOne.width

        when : 'The second box is taken away again,'
            panel.remove(1)
            panel.invalidate()
            var withOneAgain = panel.getLayout().preferredLayoutSize(panel)
        then : 'the panel is back to the size it reported at the start.'
            withOneAgain == withOne
    }

    def 'A bigger font on a child is reflected in the preferred size of its parent.'()
    {
        reportInfo """
            A font change is the case that makes the whole subject tempting,
            since laying text out is what makes a measurement expensive in the
            first place, and a font change forces it to be laid out again.
        """
        given : 'A panel holding a label in a small font.'
            var label = new JLabel("some text")
            label.setFont(new Font("Dialog", Font.PLAIN, 8))
            var panel = _panelWithLabel(label)
            var small = panel.getLayout().preferredLayoutSize(panel)

        when : 'The very same label is given a much bigger font,'
            label.setFont(new Font("Dialog", Font.PLAIN, 40))
            var big = panel.getLayout().preferredLayoutSize(panel)

        then : 'the panel reports a bigger preferred size.'
            big.width > small.width
            big.height > small.height
    }

    def 'A border added to the container is reflected in the preferred size.'()
    {
        reportInfo """
            The reported size includes the insets of the container, so a change
            of border has to move it just like a change of child does.
        """
        given : 'A panel with a box in it and no border.'
            var panel = _panelWithBoxes(1)
            var without = panel.getLayout().preferredLayoutSize(panel)

        when : 'A generous empty border is put around it,'
            panel.setBorder(new EmptyBorder(4, 6, 8, 10))
            var with = panel.getLayout().preferredLayoutSize(panel)

        then : 'the reported size grew by exactly the insets of that border.'
            with.width  == without.width  + 6 + 10
            with.height == without.height + 4 + 8
    }

    def 'The dimension handed out is the callers own, mutating it does not corrupt the next answer.'()
    {
        reportInfo """
            `Component.getPreferredSize()` hands out a fresh `Dimension` every
            time, and callers are entitled to treat it as theirs. Any layout
            manager which starts holding on to its answers has to keep handing
            out copies, or one caller writing into the dimension it received
            would silently change the size every later caller is told.
        """
        given : 'A panel with a box in it, measured once.'
            var panel = _panelWithBoxes(1)
            var first = panel.getLayout().preferredLayoutSize(panel)
            var expected = new Dimension((int) first.width, (int) first.height)

        when : 'The caller scribbles all over the dimension it was given,'
            first.width  = 12345
            first.height = 67890
            var second = panel.getLayout().preferredLayoutSize(panel)

        then : 'the next caller is told the same thing as the first one was.'
            second == expected
    }

    def 'A layout manager shared between two containers answers about the right one.'()
    {
        reportInfo """
            Sharing a single layout manager between containers is unusual, but
            it is legal, and a manager which remembers measurements must not
            answer about the container it happened to see last.
        """
        given : 'One layout manager, and two panels of different content using it.'
            var shared = new ResponsiveGridFlowLayout()
            var narrow = UI.panel().withLayout(shared).get(JPanel)
            var wide   = UI.panel().withLayout(shared).get(JPanel)
            narrow.add(_box(20, 20))
            wide.add(_box(200, 20))

        when : 'Both are measured, and then measured again in the other order,'
            var narrowFirst = shared.preferredLayoutSize(narrow)
            var wideFirst   = shared.preferredLayoutSize(wide)
            var wideAgain   = shared.preferredLayoutSize(wide)
            var narrowAgain = shared.preferredLayoutSize(narrow)

        then : 'each panel is described by its own content every single time.'
            narrowFirst == narrowAgain
            wideFirst   == wideAgain
            wideFirst.width > narrowFirst.width
    }
}
