package com.globaltcad.swingtree

import spock.lang.Specification

import javax.swing.JComboBox
import javax.swing.JMenu
import javax.swing.JMenuItem
import javax.swing.JPanel
import javax.swing.JSeparator
import javax.swing.JSlider

class Exception_Spec extends Specification
{
    def 'The given factory methods do not accept null arguments.'(
            Runnable illegalAction
    ) {
        when : 'We execute the error prone code...'
            illegalAction()
        then : 'An illegal argument exception will be thrown!'
            thrown(IllegalArgumentException)
        where :
            illegalAction << [
                    {UI.of((JMenu)null)},
                    {UI.of((JSeparator)null)},
                    {UI.of((JMenuItem)null)},
                    {UI.of((JPanel)null)},
                    {UI.labelWithIcon(null)},
                    {UI.labelWithIcon(300,200,null)},
                    {UI.buttonWithIcon(null, null,null)},
                    {UI.of((SwingBuilder)null)}
            ]
    }

    def 'On mouse click action lambdas may not be null.'() {
        given : 'We have a simple JPanel UI node.'
            var node = UI.panel()

        when : 'We try to pass null as an on click action...'
            node.onMouseClick(null)

        then : 'An illegal argument exception will be thrown!'
            thrown(IllegalArgumentException)
    }

    def 'On mouse click component action lambdas may not be null.'() {
        given : 'We have a simple JPanel UI node.'
            var node = UI.panel()

        when : 'We try to pass null as an on click action...'
            node.onMouseClickComponent(null)

        then : 'An illegal argument exception will be thrown!'
            thrown(IllegalArgumentException)
    }

    def 'On mouse click event action lambdas may not be null.'() {
        given : 'We have a simple JPanel UI node.'
            var node = UI.panel()

        when : 'We try to pass null as an on click action...'
            node.onMouseClickEvent(null)

        then : 'An illegal argument exception will be thrown!'
            thrown(IllegalArgumentException)
    }

    def 'On JComboBox change action lambdas may not be null.'() {
        given : 'We have a simple JPanel UI node.'
            var node = UI.of(new JComboBox())

        when : 'We try to pass null as an on change action...'
            node.onChange(null)

        then : 'An illegal argument exception will be thrown!'
            thrown(IllegalArgumentException)
    }

    def 'On JComboBox change component action lambdas may not be null.'() {
        given : 'We have a simple JPanel UI node.'
            var node = UI.of(new JComboBox())

        when : 'We try to pass null as an on change action...'
            node.onChangeComponent(null)

        then : 'An illegal argument exception will be thrown!'
            thrown(IllegalArgumentException)
    }

    def 'On JComboBox change event action lambdas may not be null.'() {
        given : 'We have a simple JPanel UI node.'
            var node = UI.of(new JComboBox())

        when : 'We try to pass null as an on change action...'
            node.onChangeEvent(null)

        then : 'An illegal argument exception will be thrown!'
            thrown(IllegalArgumentException)
    }

    def 'On JSlider change action lambdas may not be null.'() {
        given : 'We have a simple JPanel UI node.'
            var node = UI.of(new JSlider())

        when : 'We try to pass null as an on change action...'
            node.onChange(null)

        then : 'An illegal argument exception will be thrown!'
            thrown(IllegalArgumentException)
    }

    def 'On JSlider change component action lambdas may not be null.'() {
        given : 'We have a simple JPanel UI node.'
            var node = UI.of(new JSlider())

        when : 'We try to pass null as an on change action...'
            node.onChangeComponent(null)

        then : 'An illegal argument exception will be thrown!'
            thrown(IllegalArgumentException)
    }

    def 'On JSlider change event action lambdas may not be null.'() {
        given : 'We have a simple JPanel UI node.'
            var node = UI.of(new JSlider())

        when : 'We try to pass null as an on change action...'
            node.onChangeEvent(null)

        then : 'An illegal argument exception will be thrown!'
            thrown(IllegalArgumentException)
    }
}
