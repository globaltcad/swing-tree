package swingtree.style;

import com.google.errorprone.annotations.Immutable;
import swingtree.UI;
import swingtree.layout.Bounds;
import swingtree.layout.Size;

import java.util.Objects;

/**
 *  An immutable snapshot of all the essential component state needed for rendering
 *  and configuring the component state.
 */
@Immutable
@SuppressWarnings("Immutable")
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
    private final Bounds    _currentBounds;
    private final Outline   _marginCorrection;
    private final LazyRef<RenderConf> _renderConf; // Computed constant, used for rendering!

    private boolean _wasAlreadyHashed = false;
    private int     _hashCode         = 0; // cached hash code


    ComponentConf(
            StyleConf styleConf,
            Bounds currentBounds,
            Outline marginCorrection
    ) {
        _styleConf        = Objects.requireNonNull(styleConf);
        _currentBounds    = Objects.requireNonNull(currentBounds);
        _marginCorrection = Objects.requireNonNull(marginCorrection);
        _renderConf = new LazyRef<>(this, RenderConf::new);
    }

    StyleConf style() { return _styleConf; }

    Bounds currentBounds() { return _currentBounds; }

    Outline areaMarginCorrection() { return _marginCorrection; }

    ComponentConf withSize( int width, int height ) {
        return new ComponentConf(
                   _styleConf,
                   Bounds.of(_currentBounds.location(), Size.of(width, height)),
                _marginCorrection
               );
    }

    /**
     *  Returns a new {@link ComponentConf} instance which only contains style information relevant
     *  to the provided {@link UI.Layer}. Style information on other layers is discarded.
     * @param layer The layer to retain.
     * @return A new {@link ComponentConf} instance which only contains style information relevant to the provided {@link UI.Layer}.
     */
    LayerRenderConf renderConfFor( UI.Layer layer ) {
        switch ( layer ) {
            case BACKGROUND: return _renderConf.get().backgroundConf();
            case CONTENT:    return _renderConf.get().contentConf();
            case BORDER:     return _renderConf.get().borderConf();
            case FOREGROUND: return _renderConf.get().foregroundConf();
        }
        throw new IllegalStateException("Unexpected UI.Layer: " + layer);
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName()+"[" +
                    "style="                + _styleConf + ", "+
                    "bounds="               + _currentBounds + ", "+
                    "areaMarginCorrection=" + _marginCorrection + ", "+
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
            && Objects.equals(_marginCorrection, other._marginCorrection);
    }

    @Override
    public int hashCode() {
        if ( _wasAlreadyHashed )
            return _hashCode;

        _hashCode = Objects.hash(_styleConf, _currentBounds, _marginCorrection);
        _wasAlreadyHashed = true;
        return _hashCode;
    }
}
