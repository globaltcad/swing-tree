package swingtree.style;

import swingtree.UI;
import swingtree.layout.Bounds;
import swingtree.layout.Size;

import javax.swing.JComponent;
import javax.swing.border.Border;
import java.awt.Insets;
import java.util.Objects;

/**
 *  An immutable snapshot of all the essential component state needed for rendering
 *  and configuring the component state.
 */
final class ComponentConf
{
    public static ComponentConf none() {
        return new ComponentConf(
                    StyleConf.none(),
                    Bounds.none(),
                    Outline.none()
                );
    }

    private final StyleConf _styleConf;
    private final Bounds  _currentBounds;
    private final Outline _baseOutline;

    private boolean _wasAlreadyHashed = false;
    private int     _hashCode         = 0; // cached hash code


    private ComponentConf(
        StyleConf      styleConf,
        Bounds         currentBounds,
        Outline        baseOutline
    ) {
        _styleConf     = Objects.requireNonNull(styleConf);
        _currentBounds = Objects.requireNonNull(currentBounds);
        _baseOutline   = Objects.requireNonNull(baseOutline);
    }

    StyleConf style() { return _styleConf; }

    Bounds currentBounds() { return _currentBounds; }

    Outline baseOutline() { return _baseOutline; }

    ComponentConf with( StyleConf styleConf, JComponent component )
    {
        Outline outline = Outline.none();
        Border border = component.getBorder();
        if ( border instanceof StyleAndAnimationBorder ) {
            Insets base = ((StyleAndAnimationBorder<?>) border).getBaseInsets(true);
            outline = Outline.of(base.top, base.left, base.bottom, base.right);
        }

        boolean sameStyle   = _styleConf.equals(styleConf);
        boolean sameBounds  = _currentBounds.equals(component.getX(), component.getY(), component.getWidth(), component.getHeight());
        boolean sameOutline = _baseOutline.equals(outline);
        if ( sameStyle && sameBounds && sameOutline )
            return this;

        return new ComponentConf(
                   styleConf,
                   Bounds.of(component.getX(), component.getY(), component.getWidth(), component.getHeight()),
                   outline
               );
    }

    ComponentConf withSize( int width, int height ) {
        return new ComponentConf(
                   _styleConf,
                   Bounds.of(_currentBounds.location(), Size.of(width, height)),
                   _baseOutline
               );
    }

    /**
     *  Returns a new {@link ComponentConf} instance which only contains style information relevant
     *  to the provided {@link UI.Layer}. Style information on other layers is discarded.
     * @param layer The layer to retain.
     * @return A new {@link ComponentConf} instance which only contains style information relevant to the provided {@link UI.Layer}.
     */
    RenderConf toRenderConfFor(UI.Layer layer ) {
        return RenderConf.of(layer,this);
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName()+"[" +
                    "style="         + _styleConf + ", "+
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
        return Objects.equals(_styleConf, other._styleConf)
            && Objects.equals(_currentBounds, other._currentBounds)
            && Objects.equals(_baseOutline, other._baseOutline);
    }

    @Override
    public int hashCode() {
        if ( _wasAlreadyHashed )
            return _hashCode;

        _hashCode = Objects.hash(_styleConf, _currentBounds, _baseOutline);
        _wasAlreadyHashed = true;
        return _hashCode;
    }
}
