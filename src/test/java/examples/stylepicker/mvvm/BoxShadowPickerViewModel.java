package examples.stylepicker.mvvm;

import sprouts.*;
import sprouts.Event;
import swingtree.UI;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

/**
 *  The view model for the {@link BoxShadowPickerView} which is a UI for configuring
 *  the style of a panel with a box shadow.
 *  This is also a good demonstration of how to use the MVVM pattern with SwingTree.
 */
public class BoxShadowPickerViewModel
{
    public class BorderEdgeViewModel
    {
        private final Var<Integer> borderWidth = Var.of(3);
        private final Var<Color>   borderColor = Var.of(new Color(0,0.4f,1));

        public BorderEdgeViewModel() {
            Viewable.cast(borderWidth).onChange(From.VIEW, it -> repaint.fire() );
            Viewable.cast(borderColor).onChange(From.VIEW, it -> repaint.fire() );
        }

        public Var<Integer> borderWidth() { return borderWidth; }
        public Var<Color> borderColor() { return borderColor; }
    }

    public class BorderCornerViewModel {

        private final Var<Integer> borderArcWidth  = Var.of(25);
        private final Var<Integer> borderArcHeight = Var.of(25);

        public BorderCornerViewModel() {
            Viewable.cast(borderArcWidth).onChange(From.VIEW, it -> repaint.fire() );
            Viewable.cast(borderArcHeight).onChange(From.VIEW, it -> repaint.fire() );
        }

        public Var<Integer> borderArcWidth() { return borderArcWidth; }
        public Var<Integer> borderArcHeight() { return borderArcHeight; }
    }

    private final Event repaint = Event.create();

    // Padding
    private final Var<Integer> paddingTop = Var.of(5);
    private final Var<Integer> paddingLeft = Var.of(5);
    private final Var<Integer> paddingRight = Var.of(5);
    private final Var<Integer> paddingBottom = Var.of(5);

    // Margin
    private final Var<Integer> marginTop = Var.of(30);
    private final Var<Integer> marginLeft = Var.of(35);
    private final Var<Integer> marginRight = Var.of(35);
    private final Var<Integer> marginBottom = Var.of(30);

    // Border
    private final Var<UI.Edge>    borderEdge   = Var.of(UI.Edge.EVERY);
    private final Var<UI.Corner>  borderCorner = Var.of(UI.Corner.EVERY);

    private final Map<UI.Edge, BorderEdgeViewModel> edgeModels = new HashMap<>();
    private final Map<UI.Corner, BorderCornerViewModel> cornerModels = new HashMap<>();

    private final Var<BorderEdgeViewModel> currentEdgeModel = Var.ofNullable(BorderEdgeViewModel.class, null);
    private final Var<BorderCornerViewModel> currentCornerModel = Var.ofNullable(BorderCornerViewModel.class, null);

    // Background
    private final Var<Color> backgroundColor = Var.of(new Color(0.1f, 0.75f, 0.9f));
    private final Var<Color> foundationColor = Var.of(new Color(1f,1f,1f));

    // Box Shadow
    private final Var<Integer> horizontalShadowOffset = Var.of(0);
    private final Var<Integer> verticalShadowOffset = Var.of(0);
    private final Var<Integer> shadowBlurRadius = Var.of(6);
    private final Var<Integer> shadowSpreadRadius = Var.of(5);
    private final Var<Color> shadowColor = Var.of(Color.DARK_GRAY);
    private final Var<Boolean> shadowInset = Var.of(false);

    private final Var<UI.NoiseType>     noise       = Var.of(UI.NoiseType.TISSUE);
    private final Var<String>           noiseColors = Var.of("");
    private final Var<UI.ComponentArea> noiseArea   = Var.of(UI.ComponentArea.ALL);

    // Smiley (For fun)
    private final Var<Boolean> drawSmiley = Var.of(false);

    private final Var<String> code = Var.of("");

    public BoxShadowPickerViewModel() {
        Viewable.cast(paddingTop).onChange(From.VIEW, it -> repaint.fire() );
        Viewable.cast(paddingLeft).onChange(From.VIEW, it -> repaint.fire() );
        Viewable.cast(paddingRight).onChange(From.VIEW, it -> repaint.fire() );
        Viewable.cast(paddingBottom).onChange(From.VIEW, it -> repaint.fire() );

        Viewable.cast(marginTop).onChange(From.VIEW, it -> repaint.fire() );
        Viewable.cast(marginLeft).onChange(From.VIEW, it -> repaint.fire() );
        Viewable.cast(marginRight).onChange(From.VIEW, it -> repaint.fire() );
        Viewable.cast(marginBottom).onChange(From.VIEW, it -> repaint.fire() );

        Viewable.cast(borderEdge).onChange(From.VIEW, it -> updateEdgeSelection(it.currentValue().orElseThrowUnchecked()) );
        Viewable.cast(borderCorner).onChange(From.VIEW, it -> updateCornerSelection(it.currentValue().orElseThrowUnchecked()) );

        Viewable.cast(backgroundColor).onChange(From.VIEW, it -> repaint.fire() );
        Viewable.cast(foundationColor).onChange(From.VIEW, it -> repaint.fire() );

        Viewable.cast(horizontalShadowOffset).onChange(From.VIEW, it -> repaint.fire() );
        Viewable.cast(verticalShadowOffset).onChange(From.VIEW, it -> repaint.fire() );
        Viewable.cast(shadowBlurRadius).onChange(From.VIEW, it -> repaint.fire() );
        Viewable.cast(shadowSpreadRadius).onChange(From.VIEW, it -> repaint.fire() );
        Viewable.cast(shadowColor).onChange(From.VIEW, it -> repaint.fire() );
        Viewable.cast(shadowInset).onChange(From.VIEW, it -> repaint.fire() );

        Viewable.cast(noise).onChange(From.VIEW, it -> repaint.fire() );
        Viewable.cast(noiseColors).onChange(From.VIEW, it -> repaint.fire() );
        Viewable.cast(noiseArea).onChange(From.VIEW, it -> repaint.fire() );

        Viewable.cast(drawSmiley).onChange(From.VIEW, it -> repaint.fire() );

        // Creating sub-view models
        for (UI.Edge edge : UI.Edge.values()) {
            BorderEdgeViewModel model = new BorderEdgeViewModel();
            edgeModels.put(edge, model);
            if (edge == UI.Edge.EVERY) {
                currentEdgeModel.set(model);
            }
        }
        for (UI.Corner corner : UI.Corner.values()) {
            BorderCornerViewModel model = new BorderCornerViewModel();
            cornerModels.put(corner, model);
            if (corner == UI.Corner.EVERY) {
                currentCornerModel.set(model);
            }
        }
        Observable.cast(repaint).subscribe( () -> createCode() );
        createCode();
    }

    private void updateEdgeSelection(UI.Edge edge) {
        currentEdgeModel.set(edgeModels.get(edge));
        repaint.fire();
    }

    private void updateCornerSelection(UI.Corner corner) {
        currentCornerModel.set(cornerModels.get(corner));
        repaint.fire();
    }

    public Event repaint() { return repaint; }

    public Var<Integer> paddingTop() { return paddingTop; }

    public Var<Integer> paddingLeft() { return paddingLeft; }

    public Var<Integer> paddingRight() { return paddingRight; }

    public Var<Integer> paddingBottom() { return paddingBottom; }

    public Var<Integer> marginTop() { return marginTop; }

    public Var<Integer> marginLeft() { return marginLeft; }

    public Var<Integer> marginRight() { return marginRight; }

    public Var<Integer> marginBottom() { return marginBottom; }
    public Var<UI.Corner> borderCorner() { return borderCorner; }
    public Var<UI.Edge> borderEdge() { return borderEdge; }

    public Var<BorderEdgeViewModel> currentEdgeModel() { return currentEdgeModel; }

    public int leftBorderWidth() { return getEdgeModel(UI.Edge.LEFT).borderWidth.get(); }
    public int rightBorderWidth() { return getEdgeModel(UI.Edge.RIGHT).borderWidth.get(); }
    public int topBorderWidth() { return getEdgeModel(UI.Edge.TOP).borderWidth.get(); }
    public int bottomBorderWidth() { return getEdgeModel(UI.Edge.BOTTOM).borderWidth.get(); }

    public Color topBorderColor() { return getEdgeModel(UI.Edge.TOP).borderColor.get(); }
    public Color rightBorderColor() { return getEdgeModel(UI.Edge.RIGHT).borderColor.get(); }
    public Color bottomBorderColor() { return getEdgeModel(UI.Edge.BOTTOM).borderColor.get(); }
    public Color leftBorderColor() { return getEdgeModel(UI.Edge.LEFT).borderColor.get(); }

    private BoxShadowPickerViewModel.BorderEdgeViewModel getEdgeModel(UI.Edge edge) {
        // We check if the
        if ( currentEdgeModel.is(edgeModels.get(UI.Edge.EVERY)) )
            return currentEdgeModel.get();

        return edgeModels.get(edge);
    }

    public Var<BorderCornerViewModel> currentCornerModel() { return currentCornerModel; }

    public int arcWidthAt(UI.Corner corner) { return getCornerModel(corner).borderArcWidth.get(); }
    public int arcHeightAt(UI.Corner corner) { return getCornerModel(corner).borderArcHeight.get(); }

    private BoxShadowPickerViewModel.BorderCornerViewModel getCornerModel(UI.Corner corner) {
        // We check if the
        if ( currentCornerModel.is(cornerModels.get(UI.Corner.EVERY)) )
            return currentCornerModel.get();

        return cornerModels.get(corner);
    }

    public Var<Color> backgroundColor() { return backgroundColor; }

    public Var<Color> foundationColor() { return foundationColor; }

    public Var<Integer> horizontalShadowOffset() { return horizontalShadowOffset; }

    public Var<Integer> verticalShadowOffset() { return verticalShadowOffset; }

    public Var<Integer> shadowBlurRadius() { return shadowBlurRadius; }

    public Var<Integer> shadowSpreadRadius() { return shadowSpreadRadius; }

    public Var<Color> shadowColor() { return shadowColor; }

    public Var<Boolean> shadowInset() { return shadowInset; }

    public Var<Boolean> drawSmiley() { return drawSmiley; }

    public Var<UI.NoiseType>     noise()       { return noise; }
    public Var<String>           noiseColors() { return noiseColors; }
    public Var<UI.ComponentArea> noiseArea()   { return noiseArea; }

    public Var<String> code() { return code; }

    private void createCode() {
        String cornerRadius = "";
        if ( this.borderCorner.is(UI.Corner.EVERY) ) {
            if ( arcWidthAt(UI.Corner.TOP_LEFT) == arcHeightAt(UI.Corner.TOP_LEFT) )
                cornerRadius = "     .borderRadius(" + arcWidthAt(UI.Corner.TOP_LEFT) + ")\n";
            else
                cornerRadius = "     .borderRadius(" + arcWidthAt(UI.Corner.TOP_LEFT) + ", " + arcHeightAt(UI.Corner.TOP_LEFT) + ")\n";
        }
        else
            cornerRadius =
                "     .borderRadiusAt(UI.Corner.TOP_LEFT, " + arcWidthAt(UI.Corner.TOP_LEFT) + ", " + arcHeightAt(UI.Corner.TOP_LEFT) + ")\n" +
                "     .borderRadiusAt(UI.Corner.TOP_RIGHT, " + arcWidthAt(UI.Corner.TOP_RIGHT) + ", " + arcHeightAt(UI.Corner.TOP_RIGHT) + ")\n" +
                "     .borderRadiusAt(UI.Corner.BOTTOM_LEFT, " + arcWidthAt(UI.Corner.BOTTOM_LEFT) + ", " + arcHeightAt(UI.Corner.BOTTOM_LEFT) + ")\n" +
                "     .borderRadiusAt(UI.Corner.BOTTOM_RIGHT, " + arcWidthAt(UI.Corner.BOTTOM_RIGHT) + ", " + arcHeightAt(UI.Corner.BOTTOM_RIGHT) + ")\n";

        String borderWidth = "";
        if ( this.borderEdge.is(UI.Edge.EVERY) ) {
            if ( leftBorderWidth() == rightBorderWidth() && leftBorderWidth() == topBorderWidth() && leftBorderWidth() == bottomBorderWidth() )
                borderWidth = leftBorderWidth() == 0 ? "" : "     .borderWidth(" + leftBorderWidth() + ")\n";
            else
                borderWidth =
                    "     .borderWidth(" + leftBorderWidth() + ", " + topBorderWidth() + ", " + rightBorderWidth() + ", " + bottomBorderWidth() + ")\n";
        }
        else
            borderWidth =
                "     .borderWidthAt(UI.Edge.LEFT, " + leftBorderWidth() + ")\n" +
                "     .borderWidthAt(UI.Edge.TOP, " + topBorderWidth() + ")\n" +
                "     .borderWidthAt(UI.Edge.RIGHT, " + rightBorderWidth() + ")\n" +
                "     .borderWidthAt(UI.Edge.BOTTOM, " + bottomBorderWidth() + ")\n";

        String borderColoring = "";
        boolean allColorsAreTheSame = topBorderColor().equals(rightBorderColor()) && topBorderColor().equals(bottomBorderColor()) && topBorderColor().equals(leftBorderColor());
        if ( allColorsAreTheSame )
            borderColoring = topBorderColor().equals(Color.BLACK) ? "" : "     .borderColor(" + str(topBorderColor()) + ")\n";
        else
            borderColoring =
                "     .borderColors(\n" +
                "         " + str(topBorderColor()) + ",\n" +
                "         " + str(rightBorderColor()) + ",\n" +
                "         " + str(bottomBorderColor()) + ",\n" +
                "         " + str(leftBorderColor()) + "\n" +
                "     )\n";

        String noiseGrad = "";
        if ( !this.noiseColors.get().trim().isEmpty() )
            noiseGrad =
                "     .noise( noiseConf -> noiseConf\n" +
                "         .function(UI."+noise.get().name()+")\n" +
                "         .colors("+noiseColors.get()+")\n" +
                "         .clipTo(UI."+noiseArea.get()+")\n" +
                "     )\n";

        code.set(
            "panel(FILL)\n" +
            ".withStyle( it -> it\n" +
            "     .backgroundColor("+ str(backgroundColor) + ")\n" +
            "     .foundationColor(" + str(foundationColor) + ")\n" +
            ( drawSmiley().is(false) ? "" :
            "     .painter(UI.Layer.BACKGROUND, g2d -> {\n" +
            "         if ( vm.drawSmiley().is(false) ) return;\n" +
            "         int w = it.component().getWidth() - " + str(paddingLeft) + " - " + str(paddingRight) + " - 100;\n" +
            "         int h = it.component().getHeight() - " + str(paddingTop) + " - " + str(paddingBottom) + " - 100;\n" +
            "         int x = " + str(paddingLeft) + " + 50;\n" +
            "         int y = " + str(paddingTop) + " + 50;\n" +
            "         drawASmiley(g2d, x, y, w, h);\n" +
            "     })\n"
            ) +
            "     .padding(" + str(paddingTop) + ", " + str(paddingRight) + ", " + str(paddingBottom) + ", " + str(paddingLeft) + ")\n" +
            "     .margin(" + str(marginTop) + ", " + str(marginRight) + ", " + str(marginBottom) + ", " + str(marginLeft) + ")\n" +
            cornerRadius +
            borderWidth +
            (borderWidth.isEmpty() ? "" : borderColoring) +
            "     .shadowColor(" + str(shadowColor) + ")\n" +
            ( horizontalShadowOffset.is(0) ? "" : "     .shadowHorizontalOffset(" + str(horizontalShadowOffset) + ")\n" ) +
            ( verticalShadowOffset.is(0) ? "" : "     .shadowVerticalOffset(" + str(verticalShadowOffset) + ")\n" ) +
            ( shadowBlurRadius.is(0) ? "" : "     .shadowBlurRadius(" + str(shadowBlurRadius) + ")\n" ) +
            ( shadowSpreadRadius.is(0) ? "" : "     .shadowSpreadRadius(" + str(shadowSpreadRadius) + ")\n" ) +
            "     .shadowIsInset(" + str(shadowInset) + ")\n" +
            noiseGrad +
            ")\n" +
            ( drawSmiley.is(false) ? "" : "\n\n...\n\n" +
                    "void drawASmiley(Graphics2D g2d, int x, int y, int w, int h) {\n" +
                    "    // We crop the rectangle so that t is centered and squared:\n" +
                    "    int crop = Math.abs( w - h ) / 2;\n" +
                    "    if ( w > h ) {\n" +
                    "        x += crop;\n" +
                    "        w = h;\n" +
                    "    } else {\n" +
                    "        y += crop;\n" +
                    "        h = w;\n" +
                    "    }\n" +
                    "    g2d.setColor(Color.YELLOW);\n" +
                    "    g2d.fillOval(x, y, w, h);\n" +
                    "    g2d.setColor(Color.BLACK);\n" +
                    "    g2d.drawOval(x, y, w, h);\n" +
                    "    g2d.fillOval(x + w/4, y + h/4, w/8, h/8);\n" +
                    "    g2d.fillOval((int) (x + 2.5*w/4), y + h/4, w/8, h/8);\n" +
                    "    // Now a smile:\n" +
                    "    g2d.drawArc(x + w/4, y + 2*h/4, w/2, h/4, 0, -180);\n" +
                    "}"
                )
        );
    }

    private String str(Val<?> v) {
        if ( v.type() == Color.class ) {
            return str((Color) v.get());
        }
        return v.get().toString();
    }

    private String str(Color c) {
        return "new java.awt.Color(" + c.getRed() + "," + c.getGreen() + "," + c.getBlue() + "," + c.getAlpha() + ")";
    }
}
