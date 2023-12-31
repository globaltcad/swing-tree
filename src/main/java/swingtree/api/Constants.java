package swingtree.api;

import java.awt.*;

class Constants
{
    static final Styler<?> STYLER_NONE = delegate -> delegate;
    static final AnimatedStyler<?> ANIMATED_STYLER_NONE = (state, delegate) -> delegate;

    static final Painter PAINTER_NONE = new Painter() {
                                            @Override
                                            public void paint(Graphics2D g2d) {
                                                // None
                                            }
                                            @Override public String toString() { return "none"; }
                                        };

    private Constants() {}

}
