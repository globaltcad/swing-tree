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
            implementation and it is required to call the "show()" method on the
            property to apply its value to the view.
        """

        given : 'We create a simple swing-tree property for modelling the text.'
            Val<String> text = Var.of("Hello World")

        when : 'We create and bind to a label UI node...'
            var ui = UI.label("").withText(text)

        then : 'The label should be updated when the property changes.'
            ui.component.text == "Hello World"

        when : 'We change the property value...'
            text.set("Goodbye World")
        then : 'The label should NOT be updated.'
            ui.component.text == "Hello World"

        when : 'We call the "show" method on the property...'
            text.show()
        then : 'The label should be updated.'
            ui.component.text == "Goodbye World"
    }

    def 'We can bind to the foreground and background color of a UI node.'()
    {
        given : 'We create 2 simple swing-tree properties for modelling colors.'
            Val<Color> color1 = Var.of(Color.RED)
            Val<Color> color2 = Var.of(Color.BLUE)

        when : 'We create and bind to a label UI node...'
            var ui =
                    UI.label("")
                    .withForeground(color1)
                    .withBackground(color2)

        then : 'The label should have the property colors.'
            ui.component.foreground == Color.RED
            ui.component.background == Color.BLUE

        when : 'We change the color values of both properties...'
            color1.set(Color.GREEN)
            color2.set(Color.YELLOW)
        then : 'At first the label colors are NOT updated.'
            ui.component.foreground == Color.RED
            ui.component.background == Color.BLUE

        when : 'We call the "show" method on the properties however...'
            color1.show()
            color2.show()
        then : 'The label colors are updated.'
            ui.component.foreground == Color.GREEN
            ui.component.background == Color.YELLOW
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
            var ui =
                    UI.label("")
                    .withMinimumSize(minSize)
                    .withMaximumSize(maxSize)
                    .withPreferredSize(prefSize)

        then : 'The label should be updated when the property changes.'
            ui.component.minimumSize == new Dimension(100, 100)
            ui.component.maximumSize == new Dimension(200, 200)
            ui.component.preferredSize == new Dimension(150, 150)

        when : 'We change the property values...'
            minSize.set(new Dimension(50, 50))
            maxSize.set(new Dimension(100, 100))
            prefSize.set(new Dimension(75, 75))
        then : 'The label should NOT be updated at first.'
            ui.component.minimumSize == new Dimension(100, 100)
            ui.component.maximumSize == new Dimension(200, 200)
            ui.component.preferredSize == new Dimension(150, 150)

        when : 'We call the "show" method on the property...'
            minSize.show()
            maxSize.show()
            prefSize.show()
        then : 'The label should be updated.'
            ui.component.minimumSize == new Dimension(50, 50)
            ui.component.maximumSize == new Dimension(100, 100)
            ui.component.preferredSize == new Dimension(75, 75)
    }

    def 'You can bind a variable to the "enable" flag of a label.'()
    {
        reportInfo """
            Note that this works for all kind of UI nodes, not just labels.
        """
        given : 'We create a simple swing-tree property for modelling the size.'
            Val<Boolean> enabled = Var.of(true)

        when : 'We create and bind to a label UI node...'
            var ui =
                    UI.label("")
                    .isEnabledIf(enabled)

        then : 'The label should be updated when the property changes.'
            ui.component.enabled == true

        when : 'We change the property values...'
            enabled.set(false)
        then : 'The label should NOT be updated.'
            ui.component.enabled == true

        when : 'We call the "show" method on the property...'
            enabled.show()
        then : 'The label should be updated.'
            ui.component.enabled == false
    }

    def 'You can bind a variable to the "visible" flag of a label.'()
    {
        reportInfo """
            Note that this works for all kind of UI nodes, not just labels.
        """
        given : 'We create a simple swing-tree property for modelling the size.'
            Val<Boolean> visible = Var.of(true)

        when : 'We create and bind to a label UI node...'
            var ui =
                    UI.label("")
                    .isVisibleIf(visible)

        then : 'The label should be updated when the property changes.'
            ui.component.visible == true

        when : 'We change the property values...'
            visible.set(false)
        then : 'The label should NOT be updated.'
            ui.component.visible == true

        when : 'We call the "show" method on the property...'
            visible.show()
        then : 'The label should be updated.'
            ui.component.visible == false
    }

}
