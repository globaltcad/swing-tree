package swingtree.style;

import org.jspecify.annotations.Nullable;
import swingtree.UI;

import javax.swing.*;
import javax.swing.plaf.basic.BasicHTML;
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
     - font-weight via CSS does not render. It already propagates through
       setFont -> Font.isBold(), so we omit it.
     - letter-spacing is not honored at all.
     - User-provided <style> blocks within the original HTML still apply, and
       because their source order is later than ours, they win on conflicts.
       That matches the spirit of "user's HTML overrides framework defaults".
     - We deliberately do not scale relative units (em, rem, %): they are
       computed against another value that is itself already scaled
       (the parent's font-size, which we will have rewritten if it carried
       a px/pt declaration). Scaling them too would compound.
*/
final class LabelStyleInstallerUtility {

    private static final String _HTML_INJECTION_OPEN  = "<head><style data-swingtree=\"injected\">";
    private static final String _HTML_INJECTION_CLOSE = "</style></head>";
    private static final String _HTML_TEXT_LISTENER_KEY  = "swingtree.style.htmlTextListenerInstalled";
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

    private LabelStyleInstallerUtility() {}


    /*
        When the JLabel's text is changed externally — by a sprouts.Val binding,
        a manual setText elsewhere in the codebase, or any other mechanism — the
        renderer is rebuilt by BasicLabelUI from the new (un-styled) text and
        our injection is gone. To make the styling survive these out-of-band
        changes we install (once per label) a property change listener that
        triggers a fresh style cycle whenever the text changes to something
        that differs from what we last installed. Comparing against the
        last-installed snapshot is what lets us tell our own setText apart
        from external ones — without it, every setText we do would trigger
        another cycle and we would loop.
    */
    static void _ensureHtmlTextListenerInstalled(JLabel label) {

        if ( label.getClientProperty(_HTML_TEXT_LISTENER_KEY) != null )
            return;

        label.putClientProperty(_HTML_TEXT_LISTENER_KEY, Boolean.TRUE);

        // Installing the text listener (once):
        label.addPropertyChangeListener("text", evt -> {

            Object newValue = evt.getNewValue();
            if ( !(newValue instanceof String) )
                return;
            String newText = (String) newValue;
            if ( !BasicHTML.isHTMLString(newText) )
                return;
            Object lastInstalled = label.getClientProperty(_HTML_LAST_INSTALLED_KEY);
            if ( Objects.equals(newText, lastInstalled) )
                return; // this was our own setText
            /*
                Force the cycle (force=true): the engine's stored StyleConf may
                still match the current styler output, but the label's actual
                text was just reset externally — so the conf-equality short
                circuit would skip the re-injection we need.
            */
            ComponentExtension.from(label).gatherApplyAndInstallStyle(true);

        });
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
        String currentText = label.getText();
        if ( !BasicHTML.isHTMLString(currentText) ) {
            // Text became non-HTML: clear tracking state so a future HTML
            // value is treated as a fresh original (and not as something
            // we already installed).
            if ( label.getClientProperty(_HTML_LAST_INSTALLED_KEY) != null )
                label.putClientProperty(_HTML_LAST_INSTALLED_KEY, null);
            if ( label.getClientProperty(_HTML_ORIGINAL_KEY) != null )
                label.putClientProperty(_HTML_ORIGINAL_KEY, null);
            return;
        }

        float  scale    = UI.scale();
        String original = _recoverOriginalHtml(label, currentText);
        String scaled   = _scaleInlineFontSizes(original, scale);
        String css      = _buildHtmlBodyCss(fontConf)
                        + _buildHtmlScalingDefaultsCss(label, scale, fontConf);

        @Nullable String desired;
        if ( css.isEmpty() ) {
            desired = scaled;
        } else {
            desired = _injectStyleTag(scaled, css);
            if ( desired == null )
                desired = scaled;
        }

        label.putClientProperty(_HTML_ORIGINAL_KEY, original);

        if ( Objects.equals(desired, currentText) ) {
            label.putClientProperty(_HTML_LAST_INSTALLED_KEY, currentText);
        } else {
            // IMPORTANT: write the snapshot BEFORE setText so the listener
            // installed below can recognise this as our own change.
            label.putClientProperty(_HTML_LAST_INSTALLED_KEY, desired);
            label.setText(desired);
        }

        _ensureHtmlTextListenerInstalled(label);
    }

    /*
       Recover the un-scaled, un-injected HTML.

       The label's current text is one of three things:
        1. The text we installed on the previous cycle (matches lastInstalled).
           In that case the stored original is authoritative.
        2. Text set by external code (Var binding, setText elsewhere). The
           stored original is now stale; we strip our injection (defensively,
           in case it is still in there) and treat the result as a new
           original.
        3. The first time we see this label — neither client property is set
           yet. Same as (2).
    */
    private static String _recoverOriginalHtml(JLabel label, String currentText) {
        Object lastInstalled = label.getClientProperty(_HTML_LAST_INSTALLED_KEY);
        if ( Objects.equals(currentText, lastInstalled) ) {
            Object stored = label.getClientProperty(_HTML_ORIGINAL_KEY);
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

    static void _stripHtmlInjection(JLabel label) {
        String currentText = label.getText();
        String stripped = _stripHtmlInjection(currentText);
        if ( !Objects.equals(stripped, currentText) )
            label.setText(stripped);
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
        current font-size multiplied by UI.scale(). Headings are sized as
        multiples of the base, matching the HTML5 default ratios.

        We deliberately skip this when scale == 1: at the default scale the
        kit's own defaults are the right answer and we want unstyled labels
        to remain text-untouched.
    */
    static String _buildHtmlScalingDefaultsCss(JLabel label, float scale, FontConf fontConf) {
        if ( scale == 1f )
            return "";

        int     baseSize;
        boolean injectBody;
        if ( fontConf.size() > 0 ) {
            baseSize   = fontConf.size(); // FontConf is already scaled upstream
            injectBody = false;           // _buildHtmlBodyCss already emitted body{font-size:...}
        } else {
            Font f = label.getFont();
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
            if ( w == 2 )
                body.append("font-style:bold;");
        });

        fontConf.paint().ifPresent( paint -> {
            if ( paint instanceof Color) {
                Color c = (Color) paint;
                body.append(String.format("color:#%02x%02x%02x;", c.getRed(), c.getGreen(), c.getBlue()));
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