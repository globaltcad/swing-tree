package swingtree.mvvm

import swingtree.UI
import sprouts.Val
import sprouts.Var
import utility.Utility
import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Title

import java.awt.Color
import java.awt.Dimension

@Title("Property Binding")
@Narrative('''

    This specification shows you how to bind properties to 
    the states of common types UI components.

''')
class Property_Binding_Spec extends Specification
{
    enum Accept { YES, NO, MAYBE }

    def 'We can bind a property to the size of a swing component.'()
    {
        reportInfo"""
            Note that the binding of a Swing-Tree property will only have side effects
            when it is deliberately triggered to execute its side effects.
            This is important to allow you to decide yourself when
            the state of a property is "ready" for display in the UI.
        """
        given : 'We create a property representing the size of a component.'
            Val<Dimension> size = Var.of(new Dimension(100, 100))
        and : 'We create a UI to which we want to bind:'
            var node = UI.panel("fill, wrap 1")
                        .add(UI.label("Hello World").withPrefSize(size))
                        .add(UI.button("Click Me").withMinSize(size))
                        .add(UI.textField("Hello World").withMaxSize(size))

        expect : 'The components will have the size of the property.'
            node.component.components[0].preferredSize == new Dimension(100, 100)
            node.component.components[1].minimumSize == new Dimension(100, 100)
            node.component.components[2].maximumSize == new Dimension(100, 100)

        when : 'We change the value of the property.'
            size.set(new Dimension(200, 200))
        and : 'Then we wait for the EDT to complete the UI modifications...'
            UI.sync()

        then : 'The components will have the new sizes.'
            node.component.components[0].preferredSize == new Dimension(200, 200)
            node.component.components[1].minimumSize == new Dimension(200, 200)
            node.component.components[2].maximumSize == new Dimension(200, 200)
    }

    def 'Simple integer properties can be bound to the width or height of components.'()
    {
        given : 'We create properties representing the width and heights of a components.'
            Var<Integer> minWidth = Var.of(60)
            Var<Integer> prefHeight = Var.of(40)
            Var<Integer> maxWidth = Var.of(90)
        and : 'We create a UI to which we want to bind:'
            var node = UI.panel("fill, wrap 1")
                        .add(UI.label("Hello World").withMinWidth(minWidth))
                        .add(UI.button("Click Me").withPrefHeight(prefHeight))
                        .add(UI.textField("Hello World").withMaxWidth(maxWidth))
        expect : 'The components will have the sizes of the properties.'
            node.component.components[0].minimumSize.width == 60
            node.component.components[1].preferredSize.height == 40
            node.component.components[2].maximumSize.width == 90

        when : 'We change the value of the properties.'
            minWidth.set(100)
            prefHeight.set(80)
            maxWidth.set(120)
        and : 'Then we wait for the EDT to complete the UI modifications...'
            UI.sync()

        then : 'The components will have the new sizes.'
            node.component.components[0].minimumSize.width == 100
            node.component.components[1].preferredSize.height == 80
            node.component.components[2].maximumSize.width == 120
    }

    def 'Bind to both width and height independently if you want to.'()
    {
        given : 'We create a property representing the width of a component.'
            Var<Integer> width = Var.of(60)
            Var<Integer> height = Var.of(40)
        and : 'We create a UI to which we want to bind:'
            var node = UI.panel("fill, wrap 1")
                        .add(UI.label("Hello World").withMinSize(width, height))
                        .add(UI.toggleButton("Click Me").withPrefSize(width, height))
                        .add(UI.textArea("Hello World").withMaxSize(width, height))
        expect : 'The components will have the sizes of the properties.'
            node.component.components[0].minimumSize == new Dimension(60, 40)
            node.component.components[1].preferredSize == new Dimension(60, 40)
            node.component.components[2].maximumSize == new Dimension(60, 40)

        when : 'We change the value of the properties.'
            width.set(100)
            height.set(80)
        and : 'Then we wait for the EDT to complete the UI modifications...'
            UI.sync()

        then : 'The components will have the new sizes.'
            node.component.components[0].minimumSize == new Dimension(100, 80)
            node.component.components[1].preferredSize == new Dimension(100, 80)
            node.component.components[2].maximumSize == new Dimension(100, 80)
    }

    def 'We can bind to the color of a component.'()
    {
        given : 'We create a property representing the color of a component.'
            Val<Color> property = Var.of(Color.RED)
        and : 'We create a UI to which we want to bind:'
            var node = UI.panel("fill, wrap 1")
                        .add(UI.button("Click Me!"))
                        .add(UI.label("I have a Background").withBackground(property))
                        .add(UI.textField("Hello World"))

        expect : 'The label will have the background color of the property.'
            node.component.components[1].background == Color.RED

        when : 'We change the value of the property.'
            property.set(Color.BLUE)
        and : 'Then we wait for the EDT to complete the UI modifications...'
            UI.sync()

        then : 'The label will have the new color.'
            node.component.components[1].background == Color.BLUE
    }

    def 'We can bind to the text of a component.'()
    {
        given : 'We create a property representing the text of a component.'
            Val<String> property = Var.of("Hello World")
        and : 'We create a UI to which we want to bind:'
            var node = UI.panel("fill, wrap 1")
                        .add(UI.button("Click Me!"))
                        .add(UI.textField("Hello World").withText(property))
                        .add(UI.checkBox("Hello World"))

        expect : 'The text field will have the text of the property.'
            node.component.components[1].text == "Hello World"

        when : 'We change the value of the property.'
            property.set("Goodbye World")
        and : 'Then we wait for the EDT to complete the UI modifications...'
            UI.sync()

        then : 'The text field will have the new text.'
            node.component.components[1].text == "Goodbye World"
    }

    def 'We can enable and disable a UI component dynamically through property binding.'()
    {
        given : 'We create a property representing the enabled state of a component.'
            Val<Boolean> property = Var.of(true)
        and : 'We create a UI to which we want to bind:'
            var node = UI.panel("fill, wrap 1")
                        .add(UI.label("Below me is a spinner!"))
                        .add(UI.spinner().isEnabledIf(property))
                        .add(UI.textArea("I am here for decoration..."))

        expect : 'The spinner will be enabled.'
            node.component.components[1].enabled == true

        when : 'We change the value of the property.'
            property.set(false)
        and : 'Then we wait for the EDT to complete the UI modifications...'
            UI.sync()

        then : 'The spinner will be disabled.'
            node.component.components[1].enabled == false
    }

    def 'We can select or unselect a UI component dynamically through properties.'()
    {
        given : 'We create a property representing the selected state of a component.'
            Val<Boolean> property = Var.of(true)
        and : 'We create a UI to which we want to bind:'
            var node = UI.panel("fill, wrap 1")
                        .add(UI.label("Below me is a checkbox!"))
                        .add(UI.checkBox("I am a checkbox").isSelectedIf(property))
                        .add(UI.textArea("I am here for decoration..."))
        expect : 'The checkbox will be selected.'
            node.component.components[1].selected == true

        when : 'We change the value of the property.'
            property.set(false)
        and : 'Then we wait for the EDT to complete the UI modifications...'
            UI.sync()

        then : 'The checkbox will be unselected.'
            node.component.components[1].selected == false
    }

    def 'Enable or disable the split items of a JSplitButton through properties.'()
    {
        given : 'We create a property representing the enabled state of a component.'
            Val<Boolean> property = Var.of(true)
        and : 'We create a UI to which we want to bind:'
            var node = UI.splitButton("I am a split button")
                            .add(UI.splitItem("I am a button").isEnabledIf(property))
                            .add(UI.splitItem("I am a button"))
                            .add(UI.splitItem("I am a button"))

        expect : 'The first split item will be enabled.'
            Utility.getSplitButtonPopup(node).components[0].enabled == true

        when : 'We change the value of the property.'
            property.set(false)
        and : 'Then we wait for the EDT to complete the UI modifications...'
            UI.sync()

        then : 'The first split item will be disabled.'
            Utility.getSplitButtonPopup(node).components[0].enabled == false
    }

    def 'The visibility of a UI component can be modelled dynamically using boolean properties.'()
    {
        given : 'We create a property representing the visibility state of a component.'
            Val<Boolean> property = Var.of(true)
        and : 'We create a UI to which we want to bind:'
            var node = UI.panel("fill, wrap 1")
                        .add(UI.label("Below me is a spinner!"))
                        .add(UI.spinner().isVisibleIf(property))
                        .add(UI.textArea("I am here for decoration..."))
                        .add(UI.slider(UI.Align.VERTICAL).isVisibleIfNot(property))
        expect : 'The spinner will be visible.'
            node.component.components[1].visible == true
        and : 'The slider will be invisible.'
            node.component.components[3].visible == false

        when : 'We change the value of the property.'
            property.set(false)
        and : 'Then we wait for the EDT to complete the UI modifications...'
            UI.sync()

        then : 'The spinner will be invisible.'
            node.component.components[1].visible == false
        and : 'The slider will be visible.'
            node.component.components[3].visible == true
    }

    def 'The visibility of a UI component can be modelled using an enum property.'()
    {
        reportInfo """
            Enums are a common tool for modelling user choices and settings in you view models
            because they are type safe and descriptive. 
            A common use case is to have certain UI components only visible if a certain enum value
            in your view model is selected.

            For this example, we will use the following enum:
            ```
                enum Accept { YES, NO, MAYBE }
            ```
        """
        given : 'We create a property representing the visibility state of a component.'
            Var<Accept> property = Var.of(Accept.YES)
        and : 'We create a UI with the enum property based binding.'
            var ui = UI.panel("fill, wrap 1")
                        .add(UI.label("If you accept the terms we can proceed!"))
                        .add(UI.button("Yes proceed!").isVisibleIf(Accept.YES, property))
                        .add(UI.label("Maybe or No is not enough :/").isVisibleIfNot(Accept.YES, property))
        expect : 'Initially the bound button will be visible.'
            ui.component.components[1].visible == true
        and : 'The bound label will be invisible.'
            ui.component.components[2].visible == false

        when : 'We change the value of the property.'
            property.set(Accept.NO)
        and : 'Then we wait for the EDT to complete the UI modifications...'
            UI.sync()

        then : 'The bound button will be invisible.'
            ui.component.components[1].visible == false
        and : 'The bound label will be visible.'
            ui.component.components[2].visible == true
    }

    def 'The enabled/disabled state of a UI component can be modelled using an enum property.'()
    {
        reportInfo """
            Enums are a common tool for modelling user choices and settings in you view models
            because they are type safe and descriptive. 
            A common use case is to have certain UI components only enabled if a certain enum value
            in your view model is selected.
            For this example, we will use the following enum:
            ```
                enum Accept { YES, NO, MAYBE }
            ```
        """
        given : 'We create a property representing the enabled state of a component.'
            Var<Accept> property = Var.of(Accept.YES)
        and : 'We create a UI with the enum property based binding.'
            var ui = UI.panel("fill, wrap 1")
                        .add(UI.label("If you accept the terms we can proceed!"))
                        .add(UI.button("Yes proceed!").isEnabledIf(Accept.YES, property))
                        .add(UI.label("Maybe or No is not enough :/").isEnabledIfNot(Accept.YES, property))
        expect : 'Initially the bound button will be enabled.'
            ui.component.components[1].enabled == true
        and : 'The bound label will be disabled.'
            ui.component.components[2].enabled == false

        when : 'We change the value of the property.'
            property.set(Accept.NO)
        and : 'Then we wait for the EDT to complete the UI modifications...'
            UI.sync()

        then : 'The bound button will be disabled.'
            ui.component.components[1].enabled == false
        and : 'The bound label will be enabled.'
            ui.component.components[2].enabled == true
    }

    def 'The focusability of a UI component can be modelled using an enum property.'()
    {
        reportInfo """
            Enums are a common tool for modelling user choices and settings in you view models
            because they are descriptive and type safe (not like string values). 
            A common use case is to have certain UI components only focused if a certain enum value
            in your view model is selected.
            For this example, we will use the following enum:
            ```
                enum Accept { YES, NO, MAYBE }
            ```
        """
        given : 'We create a property representing the focusability state of a component.'
            Var<Accept> property = Var.of(Accept.YES)
        and : 'We create a UI with the enum property based binding.'
            var ui = UI.panel("fill, wrap 1")
                        .add(UI.label("If you accept the terms we can proceed!"))
                        .add(UI.button("Yes proceed!").isFocusableIf(Accept.YES, property))
                        .add(UI.label("Maybe or No is not enough :/").isFocusableIfNot(Accept.YES, property))
        expect : 'Initially the bound button will be focusable.'
            ui.component.components[1].focusable == true
        and : 'The bound label will be unfocusable.'
            ui.component.components[2].focusable == false

        when : 'We change the value of the property.'
            property.set(Accept.NO)
        and : 'Then we wait for the EDT to complete the UI modifications...'
            UI.sync()

        then : 'The bound button will be unfocusable.'
            ui.component.components[1].focusable == false
        and : 'The bound label will be focusable.'
            ui.component.components[2].focusable == true
    }

    def 'The focusability of a UI component can be modelled dynamically using boolean properties.'()
    {
        given : 'We create a property representing the focusability state of a component.'
            Val<Boolean> property = Var.of(true)
        and : 'We create a UI to which we want to bind:'
            var node = UI.panel("fill, wrap 1")
                        .add(UI.label("Below me is a spinner!"))
                        .add(UI.spinner().isFocusableIf(property))
                        .add(UI.textArea("I am here for decoration..."))
                        .add(UI.slider(UI.Align.VERTICAL).isFocusableIfNot(property))
        expect : 'The spinner will be focusable.'
            node.component.components[1].focusable == true
        and : 'The slider will be unfocusable.'
            node.component.components[3].focusable == false

        when : 'We change the value of the property.'
            property.set(false)
        and : 'Then we wait for the EDT to complete the UI modifications...'
            UI.sync()

        then : 'The spinner will be unfocusable.'
            node.component.components[1].focusable == false
        and : 'The slider will be focusable.'
            node.component.components[3].focusable == true
    }

    def 'Minimum as well as maximum height of UI components can be modelled using integer properties.'()
    {
        given : 'We create a property representing the minimum and maximum height.'
            Val<Integer> property = Var.of(50)
        and : 'We create a UI to which we want to bind:'
            var node = UI.panel("fill, wrap 1")
                        .add(UI.label("Below me is a text area!"))
                        .add(UI.textArea("hi").withMinHeight(property))
                        .add(UI.label("Below me is another text area!"))
                        .add(UI.textArea("Hey").withMaxHeight(property))

        expect : 'The minimum height of the first text area will be 50.'
            node.component.components[1].minimumSize.height == 50
        and : 'The maximum height of the second text area will be 50.'
            node.component.components[3].maximumSize.height == 50

        when : 'We change the value of the property.'
            property.set(100)
        and : 'Then we wait for the EDT to complete the UI modifications...'
            UI.sync()

        then : 'The minimum height of the first text area will be 100.'
            node.component.components[1].minimumSize.height == 100
        and : 'The maximum height of the second text area will be 100.'
            node.component.components[3].maximumSize.height == 100
    }


    def 'The width and height of UI components can be modelled using integer properties.'()
    {
        given : 'We create a property representing the width and height.'
            Val<Integer> widthProperty = Var.of(50)
            Val<Integer> heightProperty = Var.of(100)
        and : 'We create a UI to which we want to bind:'
            var node = UI.panel("fill, wrap 1")
                        .add(UI.label("Below me is a text area!"))
                        .add(UI.textArea("hi").withWidth(widthProperty).withHeight(heightProperty))
                        .add(UI.label("Below me is another text area!"))
                        .add(UI.textArea("Hey").withWidth(heightProperty).withHeight(widthProperty))

        expect : 'The width of the first text area will be 50.'
            node.component.components[1].size.width == 50
        and : 'The height of the first text area will be 100.'
            node.component.components[1].size.height == 100
        and : 'The width of the second text area will be 100.'
            node.component.components[3].size.width == 100
        and : 'The height of the second text area will be 50.'
            node.component.components[3].size.height == 50

        when : 'We change the value of the property.'
            widthProperty.set(100)
            heightProperty.set(50)
        and : 'Then we wait for the EDT to complete the UI modifications...'
            UI.sync()

        then : 'The dimensions of both UI components will be as expected.'
            node.component.components[1].size.width == 100
            node.component.components[1].size.height == 50
            node.component.components[3].size.width == 50
            node.component.components[3].size.height == 100
    }

    def 'Bind the foreground of a component to a conditional property and 2 color properties.'()
    {
        reportInfo """
            A common use case is to have a foreground color switching between two colors depending on a condition.
            This can be achieved by using properties for the condition and the colors.
            If any of these change in the view model, the UI component will be updated accordingly.
        """
        given : 'We create 3 properties, 1 boolean one and 2 color properties.'
            Val<Boolean> conditionProperty = Var.of(true)
            Val<Color>   color1Property    = Var.of(Color.RED)
            Val<Color>   color2Property    = Var.of(Color.BLUE)
        and : 'We create a UI to which we want to bind:'
            var ui =
                        UI.panel("fill, wrap 1")
                        .add(UI.label("Below me is a text area!"))
                        .add(UI.textArea("hi").withForegroundIf(conditionProperty, color1Property, color2Property))

        expect : """
                The foreground of the text area will be red, because the condition is true, 
                meaning the first color is selected!
            """
            ui.component.components[1].foreground == Color.RED

        when : 'We change the value of the condition property.'
            conditionProperty.set(false)
            UI.sync() // Wait for the EDT to complete the UI modifications...
        then : 'The foreground of the text area will now switch to blue, because the condition is false.'
            ui.component.components[1].foreground == Color.BLUE

        when : 'We change the value of the color properties.'
            color1Property.set(Color.GREEN)
            color2Property.set(Color.YELLOW)
            UI.sync() // Wait for the EDT to complete the UI modifications...
        then : 'The foreground of the text area will be yellow, because the condition is false.'
            ui.component.components[1].foreground == Color.YELLOW

        when : 'We change the value of the condition property.'
            conditionProperty.set(true)
            UI.sync() // Wait for the EDT to complete the UI modifications...
        then : 'The foreground of the text area will be green, because the condition is true.'
            ui.component.components[1].foreground == Color.GREEN
    }

}
