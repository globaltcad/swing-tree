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
}
