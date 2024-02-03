package swingtree.other

import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Title
import swingtree.UI
import swingtree.style.ComponentExtension

import javax.swing.JButton
import java.awt.*
import java.awt.image.BufferedImage

@Title("Internal Render Caching")
@Narrative('''

    **This specification covers the behaviour of an internal class!
    Which means that the contents of this may not be relevant to.
    Keep reading however if you are interested in some of the obscure details
    of the SwingTree library internals.**

    SwingTree offers advanced styling options as part of **the style API**,
    which is most commonly used using the `withStyle(Styler)` method
    on any builder node.
    The rendering of these styles can get rather compute intensive,
    especially in cases where repaints are called frequently.
    This is not really a problem for modern CPUs/GPUs, which can handle the 
    rendering of UI effortlessly, but nonetheless, we do not want to waste
    precious clock cycles and energy on meaningfully repaints of things that were already
    painted many many times before.
    This is especially important for mobile devices, which should not burn through 
    your battery just because of a few shaded buttons...
    
    ---
    
    **So how does SwingTree solve this issue?**
    
    The answer is caching based rendering where the cache keys are the
    immutable style configurations. And whenever the style configuration
    object changes a different cache area is being hit.
    
    The extensive usage of immutable data inside the SwingTree style engine
    allows us to do caching for a wide wide variety of things besides rendering,
    like the caching of complex shapes (needed for clipping and painting).
    
    But I digress... Let's ensure that the caching works as expected:

''')
class Render_Cache_Spec extends Specification
{
    def 'The cache can be created and used for painting.'()
    {
        reportInfo """
            Note that the caching class itself does not know how to paint anything itself.
            It receives a lambda which does the actual painting and whenever something was 
            found in the cache, then the lambda is simply ignored. 
        """
        given : 'We have a cache for the background layer of a component:'
            var cache = new swingtree.style.LayerCache(UI.Layer.BACKGROUND)
        and : 'And a basic style configuration object:'
            var key0 = swingtree.style.ComponentConf.none()
        and : 'Finally, a mocked graphics object we can use to check if the cache was hit or not:'
            var g = Mock(Graphics2D)

        when : 'We try to do an initial paint into the cache...'
            cache.validate(key0, key0)
            cache.paint(g,  (conf, g2) -> {g2.fillRect(0,0,10,10) })
        then : 'We did not render anything, because the component has no size!'
            0 * g.fillRect(0,0,10,10)
        and :
            !cache.hasBufferedImage()

        when : 'We change the size...'
            key0 = key0.withSize(100, 100)
        and : 'Try to do an initial paint into the cache...'
            cache.validate(key0, key0)
            cache.paint(g,  (conf, g2) -> {g2.fillRect(0,0,10,10) })
        then : 'We did not render into the cache, instead we just painted eagerly!'
            1 * g.fillRect(0,0,10,10)
        and :
            !cache.hasBufferedImage()

        when : 'We try to do it a second time...'
            cache.validate(key0, key0)
            cache.paint(g,  (conf, g2) -> {g2.fillRect(0,0,10,10) })
        then : 'Still, we just painted eagerly!'
            1 * g.fillRect(0,0,10,10)
        and :
            !cache.hasBufferedImage()

        when : 'We change the style configuration object to something that can be painted...'
            var key = ComponentExtension.from(
                        UI.button("Hello World").withStyle(conf -> conf
                            .backgroundColor(Color.BLACK)
                            .foregroundColor(Color.WHITE)
                            .size(120, 80)
                            .borderRadius(40)
                        )
                        .get(JButton)
                    )
                    .getConf();

        and : 'Then we do another round of validation and painting...'
            cache.validate(key0, key)
            var didRendering = false
            cache.paint(g,  (conf, g2) -> {
                g2.fillRect(0,0,10,10);
                didRendering = true;
            })
        then : 'We did not paint into the cache!'
            didRendering
            1 * g.fillRect(0,0,10,10)
            0 * g.drawImage({it instanceof BufferedImage},0,0,null)

        and :
            cache.hasBufferedImage()

        when : 'We do it again...'
            cache.validate(key, key)
            didRendering = false
            cache.paint(g,  (conf, g2) -> {
                g2.fillRect(0,0,10,10);
                didRendering = true;
            })
        then : 'Instead of the painter lambda being called, the buffer was used!'
            didRendering
            0 * g.fillRect(0,0,10,10)
            1 * g.drawImage({it instanceof BufferedImage},0,0,null)

        when : 'We repeat this process then the buffer will be used every time!'
            didRendering = false
            cache.validate(key, key)
            cache.paint(g,  (conf, g2) -> {
                g2.fillRect(0,0,10,10);
                didRendering = true;
            })
            cache.validate(key, key)
            cache.paint(g,  (conf, g2) -> {
                g2.fillRect(0,0,10,10);
                didRendering = true;
            })
            cache.validate(key, key)
            cache.paint(g,  (conf, g2) -> {
                g2.fillRect(0,0,10,10);
                didRendering = true;
            })
        then : 'Instead of the painter lambda being called, the buffer was used!'
            !didRendering
            0 * g.fillRect(0,0,10,10)
            3 * g.drawImage({it instanceof BufferedImage},0,0,null)

        when : 'However, we create a successor configuration object...'
            var key2 = ComponentExtension.from(
                        UI.button("Hello World").withStyle(conf -> conf
                            .backgroundColor(Color.BLACK)
                            .foregroundColor(Color.WHITE)
                            .size(120, 80)
                            .borderRadius(40)
                            .foundationColor(Color.GREEN)
                        )
                        .getComponent()
                    )
                    .getConf();
        and : 'And we revalidate the cache...'
            cache.validate(key, key2)
            didRendering = false
            cache.paint(g,  (conf, g2) -> {
                g2.fillRect(0,0,10,10);
                didRendering = true;
            })
        then : 'The painting lambda was used to rerender the cache!'
            didRendering
            1 * g.fillRect(0,0,10,10)
            0 * g.drawImage({it instanceof BufferedImage},0,0,null)
        and : 'And the buffer is still there!'
            cache.hasBufferedImage()

        when : 'We now repeat the process using the successor config...'
            cache.validate(key2, key2)
            cache.paint(g,  (conf, g2) -> {g2.fillRect(0,0,10,10) })
            cache.validate(key2, key2)
            cache.paint(g,  (conf, g2) -> {g2.fillRect(0,0,10,10) })
            cache.validate(key2, key2)
            cache.paint(g,  (conf, g2) -> {g2.fillRect(0,0,10,10) })
        then : 'Instead of the painter lambda being called, the buffer was used again!'
            0 * g.fillRect(0,0,10,10)
            3 * g.drawImage({it instanceof BufferedImage},0,0,null)
    }
}
