package swingtree.style;

import org.jspecify.annotations.Nullable;
import swingtree.UI;

import javax.swing.*;
import javax.swing.plaf.basic.BasicHTML;
import javax.swing.text.html.HTMLEditorKit;
import java.awt.*;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
    We achieve HTML-label styling and UI scaling by injecting a <head><style>...</style></head>
    block into the label's HTML text.
    Why text rewriting instead of something more
    surgical (modifying the HTMLDocument's StyleSheet, mutating element
    AttributeSets, ...)?

     - HTMLDocument resolves CSS at *parse time* into each element's AttributeSet.
       Stylesheet rules added afterwards do not propagate to existing elements
       (verified empirically: addRule, setCharacterAttributes via either
       StyleConstants.Foreground or CSS.Attribute.COLOR all leave rendering
       unchanged).
     - Setting only the renderer via BasicHTML.updateRenderer is also unreliable:
       BasicLabelUI's PropertyChangeHandler can re-derive the renderer from
       label.getText() on any "text"/"font"/"foreground" change, replacing ours
       with one built from the unstyled text.
     - Going through label.setText makes the styled HTML the source of truth, so
       subsequent re-derivations stay styled.

    So we need to modify the HTML directly:

     - To apply FontConf settings (colour, family, size, decoration) we splice
       a `<head><style data-swingtree="injected">...</style></head>` block in
       front of the user's body. HTMLDocument resolves CSS at *parse time*
       into each element's AttributeSet, so stylesheet rules added afterwards
       (addRule, setCharacterAttributes via either StyleConstants.Foreground
       or CSS.Attribute.COLOR) leave rendering unchanged. Setting only the
       renderer via BasicHTML.updateRenderer is also unreliable: BasicLabelUI's
       PropertyChangeHandler can re-derive the renderer from label.getText() on
       any "text"/"font"/"foreground" change. Going through label.setText makes
       the styled HTML the source of truth, so subsequent re-derivations stay
       styled.
     - To apply the {@link UI#scale()} factor to user-authored inline CSS we
       rewrite `font-size:NNpx|pt` in place. The scale factor is also applied
       to FontConf elsewhere in the pipeline, so user CSS and SwingTree CSS
       grow at the same rate.
     - For HTML that uses bare structural tags (`<h1>`, `<h2>`, ..., `<p>`)
       without inline sizes the inline rewrite has nothing to match, and
       `HTMLEditorKit`'s default stylesheet pins heading sizes to absolute
       keyword values that don't track UI.scale() at all. So when the scale
       differs from 1 we additionally inject a `body{font-size:Npt}` (derived
       from the JLabel's font when no FontConf size was supplied) and
       `h1..h6` overrides at HTML5-default multiples, so headings grow with
       the rest of the UI.

    Both transformations are reversible: the un-scaled, un-injected text is
    stashed on a client property so each cycle can recompute from scratch.
    Combined with a "last installed" snapshot, the property change listener
    can tell our own setText apart from external ones — and treat the latter
    as a fresh original.

    To make the rewrite robust we:
     - Anchor the splice on the literal 6-char "<html>" prefix that
       BasicHTML.isHTMLString already validated, preserving the user's case
       ("<HTML>" vs "<html>" etc.).
     - Wrap our injection in a unique marker so we can detect and strip our
       previous block on the next cycle, leaving the user's original text
       recoverable even if the client-property cache is missing (e.g. after
       a copy/paste that round-trips the text through a JComponent of a
       different type).
     - Quote and escape user-provided values (font-family) so a malicious or
       quirky string cannot break out of the CSS declaration.

    Known caveats from Swing's CSS implementation (not fixable here):
     - letter-spacing is not honored at all.
     - User-provided <style> blocks within the original HTML still apply, and
       because their source order is later than ours, they win on conflicts.
       That matches the spirit of "user's HTML overrides framework defaults".
     - We deliberately do not scale relative units (em, rem, %): they are
       computed against another value that is itself already scaled
       (the parent's font-size, which we will have rewritten if it carried
       a px/pt declaration). Scaling them too would compound.
*/
@SuppressWarnings("AlmostJavadoc") // The block comment above is an intentional implementation note, not Javadoc (it contains raw HTML tags).
final class LabelStyleInstallerUtility {

    private static final String _HTML_INJECTION_OPEN  = "<head><style data-swingtree=\"injected\">";
    private static final String _HTML_STYLE_BANNER   = "<!--swingtree-style-->";
    private static final String _HTML_INJECTION_CLOSE = "</style>" + _HTML_STYLE_BANNER + "</head>";
    private static final String _HTML_TEXT_LISTENER_KEY  = "swingtree.style.htmlTextListenerInstalled";
    private static final String _HTML_APPLYING_KEY       = "swingtree.style.htmlApplying";
    private static final String _HTML_LAST_INSTALLED_KEY = "swingtree.style.htmlLastInstalled";
    private static final String _HTML_ORIGINAL_KEY       = "swingtree.style.htmlOriginal";
    private static final int    _HTML_OPEN_TAG_LEN = 6; // "<html>" — guaranteed by BasicHTML.isHTMLString

    /*
        Match `font-size: NN(px|pt)` in any context — inline `style="..."`
        attributes, embedded `<style>` blocks, etc. Case-insensitive on the
        property name and unit. Numbers may carry a fractional part.
    */
    private static final Pattern _INLINE_FONT_SIZE_PATTERN = Pattern.compile(
        "(font-size\\s*:\\s*)(\\d+(?:\\.\\d+)?)\\s*(px|pt)",
        Pattern.CASE_INSENSITIVE
    );
    /*
        Regex pattern that extracts the inner CSS content from our injected
        style tag after HTMLEditorKit normalises it.

        HTMLEditorKit transforms:<br/>
          &lt;style data-swingtree="injected">body{...}</style><br/>
        Into:<br/>
          &lt;style type="text/css"&gt;&lt;!-- body{...} --&gt;&lt;/style&gt;<br/>

        We extract everything between <!-- and --> regardless of surrounding
        whitespace or newlines.
     */
    private static final Pattern SETTLE_MARKER_PATTERN = Pattern.compile(
            "<!--(.*?)-->",
            Pattern.DOTALL
    );

    private LabelStyleInstallerUtility() {}


    /*
        Ensures a (single) change listener is installed that reacts to externally
        supplied HTML content by forcing a fresh style pass.

        The lifecycle differs between component types:
          * JLabel fires a "text" PropertyChangeEvent whenever its text is set.
          * JEditorPane / JTextComponent does NOT fire "text". It fires a
            "document" PropertyChangeEvent when the whole document is replaced
            (e.g. via setDocument); setText goes through the kit's reader and
            fires no property event at all.

        Listening on "text" for an editor is therefore a silent no-op — externally
        replaced editor content would silently miss its re-style. We thus branch:
        labels keep the "text" listener (their own setText is recognised via the
        last-installed snapshot), editors listen on "document" (their own internal
        setText does not fire that event, so it cannot react to itself).
    */
    static void _ensureHtmlTextListenerInstalled(
        JComponent component
    ) {
        if ( component.getClientProperty(_HTML_TEXT_LISTENER_KEY) != null )
            return;

        component.putClientProperty(_HTML_TEXT_LISTENER_KEY, Boolean.TRUE);

        if ( component instanceof JEditorPane ) {
            JEditorPane editor = (JEditorPane) component;
            // JEditorPane content is delivered as a document model, not a text
            // bean property — a whole-document replacement surfaces as a
            // "document" PropertyChangeEvent.
            component.addPropertyChangeListener("document", evt -> {
                // Skip our own writes (bracketed by the re-entrancy marker in
                // _setStyledText) — otherwise setText would refire a styling
                // cycle from inside itself and read a half-swapped document.
                if ( Boolean.TRUE.equals(component.getClientProperty(_HTML_APPLYING_KEY)) )
                    return;
                if ( !BasicHTML.isHTMLString(editor.getText()) )
                    return;
                _forceRestyleOf(component);
            });
            return;
        }

        // JLabel (and any other simple "text"-property component):
        component.addPropertyChangeListener("text", evt -> {
            Object newValue = evt.getNewValue();
            if ( !(newValue instanceof String) )
                return;
            String newText = (String) newValue;
            if ( !BasicHTML.isHTMLString(newText) )
                return;
            Object lastInstalled = component.getClientProperty(_HTML_LAST_INSTALLED_KEY);
            if ( Objects.equals(newText, lastInstalled) )
                return; // this was our own setText
            _forceRestyleOf(component);
        });
    }

    private static void _forceRestyleOf( JComponent component ) {
        /*
            Force the cycle (force=true): the engine's stored StyleConf may
            still match the current styler output, but the component's
            actual text was just reset externally — so the conf-equality
            short-circuit would skip the re-injection we need.
        */
        ComponentExtension.from(component).gatherApplyAndInstallStyle(true);
    }

    /**
     *  Combined entry point that brings an HTML JLabel's text in sync with
     *  both the {@link UI#scale()} factor and the supplied {@link FontConf}.
     *
     *  <p>The label's text is treated as a function of three inputs: the
     *  user-provided "original" HTML, the current UI scale, and the active
     *  FontConf. The original is recovered from a client property each cycle,
     *  scaled, optionally augmented with a SwingTree-managed
     *  <code>&lt;head&gt;&lt;style&gt;</code> block, and reinstalled via
     *  {@link JLabel#setText(String)}. The result is also cached on the label
     *  so the next cycle can distinguish a "we did this" text from one set
     *  externally.
     */
    static void _applyHtmlScalingAndStyle(JLabel label, FontConf fontConf) {
        _applyHtmlScalingAndStyle(label, label.getText(), label::setText, fontConf);
    }

    /**
     *  Generalized variant that applies HTML scaling and styling to any component
     *  whose HTML content lives in a plain string — both {@link JLabel} and
     *  {@link JEditorPane} fall into this category when the editor kit is an
     *  {@code HTMLEditorKit} (the default for MIME type {@code text/html}).
     *
     *  <p>The caller provides the HTML string currently displayed (already read via
     *  the component's own accessor) and a setter that installs the modified string
     *  back into the component. SwingTree stores its tracking state
     *  ({@link #_HTML_LAST_INSTALLED_KEY}, {@link #_HTML_ORIGINAL_KEY}) on the
     *  component itself, so the same internal pipeline handles both types identically.
     *
     *  @param component the component being styled (used for the default font size)
     *  @param currentText the unmodified HTML string currently displayed
     *  @param setter  a function that installs a new HTML string back into the
     *                 component (for {@code JLabel}: {@code setText})
     *  @param fontConf  the font configuration derived from the style pipeline
     */
    static void _applyHtmlScalingAndStyle(
        JComponent       component,
        String           currentText,
        java.util.function.Consumer<String>   setter,
        FontConf         fontConf
    ) {
        if ( !BasicHTML.isHTMLString(currentText) ) {
            // Text became non-HTML: clear tracking state so a future HTML
            // value is treated as a fresh original (and not as something
            // we already installed).
            if ( component.getClientProperty(_HTML_LAST_INSTALLED_KEY) != null )
                component.putClientProperty(_HTML_LAST_INSTALLED_KEY, null);
            if ( component.getClientProperty(_HTML_ORIGINAL_KEY) != null )
                component.putClientProperty(_HTML_ORIGINAL_KEY, null);
            return;
        }

        float  scale    = UI.scale();
        String original = _recoverOriginalHtml(component, currentText);
        String scaled   = _scaleInlineFontSizes(original, scale);
        String css      = _buildHtmlBodyCss(fontConf)
                        + _buildHtmlScalingDefaultsCss(component, scale, fontConf);

        @Nullable String desired;
        if ( css.isEmpty() ) {
            desired = scaled;
        } else {
            desired = _injectStyleTag(scaled, css);
            if ( desired == null )
                desired = scaled;
        }

        component.putClientProperty(_HTML_ORIGINAL_KEY, original);

        if ( Objects.equals(desired, currentText) ) {
            component.putClientProperty(_HTML_LAST_INSTALLED_KEY, currentText);
        } else {
            // IMPORTANT: write the snapshot BEFORE setting the text so the
            // listener can recognise this as our own change.
            component.putClientProperty(_HTML_LAST_INSTALLED_KEY, desired);
            setter.accept(desired);
        }

        _ensureHtmlTextListenerInstalled(component);
    }

    private static String _recoverOriginalHtml(JComponent component, String currentText) {
        Object lastInstalled = component.getClientProperty(_HTML_LAST_INSTALLED_KEY);
        if ( Objects.equals(currentText, lastInstalled) ) {
            Object stored = component.getClientProperty(_HTML_ORIGINAL_KEY);
            if ( stored instanceof String )
                return (String) stored;
        }
        String stripped = _stripHtmlInjection(currentText);
        return stripped != null ? stripped : currentText;
    }

    /**
     *  Multiplies every {@code font-size:NN(px|pt)} value found in the given
     *  HTML by the supplied scale factor and returns the rewritten string.
     *  Returns the input unchanged when scale equals 1 or no match is found.
     */
    static String _scaleInlineFontSizes(String html, float scale) {
        if ( scale == 1f || html.isEmpty() )
            return html;

        Matcher m = _INLINE_FONT_SIZE_PATTERN.matcher(html);
        StringBuffer sb = new StringBuffer(html.length() + 16);
        boolean any = false;
        while ( m.find() ) {
            any = true;
            String prefix = m.group(1);
            double value  = Double.parseDouble(m.group(2));
            String unit   = m.group(3);
            double result = value * scale;
            String formatted;
            if ( !Double.isFinite(result) ) {
                formatted = m.group(2); // pathological scale; leave value alone
            } else if ( result == Math.floor(result) ) {
                formatted = Long.toString((long) result);
            } else {
                // Two decimals is plenty for sub-pixel precision and avoids
                // dragging an exponent or a 17-digit double into the CSS.
                formatted = String.format(Locale.ROOT, "%.2f", result);
            }
            m.appendReplacement(sb, Matcher.quoteReplacement(prefix + formatted + unit));
        }
        if ( !any )
            return html;
        m.appendTail(sb);
        return sb.toString();
    }

    static @Nullable String _stripHtmlInjection( @Nullable String html ) {
        if ( html == null )
            return null;

        if ( !BasicHTML.isHTMLString(html) )
            return html;

        if ( html.length() < _HTML_OPEN_TAG_LEN )
            return html;

        String afterOpen = html.substring(_HTML_OPEN_TAG_LEN);
        if ( !afterOpen.startsWith(_HTML_INJECTION_OPEN) )
            return html;

        int closeIdx = afterOpen.indexOf(_HTML_INJECTION_CLOSE, _HTML_INJECTION_OPEN.length());
        if ( closeIdx < 0 )
            return html;

        return html.substring(0, _HTML_OPEN_TAG_LEN)
                + afterOpen.substring(closeIdx + _HTML_INJECTION_CLOSE.length());
    }

    static @Nullable String _injectStyleTag( @Nullable String html, String css ) {
        if ( html == null )
            return null;

        if ( html.length() < _HTML_OPEN_TAG_LEN )
            return html;

        return html.substring(0, _HTML_OPEN_TAG_LEN)
                + _HTML_INJECTION_OPEN + css + _HTML_INJECTION_CLOSE
                + html.substring(_HTML_OPEN_TAG_LEN);
    }

    /*
        Swing's HTMLEditorKit ships a default stylesheet that pins heading
        font-sizes to absolute keywords (h1 = x-large, h2 = large, ...). Those
        keywords resolve, inside javax.swing.text.html.CSS, to fixed pt values
        that do not react to the JLabel's font, to FlatLaf's HiDPI scaling, or
        to UI.scale(). The body element is rendered using the JLabel's font,
        but headings are not.

        When UI.scale() != 1 we therefore need to override both the body and
        h1..h6 with explicit pt values so the rendered HTML actually scales.
        For the body we use either the FontConf.size (already scaled by the
        StyleConf pipeline) or, when no FontConf size was given, the JLabel's
        current font-size. Headings are sized as multiples of the base,
        matching the HTML5 default ratios.

        We deliberately skip this when scale == 1: at the default scale the
        kit's own defaults are the right answer and we want unstyled labels
        to remain text-untouched.
    */
    static String _buildHtmlScalingDefaultsCss(JComponent component, float scale, FontConf fontConf) {
        if ( scale == 1f )
            return "";

        int     baseSize;
        boolean injectBody;
        if ( fontConf.size() > 0 ) {
            baseSize   = fontConf.size(); // FontConf is already scaled upstream
            injectBody = false;           // _buildHtmlBodyCss already emitted body{font-size:...}
        } else {
            Font f = component.getFont();
            baseSize   = ( f != null && f.getSize() > 0 ) ? f.getSize() : 12; // We already expect SwingTree or the Look and Feel (like FlatLaF) to have the font size scaled!
            injectBody = true;
        }

        StringBuilder sb = new StringBuilder();
        if ( injectBody )
            sb.append("body{font-size:").append(baseSize).append("pt;}");
        sb.append("h1{font-size:").append(Math.round(baseSize * 2.00f)).append("pt;}");
        sb.append("h2{font-size:").append(Math.round(baseSize * 1.50f)).append("pt;}");
        sb.append("h3{font-size:").append(Math.round(baseSize * 1.17f)).append("pt;}");
        sb.append("h4{font-size:").append(baseSize).append("pt;}");
        sb.append("h5{font-size:").append(Math.round(baseSize * 0.83f)).append("pt;}");
        sb.append("h6{font-size:").append(Math.round(baseSize * 0.67f)).append("pt;}");
        return sb.toString();
    }

    static String _buildHtmlBodyCss(FontConf fontConf) {
        StringBuilder body = new StringBuilder();

        if ( !fontConf.family().isEmpty() )
            body.append("font-family:\"").append(_cssEscape(fontConf.family())).append("\";");

        if ( fontConf.size() > 0 )
            body.append("font-size:").append(fontConf.size()).append("pt;");

        fontConf.posture().ifPresent( p -> {
            if ( p > 0f )
                body.append("font-style:italic;");
        });

        fontConf.weight().ifPresent( w -> {
            // FontConf weight is on AWT's TextAttribute.WEIGHT scale where 1.0
            // is regular and 2.0 is bold. CSS font-weight uses 100..900 with
            // 400=normal and 700=bold. Map piecewise-linearly so the canonical
            // anchors land exactly, then snap to the nearest 100 since legacy
            // CSS only honors multiples of 100.
            int cssWeight;
            if ( w <= 1f ) cssWeight = Math.round(w * 400f);
            else           cssWeight = Math.round(400f + (w - 1f) * 300f);
            cssWeight = Math.max(100, Math.min(900, ((cssWeight + 50) / 100) * 100));
            String value;
            if ( cssWeight == 400 )      value = "normal";
            else if ( cssWeight == 700 ) value = "bold";
            else                         value = String.valueOf(cssWeight);
            body.append("font-weight:").append(value).append(";");
        });

        fontConf.paint().ifPresent( paint -> {
            if ( paint instanceof Color) {
                Color c = (Color) paint;
                body.append(String.format("color:#%02x%02x%02x;", c.getRed(), c.getGreen(), c.getBlue()));
            }
        });

        fontConf.backgroundPaint().ifPresent( paint -> {
            if ( paint instanceof Color ) {
                Color c = (Color) paint;
                body.append(String.format("background-color:#%02x%02x%02x;", c.getRed(), c.getGreen(), c.getBlue()));
            }
        });

        StringBuilder decoration = new StringBuilder();
        if ( fontConf.isUnderlined() )
            decoration.append("underline ");
        if ( fontConf.isStrikeThrough() )
            decoration.append("line-through ");
        if ( decoration.length() > 0 )
            body.append("text-decoration:").append(decoration.toString().trim()).append(";");

        if ( body.length() == 0 )
            return "";

        return "body{" + body + "}";
    }

    // ══════════════════════════════════════════════════════════════════
    // Settle-aware pipeline for JEditorPane (HTMLEditorKit normalises)
    // ══════════════════════════════════════════════════════════════════

    static void htmlSettles( JEditorPane ep, FontConf fontConf ) {
        String currentText = ep.getText();
        if ( currentText == null || !BasicHTML.isHTMLString(currentText) ) {
            ep.putClientProperty(_HTML_LAST_INSTALLED_KEY, null);
            ep.putClientProperty(_HTML_ORIGINAL_KEY, null);
            return;
        }

        float scale = UI.scale();
        String cssOutput = _buildHtmlBodyCss(fontConf)
                         +  _buildHtmlScalingDefaultsCss(ep, scale, fontConf);

        /*
            PHASE 1 SETTLE CHECK:
            Does current text already have our injected CSS that matches what
            we'd produce right now?  If yes, we're settled — just update the
            snapshot with the normalized form so future compares work.
        */
        Matcher settleM = SETTLE_MARKER_PATTERN.matcher(currentText);
        if ( settleM.find() ) {
            // Strip our sentinel banner (which sits at the start of the injected
            // CSS inside the <style> comment) so the comparison only sees real CSS.
            String existingCss = settleM.group(1)
                .replace(_HTML_STYLE_BANNER, "")
                .replaceAll("\\s+", "").replace(";","").trim();
            String expectedCss = cssOutput.replaceAll("\\s+", "").replace(";","");
            if ( existingCss.equals(expectedCss) ) {
                // Already settled — just update snapshot with current (normalized) form so
                // future recovery can recognise this write and hand back the original.
                ep.putClientProperty(_HTML_LAST_INSTALLED_KEY, ep.getText());
                return;
            }
            /* CSS differs → e.g. scale changed or external edit. Fall through to Phase 2. */
        }

        /*
            PHASE 2 INJECT / MAINTAIN:
            Recompute the desired text from the recovered original. Store the
            *normalized* round-tripped text as our write-snapshot, because a
            JEditorPane re-serialises getText() into a canonical form we cannot
            predict ahead of time.
        */
        String original = _recoverOriginalHtml(ep, currentText);
        String scaled   = _scaleInlineFontSizes(original, scale);
        // True when the current document still carries our (possibly outdated)
        // injection. Detected via the sentinel HTML comment, which survives
        // HTMLEditorKit normalisation.
        boolean currentlyStyledByUs = isHtmlStyledBySwingTree(currentText);

        ep.putClientProperty(_HTML_ORIGINAL_KEY, original);

        if ( cssOutput.isEmpty() ) {
            // No CSS required anymore (scale back to 1 / FontConf cleared). If we
            // previously injected a now-stale block, strip it so the user's editable
            // document is clean again; otherwise the document is already what we
            // want — leave it (and the caret) untouched.
            if ( currentlyStyledByUs )
                _stripStaleStyle(ep, scaled);
            else
                ep.putClientProperty(_HTML_LAST_INSTALLED_KEY, currentText);
            _ensureHtmlTextListenerInstalled(ep);
            return;
        }

        String desired = _injectStyleTag(scaled, cssOutput);
        if ( desired == null )
            desired = scaled;

        /*
            Only touch the document when the content actually changes. Writing
            setText() on every cycle would re-parse the document and reset the
            caret + selection to the start on a component whose whole purpose is
            editing. The write-snapshot stores the normalized form so the
            PropertyChange listener and future recovery can recognise it as ours.
        */
        if ( Objects.equals(desired, currentText) )
            ep.putClientProperty(_HTML_LAST_INSTALLED_KEY, currentText);
        else
            _setStyledText(ep, desired);

        _ensureHtmlTextListenerInstalled(ep);
    }

    /*
        Strip a stale SwingTree-injected <style> block from a JEditorPane. A plain
        setText() cannot do this: once an HTMLEditorKit document has seen a style
        block, it re-serialises and CARRIES THE OLD STYLE forward across subsequent
        setText() calls, so a clean setText() still yields styled getText(). The
        only reliable way to drop the stale CSS is to replace the underlying HTML
        document with a fresh one and then write the clean original on top of it.
    */
    private static void _stripStaleStyle( JEditorPane ep, String cleanHtml ) {
        ep.putClientProperty(_HTML_APPLYING_KEY, Boolean.TRUE);
        try {
            HTMLEditorKit kit = (HTMLEditorKit) ep.getEditorKit();
            ep.setDocument( kit.createDefaultDocument() );
            ep.setText(cleanHtml);
            ep.putClientProperty(_HTML_LAST_INSTALLED_KEY, ep.getText()); // normalized
        } finally {
            ep.putClientProperty(_HTML_APPLYING_KEY, Boolean.FALSE);
        }
    }

    /*
        Write a SwingTree-computed HTML document into a JEditorPane. Every write is
        bracketed by our re-entrancy marker so the "document" PropertyChange listener
        (see _ensureHtmlTextListenerInstalled) can tell our own writes apart from an
        external edit — otherwise the listener would refire a styling cycle from
        inside setText, reading a half-swapped document and cascading the stale style
        block back into the text. HTMLEditorKit normalises the serialised form, so the
        write snapshot only records what getText() reports after the swap.
    */
    private static void _setStyledText( JEditorPane ep, String text ) {
        ep.putClientProperty(_HTML_APPLYING_KEY, Boolean.TRUE);
        try {
            ep.setText(text);
            ep.putClientProperty(_HTML_LAST_INSTALLED_KEY, ep.getText()); // normalized
        } finally {
            ep.putClientProperty(_HTML_APPLYING_KEY, Boolean.FALSE);
        }
    }

    /*
        True when the given (possibly HTMLEditorKit-normalized) HTML still contains a
        SwingTree-injected style block, detected through our sentinel banner.
    */
    static boolean isHtmlStyledBySwingTree( String html ) {
        return html != null && html.indexOf(_HTML_STYLE_BANNER) >= 0;
    }

    private static String _cssEscape(String value) {
        // Strip rather than escape: Swing's CSS parser does not consistently honor
        // backslash escapes inside string values, so we play it safe and drop any
        // character that could let a hostile (or just quirky) value break out of
        // the surrounding "..." declaration.
        StringBuilder sb = new StringBuilder(value.length());
        for ( int i = 0; i < value.length(); i++ ) {
            char ch = value.charAt(i);
            if (
                ch == '"'  ||
                ch == ';'  ||
                ch == '\\' ||
                ch == '<'  ||
                ch == '>'  ||
                ch == '{'  ||
                ch == '}'  ||
                ch == '\n' ||
                ch == '\r'
            )
                sb.append(' ');
            else
                sb.append(ch);
        }
        return sb.toString();
    }

}