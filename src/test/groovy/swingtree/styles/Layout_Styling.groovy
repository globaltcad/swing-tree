package swingtree.styles

import net.miginfocom.layout.CC
import net.miginfocom.swing.MigLayout
import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Title
import swingtree.UI
import swingtree.api.Layout

import javax.swing.JPanel
import java.awt.*

@Title("Layout Styling")
@Narrative('''

    SwingTree allows you to define the layout of your components 
    through the styling API, which is a functional DSL
    that not only allows you to define how a component should be painted,
    like for example the background color, the font, the border widths, etc.
    but also allows you to define the miglayout constraints for the component.
    
    The examples defined in this specification will demonstrate how to use the styling API
    to define the layout of your components.

''')
class Layout_Styling extends Specification
{
    def 'The x and y alignment values of a component can be configured through the styling API'()
    {
        reportInfo """
            The x, and y alignment values are float values that can be used to align a component
            within its parent container. The x alignment value is used to align the component
            horizontally, and the y alignment value is used to align the component vertically.
            
            Note that not all layout managers support the alignment values, and that the alignment
            values are only used when the component is smaller than the parent container.
        """
        given :
            var ui =
                    UI.panel("fill")
                    .add(
                        UI.button().withStyle( it -> it
                            .alignmentX(0.27f)
                            .alignmentY(0.73f)
                        )
                    )
        and : 'We build the root component of the SwingTree declaration and unpack the first sub-component:'
            var panel = ui.get(JPanel)
            var button = panel.getComponent(0)

        expect : 'The alignment values of the button are set to the values we specified in the styling API:'
            button.alignmentX == 0.27f
            button.alignmentY == 0.73f

        and : """
            Although the vanilla MigLayout does not support the alignment values,
            the SwingTree layout will inform the MigLayout of the alignment values.
            So we can check and see that the MigLayout has 
            the alignment values encoded into the component constraints of the button:
        """
            ((CC)((MigLayout)panel.layout).constraintMap[button]).horizontal.getAlign().value == 27f
            ((CC)((MigLayout)panel.layout).constraintMap[button]).vertical.getAlign().value == 73f
    }

    def 'Use the style API to configure the MigLayout manager for your components.'()
    {
        reportInfo """
            The style API allows you to configure MigLayout managers for your components
            by exposing various methods for specifying layout constraints
            in a way that is more readable than the vanilla MigLayout API.
        """
        given :
            var ui =
                    UI.panel("fill")
                    .withStyle( it -> it
                        .layout("flowy, insets 10 20 30 40", "[grow, fill] 10 [grow, fill]", "[shrink] 12 [shrink]")
                    )
                    .add(
                        UI.button().withStyle( it -> it
                            .addConstraint("grow, span 2")
                        )
                    )
        and : 'We unpack the Swing tree:'
            var panel = ui.get(JPanel)
            var button = panel.getComponent(0)

        expect : 'The layout manager of the panel is a MigLayout manager:'
            panel.layout instanceof MigLayout

        and : 'The layout manager of the panel has the layout constraints we specified in the styling API:'
            panel.layout.layoutConstraints == "flowy, insets 10 20 30 40"
            panel.layout.columnConstraints == "[grow, fill] 10 [grow, fill]"
            panel.layout.rowConstraints == "[shrink] 12 [shrink]"

        and : 'Finally, the button has the layout constraints we specified in the styling API:'
            ((MigLayout)panel.layout).constraintMap[button] == "grow, span 2"
    }

    def 'The style Allows you to configure the flow layout as layout manager for components.'()
    {
        reportInfo """
            The style API allows you to configure the flow layout as layout manager for components.
        """
        given :
            var ui =
                    UI.panel("fill")
                    .withStyle( it -> it
                        .layout(Layout.flow())
                    )
                    .add(
                        UI.toggleButton().withStyle( it -> it
                            .alignmentX(0.33f)
                            .alignmentY(0.66f)
                        )
                    )
        and : 'We unpack the Swing tree:'
            var panel = ui.get(JPanel)
            var button = panel.getComponent(0)

        expect : 'The layout manager of the panel is a FlowLayout manager:'
            panel.layout instanceof FlowLayout

        and : 'The button has the alignment values we specified in the styling API:'
            button.alignmentX == 0.33f
            button.alignmentY == 0.66f
    }

    def 'Define a border layout for a parent component and the corresponding constraints for their child components through the style API.'()
    {
        reportInfo """
            The style API allows you to define a border layout for a parent component
            as well as the corresponding constraints for their child components.
            So for example in case of a component having a `BorderLayout`, 
            the constraints for the child components may be `BorderLayout.NORTH`, `BorderLayout.SOUTH`, etc.
            and they can all be specified through the style API. 
        """
        given :
            var ui =
                    UI.panel("fill")
                    .withStyle( it -> it
                        .layout(Layout.border())
                    )
                    .add(
                        UI.slider(UI.Align.VERTICAL).withStyle( it -> it
                            .addConstraint(BorderLayout.WEST)
                        )
                    )
                    .add(
                        UI.textArea("Some content").withStyle( it -> it
                            .addConstraint(BorderLayout.CENTER)
                        )
                    )
                    .add(
                        UI.button("Click me").withStyle( it -> it
                            .addConstraint(BorderLayout.SOUTH)
                        )
                    )
        and : 'We unpack the Swing tree:'
            var panel = ui.get(JPanel)
            var slider = panel.getComponent(0)
            var textArea = panel.getComponent(1)
            var button = panel.getComponent(2)
            var layout = panel.layout

        expect : 'The layout manager of the panel is a BorderLayout manager:'
            layout instanceof BorderLayout

        and : 'The box layout manager will associate the constraints we specified in the styling API with the child components:'
            ((BorderLayout)layout).getLayoutComponent(BorderLayout.WEST) == slider
            ((BorderLayout)layout).getLayoutComponent(BorderLayout.CENTER) == textArea
            ((BorderLayout)layout).getLayoutComponent(BorderLayout.SOUTH) == button
    }

    def 'If you do not want a component to be managed by a layout manager, you can set the layout manager to `Layout.none()`.'()
    {
        reportInfo """
            If you do not want a component to be managed by a layout manager, you can set the layout manager to
            the `Layout.none()` value, which is a `Layout` implementation that 
            installs a `null` layout manager on the component.
        """
        given :
            var ui =
                    UI.panel("fill")
                    .withStyle( it -> it
                        .layout(Layout.none())
                    )
                    .add(
                        UI.button()
                    )
        and : 'We unpack the Swing tree:'
            var panel = ui.get(JPanel)

        expect : 'The layout manager of the panel is null:'
            panel.layout == null
    }

    def 'Configure the `ComponentOrientation` of a component, through the style API.'()
    {
        reportInfo """
            The style API allows you to configure the `ComponentOrientation` of a component.
            We do this by passing one of the `UI.ComponentOrientation` enum constants to the `orientation` method.
            These constants directly map to the `java.awt.ComponentOrientation` constants
            of the same name.
        """
        given :
            var ui =
                    UI.panel("fill, wrap 1")
                    .add(
                        UI.menuItem("Right to left").withStyle( it -> it
                            .orientation(UI.ComponentOrientation.RIGHT_TO_LEFT)
                        )
                    )
                    .add(
                        UI.menuItem("Left to right").withStyle( it -> it
                            .orientation(UI.ComponentOrientation.LEFT_TO_RIGHT)
                        )
                    )
                    .add(
                        UI.menuItem("Unknown").withStyle( it -> it
                            .orientation(UI.ComponentOrientation.UNKNOWN)
                        )
                    )
                    .add(
                        UI.menuItem("Default")
                    )

        and : 'We unpack the Swing tree:'
            var panel = ui.get(JPanel)
            var menuItem1 = panel.getComponent(0)
            var menuItem2 = panel.getComponent(1)
            var menuItem3 = panel.getComponent(2)
            var menuItem4 = panel.getComponent(3)

        expect : 'The component orientation of the menu items is set to the values we specified in the styling API:'
            menuItem1.componentOrientation == ComponentOrientation.RIGHT_TO_LEFT
            menuItem2.componentOrientation == ComponentOrientation.LEFT_TO_RIGHT
            menuItem3.componentOrientation == ComponentOrientation.UNKNOWN
            menuItem4.componentOrientation == ComponentOrientation.UNKNOWN
    }
}
