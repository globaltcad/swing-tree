package examples.stylish;

import com.formdev.flatlaf.FlatLightLaf;
import sprouts.Var;
import swingtree.UI;

import static swingtree.UI.*;

public class TextViewer extends Panel
{
    public TextViewer() {
        Var<String> someText = Var.of(
                    "P1 (Conditional Premise): If your view affirms that a given human is trait-equalizable to " +
                     "a given nonhuman animal while retaining moral value, then your view can only deny the nonhuman " +
                     "animal has moral value on pain of contradiction (P ∧ ¬P)." +
                     "\n" +
                     "P2 (Factual Premise): Your view affirms that a given human is trait-equalizable to a " +
                     "given nonhuman animal while retaining moral value." +
                     "\n" +
                     "C (Conclusion): Therefore, your view can only deny the nonhuman animal has moral value on pain of contradiction."
                );
        Var<Placement> placement = Var.of(Placement.CENTER);
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
            panel("fill").withMinSize(120, 200)
            .withBackground(Color.LIGHTSTEELBLUE.brighter())
            .add("push, grow",
                scrollPane().add(
                    textArea(someText)
                )
            )
        )
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
                UI.box().withMinSize(230, 200)
                .withRepaintOn(someText, placement, componentBoundary, wrapLines)
                .withStyle( conf -> conf
                    .padding(24)
                    .text( t -> t
                        .content(someText.get())
                        .placement(placement.get())
                        .placementBoundary(componentBoundary.get())
                        .wrapLines(wrapLines.get())
                    )
                    .border(12, Color.LIGHTSTEELBLUE)
                    .shadowColor(Color.BLACK)
                    .shadowBlurRadius(3)
                    .shadowSpreadRadius(-1)
                    .borderRadius(8)
                    .margin(13)
                )
            )
        );
    }

    public static void main(String[] args) {
        FlatLightLaf.setup();
        UI.show( f -> new TextViewer() );
    }
}