package examples.mvi.stylepicker;

import com.google.errorprone.annotations.Immutable;
import lombok.*;
import lombok.experimental.Accessors;
import swingtree.UI;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

/**
 *  The view model for the {@link BoxShadowPickerView} which is a UI for configuring
 *  the style of a panel with a box shadow.
 *  This is also a good demonstration of how to use the MVI pattern with SwingTree.
 */
@With @Getter @Accessors( fluent = true )
@AllArgsConstructor @EqualsAndHashCode @ToString
public final class BoxShadowPickerViewModel
{
    @Immutable @With @Getter @Accessors( fluent = true )
    @AllArgsConstructor @EqualsAndHashCode @ToString
    public static final class BorderEdgeViewModel
    {
        public static BorderEdgeViewModel DEFAULT = new BorderEdgeViewModel(3, new Color(0,0.4f,1));
        private final int    borderWidth; //= 3;
        private final Color  borderColor; //= new Color(0,0.4f,1);
    }

    @Immutable @With @Getter @Accessors( fluent = true )
    @AllArgsConstructor @EqualsAndHashCode @ToString
    public static class BorderCornerViewModel{
        public static final BorderCornerViewModel DEFAULT = new BorderCornerViewModel(25, 25);
        private final int borderArcWidth ; //= 25;
        private final int borderArcHeight; //= 25;
    }

    // Padding
    private final int paddingTop   ; //= 5;
    private final int paddingLeft  ; //= 5;
    private final int paddingRight ; //= 5;
    private final int paddingBottom; //= 5;

    // Margin
    private final int marginTop   ; //= 30
    private final int marginLeft  ; //= 35
    private final int marginRight ; //= 35
    private final int marginBottom; //= 30

    // Border
    private final UI.Edge     borderEdge  ; //= UI.Edge.EVERY 
    private final UI.Corner   borderCorner; //= UI.Corner.EVERY 

    private final Map<UI.Edge,   BorderEdgeViewModel>   edgeModels  ;// = new HashMap<>();
    private final Map<UI.Corner, BorderCornerViewModel> cornerModels;// = new HashMap<>();

    private final BorderEdgeViewModel    currentEdgeModel  ; //= null;
    private final BorderCornerViewModel  currentCornerModel; //= null;

    // Background
    private final Color backgroundColor; //= new Color(0.1f, 0.75f, 0.9f);
    private final Color foundationColor; //= new Color(1f,1f,1f));

    // Box Shadow
    private final int     horizontalShadowOffset; //= 0;
    private final int     verticalShadowOffset  ; //= 0;
    private final int     shadowBlurRadius      ; //= 6;
    private final int     shadowSpreadRadius    ; //= 5;
    private final Color   shadowColor           ; //= Color.DARK_GRAY;
    private final boolean shadowInset           ; //= false;

    private final UI.NoiseType noise;
    private final String noiseColors;
    private final UI.ComponentArea noiseArea;

    // Smiley (For fun)
    private final boolean drawSmiley;// = false;


    public static BoxShadowPickerViewModel ini() {
        Map<UI.Edge, BorderEdgeViewModel> edgeModels = new HashMap<>();
        Map<UI.Corner, BorderCornerViewModel> cornerModels = new HashMap<>();
        // Creating sub-view models
        for (UI.Edge edge : UI.Edge.values()) {
            BorderEdgeViewModel model = BorderEdgeViewModel.DEFAULT;
            edgeModels.put(edge, model);
        }
        for (UI.Corner corner : UI.Corner.values()) {
            BorderCornerViewModel model = BorderCornerViewModel.DEFAULT;
            cornerModels.put(corner, model);
        }
        return new BoxShadowPickerViewModel(
                    5, 5, 5, 5,
                    30, 35, 35, 30,
                    UI.Edge.EVERY, UI.Corner.EVERY,
                    edgeModels, cornerModels,
                    BorderEdgeViewModel.DEFAULT, BorderCornerViewModel.DEFAULT,
                    new Color(0.1f, 0.75f, 0.9f), new Color(1f,1f,1f),
                    0, 0, 6, 5, Color.DARK_GRAY, false,
                    UI.NoiseType.TISSUE, "", UI.ComponentArea.ALL, false
        );
    }

    private BoxShadowPickerViewModel updateEdgeSelection(UI.Edge edge) {
        return this.updateCurrentEdgeModel(edgeModels.get(edge));
    }

    private BoxShadowPickerViewModel updateCornerSelection(UI.Corner corner) {
        return this.updateCurrentCornerModel(cornerModels.get(corner));
    }

    public BoxShadowPickerViewModel updateBorderCorner(UI.Corner borderCorner) {
        return withBorderCorner(borderCorner)
            .updateCornerSelection(borderCorner);
    }

    public BoxShadowPickerViewModel updateBorderEdge(UI.Edge borderEdge) {
        return withBorderEdge(borderEdge)
            .updateEdgeSelection(borderEdge);
    }

    public BoxShadowPickerViewModel updateCurrentEdgeModel(BorderEdgeViewModel currentEdgeModel) {
        Map<UI.Edge, BorderEdgeViewModel> newEdgeModels = new HashMap<>(edgeModels);
        newEdgeModels.put(borderEdge, currentEdgeModel);
        return withEdgeModels(newEdgeModels)
                .withCurrentEdgeModel(currentEdgeModel);
    }

    public int leftBorderWidth() { return getEdgeModel(UI.Edge.LEFT).borderWidth(); }
    public int rightBorderWidth() { return getEdgeModel(UI.Edge.RIGHT).borderWidth(); }
    public int topBorderWidth() { return getEdgeModel(UI.Edge.TOP).borderWidth(); }
    public int bottomBorderWidth() { return getEdgeModel(UI.Edge.BOTTOM).borderWidth(); }

    public Color topBorderColor() { return getEdgeModel(UI.Edge.TOP).borderColor(); }
    public Color rightBorderColor() { return getEdgeModel(UI.Edge.RIGHT).borderColor(); }
    public Color bottomBorderColor() { return getEdgeModel(UI.Edge.BOTTOM).borderColor(); }
    public Color leftBorderColor() { return getEdgeModel(UI.Edge.LEFT).borderColor(); }

    private BorderEdgeViewModel getEdgeModel(UI.Edge edge) {
        if ( this.borderEdge == UI.Edge.EVERY )
            return edgeModels.get(UI.Edge.EVERY);

        return edgeModels.get(edge);
    }

    public BoxShadowPickerViewModel updateCurrentCornerModel(BorderCornerViewModel currentCornerModel) {
        Map<UI.Corner, BorderCornerViewModel> newCornerModels = new HashMap<>(cornerModels);
        newCornerModels.put(borderCorner, currentCornerModel);
        return withCornerModels(newCornerModels)
                .withCurrentCornerModel(currentCornerModel);
    }

    public int arcWidthAt(UI.Corner corner) { return getCornerModel(corner).borderArcWidth(); }
    public int arcHeightAt(UI.Corner corner) { return getCornerModel(corner).borderArcHeight(); }

    private BorderCornerViewModel getCornerModel(UI.Corner corner) {
        // We check if the
        if ( this.borderCorner == UI.Corner.EVERY )
            return cornerModels.get(UI.Corner.EVERY);

        return cornerModels.get(corner);
    }

    public String createCode() {
        String cornerRadius = "";
        if ( this.borderCorner == UI.Corner.EVERY ) {
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
        if ( this.borderEdge == UI.Edge.EVERY ) {
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

        return (
            "panel(FILL)\n" +
            ".withStyle( it -> it\n" +
            "     .backgroundColor("+ str(backgroundColor) + ")\n" +
            "     .foundationColor(" + str(foundationColor) + ")\n" +
            ( !drawSmiley() ? "" :
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
            ( horizontalShadowOffset == 0 ? "" : "     .shadowHorizontalOffset(" + str(horizontalShadowOffset) + ")\n" ) +
            ( verticalShadowOffset == 0 ? "" : "     .shadowVerticalOffset(" + str(verticalShadowOffset) + ")\n" ) +
            ( shadowBlurRadius == 0 ? "" : "     .shadowBlurRadius(" + str(shadowBlurRadius) + ")\n" ) +
            ( shadowSpreadRadius == 0 ? "" : "     .shadowSpreadRadius(" + str(shadowSpreadRadius) + ")\n" ) +
            "     .shadowIsInset(" + shadowInset + ")\n" +
            ")\n" +
            ( !drawSmiley ? "" : "\n\n...\n\n" +
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

    private String str(Number v) {
        return v.toString();
    }

    private String str(Color c) {
        return "new java.awt.Color(" + c.getRed() + "," + c.getGreen() + "," + c.getBlue() + "," + c.getAlpha() + ")";
    }
}
