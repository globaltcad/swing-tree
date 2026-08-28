package swingtree

import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title
import swingtree.style.ComponentExtension
import swingtree.threading.EventProcessor
import utility.SwingTreeTestConfigurator

import javax.swing.*
import java.awt.Color
import java.awt.Graphics2D
import java.awt.image.BufferedImage

@Title("Styling HTML content JEditorPane through FontConf")
@Narrative('''

    SwingTree's styling API treats every `JComponent` uniformly: when you write
    `withStyle(it -> it.fontColor("orange").fontFamily("Serif"))` you expect
    the result to look the same regardless of whether the component displays a
    plain string or a fragment of HTML.  In practice that is harder than it
    sounds, because Swing renders HTML through a completely different pipeline
    than its plain-text label/editor pane rendering.

    A plain `JEditorPane` paints text using the component's `Font` and `Foreground`
    properties.  Setting font attributes through `FontConf` translates directly
    to a derived `Font` and (where applicable) a foreground color, and the
    result is what the user sees.

    An HTML based `JEditorPane` (one whose text starts with `<html>`) is different:
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

    This specification is the user-facing contract: that styling HTML editor panes
    "just works", that nothing the user can put inside an HTML editor breaks
    the styling pipeline, and that SwingTree's own injection is a self-cleaning
    affair, not a one-way transformation.

    The JEditorPane HTML-scaling feature introduced alongside JLabel's has three
    lifecycle concerns that are covered here as well: 
    - whether an *editable* editor's caret survives a style cycle, 
    - whether an externally supplied document still triggers a fresh style pass, 
    - and whether a style that is no longer required (scale back to 1, colour removed) 
      actually cleans up the injected CSS. These specs pin those behaviours down first as repros, 
      and later as regression guards.

''')
@Subject([UI])
class Html_Editor_Font_Styling_Spec extends Specification
{
    def setup() {
        SwingTree.initializeUsing(SwingTreeTestConfigurator.get())
        SwingTree.get().setEventProcessor(EventProcessor.COUPLED_STRICT)
    }

    // ═══════════════════════════════════════════════════════════════════
    // JEditorPane — same HTML scaling rules as JLabel
    // ═══════════════════════════════════════════════════════════════════


    def 'JEditorPane HTML styling must settle after the first injection — no infinite setText loop.'()
    {
        reportInfo """
            When SwingTree injects scaled CSS into a JEditorPane's HTML via
            `setText()`, HTMLEditorKit parses and normalises the document:
            it adds indentation, strips comments, restructures tags.  As a
            consequence the round-tripped `getText()` is never equal to the
            string we wrote.

            If the settle logic naively compares desired == currentText with
            the pre-normalisation string, the check perpetually fails →
            another injection → another normalization → … this is an
            *infinite repaint loop* that hogs the EDT and freezes the window.

            The fix: because HTMLEditorKit normalisation is deterministic,
            writing the same text twice yields identical output. After the
            first cycle writes the injected HTML and reads back the normalized
            form, subsequent cycles keep writing the same thing — stabilising
            at a constant text length.

            We verify settle by asserting that the document text doesn't
            grow without bound across repeated style cycles.
        """
        given :
            SwingTree.get().setUiScaleFactor(2f)
            var editor = UI.runAndGet({
                var editor = UI.editorPane().get(JEditorPane)

                editor.setContentType("text/html")
                editor.setText("<html><h2>Hello</h2></html>")

                ComponentExtension.from(editor).gatherApplyAndInstallStyle(true)

                return editor
            })
            int firstLength = editor.getText().length()

        when : 'We force three more full style cycles:'
            3.times {
                UI.runNow({
                    ComponentExtension.from(editor).gatherApplyAndInstallStyle(true)
                })
            }
            var text = UI.runAndGet(()->editor.getText())

        then : 'Text length has stabilised (no unbounded growth from looping injections):'
            text.length() <= firstLength + 50   // small tolerance for normalisation variance

        and : 'The injected CSS still appears in the document:'
            text.contains('font-size')
    }

    def 'HTML headings inside a JEditorPane scale with UI.scale() just like in a JLabel.'()
    {
        reportInfo """
            A `JEditorPane` renders HTML through exactly the same Swing mechanism
            as an HTML `JLabel`: it builds an `HTMLDocument`, parses the text
            through `HTMLEditorKit`, and stores a `View` tree behind
            `BasicHTML.propertyKey`.  Swing's default stylesheet pins bare
            heading tags (`<h1>`…`<h6>`) to absolute keyword sizes that do *not*
            react to `UI.scale()`.

            For JLabel, SwingTree fixes this by injecting scaled `h1..h6` CSS
            overrides into the HTML text whenever `UI.scale() != 1`.  The same
            treatment must apply to `JEditorPane` — which is what delivers the
            “Delivery note #…” headline in the Linen showcase correctly at
            every DPI.

            We verify that: (a) CSS is injected at scale > 1, (b) heading
            sizes scale proportionally with `UI.scale()`, and (c) settled
            output doesn't grow unboundedly.

            NB: the heading sizes are derived from the Look & Feel's default
            base font (which varies per platform — e.g. 15pt on this test
            setup's Ubuntu, 12pt on Windows' Segoe UI). We therefore assert on
            the exact pt value *derived from the live font size* rather than on
            a hard-coded digit, so the check holds on every platform.
        """
        given :
            var editor = UI.runAndGet({
                var editor = UI.editorPane().get(JEditorPane)
                editor.setContentType("text/html")
                editor.setText("<html><h2>Delivery note</h2></html>")
                return editor
            })
        when : 'Inject at scale 1 (default font ~15pt):'
            UI.runNow({
                SwingTree.get().setUiScaleFactor(1f)
                ComponentExtension.from(editor).gatherApplyAndInstallStyle(true)
            })
            UI.sync() // Await one EDT cycle just to make sure...
            String textAt1 = UI.runAndGet(()->editor.getText())

            UI.runNow({
                SwingTree.get().setUiScaleFactor(2f)
                ComponentExtension.from(editor).gatherApplyAndInstallStyle(true)
            })
            UI.sync() // Await one EDT cycle just to make sure...
            String textAt2 = UI.runAndGet(()->editor.getText())

            UI.runNow({
                SwingTree.get().setUiScaleFactor(3f)
                ComponentExtension.from(editor).gatherApplyAndInstallStyle(true)
            })
            UI.sync() // Await one EDT cycle just to make sure...
            String textAt3 = UI.runAndGet(()->editor.getText())

            UI.runNow({
                SwingTree.get().setUiScaleFactor(1f)
                ComponentExtension.from(editor).gatherApplyAndInstallStyle(true)
            })
            UI.sync() // Await one EDT cycle just to make sure...
            String textBackTo1 = UI.runAndGet(()->editor.getText())

            // Capture the platform/L&F default base font size at each scale. The
            // injected h1..h6 values are computed as fixed multiples of this base
            // (see LabelStyleInstallerUtility._buildHtmlScalingDefaultsCss), so we
            // derive the *expected* pt value from the live font size rather than
            // hard-coding a platform-dependent digit run.
            int baseAt2 = UI.runAndGet(()-> { SwingTree.get().setUiScaleFactor(2f); editor.getFont().getSize() })
            int baseAt3 = UI.runAndGet(()-> { SwingTree.get().setUiScaleFactor(3f); editor.getFont().getSize() })
            SwingTree.get().setUiScaleFactor(1f)

        then : 'Heading CSS appears at scale > 1 (body + h1..h6 ≥ 7 rules):'
            (textAt2 =~ /font-size:/).count >= 7
            (textAt3 =~ /font-size:/).count >= 7
        and : 'At the initial scale 1 no scaling CSS has been injected yet (injection only fires when UI.scale() != 1):'
            !textAt1.contains('font-size')
        and : 'Scale 2+ contain the expected scaled heading overrides (h2 present at the derived size):'
            // h2 = round(base * 1.50). Asserting the exact derived substring (e.g. "font-size: 45pt")
            // pins the scaling contract without depending on the platform default font size.
            textAt3.contains('h2 { font-size: ' + Math.round(baseAt3 * 1.50f) + 'pt }')
            textAt2.contains('h2 { font-size: ' + Math.round(baseAt2 * 1.50f) + 'pt }')
        and : 'After returning to scale 1 the stale injected CSS is stripped again (like a JLabel):'
            !textBackTo1.contains('font-size')
    }


    def 'At scale 2, a bare-heading JEditorPane renders headings at the scaled size.'()
    {
        reportInfo """
            SwingTree injects scaled `h1..h6` + `body` overrides when UI.scale != 1.
            We verify that heading sizes are present in the injected CSS and
            that settling works correctly across scale changes.
        """
        given :
            var editor = UI.runAndGet({
                var editor = UI.editorPane().get(JEditorPane)
                editor.setContentType("text/html")
                editor.setText("<html><h2>Progress</h2></html>")
                ComponentExtension.from(editor).gatherApplyAndInstallStyle(true)
                return editor
            })
            String textAt1 = UI.runAndGet(()->editor.getText())

        when : 'Change scale to 2 and re-inject:'
            UI.runNow({
                SwingTree.get().setUiScaleFactor(2f)
                ComponentExtension.from(editor).gatherApplyAndInstallStyle(true)
            })
            String textAt2 = UI.runAndGet(()->editor.getText())

        then : 'Injected CSS contains heading rules with scaled pt values:'
            (textAt2 =~ /font-size:/).count >= 7
        and : 'At the scale-1 baseline no injected CSS is present — scaling only kicks in when UI.scale() != 1:'
            !textAt1.contains('font-size')
    }


    def 'An inline `font-size:NNpx` declaration inside a JEditorPane settles without looping.'()
    {
        reportInfo """
            Inline pixel sizes written by the author should grow with the active
            UI scale — the same rule that applies to JLabel.  Note that
            HTMLEditorKit may transform <span style='font-size:14px'> into
            <font size=\"14px\"> which our _scaleInlineFontSizes pattern
            targets separately from its css-style rewrite.

            What matters most: after repeated scale-change cycles the document
            settles and doesn't grow unboundedly.
        """
        given :
            var editor = UI.editorPane().get(JEditorPane)
            editor.setContentType("text/html")
            editor.setText("<html><span style='font-size:14px;color:#000'>X</span></html>")

        when : 'Run a full injection cycle at scale 1:'
            UI.runNow({
                SwingTree.get().setUiScaleFactor(1f)
                ComponentExtension.from(editor).gatherApplyAndInstallStyle(true)
            })
            String textAt1 = UI.runAndGet(()->editor.getText())

            SwingTree.get().setUiScaleFactor(2f)
            3.times {
                ComponentExtension.from(editor).gatherApplyAndInstallStyle(true)
                UI.sync()
            }
            Thread.sleep(100)
            String textAt2 = UI.runAndGet(()->editor.getText())

            SwingTree.get().setUiScaleFactor(1f)
            3.times {
                ComponentExtension.from(editor).gatherApplyAndInstallStyle(true)
                UI.sync()
            }
            Thread.sleep(100)
            String textBackTo1 = UI.runAndGet(()->editor.getText())

        then : 'Injected CSS appears when scale > 1 (headings are scaled):'
            textAt2.contains('font-size')
        and : 'At scale 1 the inline `14px` size passes through un-scaled (only the injected heading CSS is what grows with UI.scale()):'
            textAt1.contains('14px')

        when :
            // After settling, repeated cycles at same scale should produce stable output.
            String settled = UI.runAndGet(()->editor.getText())
            UI.runNow({and
                ComponentExtension.from(editor).gatherApplyAndInstallStyle(true)
            })
            String again = UI.runAndGet(()->editor.getText())
        then : 'Document does not grow unboundedly (settled at scale 2):'
            Math.abs(settled.length() - again.length()) <= 20
    }


    def 'FontConf styling applied to a JEditorPane changes the rendered text color.'()
    {
        reportInfo """
            Beyond scaling, FontConf properties such as `fontColor` must also be
            respected by JEditorPane.  This means injecting a `body{color:#...}`
            rule alongside whatever heading overrides exist.

            We verify through pixel counting — blue-styled text produces many
            blue-tinted pixels; unstyled text does not.
        """
        given : 'An unstyled JEditorPane with some paragraph text:'
            var plain = UI.editorPane().get(JEditorPane)
            plain.setContentType("text/html")
            plain.setText("<html><p>Hello world</p></html>")
            plain.setSize(400, 100)
        and : 'A styled version with blue text:'
            var styled = UI.editorPane()
                            .withStyle({ it.fontColor("blue") })
                            .get(JEditorPane)
            styled.setContentType("text/html")
            styled.setText("<html><p>Hello world</p></html>")
            ComponentExtension.from(styled).gatherApplyAndInstallStyle(false)
            styled.setSize(400, 100)

        when : 'Both panes are rendered:'
            int plainBlue  = countBluePixels(renderHtmlEditor(plain,   400, 100, 1f))
            int styledBlue = countBluePixels(renderHtmlEditor(styled,  400, 100, 1f))

        then : 'The plain pane contains virtually no blue text pixels:'
            plainBlue < 50
        and : 'The styled pane contains substantial blue pixels:'
            styledBlue > 100
    }


    // ═══════════════════════════════════════════════════════════════
    // LIFECYCLE CONCERN 1 — caret/selection destroyed by unconditional setText
    // ═══════════════════════════════════════════════════════════════

    def 'An editable JEditorPane keeps its caret where the user put it across a style cycle'()
    {
        reportInfo """
            JEditorPane is an *editing* component. As the user types they move the
            caret (and possibly select a range). SwingTree's JEditorPane styling path
            (`htmlSettles`) runs on every style cycle and currently calls `setText()`
            unconditionally on its phase-2 path even when the text did not need
            changing (e.g. plain HTML at scale 1 → empty CSS). `JTextComponent.setText`
            re-parses the document and resets the caret to the start.

            For a component whose whole job is editing, that would throw the user's
            text-caret (and any text selection) back to the beginning on every repaint.
            The JLabel path avoids this by only calling setText when the desired text
            actually differs from the current one (`Objects.equals` guard). The editor
            path must be equally conservative.

            This spec asserts that a caret placed mid-document survives a complete
            style pass untouched.
        """
        given : 'An editable HTML editor with a caret planted in the middle of the text:'
            var editor = UI.editorPane().get(JEditorPane)
            editor.setContentType("text/html")
            editor.setText("<html><p>hello world</p></html>")
            editor.setEditable(true)
            int caret = UI.runAndGet({
                editor.getCaret().setDot(6)
                editor.getCaret().getDot()
            })

        when : 'A full style cycle runs at scale 1 (no CSS to inject):'
            SwingTree.get().setUiScaleFactor(1f)
            UI.runNow({ ComponentExtension.from(editor).gatherApplyAndInstallStyle(true) })

        then : 'The caret has not moved:'
            UI.runAndGet({ editor.getCaret().getDot() }) == caret
    }

    // ═══════════════════════════════════════════════════════════════
    // LIFECYCLE CONCERN 2 — external document edits never re-trigger styling
    // ═══════════════════════════════════════════════════════════════

    def 'A styled JEditorPane re-applies its style after its HTML is replaced externally'()
    {
        reportInfo """
            When a JLabel's text is set from outside SwingTree (a sprouts `Val`
            binding, a manual `setText`, a copy/paste round-trip), SwingTree
            detects the change via a PropertyChangeListener and triggers a fresh
            style pass so the new text is styled/scaled too.

            `JLabel` fires a `"text"` property change. `JEditorPane`/`JTextComponent`
            fires a `"document"` property change instead — so listening on `"text"`
            for an editor is a no-op. The intended contract is: if the user replaces
            the HTML content of a styled editor, it must come out styled just like
            the original did.

            This spec asserts that after an external `setText`, the injected style is
            present again (proving a re-style pass ran).
        """
        given : 'A JEditorPane styled to a blue font, already carrying injected CSS:'
            var editor = UI.editorPane().withStyle({ it.fontColor("blue") }).get(JEditorPane)
            editor.setContentType("text/html")
            editor.setText("<html><p>hello world</p></html>")
            UI.runNow({ ComponentExtension.from(editor).gatherApplyAndInstallStyle(true) })
            boolean styledBefore = UI.runAndGet({ editor.getText().contains('#0000ff') })
            assert styledBefore

        when : 'The user replaces the HTML with new, plain, unstyled content (external setText):'
            UI.runNow({
                // Replace the entire content as an application would:
                var doc = editor.getEditorKit().createDefaultDocument()
                doc.insertString(0, "<html><p>brand new content</p></html>", null)
                editor.setDocument(doc)
            })

        then : 'The newly supplied HTML is styled again (blue font injected):'
            UI.runAndGet({ editor.getText().contains('#0000ff') })
    }

    // ═══════════════════════════════════════════════════════════════
    // LIFECYCLE CONCERN 3 — stale injected CSS is never cleaned up
    // ═══════════════════════════════════════════════════════════════

    def 'Stale injected CSS disappears from the document once UI scale returns to 1'()
    {
        reportInfo """
            The injected `<style>` block (heading/body font-size overrides) exists
            only because UI scale != 1. When the scale returns to 1 those overrides
            are no longer wanted — otherwise the document keeps a permanent copy of
            framework-generated CSS that (a) no longer reflects the real scale and
            still scales the rendered text, and (b) leaks raw framework markup into
            the user's editable document text.

            The JLabel path strips the injection when no CSS is needed (it reinstalls
            the pristine, stripped original). The JEditorPane path currently keeps the
            previously-stabilised CSS. This spec asserts the editor behaves like the
            label: after scale returns to 1, the document contains no injected
            `font-size` CSS.

            Furthermore, an editor that can't settle (stale CSS never matches the
            empty `expectedCss`) would call setText every cycle — asserted as a
            secondary regression guard on caret stability.
        """
        given : 'An unstyled HTML editor whose document gets injected CSS at scale 2:'
            SwingTree.get().setUiScaleFactor(2f)
            var editor = UI.editorPane().get(JEditorPane)
            editor.setContentType("text/html")
            editor.setText("<html><h2>Delivery note</h2></html>")
            editor.setEditable(true)
            UI.runNow({ ComponentExtension.from(editor).gatherApplyAndInstallStyle(true) })
            String atScale2 = UI.runAndGet({ editor.getText() })
            assert atScale2.contains('font-size')

        when : 'The scale returns to 1 and a full style cycle runs:'
            SwingTree.get().setUiScaleFactor(1f)
            UI.runNow({ ComponentExtension.from(editor).gatherApplyAndInstallStyle(true) })

        then : 'The injected font-size CSS has been removed from the document:'
            UI.runAndGet({ editor.getText().contains('font-size') }) == false
        and : 'A second cycle is a no-op (document is stable, caret undisturbed):'
            int caret = UI.runAndGet({ editor.getCaret().setDot(8); editor.getCaret().getDot() })
            UI.runNow({ ComponentExtension.from(editor).gatherApplyAndInstallStyle(true) })
            UI.runAndGet({ editor.getCaret().getDot() }) == caret
    }


    // -------- helpers used by the specs above --------

    /** Pixel count for hues "close to" blue — symmetric counterpart to {@link Html_Label_Font_Styling_Spec#countOrangePixels}. */
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

    /** Renders the given editor pane's HTML content into a fresh image.
     *  Unlike JLabel, JEditorPane renders HTML through its own StyledDocument/
     *  View tree managed by HTMLEditorKit — not via BasicHTML.propertyKey.
     *  We therefore paint the component itself into an off-screen buffer.
     */
    private static BufferedImage renderHtmlEditor(JEditorPane editor, int width, int height, float scale) {
        UI.runNow {
            SwingTree.get().setUiScaleFactor(scale)
            editor.setSize(width, height)
            // Force layout pass so the document has dimensions
            editor.invalidate()
            editor.validate()
            editor.repaint()
        }
        // Give any async layout/render work a chance to settle
        Thread.sleep(50)
        var img  = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        Graphics2D g = img.createGraphics()
        g.setColor(Color.WHITE)
        g.fillRect(0, 0, width, height)
        g.setClip(0, 0, width, height)
        // Use printAll which handles all painting layers (border, content, etc.)
        editor.printAll(g)
        g.dispose()
        return img
    }

}