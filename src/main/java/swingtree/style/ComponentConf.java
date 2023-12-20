package swingtree.style;

import swingtree.UI;

import javax.swing.JComponent;
import javax.swing.border.Border;
import java.awt.Insets;
import java.util.Objects;

/**
 *  An immutable snapshot of essential component state needed for rendering
 *  the style of a component.
 *  This is immutable to use it as a basis for caching.
 *  When the snapshot changes compared to the previous one, the image buffer based
 *  render cache is being invalidated and the component is rendered again
 *  (potentially with a new cached image buffer).
 */
class ComponentConf
{
    private static final ComponentConf EMPTY = new ComponentConf(
                                                        Style.none(),
                                                        Bounds.none(),
                                                        Outline.none()
                                                    );

    public static ComponentConf none() { return EMPTY; }

    private final Style   _style;
    private final Bounds  _currentBounds;
    private final Outline _baseOutline;


    private ComponentConf(
        Style style,
        Bounds currentBounds,
        Outline baseOutline
    ) {
        _style         = Objects.requireNonNull(style);
        _currentBounds = Objects.requireNonNull(currentBounds);
        _baseOutline   = Objects.requireNonNull(baseOutline);
    }

    Style style() { return _style; }

    Bounds currentBounds() { return _currentBounds; }

    Outline baseOutline() { return _baseOutline; }

    ComponentConf with( Style style, JComponent component )
    {
        Outline outline = Outline.none();
        Border border = component.getBorder();
        if ( border instanceof StyleAndAnimationBorder ) {
            Insets base = ((StyleAndAnimationBorder<?>) border).getBaseInsets(true);
            outline = Outline.of(base.top, base.left, base.bottom, base.right);
        }

        boolean sameStyle   = _style.equals(style);
        boolean sameBounds  = _currentBounds.equals(component.getX(), component.getY(), component.getWidth(), component.getHeight());
        boolean sameOutline = _baseOutline.equals(outline);
        if ( sameStyle && sameBounds && sameOutline )
            return this;

        return new ComponentConf(
                        style,
                        Bounds.of(component.getX(), component.getY(), component.getWidth(), component.getHeight()),
                        outline
                    );
    }

    /**
     *  Returns a new {@link ComponentConf} instance which only contains style information relevant
     *  to the provided {@link UI.Layer}. Style information on other layers is discarded.
     * @param layer The layer to retain.
     * @return A new {@link ComponentConf} instance which only contains style information relevant to the provided {@link UI.Layer}.
     */
    ComponentConf onlyRetainingLayer( UI.Layer layer ) {
        return new ComponentConf(
                    _style.onlyRetainingLayer(layer),
                    _currentBounds,
                    _baseOutline
                );
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName()+"[" +
                    "style="         + _style         + ", "+
                    "bounds="        + _currentBounds + ", "+
                    "baseOutline="   + _baseOutline   + ", "+
                "]";
    }

    @Override
    public boolean equals( Object o ) {
        if ( o == this ) return true;
        if ( o == null ) return false;
        if ( o.getClass() != this.getClass() ) return false;
        ComponentConf other = (ComponentConf) o;
        return Objects.equals(_style, other._style)
            && Objects.equals(_currentBounds, other._currentBounds)
            && Objects.equals(_baseOutline, other._baseOutline);
    }

    @Override
    public int hashCode() {
        return Objects.hash(_style, _currentBounds, _baseOutline);
    }
}
