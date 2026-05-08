package swingtree.style;

import org.jspecify.annotations.Nullable;

import javax.swing.*;
import javax.swing.plaf.basic.BasicHTML;
import java.awt.*;

/*
    We achieve HTML-label styling by injecting a <head><style>...</style></head>
    block into the label's HTML text. Why text rewriting instead of something more
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

    To make the rewrite robust we:
     - Anchor the splice on the literal 6-char "<html>" prefix that
       BasicHTML.isHTMLString already validated, preserving the user's case
       ("<HTML>" vs "<html>" etc.).
     - Wrap our block in a unique marker so we can detect and strip our previous
       injection on the next cycle, leaving the user's original text recoverable.
     - Quote and escape user-provided values (font-family) so a malicious or
       quirky string cannot break out of the CSS declaration.

    Known caveats from Swing's CSS implementation (not fixable here):
     - font-weight via CSS does not render. It already propagates through
       setFont -> Font.isBold(), so we omit it.
     - letter-spacing is not honored at all.
     - User-provided <style> blocks within the original HTML still apply, and
       because their source order is later than ours, they win on conflicts.
       That matches the spirit of "user's HTML overrides framework defaults".
*/
final class LabelStyleInstallerUtility {

    private static final String _HTML_INJECTION_OPEN = "<head><style data-swingtree=\"injected\">";
    private static final String _HTML_INJECTION_CLOSE = "</style></head>";
    private static final String _HTML_TEXT_LISTENER_KEY = "swingtree.style.htmlTextListenerInstalled";
    private static final int    _HTML_OPEN_TAG_LEN = 6; // "<html>" — guaranteed by BasicHTML.isHTMLString

    private LabelStyleInstallerUtility() {}


    /*
        When the JLabel's text is changed externally — by a sprouts.Val binding,
        a manual setText elsewhere in the codebase, or any other mechanism — the
        renderer is rebuilt by BasicLabelUI from the new (un-styled) text and
        our injection is gone. To make the styling survive these out-of-band
        changes we install (once per label) a property change listener that
        triggers a fresh style cycle whenever the text changes to something
        that is HTML but does not carry our marker. The listener is a no-op
        for our own setText calls (the new text already has the marker) and
        for non-HTML text.
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
            if (
                newText.length() >= _HTML_OPEN_TAG_LEN &&
                newText.substring(_HTML_OPEN_TAG_LEN).startsWith(_HTML_INJECTION_OPEN)
            )
                return; // already carries our marker — this was our own setText
            /*
                Force the cycle (force=true): the engine's stored StyleConf may
                still match the current styler output, but the label's actual
                text was just reset externally — so the conf-equality short
                circuit would skip the re-injection we need.
            */
            ComponentExtension.from(label).gatherApplyAndInstallStyle(true);

        });
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
