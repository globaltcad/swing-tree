package swingtree.styles

import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Title
import swingtree.SwingTree
import swingtree.threading.EventProcessor
import swingtree.UI

import javax.swing.UIManager
import javax.swing.border.CompoundBorder
import javax.swing.plaf.metal.MetalButtonUI
import javax.swing.plaf.metal.MetalLabelUI
import java.awt.*
import java.awt.event.MouseEvent
import java.awt.image.BufferedImage
import java.util.concurrent.TimeUnit

@Title("Style Animations")
@Narrative('''
   
    Styles in SwingTree are based on a functional style engine
    which reevaluates your styles whenever your component is
    repainted. 
    This makes them a perfect fit for animations.
    Continue reading to learn how to animate your styles.
    
''')
class Style_Animations_Spec extends Specification
{
    def setupSpec() {
        SwingTree.get().setEventProcessor(EventProcessor.COUPLED)
        // This is so that the test thread is also allowed to perform UI operations
    }

    def setup()
    {
        // We reset to the default look and feel:
        UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName())
        // This is to make sure that the tests are not influenced by
        // other look and feels that might be used in the example code...
    }

    def 'An `onMouseClick` event style animation is only temporary.'()
    {
        reportInfo """
            A vanilla component created through one of the many `UI` factory
            methods will not have any animations applied to it.
            However, once you apply a style animation to it, it will
            temporarily change the component to support your animated style.
        """
        given : 'We create a simple `JLabel` UI component with a style animation.'
            var ui = UI.label("Click me!")
                        .onMouseClick(it ->
                            it.animateFor(1, TimeUnit.MINUTES, state ->
                                it.style(state, style -> style
                                    .borderWidth((int) (10 * state.cycle()))
                                    .borderColor(new Color(0, 100,200))
                                    .backgroundColor(new Color(255, 100, 0, (int) (255 * state.cycle())))
                                )
                            )
                        )
        and : 'We actually build the component:'
            var label = ui.component

        expect : """
            When the user clicks on the label, the border width of the label
            will be animated from 0 to 10 over the course of 100 milliseconds
            and the background color will be animated from transparent to orange.
            But initially the label will not have any border or background color.
            So let's check that.
        """
            label.border == null
            label.background == new Color(238, 238, 238) // default background color of a JLabel
            label.foreground == new Color(51, 51, 51) // default foreground color of a JLabel
            label.getUI() instanceof MetalLabelUI

        when : 'We simulate a user click event programmatically'
            // Note that there is no "onMouseClick()" method on the label.
            // Instead we need to do this:
            label.dispatchEvent(new MouseEvent(label, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(), 0, 0, 0, 1, false))
            Thread.sleep(100)
            UI.sync()
            label.paint(new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics())

        then : 'The label will have a border and a background color.'
            label.border != null
            label.background != new Color(238, 238, 238)
            label.foreground == new Color(51, 51, 51)
            label.getUI() instanceof MetalLabelUI
    }


    def 'An `onMouseClick` event style animation dispatched using the `animateStyleFor` method is only temporary.'()
    {
        reportInfo """
            The event delegate passed to the `onMouseClick` lambda exposes
            a useful API for interacting with the component.
            This includes the `animateStyleFor` method which allows you
            to animate the style of the component for a given duration.
            Once you apply a style animation to it, it will append the provided styler lambda
            to the end of the style chain and then when the component is repainted,
            it will temporarily change the component to support your animated style.
        """
        given : 'We create a simple `JLabel` UI component with a style animation.'
            var ui = UI.label("Click me!")
                        .onMouseClick(it ->
                            it.animateStyleFor(1, TimeUnit.MINUTES, (state, style) -> style
                                .borderWidthAt(UI.Edge.BOTTOM, (int) (12 * state.cycle()))
                                .borderColor(Color.BLACK)
                                .shadowColor(new Color(0, 100,200, (int) (255 * state.cycle())))
                                .shadowBlurRadius((int) (12 * state.cycle()))
                                .shadowIsInset(false)
                                .fontSize((int) (12 + 24 * state.cycle()))
                            )
                        )
        and : 'We actually build the component:'
            var label = ui.component

        expect : """
            When the user clicks on the label, the border width of the label
            will be animated from 0 to 10 over the course of 100 milliseconds
            and the background color will be animated from transparent to orange.
            But initially the label will not have any border or background color.
            So let's check that.
        """
            label.border == null
            label.font.size == 12
            label.getUI() instanceof MetalLabelUI

        when : 'We simulate a user click event programmatically'
            // Note that there is no "onMouseClick()" method on the label.
            // Instead we need to do this:
            label.dispatchEvent(new MouseEvent(label, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(), 0, 0, 0, 1, false))
            Thread.sleep(2000)
            UI.sync()
            label.paint(new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics())

        then : 'The label will have a border and a background color.'
            label.border != null
            label.font.size > 12
        and : 'SwingTree will haver overridden the default label UI in order to support the shadow.'
            !(label.getUI() instanceof MetalLabelUI)
    }


    def 'SwingTree will uninstall any custom border after an animation has completed.'()
    {
        reportInfo """
            A vanilla component created through one of the many `UI` factory
            methods will not have any animations applied to it.
            However, once you apply a style animation to it, it will
            temporarily change the component to support your animated style.
        """
        given : 'We create a simple `JLabel` UI component with a style animation.'
            var ui = UI.label("Click me!")
                        .onMouseClick(it ->
                            it.animateFor(1, TimeUnit.SECONDS, state ->
                                it.style(state, style -> style
                                    .borderWidth((int) (10 * state.cycle()))
                                    .backgroundColor(new Color(255, 100, 0, (int) (255 * state.cycle())))
                                )
                            )
                        )
            var label = ui.component

        expect : 'It has the expected initial state.'
            label.border == null
            label.background == new Color(238, 238, 238) // default background color of a JLabel
            label.foreground == new Color(51, 51, 51) // default foreground color of a JLabel
            label.getUI() instanceof MetalLabelUI

        when : 'We simulate a user click event programmatically'
            // Note that there is no "onMouseClick()" method on the label.
            // Instead we need to do this:
            label.dispatchEvent(new MouseEvent(label, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(), 0, 0, 0, 1, false))
            Thread.sleep(50)
            UI.sync()
            label.paint(new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics())


        then : """
            The border will have a custom border installed.
        """
            label.border != null
            label.background != new Color(238, 238, 238)
            label.foreground == new Color(51, 51, 51)
            label.getUI() instanceof MetalLabelUI

        when : 'We wait for the animation to end...'
            Thread.sleep(2000)
            UI.sync()
            label.paint(new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics())

        then : """
            The border will have been uninstalled.
        """
            label.border == null
            label.background == new Color(238, 238, 238)
            label.foreground == new Color(51, 51, 51)
            label.getUI() instanceof MetalLabelUI
    }


    def 'Advanced style animations will override the Look and Feel of a component temporarily'()
    {
        reportInfo """
            A vanilla component created through one of the many `UI` factory
            methods will not have any animations applied to it.
            However, once you apply a style animation to it, it will
            temporarily change the component to support your animated style.
            For very advanced styles, this may mean that the component
            will temporarily override the Look and Feel of the component.
        """
        given : 'We create a simple `JButton` UI component with a style animation.'
            var ui = UI.button("Click me!")
                        .onClick(it ->
                            it.animateFor(300, TimeUnit.MILLISECONDS, state ->
                                it.style(state, style -> style
                                    .border((int) (12 * state.cycle()), Color.RED)
                                    .backgroundColor(new Color(255, 100, 0, (int) (255 * state.cycle())))
                                    .margin(12)
                                    .foundationColor(new Color(0, 100, 200))
                                    .painter(UI.Layer.BACKGROUND, g2d -> {
                                        boolean isMouseRollover = style.component().getModel().isRollover()
                                        // We draw a custom gradient:
                                        var gradient = new GradientPaint(
                                            0, 0, isMouseRollover ? Color.RED : Color.BLUE,
                                                style.component().getWidth(), style.component().getHeight(), isMouseRollover ? Color.BLUE : Color.RED
                                        )
                                        g2d.setPaint(gradient)
                                        g2d.fillRect(0, 0, it.component.getWidth(), it.component.getHeight())
                                    })
                                )
                            )
                        )
        and : 'We actually build the component:'
            var button = ui.component

        expect : """
            Initially the button will the default button border and background color.
        """
            button.border instanceof CompoundBorder
            button.background == new Color(238, 238, 238) // default background color of a JButton
            button.foreground == new Color(51, 51, 51) // default foreground color of a JButton
            button.getUI() instanceof MetalButtonUI

        when : 'We simulate a user click event programmatically'
        button.doClick()
            Thread.sleep(100)
            UI.sync()
            UI.runNow({button.paint(new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics())})

        then : 'The label will have a border and a background color.'
            !(button.border instanceof CompoundBorder)
            button.background != new Color(238, 238, 238)
            button.foreground == new Color(51, 51, 51)
            !(button.getUI() instanceof MetalButtonUI)
    }


}
