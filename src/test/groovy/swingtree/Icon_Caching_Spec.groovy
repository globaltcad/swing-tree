package swingtree

import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title
import swingtree.api.IconDeclaration
import swingtree.components.JBox
import swingtree.layout.Size

import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.JToggleButton
import java.lang.ref.WeakReference

@Title("SwingTree Icon Caching")
@Narrative('''

    SwingTree does a lot of heavy lifting for
    you when it comes to image asset management.
    It can load, instantiate, cache and unload 
    image resources for you fully automatically.
    
    This is possible through instances of the `IconDeclaration`,
    which is a meta-data holder that can tell you where and
    how to obtain an image.
    Typically, this is a path to a png/jpg/svg but it
    may also be an SVG string...
    Instead of loading and instantiating `ImageIcon`s yourself,
    you only need to create a declaration pointing to the resource:
    
    `var cancel = IconDeclaration.of(path/to/cancel.svg);`
    
    You can then pass this declaration to a SwingTree GUI,
    and the heavy lifting will be done under the hood for you.
    This is especially powerful because SwingTree uses these icon
    declarations as weak cache keys for icons. So when you no longer
    need an icon, you simply get rid of a strong reference to an icon
    declaration! Under the hood these `IconDeclaration`s are also pooled,
    so you can create and hold them in multiple places of your project
    with the guarantee that the cache will be hit and not cleared prematurely.

''')
@Subject([IconDeclaration, SwingTree, UI])
class Icon_Caching_Spec extends Specification
{
    def 'SwingTree maintains a resourceful and robust image cache.'()
    {
        reportInfo """
            SwingTree has a weak hash map in its library
            context object which uses `IconDeclaration`s as keys.
            Since these icon declarations are automatically pooled
            when created through factory methods like `IconDeclaration.of(..)`
            the cache is both reliable in terms of hit rate, while also
            being resourceful, since unused entries get cleared automatically.
        """
        given : 'First we clear the library context so that there is no state leftover from other tests:'
            SwingTree.initialize()
        and : 'We creat a bunch of different icon declarations.'
            var icon1 = IconDeclaration.of("img/trees.png")
            var icon2 = IconDeclaration.of("img/plus.svg")
            var icon3 = IconDeclaration.of(Size.unknown(), "img/a-window-svg")
            var icon4 = IconDeclaration.of(Size.of(-1, 32), "img/seed.png")
            var icon5 = IconDeclaration.ofSvg("<svg width=\"24\" height=\"24\" viewBox=\"0 0 16 16\"><circle cx=\"8\" cy=\"8\" r=\"6\" fill=\"red\"/></svg>")
        expect : 'Initially the cache is empty!'
            SwingTree.get().getIconCache().isEmpty()

        when : 'We use the icons in various ways.'
            var icon1CustomLoad = UI.findIcon(icon1)
            var icon2CustomLoad = UI.findSvgIcon(icon2)
            var icon3Button = UI.toggleButton(icon3).get(JToggleButton)
            var icon4InStyle = UI.panel().withStyle(it->it.image(i->i.image(icon4))).get(JPanel)
            var icon5InStyle = UI.panel().withStyle(it->it.image(i->i.svg(icon5.source()))).get(JPanel)
        then : 'The cache now contains 5 entries, which are our icons!'
            SwingTree.get().getIconCache().size() == 5

        when : 'We reuse the icons...'
            var icon1Reused = UI.findIcon(icon1)
            var icon2Reused = UI.findIcon(icon2)
            var icon3Reused = UI.box().withStyle(it->it.image(i->i.image(icon3))).get(JBox)
            var icon4Reused = UI.findIcon(icon4)
            var icon5Reused = UI.button(icon5).get(JButton)
        then : 'The cache does not increase in size because icons are reused.'
            SwingTree.get().getIconCache().size() == 5

        when : 'We set all the usages to `null`...'
            icon1CustomLoad = null
            icon2CustomLoad = null
            icon3Button = null
            icon4InStyle = null
            icon5InStyle = null
            icon1Reused = null
            icon2Reused = null
            icon3Reused = null
            icon4Reused = null
            icon5Reused = null
        and : 'We wait for the garbage collector...'
            waitForGarbageCollection(2)
        then : 'The cache still full, since we hold on to the declarations:'
            SwingTree.get().getIconCache().size() == 5

        when : 'We replace the existing declarations with the same contents:'
            icon1 = IconDeclaration.of("img/trees.png")
            icon2 = IconDeclaration.of("img/plus.svg")
            icon3 = IconDeclaration.of(Size.unknown(), "img/a-window-svg")
            icon4 = IconDeclaration.of(Size.of(-1, 32), "img/seed.png")
            icon5 = IconDeclaration.ofSvg("<svg width=\"24\" height=\"24\" viewBox=\"0 0 16 16\"><circle cx=\"8\" cy=\"8\" r=\"6\" fill=\"red\"/></svg>")
        and : 'We wait for the garbage collector...'
            waitForGarbageCollection(2)
        then : 'The cache is still not cleared, since we are still holding on to the same (pooled) declarations!'
            SwingTree.get().getIconCache().size() == 5

        when : 'We set the first icon declaration to `null` and wait...'
            icon1 = null
            waitForGarbageCollection(2)
        then : 'The cache finally shinks by 1:'
            SwingTree.get().getIconCache().size() == 4

        when : 'We set the second icon declaration to `null` and wait...'
            icon2 = null
            waitForGarbageCollection(2)
        then : 'The cache shinks again:'
            SwingTree.get().getIconCache().size() == 3


        when : 'We do the third icon declaration...'
            icon3 = null
            waitForGarbageCollection(2)
        then : 'The cache got smaller again:'
            SwingTree.get().getIconCache().size() == 2

        when : 'We do the fourth icon declaration...'
            icon4 = null
            waitForGarbageCollection(2)
        then : 'Only one icon left:'
            SwingTree.get().getIconCache().size() == 1

        when : 'We finally clear the last icon declaration...'
            icon5 = null
            waitForGarbageCollection(2)
        then : 'The cache is empty again!'
            SwingTree.get().getIconCache().isEmpty()
    }

    def 'The `IconDeclaration` factory methods produce pooled objects.'()
    {
        given :
            var path1 = "img/dandelion.png"
            var path2 = "img/seed.png"
            var svg1 = "<svg width=\"24\" height=\"24\" viewBox=\"0 0 16 16\"><circle cx=\"8\" cy=\"8\" r=\"6\" fill=\"red\"/></svg>"
            var svg2 = "<svg width=\"14\" height=\"14\" viewBox=\"0 0 16 16\"><circle cx=\"8\" cy=\"8\" r=\"6\" fill=\"green\"/></svg>"
        expect :
            IconDeclaration.of(path1) == IconDeclaration.of(path1)
            IconDeclaration.of(path1) === IconDeclaration.of(path1)
            IconDeclaration.of(path1) != IconDeclaration.of(path2)
            IconDeclaration.of(path1) !== IconDeclaration.of(path2)
        and :
            IconDeclaration.of(Size.of(-1, 8), path1) == IconDeclaration.of(Size.of(-1, 8), path1)
            IconDeclaration.of(Size.of(-1, 8), path1) === IconDeclaration.of(Size.of(-1, 8), path1)
            IconDeclaration.of(Size.of(4, 6), path1) != IconDeclaration.of(Size.of(-1, 8), path1)
            IconDeclaration.of(Size.of(4, 6), path1) !== IconDeclaration.of(Size.of(-1, 8), path1)
        and :
            IconDeclaration.of(Size.unknown(), path1) == IconDeclaration.of(path1)
            IconDeclaration.of(Size.unknown(), path1) === IconDeclaration.of(path1)
        and :
            IconDeclaration.ofSvg(svg1) == IconDeclaration.ofSvg(svg1)
            IconDeclaration.ofSvg(svg1) === IconDeclaration.ofSvg(svg1)
            IconDeclaration.ofSvg(svg1) != IconDeclaration.ofSvg(svg2)
            IconDeclaration.ofSvg(svg1) !== IconDeclaration.ofSvg(svg2)
        and :
            IconDeclaration.ofAutoScaledSvg(svg1) == IconDeclaration.ofAutoScaledSvg(svg1)
            IconDeclaration.ofAutoScaledSvg(svg1) === IconDeclaration.ofAutoScaledSvg(svg1)
            IconDeclaration.ofAutoScaledSvg(svg1) != IconDeclaration.ofAutoScaledSvg(svg2)
            IconDeclaration.ofAutoScaledSvg(svg1) !== IconDeclaration.ofAutoScaledSvg(svg2)
    }

    /**
     * This method guarantees that garbage collection is
     * done unlike <code>{@link System#gc()}</code> for
     * {@code numberOfCycles} times!
     */
    static void waitForGarbageCollection(int numberOfCycles) {
        Object obj = new Object();
        if ( numberOfCycles > 1 )
            waitForGarbageCollection(numberOfCycles-1);
        WeakReference ref = new WeakReference<>(obj);
        obj = null;
        while(ref.get() != null) {
            System.gc();
        }
    }

    /**
     * This method guarantees that garbage collection is
     * done unlike <code>{@link System#gc()}</code>
     */
    static void waitForGarbageCollection() {
        Object obj = new Object();
        WeakReference ref = new WeakReference<>(obj);
        obj = null;
        while(ref.get() != null) {
            System.gc();
        }
    }
}
