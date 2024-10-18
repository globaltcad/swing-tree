package swingtree

import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title
import sprouts.Var
import swingtree.api.IconDeclaration
import swingtree.components.JIcon
import swingtree.layout.Size
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
        SwingTree.initialiseUsing(SwingTreeTestConfigurator.get())
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
            component instanceof JIcon
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
            component instanceof JIcon
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
            component instanceof JIcon
        and : 'The icon should be loaded.'
            component.icon.getIconWidth() > 0
            component.icon.getIconHeight() > 0
    }

    def 'Create a icon UI declaration from an existing image path with a size.'(
        float uiScale
    ) {
        given :
            SwingTree.initialiseUsing(it -> it.uiScaleFactor(uiScale) )
        and : 'We create a UI declaration for an icon at a given path with a size.'
            var ui = UI.icon(Size.of(32, 32), "img/swing.png")
        when : 'We create a UI component from the declaration.'
            var component = ui.get(JIcon)
        then : 'The component should be an instance of JIcon.'
            component instanceof JIcon
        and : 'The icon should be loaded.'
            component.icon.getIconWidth() == (int) Math.round(32 * uiScale)
            component.icon.getIconHeight() == (int) Math.round(32 * uiScale)
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
            SwingTree.initialiseUsing(it -> it.uiScaleFactor(uiScale) )
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
            SwingTree.initialiseUsing(it -> it.uiScaleFactor(uiScale) )
        and : 'We create a `Var` property with an icon declaration.'
            var icon = Var.of(IconDeclaration.of(Size.of(16, 16), "img/swing.png"))
        and : 'We create a UI declaration for the icon property.'
            var ui = UI.icon(icon)
        when : 'We create a UI component from the declaration.'
            var component = ui.get(JIcon)
        then : 'The component should be an instance of JIcon.'
            component instanceof JIcon
        and : 'The icon should be loaded.'
            component.icon.getIconWidth() == (int) Math.round(16 * uiScale)
            component.icon.getIconHeight() == (int) Math.round(16 * uiScale)

        when : 'We update the icon property.'
            icon.set(IconDeclaration.of(Size.of(28, 32), "img/swing.png"))
        then : 'The icon should be updated.'
            component.icon.getIconWidth() == (int) Math.round(28 * uiScale)
            component.icon.getIconHeight() == (int) Math.round(32 * uiScale)
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
            SwingTree.initialiseUsing(it -> it.uiScaleFactor(uiScale) )
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
            SwingTree.initialiseUsing(it -> it.uiScaleFactor(1f) )
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
}
