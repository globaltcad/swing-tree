package com.globaltcad.swingtree.mvvm

import com.globaltcad.swingtree.UI
import com.globaltcad.swingtree.api.mvvm.Val
import com.globaltcad.swingtree.api.mvvm.Var
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
            Val<Dimension> property = Var.of(new Dimension(100, 100))
        and : 'We create a UI to which we want to bind:'
            var node = UI.panel("fill, wrap 1")
                        .add(UI.label("Hello World"))
                        .add(UI.button("Click Me").withMinimumSize(property))
                        .add(UI.textField("Hello World"))

        expect : 'The button will have the size of the property.'
            node.component.components[1].minimumSize == new Dimension(100, 100)

        when : 'We change the value of the property.'
            property.set(new Dimension(200, 200))

        then : 'Nothing will happen at first.'
            node.component.components[1].minimumSize == new Dimension(100, 100)

        when : 'We call the "view()" method on the property however...'
            property.view()

        then : 'The button will have the new size.'
            node.component.components[1].minimumSize == new Dimension(200, 200)
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

        then : 'Nothing will happen at first.'
            node.component.components[1].background == Color.RED

        when : 'We call the "view()" method on the property however...'
            property.view()

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

        then : 'Nothing will happen at first.'
            node.component.components[1].text == "Hello World"

        when : 'We call the "view()" method on the property however...'
            property.view()

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

        then : 'Nothing will happen at first.'
            node.component.components[1].enabled == true

        when : 'We call the "view()" method on the property however...'
            property.view()

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

        then : 'Nothing will happen at first.'
            node.component.components[1].selected == true

        when : 'We call the "view()" method on the property however...'
            property.view()

        then : 'The checkbox will be unselected.'
            node.component.components[1].selected == false
    }
}
