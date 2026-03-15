package examples.stylish;

import sprouts.Tuple;
import swingtree.api.Configurator;
import swingtree.style.FontConf;
import swingtree.style.StyledString;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * A pure-function Markdown parser that converts a raw markdown {@link String}
 * into a {@link Tuple}{@code <}{@link StyledString}{@code >}, ready to be fed
 * into SwingTree's style engine via {@code TextConf#content(Tuple)}.
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

    // ── Reusable inline-style configurators ───────────────────────────────────
    private static final Configurator<FontConf> BOLD        = f -> f.weight(2);
    private static final Configurator<FontConf> ITALIC      = f -> f.posture(0.2f);
    private static final Configurator<FontConf> BOLD_ITALIC = BOLD.andThen(ITALIC);
    private static final Configurator<FontConf> STRIKE      = f -> f.strikeThrough(true).color(COLOR_STRIKE);
    private static final Configurator<FontConf> CODE        = f -> f.family("Monospaced").color(COLOR_CODE_FG).backgroundColor(COLOR_CODE_BG);
    private static final Configurator<FontConf> LINK        = f -> f.color(COLOR_LINK).underlined(true);

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
            if (i < lines.length - 1)
                segments.add(StyledString.of("\n"));
        }
        return Tuple.of(StyledString.class, segments);
    }

    // ── Block-level parsing ───────────────────────────────────────────────────

    private static final Pattern HRULE_PATTERN          = Pattern.compile("^[-*_]{3,}\\s*$");
    private static final Pattern UNORDERED_LIST_PATTERN = Pattern.compile("^[\\-*+] .*");
    private static final Pattern ORDERED_LIST_PATTERN   = Pattern.compile("^\\d+\\.\\s.*");

    private static void parseLine(String line, List<StyledString> out) {

        // H3 must be checked before H2, H2 before H1 to avoid prefix ambiguity.
        if (line.startsWith("### ")) {
            parseInline(line.substring(4), out,
                f -> f.size(16).weight(2).color(COLOR_H3));

        } else if (line.startsWith("## ")) {
            parseInline(line.substring(3), out,
                f -> f.size(20).weight(2).color(COLOR_H2));

        } else if (line.startsWith("# ")) {
            parseInline(line.substring(2), out,
                f -> f.size(26).weight(2).color(COLOR_H1));

        } else if (HRULE_PATTERN.matcher(line).matches()) {
            out.add(StyledString.of(
                f -> f.color(COLOR_RULE),
                "────────────────────────────────────────"));

        } else if (line.startsWith("> ")) {
            out.add(StyledString.of(
                f -> f.color(COLOR_QUOTE_BAR).size(14), " ▎ "));
            parseInline(line.substring(2), out,
                f -> f.color(COLOR_QUOTE_TXT).posture(0.2f));

        } else if (UNORDERED_LIST_PATTERN.matcher(line).matches()) {
            out.add(StyledString.of("  • "));
            parseInline(line.substring(2), out, f -> f);

        } else if (ORDERED_LIST_PATTERN.matcher(line).matches()) {
            int dot = line.indexOf('.');
            out.add(StyledString.of(f -> f.weight(1.5f), "  " + line.substring(0, dot + 1) + " "));
            parseInline(line.substring(dot + 2), out, f -> f);

        } else {
            // Plain paragraph or empty line.
            parseInline(line, out, f -> f);
        }
    }

    // ── Inline-level parsing ──────────────────────────────────────────────────

    /**
     * Tokenises a single line for inline markdown constructs, appending
     * {@link StyledString} segments to {@code out}.
     *
     * <p>Each inline style is expressed as a {@link Configurator}{@code <FontConf>}
     * composed on top of the {@code base} block-level configurator via
     * {@link Configurator#andThen}.  This means heading color/size is preserved
     * automatically on any bold, italic, or link span found inside a heading line.
     *
     * <p>Inline code is intentionally <em>not</em> composed with {@code base}: a
     * monospaced code token should look the same regardless of whether it appears
     * in body text or a heading.
     *
     * @param text  Inline text to tokenise (block prefix already stripped).
     * @param out   Accumulator list that receives the produced segments.
     * @param base  Block-level {@link FontConf} configurator in effect for this line.
     */
    private static void parseInline(
            String text,
            List<StyledString> out,
            Configurator<FontConf> base
    ) {
        int i = 0;
        StringBuilder plain = new StringBuilder();

        while (i < text.length()) {
            char c = text.charAt(i);

            // Bold-italic  ***…***  — must be checked before ** and *
            if (text.startsWith("***", i)) {
                int end = text.indexOf("***", i + 3);
                if (end != -1) {
                    flushPlain(plain, out, base);
                    String content = text.substring(i + 3, end);
                    out.add(StyledString.of(base.andThen(BOLD_ITALIC), content));
                    i = end + 3;
                    continue;
                }
            }

            // Bold  **…**
            if (text.startsWith("**", i)) {
                int end = text.indexOf("**", i + 2);
                if (end != -1) {
                    flushPlain(plain, out, base);
                    String content = text.substring(i + 2, end);
                    out.add(StyledString.of(base.andThen(BOLD), content));
                    i = end + 2;
                    continue;
                }
            }

            // Italic  *…*  (not **)
            if (c == '*' && !text.startsWith("**", i)) {
                int end = text.indexOf('*', i + 1);
                if (end != -1 && !text.startsWith("**", end)) {
                    flushPlain(plain, out, base);
                    String content = text.substring(i + 1, end);
                    out.add(StyledString.of(base.andThen(ITALIC), content));
                    i = end + 1;
                    continue;
                }
            }

            // Bold  __…__
            if (text.startsWith("__", i)) {
                int end = text.indexOf("__", i + 2);
                if (end != -1) {
                    flushPlain(plain, out, base);
                    String content = text.substring(i + 2, end);
                    out.add(StyledString.of(base.andThen(BOLD), content));
                    i = end + 2;
                    continue;
                }
            }

            // Italic  _…_  (not __)
            if (c == '_' && !text.startsWith("__", i)) {
                int end = text.indexOf('_', i + 1);
                if (end != -1 && !text.startsWith("__", end)) {
                    flushPlain(plain, out, base);
                    String content = text.substring(i + 1, end);
                    out.add(StyledString.of(base.andThen(ITALIC), content));
                    i = end + 1;
                    continue;
                }
            }

            // Strikethrough  ~~…~~
            if (text.startsWith("~~", i)) {
                int end = text.indexOf("~~", i + 2);
                if (end != -1) {
                    flushPlain(plain, out, base);
                    String content = text.substring(i + 2, end);
                    out.add(StyledString.of(base.andThen(STRIKE), content));
                    i = end + 2;
                    continue;
                }
            }

            // Inline code  `…`  — intentionally ignores base to keep a consistent look
            if (c == '`') {
                int end = text.indexOf('`', i + 1);
                if (end != -1) {
                    flushPlain(plain, out, base);
                    String content = text.substring(i + 1, end);
                    out.add(StyledString.of(CODE, content));
                    i = end + 1;
                    continue;
                }
            }

            // Link  [label](url)  — label rendered, URL discarded
            if (c == '[') {
                int closeBracket = text.indexOf(']', i + 1);
                if (closeBracket != -1
                        && closeBracket + 1 < text.length()
                        && text.charAt(closeBracket + 1) == '(') {
                    int closeParen = text.indexOf(')', closeBracket + 2);
                    if (closeParen != -1) {
                        flushPlain(plain, out, base);
                        String label = text.substring(i + 1, closeBracket);
                        out.add(StyledString.of(base.andThen(LINK), label));
                        i = closeParen + 1;
                        continue;
                    }
                }
            }

            plain.append(c);
            i++;
        }

        flushPlain(plain, out, base);
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private static void flushPlain(
            StringBuilder buffer,
            List<StyledString> out,
            Configurator<FontConf> base
    ) {
        if (buffer.length() == 0) return;
        String text = buffer.toString();
        buffer.setLength(0);
        out.add(StyledString.of(base, text));
    }
}