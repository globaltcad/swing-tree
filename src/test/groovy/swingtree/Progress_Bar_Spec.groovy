package swingtree

import groovy.transform.CompileDynamic
import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title
import sprouts.Val
import sprouts.Var
import swingtree.threading.EventProcessor
import utility.SwingTreeTestConfigurator

import javax.swing.JProgressBar
import javax.swing.SwingConstants

@Title("Progress Bars")
@Narrative('''

    Progress bars are a common way to display the progress
    of a long-running task to the user.
    SwingTree allows you to create and configure progress bars
    declaratively, and to bind their state to properties
    in your view model, so that the UI updates itself
    automatically when the state of your application changes.

    The progress of a `JProgressBar` is at its core modelled
    as an integer value between a minimum and a maximum value.
    SwingTree exposes this low-level state directly, but it also
    allows you to think in terms of a percentage based `progress`,
    a simple double value between 0 and 1, which is internally
    translated to the min-max based value of the underlying
    Swing component.

''')
@Subject([UIForProgressBar, UI, UIFactoryMethods])
@CompileDynamic
class Progress_Bar_Spec extends Specification
{
    def setupSpec() {
        SwingTree.initializeUsing(SwingTreeTestConfigurator.get())
        SwingTree.get().setEventProcessor(EventProcessor.COUPLED_STRICT)
        // In this specification we are using the strict event processor
        // which will throw exceptions if we try to perform UI operations in the test thread.
    }

    def cleanupSpec() {
        SwingTree.clear()
    }

    def 'Use the `UI.progressBar()` factory method to build a `JProgressBar` fluently.'()
    {
        reportInfo """
            The most basic progress bar factory method is `UI.progressBar()`,
            which creates a builder wrapping a plain `JProgressBar` without
            any special configuration.
            You can then use methods like `withMin(int)`, `withMax(int)` and
            `withValue(int)` to configure the component through method chaining,
            and finally call `get(JProgressBar)` to receive the fully built component.
        """
        given : 'We declare a progress bar with a custom range and initial value.'
            var ui =
                    UI.progressBar()
                    .withMin(10)
                    .withMax(90)
                    .withValue(30)
        and : 'Then we build the actual `JProgressBar` component:'
            var progressBar = ui.get(JProgressBar)

        expect : 'The component has the state we declared.'
            progressBar.minimum == 10
            progressBar.maximum == 90
            progressBar.value   == 30
    }

    def 'The `progressBar(int, int)` and `progressBar(int, int, int)` factories initialize the range and value.'()
    {
        reportInfo """
            Instead of configuring the range of the progress bar through
            the `withMin(int)` and `withMax(int)` methods, you can also
            pass the minimum and maximum values directly to the factory method,
            and optionally also the initial value.
        """
        given : 'We create one progress bar with a range and another one with a range and a value.'
            var progressBar1 = UI.progressBar(0, 1000).get(JProgressBar)
            var progressBar2 = UI.progressBar(-50, 50, 25).get(JProgressBar)

        expect : 'The first progress bar has the expected range.'
            progressBar1.minimum == 0
            progressBar1.maximum == 1000
        and : 'The second one has the expected range as well as the expected initial value.'
            progressBar2.minimum == -50
            progressBar2.maximum == 50
            progressBar2.value   == 25
    }

    def 'Use `withProgress(double)` to set the value of the bar as a percentage of its range.'()
    {
        reportInfo """
            Often you do not want to think in terms of the raw integer value
            of the progress bar, but rather in terms of a percentage,
            so a value between 0 and 1, where 0 represents 0% progress
            and 1 represents 100% progress.
            The `withProgress(double)` method does exactly that, it converts
            the given progress value to an integer value between the minimum
            and maximum values of the progress bar.

            Note that the conversion happens based on the min-max range
            declared *before* the `withProgress(double)` call, so make sure
            to declare the range first!
        """
        given : 'We declare a progress bar with a custom range and a progress of 50%.'
            var progressBar =
                    UI.progressBar()
                    .withMin(200)
                    .withMax(300)
                    .withProgress(0.5)
                    .get(JProgressBar)

        expect : 'The value of the progress bar is exactly in the middle of its range.'
            progressBar.value == 250

        and : 'A progress of 0 or 1 maps to the minimum and maximum of the range respectively:'
            UI.progressBar(200, 300).withProgress(0).get(JProgressBar).value == 200
            UI.progressBar(200, 300).withProgress(1).get(JProgressBar).value == 300
    }

    def 'A progress value outside of the 0 to 1 range is rejected eagerly with an exception.'()
    {
        reportInfo """
            The `withProgress(double)` method models progress as a percentage,
            so any value below 0 or above 1 is nonsensical.
            Instead of silently clamping such values, SwingTree fails fast
            and throws an `IllegalArgumentException` immediately when the
            UI is declared, long before the component is even built.
            This way programming errors surface as close
            to their source as possible.
        """
        when : 'We try to declare a progress bar with more than 100% progress...'
            UI.progressBar().withProgress(1.5)
        then : '...the declaration itself blows up.'
            thrown(IllegalArgumentException)

        when : 'The same is true for negative progress values.'
            UI.progressBar().withProgress(-0.01)
        then :
            thrown(IllegalArgumentException)
    }

    def 'Bind an integer property to the value of a progress bar to keep the two in sync.'()
    {
        reportInfo """
            In a typical MVVM application, the progress of a background task
            lives in the view model in the form of a property.
            The `progressBar(int, int, Val<Integer>)` factory method
            binds such an integer property to the value of the progress bar,
            so whenever the property changes, the progress bar is updated
            automatically.
        """
        given : 'An integer property, modelling the progress state of the view model.'
            var progress = Var.of(0)
        and : 'A progress bar bound to the property.'
            var progressBar = UI.progressBar(0, 100, progress).get(JProgressBar)

        expect : 'The progress bar starts out with the initial value of the property.'
            progressBar.value == 0

        when : 'We update the property, as a background task in a view model would.'
            UI.runNow({ progress.set(42) })
        then : 'The progress bar follows suit.'
            progressBar.value == 42

        when : 'The task completes...'
            UI.runNow({ progress.set(100) })
        then : '...and so does the progress bar.'
            progressBar.value == 100
    }

    def 'Bind a double property to the progress of a progress bar, expressed as a percentage.'()
    {
        reportInfo """
            The `withProgress(Val<Double>)` method is the dynamic sibling
            of `withProgress(double)`. It binds a double property, wrapping
            a value between 0 and 1, to the progress bar, so that whenever
            the property changes, the value of the progress bar is recalculated
            relative to its current min-max range.
        """
        given : 'A double property modelling percentage based progress.'
            var progress = Var.of(0.25d)
        and : 'A progress bar with a custom range, bound to the property.'
            var progressBar =
                    UI.progressBar()
                    .withMin(0)
                    .withMax(200)
                    .withProgress(progress)
                    .get(JProgressBar)

        expect : 'The initial value of the progress bar is 25% of the 0 to 200 range.'
            progressBar.value == 50

        when : 'The view model advances the progress to 75%.'
            UI.runNow({ progress.set(0.75d) })
        then : 'The progress bar value is again calculated relative to the range.'
            progressBar.value == 150
    }

    def 'Choose between a horizontal and a vertical progress bar using the `UI.Align` enum.'()
    {
        reportInfo """
            The `withOrientation(UI.Align)` method, as well as various
            alignment accepting factory methods, allow you to choose between
            a horizontal and a vertical progress bar.
            The `UI.Align` enum is SwingTree's type safe alternative to the
            integer constants used by `JProgressBar.setOrientation(int)`.
        """
        given : 'A vertical and a horizontal progress bar.'
            var vertical   = UI.progressBar(UI.Align.VERTICAL, 0, 10).get(JProgressBar)
            var horizontal = UI.progressBar().withOrientation(UI.Align.HORIZONTAL).get(JProgressBar)

        expect : 'The orientations of the underlying Swing components match the declarations.'
            vertical.orientation   == SwingConstants.VERTICAL
            horizontal.orientation == SwingConstants.HORIZONTAL
    }

    def 'The `progressBar(Align, double)` factory gives you a percentage based bar with a default range.'()
    {
        reportInfo """
            For the common case of a simple percentage based progress bar,
            the `progressBar(UI.Align, double)` factory method configures
            a default range of 0 to 100 for you, so the resulting integer
            value of the bar conveniently corresponds to the progress
            in percent.
        """
        given :
            var progressBar = UI.progressBar(UI.Align.HORIZONTAL, 0.5).get(JProgressBar)

        expect : 'The default range is 0 to 100, and 50% progress maps to a value of 50.'
            progressBar.minimum == 0
            progressBar.maximum == 100
            progressBar.value   == 50
    }

    def 'Bind both the orientation and the progress of a progress bar to properties.'()
    {
        reportInfo """
            The `progressBar(Val<UI.Align>, Val<Double>)` factory method
            creates a fully view model driven progress bar whose orientation
            and percentage based progress are both bound to properties.
            When either property changes, the component updates accordingly.
        """
        given : 'Two properties, one for the alignment and one for the progress.'
            var alignment = Var.of(UI.Align.HORIZONTAL)
            var progress  = Var.of(0.1d)
        and : 'A progress bar bound to both of them.'
            var progressBar = UI.progressBar(alignment, progress).get(JProgressBar)

        expect : 'Initially, the component reflects both property values.'
            progressBar.orientation == SwingConstants.HORIZONTAL
            progressBar.value       == 10

        when : 'We flip the alignment and advance the progress in the view model.'
            UI.runNow({
                alignment.set(UI.Align.VERTICAL)
                progress.set(0.9d)
            })
        then : 'The progress bar reflects the new state.'
            progressBar.orientation == SwingConstants.VERTICAL
            progressBar.value       == 90
    }

    def 'The minimum and maximum of a progress bar may also be bound to properties.'()
    {
        reportInfo """
            Not only the value, but also the range of a progress bar
            can change over the lifetime of an application, think of
            a download whose total size is only known after a server
            response arrived. The `withMin(Val<Integer>)` and
            `withMax(Val<Integer>)` methods bind integer properties
            to the range of the progress bar.
        """
        given : 'Two integer properties modelling a dynamic range.'
            var min = Var.of(0)
            var max = Var.of(10)
        and : 'A progress bar whose range is bound to the two properties.'
            var progressBar =
                    UI.progressBar()
                    .withMin(min)
                    .withMax(max)
                    .get(JProgressBar)

        expect : 'The initial range comes from the properties.'
            progressBar.minimum == 0
            progressBar.maximum == 10

        when : 'The view model learns about the actual size of the task at hand.'
            UI.runNow({
                min.set(100)
                max.set(1_000)
            })
        then : 'The progress bar range was updated accordingly.'
            progressBar.minimum == 100
            progressBar.maximum == 1_000
    }

    def 'SwingTree rejects null properties eagerly when declaring a progress bar.'()
    {
        reportInfo """
            All of the property based builder methods perform null checks
            on their arguments as soon as they are invoked. So passing a
            null property, or a nullable property, to a progress bar
            declaration fails immediately with an exception instead of
            causing undefined behavior later on.
        """
        when : 'We try to bind a null reference as the value property.'
            UI.progressBar().withValue((Val<Integer>) null)
        then :
            thrown(IllegalArgumentException)

        when : 'We try to bind a property which allows null values in it.'
            UI.progressBar().withProgress(Var.ofNullable(Double, null))
        then : 'This is also rejected, because there is no meaningful way to display "null progress".'
            thrown(IllegalArgumentException)
    }
}
