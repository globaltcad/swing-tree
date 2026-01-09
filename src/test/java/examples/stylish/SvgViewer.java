package examples.stylish;

import com.formdev.flatlaf.FlatLightLaf;
import sprouts.From;
import sprouts.Var;
import sprouts.Viewable;
import swingtree.UI;
import swingtree.style.SvgIcon;

import javax.swing.*;
import java.util.function.Supplier;

import static swingtree.UI.*;

public class SvgViewer extends Panel
{
    public SvgViewer() {
        Var<String> svgText = Var.of(
                "<svg width=\"80px\" height=\"80px\" viewBox=\"0 0 24 24\" fill=\"none\" xmlns=\"http://www.w3.org/2000/svg\">\n" +
                        "<path fill-rule=\"evenodd\" clip-rule=\"evenodd\" d=\"M19.4506 1.22841C20.7282 0.863369 22 1.8227 22 3.15145V16C22 18.2091 20.2091 20 18 20C15.7909 20 14 18.2091 14 16C14 13.7909 15.7909 12 18 12C18.7286 12 19.4117 12.1948 20 12.5351V7.07142L10 9.92857V19C10 21.2091 8.20914 23 6 23C3.79086 23 2 21.2091 2 19C2 16.7909 3.79086 15 6 15C6.72857 15 7.41165 15.1948 8 15.5351V6.0086C8 5.11564 8.59196 4.33086 9.45056 4.08555L19.4506 1.22841ZM18.7669 3.36991C19.3815 3.18555 20 3.64573 20 4.28734C20 4.71033 19.7225 5.08323 19.3174 5.20477L11.2331 7.63008C10.6185 7.81444 10 7.35426 10 6.71265C10 6.28966 10.2775 5.91676 10.6826 5.79522L18.7669 3.36991ZM16.0167 16C16.0167 17.0953 16.9047 17.9833 18 17.9833C19.0954 17.9833 19.9833 17.0953 19.9833 16C19.9833 14.9046 19.0954 14.0167 18 14.0167C16.9047 14.0167 16.0167 14.9046 16.0167 16ZM4.0167 19C4.0167 20.0953 4.90465 20.9833 6 20.9833C7.09535 20.9833 7.9833 20.0953 7.9833 19C7.9833 17.9046 7.09535 17.0167 6 17.0167C4.90465 17.0167 4.0167 17.9046 4.0167 19Z\" fill=\"#0F0F0F\"/>\n" +
                        "</svg>"
        );
        Var<Placement> placement = Var.of(Placement.CENTER);
        Var<FitComponent> fitComponent = Var.of(FitComponent.NO);
        Supplier<ImageIcon> svgIconCreator = () -> SvgIcon.of(svgText.get())
                                               .withPreferredPlacement(placement.get())
                                               .withFitComponent(fitComponent.get());

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
                    textArea(svgText)
                )
            )
        )
        .add("grow",
            panel("fillx")
            .withBackground(Color.LIGHTSTEELBLUE.brighter())
            .add("center", label("Placement:"))
            .add("growx, center", comboBox(placement))
            .add("center", label("Fit component:"))
            .add("growx, , center", comboBox(fitComponent))
        )
        .add("push, grow",
            tabbedPane()
            .add(
                tab("Icon in Style API").add(
                    panel("fill")
                    .withBackground(Color.LIGHTSTEELBLUE.brighter())
                    .add("center",
                        UI.box().withMinSize(290, 220)
                        .withRepaintOn(svgText, placement, fitComponent)
                        .withStyle( conf -> conf
                            .image( img -> img
                                .image(svgIconCreator.get())
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
            .add(
                tab("SVG String in Style API").add(
                    panel("fill")
                    .withBackground(Color.LIGHTSTEELBLUE.brighter())
                    .add("center",
                        UI.box().withMinSize(290, 220)
                        .withRepaintOn(svgText, placement, fitComponent)
                        .withStyle( conf -> conf
                            .image( img -> img
                                .svg(svgText.get())
                                .fitMode(fitComponent.get())
                                .placement(placement.get())
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
            .add(
                tab("Buffered Image").add(
                    panel("fill")
                    .withBackground(Color.LIGHTSTEELBLUE.brighter())
                    .add("center",
                        UI.box().withMinSize(290, 220)
                        .withRepaintOn(svgText, placement, fitComponent)
                        .withStyle( conf -> conf
                            .image( img -> img
                                .image(svgIconCreator.get().getImage())
                                .fitMode(fitComponent.get())
                                .placement(placement.get())
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
            .add(
                tab("Component Icon").add(
                    panel("fill")
                    .withBackground(Color.LIGHTSTEELBLUE.brighter())
                    .add("center",
                        UI.icon("").withMinSize(290, 220)
                        .peek( l -> {
                            Viewable.cast(svgText).onChange(From.ALL, it -> {
                                l.setIcon(svgIconCreator.get());
                            });
                            Viewable.cast(placement).onChange(From.ALL, it -> {
                                l.setIcon(svgIconCreator.get());
                            });
                            Viewable.cast(fitComponent).onChange(From.ALL, it -> {
                                l.setIcon(svgIconCreator.get());
                            });
                            l.setIcon(svgIconCreator.get());
                        })
                        .withStyle( conf -> conf
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
        );
    }

    public static void main(String[] args) {
        FlatLightLaf.setup();
        UI.show( f -> new SvgViewer() );
    }
}