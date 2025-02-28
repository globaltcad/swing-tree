package swingtree.api;

import org.slf4j.Logger;

import java.awt.*;

class Constants
{
    static final Logger LOG = org.slf4j.LoggerFactory.getLogger("swingtree");
    static final Styler<?> STYLER_NONE = delegate -> delegate;
    static final AnimatedStyler<?> ANIMATED_STYLER_NONE = (state, delegate) -> delegate;
    static final Layout UNSPECIFIC_LAYOUT_CONSTANT = new Layout.Unspecific();
    static final Layout NONE_LAYOUT_CONSTANT = new Layout.None();
    static final Configurator<?> CONFIGURATOR_NONE = delegate -> delegate;
    static final ScrollIncrementSupplier SCROLLABLE_INCREMENT_SUPPLIER_NONE = (viewRectangle, orientation, direction) -> 0;
    static final Painter PAINTER_NONE = new Painter() {
                                            @Override
                                            public void paint(Graphics2D g2d) {
                                                // None
                                            }
                                            @Override
                                            public boolean canBeCached() { return true; }
                                            @Override public String toString() { return "none"; }
                                        };

    private Constants() {}

}
