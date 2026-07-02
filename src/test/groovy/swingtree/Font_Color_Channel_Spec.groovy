package swingtree

import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Timeout
import spock.lang.Title
import sprouts.Var
import swingtree.threading.EventProcessor
import utility.Utility

import javax.swing.JButton
import javax.swing.JCheckBox
import javax.swing.JLabel
import javax.swing.JTextField
import java.awt.Color
import java.awt.font.TextAttribute
import java.util.concurrent.TimeUnit

@Title("Where The Font Color Lives")
@Narrative('''

    In AWT's model, a `java.awt.Font` describes glyph *geometry* only — family,
    size, weight, posture. The color of text natively belongs to the component's
    **foreground** property, which is what every look-and-feel consults when
    painting text (that is why `setForeground` exists at all).

    A color *can* be embedded into a font as a `TextAttribute.FOREGROUND`
    attribute — but that flips `Font.hasLayoutAttributes()`, which makes the JDK
    build a `TextLayout` for **every** `stringWidth` and `drawString` of that
    font (benchmarked as the dominant text cost of heavily styled UIs), and it
    silently overrides the look-and-feel's *state* colors, so disabled text
    keeps the embedded color instead of graying out.

    SwingTree therefore routes a **solid** font color through the foreground
    channel when installing styled fonts on components, while gradient and
    noise font paints — which no foreground property can express — stay in the
    font as attributes and knowingly pay the `TextLayout` price. This
    specification is the behavioral contract of that split: which style
    properties flip `hasLayoutAttributes`, how the foreground is applied and
    restored, and how the state-color bug this design fixes stays fixed.

''')
@Timeout(value = 60, unit = TimeUnit.SECONDS)
class Font_Color_Channel_Spec extends Specification
{
    def setup() {
        SwingTree.get().setEventProcessor(EventProcessor.COUPLED)
        SwingTree.get().setUiScaleFactor(1f)
    }

    def cleanup() {
        SwingTree.clear()
    }

    def 'Only genuinely layout-relevant font styles flip the expensive hasLayoutAttributes flag.'(
        String description, Closure<Object> style, boolean expensive
    ) {
        reportInfo """
            `Font.hasLayoutAttributes()` decides which measuring/drawing regime the
            JDK uses: `false` means cheap cached advance sums, `true` means a fresh
            `TextLayout` per call. Styling the font's geometry (size, weight, family)
            or its **solid color** (routed through the foreground channel) must NOT
            flip it — while letter spacing, underline, strikethrough and gradient
            paints genuinely need the layout pipeline and honestly do.
        """
        given : 'A label styled with one particular font property.'
            var label = UI.label("Regime").withStyle({ it.componentFont(style) }).get(JLabel)
        expect : 'The font is exactly as expensive to measure as the property demands.'
            label.getFont().hasLayoutAttributes() == expensive
        where :
            description        | style                                                        || expensive
            "size"             | { f -> f.size(23) }                                          || false
            "weight (bold)"    | { f -> f.weight(2) }                                         || false
            "family"           | { f -> f.family("Serif") }                                   || false
            "solid color"      | { f -> f.color(Color.RED) }                                  || false
            "size + color"     | { f -> f.size(23).color(Color.BLUE) }                        || false
            "letter spacing"   | { f -> f.spacing(0.05f) }                                    || true
            "underlined"       | { f -> f.underlined(true) }                                  || true
            "strike through"   | { f -> f.strikeThrough(true) }                               || true
            "gradient paint"   | { f -> f.gradient(g -> g.colors(Color.RED, Color.BLUE)) }    || true
    }

    def 'A solid font color is applied through the foreground property, not the font.'(
        Closure<Object> declaration
    ) {
        reportInfo """
            The contract, per component type: the installed font carries NO
            `FOREGROUND` attribute (so it stays cheap to measure), and the color
            arrives where every LaF natively looks for it — `getForeground()`.
        """
        given : 'A styled component of the given type.'
            var component = declaration().get(javax.swing.JComponent)
        expect : 'The color went to the foreground property...'
            component.getForeground() == Color.RED
        and : '...and explicitly NOT into the font.'
            component.getFont().getAttributes().get(TextAttribute.FOREGROUND) == null
            !component.getFont().hasLayoutAttributes()
        where :
            declaration << [
                { UI.label("L").withStyle({ it.componentFont(f -> f.color(Color.RED)) }) },
                { UI.button("B").withStyle({ it.componentFont(f -> f.color(Color.RED)) }) },
                { UI.checkBox("C").withStyle({ it.componentFont(f -> f.color(Color.RED)) }) },
                { UI.textField("T").withStyle({ it.componentFont(f -> f.color(Color.RED)) }) }
            ]
    }

    def 'A gradient font paint stays inside the font, because no foreground property can express it.'()
    {
        given : 'A label with a gradient font paint.'
            var label = UI.label("Fancy")
                          .withStyle({ it.componentFont(f -> f.gradient(g -> g.colors(Color.RED, Color.BLUE))) })
                          .get(JLabel)
        expect : 'The paint travels as a font attribute (the TextLayout pipeline renders it)...'
            label.getFont().getAttributes().get(TextAttribute.FOREGROUND) != null
        and : '...which honestly puts this font into the expensive measuring regime.'
            label.getFont().hasLayoutAttributes()
    }

    def 'The styled font color wins over the more general foregroundColor style property.'()
    {
        reportInfo """
            Both properties target the same native channel. The font color is the
            more specific of the two, so it takes precedence — one channel, one
            explicit rule, instead of two mechanisms silently fighting.
        """
        given : 'A label styled with BOTH a base foreground color and a font color.'
            var label = UI.label("Precedence")
                          .withStyle({ it
                              .foregroundColor(Color.BLUE)
                              .componentFont(f -> f.color(Color.RED))
                          })
                          .get(JLabel)
        expect : 'The font color wins.'
            label.getForeground() == Color.RED
    }

    def 'When the font color style goes away, the original foreground is restored.'()
    {
        given : 'A conditional style, switched by a flag which starts OFF.'
            var styled = Var.of(false)
            var label  = UI.label("Restore me")
                           .withStyle({ it.componentFont(f -> styled.get() ? f.color(Color.RED) : f) })
                           .get(JLabel)
        and : 'A pre-style foreground, set while no font color is active.'
            var originalForeground = Color.BLACK
            label.setForeground(originalForeground)
            paint(label)
        expect : 'Without an active font color, the style engine leaves the foreground alone.'
            label.getForeground() == originalForeground

        when : 'The font color style becomes active.'
            styled.set(true)
            paint(label)
        then : 'The foreground is the styled color.'
            label.getForeground() == Color.RED

        when : 'The flag flips back and the style engine runs again.'
            styled.set(false)
            paint(label)
        then : 'The pre-style foreground is back.'
            label.getForeground() == originalForeground
    }

    def 'Disabling a component with a styled font color grays the text out properly.'()
    {
        reportInfo """
            This is the state-color bug the foreground channel fixes: a color
            embedded in the font overrides the look-and-feel's disabled text
            color, so disabled components used to keep their styled color and
            looked enabled. Routed through the foreground property, the LaF's
            disabled painting works as designed. We verify at the pixel level:
            the enabled label paints plenty of red, the disabled one none.
        """
        given : 'A label with a large, red-styled font.'
            var label = UI.label("DISABLED")
                          .withStyle({ it.componentFont(f -> f.size(28).color(Color.RED)) })
                          .get(JLabel)
        when : 'We render it enabled and count strongly red pixels.'
            var enabledRed = countRedDominantPixels(paint(label))
        then : 'The text is clearly painted red.'
            enabledRed > 50

        when : 'We disable it and render again.'
            label.setEnabled(false)
            var disabledRed = countRedDominantPixels(paint(label))
        then : 'The look-and-feel now paints its proper disabled text — no red at all.'
            disabledRed == 0
    }

    def 'UI.Font conversion keeps its color, and the without-color variant strips exactly that.'()
    {
        reportInfo """
            The public `UI.Font` API is full-fidelity: converting to an AWT font
            keeps a configured color as a `FOREGROUND` attribute (this also fixes
            a former bug where the color was silently lost in conversion). The
            explicitly named `toAwtFontWithoutColor()` is the variant for fonts
            you install on components yourself — geometry only, cheap to measure;
            apply the color via `setForeground`, like the style engine does.
        """
        given : 'A UI.Font carrying a solid color (created from an attributed AWT font).'
            var attributes = new HashMap()
            attributes.put(TextAttribute.FAMILY, "Dialog")
            attributes.put(TextAttribute.SIZE, 14)
            attributes.put(TextAttribute.FOREGROUND, Color.RED)
            var font = UI.Font.of(attributes)

        expect : 'The faithful conversion keeps the color, and is honestly expensive.'
            font.toAwtFont().getAttributes().get(TextAttribute.FOREGROUND) == Color.RED
            font.toAwtFont().hasLayoutAttributes()
        and : 'The without-color variant strips it, and is cheap to measure.'
            font.toAwtFontWithoutColor().getAttributes().get(TextAttribute.FOREGROUND) == null
            !font.toAwtFontWithoutColor().hasLayoutAttributes()
        and : 'Both agree on the geometry.'
            font.toAwtFont().getFamily() == font.toAwtFontWithoutColor().getFamily()
            font.toAwtFont().getSize()   == font.toAwtFontWithoutColor().getSize()
    }

    def 'Underline styling reaches the font as the value AWT actually understands.'()
    {
        reportInfo """
            A regression guard for a subtle bug: `TextAttribute.UNDERLINE` expects
            the Integer constant `UNDERLINE_ON` — a raw Boolean is silently dropped
            by `Font.deriveFont`, which used to make underline styling a no-op.
        """
        given : 'An underlined label.'
            var label = UI.label("Underlined")
                          .withStyle({ it.componentFont(f -> f.underlined(true)) })
                          .get(JLabel)
        expect : 'The attribute actually landed in the font, in AWT dialect.'
            label.getFont().getAttributes().get(TextAttribute.UNDERLINE) == TextAttribute.UNDERLINE_ON
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    /** Sizes the component to its preferred size and paints it, returning the image. */
    private java.awt.image.BufferedImage paint(java.awt.Component c) {
        def pref = c.getPreferredSize()
        c.setSize(Math.max(1, (int) pref.width), Math.max(1, (int) pref.height))
        return Utility.renderSingleComponent(c)
    }

    /** Counts pixels that are unmistakably red — tolerant of antialiasing, robust against noise. */
    private static int countRedDominantPixels(java.awt.image.BufferedImage image) {
        int count = 0
        for ( int x = 0; x < image.getWidth(); x++ )
            for ( int y = 0; y < image.getHeight(); y++ ) {
                int rgb = image.getRGB(x, y)
                int r = (rgb >> 16) & 0xFF, g = (rgb >> 8) & 0xFF, b = rgb & 0xFF
                if ( r > 150 && r > g + 100 && r > b + 100 )
                    count++
            }
        return count
    }
}
