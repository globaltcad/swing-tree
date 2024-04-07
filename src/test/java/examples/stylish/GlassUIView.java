package examples.stylish;

import swingtree.UI;

import static swingtree.UI.*;

public class GlassUIView extends Panel
{
    public GlassUIView() {
        UI.of(this).withLayout("fill")
        .withStyle( conf -> conf
            .prefSize(720,350)
            .foundationColor(Color.LIGHTSTEELBLUE)
            .margin(32)
            .padding(16)
            .borderRadius(26)
            .border(3, Color.LIGHTSTEELBLUE.darker())
            .shadowColor(Color.BLACK)
            .shadowIsInset(true)
            .shadowBlurRadius(5)
            .shadowSpreadRadius(-1)
            .noise( noiseConf -> noiseConf
                .colors(Color.CYAN, Color.MAGENTA, Color.DARKBLUE)
                .function(NoiseType.RETRO)
                .scale(3)
            )
        )
        .add("center",
            UI.label("Glassy Label")
            .withStyle( conf -> conf
                .fontSize(28)
                .fontColor(Color.GREENYELLOW)
                .prefSize(200, 50)
                .margin(38)
                .padding(38)
                .borderRadius(16)
                .border(1, Color.BLACK)
                .shadowColor(Color.BLACK)
                .shadowIsInset(false)
                .shadowBlurRadius(4)
                .shadowSpreadRadius(-1)
                .backgroundColor(Color.TRANSPARENT)
                .parentFilter( filterConf -> filterConf
                    .area(ComponentArea.BODY)
                    .blur(0)
                    .scale(1.25, 1.25)
                )
            )
        );
    }

    public static void main(String[] args) {
        UI.show( f -> new GlassUIView() );
    }
}
