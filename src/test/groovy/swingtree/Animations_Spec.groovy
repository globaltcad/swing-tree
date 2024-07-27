package swingtree

import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title
import swingtree.animation.Stride
import swingtree.threading.EventProcessor
import swingtree.animation.Animator
import swingtree.animation.Animation
import swingtree.animation.AnimationStatus
import swingtree.animation.LifeTime
import utility.Wait

import javax.swing.JButton
import javax.swing.JLabel
import java.awt.Color
import java.awt.event.MouseEvent
import java.util.concurrent.TimeUnit

@Title("Animations")
@Narrative('''

    Animations are a great way to improve the UX of your application.
    Swing-Tree has a built in animation scheduler that can execute animations 
    given that you have at least specified the duration of the animation.
    Internally the animation scheduler is a `Timer` that will regularly update
    your animations and then remove them from the scheduler once they are finished.

''')
@Subject([Animator, UI, LifeTime])
class Animations_Spec extends Specification
{
    def setupSpec() {
        SwingTree.get().setEventProcessor(EventProcessor.COUPLED)
        // This is so that the test thread is also allowed to perform UI operations
    }

    def 'Use the API exposed by `UI.animateFor(..)` to schedule animations'()
    {
        reportInfo """
            Swing-Tree has a built in animation scheduler that can be used to schedule 
            implementations of the "Animation" interface. 
        """
        given : 'A list which we will use to store the animation states.'
            var progressValues = [0:[],1:[],2:[]]

        when : 'We schedule an animation that will run 3 times and has a duration of 0.1 seconds.'
            UI.animateFor(0.2, TimeUnit.SECONDS)
                .asLongAs({ it.repeats() < 3 })
                .go({
                    progressValues[(int)it.repeats()] << it.progress()
                })
        then : 'We wait for each animation to finish with a full progress:'
            Wait.until({progressValues[2].contains(1d)},2_500)
        and : """
            We confirm that the animation has been executed fully!
            The animation scheduler always ensures that the last animation run is
            executed with a progress value of 1.0. 
            This is to ensure that the animation is always finished predictably.
        """
            progressValues[2].last() == 1
        and : 'The progress values are always between 0 and 1'
            progressValues[0].every { it >= 0 && it <= 1 }
            progressValues[1].every { it >= 0 && it <= 1 }
            progressValues[2].every { it >= 0 && it <= 1 }
        and : 'The progress values are always increasing'
            progressValues[0] == new ArrayList(progressValues[0]).sort()
            progressValues[1] == new ArrayList(progressValues[1]).sort()
            progressValues[2] == new ArrayList(progressValues[2]).sort()
    }

    def 'Use the API exposed by `UI.animateFor(..)` to schedule regressive animations (whose progress goes from 1 to 0)'()
    {
        reportInfo """
            Swing-Tree has a built in animation scheduler that can be used to schedule 
            implementations of the "Animation" interface. This scheduler can also
            be used to schedule animations that go from 1 to 0.
        """
        given : 'A list which we will use to store the animation states.'
            var progressValues = [0:[],1:[],2:[]]

        when : 'We schedule an animation that will run 3 times and has a duration of 0.1 seconds.'
            UI.animateFor(0.1, TimeUnit.SECONDS, Stride.REGRESSIVE )
                .asLongAs({ it.repeats() < 3 })
                .go({
                    progressValues[(int)it.repeats()] << it.progress()
                })
        then : 'We wait for each animation to finish with a full regress:'
            Wait.until({progressValues[2].contains(0d)},2_500)
        and : """
            We confirm that the animation has been executed fully!
            The animation scheduler always ensures that in case of a regressive animation,
            the last animation run is always executed with a progress value of 0.0. 
            This is to ensure that the animation is always terminated predictably.
        """
            progressValues[2].last() == 0
        and : 'The progress values are always between 0 and 1'
            progressValues[0].every { it >= 0 && it <= 1 }
            progressValues[1].every { it >= 0 && it <= 1 }
            progressValues[2].every { it >= 0 && it <= 1 }
        and : 'The progress values are always decreasing'
            progressValues[0] == new ArrayList(progressValues[0]).sort().reverse()
            progressValues[1] == new ArrayList(progressValues[1]).sort().reverse()
            progressValues[2] == new ArrayList(progressValues[2]).sort().reverse()
    }

    def 'The event delegation object of a user event can be used to register animations.'()
    {
        reportInfo """
            This is useful if you want to animate a component when it is clicked,
            or when the mouse enters or leaves it.
            This specification shows you how to register an animation inside
            of an action listener and also shows you what kind of information
            you can get from the "AnimationState" object
            inside of your "Animation" implementation.
        """
        given :
            var iterations = [] // An iteration is when the progress goes from 0 to 1
            var progresses = [] // The progress of an animation iteration is always between 0 and 1
            var cycles = []     // The cycle value reaches 1 when the progress reaches 0.5 and then decreases to 0
            var cyclesPlus42 = [] // The cycle value can be offset by any value
            var cyclesMinus42 = [] // ...
        when :
            var button =
                    UI.button("Click me! Or don't.")
                    .onClick({
                        it.animateFor(0.2, TimeUnit.SECONDS)
                            .asLongAs({ it.repeats() < 4 })
                            .go(state -> {
                                println state
                                if ( !iterations.contains(state.repeats()) )
                                    iterations << state.repeats()
                                progresses    << state.progress()
                                cycles        << state.cycle()
                                cyclesPlus42  << state.cyclePlus(0.42)
                                cyclesMinus42 << state.cycleMinus(0.42)
                            })
                    })
                    .get(JButton)
        then : 'Initially the animation has not been executed yet.'
            iterations == []

        when : 'We simulate a click on the button'
            UI.runNow( () -> button.doClick() )
        and : 'We wait for the animation to finish'
            Wait.until({ iterations.size() == 4 && progresses.last() == 1 },3_500)
            Thread.sleep(100)
            UI.sync()
        then : 'The animation has been completed 4 times.'
            iterations == [0, 1, 2, 3]
        and : 'The progress and cycle values are always between 0 and 1'
            progresses.every { it >= 0 && it <= 1 }
            cycles.every { it >= 0 && it <= 1 }
            cyclesPlus42.every { it >= 0 && it <= 1 }
            cyclesMinus42.every { it >= 0 && it <= 1 }
        and : 'The cycles are calculated based on the progress like so:'
            cycles == progresses.collect({ 1 - Math.abs(2 * it - 1) })
            cyclesPlus42 == progresses.collect({ 1 - Math.abs(2 * ((it+0.42d)%1) - 1) })
            cyclesMinus42 == progresses.collect({
                                    double progress = ( it - 0.42 ) % 1;
                                    if ( progress < 0 ) progress += 1;
                                    return 1 - Math.abs(2 * progress - 1);
                                })
    }


    def 'Implement the "finish" method in your animation to ensure that it is called at least once.'()
    {
        reportInfo """
            Swing-Tree has a built in animation scheduler, which is essentially a
            "Timer" that can be used to schedule implementations of the "Animation" interface.
            However this timer has a fixed resolution of 16 milliseconds so that
            the animation is not executed too often but often enough to be smooth (60 fps).
            
            This means that if you schedule an animation that has a duration of 1 millisecond
            there is a chance that the animation will not be executed at all if it 
            is scheduled in between two timer ticks.
            
            If you want to ensure that your animation is executed at least once you can
            implement the "finish" method in your animation. This method will 
            always be called before the animation is removed from the scheduler.
        """
        given :
            int wasFinished = 0
        when :
            UI.animateFor(1, TimeUnit.MILLISECONDS)
              .asLongAs({ it.repeats() < 4 })
              .go(new Animation() {
                  @Override void run(AnimationStatus status) {}
                  @Override void finish(AnimationStatus status) { wasFinished++ }
              })
        and :
            Wait.until({ wasFinished > 0 },2_500)

        then :
            wasFinished == 1
    }

    def 'Animate the color of a label when it is clicked.'()
    {
        given : 'A simple list used as a trace for the animation runs.'
            var trace = []
        and : 'A JLabel that will animate its color when it is clicked.'
            var label =
                    UI.label("Some text! :)")
                    .onMouseClick(it -> {
                        it.animateFor(1, TimeUnit.SECONDS, state ->{
                            float highlight = (float) (1f - (float) state.progress())
                            it.component.setForeground(new Color(highlight, 1, highlight))
                            trace << state.progress()
                          })
                    })
                    .get(JLabel)
        expect : 'The label text is (almost) black.'
            label.getForeground().getRed() < 60
            label.getForeground().getGreen() < 60
            label.getForeground().getBlue() < 60
        when :
            label.dispatchEvent(new MouseEvent(label, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(), 0, 0, 0, 1, false))
        and :
            Wait.until({ trace.contains(1d) },2_500)
        then :
            label.getForeground().getGreen() == 255
            label.getForeground().getRed() < 10
            label.getForeground().getBlue() < 10
    }

}