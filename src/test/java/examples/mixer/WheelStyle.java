package examples.mixer;

import swingtree.UI;

/**
 *  How the pitch and modulation controls of the keyboard controller are shaped. Hardware
 *  keyboards have upright wheels, laptop setups often have flat touch ribbons. Switching
 *  between them turns the two sliders through {@code withOrientation(Val<UI.Axis>)}, labels
 *  and all, while the user keeps playing.
 */
public enum WheelStyle
{
    WHEELS ("Wheels",  UI.Axis.VERTICAL),
    RIBBONS("Ribbons", UI.Axis.HORIZONTAL);

    private final String  title;
    private final UI.Axis axis;

    WheelStyle( String title, UI.Axis axis ) {
        this.title = title;
        this.axis  = axis;
    }

    public UI.Axis axis() { return axis; }

    @Override
    public String toString() { return title; }
}
