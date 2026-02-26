package swingtree

import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Title
import sprouts.Var
import swingtree.animation.LifeTime
import swingtree.api.Styler
import swingtree.components.JBox
import swingtree.style.ComponentExtension
import swingtree.style.StyleConf

import javax.swing.*
import java.awt.*
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
    def setup() {
        // We reset to the default look and feel:
        UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName())
        // This is to make sure that the tests are not influenced by
        // other look and feels that might be used in the example code...
    }

    def cleanup() {
        UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName())
    }

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
        given : 'A text field UI declaration styled to have round corners:'
            var ui =
                    UI.textField()
                    .withStyle(it -> it
                        .borderRadius(16)
                    )

        and : 'We build the underlying Swing text field:'
            var textField = ui.get(JTextField)

        expect : """
                The component is not opaque because it has rounded corners
                exposing the parent component behind it.
        """
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
        given : 'A text field UI declaration styled to have round corners and a foundation color:'
            var ui =
                    UI.textField()
                    .withStyle(it -> it
                        .borderRadius(16)
                        .foundationColor("blue")
                    )

        and : 'We then build the underlying text field:'
            var textField = ui.get(JTextField)

        expect : """
                The component is opaque because the foundation color, which fills
                the area created by the rounded corners, is opaque.
        """
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
        given : 'A toggle button with a positive margin and a foundation color:'
            var ui =
                    UI.toggleButton()
                    .withStyle(it -> it
                        .margin(16)
                        .foundationColor("blue")
                    )

        and : 'We then build the underlying Swing component:'
            var toggleButton = ui.get(JToggleButton)

        expect : """
                The component is opaque because the foundation color, which fills
                the area created by the margin, is opaque.
        """
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
        given : 'A panel UI declaration with a positive margin, a foundation color and a border radius:'
            var ui =
                    UI.panel()
                    .withStyle(it -> it
                        .margin(16)
                        .borderRadius(16)
                        .foundationColor("blue")
                    )

        and : 'We then build the underlying panel component:'
            var panel = ui.get(JPanel)

        expect : """
                The component is flagged as opaque because the foundation color, which fills
                the exterior component area created by the margin and the border radius, is opaque too.
        """
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
        given : 'We create a simple button UI declaration with a transparent border color:'
            var ui =
                    UI.button()
                    .withStyle(it -> it
                        .border(3, new java.awt.Color(20, 230, 200, 140))
                    )

        and : 'We then build the underlying button component:'
            var button = ui.get(JButton)

        expect : """
                The component is not opaque because the border color is transparent.
                And there is nothing behind the border, which means that the parent component
                will be visible behind the border.
        """
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
        given : """
                We create a simple button UI declaration with a transparent border color,
                an opaque foundation color and an opaque background color:
        """
            var ui =
                    UI.menuItem()
                    .withStyle(it -> it
                        .border(3, new java.awt.Color(20, 230, 200, 140))
                        .foundationColor("blue")
                        .backgroundColor("red")
                    )

        and : 'We then build the underlying button component:'
            var menuItem = ui.get(JMenuItem)

        expect : """
                The component is not opaque because the border color is transparent.
                And there is nothing behind the border, which means that the parent component
                will be visible behind the border.
                The foundation color and the background color are not relevant here
                because they are only painted in the component exterior and interior
                (which does not overlap with the border area).
        """
            menuItem.isOpaque() == false
    }

    def 'If a component has a transparent background color, then it will not be opaque.'()
    {
        reportInfo """
 
            If a component has a transparent background color then it will not be opaque.
            
        """
        given : 'We create a slider UI declaration with a transparent background color:'
            var ui =
                    UI.slider(UI.Align.HORIZONTAL)
                    .withStyle(it -> it
                        .backgroundColor(new java.awt.Color(40, 210, 220, 100))
                    )

        and : 'We then build the underlying slider component:'
            var slider = ui.get(JSlider)

        expect : """
                The component is not opaque because the background color is transparent.
                And there is nothing behind the background, which means that the parent component
                will be visible behind the background.
        """
            slider.isOpaque() == false
    }


    def 'If a component has an opaque background color, then it will also be opaque.'()
    {
        reportInfo """
 
            If a component has an opaque background color, then it will also be opaque.

        """
        given : 'We define a slider UI declaration with an opaque background color:'
            var ui =
                    UI.formattedTextField()
                    .withStyle(it -> it
                        .backgroundColor(new java.awt.Color(40, 210, 220))
                    )

        and : 'We then build the underlying slider component:'
            var formattedTextField = ui.get(JFormattedTextField)

        expect : """
                The component is opaque because the background color is opaque.
                So the parent component will not be visible behind the background.
        """
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
        given : 'We first define a flag property that we will use to control the transition:'
            var isOn = Var.of(false)
        and : 'Then we create the text field UI declaration, which is styled to have temporarily rounded corners:'
            var ui =
                    UI.textField()
                    .withTransitionalStyle(isOn, LifeTime.of(1, TimeUnit.MILLISECONDS), (state, it) -> it
                        .borderRadius(16 * state.progress())
                    )

        and : 'We build the underlying Swing component:'
            var textField = ui.get(JTextField)

        expect : """
            The component is opaque because the `isOn` flag is false, which translates to a progress of 0,
            causing the border radius to be 0 as well (16 * 0 = 0).
        """
            textField.isOpaque() == true

        when : 'We set the `isOn` flag to true so that the transition starts:'
            isOn.set(true)
        and : 'We wait for the transition to complete:'
            Thread.sleep(50)
            UI.sync()

        then : """
            The component is not opaque because the `isOn` flag is true, which translates to a progress of 1
            (or greater than 0 depending on the time elapsed since the transition started),
            causing the border radius to be 16 as well (16 * 1 = 16).
        """
            textField.isOpaque() == false

        when : """
            We now want to go back to the initial state, so we set the `isOn` flag to false again...
        """
            isOn.set(false)
        and : '...again we wait for the transition to complete...'
            Thread.sleep(50)
            UI.sync()
        then : """
            The component is opaque again because the `isOn` flag is false, which translates to a progress of 0,
            causing the border radius to be 0 as well (16 * 0 = 0).
            Just like in the beginning.
        """
            textField.isOpaque() == true
    }

    def 'A component styled to have transitionally round corners together with a foundation color will always stay opaque.'()
    {
        reportInfo """
 
            The foundation color is a fill color that is used to fill the exterior of the component,
            which is the area surrounding the border.
            It is visible when the component has a margin and or a border radius.
            If the foundation color is opaque, then the component will be opaque as well
            because the entire area of the component is painted.
            
            In this test we will see that the component will stay opaque even if it transitions
            into having rounded corners. This is because in this test
            the foundation color is opaque.

        """
        given : 'We first define a flag property that we will use to control the transition:'
            var isOn = Var.of(false)
        and : 'Then we create the text field UI declaration, which is styled to have temporarily rounded corners:'
            var ui =
                    UI.textField()
                    .withTransitionalStyle(isOn, LifeTime.of(1, TimeUnit.MILLISECONDS), (state, it) -> it
                        .borderRadius(16 * state.progress())
                        .foundationColor("blue")
                    )

        and : 'We build the underlying Swing component:'
            var textField = ui.get(JTextField)

        expect : """
            The component has to be opaque because the component has no rounded 
            corners and the foundation color is opaque.
        """
            textField.isOpaque() == true

        when : 'We set the `isOn` flag to true:'
            isOn.set(true)
        and : 'We wait for the transition to complete:'
            Thread.sleep(50)
            UI.sync()

        then : """
            The component now has a border radius of 16, because the `isOn` flag is true, which translates to a progress of 1
            and consequently a border radius of 16 (16 * 1 = 16).
            But despite the rounded corners, the component is still opaque because the foundation color, which fills
            the area created by the rounded corners, is opaque.
            So the parent component will not be visible behind the border.
        """
            textField.isOpaque() == true

        when : """
            We now want to go back to the initial state, so we set the `isOn` flag to false again...
        """
            isOn.set(false)
        and : '...again we wait for the transition to complete...'
            Thread.sleep(50)
            UI.sync()
        then : """
            Again, the component has no rounded corners and the foundation color is opaque.
            So the component is opaque too.
        """
            textField.isOpaque() == true
    }

    def 'A component styled to have a transitionally positive margin together with a foundation color will stay opaque.'()
    {
        reportInfo """
 
            The foundation color is a fill color that is used to fill the exterior of the component,
            which is the area surrounding the border.
            It is visible when the component has a margin and or a border radius.
            If the foundation color is opaque, then the component will be opaque as well
            because the entire area of the component is painted.
            
            This test demonstrates that the component will stay opaque even if it transitions
            into having a positive margin. This is because in the following scenario
            the foundation color is opaque.

        """
        given : 'We first define a flag property that we will use to control the transition:'
            var isOn = Var.of(false)
        and : 'Then we create the toggle button UI declaration, which is styled to have temporarily a positive margin:'
            var ui =
                    UI.toggleButton()
                    .withTransitionalStyle(isOn, LifeTime.of(1, TimeUnit.MILLISECONDS), (state, it) -> it
                        .margin(16 * state.progress())
                        .foundationColor("blue")
                    )

        and : 'We build the underlying Swing component:'
            var toggleButton = ui.get(JToggleButton)

        expect : """
            The component has to be opaque because the component has no margin and the foundation color is opaque.
        """
            toggleButton.isOpaque() == true

        when : 'We set the `isOn` flag to true:'
            isOn.set(true)
        and : 'We wait for the transition to complete:'
            Thread.sleep(50)
            UI.sync()

        then : """
            The component now has a margin of 16, because the `isOn` flag is true, which translates to a progress of 1
            and consequently a margin of 16 (16 * 1 = 16).
            But despite the margin, the component is still opaque because the foundation color, which fills
            the area created by the margin, is opaque.
            So the parent component will not be visible behind the border.
        """
            toggleButton.isOpaque() == true

        when : """
            We now want to go back to the initial state, so we set the `isOn` flag to false again...
        """
            isOn.set(false)
        and : '...again we wait for the transition to complete...'
            Thread.sleep(50)
            UI.sync()
        then : """
            Again, as expected, the component has no margin and the foundation color is opaque.
            So the component is opaque too.
        """
            toggleButton.isOpaque() == true
    }

    def 'A component styled to have a transitionally positive margin together with a foundation color and a border radius will stay opaque.'()
    {
        reportInfo """
 
            The foundation color is a fill color that is used to fill the exterior of the component,
            which is the area surrounding the border. It is visible when the component has a 
            margin and or a border radius.
            If the foundation color is opaque, then the component will be opaque as well
            because the entire area of the component is painted.
            
            The scenario below demonstrates that the component will stay opaque even if it transitions
            into having a positive margin and rounded corners. 
            The reason is that in this scenario the foundation color is opaque.
            An opaque foundation color will fill the area created by the margin and the rounded corners
            leaving no area unpainted and no part of the parent component visible on the full component area.

        """
        given : 'We first define a flag property that we will use to control the transition:'
            var isOn = Var.of(false)
        and : 'Then we create the panel UI declaration, which is styled to have temporarily a positive margin and rounded corners:'
            var ui =
                    UI.panel()
                    .withTransitionalStyle(isOn, LifeTime.of(1, TimeUnit.MILLISECONDS), (state, it) -> it
                        .margin(16 * state.progress())
                        .borderRadius(16 * state.progress())
                        .foundationColor("blue")
                    )

        and : 'We build the underlying Swing component:'
            var panel = ui.get(JPanel)

        expect : """
            The component has to be opaque because the component has no margin and the foundation color is opaque.
        """
            panel.isOpaque() == true

        when : 'We set the `isOn` flag to true in order to start the transition:'
            isOn.set(true)
        and : 'We wait for the transition to complete:'
            Thread.sleep(50)
            UI.sync()

        then : """
            The component now has a margin of 16 and a border radius of 16, because the `isOn` flag is true, which translates to a progress of 1
            and consequently a margin of 16 (16 * 1 = 16).
            But despite the margin and the rounded corners, the component is still opaque because the foundation color, which fills
            the area created by the margin and the rounded corners, is opaque.
            So the parent component will not be visible behind the border.
        """
            panel.isOpaque() == true

        when : """
            We now want to go back to the initial state, so we set the `isOn` flag to false again...
        """
            isOn.set(false)
        and : '...again we wait for the transition to complete...'
            Thread.sleep(50)
            UI.sync()
        then : """
            Again, as expected, the component has no margin and the foundation color is opaque.
            So the component is opaque too.
        """
            panel.isOpaque() == true
    }

    def 'A component with a border color having a transitional alpha chanel is only opaque when the color is opaque'()
    {
        reportInfo """
 
            SwingTree renders the border by filling the area between the component exterior
            and its interior. There is nothing behind the border, so the component is not opaque.
            Note that the background color of the component is not relevant here because
            the background is only painted in the component interior.

        """
        given : 'We first define a flag property that we will use to control the transition:'
            var isOn = Var.of(false)
        and : 'Then we create a simple button UI declaration with a transparent border color:'
            var ui =
                    UI.button()
                    .withTransitionalStyle(isOn, LifeTime.of(1, TimeUnit.MILLISECONDS), (state, it) -> it
                        .border(3, new java.awt.Color(20, 230, 200, (int)(255 * state.progress())))
                    )

        and : 'We build the underlying button component:'
            var button = ui.get(JButton)

        expect : """
                Initially the component is not opaque because the border color is transparent
                due to the `isOn` flag being false, which translates to a progress of 0,
                causing the border color to be transparent as well (255 * 0 = 0).
                Also, there is nothing behind the border, which means that the parent component
                will be visible behind the border.
        """
            button.isOpaque() == false

        when : 'We set the `isOn` flag to true so that the transition starts:'
            isOn.set(true)
        and : 'We wait for the transition to complete:'
            Thread.sleep(50)
            UI.sync()

        then : """
            The component is now opaque because the `isOn` flag is true, which translates to 
            an animation progress transitioning to 1,
            causing the border color to be opaque as well (255 * 1 = 255).
        """
            button.isOpaque() == true

        when : """
            We now want to go back to the initial state, so we set the `isOn` flag to false again...
        """
            isOn.set(false)
        and : '...again we wait for the transition to complete...'
            Thread.sleep(50)
            UI.sync()
        then : """
            We are back to the initial state, so the component is not opaque again,
            because the border color is transparent again.
        """
            button.isOpaque() == false
    }

    def 'A component with a temporarily transparent border color, opaque a foundation and background colors will not be opaque.'()
    {
        reportInfo """
 
            SwingTree renders the border by filling the area between the component exterior
            and its interior. There is nothing behind the border, so the component is not opaque.
            Neither the background color of the component nor the foundation color are relevant here
            because the background is only painted in the component interior and the foundation color
            is only painted in the component exterior.
            
            Here you can see that the component will become opaque when the border color becomes opaque.
            
        """
        given : 'We first define a flag property that we will use to control the transition:'
            var isOn = Var.of(false)
        and : 'We create a simple menu item UI declaration with a transparent border color, an opaque foundation color and an opaque background color:'
            var ui =
                    UI.menuItem()
                    .withTransitionalStyle(isOn, LifeTime.of(1, TimeUnit.MILLISECONDS), (state, it) -> it
                        .border(3, new java.awt.Color(20, 230, 200, (int)(255 * state.progress())))
                        .foundationColor("blue")
                        .backgroundColor("red")
                    )

        and : 'We then build the underlying menu item:'
            var menuItem = ui.get(JMenuItem)

        expect : """
                Initially the component is not opaque because the border color is transparent
                due to the `isOn` flag being false, which translates to a progress of 0,
                causing the border color to be transparent as well (255 * 0 = 0).
                Also, there is nothing behind the border, which means that the parent component
                will be visible behind the border.
        """
            menuItem.isOpaque() == false

        when : 'We set the `isOn` flag to true causing the transition to start:'
            isOn.set(true)
        and : 'We wait for the transition to complete:'
            Thread.sleep(50)
            UI.sync()

        then : """
            The component is now opaque because the `isOn` flag is true, which translates to 
            an animation progress transitioning to 1,
            causing the border color to be opaque as well (255 * 1 = 255).
        """
            menuItem.isOpaque() == true

        when : """
            We now want to go back to the initial state, so we set the `isOn` flag to false again...
        """
            isOn.set(false)
        and : '...again we wait for the transition to complete...'
            Thread.sleep(50)
            UI.sync()
        then : """
            We are back to the initial state where the component is no longer opaque
            due to the border color being transparent again.
            
            Note that the foundation color and the background color are not relevant here
            because they are only painted in the component exterior and interior
            (which does not overlap with the border area).
        """
            menuItem.isOpaque() == false
    }

    def 'An otherwise opaque panel with a temporarily positive margin will be non opaque for that time.'()
    {
        reportInfo """
 
            The margins of a component create additional space around the component.
            If the component has a foundation color, then the foundation color will fill
            the area created by the margin.
            If however, there is no foundation color, then this will just be additional
            empty space around the component, which exposes the parent component behind it.
            
            Here you can see that a panel, which is initially opaque, will become non opaque
            when it transitions into having a positive margin.

        """
        given : 'We first define a boolean flag property that we will use to control the transition:'
            var isOn = Var.of(false)
        and : 'Then we create the panel based UI declaration, which is styled to temporarily have a positive margin:'
            var ui =
                    UI.panel()
                    .withTransitionalStyle(isOn, LifeTime.of(1, TimeUnit.MILLISECONDS), (state, it) -> it
                        .margin(10 * state.progress())
                    )

        and : 'We build the underlying `JPanel`:'
            var panel = ui.get(JPanel)

        expect : """
            The component has to be opaque because the component has no margin.
            So nowhere on the component area will the parent component be visible.
        """
            panel.isOpaque() == true

        when : 'We set the `isOn` flag to true in order to start the transition:'
            isOn.set(true)
        and : 'We wait for the transition to complete:'
            Thread.sleep(50)
            UI.sync()

        then : """
            The component now has a margin of 10, because the `isOn` flag is true (which translates to a progress of 1
            and consequently a margin of 10 (10 * 1 = 10)).
            This causes the component to be non opaque because the parent component will be visible
            in the area created by the margin.
        """
            panel.isOpaque() == false

        when : """
            We now want to go back to the initial state, so we set the `isOn` flag to false again...
        """
            isOn.set(false)
        and : '...again we wait for the transition to complete...'
            Thread.sleep(50)
            UI.sync()
        then : """
            As expected, the component has no margin meaning that the 
            component completely covers the parent component, which ultimately 
            causes the component to be opaque again.
        """
            panel.isOpaque() == true
    }

    def 'A label will become opaque when transitioning to an opaque background color.'()
    {
        reportInfo """
 
            A label is a component that is not opaque by default.
            It is only opaque when it has an opaque background color.
            This test demonstrates that a label will become opaque when transitioning
            to an opaque background color.

        """
        given : 'We first define a boolean flag property that we will use to control the transition:'
            var isOn = Var.of(false)
        and : 'Then we create the label based UI declaration, which is styled to temporarily have an opaque background color:'
            var ui =
                    UI.label("Hello World")
                    .withTransitionalStyle(isOn, LifeTime.of(1, TimeUnit.MILLISECONDS), (state, it) -> it
                        .backgroundColor(new java.awt.Color(40, 210, 220, (int)(255 * state.progress())))
                    )

        and : 'We build the underlying `JLabel`:'
            var label = ui.get(javax.swing.JLabel)

        expect : """
            The component has to be non opaque because the background color is transparent.
            So the parent component will be visible behind the background.
        """
            label.isOpaque() == false

        when : 'We set the `isOn` flag to true in order to start the transition:'
            isOn.set(true)
        and : 'We wait for the transition to complete:'
            Thread.sleep(50)
            UI.sync()

        then : """
            The label is now opaque because the `isOn` flag is true, which translates to 
            an animation progress transitioning to 1,
            causing the background color to be opaque (255 * 1 = 255).
        """
            label.isOpaque() == true

        when : """
            We now want to go back to the initial state, so we set the `isOn` flag to false again...
        """
            isOn.set(false)
        and : '...again we wait for the transition to complete...'
            Thread.sleep(50)
            UI.sync()
        then : """
            We are back to the initial state where the label is no longer opaque
            due to the background color being transparent again.
        """
            label.isOpaque() == false
    }

    def 'A slider (which typically transparent) will become opaque when transitioning to an opaque background color.'()
    {
        reportInfo """
 
            A slider is a component that is not opaque by default.
            It is only opaque when it has an opaque background color.
            This test demonstrates that a slider will become opaque when transitioning
            to an opaque background color.

        """
        given : 'We first define a boolean flag property that we will use to control the transition:'
            var isOn = Var.of(false)
        and : 'Then we create the slider based UI declaration, which is styled to temporarily have an opaque background color:'
            var ui =
                    UI.slider(UI.Align.HORIZONTAL)
                    .withTransitionalStyle(isOn, LifeTime.of(1, TimeUnit.MILLISECONDS), (state, it) -> it
                        .backgroundColor(new java.awt.Color(40, 210, 220, (int)(255 * state.progress())))
                    )

        and : 'We build the underlying `JSlider`:'
            var slider = ui.get(javax.swing.JSlider)

        expect : """
            The component has to be non opaque because the background color is transparent.
            So the parent component will be visible behind the background.
        """
            slider.isOpaque() == false
        and : 'This is also reflected in the background color of the slider:'
            slider.getBackground().getAlpha() == 0

        when : 'We set the `isOn` flag to true in order to start the transition:'
            isOn.set(true)
        and : 'We wait for the transition to complete:'
            Thread.sleep(50)
            UI.sync()

        then : """
            The slider is now opaque because the `isOn` flag is true, which translates to 
            an animation progress transitioning to 1,
            causing the background color to be opaque (255 * 1 = 255).
        """
            slider.isOpaque() == true

        when : """
            We now want to go back to the initial state, so we set the `isOn` flag to false again...
        """
            isOn.set(false)
        and : '...again we wait for the transition to complete...'
            Thread.sleep(50)
            UI.sync()
        then : """
            We are back to the initial state where the slider is no longer opaque
            due to the background color being transparent again.
        """
            slider.isOpaque() == false
    }


    def 'A slider (which typically opaque) will become non-opaque when transitioning to a transparent background color.'()
    {
        reportInfo """
 
            A slider is a component that is not opaque by default.
            It is only opaque when it has an opaque background color.
            This test demonstrates that a slider will become non opaque when transitioning
            to a transparent background color.

        """
        given : 'We first define a boolean flag property that we will use to control the transition:'
            var isOn = Var.of(false)
        and : 'Then we create the slider based UI declaration, which is styled to either have an undefined or transparent background color:'
            var ui =
                    UI.slider(UI.Align.HORIZONTAL)
                    .withTransitionalStyle(isOn, LifeTime.of(1, TimeUnit.MILLISECONDS), (state, it) -> it
                        .backgroundColor(
                            state.progress() == 0
                                ? UI.Color.UNDEFINED
                                : new Color(0,0,0,0)
                        )
                    )

        and : 'We build the underlying `JSlider`:'
            var slider = ui.get(javax.swing.JSlider)

        expect : """
            The component has to be opaque because the "undefined" background color 
            is conceptually equivalent to "no color specified" and therefore 
            causes the slider to have its default background color, which is opaque.
        """
            slider.isOpaque() == true
        and : 'Due to the usage of `UI.Color.UNDEFINED`, the background color of the slider is undefined too:'
            slider.getBackground() == null

        when : 'We set the `isOn` flag to true in order to start the transition:'
            isOn.set(true)
        and : 'We wait for the transition to complete:'
            Thread.sleep(50)
            UI.sync()

        then : """
            The slider is now opaque because the `isOn` flag is true, which translates to 
            an animation progress transitioning to 1,
            causing the background color to be opaque (255 * 1 = 255).
        """
            slider.isOpaque() == false
        and : 'Now the background color of the slider is transparent:'
            slider.getBackground().getAlpha() == 0

        when : """
            We now want to go back to the initial state, so we set the `isOn` flag to false again...
        """
            isOn.set(false)
        and : '...again we wait for the transition to complete...'
            Thread.sleep(50)
            UI.sync()
        then : """
            We are back to the initial state where the slider is opaque again
            due to the background color being undefined (which causes the slider to have its default background color).
        """
            slider.isOpaque() == true
        and : 'Due to the usage of `UI.Color.UNDEFINED`, the background color of the slider is undefined too:'
            slider.getBackground() == null
    }

    def 'A toggle button (which typically opaque) will become non-opaque when transitioning to a transparent background color.'()
    {
        reportInfo """
            A toggle button is a component that is opaque by default as it has a default background
            color. When it receives a transparent background color, than it will no longer be opaque however.
            This test demonstrates that it will become non opaque when transitioning
            to a transparent background color.

        """
        given : 'We first define a boolean flag property that we will use to control the transition:'
            var isOn = Var.of(false)
        and : 'Then we create the button based UI declaration, which is styled to either have an undefined or transparent background color:'
            var ui =
                    UI.toggleButton("Toggle Me!")
                    .withTransitionalStyle(isOn, LifeTime.of(1, TimeUnit.MILLISECONDS), (state, it) -> it
                        .backgroundColor(
                            state.progress() == 0
                                ? UI.Color.UNDEFINED
                                : new Color(0,0,0,0)
                        )
                    )

        and : 'We build the underlying `JToggleButton`:'
            var toggleButton = ui.get(javax.swing.JToggleButton)

        expect : """
            The component has to be opaque because the "undefined" background color 
            is conceptually equivalent to "no color specified" and therefore 
            causes the slider to have its default background color, which is opaque.
        """
            toggleButton.isOpaque() == true
        and : """
            Due to the usage of `UI.Color.UNDEFINED`, the background color of the button is now undefined
            in the sense tha it has the default background.
        """
            !toggleButton.isBackgroundSet()
            toggleButton.getBackground() == null

        when : 'We set the `isOn` flag to true in order to start the transition:'
            isOn.set(true)
        and : 'We wait for the transition to complete:'
            Thread.sleep(50)
            UI.sync()

        then : """
            The button is now non opaque because the `isOn` flag is true, which translates to 
            an animation progress transitioning to 1,
            causing the background color to have a transparent alpha channel of 0.
        """
            toggleButton.isOpaque() == false
        and : 'We confirm, the background color of the button is transparent:'
            toggleButton.getBackground().getAlpha() == 0

        when : """
            We now want to go back to the initial state, so we set the `isOn` flag to false again...
        """
            isOn.set(false)
        and : '...again we wait for the transition to complete...'
            Thread.sleep(50)
            UI.sync()
        then : """
            We are back to the initial state where the button is opaque again
            due to the background color being undefined (which causes the slider to have its default background color).
        """
            toggleButton.isOpaque() == true
        and : """
            Again, due to the usage of `UI.Color.UNDEFINED`, the background color of the button 
            is back to its original default background color.
        """
            !toggleButton.isBackgroundSet()
            toggleButton.getBackground() == null
    }

    def 'A check box (which typically opaque) may become non-opaque when transitioning to various styles.'(
        boolean opaque, int margin, int radius, int border, String borderColor, String foundation, String background, String[] gradient
    ) {
        reportInfo """
 
            A check box is a component that is opaque by default.
            
            This test demonstrates that a check box will become non-opaque when transitioning
            to various style configurations

        """
        given : 'We first define a boolean flag property that we will use to control the transition:'
            var isOn = Var.of(false)
        and : 'Then we create the check box based UI declaration, which is styled to temporarily styled:'
            var ui =
                    UI.checkBox("Checked?")
                    .withTransitionalStyle(isOn, LifeTime.of(1, TimeUnit.MILLISECONDS), (state, it) -> it
                        .margin( state.progress() == 1 ? margin : 0 )
                        .borderRadius( state.progress() == 1 ? radius : 0 )
                        .backgroundColor( state.progress() == 1 ? background : "" )
                        .foundationColor( state.progress() == 1 ? foundation : "" )
                        .borderColor( state.progress() == 1 ? borderColor : "" )
                        .borderWidth( state.progress() == 1 ? border : 0 )
                        .gradient(g -> g.colors( state.progress() == 1 ? gradient : new String[0]))
                    )

        and : 'We build the underlying check box:'
            var checkBox = ui.get(JCheckBox)

        expect : """
            The component has to be opaque because it was not yet styled and it is also opaque by default.
            So the parent component will not be visible behind the background.
        """
            checkBox.isOpaque() == true

        when : 'We set the `isOn` flag to true in order to start the transition:'
            isOn.set(true)
        and : 'We wait for the transition to complete:'
            Thread.sleep(50)
            UI.sync()

        then : """
            The check box has the expected opaqueness because the `isOn` flag is true, which translates to 
            an animation progress transitioning to 1,
            causing the various styles to take effect!
        """
            checkBox.isOpaque() == opaque

        when : """
            We now want to go back to the initial state, so we set the `isOn` flag to false again...
        """
            isOn.set(false)
        and : '...again we wait for the transition to complete...'
            Thread.sleep(50)
            UI.sync()
        then : """
            We are back to the initial state where the check box is now opaque again
        """
            checkBox.isOpaque() == true

        where :
            opaque  | margin | radius | border |     borderColor    |     foundation     |     background     | gradient
            false   | 0      | 0      | 0      |   "rgba(0,0,0,0)"  |   "rgba(0,0,0,0)"  |   "rgba(0,0,0,0)"  | []

            true    | 0      | 0      | 0      |   "rgba(0,0,0,0)"  |   "rgba(0,0,0,0)"  |   "rgb(0,0,0)"     | []

            false   | 1      | 0      | 0      |   "rgba(0,0,0,0)"  |   "rgba(0,0,0,0)"  |   "rgb(0,0,0)"     | []
            false   | 0      | 1      | 0      |   "rgba(0,0,0,0)"  |   "rgba(0,0,0,0)"  |   "rgb(0,0,0)"     | []
            false   | 0      | 0      | 1      |   "rgba(0,0,0,0)"  |   "rgba(0,0,0,0)"  |   "rgb(0,0,0)"     | []

            true    | 1      | 0      | 0      |   "rgba(0,0,0,0)"  |   "rgb(0,0,0)"     |   "rgb(0,0,0)"     | []
            true    | 0      | 1      | 0      |   "rgba(0,0,0,0)"  |   "rgb(0,0,0)"     |   "rgb(0,0,0)"     | []
            false   | 0      | 0      | 1      |   "rgba(0,0,0,0)"  |   "rgba(0,0,0,0)"  |   "rgb(0,0,0)"     | []

            true    | 0      | 0      | 1      |   "rgb(0,0,0)"     |   "rgba(0,0,0,0)"  |   "rgb(0,0,0)"     | []

            false   | 1      | 0      | 0      |   "rgba(0,0,0,0)"  |   "rgba(0,0,0,0)"  |   "rgb(0,0,0)"     | ["red", "green"]
            false   | 0      | 1      | 0      |   "rgba(0,0,0,0)"  |   "rgba(0,0,0,0)"  |   "rgb(0,0,0)"     | ["red", "green"]
            false   | 0      | 0      | 1      |   "rgba(0,0,0,0)"  |   "rgba(0,0,0,0)"  |   "rgb(0,0,0)"     | ["red", "green"]

            true    | 0      | 0      | 0      |   "rgba(0,0,0,0)"  |   "rgba(0,0,0,0)"  |   "rgba(0,0,0, 0)" | ["red", "green"]

            false   | 0      | 0      | 0      |   "rgba(0,0,0,0)"  |   "rgba(0,0,0,0)"  |   "rgba(0,0,0, 0)" | ["red", "rgba(0,0,0,0)"]
            false   | 0      | 0      | 0      |   "rgba(0,0,0,0)"  |   "rgba(0,0,0,0)"  |   "rgba(0,0,0, 0)" | ["rgba(0,0,0,0)", "green"]
    }

    def 'A `JTextPane` (which typically opaque) may become non-opaque when transitioning to various styles.'(
        boolean opaque, Styler<?> styler
    ) {
        reportInfo """
 
            A text pane is a component that is opaque by default.
            This test demonstrates that it may or may not change its opaqueness
            depending on what kind of styles are applied to it.

        """
        given : 'We first define a boolean flag property that we will use to control the transition:'
            var isOn = Var.of(false)
        and : 'Then we create the text pane based UI declaration, which is temporarily styled:'
            var ui =
                    UI.textPane().withText("Important text...")
                    .withTransitionalStyle(isOn, LifeTime.of(1, TimeUnit.MILLISECONDS), (state, it) ->
                        state.progress() == 1 ? styler.style(it) : it
                    )

        and : 'We build the underlying text pane:'
            var textPane = ui.get(JTextPane)

        expect : """
            The component has to be opaque because it was not yet styled and it is also opaque by default.
            So the parent component will not be visible behind the background.
        """
            textPane.isOpaque() == true
            textPane.getBackground() == new JTextPane().getBackground()

        when : 'We set the `isOn` flag to true in order to start the transition:'
            isOn.set(true)
        and : 'We wait for the transition to complete:'
            Thread.sleep(50)
            UI.sync()

        then : """
            The text pane has the expected opaqueness because the `isOn` flag is true, which translates to 
            an animation progress transitioning to 1,
            causing the various styles to take effect!
        """
            textPane.isOpaque() == opaque

        when : """
            We now want to go back to the initial state, so we set the `isOn` flag to false again...
        """
            isOn.set(false)
        and : '...again we wait for the transition to complete...'
            Thread.sleep(50)
            UI.sync()
        then : """
            We are back to the initial state where the text pane is now opaque again
        """
            textPane.isOpaque() == true
            textPane.getBackground() == new JTextPane().getBackground()

        where :
            opaque | styler
            true   | {it}
            true   | {it.backgroundColor("red")}
            false  | {it.backgroundColor("transparent red")}
            true   | {it.backgroundColor(UI.color(255,255,255, 255))}
            false  | {it.backgroundColor(UI.color(255,255,255, 254))}
            false  | {it.backgroundColor(UI.Color.TRANSPARENT)}
            true   | {it.backgroundColor(UI.Color.TRANSPARENT).gradient(g -> g.colors("red", "green"))}
            false  | {it.backgroundColor(UI.Color.TRANSPARENT).gradient(g -> g.colors("red", "transparent green"))}
            true   | {it.backgroundColor(UI.Color.TRANSPARENT).gradient(g -> g.colors("red", "green")).border(1, "blue")}
            false  | {it.backgroundColor(UI.Color.TRANSPARENT).gradient(g -> g.colors("red", "green")).border(1, "transparent blue")}
    }

    def 'A `JComboBox` (which is typically opaque) may become non-opaque when transitioning to various styles.'(
        boolean opaque, Styler<?> styler
    ) {
        reportInfo """
 
            A simple combo box is a component that is opaque by default,
            but when you style it through the style API then it may
            become non-opaque/transparent to accommodate that style.
            
            When reverting the style, the openness should revert to the
            initial opaqueness.
            This test demonstrates that it may or may not change its opaqueness
            depending on what kind of styles are applied to it.

        """
        given : 'We first define a boolean flag property that we will use to control the transition:'
            var isOn = Var.of(false)
        and : 'Then we create the combo box based UI declaration, which is temporarily styled:'
            var ui =
                    UI.comboBox()
                    .withTransitionalStyle(isOn, LifeTime.of(1, TimeUnit.MILLISECONDS), (state, it) ->
                        state.progress() == 1 ? styler.style(it) : it
                    )

        and : 'We then build the combo box:'
            var comboBox = ui.get(JComboBox)

        expect : """
            The component has to be opaque initially because it was not yet styled and it is also opaque by default.
            This flag tells us that the parent component will not be visible behind the background.
        """
            comboBox.isOpaque() == true
            comboBox.getBackground() == new JComboBox().getBackground()

        when : 'We set the `isOn` flag to true in order to start the transition:'
            isOn.set(true)
        and : 'We wait for the transition to complete:'
            Thread.sleep(50)
            UI.sync()

        then : """
            The combo box has the expected opaqueness because the `isOn` flag is true, which translates to 
            an animation progress transitioning to 1,
            causing the various styles to take effect!
            
            We check if the combo box has the correct opaqueness:
        """
            comboBox.isOpaque() == opaque

        when : """
            We now go back to the initial state, so we set the `isOn` flag to false again...
        """
            isOn.set(false)
        and : '...again we wait for the transition to complete...'
            Thread.sleep(50)
            UI.sync()
        then : """
            We are back to the initial state where the text pane is now opaque again
        """
            comboBox.isOpaque() == true
            comboBox.getBackground() == new JComboBox().getBackground()

        where :
            opaque | styler
            true   | {it}
            true   | {it.backgroundColor("red")}
            false  | {it.backgroundColor("transparent red")}
            true   | {it.backgroundColor(UI.color(255,255,255, 255))}
            false  | {it.backgroundColor(UI.color(255,255,255, 254))}
            false  | {it.backgroundColor(UI.Color.TRANSPARENT)}
            true   | {it.backgroundColor(UI.Color.TRANSPARENT).gradient(g -> g.colors("red", "green"))}
            false  | {it.backgroundColor(UI.Color.TRANSPARENT).gradient(g -> g.colors("red", "transparent green"))}
            true   | {it.backgroundColor(UI.Color.TRANSPARENT).gradient(g -> g.colors("red", "green")).border(1, "blue")}
            false  | {it.backgroundColor(UI.Color.TRANSPARENT).gradient(g -> g.colors("red", "green")).border(1, "transparent blue")}
            true   | {it.painter(UI.Layer.BACKGROUND, UI.ComponentArea.ALL, g2d -> {})}
            true   | {it.shadowColor(Color.BLACK).shadowBlurRadius(6)}
    }

    def 'A plain button will be opaque, even if it has a custom painter.'(
        Styler<JButton> style
    ) {
        reportInfo """
 
            A plain button is a component that is opaque by default,
            but when you call the `makePlain()` method on the button, it will become non-opaque.

        """
        given : 'We declare a plain button with a custom painter:'
            var button =
                    UI.button("Hello World")
                    .makePlain()
                    .withStyle( style )
                    .get(JButton)
        expect : """
            The component has to be opaque because the button is not plain.
        """
            !button.isOpaque()
        and : 'It has a transparent background color!'
            button.getBackground() === UI.Color.TRANSPARENT

        where :
            style << [
                        { it -> it.painter(UI.Layer.BORDER, g2d -> { }) },
                        { it -> it.painter(UI.Layer.BORDER, g2d -> { }).padding(3) },
                        { it -> it.painter(UI.Layer.BORDER, g2d -> { }).margin(3) },
                        { it -> it.painter(UI.Layer.BACKGROUND, g2d -> { }) },
                        { it -> it.painter(UI.Layer.BACKGROUND, g2d -> { }).padding(3) },
                        { it -> it.painter(UI.Layer.BACKGROUND, g2d -> { }).margin(3) },
                    ]
    }

    def 'A `JBox` may or may not be opaque, depending on its style.'(
        boolean opaque, Styler<JBox> styler
    ) {
        reportInfo """
 
            A `JBox` is a component that is npn-opaque by default.
            This test demonstrates that it may or may not change its opaqueness
            depending on what kind of styles are applied to it.

        """
        given : 'We first define a boolean flag that we will use to control the style:'
            var isOn = false
        and : 'Then we create the box based UI declaration, which is temporarily styled:'
            var ui =
                    UI.box().withSize(100, 100)
                    .withStyle({ isOn ? styler(it) : it })
        and : 'We build the underlying box:'
            var box = ui.get(JBox)
        expect : 'A plain box is transparent by default:'
            !box.isOpaque()
        when : 'We set the `isOn` flag to true and then refresh the UI:'
            isOn = true
            UI.runNow(()->{
                ComponentExtension.from(box).gatherApplyAndInstallStyle(true)
            })
        then : 'The box has the expected opaqueness:'
            box.isOpaque() == opaque
        when : 'We set the `isOn` flag to false and then refresh the UI:'
            isOn = false
            UI.runNow(()->{
                ComponentExtension.from(box).gatherApplyAndInstallStyle(true)
            })
        then : 'The box has the expected opaqueness:'
            !box.isOpaque()
        where :
            opaque | styler
            false  | {it}
            true   | {it.backgroundColor("red")}
            false  | {it.backgroundColor("transparent red")}
            true   | {it.backgroundColor(UI.color(255,255,255, 255))}
            true   | {it.backgroundColor("blue").foundationColor("red")}
            false  | {it.backgroundColor("blue").foundationColor("red").border(2, "transparent oak")}
            true   | {it.backgroundColor("rgb(220,220,220)").foundationColor("light oak").shadowIsInset(true).shadowColor("black").shadowBlurRadius(3).borderRadius(24).margin(16).padding(16)}
            false  | {it.foundationColor("light oak").shadowIsInset(true).shadowColor("black").shadowBlurRadius(3).borderRadius(24).margin(16).padding(16)}
            false  | {it.foundationColor("transparent light oak").shadowIsInset(true).shadowColor("black").shadowBlurRadius(3).borderRadius(24).margin(16).padding(16)}
            true   | {it.backgroundColor("rgb(220,220,220)").foundationColor("light oak").shadowIsInset(true).shadowColor("black").shadowBlurRadius(3).margin(16).padding(16)}
            false  | {it.foundationColor("light oak").shadowIsInset(true).shadowColor("black").shadowBlurRadius(3).margin(16).padding(16)}
            false  | {it.shadowColor(Color.BLACK).shadowBlurRadius(6)}
    }

    def 'A `JLabel` may or may not be opaque, depending on its style.'(
        boolean opaque, Styler<JBox> styler
    ) {
        reportInfo """
 
            A `JLabel` is a component that is non-opaque by default.
            This test demonstrates that it may or may not change its opaqueness
            depending on what kind of styles are applied to it.

        """
        given : 'We first define a boolean flag that we will use to control the style:'
            var isOn = false
        and : 'Then we create the label based UI declaration, which is temporarily styled:'
            var ui =
                    UI.label("Hi!").withSize(100, 100)
                    .withStyle({ isOn ? styler(it) : it })
        and : 'We build the underlying label:'
            var label = ui.get(JLabel)
        expect : 'A plain label is transparent (not opaque) by default:'
            !label.isOpaque()
        when : 'We set the `isOn` flag to true and then refresh the UI:'
            isOn = true
            UI.runNow(()->{
                ComponentExtension.from(label).gatherApplyAndInstallStyle(true)
            })
        then : 'The label has the expected opaqueness:'
            label.isOpaque() == opaque
        when : 'We set the `isOn` flag to false and then refresh the UI:'
            isOn = false
            UI.runNow(()->{
                ComponentExtension.from(label).gatherApplyAndInstallStyle(true)
            })
        then : 'The label has the expected opaqueness:'
            !label.isOpaque()
        where :
            opaque | styler
            false  | {it}
            true   | {it.backgroundColor("red")}
            false  | {it.backgroundColor("transparent red")}
            true   | {it.backgroundColor(UI.color(255,255,255, 255))}
            true   | {it.backgroundColor("blue").foundationColor("red")}
            false  | {it.backgroundColor("blue").foundationColor("red").border(2, "transparent oak")}
            true   | {it.backgroundColor("rgb(220,220,220)").foundationColor("light oak").shadowIsInset(true).shadowColor("black").shadowBlurRadius(3).borderRadius(24).margin(16).padding(16)}
            false  | {it.foundationColor("light oak").shadowIsInset(true).shadowColor("black").shadowBlurRadius(3).borderRadius(24).margin(16).padding(16)}
            false  | {it.foundationColor("transparent light oak").shadowIsInset(true).shadowColor("black").shadowBlurRadius(3).borderRadius(24).margin(16).padding(16)}
            true   | {it.backgroundColor("rgb(220,220,220)").foundationColor("light oak").shadowIsInset(true).shadowColor("black").shadowBlurRadius(3).margin(16).padding(16)}
            false  | {it.foundationColor("light oak").shadowIsInset(true).shadowColor("black").shadowBlurRadius(3).margin(16).padding(16)}
            false  | {it.shadowColor(Color.BLACK).shadowBlurRadius(6)}
    }

    def 'In Nimbus, a `JCheckBoxMenuItem` is initially transparent, but may or may not be opaque, depending on its style.'(
        boolean opaque, Styler<JBox> styler
    ) {
        reportInfo """
 
            In Nimbus, a `JCheckBoxMenuItem` is a component that is non-opaque by default.
            This test demonstrates that it may or may not change its opaqueness
            depending on what kind of styles are applied to it.

        """
        given :
            UI.runNow(()->{
                for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                    if ("Nimbus" == info.getName()) {
                        UIManager.setLookAndFeel(info.getClassName());
                    }
                }
            })
            UI.sync()
        and : 'We first define a boolean flag that we will use to control the style:'
            var isOn = false
        and : 'Then we create the menu item based UI declaration, which is temporarily styled:'
            var checkBoxMenuItem = UI.runAndGet(()->{
                UI.checkBoxMenuItem("Check me out!").withSize(200, 30)
                .withStyle({ isOn ? styler(it) : it })
                .get(JCheckBoxMenuItem)
            })
        expect : 'A plain menu item is transparent (not opaque) by default:'
            !checkBoxMenuItem.isOpaque()
        when : 'We set the `isOn` flag to true and then refresh the UI:'
            isOn = true
            UI.runNow(()->{
                ComponentExtension.from(checkBoxMenuItem).gatherApplyAndInstallStyle(true)
            })
        then : 'The menu item has the expected opaqueness:'
            checkBoxMenuItem.isOpaque() == opaque
        when : 'We set the `isOn` flag to false and then refresh the UI:'
            isOn = false
            UI.runNow(()->{
                ComponentExtension.from(checkBoxMenuItem).gatherApplyAndInstallStyle(true)
            })
        then : 'The menu item has the expected opaqueness:'
            !checkBoxMenuItem.isOpaque()
        where :
            opaque | styler
            false  | {it}
            true   | {it.backgroundColor("red")}
            false  | {it.backgroundColor("transparent red")}
            true   | {it.backgroundColor(UI.color(255,255,255, 255))}
            true   | {it.backgroundColor("blue").foundationColor("red")}
            false  | {it.backgroundColor("blue").foundationColor("red").border(2, "transparent oak")}
            true   | {it.backgroundColor("rgb(220,220,220)").foundationColor("light oak").shadowIsInset(true).shadowColor("black").shadowBlurRadius(3).borderRadius(24).margin(16).padding(16)}
            false  | {it.foundationColor("light oak").shadowIsInset(true).shadowColor("black").shadowBlurRadius(3).borderRadius(24).margin(16).padding(16)}
            false  | {it.foundationColor("transparent light oak").shadowIsInset(true).shadowColor("black").shadowBlurRadius(3).borderRadius(24).margin(16).padding(16)}
            true   | {it.backgroundColor("rgb(220,220,220)").foundationColor("light oak").shadowIsInset(true).shadowColor("black").shadowBlurRadius(3).margin(16).padding(16)}
            false  | {it.foundationColor("light oak").shadowIsInset(true).shadowColor("black").shadowBlurRadius(3).margin(16).padding(16)}
            false  | {it.shadowColor(Color.BLACK).shadowBlurRadius(6)}
    }

    def 'In many LaFs, a `JCheckBoxMenuItem` is initially opaque, but may or may not be non-opaque, depending on its style.'(
        boolean opaque, Styler<JBox> styler
    ) {
        reportInfo """
 
            A `JCheckBoxMenuItem` is a component that is opaque by default.
            This test demonstrates that it may or may not change its opaqueness
            depending on what kind of styles are applied to it.

        """
        given : 'We first define a boolean flag that we will use to control the style:'
            var isOn = false
        and : 'Then we create the menu item based UI declaration, which is temporarily styled:'
            var checkBoxMenuItem = UI.runAndGet(()->{
                UI.checkBoxMenuItem("Check me out!").withSize(200, 30)
                .withStyle({ isOn ? styler(it) : it })
                .get(JCheckBoxMenuItem)
            })
        expect : 'A plain menu item is opaque (not transparent) by default:'
            checkBoxMenuItem.isOpaque()
        when : 'We set the `isOn` flag to true and then refresh the UI:'
            isOn = true
            UI.runNow(()->{
                ComponentExtension.from(checkBoxMenuItem).gatherApplyAndInstallStyle(true)
            })
        then : 'The menu item has the expected opaqueness:'
            checkBoxMenuItem.isOpaque() == opaque
        when : 'We set the `isOn` flag to false and then refresh the UI:'
            isOn = false
            UI.runNow(()->{
                ComponentExtension.from(checkBoxMenuItem).gatherApplyAndInstallStyle(true)
            })
        then : 'The menu item is opaque again, just like it was initially:'
            checkBoxMenuItem.isOpaque()
        where :
            opaque | styler
            true   | {it}
            true   | {it.backgroundColor("red")}
            false  | {it.backgroundColor("transparent red")}
            true   | {it.backgroundColor(UI.color(255,255,255, 255))}
            true   | {it.backgroundColor("blue").foundationColor("red")}
            false  | {it.backgroundColor("blue").foundationColor("red").border(2, "transparent oak")}
            true   | {it.backgroundColor("rgb(220,220,220)").foundationColor("light oak").shadowIsInset(true).shadowColor("black").shadowBlurRadius(3).borderRadius(24).margin(16).padding(16)}
            true   | {it.foundationColor("light oak").shadowIsInset(true).shadowColor("black").shadowBlurRadius(3).borderRadius(24).margin(16).padding(16)}
            false  | {it.foundationColor("transparent light oak").shadowIsInset(true).shadowColor("black").shadowBlurRadius(3).borderRadius(24).margin(16).padding(16)}
            true   | {it.backgroundColor("rgb(220,220,220)").foundationColor("light oak").shadowIsInset(true).shadowColor("black").shadowBlurRadius(3).margin(16).padding(16)}
            true   | {it.foundationColor("light oak").shadowIsInset(true).shadowColor("black").shadowBlurRadius(3).margin(16).padding(16)}
            true   | {it.shadowColor(Color.BLACK).shadowBlurRadius(6)}
    }

    def 'A `JLabel`, which is normally non-opaque, can be opaque through an opaque default background color and depending on additional styling.'(
        java.awt.Color color, boolean opaque, Styler<JBox> styler
    ) {
        reportInfo """
 
            A `JLabel` is a component that is non-opaque by default but can be turned
            into an opaque component by passing a opaque color to it initially.
            This test demonstrates that this default opaqueness may or may not change
            depending on what kind of styles are applied to it on top of that.

        """
        given : 'We first define a boolean flag that we will use to control the style:'
            var isOn = false
        and : 'Then we create the opaque colored label based UI declaration, which is temporarily styled:'
            var ui =
                    UI.label("Hi!").withSize(100, 100)
                    .withBackground(color)
                    .withStyle({ isOn ? styler(it) : it })
        and : 'We build the underlying label:'
            var label = ui.get(JLabel)
        expect : 'The label is opaque if the color is opaque:'
            label.isOpaque() == (color.alpha == 255)
        when : 'We set the `isOn` flag to true and then refresh the UI:'
            isOn = true
            UI.runNow(()->{
                ComponentExtension.from(label).gatherApplyAndInstallStyle(true)
            })
        then : 'The label has the expected opaqueness:'
            label.isOpaque() == opaque
        when : 'We set the `isOn` flag to false and then refresh the UI:'
            isOn = false
            UI.runNow(()->{
                ComponentExtension.from(label).gatherApplyAndInstallStyle(true)
            })
        then : 'The label has the initial opaqueness again:'
            label.isOpaque() == (color.alpha == 255)
        where :
                color                   | opaque | styler
            UI.Color.GREEN              | true   | {it}
            UI.Color.GREEN              | true   | {it.backgroundColor("red")}
            UI.Color.GREEN              | false  | {it.backgroundColor("transparent red")}
            UI.Color.GREEN              | true   | {it.backgroundColor(UI.color(255,255,255, 255))}
            UI.Color.GREEN              | true   | {it.backgroundColor("blue").foundationColor("red")}
            UI.Color.GREEN              | false  | {it.backgroundColor("blue").foundationColor("red").border(2, "transparent oak")}
            UI.Color.GREEN              | true   | {it.backgroundColor("rgb(220,220,220)").foundationColor("light oak").shadowIsInset(true).shadowColor("black").shadowBlurRadius(3).borderRadius(24).margin(16).padding(16)}
            UI.Color.GREEN              | true   | {it.foundationColor("light oak").shadowIsInset(true).shadowColor("black").shadowBlurRadius(3).borderRadius(24).margin(16).padding(16)}
            UI.Color.GREEN              | false  | {it.foundationColor("transparent light oak").shadowIsInset(true).shadowColor("black").shadowBlurRadius(3).borderRadius(24).margin(16).padding(16)}
            UI.Color.GREEN              | true   | {it.backgroundColor("rgb(220,220,220)").foundationColor("light oak").shadowIsInset(true).shadowColor("black").shadowBlurRadius(3).margin(16).padding(16)}
            UI.Color.GREEN              | true   | {it.foundationColor("light oak").shadowIsInset(true).shadowColor("black").shadowBlurRadius(3).margin(16).padding(16)}
            UI.Color.GREEN              | true   | {it.shadowColor(Color.BLACK).shadowBlurRadius(6)}
            UI.Color.RED.withAlpha(100) | false  | {it}
            UI.Color.RED.withAlpha(200) | true   | {it.backgroundColor("red")}
            UI.Color.RED.withAlpha(240) | false  | {it.backgroundColor("transparent red")}
            UI.Color.RED.withAlpha(245) | true   | {it.backgroundColor(UI.color(255,255,255, 255))}
            UI.Color.RED.withAlpha(13 ) | true   | {it.backgroundColor("blue").foundationColor("red")}
            UI.Color.RED.withAlpha(0  ) | false  | {it.backgroundColor("blue").foundationColor("red").border(2, "transparent oak")}
            UI.Color.RED.withAlpha(245) | true   | {it.backgroundColor("rgb(220,220,220)").foundationColor("light oak").shadowIsInset(true).shadowColor("black").shadowBlurRadius(3).borderRadius(24).margin(16).padding(16)}
            UI.Color.RED.withAlpha(245) | false  | {it.foundationColor("light oak").shadowIsInset(true).shadowColor("black").shadowBlurRadius(3).borderRadius(24).margin(16).padding(16)}
            UI.Color.RED.withAlpha(245) | false  | {it.foundationColor("transparent light oak").shadowIsInset(true).shadowColor("black").shadowBlurRadius(3).borderRadius(24).margin(16).padding(16)}
            UI.Color.RED.withAlpha(254) | true   | {it.backgroundColor("rgb(220,220,220)").foundationColor("light oak").shadowIsInset(true).shadowColor("black").shadowBlurRadius(3).margin(16).padding(16)}
            UI.Color.RED.withAlpha(254) | false  | {it.foundationColor("light oak").shadowIsInset(true).shadowColor("black").shadowBlurRadius(3).margin(16).padding(16)}
            UI.Color.RED.withAlpha(254) | false  | {it.shadowColor(Color.BLACK).shadowBlurRadius(6)}
    }

    def 'A `JPanel`, which is normally opaque, can be non-opaque due to a default background color or depending on additional styling.'(
        java.awt.Color color, boolean opaque, Styler<JBox> styler
    ) {
        reportInfo """
 
            A `JPanel` is a component that is opaque by default but can be turned
            into a non-opaque component by passing a transparent color to it initially.
            This test demonstrates that this default opaqueness may or may not change
            depending on what kind of styles are applied to a panel on top of that.

        """
        given : 'We first define a boolean flag that we will use to control the style:'
            var isOn = false
        and : 'Then we create the opaque colored panel based UI declaration, which is temporarily styled:'
            var ui =
                    UI.panel().withSize(100, 100)
                    .withBackground(color)
                    .withStyle({ isOn ? styler(it) : it })
        and : 'We build the underlying panel:'
            var panel = ui.get(JPanel)
        expect : 'The panel is opaque if the color is opaque:'
            panel.isOpaque() == (color.alpha == 255)
        when : 'We set the `isOn` flag to true and then refresh the UI:'
            isOn = true
            UI.runNow(()->{
                ComponentExtension.from(panel).gatherApplyAndInstallStyle(true)
            })
        then : 'The panel has the expected opaqueness:'
            panel.isOpaque() == opaque
        when : 'We set the `isOn` flag to false and then refresh the UI:'
            isOn = false
            UI.runNow(()->{
                ComponentExtension.from(panel).gatherApplyAndInstallStyle(true)
            })
        then : 'The panel has the initial opaqueness again:'
            panel.isOpaque() == (color.alpha == 255)
        where :
                color                   | opaque | styler
            UI.Color.GREEN              | true   | {it}
            UI.Color.GREEN              | true   | {it.backgroundColor("red")}
            UI.Color.GREEN              | false  | {it.backgroundColor("transparent red")}
            UI.Color.GREEN              | true   | {it.backgroundColor(UI.color(255,255,255, 255))}
            UI.Color.GREEN              | true   | {it.backgroundColor("blue").foundationColor("red")}
            UI.Color.GREEN              | false  | {it.backgroundColor("blue").foundationColor("red").border(2, "transparent oak")}
            UI.Color.GREEN              | true   | {it.backgroundColor("rgb(220,220,220)").foundationColor("light oak").shadowIsInset(true).shadowColor("black").shadowBlurRadius(3).borderRadius(24).margin(16).padding(16)}
            UI.Color.GREEN              | true   | {it.foundationColor("light oak").shadowIsInset(true).shadowColor("black").shadowBlurRadius(3).borderRadius(24).margin(16).padding(16)}
            UI.Color.GREEN              | false  | {it.foundationColor("transparent light oak").shadowIsInset(true).shadowColor("black").shadowBlurRadius(3).borderRadius(24).margin(16).padding(16)}
            UI.Color.GREEN              | true   | {it.backgroundColor("rgb(220,220,220)").foundationColor("light oak").shadowIsInset(true).shadowColor("black").shadowBlurRadius(3).margin(16).padding(16)}
            UI.Color.GREEN              | true   | {it.foundationColor("light oak").shadowIsInset(true).shadowColor("black").shadowBlurRadius(3).margin(16).padding(16)}
            UI.Color.GREEN              | true   | {it.shadowColor(Color.BLACK).shadowBlurRadius(6)}
            UI.Color.GREEN              | true   | {it.gradient( g -> g.type(UI.GradientType.RADIAL).colors(UI.Color.BLUEVIOLET, UI.Color.WHITE.withAlpha(0)) )}
            UI.Color.RED.withAlpha(100) | false  | {it}
            UI.Color.RED.withAlpha(200) | true   | {it.backgroundColor("red")}
            UI.Color.RED.withAlpha(240) | false  | {it.backgroundColor("transparent red")}
            UI.Color.RED.withAlpha(245) | true   | {it.backgroundColor(UI.color(255,255,255, 255))}
            UI.Color.RED.withAlpha(13 ) | true   | {it.backgroundColor("blue").foundationColor("red")}
            UI.Color.RED.withAlpha(0  ) | false  | {it.backgroundColor("blue").foundationColor("red").border(2, "transparent oak")}
            UI.Color.RED.withAlpha(245) | true   | {it.backgroundColor("rgb(220,220,220)").foundationColor("light oak").shadowIsInset(true).shadowColor("black").shadowBlurRadius(3).borderRadius(24).margin(16).padding(16)}
            UI.Color.RED.withAlpha(245) | false  | {it.foundationColor("light oak").shadowIsInset(true).shadowColor("black").shadowBlurRadius(3).borderRadius(24).margin(16).padding(16)}
            UI.Color.RED.withAlpha(245) | false  | {it.foundationColor("transparent light oak").shadowIsInset(true).shadowColor("black").shadowBlurRadius(3).borderRadius(24).margin(16).padding(16)}
            UI.Color.RED.withAlpha(254) | true   | {it.backgroundColor("rgb(220,220,220)").foundationColor("light oak").shadowIsInset(true).shadowColor("black").shadowBlurRadius(3).margin(16).padding(16)}
            UI.Color.RED.withAlpha(254) | false  | {it.foundationColor("light oak").shadowIsInset(true).shadowColor("black").shadowBlurRadius(3).margin(16).padding(16)}
            UI.Color.RED.withAlpha(254) | false  | {it.shadowColor(Color.BLACK).shadowBlurRadius(6)}
    }


    def 'SwingTree may bypass the AWT background color using its own style based background color.'(
        boolean isColorBypass, boolean opaque, Styler<JBox> styler
    ) {
        reportInfo """
            This unit test covers an interesting but important and unfortunately
            unavoidable implementation detail of SwingTree's styling system.
            When a component is styled to have things rendered in the background layer, 
            such as a gradient or a shadow, then SwingTree needs to paint this stuff 
            before the Look and Feel renders the contents of the component.
            
            This is not a problem by itself, especially for naturally non-opaque components like `JLabel` or `JBox`,
            but for opaque components like `JPanel` most Look and Feels will always fill out
            the entire background with the AWT background color taken from `Component.getBackground()`.
            This is a problem because it will cover up any custom painting done in the background layer of the style!
            
            Now, to overcome this problem, SwingTree has a special "color bypass" mechanism that it 
            applies to opaque components that have background layer styles.
            It takes the `Component.getBackground()` and sets it in the SwingTree style,
            and then sets the AWT background color to `UI.Color.UNDEFINED`, which is transparent.
            SwingTree will effectively take over the responsibility of painting the component background!
        """
        given : 'We first define a boolean flag that we will use to control the style:'
            var isOn = false
        and : 'Then we create the opaque colored panel based UI declaration, which is temporarily styled:'
            var ui =
                    UI.panel().withSize(60, 100)
                    .withBackground(UI.Color.WHEAT)
                    .withStyle({ isOn ? styler(it) : it })
        and : 'We build the underlying panel:'
            var panel = ui.get(JPanel)
        expect : 'The panel is opaque initially:'
            panel.isOpaque()
            ComponentExtension.from(panel).getStyle() == StyleConf.none()
        when : 'We set the `isOn` flag to true and then refresh the UI:'
            isOn = true
            UI.runNow(()->{
                ComponentExtension.from(panel).gatherApplyAndInstallStyle(true)
            })
        then : 'The panel has the expected opaqueness:'
            panel.isOpaque() == opaque
            ComponentExtension.from(panel).getStyle().base().backgroundColor().isPresent() == isColorBypass
            !isColorBypass || !opaque || panel.getBackground() === UI.Color.UNDEFINED

        when : 'We set the `isOn` flag to false and then refresh the UI:'
            isOn = false
            UI.runNow(()->{
                ComponentExtension.from(panel).gatherApplyAndInstallStyle(true)
            })
        then : 'The panel, once again, is opaque:'
            panel.isOpaque()
            ComponentExtension.from(panel).getStyle() == StyleConf.none()
        where :
            isColorBypass | opaque | styler
             false        | true   | {it}
             true         | true   | {it.backgroundColor("red")}
             true         | false  | {it.backgroundColor("transparent red")}
             true         | true   | {it.backgroundColor(UI.color(255,255,255, 255))}
             true         | true   | {it.backgroundColor("blue").foundationColor("red")}
             true         | false  | {it.backgroundColor("blue").foundationColor("red").border(2, "transparent oak")}
             true         | true   | {it.backgroundColor("rgb(220,220,220)").foundationColor("light oak").shadowIsInset(true).shadowColor("black").shadowBlurRadius(3).borderRadius(24).margin(16).padding(16)}
             true         | true   | {it.foundationColor("light oak").shadowIsInset(true).shadowColor("black").shadowBlurRadius(3).borderRadius(24).margin(16).padding(16)}
             true         | false  | {it.foundationColor("transparent light oak").shadowIsInset(true).shadowColor("black").shadowBlurRadius(3).borderRadius(24).margin(16).padding(16)}
             true         | true   | {it.backgroundColor("rgb(220,220,220)").foundationColor("light oak").shadowIsInset(true).shadowColor("black").shadowBlurRadius(3).margin(16).padding(16)}
             true         | true   | {it.foundationColor("light oak").shadowIsInset(true).shadowColor("black").shadowBlurRadius(3).margin(16).padding(16)}
             false        | true   | {it.shadowColor(Color.BLACK).shadowBlurRadius(6)}
             true         | true   | {it.gradient( g -> g.type(UI.GradientType.RADIAL).colors(UI.Color.BLUEVIOLET, UI.Color.WHITE.withAlpha(0)) )}
    }

}

