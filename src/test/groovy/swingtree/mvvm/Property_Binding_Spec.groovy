package swingtree.mvvm

import swingtree.UI
import swingtree.api.mvvm.Val
import swingtree.api.mvvm.Var
import swingtree.Utility
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
                        .add(UI.label("Hello World").withPreferredSize(size))
                        .add(UI.button("Click Me").withMinimumSize(size))
                        .add(UI.textField("Hello World").withMaximumSize(size))

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
                        .add(UI.label("Hello World").withMinimumWidth(minWidth))
                        .add(UI.button("Click Me").withPreferredHeight(prefHeight))
                        .add(UI.textField("Hello World").withMaximumWidth(maxWidth))
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
                        .add(UI.label("Hello World").withMinimumSize(width, height))
                        .add(UI.toggleButton("Click Me").withPreferredSize(width, height))
                        .add(UI.textArea("Hello World").withMaximumSize(width, height))
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

}
