package swingtree

import org.slf4j.MarkerFactory
import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title
import sprouts.From
import swingtree.style.StyleSheet
import swingtree.threading.EventProcessor

import javax.swing.UIManager
import javax.swing.plaf.FontUIResource


@Title('Library Context')
@Narrative('''

    SwingTree is a feature rich library in which the
    default behavior does not fit every use case.
    This is why you can initialize the library with a custom
    start configuration expressing your needs.
    
    This library context is a singleton called `SwingTree` and
    it can be initialized with a `SwingTreeInitConfig`
    inside of a configurator lambda like so:
    
    ```
        SwingTree.initializeUsing( conf -> conf
            .uiScaleFactor(2f)
            .styleSheet(myStyleSheet)
            .defaultAnimationInterval(42)
        )
    ```
''')
@Subject([SwingTree, SwingTreeInitConfig])
class SwingTree_Library_Context_Spec extends Specification {

    def setup() {
        // Make sure to reset the library context before each test, otherwise the tests would influence each other.
        SwingTree.clear()
        // Also, make sure the "defaultFont" in the UIManager is always `null` before each test,
        // as this is the default state before the library context is initialized and it can influence the UI scale factor computation.
        UIManager.getDefaults().put("defaultFont", null)
    }

    def 'The library context can be configured with custom settings.'() {
        reportInfo """
            In this unit test you can see how to initialize the library context with
            various custom settings. These include a custom UI scale factor,
            a global style configuration in the form of a `StyleSheet`, 
            an event processor for running events on a custom thread and 
            much more.
        """
        given: 'We snapshot the original default font to reset it after the test'
            var originalDefaultFont = UIManager.getDefaults().get("defaultFont")
        and: 'We initialize SwingTree with some custom settings!'
            var myStyleSheet = Mock(StyleSheet)
            var myEventProcessor = Mock(EventProcessor)
            var myMarker = MarkerFactory.getMarker("MyMarker")
            var myFont = new java.awt.Font("Arial", java.awt.Font.ITALIC, 73)
            SwingTree.initializeUsing(conf -> conf
                    .uiScaleFactor(4.2f)
                    .styleSheet(myStyleSheet)
                    .defaultAnimationInterval(42)
                    .isUiScaleFactorEnabled(true)
                    .logMarker(myMarker)
                    .eventProcessor(myEventProcessor)
                    .defaultFont(myFont, SwingTreeInitConfig.FontInstallation.SOFT)
            )
        expect: 'The settings are applied to the library context.'
            SwingTree.get().getUiScaleFactor() == 4.2f
            SwingTree.get().getEventProcessor() == myEventProcessor
            SwingTree.get().getStyleSheet() == myStyleSheet
            SwingTree.get().getDefaultAnimationInterval() == 42
            SwingTree.get().isUiScaleFactorEnabled() == true
            SwingTree.get().logMarker() == myMarker
        and:
            UIManager.getDefaults().get("defaultFont") === myFont

        cleanup: 'Teardown the library context and reset the "defaultFont" in the `UIManager` to the original value!'
            SwingTree.clear()
            UIManager.getDefaults().put("defaultFont", originalDefaultFont)
    }

    def 'The soft font installation only installs the "defaultFont" but not every regular font.'() {
        reportInfo """
            If you use scalable Look and Feels like FlatLaf, you can easily make
            your application scale through the "defaultFont".
            SwingTree as well as FlatLaf and many other libraries and frameworks then
            use the "defaultFont" to compute a `float` scaling factor in the same magnitude
            as the default font size. This scale factor is then used to scale the UI.
            You can configure this default font in when initializing the library context!
        """
        given :
            boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win")
        and : 'We snapshot the original default and panel fonts to reset them after the test.'
            var originalDefaultFont = UIManager.getDefaults().get("defaultFont")
            var originalPanelFont = UIManager.getDefaults().get("Panel.font")
        and: 'We create a custom and unique default font as well as a custom panel font'
            var myDefaultFont = new java.awt.Font("Arial", java.awt.Font.ITALIC, 73)
            var myPanelFont = new java.awt.Font("Comic Sans MS", java.awt.Font.BOLD, 42)
        and: 'We set the panel font and initialize SwingTree with the custom default font as a soft installation.'
            UIManager.getDefaults().put("Panel.font", myPanelFont)
            SwingTree.initializeUsing(conf -> conf
                .defaultFont(myDefaultFont, SwingTreeInitConfig.FontInstallation.SOFT)
            )

        expect: 'SwingTree has its scaling factor computed from the default font size:'
            SwingTree.get().getUiScaleFactor() == (isWindows ? 6f : 4.75f)
            UI.scale() == (isWindows ? 6f : 4.75f) // delegates to the above method
        and: 'The default font is changed to the custom one.'
            UIManager.getDefaults().get("defaultFont") === myDefaultFont
        and: 'The panel font is not changed to the custom one, because we only installed the default font softly.'
            UIManager.getDefaults().get("Panel.font") === myPanelFont

        cleanup: 'Reset the library context to be `null` again internally!'
            SwingTree.clear()
        and: 'We also reset the default and panel fonts to the original ones:'
            UIManager.getDefaults().put("defaultFont", originalDefaultFont)
            UIManager.getDefaults().put("Panel.font", originalPanelFont)
    }

    def 'The hard font installation installs the default font as well as every regular font.'() {
        reportInfo """
            If the Look and Feel you are using does not support UI scaling there is
            a brutal but effective way to at least scale all of the fonts in your application.
            When you initialize the library context, you can choose to do a "hard" font installation. 
            This means that the default font is not only installed as the "defaultFont" but also as every 
            regular font like "Panel.font", "Button.font" and so on.
        """
        given :
            boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win")
        and : 'We snapshot the original default and panel fonts to reset them after the test.'
            var originalDefaultFont = UIManager.getDefaults().get("defaultFont")
            var originalPanelFont = UIManager.getDefaults().get("Panel.font")
        and: 'We create a custom and unique default font as well as a custom panel font'
            var myDefaultFont = new FontUIResource(new java.awt.Font("Arial", java.awt.Font.ITALIC, 73))
            var myPanelFont = new FontUIResource(new java.awt.Font("Comic Sans MS", java.awt.Font.BOLD, 42))
        and: 'We set the panel font and initialize SwingTree with the custom default font as a hard installation.'
            UIManager.getDefaults().put("Panel.font", myPanelFont)
            SwingTree.initializeUsing(conf -> conf
                    .defaultFont(myDefaultFont, SwingTreeInitConfig.FontInstallation.HARD)
            )

        expect: 'SwingTree has its scaling factor computed from the default font size:'
            SwingTree.get().getUiScaleFactor() == (isWindows ? 6f : 4.75f)
            UI.scale() == (isWindows ? 6f : 4.75f) // delegates to the above method
        and: 'The default font is changed to the custom one.'
            UIManager.getDefaults().get("defaultFont") === myDefaultFont
        and: 'The panel font is also changed to the custom one, because we installed every regular font hard.'
            UIManager.getDefaults().get("Panel.font") === myDefaultFont

        cleanup: 'Reset the library context to be `null` again internally!'
            SwingTree.clear()
        and: 'We also reset the default and panel fonts to the original ones:'
            UIManager.getDefaults().put("defaultFont", originalDefaultFont)
            UIManager.getDefaults().put("Panel.font", originalPanelFont)
    }

    def 'You can specify a default font for the SwingTree UI scale without installing it in the `UIManager`.'() {
        reportInfo """
            If you want SwingTree to compute its UI scale factor from a custom default font 
            but you do not want to install this font in the `UIManager`, you can choose the "none" 
            font installation when initializing the library context. 
        """
        given:
            boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win")
        and : 'We snapshot the original default font to check if it stays the same!'
            var originalDefaultFont = UIManager.getDefaults().get("defaultFont")
        and: 'We create a custom and unique default font'
            var myDefaultFont = new java.awt.Font("Arial", java.awt.Font.ITALIC, 73)
        and: 'We initialize SwingTree with the custom default font but do not install it.'
            SwingTree.initializeUsing(conf -> conf
                .defaultFont(myDefaultFont, SwingTreeInitConfig.FontInstallation.NONE)
            )

        expect: 'SwingTree has its scaling factor computed from the default font size:'
            SwingTree.get().getUiScaleFactor() == (isWindows ? 6f : 4.75f)
            UI.scale() == (isWindows ? 6f : 4.75f) // delegates to the above method
        and: 'The default font is not installed in the UIManager.'
            UIManager.getDefaults().get("defaultFont") !== myDefaultFont
            UIManager.getDefaults().get("defaultFont") === originalDefaultFont

        cleanup: 'Reset the library context to be `null` again internally!'
            SwingTree.clear()
    }

    def 'SwingTree notices when the "defaultFont" changes in the `UIManager` and it updates its UI scale factor accordingly.'() {
        reportInfo """
            When `SwingTree` initializes, it computes its UI scale factor from the size of the "defaultFont" in 
            the `UIManager` and also registers a listener to notice when the "defaultFont" changes. 
            So when you change the default font through the `UIManager` in your code, then
            the entire SwingTree UI will scale up or down accordingly.
        """
        given :
            boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win")
        and : 'We snapshot the original default font to reset it after the test'
            var originalDefaultFont = UIManager.getDefaults().get("defaultFont")
        and: 'We create a custom and unique default font in the `UIManager`:'
            var myDefaultFont = new java.awt.Font("Arial", java.awt.Font.BOLD, 42)
            UIManager.getDefaults().put("defaultFont", myDefaultFont)
        and: 'We initialize `SwingTree`, it should use the current "defaultFont" for initialization...'
            SwingTree.initialize()

        expect: 'The UI scale factor is computed from our custom default font:'
            SwingTree.get().getUiScaleFactor() == (isWindows ? 3.5f : 2.75f)
            UI.scale() == (isWindows ? 3.5f : 2.75f) // delegates to the above method

        when: 'We change the default font in the `UIManager` to another custom font...'
            var anotherDefaultFont = new java.awt.Font("Comic Sans MS", java.awt.Font.ITALIC, 73)
            UIManager.getDefaults().put("defaultFont", anotherDefaultFont)

        then: 'SwingTree notices this change and updates its UI scale factor accordingly:'
            SwingTree.get().getUiScaleFactor() == (isWindows ? 6f : 4.75f)
            UI.scale() == (isWindows ? 6f : 4.75f) // delegates to the above method

        cleanup: 'Reset the library context to be `null` again internally, as well as the default font in the `UIManager`!'
            SwingTree.clear()
            UIManager.getDefaults().put("defaultFont", originalDefaultFont)
    }

    def 'You can listen to UI scale changes through a reactive property!'()
    {
        reportInfo """
            If you need to react to listen and react to changes of the UI scale
            factor, then SwingTree offers a memory leak safe way to do this.
            More specifically you can create a weakly referenced property view
            which you can register listeners to.
            When no longer referenced by your code, the property view
            will be garbage collected together with all of the listeners it holds.
            
            In this test we demonstrate that the property view receives change events
            whenever the scale changes in the library context.
        """
        given:
            boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win")
        and : 'We snapshot the original default font to reset it after the test'
            var originalDefaultFont = UIManager.getDefaults().get("defaultFont")
        and: 'We create a custom and unique default font in the `UIManager`:'
            var myDefaultFont = new java.awt.Font("Arial", java.awt.Font.BOLD, 42)
            UIManager.getDefaults().put("defaultFont", myDefaultFont)
        and: 'We initialize `SwingTree`, it should use the current "defaultFont" for initialization...'
            SwingTree.initialize()
        and : 'Finally, we create a reactive property view with a trace list:'
            var trace = []
            var scaleView = SwingTree.get().getUiScaleView()
            scaleView.onChange(From.ALL, it -> trace.add(it.currentValue().orElseThrow()))

        when : 'We change the default font in the `UIManager` to another custom font...'
            var anotherDefaultFont = new java.awt.Font("Comic Sans MS", java.awt.Font.ITALIC, 73)
            UIManager.getDefaults().put("defaultFont", anotherDefaultFont)
        then : 'The property view receives the new scale value:'
            trace == [(isWindows ? 6f : 4.75f)]

        when : 'We set the scale factor manually through the library context...'
            SwingTree.get().setUiScaleFactor(1.25f)
        then : 'The property view receives the manually specified scale:'
            trace == [(isWindows ? 6f : 4.75f), 1.25f]

        when : 'We change back to the previous default font...'
            UIManager.getDefaults().put("defaultFont", myDefaultFont)
        then : 'The property view receives the previous scale value again:'
            trace == [(isWindows ? 6f : 4.75f), 1.25f, (isWindows ? 3.5f : 2.75f)]

        cleanup: 'Reset the library context and set the "defaultFont" back to the original value!'
            SwingTree.clear()
            UIManager.getDefaults().put("defaultFont", originalDefaultFont)
    }

    def 'You an listen to `isDevToolEnabled` property changes through a reactive property!'()
    {
        reportInfo """
            Similar to the UI scale factor, you can react to changes of the `isDevToolEnabled` property
            through a reactive property view, without having to poll for it.
            The "dev tool" is an inspector tool for 
        """
        given: 'We initialize `SwingTree`, by default, the dev tool is always disabled:'
            SwingTree.initializeUsing(conf -> conf )
        and : 'We create a reactive property view with a trace list:'
            var trace = []
            var devToolEnabledView = SwingTree.get().isDevToolEnabledView()
            devToolEnabledView.onChange(From.ALL, it -> trace.add(it.currentValue().orElseThrow()))
        expect: 'We verify, `SwingTree` reports the "isDevToolEnabled" value as `false`:'
            !SwingTree.get().isDevToolEnabled()

        when : 'We enable the dev tool through the library context...'
            SwingTree.get().setDevToolEnabled(true)
        then : 'The property view receives the new value:'
            trace == [true]

        when : 'We disable the dev tool again...'
            SwingTree.get().setDevToolEnabled(false)
        then : 'The property view receives the new value again:'
            trace == [true, false]

        cleanup: 'Reset the library context!'
            SwingTree.clear()
    }

    def 'You can configure the keystroke for summoning the dev tool in the library context!'()
    {
        reportInfo """
            The dev tool is a powerful inspector tool for inspecting the internal state of SwingTree.
            You can summon it through a custom keystroke which you can configure when
            initializing the library context. 
            
            By default, the keystroke is "ctrl shift alt I", exactly the same as in modern browsers, 
            but you can change it to whatever you like. 
            In this test we change it to "ctrl shift alt D" and verify that the new keystroke 
            is applied in the library context.
        """
        given: 'We initialize `SwingTree` with a custom dev tool keystroke:'
            SwingTree.initializeUsing(conf -> conf
                .devToolKeyStrokeShortcut("ctrl shift alt D")
            )
        expect: 'The dev tool keystroke is the one we specified:'
            SwingTree.get().getDevToolKeyStrokeShortcut().toString() == "ctrl shift alt D"

        cleanup: 'Reset the library context!'
            SwingTree.clear()
    }
}
