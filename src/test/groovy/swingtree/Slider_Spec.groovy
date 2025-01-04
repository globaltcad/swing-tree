package swingtree

import groovy.transform.CompileDynamic
import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title
import sprouts.From
import sprouts.Var

import javax.swing.JSlider

@Title("Sliders")
@Narrative('''

    Sliders are a common way to allow the user to select a value
    within a range. Swing-Tree provides a way to create sliders
    in a declarative way, allowing you to configure the slider
    and then show it to the user.
    
    Sliders can be vertical or horizontal, and can have a
    minimum and maximum value, as well as a current value.
    You can bind any of these values to properties in your
    application, so that the slider can update itself
    automatically when your application state changes.

''')
@Subject([UIForSlider, UI])
@CompileDynamic
class Slider_Spec extends Specification
{
    def 'Use the `slider(Align)` factory method to build a `JSlider`.'()
    {
        reportInfo """
            The `slider(Align)` factory method returns a builder instance
            which can be used to configure the JSlider instance
            using method chaining.
            Use the "show()" method at the end of your chain to
            show the JSlider to the user.
        """
        given :
            var record = []
        and : 'Then we create UI declaration for the `JSlider` component.'
            var slider =
                    UI.slider(UI.Align.HORIZONTAL)
                    .withMin(0)
                    .withMax(100)
                    .withValue(50)
                    .onChange( it -> record << "value changed to ${it.get().value}" )
                    .get(JSlider)

        expect :
            slider.minimum == 0
            slider.maximum == 100
            slider.value == 50

        when :
            UI.runNow({ slider.value = 75 })

        then :
            record == ["value changed to 75"]
    }

    def 'Bind the current slider state to a double property through one of its factory methods.'()
    {
        reportInfo """
            You can use the `slider(Align, N, N, Var<N>)` factory method
            to bind a number based property to the slider's current value.
            This includes floating point numbers, like `double` or `float`.
            
            When the slider's value changes through user interaction,
            the bound property will be updated
            and conversely, when the property changes, the slider's 
            value will be updated.
        """
        given : 'We create a double based Sprouts property and a list for recording changes.'
            var currentState = Var.of(0.42d)
            var trace = []
            currentState.onChange(From.ALL, it -> trace << "property=${it.currentValue().orElseThrowUnchecked()}" )
        and : 'Then we build a declarative JSlider.'
            var slider =
                    UI.slider(UI.Align.HORIZONTAL, -2d, 2d, currentState)
                    .onChange( it -> trace << "slider=${it.get().value}" )
                    .get(JSlider)
        and : 'We calculate the internal scale based on the min-max range'
            var scale = UIForSlider.PREFERRED_STEPS / 4d
        expect : 'The slider has the correct initial state:'
            slider.minimum == (int)(-2d * scale)
            slider.maximum == (int)(2d * scale)
            slider.value == (int)(0.42d * scale)
        when : 'We change the slider value'
            UI.runNow({ slider.value = (int)(1.5d * scale) })
        then : 'The property is updated'
            trace == ["slider=96", "property=1.5"]
        when : 'We change the property value'
            UI.runNow({
                currentState.set(0.75d)
            })
        then : 'The trace shows the property change.'
            trace == ["slider=96", "property=1.5", "property=0.75"]
        and : 'The slider value is updated'
            slider.value == (int)(0.75d * scale)
    }

    def 'Bind the full slider state to a double property through one of its factory methods.'()
    {
        reportInfo """
            You can use the `slider(Align, Val<N>, Val<N>, Var<N>)` factory method
            to bind min, max and value properties to the slider's current state.
            This works for any `Number` based property, including `double` or `float`.
            
            When the slider's value changes through user interaction,
            the bound property will be updated and conversely, when the property changes,
            the slider's value will be updated.
            
            The min and max range may not be updated by the user, but they can be
            updated programmatically through the bound properties.
        """
        given : 'We create a double based Sprouts property and a list for recording changes.'
            var currentState = Var.of(0.42d)
            var trace = []
            currentState.onChange(From.ALL, it -> trace << "property=${it.currentValue().orElseThrowUnchecked()}" )
        and : 'Two more properties for min and max'
            var min = Var.of(-3d)
            var max = Var.of(4d)
            min.onChange(From.ALL, it -> trace << "min=${it.currentValue().orElseThrowUnchecked()}" )
            max.onChange(From.ALL, it -> trace << "max=${it.currentValue().orElseThrowUnchecked()}" )
        and : 'Then we build a declarative JSlider.'
            var slider =
                    UI.slider(UI.Align.VERTICAL, min, max, currentState)
                    .onChange( it -> trace << "slider=${it.get().value}" )
                    .get(JSlider)
        and : 'We calculate the internal scale based on the min-max range'
            var scale = UIForSlider.PREFERRED_STEPS / 7d
        expect : 'The slider has the correct initial state:'
            slider.minimum == (int)(-3d * scale)
            slider.maximum == (int)(4d * scale)
            slider.value == (int)(0.42d * scale)
        when : 'We change the slider value'
            UI.runNow({ slider.value = (int)(2.5d * scale) })
        then : 'The property is updated'
            trace == ["slider=91", "property=2.48828125"]
        when : 'We change the property value'
            UI.runNow({
                currentState.set(0.75d)
            })
        then : 'The trace shows the property change.'
            trace == ["slider=91", "property=2.48828125", "property=0.75"]

        when : 'We change the min value and recalculate the scale.'
            UI.runNow({
                min.set(-1d)
            })
            scale = UIForSlider.PREFERRED_STEPS / 5d
        then : 'The trace shows the min change.'
            trace == ["slider=91", "property=2.48828125", "property=0.75", "min=-1.0"]
        and : 'The sliders min value is updated'
            slider.minimum == (int)(-1d * scale)
        when : 'We change the max value and recalculate the scale again.'
            UI.runNow({
                max.set(2d)
            })
            scale = UIForSlider.PREFERRED_STEPS / 3d
        then : 'The trace shows the max change.'
            trace == ["slider=91", "property=2.48828125", "property=0.75", "min=-1.0", "max=2.0"]
        and : 'The sliders max value is updated'
            slider.maximum == (int)(2d * scale)
    }
}
