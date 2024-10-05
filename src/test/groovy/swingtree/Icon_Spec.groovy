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
}
