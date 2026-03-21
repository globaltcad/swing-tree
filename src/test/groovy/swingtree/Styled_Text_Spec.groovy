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

    def 'The styled text of a component can affect its preferred height if desired.'(
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

    def 'The preferred height of styled text varies with `UI.ComponentBoundary` when margins, borders, and paddings are present.'()
    {
        reportInfo """
            The `placementBoundary` property on `TextConf` determines which rectangular
            region of the box model the text is laid out within.  When a component has
            non-zero margins, border widths, or paddings, the four available boundaries
            each carve out a progressively smaller area:

                OUTER_TO_EXTERIOR   — the full component allocation (margin included)
                EXTERIOR_TO_BORDER  — after the margin, before the border
                BORDER_TO_INTERIOR  — after the border, before the padding
                INTERIOR_TO_CONTENT — after the padding (the default)

            Because the available width shrinks as the boundary moves inward,
            line-wrapping kicks in sooner, which increases the preferred height.
            This test sets up a component with generous horizontal margin, border, and
            padding and confirms that the preferred height increases monotonically
            as the boundary moves from outer to inner.
        """
        given : 'We initialize SwingTree with a fixed UI scale to keep font metrics deterministic:'
            SwingTree.initializeUsing( it -> {
                it = it.uiScaleFactor(1.0f)
                it = SwingTreeTestConfigurator.get().configure(it)
            })
        and : 'A reasonably long text that will wrap at moderate widths:'
            var text = StyledString.of(
                            f -> f.size(14),
                            "The quick brown fox jumps over the lazy dog, and then it does it again just to be sure."
                        )
            var content = Tuple.of(text)

        and : 'A helper closure that builds a box with a given boundary, paints it, and returns the preferred height:'
            def preferredHeightFor = { UI.ComponentBoundary boundary ->
                int[] result = new int[1]
                UI.runNow({
                    var box = UI.box()
                                .withStyle(conf -> conf
                                    .margin(0, 40, 0, 40)
                                    .borderWidth(20)
                                    .padding(0, 30, 0, 30)
                                    .text(t -> t
                                        .font(f -> f.family("Ubuntu"))
                                        .content(content)
                                        .placementBoundary(boundary)
                                        .wrapLines(true)
                                        .autoPreferredHeight(true)
                                    )
                                )
                                .get(JBox)
                    box.setSize(400, 0)
                    var buf = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)
                    box.paintComponent(buf.createGraphics())
                    result[0] = box.getPreferredSize().height
                })
                return result[0]
            }

        when : 'We compute the preferred height for every boundary type:'
            int heightOuter    = preferredHeightFor(UI.ComponentBoundary.OUTER_TO_EXTERIOR)
            int heightExterior = preferredHeightFor(UI.ComponentBoundary.EXTERIOR_TO_BORDER)
            int heightBorder   = preferredHeightFor(UI.ComponentBoundary.BORDER_TO_INTERIOR)
            int heightInterior = preferredHeightFor(UI.ComponentBoundary.INTERIOR_TO_CONTENT)

        then : 'Every height is positive (the text is non-empty and wraps):'
            heightOuter    > 0
            heightExterior > 0
            heightBorder   > 0
            heightInterior > 0

        and : 'The heights increase as the boundary narrows the available width:'
            heightOuter    <= heightExterior
            heightExterior <= heightBorder
            heightBorder   <= heightInterior

        and : """
            The outermost boundary gives the most horizontal space, so
            its height must be strictly less than the innermost one, where
            the margin + border + padding eat into the width from both sides:
        """
            heightOuter < heightInterior
    }

    def 'Only horizontal padding affects the preferred height — and only when line-wrapping is enabled.'()
    {
        reportInfo """
            When a component has padding only on the left and right (no top or
            bottom padding), the text's available width is reduced, which can
            cause additional line-wrapping and therefore a taller preferred height.

            However, this effect is *only* observable when `wrapLines` is true!
            With wrapping turned off the text stays on a single line regardless
            of how narrow the available width becomes, so the horizontal padding
            cannot change the preferred height.

            This test demonstrates both sides of that coin.
        """
        given : 'We initialize SwingTree with a fixed UI scale:'
            SwingTree.initializeUsing( it -> {
                it = it.uiScaleFactor(1.0f)
                it = SwingTreeTestConfigurator.get().configure(it)
            })
        and : 'A long text that will wrap when horizontal space is tight:'
            var text = StyledString.of(
                            f -> f.size(14),
                            "Horizontal padding narrows the text area, which triggers more wrapping and a taller layout."
                        )
            var content = Tuple.of(text)

        and : 'A helper that builds a box with configurable padding and wrapLines, then returns the preferred height:'
            def heightWith = { int leftRight, boolean wrap ->
                int[] result = new int[1]
                UI.runNow({
                    var box = UI.box()
                                .withStyle(conf -> conf
                                    .padding(0, leftRight, 0, leftRight)
                                    .text(t -> t
                                        .font(f -> f.family("Ubuntu"))
                                        .content(content)
                                        .placementBoundary(UI.ComponentBoundary.INTERIOR_TO_CONTENT)
                                        .wrapLines(wrap)
                                        .autoPreferredHeight(true)
                                    )
                                )
                                .get(JBox)
                    box.setSize(350, 0)
                    var buf = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)
                    box.paintComponent(buf.createGraphics())
                    result[0] = box.getPreferredSize().height
                })
                return result[0]
            }

        when : 'We compute heights with no padding and with 60px left+right padding, wrapping ON:'
            int wrapNoPad    = heightWith(0,  true)
            int wrapWithPad  = heightWith(60, true)

        then : 'The padded version is taller because the available width is 120px narrower:'
            wrapWithPad > wrapNoPad

        when : 'We do the same comparison but with wrapping OFF:'
            int noWrapNoPad   = heightWith(0,  false)
            int noWrapWithPad = heightWith(60, false)

        then : 'Without wrapping, the padding does not change the preferred height — the text is a single line either way:'
            noWrapNoPad == noWrapWithPad
    }

    def 'Vertical padding adds to the preferred height regardless of whether line-wrapping is on or off.'()
    {
        reportInfo """
            Top and bottom padding contribute directly to the vertical extent
            of the text placement area.  Unlike horizontal padding — whose
            effect on height is indirect (through wrapping) — vertical padding
            simply shifts the text downward and extends the required height
            by the sum of top and bottom insets.

            This is true whether `wrapLines` is enabled or not, because the
            additional height comes from the insets themselves, not from extra
            line-breaks.
        """
        given : 'We initialize SwingTree with a fixed UI scale:'
            SwingTree.initializeUsing( it -> {
                it = it.uiScaleFactor(1.0f)
                it = SwingTreeTestConfigurator.get().configure(it)
            })
        and : 'A short text that fits on one line at the test width:'
            var text = StyledString.of(f -> f.size(14), "Short text.")
            var content = Tuple.of(text)

        and : 'A helper that builds a box with configurable top/bottom padding and wrapping:'
            def heightWith = { int topBottom, boolean wrap ->
                int[] result = new int[1]
                UI.runNow({
                    var box = UI.box()
                                .withStyle(conf -> conf
                                    .padding(topBottom, 0, topBottom, 0)
                                    .text(t -> t
                                        .font(f -> f.family("Ubuntu"))
                                        .content(content)
                                        .placementBoundary(UI.ComponentBoundary.INTERIOR_TO_CONTENT)
                                        .wrapLines(wrap)
                                        .autoPreferredHeight(true)
                                    )
                                )
                                .get(JBox)
                    box.setSize(350, 0)
                    var buf = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)
                    box.paintComponent(buf.createGraphics())
                    result[0] = box.getPreferredSize().height
                })
                return result[0]
            }

        expect : 'With wrapping ON, adding vertical padding increases the preferred height:'
            heightWith(20, true) > heightWith(0, true)
        and : 'With wrapping OFF, adding vertical padding still increases the preferred height:'
            heightWith(20, false) > heightWith(0, false)
        and : 'The vertical-padding increase is roughly the same whether wrapping is on or off:'
            Math.abs(
                (heightWith(20, true) - heightWith(0, true)) -
                (heightWith(20, false) - heightWith(0, false))
            ) <= 2 // rounding tolerance
    }

    def 'Border width affects the preferred height only for boundaries that include the border area.'()
    {
        reportInfo """
            A non-zero border width consumes space only **after**
            the `BORDER_TO_INTERIOR` boundaries but not **before**, like 
            `EXTERIOR_TO_BORDER` or `OUTER_TO_EXTERIOR`.  This means:

            * If the text uses `INTERIOR_TO_CONTENT` or `BORDER_TO_INTERIOR`, the
              border width reduces the available text width and therefore
              can increase the preferred height when line-wrapping is enabled.
            * If the text uses `EXTERIOR_TO_BORDER` or `OUTER_TO_EXTERIOR`, the
              border width *does NOT* reduce the available text width because the text
              must not share any horizontal space with the border.

            With line-wrapping enabled, the reduced width leads to more wrapping
            and a larger preferred height.
        """
        given : 'We initialize SwingTree with a fixed UI scale:'
            SwingTree.initializeUsing( it -> {
                it = it.uiScaleFactor(1.0f)
                it = SwingTreeTestConfigurator.get().configure(it)
            })
        and : 'A long text and a helper closure:'
            var text = StyledString.of(
                            f -> f.size(14),
                            "A moderately long sentence that will wrap at different widths depending on how much border eats into the area."
                        )
            var content = Tuple.of(text)

            def heightWith = { int borderWidth, UI.ComponentBoundary boundary ->
                int[] result = new int[1]
                UI.runNow({
                    var box = UI.box()
                                .withStyle(conf -> conf
                                    .borderWidth(borderWidth)
                                    .text(t -> t
                                        .font(f -> f.family("Ubuntu"))
                                        .content(content)
                                        .placementBoundary(boundary)
                                        .wrapLines(true)
                                        .autoPreferredHeight(true)
                                    )
                                )
                                .get(JBox)
                    box.setSize(350, 0)
                    var buf = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)
                    box.paintComponent(buf.createGraphics())
                    result[0] = box.getPreferredSize().height
                })
                return result[0]
            }

        expect : """
            For INTERIOR_TO_CONTENT, the border sits around the text area,
            so changing the border width DOES affect the preferred height:
        """
            heightWith(0, UI.ComponentBoundary.INTERIOR_TO_CONTENT) < heightWith(30, UI.ComponentBoundary.INTERIOR_TO_CONTENT)

        and : """
            For BORDER_TO_INTERIOR, the text area also starts right after the border,
            so the border width still has an effect:
        """
            heightWith(0, UI.ComponentBoundary.BORDER_TO_INTERIOR) < heightWith(30, UI.ComponentBoundary.BORDER_TO_INTERIOR)

        and : """
            For EXTERIOR_TO_BORDER, the text area we finally no longer
            place the text after the border, so the border width has NO effect on the available width or preferred height:
        """
            heightWith(30, UI.ComponentBoundary.EXTERIOR_TO_BORDER) == heightWith(0, UI.ComponentBoundary.EXTERIOR_TO_BORDER)

        and : """
            Finally, OUTER_TO_EXTERIOR, here the text is placed on the very edge of the component, 
            so the border is also outside the text area and has no effect on the preferred height:
        """
            heightWith(30, UI.ComponentBoundary.OUTER_TO_EXTERIOR) == heightWith(0, UI.ComponentBoundary.OUTER_TO_EXTERIOR)
    }

    def 'Margins affect the preferred height only when the placement boundary is inside the margin.'()
    {
        reportInfo """
            Margins live at the outermost rim of the box model. As a result, the
            `OUTER_TO_EXTERIOR` boundary is the only one whose preferred height
            is not affected by the margin area.
            All other boundaries sit inside the margin, so adding a margin
            affects the text's available width for those boundaries.

            With `wrapLines(true)`, a margin on the left and right reduces
            the available width for all boundaries except `OUTER_TO_EXTERIOR` 
            and thus triggers more wrapping and a taller preferred height for them.
        """
        given : 'We initialize SwingTree with a fixed UI scale:'
            SwingTree.initializeUsing( it -> {
                it = it.uiScaleFactor(1.0f)
                it = SwingTreeTestConfigurator.get().configure(it)
            })
        and : 'A long text and a helper closure:'
            var text = StyledString.of(
                            f -> f.size(14),
                            "Margins are on the outside, so only the outermost boundary is affected by them in terms of text width."
                        )
            var content = Tuple.of(text)

            def heightWith = { int leftRightMargin, UI.ComponentBoundary boundary ->
                int[] result = new int[1]
                UI.runNow({
                    var box = UI.box()
                                .withStyle(conf -> conf
                                    .margin(0, leftRightMargin, 0, leftRightMargin)
                                    .text(t -> t
                                        .font(f -> f.family("Ubuntu"))
                                        .content(content)
                                        .placementBoundary(boundary)
                                        .wrapLines(true)
                                        .autoPreferredHeight(true)
                                    )
                                )
                                .get(JBox)
                    box.setSize(400, 0)
                    var buf = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)
                    box.paintComponent(buf.createGraphics())
                    result[0] = box.getPreferredSize().height
                })
                return result[0]
            }

        expect : 'With OUTER_TO_EXTERIOR, adding a left+right margin has no effect the preferred height:'
            heightWith(50, UI.ComponentBoundary.OUTER_TO_EXTERIOR) == heightWith(0,  UI.ComponentBoundary.OUTER_TO_EXTERIOR)

        and : 'With EXTERIOR_TO_BORDER, the margin increases the height:'
            heightWith(50, UI.ComponentBoundary.EXTERIOR_TO_BORDER) > heightWith(0,  UI.ComponentBoundary.EXTERIOR_TO_BORDER)

        and : 'With BORDER_TO_INTERIOR, the margin also increases the height:'
            heightWith(50, UI.ComponentBoundary.BORDER_TO_INTERIOR) > heightWith(0,  UI.ComponentBoundary.BORDER_TO_INTERIOR)

        and : 'With INTERIOR_TO_CONTENT — same thing:'
            heightWith(50, UI.ComponentBoundary.INTERIOR_TO_CONTENT) > heightWith(0,  UI.ComponentBoundary.INTERIOR_TO_CONTENT)
    }

    def 'Combining margin, border, and padding produces a cumulative effect on text preferred height.'()
    {
        reportInfo """
            When margin, border, and padding are all non-zero, the combined
            horizontal insets reduce the available text width more and more
            as we move to the outermost boundary.  This test builds several
            boxes that all share the same box-model dimensions but differ in
            their `placementBoundary`, confirming that the cumulative effect
            of all three inset types is correctly reflected in the preferred
            height.

            Specifically, we expect:
              height(INTERIOR_TO_CONTENT) >= height(BORDER_TO_INTERIOR)
              >= height(EXTERIOR_TO_BORDER) >= height(OUTER_TO_EXTERIOR)
            …because each step outward adds more available width.
        """
        given : 'We initialize SwingTree with a fixed UI scale:'
            SwingTree.initializeUsing( it -> {
                it = it.uiScaleFactor(1.0f)
                it = SwingTreeTestConfigurator.get().configure(it)
            })
        and : 'A long text to ensure wrapping, and a helper closure:'
            var text = StyledString.of(
                            f -> f.size(14),
                            "All three inset types combine to squeeze the available width from both sides, forcing the text to wrap into more and more lines."
                        )
            var content = Tuple.of(text)

            def heightFor = { UI.ComponentBoundary boundary ->
                int[] result = new int[1]
                UI.runNow({
                    var box = UI.box()
                                .withStyle(conf -> conf
                                    .margin(5, 25, 5, 25)
                                    .borderWidth(15)
                                    .padding(5, 20, 5, 20)
                                    .text(t -> t
                                        .font(f -> f.family("Ubuntu"))
                                        .content(content)
                                        .placementBoundary(boundary)
                                        .wrapLines(true)
                                        .autoPreferredHeight(true)
                                    )
                                )
                                .get(JBox)
                    box.setSize(450, 0)
                    var buf = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)
                    box.paintComponent(buf.createGraphics())
                    result[0] = box.getPreferredSize().height
                })
                return result[0]
            }

        when : 'We measure the preferred height for each boundary:'
            int hOuter    = heightFor(UI.ComponentBoundary.OUTER_TO_EXTERIOR)
            int hExterior = heightFor(UI.ComponentBoundary.EXTERIOR_TO_BORDER)
            int hBorder   = heightFor(UI.ComponentBoundary.BORDER_TO_INTERIOR)
            int hInterior = heightFor(UI.ComponentBoundary.INTERIOR_TO_CONTENT)

        then : 'Moving the boundary inward never decreases the height (it can only grow in this scenario):'
            hOuter    < hExterior
            hExterior < hBorder
            hBorder   < hInterior

        and : 'The overall span from outermost to innermost is strictly greater (all insets contribute):'
            hOuter < hInterior
    }

    def 'Without line wrapping, horizontal insets have no effect on preferred height regardless of boundary.'()
    {
        reportInfo """
            When `wrapLines` is false the text is rendered on a single line
            no matter how narrow the available width becomes.  Consequently,
            neither margin, border width, nor padding can cause additional
            lines, and the preferred height stays constant across all
            `UI.ComponentBoundary` types — provided there is no vertical
            inset difference (this test uses zero vertical insets).

            This is an important edge case: users who disable wrapping
            should not see the preferred height jump around when they adjust
            horizontal spacing or switch boundary types.
        """
        given : 'We initialize SwingTree with a fixed UI scale:'
            SwingTree.initializeUsing( it -> {
                it = it.uiScaleFactor(1.0f)
                it = SwingTreeTestConfigurator.get().configure(it)
            })
        and : 'A text and a helper closure that disables wrapping:'
            var text = StyledString.of(
                            f -> f.size(14),
                            "This entire sentence stays on a single line even when the available width is very small."
                        )
            var content = Tuple.of(text)

            def heightFor = { UI.ComponentBoundary boundary ->
                int[] result = new int[1]
                UI.runNow({
                    var box = UI.box()
                                .withStyle(conf -> conf
                                    .margin(0, 30, 0, 30)
                                    .borderWidthAt(UI.Edge.LEFT, 15)
                                    .borderWidthAt(UI.Edge.RIGHT, 15)
                                    .padding(0, 25, 0, 25)
                                    .text(t -> t
                                        .font(f -> f.family("Ubuntu"))
                                        .content(content)
                                        .placementBoundary(boundary)
                                        .wrapLines(false)
                                        .autoPreferredHeight(true)
                                    )
                                )
                                .get(JBox)
                    box.setSize(400, 0)
                    var buf = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)
                    box.paintComponent(buf.createGraphics())
                    result[0] = box.getPreferredSize().height
                })
                return result[0]
            }

        expect : 'All four boundaries produce the exact same preferred height:'
            heightFor(UI.ComponentBoundary.OUTER_TO_EXTERIOR)  == heightFor(UI.ComponentBoundary.EXTERIOR_TO_BORDER)
            heightFor(UI.ComponentBoundary.EXTERIOR_TO_BORDER) == heightFor(UI.ComponentBoundary.BORDER_TO_INTERIOR)
            heightFor(UI.ComponentBoundary.BORDER_TO_INTERIOR) == heightFor(UI.ComponentBoundary.INTERIOR_TO_CONTENT)
    }

    def 'Mixed vertical and horizontal insets interact correctly with the placement boundary and wrapping.'()
    {
        reportInfo """
            This test exercises a nuanced combination: the component has
            both vertical *and* horizontal padding.  With wrapping turned on
            the horizontal padding reduces the available width and increases
            wrapping, while the vertical padding adds a fixed offset to the
            total height.

            With wrapping turned off only the vertical portion of the padding
            contributes to the preferred height, because horizontal padding
            cannot cause additional line-breaks on a single-line layout.

            We compare INTERIOR_TO_CONTENT (which honours padding) against
            BORDER_TO_INTERIOR (which ignores padding) to isolate the effect.
        """
        given : 'We initialize SwingTree with a fixed UI scale:'
            SwingTree.initializeUsing( it -> {
                it = it.uiScaleFactor(1.0f)
                it = SwingTreeTestConfigurator.get().configure(it)
            })
        and : 'A moderately long text:'
            var text = StyledString.of(
                            f -> f.size(14),
                            "Mixed insets combine vertical offsets with horizontal width constraints to shape the layout."
                        )
            var content = Tuple.of(text)

            def heightWith = { UI.ComponentBoundary boundary, boolean wrap ->
                int[] result = new int[1]
                UI.runNow({
                    var box = UI.box()
                                .withStyle(conf -> conf
                                    .padding(15, 50, 15, 50)
                                    .text(t -> t
                                        .font(f -> f.family("Ubuntu"))
                                        .content(content)
                                        .placementBoundary(boundary)
                                        .wrapLines(wrap)
                                        .autoPreferredHeight(true)
                                    )
                                )
                                .get(JBox)
                    box.setSize(350, 0)
                    var buf = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)
                    box.paintComponent(buf.createGraphics())
                    result[0] = box.getPreferredSize().height
                })
                return result[0]
            }

        when : 'We compare the two boundaries with wrapping ON:'
            int wrapInner = heightWith(UI.ComponentBoundary.INTERIOR_TO_CONTENT, true)
            int wrapOuter = heightWith(UI.ComponentBoundary.BORDER_TO_INTERIOR, true)

        then : """
            INTERIOR_TO_CONTENT accounts for both the vertical and horizontal padding,
            leading to more wrapping and additional vertical space — it must be taller:
        """
            wrapInner > wrapOuter

        when : 'We compare the two boundaries with wrapping OFF:'
            int noWrapInner = heightWith(UI.ComponentBoundary.INTERIOR_TO_CONTENT, false)
            int noWrapOuter = heightWith(UI.ComponentBoundary.BORDER_TO_INTERIOR, false)

        then : """
            Without wrapping the horizontal padding cannot add lines.
            Only the vertical padding adds height, so the inner boundary is
            still taller — but the difference comes purely from the top+bottom insets:
        """
            noWrapInner > noWrapOuter
    }
}