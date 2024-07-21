package swingtree

import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title
import swingtree.style.NoiseFunctions

@Title("Noise Functions")
@Narrative('''

    One powerful part of the style API is
    the ability to create noise gradients with
    special noise functions.
    
    These functions can generate interesting
    patterns procedurally simply by converting
    a simple pixel coordinate to a value between 0 and 1.
    
    In this specification we ensure that the noise functions
    behave as expected.

''')
@Subject([UI.NoiseType, NoiseFunctions])
class Noise_Function_Spec extends Specification
{
    def 'The available noise functions produce values in the range between 0 and 1.'()
    {
        reportInfo """
            The `noise` configuration in the style API is really a gradient,
            hence the name "noise gradient". What this means in practise is
            that in order to render the gradient, SwingTree needs to pick
            the correctly interpolated number in an array of colors.
            For this interpolation a number between 0 and 1 tells us where to interpolate.
            The number 0 means that the first color in the array is meant, whereas
            1 refers to the last. Numbers in between are interpolated.
            
            In this test we verify that the noise functions never
            produce values outside that range.
        """
        given :
            var random = new Random(42)
        expect :
            UI.NoiseType.values().every( fun ->
                1_000.every( n -> {
                    var x = (float)( random.nextGaussian() * n )
                    var y = (float)( random.nextGaussian() * n )
                    var fraction = fun.getFractionAt( x, y )
                    return 0 <= fraction && fraction <= 1
                })
            )
    }
}
