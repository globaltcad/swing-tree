package examples.stylish;

import sprouts.Tuple;
import swingtree.style.FontConf;
import swingtree.style.StyledString;

import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

/**
 * A pure-function Markdown parser that converts a raw markdown {@link String}
 * into a {@link Tuple} of {@link StyledString} instances, ready to be fed into
 * SwingTree's style engine via
 * {@code TextConf#content(Tuple)}.
 *
 * <p>Supported syntax:
 * <ul>
 *   <li>{@code # H1}, {@code ## H2}, {@code ### H3}  — headings (size + weight + color)</li>
 *   <li>{@code **bold**}  or  {@code __bold__}        — bold (weight 2)</li>
 *   <li>{@code *italic*}  or  {@code _italic_}        — italic (posture 0.2)</li>
 *   <li>{@code ***bold italic***}                     — bold + italic</li>
 *   <li>{@code ~~strikethrough~~}                     — strike-through + dimmed color</li>
 *   <li>{@code `code`}                                — monospaced, tinted background</li>
 *   <li>{@code [label](url)}                          — underlined blue link label</li>
 *   <li>{@code > quote}                               — blockquote (indent marker + gray italic)</li>
 *   <li>{@code - item} / {@code * item}               — unordered list bullet</li>
 *   <li>{@code 1. item}                               — ordered list item</li>
 *   <li>{@code ---} / {@code ***} / {@code ___}       — horizontal rule</li>
 * </ul>
 *
 * <p>All parsing is stateless; every call to {@link #parse(String)} is independent
 * and produces a fresh {@link Tuple}.
 */
public final class MarkdownParser {

    // ── Design colors (hex strings accepted by FontConf#color(String)) ────────
    private static final String COLOR_H1        = "#1a365d";
    private static final String COLOR_H2        = "#2a4365";
    private static final String COLOR_H3        = "#2c5282";
    private static final String COLOR_RULE      = "#a0aec0";
    private static final String COLOR_QUOTE_BAR = "#a0aec0";
    private static final String COLOR_QUOTE_TXT = "#4a5568";
    private static final String COLOR_STRIKE    = "#718096";
    private static final String COLOR_CODE_FG   = "#c0392b";
    private static final String COLOR_CODE_BG   = "#fff0f0";
    private static final String COLOR_LINK      = "#3182ce";

    private MarkdownParser() {}

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Parses {@code markdown} and returns a {@link Tuple}{@code <}{@link StyledString}{@code >}
     * that SwingTree can render directly.
     *
     * @param markdown Raw markdown text. Must not be {@code null}.
     * @return An immutable {@code Tuple} of styled segments in document order.
     */
    public static Tuple<StyledString> parse(String markdown) {
        List<StyledString> segments = new ArrayList<>();
        String[] lines = markdown.split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            parseLine(lines[i], segments);
            // Preserve newlines between lines; avoid a spurious trailing newline.
            if (i < lines.length - 1) {
                segments.add(StyledString.of("\n"));
            }
        }
        return Tuple.of(StyledString.class, segments.toArray(new StyledString[0]));
    }

    // ── Block-level parsing ───────────────────────────────────────────────────

    private static void parseLine(String line, List<StyledString> out) {

        // H3 must be checked before H2, H2 before H1, so that "###" isn't mis-read as H1.
        if (line.startsWith("### ")) {
            parseInline(line.substring(4), out,
                    f -> f.size(16).weight(2).color(COLOR_H3));

        } else if (line.startsWith("## ")) {
            parseInline(line.substring(3), out,
                    f -> f.size(20).weight(2).color(COLOR_H2));

        } else if (line.startsWith("# ")) {
            parseInline(line.substring(2), out,
                    f -> f.size(26).weight(2).color(COLOR_H1));

        } else if (line.matches("^[-*_]{3,}\\s*$")) {
            // Horizontal rule ─ rendered as a Unicode line since SwingTree
            // cannot draw actual HR elements inside a styled-string flow.
            out.add(StyledString.of(f -> f.color(COLOR_RULE),
                    "────────────────────────────────────────"));

        } else if (line.startsWith("> ")) {
            // Blockquote: a coloured bar glyph followed by the quoted text.
            out.add(StyledString.of(f -> f.color(COLOR_QUOTE_BAR).size(14), " ▎ "));
            parseInline(line.substring(2), out,
                    f -> f.color(COLOR_QUOTE_TXT).posture(0.2f));

        } else if (line.matches("^[\\-*+] .*")) {
            // Unordered list item.
            out.add(StyledString.of("  • "));
            parseInline(line.substring(2), out, f -> f);

        } else if (line.matches("^\\d+\\.\\s.*")) {
            // Ordered list item — "1. text".
            int dot = line.indexOf('.');
            String prefix = "  " + line.substring(0, dot + 1) + " ";
            out.add(StyledString.of(f -> f.weight(1.5f), prefix));
            parseInline(line.substring(dot + 2), out, f -> f);

        } else {
            // Plain paragraph or empty line.
            parseInline(line, out, f -> f);
        }
    }

    // ── Inline-level parsing ──────────────────────────────────────────────────

    /**
     * Tokenises a single line of text for inline markdown constructs and appends the
     * resulting {@link StyledString} segments to {@code out}.
     *
     * <p>The {@code base} operator represents the block-level style currently in effect
     * (e.g. heading size/color).  Every inline segment composes on top of it via
     * {@code f -> base.apply(f).weight(2)} and similar lambdas.
     *
     * @param text  Inline text to tokenise (no leading block markers).
     * @param out   Accumulator list that receives the produced segments.
     * @param base  Block-level {@link FontConf} transformer applied to every segment.
     */
    private static void parseInline(
            String text,
            List<StyledString> out,
            UnaryOperator<FontConf> base
    ) {
        int i = 0;
        StringBuilder plain = new StringBuilder();

        while (i < text.length()) {
            char c = text.charAt(i);

            // ── Bold-italic  ***…***  (must be checked before ** and *)
            if (text.startsWith("***", i)) {
                int end = text.indexOf("***", i + 3);
                if (end != -1) {
                    flushPlain(plain, out, base);
                    String content = text.substring(i + 3, end);
                    out.add(StyledString.of(
                            f -> base.apply(f).weight(2).posture(0.2f), content));
                    i = end + 3;
                    continue;
                }
            }

            // ── Bold  **…**
            if (text.startsWith("**", i)) {
                int end = text.indexOf("**", i + 2);
                if (end != -1) {
                    flushPlain(plain, out, base);
                    String content = text.substring(i + 2, end);
                    out.add(StyledString.of(
                            f -> base.apply(f).weight(2), content));
                    i = end + 2;
                    continue;
                }
            }

            // ── Italic  *…*  (not **)
            if (c == '*' && !text.startsWith("**", i)) {
                int end = indexOfChar(text, '*', i + 1);
                if (end != -1 && !text.startsWith("**", end)) {
                    flushPlain(plain, out, base);
                    String content = text.substring(i + 1, end);
                    out.add(StyledString.of(
                            f -> base.apply(f).posture(0.2f), content));
                    i = end + 1;
                    continue;
                }
            }

            // ── Bold  __…__
            if (text.startsWith("__", i)) {
                int end = text.indexOf("__", i + 2);
                if (end != -1) {
                    flushPlain(plain, out, base);
                    String content = text.substring(i + 2, end);
                    out.add(StyledString.of(
                            f -> base.apply(f).weight(2), content));
                    i = end + 2;
                    continue;
                }
            }

            // ── Italic  _…_  (not __)
            if (c == '_' && !text.startsWith("__", i)) {
                int end = indexOfChar(text, '_', i + 1);
                if (end != -1 && !text.startsWith("__", end)) {
                    flushPlain(plain, out, base);
                    String content = text.substring(i + 1, end);
                    out.add(StyledString.of(
                            f -> base.apply(f).posture(0.2f), content));
                    i = end + 1;
                    continue;
                }
            }

            // ── Strikethrough  ~~…~~
            if (text.startsWith("~~", i)) {
                int end = text.indexOf("~~", i + 2);
                if (end != -1) {
                    flushPlain(plain, out, base);
                    String content = text.substring(i + 2, end);
                    out.add(StyledString.of(
                            f -> base.apply(f).strikeThrough(true).color(COLOR_STRIKE), content));
                    i = end + 2;
                    continue;
                }
            }

            // ── Inline code  `…`
            if (c == '`') {
                int end = indexOfChar(text, '`', i + 1);
                if (end != -1) {
                    flushPlain(plain, out, base);
                    String content = text.substring(i + 1, end);
                    // Code segments intentionally ignore the block-level base style
                    // so that monospace + tint always apply regardless of heading level.
                    out.add(StyledString.of(
                            f -> f.family("Monospaced").color(COLOR_CODE_FG).backgroundColor(COLOR_CODE_BG),
                            content));
                    i = end + 1;
                    continue;
                }
            }

            // ── Link  [label](url)  — only the label is rendered (URL discarded)
            if (c == '[') {
                int closeBracket = text.indexOf(']', i + 1);
                if (closeBracket != -1
                        && closeBracket + 1 < text.length()
                        && text.charAt(closeBracket + 1) == '(') {
                    int closeParen = text.indexOf(')', closeBracket + 2);
                    if (closeParen != -1) {
                        flushPlain(plain, out, base);
                        String label = text.substring(i + 1, closeBracket);
                        out.add(StyledString.of(
                                f -> base.apply(f).color(COLOR_LINK).underlined(true), label));
                        i = closeParen + 1;
                        continue;
                    }
                }
            }

            // ── Accumulate plain text
            plain.append(c);
            i++;
        }

        flushPlain(plain, out, base);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Emits the accumulated plain-text buffer as a single {@link StyledString}
     * styled with {@code base}, then clears the buffer.
     * Does nothing if the buffer is empty.
     */
    private static void flushPlain(
            StringBuilder buffer,
            List<StyledString> out,
            UnaryOperator<FontConf> base
    ) {
        if (buffer.length() == 0) return;
        String text = buffer.toString();
        buffer.setLength(0);
        out.add(StyledString.of(f -> base.apply(f), text));
    }

    /** Finds the first occurrence of {@code ch} in {@code text} at or after {@code from}. */
    private static int indexOfChar(String text, char ch, int from) {
        return text.indexOf(ch, from);
    }
}
