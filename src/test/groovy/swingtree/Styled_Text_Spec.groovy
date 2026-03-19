package swingtree.style

import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title
import sprouts.Tuple
import swingtree.SwingTree
import swingtree.UI
import swingtree.components.JBox
import swingtree.threading.EventProcessor
import utility.SwingTreeTestConfigurator

import javax.swing.*
import java.awt.image.BufferedImage

@Title("The Text Style API")
@Narrative('''

    SwingTree's rich-text rendering API centres on two concepts:
    
    1. `StyledString` — an immutable, uniformly styled snippet of text.
       A sequence of them, combined into a `Tuple<StyledString>`, is the
       content model for a single rendered text region.
       
    2. `TextConf` — a sub-configuration of the component style that describes
       *how* that sequence is laid out and painted: placement, line-wrapping,
       and a base font from which individual segment fonts can inherit.
    
    The font configuration of each segment is stored as a *lazy*
    `Configurator<FontConf>` — a pure function that takes an inherited
    base `FontConf` and returns a derived one.  Resolution happens exactly
    once, at the moment the `StyledString` is "resolved" for its concrete `FontConf`
    via `resolveUsing(baseConf)`.  This separation of "recipe" from "result"
    is what allows the same styled-string tuple to be re-evaluated cheaply
    whenever the surrounding context changes.

    This specification verifies:
    - The value-semantics and immutability guarantees of `StyledString`.
    - That font properties are absent before resolution and faithfully produced
      after it, including inheritance from the base `FontConf`.
    - That the style engine reflects a non-trivial text configuration correctly
      in the component's style string representation.
    - That a text configuration which carries no visible content is simplified
      away to the canonical `TextConf[NONE]` null-object.

''')
@Subject([StyledString, TextConf, FontConf])
class Styled_Text_Spec extends Specification
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

    def setup() {
        // Reset to the default cross-platform look and feel before every test so that
        // platform-specific defaults cannot influence the style resolution results.
        UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName())
    }

    def cleanup() {
        UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName())
    }

    def 'A `StyledString` without a font configurator has value semantics based solely on its text content'()
    {
        reportInfo """
            `StyledString.of(String)` produces a segment that carries *no* font style
            information.  Its appearance will be entirely inherited from the base font
            of the surrounding `TextConf` at render time.

            Because the segment holds nothing beyond its text, two instances that wrap
            the same string are structurally identical and therefore equal — they
            represent the same value.  This makes them safe to use in immutable tuples,
            cache keys, and reactive state variables without defensive copying.
        """
        expect : 'Two plain StyledStrings wrapping the same text are equal:'
            StyledString.of("hello world") == StyledString.of("hello world")
        and : 'Their hash codes agree, as required by the equals/hashCode contract:'
            StyledString.of("hello world").hashCode() == StyledString.of("hello world").hashCode()
        and : 'Strings that differ in content are not equal, even if they look similar:'
            StyledString.of("Hello World") != StyledString.of("hello world")
            StyledString.of("hello world") != StyledString.of("hello world!")
        and : 'An empty string is a perfectly valid — and deduplicated — styled string:'
            StyledString.of("") == StyledString.of("")
        and : 'A plain StyledString is never equal to a styled one, even if the text matches:'
            StyledString.of("text") != StyledString.of(f -> f.size(12), "text")
    }

    def 'A sequence of `StyledString` stored as text content on a component appears in the style toString'()
    {
        reportInfo """
            When `StyledString` instances are assembled into a `Tuple<StyledString>` and
            passed to `TextConf#content(...)` via the component style API, the style engine
            stores them verbatim inside the `TextConf`.  The resulting style configuration
            is then observable through `ComponentExtension.from(component).getStyle()`.
            
            This unit test verifies the round-trip: a sequence of three styled segments
            — a blue bold header, a plain body paragraph, and a green italic conclusion —
            is correctly reflected in the component's style string representation, confirming
            that the content, placement, and line-wrapping hints all reach the style engine.
        """
        given : 'Three StyledString segments that together form a mini logical argument:'
            var header = StyledString.of(f -> f.color("blue").size(18).weight(2), "Premise: ")
            var body   = StyledString.of("If a trait equalized non human animal has moral worth.")
            var footer = StyledString.of(f -> f.color("green").posture(0.2f), "Then, it ought to be treated accordingly.")
        and : 'We also create styled strings which will be simplified away, since they are effectively no-ops:'
            var emptyStyledString = StyledString.of("")  // empty content, no style
            var invisibleStyledString = StyledString.of(f -> f.size(0), "Invisible")  // non-empty content, but zero-size font makes it invisible

        and : 'We pack them into a Tuple and apply them as text content on a UI box:'
            var content = Tuple.of(header, emptyStyledString, body, invisibleStyledString, footer)
            var box = UI.box()
                        .withStyle(conf -> conf
                            .text(t -> t
                                .content(content)
                                .placement(UI.Placement.TOP_LEFT)
                                .wrapLines(true)
                            )
                        )
                        .get(JBox)

        when : 'We extract the full style configuration of the box:'
            var styleString = ComponentExtension.from(box).getStyle().toString()

        then : 'The style string confirms that a non-trivial TextConf is present in the content layer:'
            !styleString.contains("texts=TextConf[NONE]")
            styleString.contains("texts=TextConf[")

        and : 'All three segment strings survive the round-trip intact:'
            styleString.contains("Premise: ")
            styleString.contains("If a trait equalized non human animal has moral worth.")
            styleString.contains("Then, it ought to be treated accordingly.")
        and : 'The full sequence is present as follows:'
            styleString.contains("texts=TextConf[" +
                        "content=Tuple<StyledString>[" +
                            "StyledString[string='Premise: ', style=FontConf[family=, size=18, posture=?, weight=2.0, spacing=0.0, underlined=?, strikeThrough=?, selectionColor=?, transform=?, paint=FontPaintConf[rgba(0,0,255,255)], backgroundPaint=FontPaintConf[NONE], horizontalAlignment=?, verticalAlignment=?]], " +
                            "StyledString[string='If a trait equalized non human animal has moral worth.', style=?], " +
                            "StyledString[string='Then, it ought to be treated accordingly.', style=FontConf[family=, size=?, posture=0.2, weight=?, spacing=0.0, underlined=?, strikeThrough=?, selectionColor=?, transform=?, paint=FontPaintConf[rgba(0,128,0,255)], backgroundPaint=FontPaintConf[NONE], horizontalAlignment=?, verticalAlignment=?]]" +
                        "], " +
                        "fontConf=FontConf[NONE], " +
                        "clipArea=INTERIOR, " +
                        "placementBoundary=INTERIOR_TO_CONTENT, " +
                        "placement=TOP_LEFT, " +
                        "offset=Offset[x=0, y=0], " +
                        "wrapLines=true, " +
                        "autoPreferredHeight=false" +
                    "]")
    }

    def 'A text configuration that carries only an empty content tuple simplifies to "TextConf[NONE]"'()
    {
        reportInfo """
            Like every other sub-configuration in SwingTree's style engine, `TextConf` can
            be simplified to its canonical null-object representation `TextConf[NONE]` when
            all of its properties are at their default, no-op values.
            
            An empty content tuple combined with a no-op base font is the most common
            "effectively invisible" case: no text is specified, so nothing would ever be
            rendered.  Simplifying it away keeps the style object lean, improves cache hit
            rates (the style is used as a cache key for rendering buffers), and prevents
            spurious repaints when the style is otherwise identical.
        """
        given : 'A UI box whose text style has an explicitly empty content tuple:'
            var box = UI.box()
                        .withStyle(conf -> conf
                            .text(t -> t
                                .content(Tuple.of(StyledString.class))  // empty tuple
                                .wrapLines(true)              // non-default, but no content to render
                            )
                        )
                        .get(JBox)

        expect : 'The style string shows that the empty text configuration was simplified to NONE:'
            ComponentExtension.from(box).getStyle().toString() == "StyleConf[" +
                    "LayoutConf[NONE], " +
                    "BorderConf[NONE], " +
                    "BaseConf[NONE], " +
                    "FontConf[NONE], " +
                    "DimensionalityConf[NONE], " +
                    "StyleConfLayers[" +
                        "filter=FilterConf[NONE], " +
                        "background=StyleConfLayer[EMPTY], " +
                        "content=StyleConfLayer[EMPTY], " +
                        "border=StyleConfLayer[EMPTY], " +
                        "foreground=StyleConfLayer[EMPTY]" +
                    "], " +
                    "properties=[]" +
                "]"
    }

    def 'The styled text of a component can affect its preferred if desired.'(
        UI.Placement placement
    ) {
        reportInfo """
            An important requirement when displaying text on a GUI component
            is for it to be resized within a layout accordingly.
            Resizing of components dynamically to fit content happens through
            the 'preferred size', which the layout manager uses to make a
            fair and balanced layout.
            
            When you configure styled text through the SwingTree style API,
            then you can use the "autoPreferredHeight" property to tell
            SwingTree to automatically compute and set the preferred height
            of a component based on the current width of the component and
            the styled text it displays.
        """
        given : 'First, we initialize SwingTree with a default UI scale of 1.0 to ensure consistent font size calculations:'
            SwingTree.initializeUsing( it -> {
                it = it.uiScaleFactor(1.0f)
                it = SwingTreeTestConfigurator.get().configure(it) // Loads default fonts!
            })
        and : 'We create variables for the "autoPreferredHeight", "wrapLines" and some styled string snippets:'
            var autoPrefHeight = true
            var wrapLines = true
            var fontSizeMultiplier = 1
            var string1 = StyledString.of(f -> f.size(18 * fontSizeMultiplier), "Hello world! ")
            var string2 = StyledString.of("\n")
            var string3 = StyledString.of(f -> f.size(12 * fontSizeMultiplier), "This is a test of the autoPreferredHeight property.")
        and : 'A tuple that contains the styled string snippets:'
            var content = Tuple.of(string1, string2, string3)
        and : 'Finally we create a box with the styled text and the `autoPreferredHeight` property:'
            var box = UI.box()
                        .withStyle(conf -> conf
                            .text(t -> t
                                .font( f -> f.family("Ubuntu") )
                                .content(content)
                                .autoPreferredHeight(autoPrefHeight)
                                .wrapLines(wrapLines)
                                .placement(placement)
                            )
                        )
                        .get(JBox)

        expect : 'Initially, the preferred height cannot be known, since the component does not have a width yet:'
            box.getSize() == new java.awt.Dimension(0,0)
            box.getPreferredSize().height == 0
        when : 'We set the width of the box to a fixed value and then simulate a paint event.'
            UI.runNow(()->{
                box.setSize(350, 0)  // set an arbitrary height
                var dummyBuffer = new java.awt.image.BufferedImage(1, 1, java.awt.image.BufferedImage.TYPE_INT_ARGB)
                box.paintComponent(dummyBuffer.createGraphics())  // trigger the paint event to compute the preferred height
            })
        then : 'The preferred height is now computed based on the styled text content and the width:'
            box.getPreferredSize().height > 30
            box.getPreferredSize().height < 40

        when : 'We now make the width much much smaller to trigger more line-wrapping and therefore a larger preferred height.'
            UI.runNow(()->{
                box.setSize(25, 0)  // set an arbitrary height
                var dummyBuffer = new java.awt.image.BufferedImage(1, 1, java.awt.image.BufferedImage.TYPE_INT_ARGB)
                box.paintComponent(dummyBuffer.createGraphics())  // trigger the paint event to compute the preferred height
            })
        then : 'The preferred height is now much larger due to the increased line-wrapping:'
            box.getPreferredSize().height > 270
            box.getPreferredSize().height < 300

        when : 'We turn off line-wrapping, which should get us back to the previous preferred height!'
            UI.runNow(()->{
                wrapLines = false
                var dummyBuffer = new java.awt.image.BufferedImage(1, 1, java.awt.image.BufferedImage.TYPE_INT_ARGB)
                box.paintComponent(dummyBuffer.createGraphics())  // trigger the paint event to compute the preferred height
            })
        then : 'The preferred height is now back to the original value since line-wrapping is turned off:'
            box.getPreferredSize().height > 30
            box.getPreferredSize().height < 40

        when : 'We now turn line-wrapping on again but at the same time turn off the `autoPreferredHeight`.'
            UI.runNow(()->{
                wrapLines = true
                autoPrefHeight = false
                var dummyBuffer = new java.awt.image.BufferedImage(1, 1, java.awt.image.BufferedImage.TYPE_INT_ARGB)
                box.paintComponent(dummyBuffer.createGraphics())  // trigger the paint event to compute the preferred height
            })
        then : 'The preferred height is still at the original value since `autoPreferredHeight` is turned off:'
            box.getPreferredSize().height > 30
            box.getPreferredSize().height < 40

        when : 'We turn `autoPreferredHeight` back on...'
            UI.runNow(()->{
                autoPrefHeight = true
                var dummyBuffer = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)
                box.paintComponent(dummyBuffer.createGraphics())  // trigger the paint event to compute the preferred height
            })
        then : 'The preferred height is now back to the larger value wrapping takes more vertical space:'
            box.getPreferredSize().height > 270
            box.getPreferredSize().height < 300

        when : 'We now go back to the original width, which should reduce the preferred height again.'
            UI.runNow(()->{
                box.setSize(350, 0)  // set an arbitrary height
                var dummyBuffer = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)
                box.paintComponent(dummyBuffer.createGraphics())  // trigger the paint event to compute the preferred height
            })
        then : 'The preferred height is now back to the original value since the width allows for less line-wrapping:'
            box.getPreferredSize().height > 30
            box.getPreferredSize().height < 40

        when : 'Finally, we change the font size multiplier to make the text larger, which should also increase the preferred height.'
            UI.runNow(()->{
                fontSizeMultiplier = 2
                var dummyBuffer = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)
                box.paintComponent(dummyBuffer.createGraphics())  // trigger the paint event to compute the preferred height
            })
        then : 'The preferred height is now much larger due to the increased font size:'
            box.getPreferredSize().height > 90
            box.getPreferredSize().height < 100

        when : 'We now reduce the content to only contain the last string...'
            UI.runNow(()->{
                content = Tuple.of(string3)  // only the last string remains
                var dummyBuffer = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)
                box.paintComponent(dummyBuffer.createGraphics())  // trigger the paint event to compute the preferred height
            })
        then : 'The preferred height is now much smaller since there is much less content to render:'
            box.getPreferredSize().height > 50
            box.getPreferredSize().height < 60
        and : 'We verify that the string representation of the style conf is as expected:'
            ComponentExtension.from(box).getStyle().toString().contains("DimensionalityConf[minWidth=?, minHeight=?, maxWidth=?, maxHeight=?, preferredWidth=?, preferredHeight=${box.getPreferredSize().height as int}, width=?, height=?]")
            ComponentExtension.from(box).getStyle().toString().contains(
                    "texts=TextConf[" +
                            "content=Tuple<StyledString>[" +
                                "StyledString[string='This is a test of the autoPreferredHeight property.', style=FontConf[family=Ubuntu, size=24, posture=?, weight=?, spacing=0.0, underlined=?, strikeThrough=?, selectionColor=?, transform=?, paint=FontPaintConf[NONE], backgroundPaint=FontPaintConf[NONE], horizontalAlignment=?, verticalAlignment=?]]" +
                            "], " +
                            "fontConf=FontConf[" +
                                "family=Ubuntu, size=?, posture=?, weight=?, spacing=0.0, underlined=?, " +
                                "strikeThrough=?, selectionColor=?, transform=?, paint=FontPaintConf[NONE], " +
                                "backgroundPaint=FontPaintConf[NONE], horizontalAlignment=?, verticalAlignment=?" +
                            "], " +
                            "clipArea=INTERIOR, placementBoundary=INTERIOR_TO_CONTENT, placement=${placement}, " +
                            "offset=Offset[x=0, y=0], wrapLines=true, autoPreferredHeight=true" +
                        "]"
            )

        when : 'We remove all contents entirely, which should reset the preferred height to 0 since there is no text to render.'
            UI.runNow(()->{
                content = Tuple.of(StyledString.class)  // empty tuple
                var dummyBuffer = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)
                box.paintComponent(dummyBuffer.createGraphics())  // trigger the paint event to compute the preferred height
            })
        then : 'The preferred height is now back to 0 since there is no content to render:'
            box.getPreferredSize().height == 0

        where : 'We use different placements, these should not affect the preferred height since the text content and wrapping is the same:'
            placement << [
                    UI.Placement.TOP_LEFT,
                    UI.Placement.TOP,
                    UI.Placement.TOP_RIGHT,
                    UI.Placement.LEFT,
                    UI.Placement.CENTER,
                    UI.Placement.RIGHT,
                    UI.Placement.BOTTOM_LEFT,
                    UI.Placement.BOTTOM,
                    UI.Placement.BOTTOM_RIGHT,
                    UI.Placement.UNDEFINED
            ]
    }
}
