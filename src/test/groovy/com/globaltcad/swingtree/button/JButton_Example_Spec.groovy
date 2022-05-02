package com.globaltcad.swingtree.button


import com.globaltcad.swingtree.UI
import spock.lang.Specification

import javax.swing.*

class JButton_Example_Spec extends Specification
{
    def 'We can easily create a button with an associated action:'()
    {
        given : 'We create a basic button which will a simple "onClick".'
            var ui =
                UI.button("I am displayed on the button!")
                .onClick( it -> it.component.setEnabled(false) )

        expect : 'The component wrapped by the UI builder is in fact a simple button:'
            ui.component instanceof JButton
        and : 'The button is enabled by default!'
            ui.component.isEnabled()

        when : 'We simulate a user click...'
            ui.component.doClick()
        then : 'The button will be disabled because of the click action we specified!'
            !ui.component.isEnabled()
    }

    def 'A button will delegate its siblings within actions:'()
    {
        given : 'We create a panel with a label and a button!'
        var ui =
            UI.panel()
                .add(UI.label("Button:"))
                .add(
                    UI.button("Click me!")
                    .onClick( it -> it.siblings.each {s -> s.setEnabled(false)} )
                )

        expect : 'The components wrapped by the UI builder are as expected:'
            ui.component.getComponent(0) instanceof JLabel
            ui.component.getComponent(1) instanceof JButton
        and : 'They are enabled by default!'
            ui.component.getComponent(0).isEnabled()
            ui.component.getComponent(1).isEnabled()

        when : 'We simulate a user click...'
            ui.component.getComponent(1).doClick()
        then :  'They are enabled by default!'
            !ui.component.getComponent(0).isEnabled()
            ui.component.getComponent(1).isEnabled()
    }

    def 'We can go through the entire siblinghood, including the current button!'()
    {
        given : 'We create a panel with some random components and a fancy click action'
            var ui =
                UI.panel()
                .add(UI.textField("text field"))
                .add(UI.label("innocent label"))
                .add(
                    UI.button("Click me!")
                    .onClick(it ->
                        it.siblinghood.each {s -> { if ( s instanceof JLabel ) s.setText("I got hacked!") }}
                    )
                )
                .add(UI.spinner())

        when : 'We simulate a user click...'
            ui.component.getComponent(2).doClick()
        then :  'The label text changed!'
            ui.component.getComponent(1).getText() == "I got hacked!"
    }

}
