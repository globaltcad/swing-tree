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
    def 'The noise functions produce exactly the same texture they always did.'()
    {
        reportInfo """
            A noise function is a *look*, which means that an optimization of one is only
            legitimate if it is invisible. This checksums a dense, deterministic sample grid
            of every noise type, so any change to what a texture looks like has to be a
            deliberate one: an accidental change fails here instead of in someone's UI.

            The `FOLIAGE` function rejects leaves whose bounding circle cannot contain the
            pixel before it computes their geometry, and the bounding circle is derived from
            the leaf shape constants. Reshaping a leaf without updating that derivation would
            clip leaves - which is precisely what this pins down.
        """
        given : 'A grid of sample coordinates, deliberately fractional so cell borders are crossed.'
            var checksums = [:]
        when : 'We sample every noise function over the very same grid.'
            UI.NoiseType.values().each( type -> {
                long checksum = 1125899906842597L
                for ( int y = 0; y < 128; y++ )
                    for ( int x = 0; x < 128; x++ ) {
                        float value = type.getFractionAt( (float)(x * 0.37f + 11.5f), (float)(y * 0.41f - 7.25f) )
                        checksum = checksum * 31 + Float.floatToIntBits(value)
                    }
                checksums[type.name()] = checksum
            })
        then : 'Each of them yields the texture it is supposed to yield.'
            checksums["CELLS"]                     == -67966932315597973L
            checksums["FABRIC"]                    == 6651072604524390879L
            checksums["GRAINY"]                    == 3354223022173255992L
            checksums["HARD_SPOTS"]                == 2823331731822084069L
            checksums["HARD_TOPOLOGY"]             == 247987333459232641L
            checksums["HAZE"]                      == 2239186621617328239L
            checksums["MANDELBROT"]                == 8809753579310685921L
            checksums["MOSAIC"]                    == -655236708833947473L
            checksums["GEM_STONES"]                == -7962641586648049109L
            checksums["RETRO"]                     == 6426964056061718787L
            checksums["STOCHASTIC"]                == -920677386671702220L
            checksums["SMOOTH_TOPOLOGY"]           == 211827332308579699L
            checksums["SMOOTH_SPOTS"]              == -3752610868464288346L
            checksums["SPIRALS"]                   == 6053336995468003023L
            checksums["TILES"]                     == -190995913331526234L
            checksums["TISSUE"]                    == -1756213074970351884L
            checksums["POND_IN_DRIZZLE"]           == 1061023393333618517L
            checksums["POND_IN_RAIN"]              == -256600488854595485L
            checksums["POND_OF_STRINGS"]           == 8316080144778139320L
            checksums["POND_OF_TANGLED_STRINGS"]   == -6671411409021708099L
            checksums["MARBLE"]                    == 5168887324820059517L
            checksums["WOOD"]                      == -7978856115662748266L
            checksums["PLASMA"]                    == 7615042703136848188L
            checksums["CLOUDS"]                    == -6553113239435585812L
            checksums["CRACKS"]                    == 5275662807912540484L
            checksums["VORTEX"]                    == -8736965031316451205L
            checksums["FLOW"]                      == -2457888643653885012L
            checksums["LIGHTNING"]                 == 754621589127762790L
            checksums["FOLIAGE"]                   == -283787215730463101L
            checksums["FRACTAL"]                   == -6268174681586509741L
            checksums["TURBULENCE"]                == 8998161648512314508L
            checksums["RIDGES"]                    == 6232354040823363669L
            checksums["BRUSHED_METAL"]             == -9208977393114050452L
            checksums["SCRATCHES"]                 == 6365147114900087972L
            checksums["CONCRETE"]                  == 9082020450477414620L
            checksums["PAPER"]                     == 4020258566111844449L
            checksums["SAND"]                      == -6170160574499958155L
            checksums["LEATHER"]                   == -3202349485011404231L
            checksums["DENIM"]                     == -7540474615121196460L
            checksums["BRICKS"]                    == 3870456342082315078L
            checksums["HERRINGBONE"]               == -1464009145850048390L
            checksums["HONEYCOMB"]                 == -8159102606362059881L
            checksums["WEAVE"]                     == -3777716089674158143L
            checksums["HALFTONE"]                  == -4027328770449941288L
            checksums["SCALES"]                    == -2591457897973207577L
            checksums["CIRCUIT"]                   == 6889813408922582408L
            checksums["BUBBLES"]                   == -4184259681698366665L
            checksums["CAMOUFLAGE"]                == -7461863450060363801L
            checksums["CAUSTICS"]                  == -7050665159230981281L
            checksums["FROST"]                     == 3633289682367620901L
            checksums["SMOKE"]                     == -6891208062746504053L
            checksums["STARS"]                     == 9063903995010459341L
            checksums["WAVES"]                     == 6560649675023506545L
    }

    def 'Every noise function is reachable through a noise type constant.'()
    {
        reportInfo """
            `NoiseFunctions` holds the implementations and `UI.NoiseType` is how the style
            API reaches them. A function added to the one but not the other is invisible to
            anyone using the style API, and a constant wired to the wrong function is a
            texture nobody can select. Counting both sides catches the first case.
        """
        given : 'All public noise functions, which are the two-coordinate ones returning a fraction.'
            var noiseFunctions = NoiseFunctions.class.getDeclaredMethods().findAll( method ->
                java.lang.reflect.Modifier.isPublic(method.getModifiers()) &&
                method.getReturnType() == float.class &&
                method.getParameterCount() == 2 &&
                method.getParameterTypes()[0] == float.class &&
                method.getParameterTypes()[1] == float.class
            )
        expect : 'Every one of them has exactly one noise type constant exposing it.'
            UI.NoiseType.values().length == noiseFunctions.size()
    }

    def 'Every noise type produces a texture of its own.'()
    {
        reportInfo """
            Two constants wired to the same function would give the user a choice that does
            not change anything. Here every noise type is sampled over one shared grid and
            the resulting textures are required to be distinct.
        """
        when : 'We reduce each noise type to a checksum of the same sample grid.'
            var textures = UI.NoiseType.values().collect( type -> {
                long checksum = 1125899906842597L
                for ( int y = 0; y < 48; y++ )
                    for ( int x = 0; x < 48; x++ ) {
                        float value = type.getFractionAt( (float)(x * 1.7f - 3.5f), (float)(y * 1.3f + 9.25f) )
                        checksum = checksum * 31 + Float.floatToIntBits(value)
                    }
                return checksum
            })
        then : 'No two of them are the same texture.'
            textures.toSet().size() == textures.size()
    }

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
