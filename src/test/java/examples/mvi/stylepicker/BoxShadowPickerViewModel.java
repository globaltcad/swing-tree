package examples.mvi.stylepicker;

import com.google.errorprone.annotations.Immutable;
import sprouts.Event;
import sprouts.From;
import sprouts.Val;
import sprouts.Var;
import swingtree.UI;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

/**
 *  The view model for the {@link BoxShadowPickerView} which is a UI for configuring
 *  the style of a panel with a box shadow.
 *  This is also a good demonstration of how to use the MVI pattern with SwingTree.
 */
public final class BoxShadowPickerViewModel
{
    @Immutable
    public static final class BorderEdgeViewModel
    {
        public static BorderEdgeViewModel DEFAULT = new BorderEdgeViewModel(3, new Color(0,0.4f,1));
        private final int    borderWidth; //= 3;
        private final Color  borderColor; //= new Color(0,0.4f,1);

        public BorderEdgeViewModel(int borderWidth, Color borderColor) {
            this.borderWidth = borderWidth;
            this.borderColor = borderColor;
        }

        public int borderWidth() { return borderWidth; }
        public Color borderColor() { return borderColor; }
        BorderEdgeViewModel withBorderWidth(int borderWidth) {
            return new BorderEdgeViewModel(borderWidth, borderColor);
        }
        BorderEdgeViewModel withBorderColor(Color borderColor) {
            return new BorderEdgeViewModel(borderWidth, borderColor);
        }
    }

    public static class BorderCornerViewModel{
        public static final BorderCornerViewModel DEFAULT = new BorderCornerViewModel(25, 25);
        private final int borderArcWidth ; //= 25;
        private final int borderArcHeight; //= 25;

        public BorderCornerViewModel(int borderArcWidth, int borderArcHeight) {
            this.borderArcWidth = borderArcWidth;
            this.borderArcHeight = borderArcHeight;
        }

        public int borderArcWidth() { return borderArcWidth; }
        public int borderArcHeight() { return borderArcHeight; }
        BorderCornerViewModel withBorderArcWidth(int borderArcWidth) {
            return new BorderCornerViewModel(borderArcWidth, borderArcHeight);
        }
        BorderCornerViewModel withBorderArcHeight(int borderArcHeight) {
            return new BorderCornerViewModel(borderArcWidth, borderArcHeight);
        }
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

    // Smiley (For fun)
    private final boolean drawSmiley;// = false;

    private final String code;// = "";

    private BoxShadowPickerViewModel(
            int paddingTop, int paddingLeft, int paddingRight, int paddingBottom,
            int marginTop, int marginLeft, int marginRight, int marginBottom,
            UI.Edge borderEdge, UI.Corner borderCorner,
            Map<UI.Edge, BorderEdgeViewModel> edgeModels,
            Map<UI.Corner, BorderCornerViewModel> cornerModels,
            BorderEdgeViewModel currentEdgeModel,
            BorderCornerViewModel currentCornerModel,
            Color backgroundColor, Color foundationColor,
            int horizontalShadowOffset, int verticalShadowOffset,
            int shadowBlurRadius, int shadowSpreadRadius, Color shadowColor, boolean shadowInset,
            boolean drawSmiley, String code
    ) {
        this.paddingTop = paddingTop;
        this.paddingLeft = paddingLeft;
        this.paddingRight = paddingRight;
        this.paddingBottom = paddingBottom;
        this.marginTop = marginTop;
        this.marginLeft = marginLeft;
        this.marginRight = marginRight;
        this.marginBottom = marginBottom;
        this.borderEdge = borderEdge;
        this.borderCorner = borderCorner;
        this.edgeModels = edgeModels;
        this.cornerModels = cornerModels;
        this.currentEdgeModel = currentEdgeModel;
        this.currentCornerModel = currentCornerModel;
        this.backgroundColor = backgroundColor;
        this.foundationColor = foundationColor;
        this.horizontalShadowOffset = horizontalShadowOffset;
        this.verticalShadowOffset = verticalShadowOffset;
        this.shadowBlurRadius = shadowBlurRadius;
        this.shadowSpreadRadius = shadowSpreadRadius;
        this.shadowColor = shadowColor;
        this.shadowInset = shadowInset;
        this.drawSmiley = drawSmiley;
        this.code = code;
    }
    
    
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
        BoxShadowPickerViewModel ini =
                new BoxShadowPickerViewModel(
                    5, 5, 5, 5,
                    30, 35, 35, 30,
                    UI.Edge.EVERY, UI.Corner.EVERY,
                    edgeModels, cornerModels,
                    BorderEdgeViewModel.DEFAULT, BorderCornerViewModel.DEFAULT,
                    new Color(0.1f, 0.75f, 0.9f), new Color(1f,1f,1f),
                    0, 0, 6, 5, Color.DARK_GRAY, false,
                    false, ""
                );
        return ini.withCode(ini.createCode());
    }

    private BoxShadowPickerViewModel updateEdgeSelection(UI.Edge edge) {
        return this.withCurrentEdgeModel(edgeModels.get(edge));
    }

    private BoxShadowPickerViewModel updateCornerSelection(UI.Corner corner) {
        return this.withCurrentCornerModel(cornerModels.get(corner));
    }

    public int paddingTop() { return paddingTop; }
    public BoxShadowPickerViewModel withPaddingTop(int paddingTop) {
        return new BoxShadowPickerViewModel(
                paddingTop, paddingLeft, paddingRight, paddingBottom, marginTop, marginLeft, marginRight, marginBottom,
                borderEdge, borderCorner, edgeModels, cornerModels, currentEdgeModel, currentCornerModel, backgroundColor, 
                foundationColor, horizontalShadowOffset, verticalShadowOffset, shadowBlurRadius, shadowSpreadRadius, 
                shadowColor, shadowInset, drawSmiley, code
            );
    }

    public int paddingLeft() { return paddingLeft; }
    public BoxShadowPickerViewModel withPaddingLeft(int paddingLeft) {
        return new BoxShadowPickerViewModel(
                paddingTop, paddingLeft, paddingRight, paddingBottom, marginTop, marginLeft, marginRight, marginBottom,
                borderEdge, borderCorner, edgeModels, cornerModels, currentEdgeModel, currentCornerModel, backgroundColor, 
                foundationColor, horizontalShadowOffset, verticalShadowOffset, shadowBlurRadius, shadowSpreadRadius, 
                shadowColor, shadowInset, drawSmiley, code
            );
    }

    public int paddingRight() { return paddingRight; }
    public BoxShadowPickerViewModel withPaddingRight(int paddingRight) {
        return new BoxShadowPickerViewModel(
                paddingTop, paddingLeft, paddingRight, paddingBottom, marginTop, marginLeft, marginRight, marginBottom,
                borderEdge, borderCorner, edgeModels, cornerModels, currentEdgeModel, currentCornerModel, backgroundColor, 
                foundationColor, horizontalShadowOffset, verticalShadowOffset, shadowBlurRadius, shadowSpreadRadius, 
                shadowColor, shadowInset, drawSmiley, code
            );
    }

    public int paddingBottom() { return paddingBottom; }
    public BoxShadowPickerViewModel withPaddingBottom(int paddingBottom) {
        return new BoxShadowPickerViewModel(
                paddingTop, paddingLeft, paddingRight, paddingBottom, marginTop, marginLeft, marginRight, marginBottom,
                borderEdge, borderCorner, edgeModels, cornerModels, currentEdgeModel, currentCornerModel, backgroundColor, 
                foundationColor, horizontalShadowOffset, verticalShadowOffset, shadowBlurRadius, shadowSpreadRadius, 
                shadowColor, shadowInset, drawSmiley, code
            );
    }

    public int marginTop() { return marginTop; }
    public BoxShadowPickerViewModel withMarginTop(int marginTop) {
        return new BoxShadowPickerViewModel(
                paddingTop, paddingLeft, paddingRight, paddingBottom, marginTop, marginLeft, marginRight, marginBottom,
                borderEdge, borderCorner, edgeModels, cornerModels, currentEdgeModel, currentCornerModel, backgroundColor, 
                foundationColor, horizontalShadowOffset, verticalShadowOffset, shadowBlurRadius, shadowSpreadRadius, 
                shadowColor, shadowInset, drawSmiley, code
            );
    }

    public int marginLeft() { return marginLeft; }
    public BoxShadowPickerViewModel withMarginLeft(int marginLeft) {
        return new BoxShadowPickerViewModel(
                paddingTop, paddingLeft, paddingRight, paddingBottom, marginTop, marginLeft, marginRight, marginBottom,
                borderEdge, borderCorner, edgeModels, cornerModels, currentEdgeModel, currentCornerModel, backgroundColor, 
                foundationColor, horizontalShadowOffset, verticalShadowOffset, shadowBlurRadius, shadowSpreadRadius, 
                shadowColor, shadowInset, drawSmiley, code
            );
    }

    public int marginRight() { return marginRight; }
    public BoxShadowPickerViewModel withMarginRight(int marginRight) {
        return new BoxShadowPickerViewModel(
                paddingTop, paddingLeft, paddingRight, paddingBottom, marginTop, marginLeft, marginRight, marginBottom,
                borderEdge, borderCorner, edgeModels, cornerModels, currentEdgeModel, currentCornerModel, backgroundColor, 
                foundationColor, horizontalShadowOffset, verticalShadowOffset, shadowBlurRadius, shadowSpreadRadius, 
                shadowColor, shadowInset, drawSmiley, code
            );
    }

    public int marginBottom() { return marginBottom; }
    public BoxShadowPickerViewModel withMarginBottom(int marginBottom) {
        return new BoxShadowPickerViewModel(
                paddingTop, paddingLeft, paddingRight, paddingBottom, marginTop, marginLeft, marginRight, marginBottom,
                borderEdge, borderCorner, edgeModels, cornerModels, currentEdgeModel, currentCornerModel, backgroundColor, 
                foundationColor, horizontalShadowOffset, verticalShadowOffset, shadowBlurRadius, shadowSpreadRadius, 
                shadowColor, shadowInset, drawSmiley, code
            );
    }

    public UI.Corner borderCorner() { return borderCorner; }
    public BoxShadowPickerViewModel withBorderCorner(UI.Corner borderCorner) {
        return new BoxShadowPickerViewModel(
                paddingTop, paddingLeft, paddingRight, paddingBottom, marginTop, marginLeft, marginRight, marginBottom,
                borderEdge, borderCorner, edgeModels, cornerModels, currentEdgeModel, currentCornerModel, backgroundColor, 
                foundationColor, horizontalShadowOffset, verticalShadowOffset, shadowBlurRadius, shadowSpreadRadius, 
                shadowColor, shadowInset, drawSmiley, code
            )
            .updateCornerSelection(borderCorner);
    }

    public UI.Edge borderEdge() { return borderEdge; }
    public BoxShadowPickerViewModel withBorderEdge(UI.Edge borderEdge) {
        return new BoxShadowPickerViewModel(
                paddingTop, paddingLeft, paddingRight, paddingBottom, marginTop, marginLeft, marginRight, marginBottom,
                borderEdge, borderCorner, edgeModels, cornerModels, currentEdgeModel, currentCornerModel, backgroundColor, 
                foundationColor, horizontalShadowOffset, verticalShadowOffset, shadowBlurRadius, shadowSpreadRadius, 
                shadowColor, shadowInset, drawSmiley, code
            )
            .updateEdgeSelection(borderEdge);
    }

    public BorderEdgeViewModel currentEdgeModel() { return currentEdgeModel; }
    public BoxShadowPickerViewModel withCurrentEdgeModel(BorderEdgeViewModel currentEdgeModel) {
        Map<UI.Edge, BorderEdgeViewModel> newEdgeModels = new HashMap<>(edgeModels);
        newEdgeModels.put(borderEdge, currentEdgeModel);
        return new BoxShadowPickerViewModel(
                paddingTop, paddingLeft, paddingRight, paddingBottom, marginTop, marginLeft, marginRight, marginBottom,
                borderEdge, borderCorner, newEdgeModels, cornerModels, currentEdgeModel, currentCornerModel, backgroundColor,
                foundationColor, horizontalShadowOffset, verticalShadowOffset, shadowBlurRadius, shadowSpreadRadius, 
                shadowColor, shadowInset, drawSmiley, code
            );
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

    public BorderCornerViewModel currentCornerModel() { return currentCornerModel; }
    public BoxShadowPickerViewModel withCurrentCornerModel(BorderCornerViewModel currentCornerModel) {
        Map<UI.Corner, BorderCornerViewModel> newCornerModels = new HashMap<>(cornerModels);
        newCornerModels.put(borderCorner, currentCornerModel);
        return new BoxShadowPickerViewModel(
                paddingTop, paddingLeft, paddingRight, paddingBottom, marginTop, marginLeft, marginRight, marginBottom,
                borderEdge, borderCorner, edgeModels, newCornerModels, currentEdgeModel, currentCornerModel, backgroundColor,
                foundationColor, horizontalShadowOffset, verticalShadowOffset, shadowBlurRadius, shadowSpreadRadius, 
                shadowColor, shadowInset, drawSmiley, code
            );
    }

    public int arcWidthAt(UI.Corner corner) { return getCornerModel(corner).borderArcWidth(); }
    public int arcHeightAt(UI.Corner corner) { return getCornerModel(corner).borderArcHeight(); }

    private BorderCornerViewModel getCornerModel(UI.Corner corner) {
        // We check if the
        if ( this.borderCorner == UI.Corner.EVERY )
            return cornerModels.get(UI.Corner.EVERY);

        return cornerModels.get(corner);
    }

    public Color backgroundColor() { return backgroundColor; }
    public BoxShadowPickerViewModel withBackgroundColor(Color backgroundColor) {
        return new BoxShadowPickerViewModel(
                paddingTop, paddingLeft, paddingRight, paddingBottom, marginTop, marginLeft, marginRight, marginBottom,
                borderEdge, borderCorner, edgeModels, cornerModels, currentEdgeModel, currentCornerModel, backgroundColor, 
                foundationColor, horizontalShadowOffset, verticalShadowOffset, shadowBlurRadius, shadowSpreadRadius, 
                shadowColor, shadowInset, drawSmiley, code
            );
    }

    public Color foundationColor() { return foundationColor; }
    public BoxShadowPickerViewModel withFoundationColor(Color foundationColor) {
        return new BoxShadowPickerViewModel(
                paddingTop, paddingLeft, paddingRight, paddingBottom, marginTop, marginLeft, marginRight, marginBottom,
                borderEdge, borderCorner, edgeModels, cornerModels, currentEdgeModel, currentCornerModel, backgroundColor, 
                foundationColor, horizontalShadowOffset, verticalShadowOffset, shadowBlurRadius, shadowSpreadRadius, 
                shadowColor, shadowInset, drawSmiley, code
            );
    }

    public int horizontalShadowOffset() { return horizontalShadowOffset; }
    public BoxShadowPickerViewModel withHorizontalShadowOffset(int horizontalShadowOffset) {
        return new BoxShadowPickerViewModel(
                paddingTop, paddingLeft, paddingRight, paddingBottom, marginTop, marginLeft, marginRight, marginBottom,
                borderEdge, borderCorner, edgeModels, cornerModels, currentEdgeModel, currentCornerModel, backgroundColor, 
                foundationColor, horizontalShadowOffset, verticalShadowOffset, shadowBlurRadius, shadowSpreadRadius, 
                shadowColor, shadowInset, drawSmiley, code
            );
    }

    public int verticalShadowOffset() { return verticalShadowOffset; }
    public BoxShadowPickerViewModel withVerticalShadowOffset(int verticalShadowOffset) {
        return new BoxShadowPickerViewModel(
                paddingTop, paddingLeft, paddingRight, paddingBottom, marginTop, marginLeft, marginRight, marginBottom,
                borderEdge, borderCorner, edgeModels, cornerModels, currentEdgeModel, currentCornerModel, backgroundColor, 
                foundationColor, horizontalShadowOffset, verticalShadowOffset, shadowBlurRadius, shadowSpreadRadius, 
                shadowColor, shadowInset, drawSmiley, code
            );
    }

    public int shadowBlurRadius() { return shadowBlurRadius; }
    public BoxShadowPickerViewModel withShadowBlurRadius(int shadowBlurRadius) {
        return new BoxShadowPickerViewModel(
                paddingTop, paddingLeft, paddingRight, paddingBottom, marginTop, marginLeft, marginRight, marginBottom,
                borderEdge, borderCorner, edgeModels, cornerModels, currentEdgeModel, currentCornerModel, backgroundColor, 
                foundationColor, horizontalShadowOffset, verticalShadowOffset, shadowBlurRadius, shadowSpreadRadius, 
                shadowColor, shadowInset, drawSmiley, code
            );
    }

    public int shadowSpreadRadius() { return shadowSpreadRadius; }
    public BoxShadowPickerViewModel withShadowSpreadRadius(int shadowSpreadRadius) {
        return new BoxShadowPickerViewModel(
                paddingTop, paddingLeft, paddingRight, paddingBottom, marginTop, marginLeft, marginRight, marginBottom,
                borderEdge, borderCorner, edgeModels, cornerModels, currentEdgeModel, currentCornerModel, backgroundColor, 
                foundationColor, horizontalShadowOffset, verticalShadowOffset, shadowBlurRadius, shadowSpreadRadius, 
                shadowColor, shadowInset, drawSmiley, code
            );
    }

    public Color shadowColor() { return shadowColor; }
    public BoxShadowPickerViewModel withShadowColor(Color shadowColor) {
        return new BoxShadowPickerViewModel(
                paddingTop, paddingLeft, paddingRight, paddingBottom, marginTop, marginLeft, marginRight, marginBottom,
                borderEdge, borderCorner, edgeModels, cornerModels, currentEdgeModel, currentCornerModel, backgroundColor, 
                foundationColor, horizontalShadowOffset, verticalShadowOffset, shadowBlurRadius, shadowSpreadRadius, 
                shadowColor, shadowInset, drawSmiley, code
            );
    }

    public boolean shadowInset() { return shadowInset; }
    public BoxShadowPickerViewModel withShadowInset(boolean shadowInset) {
        return new BoxShadowPickerViewModel(
                paddingTop, paddingLeft, paddingRight, paddingBottom, marginTop, marginLeft, marginRight, marginBottom,
                borderEdge, borderCorner, edgeModels, cornerModels, currentEdgeModel, currentCornerModel, backgroundColor, 
                foundationColor, horizontalShadowOffset, verticalShadowOffset, shadowBlurRadius, shadowSpreadRadius, 
                shadowColor, shadowInset, drawSmiley, code
            );
    }

    public boolean drawSmiley() { return drawSmiley; }
    public BoxShadowPickerViewModel withDrawSmiley(boolean drawSmiley) {
        return new BoxShadowPickerViewModel(
                paddingTop, paddingLeft, paddingRight, paddingBottom, marginTop, marginLeft, marginRight, marginBottom,
                borderEdge, borderCorner, edgeModels, cornerModels, currentEdgeModel, currentCornerModel, backgroundColor, 
                foundationColor, horizontalShadowOffset, verticalShadowOffset, shadowBlurRadius, shadowSpreadRadius, 
                shadowColor, shadowInset, drawSmiley, code
            );
    }

    public String code() { return code; }
    public BoxShadowPickerViewModel withCode(String code) {
        return new BoxShadowPickerViewModel(
                paddingTop, paddingLeft, paddingRight, paddingBottom, marginTop, marginLeft, marginRight, marginBottom,
                borderEdge, borderCorner, edgeModels, cornerModels, currentEdgeModel, currentCornerModel, backgroundColor, 
                foundationColor, horizontalShadowOffset, verticalShadowOffset, shadowBlurRadius, shadowSpreadRadius, 
                shadowColor, shadowInset, drawSmiley, code
            );
    }

    private String createCode() {
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
