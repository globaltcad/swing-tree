package swingtree.styles

import spock.lang.Specification
import swingtree.UI
import swingtree.style.Colour

import java.awt.Color

class Color_Spec extends Specification
{

    def 'Use the "brighterBy(double)" on a SwingTree color to brighten the color according to the HSB color space.'(
         Colour colorIn, double factor, Color brighter
    ) {
        reportInfo """
            The colors in SwingTree are modelled using a custom `Colour` type which for 
            compatibility reasons is a subclass of java.awt.Color.
            Amon many other useful methods, the custom color type has a method called
            `brighterBy(double)` which will brighten the color according to the HSB color space
            (Hue, Saturation, Brightness).
        """
        expect :
            colorIn.brighterBy(factor) == brighter
        where :
            colorIn             |  factor  ||  brighter
            UI.COLOR_ALICEBLUE  |   0.3    || new Color(240,248,255)
            UI.COLOR_BEIGE      |   0.4    || new Color(255,255,229)
            UI.COLOR_BLACK      |   0.6    || new Color(21,21,21)
            UI.COLOR_DARKGREEN  |   0.7    || new Color(0,143,0)
            UI.COLOR_HONEYDEW   |   0.2    || new Color(240,255,240)
            UI.COLOR_INDIANRED  |   0.7    || new Color(255,114,114)
            UI.COLOR_WHITE      |   0.9    || new Color(255,255,255)

            UI.COLOR_ALICEBLUE  |   0.0    || UI.COLOR_ALICEBLUE
            UI.COLOR_BEIGE      |   0.0    || UI.COLOR_BEIGE
            UI.COLOR_BLACK      |   0.0    || UI.COLOR_BLACK
            UI.COLOR_DARKGREEN  |   0.0    || UI.COLOR_DARKGREEN
            UI.COLOR_HONEYDEW   |   0.0    || UI.COLOR_HONEYDEW
            UI.COLOR_INDIANRED  |   0.0    || UI.COLOR_INDIANRED
            UI.COLOR_WHITE      |   0.0    || UI.COLOR_WHITE
    }

    def 'Use the "darkerBy(double)" on a SwingTree color to darken the color according to the HSB color space.'(
        Colour colorIn, double factor, Color darker
    ) {
        reportInfo """
            The colors in SwingTree are modelled using a custom `Colour` type which for 
            compatibility reasons is a subclass of java.awt.Color.
            Amon many other useful methods, the custom color type has a method called
            `darkerBy(double)` which will darken the color according to the brightness in the 
            HSB color space (Hue, Saturation, Brightness).
        """
        expect :
            colorIn.darkerBy(factor) == darker
        where :
            colorIn                |  factor  ||  darker
            UI.COLOR_DEEPSKYBLUE   |   0.3    || new Color(0,134,179)
            UI.COLOR_CORAL         |   0.4    || new Color(153,76,48)
            UI.COLOR_LAVENDERBLUSH |   0.6    || new Color(102,96,98)
            UI.COLOR_TAN           |   0.7    || new Color(63,54,42)
            UI.COLOR_LINEN         |   0.2    || new Color(200,192,184)
            UI.COLOR_WHITE         |   0.7    || new Color(77,77,77)
            UI.COLOR_BLACK         |   0.9    || new Color(0,0,0)

            UI.COLOR_DEEPSKYBLUE   |   0.0    || UI.COLOR_DEEPSKYBLUE
            UI.COLOR_CORAL         |   0.0    || UI.COLOR_CORAL
            UI.COLOR_LAVENDERBLUSH |   0.0    || UI.COLOR_LAVENDERBLUSH
            UI.COLOR_TAN           |   0.0    || UI.COLOR_TAN
            UI.COLOR_LINEN         |   0.0    || UI.COLOR_LINEN
            UI.COLOR_WHITE         |   0.0    || UI.COLOR_WHITE
            UI.COLOR_BLACK         |   0.0    || UI.COLOR_BLACK
    }

    def 'Use "saturateBy(double)" on a SwingTree color to increase the saturation of the color according to the HSB color space.'(
        Colour colorIn, double factor, Color saturated
    ) {
        reportInfo """
            The colors in SwingTree are modelled using a custom `Colour` type which for 
            compatibility reasons is a subclass of java.awt.Color.
            Amon many other useful methods, the custom color type has a method called
            `saturateBy(double)` which will increase the saturation of the color according to the HSB color space
            (Hue, Saturation, Brightness).
        """
        expect :
            colorIn.saturateBy(factor) == saturated
        where :
            colorIn                |  factor  ||  saturated
            UI.COLOR_DEEPSKYBLUE   |   0.3    || new Color(0,191,255)
            UI.COLOR_CORAL         |   0.4    || new Color(255,68,0)
            UI.COLOR_LAVENDERBLUSH |   0.6    || new Color(255,230,238)
            UI.COLOR_TAN           |   0.7    || new Color(210,167,110)
            UI.COLOR_LINEN         |   0.2    || new Color(250,200,150)
            UI.COLOR_WHITE         |   0.7    || new Color(255,255,255)
            UI.COLOR_BLACK         |   0.9    || new Color(0,0,0)

            UI.COLOR_DEEPSKYBLUE   |   0.0    || UI.COLOR_DEEPSKYBLUE
            UI.COLOR_CORAL         |   0.0    || UI.COLOR_CORAL
            UI.COLOR_LAVENDERBLUSH |   0.0    || UI.COLOR_LAVENDERBLUSH
            UI.COLOR_TAN           |   0.0    || UI.COLOR_TAN
            UI.COLOR_LINEN         |   0.0    || UI.COLOR_LINEN
            UI.COLOR_WHITE         |   0.0    || UI.COLOR_WHITE
            UI.COLOR_BLACK         |   0.0    || UI.COLOR_BLACK
    }

}
