package swingtree.animations

import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title
import swingtree.UI
import swingtree.animation.Animate
import swingtree.animation.Animation
import swingtree.animation.AnimationState
import swingtree.animation.Schedule

import java.awt.Color
import java.util.concurrent.TimeUnit

@Title("Animations")
@Narrative('''

    Animations are a great way to make your application more interactive and more fun to use.

''')
@Subject([Animate, UI, Schedule])
class Animations_Spec extends Specification
{
    def 'Use the API exposed by "UI.schedule(..)" to schedule animations'()
    {
        reportInfo """
            Swing-Tree has a built in animation scheduler that can be used to schedule 
            implementations of the "Animation" interface. 
        """
        given : 'A list which we will use to store the animation states.'
            var progressValues = [0:[],1:[],2:[]]

        when : 'We schedule an animation that will run 3 times and has a duration of 0.1 seconds.'
            UI.schedule(0.1, TimeUnit.SECONDS)
                .asLongAs({ it.currentIteration() < 3 })
                .go({
                    progressValues[(int)it.currentIteration()] << it.progress()
                })
        and : 'We wait for the animation to finish'
            TimeUnit.MILLISECONDS.sleep(500)
        then : 'The animation has been executed 3 times'
            progressValues.keySet() as List == [0, 1, 2]
        and : 'The progress values are always between 0 and 1'
            progressValues[0].every { it >= 0 && it <= 1 }
            progressValues[1].every { it >= 0 && it <= 1 }
            progressValues[2].every { it >= 0 && it <= 1 }
        and : 'The progress values are always increasing'
            progressValues[0] == new ArrayList(progressValues[0]).sort()
            progressValues[1] == new ArrayList(progressValues[1]).sort()
            progressValues[2] == new ArrayList(progressValues[2]).sort()
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
                        it.animate(0.05, TimeUnit.SECONDS)
                            .asLongAs({ it.currentIteration() < 4 })
                            .go(state -> {
                                if ( !iterations.contains(state.currentIteration()) )
                                    iterations << state.currentIteration()
                                progresses    << state.progress()
                                cycles        << state.cycle()
                                cyclesPlus42  << state.cyclePlus(0.42)
                                cyclesMinus42 << state.cycleMinus(0.42)
                            })
                    })
                    .getComponent()
        and :
            TimeUnit.MILLISECONDS.sleep(200)
        then : 'Initially the animation has not been executed yet.'
            iterations == []
        when : 'We simulate a click on the button'
            button.doClick()

        and : 'We wait for the animation to finish'
            TimeUnit.MILLISECONDS.sleep(200)
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
            UI.schedule(1, TimeUnit.MILLISECONDS)
              .asLongAs({ it.currentIteration() < 4 })
              .go(new Animation() {
                  @Override void run(AnimationState state) {}
                  @Override void finish(AnimationState state) { wasFinished++ }
              })
        and :
            TimeUnit.MILLISECONDS.sleep(100)

        then :
            wasFinished == 1
    }

    def 'Animate the color of a label when it is clicked.'()
    {
        given : 'A JLabel that will animate its color when it is clicked.'
            var label =
                    UI.label("Some text! :)")
                    .onMouseClick(it -> {
                        it.animateOnce(1, TimeUnit.SECONDS, state ->{
                            float highlight = (float) (1f - (float) state.progress())
                            it.component.setForeground(new Color(highlight, 1, highlight))
                          })
                    })
                    .getComponent()
        when :
            TimeUnit.MILLISECONDS.sleep(200)
        then :
            label.getForeground() == new Color(51, 51, 51)
        when :
            label.dispatchEvent(new java.awt.event.MouseEvent(label, java.awt.event.MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(), 0, 0, 0, 1, false))
        and :
            TimeUnit.SECONDS.sleep(2)
        then :
            label.getForeground().getGreen() == 255
            label.getForeground().getRed() < 10
            label.getForeground().getBlue() < 10
    }

}