package examples.stylish;

import com.formdev.flatlaf.FlatLightLaf;
import sprouts.Observable;
import sprouts.Var;
import swingtree.UI;

import static swingtree.UI.*;

/**
 * A live Markdown editor and preview panel built with <i>SwingTree</i>.
 *
 * <p>The panel is split vertically into two panes:
 * <ol>
 *   <li><b>Editor pane</b> — a plain {@link swingtree.UIForTextArea text area} that
 *       accepts raw <a href="https://commonmark.org">Markdown</a> input.  Every
 *       change is immediately reflected on the bottom half.</li>
 *   <li><b>Preview pane</b> — a scrollable styled-text box whose content is
 *       produced by {@link MarkdownParser#parse(String)}.  It re-renders
 *       automatically whenever the editor text, placement, boundary, or
 *       wrap-lines setting changes, courtesy of
 *       {@link swingtree.UIForAnySwing#withRepaintOn(Observable, Observable, Observable...)}.</li>
 * </ol>
 *
 * <h2>Reactive data-flow</h2>
 * <pre>{@code
 *  someText : Var<String>
 *       │  (bound to text area)
 *       ▼
 *  MarkdownParser.parse(someText.get())
 *       │  (called inside withStyle — re-evaluated on every repaint)
 *       ▼
 *  Tuple<StyledString>  →  TextConf#content(...)  →  SwingTree paint engine
 * }</pre>
 *
 * <p>Unlike a traditional listener-based approach, no intermediate
 * {@code Var<Tuple<StyledString>>} is needed: {@code withRepaintOn(someText, ...)}
 * schedules a repaint whenever {@code someText} changes, and the {@code withStyle}
 * lambda re-invokes {@code MarkdownParser.parse(someText.get())} fresh on each
 * paint pass, keeping the wiring minimal.
 *
 * <h2>Rendering controls</h2>
 * Three controls in the preview pane let you inspect how SwingTree's text-layout
 * engine positions the styled content:
 * <ul>
 *   <li><b>Placement</b> — maps to {@link swingtree.style.TextConf#placement(Placement)};
 *       controls where within the component boundary the text block is anchored.</li>
 *   <li><b>Boundary</b> — maps to
 *       {@link swingtree.style.TextConf#placementBoundary(UI.ComponentBoundary)};
 *       selects which geometric boundary (e.g. interior vs. content area) is used
 *       as the placement reference.</li>
 *   <li><b>Wrap lines</b> — maps to {@link swingtree.style.TextConf#wrapLines(boolean)};
 *       toggles soft line-wrapping in the preview.</li>
 * </ul>
 *
 * <h2>Demo content</h2>
 * {@link #INITIAL_MARKDOWN} is pre-loaded into the editor on startup. It
 * exercises every construct that {@link MarkdownParser} supports — headings,
 * bold/italic/strikethrough, inline code, lists, blockquotes, links, and
 * horizontal rules.
 *
 * @see MarkdownParser
 * @see swingtree.UIForAnySwing#withStyle(swingtree.api.Styler)
 * @see swingtree.style.TextConf
 */
public class TextViewer extends Panel
{
    private static final String INITIAL_MARKDOWN =
            "# Markdown Preview\n" +
            "\n" +
            "Edit the text on the left to see it rendered here in **real time**.\n" +
            "\n" +
            "## Text Formatting\n" +
            "\n" +
            "Combine **bold**, *italic*, and ***bold italic*** freely.\n" +
            "Use ~~strikethrough~~ for removed content.\n" +
            "\n" +
            "### Inline Code\n" +
            "\n" +
            "Reference types like `Tuple<StyledString>` or call\n" +
            "`System.out.println(\"Hello!\")` inline.\n" +
            "\n" +
            "## Lists\n" +
            "\n" +
            "Unordered:\n" +
            "- **Bold** emphasis item\n" +
            "- *Italic* style item\n" +
            "- Item with `inline code`\n" +
            "\n" +
            "Ordered:\n" +
            "1. Parse the markdown string\n" +
            "2. Build styled segments\n" +
            "3. Render via SwingTree\n" +
            "\n" +
            "## Blockquote\n" +
            "\n" +
            "> \"The purpose of abstraction is not to be vague,\n" +
            "> but to create a new semantic level.\"\n" +
            "\n" +
            "## Links\n" +
            "\n" +
            "Check out [SwingTree on GitHub](https://github.com/globaltcad/swing-tree)!\n" +
            "\n" +
            "---\n" +
            "\n" +
            "*Happy coding!*\n" +
            "\n" +
            "---\n" +
            "\n" +
            "## The Trait-Equality Argument\n" +
            "\n" +
            "**P1 (Conditional Premise):**\n" +
            "If your view affirms that a given human is trait-equalizable to a given nonhuman\n" +
            "animal while retaining moral value, then your view can only deny the nonhuman\n" +
            "animal has moral value on pain of contradiction (P ∧ ¬P).\n" +
            "\n" +
            "**P2 (Factual Premise):**\n" +
            "Your view affirms that a given human is trait-equalizable to a given nonhuman\n" +
            "animal while retaining moral value.\n" +
            "\n" +
            "> ***C (Conclusion):*** Therefore, your view can only deny the nonhuman animal\n" +
            "> has moral value on pain of contradiction.\n";

    public TextViewer() {
        Var<String> someText = Var.of(INITIAL_MARKDOWN);
        Var<Placement> placement = Var.of(Placement.TOP_LEFT);
        Var<UI.ComponentBoundary> componentBoundary = Var.of(ComponentBoundary.INTERIOR_TO_CONTENT);
        Var<Boolean> wrapLines = Var.of(true);
        UI.of(this).withLayout("fill, wrap 1")
        .withStyle( conf -> conf
            .prefSize(720,650)
            .foundationColor(Color.LIGHTSTEELBLUE)
            .margin(12)
            .padding(12)
            .borderRadius(12)
            .shadowColor(Color.BLACK)
            .shadowIsInset(true)
            .shadowBlurRadius(3)
            .shadowSpreadRadius(-1)
        )
        .add("grow",
            splitPane(Align.VERTICAL)
            .add(
                panel("fill").withMinSize(80, 20)
                .withBackground(Color.LIGHTSTEELBLUE.brighter())
                .add("push, grow",
                    scrollPane().add(
                        textArea(someText)
                    )
                )
            )
            .add(
                UI.box("fill, wrap 1")
                .add("grow",
                    panel("fillx")
                    .withBackground(Color.LIGHTSTEELBLUE.brighter())
                    .add("shrinkx", label("Placement:"))
                    .add("pushx, growx", comboBox(placement))
                    .add("shrinkx", label("Boundary:"))
                    .add("pushx, growx", comboBox(componentBoundary))
                    .add("pushx, growx", checkBox("Wrap lines", wrapLines))
                )
                .add("push, grow",
                    panel("fill")
                    .withBackground(Color.LIGHTSTEELBLUE.brighter())
                    .add("center, grow, push",
                        scrollPanels().add(
                            UI.box()
                            .withRepaintOn(someText, placement, componentBoundary, wrapLines)
                            .withStyle( conf -> conf
                                .padding(24)
                                .text( t -> t
                                    .font(f->f.color("gray"))
                                    .content(MarkdownParser.parse(someText.get()))
                                    .placement(placement.get())
                                    .placementBoundary(componentBoundary.get())
                                    .wrapLines(wrapLines.get())
                                    .autoPreferredHeight(true)
                                )
                                .border(12, Color.LIGHTSTEELBLUE)
                                .shadowColor(Color.BLACK)
                                .shadowBlurRadius(3)
                                .shadowSpreadRadius(-1)
                                .borderRadius(8)
                                .margin(13)
                            )
                        )
                    )
                )
            )
        );
    }

    public static void main(String[] args) {
        FlatLightLaf.setup();
        UI.show("Markdown Text Viewer", f -> new TextViewer() );
    }
}