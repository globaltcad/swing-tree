package swingtree.styles

import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title
import swingtree.UI
import swingtree.api.Styler
import swingtree.style.ComponentStyleDelegate

import javax.swing.JButton
import javax.swing.plaf.metal.MetalButtonUI
import java.awt.Color

@Title("Style Installation")
@Narrative('''

    **This specification covers the behaviour of the style installation process!**
    Which means that the contents of this may not be relevant to.
    Keep reading however if you are interested in some of the obscure details
    of the SwingTree library internals.

    SwingTree offers advanced styling options as part of **the style API**,
    which is most commonly used using the `withStyle(Styler)` method
    on any builder node.
   
   The installation of styles is a complex process that involves the
   the overwriting of the component's UI delegate, the application of
   the style's properties to the component and the installation of
   a custom border, all depending on the style's configuration.
   
''')
@Subject([UI, Styler])
class Style_Installation_Spec extends Specification
{
    def 'Different `Styler`s may or may not lead to the installation of a custom UI.'(
        boolean isCustom, Styler<JButton> styler
    ){
        reportInfo """
            This is a data driven test that takes a `Styler` 
            which will be applied to a `JButton` by passing it to the
            `withStyle(Styler)` method.
            Then we build the component and check if the custom UI was installed.
            
            This specification may not be relevant to you if you are not interested
            in the details of the SwingTree library internals.
            But it demonstrates the complexity of the style installation process
            and should give you a good idea of what it took to build the SwingTree library.
        """
        given: 'We create a button UI with the given styler'
            var ui = UI.button().withStyle(styler)
        when: 'We build the button'
            var button = ui.get(JButton)
        then: 'The custom UI may or may not be installed:'
            !(button.getUI() instanceof MetalButtonUI) == isCustom

        where :
            isCustom | styler
            false    | { it }
            false    | { it.backgroundColor(Color.BLACK) }
            false    | { it.foregroundColor(Color.BLUE) }
            false    | { it.foundationColor(Color.GREEN) }
            false    | { it.cursor(UI.Cursor.HAND) }
            false    | { it.margin(5) }
            false    | { it.padding(5).margin(5) }
            false    | { it.border(2, "black") }
            false    | { it.margin(5).border(3, "red").cursor(UI.Cursor.CROSS) }
            false    | { it.shadowColor("green") }
            false    | { it.shadowColor("blue").shadowBlurRadius(5) }
            false    | { it.shadowColor("pink").shadowBlurRadius(2).shadowSpreadRadius(7) }
            false    | { it.shadow(UI.Layer.CONTENT, "myShadow", conf->conf.color("black").offset(1,2).blurRadius(5)) }
            false    | { it.shadow(UI.Layer.CONTENT, "myShadow", conf->conf.color("red").spreadRadius(7).isOutset(true)) }
            false    | { it.shadow(UI.Layer.CONTENT, "myShadow", conf->conf.color("red").spreadRadius(1).blurRadius(5)) }
            false    | { it.shadow(UI.Layer.BORDER, "myShadow", conf->conf.color("black").offset(1,2).blurRadius(5)) }
            false    | { it.shadow(UI.Layer.BORDER, "myShadow", conf->conf.color("red").spreadRadius(7).isOutset(true)) }
            false    | { it.shadow(UI.Layer.BORDER, "myShadow", conf->conf.color("red").spreadRadius(1).blurRadius(5)) }
            true     | { it.shadow(UI.Layer.BACKGROUND, "myShadow", conf->conf.color("black").offset(1,2).blurRadius(5)) }
            true     | { it.shadow(UI.Layer.BACKGROUND, "myShadow", conf->conf.color("red").spreadRadius(7).isOutset(true)) }
            false    | { it.shadow(UI.Layer.FOREGROUND, "myShadow", conf->conf.color("red").spreadRadius(1).blurRadius(5)) }
            true     | { it.gradient(UI.Layer.BACKGROUND, "myGradient", conf->conf.colors(Color.RED, Color.BLUE)) }
            false    | { it.gradient(UI.Layer.FOREGROUND, "myGradient", conf->conf.colors(Color.RED, Color.BLUE)) }
            false    | { it.gradient(UI.Layer.CONTENT, "myGradient", conf->conf.colors(Color.RED, Color.BLUE)) }
            false    | { it.gradient(UI.Layer.CONTENT, "myGradient", conf->conf.colors(Color.RED, Color.BLUE)) }
            false    | { it.gradient(UI.Layer.BORDER, "myGradient", conf->conf.colors(Color.RED, Color.BLUE)) }
            false    | { it.gradient(UI.Layer.BORDER, "myGradient", conf->conf.colors(Color.RED, Color.BLUE)) }
            true     | { it.painter(UI.Layer.BACKGROUND, "myPainter", g2d -> {}) }
            false    | { it.painter(UI.Layer.FOREGROUND, "myPainter", g2d -> {}) }
            false    | { it.painter(UI.Layer.CONTENT, "myPainter", g2d -> {}) }
            false    | { it.painter(UI.Layer.CONTENT, "myPainter", g2d -> {}) }
            false    | { it.painter(UI.Layer.BORDER, "myPainter", g2d -> {}) }
            false    | { it.painter(UI.Layer.BORDER, "myPainter", g2d -> {}) }
    }
}
