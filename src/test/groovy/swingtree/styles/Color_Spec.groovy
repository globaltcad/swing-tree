package swingtree.styles

import spock.lang.Specification
import swingtree.UI

import java.awt.*

class Color_Spec extends Specification
{

    def 'Use the "brighterBy(double)" on a SwingTree color to brighten the color according to the HSB color space.'(
            UI.Colour colorIn, double factor, Color brighter
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
            UI.Colour.ALICEBLUE  |   0.3    || new Color(240,248,255)
            UI.Colour.BEIGE      |   0.4    || new Color(255,255,229)
            UI.Colour.BLACK      |   0.6    || new Color(21,21,21)
            UI.Colour.DARKGREEN  |   0.7    || new Color(0,143,0)
            UI.Colour.HONEYDEW   |   0.2    || new Color(240,255,240)
            UI.Colour.INDIANRED  |   0.7    || new Color(255,114,114)
            UI.Colour.WHITE      |   0.9    || new Color(255,255,255)

            UI.Colour.ALICEBLUE  |   0.0    || UI.Colour.ALICEBLUE
            UI.Colour.BEIGE      |   0.0    || UI.Colour.BEIGE
            UI.Colour.BLACK      |   0.0    || UI.Colour.BLACK
            UI.Colour.DARKGREEN  |   0.0    || UI.Colour.DARKGREEN
            UI.Colour.HONEYDEW   |   0.0    || UI.Colour.HONEYDEW
            UI.Colour.INDIANRED  |   0.0    || UI.Colour.INDIANRED
            UI.Colour.WHITE      |   0.0    || UI.Colour.WHITE
    }

    def 'Use the "darkerBy(double)" on a SwingTree color to darken the color according to the HSB color space.'(
            UI.Colour colorIn, double factor, Color darker
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
            UI.Colour.DEEPSKYBLUE   |   0.3    || new Color(0,134,179)
            UI.Colour.CORAL         |   0.4    || new Color(153,76,48)
            UI.Colour.LAVENDERBLUSH |   0.6    || new Color(102,96,98)
            UI.Colour.TAN           |   0.7    || new Color(63,54,42)
            UI.Colour.LINEN         |   0.2    || new Color(200,192,184)
            UI.Colour.WHITE         |   0.7    || new Color(77,77,77)
            UI.Colour.BLACK         |   0.9    || new Color(0,0,0)

            UI.Colour.DEEPSKYBLUE   |   0.0    || UI.Colour.DEEPSKYBLUE
            UI.Colour.CORAL         |   0.0    || UI.Colour.CORAL
            UI.Colour.LAVENDERBLUSH |   0.0    || UI.Colour.LAVENDERBLUSH
            UI.Colour.TAN           |   0.0    || UI.Colour.TAN
            UI.Colour.LINEN         |   0.0    || UI.Colour.LINEN
            UI.Colour.WHITE         |   0.0    || UI.Colour.WHITE
            UI.Colour.BLACK         |   0.0    || UI.Colour.BLACK
    }

    def 'Use "saturateBy(double)" on a SwingTree color to increase the saturation of the color according to the HSB color space.'(
            UI.Colour colorIn, double factor, Color saturated
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
            UI.Colour.DEEPSKYBLUE   |   0.3    || new Color(0,191,255)
            UI.Colour.CORAL         |   0.4    || new Color(255,68,0)
            UI.Colour.LAVENDERBLUSH |   0.6    || new Color(255,230,238)
            UI.Colour.TAN           |   0.7    || new Color(210,167,110)
            UI.Colour.LINEN         |   0.2    || new Color(250,200,150)
            UI.Colour.WHITE         |   0.7    || new Color(255,255,255)
            UI.Colour.BLACK         |   0.9    || new Color(0,0,0)

            UI.Colour.DEEPSKYBLUE   |   0.0    || UI.Colour.DEEPSKYBLUE
            UI.Colour.CORAL         |   0.0    || UI.Colour.CORAL
            UI.Colour.LAVENDERBLUSH |   0.0    || UI.Colour.LAVENDERBLUSH
            UI.Colour.TAN           |   0.0    || UI.Colour.TAN
            UI.Colour.LINEN         |   0.0    || UI.Colour.LINEN
            UI.Colour.WHITE         |   0.0    || UI.Colour.WHITE
            UI.Colour.BLACK         |   0.0    || UI.Colour.BLACK
    }

}
