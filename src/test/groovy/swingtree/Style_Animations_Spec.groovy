package swingtree

import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Title
import spock.util.concurrent.PollingConditions
import sprouts.Var
import swingtree.animation.LifeTime
import swingtree.style.ComponentExtension
import swingtree.threading.EventProcessor

import javax.swing.*
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

    def 'A transitory style animation always ends with a progress of 1.'()
    {
        reportInfo """
            In this example we use a transitory style animation to record the progress of the animation.
            The important note here is that the progress of the animation will always end with 1.
            What this means for you in practice is that you can use this fact as 
            a signal to perform some action once the animation has completed.
            
            So you can use the progress in a way where the animated style will be
            reset to its original state once the animation has completed.
        """
        given : 'We create an event object for triggering the animation.'
            var event = sprouts.Event.create()
        and : 'Two lists in which we record the progress and cycle values of the animation.'
            var proresses = []
            var cycles = []
        and : 'We create a simple `JTogglButton` UI component with a style animation.'
            var ui = UI.toggleButton("Click me!")
                            .withTransitoryStyle(event.observable(), LifeTime.of(0.05, TimeUnit.SECONDS), (state, conf) -> {
                                proresses << state.progress()
                                cycles << state.cycle()
                                return conf;
                            })
        and : 'We actually build the component:'
            var button = ui.get(JToggleButton)
        expect : 'Initially the two lists are empty.'
            proresses.isEmpty()
            cycles.isEmpty()

        when : 'We trigger the animation.'
            event.fire()
            Thread.sleep(100)
            UI.sync()
            button.paint(new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics())

        then : """
            The progress of the animation will always end with 1.
        """
            proresses.last() == 1
        and : """
            The cycle of the animation will always end with 1.
        """
            cycles.last() == 0
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
                            it.animateFor(1, TimeUnit.MINUTES, status ->
                                it.style(status, style -> style
                                    .borderWidth((int) (10 * status.cycle()))
                                    .borderColor(new Color(0, 100,200))
                                    .backgroundColor(new Color(255, 100, 0, (int) (255 * status.cycle())))
                                )
                            )
                        )
        and : 'We actually build the component:'
            var label = ui.get(JLabel)

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

        then : 'The label will have a border and the custom colors...'
            new PollingConditions(timeout: 10, initialDelay: 0, factor: 1.25).eventually {
                assert label.border != null
            }
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
                                .fontSize((int) (22 + 14 * state.cycle()))
                            )
                        )
        and : 'We actually build the component:'
            var label = ui.get(JLabel)

        expect : """
            When the user clicks on the label, the border width of the label
            will be animated from 0 to 10 over the course of 100 milliseconds
            and the background color will be animated from transparent to orange.
            But initially the label will not have any border or background color.
            So let's check that.
        """
            label.border == null
            label.font.size <= 13
            label.getUI() instanceof MetalLabelUI

        when : 'We simulate a user click event programmatically'
            // Note that there is no "onMouseClick()" method on the label.
            // Instead we need to do this:
            label.dispatchEvent(new MouseEvent(label, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(), 0, 0, 0, 1, false))
            Thread.sleep(1_000)
            UI.sync()
            label.paint(new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics())

        then : 'The label will have a border and a background color.'
            label.border != null
            label.font.size > 13
        and : 'The UI of the component was not overridden, because the shadow can be rendered through the border of the component.'
            (label.getUI() instanceof MetalLabelUI)
            label.border != null
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
                            it.animateFor(1, TimeUnit.SECONDS, status ->
                                it.style(status, style -> style
                                    .borderWidth((int) (10 * status.cycle()))
                                    .backgroundColor(new Color(255, 100, 0, (int) (255 * status.cycle())))
                                )
                            )
                        )
            var label = ui.get(JLabel)
            label.setSize(200, 60) // We set the size of the label to make it visible.
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
            The label will have a custom border installed.
        """
            new PollingConditions(timeout: 10, initialDelay: 0, factor: 1.25).eventually {
                assert label.border != null
            }
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
            new PollingConditions(timeout: 10, initialDelay: 0, factor: 1.25).eventually {
                assert label.border == null
            }
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
                            it.animateFor(300, TimeUnit.MILLISECONDS, status ->
                                it.style(status, style -> style
                                    .border((int) (12 * status.cycle()), Color.RED)
                                    .backgroundColor(new Color(255, 100, 0, (int) (255 * status.cycle())))
                                    .margin(12)
                                    .foundationColor(new Color(0, 100, 200))
                                    .painter(UI.Layer.BACKGROUND, g2d -> {
                                        boolean isMouseRollover = style.component().getModel().isRollover()
                                        // We draw a custom gradient:
                                        var gradient = new GradientPaint(
                                            0, 0, isMouseRollover ? Color.RED : Color.BLUE,
                                                style.componentWidth(), style.componentHeight(), isMouseRollover ? Color.BLUE : Color.RED
                                        )
                                        g2d.setPaint(gradient)
                                        g2d.fillRect(0, 0, style.componentWidth(), style.componentHeight())
                                    })
                                )
                            )
                        )
        and : 'We actually build the component:'
            var button = ui.get(JButton)

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

    def 'You can animate a style using properties and the `withRepaintOn` methods!'( float uiScale ) {
        reportInfo """
            Styles in SwingTree a functions which take in a configuration
            and transform it into a new and updated configuration.
            But this function is not invoked all the time, so when you capture
            mutable state in such a style function, like a property for example,
            it is your responsibility to trigger a re-evaluation and repaint.
            
            This is what 'withRepaintOn' is useful for, because it re-evaluates the
            style and then triggers a repaint.
        """
        given : """
            First up, we initialize SwingTree with a custom DPI scale and
            then define some scaling an scaling rounding error functions
            which mirror how SwingTree calculates margin errors based on
            its box model being fractional... This will make more sense 
            if you continue reading. 
        """
            SwingTree.initializeUsing { it.uiScaleFactor(uiScale) }
            var scale = { it * uiScale }
            var scaledToString = { String.valueOf(scale(it)).replace(".0", "") }
            var scaleError = { 1 - ( scale(it) % 1 ) }
            var scaleErrorToString = {
                var err = scaleError(it)
                if ( err == 1 ) err = 0
                var asStr = String.valueOf(err).replace(".0", "")
                return asStr == "0" ? "?" : asStr
            }
        and : "We create a simple mutable float property for modelling a dynamic border width!"
            var width = Var.of(0f)
        and : """
            We create a Swing component, a JLabel, and
            apply a style to it based on a float property which
            is bound to the 'withRepaintOn' method.
        """
            var label =
                        UI.label("Title").withSizeExactly(84, 42)
                        .withRepaintOn(width)
                        .withStyle(conf -> conf
                            .border(width.get(), "green")
                            .borderRadius(width.get())
                            .backgroundColor("oak")
                        )
                        .get(JLabel)
        and : 'Then we get a string encoding of the entire style:'
            var styleString = ComponentExtension.from(label).getStyle().toString()

        expect :
            styleString.contains("BorderConf[NONE]")
            styleString.contains("backgroundColor=rgba(216,181,137,255)")

        when : """
            We noe set a fractional border width! Note that Swing does not
            support fractional components widths, so how does it ensure that
            the layout is not broken? Well...
        """
            width.set(2.5f)
            UI.sync()
            styleString = ComponentExtension.from(label).getStyle().toString()
        then : """
            Here you can see that the custom border automatically created
            a fractional margin by which always produces a component with
            a whole width and height!
            
            All of this dynamically updated because of the 'withRepaintOn' method!
        """
            !styleString.contains("BorderConf[NONE]")
            styleString.contains("backgroundColor=rgba(216,181,137,255)")
            styleString.contains("BorderConf[" +
                        "radius=${scaledToString(2.5)}, " +
                        "width=${scaledToString(2.5)}, " +
                        "margin=Outline[" +
                            "top=${scaleErrorToString(2.5)}, " +
                            "right=${scaleErrorToString(2.5)}, " +
                            "bottom=${scaleErrorToString(2.5)}, " +
                            "left=${scaleErrorToString(2.5)}" +
                        "], " +
                        "padding=Outline[top=?, right=?, bottom=?, left=?], " +
                        "color=rgba(0,128,0,255)" +
                    "]")

        when : 'We go back to the old value...'
            width.set(0f)
            UI.sync()
            styleString = ComponentExtension.from(label).getStyle().toString()
        then : 'Everything is back to normal! No style.'
            styleString.contains("BorderConf[NONE]")
            styleString.contains("backgroundColor=rgba(216,181,137,255)")
        where :
            uiScale << [ 1.0f, 1.5f, 2.0f, 2.25f, 3.0f ]
    }

}
