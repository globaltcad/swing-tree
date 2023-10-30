package swingtree;

import sprouts.Val;

import javax.swing.JCheckBox;
import java.util.Objects;

/**
 *  A SwingTree builder node designed for configuring {@link JCheckBox} instances.
 */
public final class UIForCheckBox<B extends JCheckBox> extends UIForAnyButton<UIForCheckBox<B>, B>
{
    private final BuilderState<B> _state;

    UIForCheckBox( BuilderState<B> state ) {
        Objects.requireNonNull(state, "state");
        _state = state;
    }

    @Override
    protected BuilderState<B> _state() {
        return _state;
    }
    
    @Override
    protected UIForCheckBox<B> _with( BuilderState<B> newState ) {
        return new UIForCheckBox<>(newState);
    }


    public UIForCheckBox<B> borderIsPaintedFlatIf(boolean borderPainted ) {
        return _with( thisComponent -> {
                   thisComponent.setBorderPaintedFlat(borderPainted);
               })
               ._this();
    }

    public UIForCheckBox<B> borderIsPaintedFlatIf(Val<Boolean> val ) {
        return _withOnShow( val, (thisComponent,v) -> {
                    thisComponent.setBorderPaintedFlat(v);
                })
                ._with( thisComponent -> {
                    thisComponent.setBorderPaintedFlat( val.get() );
                })
               ._this();
    }

}
