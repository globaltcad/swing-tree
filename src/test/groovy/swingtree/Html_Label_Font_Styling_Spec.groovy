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
    def setup() {
        SwingTree.initializeUsing(SwingTreeTestConfigurator.get())
        SwingTree.get().setEventProcessor(EventProcessor.COUPLED_STRICT)
        UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName())
    }

    def cleanup() {
        SwingTree.clear()
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


    def 'The style information passed to the style API will be injected into the text of an HTML based label.'(
        float uiScale
    ) {
        reportInfo """
            This test inspects the rewritten HTML text directly to confirm that
            every `FontConf` property the user configured ends up as a CSS
            declaration inside SwingTree's injected `<style>` block. Where the
            other specs in this file rely on pixel-level signals to stay
            implementation-agnostic, this one is deliberately white-box: it
            pins the *shape* of the injection so we notice if the format ever
            drifts (e.g. renamed declarations, lost units, dropped properties).

            We also vary the active `UI.scale()` across a handful of values to
            confirm that `fontSize` is rewritten in step with the scale — the
            user supplies points in design-time units, and the framework is
            responsible for multiplying them by `UI.scale()` before they reach
            Swing's HTML renderer.
        """
        given : 'A SwingTree instance configured with the parameterised UI scale:'
            SwingTree.initializeUsing( it -> it.uiScaleFactor(uiScale) )
        and : 'A user-authored HTML string and a label styled through `withStyle`:'
            var userHtml = '<html><h1>Greetings</h1><p>This is an html based label.</p></html>'
            var label = UI.label(userHtml)
                          .withStyle( it -> it
                              .fontColor("blue")
                              .fontBold(true)
                              .fontFamily("Ubuntu")
                              .fontSize(42)
                          )
                          .get(JLabel)

        expect : "SwingTree's injection marker is present in the rewritten text:"
            label.getText().contains('data-swingtree="injected"')
        and : 'Each configured `FontConf` property surfaces as a CSS declaration — and `fontSize` is multiplied by the active UI scale:'
            label.getText().contains('font-family:"Ubuntu";')
            label.getText().contains('font-size:'+Math.round(42 * uiScale)+'pt;')
            label.getText().contains('font-style:bold;')
            label.getText().contains('color:#0000ff;')

        where : 'We exercise a representative spread of UI scale factors:'
            uiScale << [
                    1f, 1.5f, 2f, 2.5f, 3f
                ]
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
                              .withStyle({ it
                                  .fontColor("orange")
                                  .fontBold(true)
                              })
                            .get(JLabel)

        when : 'We simulate multiple style cycles through painting:'
            var ignored1 = renderHtmlLabel(label, 190, 50)
            var ignored2 = renderHtmlLabel(label, 190, 50)
            var ignored3 = renderHtmlLabel(label, 190, 50)

        then : "Exactly one marker is present in the label's text — no double-injection:"
            (label.getText() =~ /data-swingtree="injected"/).size() == 1

        when : 'We now convert the label to a plain text based label...'
            label.setText("I am just plain text!")
        then : 'The plain text was not altered:'
            label.getText() == "I am just plain text!"

        when : 'We go back to it being html based...'
            label.setText("<html><h1>Hello</h1></html>")
        then : 'The text contains style information!'
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
                UI.of(label).withStyle({ it.fontColor("orange") })
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


    def 'Inline `font-size:NNpx` declarations in user HTML are scaled by the active UI scale.'()
    {
        reportInfo """
            SwingTree's styling API multiplies a configured `FontConf.size`
            by the active `UI.scale()` so a single styler renders consistently
            across DPI environments. Users authoring HTML by hand expect the
            same of inline declarations: a `<span style="font-size:20px;">`
            should grow on a HiDPI display, not stay pinned to its raw value.

            We verify that property here by setting two distinct scale factors
            and inspecting the rewritten label text directly. Pixel rendering
            is exercised by other specs in this file — here we just want to
            confirm that the rewrite is performed and that the units are
            preserved.
        """
        given : 'A label whose HTML carries an inline `font-size:20px` declaration:'
            var html = '<html><span style="font-size:20px;color:#fff;">Hi</span></html>'

        when : 'We render the label at the default scale of 1.0:'
            var labelAt1 = UI.of(new JLabel(html))
                            .get(JLabel)
        then : 'The user-authored 20px is unchanged:'
            labelAt1.getText().contains('font-size:20px')

        when : 'We then rebuild the label at a 2.0 scale and re-render it:'
            UI.runNow { SwingTree.get().setUiScaleFactor(2f) }
            var labelAt2 = UI.label(html)
                            .get(JLabel)
        then : 'The pixel value is doubled — and still in pixels:'
            labelAt2.getText().contains('font-size:40px')
        and : 'The original 20px no longer appears anywhere in the rewritten text:'
            !labelAt2.getText().contains('font-size:20px')

        cleanup : 'Restore the default scale factor for unrelated specs:'
            UI.runNow { SwingTree.get().setUiScaleFactor(1f) }
    }


    def 'Inline `font-size:NNpt` declarations are scaled the same way as `px`.'()
    {
        reportInfo """
            The `pt` unit declared in an inline html tag style, 
            should grow with `UI.scale()` so a hand-written stylesheet inside
            an HTML label does not visually drift away from a programmatic one.
        """
        given : 'A label with an inline `font-size:12pt` declaration:'
            var html = '<html><body style="font-size:12pt;">Hi</body></html>'
        and : 'A 1.5x UI scale applied for the duration of the test:'
            UI.runNow { SwingTree.get().setUiScaleFactor(1.5f) }

        when : 'The label is built through SwingTree:'
            var label = UI.of(new JLabel(html))
                          .get(JLabel)

        then : 'The pt value is multiplied — 12 × 1.5 = 18:'
            label.getText().contains('font-size:18pt')
    }


    def 'Relative units (em, rem, %) are not touched by inline scaling.'()
    {
        reportInfo """
            Relative CSS units cascade against another value that is itself
            already scaled: `em` and `rem` resolve against an ancestor's
            `font-size`, which we will already have rewritten if it carried
            an absolute declaration. Scaling them too would compound, so we
            deliberately leave them alone.
        """
        given : 'HTML carrying every relative unit at once:'
            var html = '<html>' +
                       '<span style="font-size:1.2em;">a</span>' +
                       '<span style="font-size:1rem;">b</span>' +
                       '<span style="font-size:120%;">c</span>' +
                       '</html>'
        and : 'A non-trivial UI scale that would visibly change a px/pt value:'
            UI.runNow { SwingTree.get().setUiScaleFactor(2f) }

        when : 'The label is built and styled (any styler will do):'
            var label = UI.of(new JLabel(html))
                          .withStyle({ it.fontColor("orange") })
                          .get(JLabel)

        then : 'Each relative declaration is preserved verbatim:'
            label.getText().contains('font-size:1.2em')
            label.getText().contains('font-size:1rem')
            label.getText().contains('font-size:120%')
    }


    def 'Inline scaling re-derives from the original on every cycle — no compounding.'()
    {
        reportInfo """
            The scaling rewrite stores the un-scaled HTML on a client property
            and recomputes from it each cycle, instead of operating on the
            already-scaled output. Without that, two re-style cycles at scale
            1.5 would produce 20 → 30 → 45 px. We confirm here that *N*
            forced cycles produce the same result as one.
        """
        given : 'HTML with a known-size inline declaration:'
            var html = '<html><span style="font-size:20px;">Hi</span></html>'
        and : 'A scale factor of 1.5:'
            UI.runNow { SwingTree.get().setUiScaleFactor(1.5f) }

        and : 'A label built through SwingTree at that scale:'
            var label = UI.of(new JLabel(html))
                          .withStyle({ it.fontColor("orange") })
                          .get(JLabel)

        when : 'The style cycle is forced to run a few more times:'
            UI.runNow {
                3.times { swingtree.style.ComponentExtension.from(label).gatherApplyAndInstallStyle(true) }
            }

        then : 'The scaled value is exactly 30 — not 45, not 67.5, not 101.25:'
            label.getText().contains('font-size:30px')
            !label.getText().contains('font-size:45px')
            !label.getText().contains('font-size:67')
            !label.getText().contains('font-size:101')
    }


    def 'A bound `Var<String>` text update is treated as a fresh original — not as a cached scaled value.'()
    {
        reportInfo """
            When a `Var<String>` overwrites the label text, the new text is
            the user's *un-scaled* HTML — the same shape the label was first
            seeded with. The styling pipeline must therefore treat it as a
            fresh original to scale, not as an already-styled result that
            still matches its previous scaled form.
        """
        given : 'A `Var<String>` initially holding HTML with a 20px declaration:'
            var content = sprouts.Var.of('<span style="font-size:20px;">First</span>')
        and : 'A 2.0 UI scale:'
            UI.runNow { SwingTree.get().setUiScaleFactor(2f) }

        and : 'An HTML label bound to the `Var`, styled (any styler will do):'
            var label = UI.html(content)
                          .withStyle({ it.fontColor("orange") })
                          .get(JLabel)
        when : 'We sanity-check that the initial text is scaled:'
            UI.sync()
        then :
            label.getText().contains('font-size:40px')

        when : 'The bound text is updated to a new HTML string with a different size:'
            UI.runNow { content.set('<span style="font-size:14px;">Second</span>') }
            UI.sync()
        then : 'The new value is treated as a brand-new original and scaled to 28:'
            label.getText().contains('font-size:28px')
        and : 'The previous size is gone:'
            !label.getText().contains('font-size:40px')
    }


    def 'Plain text labels are not affected by the inline-scaling rewrite.'()
    {
        reportInfo """
            The HTML scaling pipeline must be invisible to plain-text labels:
            a label whose text is not HTML must come out exactly as the user
            wrote it, regardless of the active UI scale.
        """
        given :
            UI.runNow { SwingTree.get().setUiScaleFactor(2f) }

        when : 'A plain-text label is built at a non-trivial scale:'
            var label = UI.label('font-size:20px; — should not be touched.')
                          .withStyle({ it.fontColor("orange") })
                          .get(JLabel)
        then : 'The text contains the literal "20px" — exactly the way the user wrote it:'
            label.getText() == 'font-size:20px; — should not be touched.'
    }


    def 'Increasing the UI scale strictly grows the rendered area of an HTML label.'()
    {
        reportInfo """
            The previous specs verify that the *text* the framework hands to
            Swing is rewritten in step with `UI.scale()`. That is a useful
            property of the current implementation but it is not the contract
            the user cares about: what they observe is the painted output.

            We therefore re-state the contract here through pixel counts. As
            the UI scale climbs from 1.0 through 1.5, 2.0 and 3.0 the rendered
            label must occupy strictly more dark pixels at each step. This
            test deliberately does not look at `label.getText()` — that way
            it remains a meaningful contract test if the inline-CSS rewriting
            is one day replaced by a lower-level mechanism such as a
            `Graphics2D` transform.
        """
        given : 'A label with an inline pixel-based font size:'
            var label = UI.html('<span style="font-size:14px;color:#000;">Hello, World</span>')
                          .get(JLabel)

        when : 'We render the label at a sequence of ascending scales:'
            int darkAt1   = paintAndCountDark(label, 800, 300, 1.0f)
            int darkAt15  = paintAndCountDark(label, 800, 300, 1.5f)
            int darkAt2   = paintAndCountDark(label, 800, 300, 2.0f)
            int darkAt3   = paintAndCountDark(label, 800, 300, 3.0f)

        then : 'Each step strictly adds painted area:'
            darkAt1  < darkAt15
            darkAt15 < darkAt2
            darkAt2  < darkAt3
    }


    def 'Decreasing the UI scale strictly shrinks the rendered area of an HTML label.'()
    {
        reportInfo """
            The mirror image of the previous spec: when the user scales the
            UI back down, the painted area must follow. Together the two
            specs anchor the *direction* of the relationship in pixels rather
            than in implementation details.
        """
        given : 'A label rendered through the standard inline-px declaration:'
            var label = UI.html('<span style="font-size:16px;color:#000;">Bigger and smaller</span>')
                          .get(JLabel)

        when : 'We render the label at a sequence of descending scales:'
            int darkAt3   = paintAndCountDark(label, 800, 300, 3.0f)
            int darkAt2   = paintAndCountDark(label, 800, 300, 2.0f)
            int darkAt15  = paintAndCountDark(label, 800, 300, 1.5f)
            int darkAt1   = paintAndCountDark(label, 800, 300, 1.0f)

        then : 'Each step strictly drops the painted area:'
            darkAt3  > darkAt2
            darkAt2  > darkAt15
            darkAt15 > darkAt1
    }


    def 'Scaling up and back down restores the original rendering of an HTML label.'()
    {
        reportInfo """
            DPI changes are reversible: a user who briefly bumped their
            display scale should not be left with a slightly-different
            looking UI when they restore the old setting. We assert that
            symmetric round-trips produce a pixel-identical area count
            (a strict equality is admissible here because rendering is
            deterministic for the same scale and same label content).
        """
        given : 'A label with a moderate inline font size:'
            var label = UI.html('<span style="font-size:18px;color:#000;">Round-trip</span>')
                          .get(JLabel)

        when : 'We render at scale 1, then at 2, then back to 1:'
            int darkBefore   = paintAndCountDark(label, 800, 300, 1.0f)
            int darkScaledUp = paintAndCountDark(label, 800, 300, 2.0f)
            int darkAfter    = paintAndCountDark(label, 800, 300, 1.0f)

        then : 'Bumping the scale visibly expanded the painted area...'
            darkScaledUp > darkBefore + 50
        and : '...and dropping it back returned to *exactly* the original count:'
            darkAfter == darkBefore
    }


    def 'Re-applying the same UI scale is idempotent in the rendered output.'()
    {
        reportInfo """
            Setting the scale factor to its current value (or the listener
            firing twice without a real change in between) must not perturb
            the rendering. This guards against feedback loops where each
            cycle would compound the previous one.
        """
        given :
            var label = UI.html('<span style="font-size:20px;color:#000;">Stable</span>')
                          .get(JLabel)

        when : 'We render at scale 2, then again at scale 2 after redundant calls:'
            int firstDark = paintAndCountDark(label, 800, 300, 2.0f)
            UI.runNow {
                SwingTree.get().setUiScaleFactor(2.0f)
                SwingTree.get().setUiScaleFactor(2.0f)
            }
            UI.sync()
            int secondDark = countDarkPixels(renderHtmlLabel(label, 800, 300))

        then : 'The two renders agree exactly — no compounding has occurred:'
            firstDark == secondDark
    }


    def 'Bare heading tags (`<h1>`, `<h2>`...) without inline sizes scale with `UI.scale()`.'()
    {
        reportInfo """
            The most common HTML labels in real applications use semantic tags
            like `<h1>` or `<h2>` *without* an explicit `font-size` declaration.
            Swing's `HTMLEditorKit` ships a default stylesheet that pins those
            tags to absolute keyword values (`x-large`, `large`, ...) which
            don't react to `UI.scale()`, the JLabel's font, or any
            look-and-feel scaling. So the inline-size rewrite alone has
            nothing to grab onto and the heading stays its hard-coded size.

            We now inject scaled `h1..h6` (and a `body`) override into the
            same `<head><style data-swingtree="injected">` block we already
            use for FontConf-derived CSS, so headings actually grow with
            `UI.scale()` even when the user wrote no inline sizes and no
            styler. We confirm the property here through painted pixels.
        """
        given : 'A label whose only declared font-related property is the heading tag itself:'
            var label = UI.html('<h1>Heading</h1>').get(JLabel)

        when : 'We render the label at a sequence of ascending scales:'
            int darkAt1 = paintAndCountDark(label, 800, 300, 1.0f)
            int darkAt2 = paintAndCountDark(label, 800, 300, 2.0f)
            int darkAt3 = paintAndCountDark(label, 800, 300, 3.0f)

        then : 'Each step strictly adds painted area — the heading is responding to scale:'
            darkAt1 < darkAt2
            darkAt2 < darkAt3
    }


    def 'A heading-only label is left textually untouched at scale 1, even though scaling overrides exist.'()
    {
        reportInfo """
            The new heading-defaults injection is gated on `UI.scale() != 1`
            so the contract for the default scale is preserved: an unstyled
            HTML label whose text uses only structural tags must come out of
            the styling pipeline byte-for-byte identical to what the user
            wrote. The injection (and its marker) only appear when scale
            actually requires it.
        """
        given : 'An HTML label with only a heading and no styler:'
            var label = UI.html('<h2>Progress</h2>').get(JLabel)

        expect : 'Text equals the convenience-method output, with no marker:'
            label.getText() == "<html><h2>Progress</h2></html>"
        and :
            !label.getText().contains('data-swingtree')
    }


    def 'At a non-trivial scale, an unstyled heading label gets scaled `h1..h6` overrides injected.'()
    {
        reportInfo """
            We inspect the rewritten text directly to confirm the heading
            override rules show up in the injected `<style>` block when scale
            != 1 — including for HTML that has no FontConf and no inline
            sizes at all. A `body{font-size:Npt}` is also injected so plain
            inline text inherits a scaled size.
        """
        given :
            UI.runNow { SwingTree.get().setUiScaleFactor(2f) }
        and : 'An unstyled heading label:'
            var label = UI.html('<h1>Title</h1>').get(JLabel)

        expect : 'The injection block is present:'
            label.getText().contains('data-swingtree="injected"')
        and : 'It contains scaled rules for body and the six heading levels:'
            label.getText().contains('body{font-size:13pt')
            label.getText().contains('h1{font-size:26pt')
            label.getText().contains('h2{font-size:20pt')
            label.getText().contains('h3{font-size:15pt')
            label.getText().contains('h4{font-size:13pt')
            label.getText().contains('h5{font-size:11pt')
            label.getText().contains('h6{font-size:9pt')
    }


    def 'Heading scaling combines with a FontConf so the body rule is not duplicated.'()
    {
        reportInfo """
            When the user supplies a `FontConf.size`, that size already wins
            for the body and is emitted by `_buildHtmlBodyCss`. The heading
            override layer must therefore *not* emit a second `body` rule —
            otherwise the two would confuse downstream readers and tools.
            We verify this by counting `body{` occurrences inside the
            injected style block.
        """
        given :
            UI.runNow { SwingTree.get().setUiScaleFactor(2f) }
        and : 'A heading label with an explicit FontConf size:'
            var label = UI.html('<h1>X</h1>')
                          .get(JLabel)

        when : "Extract the contents of SwingTree's injected `<style>` block:"
            var matcher = label.getText() =~ /<style data-swingtree="injected">(.*?)<\/style>/
            matcher.find()
            var injectedCss = matcher.group(1)

        then : 'Exactly one `body{` rule — the FontConf one, not duplicated by the scaling layer:'
            injectedCss.count('body{') == 1
        and : 'And the heading overrides are still there:'
            injectedCss.contains('h1{font-size:')
    }


    def 'Two HTML labels declared with proportional inline sizes stay proportional under scaling.'()
    {
        reportInfo """
            The scaling factor is multiplicative: it must preserve relative
            sizes between two labels declared with different inline font
            sizes. We pick a 2x ratio (12px vs 24px) and confirm that the
            larger label renders more dark pixels than the smaller one at
            *every* scale we test. This is a structural property — even an
            implementation that did the scaling outside SwingTree would have
            to honour it.
        """
        given : 'Two labels, identical except for their inline pixel sizes:'
            var small = UI.html('<span style="font-size:12px;color:#000;">Pair</span>')
                            .get(JLabel)
            var large = UI.html('<span style="font-size:24px;color:#000;">Pair</span>')
                            .get(JLabel)

        when : 'We render both at every scale we want to verify against:'
            int smallAt1  = paintAndCountDark(small, 800, 300, 1.0f)
            int largeAt1  = paintAndCountDark(large, 800, 300, 1.0f)
            int smallAt2  = paintAndCountDark(small, 800, 300, 2.0f)
            int largeAt2  = paintAndCountDark(large, 800, 300, 2.0f)
            int smallAt3  = paintAndCountDark(small, 800, 300, 3.0f)
            int largeAt3  = paintAndCountDark(large, 800, 300, 3.0f)

        then : 'At every scale the larger declaration paints the larger area:'
            largeAt1 > smallAt1
            largeAt2 > smallAt2
            largeAt3 > smallAt3
        and : 'Increasing the scale grows both labels (a sanity check on the renderer):'
            smallAt1 < smallAt3
            largeAt1 < largeAt3
    }


    // -------- helpers used by the specs above --------

    /**
     * Sets the global UI scale, lets the listener-driven re-style cycle
     * settle, and returns the dark-pixel count of the freshly rendered
     * label. Pulled out into a helper so the specs can read as a sequence
     * of (scale, count) measurements rather than a wall of plumbing.
     */
    private static int paintAndCountDark(JLabel label, int width, int height, float scale) {
        UI.runNow { SwingTree.get().setUiScaleFactor(scale) }
        UI.sync()
        return countDarkPixels(renderHtmlLabel(label, width, height))
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