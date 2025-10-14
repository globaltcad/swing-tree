package swingtree

import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title
import sprouts.Var
import swingtree.api.IconDeclaration
import swingtree.components.JSplitButton
import swingtree.layout.Size
import swingtree.style.ScalableImageIcon
import swingtree.style.SvgIcon
import swingtree.threading.EventProcessor
import utility.SwingTreeTestConfigurator

import javax.swing.*
import java.awt.image.BufferedImage

@Title("Button Icons")
@Narrative('''
    
    In SwingTree there are various kinds of
    button based widget types. All of these
    button types can have different kinds of icons
    for different parts of a user interaction:
    
    - A standard icon for most situation.
    - An on rollover / hover icon for when the mouse 
      cursor moves over the button type.
    - A on press icon, which is shown when the user
      presses the button.
    - An icon shown when the component is disabled
    - An icon shown when the component is selected
    - An icon shown when the component is selected AND disabled
    - An icon shown when the component is selected AND being hovered over
     
    In this specification we show you all of the methods
    for specifying these icons and ensure that they wor as intended.

''')
@Subject([UIForAnyButton, UIForToggleButton, UIForRadioButton, UIForMenuItem])
class Button_Icons_Spec extends Specification
{
    def setup() {
        SwingTree.initialiseUsing(SwingTreeTestConfigurator.get())
        SwingTree.get().setEventProcessor(EventProcessor.COUPLED)
    }

    def cleanup() {
        SwingTree.clear()
    }


    def 'We can use `withIcon( Icon icon )` to specify a regular icon.'(
        float uiScale
    ) {
        reportInfo """
            A regular icon is the most basic kind of icon you can set
            on a button based component. It will be shown in most situations
            but can be overridden by other icons for specific situations
            (like when the button is hovered over, pressed, disabled, etc).
        """
        given : """
            We first set a scaling factor to simulate a platform with higher DPI.
        """
            SwingTree.get().setUiScaleFactor(uiScale)
        and : 'We create a new `ImageIcon` for testing purposes.'
            ImageIcon testIcon = new ImageIcon( new BufferedImage(7,13,BufferedImage.TYPE_INT_ARGB) )
        and : 'Then we create a `JMenuItem` and set its icon using `withIcon( Icon icon )`.'
            JMenuItem menuItem = UI.menuItem("I have a regular icon")
                                   .withIcon( testIcon )
                                   .get(JMenuItem)
        expect : 'The icon of the `JMenuItem` to be a correct image with scaling support.'
            menuItem.getIcon() instanceof ScalableImageIcon
            menuItem.getIcon().getIconWidth() == (7 * uiScale) as int
            menuItem.getIcon().getIconHeight() == (13 * uiScale) as int

        where : 'We test this with different UI scaling factors.'
            uiScale << [1, 2, 3]
    }

    def 'We can use `withIcon( IconDeclaration icon )` to specify a regular icon.'(float uiScale)
    {
        reportInfo """
            An `IconDeclaration` is an interface which describes the location of an icon resource
            as well as some additional metadata about the icon.
            Its implementations are intended to be constants, which can be reused throughout your application.
            
            In this test we use the `IconDeclaration` to define the icon of a button based component.
            The actual `Icon` will be loaded automatically by SwingTree.
        """
        given : """
            We first set a scaling factor to simulate a platform with higher DPI.
            So when your screen has a higher pixel density then this factor
            is used by SwingTree to ensure that the UI is upscaled accordingly!
            This is especially important for icons, as they would otherwise
            appear very small on high DPI screens.
            Please note that the line below only exists for testing purposes, 
            SwingTree will determine a suitable 
            scaling factor for the current system automatically for you,
            so you do not have to specify this factor manually. 
        """
            SwingTree.get().setUiScaleFactor(uiScale)
        and : 'We create a new `IconDeclaration` for testing purposes.'
            IconDeclaration treesIcon = IconDeclaration.of(Size.of(16, 12), "img/trees.png")
        and : 'Then we create a `JRadioButton` and set its icon using `withIcon( IconDeclaration icon )`.'
            JRadioButton radioButton = UI.radioButton("I have a regular icon")
                                      .withIcon( treesIcon )
                                      .get(JRadioButton)
        expect : 'The icon of the `JRadioButton` to be loaded and set correctly.'
            radioButton.getIcon() instanceof ScalableImageIcon
            radioButton.getIcon().getIconWidth() == (16 * uiScale) as int
            radioButton.getIcon().getIconHeight() == (12 * uiScale) as int

        where : 'We test this with different UI scaling factors.'
            uiScale << [1, 2, 3]
    }

    def 'We can use `withIcon( Val<IconDeclaration> icon )` to make a regular icon reactive.'(float uiScale)
    {
        reportInfo """
            You can use a `Var` property of an `IconDeclaration` to make the icon of a button 
            based component reactive. This means that whenever the value of the `Var` changes, 
            the icon of the component will be updated automatically.
            
            In this test we use a `Var<IconDeclaration>` to define the icon of a button based component.
            The actual `Icon` will be loaded automatically by SwingTree and then when we change the value
            of the `Var`, the icon of the component will be updated.
        """
        given : """
            We first set a scaling factor to simulate a platform with higher DPI.
            So when your screen has a higher pixel density then this factor
            is used by SwingTree to ensure that the UI is upscaled accordingly!
            This is especially important for icons, as they would otherwise
            appear very small on high DPI screens.
            Please note that the line below only exists for testing purposes, 
            SwingTree will determine a suitable 
            scaling factor for the current system automatically for you,
            so you do not have to specify this factor manually. 
        """
            SwingTree.get().setUiScaleFactor(uiScale)
        and : 'First we create two constants of `IconDeclaration` for testing purposes.'
            IconDeclaration swingIcon = IconDeclaration.of(Size.of(22, 13), "img/swing.png")
            IconDeclaration treesIcon = IconDeclaration.of(Size.of(42, 64), "img/trees.png")
        and : 'Then we create a `Var<IconDeclaration>` and set its initial value to the `swingIcon` declaration.'
            Var<IconDeclaration> iconProperty = Var.of(swingIcon)
        and : 'Next we create a regular `JButton` and set its icon using the `Var<IconDeclaration>`.'
            JButton button = UI.button("I have a reactive icon")
                             .withIcon( iconProperty )
                             .get(JButton)
        expect : 'The icon of the `JButton` is loaded and set correctly to the initial value.'
            button.getIcon() instanceof ScalableImageIcon
            button.getIcon().getIconWidth() == (22 * uiScale) as int
            button.getIcon().getIconHeight() == (13 * uiScale) as int

        when : 'We change the value of the `Var<IconDeclaration>` property to the `treesIcon` declaration.'
            iconProperty.set(treesIcon)
        and : 'We wait for the Event Dispatch Thread to process all events.'
            UI.sync()
        then : 'We can confirm that the icon of the `JButton` was set to the new value.'
            button.getIcon() instanceof ScalableImageIcon
            button.getIcon().getIconWidth() == (42 * uiScale) as int
            button.getIcon().getIconHeight() == (64 * uiScale) as int

        where : 'We test this with different UI scaling factors.'
            uiScale << [1, 2, 3]
    }

    def 'Use `withIcon( int width, int height, Icon icon )` to specify a regular icon with a custom size.'()
    {
        reportInfo """
            You can specify a custom size for an icon by providing the desired width and height
            along with the `Icon` instance itself. This is useful when you want to ensure that
            the icon fits well within the layout of your button based component.
            
            In this test we create a simple `ImageIcon` and then set it on a button based component
            using the `withIcon( int width, int height, Icon icon )` method, specifying a custom size.
        """
        given : 'We create a new `ImageIcon` for testing purposes.'
            ImageIcon testIcon = new ImageIcon( new BufferedImage(32,32,BufferedImage.TYPE_INT_ARGB) )
        and : 'Then we create a `JCheckBox` and set its icon using `withIcon( int width, int height, Icon icon )` with a custom size.'
            JCheckBox checkBox = UI.checkBox("I have a regular icon with custom size")
                                .withIcon(24, 19, testIcon)
                                .get(JCheckBox)
        expect : 'The icon of the `JCheckBox` is NOT the exact same instance as the one we set, but has the correct custom size.'
            checkBox.getIcon() !== testIcon
            checkBox.getIcon().getIconWidth() == 24
            checkBox.getIcon().getIconHeight() == 19
    }

    def 'We can use `withIcon( int width, int height, IconDeclaration icon )` to specify a regular icon with custom size.'(float uiScale)
    {
        reportInfo """
            You can specify a custom size for an icon by providing the desired width and height
            along with an `IconDeclaration`. This is useful when you want to ensure that
            the icon fits well within the layout of your button based component.
            
            In this test we create an `IconDeclaration` and then set it on a button based component
            using the `withIcon( int width, int height, IconDeclaration icon )` method, specifying a custom size.
        """
        given : """
            We first set a scaling factor to simulate a platform with higher DPI.
            So when your screen has a higher pixel density then this factor
            is used by SwingTree to ensure that the UI is upscaled accordingly!
            This is especially important for icons, as they would otherwise
            appear very small on high DPI screens.
            Please note that the line below only exists for testing purposes, 
            SwingTree will determine a suitable 
            scaling factor for the current system automatically for you,
            so you do not have to specify this factor manually. 
        """
            SwingTree.get().setUiScaleFactor(uiScale)
        and : 'We create a new `IconDeclaration` for testing purposes.'
            IconDeclaration treesIcon = IconDeclaration.of(Size.of(42, 24), "img/seed.png")
        and : 'Then we build a `JRadioButtonMenuItem` and set its icon using `withIcon( int width, int height, IconDeclaration icon )` with a custom size.'
            var radioButtonMenuItem = UI.radioButtonMenuItem("I have a regular icon with custom size")
                                              .withIcon(33, 19, treesIcon)
                                              .get(JRadioButtonMenuItem)
        expect : 'The icon of the `JRadioButtonMenuItem` is loaded and has the correct custom size.'
            radioButtonMenuItem.getIcon() instanceof ScalableImageIcon
            radioButtonMenuItem.getIcon().getIconWidth() == (33 * uiScale) as int
            radioButtonMenuItem.getIcon().getIconHeight() == (19 * uiScale) as int
        where : 'We test this with different UI scaling factors.'
            uiScale << [1, 2, 3]
    }

    def 'Use `withIcon( int width, int height, IconDeclaration icon, UI.FitComponent fitComponent )` to specify a regular icon with custom size and layout.'(
            float uiScale
    ) {
        reportInfo """
            You can specify a custom size and layout for a regular icon by providing the desired width, height,
            `IconDeclaration`, and a `UI.FitComponent` value. This is useful when you want to ensure that
            the icon fits well within the layout of your button based component and behaves in a specific way
            when the component is resized.
            
            In this test we create an `IconDeclaration` and then set it on a button based component
            using the `withIcon( int width, int height, IconDeclaration icon, UI.FitComponent fitComponent )` method,
            specifying a custom size and layout behavior.
        """
        given : """
            We first set a scaling factor to simulate a platform with higher DPI.
            So when your screen has a higher pixel density then this factor
            is used by SwingTree to ensure that the UI is upscaled accordingly!
            This is especially important for icons, as they would otherwise
            appear very small on high DPI screens.
            Please note that the line below only exists for testing purposes, 
            SwingTree will determine a suitable 
            scaling factor for the current system automatically for you,
            so you do not have to specify this factor manually. 
        """
            SwingTree.get().setUiScaleFactor(uiScale)
        and : 'We create a new `IconDeclaration` for testing purposes.'
            IconDeclaration treesIcon = IconDeclaration.of(Size.of(73, 66), "img/two-16th-notes.svg")
        and : 'Then we build a `JSplitButton` and set its icon using `withIcon( int width, int height, IconDeclaration icon, UI.FitComponent fitComponent )` with a custom size and layout.'
            var splitButton = UI.splitButton("I have a regular icon with custom size and layout")
                                .withIcon(55, 44, treesIcon, UI.FitComponent.WIDTH)
                                .get(JSplitButton)
        expect : 'The icon of the `JSplitButton` is loaded and has the correct custom size.'
            splitButton.getIcon() instanceof SvgIcon
            splitButton.getIcon().getIconWidth() == (55 * uiScale) as int
            splitButton.getIcon().getIconHeight() == (44 * uiScale) as int
        where : 'We test this with different UI scaling factors.'
            uiScale << [1, 2, 3]
    }

    def 'We can use `withIcon( Icon icon, UI.FitComponent fitComponent )` to specify a regular icon with custom layout.'(float uiScale)
    {
        reportInfo """
            You can specify a layout behavior for a regular icon by providing the `Icon` instance
            along with a `UI.FitComponent` value. This is useful when you want to ensure that
            the icon behaves in a specific way when the button based component is resized.
            
            In this test we create a simple `ImageIcon` and then set it on a button based component
            using the `withIcon( Icon icon, UI.FitComponent fitComponent )` method, specifying a layout behavior.
            
            Note that this only really works well with vector based icons (like SVG icons),
            as raster based icons (like PNG icons) will simply be scaled which can lead to
            a loss in quality and blurriness.
        """
        given : """
            We first set a scaling factor to simulate a platform with higher DPI.
            So when your screen has a higher pixel density then this factor
            is used by SwingTree to ensure that the UI is upscaled accordingly!
            This is especially important for icons, as they would otherwise
            appear very small on high DPI screens.
            Please note that the line below only exists for testing purposes, 
            SwingTree will determine a suitable 
            scaling factor for the current system automatically for you,
            so you do not have to specify this factor manually. 
        """
            SwingTree.get().setUiScaleFactor(uiScale)
        and : 'We create a new `ImageIcon` for testing purposes.'
            ImageIcon testIcon = new ImageIcon( new BufferedImage(42,32,BufferedImage.TYPE_INT_ARGB) )
        and : 'Then we create a `JButton` and set its icon using `withIcon( Icon icon, UI.FitComponent fitComponent )` with a custom layout behavior.'
            JButton button = UI.button("Click!").withSizeExactly(80, 30)
                                .withIcon( testIcon, UI.FitComponent.NO )
                                .get(JButton)
        expect : 'The icon of the `JButton` has the expected dimensions!'
            button.getIcon().getIconWidth() == UI.scale(42)
            button.getIcon().getIconHeight() == UI.scale(32)

        when : 'We re-create the button with the "WIDTH" fit component option.'
            button = UI.button("I have a regular icon with custom layout").withSizeExactly(80, 30)
                     .withIcon( testIcon, UI.FitComponent.WIDTH )
                     .get(JButton)
        then : 'The icon of the `JButton` has the expected dimensions!'
            button.getIcon().getIconWidth() == UI.scale(80 - button.insets.left - button.insets.right)
            button.getIcon().getIconHeight() == UI.scale(32)

        when : 'We re-create the button with the "HEIGHT" fit component option.'
            button = UI.button("I have a regular icon with custom layout").withSizeExactly(80, 30)
                     .withIcon( testIcon, UI.FitComponent.HEIGHT )
                     .get(JButton)
        then : 'The icon of the `JButton` has the expected dimensions!'
            button.getIcon().getIconWidth() == UI.scale(42)
            button.getIcon().getIconHeight() == UI.scale(30 - button.insets.top - button.insets.bottom)

        when : 'We re-create the button with the "WIDTH_AND_HEIGHT" fit component option.'
            button = UI.button("I have a regular icon with custom layout").withSizeExactly(80, 30)
                     .withIcon( testIcon, UI.FitComponent.WIDTH_AND_HEIGHT )
                     .get(JButton)
        then : 'The icon of the `JButton` has the expected dimensions!'
            button.getIcon().getIconWidth() == UI.scale(80 - button.insets.left - button.insets.right)
            button.getIcon().getIconHeight() == UI.scale(30 - button.insets.top - button.insets.bottom)

        when : 'We re-create the button with the "MIN_DIM" fit component option.'
            button = UI.button("I have a regular icon with custom layout").withSizeExactly(80, 30)
                     .withIcon( testIcon, UI.FitComponent.MIN_DIM )
                     .get(JButton)
        then : 'The icon of the `JButton` has the expected dimensions!'
            int minDim = Math.min(80 - button.insets.left - button.insets.right, 30 - button.insets.top - button.insets.bottom)
            button.getIcon().getIconWidth() == UI.scale(minDim)
            button.getIcon().getIconHeight() == UI.scale(minDim)

        when : 'We re-create the button with the "MAX_DIM" fit component option.'
            button = UI.button("I have a regular icon with custom layout").withSizeExactly(80, 30)
                     .withIcon( testIcon, UI.FitComponent.MAX_DIM )
                     .get(JButton)
        then : 'The icon of the `JButton` has the expected dimensions!'
            int maxDim = Math.max(80 - button.insets.left - button.insets.right, 30 - button.insets.top - button.insets.bottom)
            button.getIcon().getIconWidth() == UI.scale(maxDim)
            button.getIcon().getIconHeight() == UI.scale(maxDim)

        where : 'We test this with different UI scaling factors.'
            uiScale << [1, 2, 3]
    }

    def 'We can use `withIcon( IconDeclaration icon, UI.FitComponent fitComponent )` to specify a regular icon with custom layout.'(
            float uiScale
    ) {
        reportInfo """
            You can specify a layout behavior for a regular icon specified through an `IconDeclaration`
            along with a `UI.FitComponent` value. This is useful when you want to ensure that
            the icon behaves in a specific way when the button based component is resized.
            
            In this test we create an `IconDeclaration` and then set it on a button based component
            using the `withIcon( IconDeclaration icon, UI.FitComponent fitComponent )` method, 
            specifying a layout behavior.
            
            Note that this only really works well with vector based icons (like SVG icons),
            as raster based icons (like PNG icons) will simply be scaled which can lead to
            a loss in quality and blurriness.
        """
        given : """
            We first set a scaling factor to simulate a platform with higher DPI.
            So when your screen has a higher pixel density then this factor
            is used by SwingTree to ensure that the UI is upscaled accordingly!
            This is especially important for icons, as they would otherwise
            appear very small on high DPI screens.
            Please note that the line below only exists for testing purposes, 
            SwingTree will determine a suitable 
            scaling factor for the current system automatically for you,
            so you do not have to specify this factor manually. 
        """
            SwingTree.get().setUiScaleFactor(uiScale)
        and : 'We create a new `IconDeclaration` for testing purposes.'
            IconDeclaration treesIcon = IconDeclaration.of(Size.of(42,32), "img/trees.png")
        and : 'Then we create a `JToggleButton` and set its icon using `withIcon( IconDeclaration icon, UI.FitComponent fitComponent )` with a custom layout behavior.'
            JToggleButton toggleButton = UI.toggleButton("I have a regular icon with custom layout").withSizeExactly(80, 30)
                                       .withIcon( treesIcon, UI.FitComponent.NO )
                                       .get(JToggleButton)
        expect : 'The icon of the `JToggleButton` has the expected dimensions!'
            toggleButton.getIcon().getIconWidth() == UI.scale(42)
            toggleButton.getIcon().getIconHeight() == UI.scale(32)

        when : 'We re-create the button with the "WIDTH" fit component option.'
            toggleButton = UI.toggleButton("I have a regular icon with custom layout").withSizeExactly(80, 30)
                           .withIcon( treesIcon, UI.FitComponent.WIDTH )
                           .get(JToggleButton)
        then : 'The icon of the `JToggleButton` has the expected dimensions!'
            toggleButton.getIcon().getIconWidth() == UI.scale(80 - toggleButton.insets.left - toggleButton.insets.right)
            toggleButton.getIcon().getIconHeight() == UI.scale(32)

        when : 'We re-create the button with the "HEIGHT" fit component option.'
            toggleButton = UI.toggleButton("I have a regular icon with custom layout").withSizeExactly(80, 30)
                            .withSizeExactly(80, 30)
                            .withIcon( treesIcon, UI.FitComponent.HEIGHT )
                            .get(JToggleButton)
        then : 'The icon of the `JToggleButton` has the expected dimensions!'
            toggleButton.getIcon().getIconWidth() == UI.scale(42)
            toggleButton.getIcon().getIconHeight() == UI.scale(30 - toggleButton.insets.top - toggleButton.insets.bottom)

        when : 'We re-create the button with the "WIDTH_AND_HEIGHT" fit component option.'
            toggleButton = UI.toggleButton("I have a regular icon with custom layout").withSizeExactly(80, 30)
                            .withSizeExactly(80, 30)
                            .withIcon( treesIcon, UI.FitComponent.WIDTH_AND_HEIGHT )
                            .get(JToggleButton)
        then : 'The icon of the `JToggleButton` has the expected dimensions!'
            toggleButton.getIcon().getIconWidth() == UI.scale(80 - toggleButton.insets.left - toggleButton.insets.right)
            toggleButton.getIcon().getIconHeight() == UI.scale(30 - toggleButton.insets.top - toggleButton.insets.bottom)

        when : 'We re-create the button with the "MIN_DIM" fit component option.'
            toggleButton = UI.toggleButton("I have a regular icon with custom layout").withSizeExactly(80, 30)
                            .withIcon( treesIcon, UI.FitComponent.MIN_DIM )
                            .get(JToggleButton)
        then : 'The icon of the `JToggleButton` has the expected dimensions!'
            int minDim = Math.min(80 - toggleButton.insets.left - toggleButton.insets.right, 30 - toggleButton.insets.top - toggleButton.insets.bottom)
            toggleButton.getIcon().getIconWidth() == UI.scale(minDim)
            toggleButton.getIcon().getIconHeight() == UI.scale(minDim)

        when : 'We re-create the button with the "MAX_DIM" fit component option.'
            toggleButton = UI.toggleButton("I have a regular icon with custom layout").withSizeExactly(80, 30)
                            .withIcon( treesIcon, UI.FitComponent.MAX_DIM )
                            .get(JToggleButton)
        then : 'The icon of the `JToggleButton` has the expected dimensions!'
            int maxDim = Math.max(80 - toggleButton.insets.left - toggleButton.insets.right, 30 - toggleButton.insets.top - toggleButton.insets.bottom)
            toggleButton.getIcon().getIconWidth() == UI.scale(maxDim)
            toggleButton.getIcon().getIconHeight() == UI.scale(maxDim)

        where : 'We test this with different UI scaling factors.'
            uiScale << [2, 3]
    }

    // On Press Icon:

    def 'We can use `withIconOnPress( Icon icon )` to specify a pressed icon.'(
        float uiScale
    ) {
        reportInfo """
            A pressed icon is shown when the user presses the button based component.
            This provides visual feedback during the press interaction.
        """
        given : """
            We first set a scaling factor to simulate a platform with higher DPI.
        """
            SwingTree.get().setUiScaleFactor(uiScale)
        and : 'We create a new `ImageIcon` for testing purposes.'
            ImageIcon testIcon = new ImageIcon( new BufferedImage(7,13,BufferedImage.TYPE_INT_ARGB) )
        and : 'Then we create a `JMenuItem` and set its pressed icon using `withIconOnPress( Icon icon )`.'
            JMenuItem menuItem = UI.menuItem("I have a pressed icon")
                                   .withIconOnPress( testIcon )
                                   .get(JMenuItem)
        expect : 'The pressed icon of the `JMenuItem` to be a properly scaled icon.'
            menuItem.getPressedIcon() instanceof ScalableImageIcon
            menuItem.getPressedIcon().getIconWidth() == (7 * uiScale) as int
            menuItem.getPressedIcon().getIconHeight() == (13 * uiScale) as int

        where : 'We test this with different UI scaling factors.'
            uiScale << [1, 2, 3]
    }

    def 'We can use `withIconOnPress( IconDeclaration icon )` to specify a pressed icon.'(float uiScale)
    {
        reportInfo """
            An `IconDeclaration` can be used to define the pressed icon of a button based component.
            The actual `Icon` will be loaded automatically by SwingTree and shown when the user presses the component.
        """
        given : """
            We first set a scaling factor to simulate a platform with higher DPI.
        """
            SwingTree.get().setUiScaleFactor(uiScale)
        and : 'We create a new `IconDeclaration` for testing purposes.'
            IconDeclaration treesIcon = IconDeclaration.of(Size.of(16, 12), "img/trees.png")
        and : 'Then we create a `JRadioButton` and set its pressed icon using `withIconOnPress( IconDeclaration icon )`.'
            JRadioButton radioButton = UI.radioButton("I have a pressed icon")
                                      .withIconOnPress( treesIcon )
                                      .get(JRadioButton)
        expect : 'The pressed icon of the `JRadioButton` to be loaded and set correctly.'
            radioButton.getPressedIcon() instanceof ScalableImageIcon
            radioButton.getPressedIcon().getIconWidth() == (16 * uiScale) as int
            radioButton.getPressedIcon().getIconHeight() == (12 * uiScale) as int

        where : 'We test this with different UI scaling factors.'
            uiScale << [1, 2, 3]
    }

    def 'We can use `withIconOnPress( Val<IconDeclaration> icon )` to make a pressed icon reactive.'(float uiScale)
    {
        reportInfo """
            You can use a `Var` property of an `IconDeclaration` to make the pressed icon of a button 
            based component reactive. This means that whenever the value of the `Var` changes, 
            the pressed icon of the component will be updated automatically.
        """
        given : """
            We first set a scaling factor to simulate a platform with higher DPI.
        """
            SwingTree.get().setUiScaleFactor(uiScale)
        and : 'First we create two constants of `IconDeclaration` for testing purposes.'
            IconDeclaration swingIcon = IconDeclaration.of(Size.of(22, 13), "img/swing.png")
            IconDeclaration treesIcon = IconDeclaration.of(Size.of(42, 64), "img/trees.png")
        and : 'Then we create a `Var<IconDeclaration>` and set its initial value to the `swingIcon` declaration.'
            Var<IconDeclaration> iconProperty = Var.of(swingIcon)
        and : 'Next we create a regular `JButton` and set its pressed icon using the `Var<IconDeclaration>`.'
            JButton button = UI.button("I have a reactive pressed icon")
                             .withIconOnPress( iconProperty )
                             .get(JButton)
        expect : 'The pressed icon of the `JButton` is loaded and set correctly to the initial value.'
            button.getPressedIcon() instanceof ScalableImageIcon
            button.getPressedIcon().getIconWidth() == (22 * uiScale) as int
            button.getPressedIcon().getIconHeight() == (13 * uiScale) as int

        when : 'We change the value of the `Var<IconDeclaration>` property to the `treesIcon` declaration.'
            iconProperty.set(treesIcon)
        and : 'We wait for the Event Dispatch Thread to process all events.'
            UI.sync()
        then : 'We can confirm that the pressed icon of the `JButton` was set to the new value.'
            button.getPressedIcon() instanceof ScalableImageIcon
            button.getPressedIcon().getIconWidth() == (42 * uiScale) as int
            button.getPressedIcon().getIconHeight() == (64 * uiScale) as int

        where : 'We test this with different UI scaling factors.'
            uiScale << [1, 2, 3]
    }

    def 'Use `withIconOnPress( int width, int height, Icon icon )` to specify a pressed icon with a custom size.'()
    {
        reportInfo """
            You can specify a custom size for a pressed icon by providing the desired width and height
            along with the `Icon` instance itself. This ensures that the pressed icon fits well within 
            the layout of your button based component during the press interaction.
        """
        given : 'We create a new `ImageIcon` for testing purposes.'
            ImageIcon testIcon = new ImageIcon( new BufferedImage(32,32,BufferedImage.TYPE_INT_ARGB) )
        and : 'Then we create a `JCheckBox` and set its pressed icon using `withIconOnPress( int width, int height, Icon icon )` with a custom size.'
            JCheckBox checkBox = UI.checkBox("I have a pressed icon with custom size")
                                .withIconOnPress(24, 19, testIcon)
                                .get(JCheckBox)
        expect : 'The pressed icon of the `JCheckBox` is NOT the exact same instance as the one we set, but has the correct custom size.'
            checkBox.getPressedIcon() !== testIcon
            checkBox.getPressedIcon().getIconWidth() == 24
            checkBox.getPressedIcon().getIconHeight() == 19
    }

    def 'We can use `withIconOnPress( int width, int height, IconDeclaration icon )` to specify a pressed icon with custom size.'(float uiScale)
    {
        reportInfo """
            You can specify a custom size for a pressed icon by providing the desired width and height
            along with an `IconDeclaration`. This ensures proper sizing of the pressed icon during user interaction.
        """
        given : """
            We first set a scaling factor to simulate a platform with higher DPI.
        """
            SwingTree.get().setUiScaleFactor(uiScale)
        and : 'We create a new `IconDeclaration` for testing purposes.'
            IconDeclaration treesIcon = IconDeclaration.of(Size.of(42, 24), "img/seed.png")
        and : 'Then we build a `JRadioButtonMenuItem` and set its pressed icon using `withIconOnPress( int width, int height, IconDeclaration icon )` with a custom size.'
            var radioButtonMenuItem = UI.radioButtonMenuItem("I have a pressed icon with custom size")
                                              .withIconOnPress(33, 19, treesIcon)
                                              .get(JRadioButtonMenuItem)
        expect : 'The pressed icon of the `JRadioButtonMenuItem` is loaded and has the correct custom size.'
            radioButtonMenuItem.getPressedIcon() instanceof ScalableImageIcon
            radioButtonMenuItem.getPressedIcon().getIconWidth() == (33 * uiScale) as int
            radioButtonMenuItem.getPressedIcon().getIconHeight() == (19 * uiScale) as int
        where : 'We test this with different UI scaling factors.'
            uiScale << [1, 2, 3]
    }

    def 'Use `withIconOnPress( int width, int height, IconDeclaration icon, UI.FitComponent fitComponent )` to specify a pressed icon with custom size and layout.'(
            float uiScale
    ) {
        reportInfo """
            You can specify a custom size and layout for a pressed icon by providing the desired width, height,
            `IconDeclaration`, and a `UI.FitComponent` value. This ensures the pressed icon behaves correctly
            when the component is resized during the press interaction.
        """
        given : """
            We first set a scaling factor to simulate a platform with higher DPI.
        """
            SwingTree.get().setUiScaleFactor(uiScale)
        and : 'We create a new `IconDeclaration` for testing purposes.'
            IconDeclaration treesIcon = IconDeclaration.of(Size.of(73, 66), "img/two-16th-notes.svg")
        and : 'Then we build a `JSplitButton` and set its pressed icon using `withIconOnPress( int width, int height, IconDeclaration icon, UI.FitComponent fitComponent )` with a custom size and layout.'
            var splitButton = UI.splitButton("I have a pressed icon with custom size and layout")
                                .withIconOnPress(55, 44, treesIcon, UI.FitComponent.WIDTH)
                                .get(JSplitButton)
        expect : 'The pressed icon of the `JSplitButton` is loaded and has the correct custom size.'
            splitButton.getPressedIcon() instanceof SvgIcon
            splitButton.getPressedIcon().getIconWidth() == (55 * uiScale) as int
            splitButton.getPressedIcon().getIconHeight() == (44 * uiScale) as int
        where : 'We test this with different UI scaling factors.'
            uiScale << [1, 2, 3]
    }

    def 'We can use `withIconOnPress( Icon icon, UI.FitComponent fitComponent )` to specify a pressed icon with custom layout.'(float uiScale)
    {
        reportInfo """
            You can specify a layout behavior for a pressed icon by providing the `Icon` instance
            along with a `UI.FitComponent` value. This ensures the pressed icon behaves correctly
            when the button based component is resized during the press interaction.
        """
        given : """
            We first set a scaling factor to simulate a platform with higher DPI.
        """
            SwingTree.get().setUiScaleFactor(uiScale)
        and : 'We create a new `ImageIcon` for testing purposes.'
            ImageIcon testIcon = new ImageIcon( new BufferedImage(42,32,BufferedImage.TYPE_INT_ARGB) )
        and : 'Then we create a `JButton` and set its pressed icon using `withIconOnPress( Icon icon, UI.FitComponent fitComponent )` with a custom layout behavior.'
            JButton button = UI.button("Click!").withSizeExactly(80, 30)
                                .withIconOnPress( testIcon, UI.FitComponent.NO )
                                .get(JButton)
        expect : 'The pressed icon of the `JButton` has the expected dimensions!'
            button.getPressedIcon().getIconWidth() == UI.scale(42)
            button.getPressedIcon().getIconHeight() == UI.scale(32)

        when : 'We re-create the button with the "WIDTH" fit component option.'
            button = UI.button("I have a pressed icon with custom layout").withSizeExactly(80, 30)
                     .withIconOnPress( testIcon, UI.FitComponent.WIDTH )
                     .get(JButton)
        then : 'The pressed icon of the `JButton` has the expected dimensions!'
            button.getPressedIcon().getIconWidth() == UI.scale(80 - button.insets.left - button.insets.right)
            button.getPressedIcon().getIconHeight() == UI.scale(32)

        when : 'We re-create the button with the "HEIGHT" fit component option.'
            button = UI.button("I have a pressed icon with custom layout").withSizeExactly(80, 30)
                     .withIconOnPress( testIcon, UI.FitComponent.HEIGHT )
                     .get(JButton)
        then : 'The pressed icon of the `JButton` has the expected dimensions!'
            button.getPressedIcon().getIconWidth() == UI.scale(42)
            button.getPressedIcon().getIconHeight() == UI.scale(30 - button.insets.top - button.insets.bottom)

        when : 'We re-create the button with the "WIDTH_AND_HEIGHT" fit component option.'
            button = UI.button("I have a pressed icon with custom layout").withSizeExactly(80, 30)
                     .withIconOnPress( testIcon, UI.FitComponent.WIDTH_AND_HEIGHT )
                     .get(JButton)
        then : 'The pressed icon of the `JButton` has the expected dimensions!'
            button.getPressedIcon().getIconWidth() == UI.scale(80 - button.insets.left - button.insets.right)
            button.getPressedIcon().getIconHeight() == UI.scale(30 - button.insets.top - button.insets.bottom)

        when : 'We re-create the button with the "MIN_DIM" fit component option.'
            button = UI.button("I have a pressed icon with custom layout").withSizeExactly(80, 30)
                     .withIconOnPress( testIcon, UI.FitComponent.MIN_DIM )
                     .get(JButton)
        then : 'The pressed icon of the `JButton` has the expected dimensions!'
            int minDim = Math.min(80 - button.insets.left - button.insets.right, 30 - button.insets.top - button.insets.bottom)
            button.getPressedIcon().getIconWidth() == UI.scale(minDim)
            button.getPressedIcon().getIconHeight() == UI.scale(minDim)

        when : 'We re-create the button with the "MAX_DIM" fit component option.'
            button = UI.button("I have a pressed icon with custom layout").withSizeExactly(80, 30)
                     .withIconOnPress( testIcon, UI.FitComponent.MAX_DIM )
                     .get(JButton)
        then : 'The pressed icon of the `JButton` has the expected dimensions!'
            int maxDim = Math.max(80 - button.insets.left - button.insets.right, 30 - button.insets.top - button.insets.bottom)
            button.getPressedIcon().getIconWidth() == UI.scale(maxDim)
            button.getPressedIcon().getIconHeight() == UI.scale(maxDim)

        where : 'We test this with different UI scaling factors.'
            uiScale << [1, 2, 3]
    }

    def 'We can use `withIconOnPress( IconDeclaration icon, UI.FitComponent fitComponent )` to specify a pressed icon with custom layout.'(
            float uiScale
    ) {
        reportInfo """
            You can specify a layout behavior for a pressed icon specified through an `IconDeclaration`
            along with a `UI.FitComponent` value. This ensures proper behavior of the pressed icon
            during resize interactions.
        """
        given : """
            We first set a scaling factor to simulate a platform with higher DPI.
        """
            SwingTree.get().setUiScaleFactor(uiScale)
        and : 'We create a new `IconDeclaration` for testing purposes.'
            IconDeclaration treesIcon = IconDeclaration.of(Size.of(42,32), "img/trees.png")
        and : 'Then we create a `JToggleButton` and set its pressed icon using `withIconOnPress( IconDeclaration icon, UI.FitComponent fitComponent )` with a custom layout behavior.'
            JToggleButton toggleButton = UI.toggleButton("I have a pressed icon with custom layout").withSizeExactly(80, 30)
                                       .withIconOnPress( treesIcon, UI.FitComponent.NO )
                                       .get(JToggleButton)
        expect : 'The pressed icon of the `JToggleButton` has the expected dimensions!'
            toggleButton.getPressedIcon().getIconWidth() == UI.scale(42)
            toggleButton.getPressedIcon().getIconHeight() == UI.scale(32)

        when : 'We re-create the button with the "WIDTH" fit component option.'
            toggleButton = UI.toggleButton("I have a pressed icon with custom layout").withSizeExactly(80, 30)
                           .withIconOnPress( treesIcon, UI.FitComponent.WIDTH )
                           .get(JToggleButton)
        then : 'The pressed icon of the `JToggleButton` has the expected dimensions!'
            toggleButton.getPressedIcon().getIconWidth() == UI.scale(80 - toggleButton.insets.left - toggleButton.insets.right)
            toggleButton.getPressedIcon().getIconHeight() == UI.scale(32)

        when : 'We re-create the button with the "HEIGHT" fit component option.'
            toggleButton = UI.toggleButton("I have a pressed icon with custom layout").withSizeExactly(80, 30)
                            .withIconOnPress( treesIcon, UI.FitComponent.HEIGHT )
                            .get(JToggleButton)
        then : 'The pressed icon of the `JToggleButton` has the expected dimensions!'
            toggleButton.getPressedIcon().getIconWidth() == UI.scale(42)
            toggleButton.getPressedIcon().getIconHeight() == UI.scale(30 - toggleButton.insets.top - toggleButton.insets.bottom)

        when : 'We re-create the button with the "WIDTH_AND_HEIGHT" fit component option.'
            toggleButton = UI.toggleButton("I have a pressed icon with custom layout").withSizeExactly(80, 30)
                            .withIconOnPress( treesIcon, UI.FitComponent.WIDTH_AND_HEIGHT )
                            .get(JToggleButton)
        then : 'The pressed icon of the `JToggleButton` has the expected dimensions!'
            toggleButton.getPressedIcon().getIconWidth() == UI.scale(80 - toggleButton.insets.left - toggleButton.insets.right)
            toggleButton.getPressedIcon().getIconHeight() == UI.scale(30 - toggleButton.insets.top - toggleButton.insets.bottom)

        when : 'We re-create the button with the "MIN_DIM" fit component option.'
            toggleButton = UI.toggleButton("I have a pressed icon with custom layout").withSizeExactly(80, 30)
                            .withIconOnPress( treesIcon, UI.FitComponent.MIN_DIM )
                            .get(JToggleButton)
        then : 'The pressed icon of the `JToggleButton` has the expected dimensions!'
            int minDim = Math.min(80 - toggleButton.insets.left - toggleButton.insets.right, 30 - toggleButton.insets.top - toggleButton.insets.bottom)
            toggleButton.getPressedIcon().getIconWidth() == UI.scale(minDim)
            toggleButton.getPressedIcon().getIconHeight() == UI.scale(minDim)

        when : 'We re-create the button with the "MAX_DIM" fit component option.'
            toggleButton = UI.toggleButton("I have a pressed icon with custom layout").withSizeExactly(80, 30)
                            .withIconOnPress( treesIcon, UI.FitComponent.MAX_DIM )
                            .get(JToggleButton)
        then : 'The pressed icon of the `JToggleButton` has the expected dimensions!'
            int maxDim = Math.max(80 - toggleButton.insets.left - toggleButton.insets.right, 30 - toggleButton.insets.top - toggleButton.insets.bottom)
            toggleButton.getPressedIcon().getIconWidth() == UI.scale(maxDim)
            toggleButton.getPressedIcon().getIconHeight() == UI.scale(maxDim)

        where : 'We test this with different UI scaling factors.'
            uiScale << [1, 2, 3]
    }

    // Icon On Hover:

        // On Hover Icon:

    def 'We can use `withIconOnHover( Icon icon )` to specify a hover icon.'(
        float uiScale
    ) {
        reportInfo """
            A hover icon is shown when the user moves the mouse cursor over the button based component.
            This provides visual feedback during the hover interaction, making the interface more responsive.
        """
        given : """
            We first set a scaling factor to simulate a platform with higher DPI.
        """
            SwingTree.get().setUiScaleFactor(uiScale)
        and : 'We create a new `ImageIcon` for testing purposes.'
            ImageIcon testIcon = new ImageIcon( new BufferedImage(7,13,BufferedImage.TYPE_INT_ARGB) )
        and : 'Then we create a `JMenuItem` and set its hover icon using `withIconOnHover( Icon icon )`.'
            JMenuItem menuItem = UI.menuItem("I have a hover icon")
                                   .withIconOnHover( testIcon )
                                   .get(JMenuItem)
        expect : 'The hover icon of the `JMenuItem` to be a properly scaled icon.'
            menuItem.getRolloverIcon() instanceof ScalableImageIcon
            menuItem.getRolloverIcon().getIconWidth() == (7 * uiScale) as int
            menuItem.getRolloverIcon().getIconHeight() == (13 * uiScale) as int

        where : 'We test this with different UI scaling factors.'
            uiScale << [1, 2, 3]
    }

    def 'We can use `withIconOnHover( IconDeclaration icon )` to specify a hover icon.'(float uiScale)
    {
        reportInfo """
            An `IconDeclaration` can be used to define the hover icon of a button based component.
            The actual `Icon` will be loaded automatically by SwingTree and shown when the user hovers over the component.
            This is particularly useful for creating consistent hover states across your application.
        """
        given : """
            We first set a scaling factor to simulate a platform with higher DPI.
        """
            SwingTree.get().setUiScaleFactor(uiScale)
        and : 'We create a new `IconDeclaration` for testing purposes.'
            IconDeclaration treesIcon = IconDeclaration.of(Size.of(16, 12), "img/trees.png")
        and : 'Then we create a `JRadioButton` and set its hover icon using `withIconOnHover( IconDeclaration icon )`.'
            JRadioButton radioButton = UI.radioButton("I have a hover icon")
                                      .withIconOnHover( treesIcon )
                                      .get(JRadioButton)
        expect : 'The hover icon of the `JRadioButton` to be loaded and set correctly.'
            radioButton.getRolloverIcon() instanceof ScalableImageIcon
            radioButton.getRolloverIcon().getIconWidth() == (16 * uiScale) as int
            radioButton.getRolloverIcon().getIconHeight() == (12 * uiScale) as int

        where : 'We test this with different UI scaling factors.'
            uiScale << [1, 2, 3]
    }

    def 'We can use `withIconOnHover( Val<IconDeclaration> icon )` to make a hover icon reactive.'(float uiScale)
    {
        reportInfo """
            You can use a `Var` property of an `IconDeclaration` to make the hover icon of a button 
            based component reactive. This means that whenever the value of the `Var` changes, 
            the hover icon of the component will be updated automatically.
            
            This is useful for dynamic interfaces where hover states might change based on 
            application state, user preferences, or other runtime conditions.
        """
        given : """
            We first set a scaling factor to simulate a platform with higher DPI.
        """
            SwingTree.get().setUiScaleFactor(uiScale)
        and : 'First we create two constants of `IconDeclaration` for testing purposes.'
            IconDeclaration swingIcon = IconDeclaration.of(Size.of(22, 13), "img/swing.png")
            IconDeclaration treesIcon = IconDeclaration.of(Size.of(42, 64), "img/trees.png")
        and : 'Then we create a `Var<IconDeclaration>` and set its initial value to the `swingIcon` declaration.'
            Var<IconDeclaration> iconProperty = Var.of(swingIcon)
        and : 'Next we create a regular `JButton` and set its hover icon using the `Var<IconDeclaration>`.'
            JButton button = UI.button("I have a reactive hover icon")
                             .withIconOnHover( iconProperty )
                             .get(JButton)
        expect : 'The hover icon of the `JButton` is loaded and set correctly to the initial value.'
            button.getRolloverIcon() instanceof ScalableImageIcon
            button.getRolloverIcon().getIconWidth() == (22 * uiScale) as int
            button.getRolloverIcon().getIconHeight() == (13 * uiScale) as int

        when : 'We change the value of the `Var<IconDeclaration>` property to the `treesIcon` declaration.'
            iconProperty.set(treesIcon)
        and : 'We wait for the Event Dispatch Thread to process all events.'
            UI.sync()
        then : 'We can confirm that the hover icon of the `JButton` was set to the new value.'
            button.getRolloverIcon() instanceof ScalableImageIcon
            button.getRolloverIcon().getIconWidth() == (42 * uiScale) as int
            button.getRolloverIcon().getIconHeight() == (64 * uiScale) as int

        where : 'We test this with different UI scaling factors.'
            uiScale << [1, 2, 3]
    }

    def 'Use `withIconOnHover( int width, int height, Icon icon )` to specify a hover icon with a custom size.'()
    {
        reportInfo """
            You can specify a custom size for a hover icon by providing the desired width and height
            along with the `Icon` instance itself. This ensures that the hover icon fits well within 
            the layout of your button based component during the hover interaction.
            
            This is particularly useful when you want hover icons to maintain consistent dimensions
            regardless of their original size.
        """
        given : 'We create a new `ImageIcon` for testing purposes.'
            ImageIcon testIcon = new ImageIcon( new BufferedImage(32,32,BufferedImage.TYPE_INT_ARGB) )
        and : 'Then we create a `JCheckBox` and set its hover icon using `withIconOnHover( int width, int height, Icon icon )` with a custom size.'
            JCheckBox checkBox = UI.checkBox("I have a hover icon with custom size")
                                .withIconOnHover(24, 19, testIcon)
                                .get(JCheckBox)
        expect : 'The hover icon of the `JCheckBox` is NOT the exact same instance as the one we set, but has the correct custom size.'
            checkBox.getRolloverIcon() !== testIcon
            checkBox.getRolloverIcon().getIconWidth() == 24
            checkBox.getRolloverIcon().getIconHeight() == 19
    }

    def 'We can use `withIconOnHover( int width, int height, IconDeclaration icon )` to specify a hover icon with custom size.'(float uiScale)
    {
        reportInfo """
            You can specify a custom size for a hover icon by providing the desired width and height
            along with an `IconDeclaration`. This ensures proper sizing of the hover icon during user interaction,
            maintaining visual consistency while allowing for high-quality icon resources.
        """
        given : """
            We first set a scaling factor to simulate a platform with higher DPI.
        """
            SwingTree.get().setUiScaleFactor(uiScale)
        and : 'We create a new `IconDeclaration` for testing purposes.'
            IconDeclaration treesIcon = IconDeclaration.of(Size.of(42, 24), "img/seed.png")
        and : 'Then we build a `JRadioButtonMenuItem` and set its hover icon using `withIconOnHover( int width, int height, IconDeclaration icon )` with a custom size.'
            var radioButtonMenuItem = UI.radioButtonMenuItem("I have a hover icon with custom size")
                                              .withIconOnHover(33, 19, treesIcon)
                                              .get(JRadioButtonMenuItem)
        expect : 'The hover icon of the `JRadioButtonMenuItem` is loaded and has the correct custom size.'
            radioButtonMenuItem.getRolloverIcon() instanceof ScalableImageIcon
            radioButtonMenuItem.getRolloverIcon().getIconWidth() == (33 * uiScale) as int
            radioButtonMenuItem.getRolloverIcon().getIconHeight() == (19 * uiScale) as int
        where : 'We test this with different UI scaling factors.'
            uiScale << [1, 2, 3]
    }

    def 'Use `withIconOnHover( int width, int height, IconDeclaration icon, UI.FitComponent fitComponent )` to specify a hover icon with custom size and layout.'(
            float uiScale
    ) {
        reportInfo """
            You can specify a custom size and layout for a hover icon by providing the desired width, height,
            `IconDeclaration`, and a `UI.FitComponent` value. This ensures the hover icon behaves correctly
            when the component is resized during the hover interaction.
            
            This combination gives you fine-grained control over how hover icons appear in your interface,
            ensuring they maintain proper proportions and visual quality across different component sizes.
        """
        given : """
            We first set a scaling factor to simulate a platform with higher DPI.
        """
            SwingTree.get().setUiScaleFactor(uiScale)
        and : 'We create a new `IconDeclaration` for testing purposes.'
            IconDeclaration treesIcon = IconDeclaration.of(Size.of(73, 66), "img/two-16th-notes.svg")
        and : 'Then we build a `JSplitButton` and set its hover icon using `withIconOnHover( int width, int height, IconDeclaration icon, UI.FitComponent fitComponent )` with a custom size and layout.'
            var splitButton = UI.splitButton("I have a hover icon with custom size and layout")
                                .withIconOnHover(55, 44, treesIcon, UI.FitComponent.WIDTH)
                                .get(JSplitButton)
        expect : 'The hover icon of the `JSplitButton` is loaded and has the correct custom size.'
            splitButton.getRolloverIcon() instanceof SvgIcon
            splitButton.getRolloverIcon().getIconWidth() == (55 * uiScale) as int
            splitButton.getRolloverIcon().getIconHeight() == (44 * uiScale) as int
        where : 'We test this with different UI scaling factors.'
            uiScale << [1, 2, 3]
    }

    def 'We can use `withIconOnHover( Icon icon, UI.FitComponent fitComponent )` to specify a hover icon with custom layout.'(float uiScale)
    {
        reportInfo """
            You can specify a layout behavior for a hover icon by providing the `Icon` instance
            along with a `UI.FitComponent` value. This ensures the hover icon behaves correctly
            when the button based component is resized during the hover interaction.
            
            This is particularly useful for creating responsive interfaces where components
            may change size based on window layout or user preferences, and you want the
            hover icon to adapt appropriately.
        """
        given : """
            We first set a scaling factor to simulate a platform with higher DPI.
        """
            SwingTree.get().setUiScaleFactor(uiScale)
        and : 'We create a new `ImageIcon` for testing purposes.'
            ImageIcon testIcon = new ImageIcon( new BufferedImage(42,32,BufferedImage.TYPE_INT_ARGB) )
        and : 'Then we create a `JButton` and set its hover icon using `withIconOnHover( Icon icon, UI.FitComponent fitComponent )` with a custom layout behavior.'
            JButton button = UI.button("Click!").withSizeExactly(80, 30)
                                .withIconOnHover( testIcon, UI.FitComponent.NO )
                                .get(JButton)
        expect : 'The hover icon of the `JButton` has the expected dimensions!'
            button.getRolloverIcon().getIconWidth() == UI.scale(42)
            button.getRolloverIcon().getIconHeight() == UI.scale(32)

        when : 'We re-create the button with the "WIDTH" fit component option.'
            button = UI.button("I have a hover icon with custom layout").withSizeExactly(80, 30)
                     .withIconOnHover( testIcon, UI.FitComponent.WIDTH )
                     .get(JButton)
        then : 'The hover icon of the `JButton` has the expected dimensions!'
            button.getRolloverIcon().getIconWidth() == UI.scale(80 - button.insets.left - button.insets.right)
            button.getRolloverIcon().getIconHeight() == UI.scale(32)

        when : 'We re-create the button with the "HEIGHT" fit component option.'
            button = UI.button("I have a hover icon with custom layout").withSizeExactly(80, 30)
                     .withIconOnHover( testIcon, UI.FitComponent.HEIGHT )
                     .get(JButton)
        then : 'The hover icon of the `JButton` has the expected dimensions!'
            button.getRolloverIcon().getIconWidth() == UI.scale(42)
            button.getRolloverIcon().getIconHeight() == UI.scale(30 - button.insets.top - button.insets.bottom)

        when : 'We re-create the button with the "WIDTH_AND_HEIGHT" fit component option.'
            button = UI.button("I have a hover icon with custom layout").withSizeExactly(80, 30)
                     .withIconOnHover( testIcon, UI.FitComponent.WIDTH_AND_HEIGHT )
                     .get(JButton)
        then : 'The hover icon of the `JButton` has the expected dimensions!'
            button.getRolloverIcon().getIconWidth() == UI.scale(80 - button.insets.left - button.insets.right)
            button.getRolloverIcon().getIconHeight() == UI.scale(30 - button.insets.top - button.insets.bottom)

        when : 'We re-create the button with the "MIN_DIM" fit component option.'
            button = UI.button("I have a hover icon with custom layout").withSizeExactly(80, 30)
                     .withIconOnHover( testIcon, UI.FitComponent.MIN_DIM )
                     .get(JButton)
        then : 'The hover icon of the `JButton` has the expected dimensions!'
            int minDim = Math.min(80 - button.insets.left - button.insets.right, 30 - button.insets.top - button.insets.bottom)
            button.getRolloverIcon().getIconWidth() == UI.scale(minDim)
            button.getRolloverIcon().getIconHeight() == UI.scale(minDim)

        when : 'We re-create the button with the "MAX_DIM" fit component option.'
            button = UI.button("I have a hover icon with custom layout").withSizeExactly(80, 30)
                     .withIconOnHover( testIcon, UI.FitComponent.MAX_DIM )
                     .get(JButton)
        then : 'The hover icon of the `JButton` has the expected dimensions!'
            int maxDim = Math.max(80 - button.insets.left - button.insets.right, 30 - button.insets.top - button.insets.bottom)
            button.getRolloverIcon().getIconWidth() == UI.scale(maxDim)
            button.getRolloverIcon().getIconHeight() == UI.scale(maxDim)

        where : 'We test this with different UI scaling factors.'
            uiScale << [1, 2, 3]
    }

    def 'We can use `withIconOnHover( IconDeclaration icon, UI.FitComponent fitComponent )` to specify a hover icon with custom layout.'(
            float uiScale
    ) {
        reportInfo """
            You can specify a layout behavior for a hover icon specified through an `IconDeclaration`
            along with a `UI.FitComponent` value. This ensures proper behavior of the hover icon
            during resize interactions while maintaining the benefits of using declared icons
            (such as automatic loading and DPI scaling).
            
            This approach combines the convenience of icon declarations with the flexibility
            of responsive layout behaviors for hover states.
        """
        given : """
            We first set a scaling factor to simulate a platform with higher DPI.
        """
            SwingTree.get().setUiScaleFactor(uiScale)
        and : 'We create a new `IconDeclaration` for testing purposes.'
            IconDeclaration treesIcon = IconDeclaration.of(Size.of(42,32), "img/trees.png")
        and : 'Then we create a `JToggleButton` and set its hover icon using `withIconOnHover( IconDeclaration icon, UI.FitComponent fitComponent )` with a custom layout behavior.'
            JToggleButton toggleButton = UI.toggleButton("I have a hover icon with custom layout").withSizeExactly(80, 30)
                                       .withIconOnHover( treesIcon, UI.FitComponent.NO )
                                       .get(JToggleButton)
        expect : 'The hover icon of the `JToggleButton` has the expected dimensions!'
            toggleButton.getRolloverIcon().getIconWidth() == UI.scale(42)
            toggleButton.getRolloverIcon().getIconHeight() == UI.scale(32)

        when : 'We re-create the button with the "WIDTH" fit component option.'
            toggleButton = UI.toggleButton("I have a hover icon with custom layout").withSizeExactly(80, 30)
                           .withIconOnHover( treesIcon, UI.FitComponent.WIDTH )
                           .get(JToggleButton)
        then : 'The hover icon of the `JToggleButton` has the expected dimensions!'
            toggleButton.getRolloverIcon().getIconWidth() == UI.scale(80 - toggleButton.insets.left - toggleButton.insets.right)
            toggleButton.getRolloverIcon().getIconHeight() == UI.scale(32)

        when : 'We re-create the button with the "HEIGHT" fit component option.'
            toggleButton = UI.toggleButton("I have a hover icon with custom layout").withSizeExactly(80, 30)
                            .withIconOnHover( treesIcon, UI.FitComponent.HEIGHT )
                            .get(JToggleButton)
        then : 'The hover icon of the `JToggleButton` has the expected dimensions!'
            toggleButton.getRolloverIcon().getIconWidth() == UI.scale(42)
            toggleButton.getRolloverIcon().getIconHeight() == UI.scale(30 - toggleButton.insets.top - toggleButton.insets.bottom)

        when : 'We re-create the button with the "WIDTH_AND_HEIGHT" fit component option.'
            toggleButton = UI.toggleButton("I have a hover icon with custom layout").withSizeExactly(80, 30)
                            .withIconOnHover( treesIcon, UI.FitComponent.WIDTH_AND_HEIGHT )
                            .get(JToggleButton)
        then : 'The hover icon of the `JToggleButton` has the expected dimensions!'
            toggleButton.getRolloverIcon().getIconWidth() == UI.scale(80 - toggleButton.insets.left - toggleButton.insets.right)
            toggleButton.getRolloverIcon().getIconHeight() == UI.scale(30 - toggleButton.insets.top - toggleButton.insets.bottom)

        when : 'We re-create the button with the "MIN_DIM" fit component option.'
            toggleButton = UI.toggleButton("I have a hover icon with custom layout").withSizeExactly(80, 30)
                            .withIconOnHover( treesIcon, UI.FitComponent.MIN_DIM )
                            .get(JToggleButton)
        then : 'The hover icon of the `JToggleButton` has the expected dimensions!'
            int minDim = Math.min(80 - toggleButton.insets.left - toggleButton.insets.right, 30 - toggleButton.insets.top - toggleButton.insets.bottom)
            toggleButton.getRolloverIcon().getIconWidth() == UI.scale(minDim)
            toggleButton.getRolloverIcon().getIconHeight() == UI.scale(minDim)

        when : 'We re-create the button with the "MAX_DIM" fit component option.'
            toggleButton = UI.toggleButton("I have a hover icon with custom layout").withSizeExactly(80, 30)
                            .withIconOnHover( treesIcon, UI.FitComponent.MAX_DIM )
                            .get(JToggleButton)
        then : 'The hover icon of the `JToggleButton` has the expected dimensions!'
            int maxDim = Math.max(80 - toggleButton.insets.left - toggleButton.insets.right, 30 - toggleButton.insets.top - toggleButton.insets.bottom)
            toggleButton.getRolloverIcon().getIconWidth() == UI.scale(maxDim)
            toggleButton.getRolloverIcon().getIconHeight() == UI.scale(maxDim)

        where : 'We test this with different UI scaling factors.'
            uiScale << [1, 2, 3]
    }
}
