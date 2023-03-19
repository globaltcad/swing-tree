package swingtree.animations

import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title
import swingtree.UI
import swingtree.animation.Animate
import swingtree.animation.Schedule

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
                .run({
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
        given :
            var iterations = []
            var progressAndCycleValues = []
        when :
            var button =
                    UI.button()
                    .onClick({
                        it.animate(0.05, TimeUnit.SECONDS)
                            .asLongAs({ it.currentIteration() < 4 })
                            .run({
                                if ( !iterations.contains(it.currentIteration()) )
                                    iterations << it.currentIteration()
                                progressAndCycleValues << [
                                        it.progress(),
                                        it.cycle(),
                                        it.cyclePlus(0.42),
                                        it.cycleMinus(0.42)
                                    ]
                            })
                    })
                    .getComponent()
        and :
            TimeUnit.MILLISECONDS.sleep(200)
        then :
            iterations == []
        and : 'The progress and cycle values are always between 0 and 1'
            progressAndCycleValues.every { it >= 0 && it <= 1 }

        when : 'We simulate a click on the button'
            button.doClick()

        and :
            TimeUnit.MILLISECONDS.sleep(200)
        then :
            iterations == [0, 1, 2, 3]
    }
}