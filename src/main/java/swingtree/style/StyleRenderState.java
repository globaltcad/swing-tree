package swingtree.style;

import javax.swing.JComponent;
import java.util.Objects;

/**
 *  An immutable snapshot of essential component state needed for rendering
 *  the style of a component.
 *  This is immutable to use it as a basis for caching.
 *  When the snapshot changes compared to the previous one, the image buffer based
 *  render cache is being invalidated.
 */
public class StyleRenderState
{
    private static final StyleRenderState EMPTY = new StyleRenderState(
                                                        Style.none(),
                                                        Size.none(),
                                                        Size.none(),
                                                        Size.none(),
                                                        Size.none()
                                                    );

    public static StyleRenderState none() { return EMPTY; }

    private final Style _style;
    private final Size  _currentSize;
    private final Size  _preferredSize;
    private final Size  _minimumSize;
    private final Size  _maximumSize;


    private StyleRenderState(
        Style style,
        Size currentSize,
        Size preferredSize,
        Size minimumSize,
        Size maximumSize
    ) {
        _style        = Objects.requireNonNull(style);
        _currentSize  = Objects.requireNonNull(currentSize);
        _preferredSize= Objects.requireNonNull(preferredSize);
        _minimumSize  = Objects.requireNonNull(minimumSize);
        _maximumSize  = Objects.requireNonNull(maximumSize);
    }

    Style style() { return _style; }

    Size currentSize() { return _currentSize; }

    Size preferredSize() { return _preferredSize; }

    Size minimumSize() { return _minimumSize; }

    Size maximumSize() { return _maximumSize; }

    StyleRenderState style(Style style ) {
        return new StyleRenderState(style, _currentSize, _preferredSize, _minimumSize, _maximumSize);
    }

    StyleRenderState currentSize(Size currentSize ) {
        return new StyleRenderState(_style, currentSize, _preferredSize, _minimumSize, _maximumSize);
    }

    StyleRenderState preferredSize(Size preferredSize ) {
        return new StyleRenderState(_style, _currentSize, preferredSize, _minimumSize, _maximumSize);
    }

    StyleRenderState minimumSize(Size minimumSize ) {
        return new StyleRenderState(_style, _currentSize, _preferredSize, minimumSize, _maximumSize);
    }

    StyleRenderState maximumSize(Size maximumSize ) {
        return new StyleRenderState(_style, _currentSize, _preferredSize, _minimumSize, maximumSize);
    }

    StyleRenderState with(Style style, JComponent component ) {
        boolean sameStyle = _style.equals(style);
        boolean sameSize  = _currentSize.equals(Size.of(component.getWidth(), component.getHeight()));
        boolean samePref  = _preferredSize.equals(Size.of(component.getPreferredSize().width, component.getPreferredSize().height));
        boolean sameMin   = _minimumSize.equals(Size.of(component.getMinimumSize().width, component.getMinimumSize().height));
        boolean sameMax   = _maximumSize.equals(Size.of(component.getMaximumSize().width, component.getMaximumSize().height));
        if ( sameStyle && sameSize && samePref && sameMin && sameMax )
            return this;
        return new StyleRenderState(
            style,
            Size.of(component.getWidth(), component.getHeight()),
            Size.of(component.getPreferredSize().width, component.getPreferredSize().height),
            Size.of(component.getMinimumSize().width, component.getMinimumSize().height),
            Size.of(component.getMaximumSize().width, component.getMaximumSize().height)
        );
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName()+"[" +
                    "style="         + _style         +", "+
                    "currentSize="   + _currentSize   +", "+
                    "preferredSize=" + _preferredSize +", "+
                    "minimumSize="   + _minimumSize   +", "+
                    "maximumSize="   + _maximumSize   +
                "]";
    }

    @Override
    public boolean equals( Object o ) {
        if ( o == this ) return true;
        if ( o == null ) return false;
        if ( o.getClass() != this.getClass() ) return false;
        StyleRenderState that = (StyleRenderState)o;
        return Objects.equals(this._style,         that._style)         &&
               Objects.equals(this._currentSize,   that._currentSize)   &&
               Objects.equals(this._preferredSize, that._preferredSize) &&
               Objects.equals(this._minimumSize,   that._minimumSize)   &&
               Objects.equals(this._maximumSize,   that._maximumSize);
    }

    @Override
    public int hashCode() {
        return Objects.hash(_style, _currentSize, _preferredSize, _minimumSize, _maximumSize);
    }
}
