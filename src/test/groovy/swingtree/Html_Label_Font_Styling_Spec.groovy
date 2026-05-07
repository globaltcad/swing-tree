package swingtree

import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title
import swingtree.style.FontConf
import swingtree.threading.EventProcessor
import utility.SwingTreeTestConfigurator

import javax.swing.JLabel
import javax.swing.UIManager
import javax.swing.plaf.basic.BasicHTML
import javax.swing.text.View
import java.awt.Color
import java.awt.Graphics2D
import java.awt.Rectangle
import java.awt.image.BufferedImage

@Title("Styling HTML Labels through FontConf")
@Narrative('''

    SwingTree's styling API treats every `JComponent` uniformly: when you write
    `withStyle(it -> it.fontColor("orange").fontFamily("Serif"))` you expect
    the result to look the same regardless of whether the label happens to
    display a plain string or a fragment of HTML.  In practice that is harder
    than it sounds, because Swing renders HTML through a completely different
    pipeline than its plain-text label rendering.

    A plain `JLabel` paints text using the component's `Font` and `Foreground`
    properties.  Setting font attributes through `FontConf` translates directly
    to a derived `Font` and (where applicable) a foreground color, and the
    result is what the user sees.

    An HTML `JLabel` (one whose text starts with `<html>`) is different:
    Swing builds an `HTMLDocument` and a `View` tree from the text and renders
    everything through CSS.  The label's own `Font` and `Foreground` only feed
    the *base* `body { ... }` rule that Swing seeds at parse time — they have
    no effect on already-resolved element attributes.  Bolting `FontConf`
    styling onto that pipeline therefore requires a different approach than
    plain text.

    SwingTree handles the difference internally by injecting a small
    `<head><style data-swingtree="injected">...</style></head>` block into
    the HTML text, *before* Swing parses it.  The injection is wrapped in a
    distinctive marker so SwingTree can recognise — and strip — its own
    previous injection on the next style cycle, leaving the user's HTML
    intact and recoverable.

    This specification is the user-facing contract: that styling HTML labels
    "just works", that nothing the user can put inside an HTML label breaks
    the styling pipeline, and that SwingTree's own injection is a self-cleaning
    affair, not a one-way transformation.

''')
@Subject([FontConf])
class Html_Label_Font_Styling_Spec extends Specification
{
    def setupSpec() {
        SwingTree.initializeUsing(SwingTreeTestConfigurator.get())
        SwingTree.get().setEventProcessor(EventProcessor.COUPLED_STRICT)
    }

    def cleanupSpec() {
        SwingTree.clear()
    }

    def setup() {
        UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName())
    }

    def cleanup() {
        UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName())
    }


    def 'A `fontColor` styler applied to an HTML label paints the text in that color.'()
    {
        reportInfo """
            From a user perspective the simplest expectation is this: if I write
            `withStyle(it -> it.fontColor("orange"))` and the label happens to
            contain HTML, the rendered text should be orange. Nothing more
            complicated than that.

            We verify this by rendering the label to an off-screen image and
            counting how many pixels fall inside an orange-ish colour cube.
            A baseline (un-styled) label produces zero such pixels; a styled
            label produces hundreds.  This is the strongest possible signal —
            it is independent of *how* SwingTree achieves the colour change,
            so it remains a valid test even if the implementation is rewritten
            later.
        """
        given : 'An HTML label without any styler — used to establish a baseline:'
            var plainLabel = UI.html("<h1>Hello</h1>").get(JLabel)
        and : 'A second HTML label with `fontColor("orange")` configured through `withStyle`:'
            var styledLabel = UI.html("<h1>Hello</h1>")
                                .withStyle({ it.fontColor("orange") })
                                .get(JLabel)

        when : 'Both labels are rendered into a buffered image:'
            int plainOrange  = countOrangePixels(renderHtmlLabel(plainLabel,  400, 200))
            int styledOrange = countOrangePixels(renderHtmlLabel(styledLabel, 400, 200))

        then : 'The plain label contains no orange pixels (its text is the platform default colour):'
            plainOrange == 0
        and : 'The styled label contains a substantial orange region — the rendered HTML text:'
            styledOrange > 100
    }


    def 'Italic, underline, and strike-through all reach the rendered HTML.'()
    {
        reportInfo """
            HTML rendering only honours a *subset* of CSS — it is, after all,
            a 1990s rendering pipeline frozen into the JDK.  This spec
            documents which font attributes we have verified actually affect
            the painted output.

            For each attribute we render two labels: one plain, one styled.
            Then we compare measurable properties of the rendered images.
            For italic we compare dark-pixel counts (italic glyphs use
            different character shapes); for underline and strike-through we
            measure how far the painted area extends below or above the
            baseline (these decorations add lines outside the glyph bounds).
        """
        given : 'A baseline HTML label without any decoration:'
            var plain = UI.html("Hello").get(JLabel)
        and : 'Three styled variants, one decoration each:'
            var italic       = UI.html("Hello").withStyle({ it.fontItalic(true) }).get(JLabel)
            var underlined   = UI.html("Hello").withStyle({ it.fontUnderline(true) }).get(JLabel)
            var strikeThrough = UI.html("Hello").withStyle({ it.fontStrikeThrough(true) }).get(JLabel)

        when : 'Each label is rendered into the same canvas:'
            var plainImg       = renderHtmlLabel(plain,       200, 60)
            var italicImg      = renderHtmlLabel(italic,      200, 60)
            var underlinedImg  = renderHtmlLabel(underlined,  200, 60)
            var strikeImg      = renderHtmlLabel(strikeThrough,200, 60)

        then : 'Italic produces visibly different glyph rendering — the dark-pixel count differs from the baseline:'
            countDarkPixels(italicImg) != countDarkPixels(plainImg)
        and : 'Underline extends the painted area further down the canvas than the baseline does:'
            bottomOfPaintedArea(underlinedImg) > bottomOfPaintedArea(plainImg)
        and : 'Strike-through paints an extra line — strictly more dark pixels than the baseline:'
            countDarkPixels(strikeImg) > countDarkPixels(plainImg)
    }


    def 'Multiple font attributes combine into one rendering.'()
    {
        reportInfo """
            Setting several attributes at once is the common case — a styler
            usually configures colour, family and size together.  This spec
            confirms that combining attributes does not cause one to silently
            cancel another: a label with both `fontColor("orange")` and
            `fontUnderline(true)` should be both orange *and* underlined.
        """
        given : 'An HTML label styled with both colour and underline:'
            var label = UI.html("Hello")
                          .withStyle({ it.fontColor("orange").fontUnderline(true) })
                          .get(JLabel)
        and : 'A baseline label, plain HTML, no styling:'
            var plain = UI.html("Hello").get(JLabel)

        when : 'Both labels are rendered:'
            var styledImg = renderHtmlLabel(label, 200, 60)
            var plainImg  = renderHtmlLabel(plain, 200, 60)

        then : 'The styled label is orange — counted in pixels:'
            countOrangePixels(styledImg) > 50
        and : 'And the painted area extends further down than the baseline (the underline):'
            bottomOfPaintedArea(styledImg) > bottomOfPaintedArea(plainImg)
    }


    def 'A plain (non-HTML) label is unaffected by the HTML-specific path.'()
    {
        reportInfo """
            The HTML styling pipeline must be invisible to plain-text labels.
            This spec exists to catch regressions where the marker injection
            accidentally leaks into labels whose text does not start with
            `<html>` — the kind of bug you only notice when something else
            in your codebase compares `label.getText()` for equality.
        """
        given : 'A plain-text JLabel with a `fontColor` styler:'
            var label = UI.label("Just plain text")
                            .withStyle({ it.fontColor("orange") })
                            .get(JLabel)

        expect : 'Its text remains the verbatim string the user supplied:'
            label.getText() == "Just plain text"
        and : 'No injection marker is anywhere to be seen:'
            !label.getText().contains("data-swingtree")
    }


    def 'An HTML label without any styler keeps its text exactly as the user wrote it.'()
    {
        reportInfo """
            Symmetric to the previous spec: when there is no styler attached,
            we must not touch the HTML text at all.  This is what makes the
            injection a *self-cleaning* mechanism rather than a one-way
            transformation: if you don't ask for styling, you don't get
            modified text.
        """
        given : 'An HTML label without any styler:'
            var label = UI.html("<h1>Untouched</h1>").get(JLabel)

        expect : 'Text is exactly the convenience-method output: `<html>` + body + `</html>`:'
            label.getText() == "<html><h1>Untouched</h1></html>"
        and : 'The injection marker is not present:'
            !label.getText().contains("data-swingtree")
    }


    def 'Repeated style applications never accumulate more than one injected style block.'()
    {
        reportInfo """
            SwingTree re-applies styling whenever something it cares about
            changes — animation states, hover effects, dynamic style sheets,
            and so on.  An HTML label's text must not grow `<head><style>`
            blocks like rings on a tree as time passes.  Each cycle strips
            the previous injection before computing the new one, so the
            result always contains at most one `data-swingtree="injected"`
            marker, regardless of how many cycles ran.
        """
        given : 'A styled HTML label:'
            var label = UI.html("<h1>Hello</h1>")
                            .withStyle({ it.fontColor("orange") })
                            .get(JLabel)

        when : 'The style cycle is forced to run a few more times:'
            UI.runNow {
                3.times { swingtree.style.ComponentExtension.from(label).gatherApplyAndInstallStyle(true) }
            }

        then : "Exactly one marker is present in the label's text — no double-injection:"
            (label.getText() =~ /data-swingtree="injected"/).size() == 1
    }


    def 'Uppercase `<HTML>` is recognised — the splice preserves the original case.'()
    {
        reportInfo """
            HTML's outer `<html>` tag is case-insensitive: a user (or a piece
            of generated content) might write `<HTML>` or even `<Html>`.
            `BasicHTML.isHTMLString` accepts any case, and so must our
            injection — without rewriting the user's tag in a different case
            in the process, since that would be a surprising side-effect on
            anything that round-trips the text through a comparison.
        """
        given : 'A JLabel with an uppercase HTML opening tag:'
            var label = UI.of(new JLabel("<HTML><h1>Hi</h1></HTML>"))
                          .withStyle({ it.fontColor("orange") })
                          .get(JLabel)

        expect : 'The original `<HTML>` prefix is preserved exactly — case included:'
            label.getText().startsWith("<HTML>")
        and : 'And the marker is in there too — i.e. the styler did run:'
            label.getText().contains('data-swingtree="injected"')
    }


    def 'A user-provided `<head><style>` survives alongside the injection.'()
    {
        reportInfo """
            Some HTML labels arrive with their own embedded stylesheet — for
            example because they were generated from a Markdown converter or
            copied verbatim from a design tool.  SwingTree must coexist with
            that: the user's `<style>` block stays in the document, and our
            injection is added without touching it.  Because SwingTree's
            block appears earlier in the HTML, the user's CSS wins on
            conflicts (the same way later stylesheet rules win in a regular
            web page).  This is a deliberate choice: HTML the user explicitly
            wrote should override framework-level defaults.
        """
        given : 'A label whose HTML already contains a stylesheet of its own:'
            var userHtml = '<html><head><style>body{font-family:Serif;}</style></head><body>Hi</body></html>'
            var label = UI.of(new JLabel(userHtml))
                          .withStyle({ it.fontColor("orange") })
                          .get(JLabel)

        expect : "SwingTree's injection sits next to the user's CSS:"
            label.getText().contains('data-swingtree="injected"')
        and : "The user's original stylesheet is preserved verbatim:"
            label.getText().contains('body{font-family:Serif;}')
    }


    def 'A hostile `font-family` cannot break out of the CSS declaration.'()
    {
        reportInfo """
            CSS injection is, in principle, a real category of issue: if a
            value gets concatenated into a stylesheet without sanitisation,
            an attacker (or, more realistically, a user with a quirky font
            name) can prematurely close the value's quotes, append unrelated
            declarations, and rewrite the page's appearance.

            For a `JLabel` the consequences are limited (no script execution,
            no cross-origin context), but the hygiene matters: a malformed
            CSS block can prevent *all* of our styling from being applied,
            which would be a confusing failure mode.

            We therefore strip — rather than escape — characters that could
            terminate a CSS string or open a new declaration: `"`, `;`, `\\`,
            `<`, `>`, `{`, `}`, and line breaks.  The user's font-family
            value is preserved otherwise, but cannot escape its enclosing
            quotes.
        """
        given : 'A font-family value crafted to terminate the CSS string and append a new declaration:'
            var hostile = 'Arial"; color: red; font-family: "'
        and : 'A label styled with that hostile family:'
            var label = UI.of(new JLabel("<html>Hi</html>"))
                          .withStyle({ it.fontFamily(hostile) })
                          .get(JLabel)

        when : "We extract the contents of SwingTree's injected `<style>` block:"
            var matcher = label.getText() =~ /<style data-swingtree="injected">(.*?)<\/style>/
            matcher.find()
            var injectedCss = matcher.group(1)

        then : 'There is exactly one rule body — one `{` and one `}`. The hostile value did not open a second one:'
            injectedCss.count('{') == 1
            injectedCss.count('}') == 1
        and : 'After removing the (sanitised) `font-family` declaration, no foreign declarations remain:'
            !injectedCss.replaceAll(/font-family:"[^"]*";/, "").contains("color:")
    }


    def 'Increasing `fontSize` makes the rendered HTML occupy a larger area.'()
    {
        reportInfo """
            `fontSize` (in points) shows up in our injected CSS as `font-size: Npt`,
            which Swing's HTML renderer honours.  We verify here that doubling
            the configured size produces a visibly bigger painted area —
            a property test, not a pixel-exact comparison.

            Note that body-level `font-size` is *inherited* by children that
            do not specify their own.  Heading tags like `<h1>` set their own
            font-size in the default stylesheet, so they would *not* observe
            this change.  We use plain inline text here.
        """
        given : 'Two HTML labels with the same content but very different sizes:'
            var small = UI.html("Hello").withStyle({ it.fontSize(8) }).get(JLabel)
            var large = UI.html("Hello").withStyle({ it.fontSize(32) }).get(JLabel)

        when : 'Each label is rendered onto the same canvas:'
            var smallImg = renderHtmlLabel(small, 400, 200)
            var largeImg = renderHtmlLabel(large, 400, 200)

        then : 'The 32-pt rendering paints visibly more pixels than the 8-pt one:'
            countDarkPixels(largeImg) > countDarkPixels(smallImg) * 4
    }


    def 'When a bound colour changes, the rendered HTML switches colour with it.'()
    {
        reportInfo """
            A common dynamic-styling pattern is a `Var<Color>` fed into the
            styler, so that user interaction or app state can drive the
            label's colour.  Each `Var` change must trigger a fresh style
            cycle, and the rendered output must reflect the *current* colour —
            not an accumulation of all previously-applied colours.

            We verify this property by *rendering* both states and looking at
            the painted pixels.  An orange-styled label produces orange-coloured
            text and no blue text; switching the bound colour to blue must
            invert that — blue text and no orange.
        """
        given : 'An HTML label whose colour is bound to a `Var<Color>` initially holding orange:'
            var hue = sprouts.Var.of(new Color(255, 165, 0))
            var label = UI.html("<h1>Hi</h1>")
                          .withRepaintOn(hue)
                          .withStyle({ it.fontColor(hue.get()) })
                          .get(JLabel)
        and : "Render the initial state and check the painted colours:"
            var beforeImg = renderHtmlLabel(label, 400, 200)
            int orangeBefore = countOrangePixels(beforeImg)
            int blueBefore   = countBluePixels(beforeImg)

        when : 'The bound `Var` is set to blue:'
            UI.runNow { hue.set(new Color(0, 0, 255)) }
        and : 'And the label is rendered again:'
            var afterImg = renderHtmlLabel(label, 400, 200)
            int orangeAfter = countOrangePixels(afterImg)
            int blueAfter   = countBluePixels(afterImg)

        then : 'The initial render is orange and not blue:'
            orangeBefore > 100
            blueBefore   == 0
        and : 'After the colour change the render is blue and no longer orange:'
            blueAfter    > 100
            orangeAfter  == 0
    }


    def 'When a styler conditionally drops back to the default, the rendered HTML returns to the platform colour.'()
    {
        reportInfo """
            The styling pipeline must be a *self-cleaning* mechanism: when the
            styler stops contributing any font configuration — because a flag
            flipped, an animation ended, or some other condition turned off —
            the next style cycle has to undo the visual effect, leaving the
            HTML rendered with the platform default colour again.

            We model this with a `Var<Boolean>` driving a conditional styler.
            When the flag is true the styler returns an orange `FontConf`;
            when it flips to false the styler returns the unchanged identity.
            Switching the flag must therefore make the orange disappear from
            the rendered output without ever touching the label directly.
        """
        given : 'A flag controlling whether the styler should colour the text:'
            var enabled = sprouts.Var.of(true)
        and : 'An HTML label whose styler is conditional on that flag:'
            var label = UI.html("<h1>Hi</h1>")
                          .withRepaintOn(enabled)
                          .withStyle({ s -> enabled.get() ? s.fontColor("orange") : s })
                          .get(JLabel)
            int orangeBefore = countOrangePixels(renderHtmlLabel(label, 400, 200))

        when : 'The flag flips off, causing the styler to stop contributing any font config:'
            UI.runNow { enabled.set(false) }
        and : 'The label is rendered again:'
            int orangeAfter = countOrangePixels(renderHtmlLabel(label, 400, 200))

        then : 'The styling was clearly visible while the flag was on:'
            orangeBefore > 100
        and : 'And once the flag flipped off, no orange pixels remain — the label is back to the platform default colour:'
            orangeAfter == 0

        when : 'The flag flips on again:'
            UI.runNow { enabled.set(true) }
        and : 'We count the orange pixels again...'
            int orangeFinally = countOrangePixels(renderHtmlLabel(label, 400, 200))
        then : 'We are now back to the original number of pixels!'
            orangeBefore == orangeFinally
    }


    def 'Adding a styler dynamically (after the label is built) updates the rendering.'()
    {
        reportInfo """
            Most stylers are configured at build time via `withStyle(...)`.
            But it is also possible to install a styler after the fact —
            for example in an event handler that responds to user input.
            The rendering must reflect the new styler the next time the label
            is painted.
        """
        given : 'An HTML label without any styling configured up-front:'
            var label = UI.html("<h1>Hi</h1>").get(JLabel)
            int orangeBefore = countOrangePixels(renderHtmlLabel(label, 400, 200))

        when : 'A `fontColor` styler is added after the build:'
            UI.runNow {
                swingtree.style.ComponentExtension.from(label).addStyler({ it.fontColor("orange") })
            }
        and : 'The label is rendered again:'
            int orangeAfter = countOrangePixels(renderHtmlLabel(label, 400, 200))

        then : 'The unstyled label produced no orange pixels:'
            orangeBefore == 0
        and : 'But once the styler is in place, the rendering is orange:'
            orangeAfter > 100
    }


    def 'Stylers compose — a later styler overrides an earlier one for the same property.'()
    {
        reportInfo """
            `withStyle(...)` calls are chainable, and stylers compose in
            declaration order: the next styler receives the `FontConf`
            produced by the previous one.  When two stylers both touch the
            same property (here, `fontColor`), the last one wins.

            We make this concrete by chaining an orange styler followed by a
            blue styler, then rendering: only blue should appear.  This
            guarantees that nothing in the HTML pipeline is preserving stale
            state from the earlier styler.
        """
        given : 'An HTML label with two chained stylers — orange first, blue second:'
            var label = UI.html("<h1>Hi</h1>")
                          .withStyle({ it.fontColor("orange") })
                          .withStyle({ it.fontColor("blue") })
                          .get(JLabel)

        when : 'The label is rendered:'
            var img = renderHtmlLabel(label, 400, 200)
            int orange = countOrangePixels(img)
            int blue   = countBluePixels(img)

        then : 'Only the later (blue) styler is reflected in the painted output:'
            blue > 100
            orange == 0
    }


    def 'When a bound `Var<String>` text changes, the styling carries over to the new text.'()
    {
        reportInfo """
            HTML labels can have their text bound to a `Var<String>`.  When
            the property fires a new value, the label updates its text — and
            the styling has to ride along.  Re-rendering after the text change
            should still show the configured colour.

            This catches a class of bug where the injection survives only one
            text change: e.g. if we cached the original text by identity and
            the next `setText` from sprouts overwrote it before the next style
            cycle could re-inject.
        """
        given : 'A `Var<String>` initially holding the inner HTML:'
            var content = sprouts.Var.of("<h1>First</h1>")
        and : 'A label bound to that `Var`, styled orange:'
            var label = UI.html(content).withStyle({ it.fontColor("orange") }).get(JLabel)
            int orangeFirst = countOrangePixels(renderHtmlLabel(label, 400, 200))

        when : 'The bound text is updated:'
            UI.runNow { content.set("<h1>Second</h1>") }
        and : 'The label is rendered again:'
            int orangeSecond = countOrangePixels(renderHtmlLabel(label, 400, 200))

        then : 'Both renders show the orange styling — the change of text did not strip it:'
            orangeFirst  > 100
            orangeSecond > 100
    }


    def 'A `StyleSheet` that targets a component type applies to HTML labels too.'()
    {
        reportInfo """
            Most projects centralise styling in a `StyleSheet` rather than scattering
            `withStyle(...)` calls across the call sites.  A stylesheet rule like
            `add(type(JLabel.class), it -> it.fontColor("orange"))` should apply to
            *every* `JLabel`, HTML or not.  This spec exists to confirm that the
            HTML-specific code is just a different branch inside the regular styling
            pipeline — not a separate channel that bypasses stylesheets.

            **Note about `UI.use` usage:** the lambda passed to `UI.use` should
            return the *component itself* (or call `.get(JLabel)` inside the scope)
            rather than a builder, because each call to `.get(...)` produces a
            fresh component instance.  Returning a builder and calling `.get(...)`
            *outside* the `UI.use` scope yields an unstyled component.
        """
        given : 'A stylesheet that paints all `JLabel`s orange:'
            var sheet = new swingtree.style.StyleSheet() {
                @Override protected void configure() {
                    add(type(JLabel.class), { it.fontColor("orange") })
                }
            }
        and : 'An HTML label built and extracted inside the scope of that stylesheet:'
            var label = (JLabel) UI.use(sheet, { UI.html("<h1>Hi</h1>").get(JLabel) })

        when : 'The label is rendered:'
            int orange = countOrangePixels(renderHtmlLabel(label, 400, 200))

        then : 'The HTML text picks up the stylesheet rule and renders in orange:'
            orange > 100
    }


    // -------- helpers used by the specs above --------

    /** Renders the given label's HTML view into a fresh image. */
    private static BufferedImage renderHtmlLabel(JLabel label, int width, int height) {
        UI.runNow { label.setSize(width, height); label.doLayout() }
        var view = (View) label.getClientProperty(BasicHTML.propertyKey)
        var img  = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        Graphics2D g = img.createGraphics()
        g.setColor(Color.WHITE)
        g.fillRect(0, 0, width, height)
        g.setClip(0, 0, width, height)
        view.setSize(width, height)
        view.paint(g, new Rectangle(0, 0, width, height))
        g.dispose()
        return img
    }

    /** Pixel count for hues "close to" orange — used as a robust signal across renderers. */
    private static int countOrangePixels(BufferedImage img) {
        int count = 0
        for (int y = 0; y < img.getHeight(); y++)
            for (int x = 0; x < img.getWidth(); x++) {
                int rgb = img.getRGB(x, y)
                int r = (rgb >> 16) & 0xff
                int g = (rgb >> 8)  & 0xff
                int b =  rgb        & 0xff
                if (Math.abs(r - 255) < 60 && Math.abs(g - 165) < 60 && b < 60) count++
            }
        return count
    }

    /** Pixel count for hues "close to" blue — symmetric counterpart to {@link #countOrangePixels}. */
    private static int countBluePixels(BufferedImage img) {
        int count = 0
        for (int y = 0; y < img.getHeight(); y++)
            for (int x = 0; x < img.getWidth(); x++) {
                int rgb = img.getRGB(x, y)
                int r = (rgb >> 16) & 0xff
                int g = (rgb >> 8)  & 0xff
                int b =  rgb        & 0xff
                if (r < 60 && g < 60 && Math.abs(b - 255) < 60) count++
            }
        return count
    }

    /** Pixel count for visibly dark pixels — proxy for how much glyph + decoration was drawn. */
    private static int countDarkPixels(BufferedImage img) {
        int count = 0
        for (int y = 0; y < img.getHeight(); y++)
            for (int x = 0; x < img.getWidth(); x++) {
                int rgb = img.getRGB(x, y)
                int r = (rgb >> 16) & 0xff
                int g = (rgb >> 8)  & 0xff
                int b =  rgb        & 0xff
                if (r < 80 && g < 80 && b < 80) count++
            }
        return count
    }

    /** Y-coordinate of the bottom-most non-background pixel; useful for spotting underlines. */
    private static int bottomOfPaintedArea(BufferedImage img) {
        int bottom = 0
        for (int y = 0; y < img.getHeight(); y++)
            for (int x = 0; x < img.getWidth(); x++) {
                int rgb = img.getRGB(x, y)
                int r = (rgb >> 16) & 0xff
                int g = (rgb >> 8)  & 0xff
                int b =  rgb        & 0xff
                if (r < 200 || g < 200 || b < 200)
                    if (y > bottom) bottom = y
            }
        return bottom
    }
}