package swingtree.styles

import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Title
import sprouts.Var
import swingtree.UI
import swingtree.animation.LifeTime

import javax.swing.JButton
import javax.swing.JFormattedTextField
import javax.swing.JMenuItem
import javax.swing.JPanel
import javax.swing.JSlider
import javax.swing.JTextField
import javax.swing.JToggleButton
import java.util.concurrent.TimeUnit

@Title("Opaque or not Opaque")
@Narrative('''

    This specification focuses on the opaqueness of Swing components and
    how the SwingTree style engine relates to it.
    The opaqueness flag of a component is an interesting property.
    You might think that it merely controls whether the background of
    the component is painted or not. 
    
    But it is actually more than that.
    
    This flag is at the center of some important rendering optimizations in Swing.
    If a component is reporting itself as opaque, then Swing assumes that a repaint
    only needs to be done on that component and not on any of its ancestors.
    This is a big deal because it means that Swing can avoid repainting a lot of
    components in the hierarchy.
    
    This sound relatively straightforward, but it becomes a lot more complicated together
    with the SwingTree style API and its underlying styling engine.
    So for example, if a component has rounded corners, and no foundational background
    color, then it is technically not opaque.
    Leaving it opaque will cause strange rendering artifacts, due to the parent component
    not being repainted.
    
    In this specification we will explore the different scenarios and how the SwingTree
    style engine deals with them.

''')
class Opaqueness_Styles_Spec extends Specification
{
    def 'A component styled to have round corners will no longer be opaque.'()
    {
        reportInfo """
 
            Rounded corners are a problem with respect to opaqueness
            because the component is not painted in its entirety.
            Every Swing component is at its core a rectangle,
            and the rounded corners are in a way just cut off from the rectangle.
            This leaves the area outside of the rounded corners unpainted
            and it puts the responsibility on the parent component to paint
            that area.
            
            Therefore, a vanilla component with rounded corners is not opaque.

        """
        given :
            var ui =
                    UI.textField()
                    .withStyle(it -> it
                        .borderRadius(16)
                    )

        and :
            var textField = ui.get(JTextField)

        expect :
            textField.isOpaque() == false
    }

    def 'A component styled to have round corners together with a foundation color will stay opaque.'()
    {
        reportInfo """
 
            The foundation color is a fill color that is used to fill the exterior of the component,
            which is the area surrounding the border.
            It is especially visible when the component has a margin and or a border radius.
            If the foundation color is opaque, then the component will be opaque as well
            because the entire area of the component is painted.

        """
        given :
            var ui =
                    UI.textField()
                    .withStyle(it -> it
                        .borderRadius(16)
                        .foundationColor("blue")
                    )

        and :
            var textField = ui.get(JTextField)

        expect :
            textField.isOpaque() == true
    }


    def 'A component styled to have a positive margin together with a foundation color will stay opaque.'()
    {
        reportInfo """
 
            The foundation color is a fill color that is used to fill the exterior of the component,
            which is the area surrounding the border.
            It is especially visible when the component has a margin and or a border radius.
            If the foundation color is opaque, then the component will be opaque as well
            because the entire area of the component is painted.

        """
        given :
            var ui =
                    UI.toggleButton()
                    .withStyle(it -> it
                        .margin(16)
                        .foundationColor("blue")
                    )

        and :
            var toggleButton = ui.get(JToggleButton)

        expect :
            toggleButton.isOpaque() == true
    }

    def 'A component styled to have a positive margin together with a foundation color and a border radius will stay opaque.'()
    {
        reportInfo """
 
            The foundation color is a fill color that is used to fill the exterior of the component,
            which is the area surrounding the border.
            It is especially visible when the component has a margin and or a border radius.
            If the foundation color is opaque, then the component will be opaque as well
            because the entire area of the component is painted.

        """
        given :
            var ui =
                    UI.panel()
                    .withStyle(it -> it
                        .margin(16)
                        .borderRadius(16)
                        .foundationColor("blue")
                    )

        and :
            var panel = ui.get(JPanel)

        expect :
            panel.isOpaque() == true
    }

    def 'A component with a transparent border color will not be opaque.'()
    {
        reportInfo """
 
            SwingTree renders the border by filling the area between the component exterior
            and its interior. There is nothing behind the border, so the component is not opaque.
            Note that the background color of the component is not relevant here because
            the background is only painted in the component interior.

        """
        given :
            var ui =
                    UI.button()
                    .withStyle(it -> it
                        .border(3, new java.awt.Color(20, 230, 200, 140))
                    )

        and :
            var button = ui.get(JButton)

        expect :
            button.isOpaque() == false
    }

    def 'A component with a transparent border color and opaque a foundation and background colors will not be opaque.'()
    {
        reportInfo """
 
            SwingTree renders the border by filling the area between the component exterior
            and its interior. There is nothing behind the border, so the component is not opaque.
            Neither the background color of the component nor the foundation color are relevant here
            because the background is only painted in the component interior and the foundation color
            is only painted in the component exterior.

        """
        given :
            var ui =
                    UI.menuItem()
                    .withStyle(it -> it
                        .border(3, new java.awt.Color(20, 230, 200, 140))
                        .foundationColor("blue")
                        .backgroundColor("red")
                    )

        and :
            var menuItem = ui.get(JMenuItem)

        expect :
            menuItem.isOpaque() == false
    }

    def 'If a component has a transparent background color, then it will not be opaque.'()
    {
        reportInfo """
 
            If a component has a transparent background color then it will not be opaque.

        """
        given :
            var ui =
                    UI.slider(UI.Align.HORIZONTAL)
                    .withStyle(it -> it
                        .backgroundColor(new java.awt.Color(40, 210, 220, 100))
                    )

        and :
            var slider = ui.get(JSlider)

        expect :
            slider.isOpaque() == false
    }


    def 'If a component has an opaque background color, then it will also be opaque.'()
    {
        reportInfo """
 
            If a component has an opaque background color, then it will also be opaque.

        """
        given :
            var ui =
                    UI.formattedTextField()
                    .withStyle(it -> it
                        .backgroundColor(new java.awt.Color(40, 210, 220))
                    )

        and :
            var formattedTextField = ui.get(JFormattedTextField)

        expect :
            formattedTextField.isOpaque() == true
    }

    def 'A component styled to have transitionally round corners will only temporarily be non opaque.'()
    {
        reportInfo """
 
            Rounded corners are a problem with respect to opaqueness
            because the component is not painted in its entirety.
            Every Swing component is at its core a rectangle,
            and the rounded corners are in a way just cut off from the rectangle.
            This leaves the area outside of the rounded corners unpainted
            and it puts the responsibility on the parent component to paint
            that area.
            
            Therefore, a vanilla component with rounded corners is not opaque.
            But this is only true for the time the component actually has rounded corners.
            If the component is transitioning to have no rounded corners, then it will
            be opaque again.

        """
        given :
            var isOn = Var.of(false)
        and : 'The component is styled to have temporarily rounded corners:'
            var ui =
                    UI.textField()
                    .withTransitionalStyle(isOn, LifeTime.of(1, TimeUnit.MILLISECONDS), (state, it) -> it
                        .borderRadius(16 * state.progress())
                    )

        and : 'We build the underlying Swing component:'
            var textField = ui.get(JTextField)

        expect : 'The component is opaque because the `isOn` flag is false:'
            textField.isOpaque() == true

        when : 'We set the `isOn` flag to true:'
            isOn.set(true)
        and : 'We wait for the transition to complete:'
            Thread.sleep(50)
            UI.sync()

        then : 'The component is not opaque because the `isOn` flag is true:'
            textField.isOpaque() == false

        //when :
        //    isOn.set(false)
        //and :
        //    Thread.sleep(50)
        //    UI.sync()
        //then :
        //    textField.isOpaque() == true
    }
}

