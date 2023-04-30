package example;

import sprouts.Event;
import sprouts.Var;

import java.awt.*;

public class BockShadowPickerViewModel
{
    private final Event repaint = Event.create();

    // Padding
    private final Var<Integer> paddingTop = Var.of(30).onAct( it -> repaint.fire() );
    private final Var<Integer> paddingLeft = Var.of(30).onAct( it -> repaint.fire() );
    private final Var<Integer> paddingRight = Var.of(30).onAct( it -> repaint.fire() );
    private final Var<Integer> paddingBottom = Var.of(30).onAct( it -> repaint.fire() );

    // Border
    private final Var<Integer> borderArcWidth = Var.of(20).onAct( it -> repaint.fire() );
    private final Var<Integer> borderArcHeight = Var.of(20).onAct( it -> repaint.fire() );
    private final Var<Integer> borderThickness = Var.of(1).onAct( it -> repaint.fire() );
    private final Var<Color> borderColor = Var.of(Color.YELLOW).onAct( it -> repaint.fire() );

    // Background
    private final Var<Color> backgroundColor = Var.of(Color.GREEN).onAct( it -> repaint.fire() );

    // Box Shadow
    private final Var<Integer> horizontalShadowOffset = Var.of(0).onAct( it -> repaint.fire() );
    private final Var<Integer> verticalShadowOffset = Var.of(0).onAct( it -> repaint.fire() );
    private final Var<Integer> shadowBlurRadius = Var.of(60).onAct( it -> repaint.fire() );
    private final Var<Integer> shadowSpreadRadius = Var.of(10).onAct( it -> repaint.fire() );
    private final Var<Color> shadowColor = Var.of(Color.BLACK).onAct( it -> repaint.fire() );
    private final Var<Color> shadowBackgroundColor = Var.of(Color.RED).onAct( it -> repaint.fire() );
    private final Var<Boolean> shadowInset = Var.of(false).onAct( it -> repaint.fire() );


    public Event repaint() { return repaint; }

    public Var<Integer> paddingTop() {
        return paddingTop;
    }

    public Var<Integer> paddingLeft() {
        return paddingLeft;
    }

    public Var<Integer> paddingRight() {
        return paddingRight;
    }

    public Var<Integer> paddingBottom() {
        return paddingBottom;
    }

    public Var<Integer> borderArcWidth() {
        return borderArcWidth;
    }

    public Var<Integer> borderArcHeight() {
        return borderArcHeight;
    }

    public Var<Integer> borderThickness() {
        return borderThickness;
    }

    public Var<Color> borderColor() {
        return borderColor;
    }

    public Var<Color> backgroundColor() {
        return backgroundColor;
    }

    public Var<Integer> horizontalShadowOffset() {
        return horizontalShadowOffset;
    }

    public Var<Integer> verticalShadowOffset() {
        return verticalShadowOffset;
    }

    public Var<Integer> shadowBlurRadius() {
        return shadowBlurRadius;
    }

    public Var<Integer> shadowSpreadRadius() {
        return shadowSpreadRadius;
    }

    public Var<Color> shadowColor() {
        return shadowColor;
    }

    public Var<Color> shadowBackgroundColor() {
        return shadowBackgroundColor;
    }

    public Var<Boolean> shadowInset() {
        return shadowInset;
    }



}
