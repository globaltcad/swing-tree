package swingtree.styles

import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Title
import sprouts.Var
import swingtree.UI
import swingtree.animation.LifeTime

import javax.swing.*
import java.awt.Color
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
                                ? UI.COLOR_UNDEFINED
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
        and : 'Due to the usage of `UI.COLOR_UNDEFINED`, the background color of the slider is undefined too:'
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
        and : 'Due to the usage of `UI.COLOR_UNDEFINED`, the background color of the slider is undefined too:'
            slider.getBackground() == null
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
        and : 'Then we create the slider based UI declaration, which is styled to temporarily have an opaque background color:'
            var ui =
                    UI.checkBox("Checked?")
                    .withTransitionalStyle(isOn, LifeTime.of(1, TimeUnit.MILLISECONDS), (state, it) -> it
                        .margin(margin)
                        .borderRadius(radius)
                        .backgroundColor( state.progress() == 1 ? background : "" )
                        .foundationColor( state.progress() == 1 ? foundation : "" )
                        .borderColor( state.progress() == 1 ? borderColor : "" )
                        .borderWidth( state.progress() == 1 ? border : 0 )
                        .gradient(g -> g.colors( state.progress() == 1 ? gradient : []))
                    )

        and : 'We build the underlying check box:'
            var checkBox = ui.get(javax.swing.JCheckBox)

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


    }
}

