package swingtree

import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title

import java.awt.*

@Title("Yet Another Color")
@Narrative('''

    The age-old `java.awt.Color` class is a bit limited in what it can do.
    Not only are the color constants it has to offer rather limited,
    many of them are also inconsistent with the 
    naming and RGB values of modern color palettes.
    The `java.awt.Color.GREEN` constant for example has an RGB value of 
    `(0, 255, 0)` which is considered "lime" in modern color palettes.
    
    Furthermore, there is a lack of useful wither methods on the `Color` class
    that are expected from a modern value based type, like in this case for example
    `withRed(double)`, `withGreen(double)`, `withBlue(double)`, `withAlpha(double)`
    or `withBrightness(double)`, `withSaturation(double)`, `withHue(double)`.
    
    SwingTree addresses these shortcomings by providing a custom `Color` type
    in the `UI` namespace which is a subclass of `java.awt.Color` and has a number
    of useful methods to manipulate colors in both the RGB and HSB color spaces.  

''')
@Subject([UI.Color])
class Color_Spec extends Specification
{

    def 'Use the "brighterBy(double)" on a SwingTree color to brighten the color according to the HSB color space.'(
            UI.Color colorIn, double factor, Color brighter
    ) {
        reportInfo """
            The colors in SwingTree are modelled using a custom `Color` type which for 
            compatibility reasons is a subclass of java.awt.Color.
            Among many other useful methods, the custom color type has a method called
            `brighterBy(double)` which will brighten the color according to the HSB color space
            (Hue, Saturation, Brightness).
        """
        expect :
            colorIn.brighterBy(factor) == brighter
        where :
            colorIn             |  factor  ||  brighter
            UI.Color.ALICEBLUE  |   0.7    || new Color(240,248,255)
            UI.Color.BEIGE      |   0.6    || new Color(255,255,229)
            UI.Color.BLACK      |   0.4    || new Color(21,21,21)
            UI.Color.DARKGREEN  |   0.3    || new Color(0,143,0)
            UI.Color.HONEYDEW   |   0.8    || new Color(240,255,240)
            UI.Color.INDIANRED  |   0.3    || new Color(255,114,114)
            UI.Color.WHITE      |   0.1    || new Color(255,255,255)

            UI.Color.ALICEBLUE  |   0.0    || UI.Color.ALICEBLUE
            UI.Color.BEIGE      |   0.0    || UI.Color.BEIGE
            UI.Color.BLACK      |   0.0    || UI.Color.BLACK
            UI.Color.DARKGREEN  |   0.0    || UI.Color.DARKGREEN
            UI.Color.HONEYDEW   |   0.0    || UI.Color.HONEYDEW
            UI.Color.INDIANRED  |   0.0    || UI.Color.INDIANRED
            UI.Color.WHITE      |   0.0    || UI.Color.WHITE
    }

    def 'Use the "darkerBy(double)" on a SwingTree color to darken the color according to the HSB color space.'(
            UI.Color colorIn, double factor, Color darker
    ) {
        reportInfo """
            The colors in SwingTree are modelled using a custom `Color` type which for 
            compatibility reasons is a subclass of java.awt.Color.
            Among many other useful methods, the custom color type has a method called
            `darkerBy(double)` which will darken the color according to the brightness in the 
            HSB color space (Hue, Saturation, Brightness).
        """
        expect :
            colorIn.darkerBy(factor) == darker
        where :
            colorIn                |  factor  ||  darker
            UI.Color.DEEPSKYBLUE   |   0.3    || new Color(0,134,179)
            UI.Color.CORAL         |   0.4    || new Color(153,76,48)
            UI.Color.LAVENDERBLUSH |   0.6    || new Color(102,96,98)
            UI.Color.TAN           |   0.7    || new Color(63,54,42)
            UI.Color.LINEN         |   0.2    || new Color(200,192,184)
            UI.Color.WHITE         |   0.7    || new Color(77,77,77)
            UI.Color.BLACK         |   0.9    || new Color(0,0,0)

            UI.Color.DEEPSKYBLUE   |   0.0    || UI.Color.DEEPSKYBLUE
            UI.Color.CORAL         |   0.0    || UI.Color.CORAL
            UI.Color.LAVENDERBLUSH |   0.0    || UI.Color.LAVENDERBLUSH
            UI.Color.TAN           |   0.0    || UI.Color.TAN
            UI.Color.LINEN         |   0.0    || UI.Color.LINEN
            UI.Color.WHITE         |   0.0    || UI.Color.WHITE
            UI.Color.BLACK         |   0.0    || UI.Color.BLACK
    }

    def 'Use "saturateBy(double)" on a SwingTree color to increase the saturation of the color according to the HSB color space.'(
            UI.Color colorIn, double factor, Color saturated
    ) {
        reportInfo """
            The colors in SwingTree are modelled using a custom `Color` type which for 
            compatibility reasons is a subclass of java.awt.Color.
            Among many other useful methods, the custom color type has a method called
            `saturateBy(double)` which will increase the saturation of the color according to the HSB color space
            (Hue, Saturation, Brightness).
        """
        expect :
            colorIn.saturateBy(factor) == saturated
        where :
            colorIn                |  factor  ||  saturated
            UI.Color.DEEPSKYBLUE   |   0.7    || new Color(0,191,255)
            UI.Color.CORAL         |   0.6    || new Color(255,68,0)
            UI.Color.LAVENDERBLUSH |   0.4    || new Color(255,230,238)
            UI.Color.TAN           |   0.3    || new Color(210,167,110)
            UI.Color.LINEN         |   0.8    || new Color(250,200,150)
            UI.Color.WHITE         |   0.3    || new Color(255,255,255)
            UI.Color.BLACK         |   0.1    || new Color(0,0,0)

            UI.Color.DEEPSKYBLUE   |   0.0    || UI.Color.DEEPSKYBLUE
            UI.Color.CORAL         |   0.0    || UI.Color.CORAL
            UI.Color.LAVENDERBLUSH |   0.0    || UI.Color.LAVENDERBLUSH
            UI.Color.TAN           |   0.0    || UI.Color.TAN
            UI.Color.LINEN         |   0.0    || UI.Color.LINEN
            UI.Color.WHITE         |   0.0    || UI.Color.WHITE
            UI.Color.BLACK         |   0.0    || UI.Color.BLACK
    }

    def 'Use "desaturateBy(double)" on a SwingTree color to decrease the saturation of the color according to the HSB color space.'(
            UI.Color colorIn, double factor, Color saturated
    ) {
        reportInfo """
            The colors in SwingTree are modelled using a custom `Color` type which for
            compatibility reasons is a subclass of java.awt.Color.
            Among many other useful methods, the custom color type has a method called
            `desaturateBy(double)` which will decrease the saturation of the color according to the HSB color space
            (Hue, Saturation, Brightness).
        """
        expect :
            colorIn.desaturateBy(factor) == saturated
        where :
            colorIn                |  factor  ||  saturated
            UI.Color.DEEPSKYBLUE   |   0.3    || new Color(77,210,255)
            UI.Color.CORAL         |   0.4    || new Color(255,178,150)
            UI.Color.LAVENDERBLUSH |   0.6    || new Color(255,249,251)
            UI.Color.TAN           |   0.7    || new Color(210,201,189)
            UI.Color.LINEN         |   0.2    || new Color(250,242,234)
            UI.Color.WHITE         |   0.7    || new Color(255,255,255)
            UI.Color.BLACK         |   0.9    || new Color(0,0,0)

            UI.Color.DEEPSKYBLUE   |   0.0    || UI.Color.DEEPSKYBLUE
            UI.Color.CORAL         |   0.0    || UI.Color.CORAL
            UI.Color.LAVENDERBLUSH |   0.0    || UI.Color.LAVENDERBLUSH
            UI.Color.TAN           |   0.0    || UI.Color.TAN
            UI.Color.LINEN         |   0.0    || UI.Color.LINEN
            UI.Color.WHITE         |   0.0    || UI.Color.WHITE
            UI.Color.BLACK         |   0.0    || UI.Color.BLACK
    }

    def 'Use "blend(Color, double)" to linearly interpolate between two colors in the sRGB color space.'(
            UI.Color colorIn, Color other, double t, Color blended
    ) {
        reportInfo """
            The `blend(Color, double)` method on a SwingTree `UI.Color` performs a
            linear interpolation between this color and the supplied `other` color
            in the sRGB color space. The interpolation factor `t` controls the mix:
            a value of `0.0` returns this color unchanged, a value of `1.0` returns
            the other color, and intermediate values produce a smooth blend. Each
            of the red, green, blue and alpha channels is interpolated independently.
            This is a great building block for derived palettes — for example, mixing
            a primary color with white to produce a soft tint, or mixing two theme
            colors to produce a midpoint accent.
        """
        expect :
            colorIn.blend(other, t) == blended
        where :
            colorIn          | other            |  t    ||  blended
            UI.Color.WHITE   | UI.Color.BLACK   |  0.0  ||  new Color(255,255,255)
            UI.Color.WHITE   | UI.Color.BLACK   |  1.0  ||  new Color(  0,  0,  0)
            UI.Color.WHITE   | UI.Color.BLACK   |  0.5  ||  new Color(128,128,128)
            UI.Color.RED     | UI.Color.BLUE    |  0.5  ||  new Color(128,  0,128)
            UI.Color.BLACK   | UI.Color.WHITE   |  0.25 ||  new Color( 64, 64, 64)
            UI.Color.RED     | UI.Color.WHITE   |  0.5  ||  new Color(255,128,128)
            UI.Color.RED     | UI.Color.BLACK   |  0.5  ||  new Color(128,  0,  0)
            UI.Color.LIME    | UI.Color.RED     |  1.0  ||  new Color(255,  0,  0)
            UI.Color.LIME    | UI.Color.LIME    |  0.5  ||  UI.Color.LIME
    }

    def 'The "blend(Color, double)" method clamps the interpolation factor into the range 0.0..1.0.'(
            UI.Color colorIn, Color other, double t, Color blended
    ) {
        reportInfo """
            The `blend(Color, double)` method clamps the interpolation factor `t`
            into the inclusive range `0.0..1.0`. This means that a negative `t`
            simply returns this color unchanged, and a `t` greater than `1.0`
            returns the other color unchanged. This makes the method safe to call
            with arbitrary user-supplied values without having to clamp them
            yourself at the call site.
        """
        expect :
            colorIn.blend(other, t) == blended
        where :
            colorIn          | other           |  t     ||  blended
            UI.Color.RED     | UI.Color.BLUE   |  -0.5  ||  new Color(255,  0,  0)
            UI.Color.RED     | UI.Color.BLUE   |  -1.0  ||  new Color(255,  0,  0)
            UI.Color.RED     | UI.Color.BLUE   |   1.5  ||  new Color(  0,  0,255)
            UI.Color.RED     | UI.Color.BLUE   |  42.0  ||  new Color(  0,  0,255)
    }

    def 'The "blend(Color, double)" method also interpolates the alpha component.'() {
        reportInfo """
            The `blend(Color, double)` method blends not only the red, green and
            blue channels but also the alpha component. This means that mixing a
            fully opaque color with a fully transparent color at `t = 0.5` yields
            a half-transparent color. This is useful for fading between two layers
            without having to handle the alpha channel separately.
        """
        given : 'A fully opaque red and a fully transparent blue.'
            var opaqueRed       = UI.Color.RED
            var transparentBlue = UI.Color.BLUE.withAlpha(0)
        when : 'We blend them at the midpoint.'
            var midpoint = opaqueRed.blend(transparentBlue, 0.5)
        then : 'The resulting color has all four channels averaged.'
            midpoint.red    == 128
            midpoint.green  ==   0
            midpoint.blue   == 128
            midpoint.alpha  == 128
    }

    def 'Use "shade(double)" to lighten a color towards white or darken it towards black.'(
            UI.Color colorIn, double amount, Color shaded
    ) {
        reportInfo """
            The `shade(double)` method on a SwingTree `UI.Color` produces a tinted
            or shaded version of the color: a positive `amount` mixes this color
            towards `WHITE` (a lighter tint), a negative `amount` mixes it towards
            `BLACK` (a darker shade), and a value of `0.0` returns the color
            unchanged. The magnitude of `amount` controls how strongly the color
            is pulled towards the target.

            This is shorthand for `blend(amount < 0 ? BLACK : WHITE, Math.abs(amount))`
            and is particularly handy for deriving subtle gradient stops from a
            single base color — for example, painting a page background with a
            slightly darker bottom edge by using `pageColor.shade(-0.06)`.
        """
        expect :
            colorIn.shade(amount) == shaded
        where :
            colorIn          |  amount  ||  shaded
            UI.Color.RED     |   0.0    ||  UI.Color.RED
            UI.Color.RED     |   1.0    ||  new Color(255,255,255)
            UI.Color.RED     |  -1.0    ||  new Color(  0,  0,  0)
            UI.Color.RED     |   0.5    ||  new Color(255,128,128)
            UI.Color.RED     |  -0.5    ||  new Color(128,  0,  0)
            UI.Color.BLUE    |   0.25   ||  new Color( 64, 64,255)
            UI.Color.BLUE    |  -0.25   ||  new Color(  0,  0,191)
            UI.Color.WHITE   |   0.5    ||  new Color(255,255,255)
            UI.Color.BLACK   |  -0.5    ||  new Color(  0,  0,  0)
            UI.Color.WHITE   |  -1.0    ||  new Color(  0,  0,  0)
            UI.Color.BLACK   |   1.0    ||  new Color(255,255,255)
    }
}
