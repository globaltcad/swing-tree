package swingtree

import org.slf4j.MarkerFactory
import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title
import sprouts.From
import sprouts.Viewable
import swingtree.style.StyleSheet
import swingtree.threading.EventProcessor

import javax.swing.UIManager
import javax.swing.plaf.FontUIResource
import javax.swing.plaf.basic.BasicLookAndFeel
import java.util.concurrent.atomic.AtomicReference


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
        // Pin the cross-platform look and feel so prior specs that switched to Nimbus, FlatLaf,
        // etc. cannot leak a LaF-installed "defaultFont" through `getLookAndFeelDefaults()`
        // and skew the scale factor computation in this spec.
        UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName())
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
            // Use a real StyleSheet (no-op `configure()`) rather than `Mock(StyleSheet)` here.
            // Spock/ByteBuddy mocks bypass the constructor and leave `final` fields like
            // `_styleSheetChangeEvent` as `null`. That leak can later flow through stale
            // UI-scale listeners into other specs and surface as a `NullPointerException`
            // in `StyleSheet.observable()`. We only need identity equality below, so a real
            // instance is a behaviour-preserving substitute.
            var myStyleSheet = new StyleSheet() { @Override protected void configure() {} }
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

    def 'A NONE font installation uses the supplied font even when the `UIManager` already has a different "defaultFont".'() {
        reportInfo """
            The contract of `FontInstallation.NONE` is that SwingTree should compute its UI scale
            factor from the supplied default font *and* ignore the `UIManager` state entirely.

            This unit test reproduces the conditions under which other tests in the suite
            (typically those that exercise alternative Look and Feels like Nimbus or FlatLaf)
            can leak a non-null "defaultFont" into the `UIManager` and used to make the
            sibling specification flaky: the previously stored font was preferred over the
            user-supplied one, producing an unexpected scale factor.
        """
        given : 'A stale `UIManager` default font from a hypothetical earlier scenario:'
            boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win")
            var staleFont = new java.awt.Font("Serif", java.awt.Font.PLAIN, 12)
            UIManager.getDefaults().put("defaultFont", staleFont)
        and: 'A unique default font that we want SwingTree to scale from:'
            var myDefaultFont = new java.awt.Font("Arial", java.awt.Font.ITALIC, 73)
        and: 'We initialize SwingTree with the custom default font using a NONE installation:'
            SwingTree.initializeUsing(conf -> conf
                .defaultFont(myDefaultFont, SwingTreeInitConfig.FontInstallation.NONE)
            )

        expect: 'The scale factor is derived from our supplied font, not the leftover one in the `UIManager`:'
            SwingTree.get().getUiScaleFactor() == (isWindows ? 6f : 4.75f)
            UI.scale() == (isWindows ? 6f : 4.75f)
        and: 'The `UIManager` "defaultFont" is left untouched, as documented by the NONE installation:'
            UIManager.getDefaults().get("defaultFont") === staleFont

        cleanup:
            SwingTree.clear()
            UIManager.getDefaults().put("defaultFont", null)
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

    def 'You can listen to `isDevToolEnabled` property changes through a reactive property!'()
    {
        reportInfo """
            Similar to the UI scale factor, you can react to changes of the `isDevToolEnabled` property
            through a reactive property view, without having to poll for it.
            The "dev tool" is an inspector tool for inspecting the internal state of SwingTree
            and your UI while the application is running.
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

    def 'The static `getPlatformScaleFactor` returns a sensible UI scale for the current platform without any side effects.'() {
        reportInfo """
            `SwingTree.getPlatformScaleFactor()` is a side-effect free static utility
            that derives a sensible UI scale factor from the current platform's system
            font. It does <i>not</i> create or access the `SwingTree` singleton, does
            <i>not</i> register listeners on the `UIManager`, and does <i>not</i> mutate
            any shared state.

            On any reasonable platform, the returned value should always fall within
            `[0.2f, 5f]`. Anything outside this range is almost certainly a bug
            regardless of the operating system, look and feel, or display configuration.
        """
        given : 'We snapshot the `UIManager` "defaultFont" so we can check it is untouched afterwards.'
            var defaultFontBefore = UIManager.getDefaults().get("defaultFont")

        when : 'We ask SwingTree for a platform-derived scale factor.'
            float scale = SwingTree.getPlatformScaleFactor()

        then : 'The result is a finite, positive number well inside the sensible range.'
            Float.isFinite(scale)
            scale >= 0.2f
            scale <= 5f
        and : 'Calling the method again yields the exact same value (it is a pure read).'
            SwingTree.getPlatformScaleFactor() == scale
        and : 'The `UIManager` "defaultFont" was not modified as a side effect.'
            UIManager.getDefaults().get("defaultFont") === defaultFontBefore
    }

    def 'The `getDefaultFont()` method always returns a `FontUIResource`, wrapping a plain `Font` if necessary.'() {
        reportInfo """
            Look and Feel authors usually want a font they can drop straight into
            `UIDefaults` keys like `Label.font` without breaking Swing's
            LAF-replacement contract. That contract is gated on `instanceof UIResource`:
            a plain `Font` is treated as user-set and is *preserved* across LAF swaps,
            while a `FontUIResource` is replaceable.

            `SwingTree.get().getDefaultFont()` therefore *always* returns a
            `FontUIResource` — even when the underlying font in the `UIManager`
            is a plain `Font`, SwingTree wraps it for you.
        """
        given : 'We seed the `UIManager` with a *plain* `Font` (not a `FontUIResource`) under "defaultFont":'
            var plainFont = new java.awt.Font("Arial", java.awt.Font.PLAIN, 17)
            assert !(plainFont instanceof FontUIResource)
            UIManager.getDefaults().put("defaultFont", plainFont)
        when : 'We initialize SwingTree and ask for the default font:'
            SwingTree.initialize()
            var resolved = SwingTree.get().getDefaultFont()
        then : 'It is a `FontUIResource` (so it can be installed into UIDefaults directly):'
            resolved instanceof FontUIResource
        and : 'The wrapped font carries the same family, size and style as the original:'
            resolved.getFamily() == plainFont.getFamily()
            resolved.getSize()   == plainFont.getSize()
            resolved.getStyle()  == plainFont.getStyle()
        and : 'The method never returns `null`:'
            resolved != null

        cleanup:
            SwingTree.clear()
            UIManager.getDefaults().put("defaultFont", null)
    }

    def 'The `getDefaultFont()` method tracks the currently active "defaultFont" in the `UIManager`.'() {
        reportInfo """
            `getDefaultFont()` is the single source of truth for the active default font.
            Whatever sits under `UIManager.get("defaultFont")` at the time of the call
            is what comes back (wrapped in a `FontUIResource` if it wasn't one already).

            That means the method always reflects the *current* state — not a snapshot
            taken when the library was first initialized — so changes pushed through
            the `UIManager` after bootstrap are observed immediately.
        """
        given : 'A custom default font installed before SwingTree initializes:'
            var configuredFont = new FontUIResource(new java.awt.Font("Dialog", java.awt.Font.BOLD, 31))
            UIManager.getDefaults().put("defaultFont", configuredFont)
        and : 'SwingTree initializes:'
            SwingTree.initialize()
        expect : 'getDefaultFont() returns exactly the configured font (no wrapping needed):'
            SwingTree.get().getDefaultFont() === configuredFont

        when : 'We later swap the default font in the `UIManager`:'
            var differentFont = new FontUIResource(new java.awt.Font("Serif", java.awt.Font.ITALIC, 24))
            UIManager.getDefaults().put("defaultFont", differentFont)
        then : 'The next call to getDefaultFont() returns the new font:'
            SwingTree.get().getDefaultFont() === differentFont

        cleanup:
            SwingTree.clear()
            UIManager.getDefaults().put("defaultFont", null)
    }

    def 'You can subscribe to live `defaultFont` changes through `getDefaultFontView()`.'() {
        reportInfo """
            SwingTree publishes the authoritative default font through a reactive
            `Viewable<FontUIResource>` so a Look and Feel can be truly dynamic:
            re-install its per-component `*.font` keys and refresh open windows
            as soon as the OS, the user or another part of the application
            flips the system font.

            The view fires for every input that drives the resolved default font —
            the `UIManager` "defaultFont" key in particular — and never delivers
            a `null` payload.
        """
        given : 'A reasonable default font in place before SwingTree initializes:'
            var firstFont = new FontUIResource(new java.awt.Font("Dialog", java.awt.Font.PLAIN, 14))
            UIManager.getDefaults().put("defaultFont", firstFont)
            SwingTree.initialize()
        and : 'A view + trace list of every value the property emits:'
            var trace = []
            var fontView = SwingTree.get().getDefaultFontView()
            fontView.onChange(From.ALL, it -> trace.add(it.currentValue().orElseThrow()))
        expect : 'The initial value of the view matches the current default font:'
            fontView.get() === firstFont

        when : 'We push a new default font through the `UIManager`:'
            var secondFont = new FontUIResource(new java.awt.Font("Serif", java.awt.Font.BOLD, 28))
            UIManager.getDefaults().put("defaultFont", secondFont)
        then : 'The view subscribers see the new font:'
            trace == [secondFont]

        when : 'We push yet another font:'
            var thirdFont = new FontUIResource(new java.awt.Font("SansSerif", java.awt.Font.ITALIC, 19))
            UIManager.getDefaults().put("defaultFont", thirdFont)
        then : 'Subscribers see that one too:'
            trace == [secondFont, thirdFont]

        cleanup:
            SwingTree.clear()
            UIManager.getDefaults().put("defaultFont", null)
    }

    def 'The `getDefaultFontView()` view does NOT fire when the same font value is re-installed.'() {
        reportInfo """
            SwingTree's internal listener fires for *every* PropertyChangeEvent on
            "defaultFont" — even when an application puts the same value back into the
            `UIManager`. To prevent that low-level noise from leaking into reactive
            subscribers, the published default-font property only signals when the
            resolved value actually differs from the previous one (value equality,
            not identity). That keeps Look and Feel re-installation cheap.
        """
        given : 'An initialized SwingTree with a baseline default font:'
            var baseline = new FontUIResource(new java.awt.Font("Dialog", java.awt.Font.PLAIN, 14))
            UIManager.getDefaults().put("defaultFont", baseline)
            SwingTree.initialize()
        and : 'A subscriber that records every emitted font:'
            var trace = []
            var fontView = SwingTree.get().getDefaultFontView()
            fontView.onChange(From.ALL, it -> trace.add(it.currentValue().orElseThrow()))

        when : 'We push an *equal* (but not identical) FontUIResource back into the `UIManager`:'
            var equalCopy = new FontUIResource(new java.awt.Font("Dialog", java.awt.Font.PLAIN, 14))
            UIManager.getDefaults().put("defaultFont", equalCopy)
        then : 'The view subscriber does not fire — the resolved value did not change:'
            trace == []

        when : 'We push a genuinely different font:'
            var bigger = new FontUIResource(new java.awt.Font("Dialog", java.awt.Font.PLAIN, 28))
            UIManager.getDefaults().put("defaultFont", bigger)
        then : 'The subscriber receives exactly one change notification:'
            trace == [bigger]

        cleanup:
            SwingTree.clear()
            UIManager.getDefaults().put("defaultFont", null)
    }

    def 'A configured `uiScaleFactor` is folded into the font returned by `getDefaultFont()`.'() {
        reportInfo """
            SwingTree can arrive at its UI scale factor in two ways: by *deriving* it
            from the size of the default font, or by having it *dictated* explicitly
            through the `swingtree.uiScale` system property (equivalently
            `conf.uiScaleFactor(..)` at initialization).

            When the scale is dictated this way, the raw font sitting in the
            `UIManager` — or the active Look and Feel's `Label.font` — does **not** yet
            carry that factor. If `getDefaultFont()` handed that raw font back, a Look
            and Feel author installing it into `UIDefaults` keys like `Label.font`
            would get text at the unscaled size while the rest of SwingTree's layout
            and painting is scaled — text and chrome would visibly disagree.

            So `getDefaultFont()` folds the configured factor into the font it returns,
            honouring its documented "already scaled" contract. What scales is the font
            *size*: doubling the configured factor doubles the returned size. That
            relationship is platform independent, even though the absolute pixel size
            depends on the platform's reference font size (e.g. 15 on a typical Linux
            desktop, 12 on Windows, 13 on macOS), which is why this test asserts the
            *ratio* rather than a hard-coded size.
        """
        given : 'A deliberately small raw font in the `UIManager` — its size must NOT survive verbatim:'
            var rawFont = new java.awt.Font("Monospaced", java.awt.Font.PLAIN, 9)
            UIManager.getDefaults().put("defaultFont", rawFont)
        and : 'We initialize SwingTree with the scale dictated explicitly as a factor of 2:'
            SwingTree.initializeUsing(conf -> conf
                .isUiScaleFactorEnabled(true)
                .uiScaleFactor(2f)
            )
        and : 'We capture the resolved default font:'
            var scaledTwice = SwingTree.get().getDefaultFont()

        expect : 'It is a `FontUIResource`, so a LAF can drop it straight into `UIDefaults`:'
            scaledTwice instanceof FontUIResource
        and : 'The font family is preserved — only the size is touched:'
            scaledTwice.getFamily() == rawFont.getFamily()
        and : 'The raw, unscaled size was NOT returned verbatim — the configured factor was applied:'
            scaledTwice.getSize() != rawFont.getSize()
            scaledTwice.getSize() > rawFont.getSize()

        when : 'We re-initialize with the very same font but a factor of 1, to obtain the unscaled baseline:'
            SwingTree.clear()
            UIManager.getDefaults().put("defaultFont", rawFont)
            SwingTree.initializeUsing(conf -> conf
                .isUiScaleFactorEnabled(true)
                .uiScaleFactor(1f)
            )
            var scaledOnce = SwingTree.get().getDefaultFont()
        then : 'The returned size scales linearly with the configured factor: twice the factor, twice the size.'
            scaledTwice.getSize() == 2 * scaledOnce.getSize()

        cleanup:
            SwingTree.clear()
            UIManager.getDefaults().put("defaultFont", null)
    }

    def 'The `getDefaultFontView()` exposes the scaled default font when a `uiScaleFactor` is configured.'() {
        reportInfo """
            The reactive `getDefaultFontView()` is the hook a dynamic Look and Feel
            subscribes to so it can re-install its `*.font` keys whenever the
            authoritative default font changes. It must publish the *same* value that
            `getDefaultFont()` returns — including the scaling applied for an explicitly
            configured `uiScaleFactor`. A LAF that trusted the view but received an
            unscaled font would render text out of step with the rest of the UI.

            This test pins the scale to a factor of 2 and shows that:
            <ul>
                <li>the view's current value is the *scaled* font (not the raw
                    `UIManager` font), and matches `getDefaultFont()`; and</li>
                <li>when the application swaps the source font in the `UIManager`, the
                    view re-publishes a value that is still governed by the configured
                    factor — the new family flows through, but the size stays locked to
                    the dictated scale.</li>
            </ul>
        """
        given : 'A raw source font installed before SwingTree initializes:'
            var firstRaw = new java.awt.Font("Dialog", java.awt.Font.PLAIN, 10)
            UIManager.getDefaults().put("defaultFont", firstRaw)
        and : 'We initialize with the scale dictated explicitly as a factor of 2:'
            SwingTree.initializeUsing(conf -> conf
                .isUiScaleFactorEnabled(true)
                .uiScaleFactor(2f)
            )
        and : 'A view plus a trace list of every value the property emits:'
            var trace = []
            var fontView = SwingTree.get().getDefaultFontView()
            fontView.onChange(From.ALL, it -> trace.add(it.currentValue().orElseThrow()))

        expect : 'The view never delivers a `null` payload and its current value is a `FontUIResource`:'
            fontView.get() instanceof FontUIResource
        and : 'The view exposes exactly what `getDefaultFont()` resolves — the scaled font, not the raw one:'
            fontView.get() == SwingTree.get().getDefaultFont()
            fontView.get().getSize() != firstRaw.getSize()
            fontView.get().getSize() > firstRaw.getSize()

        when : 'The application swaps the source font in the `UIManager` for a different family:'
            var secondRaw = new java.awt.Font("Serif", java.awt.Font.BOLD, 40)
            UIManager.getDefaults().put("defaultFont", secondRaw)
        then : 'The view fires once, the new family flows through, but the size stays governed by the factor:'
            trace.size() == 1
            trace.last().getFamily() == secondRaw.getFamily()
            trace.last().getSize()   == SwingTree.get().getDefaultFont().getSize()
            trace.last().getSize()   != secondRaw.getSize()

        cleanup:
            SwingTree.clear()
            UIManager.getDefaults().put("defaultFont", null)
    }

    def 'A custom Look and Feel can fetch `getDefaultFontView()` from its `initialize()` hook without NPE.'() {
        reportInfo """
            The recommended pattern for a SwingTree-aware Look and Feel is to
            subscribe to `SwingTree.get().getDefaultFontView()` from inside the
            LAF's own `initialize()` hook — so that runtime DPI / system-font
            changes flow into the UI without a restart.

            That timing is tricky: during `LookAndFeel.initialize()`,
            `UIManager.getLookAndFeelDefaults()` can still be `null` because the
            install has not yet committed the new defaults table. SwingTree
            therefore guards each `addPropertyChangeListener` call and re-attempts
            any skipped attach on the next `"lookAndFeel"` PropertyChangeEvent
            (which is guaranteed to fire at the end of `UIManager.setLookAndFeel(..)`).

            This test reproduces the call order and verifies both halves of the
            contract — no NPE during init, and the listener still propagates
            `UIManager` font changes after the install completes.
        """
        given : 'A trace list of fonts the view emits after install:'
            var trace = []
            var capturedView = new AtomicReference<Viewable<FontUIResource>>()
        and : 'A minimal custom Look and Feel that subscribes from inside `initialize()`:'
            var laf = new BasicLookAndFeel() {
                @Override String getName()                 { "SelfHealTest" }
                @Override String getID()                   { "SelfHealTest" }
                @Override String getDescription()          { "self-heal test LAF" }
                @Override boolean isNativeLookAndFeel()    { false }
                @Override boolean isSupportedLookAndFeel() { true  }
                @Override void initialize() {
                    super.initialize()
                    // This is exactly the call pattern that used to throw an
                    // NPE because UIManager.getLookAndFeelDefaults() was null.
                    var view = SwingTree.get().getDefaultFontView()
                    view.onChange(From.ALL, it -> trace.add(it.currentValue().orElseThrow()))
                    capturedView.set(view)
                }
            }

        when : 'We install the LAF — the bootstrap MUST NOT throw:'
            UIManager.setLookAndFeel(laf)
        then : 'No exception was raised and the view was obtained successfully:'
            noExceptionThrown()
            capturedView.get() != null

        when : 'After the install completes, we push a fresh "defaultFont" through the `UIManager`:'
            var afterInstall = new FontUIResource(new java.awt.Font("Serif", java.awt.Font.BOLD, 33))
            UIManager.getDefaults().put("defaultFont", afterInstall)
        then : 'The listener that was attached during initialize self-healed: the view fires:'
            trace.contains(afterInstall)

        cleanup:
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName())
            SwingTree.clear()
            UIManager.getDefaults().put("defaultFont", null)
    }

    def 'You can configure the keystroke for summoning the dev tool in the library context!'()
    {
        reportInfo """
            The dev tool is a powerful inspector tool for inspecting the internal state of SwingTree.
            You can summon it through a custom keystroke which you can configure when
            initializing the library context. 
            
            By default, the keystroke is "ctrl shift I", exactly the same as in modern browsers, 
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
