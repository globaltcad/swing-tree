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
    def 'Use the `slider(Axis)` factory method to build a `JSlider`.'()
    {
        reportInfo """
            The `slider(Axis)` factory method returns a builder instance
            which can be used to configure the JSlider instance
            using method chaining.
            Use the "show()" method at the end of your chain to
            show the JSlider to the user.
        """
        given :
            var record = []
        and : 'Then we create UI declaration for the `JSlider` component.'
            var slider =
                    UI.slider(UI.Axis.HORIZONTAL)
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
            You can use the `slider(Axis, N, N, Var<N>)` factory method
            to bind a number based property to the slider's current value.
            This includes floating point numbers, like `double` or `float`.

            When the slider's value changes through user interaction,
            the bound property will be updated
            and conversely, when the property changes, the slider's
            value will be updated.

            A `JSlider` only knows whole numbers, so SwingTree translates your
            fractional numbers into a range of whole numbers behind the scenes.
            Which whole numbers those are is SwingTree's own business, so this
            scenario never looks at them. It only looks at where the knob sits
            as a fraction of the way along the slider, which is what the user
            actually sees on screen. The slider is roughly 256 whole numbers
            long, so the knob may be up to 1/256 of the way away from the exact
            position of a number.
        """
        given : 'We create a double based Sprouts property and a list for recording changes.'
            var currentState = Var.of(0.42d)
            var trace = []
            currentState.onChange(From.ALL, it -> trace << "property=${it.currentValue().orElseThrowUnchecked()}" )
        and : 'Then we build a declarative JSlider running from -2 to 2.'
            var slider = UI.slider(UI.Axis.HORIZONTAL, -2d, 2d, currentState).get(JSlider)
        when : 'We measure how far along the slider the knob sits.'
            var knobFraction = (slider.value - slider.minimum) / (double) (slider.maximum - slider.minimum)
        then : 'The value 0.42 is 2.42 away from -2, and the slider is 4 long, so the knob sits 2.42 / 4 = 60.5% of the way along.'
            Math.abs(knobFraction - 0.605d) <= 1d / 256

        when : 'The user moves the knob 87.5% of the way along the slider.'
            UI.runNow({ slider.value = slider.minimum + (int) ((slider.maximum - slider.minimum) * 0.875d) })
        then : 'The property now holds the number at that point: -2 + 0.875 * 4 = 1.5.'
            trace == ["property=1.5"]

        when : 'The application changes the property to 0.75.'
            UI.runNow({ currentState.set(0.75d) })
            knobFraction = (slider.value - slider.minimum) / (double) (slider.maximum - slider.minimum)
        then : 'The trace shows the property change.'
            trace == ["property=1.5", "property=0.75"]
        and : 'The knob follows it to 2.75 / 4 = 68.75% of the way along.'
            Math.abs(knobFraction - 0.6875d) <= 1d / 256
    }

    def 'Bind the full slider state to a double property through one of its factory methods.'()
    {
        reportInfo """
            You can use the `slider(Axis, Val<N>, Val<N>, Var<N>)` factory method
            to bind min, max and value properties to the slider's current state.
            This works for any `Number` based property, including `double` or `float`.

            When the slider's value changes through user interaction,
            the bound property will be updated and conversely, when the property changes,
            the slider's value will be updated.

            The min and max range may not be updated by the user, but they can be
            updated programmatically through the bound properties.

            A `JSlider` only knows whole numbers, so SwingTree translates your
            fractional numbers into a range of whole numbers behind the scenes,
            and it translates them again whenever the min or the max changes.
            This scenario only looks at where the knob sits as a fraction of the
            way along the slider, which is what the user sees on screen. The slider
            is roughly 256 whole numbers long, so the knob may be up to 1/256 of
            the way away from the exact position of a number, and a number written
            back by the slider may be up to 1/256 of the slider's length away from
            the number the user pointed at.
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
        and : 'Then we build a declarative JSlider running from -3 to 4.'
            var slider = UI.slider(UI.Axis.VERTICAL, min, max, currentState).get(JSlider)
        when : 'We measure how far along the slider the knob sits.'
            var knobFraction = (slider.value - slider.minimum) / (double) (slider.maximum - slider.minimum)
        then : 'The value 0.42 is 3.42 away from -3, and the slider is 7 long, so the knob sits 3.42 / 7 = 48.86% of the way along.'
            Math.abs(knobFraction - 3.42d / 7) <= 1d / 256

        when : 'The user moves the knob to where 2.5 is: 5.5 / 7 = 78.57% of the way along.'
            UI.runNow({ slider.value = slider.minimum + Math.round((slider.maximum - slider.minimum) * 5.5d / 7) as int })
        then : 'The property holds a number within 7 / 256 = 0.027 of 2.5.'
            trace.size() == 1
            Math.abs(currentState.get() - 2.5d) <= 7d / 256

        when : 'The application changes the property to 0.75.'
            trace.clear()
            UI.runNow({ currentState.set(0.75d) })
        then : 'The trace shows the property change.'
            trace == ["property=0.75"]

        when : 'The application changes the min to -1, so the slider is now 5 long.'
            UI.runNow({ min.set(-1d) })
            knobFraction = (slider.value - slider.minimum) / (double) (slider.maximum - slider.minimum)
        then : 'The trace shows the min change, and nothing else: the value stays 0.75.'
            trace == ["property=0.75", "min=-1.0"]
        and : 'The knob moves to 1.75 / 5 = 35% of the way along.'
            Math.abs(knobFraction - 0.35d) <= 1d / 256

        when : 'The application changes the max to 2, so the slider is now 3 long.'
            UI.runNow({ max.set(2d) })
            knobFraction = (slider.value - slider.minimum) / (double) (slider.maximum - slider.minimum)
        then : 'The trace shows the max change, and the value is still 0.75.'
            trace == ["property=0.75", "min=-1.0", "max=2.0"]
        and : 'The knob moves to 1.75 / 3 = 58.33% of the way along.'
            Math.abs(knobFraction - 1.75d / 3) <= 1d / 256
    }

    def 'A slider bound to a property writes numbers of the property\'s own type back into it.'(
        Class<?> type, Var<? extends Number> value, Number min, Number max
    ) {
        reportInfo """
            The `slider(Axis, N, N, Var<N>)` factory method accepts any of the number
            types `Integer`, `Long`, `Short`, `Byte`, `Float` and `Double`.
            Whatever type you choose for your property, the slider writes numbers of
            exactly that type back into it. A `Var<Float>` which suddenly held a
            `Double` would break the application code reading it with a
            `ClassCastException`, far away from the slider that caused it.

            In this scenario the user drags the knob to both ends of the slider,
            and the property has to hold the exact minimum and maximum afterwards,
            as numbers of its own type.
        """
        given : 'A slider bound to the property.'
            var slider = UI.slider(UI.Axis.HORIZONTAL, min, max, value).get(JSlider)

        when : 'The user drags the knob all the way to the end.'
            UI.runNow({ slider.value = slider.maximum })
        then : 'The property holds the maximum, as a number of its own type.'
            value.get() == max
            type.isInstance(value.get())

        when : 'The user drags the knob all the way to the start.'
            UI.runNow({ slider.value = slider.minimum })
        then : 'The property holds the minimum, as a number of its own type.'
            value.get() == min
            type.isInstance(value.get())

        where :
            type    | value                   | min             | max
            Integer | Var.of(5)               | 0               | 10
            Long    | Var.of(5L)              | 0L              | 10L
            Short   | Var.of((short) 5)       | (short) 0       | (short) 10
            Byte    | Var.of((byte) 5)        | (byte) 0        | (byte) 10
            Float   | Var.of(0.5f)            | 0f              | 1f
            Double  | Var.of(0.5d)            | -0.25d          | 1.75d
    }

    def 'Moving the slider after dynamic min/max changes keeps the bound value within [min, max].'()
    {
        reportInfo """
            This is a regression test for a bug where the slider's internal
            scaling used for non-whole-number bindings could become stale when
            one of the `min` or `max` properties changed dynamically.
            When the user then moved the slider knob, the resulting value
            written into the bound property could fall outside the current
            `[min, max]` range.
        """
        given : 'A bidirectional slider bound to three double properties.'
            var min = Var.of(5d)
            var max = Var.of(10d)
            var value = Var.of(7.5d)
            var slider =
                    UI.slider(UI.Axis.HORIZONTAL, min, max, value)
                      .get(JSlider)
        and : 'We compute the slider integer range after dropping the max.'
            var newMax = 6d
        when : 'The application shrinks the max property significantly.'
            UI.runNow({ max.set(newMax) })
        and : 'The user then drags the slider knob to a position near its minimum.'
            UI.runNow({ slider.value = slider.minimum + (slider.maximum - slider.minimum) / 4 as int })
        then : 'The bound value stays within the new [min, max] range.'
            value.get() >= min.get()
            value.get() <= max.get()
        when : 'The user drags the slider knob all the way to the top.'
            UI.runNow({ slider.value = slider.maximum })
        then : 'The bound value equals the user-defined maximum (within rounding).'
            value.get() == max.get()
        when : 'The user drags the slider knob all the way to the bottom.'
            UI.runNow({ slider.value = slider.minimum })
        then : 'The bound value equals the user-defined minimum (within rounding).'
            value.get() == min.get()
    }
}
