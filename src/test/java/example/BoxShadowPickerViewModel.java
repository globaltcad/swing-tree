package example;

import sprouts.Event;
import sprouts.Val;
import sprouts.Var;
import swingtree.style.Corner;
import swingtree.style.Edge;

import java.awt.*;

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
    private final Var<Corner>  borderCorner    = Var.of(Corner.EVERY).onAct(it -> repaint.fire() );
    private final Var<Edge>    borderEdge      = Var.of(Edge.EVERY).onAct(it -> repaint.fire() );
    private final Var<Integer> borderArcWidth  = Var.of(25).onAct( it -> repaint.fire() );
    private final Var<Integer> borderArcHeight = Var.of(25).onAct( it -> repaint.fire() );
    private final Var<Integer> borderThickness = Var.of(3).onAct( it -> repaint.fire() );
    private final Var<Color>   borderColor     = Var.of(new Color(0,0.4f,1)).onAct( it -> repaint.fire() );

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
        repaint.subscribe( () -> createCode() );
        createCode();
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
    public Var<Edge> borderEdge() { return borderEdge; }

    public Var<Integer> borderArcWidth() { return borderArcWidth; }

    public Var<Integer> borderArcHeight() {
        return borderArcHeight;
    }

    public Var<Integer> borderThickness() { return borderThickness; }

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
        code.set(
            "panel(FILL)\n" +
            ".withStyle( it ->\n" +
            "    it.style()\n" +
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
            "     .pad(" + str(paddingTop) + ", " + str(paddingRight) + ", " + str(paddingBottom) + ", " + str(paddingLeft) + ")\n" +
            "     .margin(" + str(marginTop) + ", " + str(marginRight) + ", " + str(marginBottom) + ", " + str(marginLeft) + ")\n" +
            ( borderArcWidth.is(0) && borderArcHeight.is(0) ? "" : "     .borderRadius(" + str(borderArcWidth) + ", " + str(borderArcHeight) + ")\n" ) +
            ( borderThickness.is(0) ? "" : "     .borderWidth(" + str(borderThickness) + ")\n" ) +
            "     .borderColor(" + str(borderColor) + ")\n" +
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
