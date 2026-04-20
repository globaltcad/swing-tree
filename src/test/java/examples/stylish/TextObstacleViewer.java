package examples.stylish;

import com.formdev.flatlaf.FlatLightLaf;
import sprouts.Val;
import sprouts.Var;
import swingtree.UI;
import swingtree.api.Layout;

import static swingtree.UI.*;

public class TextObstacleViewer extends Panel
{
    private static final String INITIAL_MARKDOWN =
            "# A Wall of Text\n" +
            "\n" +
            "This is a wall of text. It's contents are almost completely worthless, " +
            "except of course for its ability to hold a lot words and symbols, which " +
            "express their worth solely by taking up a lot of screen space. It's stream of words goes on " +
            "and on for quite a bit, and it doesn't really say anything. It just keeps dumping words " +
            "and sentences one after another into your face, like a never-ending river of text. " +
            "This is, of course, useless in most contexts, but here it serves a purpose: to be a large " +
            "block of text that can be used to test how the SwingTree text layout and rendering engine " +
            "deals with large amounts of content configured with various placement and wrapping options. " +
            "So while this wall of text may not be particularly meaningful or insightful, it is a useful " +
            "tool for testing and demonstrating the capabilities of the SwingTree library when it comes " +
            "to handling and displaying large amounts of text in a styled and configurable way. " +
            "Feel free to edit this text or replace it with your own content to see how it renders in the preview below!\n" +
            "\n" +
            "---\n" +
            "And remember, despite being so ridiculously long and verbose, this text has value, and " +
            "it deserves to be read and appreciated for what it is: a wall of text that serves a purpose in the context of this demo. " +
            "So don't just scroll past it without giving it a chance. Take a moment to read through it, and appreciate the sheer volume of " +
            "words and sentences that it contains. " +
            "And if you find yourself getting bored or overwhelmed by the sheer amount of text, " +
            "just remember that it's all part of the fun and the point of this demo: to see how SwingTree handles " +
            "a wall of text with various styling and layout options. So embrace the wall of text, and enjoy the demo!";
    public TextObstacleViewer() {
        Var<String> someText = Var.of(INITIAL_MARKDOWN);
        Var<Placement> placement = Var.of(Placement.TOP_LEFT);
        Var<ComponentBoundary> componentBoundary = Var.of(ComponentBoundary.INTERIOR_TO_CONTENT);
        Var<Boolean> wrapLines = Var.of(true);
        Var<ComponentBoundary> obstacleArea = Var.of(ComponentBoundary.INTERIOR_TO_CONTENT);
        UI.of(this).withLayout("fill, wrap 1")
        .withStyle( conf -> conf
            .prefSize(1120,650)
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
            UI.box("fill, wrap 1")
            .add("grow",
                panel("fillx")
                .withBackground(Color.LIGHTSTEELBLUE.brighter())
                .add("shrinkx", label("Placement:"))
                .add("pushx, growx", comboBox(placement))
                .add("shrinkx", label("Boundary:"))
                .add("pushx, growx", comboBox(componentBoundary))
                .add("shrinkx", label("Obstacle Area:"))
                .add("pushx, growx", comboBox(obstacleArea))
                .add("pushx, growx", checkBox("Wrap lines", wrapLines))
            )
            .add("push, grow",
                splitPane(Align.HORIZONTAL)
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
                    panel("fill")
                    .withBackground(Color.LIGHTSTEELBLUE.brighter())
                    .add("center, grow, push",
                        scrollPanels().add(
                            UI.box().withPrefHeight(800)
                            .withLayout(Val.of(Layout.none()))
                            .withRepaintOn(someText, placement, componentBoundary, wrapLines, obstacleArea)
                            .withStyle( conf -> conf
                                .padding(24)
                                .text( t -> t
                                    .font(f->f.color("dark blue"))
                                    .content(MarkdownParser.parse(someText.get()))
                                    .placement(placement.get())
                                    .placementBoundary(componentBoundary.get())
                                    .wrapLines(wrapLines.get())
                                    .autoPreferredHeight(false)
                                    .obstaclesFromChildren(obstacleArea.get())
                                )
                                .border(12, Color.LIGHTSTEELBLUE)
                                .shadowColor(Color.BLACK)
                                .shadowBlurRadius(3)
                                .shadowSpreadRadius(-1)
                                .borderRadius(8)
                                .margin(13)
                            )
                            .add(
                                panel("fill").peek( it -> it.setBounds(UI.scale(50), UI.scale(100), UI.scale(150), UI.scale(200)) )
                                .withStyle( it -> it.backgroundColor("transparent red").borderRadius(64) )
                                .onMouseDrag( e -> {
                                    e.setLocation(e.getX() + e.deltaXSinceStart(),e.getY() + e.deltaYSinceStart());
                                    e.getParent().repaint();
                                })
                            )
                            .add(
                                panel("fill").peek( it -> it.setBounds(UI.scale(350), UI.scale(200), UI.scale(200), UI.scale(150)) )
                                .withStyle( it -> it.backgroundColor("transparent green").margin(24) )
                                .onMouseDrag( e -> {
                                    e.setLocation(e.getX() + e.deltaXSinceStart(),e.getY() + e.deltaYSinceStart());
                                    e.getParent().repaint();
                                })
                            )
                            .add(
                                panel("fill").peek( it -> it.setBounds(UI.scale(650), UI.scale(300), UI.scale(100), UI.scale(100)) )
                                .withStyle( it -> it.backgroundColor("transparent blue").padding(24) )
                                .onMouseDrag( e -> {
                                    e.setLocation(e.getX() + e.deltaXSinceStart(),e.getY() + e.deltaYSinceStart());
                                    e.getParent().repaint();
                                })
                            )
                        )
                    )
                )
            )
        );
    }

    public static void main(String[] args) {
        FlatLightLaf.setup();
        UI.show("Markdown Text Viewer", f -> new TextObstacleViewer() );
    }
}