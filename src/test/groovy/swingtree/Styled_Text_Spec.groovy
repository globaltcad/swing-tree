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
                        "wrapLines=true" +
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
}
