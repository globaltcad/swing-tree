package swingtree

import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title
import sprouts.Var
import swingtree.api.IconDeclaration
import swingtree.components.JIcon
import swingtree.layout.Size
import swingtree.style.SvgIcon
import swingtree.threading.EventProcessor
import utility.SwingTreeTestConfigurator

@Title("Displaying Icons")
@Narrative("""

    For displaying icons in you UI declarations,
    SwingTree offers a dedicated component called ´JIcon´,
    an alternative to the standard `JLabel` component,
    commonly used for displaying icons.
    
    The `JIcon` component extends `JLabel` and 
    is more deliberate in its handling of icons.
    
    In this specification, we will explore how
    you can create declarative UIs
    with the `JIcon` component.

""")
@Subject([JIcon, UIForIcon, UI])
class Icon_Spec extends Specification
{
    def setupSpec() {
        SwingTree.initializeUsing(SwingTreeTestConfigurator.get())
        SwingTree.get().setEventProcessor(EventProcessor.COUPLED_STRICT)
        // In this specification we are using the strict event processor
        // which will throw exceptions if we try to perform UI operations in the test thread.
    }

    def cleanupSpec() {
        SwingTree.clear()
    }

    def 'Create a icon UI declaration from a path not leading to an existing image.'()
    {
        reportInfo """
            An important requirement of GUI code is to
            not cause exceptions when the image file is not found.
            This is important to avoid the rest of the GUI from breaking
            and not being responsive to the user or even showing up.
            
            So here we check if we can create a UI declaration
            for the `JIcon` component with an image path that does 
            not lead to an existing image file.
        """
        given : 'We create a UI declaration for an icon at a given path.'
            var ui = UI.icon("path/to/my/icon.png")
        when : 'We create a UI component from the declaration.'
            var component = ui.get(JIcon)
        then : 'The component should be an instance of JIcon.'
            (component instanceof JIcon)
        and : 'The icon should be empty.'
            component.icon.getIconWidth() == -1
            component.icon.getIconHeight() == -1
    }

    def 'Create a icon UI declaration from a non existing image path with a size.'()
    {
        reportInfo """
            An important requirement of GUI code is to
            not cause exceptions when the image file is not found.
            This is important to avoid the rest of the GUI from breaking
            and not being responsive to the user or even showing up.
            
            So here we check if we can create a UI declaration
            for the `JIcon` component with an image path that does 
            not lead to an existing image file.
        """
        given : 'We create a UI declaration for an icon at a given path with a size.'
            var ui = UI.icon(Size.of(32, 32), "path/to/my/icon.png")
        when : 'We create a UI component from the declaration.'
            var component = ui.get(JIcon)
        then : 'The component should be an instance of JIcon.'
            (component instanceof JIcon)
        and : 'The icon should be empty.'
            component.icon.getIconWidth() == -1
            component.icon.getIconHeight() == -1
    }

    def 'Create a icon UI declaration from an existing image source.'()
    {
        given : 'We create a UI declaration for an icon at a given path.'
            var ui = UI.icon("img/swing.png")
        when : 'We create a UI component from the declaration.'
            var component = ui.get(JIcon)
        then : 'The component should be an instance of JIcon.'
            (component instanceof JIcon)
        and : 'The icon should be loaded.'
            component.icon.getIconWidth() > 0
            component.icon.getIconHeight() > 0
    }

    def 'Create a icon UI declaration from an existing image path with a size.'(
        float uiScale
    ) {
        given :
            SwingTree.initializeUsing(it -> it.uiScaleFactor(uiScale) )
        and : 'We create a UI declaration for an icon at a given path with a size.'
            var ui = UI.icon(Size.of(32, 32), "img/swing.png")
        when : 'We create a UI component from the declaration.'
            var component = ui.get(JIcon)
        then : 'The component should be an instance of JIcon.'
            (component instanceof JIcon)
        and : 'The icon should be loaded.'
            component.icon.getIconWidth() == (int) Math.round(32 * uiScale)
            component.icon.getIconHeight() == (int) Math.round(32 * uiScale)

        cleanup :
            SwingTree.clear()

        where :
            uiScale << [1f, 1.5f, 2f]
    }

    def 'Build an icon UI declaration from the `IconDeclaration` type.'( float uiScale )
    {
        reportInfo """
            the `IconDeclaration` type is a value oriented interface
            which is designed to be used to implement constants 
            pointing to icon resources.
            
            This allows you to specify icons with custom sizes
            in your UI and business logic, without having to
            deal with IO loading complexities.
        """
        given :
            SwingTree.initializeUsing(it -> it.uiScaleFactor(uiScale) )
        and :
            var icon = IconDeclaration.of(Size.of(32, 42), "img/swing.png")
        when : 'We create a UI declaration from the icon declaration.'
            var ui = UI.icon(icon)
        then : 'The UI declaration should be created.'
            ui != null
        and : 'The icon should be loaded.'
            var component = ui.get(JIcon)
            component.icon.getIconWidth() == (int) Math.round(32 * uiScale)
            component.icon.getIconHeight() == (int) Math.round(42 * uiScale)

        cleanup :
            SwingTree.clear()

        where :
            uiScale << [1f, 1.5f, 2f]
    }

    def 'Create a `JIcon` dynamically bound to a `Var<IconDeclaration> property.'( float uiScale )
    {
        reportInfo """
            The `Var` type is a reactive type that allows you to
            bind UI components to a value that can change over time.
            
            This is useful for creating dynamic UIs that respond to
            changes in the application state and are also decoupled
            from the business logic.
        """
        given :
            SwingTree.initializeUsing(it -> it.uiScaleFactor(uiScale) )
        and : 'We create a `Var` property with an icon declaration.'
            var icon = Var.of(IconDeclaration.of(Size.of(16, 16), "img/swing.png"))
        and : 'We create a UI declaration for the icon property.'
            var ui = UI.icon(icon)
        when : 'We create a UI component from the declaration.'
            var component = ui.get(JIcon)
        then : 'The component should be an instance of JIcon.'
            (component instanceof JIcon)
        and : 'The icon should be loaded.'
            component.icon.getIconWidth() == (int) Math.round(16 * uiScale)
            component.icon.getIconHeight() == (int) Math.round(16 * uiScale)

        when : 'We update the icon property.'
            icon.set(IconDeclaration.of(Size.of(28, 32), "img/swing.png"))
        then : 'The icon should be updated.'
            component.icon.getIconWidth() == (int) Math.round(28 * uiScale)
            component.icon.getIconHeight() == (int) Math.round(32 * uiScale)

        cleanup :
            SwingTree.clear()

        where :
            uiScale << [1f, 1.5f, 2f]
    }

    def 'The `UI.findIcon(IconDeclaration)` method will load an icon with the correct size and scale.'(
        float uiScale
    ) {
        reportInfo """
            The `UI.findIcon(IconDeclaration)` method is a utility method
            that allows you to load an icon with the correct size and scale
            from an `IconDeclaration` constant.
        """
        given :
            SwingTree.initializeUsing(it -> it.uiScaleFactor(uiScale) )
        and : 'Five different kinds of icon declarations.'
            var iconDeclaration1 = IconDeclaration.of("img/trees.png")
            var iconDeclaration2 = IconDeclaration.of(Size.unknown(), "img/trees.png")
            var iconDeclaration3 = IconDeclaration.of(Size.of(-1, 12), "img/trees.png")
            var iconDeclaration4 = IconDeclaration.of(Size.of(16, -1), "img/trees.png")
            var iconDeclaration5 = IconDeclaration.of(Size.of(16, 12), "img/trees.png")
        when : 'We try to find the icons.'
            var icon1 = UI.findIcon(iconDeclaration1)
            var icon2 = UI.findIcon(iconDeclaration2)
            var icon3 = UI.findIcon(iconDeclaration3)
            var icon4 = UI.findIcon(iconDeclaration4)
            var icon5 = UI.findIcon(iconDeclaration5)
        then : 'They are all present:'
            icon1.isPresent() && icon2.isPresent() && icon3.isPresent() && icon4.isPresent() && icon5.isPresent()
        and : """
            The first icon should have the size from the image file scaled by the UI scale factor.
            The second icon should be the exact same, it should also have the size from the image 
            file scaled by the UI scale factor. 
        """
            icon1.get() === icon2.get()
            icon1.get().getIconWidth() == (int) Math.round(512 * uiScale)
            icon1.get().getIconHeight() == (int) Math.round(512 * uiScale)
        and : """    
            The third icon should have the width scaled by the UI scale factor and the height 
            calculated from the aspect ratio of the image.
            And the same goes for the fourth icon, but with the width calculated from the aspect ratio.
            
            Note that the aspect ratio is 1:1 for this image.
        """
            icon3.get().getIconWidth() == (int) Math.round(12 * uiScale)
            icon3.get().getIconHeight() == (int) Math.round(12 * uiScale)
            icon4.get().getIconWidth() == (int) Math.round(16 * uiScale)
            icon4.get().getIconHeight() == (int) Math.round(16 * uiScale)
        and : """       
            Finally, the fifth icon should have the specified size scaled by the UI scale factor.
            The actual size of the icon should be ignored.
        """
            icon5.get().getIconWidth() == (int) Math.round(16 * uiScale)
            icon5.get().getIconHeight() == (int) Math.round(12 * uiScale)

        cleanup :
            SwingTree.clear()

        where :
            uiScale << [1f, 1.5f, 2f]
    }

    def 'An already loaded icon will dynamically scale to mach the current DPI scale factor.'()
    {
        reportInfo """
            When an icon is loaded, it is loaded at the current DPI scale factor.
            If the DPI scale factor changes after it was already loaded, 
            then the icon will internally reload itself to match the new DPI scale factor.
        """
        given : 'We start with an initial scale of 1.'
            SwingTree.initializeUsing(it -> it.uiScaleFactor(1f) )
        and : 'We load two different types of images, an SVG and a PNG image.'
            var icon1 = UI.findIcon(IconDeclaration.of("img/trees.png").withSize(128, 512))
            var icon2 = UI.findIcon(IconDeclaration.of("img/dandelion.svg").withSize(128, 512))
        expect : 'The icons should have been loaded.'
            icon1.isPresent() && icon2.isPresent()
        and : 'The icons should have the correct size.'
            icon1.get().getIconWidth() == 128
            icon1.get().getIconHeight() == 512
            icon2.get().getIconWidth() == 128
            icon2.get().getIconHeight() == 512
        when : 'We change the scale to 2.'
            SwingTree.get().setUiScaleFactor(2f)
        then : 'The icons should have been reloaded.'
            icon1.get().getIconWidth() == 256
            icon1.get().getIconHeight() == 1024
            icon2.get().getIconWidth() == 256
            icon2.get().getIconHeight() == 1024
        when : 'We change the scale to 1.5.'
            SwingTree.get().setUiScaleFactor(1.5f)
        then : 'The icons should have been reloaded.'
            icon1.get().getIconWidth() == 192
            icon1.get().getIconHeight() == 768
            icon2.get().getIconWidth() == 192
            icon2.get().getIconHeight() == 768
    }

    def 'An SVG created from a String source has the correct dimensions.'(
        float uiScale, int width, int height
    ) {
        reportInfo """
            SVG icons created from a source string-based `IconDeclaration` have 
            the dimensions found in the string. 
            This is particularly useful for creating consistent
            icon sizes across your application.
        """
        given :
            SwingTree.initializeUsing(it -> it.uiScaleFactor(uiScale) )
        and : 'An icon declaration created from the SVG string.'
            IconDeclaration iconDecl = IconDeclaration.ofAutoScaledSvg("""
                                <svg width="${width}" height="${height}" viewBox="0 0 16 16">
                                    <circle cx="8" cy="8" r="6" fill="red"/>
                                </svg>
                            """)
        when : 'We create a UI declaration and extract the component.'
            var component = UI.icon(iconDecl).get(JIcon)
        then : 'The icon should not report any size, since they are loaded as being "scalable".'
            component.icon.getIconWidth() ==  -1
            component.icon.getIconHeight() == -1
        and : 'The icon should be an SvgIcon instance.'
            (component.icon instanceof swingtree.style.SvgIcon)

        when : 'We create a raw SVG icon declaration based component:'
            iconDecl = IconDeclaration.ofSvg(iconDecl.source())
            component = UI.icon(iconDecl).get(JIcon)
        then : 'The icon should have the specified size scaled by UI scale.'
            component.icon.getIconWidth() ==  (int) Math.round(width * uiScale)
            component.icon.getIconHeight() == (int) Math.round(height * uiScale)
        and : 'The icon should also be an SvgIcon instance.'
            (component.icon instanceof swingtree.style.SvgIcon)

        when : 'We create another UI declaration directly from an `SvgIcon`:'
            component = UI.icon(SvgIcon.of(iconDecl.source())).get(JIcon)
        then : 'The icon should have the specified size scaled by UI scale.'
            component.icon.getIconWidth() ==  (int) Math.round(width * uiScale)
            component.icon.getIconHeight() == (int) Math.round(height * uiScale)
        and : 'The icon should also be an SvgIcon instance.'
            (component.icon instanceof swingtree.style.SvgIcon)


        cleanup :
            SwingTree.clear()

        where :
            uiScale | width | height
            1.0     | 32    | 32
            1.5     | 24    | 24
            2.0     | 48    | 48
            1.0     | 16    | 32    // Non-square aspect ratio
            2.0     | 64    | 32    // Rectangular aspect ratio
    }

    def 'Create an icon UI declaration from an SVG string source.'(
        float uiScale, int width, int height, String svg
    ) {
        reportInfo """
            SVG icons can be created programmatically using XML strings.
            This allows for dynamic icon generation without external files.
            
            When using `IconDeclaration.ofSvg(String)`, the SVG content is parsed
            and rendered as a vector icon that scales crisply at any resolution.
        """
        given :
            SwingTree.initializeUsing(it -> it.uiScaleFactor(uiScale) )
        and : 'Create an icon declaration from the SVG string.'
            IconDeclaration iconDecl = IconDeclaration.ofAutoScaledSvg(svg)
        when : 'Create a UI declaration and extract the component.'
            var component = UI.icon(iconDecl).get(JIcon)
        then : 'The icon should not have the specified size, since they are loaded as having a context dependent size.'
            component.icon.getIconWidth() == -1
            component.icon.getIconHeight() == -1
        and : 'The icon should be an SvgIcon instance.'
            (component.icon instanceof swingtree.style.SvgIcon)
        and : 'The source format should be SVG_STRING.'
            iconDecl.sourceFormat() == IconDeclaration.SourceFormat.SVG_STRING

        when : 'Now use the `IconDeclaration.ofSvg` method to create a raw SVG:'
            iconDecl = IconDeclaration.ofSvg(svg)
            component = UI.icon(iconDecl).get(JIcon)
        then : 'The icon should have the specified size scaled by UI scale.'
            component.icon.getIconWidth() == (int) Math.round(width * uiScale)
            component.icon.getIconHeight() == (int) Math.round(height * uiScale)
        and : 'The icon should be an SvgIcon instance.'
            (component.icon instanceof swingtree.style.SvgIcon)

        when : 'Create a UI declaration with a custom `SvgIcon` from the same source:'
            component = UI.icon(SvgIcon.of(iconDecl.source())).get(JIcon)
        then : 'The icon should have the specified size scaled by UI scale.'
            component.icon.getIconWidth() == (int) Math.round(width * uiScale)
            component.icon.getIconHeight() == (int) Math.round(height * uiScale)
        and : 'The icon should be an SvgIcon instance.'
            (component.icon instanceof swingtree.style.SvgIcon)

        cleanup :
            SwingTree.clear()

        where :
            uiScale | width | height || svg
            1.0     | 32    | 32     || '''<svg width="32" height="32" viewBox="0 0 16 16"><circle cx="8" cy="8" r="6" fill="red"/></svg>'''
            1.5     | 24    | 24     || '''<svg width="24" height="24" viewBox="0 0 100 100"><rect x="10" y="10" width="80" height="80" fill="blue"/></svg>'''
            2.0     | 48    | 48     || '''<svg width="48" height="48" viewBox="0 0 24 24"><path d="M12 2L2 7l10 5 10-5-10-5zM2 17l10 5 10-5M2 12l10 5 10-5" stroke="green" stroke-width="2" fill="none"/></svg>'''
            1.0     | 16    | 32     || '''<svg width="16" height="32" viewBox="0 0 8 16"><ellipse cx="4" cy="8" rx="3" ry="6" fill="purple"/></svg>'''
            2.0     | 64    | 32     || '''<svg width="64" height="32" viewBox="0 0 32 16"><polygon points="16,2 30,14 2,14" fill="orange"/></svg>'''
    }

    def 'SVG string icon declarations support percentage-based dimensions that do not resolve to pixels.'(
        float uiScale, Size expectedSize, String svg
    ) {
        reportInfo """
            SVG icons can have percentage-based dimensions (e.g., width="100%", height="50%").
            When loaded through `IconDeclaration`, these percentages cannot be resolved to pixel values
            since percentages are always relative to something else. So the dimensions of the icon are -1!
            
            This is particularly useful for SVG icons designed to be responsive or
            relative to their container.
        """
        given :
            SwingTree.initializeUsing(it -> it.uiScaleFactor(uiScale) )
        and : 'Create an icon declaration from the SVG string.'
            IconDeclaration iconDecl = IconDeclaration.ofAutoScaledSvg(svg)
        when : 'We look for the icon:'
            var iconOpt = UI.findIcon(iconDecl)
            var icon = iconOpt.get()
        then : 'The icon should be present.'
            iconOpt.isPresent()
        and : 'The icon should have no size, since SVGs are designed to scale context dependent.'
            icon.getIconWidth() == -1
            icon.getIconHeight() == -1
        and : 'The base size is also unknown:'
            icon.getBaseWidth() == -1
            icon.getBaseHeight() == -1
        and : 'The icon should be an SvgIcon.'
            (icon instanceof swingtree.style.SvgIcon)

        when : 'We use the `ofSvg` method:'
            iconDecl = IconDeclaration.ofSvg(svg)
        and : 'Again find the icon:'
            iconOpt = UI.findIcon(iconDecl)
            icon = iconOpt.get()
        then : 'The icon should have no pixel-based dimensions (percentage-based is always dynamic).'
            icon.getIconWidth() == expectedSize.width().map(UI::scale).map(Math::round).orElse(-1)
            icon.getIconHeight() == expectedSize.height().map(UI::scale).map(Math::round).orElse(-1)
        and : 'The base size reflects the percentages:'
            icon.getBaseWidth() == expectedSize.width().orElse(-1)
            icon.getBaseHeight() == expectedSize.height().orElse(-1)
        and : 'The icon should be an SvgIcon.'
            (icon instanceof swingtree.style.SvgIcon)

        when : 'Recreate the `SvgIcon` directly:'
            icon = SvgIcon.of(svg)
        then : 'The icon should be present.'
            iconOpt.isPresent()
        and : 'The icon should have no pixel-based dimensions (percentage-based is always dynamic).'
            icon.getIconWidth() == expectedSize.width().map(UI::scale).map(Math::round).orElse(-1)
            icon.getIconHeight() == expectedSize.height().map(UI::scale).map(Math::round).orElse(-1)
        and : 'The base size reflects the percentages:'
            icon.getBaseWidth() == expectedSize.width().orElse(-1)
            icon.getBaseHeight() == expectedSize.height().orElse(-1)
        and : 'The icon should be an SvgIcon.'
            (icon instanceof swingtree.style.SvgIcon)

        cleanup :
            SwingTree.clear()

        where :
            uiScale | expectedSize      || svg
            1.0     | Size.of(-1,-1)    || '''<svg width="100%" height="50%" viewBox="1 0 100 100"><circle cx="50" cy="50" r="40" fill="red"/></svg>'''
            1.5     | Size.of(-1,-1)    || '''<svg width="75%" height="150%" viewBox="2 0 100 100"><rect x="20" y="20" width="60" height="60" fill="green"/></svg>'''
            2.0     | Size.of(-1,-1)    || '''<svg width="100%" height="50%" viewBox="3 0 200 200"><path d="M40,40 L160,40 L100,160 Z" fill="blue"/></svg>'''
            1.0     | Size.of(-1,-1)    || '''<svg width="50%" height="100%" viewBox="4 0 100 50"><ellipse cx="25" cy="25" rx="20" ry="10" fill="yellow"/></svg>'''
            1.0     | Size.of(100, 50)  || '''<svg width="100" height="50px" viewBox="5 0 100 100"><circle cx="50" cy="50" r="40" fill="red"/></svg>'''
            1.5     | Size.of(75, 150)  || '''<svg width="75px" height="150" viewBox="6 0 100 100"><rect x="20" y="20" width="60" height="60" fill="green"/></svg>'''
            2.0     | Size.of(-1, 50)   || '''<svg width="10%" height="50" viewBox="7 0 200 200"><path d="M40,40 L160,40 L100,160 Z" fill="blue"/></svg>'''
            1.0     | Size.of(50, -1)   || '''<svg width="50px" height="90%" viewBox="8 0 100 50"><ellipse cx="25" cy="25" rx="20" ry="10" fill="yellow"/></svg>'''
    }

}
