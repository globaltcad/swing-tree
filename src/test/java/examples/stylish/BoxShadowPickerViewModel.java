package examples.stylish;

import sprouts.Event;
import sprouts.Val;
import sprouts.Var;
import swingtree.UI;
import swingtree.components.JBox;
import swingtree.style.Corner;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class BoxShadowPickerViewModel
{
    private final Event repaint = Event.create();

    // Padding
    private final Var<Integer> paddingTop = Var.of(5).onAct( it -> repaint.fire() );
    private final Var<Integer> paddingLeft = Var.of(5).onAct( it -> repaint.fire() );
    private final Var<Integer> paddingRight = Var.of(5).onAct( it -> repaint.fire() );
    private final Var<Integer> paddingBottom = Var.of(5).onAct( it -> repaint.fire() );

    // Margin
    private final Var<Integer> marginTop = Var.of(30).onAct( it -> repaint.fire() );
    private final Var<Integer> marginLeft = Var.of(35).onAct( it -> repaint.fire() );
    private final Var<Integer> marginRight = Var.of(35).onAct( it -> repaint.fire() );
    private final Var<Integer> marginBottom = Var.of(30).onAct( it -> repaint.fire() );

    // Border
    private final Var<Color>   borderColor     = Var.of(new Color(0,0.4f,1)).onAct( it -> repaint.fire() );
    private final Var<UI.Edge>    borderEdge      = Var.of(UI.Edge.EVERY).onAct(it -> updateEdgeSelection(it.get()) );
    private final Var<Corner>  borderCorner    = Var.of(Corner.EVERY).onAct(it -> updateCornerSelection(it.get()) );

    private final Map<UI.Edge, BorderEdgeViewModel> edgeModels = new HashMap<>();
    private final Map<Corner, BorderCornerViewModel> cornerModels = new HashMap<>();

    private final Var<BorderEdgeViewModel> currentEdgeModel = Var.ofNullable(BorderEdgeViewModel.class, null);
    private final Var<BorderCornerViewModel> currentCornerModel = Var.ofNullable(BorderCornerViewModel.class, null);

    public class BorderEdgeViewModel
    {
        private final Var<Integer> borderWidth = Var.of(3).onAct(it -> repaint.fire() );

        public JComponent createView() {
            return
                UI.box("fill, wrap 2", "[shrink][grow]")
                .add(UI.label("Width:"))
                .add("growx", UI.slider(UI.Align.HORIZONTAL, 0, 100, borderWidth))
                .get(JBox.class);
        }
    }

    public class BorderCornerViewModel{

        private final Var<Integer> borderArcWidth  = Var.of(25).onAct( it -> repaint.fire() );
        private final Var<Integer> borderArcHeight = Var.of(25).onAct( it -> repaint.fire() );

        public JComponent createView() {
            return
                UI.box("fill, wrap 2", "[shrink][grow]")
                .add(UI.label("Width:"))
                .add("growx", UI.slider(UI.Align.HORIZONTAL, 0, 100, borderArcWidth))
                .add(UI.label("Height:"))
                .add("growx", UI.slider(UI.Align.HORIZONTAL, 0, 100, borderArcHeight))
                .get(JBox.class);
        }
    }

    // Background
    private final Var<Color> backgroundColor = Var.of(new Color(0.1f, 0.75f, 0.9f)).onAct( it -> repaint.fire() );
    private final Var<Color> foundationColor = Var.of(new Color(1f,1f,1f)).onAct(it -> repaint.fire() );

    // Box Shadow
    private final Var<Integer> horizontalShadowOffset = Var.of(0).onAct( it -> repaint.fire() );
    private final Var<Integer> verticalShadowOffset = Var.of(0).onAct( it -> repaint.fire() );
    private final Var<Integer> shadowBlurRadius = Var.of(6).onAct( it -> repaint.fire() );
    private final Var<Integer> shadowSpreadRadius = Var.of(5).onAct( it -> repaint.fire() );
    private final Var<Color> shadowColor = Var.of(Color.DARK_GRAY).onAct( it -> repaint.fire() );
    private final Var<Boolean> shadowInset = Var.of(false).onAct( it -> repaint.fire() );

    // Smiley (For fun)
    private final Var<Boolean> drawSmiley = Var.of(false).onAct( it -> repaint.fire() );

    private final Var<String> code = Var.of("");

    public BoxShadowPickerViewModel() {
        // Creating sub-view models
        for (UI.Edge edge : UI.Edge.values()) {
            BorderEdgeViewModel model = new BorderEdgeViewModel();
            edgeModels.put(edge, model);
            if (edge == UI.Edge.EVERY) {
                currentEdgeModel.set(model);
            }
        }
        for (Corner corner : Corner.values()) {
            BorderCornerViewModel model = new BorderCornerViewModel();
            cornerModels.put(corner, model);
            if (corner == Corner.EVERY) {
                currentCornerModel.set(model);
            }
        }
        repaint.subscribe( () -> createCode() );
        createCode();
    }

    private void updateEdgeSelection(UI.Edge edge) {
        currentEdgeModel.set(edgeModels.get(edge));
        repaint.fire();
    }

    private void updateCornerSelection(Corner corner) {
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
    public Var<Corner> borderCorner() { return borderCorner; }
    public Var<UI.Edge> borderEdge() { return borderEdge; }

    public Var<BorderEdgeViewModel> currentEdgeModel() { return currentEdgeModel; }

    public int leftBorderWidth() { return getEdgeModel(UI.Edge.LEFT).borderWidth.get(); }
    public int rightBorderWidth() { return getEdgeModel(UI.Edge.RIGHT).borderWidth.get(); }
    public int topBorderWidth() { return getEdgeModel(UI.Edge.TOP).borderWidth.get(); }
    public int bottomBorderWidth() { return getEdgeModel(UI.Edge.BOTTOM).borderWidth.get(); }

    private BoxShadowPickerViewModel.BorderEdgeViewModel getEdgeModel(UI.Edge edge) {
        // We check if the
        if ( currentEdgeModel.is(edgeModels.get(UI.Edge.EVERY)) )
            return currentEdgeModel.get();

        return edgeModels.get(edge);
    }

    public Var<BorderCornerViewModel> currentCornerModel() { return currentCornerModel; }

    public int arcWidthAt(Corner corner) { return getCornerModel(corner).borderArcWidth.get(); }
    public int arcHeightAt(Corner corner) { return getCornerModel(corner).borderArcHeight.get(); }

    private BoxShadowPickerViewModel.BorderCornerViewModel getCornerModel(Corner corner) {
        // We check if the
        if ( currentCornerModel.is(cornerModels.get(Corner.EVERY)) )
            return currentCornerModel.get();

        return cornerModels.get(corner);
    }

    public Var<Color> borderColor() { return borderColor; }

    public Var<Color> backgroundColor() { return backgroundColor; }

    public Var<Color> foundationColor() { return foundationColor; }

    public Var<Integer> horizontalShadowOffset() { return horizontalShadowOffset; }

    public Var<Integer> verticalShadowOffset() { return verticalShadowOffset; }

    public Var<Integer> shadowBlurRadius() { return shadowBlurRadius; }

    public Var<Integer> shadowSpreadRadius() { return shadowSpreadRadius; }

    public Var<Color> shadowColor() { return shadowColor; }

    public Var<Boolean> shadowInset() { return shadowInset; }

    public Var<Boolean> drawSmiley() { return drawSmiley; }

    public Var<String> code() { return code; }

    private void createCode() {
        String cornerRadius = "";
        if ( this.borderCorner.is(Corner.EVERY) ) {
            if ( arcWidthAt(Corner.TOP_LEFT) == arcHeightAt(Corner.TOP_LEFT) )
                cornerRadius = "     .borderRadius(" + arcWidthAt(Corner.TOP_LEFT) + ")\n";
            else
                cornerRadius = "     .borderRadius(" + arcWidthAt(Corner.TOP_LEFT) + ", " + arcHeightAt(Corner.TOP_LEFT) + ")\n";
        }
        else
            cornerRadius =
                "     .borderRadiusAt(Corner.TOP_LEFT, " + arcWidthAt(Corner.TOP_LEFT) + ", " + arcHeightAt(Corner.TOP_LEFT) + ")\n" +
                "     .borderRadiusAt(Corner.TOP_RIGHT, " + arcWidthAt(Corner.TOP_RIGHT) + ", " + arcHeightAt(Corner.TOP_RIGHT) + ")\n" +
                "     .borderRadiusAt(Corner.BOTTOM_LEFT, " + arcWidthAt(Corner.BOTTOM_LEFT) + ", " + arcHeightAt(Corner.BOTTOM_LEFT) + ")\n" +
                "     .borderRadiusAt(Corner.BOTTOM_RIGHT, " + arcWidthAt(Corner.BOTTOM_RIGHT) + ", " + arcHeightAt(Corner.BOTTOM_RIGHT) + ")\n";

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
                "     .borderWidthAt(Edge.LEFT, " + leftBorderWidth() + ")\n" +
                "     .borderWidthAt(Edge.TOP, " + topBorderWidth() + ")\n" +
                "     .borderWidthAt(Edge.RIGHT, " + rightBorderWidth() + ")\n" +
                "     .borderWidthAt(Edge.BOTTOM, " + bottomBorderWidth() + ")\n";

        code.set(
            "panel(FILL)\n" +
            ".withStyle( it ->\n" +
            "    it\n" +
            "     .backgroundColor("+ str(backgroundColor) + ")\n" +
            "     .foundationColor(" + str(foundationColor) + ")\n" +
            ( drawSmiley().is(false) ? "" :
            "     .backgroundPainter( g2d -> {\n" +
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
            (borderWidth.isEmpty() ? "" : "     .borderColor(" + str(borderColor) + ")\n") +
            "     .shadowColor(" + str(shadowColor) + ")\n" +
            ( horizontalShadowOffset.is(0) ? "" : "     .shadowHorizontalOffset(" + str(horizontalShadowOffset) + ")\n" ) +
            ( verticalShadowOffset.is(0) ? "" : "     .shadowVerticalOffset(" + str(verticalShadowOffset) + ")\n" ) +
            ( shadowBlurRadius.is(0) ? "" : "     .shadowBlurRadius(" + str(shadowBlurRadius) + ")\n" ) +
            ( shadowSpreadRadius.is(0) ? "" : "     .shadowSpreadRadius(" + str(shadowSpreadRadius) + ")\n" ) +
            "     .shadowIsInset(" + str(shadowInset) + ")\n" +
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
            Color c = (Color) v.get();
            return "new Color(" + c.getRed() + "," + c.getGreen() + "," + c.getBlue() + "," + c.getAlpha() + ")";
        }
        return v.get().toString();
    }
}
