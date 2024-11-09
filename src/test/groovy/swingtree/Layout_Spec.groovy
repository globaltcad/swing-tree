package swingtree

import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title
import swingtree.layout.FlowCell
import swingtree.layout.ResponsiveGridFlowLayout
import swingtree.layout.Size
import swingtree.threading.EventProcessor

import javax.swing.JPanel
import java.awt.FlowLayout
import java.awt.Rectangle

@Title("Responsive Layouts")
@Narrative("""

    A basic requirement of any UI framework is the ability to create UIs
    with layouts that adapt to different screen sizes and resolutions.
    SwingTree is primarily based on the `MigLayout` layout manager, which
    is a powerful and flexible layout manager that can be used to create
    all kinds of layouts. 
    
    However, `MigLayout` is based on a rigid grid system, which can make
    it difficult to create responsive layouts that adapt to different
    size constraints. 
    
    To address this limitation, SwingTree provides an alternative layout
    manager called `ResponsiveGridFlowLayout`, that is originally
    based on the `FlowLayout` layout manager, but has been extended to
    support responsive grid cell sizing.
    
    In this specification, we will explore the features of the `ResponsiveGridFlowLayout`
    as part of declarative SwingTree UI code.

""")
@Subject([ResponsiveGridFlowLayout, FlowCell, UIFactoryMethods, UI])
class Layout_Spec extends Specification
{
    def setup() {
        SwingTree.get().setEventProcessor(EventProcessor.COUPLED)
    }

    def cleanup() {
        SwingTree.clear()
    }

    def 'The `withFlowLayout()` method attaches a `ResponsiveGridFlowLayout` to the UI.'(
        float uiScaleFactor
    ) {
        reportInfo """
            The SwingTree UI builder API supports various `withFlowLayout(..)` method
            variants that attach a `ResponsiveGridFlowLayout` to the UI component.
            Note that due to the nature of the `ResponsiveGridFlowLayout` being
            inherently based on the `FlowLayout` layout manager, it is not 
            obvious from the API that the layout manager is actually a
            `ResponsiveGridFlowLayout`.
            
            This is intentional, as the `ResponsiveGridFlowLayout` is designed
            as a replacement for the `FlowLayout` layout manager which also
            supports high DPI scaling of gutter gaps and cell sizes.
        """
        given : 'Before we build the UI, we set the UI scale factor (for high DPI screens):'
            SwingTree.get().setUiScaleFactor(uiScaleFactor)
        and : 'We build a simple panel with a button and a label:'
            var ui =
                      UI.panel().withFlowLayout(UI.HorizontalAlignment.CENTER, 12, 12)
                      .add(
                          UI.button("Random Button")
                      )
                      .add(
                          UI.label("Some text")
                      )
        and : 'Then we construct the actual panel component:'
            var panel = ui.get(JPanel)

        expect: 'The panel has a `ResponsiveGridFlowLayout` layout manager attached:'
            panel.getLayout() instanceof ResponsiveGridFlowLayout
        and : 'The layout manager has the correct horizontal alignment:'
            panel.getLayout().getAlignment() == UI.HorizontalAlignment.CENTER
        and : 'The layout manager has the correct horizontal and vertical gap sizes:'
            panel.getLayout().horizontalGapSize() == Math.round(12 * uiScaleFactor)
            panel.getLayout().verticalGapSize() == Math.round(12 * uiScaleFactor)

        where :
            uiScaleFactor << [1.0f, 1.25f, 1.5f, 2.0f]
    }

    def 'The `ResponsiveGridFlowLayout` lays out its components exactly like the regular `FlowLayout`.'(
        UI.HorizontalAlignment alignment, int horizontalGap, int verticalGap, Size prefSize
    ) {
        reportInfo """
            Without any additional configuration, the `ResponsiveGridFlowLayout` should
            behave exactly like the regular `FlowLayout` layout manager.
            
            In this unit test confirm this functional equivalence by comparing 
            two panels with the same components, one using a `FlowLayout` and the
            other using a `ResponsiveGridFlowLayout`.
        """
        given : 'Two panels with the same components, one using a `FlowLayout` and the other using a `ResponsiveGridFlowLayout`.'
            var ourFlowPanel =
                        UI.panel().withFlowLayout(alignment, horizontalGap, verticalGap)
                        .withPrefSize(prefSize)
                        .add(UI.box().withPrefSize(10, 20))
                        .add(UI.box().withPrefSize(20, 20))
                        .add(UI.box().withPrefSize(30, 20))
                        .add(UI.box().withPrefSize(40, 20))
                        .add(UI.box().withPrefSize(50, 20))
                        .get(JPanel)
            var regularFlowPanel =
                        UI.panel().withLayout(new FlowLayout(alignment.forFlowLayout().orElse(FlowLayout.CENTER), horizontalGap, verticalGap))
                        .withPrefSize(prefSize)
                        .add(UI.box().withPrefSize(10, 20))
                        .add(UI.box().withPrefSize(20, 20))
                        .add(UI.box().withPrefSize(30, 20))
                        .add(UI.box().withPrefSize(40, 20))
                        .add(UI.box().withPrefSize(50, 20))
                        .get(JPanel)
        when : """
            We now trigger the layout managers to do their job, we should see
            that the components are laid out exactly the same way in both panels.
            
            But since this is a unit test, we need to manually trigger the layout
            of the components in both panels, so that we can compare the actual
            positions of the components.
        """
            ourFlowPanel.doLayout()
            regularFlowPanel.doLayout()
        then : 'The components are laid out exactly the same way in both panels.'
            ourFlowPanel.getComponent(0).getBounds() == regularFlowPanel.getComponent(0).getBounds()
            ourFlowPanel.getComponent(1).getBounds() == regularFlowPanel.getComponent(1).getBounds()
            ourFlowPanel.getComponent(2).getBounds() == regularFlowPanel.getComponent(2).getBounds()
            ourFlowPanel.getComponent(3).getBounds() == regularFlowPanel.getComponent(3).getBounds()
            ourFlowPanel.getComponent(4).getBounds() == regularFlowPanel.getComponent(4).getBounds()
        where :
            alignment                      | horizontalGap | verticalGap | prefSize

            UI.HorizontalAlignment.LEFT    | 0             | 0           | Size.of(100, 200)
            UI.HorizontalAlignment.LEFT    | 0             | 5           | Size.of(100, 200)
            UI.HorizontalAlignment.LEFT    | 5             | 0           | Size.of(100, 200)
            UI.HorizontalAlignment.LEFT    | 5             | 5           | Size.of(100, 200)
            UI.HorizontalAlignment.CENTER  | 0             | 0           | Size.of(100, 200)
            UI.HorizontalAlignment.CENTER  | 0             | 5           | Size.of(100, 200)
            UI.HorizontalAlignment.CENTER  | 5             | 0           | Size.of(100, 200)
            UI.HorizontalAlignment.CENTER  | 5             | 5           | Size.of(100, 200)
            UI.HorizontalAlignment.RIGHT   | 0             | 0           | Size.of(100, 200)
            UI.HorizontalAlignment.RIGHT   | 0             | 5           | Size.of(100, 200)
            UI.HorizontalAlignment.RIGHT   | 5             | 0           | Size.of(100, 200)
            UI.HorizontalAlignment.RIGHT   | 5             | 5           | Size.of(100, 200)

            UI.HorizontalAlignment.LEFT    | 0             | 0           | Size.of(200, 100)
            UI.HorizontalAlignment.LEFT    | 0             | 5           | Size.of(200, 100)
            UI.HorizontalAlignment.LEFT    | 5             | 0           | Size.of(200, 100)
            UI.HorizontalAlignment.LEFT    | 5             | 5           | Size.of(200, 100)
            UI.HorizontalAlignment.CENTER  | 0             | 0           | Size.of(200, 100)
            UI.HorizontalAlignment.CENTER  | 0             | 5           | Size.of(200, 100)
            UI.HorizontalAlignment.CENTER  | 5             | 0           | Size.of(200, 100)
            UI.HorizontalAlignment.CENTER  | 5             | 5           | Size.of(200, 100)
            UI.HorizontalAlignment.RIGHT   | 0             | 0           | Size.of(200, 100)
            UI.HorizontalAlignment.RIGHT   | 0             | 5           | Size.of(200, 100)
            UI.HorizontalAlignment.RIGHT   | 5             | 0           | Size.of(200, 100)
            UI.HorizontalAlignment.RIGHT   | 5             | 5           | Size.of(200, 100)
    }

    def 'Use the `UI.AUTO_SPAN(..)` factory method to define responsive component constraints.'()
    {
        reportInfo """
            To make the `ResponsiveGridFlowLayout` layout manager truly responsive,
            you have to define responsive cell span constraints for the components.
            This means that for different sizes of the parent container, the
            components will span different numbers of cells in the grid.
            
            A parent container is considered larger if its width is closer to 
            or larger than its preferred width. Conversely, a parent container
            is considered smaller if its width is getting closer to 0.
            
            In this unit test, we will use the `UI.AUTO_SPAN(..)` factory method
            to define responsive cell span constraints for the components in a panel,
            and then demonstrate how the components span different numbers of cells
            in the grid for different sizes of the parent container.
        """
        given : 'A panel with components that have responsive cell span constraints.'
            var ui =
                          UI.panel("ins 0").withFlowLayout(UI.HorizontalAlignment.CENTER, 10, 20)
                          .withPrefSize(120, 200)
                          .add(UI.AUTO_SPAN({it.small(6).medium(3).large(2)}),
                                UI.box().withPrefHeight(20)
                          )
                          .add(UI.AUTO_SPAN({it.small(6).medium(2).large(2)}),
                              UI.box().withPrefHeight(20)
                          )
                          .add(UI.AUTO_SPAN({it.small(6).medium(4).large(4)}),
                              UI.box().withPrefHeight(20)
                          )
                          .add(UI.AUTO_SPAN({it.small(12).medium(4).large(3)}),
                              UI.box().withPrefHeight(20)
                          )
        and : 'We construct the actual panel component:'
            var panel = ui.get(JPanel)
        when : 'We trigger the layout manager to do its job based on the panel having its full preferred width.'
            panel.setSize(120, 200)
            panel.doLayout()
        then : 'The components span the correct number of cells in the grid.'
            panel.getComponent(0).getBounds() == new Rectangle(14, 20, 11, 20)
            panel.getComponent(1).getBounds() == new Rectangle(35, 20, 11, 20)
            panel.getComponent(2).getBounds() == new Rectangle(56, 20, 23, 20)
            panel.getComponent(3).getBounds() == new Rectangle(89, 20, 17, 20)

        when : """
            We now target the medium size of the parent container, which is
            defined as having a width that is between 2/5 and 3/5 of the
            preferred width of the parent container.
        """
            panel.setSize(60, 200)
            panel.doLayout()
        then : 'The components span the correct number of cells in the grid.'
            panel.getComponent(0).getBounds() == new Rectangle(13, 20, 5, 20)
            panel.getComponent(1).getBounds() == new Rectangle(28, 20, 3, 20)
            panel.getComponent(2).getBounds() == new Rectangle(41, 20, 6, 20)
            panel.getComponent(3).getBounds() == new Rectangle(23, 60, 13, 20)

        when : """
            We now target the small size of the parent container, which is
            defined as having a width that is less than 2/5 of the preferred
            width of the parent container.
        """
            panel.setSize(40, 200)
            panel.doLayout()
        then : 'The components span the correct number of cells in the grid.'
            panel.getComponent(0).getBounds() == new Rectangle(10, 20, 5, 20)
            panel.getComponent(1).getBounds() == new Rectangle(25, 20, 5, 20)
            panel.getComponent(2).getBounds() == new Rectangle(15, 60, 10, 20)
            panel.getComponent(3).getBounds() == new Rectangle(10, 100, 20, 20)
    }

    def 'You can configure how the cell of a responsive flow layout is used vertically.'(
        Size layoutSize, boolean isFill, UI.VerticalAlignment alignInCell, List<List<Integer>> expectedBounds
    ) {
        reportInfo """
            The `ResponsiveGridFlowLayout` layout manager supports vertical alignment
            of components within their cells. The default vertical alignment is
            that they are being centered within their cells and their height is
            determined by the preferred height of the component.
            
            However, you can use the `UI.HorizontalAlignment` enum to configure 
            if the components should stick to the top or bottom of their cells.
            If you want the components to fill the entire height of their cells,
            you can set the `fill` property to `true`.
            
            In this unit test, we will demonstrate how the vertical alignment
            of components within their cells can be configured.
        """
        given : 'A panel with components that have responsive cell span constraints.'
            var ui =
                          UI.panel("ins 0").withFlowLayout(UI.HorizontalAlignment.CENTER, 5, 10)
                          .withPrefSize(120, 200)
                          .add(UI.AUTO_SPAN({it.small(6).medium(3).large(2).fill(isFill).align(alignInCell)}),
                                UI.box().withPrefHeight(10)
                          )
                          .add(UI.AUTO_SPAN({it.small(6).large(2).veryLarge(1).fill(isFill).align(alignInCell)}),
                              UI.box().withPrefHeight(20)
                          )
                          .add(UI.AUTO_SPAN({it.verySmall(8).small(6).large(4).oversize(1).fill(isFill).align(alignInCell)}),
                              UI.box().withPrefHeight(30)
                          )
                          .add(UI.AUTO_SPAN({it.small(12).medium(4).large(3).fill(isFill).align(alignInCell)}),
                              UI.box().withPrefHeight(40)
                          )
        and : 'We construct the actual panel component and unpack the expected bounds:'
            var panel = ui.get(JPanel)
            var bounds1 = new Rectangle(expectedBounds[0][0], expectedBounds[0][1], expectedBounds[0][2], expectedBounds[0][3])
            var bounds2 = new Rectangle(expectedBounds[1][0], expectedBounds[1][1], expectedBounds[1][2], expectedBounds[1][3])
            var bounds3 = new Rectangle(expectedBounds[2][0], expectedBounds[2][1], expectedBounds[2][2], expectedBounds[2][3])
            var bounds4 = new Rectangle(expectedBounds[3][0], expectedBounds[3][1], expectedBounds[3][2], expectedBounds[3][3])

        when : 'We trigger the layout manager to do its job based on the targeted size.'
            panel.setSize(layoutSize.width().map(Number::intValue).orElse(0), layoutSize.height().map(Number::intValue).orElse(0))
            panel.doLayout()
        then : 'The components span the correct number of cells in the grid.'
            panel.getComponent(0).getBounds() == bounds1
            panel.getComponent(1).getBounds() == bounds2
            panel.getComponent(2).getBounds() == bounds3
            panel.getComponent(3).getBounds() == bounds4

        where :
            layoutSize       | isFill | alignInCell                    || expectedBounds

            Size.of(120,200) | false  | UI.VerticalAlignment.UNDEFINED || [[14, 25, 15, 10],[34, 20, 7, 20],[46, 15, 31, 30],[82, 10, 23, 40]]
            Size.of(120,200) | false  | UI.VerticalAlignment.CENTER    || [[14, 25, 15, 10],[34, 20, 7, 20],[46, 15, 31, 30],[82, 10, 23, 40]]
            Size.of(120,200) | false  | UI.VerticalAlignment.TOP       || [[14, 10, 15, 10],[34, 10, 7, 20],[46, 10, 31, 30],[82, 10, 23, 40]]
            Size.of(120,200) | false  | UI.VerticalAlignment.BOTTOM    || [[14, 40, 15, 10],[34, 30, 7, 20],[46, 20, 31, 30],[82, 10, 23, 40]]

            Size.of(120,200) | true   | UI.VerticalAlignment.UNDEFINED || [[14, 10, 15, 40],[34, 10, 7, 40],[46, 10, 31, 40],[82, 10, 23, 40]]
            Size.of(120,200) | true   | UI.VerticalAlignment.CENTER    || [[14, 10, 15, 40],[34, 10, 7, 40],[46, 10, 31, 40],[82, 10, 23, 40]]
            Size.of(120,200) | true   | UI.VerticalAlignment.TOP       || [[14, 10, 15, 40],[34, 10, 7, 40],[46, 10, 31, 40],[82, 10, 23, 40]]
            Size.of(120,200) | true   | UI.VerticalAlignment.BOTTOM    || [[14, 10, 15, 40],[34, 10, 7, 40],[46, 10, 31, 40],[82, 10, 23, 40]]
    }

}
