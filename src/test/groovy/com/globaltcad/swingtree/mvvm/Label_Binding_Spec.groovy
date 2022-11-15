package com.globaltcad.swingtree.mvvm

import com.globaltcad.swingtree.UI
import com.globaltcad.swingtree.api.mvvm.Val
import com.globaltcad.swingtree.api.mvvm.Var
import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Title

import java.awt.Color
import java.awt.Dimension

@Title("Label Binding")
@Narrative('''
    
    As a developer, I want to bind a label to a property so 
    that the label is updated when the property changes.
    Binding is a powerful feature that allows us to create
    UIs which are not only decoupled from the business logic of
    an application, but also make it easy to create UIs which
    are dynamic and reactive.
    
''')
class Label_Binding_Spec extends Specification
{
    def 'We can bind to the text of a label.'()
    {
        reportInfo """
            Note that for a binding to work, the property must be a Var or Val
            implementation and it is required to call the "view()" method on the
            property to apply its value to the view.
        """

        given : 'We create a simple swing-tree property for modelling the text.'
            Val<String> text = Var.of("Hello World")

        when : 'We create and bind to a label UI node...'
            var node = UI.label("").withText(text)

        then : 'The label should be updated when the property changes.'
            node.component.text == "Hello World"

        when : 'We change the property value...'
            text.set("Goodbye World")
        then : 'The label should NOT be updated.'
            node.component.text == "Hello World"

        when : 'We call the "view" method on the property...'
            text.view()
        then : 'The label should be updated.'
            node.component.text == "Goodbye World"
    }

    def 'We can bind to the foreground and background color of a UI node.'()
    {
        given : 'We create a simple swing-tree property for modelling the color.'
            Val<Color> color1 = Var.of(Color.RED)
            Val<Color> color2 = Var.of(Color.BLUE)

        when : 'We create and bind to a label UI node...'
            var node =
                    UI.label("")
                    .withForeground(color1)
                    .withBackground(color2)

        then : 'The label should be updated when the property changes.'
            node.component.foreground == Color.RED
            node.component.background == Color.BLUE

        when : 'We change the property values...'
            color1.set(Color.GREEN)
            color2.set(Color.YELLOW)
        then : 'The label should NOT be updated.'
            node.component.foreground == Color.RED
            node.component.background == Color.BLUE

        when : 'We call the "view" method on the property...'
            color1.view()
            color2.view()
        then : 'The label should be updated.'
            node.component.foreground == Color.GREEN
            node.component.background == Color.YELLOW
    }

    def 'It is possible to bind to the minimum, maximum and preferred size of a label'()
    {
        reportInfo """
            Note that this works for all kind of UI nodes, not just labels.
        """
        given : 'We create a simple swing-tree property for modelling the size.'
            Val<Dimension> minSize = Var.of(new Dimension(100, 100))
            Val<Dimension> maxSize = Var.of(new Dimension(200, 200))
            Val<Dimension> prefSize = Var.of(new Dimension(150, 150))

        when : 'We create and bind to a label UI node...'
            var node =
                    UI.label("")
                    .withMinimumSize(minSize)
                    .withMaximumSize(maxSize)
                    .withPreferredSize(prefSize)

        then : 'The label should be updated when the property changes.'
            node.component.minimumSize == new Dimension(100, 100)
            node.component.maximumSize == new Dimension(200, 200)
            node.component.preferredSize == new Dimension(150, 150)

        when : 'We change the property values...'
            minSize.set(new Dimension(50, 50))
            maxSize.set(new Dimension(100, 100))
            prefSize.set(new Dimension(75, 75))
        then : 'The label should NOT be updated.'
            node.component.minimumSize == new Dimension(100, 100)
            node.component.maximumSize == new Dimension(200, 200)
            node.component.preferredSize == new Dimension(150, 150)

        when : 'We call the "view" method on the property...'
            minSize.view()
            maxSize.view()
            prefSize.view()
        then : 'The label should be updated.'
            node.component.minimumSize == new Dimension(50, 50)
            node.component.maximumSize == new Dimension(100, 100)
            node.component.preferredSize == new Dimension(75, 75)
    }

    def 'You can bind a variable to the "enable" flag of a label.'()
    {
        reportInfo """
            Note that this works for all kind of UI nodes, not just labels.
        """
        given : 'We create a simple swing-tree property for modelling the size.'
            Val<Boolean> enabled = Var.of(true)

        when : 'We create and bind to a label UI node...'
            var node =
                    UI.label("")
                    .isEnabledIf(enabled)

        then : 'The label should be updated when the property changes.'
            node.component.enabled == true

        when : 'We change the property values...'
            enabled.set(false)
        then : 'The label should NOT be updated.'
            node.component.enabled == true

        when : 'We call the "view" method on the property...'
            enabled.view()
        then : 'The label should be updated.'
            node.component.enabled == false
    }

    def 'You can bind a variable to the "visible" flag of a label.'()
    {
        reportInfo """
            Note that this works for all kind of UI nodes, not just labels.
        """
        given : 'We create a simple swing-tree property for modelling the size.'
            Val<Boolean> visible = Var.of(true)

        when : 'We create and bind to a label UI node...'
            var node =
                    UI.label("")
                    .isVisibleIf(visible)

        then : 'The label should be updated when the property changes.'
            node.component.visible == true

        when : 'We change the property values...'
            visible.set(false)
        then : 'The label should NOT be updated.'
            node.component.visible == true

        when : 'We call the "view" method on the property...'
            visible.view()
        then : 'The label should be updated.'
            node.component.visible == false
    }

}
